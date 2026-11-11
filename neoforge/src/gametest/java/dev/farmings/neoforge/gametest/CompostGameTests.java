package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsItems;
import dev.farmings.compost.CompostBlocks;
import dev.farmings.compost.CompostControllerEntity;
import dev.farmings.compost.TurnerEntity;
import dev.farmings.crops.FieldCrops;
import dev.farmings.farm.compost.Windrow;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** The compost windrow (docs/multiblocks/compost.md). */
final class CompostGameTests {
	private CompostGameTests() {
	}

	/** The controller at (1,1,3), pad x 2-5, the turner at (6,1,3), a chest at (1,1,4). */
	static CompostControllerEntity build(GameTestHelper helper, boolean power) {
		FarmingsGameTests.floor(helper);
		helper.setBlock(new BlockPos(1, 1, 3), CompostBlocks.COMPOST_CONTROLLER);
		for (int x = 2; x <= 5; x++) {
			helper.setBlock(new BlockPos(x, 1, 3), CompostBlocks.COMPOST_PAD);
		}
		helper.setBlock(new BlockPos(6, 1, 3), CompostBlocks.WINDROW_TURNER);
		helper.setBlock(new BlockPos(1, 1, 4), Blocks.CHEST);
		((TurnerEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(6, 1, 3)))).forceRunning(power);
		return (CompostControllerEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 3)));
	}

	static void load(CompostControllerEntity c) {
		c.accept(new ItemStack(FarmingsItems.MANURE, 10));
		c.accept(new ItemStack(FieldCrops.STRAW, 20));
		c.accept(new ItemStack(FarmingsItems.POULTRY_LITTER, 10));
	}

	/** Manure, straw and litter in range compost in 8 weeks with 5 turns into 34 items of 10 kg (42.5% of 800 kg). */
	static void compostMatures(GameTestHelper helper) {
		CompostControllerEntity c = build(helper, true);
		c.accept(new ItemStack(FarmingsItems.MANURE, 10));
		helper.assertTrue(c.pile().problem().equals("TOO WET") && !c.start(), Component.literal("manure alone is too wet: " + c.readout()));
		c.accept(new ItemStack(FieldCrops.STRAW, 20));
		c.accept(new ItemStack(FarmingsItems.POULTRY_LITTER, 10));
		helper.assertTrue(c.start(), Component.literal("in range, it starts: " + c.readout()));
		for (int i = 0; i < 6; i++) {
			c.step(helper.getLevel(), Windrow.TURN_EVERY_DAYS);
		}
		var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 4)));
		helper.assertTrue(chest.countItem(CompostBlocks.COMPOST) == 34, Component.literal("34 compost items: " + chest.countItem(CompostBlocks.COMPOST) + " " + c.readout()));
		helper.assertTrue(dev.farmings.gantry.Fertilizers.is(new ItemStack(CompostBlocks.COMPOST)), Component.literal("compost is a fertilizer use"));
		helper.succeed();
	}

	/** A pile whose turner has no power waits at its first turn. */
	static void compostWaitsForTurns(GameTestHelper helper) {
		CompostControllerEntity c = build(helper, false);
		load(c);
		c.start();
		for (int i = 0; i < 6; i++) {
			c.step(helper.getLevel(), Windrow.TURN_EVERY_DAYS);
		}
		helper.assertTrue(c.pile().turns() == 0 && Math.abs(c.pile().ageDays() - Windrow.TURN_EVERY_DAYS) < 1e-9 && c.readout().contains("NO POWER"),
				Component.literal(c.readout()));
		helper.succeed();
	}
}
