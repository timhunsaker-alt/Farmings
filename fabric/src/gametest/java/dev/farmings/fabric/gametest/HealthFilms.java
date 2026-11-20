package dev.farmings.fabric.gametest;

import dev.farmings.FarmingsBlocks;
import dev.farmings.FarmingsEntities;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import dev.farmings.entity.CattleEntity;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.health.Health;
import dev.farmings.health.HealthBlocks;
import dev.farmings.health.HealthSounds;
import dev.farmings.health.IsolationController;
import dev.farmings.health.IsolationHold;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.husbandry.SheepBreed;
import dev.farmings.husbandry.health.Disease;
import dev.farmings.husbandry.health.Drug;
import dev.farmings.husbandry.health.HealthRecord.Stage;
import dev.farmings.parlor.ParlorControllerEntity;
import dev.farmings.parlor.ParlorParts;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * {@code healthFilms} (FARMINGS_ONLY=healthFilms): the animal health films for the trailer, in real time, on the music videos' generated
 * world (fixed seed, its plains), in a large landscape window with the HUD hidden. Each film's readout is a large text panel in the world
 * just under the action (it is read on a phone), kept narrow so a centred 9:16 crop holds it. One lame Holstein carries the story: she limps
 * past the camera head low, is examined with the vet kit, sent to the sick pen and treated (the red band goes on), and her withheld milk
 * goes to the waste can at the parlor. Then three Suffolk ewes through a footbath whose level drops, and a coughing heifer in a second
 * pen. Every event is logged as {@code [farmings-films] event <film> <what> at <s> s} from the film's start. FARMINGS_FILMS picks films
 * by number (1-6).
 */
final class HealthFilms {
	private static final String SEED = "farmings";
	private final ClientGameTestContext context;
	private TestServerContext server;
	private int g;
	private int sx;
	private int sz;
	private CattleEntity daisy;
	private CattleEntity heifer;
	private final List<SheepEntity> ewes = new ArrayList<>();
	private BlockPos penCtl;
	private BlockPos coughPen;
	private BlockPos parlorAt;
	private BlockPos canAt;
	private BlockPos bathAt;
	private String film = "";
	private long filmStart;

	HealthFilms(ClientGameTestContext context) {
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
		// a large 16:9 window that fits whole on the laptop's screen (1512 x 982 points): 2880 x 1620 pixels recorded on its Retina panel
		context.getInput().resizeWindow(1440, 810);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("time set 3500");
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
			// away from the short's dairy (160 east of the plains point)
			sx = plains.getX() - 120;
			sz = plains.getZ() + 40;
			server.runCommand("tp @p " + sx + " 160 " + sz);
			context.waitTicks(100);
			build();
			context.waitTicks(60);
			if (wanted(1)) {
				lameCow();
			}
			if (wanted(2)) {
				exam();
			}
			if (wanted(3)) {
				sickPen();
			}
			if (wanted(4)) {
				parlor();
			}
			if (wanted(5)) {
				footbath();
			}
			if (wanted(6)) {
				cough();
			}
			server.runOnServer(srv -> dev.farmings.health.FarmingsRules.set(srv, dev.farmings.health.FarmingsRules.DISEASE, false));
		}
	}

	// ---- the set ----

	/** Layout from the site corner (sx, sz): the walk at z+4 along x, the sick pen at x+20, the parlor at z+30, the footbath at x+40 z+30,
	 * the coughing heifer's pen at x+46 z+4: far enough apart that no film shows another's animals. */
	private void build() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			dev.farmings.health.FarmingsRules.set(srv, dev.farmings.health.FarmingsRules.DISEASE, true);
			int ground = MusicVideo.surface(level, sx + 25, sz + 20);
			g = Math.max(ground, level.getSeaLevel() + 2);
			MusicVideo.pad(level, sx - 6, sz - 6, sx + 56, sz + 42, g, 14, 12);
			MusicVideo.meadow(level, sx - 30, sz - 30, sx + 86, sz + 72, 77, 0.25, 0.01);
			for (int x = sx - 6; x <= sx + 56; x++) {
				for (int z = sz - 6; z <= sz + 42; z++) {
					// short grass only on the set itself: the animals' feet stay in view
					BlockPos p = new BlockPos(x, g, z);
					if (!level.getBlockState(p).isAir() && level.getBlockState(p).canBeReplaced()) {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			long now = level.getGameTime();
			// Daisy: a Holstein 30 days in milk, foot rot (lame) since this morning
			daisy = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
			daisy.initFounder(CattleBreed.HOLSTEIN, Sex.FEMALE, 9301L, 4 * 365, 650, now);
			daisy.setCalvedForDisplay(30);
			daisy.snapTo(sx + 0.5, g, sz + 4.5, -90, 0);
			level.addFreshEntity(daisy);
			daisy.health().setLastTick(now);
			daisy.health().infect(Disease.FOOT_ROT, Stage.SICK, now, now + 10_000_000L);
			daisy.settle(level);
			// the sick pen (an isolation pen switched to HOSPITAL), 20 east
			penCtl = new BlockPos(sx + 20, g, sz + 6);
			pen(level, penCtl, IsolationController.Mode.HOSPITAL);
			// the coughing heifer's pen, far to the east
			coughPen = new BlockPos(sx + 46, g, sz + 6);
			pen(level, coughPen, IsolationController.Mode.HOSPITAL);
			heifer = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
			heifer.initFounder(CattleBreed.JERSEY, Sex.FEMALE, 9302L, 400, 280, now);
			heifer.snapTo(coughPen.getX() + 2.5, g + 0.2, coughPen.getZ() + 0.5, 180, 0);
			level.addFreshEntity(heifer);
			heifer.health().setLastTick(now);
			heifer.health().infect(Disease.BRD, Stage.SICK, now, now + 10_000_000L);
			heifer.health().setPen(coughPen.asLong());
			// the parlor: 3 stalls, a holding pen, a stub barn on its lane (as the parlor game tests build it), the waste can beside it
			parlorAt = new BlockPos(sx + 10, g + 1, sz + 30);
			buildParlor(level, parlorAt, 3);
			canAt = parlorAt.offset(1, -1, 2);
			level.setBlockAndUpdate(canAt, HealthBlocks.WASTE_MILK_CAN.defaultBlockState());
			var ctl = (ParlorControllerEntity) level.getBlockEntity(parlorAt);
			ctl.refresh(level);
			ctl.pump(level).forceRunning(true);
			ctl.tank(level).forceChilling(true);
			log("parlor: " + ctl.readout(level));
			// the footbath lane: fenced both sides, one bath in its middle, part used (89 of 100 L), charged
			int lz = sz + 30;
			for (int x = sx + 34; x <= sx + 46; x++) {
				level.setBlockAndUpdate(new BlockPos(x, g, lz - 1), Blocks.OAK_FENCE.defaultBlockState());
				level.setBlockAndUpdate(new BlockPos(x, g, lz + 1), Blocks.OAK_FENCE.defaultBlockState());
			}
			bathAt = new BlockPos(sx + 40, g, lz);
			level.setBlockAndUpdate(bathAt, HealthBlocks.FOOTBATH.defaultBlockState());
			var bath = (HealthBlocks.FootbathEntity) level.getBlockEntity(bathAt);
			bath.addWater(89);
			bath.charge(100);
			for (int i = 0; i < 3; i++) {
				SheepEntity s = FarmingsEntities.SHEEP.create(level, EntitySpawnReason.COMMAND);
				java.util.Map<String, Integer> comp = java.util.Map.of(SheepBreed.SUFFOLK.code, 10_000);
				s.initFounder(SheepBreed.SUFFOLK, Sex.FEMALE, 9310L + i, 700, dev.farmings.husbandry.Flock.targetKg(comp, 700), now);
				s.snapTo(sx + 35.5 - i * 1.4, g, lz + 0.5, -90, 0);
				s.setNoAi(true);
				level.addFreshEntity(s);
				s.health().setLastTick(now);
				s.health().infect(Disease.FOOT_ROT, Stage.SICK, now, now + 10_000_000L);
				s.settle(level);
				ewes.add(s);
			}
			MusicVideo.discardItems(level, new BlockPos(sx + 25, g, sz + 20), 80);
			log("site " + sx + "," + sz + " g " + g);
		});
	}

	private void pen(ServerLevel level, BlockPos c, IsolationController.Mode mode) {
		int y = c.getY();
		for (int x = c.getX() + 1; x <= c.getX() + 4; x++) {
			for (int z = c.getZ() - 1; z <= c.getZ() + 2; z++) {
				level.setBlockAndUpdate(new BlockPos(x, y, z), HealthBlocks.ISOLATION_PEN.defaultBlockState());
			}
		}
		level.setBlockAndUpdate(c, HealthBlocks.ISOLATION_CONTROLLER.defaultBlockState());
		FarmingsClientGameTest.barnPart(level, BarnParts.HAY_FEEDER, new BlockPos(c.getX() + 2, y, c.getZ() - 2));
		FarmingsClientGameTest.barnPart(level, BarnParts.WATER_TROUGH, new BlockPos(c.getX() + 5, y, c.getZ()));
		((FeederEntity) BigParts.entity(level, new BlockPos(c.getX() + 2, y, c.getZ() - 2))).addBlock();
		((TroughEntity) BigParts.entity(level, new BlockPos(c.getX() + 5, y, c.getZ()))).pourBucket();
		for (int x = c.getX(); x <= c.getX() + 5; x++) {
			level.setBlockAndUpdate(new BlockPos(x, y, c.getZ() + 3), Blocks.OAK_FENCE.defaultBlockState());
		}
		var ctl = (IsolationController) level.getBlockEntity(c);
		ctl.setMode(mode);
		ctl.refresh(level);
	}

	/** The parlor game tests' parlor: its parts, a 2-deep holding pen, a lane west to a stub barn whose cows it milks. */
	private static void buildParlor(ServerLevel level, BlockPos c, int stalls) {
		int fl = c.getY() - 1;
		for (int x = c.getX() - stalls - 12; x <= c.getX() + 4; x++) {
			for (int z = c.getZ() - 3; z <= c.getZ() + 6; z++) {
				level.setBlock(new BlockPos(x, fl - 1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		level.setBlock(c, FarmingsBlocks.PARLOR_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
		FarmingsClientGameTest.parlorPart(level, ParlorParts.BULK_TANK, new BlockPos(c.getX() + 1, fl, c.getZ()));
		int xp = c.getX() - stalls - 1;
		FarmingsClientGameTest.parlorPart(level, ParlorParts.VACUUM_PUMP, new BlockPos(xp, fl, c.getZ()));
		for (int x = xp + 1; x <= c.getX(); x++) {
			FarmingsClientGameTest.parlorPart(level, ParlorParts.MILK_LINE, new BlockPos(x, fl, c.getZ()));
		}
		for (int x = xp + 1; x < c.getX(); x++) {
			FarmingsClientGameTest.parlorPart(level, ParlorParts.MILKING_UNIT, new BlockPos(x, c.getY(), c.getZ()));
			FarmingsClientGameTest.parlorPart(level, ParlorParts.PARLOR_STALL, new BlockPos(x, fl, c.getZ() + 1));
			FarmingsClientGameTest.parlorPart(level, ParlorParts.HOLDING_PEN, new BlockPos(x, fl, c.getZ() + 3));
			FarmingsClientGameTest.parlorPart(level, ParlorParts.HOLDING_PEN, new BlockPos(x, fl, c.getZ() + 4));
		}
		for (int x = xp - 3; x <= xp; x++) {
			FarmingsClientGameTest.parlorPart(level, ParlorParts.COW_LANE, new BlockPos(x, fl, c.getZ() + 3));
		}
		for (int x = xp - 5; x <= xp - 4; x++) {
			FarmingsClientGameTest.barnPart(level, BarnParts.ALLEY_FLOOR, new BlockPos(x, fl, c.getZ() + 3));
		}
		level.setBlock(stub(c, stalls), FarmingsBlocks.BARN_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
	}

	private static BlockPos stub(BlockPos c, int stalls) {
		return new BlockPos(c.getX() - stalls - 7, c.getY(), c.getZ() + 3);
	}

	// ---- camera and readout ----

	/** The camera: spectator at x, y, z looking yaw, pitch; then the readout panel placed `ahead` blocks in front, `down` below the line of sight. */
	private void cam(double x, double y, double z, float yaw, float pitch, double ahead, double down, double scale) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		double ry = Math.toRadians(yaw);
		double rp = Math.toRadians(pitch);
		double fx = -Math.sin(ry) * Math.cos(rp);
		double fy = -Math.sin(rp);
		double fz = Math.cos(ry) * Math.cos(rp);
		double eye = y + 1.62;
		double px = x + fx * ahead;
		double py = eye + fy * ahead - down;
		double pz = z + fz * ahead;
		server.runCommand("kill @e[type=text_display,tag=readout]");
		server.runCommand(String.format(Locale.ROOT, "summon text_display %.2f %.2f %.2f {Tags:[\"readout\"],billboard:\"center\",line_width:150,"
				+ "background:-1440603614,brightness:{sky:15,block:15},alignment:\"left\",text:{text:\"\"},"
				+ "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[%.2ff,%.2ff,%.2ff]}}",
				px, py, pz, scale, scale, scale));
	}

	/** The panel's text: a yellow title, then the lines. */
	private void readout(String title, String... lines) {
		StringBuilder b = new StringBuilder();
		for (String l : lines) {
			b.append("\\n").append(l.replace("\"", "'").replace("\\", "/"));
		}
		server.runCommand("data merge entity @e[type=text_display,tag=readout,limit=1] {text:{text:\"" + title.replace("\"", "'")
				+ "\",color:\"yellow\",bold:true,extra:[{text:\"" + b + "\",color:\"white\",bold:false}]}}");
	}

	private void clearReadout() {
		server.runCommand("kill @e[type=text_display,tag=readout]");
	}

	private Filming start(String name) {
		film = name;
		Filming f = context.computeOnClient(client -> Filming.start(client, name));
		filmStart = System.nanoTime();
		return f;
	}

	private static void walk(Mob m, double x, double z, double speed) {
		m.getNavigation().moveTo(x, m.getY(), z, speed);
	}

	/** What follows "LOOKS SICK: " in an animal's readout, up to the next part. */
	private static String signs(String readout) {
		int i = readout.indexOf("LOOKS SICK: ");
		if (i < 0) {
			return "";
		}
		String s = readout.substring(i + 12);
		int j = s.indexOf(", ");
		return j < 0 ? s : s.substring(0, j);
	}

	// ---- the films ----

	/** 1. Daisy limps toward the camera, head low, along the walk; close at the end. */
	private void lameCow() {
		int z = sz + 4;
		server.runOnServer(srv -> daisy.snapTo(sx + 0.5, g, z + 0.5, -90, 0));
		// low, at her head's height, ahead of her and a little to the side: she walks into a close three-quarter view
		cam(sx + 13.5, g - 0.3, z + 3.2, 120, 6, 2.4, 0.95, 0.42);
		context.waitTicks(40);
		String sign = server.computeOnServer(srv -> signs(daisy.readout()));
		readout("LAME", "foot rot: " + sign, "head low, walks 30% slower");
		context.waitTicks(10);
		Filming f = start("health-1-lame-cow");
		event("start: Daisy far, walking toward the camera");
		for (int t = 0; t < 20 * 12; t++) {
			if (t % 20 == 0) {
				server.runOnServer(srv -> walk(daisy, sx + 12.5, z + 0.5, 1.0));
			}
			if (t == 20 * 6) {
				event("Daisy mid-frame, limp and head low clear");
			}
			context.waitTick();
		}
		event("Daisy close, end");
		f.stop();
		double end = server.computeOnServer(srv -> daisy.getX());
		log("lame cow ended at x " + (end - sx));
	}

	/** 2. The vet kit exam: close on her head and shoulder, the exam's lines large. */
	private void exam() {
		server.runOnServer(srv -> {
			daisy.getNavigation().stop();
			daisy.snapTo(sx + 12.5, g, sz + 4.5, -90, 0);
			daisy.setYHeadRot(-90);
			daisy.setNoAi(true);
		});
		cam(sx + 15.6, g - 0.1, sz + 6.6, 125, 10, 2.3, 0.9, 0.40);
		context.waitTicks(30);
		readout("VET KIT", "...");
		Filming f = start("health-2-exam");
		event("start: Daisy standing, head low");
		context.waitTicks(30);
		String exam = server.computeOnServer(srv -> {
			srv.overworld().playSound(null, daisy.blockPosition(), HealthSounds.VET_KIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
			return Health.exam(srv.overworld(), daisy);
		});
		log("exam: " + exam);
		String[] parts = exam.split("; ");
		List<String> lines = new ArrayList<>();
		String head = parts[0].replaceFirst("^Exam [^:]*: ", "");
		lines.add("Temperature " + head.replace(" C ", " °C ").replace("(normal", "(normal").replace(" C)", " °C)"));
		for (int i = 1; i < parts.length; i++) {
			lines.add(parts[i]);
		}
		readout("VET KIT EXAM", lines.toArray(new String[0]));
		event("exam readout up: temperature and diagnosis");
		context.waitTicks(20 * 8);
		event("end");
		f.stop();
	}

	/** 3. To the sick pen (she limps there herself), then the injection: the red band goes on her hind legs. */
	private void sickPen() {
		server.runOnServer(srv -> {
			daisy.setNoAi(false);
			daisy.snapTo(sx + 14.5, g, sz + 4.5, -90, 0);
		});
		cam(penCtl.getX() + 7.2, g + 0.6, penCtl.getZ() + 6.2, 135, 14, 2.4, 0.95, 0.40);
		context.waitTicks(30);
		readout("SICK PEN", "sneak + vet kit: isolate her");
		Filming f = start("health-3-sick-pen");
		event("start: Daisy outside the sick pen");
		context.waitTicks(20);
		String msg = server.computeOnServer(srv -> IsolationHold.sendToHospital(srv.overworld(), daisy));
		log("isolate: " + msg);
		readout("SICK PEN", msg.replaceFirst(" at .*$", ""), "she walks in on her own");
		event("sent to the sick pen; she limps in");
		boolean in = false;
		for (int t = 0; t < 20 * 14 && !in; t++) {
			context.waitTick();
			if (t % 10 == 0) {
				in = server.computeOnServer(srv -> {
					BlockPos p = daisy.blockPosition();
					return p.getX() > penCtl.getX() && p.getX() <= penCtl.getX() + 4 && p.getZ() >= penCtl.getZ() - 1 && p.getZ() <= penCtl.getZ() + 2;
				});
			}
		}
		event(in ? "Daisy in the pen" : "Daisy NOT in the pen after 14 s");
		context.waitTicks(30);
		String treat = server.computeOnServer(srv -> {
			srv.overworld().playSound(null, daisy.blockPosition(), HealthSounds.VET_KIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
			return Health.treat(srv.overworld(), daisy, Drug.INJECTION);
		});
		log("treat: " + treat);
		readout("TREATED", "long-acting antibiotic", "RED LEG BAND: withdrawal", "milk 10 real days, meat 28");
		event("injection given; red leg bands appear within 1 s");
		context.waitTicks(20 * 7);
		event("end");
		f.stop();
	}

	/** 4. At the parlor: she is milked, and her withheld milk goes to the waste can, not the tank. */
	private void parlor() {
		BlockPos near = parlorAt.offset(-2, -1, 3);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			if (!Health.readout(daisy).contains("RED LEG BAND")) {
				Health.treat(level, daisy, Drug.INJECTION);     // when filmed alone
			}
			daisy.health().setPen(Long.MIN_VALUE);
			daisy.setNoAi(false);
			daisy.setCalvedForDisplay(30);
			daisy.assignStall(stub(parlorAt, 3), near, null);
			daisy.snapTo(near.getX() + 0.5, near.getY() + 1, near.getZ() + 0.5, 0, 0);
			daisy.settle(level);
		});
		// close on the stalls and the can, from the open side of the parlor
		cam(parlorAt.getX() + 2.6, g + 0.9, parlorAt.getZ() - 3.6, 30, 18, 2.4, 0.95, 0.38);
		context.waitTicks(30);
		readout("PARLOR", parlorLine(), "");
		Filming f = start("health-4-parlor");
		event("start: Daisy in the holding pen, red bands on");
		double canBefore = server.computeOnServer(srv -> can(srv.overworld()).litres());
		boolean done = false;
		for (int t = 0; t < 20 * 20; t++) {
			context.waitTick();
			if (t % 20 == 0) {
				double[] st = server.computeOnServer(srv -> new double[] {can(srv.overworld()).litres(), ((ParlorControllerEntity) srv.overworld().getBlockEntity(parlorAt)).tank(srv.overworld()).tank().litres(), daisy.milkKg()});
				readout("PARLOR", daisyId() + ": WITHHELD (red band)", String.format(Locale.ROOT, "waste can %.0f L", st[0]),
						String.format(Locale.ROOT, "bulk tank %.0f L", st[1]));
				if (!done && st[0] > canBefore) {
					done = true;
					event(String.format(Locale.ROOT, "milked: %.0f L into the waste can, tank stays %.0f L", st[0] - canBefore, st[1]));
				}
				if (done && t > 20 * 14) {
					break;
				}
			}
		}
		if (!done) {
			event("NOT milked in 20 s");
		}
		context.waitTicks(40);
		event("end");
		f.stop();
		log("parlor: " + server.computeOnServer(srv -> ((ParlorControllerEntity) srv.overworld().getBlockEntity(parlorAt)).readout(srv.overworld())));
	}

	private String daisyId() {
		return server.computeOnServer(srv -> daisy.herdId());
	}

	private HealthBlocks.WasteCanEntity can(ServerLevel level) {
		return (HealthBlocks.WasteCanEntity) level.getBlockEntity(canAt);
	}

	private String parlorLine() {
		return "withheld milk: to the waste can";
	}

	/** 5. Three ewes walk the lane, each held in the bath two seconds; the bath's litres and drawn level fall. */
	private void footbath() {
		int lz = bathAt.getZ();
		cam(bathAt.getX() + 0.5, g + 1.0, lz + 4.3, 180, 22, 2.3, 0.95, 0.40);
		context.waitTicks(30);
		readout("FOOTBATH", bathText());
		server.runOnServer(srv -> {
			for (SheepEntity s : ewes) {
				s.setNoAi(false);
			}
		});
		Filming f = start("health-5-footbath");
		event("start: three Suffolk ewes (lame) at the lane's west end");
		int[] phase = new int[3];        // 0 waiting, 1 to the bath, 2 standing in it, 3 on out
		int[] since = new int[3];
		int lastPasses = server.computeOnServer(srv -> bath(srv.overworld()).passes());
		for (int t = 0; t < 20 * 30; t++) {
			context.waitTick();
			int tt = t;
			for (int i = 0; i < 3; i++) {
				int k = i;
				if (phase[k] == 0 && tt >= k * 20 * 5) {
					phase[k] = 1;
					since[k] = tt;
				}
				if (tt % 10 == 0 && phase[k] > 0) {
					boolean at = server.computeOnServer(srv -> {
						SheepEntity s = ewes.get(k);
						if (phase[k] == 1) {
							walk(s, bathAt.getX() + 0.5, lz + 0.5, 1.0);
							return s.blockPosition().getX() == bathAt.getX();
						}
						if (phase[k] == 2) {
							s.getNavigation().stop();
							return false;
						}
						walk(s, bathAt.getX() + 6.5, lz + 0.5, 1.0);
						return false;
					});
					if (phase[k] == 1 && at) {
						phase[k] = 2;
						since[k] = tt;
						event("ewe " + (k + 1) + " in the bath");
					} else if (phase[k] == 2 && tt - since[k] >= 40) {
						phase[k] = 3;
					}
				}
			}
			if (tt % 10 == 0) {
				int passes = server.computeOnServer(srv -> bath(srv.overworld()).passes());
				readout("FOOTBATH", bathText());
				if (passes != lastPasses) {
					event("dose: " + server.computeOnServer(srv -> String.format(Locale.ROOT, "%.0f L left, level %d/8", bath(srv.overworld()).litres(),
							srv.overworld().getBlockState(bathAt).getValue(HealthBlocks.FootbathBlock.LEVEL))));
					lastPasses = passes;
				}
			}
			if (phase[2] == 3 && tt - since[2] > 60) {
				break;
			}
		}
		event("end");
		f.stop();
	}

	private HealthBlocks.FootbathEntity bath(ServerLevel level) {
		return (HealthBlocks.FootbathEntity) level.getBlockEntity(bathAt);
	}

	private String[] bathText() {
		return server.computeOnServer(srv -> {
			var b = bath(srv.overworld());
			long treated = ewes.stream().filter(s -> s.health().caseOf(Disease.FOOT_ROT) != null && s.health().caseOf(Disease.FOOT_ROT).treated).count();
			return new String[] {"10% zinc sulfate", String.format(Locale.ROOT, "level %.0f of 100 L", b.litres()), "ewes treated " + treated + " of 3"};
		});
	}

	/** 6. The coughing heifer in her pen: head low; each cough's second is logged (the recording has no sound). */
	private void cough() {
		cam(coughPen.getX() + 2.6, g + 0.4, coughPen.getZ() + 5.4, 175, 12, 2.3, 0.95, 0.40);
		context.waitTicks(30);
		String sign = server.computeOnServer(srv -> signs(heifer.readout()));
		readout("SICK PEN", "BRD: " + sign, "cough every 15 s or so");
		Filming f = start("health-6-cough");
		event("start: Jersey heifer with BRD, head low");
		for (int t = 0; t < 20 * 32; t++) {
			context.waitTick();
			boolean c = server.computeOnServer(srv -> Math.floorMod(srv.overworld().getGameTime() + heifer.getId() * 7L, 300L) == 0);
			if (c) {
				event("cough (sound plays within 1 s: entity.cattle.cough)");
			}
		}
		event("end");
		f.stop();
		clearReadout();
	}
}
