package dev.farmings.neoforge.gametest;

import dev.farmings.conveyor.ConveyorBlocks;
import dev.farmings.conveyor.ConveyorDriveBlock;
import dev.farmings.conveyor.ConveyorDriveEntity;
import dev.farmings.conveyor.ConveyorPieceBlock;
import dev.farmings.crops.FieldCrops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Belts and augers (docs/conveyors.md). */
final class ConveyorGameTests {
	private ConveyorGameTests() {
	}

	/** A 6 m belt east from x 1 to 6, its drive at x 7 facing east, a chest at x 8. */
	static ConveyorDriveEntity belt(GameTestHelper helper, boolean chest) {
		FarmingsGameTests.floor(helper);
		for (int x = 1; x <= 6; x++) {
			helper.setBlock(new BlockPos(x, 1, 3), ConveyorBlocks.FARM_BELT.defaultBlockState().setValue(ConveyorPieceBlock.FACING, Direction.EAST));
		}
		helper.setBlock(new BlockPos(7, 1, 3), ConveyorBlocks.FARM_BELT_DRIVE.defaultBlockState().setValue(ConveyorDriveBlock.FACING, Direction.EAST));
		if (chest) {
			helper.setBlock(new BlockPos(8, 1, 3), Blocks.CHEST);
		}
		ConveyorDriveEntity d = (ConveyorDriveEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(7, 1, 3)));
		d.refresh();
		d.forceRunning(true);
		return d;
	}

	/** Grain dropped on the tail rides 6 m at 2 m/s (3 s) into the chest at the head; the readout knows its run. */
	static void beltCarriesToChest(GameTestHelper helper) {
		ConveyorDriveEntity d = belt(helper, true);
		ServerLevel level = helper.getLevel();
		helper.assertTrue(d.length() == 6, Component.literal("a 6-cell run: " + d.readout()));
		BlockPos tail = helper.absolutePos(new BlockPos(1, 1, 3));
		ItemEntity e = new ItemEntity(level, tail.getX() + 0.5, tail.getY() + 0.3, tail.getZ() + 0.5, new ItemStack(FieldCrops.CORN_GRAIN, 8));
		e.setDeltaMovement(0, 0, 0);
		level.addFreshEntity(e);
		helper.runAfterDelay(30, () -> {
			var chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(8, 1, 3)));
			helper.assertTrue(chest.isEmpty() && !d.parcels().isEmpty(), Component.literal("still riding after 1.5 s: " + d.readout()));
			TamingGameTests.waitFor(helper, 0, 100, () -> chest.countItem(FieldCrops.CORN_GRAIN) == 8, "the grain reaches the chest: " + d.readout(), helper::succeed);
		});
	}

	/** With nothing to take it at the head, the load waits there and the run reads HEAD BLOCKED. */
	static void blockedHeadHolds(GameTestHelper helper) {
		ConveyorDriveEntity d = belt(helper, false);
		d.offer(new ItemStack(Items.WHEAT, 4));
		helper.runAfterDelay(80, () -> {
			helper.assertTrue(d.blocked() && d.parcels().size() == 1 && d.delivered() == 0, Component.literal(d.readout()));
			helper.succeed();
		});
	}

	/** An auger lifts grain straight up into a chest, and refuses what is not grain. */
	static void augerLiftsGrain(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		for (int y = 1; y <= 3; y++) {
			helper.setBlock(new BlockPos(3, y, 3), ConveyorBlocks.GRAIN_AUGER.defaultBlockState().setValue(ConveyorPieceBlock.FACING, Direction.UP));
		}
		helper.setBlock(new BlockPos(3, 4, 3), ConveyorBlocks.GRAIN_AUGER_DRIVE.defaultBlockState().setValue(ConveyorDriveBlock.FACING, Direction.UP));
		helper.setBlock(new BlockPos(3, 5, 3), Blocks.CHEST);
		ConveyorDriveEntity d = (ConveyorDriveEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 4, 3)));
		d.refresh();
		d.forceRunning(true);
		helper.assertTrue(!d.offer(new ItemStack(Items.COBBLESTONE)), Component.literal("an auger takes no cobblestone"));
		helper.assertTrue(d.offer(new ItemStack(FieldCrops.OAT_GRAIN, 16)), Component.literal("it takes oats"));
		var chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 5, 3)));
		TamingGameTests.waitFor(helper, 0, 120, () -> chest.countItem(FieldCrops.OAT_GRAIN) == 16, "the oats go up: " + d.readout(), helper::succeed);
	}

	/**
	 * A belt climbs: flat, a rising cell, three flat cells a block up, then down again into a chest (docs/conveyors.md, "Slopes"). Two slopes
	 * within three cells are too steep and the run doesn't move.
	 */
	static void beltClimbsSlope(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		net.minecraft.world.level.block.state.BlockState belt = ConveyorBlocks.FARM_BELT.defaultBlockState().setValue(ConveyorPieceBlock.FACING, Direction.EAST);
		helper.setBlock(new BlockPos(1, 1, 3), belt);
		helper.setBlock(new BlockPos(2, 1, 3), belt.setValue(ConveyorPieceBlock.SLOPE, ConveyorPieceBlock.Slope.UP));
		for (int x = 3; x <= 5; x++) {
			helper.setBlock(new BlockPos(x, 2, 3), belt);
		}
		helper.setBlock(new BlockPos(6, 1, 3), belt.setValue(ConveyorPieceBlock.SLOPE, ConveyorPieceBlock.Slope.DOWN));
		helper.setBlock(new BlockPos(7, 1, 3), belt);
		helper.setBlock(new BlockPos(8, 1, 3), ConveyorBlocks.FARM_BELT_DRIVE.defaultBlockState().setValue(ConveyorDriveBlock.FACING, Direction.EAST));
		helper.setBlock(new BlockPos(9, 1, 3), Blocks.CHEST);
		ConveyorDriveEntity d = (ConveyorDriveEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(8, 1, 3)));
		d.refresh();
		d.forceRunning(true);
		helper.assertTrue(d.length() == 7 && !d.tooSteep(), Component.literal("a 7-cell run over a rise: " + d.readout()));
		helper.assertTrue(d.readout().contains(String.format(java.util.Locale.ROOT, "%.1f m", 5 + 2 * Math.sqrt(2))), Component.literal("its sloped cells are longer: " + d.readout()));
		helper.assertTrue(ConveyorDriveEntity.driveOf(level, helper.absolutePos(new BlockPos(1, 1, 3))) == d, Component.literal("the tail cell finds its drive up and down the slope"));
		d.offer(new ItemStack(FieldCrops.CORN_GRAIN, 4));
		var chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(9, 1, 3)));
		TamingGameTests.waitFor(helper, 0, 120, () -> chest.countItem(FieldCrops.CORN_GRAIN) == 4, () -> "the grain rides over the rise: " + d.readout(), () -> {
			// two rises one after the other, on a second run: too steep
			helper.setBlock(new BlockPos(1, 1, 6), belt.setValue(ConveyorPieceBlock.SLOPE, ConveyorPieceBlock.Slope.UP));
			helper.setBlock(new BlockPos(2, 2, 6), belt.setValue(ConveyorPieceBlock.SLOPE, ConveyorPieceBlock.Slope.UP));
			helper.setBlock(new BlockPos(3, 3, 6), belt);
			helper.setBlock(new BlockPos(4, 3, 6), ConveyorBlocks.FARM_BELT_DRIVE.defaultBlockState().setValue(ConveyorDriveBlock.FACING, Direction.EAST));
			ConveyorDriveEntity steep = (ConveyorDriveEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(4, 3, 6)));
			steep.refresh();
			helper.assertTrue(steep.length() == 3 && steep.tooSteep() && steep.readout().contains("TOO STEEP"), Component.literal("two slopes in three cells: " + steep.readout()));
			helper.succeed();
		});
	}
}
