package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.Flock;
import dev.farmings.husbandry.SheepBreed;
import dev.farmings.husbandry.SheepStage;
import dev.farmings.item.FleeceItem;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/** Headless scenes for sheep (docs/animals/sheep.md). Registered from {@link FarmingsGameTests}. */
final class SheepGameTests {
	private SheepGameTests() {
	}

	static SheepEntity sheep(GameTestHelper helper, SheepBreed breed, Sex sex, long seed, double ageDays, BlockPos at) {
		SheepEntity s = helper.spawn(FarmingsEntities.SHEEP, at);
		Map<String, Integer> c = Map.of(breed.code, 10_000);
		s.initFounder(breed, sex, seed, ageDays, Flock.targetKg(c, ageDays), helper.getLevel().getGameTime());
		return s;
	}

	static SheepEntity adult(GameTestHelper helper, SheepBreed breed, Sex sex, long seed, BlockPos at) {
		return sheep(helper, breed, sex, seed, 800, at);
	}

	private static <T extends net.minecraft.world.entity.Entity> List<T> around(GameTestHelper helper, Class<T> type) {
		return helper.getLevel().getEntitiesOfClass(type, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12));
	}

	private static List<ItemStack> drops(GameTestHelper helper, net.minecraft.world.item.Item item) {
		return around(helper, ItemEntity.class).stream().map(ItemEntity::getItem).filter(s -> s.is(item)).toList();
	}

	/** A spawned sheep is a founder adult with a sheep genome, and everything about it comes back from a save. */
	static void sheepSpawnsAndSurvivesSave(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		SheepEntity plain = helper.spawn(FarmingsEntities.SHEEP, new BlockPos(1, 1, 1), EntitySpawnReason.COMMAND);
		helper.assertTrue(plain.genome() != null && plain.genome().species() == Species.SHEEP, Component.literal("a spawned sheep has a sheep genome"));
		helper.assertTrue(!plain.isBaby(), Component.literal("a spawned sheep is an adult"));

		SheepEntity a = adult(helper, SheepBreed.EAST_FRIESIAN, Sex.FEMALE, 4242L, new BlockPos(4, 1, 4));
		a.setFleeceDaysForTest(200);
		a.startHeatForDisplay();
		SheepEntity ram = adult(helper, SheepBreed.SUFFOLK, Sex.MALE, 77L, new BlockPos(5, 1, 4));
		a.conceive(ram.genome(), ram.genes(), level.getGameTime());
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		a.saveWithoutId(out);
		CompoundTag tag = out.buildResult();
		SheepEntity b = FarmingsEntities.SHEEP.create(level, EntitySpawnReason.LOAD);
		b.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		helper.assertTrue(b.breed() == SheepBreed.EAST_FRIESIAN && b.sex() == Sex.FEMALE, Component.literal("breed and sex survive"));
		helper.assertTrue(b.genome().equals(a.genome()), Component.literal("genome survives"));
		helper.assertTrue(b.genes().equals(a.genes()), Component.literal("coat genes survive"));
		for (Trait t : Species.SHEEP.traits()) {
			helper.assertTrue(b.score(t) == a.score(t), Component.literal("score " + t.id + " survives"));
		}
		helper.assertTrue(Math.abs(b.fleeceDays() - a.fleeceDays()) < 0.01, Component.literal("fleece growth survives"));
		helper.assertTrue(b.pregnant(), Component.literal("pregnancy survives"));
		helper.assertTrue(Math.abs(b.weightKg() - a.weightKg()) < 1e-9, Component.literal("weight survives"));
		helper.succeed();
	}

	/** Lamb, then ewe lamb at weaning, a ram lamb castrated with shears becomes a wether, a grown ram is a ram. */
	static void sheepLifecycle(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		SheepEntity lamb = sheep(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 1L, 30, new BlockPos(2, 1, 2));
		helper.assertTrue(lamb.stage() == SheepStage.LAMB && lamb.isBaby(), Component.literal("a 30-day lamb is a lamb, got " + lamb.stage()));
		lamb.advanceForTest(150);
		lamb.settle(helper.getLevel());
		helper.assertTrue(lamb.stage() == SheepStage.EWE_LAMB && !lamb.isBaby(), Component.literal("weaned she is a ewe lamb, got " + lamb.stage()));
		SheepEntity ramLamb = sheep(helper, SheepBreed.SUFFOLK, Sex.MALE, 2L, 30, new BlockPos(5, 1, 2));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		ramLamb.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(ramLamb.castrated(), Component.literal("shears castrate a ram lamb"));
		ramLamb.advanceForTest(200);
		ramLamb.settle(helper.getLevel());
		helper.assertTrue(ramLamb.stage() == SheepStage.WETHER, Component.literal("he grows as a wether, got " + ramLamb.stage()));
		SheepEntity ram = adult(helper, SheepBreed.MERINO, Sex.MALE, 3L, new BlockPos(5, 1, 5));
		helper.assertTrue(ram.stage() == SheepStage.RAM, Component.literal("a grown male is a ram"));
		helper.assertTrue(ram.look().horns() == 2, Component.literal("a Merino ram is horned"));
		helper.succeed();
	}

	/** A ram serves a ewe in heat; she lambs the litter rolled (forced to twins here), beside her, with coat genes from both parents. */
	static void eweLambsTwins(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		SheepEntity ewe = adult(helper, SheepBreed.EAST_FRIESIAN, Sex.FEMALE, 10L, new BlockPos(3, 1, 3));
		SheepEntity ram = adult(helper, SheepBreed.SUFFOLK, Sex.MALE, 11L, new BlockPos(5, 1, 3));
		ewe.settle(level);
		ewe.startHeatForDisplay();
		ewe.settle(level);
		helper.assertTrue(ewe.pregnant(), Component.literal("the ram should serve her in heat"));
		ewe.forceLambsForTest(2);
		ewe.makeDueNow();
		ewe.settle(level);
		List<SheepEntity> lambs = around(helper, SheepEntity.class).stream().filter(s -> s != ewe && s != ram).toList();
		helper.assertTrue(lambs.size() == 2, Component.literal("twins expected, got " + lambs.size()));
		for (SheepEntity l : lambs) {
			helper.assertTrue(l.stage() == SheepStage.LAMB && l.isBaby(), Component.literal("a newborn is a lamb"));
			helper.assertTrue(l.composition().equals(Map.of("EFR", 5000, "SUF", 5000)), Component.literal("an F1 cross, got " + l.composition()));
			helper.assertTrue(l.look().head() == 1, Component.literal("a white-faced x black-headed lamb has a mottled head"));
			helper.assertTrue(l.damId().equals(ewe.herdId()) && l.sireId().equals(ram.herdId()), Component.literal("parents recorded"));
			helper.assertTrue(l.distanceTo(ewe) < 3, Component.literal("born beside the ewe"));
		}
		helper.assertTrue(!ewe.pregnant() && ewe.lambings() == 1 && ewe.lambsNursing() == 2, Component.literal("she has lambed and nurses two"));
		helper.succeed();
	}

	/** A year's wool is the breed fleece (by wool_yield); hand shears take it as a fleece item a grade lower; then it starts again. */
	static void fleeceGrowsAndShears(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		SheepEntity m = adult(helper, SheepBreed.MERINO, Sex.FEMALE, 21L, new BlockPos(3, 1, 3));
		m.setFleeceDaysForTest(365);
		double expected = SheepBreed.MERINO.fleeceKg * Flock.factor(m.score(Trait.WOOL_YIELD));
		helper.assertTrue(Math.abs(m.fleeceKg() - expected) < 0.01, Component.literal("fleece " + m.fleeceKg() + " expected " + expected));
		helper.assertTrue(m.fleeceReady(), Component.literal("a year's fleece is ready"));
		Flock.Grade standGrade = Flock.grade(m.micron());
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		m.mobInteract(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(m.fleeceKg() < 0.01, Component.literal("she is shorn"));
		helper.runAfterDelay(5, () -> {
			List<ItemStack> fleeces = drops(helper, FarmingsItems.FLEECE);
			helper.assertTrue(fleeces.size() == 1, Component.literal("one fleece dropped, got " + fleeces.size()));
			ItemStack f = fleeces.get(0);
			helper.assertTrue(Math.abs(FleeceItem.greasyKg(f) - expected) < 0.01, Component.literal("the fleece weighs what grew"));
			helper.assertTrue(FleeceItem.grade(f) == standGrade.lower(), Component.literal("hand shearing is a grade lower: " + FleeceItem.grade(f)));
			helper.succeed();
		});
	}

	/** A Dorper (hair sheep) has no fleece to shear and drops a little loose wool once a year. */
	static void dorperSheds(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		SheepEntity d = adult(helper, SheepBreed.DORPER, Sex.FEMALE, 31L, new BlockPos(3, 1, 3));
		d.setFleeceDaysForTest(365);
		helper.assertTrue(d.look().sheds() && d.fleeceKg() == 0, Component.literal("a Dorper has no fleece"));
		d.advanceForTest(366);
		d.settle(helper.getLevel());
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(drops(helper, FarmingsItems.LOOSE_WOOL).size() == 1, Component.literal("she sheds a little loose wool"));
			helper.succeed();
		});
	}

	/** Only East Friesian blood gives parlor milk: a lambed East Friesian has milk, a lambed Suffolk none. */
	static void eweMilkByBreed(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		SheepEntity efr = adult(helper, SheepBreed.EAST_FRIESIAN, Sex.FEMALE, 41L, new BlockPos(2, 1, 3));
		SheepEntity suf = adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 42L, new BlockPos(5, 1, 3));
		efr.setLambedForTest(30, 2);
		suf.setLambedForTest(30, 2);
		helper.assertTrue(efr.milkKg() > 0.5 && efr.dairyEwe(), Component.literal("an East Friesian gives milk, got " + efr.milkKg()));
		helper.assertTrue(suf.milkKg() == 0 && !suf.dairyEwe(), Component.literal("a Suffolk gives no parlor milk"));
		helper.succeed();
	}

	/** Sheep drop lamb chops under a year and mutton chops after, by weight. */
	static void sheepMeatByAge(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		SheepEntity young = sheep(helper, SheepBreed.SUFFOLK, Sex.MALE, 51L, 200, new BlockPos(2, 1, 3));
		SheepEntity old = adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 52L, new BlockPos(5, 1, 3));
		int youngItems = Flock.meatItems(young.weightKg());
		int oldItems = Flock.meatItems(old.weightKg());
		young.kill(helper.getLevel());
		old.kill(helper.getLevel());
		helper.runAfterDelay(5, () -> {
			int lamb = drops(helper, FarmingsItems.LAMB_CHOP).stream().mapToInt(ItemStack::getCount).sum();
			int mutton = drops(helper, FarmingsItems.MUTTON_CHOP).stream().mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(lamb == youngItems, Component.literal("lamb chops " + lamb + " expected " + youngItems));
			helper.assertTrue(mutton == oldItems, Component.literal("mutton chops " + mutton + " expected " + oldItems));
			helper.succeed();
		});
	}

	// ---- the sheep shed (docs/multiblocks/sheep-shed.md) ----

	/**
	 * A shed and pasture, relative to the scene: stone under the shed, grass under the pasture. Controller at (1,1,2); pen cells x 2-4,
	 * z 1-3; hay rack north of the pen at (2,1,0); trough south at (3,1,4)-(4,1,4); a lambing pen at (5,1,1) and the pasture gate at
	 * (5,1,3), both in the pasture's west fence; the pasture is x 6-13, z 0-5 inside an oak fence.
	 */
	static BlockPos buildShed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = -1; x <= 15; x++) {
			for (int z = -2; z <= 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), x >= 6 && x <= 13 && z >= 0 && z <= 5 ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK
						: net.minecraft.world.level.block.Blocks.STONE);
				for (int y = 1; y <= 3; y++) {
					helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
				}
			}
		}
		for (int x = 5; x <= 14; x++) {
			for (int z = -1; z <= 6; z++) {
				if (x == 5 || x == 14 || z == -1 || z == 6) {
					helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.OAK_FENCE);
				}
			}
		}
		helper.setBlock(new BlockPos(1, 1, 2), dev.farmings.shed.ShedBlocks.SHED_CONTROLLER);
		for (int x = 2; x <= 4; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), dev.farmings.shed.ShedBlocks.SHEEP_PEN);
			}
		}
		helper.setBlock(new BlockPos(5, 1, 1), dev.farmings.shed.ShedBlocks.LAMBING_PEN);
		helper.setBlock(new BlockPos(5, 1, 3), dev.farmings.shed.ShedBlocks.PASTURE_GATE);
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.HAY_FEEDER, helper.absolutePos(new BlockPos(2, 1, 0)), net.minecraft.core.Direction.NORTH);
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.WATER_TROUGH, helper.absolutePos(new BlockPos(3, 1, 4)), net.minecraft.core.Direction.NORTH);
		return helper.absolutePos(new BlockPos(1, 1, 2));
	}

	static dev.farmings.shed.ShedControllerEntity shed(GameTestHelper helper, BlockPos c) {
		return (dev.farmings.shed.ShedControllerEntity) helper.getLevel().getBlockEntity(c);
	}

	static void fill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		if (level.getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 0))) instanceof dev.farmings.barn.FeederEntity f) {
			f.addBlock();
			f.addBlock();
		}
		if (level.getBlockEntity(dev.wildcatter.multiblock.BigParts.owner(level, helper.absolutePos(new BlockPos(3, 1, 4)))) instanceof dev.farmings.barn.TroughEntity t) {
			t.pourBucket();
		}
	}

	/** The shed forms with its pen, rack, trough, lambing pen and a fenced pasture behind the gate. */
	static void shedForms(GameTestHelper helper) {
		BlockPos c = buildShed(helper);
		dev.farmings.shed.ShedControllerEntity shed = shed(helper, c);
		shed.refresh(helper.getLevel());
		helper.assertTrue(shed.check().formed(), Component.literal("the shed should form: " + shed.check().status()));
		helper.assertTrue(shed.check().pen().size() == 9 && shed.check().capacity() == 6, Component.literal("9 pen cells hold 6 sheep"));
		helper.assertTrue(shed.check().lambing().size() == 1 && shed.check().gates().size() == 1, Component.literal("a lambing pen and a gate"));
		helper.assertTrue(shed.pasture() != null && shed.pasture().found(), Component.literal("pasture: " + (shed.pasture() == null ? "none" : shed.pasture().problem())));
		helper.assertTrue(shed.pasture().spots().size() == 48 && shed.pasture().grass() == 48, Component.literal("an 8 x 6 grass pasture, got " + shed.pasture().spots().size()));
		helper.assertTrue(shed.readout().contains("FORMED"), Component.literal(shed.readout()));
		helper.succeed();
	}

	/** Without a trough the shed names it; a gap in the pasture fence is named too. */
	static void shedGapNamed(GameTestHelper helper) {
		BlockPos c = buildShed(helper);
		ServerLevel level = helper.getLevel();
		dev.farmings.shed.ShedControllerEntity shed = shed(helper, c);
		level.destroyBlock(helper.absolutePos(new BlockPos(3, 1, 4)), false);
		shed.refresh(level);
		helper.assertTrue(shed.check().status().equals("No water trough beside the pen"), Component.literal(shed.check().status()));
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.WATER_TROUGH, helper.absolutePos(new BlockPos(3, 1, 4)), net.minecraft.core.Direction.NORTH);
		helper.setBlock(new BlockPos(14, 1, 2), net.minecraft.world.level.block.Blocks.AIR);
		for (int x = 15; x <= 40; x++) {
			for (int z = -10; z <= 16; z++) {
				helper.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
				helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.AIR);
				helper.setBlock(new BlockPos(x, 2, z), net.minecraft.world.level.block.Blocks.AIR);
			}
		}
		shed.refresh(level);
		helper.assertTrue(shed.check().formed(), Component.literal("the shed forms again: " + shed.check().status()));
		// the gap lets the pasture run out into the open field (the not-fenced-in limit itself is in ShedLayoutTest)
		helper.assertTrue(shed.pasture().spots().size() > 48, Component.literal("the pasture should spill through the gap: " + shed.readout()));
		helper.succeed();
	}

	/** By day the flock goes out through the gate onto the pasture; at night it comes back to the pen. */
	static void flockGoesToPastureAndBack(GameTestHelper helper) {
		BlockPos c = buildShed(helper);
		ServerLevel level = helper.getLevel();
		dev.farmings.shed.ShedControllerEntity shed = shed(helper, c);
		fill(helper);
		shed.forceDayForTest(true);
		SheepEntity a = adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 61L, new BlockPos(3, 1, 2));
		SheepEntity b = adult(helper, SheepBreed.MERINO, Sex.FEMALE, 62L, new BlockPos(2, 1, 1));
		a.assignShed(c, null);       // scenes run side by side: tie these sheep to this scene's shed
		b.assignShed(c, null);
		helper.runAfterDelay(500, () -> {
			for (SheepEntity s : List.of(a, b)) {
				double x = s.getX() - helper.absolutePos(BlockPos.ZERO).getX();
				helper.assertTrue(x > 5.5, Component.literal("by day she should be out on the pasture, at x " + x + "; " + shed.readout()));
			}
			shed.forceDayForTest(false);
			helper.runAfterDelay(500, () -> {
				for (SheepEntity s : List.of(a, b)) {
					double x = s.getX() - helper.absolutePos(BlockPos.ZERO).getX();
					helper.assertTrue(x < 5.5, Component.literal("at night she should be back in the pen, at x " + x));
				}
				helper.succeed();
			});
		});
	}

	/** A ewe due within three days goes to the lambing pen. */
	static void eweMovesToLambingPen(GameTestHelper helper) {
		BlockPos c = buildShed(helper);
		ServerLevel level = helper.getLevel();
		dev.farmings.shed.ShedControllerEntity shed = shed(helper, c);
		fill(helper);
		shed.forceDayForTest(false);
		SheepEntity ewe = adult(helper, SheepBreed.EAST_FRIESIAN, Sex.FEMALE, 71L, new BlockPos(3, 1, 2));
		SheepEntity ram = adult(helper, SheepBreed.SUFFOLK, Sex.MALE, 72L, new BlockPos(3, 1, 3));
		ewe.assignShed(c, null);
		ram.assignShed(c, null);
		ewe.startHeatForDisplay();
		ewe.conceive(ram.genome(), ram.genes(), level.getGameTime());
		ewe.setDueInForTest(2);
		helper.runAfterDelay(200, () -> {
			BlockPos jug = helper.absolutePos(new BlockPos(5, 1, 1));
			helper.assertTrue(jug.equals(ewe.home()), Component.literal("she should be given the lambing pen " + jug + ", home " + ewe.home() + ", shed " + ewe.shedController() + " (mine " + c + "), due in "
					+ ewe.daysToLambing() + " days; " + shed.readout()));
			helper.assertTrue(ewe.blockPosition().distManhattan(jug) <= 2, Component.literal("and walk to it, at " + ewe.blockPosition()));
			helper.succeed();
		});
	}

	/** On pasture a sheep grazes: grass turns to dirt and she is fed with no hay in the racks. */
	static void grazingFeeds(GameTestHelper helper) {
		BlockPos c = buildShed(helper);
		ServerLevel level = helper.getLevel();
		dev.farmings.shed.ShedControllerEntity shed = shed(helper, c);
		shed.forceDayForTest(true);
		shed.refresh(level);
		SheepEntity s = adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 81L, new BlockPos(9, 1, 3));
		s.assignShed(c, helper.absolutePos(new BlockPos(3, 1, 2)));
		s.recordTending(0, 1);
		for (int i = 0; i < 100; i++) {
			shed.tend(level, List.of(s), 0.01);
		}
		helper.assertTrue(shed.grazedBlocks() >= 1, Component.literal("she should have grazed a grass block, grazed " + shed.grazedBlocks()));
		helper.assertTrue(s.lastFed() > 0.5, Component.literal("and be fed from it, fed " + s.lastFed()));
		helper.succeed();
	}

}
