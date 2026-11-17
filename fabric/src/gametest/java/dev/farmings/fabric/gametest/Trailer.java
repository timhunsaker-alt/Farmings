package dev.farmings.fabric.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import dev.farmings.entity.CattleEntity;
import dev.farmings.entity.HorseEntity;
import dev.farmings.entity.PigEntity;
import dev.farmings.farm.crops.FieldCrop;
import dev.farmings.farm.gantry.GantryTool;
import dev.farmings.gantry.GantryBlocks;
import dev.farmings.gantry.GantryCabEntity;
import dev.farmings.gantry.GantryPartBlock;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.husbandry.HorseBreed;
import dev.farmings.husbandry.PigBreed;
import dev.wildcatter.multiblock.BigParts;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The trailer's shots (FARMINGS_ONLY=trailer): the farm staged on a real generated world at golden hour, each shot a still at the start
 * and the end of a short camera move (1920 x 1080, no HUD) and a film of the move. The sites are levelled pads blended into the seed's
 * own plains; every block and animal is the mod's or vanilla's, built the way the scenes in {@link FarmingsClientGameTest} build them.
 * FARMINGS_TRAILER_SHOTS=1,3 runs only those shots; the stills go to FARMINGS_TRAILER_OUT (default: the run folder's {@code trailer}),
 * the films to {@code fabric/build/scene-videos/trailer_*.mov}. Same shape as Tailings' trailer scene.
 */
final class Trailer {
	private static final String SEED = System.getenv().getOrDefault("FARMINGS_TRAILER_SEED", "farmings");
	private static final Path OUT = Path.of(System.getenv().getOrDefault("FARMINGS_TRAILER_OUT", "trailer")).toAbsolutePath();
	private static final int W = 1920, H = 1080;
	/** Ticks of each filmed camera move. */
	private static final int MOVE = 180;

	private final ClientGameTestContext context;
	private TestServerContext server;

	Trailer(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[farmings-trailer] " + line);
	}

	private static boolean wanted(int shot) {
		String only = System.getenv("FARMINGS_TRAILER_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(Integer.toString(shot));
	}

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(16);
			client.options.graphicsPreset().set(GraphicsPreset.FANCY);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.cloudStatus().set(CloudStatus.FANCY);
			client.options.entityDistanceScaling().set(3.0);
			client.options.fov().set(70);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
		});
		// a 16:9 window for the films (the stills render at 1920 x 1080 whatever the window)
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("weather clear");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			BlockPos plains = server.computeOnServer(srv -> {
				var r = srv.overworld().findClosestBiome3d(h -> h.is(ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace("plains"))),
						new BlockPos(0, 64, 0), 3200, 32, 64);
				return r == null ? new BlockPos(0, 64, 0) : r.getFirst();
			});
			log("plains at " + plains.toShortString());
			server.runCommand("tp @p " + plains.getX() + " 160 " + plains.getZ());
			context.waitTicks(100);
			List<String> failed = new ArrayList<>();
			// the sites start 200 apart and never overlap (site() keeps 72 clear of the others), so no stable or barn claims another
			// site's animals (they look 48 out)
			BlockPos[] near = {plains, plains.offset(0, 0, 200), plains.offset(0, 0, 200), plains.offset(0, 0, -200)};
			BlockPos farmstead = null;
			for (int i = 0; i < 4; i++) {
				if (!wanted(i + 1)) {
					continue;
				}
				try {
					switch (i) {
						case 0 -> fields(near[0]);
						case 1, 2 -> {
							if (farmstead == null) {
								farmstead = farmstead(near[1]);
							}
							if (i == 1) {
								cattle(farmstead);
							} else {
								barns(farmstead);
							}
						}
						default -> horses(near[3]);
					}
				} catch (RuntimeException | AssertionError ex) {
					log("shot " + (i + 1) + " failed: " + ex);
					ex.printStackTrace();
					failed.add(Integer.toString(i + 1));
				}
			}
			log("done; failed shots " + failed);
		}
	}

	// ---- the camera ----

	/** Where the camera stands (feet) and the point it looks at. */
	private record Cam(double x, double y, double z, double tx, double ty, double tz) {
		Cam lerp(Cam o, double t) {
			return new Cam(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t, tx + (o.tx - tx) * t, ty + (o.ty - ty) * t, tz + (o.tz - tz) * t);
		}

		static Cam of(BlockPos o, double x, double y, double z, double tx, double ty, double tz) {
			return new Cam(o.getX() + x, o.getY() + y, o.getZ() + z, o.getX() + tx, o.getY() + ty, o.getZ() + tz);
		}
	}

	private void put(Cam c) {
		double dx = c.tx - c.x, dy = c.ty - c.y - 1.62, dz = c.tz - c.z;
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", c.x, c.y, c.z, yaw, pitch));
	}

	private void settle(int ticks) {
		context.waitTicks(ticks);
		try {
			context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 400);
		} catch (RuntimeException | AssertionError ex) {
			log("sections still compiling: " + ex.getMessage());
		}
		context.waitTicks(10);
	}

	private void still(String name) {
		Path at = context.takeScreenshot(TestScreenshotOptions.of(name).withSize(W, H).withDestinationDir(OUT).disableCounterPrefix());
		log("still " + at);
	}

	/** A shot: the start still, the filmed move from {@code a} to {@code b} (eased), the end still. */
	private void shot(String id, Cam a, Cam b) {
		// loose items (plants that popped off the levelled ground's edge) out of the shot
		server.runOnServer(srv -> srv.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(a.tx, a.ty, a.tz, a.tx, a.ty, a.tz).inflate(120, 60, 120)).forEach(net.minecraft.world.entity.Entity::discard));
		put(a);
		settle(60);
		still(id + "_a");
		Filming film = context.computeOnClient(client -> Filming.start(client, "trailer_" + id));
		for (int t = 0; t <= MOVE; t++) {
			double s = (double) t / MOVE;
			put(a.lerp(b, s * s * (3 - 2 * s)));
			context.waitTick();
		}
		film.stop();
		settle(5);
		still(id + "_b");
	}

	private void time(int t) {
		server.runCommand("time set " + t);
	}

	// ---- the ground (as Tailings' trailer) ----

	private static boolean loose(BlockState s) {
		return s.isAir() || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || (s.canBeReplaced() && s.getFluidState().isEmpty()) || s.is(Blocks.SNOW)
				|| s.is(Blocks.BAMBOO) || s.is(Blocks.CACTUS) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.PUMPKIN) || s.is(Blocks.MELON) || s.is(BlockTags.FLOWERS);
	}

	/** The first free block over the natural ground at a column: trees and plants not counted. */
	private static int surface(ServerLevel level, int x, int z) {
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y - 1, z);
		while (p.getY() > level.getMinY() && loose(level.getBlockState(p))) {
			p.move(0, -1, 0);
		}
		return p.getY() + 1;
	}

	private static boolean wet(ServerLevel level, int x, int z) {
		return !level.getBlockState(new BlockPos(x, surface(level, x, z) - 1, z)).getFluidState().isEmpty();
	}

	/** A dry, fairly even site near (x, z) for a w by d pad, searched outward in rings: its corner at the ground's middle height. */
	private static BlockPos site(ServerLevel level, int x, int z, int w, int d) {
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		for (int ring = 0; ring <= 5 && (best == null || bestScore > 3); ring++) {
			for (int i = -ring; i <= ring; i++) {
				for (int j = -ring; j <= ring; j++) {
					if (Math.max(Math.abs(i), Math.abs(j)) != ring) {
						continue;
					}
					int cx = x + i * 48, cz = z + j * 48;
					if (taken(cx, cz, w, d)) {
						continue;
					}
					List<Integer> hs = new ArrayList<>();
					boolean dry = true;
					for (int a = 0; a <= 4 && dry; a++) {
						for (int b = 0; b <= 4 && dry; b++) {
							int px = cx + a * w / 4, pz = cz + b * d / 4;
							if (wet(level, px, pz)) {
								dry = false;
							}
							hs.add(surface(level, px, pz));
						}
					}
					if (!dry) {
						continue;
					}
					hs.sort(Comparator.naturalOrder());
					int mid = hs.get(hs.size() / 2);
					if (mid < level.getSeaLevel() + 2 || mid > 110) {
						continue;
					}
					double score = hs.get(hs.size() - 2) - hs.get(1);
					if (score < bestScore) {
						bestScore = score;
						best = new BlockPos(cx, mid, cz);
					}
				}
			}
		}
		if (best == null) {
			best = new BlockPos(x, Math.max(surface(level, x, z), level.getSeaLevel() + 2), z);
		}
		log("site near " + x + "," + z + ": " + best.toShortString() + ", unevenness " + bestScore);
		USED.add(new int[] {best.getX(), best.getZ(), best.getX() + w, best.getZ() + d});
		return best;
	}

	/** The sites already built on (x0, z0, x1, z1), so a later pad keeps clear of them with its blend and a margin for the animals. */
	private static final List<int[]> USED = new ArrayList<>();

	private static boolean taken(int x, int z, int w, int d) {
		int m = 72;
		for (int[] u : USED) {
			if (x < u[2] + m && x + w > u[0] - m && z < u[3] + m && z + d > u[1] - m) {
				return true;
			}
		}
		return false;
	}

	private static void forceChunks(ServerLevel level, int x0, int z0, int x1, int z1) {
		for (int cx = Math.floorDiv(x0, 16); cx <= Math.floorDiv(x1, 16); cx++) {
			for (int cz = Math.floorDiv(z0, 16); cz <= Math.floorDiv(z1, 16); cz++) {
				level.setChunkForced(cx, cz, true);
			}
		}
	}

	/** Levels the rectangle to ground at g (the first air), cleared {@code clear} above; a ring {@code blend} wide eases back to the land. */
	private static void pad(ServerLevel level, int x0, int z0, int x1, int z1, int g, int blend, int clear) {
		forceChunks(level, x0 - blend, z0 - blend, x1 + blend, z1 + blend);
		for (int x = x0 - blend; x <= x1 + blend; x++) {
			for (int z = z0 - blend; z <= z1 + blend; z++) {
				int d = Math.max(Math.max(x0 - x, x - x1), Math.max(z0 - z, z - z1));
				int nat = surface(level, x, z);
				int h = g;
				if (d > 0) {
					double t = (double) d / (blend + 1);
					t = t * t * (3 - 2 * t);
					h = (int) Math.round(g + (nat - g) * t);
				}
				column(level, x, z, h, Math.min(nat, h) - 4, Math.max(nat, h) + (d > 0 ? 24 : clear));
			}
		}
	}

	/** Ground up to h (grass on top) from {@code from}, air from h to {@code to}. */
	private static void column(ServerLevel level, int x, int z, int h, int from, int to) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int yy = from; yy < h; yy++) {
			BlockState s = yy == h - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : yy >= h - 4 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
			level.setBlock(p.set(x, yy, z), s, Block.UPDATE_CLIENTS);
		}
		for (int yy = h; yy <= to; yy++) {
			if (!level.getBlockState(p.set(x, yy, z)).isAir()) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	private static void fence(ServerLevel level, BlockPos o, int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (x == x0 || x == x1 || z == z0 || z == z1) {
					level.setBlockAndUpdate(o.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState());
				}
			}
		}
	}

	// ---- 1. fields: the field gantry spreading fertilizer between ripe fields ----

	/**
	 * Four fields 56 rows long side by side: ripe barley, the gantry's field of barley seedlings (span 12 between its rails), tall corn
	 * and oats. The gantry spreads Wildcatter TSP (a 1 t bag is 100 spreads), so the seedlings grow behind it as it comes down the field.
	 */
	private void fields(BlockPos near) {
		time(12300);
		int len = 56;
		BlockPos[] ob = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, near.getX(), near.getZ(), 64, len + 8);
			BlockPos o = s.offset(24, 0, 4);     // the gantry's west rail at x 0, its home row at z 0
			pad(level, o.getX() - 26, o.getZ() - 4, o.getX() + 34, o.getZ() + len + 4, o.getY(), 14, 24);
			int g = o.getY();
			java.util.Random r = new java.util.Random(7);
			FieldCrop[] crop = {FieldCrop.BARLEY, FieldCrop.BARLEY, FieldCrop.CORN, FieldCrop.OATS};
			int[][] cols = {{-24, -2}, {1, 12}, {15, 26}, {29, 32}};
			for (int f = 0; f < crop.length; f++) {
				for (int x = cols[f][0]; x <= cols[f][1]; x++) {
					for (int z = 0; z < len; z++) {
						int age = f == 1 ? r.nextInt(2) : f == 2 ? 7 : 6 + r.nextInt(2);
						level.setBlock(o.offset(x, -1, z), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
						level.setBlock(o.offset(x, 0, z), dev.farmings.crops.FieldCrops.block(crop[f]).defaultBlockState().setValue(CropBlock.AGE, age), Block.UPDATE_CLIENTS);
					}
				}
			}
			BlockState rail = GantryBlocks.FIELD_RAIL.defaultBlockState().setValue(GantryPartBlock.AXIS, Direction.Axis.Z);
			for (int z = 0; z < len; z++) {
				level.setBlockAndUpdate(o.offset(0, 0, z), rail);
				level.setBlockAndUpdate(o.offset(13, 0, z), rail);
			}
			level.setBlockAndUpdate(o.offset(0, 1, 0), GantryBlocks.GANTRY_TRUCK.defaultBlockState());
			level.setBlockAndUpdate(o.offset(13, 1, 0), GantryBlocks.GANTRY_TRUCK.defaultBlockState());
			for (int x = 0; x <= 13; x++) {
				level.setBlockAndUpdate(o.offset(x, 2, 0), GantryBlocks.GANTRY_BEAM.defaultBlockState().setValue(GantryPartBlock.AXIS, Direction.Axis.X));
			}
			level.setBlockAndUpdate(o.offset(0, 3, 0), GantryBlocks.GANTRY_CAB.defaultBlockState());
			level.setBlockAndUpdate(o.offset(0, 0, len), GantryBlocks.HEADLAND_STOP.defaultBlockState());
			var c = (GantryCabEntity) level.getBlockEntity(o.offset(0, 3, 0));
			c.forcePowerForTest(true);
			var bag = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("wildcatter", "tsp_bag"));
			c.addInput(new ItemStack(bag, 8));
			ob[0] = o;
		});
		BlockPos o = ob[0];
		put(Cam.of(o, 6, 14, len + 14, 6, 0, 20));
		settle(80);
		server.runOnServer(srv -> {
			var c = (GantryCabEntity) srv.overworld().getBlockEntity(o.offset(0, 3, 0));
			c.fitTool(GantryTool.SPREADER);
			c.toggle();
			log("gantry: " + c.readout());
		});
		context.waitTicks(150);     // into the field before the first still: it comes on toward the camera through the move
		shot("01_fields", Cam.of(o, 9.5, 9, len + 9, 6.5, 1, 26), Cam.of(o, 4.5, 6, len + 4, 6.5, 2, 32));
		log("gantry after: " + server.computeOnServer(srv -> {
			for (int z = 0; z <= len; z++) {
				if (srv.overworld().getBlockEntity(o.offset(0, 3, z)) instanceof GantryCabEntity c) {
					return "at row " + z + ": " + c.readout();
				}
			}
			return "gliding";
		}));
	}

	// ---- 2 and 3. the farmstead: a freestall barn and corral, a pig barn, a stable ----

	/**
	 * The farmstead, built once for shots 2 and 3: a 4-stall-a-side freestall barn with hay and water and cows in it, a corral of wild
	 * cattle beside it (claimed, baited), a pig barn with its pasture and a stable with its paddock, both out by day. Returns the barn's
	 * controller.
	 */
	private BlockPos farmstead(BlockPos near) {
		BlockPos[] cb = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, near.getX(), near.getZ(), 48, 56);
			BlockPos c = s.offset(24, 1, 30);     // barn controller at ground + 1, as FarmingsClientGameTest.barn
			int g = s.getY();
			pad(level, c.getX() - 24, c.getZ() - 30, c.getX() + 24, c.getZ() + 24, g, 14, 20);
			// the freestall barn, with hay and water, and cows
			FarmingsClientGameTest.buildBarn(level, c, 4);
			int row = c.getX() - dev.farmings.husbandry.barn.BarnLayout.alleyLength(4) + 4;
			for (int dir = -1; dir <= 1; dir += 2) {
				((TroughEntity) BigParts.entity(level, new BlockPos(c.getX() + 1, g, c.getZ() + dir * 2))).pourBucket();
				for (int i = 0; i < 4; i++) {
					if (BigParts.entity(level, new BlockPos(row + 1 + 2 * i, g + 1, c.getZ() + dir * 5)) instanceof FeederEntity f) {
						f.addBlock();
						f.addBlock();
					}
				}
			}
			CattleBreed[] barnCows = {CattleBreed.HOLSTEIN, CattleBreed.HOLSTEIN, CattleBreed.JERSEY, CattleBreed.BROWN_SWISS, CattleBreed.HOLSTEIN, CattleBreed.JERSEY};
			for (int i = 0; i < barnCows.length; i++) {
				CattleEntity cow = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
				// a founder of her breed (genes and coat), as the coats scene makes them; the coat shows only with a genome
				cow.initFounder(barnCows[i], Sex.FEMALE, 4100 + i, 4 * 365, barnCows[i].matureWeightKg, level.getGameTime());
				cow.setVisualGenome(cow.visualGenome());
				cow.setPos(c.getX() - 3.5 - i, g + 0.2, c.getZ() + (i % 2 == 0 ? 0.5 : -0.5));
				level.addFreshEntity(cow);
			}
			// the corral east of the barn: a fence ring 13 x 12, its post, its one-way gate on the south side, bait and water
			BlockPos k = new BlockPos(c.getX() + 6, g, c.getZ() - 6);
			fence(level, k, 0, 0, 12, 11);
			level.setBlockAndUpdate(k.offset(6, 0, 11), dev.farmings.corral.CorralBlocks.CORRAL_GATE.defaultBlockState().setValue(dev.farmings.corral.CorralGateBlock.FACING, Direction.NORTH));
			BlockPos post = k.offset(1, 0, 1);
			level.setBlockAndUpdate(post, dev.farmings.corral.CorralBlocks.CORRAL_POST.defaultBlockState());
			level.setBlockAndUpdate(k.offset(6, 0, 3), dev.farmings.corral.CorralBlocks.BAIT_TROUGH.defaultBlockState());
			((dev.farmings.corral.BaitTroughEntity) level.getBlockEntity(k.offset(6, 0, 3))).addBlock();
			((dev.farmings.corral.BaitTroughEntity) level.getBlockEntity(k.offset(6, 0, 3))).addBlock();
			FarmingsClientGameTest.barnPart(level, BarnParts.WATER_TROUGH, k.offset(9, 0, 6));
			if (level.getBlockEntity(BigParts.owner(level, k.offset(9, 0, 6))) instanceof TroughEntity t) {
				t.pourBucket();
			}
			((dev.farmings.corral.CorralPostEntity) level.getBlockEntity(post)).claim(srv.getPlayerList().getPlayers().get(0).getUUID());
			// the breeds with coat genes (Angus and Wagyu have none yet, so they would show a default coat)
			Object[][] corral = {{CattleBreed.JERSEY, Sex.FEMALE, 430.0}, {CattleBreed.BROWN_SWISS, Sex.MALE, 900.0}, {CattleBreed.GYR, Sex.FEMALE, 420.0},
					{CattleBreed.HOLSTEIN, Sex.FEMALE, 650.0}, {CattleBreed.JERSEY, Sex.FEMALE, 120.0}, {CattleBreed.GYR, Sex.FEMALE, 400.0}, {CattleBreed.BROWN_SWISS, Sex.FEMALE, 140.0}};
			for (int i = 0; i < corral.length; i++) {
				CattleEntity cow = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
				double kg = (double) corral[i][2];
				cow.initFounder((CattleBreed) corral[i][0], (Sex) corral[i][1], 4200 + i, kg < 200 ? 90 : 4 * 365, kg, level.getGameTime());
				cow.setVisualGenome(cow.visualGenome());
				cow.setWild(true);
				cow.snapTo(k.getX() + 2.5 + (i * 3) % 9, g, k.getZ() + 3.5 + (i % 3) * 2.5, i * 50, 0);
				level.addFreshEntity(cow);
			}
			// the pig barn south-west (as FarmingsClientGameTest.pigBarn): pens, a farrowing crate under a heat lamp, feeder, drinker, pasture, slurry
			int ox = c.getX() - 18, oz = c.getZ() + 10;
			java.util.function.BiFunction<Integer, Integer, BlockPos> at = (x, z) -> new BlockPos(ox + x, g, oz + z);
			fence(level, at.apply(0, 0), 5, -1, 16, 8);
			level.setBlockAndUpdate(at.apply(1, 2), dev.farmings.piggery.PigBarnBlocks.PIG_BARN_CONTROLLER.defaultBlockState());
			for (int x = 2; x <= 4; x++) {
				for (int z = 1; z <= 3; z++) {
					level.setBlockAndUpdate(at.apply(x, z), dev.farmings.piggery.PigBarnBlocks.SLATTED_FLOOR.defaultBlockState());
				}
			}
			level.setBlockAndUpdate(at.apply(5, 1), dev.farmings.piggery.PigBarnBlocks.FARROWING_CRATE.defaultBlockState());
			level.setBlockAndUpdate(at.apply(5, 3), dev.farmings.shed.ShedBlocks.PASTURE_GATE.defaultBlockState());
			level.setBlockAndUpdate(at.apply(5, 1).above(2), dev.farmings.piggery.PigBarnBlocks.HEAT_LAMP.defaultBlockState());
			FarmingsClientGameTest.barnPart(level, BarnParts.HAY_FEEDER, at.apply(2, 0));
			FarmingsClientGameTest.barnPart(level, BarnParts.WATER_TROUGH, at.apply(3, 4));
			level.setBlockAndUpdate(at.apply(1, 3), dev.farmings.piggery.PigBarnBlocks.SLURRY_PUMP.defaultBlockState());
			level.setBlockAndUpdate(at.apply(1, 4), dev.farmings.piggery.PigBarnBlocks.SLURRY_PIPE.defaultBlockState());
			level.setBlockAndUpdate(at.apply(1, 5), dev.farmings.piggery.PigBarnBlocks.SLURRY_PIPE.defaultBlockState());
			level.setBlockAndUpdate(at.apply(1, 6), dev.farmings.piggery.PigBarnBlocks.SLURRY_STORE.defaultBlockState());
			if (level.getBlockEntity(at.apply(5, 1).above(2)) instanceof dev.farmings.piggery.HeatLampEntity lamp) {
				lamp.forceRunning(true);
			}
			if (level.getBlockEntity(at.apply(1, 2)) instanceof dev.farmings.piggery.PigBarnControllerEntity barn) {
				barn.forceDayForTest(true);
				barn.refresh(level);
				log("pig barn: " + barn.status());
			}
			var sow = FarmingsClientGameTest.pig(level, PigBreed.LARGE_WHITE, Sex.FEMALE, 1900, 400);
			sow.setNoAi(false);
			sow.setFarrowedForTest(20, 10);
			sow.snapTo(ox + 3.5, g, oz + 2.5, 90, 0);
			for (int i = 0; i < 7; i++) {
				var p = FarmingsClientGameTest.pig(level, PigBreed.values()[i % PigBreed.values().length], i % 3 == 0 ? Sex.MALE : Sex.FEMALE, 1910 + i, i < 3 ? 120 : 400);
				p.setNoAi(false);
				p.snapTo(ox + 7.5 + (i % 4) * 2.2, g, oz + 1.5 + (i / 4) * 3, i * 40, 0);
			}
			// the stable north-west (as HorseGameTests.buildStable): stall floor 3 x 8, rack, trough, a gate to a fenced paddock
			int sx = c.getX() - 18, sz = c.getZ() - 19;
			java.util.function.BiFunction<Integer, Integer, BlockPos> st = (x, z) -> new BlockPos(sx + x, g, sz + z);
			fence(level, st.apply(0, 0), 5, -1, 16, 10);
			level.setBlockAndUpdate(st.apply(1, 2), dev.farmings.stable.StableBlocks.STABLE_CONTROLLER.defaultBlockState());
			for (int x = 2; x <= 4; x++) {
				for (int z = 1; z <= 8; z++) {
					level.setBlockAndUpdate(st.apply(x, z), dev.farmings.stable.StableBlocks.HORSE_STALL.defaultBlockState());
				}
			}
			level.setBlockAndUpdate(st.apply(5, 3), dev.farmings.shed.ShedBlocks.PASTURE_GATE.defaultBlockState());
			FarmingsClientGameTest.barnPart(level, BarnParts.HAY_FEEDER, st.apply(2, 0));
			FarmingsClientGameTest.barnPart(level, BarnParts.WATER_TROUGH, st.apply(3, 9));
			if (level.getBlockEntity(st.apply(1, 2)) instanceof dev.farmings.stable.StableControllerEntity stable) {
				stable.forceDayForTest(true);
				stable.refresh(level);
				log("stable: " + stable.status());
			}
			HorseBreed[] hb = {HorseBreed.THOROUGHBRED, HorseBreed.ARABIAN, HorseBreed.QUARTER_HORSE};
			for (int i = 0; i < 3; i++) {
				var h = FarmingsClientGameTest.horse(level, hb[i], Sex.FEMALE, 3100 + i, 6 * 365);
				h.setNoAi(false);
				h.snapTo(sx + 8.5 + i * 2.5, g, sz + 3.5 + i * 2, 120, 0);
			}
			cb[0] = c;
		});
		return cb[0];
	}

	/** Shot 2: wild cattle settling in the corral by the barn, from a farmer's eye at the corral fence, pushing in. */
	private void cattle(BlockPos c) {
		time(12200);
		BlockPos k = new BlockPos(c.getX() + 6, c.getY() - 1, c.getZ() - 6);
		put(Cam.of(k, 9, 1, 17, 5, 1, 4));
		settle(120);
		shot("02_cattle", Cam.of(k, 10.5, 0.6, 17.5, 5.5, 1.2, 4.5), Cam.of(k, 8.5, 0.9, 13.5, 5.5, 1.2, 4.5));
	}

	/** Shot 3: the barns from above, a crane down across the farmstead: the freestall barn, the pig barn and the stable with their animals. */
	private void barns(BlockPos c) {
		time(11900);
		put(Cam.of(c, 6, 9, 16, -7, 0, 0));
		settle(60);
		shot("03_barns", Cam.of(c, 7, 9, 17, -7, 0, 0), Cam.of(c, 2, 7, 14, -8, 0, -1));
	}

	// ---- 4. horses: the race track, with the herd grazing in it and on the hills beyond ----

	private void horses(BlockPos near) {
		time(12300);
		BlockPos[] ob = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, near.getX(), near.getZ(), 80, 48);
			BlockPos o = s.offset(40, 0, 24);     // the track's centre
			int g = o.getY();
			pad(level, o.getX() - 40, o.getZ() - 22, o.getX() + 40, o.getZ() + 22, g, 16, 20);
			BlockPos gate = FarmingsClientGameTest.raceTrack(level, o.getX(), o.getZ(), g);
			((dev.farmings.racing.StartGateEntity) level.getBlockEntity(gate)).refresh(level);
			HorseBreed[] hb = {HorseBreed.THOROUGHBRED, HorseBreed.ARABIAN, HorseBreed.QUARTER_HORSE, HorseBreed.THOROUGHBRED, HorseBreed.QUARTER_HORSE};
			int[][] coats = {{2, 2, 0, 0, 0, 0, 0}, {2, 0, 0, 0, 0, 0, 0}, {0, 0, 1, 0, 0, 0, 0}, {2, 2, 0, 0, 1, 0, 0}, {2, 0, 0, 1, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0}};
			long seed = 3200;
			// on the track's south straight, near the camera, and in the infield: mares, foals and a stallion
			double[][] spots = {{-6, 9}, {-2, 10}, {3, 9.5}, {8, 10.5}, {-10, 3}, {-4, 2}, {2, -1}, {6, 3}, {12, 1}, {-14, -3}};
			for (int i = 0; i < spots.length; i++) {
				HorseBreed b = hb[i % hb.length];
				var h = FarmingsClientGameTest.horse(level, b, i == 3 ? Sex.MALE : Sex.FEMALE, seed++, i == 5 || i == 8 ? 60 : 5 * 365);
				h.setGenesForTest(FarmingsClientGameTest.coat(h.genes(), coats[i % coats.length][0], coats[i % coats.length][1], coats[i % coats.length][2],
						coats[i % coats.length][3], coats[i % coats.length][4], coats[i % coats.length][5], coats[i % coats.length][6]));
				h.setNoAi(i % 3 == 0);
				h.snapTo(o.getX() + spots[i][0], g, o.getZ() + spots[i][1], 90 + i * 37, 0);
			}
			// a herd on the land beyond the track, on its own ground
			for (int i = 0; i < 9; i++) {
				int x = o.getX() - 30 + i * 8, z = o.getZ() - 44 - (i % 3) * 7;
				int y = surface(level, x, z);
				var h = FarmingsClientGameTest.horse(level, hb[i % hb.length], i == 4 ? Sex.MALE : Sex.FEMALE, seed++, i % 4 == 1 ? 80 : 4 * 365);
				h.setGenesForTest(FarmingsClientGameTest.coat(h.genes(), coats[(i + 2) % coats.length][0], coats[(i + 2) % coats.length][1], coats[(i + 2) % coats.length][2],
						coats[(i + 2) % coats.length][3], coats[(i + 2) % coats.length][4], coats[(i + 2) % coats.length][5], coats[(i + 2) % coats.length][6]));
				h.setNoAi(false);
				h.snapTo(x + 0.5, y, z + 0.5, i * 41, 0);
			}
			ob[0] = o;
		});
		BlockPos o = ob[0];
		put(Cam.of(o, 0, 3, 24, 0, 1, 0));
		settle(120);
		// low from the outside of the south straight, rising and pushing over the rail toward the herd
		shot("04_horses", Cam.of(o, 13.5, 1.4, 17.5, -3, 2, 1), Cam.of(o, 8.5, 3.2, 14.5, -4, 1.5, 0));
	}
}
