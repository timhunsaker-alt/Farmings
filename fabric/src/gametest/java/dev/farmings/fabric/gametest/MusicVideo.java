package dev.farmings.fabric.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.barn.BarnControllerEntity;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.DriveEntity;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import dev.farmings.entity.CattleEntity;
import dev.farmings.entity.GoatEntity;
import dev.farmings.entity.HorseEntity;
import dev.farmings.entity.SheepDogEntity;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.husbandry.GoatBreed;
import dev.farmings.husbandry.HorseBreed;
import dev.farmings.husbandry.SheepBreed;
import dev.wildcatter.multiblock.BigParts;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Footage for three songs about the farm (FARMINGS_ONLY=musicVideo, FARMINGS_MV_SONG=cattle | sheep_goats | horses): each song's sets
 * staged on a real generated world (fixed seed, its plains) and filmed shot by shot, about 10 s each, with a start still (1920 x 1080,
 * no HUD). The camera is the spectator player, moved on the client every tick along a dolly, orbit, crane or a smoothed track of moving
 * animals. Animals move by their own AI (lures, sheds, stables, the sheep dog), by navigation to points, or, galloping, by the start
 * gate's own way of moving a horse (a step a tick). Every block and animal is the mod's or vanilla's, built as the scenes in
 * {@link FarmingsClientGameTest} build them. FARMINGS_MV_SHOTS=3,7 films only those shots; stills go to FARMINGS_MV_OUT/&lt;song&gt;, films to
 * {@code fabric/build/scene-videos/mv_<song>_NN_<shot>.mov}. Built on {@link Trailer}'s world and ground helpers.
 */
final class MusicVideo {
	private static final String SEED = System.getenv().getOrDefault("FARMINGS_MV_SEED", "farmings");
	private static final String SONG = System.getenv().getOrDefault("FARMINGS_MV_SONG", "cattle");
	private static final Path OUT = Path.of(System.getenv().getOrDefault("FARMINGS_MV_OUT", "mv")).resolve(SONG).toAbsolutePath();
	private static final int W = 1920, H = 1080;
	/** Ticks of a usual shot: 10 s. */
	private static final int LEN = 200;

	private final ClientGameTestContext context;
	private TestServerContext server;
	private BlockPos plains;
	private int shotNo;
	private final List<String> failed = new ArrayList<>();
	private final List<int[]> used = new ArrayList<>();

	MusicVideo(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[farmings-mv] " + line);
	}

	private static boolean wanted(int shot) {
		String only = System.getenv("FARMINGS_MV_SHOTS");
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
		// 1280 x 720 points: 2560 x 1440 on the laptop's screen, which fits it (the first trailer's size)
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
			plains = server.computeOnServer(srv -> {
				var r = srv.overworld().findClosestBiome3d(h -> h.is(ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace("plains"))),
						new BlockPos(0, 64, 0), 3200, 32, 64);
				return r == null ? new BlockPos(0, 64, 0) : r.getFirst();
			});
			log("plains at " + plains.toShortString() + ", song " + SONG);
			server.runCommand("tp @p " + plains.getX() + " 160 " + plains.getZ());
			context.waitTicks(100);
			switch (SONG) {
				case "sheep_goats" -> sheepGoats();
				case "horses" -> horses();
				default -> cattle();
			}
			log("done; " + shotNo + " shots; failed " + failed);
		}
	}

	// ---- the camera ----

	/** Where the camera stands (feet) and the point it looks at. */
	record Cam(double x, double y, double z, double tx, double ty, double tz) {
		Cam lerp(Cam o, double t) {
			return new Cam(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t, tx + (o.tx - tx) * t, ty + (o.ty - ty) * t, tz + (o.tz - tz) * t);
		}
	}

	/** A camera spot given over the ground: x, z and the target's x, z from an origin, each height above the ground under it. */
	record G(BlockPos o, double x, double h, double z, double tx, double th, double tz) {
		Cam at(ServerLevel level) {
			int ax = o.getX() + (int) Math.floor(x), az = o.getZ() + (int) Math.floor(z);
			int bx = o.getX() + (int) Math.floor(tx), bz = o.getZ() + (int) Math.floor(tz);
			return new Cam(o.getX() + x, surface(level, ax, az) + h, o.getZ() + z, o.getX() + tx, surface(level, bx, bz) + th, o.getZ() + tz);
		}
	}

	@FunctionalInterface
	interface CamPath {
		/** The camera at s (0..1) of the shot, tick t (-1 before it starts). Runs on the server thread. */
		Cam at(ServerLevel level, double s, int t);
	}

	@FunctionalInterface
	interface Act {
		void at(ServerLevel level, int t);
	}

	private static final Act NONE = (l, t) -> {
	};

	static double ease(double s) {
		return s * s * (3 - 2 * s);
	}

	/** A slow eased move from a to b, both over the ground. */
	static CamPath dolly(G a, G b) {
		Cam[] ends = new Cam[2];
		return (level, s, t) -> {
			if (ends[0] == null) {
				ends[0] = a.at(level);
				ends[1] = b.at(level);
			}
			return ends[0].lerp(ends[1], ease(s));
		};
	}

	/** Round a point (over the ground at c), radius r0 to r1, height h0 to h1 over that ground, angle a0 to a1 degrees (0 = east, 90 = south). */
	static CamPath orbit(BlockPos o, double cx, double cz, double r0, double r1, double h0, double h1, double a0, double a1, double th) {
		double[] g = {Double.NaN};
		return (level, s, t) -> {
			if (Double.isNaN(g[0])) {
				g[0] = surface(level, o.getX() + (int) Math.floor(cx), o.getZ() + (int) Math.floor(cz));
			}
			double e = 0.15 * ease(s) + 0.85 * s;
			double r = r0 + (r1 - r0) * e, h = h0 + (h1 - h0) * e, a = Math.toRadians(a0 + (a1 - a0) * e);
			double x = o.getX() + cx, z = o.getZ() + cz;
			return new Cam(x + r * Math.cos(a), g[0] + h, z + r * Math.sin(a), x, g[0] + th, z);
		};
	}

	/**
	 * Follows a moving subject: the camera at the subject plus an offset (eased from off0 to off1), looking at the subject plus a look
	 * offset; both smoothed (k a tick) so the animals' jitter does not shake the frame.
	 */
	private static CamPath track(Supplier<Vec3> subject, Vec3 off0, Vec3 off1, Vec3 look, double k) {
		Cam[] cur = new Cam[1];
		return (level, s, t) -> {
			Vec3 p = subject.get();
			Vec3 off = off0.lerp(off1, ease(s));
			Cam want = new Cam(p.x + off.x, p.y + off.y, p.z + off.z, p.x + look.x, p.y + look.y, p.z + look.z);
			cur[0] = cur[0] == null || t <= 0 ? want : cur[0].lerp(want, k);
			return cur[0];
		};
	}

	/** A camera moving a to b (over the ground) and turning to keep a moving subject in frame. */
	private static CamPath pan(G a, G b, Supplier<Vec3> subject, Vec3 look, double k) {
		Cam[] ends = new Cam[2];
		Vec3[] aim = new Vec3[1];
		return (level, s, t) -> {
			if (ends[0] == null) {
				ends[0] = a.at(level);
				ends[1] = b.at(level);
			}
			Cam c = ends[0].lerp(ends[1], ease(s));
			Vec3 want = subject.get().add(look);
			aim[0] = aim[0] == null || t <= 0 ? want : aim[0].lerp(want, k);
			return new Cam(c.x, c.y, c.z, aim[0].x, aim[0].y, aim[0].z);
		};
	}

	private static Supplier<Vec3> at(Entity e) {
		return e::position;
	}

	/** The middle of the living ones of a group. */
	private static Supplier<Vec3> middle(List<? extends Entity> group) {
		return () -> {
			double x = 0, y = 0, z = 0;
			int n = 0;
			for (Entity e : group) {
				if (e.isAlive()) {
					x += e.getX();
					y += e.getY();
					z += e.getZ();
					n++;
				}
			}
			return n == 0 ? Vec3.ZERO : new Vec3(x / n, y / n, z / n);
		};
	}

	/** Moves the server's player there (so the chunks load and the animals see it), then the camera. */
	private void put(Cam c) {
		double dx = c.tx - c.x, dy = c.ty - c.y - 1.62, dz = c.tz - c.z;
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", c.x, c.y, c.z, yaw, pitch));
		look(c);
	}

	/** Places the camera on the client: the local spectator player, its old position the last tick's, so frames ease between them. */
	private void look(Cam c) {
		double dx = c.tx - c.x, dy = c.ty - c.y - 1.62, dz = c.tz - c.z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		context.runOnClient(client -> {
			var p = client.player;
			if (p == null) {
				return;
			}
			p.setPos(c.x, c.y, c.z);
			p.setYRot(yaw);
			p.setYHeadRot(yaw);
			p.setXRot(pitch);
			p.setDeltaMovement(Vec3.ZERO);
		});
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

	private void time(int t) {
		server.runCommand("time set " + t);
	}

	private String next(String id) {
		shotNo++;
		return String.format(Locale.ROOT, "%02d_%s", shotNo, id);
	}

	/**
	 * One shot: {@code prep} on the server, {@code lead} ticks for it to take, the camera to its start, a still, then the film of
	 * {@code ticks} ticks with {@code act} on the server every tick and the camera on its path.
	 */
	private void film(String id, int clock, int ticks, Act prep, int lead, CamPath path, Act act) {
		String name = next(id);
		if (!wanted(shotNo)) {
			return;
		}
		log("shot " + name);
		try {
			time(clock);
			if (prep != null) {
				server.runOnServer(srv -> prep.at(srv.overworld(), -1));
			}
			Cam c0 = server.computeOnServer(srv -> path.at(srv.overworld(), 0, -1));
			put(c0);
			settle(Math.max(30, lead));
			look(server.computeOnServer(srv -> path.at(srv.overworld(), 0, -1)));
			context.waitTicks(2);
			still(name + "_a");
			Filming film = context.computeOnClient(client -> Filming.start(client, "mv_" + SONG + "_" + name));
			try {
				for (int t = 0; t <= ticks; t++) {
					int tt = t;
					double s = (double) t / ticks;
					Cam c = server.computeOnServer(srv -> {
						act.at(srv.overworld(), tt);
						return path.at(srv.overworld(), s, tt);
					});
					look(c);
					context.waitTick();
				}
			} finally {
				film.stop();
			}
		} catch (RuntimeException | AssertionError ex) {
			log("shot " + name + " failed: " + ex);
			ex.printStackTrace();
			failed.add(name);
		}
	}

	/** A book open over the world (the herd or stud book), its pages turned every 3 s. */
	private void filmBook(String id, int clock, Function<ServerLevel, ItemStack> book, G behind) {
		String name = next(id);
		if (!wanted(shotNo)) {
			return;
		}
		log("shot " + name);
		try {
			time(clock);
			ItemStack b = server.computeOnServer(srv -> book.apply(srv.overworld()));
			Cam c = server.computeOnServer(srv -> behind.at(srv.overworld()));
			put(c);
			settle(30);
			look(c);
			context.runOnClient(client -> {
				client.options.guiScale().set(4);
				client.resizeGui();
			});
			context.runOnClient(client -> client.setScreenAndShow(new net.minecraft.client.gui.screens.inventory.BookViewScreen(
					net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess.fromItem(b))));
			context.waitTicks(10);
			still(name + "_a");
			Filming film = context.computeOnClient(client -> Filming.start(client, "mv_" + SONG + "_" + name));
			for (int t = 0; t < 180; t++) {
				if (t % 60 == 59) {
					context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN);
				}
				context.waitTick();
			}
			film.stop();
		} catch (RuntimeException | AssertionError ex) {
			log("shot " + name + " failed: " + ex);
			ex.printStackTrace();
			failed.add(name);
		} finally {
			context.runOnClient(client -> {
				client.setScreenAndShow(null);
				client.options.guiScale().set(0);
				client.resizeGui();
			});
		}
	}

	// ---- moving animals ----

	/** Sends a mob walking (its own AI and pathfinding) to x, z at a speed modifier. */
	private static void walk(Mob m, double x, double z, double speed) {
		m.getNavigation().moveTo(x, m.getY(), z, speed);
	}

	/** One tick of a gallop toward (x, z) at v m/s, moved the way the start gate's jockey moves a horse; its AI held off meanwhile. */
	private static void gallop(Mob m, double x, double z, double v) {
		m.setNoAi(true);
		double dx = x - m.getX(), dz = z - m.getZ(), len = Math.hypot(dx, dz);
		if (len < 0.05) {
			return;
		}
		double step = Math.min(v / 20, len);
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		m.setYRot(yaw);
		m.setYHeadRot(yaw);
		m.setYBodyRot(yaw);
		m.move(MoverType.SELF, new Vec3(dx / len * step, m.onGround() ? -0.01 : -0.3, dz / len * step));
	}

	static void place(Entity e, double x, double y, double z, float yaw) {
		e.snapTo(x, y, z, yaw, 0);
		if (e instanceof Mob m) {
			m.getNavigation().stop();
			m.setYHeadRot(yaw);
			m.setYBodyRot(yaw);
		}
		e.setDeltaMovement(Vec3.ZERO);
	}

	// ---- the ground (as the trailer) ----

	static boolean loose(BlockState s) {
		return s.isAir() || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || (s.canBeReplaced() && s.getFluidState().isEmpty()) || s.is(Blocks.SNOW)
				|| s.is(Blocks.BAMBOO) || s.is(Blocks.CACTUS) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.PUMPKIN) || s.is(Blocks.MELON) || s.is(BlockTags.FLOWERS);
	}

	/** The first free block over the natural ground at a column: trees and plants not counted. */
	static int surface(ServerLevel level, int x, int z) {
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y - 1, z);
		while (p.getY() > level.getMinY() && loose(level.getBlockState(p))) {
			p.move(0, -1, 0);
		}
		return p.getY() + 1;
	}

	static boolean wet(ServerLevel level, int x, int z) {
		return !level.getBlockState(new BlockPos(x, surface(level, x, z) - 1, z)).getFluidState().isEmpty();
	}

	/** A dry, fairly even site near (x, z) for a w by d pad, searched outward in rings: its corner at the ground's middle height. */
	private BlockPos site(ServerLevel level, int x, int z, int w, int d) {
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		for (int ring = 0; ring <= 6 && (best == null || bestScore > 3); ring++) {
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
		log("site near " + x + "," + z + " (" + w + " x " + d + "): " + best.toShortString() + ", unevenness " + bestScore);
		used.add(new int[] {best.getX(), best.getZ(), best.getX() + w, best.getZ() + d});
		return best;
	}

	private boolean taken(int x, int z, int w, int d) {
		int m = 72;
		for (int[] u : used) {
			if (x < u[2] + m && x + w > u[0] - m && z < u[3] + m && z + d > u[1] - m) {
				return true;
			}
		}
		return false;
	}

	static void forceChunks(ServerLevel level, int x0, int z0, int x1, int z1) {
		for (int cx = Math.floorDiv(x0, 16); cx <= Math.floorDiv(x1, 16); cx++) {
			for (int cz = Math.floorDiv(z0, 16); cz <= Math.floorDiv(z1, 16); cz++) {
				level.setChunkForced(cx, cz, true);
			}
		}
	}

	/** Levels the rectangle to ground at g (the first air), cleared {@code clear} above; a ring {@code blend} wide eases back to the land. */
	static void pad(ServerLevel level, int x0, int z0, int x1, int z1, int g, int blend, int clear) {
		forceChunks(level, x0 - blend - 16, z0 - blend - 16, x1 + blend + 16, z1 + blend + 16);
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
	static void column(ServerLevel level, int x, int z, int h, int from, int to) {
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

	/** A round grassy hill: up to h blocks over the ground at its middle, easing to nothing at radius r. */
	private static void hill(ServerLevel level, int cx, int cz, int r, int h) {
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				double d = Math.hypot(x - cx, z - cz) / r;
				if (d >= 1) {
					continue;
				}
				double t = 1 - d;
				int add = (int) Math.round(h * t * t * (3 - 2 * t));
				if (add <= 0) {
					continue;
				}
				int base = surface(level, x, z);
				column(level, x, z, base + add, base - 2, base + add + 6);
			}
		}
	}

	/** A stepped outcrop of stone for goats: a rough cone of rock up to h blocks, its steps a block each. */
	private static void rocks(ServerLevel level, int cx, int cz, int r, int h, long seed) {
		Random rnd = new Random(seed);
		BlockState[] stone = {Blocks.STONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(),
				Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.STONE.defaultBlockState()};
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				double d = Math.hypot(x - cx, z - cz) / r + rnd.nextDouble() * 0.15;
				if (d >= 1) {
					continue;
				}
				int top = (int) Math.ceil(h * (1 - d));
				int base = surface(level, x, z);
				for (int k = 0; k < top; k++) {
					level.setBlock(new BlockPos(x, base + k, z), stone[rnd.nextInt(stone.length)], Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Grass and flowers on the open grass of the rectangle. */
	static void meadow(ServerLevel level, int x0, int z0, int x1, int z1, long seed, double grass, double flowers) {
		Random rnd = new Random(seed);
		BlockState[] fl = {Blocks.DANDELION.defaultBlockState(), Blocks.POPPY.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
				Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.DANDELION.defaultBlockState()};
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				int y = surface(level, x, z);
				BlockPos p = new BlockPos(x, y, z);
				if (!level.getBlockState(p.below()).is(Blocks.GRASS_BLOCK) || !level.getBlockState(p).isAir()) {
					continue;
				}
				double r = rnd.nextDouble();
				if (r < flowers) {
					level.setBlock(p, fl[rnd.nextInt(fl.length)], Block.UPDATE_CLIENTS);
				} else if (r < flowers + grass) {
					level.setBlock(p, Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Trees (vanilla features) on the grass at those spots; a spot that will not take one is skipped. */
	private void trees(List<int[]> spots, String... kinds) {
		int i = 0;
		for (int[] s : spots) {
			int y = server.computeOnServer(srv -> surface(srv.overworld(), s[0], s[1]));
			String kind = kinds[i++ % kinds.length];
			try {
				server.runCommand("place feature minecraft:" + kind + " " + s[0] + " " + y + " " + s[1]);
			} catch (RuntimeException | AssertionError ex) {
				log("no tree at " + s[0] + "," + s[1] + ": " + ex.getMessage());
			}
		}
	}

	/** Spots round the edge of a rectangle, every {@code step} blocks, jittered, {@code out} blocks outside it. */
	static List<int[]> ring(int x0, int z0, int x1, int z1, int out, int step, long seed) {
		Random r = new Random(seed);
		List<int[]> l = new ArrayList<>();
		for (int x = x0 - out; x <= x1 + out; x += step) {
			l.add(new int[] {x + r.nextInt(5) - 2, z0 - out - r.nextInt(6)});
			l.add(new int[] {x + r.nextInt(5) - 2, z1 + out + r.nextInt(6)});
		}
		for (int z = z0 - out + step; z <= z1 + out - step; z += step) {
			l.add(new int[] {x0 - out - r.nextInt(6), z + r.nextInt(5) - 2});
			l.add(new int[] {x1 + out + r.nextInt(6), z + r.nextInt(5) - 2});
		}
		return l;
	}

	static void fence(ServerLevel level, BlockPos o, int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (x == x0 || x == x1 || z == z0 || z == z1) {
					level.setBlockAndUpdate(o.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState());
				}
			}
		}
	}

	static void discardItems(ServerLevel level, BlockPos at, int r) {
		level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(r, 40, r)).forEach(Entity::discard);
	}

	// ---- the corral (taming.md), as the taming scene builds it ----

	/** Fence ring w x d at k, its one-way gate mid-south (animals come in from the south), the post, a bait trough and a water trough. */
	private static void corral(ServerLevel level, BlockPos k, int w, int d) {
		fence(level, k, 0, 0, w, d);
		level.setBlockAndUpdate(k.offset(w / 2, 0, d), dev.farmings.corral.CorralBlocks.CORRAL_GATE.defaultBlockState()
				.setValue(dev.farmings.corral.CorralGateBlock.FACING, Direction.NORTH));
		level.setBlockAndUpdate(k.offset(1, 0, 1), dev.farmings.corral.CorralBlocks.CORRAL_POST.defaultBlockState());
		level.setBlockAndUpdate(k.offset(w / 2, 0, 3), dev.farmings.corral.CorralBlocks.BAIT_TROUGH.defaultBlockState());
		FarmingsClientGameTest.barnPart(level, BarnParts.WATER_TROUGH, k.offset(w / 2 + 3, 0, 5));
		if (level.getBlockEntity(BigParts.owner(level, k.offset(w / 2 + 3, 0, 5))) instanceof TroughEntity t) {
			t.pourBucket();
		}
	}

	/** Hay (or meat) in the corral's bait trough: from now it draws the wild animals round it in. */
	private static void bait(ServerLevel level, BlockPos k, int w, boolean meat) {
		if (level.getBlockEntity(k.offset(w / 2, 0, 3)) instanceof dev.farmings.corral.BaitTroughEntity b) {
			if (meat) {
				b.addMeat();
				b.addMeat();
			}
			while (b.dryMatterKg() < dev.farmings.corral.BaitTroughEntity.CAPACITY_KG * 0.6 && b.addBlock()) {
				// fill to over half
			}
		}
		if (level.getBlockEntity(k.offset(1, 0, 1)) instanceof dev.farmings.corral.CorralPostEntity post && post.owner() == null) {
			post.claim(level.getServer().getPlayerList().getPlayers().get(0).getUUID());
		}
	}

	// ---- animals ----

	private static CattleEntity cow(ServerLevel level, CattleBreed breed, Sex sex, long seed, double ageDays, double kg, double x, double z, float yaw, boolean wild) {
		CattleEntity c = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
		c.initFounder(breed, sex, seed, ageDays, kg, level.getGameTime());
		c.setVisualGenome(c.visualGenome());
		c.setWild(wild);
		c.snapTo(x, surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, yaw, 0);
		level.addFreshEntity(c);
		return c;
	}

	private static SheepEntity sheep(ServerLevel level, SheepBreed breed, Sex sex, long seed, double ageDays, double x, double z, boolean wild) {
		SheepEntity s = FarmingsClientGameTest.sheep(level, breed, sex, seed, ageDays);
		s.setNoAi(false);
		s.setWild(wild);
		s.setFleeceDaysForTest(ageDays < 200 ? 30 : 250);
		place(s, x, surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, (float) (seed * 47 % 360));
		return s;
	}

	private static GoatEntity goat(ServerLevel level, GoatBreed breed, Sex sex, long seed, double ageDays, double x, double z, boolean wild) {
		GoatEntity g = FarmingsClientGameTest.goat(level, breed, sex, seed, ageDays);
		g.setNoAi(false);
		g.setWild(wild);
		place(g, x, surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, (float) (seed * 53 % 360));
		return g;
	}

	private static HorseEntity horse(ServerLevel level, HorseBreed breed, Sex sex, long seed, double ageDays, int coat, double x, double z, boolean wild) {
		HorseEntity h = FarmingsClientGameTest.horse(level, breed, sex, seed, ageDays);
		int[] c = COATS[coat % COATS.length];
		h.setGenesForTest(FarmingsClientGameTest.coat(h.genes(), c[0], c[1], c[2], c[3], c[4], c[5], c[6]));
		h.setWild(wild);
		h.setNoAi(false);
		place(h, x, surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, (float) (seed * 61 % 360));
		return h;
	}

	/** Coat genes (E, A, Cr, D, G, Rn, SB1 copies): bay, chestnut, black, palomino, buckskin, dun, roan, sabino, grey. */
	private static final int[][] COATS = {{2, 2, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0}, {2, 0, 0, 0, 0, 0, 0}, {0, 0, 1, 0, 0, 0, 0}, {2, 2, 1, 0, 0, 0, 0},
			{2, 2, 0, 1, 0, 0, 0}, {2, 2, 0, 0, 0, 1, 0}, {0, 0, 0, 0, 0, 0, 1}, {2, 2, 0, 0, 1, 0, 0}};

	/** Each mob of the group to a new point near its own every {@code every} ticks, within the box. */
	private static void roam(List<? extends Mob> group, int t, int every, Random r, double x0, double z0, double x1, double z1, double speed) {
		for (int i = 0; i < group.size(); i++) {
			Mob m = group.get(i);
			if ((t + i * 7) % every == 0) {
				double x = Math.max(x0, Math.min(x1, m.getX() + r.nextGaussian() * 6));
				double z = Math.max(z0, Math.min(z1, m.getZ() + r.nextGaussian() * 6));
				walk(m, x, z, speed);
			}
		}
	}

	// =====================================================================================================================
	// 1. cattle
	// =====================================================================================================================

	private void cattle() {
		// ---- set A: the plain and the taming corral ----
		BlockPos[] ks = new BlockPos[1];
		List<CattleEntity> herd = new ArrayList<>();
		double[][] herdSpots = {{-5, 14}, {-2, 16}, {1, 15}, {4, 17}, {-3, 19}, {2, 20}, {6, 14}, {-7, 18}, {0, 22}};
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, plains.getX(), plains.getZ(), 60, 52);
			int g = s.getY();
			pad(level, s.getX(), s.getZ(), s.getX() + 60, s.getZ() + 52, g, 14, 16);
			BlockPos k = new BlockPos(s.getX() + 23, g, s.getZ() + 6);
			meadow(level, s.getX() - 40, s.getZ() - 30, s.getX() + 68, s.getZ() + 70, 11, 0.3, 0.015);
			corral(level, k, 14, 12);
			Object[][] breeds = {{CattleBreed.HOLSTEIN, Sex.FEMALE, 650.0}, {CattleBreed.JERSEY, Sex.FEMALE, 430.0}, {CattleBreed.BROWN_SWISS, Sex.MALE, 900.0},
					{CattleBreed.GYR, Sex.FEMALE, 420.0}, {CattleBreed.HOLSTEIN, Sex.FEMALE, 600.0}, {CattleBreed.JERSEY, Sex.FEMALE, 120.0},
					{CattleBreed.BROWN_SWISS, Sex.FEMALE, 600.0}, {CattleBreed.GYR, Sex.FEMALE, 140.0}, {CattleBreed.HOLSTEIN, Sex.FEMALE, 130.0}};
			BlockPos gate = k.offset(7, 0, 12);
			for (int i = 0; i < breeds.length; i++) {
				double kg = (double) breeds[i][2];
				herd.add(cow(level, (CattleBreed) breeds[i][0], (Sex) breeds[i][1], 5100 + i, kg < 200 ? 90 : 4 * 365, kg,
						gate.getX() + 0.5 + herdSpots[i][0], gate.getZ() + 0.5 + herdSpots[i][1], i * 40, true));
			}
			discardItems(level, k, 80);
			ks[0] = k;
		});
		BlockPos k = ks[0];
		BlockPos gate = k.offset(7, 0, 12);
		trees(ring(k.getX() - 14, k.getZ() - 14, k.getX() + 28, k.getZ() + 40, 10, 13, 5), "oak", "birch", "fancy_oak");
		Act resetHerd = (level, t) -> {
			for (int i = 0; i < herd.size(); i++) {
				double x = gate.getX() + 0.5 + herdSpots[i][0], z = gate.getZ() + 0.5 + herdSpots[i][1];
				place(herd.get(i), x, surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, i * 40);
				herd.get(i).setLure(null);
			}
		};
		Act herdInside = (level, t) -> {
			for (int i = 0; i < herd.size(); i++) {
				double x = k.getX() + 2.5 + (i * 3) % 10, z = k.getZ() + 2.5 + (i % 4) * 2.2;
				place(herd.get(i), x, k.getY(), z, i * 50);
			}
		};
		Vec3 up = new Vec3(0, 1, 0);
		// 1. the wild herd on the plain at sunrise, the sun behind them
		film("plain_dawn", 23700, LEN, resetHerd, 40, dolly(new G(gate, -28, 8, 34, 6, 1, 17), new G(gate, -21, 6, 30, 6, 1, 17)), NONE);
		// 2. low along the grazing herd
		film("herd_grazing", 600, LEN, resetHerd, 40, dolly(new G(gate, -12, 0.7, 28, -3, 1, 17), new G(gate, 3, 0.7, 29, 3, 1, 17)), NONE);
		// 3. hay in the corral's bait trough: the herd heads for it, followed from beside
		film("herd_lured", 1500, LEN, (level, t) -> {
			resetHerd.at(level, t);
			bait(level, k, 14, false);
		}, 30, track(middle(herd), new Vec3(9, 2.6, 4), new Vec3(7, 2.0, -1), up, 0.08), NONE);
		// 4. at the one-way gate, low, as they come through
		film("herd_gate", 2000, LEN, (level, t) -> {
			for (int i = 0; i < herd.size(); i++) {
				double x = gate.getX() + 0.5 + herdSpots[i][0] * 0.6, z = gate.getZ() + 5.5 + (herdSpots[i][1] - 14) * 0.8;
				place(herd.get(i), x, surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, 180);
			}
			bait(level, k, 14, false);
		}, 20, dolly(new G(gate, 3.5, 0.8, -3.5, -0.5, 1.0, 6), new G(gate, 2.5, 0.9, -5, -0.5, 1.0, 6)), NONE);
		// 5. at the bait trough, close
		BlockPos trough = k.offset(7, 0, 3);
		film("hay_trough", 2600, LEN, (level, t) -> {
			herdInside.at(level, t);
			bait(level, k, 14, false);
		}, 60, dolly(new G(trough, 5.5, 1.3, 5.5, 0.5, 0.7, 0.5), new G(trough, 4.2, 1.1, 4.4, 0.5, 0.7, 0.5)), NONE);
		// 6. the water trough
		BlockPos water = k.offset(10, 0, 5);
		film("water_trough", 3000, LEN, null, 20, dolly(new G(water, -3, 1.1, 4.5, 1, 0.5, 0.5), new G(water, -1.8, 0.9, 3.4, 1, 0.5, 0.5)), NONE);
		// 7. the corral settling at golden hour, an orbit
		film("corral_golden", 12300, LEN, null, 20, orbit(k, 7, 6, 13, 11, 4, 2.8, 200, 255, 1), NONE);

		// ---- set B: the dairy: freestall barn, cow lane, holding pen, parlor and bulk tank ----
		BlockPos[] cs = new BlockPos[1];
		List<CattleEntity> dry = new ArrayList<>();
		List<CattleEntity> milkers = new ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, plains.getX() + 160, plains.getZ(), 40, 36);
			int g = s.getY();
			pad(level, s.getX(), s.getZ(), s.getX() + 40, s.getZ() + 36, g, 14, 20);
			meadow(level, s.getX() - 30, s.getZ() - 30, s.getX() + 70, s.getZ() + 66, 21, 0.3, 0.01);
			int cx = s.getX() + 30, cz = s.getZ() + 24;
			BlockPos c = new BlockPos(cx, g + 1, cz);
			BlockPos p = new BlockPos(cx, g + 1, cz - 13);
			for (int x = cx - 24; x <= cx + 4; x++) {
				for (int z = cz - 18; z <= cz + 6; z++) {
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
			int fl = g;
			level.setBlock(p, dev.farmings.FarmingsBlocks.PARLOR_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
			FarmingsClientGameTest.parlorPart(level, dev.farmings.parlor.ParlorParts.BULK_TANK, new BlockPos(p.getX() + 1, fl, p.getZ()));
			int xp = p.getX() - 5;
			FarmingsClientGameTest.parlorPart(level, dev.farmings.parlor.ParlorParts.VACUUM_PUMP, new BlockPos(xp, fl, p.getZ()));
			for (int x = xp + 1; x <= p.getX(); x++) {
				FarmingsClientGameTest.parlorPart(level, dev.farmings.parlor.ParlorParts.MILK_LINE, new BlockPos(x, fl, p.getZ()));
			}
			for (int x = xp + 1; x < p.getX(); x++) {
				FarmingsClientGameTest.parlorPart(level, dev.farmings.parlor.ParlorParts.MILKING_UNIT, new BlockPos(x, p.getY(), p.getZ()));
				FarmingsClientGameTest.parlorPart(level, dev.farmings.parlor.ParlorParts.PARLOR_STALL, new BlockPos(x, fl, p.getZ() + 1));
				FarmingsClientGameTest.parlorPart(level, dev.farmings.parlor.ParlorParts.HOLDING_PEN, new BlockPos(x, fl, p.getZ() + 3));
				FarmingsClientGameTest.parlorPart(level, dev.farmings.parlor.ParlorParts.HOLDING_PEN, new BlockPos(x, fl, p.getZ() + 4));
			}
			for (int z = p.getZ() + 5; z <= cz - 2; z++) {
				FarmingsClientGameTest.parlorPart(level, dev.farmings.parlor.ParlorParts.COW_LANE, new BlockPos(cx - 3, fl, z));
			}
			for (int dir = -1; dir <= 1; dir += 2) {
				if (BigParts.entity(level, new BlockPos(cx + 1, g, cz + dir * 2)) instanceof TroughEntity t) {
					t.pourBucket();
				}
				int row = cx - dev.farmings.husbandry.barn.BarnLayout.alleyLength(4) + 4;
				for (int i = 0; i < 4; i++) {
					if (BigParts.entity(level, new BlockPos(row + 1 + 2 * i, g + 1, cz + dir * 5)) instanceof FeederEntity f) {
						f.addBlock();
						f.addBlock();
					}
				}
			}
			var barn = (BarnControllerEntity) level.getBlockEntity(c);
			barn.refresh(level);
			var parlor = (dev.farmings.parlor.ParlorControllerEntity) level.getBlockEntity(p);
			parlor.refresh(level);
			parlor.pump(level).forceRunning(true);
			parlor.tank(level).forceChilling(true);
			log("barn " + barn.formed() + ", parlor " + parlor.formed() + " " + parlor.fault());
			CattleBreed[] breeds = {CattleBreed.JERSEY, CattleBreed.BROWN_SWISS, CattleBreed.GYR, CattleBreed.JERSEY};
			for (int i = 0; i < breeds.length; i++) {
				CattleEntity cow = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
				cow.setBreedForDisplay(breeds[i], Sex.FEMALE, breeds[i].matureWeightKg, true);
				cow.snapTo(cx - 2.5 - 2 * i, g + 0.2, cz + 0.5, 90, 0);
				level.addFreshEntity(cow);
				dry.add(cow);
			}
			discardItems(level, c, 60);
			cs[0] = c;
		});
		BlockPos c = cs[0];
		int g = c.getY() - 1;
		int xd = c.getX() - dev.farmings.husbandry.barn.BarnLayout.alleyLength(4);
		BlockPos bo = new BlockPos(c.getX(), g, c.getZ());     // the barn's pit end, on the floor
		trees(ring(c.getX() - 30, c.getZ() - 24, c.getX() + 10, c.getZ() + 12, 9, 12, 6), "oak", "fancy_oak", "birch");
		Act dryInAlley = (level, t) -> {
			for (int i = 0; i < dry.size(); i++) {
				place(dry.get(i), c.getX() - 2.5 - 2 * i, g + 0.2, c.getZ() + (i % 2 == 0 ? 0.5 : -0.5), 90);
			}
		};
		// 8. the barn in the morning: a crane down from over it
		film("barn_morning", 1200, LEN, null, 60, dolly(new G(bo, 9, 9, 11, -5, 0, 0), new G(bo, 5, 3.5, 7, -5, 1, 0)), NONE);
		// 9. cows walking up the alley to take their stalls
		film("cows_take_stalls", 1500, LEN, dryInAlley, 10, dolly(new G(bo, 5, 6, 0.5, -6, 0, 0), new G(bo, 2.5, 4.5, 2, -7, 0, 0)), NONE);
		// 10. along the stalls at the feed bunk, low
		film("stalls_close", 3000, LEN, null, 20, dolly(new G(bo, 0.5, 1.2, 8, -3, 1.0, 3), new G(bo, -7, 1.2, 8, -9, 1.0, 3)), NONE);
		// 11. the herd book, open over the barn
		filmBook("herd_book", 3000, level -> {
			ItemStack b = new ItemStack(FarmingsItems.HERD_BOOK);
			for (CattleEntity cow : dry) {
				dev.farmings.item.HerdBookItem.record(b, cow);
			}
			for (CattleEntity cow : herd) {
				dev.farmings.item.HerdBookItem.record(b, cow);
			}
			return b;
		}, new G(bo, 6, 3, 6, -6, 0, 0));
		// 12. the milking cows called along the lane to the holding pen
		film("cow_lane", 2000, 240, (level, t) -> {
			if (milkers.isEmpty()) {
				for (int i = 0; i < 4; i++) {
					CattleEntity cow = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
					cow.initFounder(CattleBreed.HOLSTEIN, Sex.FEMALE, 90L + i, 900, 650, level.getGameTime());
					cow.setCalvedForDisplay(30);
					cow.setPos(c.getX() - 12 + i * 2 + 0.5, g + 0.2, c.getZ() + 0.5);
					cow.settle(level);
					level.addFreshEntity(cow);
					milkers.add(cow);
				}
			}
			((BarnControllerEntity) level.getBlockEntity(c)).tend(level, 20);
		}, 60, dolly(new G(bo, 8, 16, 6, -3, 0, -7), new G(bo, 5, 11, 0, -3, 0, -9)), NONE);
		// 13. the parlor: milking units on the cows, the vacuum pump running
		film("parlor", 2000, LEN, null, 160, dolly(new G(bo, 4, 1.8, -9, -3, 1.2, -12.5), new G(bo, 0, 1.6, -8.5, -3.5, 1.2, -12.5)), NONE);
		// 14. the chilled bulk tank, close
		film("bulk_tank", 2200, LEN, null, 10, orbit(bo, 1.5, -12.5, 5, 4, 1.6, 2.6, 10, 75, 1.2), NONE);

		// ---- set C: the pasture: bull and heifer, calves in spring ----
		BlockPos[] ps = new BlockPos[1];
		CattleEntity[] pair = new CattleEntity[3];
		List<CattleEntity> calves = new ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, plains.getX(), plains.getZ() + 160, 38, 32);
			int gg = s.getY();
			pad(level, s.getX(), s.getZ(), s.getX() + 38, s.getZ() + 32, gg, 12, 16);
			meadow(level, s.getX() - 20, s.getZ() - 20, s.getX() + 58, s.getZ() + 52, 31, 0.35, 0.12);
			fence(level, new BlockPos(s.getX(), gg, s.getZ()), 2, 2, 36, 30);
			BlockPos o = new BlockPos(s.getX(), gg, s.getZ());
			pair[0] = cow(level, CattleBreed.BROWN_SWISS, Sex.MALE, 5301, 5 * 365, 950, o.getX() + 11.5, o.getZ() + 14.5, 90, false);
			pair[1] = cow(level, CattleBreed.JERSEY, Sex.FEMALE, 5302, 500, 300, o.getX() + 16.5, o.getZ() + 14.5, 270, false);
			pair[1].startHeatForDisplay();
			pair[2] = cow(level, CattleBreed.HOLSTEIN, Sex.FEMALE, 5303, 4 * 365, 650, o.getX() + 24.5, o.getZ() + 9.5, 200, false);
			pair[2].setOwner(srv.getPlayerList().getPlayers().get(0).getUUID());
			pair[2].startHeatForDisplay();
			pair[2].conceive(pair[0].genome(), level.getGameTime());
			Object[][] young = {{CattleBreed.HOLSTEIN, 90.0}, {CattleBreed.JERSEY, 70.0}, {CattleBreed.BROWN_SWISS, 100.0}, {CattleBreed.GYR, 80.0}, {CattleBreed.HOLSTEIN, 110.0}};
			for (int i = 0; i < young.length; i++) {
				calves.add(cow(level, (CattleBreed) young[i][0], i % 2 == 0 ? Sex.FEMALE : Sex.MALE, 5310 + i, 60 + i * 10, (double) young[i][1],
						o.getX() + 8.5 + i * 2.5, o.getZ() + 22.5 + (i % 2) * 2, i * 70, false));
			}
			cow(level, CattleBreed.JERSEY, Sex.FEMALE, 5320, 5 * 365, 430, o.getX() + 22.5, o.getZ() + 24.5, 30, false);
			cow(level, CattleBreed.GYR, Sex.FEMALE, 5321, 5 * 365, 420, o.getX() + 6.5, o.getZ() + 26.5, 300, false);
			discardItems(level, o, 60);
			ps[0] = o;
		});
		BlockPos po = ps[0];
		trees(ring(po.getX(), po.getZ(), po.getX() + 38, po.getZ() + 32, 6, 11, 7), "oak", "birch", "fancy_oak");
		Random rnd = new Random(17);
		// 15. the bull and the heifer in heat: he goes to her; an orbit, afternoon
		film("bull_heifer", 9000, LEN, (level, t) -> {
			place(pair[0], po.getX() + 11.5, po.getY(), po.getZ() + 14.5, 270);
			place(pair[1], po.getX() + 16.5, po.getY(), po.getZ() + 14.5, 90);
			pair[1].startHeatForDisplay();
		}, 10, orbit(po, 14, 14.5, 6, 5, 1.0, 0.9, 70, 125, 1.2), (level, t) -> {
			if (t % 20 == 0) {
				walk(pair[0], pair[1].getX() - 1.5, pair[1].getZ(), 0.9);
			}
		});
		// 16. the bull, low and close
		film("bull_close", 9600, LEN, (level, t) -> place(pair[0], po.getX() + 12.5, po.getY(), po.getZ() + 18.5, 200), 10, orbit(po, 13, 19, 4.2, 3.4, 0.3, 0.3, 120, 165, 1.3), NONE);
		// 17. a calf born in the spring grass
		film("calving", 1200, LEN, (level, t) -> place(pair[2], po.getX() + 24.5, po.getY(), po.getZ() + 9.5, 200), 10,
				dolly(new G(po, 20, 1.2, 14, 24.5, 0.8, 9.5), new G(po, 21, 1.0, 12.5, 24.5, 0.8, 9.5)), (level, t) -> {
					if (t == 40 && pair[2].isInCalf()) {
						pair[2].makeDueNow();
						pair[2].settle(level);
						log("calving: " + pair[2].lastCalving());
					}
				});
		// 18. calves among the flowers, low through them
		film("calves_spring", 1800, LEN, null, 10, dolly(new G(po, 4, 0.45, 29, 14, 0.8, 22), new G(po, 10, 0.45, 30, 18, 0.8, 22)), (level, t) ->
				roam(calves, t, 60, rnd, po.getX() + 4, po.getZ() + 16, po.getX() + 34, po.getZ() + 28, 0.9));
		// 19. the calves running about
		film("calves_run", 2200, LEN, null, 10, track(middle(calves), new Vec3(-2, 2.6, 10), new Vec3(-6, 2.0, 8), new Vec3(0, 0.6, 0), 0.06), (level, t) ->
				roam(calves, t, 30, rnd, po.getX() + 4, po.getZ() + 5, po.getX() + 34, po.getZ() + 28, 1.6));

		// ---- back to the barn: the scraper ----
		// 20. the manure scraper pushing down the alley, followed
		film("scraper_follow", 4000, LEN, (level, t) -> {
			if (BigParts.entity(level, new BlockPos(xd, g + 1, c.getZ())) instanceof DriveEntity d) {
				d.forceRunning(true);
			}
			for (int x = xd + 2; x < c.getX() - 1; x += 2) {
				ItemEntity e = new ItemEntity(level, x + 0.5, g + 0.3, c.getZ() + 0.5, new ItemStack(FarmingsItems.MANURE, 8));
				e.setDeltaMovement(Vec3.ZERO);
				level.addFreshEntity(e);
			}
			log("scraper pass: " + ((BarnControllerEntity) level.getBlockEntity(c)).startPass(level));
		}, 10, dolly(new G(bo, xd - c.getX() - 1, 4.5, 3, xd - c.getX() + 4, 0, 0), new G(bo, -3, 4, 3, 1, 0, 0)), NONE);
		// 21. low at the pit end, the scraper coming down the alley
		film("scraper_low", 4400, LEN, null, 5, dolly(new G(bo, 3.5, 0.9, 1.2, xd - c.getX(), 0.6, 0), new G(bo, 2.6, 0.8, 0.9, xd - c.getX(), 0.6, 0)), NONE);
		// 22. the farmstead at golden hour, a crane over it
		film("farmstead_golden", 12300, LEN, null, 20, dolly(new G(bo, 22, 12, 18, -10, 0, -6), new G(bo, 15, 7, 13, -10, 0, -6)), NONE);
		// 23. the sun going down behind the barn
		film("farmstead_sunset", 12500, LEN, null, 20, dolly(new G(bo, 12, 3, 5, -30, 6, -3), new G(bo, 24, 7, 8, -30, 6, -3)), NONE);
	}

	// =====================================================================================================================
	// 2. sheep and goats
	// =====================================================================================================================

	private void sheepGoats() {
		// ---- set H: the hill pasture, the flock and the sheep dog ----
		BlockPos[] hs = new BlockPos[1];
		List<Mob> flock = new ArrayList<>();
		SheepDogEntity[] dog = new SheepDogEntity[1];
		List<Mob> rams = new ArrayList<>();
		List<Mob> young = new ArrayList<>();
		double[][] top = {{-2, -1}, {0, -2}, {2, -1}, {-1, 1}, {1, 1}, {3, 1}, {-3, 2}, {0, 3}, {2, 3}, {-2, 4}, {4, -2}, {1, -4}};
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, plains.getX(), plains.getZ(), 64, 64);
			int g = s.getY();
			pad(level, s.getX(), s.getZ(), s.getX() + 64, s.getZ() + 64, g, 14, 20);
			BlockPos hc = new BlockPos(s.getX() + 32, g, s.getZ() + 26);
			hill(level, hc.getX(), hc.getZ(), 24, 9);
			meadow(level, s.getX() - 20, s.getZ() - 20, s.getX() + 84, s.getZ() + 84, 41, 0.3, 0.04);
			for (int i = 0; i < top.length; i++) {
				double x = hc.getX() + 0.5 + top[i][0], z = hc.getZ() + 0.5 + top[i][1];
				if (i < 9) {
					SheepBreed[] b = {SheepBreed.MERINO, SheepBreed.SUFFOLK, SheepBreed.DORPER, SheepBreed.EAST_FRIESIAN};
					flock.add(sheep(level, b[i % b.length], Sex.FEMALE, 6100 + i, 900, x, z, false));
				} else {
					GoatBreed[] b = {GoatBreed.SAANEN, GoatBreed.NUBIAN, GoatBreed.BOER};
					flock.add(goat(level, b[i % b.length], Sex.FEMALE, 6100 + i, 900, x, z, false));
				}
			}
			var player = srv.getPlayerList().getPlayers().get(0);
			SheepDogEntity d = FarmingsEntities.SHEEP_DOG.create(level, EntitySpawnReason.COMMAND);
			d.setWild(false);
			d.setOwner(player.getUUID());
			d.snapTo(hc.getX() + 0.5, surface(level, hc.getX(), hc.getZ() - 9), hc.getZ() - 8.5, 0, 0);
			level.addFreshEntity(d);
			dog[0] = d;
			// rams and bucks, out on the flat east of the hill
			Object[] males = {SheepBreed.MERINO, SheepBreed.SUFFOLK, SheepBreed.DORPER, GoatBreed.BOER, GoatBreed.NUBIAN, GoatBreed.SAANEN};
			for (int i = 0; i < males.length; i++) {
				double x = s.getX() + 58.5 + (i % 3) * 2.2, z = s.getZ() + 8.5 + (i / 3) * 6 + (i % 3);
				rams.add(males[i] instanceof SheepBreed sb ? sheep(level, sb, Sex.MALE, 6200 + i, 1000, x, z, false) : goat(level, (GoatBreed) males[i], Sex.MALE, 6200 + i, 1000, x, z, false));
			}
			// lambs and kids with their dams, south-west of the hill
			for (int i = 0; i < 10; i++) {
				double x = s.getX() + 8.5 + (i % 5) * 2.5, z = s.getZ() + 52.5 + (i / 5) * 3;
				boolean lamb = i < 6;
				young.add(lamb ? sheep(level, i % 2 == 0 ? SheepBreed.SUFFOLK : SheepBreed.MERINO, i % 3 == 0 ? Sex.MALE : Sex.FEMALE, 6300 + i, 30 + i * 4, x, z, false)
						: goat(level, i % 2 == 0 ? GoatBreed.BOER : GoatBreed.SAANEN, Sex.FEMALE, 6300 + i, 30 + i * 3, x, z, false));
			}
			SheepEntity e1 = sheep(level, SheepBreed.SUFFOLK, Sex.FEMALE, 6320, 900, s.getX() + 14.5, s.getZ() + 50.5, false);
			e1.setLambedForTest(30, 2);
			GoatEntity d1 = goat(level, GoatBreed.BOER, Sex.FEMALE, 6321, 900, s.getX() + 20.5, s.getZ() + 55.5, false);
			d1.setKiddedForTest(30, 2);
			discardItems(level, hc, 80);
			hs[0] = hc;
		});
		BlockPos hc = hs[0];
		BlockPos foot = hc.offset(0, 0, 30);
		trees(ring(hc.getX() - 32, hc.getZ() - 26, hc.getX() + 32, hc.getZ() + 38, 8, 12, 8), "oak", "birch", "fancy_oak", "spruce");
		Act flockTop = (level, t) -> {
			for (int i = 0; i < flock.size(); i++) {
				double x = hc.getX() + 0.5 + top[i][0], z = hc.getZ() + 0.5 + top[i][1];
				place(flock.get(i), x, surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, i * 30);
			}
			place(dog[0], hc.getX() + 0.5, surface(level, hc.getX(), hc.getZ() - 9), hc.getZ() - 8.5, 0);
			dog[0].hear(SheepDogEntity.Command.THATLL_DO, null, level.getServer().getPlayerList().getPlayers().get(0));
		};
		Act walkUp = (level, t) -> {
			flockTop.at(level, t);
			dev.farmings.item.WhistleItem.blow(level, level.getServer().getPlayerList().getPlayers().get(0), SheepDogEntity.Command.WALK_UP, foot);
		};
		Vec3 up = new Vec3(0, 1, 0);
		// 1. the hill at sunrise, the flock on top
		film("hill_dawn", 23300, LEN, flockTop, 30, dolly(new G(hc, -34, 5, 34, 0, 2, 0), new G(hc, -27, 4, 28, 0, 2, 0)), NONE);
		// 2. the dog brings the flock down the hill, from high beside
		film("dog_brings_flock", 800, 260, walkUp, 5, track(middle(flock), new Vec3(18, 9, 2), new Vec3(14, 6, 10), up, 0.06), NONE);
		// 3. behind the dog, low, the flock ahead of it
		film("dog_close", 1200, 240, walkUp, 5, track(at(dog[0]), new Vec3(-4, 1.3, -5), new Vec3(-3, 1.1, -4), new Vec3(0, 0.5, 6), 0.1), NONE);
		// 4. at the foot of the hill: the flock coming down toward the camera
		film("flock_down", 1600, 240, walkUp, 5, pan(new G(foot, 9, 2.5, 4, 0, 4, -20), new G(foot, 7, 2, 2, 0, 4, -20), middle(flock), new Vec3(0, 0.8, 0), 0.05), NONE);

		// ---- set S: the sheep shed, its pasture, rocks for the goats, the shearing shed, the wool mill and the creamery ----
		BlockPos[] os = new BlockPos[1];
		List<Mob> shedFlock = new ArrayList<>();
		List<GoatEntity> goats = new ArrayList<>();
		List<Mob> ewes = new ArrayList<>();
		List<Mob> lambs = new ArrayList<>();
		int[] agingX = new int[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, plains.getX() + 170, plains.getZ(), 52, 52);
			int g = s.getY();
			pad(level, s.getX(), s.getZ(), s.getX() + 52, s.getZ() + 52, g, 14, 20);
			BlockPos o = new BlockPos(s.getX() + 10, g, s.getZ() + 14);
			meadow(level, s.getX() - 20, s.getZ() - 20, s.getX() + 72, s.getZ() + 72, 51, 0.3, 0.03);
			BlockPos shed = o.offset(1, 0, 2);
			level.setBlockAndUpdate(shed, dev.farmings.shed.ShedBlocks.SHED_CONTROLLER.defaultBlockState());
			for (int x = 2; x <= 5; x++) {
				for (int z = 1; z <= 8; z++) {
					level.setBlockAndUpdate(o.offset(x, 0, z), z == 5 ? dev.farmings.shed.ShedBlocks.LAMBING_PEN.defaultBlockState() : dev.farmings.shed.ShedBlocks.SHEEP_PEN.defaultBlockState());
				}
			}
			fence(level, o, 6, -2, 30, 12);
			level.setBlockAndUpdate(o.offset(6, 0, 3), dev.farmings.shed.ShedBlocks.PASTURE_GATE.defaultBlockState());
			level.setBlockAndUpdate(o.offset(6, 0, 7), dev.farmings.shed.ShedBlocks.PASTURE_GATE.defaultBlockState());
			FarmingsClientGameTest.barnPart(level, BarnParts.HAY_FEEDER, o.offset(2, 0, 0));
			FarmingsClientGameTest.barnPart(level, BarnParts.WATER_TROUGH, o.offset(3, 0, 9));
			if (level.getBlockEntity(o.offset(2, 0, 0)) instanceof FeederEntity f) {
				for (int i = 0; i < 4; i++) {
					f.addBlock();
				}
			}
			if (level.getBlockEntity(BigParts.owner(level, o.offset(3, 0, 9))) instanceof TroughEntity t) {
				t.pourBucket();
			}
			// the shearing shed's stand, catching pen, wool table and press, as the sheep farm scene
			level.setBlockAndUpdate(o.offset(5, 0, 0), dev.farmings.shearing.ShearingBlocks.CATCHING_PEN.defaultBlockState());
			level.setBlockAndUpdate(o.offset(5, 0, -1), dev.farmings.shearing.ShearingBlocks.SHEARING_STAND.defaultBlockState());
			level.setBlockAndUpdate(o.offset(4, 0, -1), dev.farmings.shearing.ShearingBlocks.WOOL_TABLE.defaultBlockState());
			level.setBlockAndUpdate(o.offset(3, 0, -1), dev.farmings.shearing.ShearingBlocks.WOOL_PRESS.defaultBlockState());
			// a hay feeder out in the pasture for the ewes and lambs
			FarmingsClientGameTest.barnPart(level, BarnParts.HAY_FEEDER, o.offset(14, 0, 9));
			if (level.getBlockEntity(o.offset(14, 0, 9)) instanceof FeederEntity f) {
				for (int i = 0; i < 4; i++) {
					f.addBlock();
				}
			}
			// rocks in the pasture
			rocks(level, o.getX() + 24, o.getZ() + 4, 4, 4, 9);
			// the flock: ewes and does
			SheepBreed[] sb = {SheepBreed.MERINO, SheepBreed.MERINO, SheepBreed.SUFFOLK, SheepBreed.DORPER, SheepBreed.EAST_FRIESIAN, SheepBreed.SUFFOLK};
			for (int i = 0; i < sb.length; i++) {
				SheepEntity e = sheep(level, sb[i], Sex.FEMALE, 6400 + i, 900, o.getX() + 10.5 + (i % 3) * 2, o.getZ() + 2.5 + (i / 3) * 3, false);
				e.setFleeceDaysForTest(i == 0 ? 365 : 200);
				e.assignShed(shed, null);
				shedFlock.add(e);
			}
			GoatBreed[] gb = {GoatBreed.SAANEN, GoatBreed.SAANEN, GoatBreed.NUBIAN, GoatBreed.BOER};
			for (int i = 0; i < gb.length; i++) {
				GoatEntity gt = goat(level, gb[i], Sex.FEMALE, 6410 + i, 900, o.getX() + 18.5 + i * 1.5, o.getZ() + 6.5 + (i % 2) * 2, false);
				gt.setKiddedForTest(30, 1);
				gt.assignShed(shed, null);
				shedFlock.add(gt);
				goats.add(gt);
			}
			((dev.farmings.shed.ShedControllerEntity) level.getBlockEntity(shed)).forceDayForTest(true);
			// ewes with lambs at the pasture feeder
			for (int i = 0; i < 3; i++) {
				SheepEntity e = sheep(level, i == 0 ? SheepBreed.SUFFOLK : i == 1 ? SheepBreed.DORPER : SheepBreed.MERINO, Sex.FEMALE, 6420 + i, 900,
						o.getX() + 12.5 + i * 2, o.getZ() + 10.5, false);
				e.setLambedForTest(25, 2);
				ewes.add(e);
				for (int j = 0; j < 2; j++) {
					lambs.add(sheep(level, i == 0 ? SheepBreed.SUFFOLK : i == 1 ? SheepBreed.DORPER : SheepBreed.MERINO, j == 0 ? Sex.FEMALE : Sex.MALE, 6430 + i * 2 + j, 25,
							o.getX() + 13 + i * 2 + j, o.getZ() + 11.5, false));
				}
			}
			// the wool mill line, south of the pasture
			dev.farmings.woolmill.WoolMachine[] kinds = dev.farmings.woolmill.WoolMachine.values();
			for (int i = 0; i < kinds.length; i++) {
				BlockPos p = o.offset(2 + 2 * i, 0, 17);
				level.setBlockAndUpdate(p, dev.farmings.woolmill.WoolMillBlocks.machine(kinds[i]).defaultBlockState());
			}
			// the creamery, south of the mill
			BlockPos cc = o.offset(2, 0, 23);
			level.setBlock(cc, dev.farmings.FarmingsBlocks.CREAMERY_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
			int x = cc.getX() + 1;
			for (dev.farmings.creamery.CreameryParts kd : dev.farmings.creamery.CreameryParts.values()) {
				FarmingsClientGameTest.placePart(level, dev.farmings.FarmingsBlocks.creameryPart(kd), new BlockPos(x, g, cc.getZ()));
				if (kd == dev.farmings.creamery.CreameryParts.AGING_SHELF) {
					agingX[0] = x;
				}
				x += kd.part.length();
			}
			var ctl = (dev.farmings.creamery.CreameryControllerEntity) level.getBlockEntity(cc);
			ctl.refresh(level);
			discardItems(level, o, 80);
			os[0] = o;
		});
		BlockPos o = os[0];
		BlockPos shed = o.offset(1, 0, 2);
		trees(ring(o.getX() - 10, o.getZ() - 14, o.getX() + 42, o.getZ() + 38, 7, 12, 9), "oak", "birch", "fancy_oak");
		Act day = (level, t) -> ((dev.farmings.shed.ShedControllerEntity) level.getBlockEntity(shed)).forceDayForTest(true);
		// 5. evening: the ewes and does head in through the gates to the shed
		film("into_the_shed", 11600, 240, (level, t) -> {
			day.at(level, t);
			for (int i = 0; i < shedFlock.size(); i++) {
				place(shedFlock.get(i), o.getX() + 10.5 + (i % 5) * 2.2, o.getY(), o.getZ() + 1.5 + (i / 5) * 4, 270);
			}
		}, 10, dolly(new G(o, 20, 9, 18, 6, 0, 4), new G(o, 15, 7, 15, 5, 0, 4)), (level, t) -> {
			if (t == 1) {
				((dev.farmings.shed.ShedControllerEntity) level.getBlockEntity(shed)).forceDayForTest(false);
				log("shed: " + ((dev.farmings.shed.ShedControllerEntity) level.getBlockEntity(shed)).readout());
			}
		});
		// 6. low by the pens as they come in
		film("shed_pens", 11800, LEN, null, 5, dolly(new G(o, 9.5, 1.3, 9.5, 3.5, 0.5, 4), new G(o, 8.5, 1.1, 7.5, 3.5, 0.5, 4)), NONE);
		// 7. goats climbing the rocks: an orbit
		BlockPos rock = o.offset(24, 0, 4);
		Random rnd = new Random(23);
		film("goats_climb", 2500, LEN, (level, t) -> {
			day.at(level, t);
			for (int i = 0; i < goats.size(); i++) {
				place(goats.get(i), rock.getX() - 5.5 + i * 3, o.getY(), rock.getZ() + 5.5, 0);
			}
		}, 10, orbit(o, 24.5, 4.5, 10, 8.5, 3, 4.5, 60, 125, 2.5), (level, t) -> {
			for (int i = 0; i < goats.size(); i++) {
				if ((t + i * 11) % 40 == 0) {
					int dx = (i % 2 == 0 ? 1 : -1) * (i / 2), dz = i % 2;
					BlockPos to = new BlockPos(rock.getX() + dx, surface(level, rock.getX() + dx, rock.getZ() + dz), rock.getZ() + dz);
					goats.get(i).getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 1.0);
				}
			}
		});
		// 8. from below, goats on the rock against the sky
		film("goats_on_top", 3000, LEN, null, 5, dolly(new G(rock, -7, 1.0, 6.5, 0, 3, 0), new G(rock, -5.5, 0.8, 5, 0, 3.2, 0)), (level, t) -> {
			for (int i = 0; i < goats.size(); i++) {
				if ((t + i * 13) % 50 == 0) {
					int dx = rnd.nextInt(3) - 1, dz = rnd.nextInt(3) - 1;
					goats.get(i).getNavigation().moveTo(rock.getX() + dx + 0.5, surface(level, rock.getX() + dx, rock.getZ() + dz), rock.getZ() + dz + 0.5, 1.0);
				}
			}
		});
		// 9. ewes and lambs at the hay feeder
		BlockPos feeder = o.offset(14, 0, 9);
		film("lambs_feed", 3200, LEN, (level, t) -> {
			for (int i = 0; i < ewes.size(); i++) {
				place(ewes.get(i), feeder.getX() - 1.5 + i * 1.6, o.getY(), feeder.getZ() - 1.2, 180);
			}
			for (int i = 0; i < lambs.size(); i++) {
				place(lambs.get(i), feeder.getX() - 2 + i * 0.9, o.getY(), feeder.getZ() - 2.6 - (i % 2) * 0.8, 180 + i * 20);
			}
		}, 10, dolly(new G(feeder, 3.5, 0.7, -4.5, 0.5, 0.6, -0.5), new G(feeder, 2.2, 0.6, -3.4, 0.5, 0.6, -0.5)), (level, t) -> {
			if (t % 40 == 0) {
				for (Mob l : lambs) {
					walk(l, feeder.getX() + 0.5 + rnd.nextGaussian(), feeder.getZ() - 1.8 + rnd.nextGaussian() * 0.6, 1.0);
				}
			}
		});
		// 10. a ewe walking the pasture, her lambs at her heels
		film("lambs_follow", 3600, LEN, (level, t) -> {
			place(ewes.get(0), o.getX() + 10.5, o.getY(), o.getZ() + 3.5, 0);
			for (int i = 0; i < 2; i++) {
				place(lambs.get(i), o.getX() + 10 + i, o.getY(), o.getZ() + 2, 0);
			}
		}, 10, track(at(ewes.get(0)), new Vec3(5, 1.1, 2), new Vec3(4, 0.9, -2), new Vec3(0, 0.6, 0), 0.07), (level, t) -> {
			if (t % 30 == 0) {
				walk(ewes.get(0), o.getX() + 10.5 + t / 12.0, o.getZ() + 9.5, 0.8);
			}
			if (t % 10 == 0) {
				for (int i = 0; i < 2; i++) {
					walk(lambs.get(i), ewes.get(0).getX() - 1.2 + i * 0.6, ewes.get(0).getZ() - 1.4, 1.2);
				}
			}
		});

		// ---- set C: the taming corral, wild sheep and goats ----
		BlockPos[] ks = new BlockPos[1];
		List<Mob> wild = new ArrayList<>();
		double[][] wildSpots = {{-4, 12}, {-2, 13}, {0, 12.5}, {2, 13.5}, {4, 12}, {-3, 15}, {1, 15.5}, {3, 16}, {-1, 17}};
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, plains.getX(), plains.getZ() + 170, 30, 26);
			int g = s.getY();
			pad(level, s.getX(), s.getZ(), s.getX() + 30, s.getZ() + 26, g, 12, 16);
			meadow(level, s.getX() - 30, s.getZ() - 30, s.getX() + 60, s.getZ() + 60, 61, 0.3, 0.02);
			BlockPos k = new BlockPos(s.getX() + 8, g, s.getZ() + 4);
			corral(level, k, 14, 12);
			BlockPos gate = k.offset(7, 0, 12);
			for (int i = 0; i < wildSpots.length; i++) {
				double x = gate.getX() + 0.5 + wildSpots[i][0], z = gate.getZ() + 0.5 + wildSpots[i][1];
				wild.add(i < 6 ? sheep(level, i % 2 == 0 ? SheepBreed.MERINO : SheepBreed.SUFFOLK, i == 0 ? Sex.MALE : Sex.FEMALE, 6500 + i, 700, x, z, true)
						: goat(level, i % 2 == 0 ? GoatBreed.BOER : GoatBreed.NUBIAN, Sex.FEMALE, 6500 + i, 700, x, z, true));
			}
			discardItems(level, k, 60);
			ks[0] = k;
		});
		BlockPos k = ks[0];
		BlockPos gate = k.offset(7, 0, 12);
		trees(ring(k.getX() - 8, k.getZ() - 4, k.getX() + 22, k.getZ() + 34, 8, 12, 10), "oak", "birch");
		// 11. the bait draws the wild sheep and goats in through the gate
		film("corral_bait", 1500, 240, (level, t) -> {
			for (int i = 0; i < wild.size(); i++) {
				double x = gate.getX() + 0.5 + wildSpots[i][0], z = gate.getZ() + 0.5 + wildSpots[i][1];
				place(wild.get(i), x, surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, 180);
			}
			bait(level, k, 14, false);
		}, 5, track(middle(wild), new Vec3(4, 5, 10), new Vec3(2, 3.5, 7), new Vec3(0, 0, -5), 0.06), NONE);
		// 12. settled in the corral at golden hour
		film("corral_settled", 12300, LEN, (level, t) -> {
			for (int i = 0; i < wild.size(); i++) {
				place(wild.get(i), k.getX() + 3.5 + (i * 3) % 9, k.getY(), k.getZ() + 2.5 + (i % 3) * 2.5, i * 40);
			}
		}, 20, orbit(k, 7, 6, 14, 11, 4, 3, 210, 265, 1), NONE);

		// ---- rams and bucks, on the flat east of the hill ----
		// 13. rams, low and close
		film("rams", 3000, LEN, (level, t) -> { for (int i = 0; i < 3; i++) { place(rams.get(i), hc.getX() + 27 + i * 1.6, hc.getY(), hc.getZ() - 15.5 + (i % 2), 160 + i * 30); } }, 10, orbit(hc, 28.5, -15.3, 6, 5, 0.5, 0.4, 20, 75, 1.0), NONE);
		// 14. bucks, low
		film("bucks", 3400, LEN, (level, t) -> { for (int i = 3; i < 6; i++) { place(rams.get(i), hc.getX() + 27 + (i - 3) * 1.6, hc.getY(), hc.getZ() - 9.5 + (i % 2), 200 + i * 30); } }, 10, orbit(hc, 28.5, -9.3, 6, 4.8, 0.4, 0.4, 100, 150, 1.0), NONE);

		// ---- back at the shed: shearing, the mill, the creamery ----
		// 15. the shearing stand: the shed's night, the stand running; the Merino with a year's fleece is called in and shorn
		film("shearing", 12000, 240, (level, t) -> {
			((dev.farmings.shed.ShedControllerEntity) level.getBlockEntity(shed)).forceDayForTest(false);
			((dev.farmings.shearing.ShearingStandEntity) level.getBlockEntity(o.offset(5, 0, -1))).forceRunning(true);
			((SheepEntity) shedFlock.get(0)).setFleeceDaysForTest(365);
			((SheepEntity) shedFlock.get(1)).setFleeceDaysForTest(365);
		}, 160, dolly(new G(o, 9.5, 3, -4.5, 4.5, 0.5, -0.5), new G(o, 8, 2.4, -3.5, 4.5, 0.5, -0.5)), NONE);
		// 16. close at the stand
		film("shearing_close", 12000, LEN, null, 5, dolly(new G(o, 7.2, 1.2, -2.2, 5, 0.6, -0.5), new G(o, 6.6, 1.0, -2.9, 5, 0.6, -0.5)), NONE);
		// 17. the wool mill line running: scouring, carding, spinning, the loom
		Act mill = (level, t) -> {
			dev.farmings.woolmill.WoolMachine[] kinds = dev.farmings.woolmill.WoolMachine.values();
			for (int i = 0; i < kinds.length; i++) {
				if (level.getBlockEntity(o.offset(2 + 2 * i, 0, 17)) instanceof dev.farmings.woolmill.WoolMachineEntity m) {
					m.forceRunning(true);
				}
			}
			var ms = new dev.farmings.woolmill.WoolMachineEntity[kinds.length];
			for (int i = 0; i < kinds.length; i++) {
				ms[i] = (dev.farmings.woolmill.WoolMachineEntity) level.getBlockEntity(o.offset(2 + 2 * i, 0, 17));
			}
			ms[0].pourBucket();
			ms[0].insertByHand(dev.farmings.item.FleeceItem.make(FarmingsItems.FLEECE, 4.4, 20.5, 8, 0.5, dev.farmings.husbandry.Flock.Grade.FINE, dev.farmings.genetics.SheepLook.Wool.WHITE));
			ms[1].insertByHand(dev.farmings.item.FleeceItem.make(FarmingsItems.CLEAN_WOOL, 2.2, 20.5, 8, 1, dev.farmings.husbandry.Flock.Grade.FINE, dev.farmings.genetics.SheepLook.Wool.WHITE));
			ms[2].insertByHand(dev.farmings.item.FleeceItem.make(FarmingsItems.SLIVER, 2.0, 29, 9, 1, dev.farmings.husbandry.Flock.Grade.QUARTER, dev.farmings.genetics.SheepLook.Wool.BLACK));
			ms[3].insertByHand(new ItemStack(FarmingsItems.YARN.get(net.minecraft.world.item.DyeColor.RED), 8));
			if (ms.length > 4) {
				ms[4].insertByHand(dev.farmings.item.FleeceItem.make(FarmingsItems.CLEAN_WOOL, 1.0, 30, 9, 1, dev.farmings.husbandry.Flock.Grade.QUARTER, dev.farmings.genetics.SheepLook.Wool.WHITE));
			}
		};
		film("wool_mill", 6000, LEN, mill, 40, dolly(new G(o, 1, 1.1, 19.6, 4, 0.5, 17), new G(o, 10, 1.1, 19.6, 8, 0.5, 17)), NONE);
		// 18. a machine close, with what the line has made shown over it
		film("wool_close", 6000, LEN, (level, t) -> {
			for (int i = 0; i < dev.farmings.woolmill.WoolMachine.values().length; i++) {
				if (level.getBlockEntity(o.offset(2 + 2 * i, 0, 17)) instanceof dev.farmings.woolmill.WoolMachineEntity m && !m.items().get(1).isEmpty()) {
					ItemEntity shown = new ItemEntity(level, m.getBlockPos().getX() + 0.5, m.getBlockPos().getY() + 1.1, m.getBlockPos().getZ() + 0.5, m.items().get(1).copy());
					shown.setNoGravity(true);
					shown.setDeltaMovement(Vec3.ZERO);
					shown.setNeverPickUp();
					level.addFreshEntity(shown);
				}
			}
		}, 10, dolly(new G(o, 8, 1.5, 19.5, 6.5, 0.9, 17.5), new G(o, 5.5, 1.3, 19.3, 5.5, 0.9, 17.5)), NONE);
		// 19. the does in milk
		film("goat_milk", 2000, LEN, (level, t) -> {
			day.at(level, t);
			for (int i = 0; i < goats.size(); i++) {
				place(goats.get(i), o.getX() + 12.5 + i * 1.8, o.getY(), o.getZ() + 5.5 + (i % 2), 90);
			}
		}, 20, track(at(goats.get(0)), new Vec3(3.2, 0.8, 2.6), new Vec3(2.2, 0.7, -2.4), new Vec3(0, 0.7, 0), 0.07), NONE);
		// 20. the creamery making cheese from goat milk
		BlockPos cc = o.offset(2, 0, 23);
		film("creamery", 6000, LEN, (level, t) -> {
			var ctl = (dev.farmings.creamery.CreameryControllerEntity) level.getBlockEntity(cc);
			ctl.refresh(level);
			for (dev.farmings.process.creamery.CreameryPart p : new dev.farmings.process.creamery.CreameryPart[] {dev.farmings.process.creamery.CreameryPart.BATCH_PASTEURIZER,
					dev.farmings.process.creamery.CreameryPart.CREAM_SEPARATOR, dev.farmings.process.creamery.CreameryPart.BUTTER_CHURN, dev.farmings.process.creamery.CreameryPart.CHEESE_VAT}) {
				ctl.machine(level, p).forceRunning(true);
			}
			ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.CHEESE);
			ctl.creamery().addMilk(200, goats.get(0).milkFatPct(), goats.get(0).milkProteinPct(), dev.farmings.husbandry.parlor.MilkKind.GOAT);
			log("creamery: " + ctl.readout());
		}, 20, dolly(new G(cc, 2, 2.4, 6, 4, 0.8, 0), new G(cc, 9, 2.0, 5, 8, 0.8, 0)), (level, t) -> {
			if (t % 20 == 0) {
				((dev.farmings.creamery.CreameryControllerEntity) level.getBlockEntity(cc)).run(level, 20);
			}
		});
		// 21. the cheese room: the aging shelf, goat cheese and milk set out before it
		film("cheese_room", 6400, LEN, (level, t) -> {
			int n = 0;
			for (ItemStack st : new ItemStack[] {new ItemStack(FarmingsItems.CHEVRE, 4), new ItemStack(FarmingsItems.GOAT_MILK_BOTTLE, 3), new ItemStack(FarmingsItems.CHEDDAR_WHEEL, 2)}) {
				ItemEntity e = new ItemEntity(level, agingX[0] - 1 + n * 1.2, o.getY() + 0.2, cc.getZ() + 1.6, st);
				e.setDeltaMovement(Vec3.ZERO);
				e.setNeverPickUp();
				level.addFreshEntity(e);
				n++;
			}
		}, 20, dolly(new G(cc, agingX[0] - cc.getX() + 2.5, 1.2, 3.5, agingX[0] - cc.getX() + 0.5, 0.8, 0.5), new G(cc, agingX[0] - cc.getX() + 0.5, 1.0, 3.0, agingX[0] - cc.getX() + 0.5, 0.8, 0.5)), NONE);
		// 22. lambs and kids in the spring grass, low through the flowers
		BlockPos yo = hc.offset(-24, 0, 26);
		film("spring_young", 1000, LEN, (level, t) -> { for (int i = 0; i < young.size(); i++) { place(young.get(i), yo.getX() + 1.5 + (i % 5) * 2, yo.getY(), yo.getZ() + 0.5 + (i / 5) * 2.5, i * 40); } }, 10, dolly(new G(yo, -1, 0.5, 7.5, 5, 0.6, 1.5), new G(yo, 6, 0.5, 8, 7, 0.6, 1.5)), (level, t) ->
				roam(young, t, 35, rnd, yo.getX(), yo.getZ() - 2, yo.getX() + 12, yo.getZ() + 5, 1.4));
		// 23. the flock on the hill at sunset
		film("hill_sunset", 12500, LEN, flockTop, 20, dolly(new G(hc, 26, 3, 10, -20, 8, -4), new G(hc, 36, 6, 14, -20, 8, -4)), NONE);
	}

	// =====================================================================================================================
	// 3. horses
	// =====================================================================================================================

	private void horses() {
		// ---- set R: the river valley ----
		BlockPos[] rs = new BlockPos[1];
		List<HorseEntity> herd = new ArrayList<>();
		HorseEntity[] fast = new HorseEntity[1];
		int len = 150;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, plains.getX(), plains.getZ(), len, 40);
			int g = s.getY();
			pad(level, s.getX(), s.getZ(), s.getX() + len, s.getZ() + 40, g, 14, 20);
			// the river: 7 wide, meandering along x, its water a block under the banks
			for (int x = s.getX() - 14; x <= s.getX() + len + 14; x++) {
				double zc = s.getZ() + 16 + 4 * Math.sin((x - s.getX()) / 14.0);
				for (int z = (int) zc - 6; z <= (int) zc + 6; z++) {
					double d = Math.abs(z + 0.5 - zc);
					if (d <= 3.5) {
						level.setBlock(new BlockPos(x, g - 4, z), Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
						for (int y = g - 3; y <= g - 2; y++) {
							level.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
						level.setBlock(new BlockPos(x, g - 1, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					} else if (d <= 4.5) {
						level.setBlock(new BlockPos(x, g - 2, z), Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
						level.setBlock(new BlockPos(x, g - 1, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			meadow(level, s.getX() - 20, s.getZ() - 20, s.getX() + len + 20, s.getZ() + 60, 71, 0.3, 0.03);
			// reeds and lily pads
			Random r = new Random(72);
			for (int x = s.getX(); x <= s.getX() + len; x++) {
				double zc = s.getZ() + 16 + 4 * Math.sin((x - s.getX()) / 14.0);
				if (r.nextInt(4) == 0) {
					BlockPos p = new BlockPos(x, g - 1, (int) Math.floor(zc - 4));
					if (level.getBlockState(p.below()).is(Blocks.SAND)) {
						level.setBlock(p, Blocks.SUGAR_CANE.defaultBlockState(), Block.UPDATE_CLIENTS);
						level.setBlock(p.above(), Blocks.SUGAR_CANE.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
				if (r.nextInt(7) == 0) {
					level.setBlock(new BlockPos(x, g - 1, (int) Math.floor(zc + r.nextGaussian())), Blocks.LILY_PAD.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			for (int i = 0; i < 7; i++) {
				herd.add(horse(level, i % 3 == 0 ? HorseBreed.ARABIAN : i % 3 == 1 ? HorseBreed.QUARTER_HORSE : HorseBreed.THOROUGHBRED, i == 0 ? Sex.MALE : Sex.FEMALE,
						7100 + i, i == 5 ? 200 : 5 * 365, i, s.getX() + 20.5 - (i % 3) * 3, s.getZ() + 26.5 + (i % 3) * 2, true));
			}
			fast[0] = horse(level, HorseBreed.THOROUGHBRED, Sex.MALE, 7150, 5 * 365, 0, s.getX() + 8.5, s.getZ() + 30.5, false);
			fast[0].setFitnessForTest(0.95);
			discardItems(level, s, 100);
			rs[0] = s;
		});
		BlockPos rv = rs[0];
		java.util.function.DoubleUnaryOperator zc = x -> rv.getZ() + 16 + 4 * Math.sin((x - rv.getX()) / 14.0);
		List<int[]> riverTrees = new ArrayList<>();
		Random tr = new Random(73);
		for (int x = rv.getX() - 10; x <= rv.getX() + len + 10; x += 9 + tr.nextInt(6)) {
			riverTrees.add(new int[] {x, (int) zc.applyAsDouble(x) - 8 - tr.nextInt(6)});
		}
		for (int x = rv.getX() - 10; x <= rv.getX() + len + 10; x += 16 + tr.nextInt(10)) {
			riverTrees.add(new int[] {x, rv.getZ() + 44 + tr.nextInt(8)});
		}
		trees(riverTrees, "oak", "birch", "fancy_oak", "oak");
		// the herd runs east along the south bank, each in its own lane
		Act herdStart = (level, t) -> {
			for (int i = 0; i < herd.size(); i++) {
				double x = rv.getX() + 18.5 - (i % 4) * 3 - i, z = zc.applyAsDouble(x) + 8 + (i % 3) * 2.2;
				place(herd.get(i), x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)), z, -90);
				herd.get(i).setNoAi(true);
			}
		};
		Act herdRun = (level, t) -> {
			for (int i = 0; i < herd.size(); i++) {
				HorseEntity h = herd.get(i);
				double x = h.getX() + 6, z = zc.applyAsDouble(x) + 8 + (i % 3) * 2.2;
				gallop(h, x, z, i == 5 ? 8.6 : 9 + (i % 3) * 0.3);
			}
		};
		Vec3 up = new Vec3(0, 1.2, 0);
		// 1. dawn: wild horses running by the river toward the sun
		film("river_dawn", 23900, LEN, herdStart, 30, track(middle(herd), new Vec3(-9, 7, 14), new Vec3(4, 6, 12), up, 0.1), herdRun);
		// 2. alongside them, low, the river beyond
		film("river_running", 600, LEN, herdStart, 10, track(at(herd.get(0)), new Vec3(1, 1.6, 8), new Vec3(-2, 1.4, 7), new Vec3(1, 1, -1), 0.25), herdRun);
		// 3. across the water: they pass
		film("river_pass", 1200, LEN, herdStart, 10, pan(new G(rv, 62, 1.3, 17.5, 50, 1, 22), new G(rv, 64, 1.1, 17.5, 50, 1, 22), middle(herd), up, 0.12), herdRun);

		// ---- set C: the taming corral ----
		BlockPos[] ks = new BlockPos[1];
		List<HorseEntity> wild = new ArrayList<>();
		double[][] wildSpots = {{-5, 13}, {-1, 14}, {3, 13}, {6, 15}, {-3, 17}, {1, 18}};
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, plains.getX(), plains.getZ() + 170, 34, 28);
			int g = s.getY();
			pad(level, s.getX(), s.getZ(), s.getX() + 34, s.getZ() + 28, g, 12, 16);
			meadow(level, s.getX() - 30, s.getZ() - 30, s.getX() + 64, s.getZ() + 64, 81, 0.3, 0.02);
			BlockPos k = new BlockPos(s.getX() + 9, g, s.getZ() + 4);
			corral(level, k, 16, 14);
			BlockPos gate = k.offset(8, 0, 14);
			for (int i = 0; i < wildSpots.length; i++) {
				wild.add(horse(level, i % 2 == 0 ? HorseBreed.QUARTER_HORSE : HorseBreed.ARABIAN, i == 0 ? Sex.MALE : Sex.FEMALE, 7200 + i, 4 * 365, i + 3,
						gate.getX() + 0.5 + wildSpots[i][0], gate.getZ() + 0.5 + wildSpots[i][1], true));
			}
			discardItems(level, k, 60);
			ks[0] = k;
		});
		BlockPos k = ks[0];
		BlockPos gate = k.offset(8, 0, 14);
		trees(ring(k.getX() - 9, k.getZ() - 4, k.getX() + 25, k.getZ() + 36, 8, 12, 11), "oak", "birch", "fancy_oak");
		Act wildOut = (level, t) -> {
			for (int i = 0; i < wild.size(); i++) {
				double x = gate.getX() + 0.5 + wildSpots[i][0], z = gate.getZ() + 0.5 + wildSpots[i][1];
				place(wild.get(i), x, surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, 180);
			}
		};
		// 4. hay in the bait trough draws the wild horses to the corral
		film("lure_corral", 1600, 240, (level, t) -> {
			wildOut.at(level, t);
			bait(level, k, 16, false);
		}, 5, track(middle(wild), new Vec3(5, 7, 12), new Vec3(3, 5, 9), new Vec3(0, 0, -6), 0.06), NONE);
		// 5. at the gate, low, as they come in
		film("corral_gate", 2000, LEN, (level, t) -> {
			for (int i = 0; i < wild.size(); i++) {
				double x = gate.getX() + 0.5 + wildSpots[i][0] * 0.6, z = gate.getZ() + 5.5 + (wildSpots[i][1] - 13) * 0.9;
				place(wild.get(i), x, surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, 180);
			}
			bait(level, k, 16, false);
		}, 10, dolly(new G(gate, 3.5, 0.8, -3.5, -0.5, 1.2, 6), new G(gate, 2.5, 0.9, -5, -0.5, 1.2, 6)), NONE);
		// 6. gentling: at the hay, close
		BlockPos bt = k.offset(8, 0, 3);
		Act inside = (level, t) -> {
			for (int i = 0; i < wild.size(); i++) {
				place(wild.get(i), k.getX() + 3.5 + (i * 3) % 11, k.getY(), k.getZ() + 3.5 + (i % 3) * 3, i * 60);
			}
		};
		film("gentling_hay", 3000, LEN, (level, t) -> {
			inside.at(level, t);
			bait(level, k, 16, false);
		}, 60, dolly(new G(bt, 4, 1.1, 4.5, 0.5, 0.9, 0.5), new G(bt, 2.8, 1.0, 3.3, 0.5, 0.9, 0.5)), NONE);
		// 7. and the water
		BlockPos wt = k.offset(11, 0, 5);
		film("gentling_water", 3400, LEN, null, 10, dolly(new G(wt, -3, 1.2, 4.5, 1, 0.6, 0.5), new G(wt, -2, 1.0, 3.3, 1, 0.6, 0.5)), NONE);
		// 8. the corral at golden hour
		film("corral_golden", 12300, LEN, null, 10, orbit(k, 8, 7, 17, 14, 5, 3.5, 200, 255, 1.2), NONE);

		// ---- back at the river: one fast horse ----
		Act fastStart = (level, t) -> {
			double x = rv.getX() + 8.5, z = zc.applyAsDouble(x) + 10;
			place(fast[0], x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z), z, -90);
			fast[0].setNoAi(true);
		};
		Act fastRun = (level, t) -> {
			double x = fast[0].getX() + 8, z = zc.applyAsDouble(x) + 10;
			gallop(fast[0], x, z, Math.min(15, 4 + t * 0.25));
		};
		// 9. a Thoroughbred at full gallop, tracked from beside
		film("fast_horse", 4500, 180, fastStart, 10, track(at(fast[0]), new Vec3(0, 1.5, 6.5), new Vec3(-2, 1.3, 5.5), new Vec3(2, 1.1, 0), 0.35), fastRun);
		// 10. head-on and past, low
		film("fast_horse_past", 5000, LEN, fastStart, 10, pan(new G(rv, 92, 0.5, zc.applyAsDouble(rv.getX() + 92) - rv.getZ() + 13, 60, 1, 26),
				new G(rv, 92, 0.5, zc.applyAsDouble(rv.getX() + 92) - rv.getZ() + 13, 60, 1, 26), at(fast[0]), new Vec3(0, 1.1, 0), 0.3), fastRun);

		// ---- set T: the race track at sunset ----
		BlockPos[] ts = new BlockPos[2];
		List<HorseEntity> field = new ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, plains.getX() + 200, plains.getZ(), 80, 48);
			BlockPos o = s.offset(40, 0, 24);
			int g = o.getY();
			pad(level, o.getX() - 40, o.getZ() - 22, o.getX() + 40, o.getZ() + 22, g, 16, 20);
			meadow(level, o.getX() - 60, o.getZ() - 44, o.getX() + 60, o.getZ() + 44, 91, 0.25, 0.01);
			BlockPos gate2 = FarmingsClientGameTest.raceTrack(level, o.getX(), o.getZ(), g);
			var gt = (dev.farmings.racing.StartGateEntity) level.getBlockEntity(gate2);
			gt.refresh(level);
			log("start gate: " + gt.formed() + " " + gt.problem() + ", stalls " + gt.stalls().size());
			HorseBreed[] b = {HorseBreed.THOROUGHBRED, HorseBreed.QUARTER_HORSE, HorseBreed.ARABIAN, HorseBreed.THOROUGHBRED, HorseBreed.THOROUGHBRED, HorseBreed.ARABIAN};
			for (int i = 0; i < Math.min(b.length, gt.stalls().size()); i++) {
				var h = horse(level, b[i], Sex.MALE, 7300 + i, 5 * 365, i + 1, o.getX(), o.getZ(), false);
				h.setFitnessForTest(0.9);
				field.add(h);
			}
			discardItems(level, o, 80);
			ts[0] = o;
			ts[1] = gate2;
		});
		BlockPos to = ts[0];
		BlockPos sg = ts[1];
		trees(ring(to.getX() - 40, to.getZ() - 22, to.getX() + 40, to.getZ() + 22, 12, 14, 12), "oak", "fancy_oak", "birch");
		Act race = (level, t) -> {
			var gt = (dev.farmings.racing.StartGateEntity) level.getBlockEntity(sg);
			if (gt.state() != dev.farmings.racing.StartGateEntity.State.IDLE) {
				return;     // the race in hand runs on into this shot
			}
			for (int i = 0; i < field.size(); i++) {
				field.get(i).releaseFromRace();
				BlockPos st = gt.stalls().get(i);
				place(field.get(i), st.getX() + 0.5, to.getY(), st.getZ() + 0.5, 180);
				field.get(i).setNoAi(false);
			}
			log("race: " + gt.start(level));
		};
		Supplier<Vec3> leader = () -> field.get(0).position();
		// 11. at the gate: they're off
		film("race_start", 12000, 320, race, 5, orbit(sg, 0.5, 0, 9, 8, 1.6, 2.2, 120, 165, 1.2), NONE);
		// 12. beside the leaders, from the infield
		film("race_alongside", 12200, 300, race, 50, (level, s, t) -> {
			Vec3 p = leader.get();
			Vec3 in = new Vec3(to.getX() - p.x, 0, to.getZ() - p.z).normalize();
			return new Cam(p.x + in.x * 7, p.y + 1.8, p.z + in.z * 7, p.x, p.y + 1, p.z);
		}, NONE);
		// 13. the whole track from high, the sun going down
		film("race_wide", 12000, 300, race, 40, dolly(new G(to, -6, 15, 32, 0, 0, 0), new G(to, 6, 12, 28, 0, 0, 0)), NONE);
		// 14. low on the infield by the home straight
		film("race_rail", 12100, 300, race, 40, pan(new G(to, -4, 1.0, 9.5, 0, 1, 14), new G(to, 4, 1.0, 9.5, 0, 1, 14), leader, new Vec3(0, 1, 0), 0.15), NONE);

		// ---- set S: the stable, its paddock, sire and mare, foals ----
		BlockPos[] ss = new BlockPos[1];
		List<HorseEntity> mares = new ArrayList<>();
		List<HorseEntity> foals = new ArrayList<>();
		HorseEntity[] sire = new HorseEntity[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, plains.getX() + 200, plains.getZ() + 170, 44, 36);
			int g = s.getY();
			pad(level, s.getX(), s.getZ(), s.getX() + 44, s.getZ() + 36, g, 14, 16);
			meadow(level, s.getX() - 20, s.getZ() - 20, s.getX() + 64, s.getZ() + 56, 101, 0.3, 0.08);
			BlockPos st = s.offset(4, 0, 6);
			java.util.function.BiFunction<Integer, Integer, BlockPos> at = (x, z) -> st.offset(x, 0, z);
			fence(level, at.apply(0, 0), 5, -1, 36, 24);
			level.setBlockAndUpdate(at.apply(1, 2), dev.farmings.stable.StableBlocks.STABLE_CONTROLLER.defaultBlockState());
			for (int x = 2; x <= 4; x++) {
				for (int z = 1; z <= 8; z++) {
					level.setBlockAndUpdate(at.apply(x, z), dev.farmings.stable.StableBlocks.HORSE_STALL.defaultBlockState());
				}
			}
			level.setBlockAndUpdate(at.apply(5, 3), dev.farmings.shed.ShedBlocks.PASTURE_GATE.defaultBlockState());
			level.setBlockAndUpdate(at.apply(5, 6), dev.farmings.shed.ShedBlocks.PASTURE_GATE.defaultBlockState());
			FarmingsClientGameTest.barnPart(level, BarnParts.HAY_FEEDER, at.apply(2, 0));
			FarmingsClientGameTest.barnPart(level, BarnParts.WATER_TROUGH, at.apply(3, 9));
			if (level.getBlockEntity(at.apply(2, 0)) instanceof FeederEntity f) {
				for (int i = 0; i < 4; i++) {
					f.addBlock();
				}
			}
			if (level.getBlockEntity(BigParts.owner(level, at.apply(3, 9))) instanceof TroughEntity t) {
				t.pourBucket();
			}
			if (level.getBlockEntity(at.apply(1, 2)) instanceof dev.farmings.stable.StableControllerEntity stable) {
				stable.forceDayForTest(true);
				stable.refresh(level);
				log("stable: " + stable.status());
			}
			HorseBreed[] mb = {HorseBreed.THOROUGHBRED, HorseBreed.ARABIAN, HorseBreed.QUARTER_HORSE, HorseBreed.THOROUGHBRED};
			for (int i = 0; i < mb.length; i++) {
				mares.add(horse(level, mb[i], Sex.FEMALE, 7400 + i, 6 * 365, i + 2, st.getX() + 12.5 + i * 3, st.getZ() + 6.5 + (i % 2) * 4, false));
			}
			sire[0] = horse(level, HorseBreed.THOROUGHBRED, Sex.MALE, 7410, 7 * 365, 2, st.getX() + 22.5, st.getZ() + 9.5, false);
			for (int i = 0; i < 3; i++) {
				foals.add(horse(level, mb[i], i == 1 ? Sex.MALE : Sex.FEMALE, 7420 + i, 50 + i * 15, i + 4, st.getX() + 14.5 + i * 3, st.getZ() + 14.5, false));
			}
			discardItems(level, st, 60);
			ss[0] = st;
		});
		BlockPos st = ss[0];
		BlockPos stable = st.offset(1, 0, 2);
		trees(ring(st.getX() - 4, st.getZ() - 6, st.getX() + 40, st.getZ() + 30, 7, 11, 13), "oak", "birch", "fancy_oak");
		List<HorseEntity> all = new ArrayList<>(mares);
		all.add(sire[0]);
		all.addAll(foals);
		Act stableDay = (level, t) -> ((dev.farmings.stable.StableControllerEntity) level.getBlockEntity(stable)).forceDayForTest(true);
		// 15. evening: into the stable
		film("into_the_stable", 11500, 240, (level, t) -> {
			stableDay.at(level, t);
			for (int i = 0; i < all.size(); i++) {
				place(all.get(i), st.getX() + 10.5 + (i % 4) * 2.5, st.getY(), st.getZ() + 3.5 + (i / 4) * 3, 270);
			}
		}, 10, dolly(new G(st, 20, 8, 18, 4, 0, 5), new G(st, 14, 6, 15, 4, 0, 5)), (level, t) -> {
			if (t == 1) {
				((dev.farmings.stable.StableControllerEntity) level.getBlockEntity(stable)).forceDayForTest(false);
			}
		});
		// 16. in the stalls, close
		film("stalls", 12000, LEN, null, 10, dolly(new G(st, 7, 1.5, 8.5, 3, 1.0, 4), new G(st, 6.6, 1.4, 6.5, 3, 1.0, 3)), NONE);
		// 17. the stud book
		filmBook("stud_book", 12000, level -> {
			ItemStack b = new ItemStack(FarmingsItems.STUD_BOOK);
			for (HorseEntity h : all) {
				dev.farmings.item.StudBookItem.record(b, h);
			}
			for (HorseEntity h : field) {
				dev.farmings.item.StudBookItem.record(b, h);
			}
			return b;
		}, new G(st, 9, 2.5, 6, 3, 0.5, 4));
		Random rnd = new Random(31);
		// 18. sire and mare in the paddock in the sun, an orbit
		film("sire_and_mare", 6000, LEN, (level, t) -> {
			stableDay.at(level, t);
			for (int i = 0; i < all.size(); i++) {
				place(all.get(i), st.getX() + 9.5 + (i % 4) * 5, st.getY(), st.getZ() + 2.5 + (i / 4) * 6, i * 50);
			}
			place(sire[0], st.getX() + 20.5, st.getY(), st.getZ() + 12.5, 90);
			place(mares.get(0), st.getX() + 23, st.getY(), st.getZ() + 13, 270);
		}, 10, orbit(st, 21.8, 12.8, 6.5, 5.5, 1.2, 1.0, 20, 85, 1.4), NONE);
		// 19. foals playing in the sun
		film("foals_play", 5000, LEN, (level, t) -> {
			for (int i = 0; i < foals.size(); i++) {
				place(foals.get(i), st.getX() + 15.5 + i * 3, st.getY(), st.getZ() + 15.5, 0);
			}
		}, 10, track(middle(foals), new Vec3(-2, 2.4, 9), new Vec3(-6, 1.8, 7), new Vec3(0, 0.7, 0), 0.06), (level, t) ->
				roam(foals, t, 25, rnd, st.getX() + 8, st.getZ() + 2, st.getX() + 34, st.getZ() + 22, 2.0));
		// 20. mares and foals grazing, late afternoon
		film("mares_and_foals", 10800, LEN, null, 10, dolly(new G(st, 38, 2.5, 26, 20, 1, 12), new G(st, 33, 2.2, 23, 20, 1, 12)), (level, t) ->
				roam(foals, t, 40, rnd, st.getX() + 8, st.getZ() + 2, st.getX() + 34, st.getZ() + 22, 1.4));

		// ---- the evening ride, by the river ----
		// 21. ridden west along the river into the sunset, from behind the rider
		ride("evening_ride", 12600, CameraType.THIRD_PERSON_BACK, rv, zc);
		// 22. the rider from the front
		ride("evening_ride_front", 12800, CameraType.THIRD_PERSON_FRONT, rv, zc);
		// 23. dusk: the wild herd on the river bank, a slow crane up and away
		film("river_dusk", 13000, LEN, (level, t) -> {
			for (int i = 0; i < herd.size(); i++) {
				double x = rv.getX() + 60.5 + (i % 4) * 4, z = zc.applyAsDouble(x) + 8 + (i / 4) * 4;
				place(herd.get(i), x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)), z, i * 50);
				herd.get(i).setNoAi(false);
			}
		}, 10, dolly(new G(rv, 82, 2, 34, 52, 2, 20), new G(rv, 92, 9, 42, 52, 2, 18)), NONE);
	}

	/** A ride: the player saddles a Thoroughbred, rides it west along the south bank, the camera third person. */
	private void ride(String id, int clock, CameraType view, BlockPos rv, java.util.function.DoubleUnaryOperator zc) {
		String name = next(id);
		if (!wanted(shotNo)) {
			return;
		}
		log("shot " + name);
		HorseEntity[] mount = new HorseEntity[1];
		try {
			time(clock);
			double x0 = rv.getX() + 120.5;
			double z0 = zc.applyAsDouble(x0) + 9;
			mount[0] = server.computeOnServer(srv -> {
				ServerLevel level = srv.overworld();
				HorseEntity h = horse(level, HorseBreed.ARABIAN, Sex.FEMALE, 7500, 6 * 365, 8, x0, z0, false);
				h.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new ItemStack(net.minecraft.world.item.Items.SADDLE));
				place(h, x0, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x0), (int) Math.floor(z0)), z0, 90);
				return h;
			});
			server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f 90 5", x0, mount[0].getY() + 2, z0));
			server.runCommand("gamemode creative @p");
			server.runOnServer(srv -> {
				var p = srv.overworld().players().get(0);
				p.getInventory().clearContent();
				p.startRiding(mount[0]);
			});
			context.runOnClient(client -> client.options.setCameraType(view));
			settle(40);
			still(name + "_a");
			Filming film = context.computeOnClient(client -> Filming.start(client, "mv_" + SONG + "_" + name));
			context.getInput().holdKey(o -> o.keyUp);
			try {
				for (int t = 0; t < LEN; t++) {
					int tt = t;
					context.runOnClient(client -> {
						var p = client.player;
						if (p == null) {
							return;
						}
						double ax = p.getX() - 8, az = zc.applyAsDouble(ax) + 9;
						float yaw = (float) Math.toDegrees(Math.atan2(-(ax - p.getX()), az - p.getZ()));
						p.setYRot(yaw);
						p.setXRot(view == CameraType.THIRD_PERSON_FRONT ? 8 : 12);
					});
					if (t == 60) {
						context.getInput().holdKey(o -> o.keySprint);
					}
					context.waitTick();
				}
			} finally {
				context.getInput().releaseKey(o -> o.keyUp);
				context.getInput().releaseKey(o -> o.keySprint);
				film.stop();
			}
		} catch (RuntimeException | AssertionError ex) {
			log("shot " + name + " failed: " + ex);
			ex.printStackTrace();
			failed.add(name);
		} finally {
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			server.runOnServer(srv -> {
				srv.overworld().players().get(0).stopRiding();
				if (mount[0] != null) {
					mount[0].discard();
				}
			});
			server.runCommand("gamemode spectator @p");
		}
	}
}
