package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsItems;
import dev.farmings.compost.CompostBlocks;
import dev.farmings.digester.DigesterBlocks;
import dev.farmings.digester.DigesterEntity;
import dev.farmings.digester.FeedPitEntity;
import dev.farmings.digester.GasHolderEntity;
import dev.farmings.digester.PressEntity;
import dev.wildcatter.block.FluidNode;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** The digester and separator (docs/multiblocks/digester.md). */
final class DigesterGameTests {
	private DigesterGameTests() {
	}

	/** Tank at (3,1,3), gas holder on it, feed pit west, digestate tank east, screw press beyond it, a chest beyond that. */
	static DigesterEntity build(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		helper.setBlock(new BlockPos(3, 1, 3), DigesterBlocks.DIGESTER_TANK);
		helper.setBlock(new BlockPos(3, 2, 3), DigesterBlocks.GAS_HOLDER);
		helper.setBlock(new BlockPos(2, 1, 3), DigesterBlocks.FEED_PIT);
		helper.setBlock(new BlockPos(4, 1, 3), DigesterBlocks.DIGESTATE_TANK);
		helper.setBlock(new BlockPos(5, 1, 3), DigesterBlocks.SCREW_PRESS);
		helper.setBlock(new BlockPos(6, 1, 3), Blocks.CHEST);
		DigesterEntity d = (DigesterEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 3)));
		d.forceRunning(true);
		return d;
	}

	/** 20 manure items (1 t) give 23 m3 of biogas over 20 days, half to the holder; the holder offers it as Wildcatter biogas. */
	static void digesterFillsHolder(GameTestHelper helper) {
		DigesterEntity d = build(helper);
		var level = helper.getLevel();
		FeedPitEntity pit = (FeedPitEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 3)));
		helper.assertTrue(pit.accept(new ItemStack(FarmingsItems.MANURE, 20)).isEmpty(), Component.literal("the pit takes manure"));
		d.step(20);
		helper.assertTrue(Math.abs(d.digester().gasM3() - 11.5) < 1e-6, Component.literal("11.5 m3 in the holder: " + d.readout()));
		GasHolderEntity holder = (GasHolderEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 2, 3)));
		BlockPos cell = helper.absolutePos(new BlockPos(3, 2, 3));
		helper.assertTrue(holder.offers(cell, Direction.UP) == Product.BIOGAS, Component.literal("it offers biogas on its pipe faces"));
		double got = holder.take(cell, Direction.UP, Product.BIOGAS, 5, FluidNode.Pull.MACHINE);
		helper.assertTrue(Math.abs(got - 5) < 1e-9 && Math.abs(holder.m3() - 6.5) < 1e-6, Component.literal("a gas set draws 5 m3: " + holder.m3()));
		helper.assertTrue(Math.abs(d.digester().digestateKg() - 1000 * (1 - 0.023 * 1.2)) < 1e-6, Component.literal("digestate: " + d.readout()));
		d.forceRunning(false);
		pit.accept(new ItemStack(FarmingsItems.MANURE, 4));
		d.step(20);
		helper.assertTrue(Math.abs(d.digester().inTankKg() - 200) < 1e-6, Component.literal("no power, no digestion: " + d.readout()));
		helper.succeed();
	}

	/** The screw press splits digestate 10% solids (10 kg items) and 90% liquid fertilizer (100 L items). */
	static void separatorSplits(GameTestHelper helper) {
		DigesterEntity d = build(helper);
		var level = helper.getLevel();
		d.digester().feed(1000);
		d.step(20);
		PressEntity press = (PressEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(5, 1, 3)));
		press.forceRunning(true);
		TamingGameTests.waitFor(helper, 0, 400, () -> d.digester().digestateKg() < 1e-6, "the press empties the digestate: " + d.readout(), () -> {
			var chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(6, 1, 3)));
			int solids = chest.countItem(CompostBlocks.SEPARATED_SOLIDS);
			int liquid = chest.countItem(DigesterBlocks.LIQUID_DIGESTATE);
			helper.assertTrue(solids == 9 && liquid == 8, Component.literal("972 kg: 9 solids, 8 liquid: " + solids + ", " + liquid));
			helper.assertTrue(dev.farmings.gantry.Fertilizers.is(new ItemStack(DigesterBlocks.LIQUID_DIGESTATE)), Component.literal("liquid digestate fertilizes"));
			helper.succeed();
		});
	}

	/**
	 * A real Wildcatter gas set (0.6.0) on the digester's gas holder: steel gas line from the holder to the engine's north side, the set
	 * started by hook (as Wildcatter's own biogas scene). It runs on the farm's biogas and draws it from the holder.
	 */
	static void gensetBurnsFarmBiogas(GameTestHelper helper) {
		var level = helper.getLevel();
		for (int x = -1; x <= 10; x++) {
			for (int z = -4; z <= 2; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
				for (int y = 2; y <= 6; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		helper.setBlock(new BlockPos(3, 2, -3), DigesterBlocks.DIGESTER_TANK);
		helper.setBlock(new BlockPos(3, 3, -3), DigesterBlocks.GAS_HOLDER);
		helper.setBlock(new BlockPos(4, 2, -3), DigesterBlocks.DIGESTATE_TANK);
		DigesterEntity d = (DigesterEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 2, -3)));
		d.forceRunning(true);
		d.digester().feed(20_000);     // 400 manure items: 230 m3 of biogas over 20 days, the holder's 200 m3 fills
		d.step(20);
		double full = d.digester().gasM3();
		var gasLine = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id("gas_line"));
		helper.setBlock(new BlockPos(3, 3, -2), gasLine);
		helper.setBlock(new BlockPos(3, 2, -2), gasLine);
		helper.setBlock(new BlockPos(3, 2, -1), gasLine);           // to the engine's north side
		dev.wildcatter.block.GensetSkid.build(level, helper.absolutePos(new BlockPos(8, 2, 0)), Direction.NORTH, true);
		var panel = (dev.wildcatter.block.GensetControlPanelBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(8, 2, 0)));
		helper.runAfterDelay(30, () -> panel.setWantRun(true));
		helper.runAfterDelay(400, () -> {
			String set = panel.inspect().getString();
			helper.assertTrue(panel.biogasBurned() > 0 && d.digester().gasM3() < full,
					Component.literal("the gas set burns the farm's biogas: burned " + panel.biogasBurned() + " m3, holder " + d.digester().gasM3() + " of " + full + "; " + set));
			helper.succeed();
		});
	}
}
