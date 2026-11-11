package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.entity.FowlEggs;
import dev.farmings.entity.FowlEntity;
import dev.farmings.genetics.PoultryLocus;
import dev.farmings.genetics.PoultryLook;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.husbandry.poultry.FowlBreed;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Poultry scenes (docs/animals/poultry.md). */
final class PoultryGameTests {
	private PoultryGameTests() {
	}

	/** An adult founder of a breed, past its first-egg age. */
	static FowlEntity adult(GameTestHelper helper, FowlBreed breed, Sex sex, long seed, BlockPos at) {
		FowlEntity f = helper.spawn(FarmingsEntities.fowl(breed.species), at, EntitySpawnReason.COMMAND);
		f.initFounder(breed, sex, seed, breed.firstEggDays + 30, helper.getLevel().getGameTime());
		return f;
	}

	/** Every breed can be made, belongs to its species, and reads out as a hen, tom, drake and so on. */
	static void foundersPerBreed(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		int x = 1;
		for (FowlBreed b : FowlBreed.values()) {
			FowlEntity hen = adult(helper, b, Sex.FEMALE, x, new BlockPos(1 + (x % 6), 1, 1 + x / 6));
			helper.assertTrue(hen.species() == b.species && hen.breed() == b, Component.literal(b + " is a " + b.species));
			helper.assertTrue(hen.adult() && hen.laying(), Component.literal(b + " hen lays: " + hen.readout()));
			x++;
		}
		FowlEntity drake = adult(helper, FowlBreed.MALLARD, Sex.MALE, 99L, new BlockPos(6, 1, 4));
		helper.assertTrue(drake.readout().contains("drake"), Component.literal("a male duck is a drake: " + drake.readout()));
		helper.assertTrue(drake.look().plumage() == PoultryLook.Plumage.MALLARD, Component.literal("a Mallard shows the mallard pattern"));
		helper.succeed();
	}

	/** Genes, sex, weight and a mating survive a save. */
	static void fowlSaveSurvives(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity hen = adult(helper, FowlBreed.RHODE_ISLAND_RED, Sex.FEMALE, 1L, new BlockPos(2, 1, 2));
		FowlEntity cock = adult(helper, FowlBreed.LEGHORN, Sex.MALE, 2L, new BlockPos(4, 1, 2));
		hen.mate(cock);
		var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		hen.saveWithoutId(out);
		FowlEntity b = FarmingsEntities.CHICKEN.create(level, EntitySpawnReason.LOAD);
		b.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		helper.assertTrue(b.genome() != null && b.genome().equals(hen.genome()), Component.literal("genome survives"));
		helper.assertTrue(b.breed() == FowlBreed.RHODE_ISLAND_RED && b.sex() == Sex.FEMALE, Component.literal("breed and sex survive"));
		helper.assertTrue(Math.abs(b.weightKg() - hen.weightKg()) < 1e-9, Component.literal("weight survives"));
		helper.assertTrue(b.fertile() && b.genes().equals(hen.genes()), Component.literal("mating and plumage survive"));
		helper.succeed();
	}

	/** With no coop, a hen lays on the ground: a floor egg with her weight and shell. */
	static void henLaysFloorEgg(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity hen = adult(helper, FowlBreed.RHODE_ISLAND_RED, Sex.FEMALE, 3L, new BlockPos(3, 1, 3));
		hen.layNowForTest(level);
		helper.runAfterDelay(5, () -> {
			List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, hen.getBoundingBox().inflate(3));
			helper.assertTrue(items.size() == 1, Component.literal("one egg on the ground, got " + items.size()));
			ItemStack egg = items.get(0).getItem();
			helper.assertTrue(FowlEggs.species(egg) == Species.CHICKEN, Component.literal("a chicken egg"));
			double g = FowlEggs.grams(egg);
			helper.assertTrue(g > 50 && g < 70, Component.literal("a Rhode Island Red egg weighs about 60 g, got " + g));
			helper.assertTrue(FowlEggs.floor(egg) && !FowlEggs.fertile(egg), Component.literal("a floor egg, not fertile (no rooster)"));
			helper.assertTrue(PoultryLook.shellColour(FowlEggs.shade(egg)).equals("brown"), Component.literal("brown shell"));
			helper.succeed();
		});
	}

	/**
	 * A fertile egg carries both parents' genes: a Rhode Island Red rooster over a barred Plymouth Rock hen gives barred sons and
	 * non-barred daughters (barring is on the Z chromosome).
	 */
	static void eggCarriesGenes(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity hen = adult(helper, FowlBreed.PLYMOUTH_ROCK, Sex.FEMALE, 4L, new BlockPos(2, 1, 2));
		FowlEntity cock = adult(helper, FowlBreed.RHODE_ISLAND_RED, Sex.MALE, 5L, new BlockPos(5, 1, 2));
		hen.mate(cock);
		int sons = 0;
		int daughters = 0;
		for (int i = 0; i < 12; i++) {
			ItemStack egg = hen.makeEgg(level.getGameTime() + i);
			helper.assertTrue(FowlEggs.fertile(egg), Component.literal("a mated hen lays fertile eggs"));
			FowlEntity chick = FowlEggs.hatch(level, egg, helper.absolutePos(new BlockPos(3, 1, 4)), null);
			helper.assertTrue(chick != null && chick.species() == Species.CHICKEN, Component.literal("a chick hatches"));
			helper.assertTrue(chick.composition().getOrDefault("PLR", 0) == 5000 && chick.composition().getOrDefault("RIR", 0) == 5000,
					Component.literal("half Plymouth Rock, half Rhode Island Red"));
			helper.assertTrue(chick.young(), Component.literal("a hatchling needs heat"));
			int barred = chick.genes().expressed(PoultryLocus.BARRING, chick.sex());
			if (chick.sex() == Sex.MALE) {
				sons++;
				helper.assertTrue(barred == 1, Component.literal("sons are barred"));
			} else {
				daughters++;
				helper.assertTrue(barred == 0, Component.literal("daughters are not"));
			}
			chick.discard();
		}
		helper.assertTrue(sons > 0 && daughters > 0, Component.literal("both sexes hatch: " + sons + " sons, " + daughters + " daughters"));
		helper.succeed();
	}

	/** A hen near a standard tom is mated; a Broad Breasted White tom cannot mate (Livestock Conservancy). */
	static void bbwTomCannotMate(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity bbwHen = adult(helper, FowlBreed.BROAD_BREASTED_WHITE, Sex.FEMALE, 6L, new BlockPos(1, 1, 1));
		FowlEntity bbwTom = adult(helper, FowlBreed.BROAD_BREASTED_WHITE, Sex.MALE, 7L, new BlockPos(2, 1, 1));
		helper.assertTrue(!bbwTom.canServe(), Component.literal("a BBW tom cannot serve"));
		bbwHen.settle(level);
		helper.assertTrue(!bbwHen.fertile(), Component.literal("a BBW hen beside only a BBW tom stays unmated"));
		FowlEntity bronzeTom = adult(helper, FowlBreed.BRONZE, Sex.MALE, 8L, new BlockPos(1, 1, 3));
		helper.assertTrue(bronzeTom.canServe(), Component.literal("a Bronze tom can"));
		bbwHen.settle(level);
		helper.assertTrue(bbwHen.fertile(), Component.literal("a Bronze tom serves her"));
		helper.succeed();
	}
}
