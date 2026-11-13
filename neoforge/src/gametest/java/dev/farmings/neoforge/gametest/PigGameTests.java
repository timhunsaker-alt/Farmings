package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.PigEntity;
import dev.farmings.genetics.PigLook;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.PigBreed;
import dev.farmings.husbandry.PigStage;
import dev.farmings.husbandry.Swine;
import dev.farmings.item.StrawItem;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/** Headless scenes for pigs (docs/animals/pigs.md). Registered from {@link FarmingsGameTests}. */
final class PigGameTests {
	private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

	private PigGameTests() {
	}

	static PigEntity pig(GameTestHelper helper, PigBreed breed, Sex sex, long seed, double ageDays, BlockPos at) {
		PigEntity p = helper.spawn(FarmingsEntities.PIG, at);
		p.initFounder(breed, sex, seed, ageDays, Swine.targetKg(Map.of(breed.code, 10_000), sex == Sex.MALE, ageDays), helper.getLevel().getGameTime());
		return p;
	}

	static PigEntity adult(GameTestHelper helper, PigBreed breed, Sex sex, long seed, BlockPos at) {
		return pig(helper, breed, sex, seed, 400, at);
	}

	private static <T extends net.minecraft.world.entity.Entity> List<T> around(GameTestHelper helper, Class<T> type) {
		return helper.getLevel().getEntitiesOfClass(type, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12));
	}

	/** Founders look like their breed and carry a pig genome; everything survives a save. */
	static void pigSpawnsAndSurvivesSave(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		PigEntity plain = helper.spawn(FarmingsEntities.PIG, new BlockPos(1, 1, 1), EntitySpawnReason.COMMAND);
		helper.assertTrue(plain.genome() != null && plain.genome().species() == Species.PIG, Component.literal("a spawned pig has a pig genome"));
		PigEntity lw = adult(helper, PigBreed.LARGE_WHITE, Sex.FEMALE, 1L, new BlockPos(3, 1, 1));
		PigEntity dur = adult(helper, PigBreed.DUROC, Sex.MALE, 2L, new BlockPos(5, 1, 1));
		PigEntity brk = adult(helper, PigBreed.BERKSHIRE, Sex.FEMALE, 3L, new BlockPos(7, 1, 1));
		helper.assertTrue(lw.look().colour() == PigLook.Colour.WHITE && dur.look().colour() == PigLook.Colour.RED
				&& brk.look().colour() == PigLook.Colour.BERKSHIRE, Component.literal("white, red, Berkshire"));
		lw.startHeatForDisplay();
		lw.sureConceptionForTest();
		helper.assertTrue(lw.conceive(dur.genome(), dur.genes(), level.getGameTime()), Component.literal("she conceives"));
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		lw.saveWithoutId(out);
		PigEntity b = FarmingsEntities.PIG.create(level, EntitySpawnReason.LOAD);
		b.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		helper.assertTrue(b.breed() == PigBreed.LARGE_WHITE && b.genome().equals(lw.genome()) && b.genes().equals(lw.genes()) && b.pregnant(),
				Component.literal("breed, genome, genes and pregnancy survive"));
		for (Trait t : Species.PIG.traits()) {
			helper.assertTrue(b.score(t) == lw.score(t), Component.literal("score " + t.id + " survives"));
		}
		helper.succeed();
	}

	/** Piglet, gilt at weaning (21 days); a castrated boar piglet grows as a barrow; a grown male is a boar. */
	static void pigLifecycle(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		PigEntity piglet = pig(helper, PigBreed.DUROC, Sex.FEMALE, 11L, 10, new BlockPos(2, 1, 2));
		helper.assertTrue(piglet.stage() == PigStage.PIGLET && piglet.isBaby(), Component.literal("a 10-day pig is a piglet"));
		piglet.advanceForTest(20);
		piglet.settle(helper.getLevel());
		helper.assertTrue(piglet.stage() == PigStage.GILT, Component.literal("weaned she is a gilt: " + piglet.stage()));
		PigEntity male = pig(helper, PigBreed.DUROC, Sex.MALE, 12L, 10, new BlockPos(5, 1, 2));
		var player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(HAND, new ItemStack(Items.SHEARS));
		male.mobInteract(player, HAND);
		male.advanceForTest(300);
		male.setWeightKg(150);
		helper.assertTrue(male.stage() == PigStage.BARROW, Component.literal("he grows as a barrow: " + male.stage()));
		helper.assertTrue(adult(helper, PigBreed.BERKSHIRE, Sex.MALE, 13L, new BlockPos(5, 1, 5)).stage() == PigStage.BOAR, Component.literal("a grown male is a boar"));
		player.discard();
		helper.succeed();
	}

	/** A boar serves a sow in heat; she farrows a litter beside her, the piglets at 1.5 kg, with the stillborn ones not spawned. */
	static void sowFarrowsLitter(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		PigEntity sow = adult(helper, PigBreed.LARGE_WHITE, Sex.FEMALE, 21L, new BlockPos(4, 1, 4));
		PigEntity boar = adult(helper, PigBreed.DUROC, Sex.MALE, 22L, new BlockPos(5, 1, 4));
		sow.setNoAi(true);
		boar.setNoAi(true);
		sow.sureConceptionForTest();
		sow.settle(level);
		sow.startHeatForDisplay();
		sow.settle(level);
		helper.assertTrue(sow.pregnant(), Component.literal("the boar serves her: " + sow.readout()));
		sow.forcePigletsForTest(10);
		sow.makeDueNow();
		sow.settle(level);
		List<PigEntity> piglets = around(helper, PigEntity.class).stream().filter(p -> p != sow && p != boar).toList();
		helper.assertTrue(piglets.size() == 10, Component.literal("ten piglets, got " + piglets.size()));
		for (PigEntity p : piglets) {
			helper.assertTrue(p.stage() == PigStage.PIGLET && Math.abs(p.weightKg() - 1.5) < 1e-9, Component.literal("a 1.5 kg piglet"));
			helper.assertTrue(p.composition().equals(Map.of("LWH", 5000, "DUR", 5000)) && p.look().colour() == PigLook.Colour.WHITE,
					Component.literal("an F1 cross, white over red (dominant white)"));
		}
		helper.assertTrue(sow.pigletsNursing() == 10 && sow.feedKgPerDay() == 6.5, Component.literal("she nurses 10 on 6.5 kg a day"));
		helper.succeed();
	}

	/** A market pig gives chops, bellies, shoulders, fat and two hams of its weight. */
	static void pigCutsDrop(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		PigEntity p = pig(helper, PigBreed.DUROC, Sex.FEMALE, 31L, 186, new BlockPos(3, 1, 3));
		p.setWeightKg(120);
		p.kill(helper.getLevel());
		helper.runAfterDelay(5, () -> {
			var items = around(helper, ItemEntity.class).stream().map(ItemEntity::getItem).toList();
			int chops = items.stream().filter(s -> s.is(FarmingsItems.PORK_CHOP)).mapToInt(ItemStack::getCount).sum();
			int bellies = items.stream().filter(s -> s.is(FarmingsItems.PORK_BELLY)).mapToInt(ItemStack::getCount).sum();
			int fat = items.stream().filter(s -> s.is(FarmingsItems.PORK_FAT)).mapToInt(ItemStack::getCount).sum();
			List<ItemStack> hams = items.stream().filter(s -> s.is(FarmingsItems.FRESH_HAM)).toList();
			helper.assertTrue(chops == 24 && bellies == 5 && fat == 12, Component.literal("chops " + chops + ", bellies " + bellies + ", fat " + fat));
			helper.assertTrue(hams.size() == 2 && Math.abs(FarmingsItems.kgOf(hams.get(0)) - 84 * 0.158 / 2) < 1e-6, Component.literal("two hams of 6.6 kg"));
			helper.succeed();
		});
	}

	/** Farmings pigs take vanilla pigs' spawns; a wild group has one boar. */
	static void wildPigReplacesVanilla(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		var plains = TamingGameTests.creatures(level, net.minecraft.world.level.biome.Biomes.PLAINS);
		var types = plains.stream().map(w -> w.value().type()).toList();
		helper.assertTrue(!types.contains(net.minecraft.world.entity.EntityTypes.PIG) && types.contains(FarmingsEntities.PIG),
				Component.literal("plains have Farmings pigs: " + types));
		PigEntity a = FarmingsEntities.PIG.create(level, EntitySpawnReason.NATURAL);
		BlockPos p = helper.absolutePos(new BlockPos(2, 1, 2));
		a.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
		var data = a.finalizeSpawn(level, level.getCurrentDifficultyAt(a.blockPosition()), EntitySpawnReason.NATURAL, null);
		PigEntity b = FarmingsEntities.PIG.create(level, EntitySpawnReason.NATURAL);
		b.snapTo(a.getX() + 1, a.getY(), a.getZ(), 0, 0);
		b.finalizeSpawn(level, level.getCurrentDifficultyAt(b.blockPosition()), EntitySpawnReason.NATURAL, data);
		helper.assertTrue(a.isWild() && b.isWild() && a.breed() == b.breed() && a.sex() == Sex.MALE && b.sex() == Sex.FEMALE,
				Component.literal("a wild group: one breed, one boar"));
		helper.succeed();
	}

	/** A boar's straw takes on a sow in heat within 6 days; past 6 days it is stale. */
	static void boarStrawGoesStale(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		PigEntity boar = adult(helper, PigBreed.BERKSHIRE, Sex.MALE, 41L, new BlockPos(2, 1, 2));
		PigEntity sow = adult(helper, PigBreed.LARGE_WHITE, Sex.FEMALE, 42L, new BlockPos(4, 1, 2));
		boar.setNoAi(true);
		sow.setNoAi(true);
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack empty = new ItemStack(FarmingsItems.EMPTY_STRAW, 1);
		helper.assertTrue(empty.getItem().interactLivingEntity(empty, player, boar, HAND).consumesAction(), Component.literal("the boar fills a straw"));
		ItemStack straw = player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(FarmingsItems.SEMEN_STRAW)).findFirst().orElseThrow();
		long now = level.getGameTime();
		helper.assertTrue(!StrawItem.stale(straw, now + StrawItem.BOAR_FRESH_TICKS - 20) && StrawItem.stale(straw, now + StrawItem.BOAR_FRESH_TICKS + 20),
				Component.literal("fresh for 6 days, then stale"));
		sow.startHeatForDisplay();
		sow.sureConceptionForTest();
		helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, sow, HAND).consumesAction() && sow.pregnant(), Component.literal("the sow takes it"));
		helper.succeed();
	}

	// ---- the pig barn (docs/multiblocks/pig-barn.md) ----

	/**
	 * A pig barn and pasture, relative to the scene: the controller at (1,1,2); slatted floor x 2-4, z 1-3; a feeder north at (2,1,0); a
	 * trough south at (3,1,4); a farrowing crate at (5,1,1) and the pasture gate at (5,1,3); the pasture x 6-13, z 0-5, fenced.
	 */
	static BlockPos buildBarn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = -1; x <= 15; x++) {
			for (int z = -2; z <= 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), x >= 6 && x <= 13 && z >= 0 && z <= 5 ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK
						: net.minecraft.world.level.block.Blocks.STONE);
				for (int y = 1; y <= 4; y++) {
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
		helper.setBlock(new BlockPos(1, 1, 2), dev.farmings.piggery.PigBarnBlocks.PIG_BARN_CONTROLLER);
		for (int x = 2; x <= 4; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), dev.farmings.piggery.PigBarnBlocks.SLATTED_FLOOR);
			}
		}
		helper.setBlock(new BlockPos(5, 1, 1), dev.farmings.piggery.PigBarnBlocks.FARROWING_CRATE);
		helper.setBlock(new BlockPos(5, 1, 3), dev.farmings.shed.ShedBlocks.PASTURE_GATE);
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.HAY_FEEDER, helper.absolutePos(new BlockPos(2, 1, 0)), net.minecraft.core.Direction.NORTH);
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.WATER_TROUGH, helper.absolutePos(new BlockPos(3, 1, 4)), net.minecraft.core.Direction.NORTH);
		return helper.absolutePos(new BlockPos(1, 1, 2));
	}

	static dev.farmings.piggery.PigBarnControllerEntity barn(GameTestHelper helper, BlockPos c) {
		return (dev.farmings.piggery.PigBarnControllerEntity) helper.getLevel().getBlockEntity(c);
	}

	/** The barn forms with its pens, crate, feeder, drinker and pasture; without a drinker it names it. */
	static void pigBarnForms(GameTestHelper helper) {
		BlockPos c = buildBarn(helper);
		ServerLevel level = helper.getLevel();
		var barn = barn(helper, c);
		barn.refresh(level);
		helper.assertTrue(barn.check().formed() && barn.check().pen().size() == 9 && barn.check().lambing().size() == 1,
				Component.literal("the barn forms: " + barn.readout()));
		helper.assertTrue(barn.pasture() != null && barn.pasture().found() && barn.pitCapacityKg() == 4500, Component.literal(barn.readout()));
		level.destroyBlock(helper.absolutePos(new BlockPos(3, 1, 4)), false);
		barn.refresh(level);
		helper.assertTrue(barn.status().equals("No drinker (trough) beside the pen"), Component.literal(barn.status()));
		helper.succeed();
	}

	/** A sow due within 5 days is given the farrowing crate and walks to it. */
	static void sowGoesToCrate(GameTestHelper helper) {
		BlockPos c = buildBarn(helper);
		var barn = barn(helper, c);
		barn.forceDayForTest(false);
		PigEntity sow = adult(helper, PigBreed.LARGE_WHITE, Sex.FEMALE, 51L, new BlockPos(3, 1, 2));
		PigEntity boar = adult(helper, PigBreed.DUROC, Sex.MALE, 52L, new BlockPos(3, 1, 3));
		sow.place().assignShed(c, null);
		boar.place().assignShed(c, null);
		sow.startHeatForDisplay();
		sow.sureConceptionForTest();
		helper.assertTrue(sow.conceive(boar.genome(), boar.genes(), helper.getLevel().getGameTime()), Component.literal("she conceives"));
		sow.setDueInForTest(3);
		BlockPos crate = helper.absolutePos(new BlockPos(5, 1, 1));
		TamingGameTests.waitFor(helper, 0, 600, () -> crate.equals(sow.place().home()) && sow.blockPosition().distManhattan(crate) <= 1,
				() -> "she should go to the crate: home " + sow.place().home() + ", at " + sow.blockPosition() + "; " + barn.readout(), helper::succeed);
	}

	/** A piglet in a crate under a powered heat lamp keeps its health; one without a lamp loses some. */
	static void pigletColdWithoutLamp(GameTestHelper helper) {
		BlockPos c = buildBarn(helper);
		ServerLevel level = helper.getLevel();
		var barn = barn(helper, c);
		barn.refresh(level);
		BlockPos crate = helper.absolutePos(new BlockPos(5, 1, 1));
		PigEntity warm = pig(helper, PigBreed.LARGE_WHITE, Sex.FEMALE, 61L, 3, new BlockPos(5, 1, 1));
		PigEntity cold = pig(helper, PigBreed.LARGE_WHITE, Sex.FEMALE, 62L, 3, new BlockPos(3, 1, 2));
		warm.setNoAi(true);
		cold.setNoAi(true);
		warm.place().assignShed(c, crate);
		cold.place().assignShed(c, helper.absolutePos(new BlockPos(3, 1, 2)));
		helper.setBlock(new BlockPos(5, 3, 1), dev.farmings.piggery.PigBarnBlocks.HEAT_LAMP);
		((dev.farmings.piggery.HeatLampEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(5, 3, 1)))).forceRunning(true);
		float full = cold.getMaxHealth();
		for (int i = 0; i < 50; i++) {
			barn.tend(level, List.of(warm, cold), 0.01);
		}
		helper.assertTrue(warm.getHealth() == full && cold.getHealth() < full, Component.literal("warm " + warm.getHealth() + ", cold " + cold.getHealth()));
		helper.succeed();
	}

	/** The pump moves slurry from the pit along a pipe to a slurry store. */
	static void slurryPumpedToStore(GameTestHelper helper) {
		BlockPos c = buildBarn(helper);
		ServerLevel level = helper.getLevel();
		var barn = barn(helper, c);
		barn.refresh(level);
		barn.addSlurryForTest(100);
		helper.setBlock(new BlockPos(1, 1, 3), dev.farmings.piggery.PigBarnBlocks.SLURRY_PUMP);
		helper.setBlock(new BlockPos(1, 1, 4), dev.farmings.piggery.PigBarnBlocks.SLURRY_PIPE);
		helper.setBlock(new BlockPos(1, 1, 5), dev.farmings.piggery.PigBarnBlocks.SLURRY_PIPE);
		helper.setBlock(new BlockPos(1, 1, 6), dev.farmings.piggery.PigBarnBlocks.SLURRY_STORE);
		var pump = (dev.farmings.piggery.SlurryPumpEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 3)));
		var store = (dev.farmings.piggery.SlurryStoreEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 6)));
		pump.forceRunning(true);
		TamingGameTests.waitFor(helper, 0, 200, () -> store.kg() >= 100 - 1e-6 && barn.pitKg() < 1e-6,
				() -> "the slurry should reach the store: " + pump.readout() + "; " + store.readout(), helper::succeed);
	}

	/** On the pasture a pig roots: grass turns to dirt and it gets part of its feed with an empty feeder. */
	static void pigsRootPasture(GameTestHelper helper) {
		BlockPos c = buildBarn(helper);
		ServerLevel level = helper.getLevel();
		var barn = barn(helper, c);
		barn.refresh(level);
		PigEntity sow = adult(helper, PigBreed.BERKSHIRE, Sex.FEMALE, 71L, new BlockPos(9, 1, 3));
		sow.setNoAi(true);
		sow.setFarrowedForTest(60, 8);     // a sow, her litter weaned
		sow.place().assignShed(c, helper.absolutePos(new BlockPos(3, 1, 2)));
		sow.recordTending(0, 1);
		for (int i = 0; i < 200; i++) {
			barn.tend(level, List.of(sow), 0.01);
		}
		helper.assertTrue(barn.rooted() >= 1, Component.literal("she roots grass: " + barn.rooted()));
		helper.assertTrue(sow.lastFed() > 0.4 && sow.lastFed() < 0.6, Component.literal("a sow gets half her feed rooting, fed " + sow.lastFed()));
		helper.succeed();
	}

	// ---- curing, lard and sausage (docs/multiblocks/curing-room.md) ----

	/** A belly with its cure on a cold rack cures in 7 days, a lit smokehouse beside it smokes it, and out comes bacon; warm, it spoils. */
	static void cureMakesBacon(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(2, 1, 2), dev.farmings.curing.CuringBlocks.CURING_RACK);
		helper.setBlock(new BlockPos(3, 1, 2), dev.farmings.curing.CuringBlocks.SMOKEHOUSE);
		helper.setBlock(new BlockPos(6, 1, 2), dev.farmings.curing.CuringBlocks.CURING_RACK);
		var rack = (dev.farmings.curing.CuringRackEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 2)));
		var warm = (dev.farmings.curing.CuringRackEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(6, 1, 2)));
		var smoke = (dev.farmings.curing.SmokehouseEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 2)));
		helper.assertTrue(!rack.insert(new ItemStack(FarmingsItems.PORK_BELLY), level), Component.literal("no belly goes in without its cure"));
		rack.insert(new ItemStack(FarmingsItems.CURE, 4), level);
		warm.insert(new ItemStack(FarmingsItems.CURE, 2), level);
		helper.assertTrue(rack.insert(new ItemStack(FarmingsItems.PORK_BELLY), level) && warm.insert(new ItemStack(FarmingsItems.PORK_BELLY), level),
				Component.literal("a belly with 2 kg of cure goes in"));
		smoke.insert(new ItemStack(Items.OAK_LOG, 2), level);
		rack.forceColdForTest(true);
		long day = dev.farmings.husbandry.TimeScale.LIVESTOCK.realDaysToTicks(1);
		for (int i = 0; i < 8; i++) {
			rack.run(level, day);
			warm.run(level, day);
		}
		ItemStack bacon = rack.take(level);
		helper.assertTrue(bacon.is(FarmingsItems.BACON) && bacon.getCount() == 3, Component.literal("3 bacon (2 kg x 90%): " + bacon + "; " + rack.readout()));
		helper.assertTrue(warm.take(level).is(FarmingsItems.SPOILED_PORK), Component.literal("the warm rack's belly spoiled: " + warm.readout()));
		helper.succeed();
	}

	/** The lard kettle renders fat over a lit campfire, 0.75 kg of lard a kg; with no fire it waits. */
	static void lardFromFat(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.CAMPFIRE);
		helper.setBlock(new BlockPos(2, 2, 2), dev.farmings.curing.CuringBlocks.LARD_KETTLE);
		var kettle = (dev.farmings.curing.LardKettleEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(2, 2, 2)));
		kettle.insert(new ItemStack(FarmingsItems.PORK_FAT, 12), level);
		kettle.run(level, dev.farmings.husbandry.TimeScale.LIVESTOCK.realDaysToTicks(0.2));
		ItemStack lard = kettle.take(level);
		helper.assertTrue(lard.is(FarmingsItems.LARD) && lard.getCount() == 9, Component.literal("12 kg of fat gives 9 lard: " + lard));
		helper.succeed();
	}

	/** The grinder makes 32 sausages from 3 shoulders, 2 fat and a salt; salt is boiled from a water bucket, which comes back. */
	static void sausageFromGrinder(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(2, 1, 2), dev.farmings.curing.CuringBlocks.MEAT_GRINDER);
		var grinder = (dev.farmings.curing.MeatGrinderEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 2)));
		for (int i = 0; i < 3; i++) {
			grinder.insert(new ItemStack(FarmingsItems.PORK_SHOULDER), level);
		}
		grinder.insert(new ItemStack(FarmingsItems.PORK_FAT), level);
		helper.assertTrue(grinder.take(level).isEmpty(), Component.literal("not enough fat yet"));
		grinder.insert(new ItemStack(FarmingsItems.PORK_FAT), level);
		grinder.insert(new ItemStack(FarmingsItems.SALT), level);
		ItemStack out = grinder.take(level);
		helper.assertTrue(out.is(FarmingsItems.SAUSAGE) && out.getCount() == 32, Component.literal("32 sausages: " + out));
		helper.assertTrue(new ItemStack(Items.WATER_BUCKET).getItem().getCraftingRemainder().is(Items.BUCKET),
				Component.literal("the salt recipe gives the bucket back"));
		helper.succeed();
	}
}
