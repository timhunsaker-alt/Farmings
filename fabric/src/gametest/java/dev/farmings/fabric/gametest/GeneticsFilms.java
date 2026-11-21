package dev.farmings.fabric.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.CattleEntity;
import dev.farmings.genetics.BreedingSeed;
import dev.farmings.genetics.Ruleset;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.VisualGenome;
import dev.farmings.genetics.VisualLocus;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.item.HerdBookItem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * {@code genetics} (FARMINGS_ONLY=genetics): the coat genetics short, in real time on the music videos' world (fixed seed, its plains),
 * in the landscape window (2880 x 1620) for a vertical cut from its middle 9:16, the readout card at the top. Two black-and-white
 * Holsteins that both carry red (MC1R ED/e: Red and White Holsteins come from such pairs) are read in the herd book, bred, and calve
 * three times; the scene picks the calving moments so the first two calves are black and the third red (each calving's coat is the
 * game's own draw from both parents' genes, predicted before it happens). The gestation and the years between are skipped (the sky runs
 * fast; the cut says so). Every event is logged as {@code [farmings-films] event <film> <what> at <s> s}. GENETICS_CHECK=1 takes stills
 * of every camera instead of filming; FARMINGS_FILMS picks films by number.
 */
final class GeneticsFilms {
	private static final String SEED = "farmings";
	private final ClientGameTestContext context;
	private TestServerContext server;
	private int g;
	private int sx;
	private int sz;
	private CattleEntity bull;
	private CattleEntity cow;
	private final List<CattleEntity> calves = new ArrayList<>();
	private String film = "";
	private long filmStart;
	private final boolean check = System.getenv("GENETICS_CHECK") != null;

	GeneticsFilms(ClientGameTestContext context) {
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
			// away from the dairy short (160 east) and the health films (120 west, 40 south)
			sx = plains.getX() - 60;
			sz = plains.getZ() - 90;
			server.runCommand("tp @p " + sx + " 160 " + sz);
			context.waitTicks(100);
			build();
			context.waitTicks(60);
			guiWidth(480);
			FilmCard.narrow = true;
			try {
				if (wanted(1)) {
					pair();
				}
				if (wanted(2)) {
					parent(bull, "BULL", "He", 2);
				}
				if (wanted(3)) {
					parent(cow, "COW", "She", 3);
				}
				if (wanted(4)) {
					breed();
				}
				if (wanted(5) || wanted(6) || wanted(7)) {
					calving(5, false);
					calving(6, false);
					calving(7, true);
				}
				if (wanted(8)) {
					why();
				}
				if (wanted(9)) {
					family();
				}
			} finally {
				FilmCard.narrow = false;
				card();
				context.runOnClient(client -> client.setScreenAndShow(null));
			}
		}
	}

	// ---- the set ----

	private BlockPos o() {
		return new BlockPos(sx, g, sz);
	}

	private void build() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int ground = MusicVideo.surface(level, sx, sz);
			g = Math.max(ground, level.getSeaLevel() + 2);
			MusicVideo.pad(level, sx - 14, sz - 12, sx + 14, sz + 12, g, 14, 12);
			MusicVideo.meadow(level, sx - 40, sz - 40, sx + 40, sz + 40, 91, 0.30, 0.04);
			for (int x = sx - 10; x <= sx + 10; x++) {
				for (int z = sz - 8; z <= sz + 8; z++) {
					// short grass only: hooves and newborn calves stay in view
					BlockPos p = new BlockPos(x, g, z);
					if (!level.getBlockState(p).isAir() && level.getBlockState(p).canBeReplaced()) {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			MusicVideo.fence(level, o(), -11, -9, 11, 9);
			long now = level.getGameTime();
			// two black-and-white Holsteins, both MC1R ED/e (black, carrying red); he mostly black, she mostly white
			bull = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
			bull.initFounder(CattleBreed.HOLSTEIN, Sex.MALE, 7701L, 5 * 365, 1000, now);
			bull.setVisualGenome(with(bull.visualGenome(), new int[] {0, 3}, new int[] {460, 480}));
			cow = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
			cow.initFounder(CattleBreed.HOLSTEIN, Sex.FEMALE, 7702L, 4 * 365, 650, now);
			cow.setVisualGenome(with(cow.visualGenome(), new int[] {0, 3}, new int[] {860, 900}));
			for (CattleEntity c : new CattleEntity[] {bull, cow}) {
				c.snapTo(sx + 0.5, g, sz + 0.5, 0, 0);
				level.addFreshEntity(c);
				c.setNoAi(true);
				c.settle(level);
			}
			MusicVideo.discardItems(level, o(), 60);
			log("site " + sx + "," + sz + " g " + g);
			log("bull " + bull.herdId() + " " + bull.look().describe() + " " + bull.visualGenome());
			log("cow " + cow.herdId() + " " + cow.look().describe() + " " + cow.visualGenome());
		});
	}

	/** The genes with MC1R and white coverage set (both within the Holstein founder alleles, docs/GENETICS.md). */
	private static VisualGenome with(VisualGenome v, int[] extension, int[] white) {
		EnumMap<VisualLocus, int[]> m = new EnumMap<>(VisualLocus.class);
		for (VisualLocus l : VisualLocus.values()) {
			m.put(l, v.pair(l));
		}
		m.put(VisualLocus.EXTENSION, extension);
		m.put(VisualLocus.WHITE, white);
		return new VisualGenome(m);
	}

	private void place(CattleEntity c, double x, double z, float yaw) {
		server.runOnServer(srv -> {
			c.setNoAi(true);
			c.getNavigation().stop();
			c.snapTo(sx + x, g, sz + z, yaw, 0);
			c.setYBodyRot(yaw);
			c.setYHeadRot(yaw);
		});
	}

	private void hearts(CattleEntity c) {
		server.runOnServer(srv -> srv.overworld().sendParticles(ParticleTypes.HEART, c.getX(), c.getY() + c.getBbHeight() + 0.3, c.getZ(), 2, 0.3, 0.2, 0.3, 0.0));
	}

	// ---- camera, card, book ----

	/** The camera at (x, y, z) from the site corner (feet; the eye is 1.62 higher) looking at (tx, ty, tz). */
	private void look(double x, double y, double z, double tx, double ty, double tz) {
		double dx = tx - x;
		double dy = ty - (y + 1.62);
		double dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", sx + x, g + y, sz + z, yaw, pitch));
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
		FilmCard.lines = lines;
	}

	private void showCard() {
		context.runOnClient(client -> {
			if (!(client.gui.screen() instanceof FilmCard)) {
				client.setScreenAndShow(new FilmCard());
			}
		});
		context.waitTicks(2);
		hideCursor();
	}

	/** The herd book open on this animal, large enough for a phone (GUI scale 4: the book 768 px of the frame's middle 911). */
	private void openBook(CattleEntity c) {
		ItemStack b = server.computeOnServer(srv -> {
			ItemStack s = new ItemStack(FarmingsItems.HERD_BOOK);
			HerdBookItem.record(s, c);
			return s;
		});
		context.runOnClient(client -> {
			client.options.guiScale().set(4);
			client.resizeGui();
			client.setScreenAndShow(new BookViewScreen(BookViewScreen.BookAccess.fromItem(b)));
		});
		context.waitTicks(2);
		hideCursor();
	}

	private void closeBook() {
		context.runOnClient(client -> client.setScreenAndShow(null));
		guiWidth(480);
	}

	private void pageDown() {
		context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN);
		hideCursor();
	}

	private Filming start(String name) {
		film = name;
		if (check) {
			context.takeScreenshot(TestScreenshotOptions.of(name).withSize(2880, 1620).withDestinationDir(stills()).disableCounterPrefix());
			filmStart = System.nanoTime();
			return null;
		}
		Filming f = context.computeOnClient(client -> Filming.start(client, name));
		hideCursor();
		filmStart = System.nanoTime();
		return f;
	}

	private void stop(Filming f) {
		if (f != null) {
			f.stop();
		}
	}

	private void still(String name) {
		if (check) {
			context.takeScreenshot(TestScreenshotOptions.of(name).withSize(2880, 1620).withDestinationDir(stills()).disableCounterPrefix());
		}
	}

	private static Path stills() {
		return Path.of("../../scene-videos/genetics-check").toAbsolutePath().normalize();
	}

	/** Ticks of film; in check mode a still at the start and a few ticks only. */
	private void hold(int ticks) {
		context.waitTicks(check ? Math.min(ticks, 5) : ticks);
	}

	private static String mc1r(CattleEntity c) {
		int[] p = c.visualGenome().pair(VisualLocus.EXTENSION).clone();
		java.util.Arrays.sort(p);     // the dominant allele first: ED / e
		return VisualLocus.EXTENSION.label(p[0]) + " / " + VisualLocus.EXTENSION.label(p[1]);
	}

	private static boolean red(VisualGenome v) {
		return v.expressed(VisualLocus.EXTENSION) == 3;
	}

	// ---- the films ----

	/** 1. The pair in the paddock: two black-and-white Holsteins, he mostly black, she mostly white. */
	private void pair() {
		place(bull, -0.5, -1.0, -35);
		place(cow, 0.7, 0.8, 35);
		look(0.1, 0.4, 4.6, 0.1, 0.8, 0.0);
		card();
		showCard();
		context.waitTicks(40);
		Filming f = start("genetics-1-pair");
		event("start: the bull (behind, mostly black) and the cow (front, mostly white), both black and white");
		hold(20 * 9);
		event("end");
		stop(f);
	}

	/** 2, 3. One parent close; the herd book opens on its page, turns to its genes; then the genes on the card. */
	private void parent(CattleEntity c, String who, String pronoun, int n) {
		boolean isBull = c == bull;
		place(bull, -0.6, -1.2, -70);
		place(cow, 0.9, 1.0, -110);
		if (isBull) {
			look(-1.4, 0.6, -5.0, -0.6, 0.8, -1.2);
		} else {
			look(1.4, 0.4, 4.6, 0.9, 0.8, 1.0);
		}
		card();
		showCard();
		context.waitTicks(30);
		String name = "genetics-" + n + "-" + who.toLowerCase(Locale.ROOT);
		Filming f = start(name);
		event("start: the " + who.toLowerCase(Locale.ROOT) + " close");
		hold(25);
		openBook(c);
		event("herd book open on his/her first page (Coat ...)");
		still(name + "_book1");
		hold(40);
		pageDown();
		event("page turned: the genes page (Color (MC1R) ED/e, shows black, carries red)");
		hold(5);
		still(name + "_book2");
		hold(85);
		closeBook();
		String g1 = server.computeOnServer(srv -> mc1r(c));
		card("^" + who + "  ·  black and white", "Color gene (MC1R): " + g1, "ED = black (dominant)", "e = red (hidden)", "+" + pronoun + " looks black, carries red");
		showCard();
		event("card up: MC1R " + g1);
		still(name + "_card");
		hold(20 * 5);
		event("end");
		card();
		stop(f);
		log(who + " genes page: " + server.computeOnServer(srv -> HerdBookItem.genesPage(c)).replace('\n', '|'));
	}

	/** 4. She comes into heat (hearts); the bull walks to her and serves her: in calf. */
	private void breed() {
		place(cow, 0.8, 0.4, 60);
		place(bull, -4.2, 0.4, -90);
		server.runOnServer(srv -> cow.startHeatForDisplay());
		look(-0.9, 0.7, 6.6, -0.9, 0.8, 0.4);
		card("^COW IN HEAT");
		showCard();
		context.waitTicks(30);
		Filming f = start("genetics-4-breed");
		event("start: the cow in heat (hearts), the bull to the left");
		hearts(cow);
		server.runOnServer(srv -> bull.setNoAi(false));
		boolean bred = false;
		int after = -1;
		for (int t = 0; t < 20 * 16; t++) {
			context.waitTick();
			if (t % 60 == 30 && !bred) {
				hearts(cow);
			}
			if (t % 20 == 0 && !bred) {
				int tt = t;
				server.runOnServer(srv -> {
					boolean path = bull.getNavigation().moveTo(cow.getX() - 2.0, cow.getY(), cow.getZ() + 0.4, 0.8);
					if (!path) {
						bull.getMoveControl().setWantedPosition(cow.getX() - 2.0, cow.getY(), cow.getZ() + 0.4, 0.8);
					}
					log(String.format(Locale.ROOT, "breed t %d bull x %.2f z %.2f path %b noAi %b dist %.2f", tt, bull.getX() - sx, bull.getZ() - sz, path, bull.isNoAi(), bull.distanceTo(cow)));
				});
			}
			if (!bred && t % 5 == 0) {
				bred = server.computeOnServer(srv -> {
					if (bull.distanceTo(cow) > 3.8) {
						return false;
					}
					bull.getNavigation().stop();
					bull.setNoAi(true);
					return cow.conceive(bull.genome(), bull.visualGenome(), srv.overworld().getGameTime());
				});
				if (bred) {
					after = t;
					String due = server.computeOnServer(srv -> cow.readout().replaceAll(".*in calf, due in ([0-9.]+) game days.*", "$1"));
					card("+IN CALF", "Due in " + due + " game days", "(" + CattleBreed.HOLSTEIN.gestationDays + " real days)");
					event("served: in calf, due in " + due + " game days");
				}
			}
			if (check && t > 5) {
				still("genetics-4-breed_mid");
				server.runOnServer(srv -> {
					bull.setNoAi(true);
					if (!cow.isInCalf()) {
						cow.conceive(bull.genome(), bull.visualGenome(), srv.overworld().getGameTime());
					}
				});
				bred = true;
				break;
			}
			if (bred && t - after > 20 * 4) {
				break;
			}
		}
		if (!bred) {
			event("NOT served in 16 s");
			server.runOnServer(srv -> cow.conceive(bull.genome(), bull.visualGenome(), srv.overworld().getGameTime()));
		}
		event("end");
		stop(f);
	}

	/** The calf this calving would give at {@code now}: the game's own draw (CattleEntity.calve), from both parents' coat genes. */
	private VisualGenome predict(ServerLevel level, long now) {
		String eventId = "calving:" + cow.getUUID() + ":" + now;
		long seed = BreedingSeed.seed(Long.toString(level.getSeed()), eventId, cow.genome().hash(), bull.genome().hash(), Ruleset.defaults().version());
		return VisualGenome.inherit(cow.visualGenome(), bull.visualGenome(), seed);
	}

	/**
	 * 5-7. The sky runs (months pass), she calves beside the camera; the calf's coat on the card. Calves 1 and 2 black, 3 red. Before
	 * calves 2 and 3 she is bred back to the same bull and the earlier calves are a year older (off to the side).
	 */
	private void calving(int n, boolean wantRed) {
		int k = n - 4;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			if (!cow.isInCalf()) {
				cow.startHeatForDisplay();
				cow.conceive(bull.genome(), bull.visualGenome(), level.getGameTime());
			}
			for (int i = 0; i < calves.size(); i++) {
				CattleEntity c = calves.get(i);
				c.advanceForTest(365);
				c.settle(level);
				c.snapTo(sx - 7.5 + i * 2.5, g, sz - 5.5, 180, 0);
			}
			cow.forceCalvingForTest(1, false, false);
		});
		place(cow, 0.7, 0.2, -25);
		place(bull, 6.5, -4.0, -120);
		look(0.0, 0.5, 5.0, 0.0, 0.7, 0.6);
		card();
		showCard();
		server.runCommand("time set 3500");
		context.waitTicks(20);
		String name = "genetics-" + n + "-calf" + k;
		Filming f = start(name);
		event("start: the cow, in calf; the sky runs (months pass)");
		// two days of sky in 4 s: the scene skips the rest of the 283 days (the cut says so)
		for (int t = 0; t < (check ? 2 : 80); t++) {
			server.runCommand("time add 600");
			context.waitTick();
		}
		server.runCommand("time set 3500");
		event("sky stops: calving day");
		hold(10);
		CattleEntity[] born = new CattleEntity[1];
		int tries = 0;
		while (born[0] == null && tries++ < 400) {
			born[0] = server.computeOnServer(srv -> {
				ServerLevel level = srv.overworld();
				long now = level.getGameTime();
				VisualGenome next = predict(level, now);
				if (red(next) != wantRed) {
					return null;
				}
				var before = new java.util.HashSet<>(level.getEntitiesOfClass(CattleEntity.class, cow.getBoundingBox().inflate(6)));
				cow.makeDueNow();
				cow.settle(level);
				for (CattleEntity c : level.getEntitiesOfClass(CattleEntity.class, cow.getBoundingBox().inflate(6))) {
					if (!before.contains(c)) {
						log("calf " + c.herdId() + " predicted " + next + " got " + c.visualGenome() + " same " + next.equals(c.visualGenome()));
						c.setNoAi(true);
						c.snapTo(sx - 0.8, g, sz + 1.6, 15, 0);
						c.setYBodyRot(15);
						c.setYHeadRot(15);
						return c;
					}
				}
				return null;
			});
			if (born[0] == null) {
				context.waitTick();
			}
		}
		if (born[0] == null) {
			event("NO calf after " + tries + " ticks");
			stop(f);
			return;
		}
		calves.add(born[0]);
		CattleEntity calf = born[0];
		String look = server.computeOnServer(srv -> calf.look().describe());
		String sex = server.computeOnServer(srv -> calf.sex() == Sex.FEMALE ? "heifer" : "bull");
		String g1 = server.computeOnServer(srv -> mc1r(calf));
		event("calf " + k + " born (" + tries + " ticks after the sky stopped): " + sex + ", " + look + ", MC1R " + g1);
		hold(15);
		if (wantRed) {
			card("!CALF " + k + ": RED", "MC1R: " + g1, "An e from each parent");
		} else {
			card("^CALF " + k + ": BLACK", "MC1R: " + g1, look.startsWith("black") ? "Black and white, like them" : look);
		}
		event("card up");
		still(name + "_card");
		hold(20 * 6);
		event("end");
		card();
		stop(f);
	}

	/** 8. The herd book on the red calf: e/e. Then why, on the card. */
	private void why() {
		if (calves.isEmpty()) {
			return;
		}
		CattleEntity red = calves.get(calves.size() - 1);
		look(-2.4, 0.2, 3.6, -1.4, 0.4, 1.6);
		card();
		showCard();
		context.waitTicks(20);
		Filming f = start("genetics-8-why");
		event("start: the red calf close");
		hold(20);
		openBook(red);
		event("herd book open on the calf's first page (Dam, Sire)");
		still("genetics-8-why_book1");
		hold(45);
		pageDown();
		event("page turned: the calf's genes page (Color (MC1R) e/e, shows red)");
		hold(5);
		still("genetics-8-why_book2");
		hold(85);
		closeBook();
		card("^WHY RED?", "Bull ED/e  x  Cow ED/e", "Each passes on e half the time", "e/e = red: 1 calf in 4");
		showCard();
		event("card up: why red");
		still("genetics-8-why_card");
		hold(20 * 6);
		event("end");
		card();
		stop(f);
		log("red calf genes page: " + server.computeOnServer(srv -> HerdBookItem.genesPage(red)).replace('\n', '|'));
	}

	/** 9. The family: the bull, the cow and their three calves (two, one and newborn), wide. */
	private void family() {
		place(bull, -1.5, -1.2, -30);
		place(cow, 1.5, -1.2, 30);
		double[][] at = {{-1.9, 1.4, -15}, {0.1, 1.2, 5}, {1.8, 2.2, 20}};
		for (int i = 0; i < calves.size() && i < 3; i++) {
			place(calves.get(i), at[i][0], at[i][1], (float) at[i][2]);
		}
		look(0.0, 0.9, 7.0, 0.0, 0.7, 0.2);
		card();
		showCard();
		context.waitTicks(30);
		Filming f = start("genetics-9-family");
		event("start: the family, wide");
		hold(20 * 8);
		event("end");
		stop(f);
	}

	// ---- the readout card (Wildcatter's Films010 FilmCard) ----

	/**
	 * A transparent screen that draws the readout card across the top of the frame, kept inside the middle 9:16 for the vertical cut.
	 * Lines starting {@code ^} are titles (amber), {@code !} alarms (red), {@code +} good news (green).
	 */
	static final class FilmCard extends Screen {
		static volatile String[] lines = new String[0];
		static volatile boolean narrow;

		FilmCard() {
			super(Component.literal("readout"));
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
			String[] now = lines;
			if (now.length == 0) {
				return;
			}
			int widest = 1;
			for (String l : now) {
				widest = Math.max(widest, font.width(text(l)));
			}
			float room = narrow ? Math.min(width, height * 9f / 16f) : width;
			float s = Math.min(narrow ? 4f : 2.4f, (room * 0.90f) / widest);
			int line = 11;
			int boxW = (int) (widest * s) + 16;
			int boxH = (int) (now.length * line * s) + 10;
			int x0 = (width - boxW) / 2;
			int y0 = (int) (height * 0.07f);
			graphics.fill(x0, y0, x0 + boxW, y0 + boxH, 0xC8101418);
			graphics.pose().pushMatrix();
			graphics.pose().translate(x0 + 8, y0 + 6);
			graphics.pose().scale(s, s);
			for (int i = 0; i < now.length; i++) {
				String l = now[i];
				int colour = l.startsWith("^") ? 0xFFF0C040 : l.startsWith("!") ? 0xFFFF5A50 : l.startsWith("+") ? 0xFF70E070 : 0xFFF2F2F2;
				graphics.text(font, text(l), 0, i * line, colour, true);
			}
			graphics.pose().popMatrix();
		}

		private static String text(String l) {
			return l.startsWith("^") || l.startsWith("!") || l.startsWith("+") ? l.substring(1) : l;
		}
	}
}
