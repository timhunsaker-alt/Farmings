package dev.farmings.fabric.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.TroughEntity;
import dev.farmings.corral.BaitTroughEntity;
import dev.farmings.corral.CorralPostEntity;
import dev.farmings.entity.CattleEntity;
import dev.wildcatter.multiblock.BigParts;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;

/**
 * {@code tamingFilm} (FARMINGS_ONLY=tamingFilm): the taming short, in real time on the music videos' world, in a savanna, in the
 * landscape window (2880 x 1620) for a vertical cut from its middle 9:16, the readout card at the top. A wild herd spawned there as a
 * natural group (so its breed is the biome's: Gyr in hot, dry country) grazes outside a taming corral (docs/taming.md); hay goes in the
 * bait trough and draws them through the one-way gate; fed and watered, they settle for 7 real days (the sky runs half a game day, which
 * is those 7 days at the Farmings time scale; the corral's tending is called by hook at that pace) and are tamed, the corral post firing
 * the {@code farmings:tamed} milestone. TAMING_CHECK=1 takes stills instead of filming; FARMINGS_FILMS picks films by number.
 */
final class TamingFilms {
	private static final String SEED = "farmings";
	private static final int HERD = 4;
	private final ClientGameTestContext context;
	private TestServerContext server;
	private int g;
	private int sx;
	private int sz;
	private BlockPos k;
	private final List<CattleEntity> herd = new ArrayList<>();
	private String film = "";
	private long filmStart;
	private final boolean check = System.getenv("TAMING_CHECK") != null;

	TamingFilms(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[farmings-films] " + line);
	}

	private void event(String what) {
		log(String.format(Locale.ROOT, "event %s %s at %.1f s", film, what, (System.nanoTime() - filmStart) / 1e9));
	}

	private static boolean wanted(int n) {
		String only = System.getenv("FARMINGS_FILMS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(Integer.toString(n));
	}

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(10);
			client.options.graphicsPreset().set(GraphicsPreset.FANCY);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.cloudStatus().set(CloudStatus.FANCY);
			client.options.entityDistanceScaling().set(3.0);
			client.options.fov().set(70);
			client.options.bobView().set(false);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
		});
		context.getInput().resizeWindow(1440, 810);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set 1500");
			server.runCommand("weather clear");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			BlockPos savanna = server.computeOnServer(srv -> {
				var r = srv.overworld().findClosestBiome3d(h -> h.is(ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace("savanna"))),
						new BlockPos(0, 64, 0), 6400, 32, 64);
				return r == null ? null : r.getFirst();
			});
			if (savanna == null) {
				log("no savanna within 6400 blocks: nothing filmed");
				return;
			}
			sx = savanna.getX();
			sz = savanna.getZ();
			server.runCommand("tp @p " + sx + " 160 " + sz);
			context.waitTicks(160);
			build();
			context.waitTicks(80);
			guiWidth(480);
			GeneticsFilms.FilmCard.narrow = true;
			try {
				if (wanted(1)) {
					wild();
				}
				lured(wanted(2));
				settling(wanted(3));
				if (wanted(4)) {
					tamed();
				}
			} finally {
				GeneticsFilms.FilmCard.narrow = false;
				card();
				context.runOnClient(client -> client.setScreenAndShow(null));
			}
		}
	}

	// ---- the set ----

	private BlockPos gate() {
		return k.offset(7, 0, 12);
	}

	private void build() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int ground = MusicVideo.surface(level, sx, sz);
			g = Math.max(ground, level.getSeaLevel() + 2);
			// the corral (as the music video's): 14 x 12, its one-way gate mid-south; the herd grazes south of it
			k = new BlockPos(sx - 7, g, sz - 12);
			MusicVideo.pad(level, sx - 12, sz - 16, sx + 12, sz + 14, g, 10, 8);
			for (int x = sx - 12; x <= sx + 12; x++) {
				for (int z = sz - 16; z <= sz + 14; z++) {
					BlockPos p = new BlockPos(x, g, z);
					if (!level.getBlockState(p).isAir() && level.getBlockState(p).canBeReplaced()) {
						level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
					}
				}
			}
			MusicVideo.fence(level, k, 0, 0, 14, 12);
			level.setBlockAndUpdate(gate(), dev.farmings.corral.CorralBlocks.CORRAL_GATE.defaultBlockState()
					.setValue(dev.farmings.corral.CorralGateBlock.FACING, Direction.NORTH));
			level.setBlockAndUpdate(k.offset(1, 0, 1), dev.farmings.corral.CorralBlocks.CORRAL_POST.defaultBlockState());
			level.setBlockAndUpdate(k.offset(7, 0, 3), dev.farmings.corral.CorralBlocks.BAIT_TROUGH.defaultBlockState());
			FarmingsClientGameTest.barnPart(level, BarnParts.WATER_TROUGH, k.offset(10, 0, 5));
			water(level);
			for (var m : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(k).inflate(48))) {
				m.discard();
			}
			// a wild herd as the spawn cycle makes one here: a natural group, its breed the biome's, one bull and the rest cows
			SpawnGroupData group = null;
			double[][] spots = {{-2.5, 9.0}, {1.5, 8.0}, {3.5, 10.5}, {-0.5, 11.5}};
			for (int i = 0; i < HERD; i++) {
				CattleEntity c = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.NATURAL);
				double x = gate().getX() + 0.5 + spots[i][0], z = gate().getZ() + 0.5 + spots[i][1];
				c.snapTo(x, MusicVideo.surface(level, (int) Math.floor(x), (int) Math.floor(z)), z, 150 + i * 35, 0);
				group = c.finalizeSpawn(level, level.getCurrentDifficultyAt(c.blockPosition()), EntitySpawnReason.NATURAL, group);
				level.addFreshEntity(c);
				herd.add(c);
			}
			MusicVideo.discardItems(level, k, 60);
			log("site " + sx + "," + sz + " g " + g + " biome " + level.getBiome(new BlockPos(sx, g, sz + 8)).getRegisteredName());
			for (CattleEntity c : herd) {
				log("herd " + c.herdId() + " " + c.breed().code + " " + c.sex() + " wild " + c.isWild() + " | " + c.readout());
			}
		});
	}

	private void water(ServerLevel level) {
		if (level.getBlockEntity(BigParts.owner(level, k.offset(10, 0, 5))) instanceof TroughEntity t) {
			while (t.pourBucket()) {
				// full
			}
		}
	}

	private CorralPostEntity post(ServerLevel level) {
		return (CorralPostEntity) level.getBlockEntity(k.offset(1, 0, 1));
	}

	/** Hay in the bait trough (filled to the top), the post claimed by the player: from now it draws the herd in. */
	private void bait(ServerLevel level) {
		if (level.getBlockEntity(k.offset(7, 0, 3)) instanceof BaitTroughEntity b) {
			while (b.addBlock()) {
				// full
			}
		}
		CorralPostEntity p = post(level);
		if (p.owner() == null) {
			p.claim(level.getServer().getPlayerList().getPlayers().get(0).getUUID());
		}
	}

	private int inside() {
		return server.computeOnServer(srv -> {
			CorralPostEntity p = post(srv.overworld());
			p.refresh(srv.overworld());
			int n = 0;
			for (CattleEntity c : herd) {
				n += p.inside(c) ? 1 : 0;
			}
			return n;
		});
	}

	private boolean milestone() {
		return server.computeOnServer(srv -> {
			var player = srv.getPlayerList().getPlayers().get(0);
			return dev.wildcatter.quest.QuestData.get(srv).of(player.getStringUUID()).milestones.contains(dev.farmings.quest.Milestones.TAMED.toString());
		});
	}

	/** The herd's middle, from the gate (x, z). */
	private double[] herdMiddle() {
		return server.computeOnServer(srv -> {
			double x = 0, z = 0;
			for (CattleEntity c : herd) {
				x += c.getX();
				z += c.getZ();
			}
			return new double[] {x / herd.size() - gate().getX() - 0.5, z / herd.size() - gate().getZ() - 0.5};
		});
	}

	private String breed() {
		String code = herd.get(0).breed().code;
		return code.charAt(0) + code.substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
	}

	// ---- camera, card ----

	/** The camera at (x, y, z) from the gate (eye height), looking at (tx, ty, tz). */
	private void look(double x, double y, double z, double tx, double ty, double tz) {
		look(x, y, z, tx, ty, tz, true);
	}

	/** As above; {@code snap} teleports (server and client), otherwise the client's camera alone moves, smoothly, each tick. */
	private void look(double x, double y, double z, double tx, double ty, double tz, boolean snap) {
		BlockPos o = gate();
		double dx = tx - x, dy = ty - y, dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		double px = o.getX() + 0.5 + x, py = o.getY() + y - 1.62, pz = o.getZ() + 0.5 + z;
		if (snap) {
			server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", px, py, pz, yaw, pitch));
		}
		context.runOnClient(client -> {
			var p = client.player;
			if (snap) {
				p.snapTo(px, py, pz, yaw, pitch);
			} else {
				p.setPos(px, py, pz);
				p.setYRot(yaw);
				p.setXRot(pitch);
			}
			p.setYHeadRot(yaw);
			p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		});
	}

	/** A slow move from camera a to b over {@code ticks}, eased, with a still of {@code hold} ticks at each end. */
	private void move(double[] a, double[] b, int t, int ticks, int hold) {
		double s = Math.max(0, Math.min(1, (t - hold) / (double) Math.max(1, ticks - 2 * hold)));
		s = s * s * (3 - 2 * s);
		double[] c = new double[6];
		for (int i = 0; i < 6; i++) {
			c[i] = a[i] + (b[i] - a[i]) * s;
		}
		look(c[0], c[1], c[2], c[3], c[4], c[5], false);
	}

	private void guiWidth(int units) {
		context.runOnClient(client -> {
			int scale = Math.max(1, Math.round(client.getWindow().getWidth() / (float) units));
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
		context.waitTicks(2);
	}

	private void hideCursor() {
		context.runOnClient(client -> org.lwjgl.sdl.SDLMouse.SDL_HideCursor());
	}

	private void card(String... lines) {
		GeneticsFilms.FilmCard.lines = lines;
	}

	private void showCard() {
		context.runOnClient(client -> {
			if (!(client.gui.screen() instanceof GeneticsFilms.FilmCard)) {
				client.setScreenAndShow(new GeneticsFilms.FilmCard());
			}
		});
		context.waitTicks(2);
		hideCursor();
	}

	private Filming start(String name, boolean want) {
		film = name;
		filmStart = System.nanoTime();
		if (!want) {
			return null;
		}
		if (check) {
			context.takeScreenshot(TestScreenshotOptions.of(name).withSize(2880, 1620).withDestinationDir(stills()).disableCounterPrefix());
			return null;
		}
		Filming f = context.computeOnClient(client -> Filming.start(client, name));
		hideCursor();
		filmStart = System.nanoTime();
		return f;
	}

	private void stop(Filming f, String name, boolean want) {
		if (want && check) {
			context.takeScreenshot(TestScreenshotOptions.of(name + "_end").withSize(2880, 1620).withDestinationDir(stills()).disableCounterPrefix());
		}
		if (f != null) {
			f.stop();
		}
	}

	private static Path stills() {
		return Path.of("../../scene-videos/taming-check").toAbsolutePath().normalize();
	}

	// ---- the films ----

	/** 1. The wild herd grazing in the savanna outside the corral, a slow push in. */
	private void wild() {
		double[] m = herdMiddle();
		double[] a = {m[0] - 2.0, 2.4, m[1] + 8.0, m[0], 1.0, m[1] - 1.0}, b = {m[0] - 1.2, 2.2, m[1] + 6.5, m[0], 1.0, m[1] - 1.0};
		look(a[0], a[1], a[2], a[3], a[4], a[5]);
		card("^WILD CATTLE", breed() + ", in the savanna", "Bred for hot, dry country", "!WILD: nobody's herd");
		showCard();
		context.waitTicks(40);
		String name = "taming-1-wild";
		Filming f = start(name, true);
		event("start: the wild " + breed() + " herd grazing outside the corral");
		int ticks = check ? 5 : 20 * 12;
		for (int t = 0; t <= ticks; t++) {
			move(a, b, t, ticks, 60);
			context.waitTick();
		}
		event("end");
		stop(f, name, true);
	}

	/** 2. Hay in the bait trough: the herd walks to the gate and through it, one by one. */
	private void lured(boolean want) {
		double[] a = {-5.0, 2.4, -5.0, 0.0, 0.8, 1.5}, b = {-4.4, 2.2, -5.6, 0.0, 0.8, 0.5};
		look(a[0], a[1], a[2], a[3], a[4], a[5]);
		card("^HAY IN THE BAIT TROUGH", "It draws them to the gate", "Inside: 0 of " + HERD);
		showCard();
		context.waitTicks(40);
		String name = "taming-2-lured";
		Filming f = start(name, want);
		event("start: the gate from inside, the herd beyond it");
		int all = -1;
		int last = 0;
		int max = check ? 1600 : 20 * 75;
		for (int t = 0; t <= max; t++) {
			if (t == 60) {
				server.runOnServer(srv -> bait(srv.overworld()));
				event("hay in the bait trough");
			}
			if (want) {
				move(a, b, t, Math.min(max, 20 * 30), 60);
			}
			if (t % 5 == 0) {
				int n = inside();
				if (n != last) {
					event("inside: " + n + " of " + HERD);
					last = n;
				}
				if (n == HERD && all < 0) {
					all = t;
				}
				card("^HAY IN THE BAIT TROUGH", "Through the one-way gate", (n == HERD ? "+" : "") + "Inside: " + n + " of " + HERD);
			}
			if (all >= 0 && t > all + 70) {
				break;
			}
			context.waitTick();
		}
		if (all < 0) {
			event("NOT all in: " + last + " of " + HERD);
			server.runOnServer(srv -> {
				CorralPostEntity p = post(srv.overworld());
				for (int i = 0; i < herd.size(); i++) {
					if (!p.inside(herd.get(i))) {
						herd.get(i).snapTo(k.getX() + 3.5 + i * 2, g, k.getZ() + 8.5, 0, 0);
					}
				}
			});
		}
		event("end");
		stop(f, name, want);
	}

	/**
	 * 3. Settling: half a game day of sky (7 real days at the Farmings time scale) in about 6 s; the corral feeds and waters each animal
	 * (its tend, called at that pace) and counts its settled days; at 7 they are tamed and the post fires the milestone.
	 */
	private void settling(boolean want) {
		double[] m = herdMiddle();
		double[] a = {m[0] - 6.5, 5.5, m[1] - 6.5, m[0], 0.8, m[1]}, b = {m[0] - 5.5, 5.0, m[1] - 5.5, m[0], 0.8, m[1]};
		look(a[0], a[1], a[2], a[3], a[4], a[5]);
		server.runCommand("time set 23500");
		card("^SETTLING IN", "Fed and watered daily", "Day 0.0 of 7");
		showCard();
		context.waitTicks(40);
		String name = "taming-3-settling";
		Filming f = start(name, want);
		event("start: the herd in the corral at dawn");
		int run = 140;     // ticks of sped-up time
		int perTick = (int) Math.ceil(dev.farmings.husbandry.TimeScale.realDaysToTicks(7.02) / run);
		int tamedAt = -1;
		int total = check ? 300 : 20 * 16;
		for (int t = 0; t <= total; t++) {
			if (want) {
				move(a, b, t, total, 60);
			}
			if (t >= 50 && t < 50 + run && tamedAt < 0) {
				server.runCommand("time add " + (12000 / run));
				server.runOnServer(srv -> {
					ServerLevel level = srv.overworld();
					CorralPostEntity p = post(level);
					bait(level);
					water(level);
					for (CattleEntity c : herd) {
						if (c.isWild() && p.inside(c)) {
							p.tend(level, c, c, perTick);
						}
					}
				});
			}
			double[] most = server.computeOnServer(srv -> {
				double d = 0;
				int tame = 0;
				for (CattleEntity c : herd) {
					d = Math.max(d, c.isWild() ? c.settledDays() : 7.0);
					tame += c.isWild() ? 0 : 1;
				}
				return new double[] {d, tame};
			});
			int tame = (int) most[1];
			if (tame > 0 && tamedAt < 0) {
				tamedAt = t;
				event("TAMED: " + tame + " of " + HERD + " | milestone " + milestone() + " | " + server.computeOnServer(srv -> post(srv.overworld()).readout(srv.overworld())));
			}
			if (tamedAt >= 0) {
				card("+TAMED: " + tame + " OF " + HERD, milestone() ? "+Milestone: Tamed" : "Milestone: not yet", "In your herd now");
			} else {
				card("^SETTLING IN", "Fed and watered daily", String.format(Locale.ROOT, "Day %.1f of 7", Math.min(7.0, most[0])), "(time sped up)");
			}
			if (tamedAt >= 0 && t > tamedAt + 110) {
				break;
			}
			context.waitTick();
		}
		event("end");
		stop(f, name, want);
		log("after settling: " + server.computeOnServer(srv -> post(srv.overworld()).readout(srv.overworld())));
		for (CattleEntity c : herd) {
			log("herd " + c.herdId() + " wild " + c.isWild() + " owner " + c.owner() + " | " + server.computeOnServer(srv -> c.readout()));
		}
	}

	/** 4. Close on the tamed herd at the trough: the milestone on the card. */
	private void tamed() {
		double[] m = herdMiddle();
		double[] a = {m[0] - 4.0, 1.9, m[1] - 3.5, m[0], 0.9, m[1]}, b = {m[0] - 3.2, 1.8, m[1] - 2.8, m[0], 0.9, m[1]};
		look(a[0], a[1], a[2], a[3], a[4], a[5]);
		int tame = server.computeOnServer(srv -> (int) herd.stream().filter(c -> !c.isWild()).count());
		card("+TAMED: " + tame + " OF " + HERD, milestone() ? "+Milestone: Tamed" : "!Milestone missing", "Owner: the corral's", "The barn takes them now");
		showCard();
		context.waitTicks(40);
		String name = "taming-4-tamed";
		Filming f = start(name, true);
		event("start: the tamed herd close");
		int ticks = check ? 5 : 20 * 10;
		for (int t = 0; t <= ticks; t++) {
			move(a, b, t, ticks, 60);
			context.waitTick();
		}
		event("end");
		stop(f, name, true);
	}
}
