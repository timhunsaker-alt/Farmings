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
	private static void parlor(ClientGameTestContext context, TestServerContext server, int y) {
		int cx = 60;
		int cz = 60;
		int stalls = 4;
		BlockPos c = new BlockPos(cx, y + 1, cz);
		CattleEntity[] cows = new CattleEntity[4];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = cx - 12; x <= cx + 8; x++) {
				for (int z = cz - 6; z <= cz + 14; z++) {
					level.setBlockAndUpdate(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
					for (int h = 0; h < 6; h++) {
						level.setBlockAndUpdate(new BlockPos(x, y + h, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
					}
				}
			}
			int fl = c.getY() - 1;
			level.setBlock(c, FarmingsBlocks.PARLOR_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
			parlorPart(level, dev.farmings.parlor.ParlorParts.BULK_TANK, new BlockPos(c.getX() + 1, fl, c.getZ()));
			int xp = c.getX() - stalls - 1;
			parlorPart(level, dev.farmings.parlor.ParlorParts.VACUUM_PUMP, new BlockPos(xp, fl, c.getZ()));
			for (int x = xp + 1; x <= c.getX(); x++) {
				parlorPart(level, dev.farmings.parlor.ParlorParts.MILK_LINE, new BlockPos(x, fl, c.getZ()));
			}
			for (int x = xp + 1; x < c.getX(); x++) {
				parlorPart(level, dev.farmings.parlor.ParlorParts.MILKING_UNIT, new BlockPos(x, c.getY(), c.getZ()));
				parlorPart(level, dev.farmings.parlor.ParlorParts.PARLOR_STALL, new BlockPos(x, fl, c.getZ() + 1));
			}
			dev.farmings.parlor.ParlorControllerEntity ctl = (dev.farmings.parlor.ParlorControllerEntity) level.getBlockEntity(c);
			ctl.refresh(level);
			ctl.pump(level).forceRunning(true);
			ctl.tank(level).forceChilling(true);
			for (int i = 0; i < cows.length; i++) {
				CattleEntity cow = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
				cow.initFounder(CattleBreed.HOLSTEIN, Sex.FEMALE, 90L + i, 900, 650, level.getGameTime());
				cow.setCalvedForDisplay(30);
				cow.setPos(cx - 5 + i * 1.5, y, cz + 9 + 0.5);
				cow.settle(level);
				cows[i] = cow;
				level.addFreshEntity(cow);
			}
		});
		context.waitTicks(40);
		server.runCommand("tp @p " + (cx - 2) + " " + (y + 6) + " " + (cz + 12) + " 180 45");
		context.waitTicks(20);
		Filming film = context.computeOnClient(client -> Filming.start(client, "parlor"));
		context.waitTicks(20);
		context.takeScreenshot("farmings-parlor-start");
		for (int i = 0; i < 16; i++) {
			context.waitTicks(20);
			String text = server.computeOnServer(srv -> {
				dev.farmings.parlor.ParlorControllerEntity ctl = (dev.farmings.parlor.ParlorControllerEntity) srv.overworld().getBlockEntity(c);
				ctl.milk(srv.overworld(), 20);
				return ctl.readout(srv.overworld());
			});
			server.runCommand("title @p actionbar \"" + text + "\"");
			if (i == 5) {
				context.takeScreenshot("farmings-parlor-walking");
			}
		}
		context.takeScreenshot("farmings-parlor-milked");
		film.stop();
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

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
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
			if ("parlor".equals(System.getenv("FARMINGS_ONLY"))) {
				context.waitTicks(200);
				parlor(context, server, y);
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
