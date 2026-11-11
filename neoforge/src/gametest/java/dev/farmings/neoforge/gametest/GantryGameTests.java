package dev.farmings.neoforge.gametest;

import dev.farmings.crops.FieldCrops;
import dev.farmings.crops.PerennialCropBlock;
import dev.farmings.farm.crops.FieldCrop;
import dev.farmings.farm.gantry.GantryTool;
import dev.farmings.gantry.GantryBlocks;
import dev.farmings.gantry.GantryCabEntity;
import dev.farmings.gantry.GantryPartBlock;
import dev.farmings.gantry.SwathBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The field gantry (docs/multiblocks/field-gantry.md). */
final class GantryGameTests {
	private GantryGameTests() {
	}

	/**
	 * A field x 2-6 (span 5) by z 0-8 of dirt, rails along z at x 1 and 7, the gantry at its home row z 0 (trucks y 2, beam y 3, cab
	 * y 4 over x 1), a headland stop at (1,1,9) and a chest beside it at (0,1,9). Power is forced.
	 */
	static BlockPos build(GameTestHelper helper) {
		for (int x = 0; x <= 8; x++) {
			for (int z = 0; z <= 9; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
				for (int y = 1; y <= 5; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		for (int z = 0; z <= 8; z++) {
			helper.setBlock(new BlockPos(1, 1, z), GantryBlocks.FIELD_RAIL.defaultBlockState().setValue(GantryPartBlock.AXIS, Direction.Axis.Z));
			helper.setBlock(new BlockPos(7, 1, z), GantryBlocks.FIELD_RAIL.defaultBlockState().setValue(GantryPartBlock.AXIS, Direction.Axis.Z));
		}
		helper.setBlock(new BlockPos(1, 2, 0), GantryBlocks.GANTRY_TRUCK);
		helper.setBlock(new BlockPos(7, 2, 0), GantryBlocks.GANTRY_TRUCK);
		for (int x = 1; x <= 7; x++) {
			helper.setBlock(new BlockPos(x, 3, 0), GantryBlocks.GANTRY_BEAM.defaultBlockState().setValue(GantryPartBlock.AXIS, Direction.Axis.X));
		}
		helper.setBlock(new BlockPos(1, 4, 0), GantryBlocks.GANTRY_CAB);
		helper.setBlock(new BlockPos(1, 1, 9), GantryBlocks.HEADLAND_STOP);
		helper.setBlock(new BlockPos(0, 1, 9), Blocks.CHEST);
		cab(helper).forcePowerForTest(true);
		return helper.absolutePos(new BlockPos(1, 4, 0));
	}

	static GantryCabEntity cab(GameTestHelper helper) {
		return (GantryCabEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 4, 0)));
	}

	static GantryCabEntity run(GameTestHelper helper, GantryTool tool) {
		GantryCabEntity c = cab(helper);
		c.fitTool(tool);
		c.toggle();
		helper.assertTrue(c.phase() == GantryCabEntity.Phase.WORKING, Component.literal("it starts: " + c.readout()));
		c.runForTest(helper.getLevel(), 200);
		GantryCabEntity home = cab(helper);
		helper.assertTrue(home != null && home.phase() == GantryCabEntity.Phase.PARKED && home.step() == 0,
				Component.literal("it works the field and comes home: " + (home == null ? "no cab at home" : home.readout())));
		return home;
	}

	static void cells(GameTestHelper helper, java.util.function.BiConsumer<BlockPos, BlockPos> each) {
		for (int x = 2; x <= 6; x++) {
			for (int z = 0; z <= 8; z++) {
				each.accept(new BlockPos(x, 1, z), new BlockPos(x, 0, z));
			}
		}
	}

	/** The seed drill tills and sows every cell between the rails in one pass, and comes home. */
	static void gantryPlantsStrip(GameTestHelper helper) {
		build(helper);
		cab(helper).addInput(new ItemStack(FieldCrops.seed(FieldCrop.BARLEY), 64));
		run(helper, GantryTool.SEED_DRILL);
		int[] sown = {0};
		cells(helper, (crop, ground) -> {
			if (helper.getBlockState(crop).getBlock() == FieldCrops.block(FieldCrop.BARLEY) && helper.getBlockState(ground).is(Blocks.FARMLAND)) {
				sown[0]++;
			}
		});
		helper.assertTrue(sown[0] == 45, Component.literal("45 cells tilled and sown: " + sown[0]));
		helper.succeed();
	}

	/** The combine takes ripe barley (4.13 kg a block), leaves straw swaths, and unloads the grain at the headland stop. */
	static void gantryHarvestsToStop(GameTestHelper helper) {
		build(helper);
		cells(helper, (crop, ground) -> {
			helper.setBlock(ground, Blocks.FARMLAND);
			helper.setBlock(crop, FieldCrops.block(FieldCrop.BARLEY).defaultBlockState().setValue(CropBlock.AGE, 7));
		});
		run(helper, GantryTool.HARVESTER);
		var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(0, 1, 9)));
		int grain = chest.countItem(FieldCrops.BARLEY_GRAIN);
		helper.assertTrue(grain == (int) Math.floor(45 * 4.13), Component.literal("185 kg of barley in the chest: " + grain));
		helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 4)).getBlock() instanceof SwathBlock, Component.literal("straw lies in a swath"));
		helper.succeed();
	}

	/** Mown alfalfa dries in the swath; the rake waits a day, the baler a day and a half; the hay goes to the bin; the plants regrow. */
	static void gantryMowsRakesBales(GameTestHelper helper) {
		build(helper);
		cells(helper, (crop, ground) -> {
			helper.setBlock(ground, Blocks.FARMLAND);
			helper.setBlock(crop, FieldCrops.block(FieldCrop.ALFALFA).defaultBlockState().setValue(CropBlock.AGE, 7));
		});
		run(helper, GantryTool.MOWER);
		BlockState s = helper.getBlockState(new BlockPos(4, 1, 3));
		helper.assertTrue(s.getValue(PerennialCropBlock.SWATH) == 1 && s.getValue(CropBlock.AGE) == FieldCrop.REGROWTH_STAGE,
				Component.literal("cut, regrowing, a swath on it: " + s));
		run(helper, GantryTool.RAKE);
		helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 3)).getValue(PerennialCropBlock.SWATH) == 1,
				Component.literal("too wet to rake on the day it was cut"));
		cab(helper).dryForTest(1.0);
		run(helper, GantryTool.RAKE);
		helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 3)).getValue(PerennialCropBlock.SWATH) == 2, Component.literal("raked after a day"));
		cab(helper).dryForTest(0.5);
		GantryCabEntity c = run(helper, GantryTool.BALER);
		double hay = c.bin().getOrDefault("alfalfa_hay", 0.0);
		helper.assertTrue(Math.abs(hay - 45 * FieldCrop.ALFALFA.hayKg()) < 0.01, Component.literal("101 kg of hay in the bin, short of a bale: " + hay));
		helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 3)).getValue(PerennialCropBlock.SWATH) == 0, Component.literal("the swath is picked up"));
		helper.succeed();
	}

	/** On its own clock (a block every 0.45 s with the drill), the gantry sows the field and parks at home again. */
	static void gantryRunsOnItsClock(GameTestHelper helper) {
		build(helper);
		GantryCabEntity c = cab(helper);
		c.addInput(new ItemStack(FieldCrops.seed(FieldCrop.OATS), 64));
		c.fitTool(GantryTool.SEED_DRILL);
		c.toggle();
		TamingGameTests.waitFor(helper, 0, 600, () -> {
			GantryCabEntity home = cab(helper);
			return home != null && home.phase() == GantryCabEntity.Phase.PARKED && helper.getBlockState(new BlockPos(6, 1, 8)).getBlock() == FieldCrops.block(FieldCrop.OATS);
		}, "the gantry sows to the far end and comes home", helper::succeed);
	}
}
