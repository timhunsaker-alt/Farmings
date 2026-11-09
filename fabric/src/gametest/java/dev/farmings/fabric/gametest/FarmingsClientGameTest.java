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
		int xd = c.getX() - (2 * stalls + 1);
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
				barnPart(level, BarnParts.STALL_DIVIDER, new BlockPos(xd + 2 * j, fl, firstZ));
			}
			for (int i = 0; i < stalls; i++) {
				int x = xd + 1 + 2 * i;
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

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			context.waitTicks(40);
			int y = server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0));
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
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
