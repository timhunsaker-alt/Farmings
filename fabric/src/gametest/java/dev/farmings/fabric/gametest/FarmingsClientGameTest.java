package dev.farmings.fabric.gametest;

import dev.farmings.FarmingsBlocks;
import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.barn.BarnControllerEntity;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.DriveEntity;
import dev.farmings.barn.PitEntity;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import dev.farmings.entity.CattleEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.CattleBreed;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The cattle in a real game client: all six breeds, then calves and a bull, photographed from the side and the front. The screenshots
 * land in the run's screenshots folder ({@code ./gradlew :fabric:runClientGameTest}).
 */
public class FarmingsClientGameTest implements FabricClientGameTest {
	private static final CattleBreed[] BREEDS = {CattleBreed.HOLSTEIN, CattleBreed.JERSEY, CattleBreed.BROWN_SWISS, CattleBreed.GYR, CattleBreed.ANGUS, CattleBreed.WAGYU};

	/** Three at a time, close: first a line along z seen from the east (side view), then a row along x seen from the south (front). */
	private static void shoot(ClientGameTestContext context, TestServerContext server, CattleEntity[] herd, int y, int z0, String name) {
		for (int group = 0; group < 2; group++) {
			int first = group * 3;
			String tag = name + (group == 0 ? "-a" : "-b");
			server.runOnServer(srv -> {
				for (int i = 0; i < herd.length; i++) {          // everyone not in this shot waits far to the east
					herd[i].setPos(60.5, y, z0 + 3 * i + 0.5);
				}
				for (int i = 0; i < 3; i++) {
					herd[first + i].setPos(0.5, y, z0 + 3 * i + 0.5);
					herd[first + i].setYRot(0);
					herd[first + i].setYHeadRot(0);
				}
			});
			server.runCommand("tp @p 6.5 " + (y + 1.6) + " " + (z0 + 3.5) + " 90 2");
			context.waitTicks(30);
			context.takeScreenshot("farmings-" + tag + "-side");
			server.runOnServer(srv -> {
				for (int i = 0; i < 3; i++) {
					herd[first + i].setPos(3 * i - 3 + 0.5, y, z0 + 10.5);
				}
			});
			server.runCommand("tp @p 0.5 " + (y + 1.5) + " " + (z0 + 15.5) + " 180 4");
			context.waitTicks(30);
			context.takeScreenshot("farmings-" + tag + "-front");
		}
	}

	private static void barnPart(ServerLevel level, BarnParts kind, BlockPos main) {
		BlockState state = FarmingsBlocks.part(kind).defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
	}

	/** The same barn the NeoForge game test builds: alley east-west, pit at the east end, controller at c. */
	private static void buildBarn(ServerLevel level, BlockPos c, int stalls) {
		int fl = c.getY() - 1;
		int xd = c.getX() - dev.farmings.husbandry.barn.BarnLayout.alleyLength(stalls);
		int row = xd + stalls;
		barnPart(level, BarnParts.COLLECTION_PIT, new BlockPos(c.getX(), fl - 1, c.getZ()));
		level.setBlock(c, FarmingsBlocks.BARN_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
		barnPart(level, BarnParts.SCRAPER_DRIVE, new BlockPos(xd, c.getY(), c.getZ()));
		for (int x = xd; x < c.getX(); x++) {
			barnPart(level, BarnParts.SCRAPER_RAIL, new BlockPos(x, fl, c.getZ()));
			barnPart(level, BarnParts.ALLEY_FLOOR, new BlockPos(x, fl, c.getZ() - 1));
			barnPart(level, BarnParts.ALLEY_FLOOR, new BlockPos(x, fl, c.getZ() + 1));
		}
		barnPart(level, BarnParts.MANURE_SCRAPER, new BlockPos(xd + 1, c.getY(), c.getZ()));
		for (int dir = -1; dir <= 1; dir += 2) {
			int firstZ = dir < 0 ? c.getZ() - 4 : c.getZ() + 2;
			for (int j = 0; j <= stalls; j++) {
				barnPart(level, BarnParts.STALL_DIVIDER, new BlockPos(row + 2 * j, fl, firstZ));
			}
			for (int i = 0; i < stalls; i++) {
				int x = row + 1 + 2 * i;
				barnPart(level, BarnParts.STALL_BED, new BlockPos(x, fl, firstZ));
				barnPart(level, BarnParts.FEED_BUNK, new BlockPos(x, fl, c.getZ() + dir * 5));
				barnPart(level, BarnParts.HAY_FEEDER, new BlockPos(x, fl + 1, c.getZ() + dir * 5));
			}
			barnPart(level, BarnParts.WATER_TROUGH, new BlockPos(c.getX() + 1, fl, c.getZ() + dir * 2));
		}
	}

	/** A 4-stall barn (8 cows) on grass, photographed from above, from the alley, and with one divider missing. */
	private static void barn(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 60;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = cx - 16; x <= cx + 6; x++) {
				for (int z = cz - 10; z <= cz + 10; z++) {
					level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
					for (int h = 0; h < 6; h++) {
						level.setBlockAndUpdate(new BlockPos(x, y + h, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
					}
				}
			}
			buildBarn(level, new BlockPos(cx, y + 1, cz), 4);
		});
		context.waitTicks(40);
		server.runCommand("tp @p " + (cx - 6) + " " + (y + 9) + " " + (cz + 11) + " 180 50");
		context.waitTicks(40);
		context.takeScreenshot("farmings-barn-overview");
		server.runCommand("tp @p " + (cx + 3) + " " + (y + 3) + " " + cz + " 90 12");
		context.waitTicks(40);
		context.takeScreenshot("farmings-barn-alley");
		server.runCommand("tp @p " + (cx - 13) + " " + (y + 4) + " " + (cz - 8) + " -50 25");
		context.waitTicks(40);
		context.takeScreenshot("farmings-barn-corner");
		// one divider gone: the status the controller would show is checked by the NeoForge game test; here, how it looks
		server.runOnServer(srv -> srv.overworld().destroyBlock(new BlockPos(cx - 9 + 4, y, cz - 4), false));
		context.waitTicks(20);
		server.runCommand("tp @p " + (cx - 6) + " " + (y + 9) + " " + (cz + 11) + " 180 50");
		context.waitTicks(40);
		context.takeScreenshot("farmings-barn-missing-divider");
	}

	/** A powered scraper pushes manure along a 4-stall barn's alley into the pit. Filmed, with screenshots before, during and after. */
	private static void scraper(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 60;
		BlockPos controller = new BlockPos(cx, y + 1, cz);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = cx - 16; x <= cx + 6; x++) {
				for (int z = cz - 10; z <= cz + 10; z++) {
					level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
					for (int h = 0; h < 6; h++) {
						level.setBlockAndUpdate(new BlockPos(x, y + h, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
					}
				}
			}
			buildBarn(level, controller, 4);
			((DriveEntity) BigParts.entity(level, new BlockPos(cx - 9, y + 1, cz))).forceRunning(true);
			int[][] spots = {{-7, 0}, {-6, -1}, {-5, 1}, {-5, 0}, {-4, -1}, {-3, 1}, {-3, 0}, {-2, -1}, {-2, 1}};
			for (int[] s : spots) {
				ItemEntity e = new ItemEntity(level, cx + s[0] + 0.5, y + 0.3, cz + s[1] + 0.5, new ItemStack(FarmingsItems.MANURE, 8));
				e.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
				level.addFreshEntity(e);
			}
		});
		context.waitTicks(40);
		server.runCommand("tp @p " + (cx + 5) + " " + (y + 6) + " " + cz + " 90 40");
		context.waitTicks(40);
		Filming film = context.computeOnClient(client -> Filming.start(client, "scraper-pass"));
		context.waitTicks(30);
		context.takeScreenshot("farmings-scraper-before");
		server.runOnServer(srv -> ((BarnControllerEntity) srv.overworld().getBlockEntity(controller)).startPass(srv.overworld()));
		for (int i = 0; i < 22; i++) {
			context.waitTicks(20);
			String text = server.computeOnServer(srv -> {
				BarnControllerEntity c = (BarnControllerEntity) srv.overworld().getBlockEntity(controller);
				return c.readout() + "   |   pit " + c.pit(srv.overworld()).manure() + " / " + PitEntity.CAPACITY;
			});
			server.runCommand("title @p actionbar \"" + text + "\"");
			if (i == 3) {
				context.takeScreenshot("farmings-scraper-pushing");
			}
			if (i == 11) {
				context.takeScreenshot("farmings-scraper-returning");
			}
		}
		context.takeScreenshot("farmings-scraper-after");
		film.stop();
	}

	/** Six cows let into a 4-stall barn with hay and water: they take stalls and walk to them. Filmed, with screenshots. */
	private static void herd(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 60;
		BlockPos controller = new BlockPos(cx, y + 1, cz);
		CattleEntity[] cows = new CattleEntity[6];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = cx - 16; x <= cx + 6; x++) {
				for (int z = cz - 10; z <= cz + 10; z++) {
					level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
					for (int h = 0; h < 6; h++) {
						level.setBlockAndUpdate(new BlockPos(x, y + h, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
					}
				}
			}
			buildBarn(level, controller, 4);
			for (int dir = -1; dir <= 1; dir += 2) {
				((TroughEntity) BigParts.entity(level, new BlockPos(cx + 1, y, cz + dir * 2))).pourBucket();
				for (int i = 0; i < 4; i++) {
					FeederEntity f = (FeederEntity) BigParts.entity(level, new BlockPos(cx - 8 + 2 * i, y + 1, cz + dir * 5));
					f.addBlock();
					f.addBlock();
				}
			}
			CattleBreed[] breeds = {CattleBreed.HOLSTEIN, CattleBreed.JERSEY, CattleBreed.BROWN_SWISS, CattleBreed.GYR, CattleBreed.HOLSTEIN, CattleBreed.JERSEY};
			double[][] spots = {{-2.5, 0.5}, {-4.5, -0.5}, {-6.5, 0.5}, {2.5, -2.5}, {3.5, 2.5}, {-3.5, 0.5}};
			for (int i = 0; i < cows.length; i++) {
				CattleEntity c = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
				c.setBreedForDisplay(breeds[i], Sex.FEMALE, breeds[i].matureWeightKg, true);
				c.setPos(cx + spots[i][0] + 0.5, y + 0.2, cz + spots[i][1] + 0.5);
				cows[i] = c;
				level.addFreshEntity(c);
			}
		});
		context.waitTicks(40);
		server.runCommand("tp @p " + (cx + 5) + " " + (y + 6) + " " + cz + " 90 40");
		context.waitTicks(20);
		Filming film = context.computeOnClient(client -> Filming.start(client, "cows-take-stalls"));
		context.waitTicks(20);
		context.takeScreenshot("farmings-herd-start");
		for (int i = 0; i < 24; i++) {
			context.waitTicks(20);
			String text = server.computeOnServer(srv -> {
				BarnControllerEntity c = (BarnControllerEntity) srv.overworld().getBlockEntity(controller);
				return c.readout();
			});
			server.runCommand("title @p actionbar \"" + text + "\"");
			if (i == 4) {
				context.takeScreenshot("farmings-herd-walking");
			}
		}
		context.takeScreenshot("farmings-herd-in-stalls");
		server.runCommand("tp @p " + (cx - 12) + " " + (y + 4) + " " + (cz - 9) + " -50 28");
		context.waitTicks(40);
		context.takeScreenshot("farmings-herd-in-stalls-corner");
		film.stop();
	}

	private static void parlorPart(ServerLevel level, dev.farmings.parlor.ParlorParts kind, BlockPos main) {
		BlockState state = FarmingsBlocks.parlorPart(kind).defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
	}

	/** Four cows 30 days in milk walk into a 4-stall parlor and are milked into the bulk tank. Filmed, with screenshots. */
	/**
	 * The whole dairy (docs/multiblocks/parlor.md, "The cow lane and holding pen"): a 4-stall barn with the doubled alley, a cow lane from
	 * its pit-end open stretch north to a holding pen, and a 4-stall parlor. Four milking Holsteins take stalls, are called along the lane,
	 * wait in the pen, are milked and walk home, on their own AI. Filmed from above.
	 */
	private static void dairy(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 70;
		BlockPos c = new BlockPos(cx, y + 1, cz);                 // the barn controller
		BlockPos p = new BlockPos(cx, y + 1, cz - 13);            // the parlor controller
		CattleEntity[] cows = new CattleEntity[4];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, cx - 10, cz - 6, y, 14, 12, 7);
			for (int x = cx - 24; x <= cx + 4; x++) {
				for (int z = cz - 18; z <= cz + 6; z++) {
					boolean pit = x >= cx && x <= cx + 1 && z >= cz - 1 && z <= cz + 1;
					if (!pit) {
						level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
					}
				}
			}
			buildBarn(level, c, 4);
			int fl = y;
			level.setBlock(p, FarmingsBlocks.PARLOR_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
			parlorPart(level, dev.farmings.parlor.ParlorParts.BULK_TANK, new BlockPos(p.getX() + 1, fl, p.getZ()));
			int xp = p.getX() - 5;
			parlorPart(level, dev.farmings.parlor.ParlorParts.VACUUM_PUMP, new BlockPos(xp, fl, p.getZ()));
			for (int x = xp + 1; x <= p.getX(); x++) {
				parlorPart(level, dev.farmings.parlor.ParlorParts.MILK_LINE, new BlockPos(x, fl, p.getZ()));
			}
			for (int x = xp + 1; x < p.getX(); x++) {
				parlorPart(level, dev.farmings.parlor.ParlorParts.MILKING_UNIT, new BlockPos(x, p.getY(), p.getZ()));
				parlorPart(level, dev.farmings.parlor.ParlorParts.PARLOR_STALL, new BlockPos(x, fl, p.getZ() + 1));
				parlorPart(level, dev.farmings.parlor.ParlorParts.HOLDING_PEN, new BlockPos(x, fl, p.getZ() + 3));
				parlorPart(level, dev.farmings.parlor.ParlorParts.HOLDING_PEN, new BlockPos(x, fl, p.getZ() + 4));
			}
			for (int z = p.getZ() + 5; z <= cz - 2; z++) {
				parlorPart(level, dev.farmings.parlor.ParlorParts.COW_LANE, new BlockPos(cx - 3, fl, z));
			}
			dev.farmings.barn.BarnControllerEntity barn = (dev.farmings.barn.BarnControllerEntity) level.getBlockEntity(c);
			barn.refresh(level);
			dev.farmings.parlor.ParlorControllerEntity parlor = (dev.farmings.parlor.ParlorControllerEntity) level.getBlockEntity(p);
			parlor.refresh(level);
			parlor.pump(level).forceRunning(true);
			parlor.tank(level).forceChilling(true);
			for (int i = 0; i < cows.length; i++) {
				CattleEntity cow = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
				cow.initFounder(CattleBreed.HOLSTEIN, Sex.FEMALE, 90L + i, 900, 650, level.getGameTime());
				cow.setCalvedForDisplay(30);
				cow.setPos(cx - 12 + i * 2 + 0.5, y + 0.2, cz + 0.5);
				cow.settle(level);
				cows[i] = cow;
				level.addFreshEntity(cow);
			}
			barn.tend(level, 20);
		});
		context.waitTicks(40);
		server.runCommand("tp @p " + (cx + 7) + " " + (y + 17) + " " + (cz + 5) + " 125 48");
		context.waitTicks(20);
		Filming film = context.computeOnClient(client -> Filming.start(client, "dairy-lane"));
		context.takeScreenshot("farmings-dairy-start");
		for (int i = 0; i < 45; i++) {
			context.waitTicks(20);
			String text = server.computeOnServer(srv -> {
				dev.farmings.parlor.ParlorControllerEntity ctl = (dev.farmings.parlor.ParlorControllerEntity) srv.overworld().getBlockEntity(p);
				return ctl.readout(srv.overworld());
			});
			actionbar(server, text);
			if (i == 10) {
				context.takeScreenshot("farmings-dairy-lane");
			}
			if (i == 22) {
				context.takeScreenshot("farmings-dairy-pen-and-parlor");
			}
		}
		context.takeScreenshot("farmings-dairy-home");
		film.stop();
	}

	/**
	 * The walk-in fridge (docs/multiblocks/cold-room.md) from outside and inside, with chests of dairy, and food freshness: the bars on
	 * fresh, half-gone and nearly spoiled milk, and a bottle turning spoiled in the hotbar (filmed).
	 */
	private static void fridge(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 60;
		BlockPos a = new BlockPos(cx, y + 1, cz);       // the inside's corner: 4 x 3 x 4 inside
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, cx + 2, cz + 2, y, 9, 9, 7);
			for (int x = -1; x <= 4; x++) {
				for (int yy = -1; yy <= 3; yy++) {
					for (int z = -1; z <= 4; z++) {
						boolean in = x >= 0 && x < 4 && yy >= 0 && yy < 3 && z >= 0 && z < 4;
						level.setBlockAndUpdate(a.offset(x, yy, z), in ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
								: dev.farmings.coldroom.ColdRoomBlocks.PANEL.defaultBlockState());
					}
				}
			}
			level.setBlockAndUpdate(a.offset(-1, 1, 2), dev.farmings.coldroom.ColdRoomBlocks.EVAPORATOR.defaultBlockState()
					.setValue(dev.farmings.coldroom.EvaporatorBlock.FACING, Direction.EAST));
			BlockState door = dev.farmings.coldroom.ColdRoomBlocks.DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.NORTH)
					.setValue(net.minecraft.world.level.block.DoorBlock.OPEN, true);
			level.setBlock(a.offset(1, 0, 4), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
			level.setBlock(a.offset(1, 1, 4), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
			level.setBlockAndUpdate(a.offset(-2, 0, 2), dev.farmings.coldroom.ColdRoomBlocks.CONDENSING_UNIT.defaultBlockState());
			long now = level.getGameTime();
			for (int i = 0; i < 4; i++) {
				BlockPos chest = a.offset(i, 0, 0);
				level.setBlockAndUpdate(chest, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
				var box = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chest);
				box.setItem(0, dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.BOTTLED_MILK, 12), now));
				box.setItem(1, dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.BUTTER, 20), now));
				box.setItem(2, dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.CHEDDAR_WHEEL, 4), now));
			}
			level.setBlockAndUpdate(a.offset(3, 0, 2), net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState());
			var room = (dev.farmings.coldroom.ColdRoomEntity) level.getBlockEntity(a.offset(-1, 1, 2));
			room.refresh(level);
			room.condenser(level).forceRunning(true);
			room.cool(level, now, dev.farmings.process.food.Spoilage.STEP);
		});
		context.waitTicks(40);
		String readout = server.computeOnServer(srv -> ((dev.farmings.coldroom.ColdRoomEntity) srv.overworld().getBlockEntity(a.offset(-1, 1, 2))).readout(srv.overworld()));
		actionbar(server, readout);
		server.runCommand("tp @p " + (cx + 7.5) + " " + (y + 4) + " " + (cz + 9.5) + " 145 20");
		context.waitTicks(40);
		context.takeScreenshot("farmings-fridge-outside");
		server.runCommand("tp @p " + (cx + 3.5) + " " + (y + 2.6) + " " + (cz + 3.5) + " 135 25");
		actionbar(server, readout);
		context.waitTicks(40);
		context.takeScreenshot("farmings-fridge-inside");
		// freshness bars in the hotbar: fresh, half gone, nearly spoiled, whey, and the four spoiled foods
		server.runCommand("gamemode survival @p");
		server.runOnServer(srv -> {
			var player = srv.getPlayerList().getPlayers().get(0);
			long now = srv.overworld().getGameTime();
			player.getInventory().clearContent();
			long room = dev.farmings.process.food.Spoilage.Food.MILK.roomTicks();
			ItemStack fresh = new ItemStack(FarmingsItems.BOTTLED_MILK, 4);
			dev.farmings.item.Perishables.setSpoilsAt(fresh, now + room);
			ItemStack half = new ItemStack(FarmingsItems.BOTTLED_MILK, 4);
			dev.farmings.item.Perishables.setSpoilsAt(half, now + room / 2);
			ItemStack last = new ItemStack(FarmingsItems.BOTTLED_MILK, 4);
			dev.farmings.item.Perishables.setSpoilsAt(last, now + 200);
			ItemStack[] bar = {fresh, half, last, dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.WHEY_BOTTLE, 2), now),
					new ItemStack(FarmingsItems.SPOILED_BOTTLE), new ItemStack(FarmingsItems.RANCID_BUTTER), new ItemStack(FarmingsItems.MOLDY_CHEESE),
					new ItemStack(FarmingsItems.SPOILED_STEAK)};
			for (int i = 0; i < bar.length; i++) {
				player.getInventory().setItem(i, bar[i]);
			}
			player.getInventory().setSelectedSlot(8);
		});
		server.runCommand("tp @p " + (cx + 7.5) + " " + (y + 1) + " " + (cz + 9.5) + " 145 0");
		actionbar(server, "fresh milk | half a day left | about to spoil | whey | spoiled bottle, rancid butter, moldy cheese, spoiled steak");
		context.waitTicks(30);
		Filming film = context.computeOnClient(client -> Filming.start(client, "milk-spoils"));
		context.takeScreenshot("farmings-food-freshness");
		context.waitTicks(260);           // the third stack runs out and turns
		actionbar(server, "the third stack has spoiled");
		context.waitTicks(40);
		context.takeScreenshot("farmings-food-spoiled");
		film.stop();
	}

	/** Hoppers on the creamery (creamery.md, "Hoppers"): butter, wheels and bottles taken into chests in front, glass fed from above. */
	private static void hoppers(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 56;
		int cz = 60;
		BlockPos c = new BlockPos(cx, y, cz);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, cx + 6, cz, y, 10, 6, 5);
			level.setBlock(c, FarmingsBlocks.CREAMERY_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
			int x = cx + 1;
			java.util.Map<dev.farmings.creamery.CreameryParts, Integer> at = new java.util.EnumMap<>(dev.farmings.creamery.CreameryParts.class);
			for (dev.farmings.creamery.CreameryParts k : dev.farmings.creamery.CreameryParts.values()) {
				placePart(level, FarmingsBlocks.creameryPart(k), new BlockPos(x, y, cz));
				at.put(k, x);
				x += k.part.length();
			}
			dev.farmings.creamery.CreameryControllerEntity ctl = (dev.farmings.creamery.CreameryControllerEntity) level.getBlockEntity(c);
			ctl.refresh(level);
			ctl.creamery().addMilk(20, 3.72, 3.06);
			for (int i = 0; i < 6; i++) {
				ctl.run(level, 24_000);
			}
			ctl.creamery().returnButter(30);
			ctl.creamery().returnWheels(3);
			for (dev.farmings.creamery.CreameryParts k : new dev.farmings.creamery.CreameryParts[] {dev.farmings.creamery.CreameryParts.BUTTER_CHURN,
					dev.farmings.creamery.CreameryParts.AGING_SHELF, dev.farmings.creamery.CreameryParts.BOTTLER}) {
				int px = at.get(k);
				// a trench in front, the hopper under the part pointing into a chest in the trench
				for (int dy = 1; dy <= 2; dy++) {
					level.setBlockAndUpdate(new BlockPos(px, y - dy, cz + 1), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				}
				level.setBlockAndUpdate(new BlockPos(px, y - 1, cz), net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState()
						.setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.SOUTH));
				level.setBlockAndUpdate(new BlockPos(px, y - 2, cz + 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(new BlockPos(px, y - 1, cz + 1), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
			}
			int bx = at.get(dev.farmings.creamery.CreameryParts.BOTTLER);
			level.setBlockAndUpdate(new BlockPos(bx, y + 1, cz), net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState());
			var top = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(new BlockPos(bx, y + 1, cz));
			top.setItem(0, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 16));
		});
		server.runCommand("tp @p " + (cx + 6) + " " + (y + 2.5) + " " + (cz + 6) + " 180 25");
		context.waitTicks(200);
		String text = server.computeOnServer(srv -> ((dev.farmings.creamery.CreameryControllerEntity) srv.overworld().getBlockEntity(c)).readout());
		actionbar(server, "hoppers took the butter, the aged wheels and the bottled milk; " + text);
		context.waitTicks(20);
		context.takeScreenshot("farmings-creamery-hoppers");
	}

	private static void calving(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 60;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = cx - 8; x <= cx + 8; x++) {
				for (int z = cz - 8; z <= cz + 8; z++) {
					level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
					for (int h = 0; h < 5; h++) {
						level.setBlockAndUpdate(new BlockPos(x, y + h, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
					}
				}
			}
			CattleEntity bull = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
			bull.initFounder(CattleBreed.HOLSTEIN, Sex.MALE, 7L, 1500, 800, level.getGameTime());
			bull.setPos(cx - 3 + 0.5, y, cz + 0.5);
			bull.setNoAi(true);
			level.addFreshEntity(bull);
			CattleEntity dam = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
			dam.initFounder(CattleBreed.HOLSTEIN, Sex.FEMALE, 8L, 500, 400, level.getGameTime());
			dam.setPos(cx + 0.5, y, cz + 0.5);
			dam.setNoAi(true);
			dam.setOwner(java.util.UUID.randomUUID());
			level.addFreshEntity(dam);
			dam.startHeatForDisplay();
			dam.conceive(bull.genome(), level.getGameTime());
		});
		context.waitTicks(40);
		server.runCommand("tp @p " + (cx + 1) + " " + (y + 3) + " " + (cz + 5) + " 180 30");
		context.waitTicks(20);
		Filming film = context.computeOnClient(client -> Filming.start(client, "calving"));
		context.waitTicks(40);
		context.takeScreenshot("farmings-calving-before");
		server.runOnServer(srv -> {
			for (CattleEntity c : srv.overworld().getEntitiesOfClass(CattleEntity.class, new net.minecraft.world.phys.AABB(cx - 5, y - 1, cz - 5, cx + 5, y + 4, cz + 5))) {
				if (c.isInCalf()) {
					c.makeDueNow();
					c.settle(srv.overworld());
				}
			}
		});
		context.waitTicks(40);
		context.takeScreenshot("farmings-calving-after");
		context.waitTicks(60);
		film.stop();
	}

	// ---- 2026-11-09: the cattle finish, the creamery and the art ----

	/** A big part placed whole with its main cell at {@code main}, facing north. */
	private static void bigPart(ServerLevel level, net.minecraft.world.level.block.Block b, BlockPos main) {
		var state = b.defaultBlockState().setValue(dev.wildcatter.block.BigPartBlock.FACING, Direction.NORTH);
		level.setBlockAndUpdate(main, state);
		dev.wildcatter.multiblock.BigParts.grow(level, main, state);
	}

	private static void clear(ServerLevel level, int cx, int cz, int y, int rx, int rz, int h) {
		for (int x = cx - rx; x <= cx + rx; x++) {
			for (int z = cz - rz; z <= cz + rz; z++) {
				level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
				for (int k = 0; k < h; k++) {
					level.setBlockAndUpdate(new BlockPos(x, y + k, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	private static void placePart(ServerLevel level, Block block, BlockPos main) {
		BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
	}

	private static void actionbar(TestServerContext server, String text) {
		server.runCommand("title @p actionbar \"" + text.replace("\"", "'") + "\"");
	}

	/** Every block, close up, in three groups (barn, parlor, creamery), and every item in item frames on a wall. */
	private static void showroom(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 60;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, cx, cz, y, 24, 14, 8);
			int x = cx - 20;
			for (BarnParts k : BarnParts.values()) {
				placePart(level, FarmingsBlocks.part(k), new BlockPos(x, y, cz + 1));
				x += k == BarnParts.WATER_TROUGH || k == BarnParts.COLLECTION_PIT ? 3 : 2;
			}
			level.setBlockAndUpdate(new BlockPos(x, y, cz + 1), FarmingsBlocks.BARN_CONTROLLER.defaultBlockState());
			x = cx - 20;
			for (dev.farmings.parlor.ParlorParts k : dev.farmings.parlor.ParlorParts.values()) {
				placePart(level, FarmingsBlocks.parlorPart(k), new BlockPos(x, y, cz - 4));
				x += k == dev.farmings.parlor.ParlorParts.BULK_TANK ? 3 : 2;
			}
			level.setBlockAndUpdate(new BlockPos(x, y, cz - 4), FarmingsBlocks.PARLOR_CONTROLLER.defaultBlockState());
			x = cx - 20;
			for (dev.farmings.creamery.CreameryParts k : dev.farmings.creamery.CreameryParts.values()) {
				placePart(level, FarmingsBlocks.creameryPart(k), new BlockPos(x, y, cz - 9));
				x += k.part.length() + 1;
			}
			level.setBlockAndUpdate(new BlockPos(x, y, cz - 9), FarmingsBlocks.CREAMERY_CONTROLLER.defaultBlockState());
			// the item wall, facing south
			int wz = cz + 8;
			for (int wx = cx - 6; wx <= cx + 6; wx++) {
				for (int wy = y; wy < y + 4; wy++) {
					level.setBlockAndUpdate(new BlockPos(wx, wy, wz), net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS.defaultBlockState());
				}
			}
			int i = 0;
			java.util.List<net.minecraft.world.item.Item> items = new java.util.ArrayList<>(FarmingsItems.ALL.values());
			for (net.minecraft.world.item.Item item : items) {
				BlockPos at = new BlockPos(cx - 5 + i % 11, y + 3 - i / 11, wz - 1);
				net.minecraft.world.entity.decoration.ItemFrame frame = new net.minecraft.world.entity.decoration.ItemFrame(level, at, Direction.NORTH);
				ItemStack stack = new ItemStack(item);
				if (item instanceof dev.farmings.item.MilkArmorItem && i % 2 == 1) {
					dev.farmings.item.MilkArmorItem.spoil(stack);
				}
				frame.setItem(stack, false);
				level.addFreshEntity(frame);
				i++;
			}
		});
		context.waitTicks(80);
		String[][] shots = {{"barn-a", (cx - 17) + " " + (y + 2.5) + " " + (cz + 6.5) + " 180 25"}, {"barn-b", (cx - 5) + " " + (y + 2.5) + " " + (cz + 6.5) + " 180 25"},
				{"parlor", (cx - 15) + " " + (y + 3) + " " + (cz + 0.5) + " 180 30"}, {"creamery-a", (cx - 15) + " " + (y + 3) + " " + (cz - 3.5) + " 180 30"},
				{"creamery-b", (cx - 4) + " " + (y + 3) + " " + (cz - 3.5) + " 180 30"}, {"items", (cx + 0.5) + " " + (y + 2) + " " + (cz + 2.5) + " 0 0"}};
		for (String[] shot : shots) {
			server.runCommand("tp @p " + shot[1]);
			context.waitTicks(40);
			context.takeScreenshot("farmings-art-" + shot[0]);
		}
	}

	private static CattleEntity founder(ServerLevel level, CattleBreed breed, Sex sex, long seed, double age, double kg, double x, int y, double z) {
		CattleEntity c = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
		c.initFounder(breed, sex, seed, age, kg, level.getGameTime());
		c.setPos(x, y, z);
		c.setNoAi(true);
		level.addFreshEntity(c);
		return c;
	}

	private static dev.farmings.genetics.VisualGenome withCoat(CattleEntity c, dev.farmings.genetics.VisualLocus l, int a, int b) {
		java.util.EnumMap<dev.farmings.genetics.VisualLocus, int[]> m = new java.util.EnumMap<>(c.visualGenome().alleles());
		m.put(l, new int[] {a, b});
		return new dev.farmings.genetics.VisualGenome(m);
	}

	/** Bred and calved now; the calf, grown to {@code ageDays} if that is above zero. */
	private static CattleEntity crossOf(ServerLevel level, CattleEntity dam, CattleEntity sire, double ageDays) {
		dam.forceCalvingForTest(1, false, false);
		dam.startHeatForDisplay();
		dam.conceive(sire.genome(), sire.visualGenome(), level.getGameTime());
		java.util.Set<java.util.UUID> before = new java.util.HashSet<>();
		for (CattleEntity e : level.getEntitiesOfClass(CattleEntity.class, dam.getBoundingBox().inflate(6))) {
			before.add(e.getUUID());
		}
		dam.makeDueNow();
		dam.settle(level);
		for (CattleEntity e : level.getEntitiesOfClass(CattleEntity.class, dam.getBoundingBox().inflate(6))) {
			if (!before.contains(e.getUUID())) {
				e.setNoAi(true);
				if (ageDays > 0) {
					e.settle(level);              // growth counts from the first settle
					e.advanceForTest(ageDays);
					e.settle(level);
				}
				return e;
			}
		}
		return null;
	}

	/**
	 * The coat lineup (docs/animals/cattle.md, "Models and coats"): a cow of each breed, a red Holstein, a Holstein bull and a Gyr bull, three
	 * F1 crosses grown up, and two calves; side views in groups of four, and a front view of the horns, ears and hump.
	 */
	private static void coats(ClientGameTestContext context, TestServerContext server, int y) {
		java.util.List<CattleEntity> line = new java.util.ArrayList<>();
		java.util.List<String> names = new java.util.ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 0, 40, y, 16, 34, 6);
			for (int x = -16; x <= 16; x++) {
				for (int z = 6; z <= 80; z++) {
					level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
				}
			}
			long seed = 300;
			for (CattleBreed b : CattleBreed.values()) {
				CattleEntity c = founder(level, b, Sex.FEMALE, seed++, 1500, b.matureWeightKg, 60.5, y, 60.5);
				c.setCalvedForDisplay(30);
				c.settle(level);
				line.add(c);
				names.add(b.name().toLowerCase(java.util.Locale.ROOT));
			}
			CattleEntity red = founder(level, CattleBreed.HOLSTEIN, Sex.FEMALE, seed++, 1500, 680, 60.5, y, 60.5);
			red.setVisualGenome(withCoat(red, dev.farmings.genetics.VisualLocus.EXTENSION, 3, 3));
			red.setCalvedForDisplay(30);
			red.settle(level);
			line.add(red);
			names.add("red holstein");
			CattleEntity holBull = founder(level, CattleBreed.HOLSTEIN, Sex.MALE, seed++, 1500, 1000, 60.5, y, 60.5);
			line.add(holBull);
			names.add("holstein bull");
			CattleEntity gyrBull = founder(level, CattleBreed.GYR, Sex.MALE, seed++, 1500, 700, 60.5, y, 60.5);
			line.add(gyrBull);
			names.add("gyr bull");
			CattleEntity angBull = founder(level, CattleBreed.ANGUS, Sex.MALE, seed++, 1500, 900, 60.5, y, 60.5);
			CattleEntity jerBull = founder(level, CattleBreed.JERSEY, Sex.MALE, seed++, 1500, 700, 60.5, y, 60.5);
			CattleEntity[] dams = new CattleEntity[3];
			for (int i = 0; i < 3; i++) {
				dams[i] = founder(level, i == 2 ? CattleBreed.GYR : CattleBreed.HOLSTEIN, Sex.FEMALE, seed++, 1500, 650, 60.5, y, 60.5);
			}
			Object[][] crosses = {{dams[0], angBull, "holstein x angus"}, {dams[1], jerBull, "holstein x jersey"}, {dams[2], holBull, "gyr x holstein"}};
			for (Object[] x : crosses) {
				CattleEntity f1 = crossOf(level, (CattleEntity) x[0], (CattleEntity) x[1], 900);
				if (f1 != null) {
					line.add(f1);
					names.add((String) x[2] + " F1");
				}
			}
			CattleEntity calf = crossOf(level, line.get(0), holBull, 0);
			if (calf != null) {
				line.add(calf);
				names.add("holstein calf");
			}
			for (CattleEntity d : dams) {
				d.discard();
			}
			angBull.discard();
			jerBull.discard();
			for (int i = 0; i < line.size(); i++) {
				line.get(i).setPos(0.5, y, 10.5 + 4 * i);
				line.get(i).setYRot(0);
				line.get(i).setYHeadRot(0);
				line.get(i).setYBodyRot(0);
			}
		});
		context.waitTicks(60);
		// one at a time, a three-quarter view from the front left, everyone else parked out of sight
		for (int i = 0; i < line.size(); i++) {
			int shown = i;
			server.runOnServer(srv -> {
				for (int j = 0; j < line.size(); j++) {
					CattleEntity c = line.get(j);
					c.setPos(j == shown ? 0.5 : 200.5 + 4 * j, y, j == shown ? 20.5 : 200.5);
					c.setYRot(0);
					c.setYHeadRot(0);
					c.setYBodyRot(0);
				}
			});
			actionbar(server, names.get(i) + ": " + line.get(i).look().describe());
			server.runCommand("tp @p 2.9 " + (y + 2.4) + " 22.9 135 32");
			context.waitTicks(30);
			context.takeScreenshot("farmings-coat-" + String.format(java.util.Locale.ROOT, "%02d", i) + "-" + names.get(i).replace(' ', '-'));
		}
	}

	private static dev.farmings.entity.SheepEntity sheep(ServerLevel level, dev.farmings.husbandry.SheepBreed breed, Sex sex, long seed, double age) {
		dev.farmings.entity.SheepEntity s = FarmingsEntities.SHEEP.create(level, EntitySpawnReason.COMMAND);
		java.util.Map<String, Integer> c = java.util.Map.of(breed.code, 10_000);
		s.initFounder(breed, sex, seed, age, dev.farmings.husbandry.Flock.targetKg(c, age), level.getGameTime());
		s.setPos(200.5, 0, 200.5);
		s.setNoAi(true);
		level.addFreshEntity(s);
		return s;
	}

	/** A cross of two sheep, made directly from the genetics (as a lambing does) and grown to {@code age} real days. */
	private static dev.farmings.entity.SheepEntity sheepCross(ServerLevel level, dev.farmings.entity.SheepEntity dam, dev.farmings.entity.SheepEntity sire, long seed, double age) {
		dev.farmings.genetics.Offspring.Result r = dev.farmings.genetics.Offspring.create(dam.genome(), sire.genome(), "scene-" + seed, "scene",
				dev.farmings.genetics.Ruleset.defaults(), Sex.FEMALE);
		dev.farmings.entity.SheepEntity s = FarmingsEntities.SHEEP.create(level, EntitySpawnReason.COMMAND);
		s.initLamb(r, dev.farmings.genetics.SheepGenes.inherit(dam.genes(), sire.genes(), seed), level.getGameTime());
		s.advanceForTest(age);
		s.setWeightKg(dev.farmings.husbandry.Flock.targetKg(r.genome().breedCompositionBp(), age));
		s.setPos(200.5, 0, 200.5);
		s.setNoAi(true);
		level.addFreshEntity(s);
		s.settle(level);
		return s;
	}

	/**
	 * The flock lineup (docs/animals/sheep.md, "Coat genes"): a ewe of each breed with a year's fleece, a Merino ram, a just-shorn Merino,
	 * three F1 crosses, a lamb, and the recessive colours (black, brown, a spotted black); one at a time, a three-quarter view, then a group.
	 */
	private static void flock(ClientGameTestContext context, TestServerContext server, int y) {
		java.util.List<dev.farmings.entity.SheepEntity> line = new java.util.ArrayList<>();
		java.util.List<String> names = new java.util.ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 0, 20, y, 16, 16, 6);
			long seed = 900;
			for (dev.farmings.husbandry.SheepBreed b : dev.farmings.husbandry.SheepBreed.values()) {
				dev.farmings.entity.SheepEntity s = sheep(level, b, Sex.FEMALE, seed++, 900);
				s.setFleeceDaysForTest(365);
				if (b == dev.farmings.husbandry.SheepBreed.EAST_FRIESIAN) {
					s.setLambedForTest(20, 2);
				}
				line.add(s);
				names.add(b.label().toLowerCase(java.util.Locale.ROOT) + " ewe");
			}
			dev.farmings.entity.SheepEntity ram = sheep(level, dev.farmings.husbandry.SheepBreed.MERINO, Sex.MALE, seed++, 900);
			ram.setFleeceDaysForTest(365);
			line.add(ram);
			names.add("merino ram");
			dev.farmings.entity.SheepEntity shorn = sheep(level, dev.farmings.husbandry.SheepBreed.MERINO, Sex.FEMALE, seed++, 900);
			shorn.setFleeceDaysForTest(0);
			line.add(shorn);
			names.add("merino just shorn");
			dev.farmings.entity.SheepEntity sufRam = sheep(level, dev.farmings.husbandry.SheepBreed.SUFFOLK, Sex.MALE, seed++, 900);
			dev.farmings.entity.SheepEntity dorRam = sheep(level, dev.farmings.husbandry.SheepBreed.DORPER, Sex.MALE, seed++, 900);
			Object[][] crosses = {{line.get(0), sufRam, "merino x suffolk F1"}, {line.get(3), sufRam, "east friesian x suffolk F1"}, {line.get(0), dorRam, "merino x dorper F1"}};
			for (Object[] x : crosses) {
				dev.farmings.entity.SheepEntity f1 = sheepCross(level, (dev.farmings.entity.SheepEntity) x[0], (dev.farmings.entity.SheepEntity) x[1], seed++, 500);
				f1.setFleeceDaysForTest(300);
				line.add(f1);
				names.add((String) x[2]);
			}
			dev.farmings.entity.SheepEntity lamb = sheepCross(level, line.get(1), sufRam, seed++, 40);
			line.add(lamb);
			names.add("suffolk lamb");
			Object[][] colours = {{1, 0, 0, "black (non-agouti)"}, {1, 1, 0, "brown (moorit)"}, {1, 0, 1, "spotted black"}};
			for (Object[] col : colours) {
				dev.farmings.entity.SheepEntity s = sheep(level, dev.farmings.husbandry.SheepBreed.MERINO, Sex.FEMALE, seed++, 600);
				java.util.EnumMap<dev.farmings.genetics.SheepLocus, int[]> m = new java.util.EnumMap<>(s.genes().alleles());
				m.put(dev.farmings.genetics.SheepLocus.AGOUTI, new int[] {(Integer) col[0], (Integer) col[0]});
				m.put(dev.farmings.genetics.SheepLocus.BROWN, new int[] {(Integer) col[1], (Integer) col[1]});
				m.put(dev.farmings.genetics.SheepLocus.SPOTTING, new int[] {(Integer) col[2], 0});
				m.put(dev.farmings.genetics.SheepLocus.WHITE, new int[] {500, 500});
				s.setGenesForTest(new dev.farmings.genetics.SheepGenes(m));
				s.setFleeceDaysForTest(365);
				line.add(s);
				names.add((String) col[3]);
			}
			sufRam.discard();
			dorRam.discard();
		});
		context.waitTicks(60);
		for (int i = 0; i < line.size(); i++) {
			int shown = i;
			server.runOnServer(srv -> {
				for (int j = 0; j < line.size(); j++) {
					dev.farmings.entity.SheepEntity s = line.get(j);
					s.setPos(j == shown ? 0.5 : 200.5 + 4 * j, y, j == shown ? 20.5 : 200.5);
					s.setYRot(0);
					s.setYHeadRot(0);
					s.setYBodyRot(0);
				}
			});
			actionbar(server, names.get(i) + ": " + line.get(i).look().describe());
			server.runCommand("tp @p 2.1 " + (y - 0.3) + " 22.1 135 18");
			context.waitTicks(30);
			context.takeScreenshot("farmings-flock-" + String.format(java.util.Locale.ROOT, "%02d", i) + "-" + names.get(i).replace(' ', '-').replace("(", "").replace(")", ""));
		}
		// everyone in a row, side on
		server.runOnServer(srv -> {
			for (int j = 0; j < line.size(); j++) {
				dev.farmings.entity.SheepEntity s = line.get(j);
				s.setPos(-13.5 + 2.2 * j, y, 20.5);
				s.setYRot(90);
				s.setYHeadRot(90);
				s.setYBodyRot(90);
			}
		});
		actionbar(server, "the flock: breeds, ram, shorn, crosses, lamb, colours");
		server.runCommand("tp @p 0.5 " + (y + 4.5) + " 32 180 18");
		context.waitTicks(40);
		context.takeScreenshot("farmings-flock-all");
	}

	/** A heifer in heat (hearts), twins born to a cow, the readout of a calf, and the herd book open on its page. */
	private static void breeding(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 60;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, cx, cz, y, 10, 10, 6);
			CattleEntity heifer = founder(level, CattleBreed.JERSEY, Sex.FEMALE, 21L, 500, 280, cx - 3.5, y, cz + 0.5);
			heifer.setNoAi(false);
			heifer.startHeatForDisplay();
			CattleEntity bull = founder(level, CattleBreed.ANGUS, Sex.MALE, 22L, 1500, 550, cx + 6.5, y, cz - 3.5);
			CattleEntity dam = founder(level, CattleBreed.HOLSTEIN, Sex.FEMALE, 23L, 900, 650, cx + 1.5, y, cz + 0.5);
			dam.setOwner(java.util.UUID.randomUUID());
			dam.startHeatForDisplay();
			dam.conceive(bull.genome(), level.getGameTime());
			dam.forceCalvingForTest(2, false, false);
		});
		context.waitTicks(60);
		server.runCommand("tp @p " + (cx - 1) + " " + (y + 2.5) + " " + (cz + 6) + " 180 20");
		Filming film = context.computeOnClient(client -> Filming.start(client, "heat-and-twins"));
		context.waitTicks(60);
		context.takeScreenshot("farmings-breeding-heat");
		server.runOnServer(srv -> {
			for (CattleEntity c : srv.overworld().getEntitiesOfClass(CattleEntity.class, new net.minecraft.world.phys.AABB(cx - 8, y - 1, cz - 8, cx + 8, y + 4, cz + 8))) {
				if (c.isInCalf()) {
					c.makeDueNow();
					c.settle(srv.overworld());
				}
			}
		});
		context.waitTicks(60);
		context.takeScreenshot("farmings-breeding-twins");
		film.stop();
		String[] read = new String[1];
		ItemStack[] book = new ItemStack[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			java.util.List<CattleEntity> calves = level.getEntitiesOfClass(CattleEntity.class, new net.minecraft.world.phys.AABB(cx - 8, y - 1, cz - 8, cx + 8, y + 4, cz + 8), CattleEntity::isBaby);
			CattleEntity calf = calves.get(0);
			read[0] = calf.readout();
			ItemStack b = new ItemStack(FarmingsItems.HERD_BOOK);
			for (CattleEntity c : calves) {
				dev.farmings.item.HerdBookItem.record(b, c);
			}
			book[0] = b;
			for (net.minecraft.server.level.ServerPlayer p : srv.getPlayerList().getPlayers()) {
				p.sendSystemMessage(net.minecraft.network.chat.Component.literal(read[0]));
			}
		});
		context.waitTicks(20);
		context.takeScreenshot("farmings-breeding-readout");
		context.runOnClient(client -> client.setScreenAndShow(new net.minecraft.client.gui.screens.inventory.BookViewScreen(
				net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess.fromItem(book[0]))));
		context.waitTicks(20);
		context.takeScreenshot("farmings-breeding-herd-book");
		context.runOnClient(client -> client.setScreenAndShow(null));
	}

	/** An Angus steer and a Wagyu steer killed side by side: their beef, graded steaks and leather on the ground. */
	private static void beef(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 60;
		String[] counts = new String[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, cx, cz, y, 8, 8, 6);
			founder(level, CattleBreed.ANGUS, Sex.MALE, 31L, 700, 550, cx - 2.5, y, cz + 0.5);
			founder(level, CattleBreed.WAGYU, Sex.MALE, 32L, 700, 487, cx + 3.5, y, cz + 0.5);
		});
		context.waitTicks(40);
		server.runCommand("tp @p " + (cx + 0.5) + " " + (y + 3) + " " + (cz + 5) + " 180 35");
		context.waitTicks(40);
		context.takeScreenshot("farmings-beef-alive");
		server.runOnServer(srv -> {
			StringBuilder b = new StringBuilder();
			for (CattleEntity c : srv.overworld().getEntitiesOfClass(CattleEntity.class, new net.minecraft.world.phys.AABB(cx - 8, y - 1, cz - 8, cx + 8, y + 4, cz + 8))) {
				dev.farmings.husbandry.Carcass.Yield yv = c.carcass();
				b.append(c.breed().assetName()).append(": ").append(yv.beefItems()).append(" beef, ").append(yv.steakItems()).append(' ')
						.append(yv.grade().name().toLowerCase(java.util.Locale.ROOT)).append(" steaks; ");
				c.kill(srv.overworld());
			}
			counts[0] = b.toString();
		});
		actionbar(server, counts[0]);
		context.waitTicks(40);
		context.takeScreenshot("farmings-beef-drops");
	}

	/** A creamery fed 200 L of Jersey milk makes butter; then cheese; the readout runs in the action bar. Filmed. */
	private static void creamery(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 56;
		int cz = 60;
		BlockPos c = new BlockPos(cx, y, cz);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, cx + 6, cz, y, 10, 6, 5);
			level.setBlock(c, FarmingsBlocks.CREAMERY_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
			int x = cx + 1;
			for (dev.farmings.creamery.CreameryParts k : dev.farmings.creamery.CreameryParts.values()) {
				placePart(level, FarmingsBlocks.creameryPart(k), new BlockPos(x, y, cz));
				x += k.part.length();
			}
			dev.farmings.creamery.CreameryControllerEntity ctl = (dev.farmings.creamery.CreameryControllerEntity) level.getBlockEntity(c);
			ctl.refresh(level);
			for (dev.farmings.process.creamery.CreameryPart p : new dev.farmings.process.creamery.CreameryPart[] {dev.farmings.process.creamery.CreameryPart.BATCH_PASTEURIZER,
					dev.farmings.process.creamery.CreameryPart.CREAM_SEPARATOR, dev.farmings.process.creamery.CreameryPart.BUTTER_CHURN, dev.farmings.process.creamery.CreameryPart.CHEESE_VAT}) {
				ctl.machine(level, p).forceRunning(true);
			}
			ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.BUTTER);
			ctl.creamery().addMilk(200, 4.76, 3.63);
		});
		context.waitTicks(40);
		server.runCommand("tp @p " + (cx + 6) + " " + (y + 3.5) + " " + (cz + 6) + " 180 25");
		context.waitTicks(20);
		Filming film = context.computeOnClient(client -> Filming.start(client, "creamery"));
		context.waitTicks(20);
		context.takeScreenshot("farmings-creamery-start");
		for (int i = 0; i < 12; i++) {
			String text = server.computeOnServer(srv -> {
				dev.farmings.creamery.CreameryControllerEntity ctl = (dev.farmings.creamery.CreameryControllerEntity) srv.overworld().getBlockEntity(c);
				ctl.run(srv.overworld(), 20);
				if (ctl.creamery().phase() == dev.farmings.process.creamery.Creamery.Phase.IDLE && ctl.creamery().butterKg() > 0
						&& ctl.creamery().mode() == dev.farmings.process.creamery.Creamery.Mode.BUTTER) {
					ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.CHEESE);
					ctl.creamery().addMilk(200, 3.72, 3.06);
				}
				return ctl.readout();
			});
			actionbar(server, text);
			context.waitTicks(20);
			if (i == 3) {
				context.takeScreenshot("farmings-creamery-butter");
			}
		}
		server.runOnServer(srv -> {
			dev.farmings.creamery.CreameryControllerEntity ctl = (dev.farmings.creamery.CreameryControllerEntity) srv.overworld().getBlockEntity(c);
			for (int d = 0; d < 6; d++) {
				ctl.run(srv.overworld(), 24_000);    // press and age the cheese: 4 game days
			}
			ServerLevel level = srv.overworld();
			int n = 0;
			for (ItemStack stack : new ItemStack[] {new ItemStack(FarmingsItems.BUTTER, ctl.creamery().takeButter(64)), new ItemStack(FarmingsItems.CHEDDAR_WHEEL, ctl.creamery().takeWheels(16)),
					new ItemStack(FarmingsItems.SKIM_MILK_BOTTLE, 4), new ItemStack(FarmingsItems.BUTTERMILK_BOTTLE, 2)}) {
				ItemEntity e = new ItemEntity(level, cx + 3 + n * 1.5, y + 0.2, cz + 2.5, stack);
				e.setDeltaMovement(Vec3.ZERO);
				level.addFreshEntity(e);
				n++;
			}
		});
		String text = server.computeOnServer(srv -> ((dev.farmings.creamery.CreameryControllerEntity) srv.overworld().getBlockEntity(c)).readout());
		actionbar(server, text);
		context.waitTicks(40);
		context.takeScreenshot("farmings-creamery-products");
		film.stop();
	}

	/**
	 * The sheep farm (docs/multiblocks/sheep-shed.md, shearing-shed.md, wool-mill.md, docs/wool.md): the flock in the shed at night, filmed
	 * going out to the pasture by day; a Merino filmed walking to the shearing stand and shorn; the mill line with its products; the player
	 * in the wool clothing.
	 */
	private static void sheepFarm(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 40;
		int cz = 40;
		java.util.List<dev.farmings.entity.SheepEntity> flock = new java.util.ArrayList<>();
		BlockPos shedPos = new BlockPos(cx + 1, y, cz + 2);
		BlockPos standPos = new BlockPos(cx + 5, y, cz - 1);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, cx + 8, cz + 6, y, 16, 14, 6);
			level.setBlockAndUpdate(shedPos, dev.farmings.shed.ShedBlocks.SHED_CONTROLLER.defaultBlockState());
			for (int x = cx + 2; x <= cx + 5; x++) {
				for (int z = cz + 1; z <= cz + 4; z++) {
					level.setBlockAndUpdate(new BlockPos(x, y, z), dev.farmings.shed.ShedBlocks.SHEEP_PEN.defaultBlockState());
				}
			}
			for (int x = cx + 6; x <= cx + 20; x++) {
				for (int z = cz - 2; z <= cz + 8; z++) {
					if (x == cx + 6 || x == cx + 20 || z == cz - 2 || z == cz + 8) {
						level.setBlockAndUpdate(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState());
					}
				}
			}
			level.setBlockAndUpdate(new BlockPos(cx + 6, y, cz + 1), dev.farmings.shed.ShedBlocks.LAMBING_PEN.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(cx + 6, y, cz + 3), dev.farmings.shed.ShedBlocks.PASTURE_GATE.defaultBlockState());
			barnPart(level, BarnParts.HAY_FEEDER, new BlockPos(cx + 2, y, cz));
			barnPart(level, BarnParts.WATER_TROUGH, new BlockPos(cx + 3, y, cz + 5));
			level.setBlockAndUpdate(new BlockPos(cx + 5, y, cz), dev.farmings.shearing.ShearingBlocks.CATCHING_PEN.defaultBlockState());
			level.setBlockAndUpdate(standPos, dev.farmings.shearing.ShearingBlocks.SHEARING_STAND.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(cx + 4, y, cz - 1), dev.farmings.shearing.ShearingBlocks.WOOL_TABLE.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(cx + 3, y, cz - 1), dev.farmings.shearing.ShearingBlocks.WOOL_PRESS.defaultBlockState());
			if (level.getBlockEntity(new BlockPos(cx + 2, y, cz)) instanceof dev.farmings.barn.FeederEntity f) {
				for (int i = 0; i < 4; i++) {
					f.addBlock();
				}
			}
			if (level.getBlockEntity(BigParts.owner(level, new BlockPos(cx + 3, y, cz + 5))) instanceof dev.farmings.barn.TroughEntity t) {
				t.pourBucket();
			}
			long seed = 1200;
			dev.farmings.husbandry.SheepBreed[] breeds = {dev.farmings.husbandry.SheepBreed.MERINO, dev.farmings.husbandry.SheepBreed.MERINO,
					dev.farmings.husbandry.SheepBreed.SUFFOLK, dev.farmings.husbandry.SheepBreed.DORPER, dev.farmings.husbandry.SheepBreed.EAST_FRIESIAN,
					dev.farmings.husbandry.SheepBreed.SUFFOLK};
			for (int i = 0; i < breeds.length; i++) {
				dev.farmings.entity.SheepEntity s = sheep(level, breeds[i], i == 5 ? Sex.MALE : Sex.FEMALE, seed++, 900);
				s.setNoAi(false);
				s.setFleeceDaysForTest(i == 0 ? 365 : 200);
				s.setPos(cx + 2.5 + i % 4, y + 0.2, cz + 1.5 + i / 4 * 2);
				s.assignShed(shedPos, null);
				flock.add(s);
			}
			((dev.farmings.shed.ShedControllerEntity) level.getBlockEntity(shedPos)).forceDayForTest(false);
		});
		context.waitTicks(100);
		actionbar(server, "Night: the flock is in the shed");
		server.runCommand("tp @p " + (cx + 4.5) + " " + (y + 9) + " " + (cz + 15) + " 160 38");
		context.waitTicks(40);
		context.takeScreenshot("farmings-sheepfarm-night");
		Filming film = context.computeOnClient(client -> Filming.start(client, "flock-to-pasture"));
		server.runOnServer(srv -> ((dev.farmings.shed.ShedControllerEntity) srv.overworld().getBlockEntity(shedPos)).forceDayForTest(true));
		actionbar(server, "Morning: out through the pasture gate");
		context.waitTicks(400);
		context.takeScreenshot("farmings-sheepfarm-pasture");
		String[] shedRead = new String[1];
		server.runOnServer(srv -> shedRead[0] = ((dev.farmings.shed.ShedControllerEntity) srv.overworld().getBlockEntity(shedPos)).readout());
		actionbar(server, shedRead[0]);
		context.waitTicks(60);
		context.takeScreenshot("farmings-sheepfarm-readout");
		film.stop();
		// the shearing stand: night again, power on; the Merino with a year's fleece is called in and shorn
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			((dev.farmings.shed.ShedControllerEntity) level.getBlockEntity(shedPos)).forceDayForTest(false);
			((dev.farmings.shearing.ShearingStandEntity) level.getBlockEntity(standPos)).forceRunning(true);
		});
		context.waitTicks(200);
		server.runCommand("tp @p " + (cx + 8.5) + " " + (y + 3.5) + " " + (cz - 4.5) + " 60 25");
		Filming shear = context.computeOnClient(client -> Filming.start(client, "shearing"));
		for (int i = 0; i < 12; i++) {
			String[] read = new String[1];
			server.runOnServer(srv -> read[0] = ((dev.farmings.shearing.ShearingStandEntity) srv.overworld().getBlockEntity(standPos)).readout());
			actionbar(server, read[0]);
			context.waitTicks(40);
			if (i == 5) {
				context.takeScreenshot("farmings-shearing-stand");
			}
		}
		String[] table = new String[1];
		server.runOnServer(srv -> {
			dev.farmings.shearing.WoolStoreEntity t = (dev.farmings.shearing.WoolStoreEntity) srv.overworld().getBlockEntity(new BlockPos(cx + 4, y, cz - 1));
			StringBuilder b = new StringBuilder("Wool table:");
			for (ItemStack st : t.items()) {
				if (!st.isEmpty()) {
					b.append(' ').append(st.getHoverName().getString()).append(String.format(java.util.Locale.ROOT, " %.2f kg %s;",
							dev.farmings.item.FleeceItem.greasyKg(st), dev.farmings.item.FleeceItem.grade(st) == null ? "" : dev.farmings.item.FleeceItem.grade(st).label));
				}
			}
			table[0] = b.toString();
		});
		actionbar(server, table[0]);
		context.waitTicks(40);
		context.takeScreenshot("farmings-shearing-done");
		shear.stop();
		// the mill line, fed by hand in a hurry: each machine shows what it made
		java.util.List<dev.farmings.woolmill.WoolMachineEntity> mill = new java.util.ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int mz = cz + 13;
			dev.farmings.woolmill.WoolMachine[] kinds = dev.farmings.woolmill.WoolMachine.values();
			for (int i = 0; i < kinds.length; i++) {
				BlockPos p = new BlockPos(cx + 2 + 2 * i, y, mz);
				level.setBlockAndUpdate(p, dev.farmings.woolmill.WoolMillBlocks.machine(kinds[i]).defaultBlockState());
				dev.farmings.woolmill.WoolMachineEntity m = (dev.farmings.woolmill.WoolMachineEntity) level.getBlockEntity(p);
				m.forceRunning(true);
				mill.add(m);
			}
			mill.get(0).pourBucket();
			mill.get(0).insertByHand(dev.farmings.item.FleeceItem.make(FarmingsItems.FLEECE, 4.4, 20.5, 8, 0.5, dev.farmings.husbandry.Flock.Grade.FINE, dev.farmings.genetics.SheepLook.Wool.WHITE));
			mill.get(1).insertByHand(dev.farmings.item.FleeceItem.make(FarmingsItems.CLEAN_WOOL, 2.2, 20.5, 8, 1, dev.farmings.husbandry.Flock.Grade.FINE, dev.farmings.genetics.SheepLook.Wool.WHITE));
			mill.get(2).insertByHand(dev.farmings.item.FleeceItem.make(FarmingsItems.SLIVER, 2.0, 29, 9, 1, dev.farmings.husbandry.Flock.Grade.QUARTER, dev.farmings.genetics.SheepLook.Wool.BLACK));
			mill.get(3).insertByHand(new ItemStack(FarmingsItems.YARN.get(net.minecraft.world.item.DyeColor.RED), 8));
			mill.get(4).insertByHand(dev.farmings.item.FleeceItem.make(FarmingsItems.CLEAN_WOOL, 1.0, 30, 9, 1, dev.farmings.husbandry.Flock.Grade.QUARTER, dev.farmings.genetics.SheepLook.Wool.WHITE));
		});
		context.waitTicks(130);
		String[] made = new String[1];
		server.runOnServer(srv -> {
			StringBuilder b = new StringBuilder();
			for (dev.farmings.woolmill.WoolMachineEntity m : mill) {
				b.append(m.kind().title).append(':');
				for (int i = 1; i < 5; i++) {
					ItemStack st = m.items().get(i);
					if (!st.isEmpty()) {
						b.append(' ').append(st.getCount()).append(' ').append(st.getHoverName().getString());
					}
				}
				b.append("  ");
				// show the output on top of each machine
				ItemStack first = m.items().get(1).copy();
				net.minecraft.world.entity.item.ItemEntity shown = new net.minecraft.world.entity.item.ItemEntity(srv.overworld(), m.getBlockPos().getX() + 0.5,
						m.getBlockPos().getY() + 1.1, m.getBlockPos().getZ() + 0.5, first);
				shown.setNoGravity(true);
				shown.setDeltaMovement(0, 0, 0);
				shown.setNeverPickUp();
				srv.overworld().addFreshEntity(shown);
			}
			made[0] = b.toString();
		});
		actionbar(server, made[0]);
		server.runCommand("tp @p " + (cx + 6.5) + " " + (y + 2.6) + " " + (cz + 18.5) + " 180 22");
		context.waitTicks(40);
		context.takeScreenshot("farmings-wool-mill");
		// the wool clothing, worn
		server.runCommand("gamemode survival @p");
		server.runCommand("tp @p " + (cx + 6.5) + " " + y + " " + (cz + 22.5) + " 180 10");
		server.runOnServer(srv -> {
			net.minecraft.server.level.ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
			p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(FarmingsItems.WOOL_CAP));
			p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(FarmingsItems.WOOL_SWEATER));
			p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(FarmingsItems.WOOL_TROUSERS));
			p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(FarmingsItems.WOOL_SOCKS));
		});
		actionbar(server, "Wool clothing: cap, sweater, trousers, socks");
		context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(60);
		context.takeScreenshot("farmings-wool-clothing");
		context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
	}

	/** The player in milk armor, seen from the front: fresh, then a game day later spoiled, with Nausea and Hunger. Filmed. */
	private static void milkArmor(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 60;
		server.runOnServer(srv -> clear(srv.overworld(), cx, cz, y, 6, 6, 5));
		server.runCommand("gamemode survival @p");
		server.runCommand("tp @p " + (cx + 0.5) + " " + y + " " + (cz + 0.5) + " 180 10");
		server.runOnServer(srv -> {
			net.minecraft.server.level.ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
			net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
					net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
			net.minecraft.world.item.Item[] items = {FarmingsItems.MILK_HELMET, FarmingsItems.MILK_CHESTPLATE, FarmingsItems.MILK_LEGGINGS, FarmingsItems.MILK_BOOTS};
			for (int i = 0; i < 4; i++) {
				ItemStack stack = new ItemStack(items[i]);
				dev.farmings.item.MilkArmorItem.setMade(stack, srv.overworld().getGameTime());
				p.setItemSlot(slots[i], stack);
			}
		});
		context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(40);
		Filming film = context.computeOnClient(client -> Filming.start(client, "milk-armor"));
		context.waitTicks(30);
		context.takeScreenshot("farmings-armor-fresh");
		actionbar(server, "A game day later...");
		server.runOnServer(srv -> {
			net.minecraft.server.level.ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
			for (net.minecraft.world.entity.EquipmentSlot s : new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
					net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
				dev.farmings.item.MilkArmorItem.setMade(p.getItemBySlot(s), srv.overworld().getGameTime() - dev.farmings.item.MilkArmorItem.SPOIL_TICKS);
			}
		});
		context.waitTicks(80);
		context.takeScreenshot("farmings-armor-spoiled");
		context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		context.waitTicks(60);
		context.takeScreenshot("farmings-armor-sick-first-person");
		film.stop();
	}


	/** Wild animals counted around a spot: Farmings wild cattle and sheep by breed, and any vanilla cows or sheep. */
	private static String census(ServerLevel level, BlockPos at, String where) {
		java.util.Map<String, Integer> n = new java.util.TreeMap<>();
		int vanilla = 0;
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(at).inflate(96, 64, 96);
		for (net.minecraft.world.entity.Mob m : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box)) {
			if (m instanceof CattleEntity c && c.isWild()) {
				n.merge(c.breed().code + " cattle", 1, Integer::sum);
			} else if (m instanceof dev.farmings.entity.SheepEntity sh && sh.isWild()) {
				n.merge(sh.breed().code + " sheep", 1, Integer::sum);
			} else if (m.getType() == net.minecraft.world.entity.EntityTypes.COW || m.getType() == net.minecraft.world.entity.EntityTypes.SHEEP) {
				vanilla++;
			}
		}
		return where + ": wild " + n + ", vanilla cows/sheep " + vanilla;
	}

	/**
	 * Wild animals and the taming corral (docs/taming.md): wild herds where the world spawned them (plains, savanna), a wild cow filmed
	 * following hay into the corral, a wild flock filmed drawn by the bait through the one-way gate, and the readouts after settling.
	 */
	private static void taming(ClientGameTestContext context, TestServerContext server, int y0) {
		BlockPos[] plains = new BlockPos[1];
		for (String biome : new String[] {"plains", "savanna"}) {
			BlockPos[] found = new BlockPos[1];
			server.runOnServer(srv -> {
				var r = srv.overworld().findClosestBiome3d(h -> h.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
						net.minecraft.resources.Identifier.withDefaultNamespace(biome))), new BlockPos(0, 64, 0), 3200, 32, 64);
				found[0] = r == null ? null : r.getFirst();
			});
			if (found[0] == null) {
				actionbar(server, "No " + biome + " within 3200 blocks");
				context.waitTicks(20);
				continue;
			}
			server.runCommand("tp @p " + found[0].getX() + " 140 " + found[0].getZ() + " 0 60");
			context.waitTicks(300);
			String text = server.computeOnServer(srv -> census(srv.overworld(), found[0], biome));
			BlockPos herd = server.computeOnServer(srv -> {
				for (net.minecraft.world.entity.Mob m : srv.overworld().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
						new net.minecraft.world.phys.AABB(found[0]).inflate(96, 64, 96), m -> m instanceof dev.farmings.entity.Wild.Animal a && a.isWild())) {
					return m.blockPosition();
				}
				return null;
			});
			System.out.println("[farmings] " + text);
			if (biome.equals("plains")) {
				plains[0] = found[0];
			}
			if (herd != null) {
				server.runCommand("tp @p " + (herd.getX() + 0.5) + " " + (herd.getY() + 9) + " " + (herd.getZ() + 12.5) + " 180 35");
				context.waitTicks(60);
			}
			actionbar(server, text);
			context.waitTicks(20);
			context.takeScreenshot("farmings-wild-" + biome);
		}
		// the corral, built on the plains spot (or at 40, 40 if there were none)
		int cx = plains[0] == null ? 40 : plains[0].getX() + 24;
		int cz = plains[0] == null ? 40 : plains[0].getZ() + 24;
		server.runCommand("tp @p " + (cx + 5.5) + " 160 " + (cz + 12.5));
		context.waitTicks(120);
		int y = server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + 5, cz + 12));
		BlockPos post = new BlockPos(cx + 1, y, cz + 1);
		BlockPos gate = new BlockPos(cx + 5, y, cz + 9);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (net.minecraft.world.entity.Mob m : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(new BlockPos(cx + 5, y, cz + 12)).inflate(40))) {
				m.discard();
			}
			clear(level, cx + 5, cz + 12, y, 14, 16, 16);
			for (int x = cx - 9; x <= cx + 19; x++) {
				for (int z = cz - 4; z <= cz + 28; z++) {
					for (int k = 2; k <= 4; k++) {
						level.setBlockAndUpdate(new BlockPos(x, y - k, z), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
					}
				}
			}
			for (int x = cx; x <= cx + 10; x++) {
				for (int z = cz; z <= cz + 9; z++) {
					if (x == cx || x == cx + 10 || z == cz || z == cz + 9) {
						level.setBlockAndUpdate(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState());
					}
				}
			}
			level.setBlockAndUpdate(gate, dev.farmings.corral.CorralBlocks.CORRAL_GATE.defaultBlockState().setValue(dev.farmings.corral.CorralGateBlock.FACING, Direction.NORTH));
			level.setBlockAndUpdate(post, dev.farmings.corral.CorralBlocks.CORRAL_POST.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(cx + 5, y, cz + 3), dev.farmings.corral.CorralBlocks.BAIT_TROUGH.defaultBlockState());
			barnPart(level, BarnParts.WATER_TROUGH, new BlockPos(cx + 7, y, cz + 5));
			if (level.getBlockEntity(BigParts.owner(level, new BlockPos(cx + 7, y, cz + 5))) instanceof TroughEntity t) {
				t.pourBucket();
			}
		});
		// 1. a wild Holstein follows a player holding hay, through the gate into the corral
		server.runCommand("gamemode creative @p");
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:hay_block");
		CattleEntity[] cow = new CattleEntity[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			CattleEntity c = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
			c.setBreedForDisplay(CattleBreed.HOLSTEIN, Sex.FEMALE, 650, false);
			c.setWild(true);
			c.setPos(cx + 5.5, y, cz + 15.5);
			level.addFreshEntity(c);
			cow[0] = c;
		});
		server.runCommand("tp @p " + (cx + 5.5) + " " + y + " " + (cz + 6.5) + " 0 20");
		context.waitTicks(40);
		actionbar(server, "A wild Holstein follows the hay in the player's hand");
		context.takeScreenshot("farmings-taming-wild-cow");
		Filming follow = context.computeOnClient(client -> Filming.start(client, "wild-cow-follows-hay"));
		for (int i = 0; i < 14; i++) {
			context.waitTicks(20);
			String read = server.computeOnServer(srv -> cow[0].readout());
			actionbar(server, read);
		}
		context.takeScreenshot("farmings-taming-cow-in");
		follow.stop();
		// 2. a wild flock, spawned as a natural group, drawn by the bait through the one-way gate
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		server.runCommand("gamemode spectator @p");
		java.util.List<dev.farmings.entity.SheepEntity> flock = new java.util.ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			((dev.farmings.corral.BaitTroughEntity) level.getBlockEntity(new BlockPos(cx + 5, y, cz + 3))).addBlock();
			((dev.farmings.corral.BaitTroughEntity) level.getBlockEntity(new BlockPos(cx + 5, y, cz + 3))).addBlock();
			net.minecraft.world.entity.SpawnGroupData group = null;
			for (int i = 0; i < 4; i++) {
				dev.farmings.entity.SheepEntity s = FarmingsEntities.SHEEP.create(level, EntitySpawnReason.NATURAL);
				s.snapTo(cx + 3.5 + i * 1.5, y, cz + 24.5, 0, 0);
				group = s.finalizeSpawn(level, level.getCurrentDifficultyAt(s.blockPosition()), EntitySpawnReason.NATURAL, group);
				level.addFreshEntity(s);
				flock.add(s);
			}
			((dev.farmings.corral.CorralPostEntity) level.getBlockEntity(post)).claim(srv.getPlayerList().getPlayers().get(0).getUUID());
		});
		server.runCommand("tp @p " + (cx + 5.5) + " " + (y + 9) + " " + (cz - 4.5) + " 0 40");
		context.waitTicks(40);
		context.takeScreenshot("farmings-taming-flock-outside");
		Filming bait = context.computeOnClient(client -> Filming.start(client, "bait-draws-flock"));
		for (int i = 0; i < 24; i++) {
			context.waitTicks(20);
			String read = server.computeOnServer(srv -> ((dev.farmings.corral.CorralPostEntity) srv.overworld().getBlockEntity(post)).readout(srv.overworld()));
			actionbar(server, read);
		}
		context.takeScreenshot("farmings-taming-flock-in");
		bait.stop();
		// 3. seven real days fed and watered: the corral tames them; the readouts show it
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			dev.farmings.corral.CorralPostEntity p = (dev.farmings.corral.CorralPostEntity) level.getBlockEntity(post);
			p.refresh(level);
			for (net.minecraft.world.entity.Mob m : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(post).inflate(16),
					m -> m instanceof dev.farmings.entity.Wild.Animal a && a.isWild() && p.inside(m))) {
				for (int k = 0; k < 8; k++) {
					p.tend(level, m, (dev.farmings.entity.Wild.Animal) m, (int) dev.farmings.husbandry.TimeScale.realDaysToTicks(1));
				}
			}
		});
		String postRead = server.computeOnServer(srv -> ((dev.farmings.corral.CorralPostEntity) srv.overworld().getBlockEntity(post)).readout(srv.overworld()));
		actionbar(server, postRead);
		System.out.println("[farmings] " + postRead);
		context.waitTicks(40);
		context.takeScreenshot("farmings-taming-post-readout");
		String sheepRead = server.computeOnServer(srv -> flock.get(0).readout());
		System.out.println("[farmings] " + sheepRead);
		actionbar(server, sheepRead);
		context.waitTicks(40);
		context.takeScreenshot("farmings-taming-tamed-sheep");
	}

	/** A bird of one breed, grown to {@code age} real days, standing still (docs/animals/poultry.md). */
	private static dev.farmings.entity.FowlEntity bird(ServerLevel level, dev.farmings.husbandry.poultry.FowlBreed breed, Sex sex, long seed, double age) {
		dev.farmings.entity.FowlEntity f = FarmingsEntities.fowl(breed.species).create(level, EntitySpawnReason.COMMAND);
		f.initFounder(breed, sex, seed, age, level.getGameTime());
		f.setPos(200.5, 0, 200.5);
		f.setNoAi(true);
		level.addFreshEntity(f);
		return f;
	}

	/** A hatchling from a mated pair, through a fertile egg. */
	private static dev.farmings.entity.FowlEntity hatchling(ServerLevel level, dev.farmings.entity.FowlEntity hen, dev.farmings.entity.FowlEntity male, long t) {
		hen.mate(male);
		ItemStack egg = hen.makeEgg(level.getGameTime() + t);
		dev.farmings.entity.FowlEntity c = dev.farmings.entity.FowlEggs.hatch(level, egg, new BlockPos(200, 0, 200), null);
		c.setNoAi(true);
		return c;
	}

	/** Every poultry breed, hen and male, the young of each species, and a cross: a row per species, then close-ups. */
	private static void poultry(ClientGameTestContext context, TestServerContext server, int y) {
		java.util.List<java.util.List<dev.farmings.entity.FowlEntity>> rows = new java.util.ArrayList<>();
		java.util.List<dev.farmings.entity.FowlEntity> close = new java.util.ArrayList<>();
		java.util.List<String> closeNames = new java.util.ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 0, 30, y, 20, 20, 6);
			long seed = 300;
			for (dev.farmings.genetics.Species sp : new dev.farmings.genetics.Species[] {dev.farmings.genetics.Species.CHICKEN,
					dev.farmings.genetics.Species.TURKEY, dev.farmings.genetics.Species.QUAIL, dev.farmings.genetics.Species.DUCK}) {
				java.util.List<dev.farmings.entity.FowlEntity> row = new java.util.ArrayList<>();
				dev.farmings.entity.FowlEntity firstHen = null;
				dev.farmings.entity.FowlEntity firstMale = null;
				for (dev.farmings.husbandry.poultry.FowlBreed b : dev.farmings.husbandry.poultry.FowlBreed.values()) {
					if (b.species != sp) {
						continue;
					}
					dev.farmings.entity.FowlEntity hen = bird(level, b, Sex.FEMALE, seed++, b.firstEggDays + 60);
					dev.farmings.entity.FowlEntity male = bird(level, b, Sex.MALE, seed++, b.firstEggDays + 60);
					row.add(hen);
					row.add(male);
					if (firstHen == null) {
						firstHen = hen;
					} else if (firstMale == null) {
						firstMale = male;
					}
				}
				dev.farmings.entity.FowlEntity young = hatchling(level, firstHen, firstMale, seed++);
				row.add(young);
				rows.add(row);
			}
			// close-ups: the birds with the most to show
			Object[][] picks = {{0, 3, "rhode island red rooster"}, {0, 6, "silkie hen"}, {1, 3, "bronze tom"}, {3, 5, "mallard drake"},
					{3, 6, "muscovy duck"}, {3, 0, "pekin duck"}, {2, 0, "coturnix hen"}, {0, 8, "chick"}};
			for (Object[] pk : picks) {
				close.add(rows.get((Integer) pk[0]).get((Integer) pk[1]));
				closeNames.add((String) pk[2]);
			}
		});
		context.waitTicks(60);
		double[] spacing = {1.4, 2.6, 0.7, 1.5};
		double[] back = {6.5, 9.5, 2.2, 7.0};
		String[] names = {"chickens", "turkeys", "quail", "ducks"};
		for (int r = 0; r < rows.size(); r++) {
			int row = r;
			server.runOnServer(srv -> {
				for (int k = 0; k < rows.size(); k++) {
					java.util.List<dev.farmings.entity.FowlEntity> l = rows.get(k);
					for (int j = 0; j < l.size(); j++) {
						dev.farmings.entity.FowlEntity f = l.get(j);
						if (k == row) {
							f.setPos(-(l.size() - 1) * spacing[row] / 2 + j * spacing[row] + 0.5, y, 30.5);
						} else {
							f.setPos(200.5 + 3 * j, y, 200.5 + 10 * k);
						}
						f.setYRot(90);
						f.setYHeadRot(90);
						f.setYBodyRot(90);
					}
				}
			});
			actionbar(server, names[row] + ": each breed hen then male, and a hatchling");
			server.runCommand("tp @p 0.5 " + (y + back[row] * 0.35) + " " + (30.5 + back[row]) + " 180 20");
			context.waitTicks(40);
			context.takeScreenshot("farmings-poultry-" + names[row]);
		}
		for (int i = 0; i < close.size(); i++) {
			int shown = i;
			server.runOnServer(srv -> {
				for (java.util.List<dev.farmings.entity.FowlEntity> l : rows) {
					for (dev.farmings.entity.FowlEntity f : l) {
						f.setPos(200.5, y, 200.5);
					}
				}
				dev.farmings.entity.FowlEntity f = close.get(shown);
				f.setPos(0.5, y, 30.5);
				f.setYRot(0);
				f.setYHeadRot(0);
				f.setYBodyRot(0);
			});
			dev.farmings.entity.FowlEntity f = close.get(i);
			actionbar(server, closeNames.get(i) + ": " + f.readout());
			double d = f.species() == dev.farmings.genetics.Species.TURKEY ? 5.0 : f.species() == dev.farmings.genetics.Species.QUAIL || f.young() ? 1.3 : 2.6;
			server.runCommand("tp @p " + (0.5 + d * 0.7) + " " + (y + d * 0.55) + " " + (30.5 + d * 0.7) + " 135 30");
			context.waitTicks(30);
			context.takeScreenshot("farmings-poultry-close-" + String.format(java.util.Locale.ROOT, "%02d", i) + "-" + closeNames.get(i).replace(' ', '-'));
		}
	}

	/** Places one of the barn's big parts (the water trough) facing north at {@code at}. */
	private static void trough(ServerLevel level, BlockPos at) {
		barnPart(level, BarnParts.WATER_TROUGH, at);
		if (level.getBlockEntity(BigParts.owner(level, at)) instanceof TroughEntity t) {
			t.pourBucket();
		}
	}

	/**
	 * Poultry at work, filmed: a coop's flock goes out to its run and pond by day and back in at dusk; wild hens are drawn into a corral by
	 * grain; an incubator hatches a clutch under a brooder.
	 */
	private static void poultryFarm(ClientGameTestContext context, TestServerContext server, int y) {
		// 1. the coop: controller (0,y,0); floor x 1-3, z -1..1; nests north; perch; feeder and trough south; pop hole (4,y,0); run x 5-13
		BlockPos c = new BlockPos(0, y, 0);
		java.util.List<dev.farmings.entity.FowlEntity> flock = new java.util.ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 6, 0, y, 16, 12, 6);
			for (int x = 4; x <= 14; x++) {
				for (int z = -5; z <= 5; z++) {
					if (x == 4 || x == 14 || z == -5 || z == 5) {
						level.setBlockAndUpdate(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState());
					}
				}
			}
			level.setBlockAndUpdate(c, dev.farmings.coop.CoopBlocks.COOP_CONTROLLER.defaultBlockState());
			for (int x = 1; x <= 3; x++) {
				for (int z = -1; z <= 1; z++) {
					level.setBlockAndUpdate(new BlockPos(x, y, z), dev.farmings.coop.CoopBlocks.COOP_FLOOR.defaultBlockState());
				}
			}
			level.setBlockAndUpdate(new BlockPos(4, y, 0), dev.farmings.coop.CoopBlocks.POP_HOLE.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(1, y, -2), dev.farmings.coop.CoopBlocks.NEST_BOX.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(2, y, -2), dev.farmings.coop.CoopBlocks.NEST_BOX.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(3, y, -2), dev.farmings.coop.CoopBlocks.PERCH.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(1, y, 2), dev.farmings.coop.CoopBlocks.GRAIN_FEEDER.defaultBlockState());
			if (level.getBlockEntity(new BlockPos(1, y, 2)) instanceof dev.farmings.coop.GrainFeederEntity g) {
				for (int i = 0; i < 200; i++) {
					g.addItem();
				}
			}
			trough(level, new BlockPos(2, y, 2));
			for (int x = 10; x <= 12; x++) {
				for (int z = 1; z <= 3; z++) {
					level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
				}
			}
			long seed = 700;
			Object[][] birds = {{dev.farmings.husbandry.poultry.FowlBreed.RHODE_ISLAND_RED, Sex.FEMALE}, {dev.farmings.husbandry.poultry.FowlBreed.RHODE_ISLAND_RED, Sex.FEMALE},
					{dev.farmings.husbandry.poultry.FowlBreed.PLYMOUTH_ROCK, Sex.FEMALE}, {dev.farmings.husbandry.poultry.FowlBreed.LEGHORN, Sex.FEMALE},
					{dev.farmings.husbandry.poultry.FowlBreed.RHODE_ISLAND_RED, Sex.MALE}, {dev.farmings.husbandry.poultry.FowlBreed.PEKIN, Sex.FEMALE},
					{dev.farmings.husbandry.poultry.FowlBreed.KHAKI_CAMPBELL, Sex.FEMALE}};
			for (Object[] b : birds) {
				dev.farmings.husbandry.poultry.FowlBreed br = (dev.farmings.husbandry.poultry.FowlBreed) b[0];
				dev.farmings.entity.FowlEntity f = bird(level, br, (Sex) b[1], seed++, br.firstEggDays + 60);
				f.setNoAi(false);
				f.setPos(2.5, y, 0.5);
				flock.add(f);
			}
			var coop = (dev.farmings.coop.CoopControllerEntity) level.getBlockEntity(c);
			coop.forceDayForTest(false);
		});
		server.runCommand("tp @p 8.5 " + (y + 9) + " 13.5 160 40");
		context.waitTicks(80);
		actionbar(server, server.computeOnServer(srv -> ((dev.farmings.coop.CoopControllerEntity) srv.overworld().getBlockEntity(c)).readout()));
		context.takeScreenshot("farmings-poultryfarm-coop-night");
		server.runOnServer(srv -> ((dev.farmings.coop.CoopControllerEntity) srv.overworld().getBlockEntity(c)).forceDayForTest(true));
		Filming out = context.computeOnClient(client -> Filming.start(client, "coop-birds-out-to-run"));
		for (int i = 0; i < 16; i++) {
			context.waitTicks(20);
			actionbar(server, "Morning: the flock goes out through the pop hole to the run and the pond");
		}
		context.takeScreenshot("farmings-poultryfarm-run-by-day");
		out.stop();
		server.runOnServer(srv -> ((dev.farmings.coop.CoopControllerEntity) srv.overworld().getBlockEntity(c)).forceDayForTest(false));
		Filming in = context.computeOnClient(client -> Filming.start(client, "coop-birds-in-at-dusk"));
		for (int i = 0; i < 16; i++) {
			context.waitTicks(20);
			actionbar(server, "Dusk: the flock comes home to the coop and its perch");
		}
		context.takeScreenshot("farmings-poultryfarm-in-at-dusk");
		in.stop();
		// 2. wild hens drawn into a corral by grain (x 20-30, z -10..-1; gate on the south side)
		int cx = 20;
		int cz = -12;
		BlockPos post = new BlockPos(cx + 1, y, cz + 1);
		java.util.List<dev.farmings.entity.FowlEntity> wild = new java.util.ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, cx + 5, cz + 10, y, 10, 14, 6);
			for (int x = cx; x <= cx + 10; x++) {
				for (int z = cz; z <= cz + 9; z++) {
					if (x == cx || x == cx + 10 || z == cz || z == cz + 9) {
						level.setBlockAndUpdate(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState());
					}
				}
			}
			level.setBlockAndUpdate(new BlockPos(cx + 5, y, cz + 9), dev.farmings.corral.CorralBlocks.CORRAL_GATE.defaultBlockState()
					.setValue(dev.farmings.corral.CorralGateBlock.FACING, Direction.NORTH));
			level.setBlockAndUpdate(post, dev.farmings.corral.CorralBlocks.CORRAL_POST.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(cx + 5, y, cz + 3), dev.farmings.corral.CorralBlocks.BAIT_TROUGH.defaultBlockState());
			if (level.getBlockEntity(new BlockPos(cx + 5, y, cz + 3)) instanceof dev.farmings.corral.BaitTroughEntity b) {
				for (int i = 0; i < 64; i++) {
					b.addGrain();
				}
			}
			trough(level, new BlockPos(cx + 7, y, cz + 5));
			net.minecraft.world.entity.SpawnGroupData group = null;
			for (int i = 0; i < 4; i++) {
				dev.farmings.entity.FowlEntity h = FarmingsEntities.CHICKEN.create(level, EntitySpawnReason.NATURAL);
				h.snapTo(cx + 3.5 + i * 1.2, y, cz + 18.5, 0, 0);
				group = h.finalizeSpawn(level, level.getCurrentDifficultyAt(h.blockPosition()), EntitySpawnReason.NATURAL, group);
				level.addFreshEntity(h);
				wild.add(h);
			}
			((dev.farmings.corral.CorralPostEntity) level.getBlockEntity(post)).claim(srv.getPlayerList().getPlayers().get(0).getUUID());
		});
		server.runCommand("tp @p " + (cx + 5.5) + " " + (y + 9) + " " + (cz - 4.5) + " 0 40");
		context.waitTicks(40);
		actionbar(server, server.computeOnServer(srv -> wild.get(0).readout()));
		context.takeScreenshot("farmings-poultryfarm-wild-hens");
		Filming lure = context.computeOnClient(client -> Filming.start(client, "grain-draws-wild-hens"));
		for (int i = 0; i < 24; i++) {
			context.waitTicks(20);
			actionbar(server, server.computeOnServer(srv -> ((dev.farmings.corral.CorralPostEntity) srv.overworld().getBlockEntity(post)).readout(srv.overworld())));
		}
		context.takeScreenshot("farmings-poultryfarm-hens-in-corral");
		lure.stop();
		// 3. an incubator one hour of real time from hatching, under a brooder (x -12)
		BlockPos inc = new BlockPos(-12, y, 0);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, -12, 0, y, 5, 5, 6);
			level.setBlockAndUpdate(inc, dev.farmings.hatchery.HatcheryBlocks.INCUBATOR.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(-12, y + 2, 2), dev.farmings.hatchery.HatcheryBlocks.BROODER.defaultBlockState());
			var incubator = (dev.farmings.hatchery.IncubatorEntity) level.getBlockEntity(inc);
			incubator.forceRunning(true);
			((dev.farmings.hatchery.BrooderEntity) level.getBlockEntity(new BlockPos(-12, y + 2, 2))).forceRunning(true);
			dev.farmings.entity.FowlEntity hen = bird(level, dev.farmings.husbandry.poultry.FowlBreed.PLYMOUTH_ROCK, Sex.FEMALE, 760, 200);
			dev.farmings.entity.FowlEntity cock = bird(level, dev.farmings.husbandry.poultry.FowlBreed.RHODE_ISLAND_RED, Sex.MALE, 761, 200);
			hen.mate(cock);
			for (int i = 0; i < 6; i++) {
				ItemStack egg = hen.makeEgg(level.getGameTime() + i);
				dev.farmings.entity.FowlEggs.setWarmDays(egg, 20.97 - 0.004 * i);
				incubator.add(egg);
			}
			hen.discard();
			cock.discard();
		});
		server.runCommand("tp @p -12.5 " + (y + 4) + " 6.5 180 35");
		context.waitTicks(30);
		actionbar(server, server.computeOnServer(srv -> ((dev.farmings.hatchery.IncubatorEntity) srv.overworld().getBlockEntity(inc)).readout()));
		context.takeScreenshot("farmings-poultryfarm-incubator");
		Filming hatch = context.computeOnClient(client -> Filming.start(client, "incubator-hatch"));
		for (int i = 0; i < 20; i++) {
			context.waitTicks(20);
			actionbar(server, server.computeOnServer(srv -> ((dev.farmings.hatchery.IncubatorEntity) srv.overworld().getBlockEntity(inc)).readout()));
		}
		context.takeScreenshot("farmings-poultryfarm-hatched");
		hatch.stop();
		String chick = server.computeOnServer(srv -> {
			var l = srv.overworld().getEntitiesOfClass(dev.farmings.entity.FowlEntity.class, new net.minecraft.world.phys.AABB(inc).inflate(4), dev.farmings.entity.FowlEntity::young);
			return l.isEmpty() ? "no chicks" : l.size() + " chicks; " + l.get(0).readout();
		});
		System.out.println("[farmings] " + chick);
		actionbar(server, chick);
		context.waitTicks(30);
		context.takeScreenshot("farmings-poultryfarm-chick-readout");
	}


	/** The gantry's cab, wherever it stands on its rail at x 1 (it steps as blocks; docs/multiblocks/field-gantry.md). */
	private static dev.farmings.gantry.GantryCabEntity cab(ServerLevel level, int y) {
		for (int z = 0; z <= 16; z++) {
			if (level.getBlockEntity(new BlockPos(1, y + 3, z)) instanceof dev.farmings.gantry.GantryCabEntity c) {
				return c;
			}
		}
		return null;
	}

	/** Starts the gantry with a tool and films it until it is parked at home again (at most {@code limit} seconds). */
	private static void gantryPass(ClientGameTestContext context, TestServerContext server, int y, dev.farmings.farm.gantry.GantryTool tool,
			String film, int limit, int after) {
		server.runOnServer(srv -> {
			var c = cab(srv.overworld(), y);
			c.fitTool(tool);
			c.toggle();
		});
		Filming f = context.computeOnClient(client -> Filming.start(client, film));
		int s = 0;
		while (s < limit) {
			context.waitTicks(20);
			s++;
			String r = server.computeOnServer(srv -> {
				var c = cab(srv.overworld(), y);
				return c == null ? "Field gantry: lifted, gliding along the rails" : c.readout().replace("Field gantry: span 8, tool ", "");
			});
			actionbar(server, r);
			if (s == 4) {
				context.takeScreenshot("farmings-fields-" + film + "-gliding");     // mid-pass: lifted into one entity (field-gantry.md, "The lift")
			}
			boolean home = server.computeOnServer(srv -> {
				var c = cab(srv.overworld(), y);
				return c != null && c.phase() == dev.farmings.gantry.GantryCabEntity.Phase.PARKED && c.step() == 0;
			});
			if (home && s > 3) {
				break;
			}
		}
		for (int i = 0; i < after; i++) {
			context.waitTicks(20);
		}
		f.stop();
		System.out.println("[farmings] " + film + " took " + s + " s: " + server.computeOnServer(srv -> cab(srv.overworld(), y).readout()));
	}

	/** Every field cell (x 2-9, z 0-15) set to {@code state} on farmland. */
	private static void sowField(ServerLevel level, int y, BlockState state) {
		for (int x = 2; x <= 9; x++) {
			for (int z = 0; z <= 15; z++) {
				level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState());
				level.setBlockAndUpdate(new BlockPos(x, y, z), state);
			}
		}
	}

	/**
	 * Phase 3 (docs/crops.md, field-gantry.md, conveyors.md, silo.md, feed-mill.md, compost.md, digester.md): a field gantry spanning 8
	 * blocks over a 16-row field sows corn, chops it onto a belt that blows it into a tower silo, then mows, rakes and bales alfalfa; a TMR
	 * mixer feeds a barn feeder by belt; a compost windrow is turned; and a digester's biogas runs a Wildcatter gas set.
	 */
	/** The loose-ends pass (2026-11-12): the sheep dog driving a flock, coat genes, plumage mutants and a thrown rotten egg. */
	private static void looseEnds(ClientGameTestContext context, TestServerContext server, int y) {
		// 1. a Border Collie drives five ewes from the far side of a field to a marked spot (Walk up)
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 8, 8, y, 16, 16, 6);
			var player = srv.getPlayerList().getPlayers().get(0);
			var dog = FarmingsEntities.SHEEP_DOG.create(level, EntitySpawnReason.COMMAND);
			dog.setWild(false);
			dog.setOwner(player.getUUID());
			dog.snapTo(20.5, y, 18.5, 0, 0);
			level.addFreshEntity(dog);
			dev.farmings.husbandry.SheepBreed[] breeds = {dev.farmings.husbandry.SheepBreed.SUFFOLK, dev.farmings.husbandry.SheepBreed.MERINO, dev.farmings.husbandry.SheepBreed.DORPER, dev.farmings.husbandry.SheepBreed.EAST_FRIESIAN, dev.farmings.husbandry.SheepBreed.SUFFOLK};
			for (int i = 0; i < 5; i++) {
				var s = FarmingsEntities.SHEEP.create(level, EntitySpawnReason.COMMAND);
				s.initFounder(breeds[i], Sex.FEMALE, 500 + i, 800, 70, level.getGameTime());
				s.snapTo(15.5 + i, y, 15.5 + (i % 2), 0, 0);
				level.addFreshEntity(s);
			}
			level.setBlockAndUpdate(new BlockPos(0, y - 1, 0), net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState());
		});
		server.runCommand("tp @p 10.5 " + (y + 9) + " 27.5 200 38");
		context.waitTicks(40);
		context.takeScreenshot("farmings-loose-dog-before");
		Filming herd = context.computeOnClient(client -> Filming.start(client, "dog-drives-flock"));
		server.runOnServer(srv -> dev.farmings.item.WhistleItem.blow(srv.overworld(), srv.getPlayerList().getPlayers().get(0), dev.farmings.entity.SheepDogEntity.Command.WALK_UP,
				new BlockPos(0, y, 0)));
		for (int i = 0; i < 40; i++) {
			context.waitTicks(10);
			if (i % 4 == 0) {
				actionbar(server, server.computeOnServer(srv -> srv.overworld().getEntitiesOfClass(dev.farmings.entity.SheepDogEntity.class,
						new net.minecraft.world.phys.AABB(0, y - 2, 0, 1, y + 2, 1).inflate(40)).stream().findFirst().map(dev.farmings.entity.SheepDogEntity::readout).orElse("no dog")));
			}
			if (i == 20) {
				context.takeScreenshot("farmings-loose-dog-driving");
			}
		}
		context.takeScreenshot("farmings-loose-dog-after");
		herd.stop();
		// 2. coat genes: a roan, a white (R/R), a brindle (Abr on E+) and a dun (PMEL Dh), with their plain founders
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 40, 0, y, 10, 6, 6);
			String[] names = {"roan", "white", "brindle", "dun", "silver dun"};
			for (int i = 0; i < 5; i++) {
				CattleEntity cow = founder(level, CattleBreed.JERSEY, Sex.FEMALE, 700 + i, 1500, CattleBreed.JERSEY.matureWeightKg, 34.5 + i * 3, y, 0.5);
				java.util.EnumMap<dev.farmings.genetics.VisualLocus, int[]> m = new java.util.EnumMap<>(dev.farmings.genetics.VisualGenome.founder("JER", 9L).alleles());
				m.put(dev.farmings.genetics.VisualLocus.PATTERN, new int[] {0, 0});
				m.put(dev.farmings.genetics.VisualLocus.EXTENSION, new int[] {2, 2});
				m.put(dev.farmings.genetics.VisualLocus.DILUTION, new int[] {0, 0});
				switch (i) {
					case 0 -> m.put(dev.farmings.genetics.VisualLocus.ROAN, new int[] {1, 0});
					case 1 -> m.put(dev.farmings.genetics.VisualLocus.ROAN, new int[] {1, 1});
					case 2 -> m.put(dev.farmings.genetics.VisualLocus.AGOUTI, new int[] {1, 0});
					case 3 -> m.put(dev.farmings.genetics.VisualLocus.PMEL, new int[] {1, 0});
					default -> m.put(dev.farmings.genetics.VisualLocus.PMEL, new int[] {1, 1});
				}
				cow.setVisualGenome(new dev.farmings.genetics.VisualGenome(m));
				cow.setNoAi(true);
				cow.setCustomName(net.minecraft.network.chat.Component.literal(names[i]));
				cow.setCustomNameVisible(true);
				cow.snapTo(34.5 + i * 3, y, 0.5, 180, 0);
			}
		});
		server.runCommand("tp @p 40.5 " + (y + 3) + " -8.5 0 15");
		context.waitTicks(40);
		context.takeScreenshot("farmings-loose-coats");
		// 3. plumage mutants: Muscovy blue, silver, lavender, chocolate; Coturnix Italian, Golden, Rosetta, Tibetan, English White
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 70, 0, y, 10, 6, 6);
			Object[][] birds = {{dev.farmings.husbandry.poultry.FowlBreed.MUSCOVY, dev.farmings.genetics.PoultryLocus.MUSCOVY_BLUE, 1, 0},
					{dev.farmings.husbandry.poultry.FowlBreed.MUSCOVY, dev.farmings.genetics.PoultryLocus.MUSCOVY_BLUE, 1, 1},
					{dev.farmings.husbandry.poultry.FowlBreed.MUSCOVY, dev.farmings.genetics.PoultryLocus.MUSCOVY_LAVENDER, 1, 1},
					{dev.farmings.husbandry.poultry.FowlBreed.MUSCOVY, dev.farmings.genetics.PoultryLocus.MUSCOVY_CHOCOLATE, 1, 1},
					{dev.farmings.husbandry.poultry.FowlBreed.COTURNIX, dev.farmings.genetics.PoultryLocus.QUAIL_FAWN, 1, 0},
					{dev.farmings.husbandry.poultry.FowlBreed.COTURNIX, dev.farmings.genetics.PoultryLocus.QUAIL_FAWN, 1, 1},
					{dev.farmings.husbandry.poultry.FowlBreed.COTURNIX, dev.farmings.genetics.PoultryLocus.QUAIL_BROWN, 1, 0},
					{dev.farmings.husbandry.poultry.FowlBreed.COTURNIX, dev.farmings.genetics.PoultryLocus.QUAIL_BROWN, 1, 1},
					{dev.farmings.husbandry.poultry.FowlBreed.COTURNIX, dev.farmings.genetics.PoultryLocus.QUAIL_WHITE, 1, 1}};
			for (int i = 0; i < birds.length; i++) {
				var b = (dev.farmings.husbandry.poultry.FowlBreed) birds[i][0];
				var f = FarmingsEntities.fowl(b.species).create(level, EntitySpawnReason.COMMAND);
				f.initFounder(b, Sex.MALE, 900 + i, b.firstEggDays + 30, level.getGameTime());
				java.util.EnumMap<dev.farmings.genetics.PoultryLocus, int[]> m = new java.util.EnumMap<>(f.genes().alleles());
				for (dev.farmings.genetics.PoultryLocus l : dev.farmings.genetics.PoultryLocus.values()) {
					if (l.ordinal() > dev.farmings.genetics.PoultryLocus.SHELL.ordinal()) {
						m.put(l, new int[] {0, 0});
					}
				}
				m.put((dev.farmings.genetics.PoultryLocus) birds[i][1], new int[] {(int) birds[i][2], (int) birds[i][3]});
				f.setGenes(new dev.farmings.genetics.PoultryGenes(m));
				f.setNoAi(true);
				f.setCustomName(net.minecraft.network.chat.Component.literal(f.look().plumage().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')));
				f.setCustomNameVisible(true);
				f.snapTo(i < 4 ? 62.5 + i * 2.5 : 72.5 + (i - 4) * 1.5, y, 0.5, 180, 0);
				level.addFreshEntity(f);
			}
		});
		server.runCommand("tp @p 70.5 " + (y + 2) + " -5.5 0 15");
		context.waitTicks(40);
		context.takeScreenshot("farmings-loose-plumage");
		// 4. a rotten egg thrown at a cow: it breaks and she gets Nausea
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 100, 0, y, 8, 6, 6);
			var cow = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
			cow.setBreedForDisplay(CattleBreed.HOLSTEIN, Sex.FEMALE, CattleBreed.HOLSTEIN.matureWeightKg, false);
			cow.setNoAi(true);
			cow.snapTo(104.5, y, 0.5, 90, 0);
			level.addFreshEntity(cow);
		});
		server.runCommand("tp @p 100.5 " + (y + 2) + " -6.5 0 15");
		context.waitTicks(30);
		Filming egg = context.computeOnClient(client -> Filming.start(client, "rotten-egg-thrown"));
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var e = new dev.farmings.entity.ThrownRottenEgg(FarmingsEntities.ROTTEN_EGG, level);
			e.setPos(96.5, y + 1.5, 0.5);
			e.shoot(1, 0.12, 0, 1.0F, 0);
			level.addFreshEntity(e);
		});
		context.waitTicks(6);
		context.takeScreenshot("farmings-loose-egg-flying");
		context.waitTicks(30);
		actionbar(server, server.computeOnServer(srv -> srv.overworld().getEntitiesOfClass(dev.farmings.entity.CattleEntity.class,
				new net.minecraft.world.phys.AABB(104, y, 0, 105, y + 2, 1).inflate(2)).stream().findFirst()
				.map(c -> c.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA) ? "The cow has Nausea from the rotten egg" : "missed").orElse("no cow")));
		context.waitTicks(10);
		context.takeScreenshot("farmings-loose-egg-hit");
		egg.stop();
	}

	/** A belt over a rise (docs/conveyors.md, "Slopes"), with grain and hay riding it (loose ends, 2026-11-11). */
	private static void belts(ClientGameTestContext context, TestServerContext server, int y) {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 4, 2, y, 8, 6, 8);
			var belt = dev.farmings.conveyor.ConveyorBlocks.FARM_BELT.defaultBlockState().setValue(dev.farmings.conveyor.ConveyorPieceBlock.FACING, Direction.EAST);
			level.setBlockAndUpdate(new BlockPos(0, y, 2), belt);
			level.setBlockAndUpdate(new BlockPos(1, y, 2), belt.setValue(dev.farmings.conveyor.ConveyorPieceBlock.SLOPE, dev.farmings.conveyor.ConveyorPieceBlock.Slope.UP));
			for (int x = 2; x <= 4; x++) {
				level.setBlockAndUpdate(new BlockPos(x, y + 1, 2), belt);
				level.setBlockAndUpdate(new BlockPos(x, y, 2), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
			}
			level.setBlockAndUpdate(new BlockPos(5, y + 1, 2), belt);
			level.setBlockAndUpdate(new BlockPos(5, y, 2), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(6, y, 2), belt.setValue(dev.farmings.conveyor.ConveyorPieceBlock.SLOPE, dev.farmings.conveyor.ConveyorPieceBlock.Slope.DOWN));
			level.setBlockAndUpdate(new BlockPos(7, y, 2), belt);
			level.setBlockAndUpdate(new BlockPos(8, y, 2), dev.farmings.conveyor.ConveyorBlocks.FARM_BELT_DRIVE.defaultBlockState().setValue(dev.farmings.conveyor.ConveyorDriveBlock.FACING, Direction.EAST));
			level.setBlockAndUpdate(new BlockPos(9, y, 2), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
			var drive = (dev.farmings.conveyor.ConveyorDriveEntity) level.getBlockEntity(new BlockPos(8, y, 2));
			drive.refresh();
			drive.forceRunning(true);
		});
		server.runCommand("tp @p 4.5 " + (y + 4) + " 8.5 180 25");
		context.waitTicks(40);
		actionbar(server, server.computeOnServer(srv -> ((dev.farmings.conveyor.ConveyorDriveEntity) srv.overworld().getBlockEntity(new BlockPos(8, y, 2))).readout()));
		context.takeScreenshot("farmings-belts-over-a-rise");
		Filming film = context.computeOnClient(client -> Filming.start(client, "belt-over-a-rise"));
		for (int i = 0; i < 12; i++) {
			int k = i;
			server.runOnServer(srv -> {
				var drive = (dev.farmings.conveyor.ConveyorDriveEntity) srv.overworld().getBlockEntity(new BlockPos(8, y, 2));
				drive.offer(new ItemStack(k % 3 == 0 ? dev.farmings.crops.FieldCrops.ALFALFA_HAY : k % 3 == 1 ? dev.farmings.crops.FieldCrops.CORN_GRAIN : net.minecraft.world.item.Items.WHEAT, 1));
			});
			context.waitTicks(10);
			if (i == 8) {
				context.takeScreenshot("farmings-belts-items-riding");
			}
		}
		context.waitTicks(60);
		context.takeScreenshot("farmings-belts-items-riding-2");
		server.runCommand("tp @p 10.5 " + (y + 2) + " 2.5 90 15");
		context.waitTicks(20);
		context.takeScreenshot("farmings-belts-side");
		context.waitTicks(40);
		film.stop();
	}

	private static void fields(ClientGameTestContext context, TestServerContext server, int y) {
		// field x 2-9, z 0-15; rails x 1 and 10; the gantry at home row z 0; headland stop (1,y,16); belt west to a forage blower and silo
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 0, 8, y, 16, 14, 14);
			for (int z = 0; z <= 15; z++) {
				level.setBlockAndUpdate(new BlockPos(1, y, z), dev.farmings.gantry.GantryBlocks.FIELD_RAIL.defaultBlockState().setValue(dev.farmings.gantry.GantryPartBlock.AXIS, Direction.Axis.Z));
				level.setBlockAndUpdate(new BlockPos(10, y, z), dev.farmings.gantry.GantryBlocks.FIELD_RAIL.defaultBlockState().setValue(dev.farmings.gantry.GantryPartBlock.AXIS, Direction.Axis.Z));
			}
			level.setBlockAndUpdate(new BlockPos(1, y + 1, 0), dev.farmings.gantry.GantryBlocks.GANTRY_TRUCK.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(10, y + 1, 0), dev.farmings.gantry.GantryBlocks.GANTRY_TRUCK.defaultBlockState());
			for (int x = 1; x <= 10; x++) {
				level.setBlockAndUpdate(new BlockPos(x, y + 2, 0), dev.farmings.gantry.GantryBlocks.GANTRY_BEAM.defaultBlockState().setValue(dev.farmings.gantry.GantryPartBlock.AXIS, Direction.Axis.X));
			}
			level.setBlockAndUpdate(new BlockPos(1, y + 3, 0), dev.farmings.gantry.GantryBlocks.GANTRY_CAB.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(1, y, 16), dev.farmings.gantry.GantryBlocks.HEADLAND_STOP.defaultBlockState());
			for (int x = -5; x <= 0; x++) {
				level.setBlockAndUpdate(new BlockPos(x, y, 16), dev.farmings.conveyor.ConveyorBlocks.FARM_BELT.defaultBlockState().setValue(dev.farmings.conveyor.ConveyorPieceBlock.FACING, Direction.WEST));
			}
			level.setBlockAndUpdate(new BlockPos(-6, y, 16), dev.farmings.conveyor.ConveyorBlocks.FARM_BELT_DRIVE.defaultBlockState().setValue(dev.farmings.conveyor.ConveyorDriveBlock.FACING, Direction.WEST));
			level.setBlockAndUpdate(new BlockPos(-7, y, 16), dev.farmings.silo.SiloBlocks.FORAGE_BLOWER.defaultBlockState());
			for (int k = 0; k < 10; k++) {
				for (int x = -10; x <= -8; x++) {
					for (int z = 15; z <= 17; z++) {
						level.setBlockAndUpdate(new BlockPos(x, y + k, z), x == -9 && z == 16 ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
								: dev.farmings.silo.SiloBlocks.SILO_STAVE.defaultBlockState());
					}
				}
			}
			level.setBlockAndUpdate(new BlockPos(-9, y + 10, 16), dev.farmings.silo.SiloBlocks.SILO_ROOF.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(-11, y, 16), dev.farmings.silo.SiloBlocks.SILO_UNLOADER.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(-12, y, 16), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
			var drive = (dev.farmings.conveyor.ConveyorDriveEntity) level.getBlockEntity(new BlockPos(-6, y, 16));
			drive.refresh();
			drive.forceRunning(true);
			((dev.farmings.silo.BlowerEntity) level.getBlockEntity(new BlockPos(-7, y, 16))).forceRunning(true);
			((dev.farmings.silo.SiloEntity) level.getBlockEntity(new BlockPos(-11, y, 16))).forceRunning(true);
			var c = cab(level, y);
			c.forcePowerForTest(true);
			c.addInput(new ItemStack(dev.farmings.crops.FieldCrops.seed(dev.farmings.farm.crops.FieldCrop.CORN), 64));
			c.addInput(new ItemStack(dev.farmings.crops.FieldCrops.seed(dev.farmings.farm.crops.FieldCrop.CORN), 64));
		});
		server.runCommand("tp @p 15.5 " + (y + 7) + " -2.5 42 27");
		context.waitTicks(60);
		actionbar(server, server.computeOnServer(srv -> cab(srv.overworld(), y).readout()));
		context.takeScreenshot("farmings-fields-gantry-parked");
		// 1. the seed drill tills and sows corn in one pass
		gantryPass(context, server, y, dev.farmings.farm.gantry.GantryTool.SEED_DRILL, "gantry-sows-corn", 60, 1);
		context.takeScreenshot("farmings-fields-corn-sown");
		// 2. grown corn (stage 7, before the 114-day grain stage) is chopped, unloaded at the stop onto the belt and blown into the silo
		server.runOnServer(srv -> sowField(srv.overworld(), y, dev.farmings.crops.FieldCrops.block(dev.farmings.farm.crops.FieldCrop.CORN).defaultBlockState()
				.setValue(net.minecraft.world.level.block.CropBlock.AGE, 7)));
		context.waitTicks(40);
		context.takeScreenshot("farmings-fields-corn-grown");
		// from the headland: the gantry comes toward the stop, unloads onto the belt, and the blower fills the silo
		server.runCommand("tp @p 9.5 " + (y + 7) + " 27.5 145 18");
		context.waitTicks(30);
		gantryPass(context, server, y, dev.farmings.farm.gantry.GantryTool.HARVESTER, "gantry-chops-corn-to-silo", 90, 6);
		String silo = server.computeOnServer(srv -> ((dev.farmings.silo.SiloEntity) srv.overworld().getBlockEntity(new BlockPos(-11, y, 16))).readout());
		System.out.println("[farmings] " + silo);
		actionbar(server, silo.replace("Tower silo: ", ""));
		context.waitTicks(40);
		context.takeScreenshot("farmings-fields-silo-filled");
		server.runOnServer(srv -> ((dev.farmings.silo.SiloEntity) srv.overworld().getBlockEntity(new BlockPos(-11, y, 16))).ageForTest(21));
		context.waitTicks(100);
		actionbar(server, server.computeOnServer(srv -> ((dev.farmings.silo.SiloEntity) srv.overworld().getBlockEntity(new BlockPos(-11, y, 16))).readout().replace("Tower silo: ", "")));
		context.waitTicks(20);
		context.takeScreenshot("farmings-fields-silage-unloads");
		// 3. alfalfa: mown into swaths, raked after a day, baled after a day and a half
		server.runOnServer(srv -> sowField(srv.overworld(), y, dev.farmings.crops.FieldCrops.block(dev.farmings.farm.crops.FieldCrop.ALFALFA).defaultBlockState()
				.setValue(net.minecraft.world.level.block.CropBlock.AGE, 7)));
		server.runCommand("tp @p 15.5 " + (y + 7) + " -2.5 42 27");
		context.waitTicks(40);
		context.takeScreenshot("farmings-fields-alfalfa");
		gantryPass(context, server, y, dev.farmings.farm.gantry.GantryTool.MOWER, "gantry-mows-hay", 60, 1);
		context.takeScreenshot("farmings-fields-swaths");
		server.runOnServer(srv -> cab(srv.overworld(), y).dryForTest(1.0));
		gantryPass(context, server, y, dev.farmings.farm.gantry.GantryTool.RAKE, "gantry-rakes-hay", 60, 1);
		context.takeScreenshot("farmings-fields-windrows");
		server.runOnServer(srv -> cab(srv.overworld(), y).dryForTest(0.5));
		gantryPass(context, server, y, dev.farmings.farm.gantry.GantryTool.BALER, "gantry-bales-hay", 60, 1);
		context.takeScreenshot("farmings-fields-baled");
		// 4. a TMR mixer hands its rations onto a belt that feeds a barn hay feeder (x 20-26, z -6)
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 23, -6, y, 6, 4, 5);
			bigPart(level, dev.farmings.feedmill.FeedMillBlocks.TMR_MIXER, new BlockPos(19, y, -6));     // 2 x 2 x 2: x 19-20, z -6..-5
			for (int x = 21; x <= 24; x++) {
				level.setBlockAndUpdate(new BlockPos(x, y, -6), dev.farmings.conveyor.ConveyorBlocks.FARM_BELT.defaultBlockState().setValue(dev.farmings.conveyor.ConveyorPieceBlock.FACING, Direction.EAST));
			}
			level.setBlockAndUpdate(new BlockPos(25, y, -6), dev.farmings.conveyor.ConveyorBlocks.FARM_BELT_DRIVE.defaultBlockState().setValue(dev.farmings.conveyor.ConveyorDriveBlock.FACING, Direction.EAST));
			barnPart(level, BarnParts.HAY_FEEDER, new BlockPos(26, y, -6));
			var drive = (dev.farmings.conveyor.ConveyorDriveEntity) level.getBlockEntity(new BlockPos(25, y, -6));
			drive.refresh();
			drive.forceRunning(true);
			var cow = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
			cow.setBreedForDisplay(CattleBreed.HOLSTEIN, Sex.FEMALE, CattleBreed.HOLSTEIN.matureWeightKg, false);
			cow.setNoAi(true);
			cow.snapTo(27.5, y, -4.5, 160, 0);
			level.addFreshEntity(cow);
		});
		server.runCommand("tp @p 23.5 " + (y + 3) + " -1.5 180 30");
		context.waitTicks(40);
		Filming mix = context.computeOnClient(client -> Filming.start(client, "mixer-feeds-barn"));
		server.runOnServer(srv -> {
			var m = (dev.farmings.feedmill.FeedMachineEntity) srv.overworld().getBlockEntity(new BlockPos(19, y, -6));
			m.forceRunning(true);
			m.accept(new ItemStack(dev.farmings.crops.FieldCrops.CORN_SILAGE, 6));
			m.accept(new ItemStack(dev.farmings.crops.FieldCrops.ALFALFA_HAY, 20));
			m.accept(new ItemStack(dev.farmings.crops.FieldCrops.CORN_GRAIN, 64));
			m.accept(new ItemStack(dev.farmings.feedmill.FeedItems.FISH_MEAL, 10));
			m.accept(new ItemStack(dev.farmings.feedmill.FeedItems.MINERAL_PREMIX, 3));
		});
		for (int i = 0; i < 14; i++) {
			context.waitTicks(20);
			actionbar(server, server.computeOnServer(srv -> {
				ServerLevel level = srv.overworld();
				var m = (dev.farmings.feedmill.FeedMachineEntity) level.getBlockEntity(new BlockPos(19, y, -6));
				var f = (FeederEntity) level.getBlockEntity(new BlockPos(26, y, -6));
				var a = f.analysis();
				return String.format(java.util.Locale.ROOT, "%s | feeder %.0f kg DM, CP %.1f%%, NDF %.1f%%, TDN %.1f%%",
						m.readout().replaceAll(" \\(CP.*\\)", ""), f.dryMatterKg(), a.cp(), a.ndf(), a.tdn());
			}));
			if (i == 6) {
				context.takeScreenshot("farmings-fields-mixer-running");
			}
		}
		context.takeScreenshot("farmings-fields-feeder-filled");
		mix.stop();
		// 5. a compost windrow of manure, straw and litter, turned five times over eight weeks (x 20-26, z 6)
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, 23, 6, y, 6, 3, 4);
			level.setBlockAndUpdate(new BlockPos(20, y, 6), dev.farmings.compost.CompostBlocks.COMPOST_CONTROLLER.defaultBlockState());
			for (int x = 21; x <= 24; x++) {
				level.setBlockAndUpdate(new BlockPos(x, y, 6), dev.farmings.compost.CompostBlocks.COMPOST_PAD.defaultBlockState());
			}
			level.setBlockAndUpdate(new BlockPos(25, y, 6), dev.farmings.compost.CompostBlocks.WINDROW_TURNER.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(20, y, 7), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
			((dev.farmings.compost.TurnerEntity) level.getBlockEntity(new BlockPos(25, y, 6))).forceRunning(true);
			var c = (dev.farmings.compost.CompostControllerEntity) level.getBlockEntity(new BlockPos(20, y, 6));
			c.accept(new ItemStack(FarmingsItems.MANURE, 10));
			c.accept(new ItemStack(dev.farmings.crops.FieldCrops.STRAW, 20));
			c.accept(new ItemStack(FarmingsItems.POULTRY_LITTER, 10));
			c.start();
		});
		server.runCommand("tp @p 22.5 " + (y + 3) + " 10.5 180 30");
		context.waitTicks(40);
		actionbar(server, server.computeOnServer(srv -> ((dev.farmings.compost.CompostControllerEntity) srv.overworld().getBlockEntity(new BlockPos(20, y, 6))).readout()));
		context.waitTicks(10);
		context.takeScreenshot("farmings-fields-windrow");
		Filming turn = context.computeOnClient(client -> Filming.start(client, "compost-turned"));
		for (int i = 0; i < 6; i++) {
			server.runOnServer(srv -> ((dev.farmings.compost.CompostControllerEntity) srv.overworld().getBlockEntity(new BlockPos(20, y, 6)))
					.step(srv.overworld(), dev.farmings.farm.compost.Windrow.TURN_EVERY_DAYS));
			for (int k = 0; k < 2; k++) {
				context.waitTicks(20);
				actionbar(server, server.computeOnServer(srv -> ((dev.farmings.compost.CompostControllerEntity) srv.overworld().getBlockEntity(new BlockPos(20, y, 6))).readout()));
			}
		}
		context.takeScreenshot("farmings-fields-compost-done");
		turn.stop();
		// 6. a digester full of a dairy's manure, its gas holder piped to a Wildcatter gas set that runs on it (x -20.., z -20..)
		int gx = -24;
		int gz = -20;
		BlockPos panelAt = new BlockPos(gx + 8, y, gz);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clear(level, gx + 5, gz - 1, y, 9, 7, 8);
			// the full-size tank: x gx+2..gx+4, y..y+2, z gz-4..gz-2 (main cell at the bottom centre)
			bigPart(level, dev.farmings.digester.DigesterBlocks.DIGESTER_TANK, new BlockPos(gx + 3, y, gz - 3));
			level.setBlockAndUpdate(new BlockPos(gx + 3, y + 3, gz - 3), dev.farmings.digester.DigesterBlocks.GAS_HOLDER.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(gx + 1, y, gz - 3), dev.farmings.digester.DigesterBlocks.FEED_PIT.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(gx + 5, y, gz - 3), dev.farmings.digester.DigesterBlocks.DIGESTATE_TANK.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(gx + 6, y, gz - 3), dev.farmings.digester.DigesterBlocks.SCREW_PRESS.defaultBlockState());
			var d = (dev.farmings.digester.DigesterEntity) level.getBlockEntity(new BlockPos(gx + 3, y, gz - 3));
			d.digester().feed(20_000);
			d.forceRunning(true);
			d.step(20);
			d.forceRunning(false);     // from here the agitator runs on the gas set's power: a real load on it
			var gasLine = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("gas_line"));
			for (BlockPos p : new BlockPos[] {new BlockPos(gx + 3, y + 3, gz - 2), new BlockPos(gx + 3, y + 3, gz - 1), new BlockPos(gx + 3, y + 2, gz - 1),
					new BlockPos(gx + 3, y + 1, gz - 1), new BlockPos(gx + 3, y, gz - 1)}) {
				level.setBlockAndUpdate(p, gasLine.defaultBlockState());     // from the holder's side, down to the engine's north side
			}
			// 480 V cable from the gas set's panel to the tank's east side (Wildcatter's own wiring, as its cracker_pump scene)
			var cable = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("cable_industrial"));
			for (int z = gz; z >= gz - 4; z--) {
				level.setBlockAndUpdate(new BlockPos(gx + 9, y, z), cable.defaultBlockState());
			}
			for (int x = gx + 8; x >= gx + 5; x--) {
				level.setBlockAndUpdate(new BlockPos(x, y, gz - 4), cable.defaultBlockState());
			}
			dev.wildcatter.block.GensetSkid.build(level, panelAt, Direction.NORTH, true);
		});
		server.runCommand("tp @p " + (gx + 5.5) + " " + (y + 5) + " " + (gz + 7.5) + " 170 28");
		context.waitTicks(40);
		actionbar(server, server.computeOnServer(srv -> ((dev.farmings.digester.DigesterEntity) srv.overworld().getBlockEntity(new BlockPos(gx + 3, y, gz - 3))).readout()));
		context.waitTicks(10);
		context.takeScreenshot("farmings-fields-digester");
		Filming gen = context.computeOnClient(client -> Filming.start(client, "digester-runs-genset"));
		server.runOnServer(srv -> ((dev.wildcatter.block.GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panelAt)).setWantRun(true));
		for (int i = 0; i < 20; i++) {
			context.waitTicks(20);
			actionbar(server, server.computeOnServer(srv -> {
				ServerLevel level = srv.overworld();
				var p = (dev.wildcatter.block.GensetControlPanelBlockEntity) level.getBlockEntity(panelAt);
				var d = (dev.farmings.digester.DigesterEntity) level.getBlockEntity(new BlockPos(gx + 3, y, gz - 3));
				return String.format(java.util.Locale.ROOT, "Gas set burned %.2f m3 of farm biogas; holder %.1f m3; agitator %s", p.biogasBurned(), d.digester().gasM3(),
						d.powered() ? "ON (on the set's power)" : "off");
			}));
			if (i == 14) {
				context.takeScreenshot("farmings-fields-genset-running");
			}
		}
		gen.stop();
		System.out.println("[farmings] genset: " + server.computeOnServer(srv -> ((dev.wildcatter.block.GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panelAt)).inspect().getString()));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		// the taming scene needs a generated world (biomes and their animals); every other scene uses the flat test world
		boolean generated = "taming".equals(System.getenv("FARMINGS_ONLY"));
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(!generated).create()) {
			TestServerContext server = world.getServer();
			context.waitTicks(40);
			int y = server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0));
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			String only = System.getenv("FARMINGS_ONLY");
			if ("showroom".equals(only)) {
				context.waitTicks(200);
				showroom(context, server, y);
				return;
			}
			if ("fields".equals(only)) {
				context.waitTicks(200);
				fields(context, server, y);
				return;
			}
			if ("belts".equals(only)) {
				context.waitTicks(200);
				belts(context, server, y);
				return;
			}
			if ("looseends".equals(only)) {
				context.waitTicks(200);
				looseEnds(context, server, y);
				return;
			}
			if ("poultryfarm".equals(only)) {
				context.waitTicks(200);
				poultryFarm(context, server, y);
				return;
			}
			if ("poultry".equals(only)) {
				context.waitTicks(200);
				poultry(context, server, y);
				return;
			}
			if ("taming".equals(only)) {
				context.waitTicks(200);
				taming(context, server, y);
				return;
			}
			if ("sheepfarm".equals(only)) {
				context.waitTicks(200);
				sheepFarm(context, server, y);
				return;
			}
			if ("flock".equals(only)) {
				context.waitTicks(200);
				flock(context, server, y);
				return;
			}
			if ("coats".equals(only)) {
				context.waitTicks(200);
				coats(context, server, y);
				return;
			}
			if ("breeding".equals(only)) {
				context.waitTicks(200);
				breeding(context, server, y);
				return;
			}
			if ("beef".equals(only)) {
				context.waitTicks(200);
				beef(context, server, y);
				return;
			}
			if ("creamery".equals(only)) {
				context.waitTicks(200);
				creamery(context, server, y);
				return;
			}
			if ("armor".equals(only)) {
				context.waitTicks(200);
				milkArmor(context, server, y);
				return;
			}
			if ("parlor".equals(only) || "dairy".equals(only)) {
				context.waitTicks(200);
				dairy(context, server, y);
				return;
			}
			if ("fridge".equals(only)) {
				context.waitTicks(200);
				fridge(context, server, y);
				return;
			}
			if ("hoppers".equals(only)) {
				context.waitTicks(200);
				hoppers(context, server, y);
				return;
			}
			if ("calving".equals(System.getenv("FARMINGS_ONLY"))) {
				context.waitTicks(200);
				calving(context, server, y);
				return;
			}
			if ("herd".equals(System.getenv("FARMINGS_ONLY"))) {
				context.waitTicks(200);
				herd(context, server, y);
				return;
			}
			if ("scraper".equals(System.getenv("FARMINGS_ONLY"))) {
				context.waitTicks(200);
				scraper(context, server, y);
				return;
			}
			if ("barn".equals(System.getenv("FARMINGS_ONLY"))) {
				context.waitTicks(200);
				barn(context, server, y);
				return;
			}
			int z0 = 40;
			CattleEntity[] herd = new CattleEntity[BREEDS.length];
			server.runOnServer(srv -> {
				ServerLevel level = srv.overworld();
				for (int x = -6; x <= 14; x++) {
					for (int z = z0 - 2; z <= z0 + 22; z++) {
						level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
					}
				}
				for (int i = 0; i < BREEDS.length; i++) {
					CattleEntity c = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
					c.setBreedForDisplay(BREEDS[i], Sex.FEMALE, BREEDS[i].matureWeightKg, true);
					c.setNoAi(true);
					c.setPos(0.5, y, z0 + 3 * i + 0.5);       // a line along z, all facing south: the side view
					herd[i] = c;
					level.addFreshEntity(c);
				}
			});
			context.waitTicks(200);   // let the game-mode chat message fade out of the shot
			shoot(context, server, herd, y, z0, "herd");
			// calves, a bull, a pregnant Jersey and Wagyu: the other stages
			server.runOnServer(srv -> {
				herd[0].setBreedForDisplay(CattleBreed.HOLSTEIN, Sex.FEMALE, 120, false);
				herd[1].setBreedForDisplay(CattleBreed.GYR, Sex.FEMALE, 90, false);
				herd[2].setBreedForDisplay(CattleBreed.BROWN_SWISS, Sex.MALE, 600, false);
				herd[3].setBreedForDisplay(CattleBreed.JERSEY, Sex.FEMALE, 470, true);
				herd[4].setBreedForDisplay(CattleBreed.ANGUS, Sex.MALE, 550, false);
				herd[5].setBreedForDisplay(CattleBreed.WAGYU, Sex.FEMALE, 487, true);
			});
			context.waitTicks(30);
			shoot(context, server, herd, y, z0, "mixed");
		}
	}
}
