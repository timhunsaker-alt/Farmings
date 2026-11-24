package dev.farmings.fabric.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.CattleEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.item.MilkArmorItem;
import java.util.Locale;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * {@code milkArmor} (FARMINGS_ONLY=milkArmor): the milk armor short (docs/milk-armor.md), in real time on the music videos' world, in a
 * plains pasture with cattle, in the landscape window (2880 x 1620) for a vertical cut from its middle 9:16. Six films: 1 the chestplate
 * crafted from eight milk buckets (real clicks in the crafting screen; the empty buckets come back), 2 the fresh set worn, the player
 * turning, 3 a game day passes (the sky runs; each piece's own freshness clock is moved by the same ticks, so it spoils by its own rule),
 * 4 the inventory: "Spoiled Milk Chestplate", 5 sick in first person (Hunger and Nausea; the hunger drain sped up 20x, on the card),
 * 6 the armor taken off and the effects wearing off. Every event is logged as {@code [farmings-films] event <film> <what> at <s> s}.
 * FARMINGS_FILMS picks films by number (film 1 is needed for the others: it makes the chestplate).
 */
final class MilkArmorFilms {
	private static final String SEED = "farmings";
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final float PITCH_3P = 12f;
	private static volatile float spin = Float.NaN;
	private static float spinLast;
	private static boolean hooked;
	private final ClientGameTestContext context;
	private TestServerContext server;
	private int g;
	private int sx;
	private int sz;
	private String film = "";
	private long filmStart;
	private double cx = 720;
	private double cy = 405;

	MilkArmorFilms(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[farmings-films] " + line);
	}

	private void event(String what) {
		log(String.format(Locale.ROOT, "event %s %s at %.2f s", film, what, (System.nanoTime() - filmStart) / 1e9));
	}

	private static boolean wanted(int n) {
		String only = System.getenv("FARMINGS_FILMS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(Integer.toString(n));
	}

	void run() {
		if (!hooked) {
			hooked = true;
			// the player's body and head turned to {@code spin} at the end of each client tick, so the third-person camera (which follows
			// the view rotation) stays put while the player turns in front of it
			ClientTickEvents.END_CLIENT_TICK.register(client -> {
				float s = spin;
				if (!Float.isNaN(s) && client.player != null) {
					var p = client.player;
					p.yBodyRotO = spinLast;
					p.yHeadRotO = spinLast;
					p.yBodyRot = s;
					p.yHeadRot = s;
					spinLast = s;
				}
			});
		}
		context.runOnClient(client -> {
			client.options.renderDistance().set(10);
			client.options.graphicsPreset().set(GraphicsPreset.FANCY);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.cloudStatus().set(CloudStatus.FANCY);
			client.options.entityDistanceScaling().set(3.0);
			client.options.fov().set(70);
			client.options.bobView().set(false);
			client.options.menuBackgroundBlurriness().set(0);
			client.options.pauseOnLostFocus = false;
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
			server.runCommand("difficulty easy");     // not peaceful: peaceful refills the hunger bar
			server.runCommand("gamemode spectator @p");
			hud(false);
			BlockPos plains = server.computeOnServer(srv -> {
				var r = srv.overworld().findClosestBiome3d(h -> h.is(ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace("plains"))),
						new BlockPos(0, 64, 0), 3200, 32, 64);
				return r == null ? new BlockPos(0, 64, 0) : r.getFirst();
			});
			// away from the genetics films (60 west, 90 north) and the dairy short (160 east)
			sx = plains.getX() - 60;
			sz = plains.getZ() - 170;
			server.runCommand("tp @p " + sx + " 160 " + sz);
			context.waitTicks(100);
			build();
			context.waitTicks(60);
			server.runCommand("gamemode survival @p");
			server.runOnServer(srv -> {
				ServerPlayer p = player(srv);
				p.getInventory().clearContent();
				p.setHealth(p.getMaxHealth());
				p.getFoodData().setFoodLevel(20);
				p.getFoodData().setSaturation(5);
			});
			guiWidth(720);     // GUI scale 4: the crafting and inventory screens and the hotbar fit the middle 9:16
			GeneticsFilms.FilmCard.narrow = true;
			try {
				craft(wanted(1));
				if (wanted(2)) {
					wear();
				} else {
					equip();
				}
				day(wanted(3));
				if (wanted(4)) {
					name();
				}
				sick(wanted(5));
				if (wanted(6)) {
					off();
				}
			} finally {
				spin = Float.NaN;
				GeneticsFilms.FilmCard.narrow = false;
				card();
				context.runOnClient(client -> {
					client.setScreenAndShow(null);
					client.options.setCameraType(CameraType.FIRST_PERSON);
					client.options.fov().set(70);
				});
			}
		}
	}

	private static ServerPlayer player(net.minecraft.server.MinecraftServer srv) {
		return srv.getPlayerList().getPlayers().get(0);
	}

	// ---- the set ----

	private BlockPos table() {
		return new BlockPos(sx, g - 2, sz);
	}

	/**
	 * A pasture north of the player: fenced 25 x 13, six dairy cows (Holstein, Jersey, Brown Swiss) grazing, a meadow around it. The player
	 * stands 3 blocks south of its fence; a crafting table is buried under them (the crafting screen needs one within reach).
	 */
	private void build() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int ground = MusicVideo.surface(level, sx, sz);
			g = Math.max(ground, level.getSeaLevel() + 2);
			MusicVideo.pad(level, sx - 16, sz - 20, sx + 16, sz + 10, g, 14, 12);
			MusicVideo.meadow(level, sx - 44, sz - 50, sx + 44, sz + 40, 93, 0.30, 0.05);
			for (int x = sx - 4; x <= sx + 4; x++) {
				for (int z = sz - 2; z <= sz + 6; z++) {
					BlockPos p = new BlockPos(x, g, z);
					if (!level.getBlockState(p).isAir() && level.getBlockState(p).canBeReplaced()) {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			MusicVideo.fence(level, new BlockPos(sx, g, sz), -12, -16, 12, -3);
			level.setBlockAndUpdate(table(), Blocks.CRAFTING_TABLE.defaultBlockState());
			for (var m : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(new BlockPos(sx, g, sz)).inflate(48))) {
				m.discard();
			}
			long now = level.getGameTime();
			CattleBreed[] breeds = {CattleBreed.HOLSTEIN, CattleBreed.JERSEY, CattleBreed.HOLSTEIN, CattleBreed.BROWN_SWISS, CattleBreed.HOLSTEIN,
					CattleBreed.JERSEY};
			double[][] spots = {{-3.5, -7.5}, {2.5, -6.0}, {5.5, -10.5}, {-7.0, -11.0}, {0.5, -13.0}, {8.0, -6.5}};
			for (int i = 0; i < breeds.length; i++) {
				CattleEntity c = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
				c.initFounder(breeds[i], Sex.FEMALE, 8800L + i, (3 + i % 3) * 365, breeds[i].breedingWeightKg(), now);
				c.snapTo(sx + 0.5 + spots[i][0], g, sz + 0.5 + spots[i][1], 40 + i * 70, 0);
				level.addFreshEntity(c);
				c.settle(level);
			}
			MusicVideo.discardItems(level, new BlockPos(sx, g, sz), 60);
			log("site " + sx + "," + sz + " g " + g + " biome " + level.getBiome(new BlockPos(sx, g, sz)).getRegisteredName());
		});
	}

	// ---- camera, card, cursor ----

	/** The player at its mark, looking at yaw/pitch (teleported on both sides). */
	private void stand(float yaw, float pitch) {
		double px = sx + 0.5, py = g, pz = sz + 0.5;
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", px, py, pz, yaw, pitch));
		context.runOnClient(client -> {
			var p = client.player;
			p.snapTo(px, py, pz, yaw, pitch);
			p.setYHeadRot(yaw);
			p.setYBodyRot(yaw);
			p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		});
		context.waitTicks(2);
	}

	/** First person (the HUD shown or not), or the third-person front camera, closer in (fov 50), the player facing it. */
	private void firstPerson(boolean hud) {
		spin = Float.NaN;
		context.runOnClient(client -> {
			client.options.setCameraType(CameraType.FIRST_PERSON);
			client.options.fov().set(70);
		});
		stand(180, 6);
		hud(hud);
	}

	private void thirdPerson() {
		stand(0, PITCH_3P);
		spinLast = 0;
		spin = 0;
		context.runOnClient(client -> {
			client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			client.options.fov().set(50);
		});
		hud(false);
		context.waitTicks(4);
	}

	private void hud(boolean show) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() == show) {
				client.gui.hud.toggle();
			}
		});
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

	private void closeScreen() {
		context.runOnClient(client -> {
			if (client.player != null && client.player.containerMenu != client.player.inventoryMenu) {
				client.player.closeContainer();
			}
			client.setScreenAndShow(null);
		});
		context.waitTicks(2);
	}

	/** A slot's middle in window coordinates, for the open container screen (176 x 166, centred, as both screens used here are). */
	private double[] slot(int i) {
		return context.computeOnClient(client -> {
			var s = client.player.containerMenu.slots.get(i);
			var w = client.getWindow();
			double toWindow = (double) w.getScreenWidth() / w.getGuiScaledWidth();
			int left = (w.getGuiScaledWidth() - 176) / 2;
			int top = (w.getGuiScaledHeight() - 166) / 2;
			return new double[] {(left + s.x + 8) * toWindow, (top + s.y + 8) * toWindow};
		});
	}

	/** The cursor eased to (x, y) over {@code ticks}. */
	private void cursorTo(double[] to, int ticks) {
		double x0 = cx, y0 = cy;
		for (int t = 1; t <= ticks; t++) {
			double s = t / (double) ticks;
			s = s * s * (3 - 2 * s);
			context.getInput().setCursorPos(x0 + (to[0] - x0) * s, y0 + (to[1] - y0) * s);
			context.waitTick();
		}
		cx = to[0];
		cy = to[1];
	}

	private void cursorAt(double x, double y) {
		context.getInput().setCursorPos(x, y);
		cx = x;
		cy = y;
	}

	private void click() {
		context.getInput().pressMouse(1);     // SDL numbers the left button 1
		context.waitTick();
	}

	private Filming start(String name, boolean want) {
		film = name;
		filmStart = System.nanoTime();
		if (!want) {
			return null;
		}
		Filming f = context.computeOnClient(client -> Filming.start(client, name));
		hideCursor();
		// a lead-in: the first take's first seconds caught other windows still on top of the game
		filmStart = System.nanoTime();     // event times are video times (about 0.1 s late)
		context.waitTicks(40);
		return f;
	}

	private void stop(Filming f) {
		if (f != null) {
			f.stop();
		}
	}

	private String worn() {
		return server.computeOnServer(srv -> {
			StringBuilder b = new StringBuilder();
			ServerPlayer p = player(srv);
			for (EquipmentSlot s : SLOTS) {
				ItemStack st = p.getItemBySlot(s);
				b.append(s.getName()).append('=').append(st.isEmpty() ? "-" : st.getHoverName().getString()).append(' ');
			}
			b.append("| food ").append(p.getFoodData().getFoodLevel()).append(" sat ").append(String.format(Locale.ROOT, "%.1f", p.getFoodData().getSaturationLevel()))
					.append(" hunger ").append(p.hasEffect(MobEffects.HUNGER)).append(" nausea ").append(p.hasEffect(MobEffects.NAUSEA));
			return b.toString();
		});
	}

	// ---- the films ----

	/**
	 * 1. The crafting screen over the pasture: eight milk buckets from the hotbar, one by one, into the chestplate's shape; the Milk
	 * Chestplate appears; taken, the eight buckets come back empty in the grid; the chestplate set in the inventory's first slot, its
	 * tooltip (the game's toughest armor's numbers).
	 */
	private void craft(boolean want) {
		firstPerson(false);
		server.runOnServer(srv -> {
			ServerPlayer p = player(srv);
			p.getInventory().clearContent();
			for (int i = 0; i < 8; i++) {
				p.getInventory().setItem(i, new ItemStack(Items.MILK_BUCKET));
			}
			p.getInventory().setSelectedSlot(8);
			p.openMenu(Blocks.CRAFTING_TABLE.defaultBlockState().getMenuProvider(srv.overworld(), table()));
		});
		context.waitTicks(10);
		hideCursor();
		double[] park = slot(5);
		cursorAt(park[0] + 60, park[1] + 10);
		context.waitTicks(20);
		String name = "milkarmor-1-craft";
		Filming f = start(name, want);
		event("start: the crafting screen, eight milk buckets in the hotbar");
		context.waitTicks(30);
		int[] grid = {1, 3, 4, 5, 6, 7, 8, 9};
		for (int i = 0; i < 8; i++) {
			cursorTo(slot(37 + i), 5);
			click();
			context.waitTicks(1);
			cursorTo(slot(grid[i]), 6);
			click();
			event("bucket " + (i + 1) + " in the grid");
			context.waitTicks(1);
		}
		boolean made = context.computeOnClient(client -> client.player.containerMenu.slots.get(0).getItem().is(FarmingsItems.MILK_CHESTPLATE));
		event("result: " + (made ? "Milk Chestplate" : "NOTHING"));
		context.waitTicks(16);
		cursorTo(slot(0), 8);
		event("hover the result");
		context.waitTicks(40);
		click();
		int buckets = context.computeOnClient(client -> {
			int n = 0;
			for (int i = 1; i <= 9; i++) {
				n += client.player.containerMenu.slots.get(i).getItem().is(Items.BUCKET) ? 1 : 0;
			}
			return n;
		});
		event("taken: " + buckets + " empty buckets back in the grid");
		context.waitTicks(12);
		cursorTo(slot(10), 10);
		click();
		event("chestplate in the inventory; its tooltip");
		context.waitTicks(70);
		cursorTo(slot(5), 10);
		event("hover an empty bucket");
		context.waitTicks(40);
		event("end");
		stop(f);
		closeScreen();
		log("after craft: inventory slot 9 = " + server.computeOnServer(srv -> player(srv).getInventory().getItem(9).getHoverName().getString()));
	}

	/** The fresh set on the player, every piece's clock started now (the crafted chestplate kept). */
	private void equip() {
		server.runOnServer(srv -> {
			ServerPlayer p = player(srv);
			ItemStack chest = ItemStack.EMPTY;
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
				if (p.getInventory().getItem(i).is(FarmingsItems.MILK_CHESTPLATE)) {
					chest = p.getInventory().getItem(i).copy();
				}
			}
			p.getInventory().clearContent();
			Item[] items = {FarmingsItems.MILK_HELMET, FarmingsItems.MILK_CHESTPLATE, FarmingsItems.MILK_LEGGINGS, FarmingsItems.MILK_BOOTS};
			long now = srv.overworld().getGameTime();
			for (int i = 0; i < 4; i++) {
				ItemStack st = i == 1 && !chest.isEmpty() ? chest : new ItemStack(items[i]);
				MilkArmorItem.setMade(st, now);
				p.setItemSlot(SLOTS[i], st);
			}
		});
		context.waitTicks(4);
	}

	private void turn(float from, float to, int ticks) {
		for (int t = 0; t <= ticks; t++) {
			double s = t / (double) ticks;
			s = s * s * (3 - 2 * s);
			spin = (float) (from + (to - from) * s);
			context.waitTick();
		}
	}

	/** 2. The full fresh set, third person from the front, the pasture behind: the player turns all the way round. */
	private void wear() {
		equip();
		thirdPerson();
		context.waitTicks(30);
		String name = "milkarmor-2-wear";
		Filming f = start(name, true);
		event("start: the full fresh milk set, " + worn());
		context.waitTicks(50);
		event("turn starts");
		turn(0, 360, 130);
		event("turn ends");
		context.waitTicks(50);
		event("end");
		stop(f);
	}

	/**
	 * 3. One game day, sped up: the sky runs 24,000 ticks in 8 s, and each piece's freshness clock is moved back by the same ticks each
	 * tick (game time itself does not run with the sky), so the armor spoils by its own rule as the day ends. The card says so.
	 */
	private void day(boolean want) {
		thirdPerson();
		server.runCommand("time set 1500");
		server.runOnServer(srv -> {
			ServerPlayer p = player(srv);
			long now = srv.overworld().getGameTime();
			for (EquipmentSlot s : SLOTS) {
				MilkArmorItem.setMade(p.getItemBySlot(s), now);
			}
			p.getFoodData().setFoodLevel(20);
			p.getFoodData().setSaturation(0);
		});
		card("^ONE DAY LATER...", "Hour 0 of 24", "(time sped up)");
		showCard();
		context.waitTicks(30);
		String name = "milkarmor-3-day";
		Filming f = start(name, want);
		event("start: fresh, morning");
		context.waitTicks(40);
		int run = 160;
		int per = (int) (MilkArmorItem.SPOIL_TICKS / run);
		event("sky starts running");
		int spoiledAt = -1;
		for (int t = 0; t < run + 200; t++) {
			if (t < run) {
				server.runCommand("time add " + per);
				server.runOnServer(srv -> {
					ServerPlayer p = player(srv);
					for (EquipmentSlot s : SLOTS) {
						ItemStack st = p.getItemBySlot(s);
						MilkArmorItem.setMade(st, MilkArmorItem.made(st) - per);
					}
				});
				card("^ONE DAY LATER...", String.format(Locale.ROOT, "Hour %d of 24", (t + 1) * 24 / run), "(time sped up)");
			}
			if (t == run) {
				event("sky stops (a full day run)");
			}
			if (spoiledAt < 0 && server.computeOnServer(srv -> MilkArmorItem.spoiled(player(srv).getItemBySlot(EquipmentSlot.CHEST)))) {
				spoiledAt = t;
				event("SPOILED: " + worn());
				card("!SPOILED!", "No fridge");
			}
			if (spoiledAt >= 0 && t == spoiledAt + 50) {
				event("turn starts");
			}
			if (spoiledAt >= 0 && t >= spoiledAt + 50 && t <= spoiledAt + 170) {
				double s = (t - spoiledAt - 50) / 120.0;
				s = s * s * (3 - 2 * s);
				spin = (float) (360 * s);
			}
			if (spoiledAt >= 0 && t >= spoiledAt + 200) {
				break;
			}
			context.waitTick();
		}
		event("end: " + worn());
		stop(f);
		card();
		closeScreen();
	}

	/** 4. The inventory: the player in the spoiled set, the cursor on the chestplate: "Spoiled Milk Chestplate". */
	private void name() {
		firstPerson(true);
		context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
		context.waitTicks(6);
		hideCursor();
		double[] chest = slot(6);
		cursorAt(chest[0] + 150, chest[1] + 20);
		context.waitTicks(10);
		String name = "milkarmor-4-name";
		Filming f = start(name, true);
		event("start: the inventory, the spoiled set on the player");
		context.waitTicks(50);
		cursorTo(chest, 10);
		event("hover the chestplate: " + context.computeOnClient(client -> client.player.containerMenu.slots.get(6).getItem().getHoverName().getString()));
		context.waitTicks(90);
		cursorTo(slot(5), 8);
		event("hover the helmet");
		context.waitTicks(40);
		event("end");
		stop(f);
		closeScreen();
	}

	/** 5. First person, sick: the nausea wobble, the Hunger and Nausea icons, the hunger bar draining (20x, the card says so). */
	private void sick(boolean want) {
		firstPerson(true);
		server.runOnServer(srv -> {
			player(srv).getFoodData().setFoodLevel(20);
			player(srv).getFoodData().setSaturation(0);
		});
		card("^SICK", "Hunger and Nausea", "(hunger drain shown 20x)");
		showCard();
		context.waitTicks(10);
		String name = "milkarmor-5-sick";
		Filming f = start(name, want);
		event("start: " + worn());
		int last = 20;
		for (int t = 0; t < 20 * 13; t++) {
			if (t >= 20) {
				// the Hunger effect adds 0.005 exhaustion a tick; this adds 19 times more, so the bar drains 20 times as fast
				server.runOnServer(srv -> player(srv).causeFoodExhaustion(0.095f));
			}
			int food = server.computeOnServer(srv -> player(srv).getFoodData().getFoodLevel());
			if (food != last) {
				event("food " + food);
				last = food;
			}
			context.waitTick();
		}
		event("end: " + worn());
		stop(f);
		card();
		closeScreen();
	}

	/** 6. The armor off (picked out of its slots) and the Hunger and Nausea wearing off a few seconds later. */
	private void off() {
		firstPerson(true);
		context.waitTicks(10);
		String name = "milkarmor-6-off";
		Filming f = start(name, true);
		event("start: " + worn());
		context.waitTicks(30);
		context.runOnClient(client -> client.setScreenAndShow(new InventoryScreen(client.player)));
		context.waitTicks(4);
		hideCursor();
		double[] chest = slot(6);
		cursorAt(chest[0] + 150, chest[1] + 20);
		event("inventory open");
		context.waitTicks(10);
		// each piece picked out of its armor slot and set down in the inventory's top row
		for (int i = 5; i <= 8; i++) {
			cursorTo(slot(i), 5);
			click();
			cursorTo(slot(i + 4), 5);
			click();
			event("armor slot " + i + " emptied");
			context.waitTicks(2);
		}
		context.waitTicks(16);
		closeScreen();
		event("armor off: " + worn());
		boolean hunger = true;
		boolean nausea = true;
		for (int t = 0; t < 20 * 14; t++) {
			boolean h = context.computeOnClient(client -> client.player.hasEffect(MobEffects.HUNGER));
			boolean n = context.computeOnClient(client -> client.player.hasEffect(MobEffects.NAUSEA));
			if (hunger && !h) {
				event("Hunger gone");
			}
			if (nausea && !n) {
				event("Nausea gone (its wobble fades over 3 s)");
			}
			hunger = h;
			nausea = n;
			if (!h && !n && t > 20 * 10) {
				break;
			}
			context.waitTick();
		}
		event("end: " + worn());
		stop(f);
	}
}
