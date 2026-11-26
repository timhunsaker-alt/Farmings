package dev.farmings.fabric.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.CattleEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.item.CheeseArmorItem;
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
 * {@code cheeseArmor} (FARMINGS_ONLY=cheeseArmor): the cheese armor short (docs/cheese-armor.md), in real time on the milk armor short's
 * set (the music videos' seed, the plains pasture) with a creamery built south of it, formed, its vat powered. Four films: 1 a fresh milk
 * chestplate put in the cheese vat, filmed side on (a client-only camera, the player drawn by a double), the vat sets it, the press
 * presses it (a cut to it close, sped up 8x on the card, whey dripping) and the cheese chestplate is taken from the press with an empty
 * hand; 2 the full cheese set worn, third person, turning; 3 (third person from behind) a spoiled milk helmet
 * refused at the vat ("Spoiled milk does not make cheese"); 4 hungry, a cheese chestplate sneak-eaten, the hunger bar filling. Events are
 * logged as {@code [farmings-films] event <film> <what> at <s> s}, in video seconds. FARMINGS_FILMS picks films by number (1 makes the
 * chestplate 2 wears; without it the scene makes one).
 */
final class CheeseArmorFilms {
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
	private BlockPos ctlPos;
	private BlockPos vatPos;
	private BlockPos pressPos;

	CheeseArmorFilms(ClientGameTestContext context) {
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
			creamery();
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
				vat(wanted(1));
				if (wanted(2)) {
					wear();
				} else {
					equip();
				}
				if (wanted(3)) {
					refused();
				}
				if (wanted(4)) {
					eat();
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

	// ---- the creamery ----

	/**
	 * The creamery's row south of the player's mark, its fronts north toward the pasture: controller, then each part in its order (tank,
	 * pasteurizer, separator, churn, vat, press, shelf, bottler), as the creamery game tests build it. Formed, the vat given power.
	 */
	private void creamery() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int cz = sz + 9;
			for (int x = sx - 14; x <= sx + 14; x++) {
				for (int z = sz + 3; z <= sz + 13; z++) {
					for (int y = g; y <= g + 4; y++) {
						BlockPos p = new BlockPos(x, y, z);
						if (!level.getBlockState(p).isAir()) {
							level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
					}
					level.setBlock(new BlockPos(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			ctlPos = new BlockPos(sx - 9, g, cz);
			level.setBlock(ctlPos, dev.farmings.FarmingsBlocks.CREAMERY_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
			int x = ctlPos.getX() + 1;
			for (dev.farmings.creamery.CreameryParts kd : dev.farmings.creamery.CreameryParts.values()) {
				BlockPos at = new BlockPos(x, g, cz);
				FarmingsClientGameTest.placePart(level, dev.farmings.FarmingsBlocks.creameryPart(kd), at);
				if (kd == dev.farmings.creamery.CreameryParts.CHEESE_VAT) {
					vatPos = at;
				}
				if (kd == dev.farmings.creamery.CreameryParts.CHEESE_PRESS) {
					pressPos = at;
				}
				x += kd.part.length();
			}
			var ctl = controller(level);
			ctl.refresh(level);
			if (ctl.formed()) {
				ctl.machine(level, dev.farmings.process.creamery.CreameryPart.CHEESE_VAT).forceRunning(true);
			}
			log("creamery at " + ctlPos + " vat " + vatPos + " press " + pressPos + ": " + ctl.readout());
		});
	}

	private dev.farmings.creamery.CreameryControllerEntity controller(ServerLevel level) {
		return (dev.farmings.creamery.CreameryControllerEntity) level.getBlockEntity(ctlPos);
	}

	private dev.farmings.process.creamery.Creamery.ArmorStep step() {
		return server.computeOnServer(srv -> controller(srv.overworld()).creamery().armor());
	}

	/** The card's line for the piece in the creamery. */
	private String stepLine() {
		return server.computeOnServer(srv -> {
			var c = controller(srv.overworld()).creamery();
			return switch (c.armor()) {
				case NONE -> "Fresh milk chestplate in hand";
				case VAT -> "+In the vat: the milk sets";
				case PRESS -> String.format(Locale.ROOT, "Pressing: %d%%", 100 * c.armorTicks() / dev.farmings.process.creamery.Creamery.PRESS_TICKS);
				default -> "+Pressed: ready at the press";
			};
		});
	}

	/** First person, the player at (x, z) (block-centred offsets in world units), looking at a point. */
	private float[] aim(double px, double pz, double tx, double ty, double tz) {
		double dx = tx - px, dy = ty - (g + 1.62), dz = tz - pz;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		return new float[] {yaw, pitch};
	}

	private void placeAt(double px, double pz, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %d %.3f %.2f %.2f", px, g, pz, yaw, pitch));
		context.runOnClient(client -> {
			var p = client.player;
			p.snapTo(px, g, pz, yaw, pitch);
			p.setYHeadRot(yaw);
			p.setYBodyRot(yaw);
			p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		});
		context.waitTicks(2);
	}

	/** The view eased from its rotation to (yaw, pitch) over {@code ticks} (client side: the local player's rotation is the client's). */
	private void lookTo(float yaw, float pitch, int ticks) {
		float[] from = context.computeOnClient(client -> new float[] {client.player.getYRot(), client.player.getXRot()});
		for (int t = 1; t <= ticks; t++) {
			double s = t / (double) ticks;
			s = s * s * (3 - 2 * s);
			float y = (float) (from[0] + (yaw - from[0]) * s), x = (float) (from[1] + (pitch - from[1]) * s);
			context.runOnClient(client -> {
				client.player.setYRot(y);
				client.player.setXRot(x);
				client.player.setYHeadRot(y);
			});
			context.waitTick();
		}
	}

	/** Third person from behind (fov 60, the HUD on): the player in the middle of the frame, what it looks at beyond. */
	private void backView() {
		spin = Float.NaN;
		context.runOnClient(client -> {
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			client.options.fov().set(60);
		});
		hud(true);
	}

	private double[] vatSpot() {
		// in front of the vat and the press, between them, 3.5 blocks back
		return new double[] {(vatPos.getX() + pressPos.getX()) / 2.0 + 0.5, vatPos.getZ() - 3.5};
	}

	private void inHand(ItemStack stack) {
		server.runOnServer(srv -> {
			ServerPlayer p = player(srv);
			p.getInventory().setItem(0, stack);
			p.getInventory().setSelectedSlot(0);
		});
		context.waitTicks(3);
	}

	private void use() {
		context.getInput().pressKey(o -> o.keyUse);
		context.waitTick();
	}

	// ---- the films ----

	// ---- the side camera ----

	/**
	 * The side camera: a client-only armor stand (never added to the world, so never ticked, drawn or sent to the server) that the client
	 * films from, so the player is seen side on with the vat or press beyond (from behind, the player's body hid them).
	 */
	private net.minecraft.world.entity.decoration.ArmorStand cam;
	private double[] camPose;
	/**
	 * The player's double: the game never draws the local player for another camera entity, so while the side camera films, a client-only
	 * copy of the player (its profile and so its skin, a UUID of its own) is drawn in its place, set each tick to the player's position,
	 * view, body turn and what it holds and wears, and swung when the player's arm swings.
	 */
	private static volatile net.minecraft.client.player.RemotePlayer twin;
	private static boolean twinHooked;
	private static final int TWIN_ID = -4242;
	private static final EquipmentSlot[] TWIN_SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
			EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private static void followPlayer(net.minecraft.client.Minecraft client) {
		var d = twin;
		var p = client.player;
		if (d == null || p == null) {
			return;
		}
		d.setPos(p.getX(), p.getY(), p.getZ());
		d.setYRot(p.getYRot());
		d.setXRot(p.getXRot());
		d.setYHeadRot(p.getYHeadRot());
		d.setYBodyRot(p.yBodyRot);
		for (EquipmentSlot slot : TWIN_SLOTS) {
			ItemStack want = p.getItemBySlot(slot);
			if (!ItemStack.matches(want, d.getItemBySlot(slot))) {
				d.setItemSlot(slot, want.copy());
			}
		}
	}

	private void showTwin(boolean on) {
		if (!twinHooked) {
			twinHooked = true;
			ClientTickEvents.END_CLIENT_TICK.register(CheeseArmorFilms::followPlayer);
		}
		context.runOnClient(client -> {
			if (twin != null) {
				client.level.removeEntity(TWIN_ID, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
				twin = null;
			}
			if (on) {
				var me = client.player.getGameProfile();
				java.util.UUID id = me.id();
				// flipped by the same bits in both halves: a new UUID with the same hash, so the same default skin and arms
				long x = 0x5a5a5a5a5a5a5a5aL;
				var d = new net.minecraft.client.player.RemotePlayer(client.level, new com.mojang.authlib.GameProfile(
						new java.util.UUID(id.getMostSignificantBits() ^ x, id.getLeastSignificantBits() ^ x), me.name()));
				d.setId(TWIN_ID);
				var p = client.player;
				d.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
				client.level.addEntity(d);
				twin = d;
				followPlayer(client);
			}
		});
	}

	private static double[] env(String name, double... fallback) {
		String v = System.getenv(name);
		if (v == null || v.isBlank()) {
			return fallback;
		}
		String[] parts = v.split(",");
		double[] out = new double[parts.length];
		for (int i = 0; i < parts.length; i++) {
			out[i] = Double.parseDouble(parts[i].trim());
		}
		return out;
	}

	/** The camera's eye at (x, y, z) looking at (tx, ty, tz), as a pose {x, y, z, yaw, pitch}. */
	private static double[] pose(double x, double y, double z, double tx, double ty, double tz) {
		double dx = tx - x, dy = ty - y, dz = tz - z;
		return new double[] {x, y, z, Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))};
	}

	/** A pose looking at (tx, ty, tz) from {@code off} = {dx, dy, dz} away. */
	private static double[] poseAt(double tx, double ty, double tz, double[] off) {
		return pose(tx + off[0], ty + off[1], tz + off[2], tx, ty, tz);
	}

	/** Films from the side camera at {@code p} (first person on it, fov 60, no HUD: the card is a screen and stays). */
	private void sideCamera(double[] p) {
		context.runOnClient(client -> {
			cam = new net.minecraft.world.entity.decoration.ArmorStand(client.level, p[0], p[1], p[2]);
			client.options.setCameraType(CameraType.FIRST_PERSON);
			client.options.fov().set(60);
			client.setCameraEntity(cam);
		});
		showTwin(true);
		hud(false);
		camTo(p, true);
	}

	private void camTo(double[] p, boolean snap) {
		context.runOnClient(client -> {
			var e = cam;
			double eye = e.getEyeHeight();
			if (snap) {
				e.setPos(p[0], p[1] - eye, p[2]);
				e.xo = e.xOld = p[0];
				e.yo = e.yOld = p[1] - eye;
				e.zo = e.zOld = p[2];
				e.yRotO = (float) p[3];
				e.xRotO = (float) p[4];
			} else {
				e.xo = e.xOld = e.getX();
				e.yo = e.yOld = e.getY();
				e.zo = e.zOld = e.getZ();
				e.yRotO = e.getYRot();
				e.xRotO = e.getXRot();
				e.setPos(p[0], p[1] - eye, p[2]);
			}
			// a living entity's view turns with its head, not its yaw
			float headO = snap ? (float) p[3] : e.getYHeadRot();
			e.setYRot((float) p[3]);
			e.setXRot((float) p[4]);
			e.setYHeadRot((float) p[3]);
			e.yHeadRotO = headO;
			e.setYBodyRot((float) p[3]);
			e.yBodyRotO = headO;
		});
		camPose = p;
	}

	/** The camera eased from where it is to {@code to} over {@code ticks}. */
	private void camMove(double[] to, int ticks) {
		camMove(to, ticks, () -> {});
	}

	/** The camera eased to {@code to} over {@code ticks}, {@code each} run every tick of the move. */
	private void camMove(double[] to, int ticks, Runnable each) {
		double[] from = camPose;
		double turn = Math.IEEEremainder(to[3] - from[3], 360);
		for (int t = 1; t <= ticks; t++) {
			double s = t / (double) ticks;
			s = s * s * (3 - 2 * s);
			double[] p = new double[5];
			for (int i = 0; i < 5; i++) {
				p[i] = from[i] + (to[i] - from[i]) * s;
			}
			p[3] = from[3] + turn * s;
			camTo(p, false);
			each.run();
			context.waitTick();
		}
		camTo(to, true);
	}

	private void playerCamera() {
		showTwin(false);
		context.runOnClient(client -> client.setCameraEntity(client.player));
		cam = null;
	}

	/**
	 * The use key's own work on a block, without its aim: the client's game mode uses the hand on the block's north face (as the use key
	 * does when the crosshair is on it), and the arm swings. Filmed from the side camera, the crosshair's ray would start at the camera.
	 */
	private void useOn(BlockPos pos) {
		context.runOnClient(client -> {
			var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ()),
					net.minecraft.core.Direction.NORTH, pos, false);
			client.gameMode.useItemOn(client.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
			client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
			if (twin != null) {
				twin.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
			}
		});
		context.waitTick();
	}

	// ---- the films ----

	/**
	 * 1. At the creamery, filmed side on from the west, a little north (the side camera): the player stands 0.9 blocks in front of the vat's
	 * middle, a fresh milk chestplate in the right hand (toward the camera), and sets it in the cheese vat (the use on the vat); the vat
	 * sets it on its next working second and it goes to the press; a cut to the press, close from the north-east, pushing in as it presses
	 * for its own time (800 ticks), sped up 8x here (the card says so), whey running down its mold; a cut to the south-east, over the
	 * row's back (the press in front, the player behind it), and the player turns to the press and takes the cheese chestplate from it
	 * with an empty hand. CHEESE_CAM, CHEESE_PRESS_CAM and CHEESE_TAKE_CAM (dx,dy,dz from what each looks at) try other cameras.
	 */
	private void vat(boolean want) {
		firstPerson(true);
		double vx = vatPos.getX() + 1.0, vz = vatPos.getZ();
		double px = vatPos.getX() + 1.0, pz = vatPos.getZ() - 0.9;
		float[] v = aim(px, pz, vx, g + 0.5, vz + 0.5);
		float[] pr = aim(px, pz, pressPos.getX() + 0.5, g + 0.7, pressPos.getZ() + 0.5);
		placeAt(px, pz, v[0], v[1]);
		double[] wide = poseAt(px, g + 0.8, vz - 0.3, env("CHEESE_CAM", -3.3, 1.1, -0.7));
		double[] pressOff = env("CHEESE_PRESS_CAM", 0.9, 0.9, -2.0);
		// the take: from the south-east, over the row's back, the press in front and the player behind it, turned to it (and so to the camera)
		double[] take = poseAt((pressPos.getX() + 0.5 + px) / 2, g + 0.9, (pressPos.getZ() + 0.5 + pz) / 2, env("CHEESE_TAKE_CAM", 2.2, 1.6, 3.0));
		double[] close = poseAt(pressPos.getX() + 0.5, g + 0.35, pressPos.getZ() + 0.3, pressOff);
		double[] closer = poseAt(pressPos.getX() + 0.5, g + 0.35, pressPos.getZ() + 0.3, new double[] {pressOff[0] * 0.7, pressOff[1] * 0.7, pressOff[2] * 0.7});
		ItemStack milk = new ItemStack(FarmingsItems.MILK_CHESTPLATE);
		server.runOnServer(srv -> {
			MilkArmorItem.setMade(milk, srv.overworld().getGameTime());
			ServerPlayer p = player(srv);
			p.getInventory().clearContent();
			p.getFoodData().setFoodLevel(20);
		});
		inHand(milk);
		sideCamera(wide);
		card("^CHEESE VAT", stepLine());
		showCard();
		context.waitTicks(10);
		String name = "cheesearmor-1-vat";
		Filming f = start(name, want);
		event("start: a fresh milk chestplate in hand, the vat ahead, the press beside it, side on | " + server.computeOnServer(srv -> controller(srv.overworld()).readout()));
		context.waitTicks(30);
		useOn(vatPos);
		event("use on the vat: " + step() + " | in hand " + server.computeOnServer(srv -> player(srv).getMainHandItem().isEmpty() ? "nothing" : player(srv).getMainHandItem().getHoverName().getString()));
		int pressAt = -1;
		for (int t = 0; t < 20 * 20; t++) {
			var s = step();
			card("^CHEESE VAT AND PRESS", stepLine(), s == dev.farmings.process.creamery.Creamery.ArmorStep.PRESS ? "(press time sped up 8x)" : "");
			if (s == dev.farmings.process.creamery.Creamery.ArmorStep.PRESS && pressAt < 0) {
				pressAt = t;
				event("set: to the press");
			}
			if (pressAt >= 0 && t == pressAt + 10) {
				// a cut to the press, close, while it works (a move would pass through the player)
				card("^CHEESE PRESS", stepLine(), "(press time sped up 8x)");
				camTo(close, true);
				event("cut: on the press");
			}
			if (pressAt >= 0 && t > pressAt + 10 && s == dev.farmings.process.creamery.Creamery.ArmorStep.PRESS) {
				// pushing in slowly as it presses
				double k = Math.min(1.0, (t - pressAt - 10) / 90.0);
				k = k * k * (3 - 2 * k);
				double[] at = new double[5];
				for (int i = 0; i < 5; i++) {
					at[i] = close[i] + (closer[i] - close[i]) * k;
				}
				camTo(at, false);
				card("^CHEESE PRESS", stepLine(), "(press time sped up 8x)");
			}
			if (s == dev.farmings.process.creamery.Creamery.ArmorStep.PRESS) {
				server.runOnServer(srv -> controller(srv.overworld()).run(srv.overworld(), 7));
			}
			if (s == dev.farmings.process.creamery.Creamery.ArmorStep.READY) {
				event("pressed: ready at the press");
				break;
			}
			context.waitTick();
		}
		card("^CHEESE PRESS", stepLine());
		context.waitTicks(20);
		camTo(take, true);
		event("cut: to the take, the press in front of the player");
		context.waitTicks(10);
		lookTo(pr[0], pr[1], 12);
		event("turned to the press");
		context.waitTicks(4);
		useOn(pressPos);
		String got = server.computeOnServer(srv -> player(srv).getMainHandItem().getHoverName().getString());
		event("use on the press, empty hand: in hand " + got);
		card("+CHEESE CHESTPLATE", "From the press");
		context.waitTicks(55);
		event("end");
		stop(f);
		playerCamera();
		card();
		closeScreen();
	}

	/** The cheese set on the player, the press's chestplate kept (or a new one), each piece's clock started now. */
	private void equip() {
		server.runOnServer(srv -> {
			ServerPlayer p = player(srv);
			ItemStack chest = ItemStack.EMPTY;
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
				if (p.getInventory().getItem(i).is(FarmingsItems.CHEESE_CHESTPLATE)) {
					chest = p.getInventory().getItem(i).copy();
				}
			}
			p.getInventory().clearContent();
			Item[] items = {FarmingsItems.CHEESE_HELMET, FarmingsItems.CHEESE_CHESTPLATE, FarmingsItems.CHEESE_LEGGINGS, FarmingsItems.CHEESE_BOOTS};
			long now = srv.overworld().getGameTime();
			for (int i = 0; i < 4; i++) {
				ItemStack st = i == 1 && !chest.isEmpty() ? chest : new ItemStack(items[i]);
				if (st != chest) {
					CheeseArmorItem.setMade(st, now);
				}
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

	/** 2. The full cheese set, third person from the front, the pasture behind: the player turns all the way round. */
	private void wear() {
		equip();
		thirdPerson();
		context.waitTicks(30);
		String name = "cheesearmor-2-wear";
		Filming f = start(name, true);
		event("start: the full cheese set, " + worn());
		context.waitTicks(30);
		event("turn starts");
		turn(0, 360, 130);
		event("turn ends");
		context.waitTicks(40);
		event("end");
		stop(f);
	}

	/** 3. A spoiled milk helmet used on the vat: refused, "Spoiled milk does not make cheese" over the hotbar. */
	private void refused() {
		firstPerson(true);
		backView();
		double[] at = vatSpot();
		float[] v = aim(at[0], at[1], vatPos.getX() + 1.0, g + 0.7, vatPos.getZ() + 0.5);
		placeAt(at[0], at[1], v[0], v[1]);
		ItemStack sour = new ItemStack(FarmingsItems.MILK_HELMET);
		MilkArmorItem.spoil(sour);
		inHand(sour);
		context.waitTicks(10);
		String name = "cheesearmor-3-refused";
		Filming f = start(name, true);
		event("start: a spoiled milk helmet in hand at the vat");
		context.waitTicks(30);
		use();
		event("use key on the vat: " + step() + " | still in hand " + server.computeOnServer(srv -> player(srv).getMainHandItem().getHoverName().getString()));
		context.waitTicks(70);
		event("end");
		stop(f);
		server.runOnServer(srv -> player(srv).getInventory().setItem(0, ItemStack.EMPTY));
	}

	/** 4. Hungry (food 1): a cheese chestplate in hand, sneak-use eats it (1.6 s) and the hunger bar fills. */
	private void eat() {
		firstPerson(true);
		server.runOnServer(srv -> {
			player(srv).getFoodData().setFoodLevel(1);
			player(srv).getFoodData().setSaturation(0);
		});
		ItemStack food = new ItemStack(FarmingsItems.CHEESE_CHESTPLATE);
		server.runOnServer(srv -> CheeseArmorItem.setMade(food, srv.overworld().getGameTime()));
		inHand(food);
		context.waitTicks(10);
		String name = "cheesearmor-4-eat";
		Filming f = start(name, true);
		event("start: hungry, " + worn());
		context.waitTicks(30);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(4);
		event("sneak, use held");
		context.getInput().holdKeyFor(o -> o.keyUse, 40);
		context.getInput().releaseKey(o -> o.keyShift);
		event("eaten: " + worn() + " | in hand " + server.computeOnServer(srv -> player(srv).getMainHandItem().isEmpty() ? "nothing" : player(srv).getMainHandItem().getHoverName().getString()));
		context.waitTicks(60);
		event("end");
		stop(f);
	}
}
