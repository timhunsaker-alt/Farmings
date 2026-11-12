package dev.farmings.neoforge.gametest;

import dev.farmings.crops.FieldCrops;
import dev.farmings.feed.Feedstuff;
import dev.farmings.feed.Ration;
import dev.farmings.feedmill.FeedItems;
import dev.farmings.feedmill.FeedMachineEntity;
import dev.farmings.feedmill.FeedMillBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** The feed mill and rations (docs/multiblocks/feed-mill.md). */
final class FeedMillGameTests {
	private FeedMillGameTests() {
	}

	/** The machine with its main cell at (3,1,3) and a chest beside it: at (4,1,3) for the mill, (5,1,3) for the 2 x 2 x 2 mixer (x 3-4, z 3-4). */
	static FeedMachineEntity machine(GameTestHelper helper, net.minecraft.world.level.block.Block b) {
		FarmingsGameTests.floor(helper);
		if (b == FeedMillBlocks.TMR_MIXER) {
			FarmingsGameTests.bigPart(helper.getLevel(), b, helper.absolutePos(new BlockPos(3, 1, 3)));
			helper.setBlock(new BlockPos(5, 1, 3), Blocks.CHEST);
		} else {
			helper.setBlock(new BlockPos(3, 1, 3), b);
			helper.setBlock(new BlockPos(4, 1, 3), Blocks.CHEST);
		}
		FeedMachineEntity m = (FeedMachineEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 3)));
		m.forceRunning(true);
		return m;
	}

	static ChestBlockEntity chest(GameTestHelper helper) {
		var near = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(4, 1, 3)));
		return near instanceof ChestBlockEntity c ? c : (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(5, 1, 3)));
	}

	/** The hammer mill grinds 3 kg of corn into 3 kg of corn meal at 0.53 kg/s, and refuses what it cannot grind. */
	static void millGrinds(GameTestHelper helper) {
		FeedMachineEntity m = machine(helper, FeedMillBlocks.HAMMER_MILL);
		helper.assertTrue(m.accept(new ItemStack(FieldCrops.CORN_GRAIN, 3)).isEmpty(), Component.literal("it takes corn"));
		helper.assertTrue(!m.accept(new ItemStack(Blocks.STONE)).isEmpty(), Component.literal("it refuses stone"));
		TamingGameTests.waitFor(helper, 0, 300, () -> chest(helper).countItem(FeedItems.CORN_MEAL) == 3, "3 kg of meal: " + m.readout(), helper::succeed);
	}

	/** Silage, hay, meal and premix mixed into 25 kg rations that carry their analysis and score well for a cow in milk. */
	static void mixerMakesRation(GameTestHelper helper) {
		FeedMachineEntity m = machine(helper, FeedMillBlocks.TMR_MIXER);
		// corn silage and corn alone are short of protein (CP 10%); alfalfa and a little fish meal bring it to 15%
		m.accept(new ItemStack(FieldCrops.CORN_SILAGE, 6));       // 150 kg as fed, 51 kg DM
		m.accept(new ItemStack(FieldCrops.ALFALFA_HAY, 20));      // 200 kg, 164 kg DM
		m.accept(new ItemStack(FieldCrops.CORN_GRAIN, 64));       // 64 kg, 56 kg DM
		m.accept(new ItemStack(FeedItems.FISH_MEAL, 10));         // 10 kg
		m.accept(new ItemStack(FeedItems.MINERAL_PREMIX, 3));     // 3 kg
		helper.assertTrue(Math.abs(m.asFed() - 427) < 1e-6, Component.literal("427 kg in the batch: " + m.readout()));
		helper.assertTrue(m.batchAnalysis().cp() >= 15, Component.literal("CP 15% or more: " + m.batchAnalysis()));
		TamingGameTests.waitFor(helper, 0, 700, () -> chest(helper).countItem(FeedItems.RATION) == 16, "16 rations of 25 kg: " + m.readout(), () -> {
			ItemStack r = null;
			for (int i = 0; i < chest(helper).getContainerSize(); i++) {
				if (chest(helper).getItem(i).is(FeedItems.RATION)) {
					r = chest(helper).getItem(i);
				}
			}
			FeedItems.Feed f = FeedItems.feed(r);
			helper.assertTrue(f != null && Ration.milkFactor(f.analysis()) > Ration.milkFactor(Feedstuff.GRASS_HAY.analysis()) + 0.1,
					Component.literal("the TMR beats grass hay for milk: " + (f == null ? "no analysis" : f.analysis())));
			helper.succeed();
		});
	}

	/** A barn hay feeder takes a ration; a cow eating it moves her running ration toward it; a round bale goes into an empty feeder only. */
	static void feederRationFeedsCow(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		var level = helper.getLevel();
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.HAY_FEEDER, helper.absolutePos(new BlockPos(2, 1, 2)), net.minecraft.core.Direction.NORTH);
		var feeder = (dev.farmings.barn.FeederEntity) level.getBlockEntity(dev.wildcatter.multiblock.BigParts.owner(level, helper.absolutePos(new BlockPos(2, 1, 2))));
		helper.assertTrue(feeder.addFeed(new ItemStack(FieldCrops.GRASS_BALE)), Component.literal("a bale into an empty feeder"));
		helper.assertTrue(!feeder.addFeed(new ItemStack(FieldCrops.GRASS_BALE)), Component.literal("not a second bale"));
		helper.assertTrue(Math.abs(feeder.dryMatterKg() - 454 * 0.82) < 1e-6, Component.literal("372 kg DM: " + feeder.dryMatterKg()));
		var cow = helper.spawn(dev.farmings.FarmingsEntities.CATTLE, new BlockPos(4, 1, 4), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		double before = Ration.milkFactor(cow.ration());
		for (int i = 0; i < 200; i++) {
			cow.recordRation(feeder.analysis());
		}
		helper.assertTrue(before == 1.0 && Ration.milkFactor(cow.ration()) < 0.85, Component.literal("on grass hay alone her milk falls: " + Ration.milkFactor(cow.ration())));
		helper.succeed();
	}

	/** The mixer hands its rations onto a belt, and the belt delivers them into a barn hay feeder at its head. */
	static void beltFeedsBarnFeeder(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		var level = helper.getLevel();
		FarmingsGameTests.bigPart(level, FeedMillBlocks.TMR_MIXER, helper.absolutePos(new BlockPos(0, 1, 3)));     // cells x 0-1, z 3-4
		for (int x = 2; x <= 4; x++) {
			helper.setBlock(new BlockPos(x, 1, 3), dev.farmings.conveyor.ConveyorBlocks.FARM_BELT.defaultBlockState()
					.setValue(dev.farmings.conveyor.ConveyorPieceBlock.FACING, net.minecraft.core.Direction.EAST));
		}
		helper.setBlock(new BlockPos(5, 1, 3), dev.farmings.conveyor.ConveyorBlocks.FARM_BELT_DRIVE.defaultBlockState()
				.setValue(dev.farmings.conveyor.ConveyorDriveBlock.FACING, net.minecraft.core.Direction.EAST));
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.HAY_FEEDER, helper.absolutePos(new BlockPos(6, 1, 3)), net.minecraft.core.Direction.NORTH);
		var drive = (dev.farmings.conveyor.ConveyorDriveEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(5, 1, 3)));
		drive.refresh();
		drive.forceRunning(true);
		FeedMachineEntity m = (FeedMachineEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(0, 1, 3)));
		m.forceRunning(true);
		m.accept(new ItemStack(FieldCrops.CORN_SILAGE, 6));
		m.accept(new ItemStack(FieldCrops.ALFALFA_HAY, 20));
		m.accept(new ItemStack(FieldCrops.CORN_GRAIN, 64));
		m.accept(new ItemStack(FeedItems.FISH_MEAL, 10));
		m.accept(new ItemStack(FeedItems.MINERAL_PREMIX, 3));
		var feeder = (dev.farmings.barn.FeederEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(6, 1, 3)));
		TamingGameTests.waitFor(helper, 0, 400, () -> drive.delivered() >= 16, "16 or more rations ride the belt into the feeder", () -> {
			helper.assertTrue(feeder.dryMatterKg() > 200 && feeder.analysis().cp() >= 15,
					Component.literal("the feeder holds the TMR: " + feeder.dryMatterKg() + " kg DM, " + feeder.analysis()));
			helper.succeed();
		});
	}

	/** The mixer fills 2 x 2 x 2 from one item; a click on any cell loads the batch; it hands rations on from any face. */
	static void mixerMultiblockForms(GameTestHelper helper) {
		FeedMachineEntity m = machine(helper, FeedMillBlocks.TMR_MIXER);
		var level = helper.getLevel();
		int cells = 0;
		for (int x = 3; x <= 4; x++) {
			for (int y = 1; y <= 2; y++) {
				for (int z = 3; z <= 4; z++) {
					if (dev.wildcatter.multiblock.BigParts.entity(level, helper.absolutePos(new BlockPos(x, y, z))) == m) {
						cells++;
					}
				}
			}
		}
		helper.assertTrue(cells == 8, Component.literal("8 cells, all one mixer: " + cells));
		net.minecraft.world.entity.player.Player p = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack hay = new ItemStack(FieldCrops.ALFALFA_HAY, 5);
		p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, hay);
		BlockPos top = helper.absolutePos(new BlockPos(4, 2, 4));
		level.getBlockState(top).useItemOn(hay, level, p, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(top), net.minecraft.core.Direction.UP, top, false));
		helper.assertTrue(hay.isEmpty() && Math.abs(m.asFed() - 50) < 1e-6, Component.literal("loaded from the far top cell: " + m.readout()));
		helper.succeed();
	}
}
