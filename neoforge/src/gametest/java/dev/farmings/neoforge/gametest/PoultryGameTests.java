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

	// ---- wild fowl (docs/animals/poultry.md, "Wild fowl") ----

	/** Natural spawns are wild; a group shares a breed and has one male; wild hens lay nothing; summoned birds are herd birds. */
	static void fowlSpawnIsWild(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
		FowlEntity a = FarmingsEntities.CHICKEN.create(level, EntitySpawnReason.NATURAL);
		a.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		var data = a.finalizeSpawn(level, level.getCurrentDifficultyAt(a.blockPosition()), EntitySpawnReason.NATURAL, null);
		FowlEntity b = FarmingsEntities.CHICKEN.create(level, EntitySpawnReason.NATURAL);
		b.snapTo(a.getX() + 1, a.getY(), a.getZ(), 0, 0);
		b.finalizeSpawn(level, level.getCurrentDifficultyAt(b.blockPosition()), EntitySpawnReason.NATURAL, data);
		level.addFreshEntity(a);
		level.addFreshEntity(b);
		helper.assertTrue(a.isWild() && b.isWild(), Component.literal("natural spawns are wild"));
		helper.assertTrue(a.breed() == b.breed(), Component.literal("a spawn group shares one breed"));
		helper.assertTrue(a.sex() == Sex.MALE && b.sex() == Sex.FEMALE, Component.literal("the first of a group is the rooster"));
		helper.assertTrue(b.adult() && !b.laying(), Component.literal("a wild hen lays no eggs: " + b.readout()));
		helper.assertTrue(b.readout().startsWith("WILD"), Component.literal(b.readout()));
		FowlEntity tame = helper.spawn(FarmingsEntities.DUCK, new BlockPos(5, 1, 5), EntitySpawnReason.COMMAND);
		helper.assertTrue(!tame.isWild(), Component.literal("a summoned duck is a herd bird"));
		helper.succeed();
	}

	/**
	 * Vanilla chickens are gone from every biome: plains share the chicken entry (weight 10, groups of 4) between quail (3) and chickens
	 * (7); forests get turkeys, savannas quail, rivers ducks.
	 */
	static void fowlBiomeSpawnsReplaced(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var plains = TamingGameTests.creatures(level, net.minecraft.world.level.biome.Biomes.PLAINS);
		helper.assertTrue(plains.stream().noneMatch(w -> w.value().type() == net.minecraft.world.entity.EntityTypes.CHICKEN),
				Component.literal("no vanilla chickens in plains"));
		helper.assertTrue(plains.stream().anyMatch(w -> w.value().type() == FarmingsEntities.CHICKEN && w.weight() == 7 && w.value().count().maxInclusive() == 4),
				Component.literal("plains chickens at weight 7, groups of 4"));
		helper.assertTrue(plains.stream().anyMatch(w -> w.value().type() == FarmingsEntities.QUAIL && w.weight() == 3), Component.literal("plains quail at weight 3"));
		helper.assertTrue(TamingGameTests.creatures(level, net.minecraft.world.level.biome.Biomes.FOREST).stream()
				.anyMatch(w -> w.value().type() == FarmingsEntities.TURKEY && w.weight() == 10), Component.literal("forest turkeys"));
		helper.assertTrue(TamingGameTests.creatures(level, net.minecraft.world.level.biome.Biomes.SAVANNA).stream()
				.anyMatch(w -> w.value().type() == FarmingsEntities.QUAIL && w.weight() == 10), Component.literal("savanna quail"));
		helper.assertTrue(TamingGameTests.creatures(level, net.minecraft.world.level.biome.Biomes.RIVER).stream()
				.anyMatch(w -> w.value().type() == FarmingsEntities.DUCK && w.weight() == 10), Component.literal("river ducks"));
		helper.succeed();
	}

	/** A wild chicken follows wheat seeds and keeps away from empty hands. */
	static void fowlFollowsSeeds(GameTestHelper helper) {
		TamingGameTests.field(helper, 12);
		FowlEntity hen = adult(helper, FowlBreed.RHODE_ISLAND_RED, Sex.FEMALE, 11L, new BlockPos(2, 1, 6));
		hen.setWild(true);
		net.minecraft.server.level.ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(new BlockPos(8, 1, 6));
		p.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 90, 0);
		p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS));
		double start = hen.distanceTo(p);
		TamingGameTests.waitFor(helper, 0, 500, () -> hen.distanceTo(p) < start - 2, "she follows the seeds: " + start, () -> {
			double near = hen.distanceTo(p);
			p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			TamingGameTests.waitFor(helper, 0, 500, () -> hen.distanceTo(p) > near + 1, "she keeps away from empty hands: " + near, () -> {
				p.discard();
				helper.succeed();
			});
		});
	}

	/** A wild hen in a corral settles on grain, not hay: 7 real days fed and watered tames her (2.3 game days at 3:1). */
	static void fowlSettlesInCorral(GameTestHelper helper) {
		BlockPos c = TamingGameTests.buildCorral(helper);
		ServerLevel level = helper.getLevel();
		TamingGameTests.fillCorral(helper);
		var post = TamingGameTests.post(helper, c);
		java.util.UUID tim = java.util.UUID.randomUUID();
		post.claim(tim);
		post.refresh(level);
		FowlEntity hen = adult(helper, FowlBreed.PLYMOUTH_ROCK, Sex.FEMALE, 12L, new BlockPos(4, 1, 6));
		hen.setWild(true);
		hen.setNoAi(true);
		int half = (int) dev.farmings.husbandry.TimeScale.FOWL.realDaysToTicks(3.5);
		post.tend(level, hen, hen, half);
		helper.assertTrue(hen.settledDays() == 0, Component.literal("hay is no feed for a hen: " + hen.settledDays()));
		var bait = (dev.farmings.corral.BaitTroughEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(5, 1, 4)));
		for (int i = 0; i < 20; i++) {
			bait.addGrain();
		}
		post.tend(level, hen, hen, half);
		helper.assertTrue(hen.isWild() && Math.abs(hen.settledDays() - 3.5) < 0.01, Component.literal("half way on grain: " + hen.settledDays()));
		post.tend(level, hen, hen, half);
		helper.assertTrue(!hen.isWild() && tim.equals(hen.owner()), Component.literal("tamed, and the corral owner's"));
		helper.assertTrue(bait.grainKg() < 2.0, Component.literal("she ate grain: " + bait.grainKg()));
		helper.succeed();
	}
}
