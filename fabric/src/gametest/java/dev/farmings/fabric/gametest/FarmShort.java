package dev.farmings.fabric.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.barn.BarnControllerEntity;
import dev.farmings.barn.DriveEntity;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import dev.farmings.digester.DigesterBlocks;
import dev.farmings.digester.DigesterEntity;
import dev.farmings.entity.CattleEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.husbandry.barn.BarnLayout;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.multiblock.BigParts;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * {@code farmShort} (FARMINGS_ONLY=farmShort): the shots for the vertical short "From the barn to the grid", on the music videos' generated
 * world (fixed seed, its plains), at golden hour. One dairy: a 4-stall freestall barn with eight cows, its scraper and collection pit; a feed
 * pit against the pit feeding the full-size digester with its gas holder; the holder piped to a Wildcatter gas set; and the set's 480 V
 * cable back to the digester's mixer and to the barn's scraper drive. Nothing is forced on: the set starts on the holder's biogas, and the
 * mixer and the scraper run on its power. The holder starts full (20 real days of a dairy's manure stepped at set-up, as the
 * genset_burns_farm_biogas test does).
 * <p>
 * Portrait world shots (486 x 864 points; a 1080 x 1920 start still). Recorded with {@link Filming}, or with FARMINGS_FRAMES set (a locked
 * screen) saved as a 1080 x 1920 frame a tick into FARMINGS_SHORT_OUT/frames/&lt;shot&gt;/ (20 frames a second). Every event a film shows is
 * logged as {@code [farmings-short] event <shot> <what> at <s> s}, its second from the film's start. FARMINGS_SHORT_SHOTS picks shots by number.
 */
final class FarmShort {
	private static final String SEED = "farmings";
	private static final Path OUT = Path.of(System.getenv().getOrDefault("FARMINGS_SHORT_OUT", "farm-short")).toAbsolutePath();
	private static final boolean FRAMES = System.getenv("FARMINGS_FRAMES") != null && !System.getenv("FARMINGS_FRAMES").isBlank();
	/** Late golden hour, the sun low in the west, down the barn's alley. */
	private static final int GOLDEN = 12250;

	private final ClientGameTestContext context;
	private TestServerContext server;
	private BlockPos c;
	private int g;
	private BlockPos tank;
	private BlockPos panelAt;
	private BlockPos breakerAt;
	private final List<BlockPos> lamps = new ArrayList<>();
	private final List<CattleEntity> cows = new ArrayList<>();
	private String filmId = "";
	private int frame = -1;
	private long filmStart;

	FarmShort(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[farmings-short] " + line);
	}

	private static boolean wanted(int shot) {
		String only = System.getenv("FARMINGS_SHORT_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(Integer.toString(shot));
	}

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(12);
			client.options.graphicsPreset().set(GraphicsPreset.FANCY);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.cloudStatus().set(CloudStatus.FANCY);
			client.options.entityDistanceScaling().set(3.0);
			client.options.fov().set(70);
			client.options.bobView().set(false);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
		});
		// the tall window: 9:16, the largest that fits whole on the laptop's screen (1512 x 982 points)
		context.getInput().resizeWindow(486, 864);
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
			// the music video's dairy site, 160 east of the plains point
			int sx = plains.getX() + 160, sz = plains.getZ();
			server.runCommand("tp @p " + sx + " 160 " + sz);
			context.waitTicks(100);
			build(sx, sz);
			shots();
		}
	}

	// ---- the set ----

	private void build(int sx, int sz) {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int ground = MusicVideo.surface(level, sx + 20, sz + 18);
			g = Math.max(ground, level.getSeaLevel() + 2);
			int cx = sx + 26, cz = sz + 18;
			MusicVideo.pad(level, sx, sz, sx + 46, sz + 36, g, 14, 20);
			MusicVideo.meadow(level, sx - 30, sz - 30, sx + 76, sz + 66, 41, 0.3, 0.01);
			c = new BlockPos(cx, g + 1, cz);
			// the barn, as the music video's dairy: stone under it, the pit sunk at the east end
			for (int x = cx - 24; x <= cx + 2; x++) {
				for (int z = cz - 8; z <= cz + 8; z++) {
					boolean pit = x >= cx && x <= cx + 1 && z >= cz - 1 && z <= cz + 1;
					if (!pit) {
						level.setBlockAndUpdate(new BlockPos(x, g - 1, z), Blocks.STONE.defaultBlockState());
					}
					for (int h = 0; h < 6; h++) {
						level.setBlock(new BlockPos(x, g + h, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			FarmingsClientGameTest.buildBarn(level, c, 4);
			for (int dir = -1; dir <= 1; dir += 2) {
				if (BigParts.entity(level, new BlockPos(cx + 1, g, cz + dir * 2)) instanceof TroughEntity t) {
					t.pourBucket();
				}
				int row = cx - BarnLayout.alleyLength(4) + 4;
				for (int i = 0; i < 4; i++) {
					if (BigParts.entity(level, new BlockPos(row + 1 + 2 * i, g + 1, cz + dir * 5)) instanceof FeederEntity f) {
						f.addBlock();
						f.addBlock();
					}
				}
			}
			// the digester: tank T (3 x 3 x 3, main cell at the bottom centre) east of the pit, the feed pit between them touching both,
			// laid out round T as the fields scene lays it out (FarmingsClientGameTest, "6. a digester ...")
			tank = new BlockPos(cx + 4, g, cz);
			for (int x = cx + 3; x <= cx + 14; x++) {
				for (int z = cz - 8; z <= cz + 12; z++) {
					level.setBlockAndUpdate(new BlockPos(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState());
					for (int h = 0; h < 8; h++) {
						level.setBlock(new BlockPos(x, g + h, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			FarmingsClientGameTest.bigPart(level, DigesterBlocks.DIGESTER_TANK, tank);
			level.setBlockAndUpdate(tank.offset(0, 3, 0), DigesterBlocks.GAS_HOLDER.defaultBlockState());
			level.setBlockAndUpdate(tank.offset(-2, 0, 0), DigesterBlocks.FEED_PIT.defaultBlockState());
			level.setBlockAndUpdate(tank.offset(2, 0, 0), DigesterBlocks.DIGESTATE_TANK.defaultBlockState());
			level.setBlockAndUpdate(tank.offset(3, 0, 0), DigesterBlocks.SCREW_PRESS.defaultBlockState());
			var d = (DigesterEntity) level.getBlockEntity(tank);
			d.digester().feed(20_000);     // a dairy's manure, 20 real days on: the holder full
			d.forceRunning(true);
			d.step(20);
			d.forceRunning(false);         // from here the mixer runs only on the gas set's power
			d.digester().feed(2_000);      // today's manure, fresh in the tank: the mixer has work as soon as it has power
			var gasLine = BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("gas_line"));
			for (BlockPos p : new BlockPos[] {tank.offset(0, 3, 1), tank.offset(0, 3, 2), tank.offset(0, 2, 2), tank.offset(0, 1, 2), tank.offset(0, 0, 2)}) {
				level.setBlockAndUpdate(p, gasLine.defaultBlockState());
			}
			var cable = BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("cable_industrial")).defaultBlockState();
			panelAt = tank.offset(5, 0, 3);
			// 480 V from the set's panel: north past the digester's east side, then on round the barn to its scraper drive
			for (int z = tank.getZ() + 3; z >= cz - 7; z--) {
				level.setBlockAndUpdate(new BlockPos(tank.getX() + 6, g, z), cable);
			}
			for (int x = tank.getX() + 5; x >= tank.getX() + 2; x--) {
				level.setBlockAndUpdate(new BlockPos(x, g, tank.getZ() - 1), cable);
			}
			// and buried, from under that run to under the tank's main cell: a cable against only its side cells left the mixer with no
			// voltage (found filming, 2026-11-19), so the feed goes to the main cell, under its bottom centre
			for (BlockPos p : new BlockPos[] {tank.offset(2, -1, -1), tank.offset(1, -1, -1), tank.offset(0, -1, -1), tank.offset(0, -1, 0)}) {
				level.setBlockAndUpdate(p, cable);
			}
			int xd = cx - BarnLayout.alleyLength(4);
			for (int x = tank.getX() + 5; x >= xd - 1; x--) {
				level.setBlockAndUpdate(new BlockPos(x, g, cz - 7), cable);
			}
			for (int z = cz - 6; z <= cz; z++) {
				level.setBlockAndUpdate(new BlockPos(xd - 1, g, z), cable);
			}
			level.setBlockAndUpdate(new BlockPos(xd - 1, g + 1, cz), cable);
			dev.wildcatter.block.GensetSkid.build(level, panelAt, Direction.NORTH, true);
			// the barn's lights: a 480 V branch through a breaker (open) to a dry-type transformer by the barn's south side, its 208Y/120 V
			// side to three lamps on wire posts along the stalls (phase A to neutral, as Wildcatter's own lamp scenes wire them)
			List<BlockPos> branch = new ArrayList<>();
			breakerAt = new BlockPos(tank.getX() + 6, g, cz + 5);
			for (int z = cz + 4; z <= cz + 9; z++) {
				branch.add(new BlockPos(tank.getX() + 6, g, z));
			}
			for (int x = tank.getX() + 5; x >= cx - 2; x--) {
				branch.add(new BlockPos(x, g, cz + 9));
			}
			branch.add(new BlockPos(cx - 2, g, cz + 8));
			branch.add(new BlockPos(cx - 2, g, cz + 7));
			branch.add(new BlockPos(cx - 2, g + 1, cz + 7));
			branch.add(new BlockPos(cx - 3, g + 1, cz + 7));     // onto the transformer's top: its 480 V side
			for (BlockPos p : branch) {
				if (!level.getBlockState(p).isAir()) {
					log("branch blocked at " + p.toShortString() + " by " + level.getBlockState(p));
					continue;
				}
				level.setBlockAndUpdate(p, p.equals(breakerAt) ? BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("breaker_3p_industrial")).defaultBlockState()
						.setValue(dev.wildcatter.block.BreakerBlock.FACING, Direction.SOUTH).setValue(dev.wildcatter.block.BreakerBlock.POSITION, dev.wildcatter.block.BreakerBlock.Position.OFF) : cable);
			}
			BlockPos xf = new BlockPos(cx - 3, g, cz + 7);
			level.setBlockAndUpdate(xf, BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("dry_type_transformer")).defaultBlockState());
			var hot = BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("wool_wire_hot")).defaultBlockState();
			var neutral = BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("wool_wire_neutral")).defaultBlockState();
			var lamp = BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("electric_lamp")).defaultBlockState();
			for (int x = cx - 4; x >= cx - 16; x--) {
				level.setBlockAndUpdate(new BlockPos(x, g, cz + 7), hot);          // out of its west side
			}
			level.setBlockAndUpdate(xf.below(), neutral);                           // out of its bottom, buried
			for (int x = cx - 3; x >= cx - 16; x--) {
				level.setBlockAndUpdate(new BlockPos(x, g - 1, cz + 8), neutral);
			}
			for (int x = cx - 7; x >= cx - 15; x -= 4) {
				level.setBlockAndUpdate(new BlockPos(x, g + 1, cz + 7), hot);
				level.setBlockAndUpdate(new BlockPos(x, g + 2, cz + 7), hot);
				level.setBlockAndUpdate(new BlockPos(x, g + 3, cz + 7), lamp);
				for (int y = g; y <= g + 3; y++) {
					level.setBlockAndUpdate(new BlockPos(x, y, cz + 8), neutral);
				}
				lamps.add(new BlockPos(x, g + 3, cz + 7));
			}
			var barn = (BarnControllerEntity) level.getBlockEntity(c);
			barn.refresh(level);
			// eight cows at their stalls, Holsteins and Jerseys
			CattleBreed[] breeds = {CattleBreed.HOLSTEIN, CattleBreed.JERSEY, CattleBreed.HOLSTEIN, CattleBreed.BROWN_SWISS,
					CattleBreed.HOLSTEIN, CattleBreed.JERSEY, CattleBreed.HOLSTEIN, CattleBreed.HOLSTEIN};
			int row = xd + 4;
			for (int i = 0; i < 8; i++) {
				int side = i < 4 ? -1 : 1;
				CattleEntity cow = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
				cow.initFounder(breeds[i], Sex.FEMALE, 7100L + i, 4 * 365, breeds[i].matureWeightKg, level.getGameTime());
				cow.setCalvedForDisplay(40);
				cow.snapTo(row + 1 + 2 * (i % 4) + 0.5, g + 0.2, cz + side * 3 + 0.5, side < 0 ? 180 : 0, 0);
				level.addFreshEntity(cow);
				cow.settle(level);
				cows.add(cow);
			}
			barn.tend(level, 20);
			MusicVideo.discardItems(level, c, 60);
			log("site " + sx + "," + sz + " g " + g + "; barn " + barn.formed() + " " + barn.readout() + "; digester " + d.readout());
		});
		List<int[]> spots = MusicVideo.ring(c.getX() - 34, c.getZ() - 20, c.getX() + 18, c.getZ() + 20, 10, 12, 9);
		for (int[] s : spots) {
			int y = server.computeOnServer(srv -> MusicVideo.surface(srv.overworld(), s[0], s[1]));
			try {
				server.runCommand("place feature minecraft:" + (s[0] % 3 == 0 ? "birch" : "oak") + " " + s[0] + " " + y + " " + s[1]);
			} catch (RuntimeException | AssertionError ex) {
				log("no tree at " + s[0] + "," + s[1]);
			}
		}
		context.waitTicks(60);
		server.runOnServer(srv -> ((BarnControllerEntity) srv.overworld().getBlockEntity(c)).tend(srv.overworld(), 20));
		log("stalls taken " + server.computeOnServer(srv -> ((BarnControllerEntity) srv.overworld().getBlockEntity(c)).stallsTaken(srv.overworld())));
	}

	// ---- reading the plant ----

	private GensetControlPanelBlockEntity panel(ServerLevel level) {
		return (GensetControlPanelBlockEntity) level.getBlockEntity(panelAt);
	}

	private DigesterEntity digester(ServerLevel level) {
		return (DigesterEntity) level.getBlockEntity(tank);
	}

	private BarnControllerEntity barn(ServerLevel level) {
		return (BarnControllerEntity) level.getBlockEntity(c);
	}

	/** What a change of marks an event: the set's state, the breaker, biogas, and what runs on the set's power. */
	private String state() {
		return server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var p = panel(level);
			DriveEntity drive = barn(level).drive(level);
			return "set " + p.run() + ", breaker " + (p.breakerClosed() ? "closed" : "open") + (p.onBiogas() ? ", on biogas" : "") + ", mixer "
					+ (digester(level).powered() ? "on" : "off") + ", scraper drive " + (drive != null && drive.powered() ? "on" : "off")
					+ ", scraper " + (barn(level).pass().running() ? "moving" : "stopped") + ", lamps lit " + lamps.stream()
					.filter(l -> level.getBlockState(l).hasProperty(dev.wildcatter.block.LoadBlock.LIT) && level.getBlockState(l).getValue(dev.wildcatter.block.LoadBlock.LIT)).count();
		});
	}

	private String numbers() {
		return server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var p = panel(level);
			var d = digester(level);
			var pit = barn(level).pit(level);
			return String.format(Locale.ROOT, "%.1f kW, %.3f m3 biogas burned, holder %.1f m3, manure in the digester %.0f kg, pit %s items", p.watts() / 1000, p.biogasBurned(),
					d.digester().gasM3(), d.digester().inTankKg(), pit == null ? "?" : Integer.toString(pit.manure()));
		});
	}

	// ---- filming ----

	private void mark(String what) {
		double at = frame >= 0 ? frame / 20.0 : (System.nanoTime() - filmStart) / 1e9;
		log(String.format(Locale.ROOT, "event %s %s at %.2f s", filmId, what, at));
	}

	private void look(MusicVideo.Cam cam, boolean snap) {
		double dx = cam.tx() - cam.x(), dy = cam.ty() - cam.y() - 1.62, dz = cam.tz() - cam.z();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		if (snap) {
			server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", cam.x(), cam.y(), cam.z(), yaw, pitch));
		}
		context.runOnClient(client -> {
			var p = client.player;
			if (p == null) {
				return;
			}
			if (snap) {
				p.snapTo(cam.x(), cam.y(), cam.z(), yaw, pitch);
			} else {
				p.setPos(cam.x(), cam.y(), cam.z());
				p.setYRot(yaw);
				p.setXRot(pitch);
			}
			p.setYHeadRot(yaw);
			p.setDeltaMovement(Vec3.ZERO);
		});
	}

	@FunctionalInterface
	interface Beat {
		void at(int t);
	}

	/**
	 * A portrait shot: the camera to its start, a 1080 x 1920 still, then {@code ticks} ticks filmed with {@code beat} run on each and the camera
	 * on its path; every change of {@link #state()} is marked. A shot not picked still runs its beats, unfilmed, so the plant stays in step.
	 */
	private void shot(int n, String name, MusicVideo.CamPath path, int ticks, Beat beat) {
		shot(n, name, GOLDEN, path, ticks, beat);
	}

	private void shot(int n, String name, int clock, MusicVideo.CamPath path, int ticks, Beat beat) {
		String id = String.format(Locale.ROOT, "%02d_%s", n, name);
		boolean film = wanted(n);
		log("shot " + id + (film ? "" : " (not filmed)"));
		server.runCommand("time set " + clock);
		look(server.computeOnServer(srv -> path.at(srv.overworld(), 0, -1)), true);
		if (film) {
			context.waitTicks(30);
			try {
				context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 300);
			} catch (RuntimeException | AssertionError ex) {
				log("sections still compiling");
			}
			look(server.computeOnServer(srv -> path.at(srv.overworld(), 0, -1)), true);
			context.waitTicks(3);
			context.takeScreenshot(TestScreenshotOptions.of(id + "_a").withSize(1080, 1920).withDestinationDir(OUT).disableCounterPrefix());
		}
		filmId = id;
		Path dir = OUT.resolve("frames").resolve(id);
		Filming rec = null;
		if (film && FRAMES) {
			try {
				Files.createDirectories(dir);
			} catch (java.io.IOException ex) {
				throw new RuntimeException(ex);
			}
		} else if (film) {
			rec = context.computeOnClient(client -> Filming.start(client, "farmshort_" + id));
			filmStart = System.nanoTime();
		}
		String last = state();
		mark("start: " + last + "; " + numbers());
		try {
			for (int t = 0; t <= ticks; t++) {
				if (film && FRAMES) {
					frame = t;
				}
				double s = (double) t / ticks;
				int tt = t;
				look(server.computeOnServer(srv -> path.at(srv.overworld(), s, tt)), false);
				beat.at(t);
				context.waitTick();
				if (film && FRAMES) {
					context.takeScreenshot(TestScreenshotOptions.of(String.format(Locale.ROOT, "%04d", t)).withSize(1080, 1920).withDestinationDir(dir).disableCounterPrefix());
				}
				String now = state();
				if (!now.equals(last)) {
					mark(now);
					last = now;
				}
				if (t % 40 == 0) {
					mark("numbers: " + numbers());
				}
			}
			mark("end: " + numbers());
		} finally {
			frame = -1;
			if (rec != null) {
				rec.stop();
			}
		}
	}

	/** Manure on the alley ahead of the blade, as the cows leave it between passes (the music video's scraper shot does the same). */
	private void manureInAlley() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int xd = c.getX() - BarnLayout.alleyLength(4);
			for (int x = xd + 3; x < c.getX() - 1; x += 2) {
				ItemEntity e = new ItemEntity(level, x + 0.5, g + 0.3, c.getZ() + 0.5, new ItemStack(FarmingsItems.MANURE, 8));
				e.setDeltaMovement(Vec3.ZERO);
				e.setPickUpDelay(32767);
				level.addFreshEntity(e);
			}
		});
	}

	/** A slow eased move between two cameras given in world coordinates (feet, and the point looked at). */
	private static MusicVideo.CamPath lin(MusicVideo.Cam a, MusicVideo.Cam b) {
		return (level, s, t) -> a.lerp(b, MusicVideo.ease(s));
	}

	private MusicVideo.Cam cam(BlockPos o, double x, double y, double z, double tx, double ty, double tz) {
		return new MusicVideo.Cam(o.getX() + x, o.getY() + y, o.getZ() + z, o.getX() + tx, o.getY() + ty, o.getZ() + tz);
	}

	private void waitPassDone() {
		for (int i = 0; i < 1500 && server.computeOnServer(srv -> barn(srv.overworld()).pass().running()); i += 20) {
			context.waitTicks(20);
		}
		log("pass done: " + state());
	}

	private void diagnose(String when) {
		log(when + ": " + server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var press = level.getBlockEntity(tank.offset(3, 0, 0));
			return digester(level).inspect().getString() + " | press " + (press instanceof dev.wildcatter.energy.Inspectable i ? i.inspect().getString() : "?")
					+ " | set " + panel(level).inspect().getString() + " | lamp " + (lamps.isEmpty() ? "" : level.getBlockState(lamps.getFirst()).toString())
					+ " | breaker " + level.getBlockState(breakerAt);
		}));
	}

	private void shots() {
		BlockPos bo = new BlockPos(c.getX(), g, c.getZ());     // the barn's pit end, on the floor
		int xd = -BarnLayout.alleyLength(4);                     // the drive end, from bo
		BlockPos t = tank;
		diagnose("before start");
		// 5. the gas set starting on the holder's biogas: low beside the set, the dome and gas line behind it; START at 2 s
		shot(5, "genset_start", lin(cam(t, 10.5, 0.4, 9.5, 3.5, 2.2, 2.5), cam(t, 9.0, 1.0, 8.0, 3.0, 2.6, 2.5)), 340, tt -> {
			if (tt == 40) {
				server.runOnServer(srv -> panel(srv.overworld()).setWantRun(true));
				mark("START pressed (remote start)");
			}
		});
		context.waitTicks(100);
		diagnose("after start");
		// 4. the digester and its gas holder, a crane up the tank to the dome, the line down to the running set
		shot(4, "digester_holder", lin(cam(t, -5.0, 0.6, -5.5, 0.5, 2.0, 0.5), cam(t, -7.0, 3.6, -7.5, 0.8, 2.8, 0.8)), 200, tt -> {
		});
		// 1. the hook: the scraper, on the set's power, pushing manure down the alley between the stalls, followed low
		manureInAlley();
		boolean started = server.computeOnServer(srv -> barn(srv.overworld()).startPass(srv.overworld()));
		log("scraper pass 1 started: " + started + "; " + state());
		shot(1, "scraper_hook", MusicVideo.dolly(new MusicVideo.G(bo, xd + 0.5, 1.4, -1.2, xd + 9, 0.4, 0.5), new MusicVideo.G(bo, xd + 6.5, 1.2, -1.2, xd + 15, 0.4, 0.5)), 160, tt -> {
		});
		waitPassDone();
		// 3. from beside the pit: the blade bringing the manure in, the pit taking it, the feed pit passing it to the digester behind
		manureInAlley();
		started = server.computeOnServer(srv -> barn(srv.overworld()).startPass(srv.overworld()));
		log("scraper pass 2 started: " + started);
		context.waitTicks(220);
		shot(3, "pit_to_digester", lin(cam(bo, -5.5, 2.6, -5.0, 2.5, 0.2, 0.5), cam(bo, -3.0, 2.0, -4.2, 2.5, 0.6, 0.5)), 240, tt -> {
		});
		// 2. the cows at their stalls, eating at the bunk, a slow dolly along the row
		shot(2, "cows_stalls", MusicVideo.dolly(new MusicVideo.G(bo, -5.5, 2.2, 7.6, -9, 1.0, 3.5), new MusicVideo.G(bo, -12.5, 2.0, 7.6, -14, 1.0, 3.5)), 180, tt -> {
		});
		waitPassDone();
		// 7. night: the barn's lights come on as the breaker on the set's lighting branch closes (at 2 s)
		shot(7, "barn_lights", 14200, lin(cam(bo, -3.0, 1.0, 12.5, -10, 2.4, 6.0), cam(bo, -4.5, 1.3, 11.5, -10.5, 2.4, 6.0)), 200, tt -> {
			if (tt == 40) {
				server.runOnServer(srv -> ((dev.wildcatter.block.BreakerBlockEntity) srv.overworld().getBlockEntity(breakerAt)).setSwitch(true));
				mark("lighting breaker closed");
			}
		});
		diagnose("after lights");
		// 6. the whole loop: a crane up from the barn's pit end over the digester to the running set, the lights on, the scraper running
		manureInAlley();
		server.runOnServer(srv -> barn(srv.overworld()).startPass(srv.overworld()));
		shot(6, "barn_to_grid", 12500, MusicVideo.dolly(new MusicVideo.G(bo, 14, 3, 16, -6, 1, 0), new MusicVideo.G(bo, 18, 13, 20, -7, 0, -1)), 220, tt -> {
		});
		log("done: " + state() + "; " + numbers());
	}
}
