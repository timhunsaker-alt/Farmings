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
			hen.setDayOfYearForTest(110);     // 20 April: geese and guinea fowl lay in season
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
		TamingGameTests.field(helper, 32);     // room to flee: the flee point is up to 16 blocks away, and must be on solid ground
		FowlEntity hen = adult(helper, FowlBreed.RHODE_ISLAND_RED, Sex.FEMALE, 11L, new BlockPos(18, 1, 16));
		hen.setWild(true);
		net.minecraft.server.level.ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(new BlockPos(24, 1, 16));
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

	// ---- the coop (docs/multiblocks/coop.md) ----

	/**
	 * A coop on a stone field x -1..14, z -2..7: the controller at (1,1,2); floor x 2-4, z 1-3; nest boxes at (2,1,0) and (3,1,0); a
	 * perch at (4,1,0); a grain feeder at (2,1,4) and a water trough at (3,1,4); a pop hole at (5,1,2) into a fenced run x 6-12, z 0-5.
	 */
	static BlockPos buildCoop(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = -1; x <= 14; x++) {
			for (int z = -2; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 0, z), x >= 6 ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK : net.minecraft.world.level.block.Blocks.STONE);
				for (int y = 1; y <= 3; y++) {
					helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
				}
			}
		}
		for (int x = 5; x <= 13; x++) {
			for (int z = -1; z <= 6; z++) {
				if (x == 5 || x == 13 || z == -1 || z == 6) {
					helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.OAK_FENCE);
				}
			}
		}
		helper.setBlock(new BlockPos(1, 1, 2), dev.farmings.coop.CoopBlocks.COOP_CONTROLLER);
		for (int x = 2; x <= 4; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), dev.farmings.coop.CoopBlocks.COOP_FLOOR);
			}
		}
		helper.setBlock(new BlockPos(5, 1, 2), dev.farmings.coop.CoopBlocks.POP_HOLE);
		helper.setBlock(new BlockPos(2, 1, 0), dev.farmings.coop.CoopBlocks.NEST_BOX);
		helper.setBlock(new BlockPos(3, 1, 0), dev.farmings.coop.CoopBlocks.NEST_BOX);
		helper.setBlock(new BlockPos(4, 1, 0), dev.farmings.coop.CoopBlocks.PERCH);
		helper.setBlock(new BlockPos(2, 1, 4), dev.farmings.coop.CoopBlocks.GRAIN_FEEDER);
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.WATER_TROUGH, helper.absolutePos(new BlockPos(3, 1, 4)), net.minecraft.core.Direction.NORTH);
		return helper.absolutePos(new BlockPos(1, 1, 2));
	}

	static dev.farmings.coop.CoopControllerEntity coop(GameTestHelper helper, BlockPos c) {
		return (dev.farmings.coop.CoopControllerEntity) helper.getLevel().getBlockEntity(c);
	}

	static void fillCoop(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var feeder = (dev.farmings.coop.GrainFeederEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 4)));
		for (int i = 0; i < 100; i++) {
			feeder.addItem();
		}
		((dev.farmings.barn.TroughEntity) level.getBlockEntity(dev.wildcatter.multiblock.BigParts.owner(level, helper.absolutePos(new BlockPos(3, 1, 4))))).pourBucket();
	}

	static dev.farmings.coop.NestBoxEntity nest(GameTestHelper helper, int x) {
		return (dev.farmings.coop.NestBoxEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(x, 1, 0)));
	}

	/** The coop forms with its run; without its grain feeder it names it. */
	static void coopForms(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		ServerLevel level = helper.getLevel();
		var coop = coop(helper, c);
		coop.refresh(level);
		helper.assertTrue(coop.check().formed(), Component.literal("the coop should form: " + coop.check().problem()));
		helper.assertTrue(coop.check().floor().size() == 9 && coop.check().nests().size() == 2 && coop.check().perches().size() == 1,
				Component.literal("9 floor cells, 2 nests, 1 perch: " + coop.readout()));
		helper.assertTrue(coop.run() != null && coop.run().spots().size() > 30, Component.literal("a run behind the pop hole: " + coop.readout()));
		helper.setBlock(new BlockPos(2, 1, 4), net.minecraft.world.level.block.Blocks.AIR);
		coop.refresh(level);
		helper.assertTrue(coop.check().problem().startsWith("No grain feeder"), Component.literal(coop.check().problem()));
		helper.succeed();
	}

	/** The coop takes in a tame bird beside it but leaves a wild one alone. */
	static void coopTakesTameBirds(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		FowlEntity tame = adult(helper, FowlBreed.LEGHORN, Sex.FEMALE, 21L, new BlockPos(3, 1, 2));
		FowlEntity wild = adult(helper, FowlBreed.LEGHORN, Sex.FEMALE, 22L, new BlockPos(4, 1, 2));
		wild.setWild(true);
		helper.runAfterDelay(41, () -> {
			helper.assertTrue(c.equals(tame.coop()) && tame.home() != null, Component.literal("the tame hen is taken in"));
			helper.assertTrue(wild.coop() == null, Component.literal("the wild hen is not"));
			helper.succeed();
		});
	}

	/** A hen in a coop lays into a nest box, not on the floor. */
	static void henLaysInNest(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		ServerLevel level = helper.getLevel();
		coop(helper, c).refresh(level);
		FowlEntity hen = adult(helper, FowlBreed.RHODE_ISLAND_RED, Sex.FEMALE, 23L, new BlockPos(3, 1, 1));
		hen.assignCoop(c, null);
		hen.layNowForTest(level);
		hen.layNowForTest(level);
		int inNests = nest(helper, 2).count() + nest(helper, 3).count();
		helper.assertTrue(inNests == 2 && hen.laidForTest().isEmpty(), Component.literal("two eggs in the nests, none on the floor: " + inNests));
		helper.assertTrue(!FowlEggs.floor(nest(helper, 3).getItem(0).isEmpty() ? nest(helper, 2).getItem(0) : nest(helper, 3).getItem(0)),
				Component.literal("a nest egg is not a floor egg"));
		helper.succeed();
	}

	/** A broody Silkie sits on six fertile eggs; after 21 days of warmth they hatch beside the nest and she mothers the chicks. */
	static void broodyHenHatches(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		ServerLevel level = helper.getLevel();
		var coop = coop(helper, c);
		coop.refresh(level);
		FowlEntity hen = adult(helper, FowlBreed.SILKIE, Sex.FEMALE, 24L, new BlockPos(2, 1, 1));
		FowlEntity cock = adult(helper, FowlBreed.SILKIE, Sex.MALE, 25L, new BlockPos(4, 1, 3));
		hen.mate(cock);
		cock.discard();
		for (int i = 0; i < 6; i++) {
			nest(helper, 2).add(hen.makeEgg(level.getGameTime() + i));
		}
		hen.assignCoop(c, helper.absolutePos(new BlockPos(2, 1, 1)));
		hen.setBroody(true, helper.absolutePos(new BlockPos(2, 1, 1)));
		hen.setNoAi(true);
		java.util.List<FowlEntity> members = java.util.List.of(hen);
		long half = dev.farmings.husbandry.TimeScale.FOWL.realDaysToTicks(10.5);
		coop.brood(level, members, half);
		helper.assertTrue(hen.broody() && nest(helper, 2).count() == 6, Component.literal("half way, still sitting"));
		helper.assertTrue(FowlEggs.warmDays(nest(helper, 2).getItem(0)) > 10, Component.literal("the eggs are warmed"));
		coop.brood(level, members, half);
		helper.assertTrue(coop.hatched() == 6 && nest(helper, 2).count() == 0, Component.literal("six chicks hatch: " + coop.hatched()));
		coop.brood(level, members, 20);
		helper.assertTrue(!hen.broody() && hen.mothering(), Component.literal("she stops sitting and mothers them: " + hen.readout()));
		var chicks = level.getEntitiesOfClass(FowlEntity.class, hen.getBoundingBox().inflate(4), f -> f.young());
		helper.assertTrue(chicks.size() == 6 && chicks.stream().allMatch(f -> c.equals(f.coop())), Component.literal("six chicks in the coop: " + chicks.size()));
		helper.succeed();
	}

	/** By day the birds go out on the run; at night they come in. */
	static void birdsOutByDayInAtNight(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		fillCoop(helper);
		var coop = coop(helper, c);
		coop.forceDayForTest(true);
		FowlEntity hen = adult(helper, FowlBreed.PLYMOUTH_ROCK, Sex.FEMALE, 26L, new BlockPos(3, 1, 2));
		TamingGameTests.waitFor(helper, 0, 200, () -> hen.sentTo() != null && hen.sentTo().getX() >= helper.absolutePos(new BlockPos(6, 1, 0)).getX(),
				"she is sent out to the run: " + coop.readout(), () -> {
					coop.forceDayForTest(false);
					TamingGameTests.waitFor(helper, 0, 100, () -> hen.sentTo() == null, "she is called in at night", helper::succeed);
				});
	}

	/** Ducks without water in or beside their run lose a little health each day; with a pond they don't. */
	static void duckNeedsPond(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		ServerLevel level = helper.getLevel();
		var coop = coop(helper, c);
		coop.refresh(level);
		FowlEntity duck = adult(helper, FowlBreed.PEKIN, Sex.FEMALE, 27L, new BlockPos(3, 1, 2));
		duck.assignCoop(c, null);
		java.util.List<FowlEntity> members = java.util.List.of(duck);
		float full = duck.getMaxHealth();
		coop.daily(level, members);
		helper.assertTrue(duck.getHealth() < full, Component.literal("no pond: she loses health: " + duck.getHealth()));
		helper.setBlock(new BlockPos(10, 0, 3), net.minecraft.world.level.block.Blocks.WATER);
		coop.refresh(level);
		helper.assertTrue(coop.pond() >= 1, Component.literal("the pond counts: " + coop.readout()));
		duck.setHealth(full);
		coop.daily(level, members);
		helper.assertTrue(duck.getHealth() == full, Component.literal("with a pond she keeps her health"));
		helper.succeed();
	}

	/** Birds leave litter; a hopper under the controller pulls it out as 10 kg items. */
	static void litterBuildsUp(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		ServerLevel level = helper.getLevel();
		fillCoop(helper);
		var coop = coop(helper, c);
		coop.refresh(level);
		FowlEntity hen = adult(helper, FowlBreed.PLYMOUTH_ROCK, Sex.FEMALE, 28L, new BlockPos(3, 1, 2));
		hen.assignCoop(c, null);
		coop.tend(level, java.util.List.of(hen), 100);          // 100 real days of one 3.4 kg hen: 3.4 x 0.05 x 100 = 17 kg
		helper.assertTrue(Math.abs(coop.litterKg() - 17.0) < 0.1, Component.literal("17 kg of litter: " + coop.litterKg()));
		ItemStack out = coop.removeItem(0, 64);
		helper.assertTrue(out.is(dev.farmings.FarmingsItems.POULTRY_LITTER) && out.getCount() == 1, Component.literal("one 10 kg item: " + out));
		helper.succeed();
	}

	/** A chick out of the warmth loses health each day; a warm one does not. */
	static void chickColdWithoutHeat(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity hen = adult(helper, FowlBreed.LEGHORN, Sex.FEMALE, 29L, new BlockPos(2, 1, 2));
		FowlEntity cock = adult(helper, FowlBreed.LEGHORN, Sex.MALE, 30L, new BlockPos(5, 1, 5));
		hen.mate(cock);
		FowlEntity cold = FowlEggs.hatch(level, hen.makeEgg(level.getGameTime()), helper.absolutePos(new BlockPos(3, 1, 3)), null);
		FowlEntity warm = FowlEggs.hatch(level, hen.makeEgg(level.getGameTime() + 1), helper.absolutePos(new BlockPos(4, 1, 3)), null);
		warm.keepWarm(level.getGameTime() + 1000);
		float full = cold.getHealth();
		cold.coldDayForTest();
		warm.coldDayForTest();
		helper.assertTrue(cold.getHealth() < full && warm.getHealth() == full, Component.literal("cold " + cold.getHealth() + ", warm " + warm.getHealth()));
		helper.assertTrue(cold.readout().contains("NEEDS HEAT"), Component.literal(cold.readout()));
		helper.succeed();
	}

	// ---- the hatchery (docs/multiblocks/hatchery.md) ----

	static java.util.List<ItemStack> fertileEggs(GameTestHelper helper, FowlBreed breed, int n, long seed) {
		ServerLevel level = helper.getLevel();
		FowlEntity hen = adult(helper, breed, Sex.FEMALE, seed, new BlockPos(8, 1, 8));
		FowlEntity male = adult(helper, breed, Sex.MALE, seed + 1, new BlockPos(9, 1, 8));
		hen.mate(male);
		java.util.List<ItemStack> eggs = new java.util.ArrayList<>();
		for (int i = 0; i < n; i++) {
			eggs.add(hen.makeEgg(level.getGameTime() + i));
		}
		hen.discard();
		male.discard();
		return eggs;
	}

	/** A powered incubator hatches chicken eggs after 21 days and duck eggs after 28; it refuses infertile eggs. */
	static void incubatorHatches(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(3, 1, 3), dev.farmings.hatchery.HatcheryBlocks.INCUBATOR);
		var inc = (dev.farmings.hatchery.IncubatorEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 3)));
		inc.forceRunning(true);
		for (ItemStack e : fertileEggs(helper, FowlBreed.LEGHORN, 3, 31L)) {
			inc.add(e);
		}
		for (ItemStack e : fertileEggs(helper, FowlBreed.PEKIN, 2, 41L)) {
			inc.add(e);
		}
		FowlEntity layer = adult(helper, FowlBreed.LEGHORN, Sex.FEMALE, 51L, new BlockPos(7, 1, 1));
		helper.assertTrue(!inc.add(layer.makeEgg(level.getGameTime())), Component.literal("an infertile egg is refused"));
		inc.warm(level, dev.farmings.husbandry.TimeScale.FOWL.realDaysToTicks(20.9));
		helper.assertTrue(inc.hatched() == 0 && inc.count() == 5, Component.literal("nothing at 20.9 days: " + inc.readout()));
		inc.warm(level, dev.farmings.husbandry.TimeScale.FOWL.realDaysToTicks(0.2));
		helper.assertTrue(inc.hatched() == 3 && inc.count() == 2, Component.literal("the chicks hatch at 21 days: " + inc.readout()));
		inc.warm(level, dev.farmings.husbandry.TimeScale.FOWL.realDaysToTicks(7));
		helper.assertTrue(inc.hatched() == 5 && inc.count() == 0, Component.literal("the ducklings at 28: " + inc.readout()));
		var ducklings = level.getEntitiesOfClass(FowlEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(3, 1, 3))).inflate(2),
				f -> f.species() == Species.DUCK && f.young());
		helper.assertTrue(ducklings.size() == 2, Component.literal("two ducklings beside it: " + ducklings.size()));
		helper.succeed();
	}

	/** Without power the incubator's clocks stop; nothing is lost. */
	static void incubatorNoPower(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(3, 1, 3), dev.farmings.hatchery.HatcheryBlocks.INCUBATOR);
		var inc = (dev.farmings.hatchery.IncubatorEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 3)));
		inc.forceRunning(true);
		inc.add(fertileEggs(helper, FowlBreed.COTURNIX, 1, 61L).get(0));
		inc.warm(level, dev.farmings.husbandry.TimeScale.FOWL.realDaysToTicks(5));
		inc.forceRunning(false);
		inc.warm(level, dev.farmings.husbandry.TimeScale.FOWL.realDaysToTicks(30));
		helper.assertTrue(inc.count() == 1 && Math.abs(FowlEggs.warmDays(inc.getItem(0)) - 5) < 0.01, Component.literal("paused at 5 days: " + inc.readout()));
		helper.assertTrue(inc.readout().contains("NO POWER"), Component.literal(inc.readout()));
		inc.forceRunning(true);
		inc.warm(level, dev.farmings.husbandry.TimeScale.FOWL.realDaysToTicks(12));
		helper.assertTrue(inc.hatched() == 1, Component.literal("a quail hatches at 17 days: " + inc.readout()));
		helper.succeed();
	}

	/** A powered brooder keeps the chicks under it warm, and feeds the ones with no coop from a feeder nearby. */
	static void brooderKeepsChicks(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(3, 2, 3), dev.farmings.hatchery.HatcheryBlocks.BROODER);
		helper.setBlock(new BlockPos(5, 1, 3), dev.farmings.coop.CoopBlocks.GRAIN_FEEDER);
		var feeder = (dev.farmings.coop.GrainFeederEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(5, 1, 3)));
		for (int i = 0; i < 10; i++) {
			feeder.addItem();
		}
		var br = (dev.farmings.hatchery.BrooderEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 2, 3)));
		var eggs = fertileEggs(helper, FowlBreed.RHODE_ISLAND_RED, 2, 71L);
		FowlEntity under = FowlEggs.hatch(level, eggs.get(0), helper.absolutePos(new BlockPos(3, 1, 3)), null);
		FowlEntity away = FowlEggs.hatch(level, eggs.get(1), helper.absolutePos(new BlockPos(8, 1, 8)), null);
		under.setNoAi(true);
		away.setNoAi(true);
		br.forceRunning(true);
		br.step(level, 20);
		helper.assertTrue(under.warm() && !away.warm(), Component.literal("warm under the lamp, cold away from it"));
		helper.assertTrue(feeder.kg() < 1.0, Component.literal("the chick under it ate from the feeder: " + feeder.kg()));
		br.forceRunning(false);
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(!under.warm(), Component.literal("no power, no warmth"));
			helper.succeed();
		});
	}

	// ---- products (docs/eggs.md) ----

	/** Twelve Leghorn eggs of one size and grade fill a carton; fertile eggs are refused; the carton opens back into its eggs. */
	static void gradingPacksCartons(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(3, 1, 3), dev.farmings.eggs.EggBlocks.EGG_GRADER);
		var g = (dev.farmings.eggs.EggGraderEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 3)));
		FowlEntity hen = adult(helper, FowlBreed.LEGHORN, Sex.FEMALE, 81L, new BlockPos(6, 1, 6));
		hen.advanceForTest((32 - 20.4) * 7 - 30);      // about 32 weeks old: 58.5 g eggs, USDA Large
		String label = null;
		for (int i = 0; i < 12; i++) {
			ItemStack egg = hen.makeEgg(level.getGameTime());
			label = dev.farmings.eggs.EggGraderEntity.label(egg, level.getGameTime());
			helper.assertTrue(g.grade(egg), Component.literal("a table egg is graded"));
		}
		helper.assertTrue("Large, Grade AA".equals(label), Component.literal("fresh Leghorn eggs are Large AA: " + label + " (" + g.readout() + ")"));
		helper.assertTrue(g.cartons().size() == 1, Component.literal("one carton of 12: " + g.readout()));
		ItemStack carton = g.removeItem(1, 1);
		helper.assertTrue(dev.farmings.item.EggCartonItem.eggs(carton).size() == 12 && dev.farmings.item.EggCartonItem.label(carton).equals("Large, Grade AA"),
				Component.literal("the carton holds the 12 eggs and their label"));
		var fertile = fertileEggs(helper, FowlBreed.LEGHORN, 1, 82L).get(0);
		helper.assertTrue(!g.grade(fertile), Component.literal("a fertile egg is a hatching egg, not graded"));
		helper.succeed();
	}

	/** A table egg's spoil clock starts at laying (7 days out, 35 cold); a fertile egg has none. */
	static void tableEggsSpoil(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity hen = adult(helper, FowlBreed.RHODE_ISLAND_RED, Sex.FEMALE, 83L, new BlockPos(2, 1, 2));
		ItemStack table = hen.makeEgg(level.getGameTime());
		long at = dev.farmings.item.Perishables.spoilsAt(table);
		helper.assertTrue(at >= level.getGameTime() + 7 * 24_000 && at < level.getGameTime() + 7 * 24_000 + 2_000, Component.literal("spoils 7 days on: " + at));
		ItemStack rotten = dev.farmings.item.Perishables.spoiledVersion(table, at);
		helper.assertTrue(rotten != null && rotten.is(dev.farmings.FarmingsItems.ROTTEN_EGG), Component.literal("it becomes a rotten egg"));
		ItemStack fertile = fertileEggs(helper, FowlBreed.RHODE_ISLAND_RED, 1, 84L).get(0);
		helper.assertTrue(dev.farmings.item.Perishables.spoilsAt(fertile) == Long.MIN_VALUE, Component.literal("a fertile egg has no spoil clock"));
		helper.assertTrue(dev.farmings.FarmingsItems.POULTRY_LITTER.getDefaultInstance().has(net.minecraft.core.component.DataComponents.COMPOSTABLE),
				Component.literal("poultry litter goes in the composter"));
		helper.succeed();
	}

	/** A Bronze tom gives turkey by dressed weight and feathers; a duck gives duck meat and down. */
	static void poultryMeatAndDown(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity tom = adult(helper, FowlBreed.BRONZE, Sex.MALE, 85L, new BlockPos(2, 1, 2));
		FowlEntity duck = adult(helper, FowlBreed.PEKIN, Sex.FEMALE, 86L, new BlockPos(6, 1, 6));
		int turkeyWant = dev.farmings.husbandry.poultry.Fowl.meatItems(tom.composition(), tom.weightKg());
		tom.kill(level);
		duck.kill(level);
		helper.runAfterDelay(5, () -> {
			var items = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(12));
			int turkey = items.stream().filter(i -> i.getItem().is(dev.farmings.FarmingsItems.TURKEY_MEAT)).mapToInt(i -> i.getItem().getCount()).sum();
			int duckMeat = items.stream().filter(i -> i.getItem().is(dev.farmings.FarmingsItems.DUCK_MEAT)).mapToInt(i -> i.getItem().getCount()).sum();
			boolean down = items.stream().anyMatch(i -> i.getItem().is(dev.farmings.FarmingsItems.DOWN));
			boolean feathers = items.stream().anyMatch(i -> i.getItem().is(net.minecraft.world.item.Items.FEATHER));
			helper.assertTrue(turkey == turkeyWant && turkey >= 20, Component.literal("turkey " + turkey + ", expected " + turkeyWant));
			helper.assertTrue(duckMeat > 0 && down && feathers, Component.literal("duck " + duckMeat + ", down " + down + ", feathers " + feathers));
			helper.succeed();
		});
	}

	// ---- loose ends, 2026-11-11 ----

	/** A thrown rotten egg flies like a snowball and gives what it hits Nausea (docs/eggs.md, "Loose ends"). */
	static void rottenEggThrownNauseates(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		net.minecraft.world.entity.animal.cow.Cow cow = helper.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(5, 1, 2));
		cow.setNoAi(true);
		net.minecraft.server.level.ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(new BlockPos(1, 1, 2));
		p.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, -90, 10);
		ItemStack eggs = new ItemStack(dev.farmings.FarmingsItems.ROTTEN_EGG, 2);
		p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, eggs);
		eggs.use(level, p, net.minecraft.world.InteractionHand.MAIN_HAND);
		helper.assertTrue(p.getMainHandItem().getCount() == 1, Component.literal("one egg thrown, one left: " + p.getMainHandItem()));
		TamingGameTests.waitFor(helper, 0, 60, () -> cow.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA),
				() -> "the cow it hits gets Nausea; eggs flying " + level.getEntitiesOfClass(dev.farmings.entity.ThrownRottenEgg.class, cow.getBoundingBox().inflate(8)).size(), () -> {
					p.discard();
					helper.succeed();
				});
	}

	/** A Farmings chicken drops Farmings chicken, which spoils into spoiled poultry (docs/eggs.md, "Loose ends"). */
	static void farmChickenSpoils(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity hen = adult(helper, FowlBreed.PLYMOUTH_ROCK, Sex.FEMALE, 91L, new BlockPos(3, 1, 3));
		hen.kill(level);
		helper.runAfterDelay(5, () -> {
			var items = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12));
			ItemStack meat = items.stream().map(ItemEntity::getItem).filter(i -> i.is(dev.farmings.FarmingsItems.CHICKEN)).findFirst().orElse(ItemStack.EMPTY);
			helper.assertTrue(!meat.isEmpty(), Component.literal("a Farmings chicken drops farmings:chicken"));
			helper.assertTrue(items.stream().noneMatch(i -> i.getItem().is(net.minecraft.world.item.Items.CHICKEN)), Component.literal("and no vanilla chicken"));
			long now = level.getGameTime();
			ItemStack later = dev.farmings.item.Perishables.spoiledVersion(meat, now + 2 * 24_000L);
			helper.assertTrue(later != null && later.is(dev.farmings.FarmingsItems.SPOILED_POULTRY), Component.literal("two days out of the cold it spoils: " + later));
			helper.assertTrue(dev.farmings.item.Perishables.spoiledVersion(meat, now + 100) == null, Component.literal("fresh at first"));
			helper.succeed();
		});
	}

	/** A Coturnix male serves at most three hens at once (poultry.md, "Hens per male"). */
	static void maleServesLimitedHens(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity male = adult(helper, FowlBreed.COTURNIX, Sex.MALE, 92L, new BlockPos(4, 1, 4));
		java.util.List<FowlEntity> hens = new java.util.ArrayList<>();
		for (int i = 0; i < 5; i++) {
			FowlEntity h = adult(helper, FowlBreed.COTURNIX, Sex.FEMALE, 93L + i, new BlockPos(2 + i, 1, 3));
			h.setNoAi(true);
			hens.add(h);
		}
		male.setNoAi(true);
		for (FowlEntity h : hens) {
			h.settle(level);
		}
		long fertile = hens.stream().filter(FowlEntity::fertile).count();
		helper.assertTrue(fertile == 3, Component.literal("three hens fertile from one quail male, got " + fertile));
		helper.assertTrue(male.hensServed(level) == 3, Component.literal("he counts three: " + male.hensServed(level)));
		helper.succeed();
	}

	/** Coturnix and Bobwhite don't cross (poultry.md, "The two quail don't cross"). */
	static void quailSpeciesDontCross(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity bob = adult(helper, FowlBreed.BOBWHITE, Sex.MALE, 97L, new BlockPos(3, 1, 3));
		FowlEntity hen = adult(helper, FowlBreed.COTURNIX, Sex.FEMALE, 98L, new BlockPos(4, 1, 3));
		bob.setNoAi(true);
		hen.setNoAi(true);
		hen.settle(level);
		helper.assertTrue(!hen.fertile(), Component.literal("a Bobwhite male doesn't serve a Coturnix hen"));
		hen.mate(bob);
		helper.assertTrue(!hen.fertile(), Component.literal("nor by mating directly"));
		FowlEntity cot = adult(helper, FowlBreed.COTURNIX, Sex.MALE, 99L, new BlockPos(5, 1, 3));
		cot.setNoAi(true);
		hen.settle(level);
		helper.assertTrue(hen.fertile(), Component.literal("a Coturnix male does"));
		helper.succeed();
	}

	/** A hen on the grass run takes a fifth of her feed as forage; heavy use bares grass (poultry.md, "Foraging on the run"). */
	static void hensForageOnRun(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		ServerLevel level = helper.getLevel();
		fillCoop(helper);
		var coop = coop(helper, c);
		coop.refresh(level);
		var feeder = (dev.farmings.coop.GrainFeederEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 4)));
		FowlEntity inside = adult(helper, FowlBreed.PLYMOUTH_ROCK, Sex.FEMALE, 101L, new BlockPos(3, 1, 2));
		FowlEntity out = adult(helper, FowlBreed.PLYMOUTH_ROCK, Sex.FEMALE, 101L, new BlockPos(9, 1, 3));
		inside.setNoAi(true);
		out.setNoAi(true);
		inside.assignCoop(c, null);
		out.assignCoop(c, null);
		double before = feeder.kg();
		coop.tend(level, java.util.List.of(inside), 1.0);
		double tookInside = before - feeder.kg();
		before = feeder.kg();
		coop.tend(level, java.util.List.of(out), 1.0);
		double tookOut = before - feeder.kg();
		helper.assertTrue(Math.abs(tookOut - 0.8 * tookInside) < 1e-6, Component.literal("on the run she takes 80% from the feeder: " + tookOut + " vs " + tookInside));
		helper.assertTrue(coop.runDrawnKg() > 0, Component.literal("the run is drawn down a little: " + coop.runDrawnKg()));
		int grassBefore = coop.run().grass();
		coop.tend(level, java.util.List.of(out), 400.0);     // 400 days of one hen: about 2.5 kg past regrowth
		int dirt = 0;
		for (var s : coop.run().spots()) {
			if (level.getBlockState(new BlockPos(s.x(), s.y() - 1, s.z())).is(net.minecraft.world.level.block.Blocks.DIRT)) {
				dirt++;
			}
		}
		helper.assertTrue(dirt >= 1, Component.literal("heavy use bares grass: " + dirt + " of " + grassBefore + ", drawn " + coop.runDrawnKg()));
		helper.succeed();
	}

	/** A blue Muscovy drake reads blue, a double-blue one silver; their genes carry to the egg (poultry.md, "Colour mutants"). */
	static void muscovyColourMutantInherits(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		FowlEntity drake = adult(helper, FowlBreed.MUSCOVY, Sex.MALE, 102L, new BlockPos(2, 1, 2));
		FowlEntity duck = adult(helper, FowlBreed.MUSCOVY, Sex.FEMALE, 103L, new BlockPos(3, 1, 2));
		java.util.EnumMap<dev.farmings.genetics.PoultryLocus, int[]> m = new java.util.EnumMap<>(drake.genes().alleles());
		m.put(dev.farmings.genetics.PoultryLocus.MUSCOVY_BLUE, new int[] {1, 1});
		drake.setGenes(new dev.farmings.genetics.PoultryGenes(m));
		helper.assertTrue(drake.look().plumage() == dev.farmings.genetics.PoultryLook.Plumage.MUSCOVY_SILVER, Component.literal("Bl/Bl is silver: " + drake.look().describe()));
		java.util.EnumMap<dev.farmings.genetics.PoultryLocus, int[]> d = new java.util.EnumMap<>(duck.genes().alleles());
		d.put(dev.farmings.genetics.PoultryLocus.MUSCOVY_BLUE, new int[] {0, 0});
		duck.setGenes(new dev.farmings.genetics.PoultryGenes(d));
		// every chick of silver x black is Bl/+: blue
		var chick = dev.farmings.genetics.PoultryGenes.inherit(duck.genes(), drake.genes(), Sex.FEMALE, 5L);
		helper.assertTrue(dev.farmings.genetics.PoultryLook.of(dev.farmings.genetics.Species.DUCK, "MUS", chick, Sex.FEMALE).plumage() == dev.farmings.genetics.PoultryLook.Plumage.MUSCOVY_BLUE,
				Component.literal("silver x black gives blue"));
		helper.succeed();
	}

	// ---- geese (docs/animals/poultry.md, "Geese and guinea fowl") ----

	/** The three geese: a white Embden gander and goose, a grey Toulouse, a Chinese with its knob; named gander and goose; saved. */
	static void gooseFounders(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity gander = adult(helper, FowlBreed.EMBDEN, Sex.MALE, 201L, new BlockPos(1, 1, 1));
		FowlEntity embden = adult(helper, FowlBreed.EMBDEN, Sex.FEMALE, 202L, new BlockPos(3, 1, 1));
		FowlEntity toulouse = adult(helper, FowlBreed.TOULOUSE, Sex.FEMALE, 203L, new BlockPos(5, 1, 1));
		FowlEntity chinese = adult(helper, FowlBreed.CHINESE, Sex.FEMALE, 204L, new BlockPos(7, 1, 1));
		helper.assertTrue(gander.species() == Species.GOOSE && gander.readout().contains("gander") && embden.readout().contains("goose"),
				Component.literal("a gander and a goose: " + gander.readout()));
		helper.assertTrue(gander.look().plumage() == PoultryLook.Plumage.GOOSE_WHITE && embden.look().plumage() == PoultryLook.Plumage.GOOSE_WHITE,
				Component.literal("Embden white both sexes (dilution and spotting): " + embden.look().describe()));
		helper.assertTrue(toulouse.look().plumage() == PoultryLook.Plumage.GOOSE_GREY, Component.literal("Toulouse grey: " + toulouse.look().describe()));
		var chineseLook = chinese.look().plumage();
		helper.assertTrue(chineseLook == PoultryLook.Plumage.CHINESE_WHITE || chineseLook == PoultryLook.Plumage.CHINESE_BROWN, Component.literal("Chinese: " + chineseLook));
		var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		toulouse.saveWithoutId(out);
		FowlEntity b = FarmingsEntities.GOOSE.create(level, EntitySpawnReason.LOAD);
		b.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		helper.assertTrue(b.breed() == FowlBreed.TOULOUSE && b.genome().equals(toulouse.genome()) && b.genes().equals(toulouse.genes()), Component.literal("a goose survives a save"));
		helper.succeed();
	}

	/** A goose lays in her season (15 Feb - 15 Jun) and not out of it; her eggs are goose eggs of the breed weight. */
	static void gooseLaysInSeason(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity goose = adult(helper, FowlBreed.EMBDEN, Sex.FEMALE, 211L, new BlockPos(3, 1, 3));
		goose.setNoAi(true);
		goose.setDayOfYearForTest(273);
		helper.assertTrue(!goose.laying() && goose.readout().contains("OUT OF SEASON"), Component.literal("1 October: out of season: " + goose.readout()));
		goose.setDayOfYearForTest(100);
		helper.assertTrue(goose.laying(), Component.literal("10 April: she lays: " + goose.readout()));
		double rate = dev.farmings.husbandry.poultry.LayingCycle.eggsPerDay(Species.GOOSE, goose.composition(), goose.ageRealDays(), 500, 100);
		helper.assertTrue(Math.abs(rate - 40.0 / 121) < 1e-9, Component.literal("40 eggs over a 121-day season: " + rate));
		ItemStack egg = goose.makeEgg(level.getGameTime());
		helper.assertTrue(egg.is(dev.farmings.FarmingsItems.GOOSE_EGG), Component.literal("a goose egg"));
		helper.succeed();
	}

	/** On a grassy run a goose takes two-thirds of her feed from the grass; she draws the run down far faster than a hen. */
	static void gooseGrazesRun(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		ServerLevel level = helper.getLevel();
		fillCoop(helper);
		var coop = coop(helper, c);
		coop.refresh(level);
		var feeder = (dev.farmings.coop.GrainFeederEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 4)));
		FowlEntity inside = adult(helper, FowlBreed.TOULOUSE, Sex.FEMALE, 221L, new BlockPos(3, 1, 2));
		FowlEntity out = adult(helper, FowlBreed.TOULOUSE, Sex.FEMALE, 221L, new BlockPos(9, 1, 3));
		inside.setNoAi(true);
		out.setNoAi(true);
		inside.assignCoop(c, null);
		out.assignCoop(c, null);
		double before = feeder.kg();
		coop.tend(level, java.util.List.of(inside), 1.0);
		double tookInside = before - feeder.kg();
		before = feeder.kg();
		coop.tend(level, java.util.List.of(out), 1.0);
		double tookOut = before - feeder.kg();
		helper.assertTrue(Math.abs(tookOut - tookInside / 3) < 1e-6, Component.literal("on the run she takes a third from the feeder: " + tookOut + " vs " + tookInside));
		coop.tend(level, java.util.List.of(out), 40.0);
		int dirt = 0;
		for (var sp : coop.run().spots()) {
			if (level.getBlockState(new BlockPos(sp.x(), sp.y() - 1, sp.z())).is(net.minecraft.world.level.block.Blocks.DIRT)) {
				dirt++;
			}
		}
		helper.assertTrue(dirt >= 2, Component.literal("40 days of one goose bares grass: " + dirt));
		helper.succeed();
	}

	/** A goose gives goose meat, feathers and down. */
	static void gooseDropsDown(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity g = adult(helper, FowlBreed.EMBDEN, Sex.MALE, 231L, new BlockPos(3, 1, 3));
		int want = dev.farmings.husbandry.poultry.Fowl.meatItems(g.composition(), g.weightKg());
		g.kill(level);
		helper.runAfterDelay(5, () -> {
			var items = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(12));
			int meat = items.stream().filter(i -> i.getItem().is(dev.farmings.FarmingsItems.GOOSE_MEAT)).mapToInt(i -> i.getItem().getCount()).sum();
			boolean down = items.stream().anyMatch(i -> i.getItem().is(dev.farmings.FarmingsItems.DOWN));
			helper.assertTrue(meat == want && meat >= 16 && down, Component.literal("goose meat " + meat + " of " + want + ", down " + down));
			helper.succeed();
		});
	}

	/** A tame goose of a coop charges a zombie that comes near and bites it. */
	static void guardGooseChasesZombie(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		ServerLevel level = helper.getLevel();
		coop(helper, c).refresh(level);
		FowlEntity goose = adult(helper, FowlBreed.TOULOUSE, Sex.MALE, 241L, new BlockPos(8, 1, 2));
		goose.assignCoop(c, null);
		var zombie = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(10, 1, 3));
		zombie.setNoAi(true);
		TamingGameTests.waitFor(helper, 0, 200, () -> goose.bites() > 0, () -> "the guard goose should bite the zombie: at " + goose.distanceTo(zombie), helper::succeed);
	}

	/** Goose eggs hatch in a powered incubator after 30 days. */
	static void gooseEggHatches(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(3, 1, 3), dev.farmings.hatchery.HatcheryBlocks.INCUBATOR);
		var inc = (dev.farmings.hatchery.IncubatorEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 3)));
		inc.forceRunning(true);
		for (ItemStack e : fertileEggs(helper, FowlBreed.CHINESE, 2, 251L)) {
			helper.assertTrue(inc.add(e), Component.literal("a fertile goose egg goes in"));
		}
		inc.warm(level, dev.farmings.husbandry.TimeScale.FOWL.realDaysToTicks(29.9));
		helper.assertTrue(inc.hatched() == 0, Component.literal("nothing at 29.9 days: " + inc.readout()));
		inc.warm(level, dev.farmings.husbandry.TimeScale.FOWL.realDaysToTicks(0.2));
		var goslings = level.getEntitiesOfClass(FowlEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(3, 1, 3))).inflate(2),
				f -> f.species() == Species.GOOSE && f.young());
		helper.assertTrue(inc.hatched() == 2 && goslings.size() == 2 && goslings.get(0).readout().contains("gosling"), Component.literal("two goslings at 30 days: " + inc.readout()));
		helper.succeed();
	}

	// ---- guinea fowl ----

	/** Pearl, lavender and white guinea fowl: lavender and white are recessive and breed true; a guinea survives a save. */
	static void guineaColoursInherit(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		FowlEntity hen = adult(helper, FowlBreed.HELMETED, Sex.FEMALE, 301L, new BlockPos(2, 1, 2));
		FowlEntity cock = adult(helper, FowlBreed.HELMETED, Sex.MALE, 302L, new BlockPos(4, 1, 2));
		hen.setNoAi(true);
		cock.setNoAi(true);
		java.util.EnumMap<PoultryLocus, int[]> lav = new java.util.EnumMap<>(hen.genes().alleles());
		lav.put(PoultryLocus.GUINEA_LAVENDER, new int[] {1, 1});
		lav.put(PoultryLocus.GUINEA_WHITE, new int[] {0, 0});
		hen.setGenes(new dev.farmings.genetics.PoultryGenes(lav));
		cock.setGenes(new dev.farmings.genetics.PoultryGenes(lav));
		helper.assertTrue(hen.look().plumage() == PoultryLook.Plumage.LAVENDER && hen.readout().contains("hen"), Component.literal("a lavender hen: " + hen.readout()));
		hen.setDayOfYearForTest(150);
		hen.mate(cock);
		ItemStack egg = hen.makeEgg(level.getGameTime());
		helper.assertTrue(egg.is(dev.farmings.FarmingsItems.GUINEA_EGG), Component.literal("a guinea egg"));
		var chick = FowlEggs.hatch(level, egg, helper.absolutePos(new BlockPos(6, 1, 2)), null);
		helper.assertTrue(chick != null && chick.look().plumage() == PoultryLook.Plumage.LAVENDER && chick.readout().contains("keet"),
				Component.literal("lavender x lavender breeds true: " + (chick == null ? "none" : chick.readout())));
		java.util.EnumMap<PoultryLocus, int[]> wh = new java.util.EnumMap<>(lav);
		wh.put(PoultryLocus.GUINEA_WHITE, new int[] {1, 1});
		cock.setGenes(new dev.farmings.genetics.PoultryGenes(wh));
		helper.assertTrue(cock.look().plumage() == PoultryLook.Plumage.GUINEA_WHITE, Component.literal("white shows over lavender"));
		wh.put(PoultryLocus.GUINEA_WHITE, new int[] {1, 0});
		cock.setGenes(new dev.farmings.genetics.PoultryGenes(wh));
		helper.assertTrue(cock.look().plumage() == PoultryLook.Plumage.LAVENDER, Component.literal("one white is hidden"));
		var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		hen.saveWithoutId(out);
		FowlEntity b = FarmingsEntities.GUINEA.create(level, EntitySpawnReason.LOAD);
		b.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		helper.assertTrue(b.species() == Species.GUINEA && b.genes().equals(hen.genes()), Component.literal("a guinea fowl survives a save"));
		helper.succeed();
	}

	/** Guinea hens lay 1 April - 31 October, about 100 eggs over the season. */
	static void guineaLaysInSeason(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		FowlEntity hen = adult(helper, FowlBreed.HELMETED, Sex.FEMALE, 311L, new BlockPos(3, 1, 3));
		hen.setNoAi(true);
		hen.setDayOfYearForTest(20);
		helper.assertTrue(!hen.laying() && hen.readout().contains("OUT OF SEASON"), Component.literal("21 January: out of season: " + hen.readout()));
		hen.setDayOfYearForTest(180);
		helper.assertTrue(hen.laying(), Component.literal("30 June: she lays: " + hen.readout()));
		double rate = dev.farmings.husbandry.poultry.LayingCycle.eggsPerDay(Species.GUINEA, hen.composition(), hen.ageRealDays(), 500, 180);
		helper.assertTrue(Math.abs(rate - 100.0 / 214) < 1e-9, Component.literal("100 eggs over 214 days: " + rate));
		helper.succeed();
	}

	/** A guinea fowl of a coop sees a zombie and calls: the coop's birds come in from the run and the readout names the alarm. */
	static void guineaAlarmOnZombie(GameTestHelper helper) {
		BlockPos c = buildCoop(helper);
		ServerLevel level = helper.getLevel();
		fillCoop(helper);
		var coop = coop(helper, c);
		coop.refresh(level);
		FowlEntity guinea = adult(helper, FowlBreed.HELMETED, Sex.FEMALE, 321L, new BlockPos(8, 1, 2));
		guinea.assignCoop(c, null);
		var zombie = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(12, 1, 4));
		zombie.setNoAi(true);
		TamingGameTests.waitFor(helper, 0, 200, () -> guinea.alarms() > 0, () -> "the guinea should call: " + guinea.readout(), () -> {
			helper.assertTrue(coop.alarmed(level.getGameTime()) && coop.readout().contains("ALARM: Zombie"), Component.literal(coop.readout()));
			helper.succeed();
		});
	}
}
