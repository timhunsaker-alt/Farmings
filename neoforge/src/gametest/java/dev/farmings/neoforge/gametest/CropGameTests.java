package dev.farmings.neoforge.gametest;

import dev.farmings.crops.CornCropBlock;
import dev.farmings.crops.FieldCropBlock;
import dev.farmings.crops.FieldCrops;
import dev.farmings.crops.PerennialCropBlock;
import dev.farmings.farm.crops.FieldCrop;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Phase 3 crops (docs/crops.md). */
final class CropGameTests {
	private CropGameTests() {
	}

	/** Farmland at y 0 under the cell, the crop at y 1. */
	static BlockPos plant(GameTestHelper helper, FieldCrop c, int x, int z) {
		helper.setBlock(new BlockPos(x, 0, z), Blocks.FARMLAND);
		helper.setBlock(new BlockPos(x, 1, z), FieldCrops.block(c));
		return helper.absolutePos(new BlockPos(x, 1, z));
	}

	/** A planted crop schedules its next stage on the 15:1 clock, and seven stages bring it to harvest. */
	static void cropGrowsOnClock(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos p = plant(helper, FieldCrop.BARLEY, 2, 2);
		FieldCropBlock b = (FieldCropBlock) FieldCrops.block(FieldCrop.BARLEY);
		helper.assertTrue(level.getBlockTicks().hasScheduledTick(p, b), Component.literal("planting schedules the next stage"));
		helper.assertTrue(!level.getBlockState(p).isRandomlyTicking(), Component.literal("no random ticks"));
		for (int i = 0; i < 7; i++) {
			b.stageForTest(level, p);
		}
		helper.assertTrue(b.isMaxAge(level.getBlockState(p)), Component.literal("ripe after 7 stages"));
		helper.assertTrue(FieldCrop.BARLEY.stageTicks() * 7 == dev.farmings.husbandry.TimeScale.realDaysToTicks(FieldCrop.BARLEY.harvestDays) / 7 * 7,
				Component.literal(FieldCrop.BARLEY.harvestDays + " days to harvest on the 15:1 clock"));
		helper.succeed();
	}

	/** Cut alfalfa drops back to stage 3 and counts the cut; after its stand's 22 cuts it dies. */
	static void alfalfaRegrowsAndDies(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos p = plant(helper, FieldCrop.ALFALFA, 2, 2);
		PerennialCropBlock b = (PerennialCropBlock) FieldCrops.block(FieldCrop.ALFALFA);
		helper.assertTrue(b.cut(level, p) == 0, Component.literal("an unripe plant gives nothing"));
		level.setBlock(p, level.getBlockState(p).setValue(CropBlock.AGE, 7), 2);
		double kg = b.cut(level, p);
		BlockState s = level.getBlockState(p);
		helper.assertTrue(kg == 2.0 && s.getValue(CropBlock.AGE) == FieldCrop.REGROWTH_STAGE && s.getValue(PerennialCropBlock.CUTS) == 1,
				Component.literal("2 kg DM a cut, back to stage 3, one cut: " + kg + " " + s));
		helper.assertTrue(level.getBlockTicks().hasScheduledTick(p, b), Component.literal("it regrows on the clock"));
		level.setBlock(p, s.setValue(CropBlock.AGE, 7).setValue(PerennialCropBlock.CUTS, 21), 2);
		b.cut(level, p);
		helper.assertTrue(level.getBlockState(p).isAir(), Component.literal("the 22nd cut ends the stand"));
		helper.succeed();
	}

	/** Corn at stage 7 (125 days) is ready to chop; 5 days later (130, R6) it is ripe for grain. */
	static void cornRipensForGrain(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos p = plant(helper, FieldCrop.CORN, 3, 3);
		CornCropBlock b = (CornCropBlock) FieldCrops.block(FieldCrop.CORN);
		for (int i = 0; i < 7; i++) {
			b.stageForTest(level, p);
		}
		helper.assertTrue(CornCropBlock.silageReady(level.getBlockState(p)) && !CornCropBlock.grainReady(level.getBlockState(p)),
				Component.literal("ready to chop, not yet for grain"));
		b.stageForTest(level, p);
		helper.assertTrue(CornCropBlock.grainReady(level.getBlockState(p)), Component.literal("ripe for grain"));
		helper.succeed();
	}

	/** Ripe oats broken by hand give 2 kg of grain and seed back. */
	static void ripeOatsByHand(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos p = plant(helper, FieldCrop.OATS, 2, 2);
		level.setBlock(p, level.getBlockState(p).setValue(CropBlock.AGE, 7), 2);
		level.destroyBlock(p, true);
		helper.runAfterDelay(3, () -> {
			var items = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(p).inflate(2));
			int grain = items.stream().filter(i -> i.getItem().is(FieldCrops.OAT_GRAIN)).mapToInt(i -> i.getItem().getCount()).sum();
			int seed = items.stream().filter(i -> i.getItem().is(FieldCrops.seed(FieldCrop.OATS))).mapToInt(i -> i.getItem().getCount()).sum();
			helper.assertTrue(grain == 2 && seed >= 1, Component.literal("2 grain and seed: " + grain + ", " + seed));
			helper.assertTrue(dev.farmings.crops.GrassSeeds.crops("plains").contains(FieldCrop.CORN), Component.literal("plains grass gives corn seed"));
			helper.succeed();
		});
	}

	/** Dry farmland under a Farmings crop stays farmland, as under wheat (the maintains_farmland tag). */
	static void cropKeepsDryFarmland(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos soil = helper.absolutePos(new BlockPos(2, 1, 2));
		for (int x = 0; x <= 4; x++) {
			for (int z = 0; z <= 4; z++) {
				helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE);
				helper.setBlock(new BlockPos(x, 2, z), net.minecraft.world.level.block.Blocks.AIR);
			}
		}
		level.setBlockAndUpdate(soil, net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState());
		level.setBlockAndUpdate(soil.above(), FieldCrops.block(FieldCrop.OATS).defaultBlockState());
		for (int i = 0; i < 200; i++) {
			level.getBlockState(soil).randomTick(level, soil, level.getRandom());
		}
		helper.assertTrue(level.getBlockState(soil).is(net.minecraft.world.level.block.Blocks.FARMLAND), Component.literal("dry farmland under oats stays farmland: " + level.getBlockState(soil)));
		helper.assertTrue(level.getBlockState(soil.above()).getBlock() == FieldCrops.block(FieldCrop.OATS), Component.literal("and the oats stay"));
		helper.succeed();
	}
}
