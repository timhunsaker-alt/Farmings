package dev.farmings.neoforge.gametest;

import dev.farmings.crops.FieldCrops;
import dev.farmings.farm.silo.Silage;
import dev.farmings.silo.BinRoofEntity;
import dev.farmings.silo.BlowerEntity;
import dev.farmings.silo.GrainBinEntity;
import dev.farmings.silo.SiloBlocks;
import dev.farmings.silo.SiloEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** The tower silo and the grain bin (docs/multiblocks/silo.md). */
final class SiloGameTests {
	private SiloGameTests() {
	}

	/** A 3 x 3 ring around (3, 3), {@code h} high from y 1, with a roof at its top over the middle. */
	static void ring(GameTestHelper helper, Block wall, Block roof, int h) {
		FarmingsGameTests.floor(helper);
		for (int y = 1; y <= h; y++) {
			for (int x = 2; x <= 4; x++) {
				for (int z = 2; z <= 4; z++) {
					helper.setBlock(new BlockPos(x, y, z), x == 3 && z == 3 ? Blocks.AIR : wall);
				}
			}
		}
		helper.setBlock(new BlockPos(3, h + 1, 3), roof);
	}

	/** Chopped corn blown in loses 2.5% of its dry matter at filling, ferments 21 days, then comes out as 25 kg corn silage items. */
	static void siloFermentsAndUnloads(GameTestHelper helper) {
		ring(helper, SiloBlocks.SILO_STAVE, SiloBlocks.SILO_ROOF, 6);
		helper.setBlock(new BlockPos(1, 1, 3), SiloBlocks.FORAGE_BLOWER);
		helper.setBlock(new BlockPos(5, 1, 3), SiloBlocks.SILO_UNLOADER);
		helper.setBlock(new BlockPos(6, 1, 3), Blocks.CHEST);
		var level = helper.getLevel();
		BlowerEntity blower = (BlowerEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 3)));
		SiloEntity silo = (SiloEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(5, 1, 3)));
		blower.forceRunning(true);
		silo.forceRunning(true);
		helper.assertTrue(silo.tower() != null && silo.tower().formed() && silo.tower().volume() == 6, Component.literal(silo.readout()));
		ItemStack rest = blower.accept(new ItemStack(FieldCrops.CHOPPED_CORN, 20));
		double want = Silage.kept(20 * 25 * 0.35);
		helper.assertTrue(rest.isEmpty() && Math.abs(silo.dmKg() - want) < 1e-6, Component.literal("156.6 kg DM kept: " + silo.readout()));
		helper.assertTrue(!blower.accept(new ItemStack(FieldCrops.CHOPPED_ALFALFA)).isEmpty(), Component.literal("one kind at a time"));
		helper.assertTrue(silo.fermenting(), Component.literal("fermenting after the fill"));
		helper.runAfterDelay(200, () -> {
			helper.assertTrue(silo.unloaded() == 0, Component.literal("nothing comes out while it ferments"));
			silo.ageForTest(21);
			TamingGameTests.waitFor(helper, 0, 400, () -> silo.unloaded() >= 1, "corn silage comes out after 21 days: " + silo.readout(), () -> {
				var chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(6, 1, 3)));
				helper.assertTrue(chest.countItem(FieldCrops.CORN_SILAGE) >= 1, Component.literal("silage in the chest"));
				helper.succeed();
			});
		});
	}

	/** Grain fills a bin through its roof, up to its volume at the grain's density, one grain at a time, and runs out of its hopper. */
	static void grainBinFillsAndEmpties(GameTestHelper helper) {
		ring(helper, SiloBlocks.BIN_WALL, SiloBlocks.BIN_ROOF, 2);
		helper.setBlock(new BlockPos(1, 1, 3), SiloBlocks.GRAIN_BIN_HOPPER);
		var level = helper.getLevel();
		BinRoofEntity roof = (BinRoofEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 3, 3)));
		GrainBinEntity bin = (GrainBinEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 3)));
		ItemStack rest = roof.accept(new ItemStack(FieldCrops.OAT_GRAIN, 64));
		helper.assertTrue(rest.isEmpty() && bin.items() == 64, Component.literal(bin.readout()));
		helper.assertTrue(!roof.accept(new ItemStack(FieldCrops.CORN_GRAIN)).isEmpty(), Component.literal("one grain at a time"));
		for (int i = 0; i < 20; i++) {
			roof.accept(new ItemStack(FieldCrops.OAT_GRAIN, 64));
		}
		helper.assertTrue(bin.items() == (int) Math.floor(2 * Silage.OAT_DENSITY), Component.literal("full at 2 m3 x 412 kg: " + bin.readout()));
		helper.setBlock(new BlockPos(0, 1, 3), Blocks.CHEST);
		TamingGameTests.waitFor(helper, 0, 200, () -> ((ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(0, 1, 3)))).countItem(FieldCrops.OAT_GRAIN) > 0,
				"the hopper lets grain out: " + bin.readout(), helper::succeed);
	}

	/** Losses over time (silo.md, "losses over time"): 2.5% at fill, 2.5% over fermenting, then about 4.3% over two years in storage. */
	static void siloLossOverStorage(GameTestHelper helper) {
		ring(helper, SiloBlocks.SILO_STAVE, SiloBlocks.SILO_ROOF, 6);
		helper.setBlock(new BlockPos(1, 1, 3), SiloBlocks.FORAGE_BLOWER);
		helper.setBlock(new BlockPos(5, 1, 3), SiloBlocks.SILO_UNLOADER);
		var level = helper.getLevel();
		BlowerEntity blower = (BlowerEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 3)));
		SiloEntity silo = (SiloEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(5, 1, 3)));
		blower.forceRunning(true);
		silo.forceRunning(false);
		blower.accept(new ItemStack(FieldCrops.CHOPPED_CORN, 20));
		double in = 20 * 25 * 0.35;
		helper.assertTrue(Math.abs(silo.dmKg() - in * 0.975) < 1e-6, Component.literal("2.5% at filling: " + silo.readout()));
		silo.ageForTest(21);
		helper.assertTrue(Math.abs(silo.dmKg() - in * 0.975 * 0.975) < 0.05, Component.literal("2.5% more over fermenting: " + silo.readout()));
		double fermented = silo.dmKg();
		silo.ageForTest(730);
		helper.assertTrue(Math.abs(silo.dmKg() / fermented - (1 - 0.043)) < 0.002, Component.literal("about 4.3% over two years stored: " + silo.readout()));
		helper.assertTrue(Math.abs(silo.lostKg() + silo.dmKg() - in) < 1e-6 && silo.readout().contains("lost so far"), Component.literal("what's lost is counted: " + silo.readout()));
		helper.succeed();
	}
}
