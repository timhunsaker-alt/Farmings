package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.HorseEntity;
import dev.farmings.genetics.HorseGenes;
import dev.farmings.genetics.HorseLocus;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.Equine;
import dev.farmings.husbandry.Gallop;
import dev.farmings.husbandry.HorseBreed;
import dev.farmings.husbandry.HorseStage;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/** Headless scenes for horses (docs/animals/horses.md). Registered from {@link FarmingsGameTests}. */
final class HorseGameTests {
	private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

	private HorseGameTests() {
	}

	static HorseEntity horse(GameTestHelper helper, HorseBreed breed, Sex sex, long seed, double ageDays, BlockPos at) {
		HorseEntity h = helper.spawn(FarmingsEntities.HORSE, at);
		h.initFounder(breed, sex, seed, ageDays, helper.getLevel().getGameTime());
		h.setWild(false);
		return h;
	}

	static HorseEntity adult(GameTestHelper helper, HorseBreed breed, Sex sex, long seed, BlockPos at) {
		return horse(helper, breed, sex, seed, 5 * 365, at);
	}

	/** Founders carry a horse genome with SPEED and STAMINA; breed, genes, fitness and pregnancy survive a save. */
	static void horseSpawnsAndSurvivesSave(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		HorseEntity plain = helper.spawn(FarmingsEntities.HORSE, new BlockPos(1, 1, 1), EntitySpawnReason.COMMAND);
		helper.assertTrue(plain.genome() != null && plain.genome().species() == Species.HORSE && plain.ageYears() >= 3,
				Component.literal("a spawned horse is an adult with a horse genome"));
		HorseEntity mare = adult(helper, HorseBreed.ARABIAN, Sex.FEMALE, 1L, new BlockPos(3, 1, 3));
		HorseEntity stallion = adult(helper, HorseBreed.THOROUGHBRED, Sex.MALE, 2L, new BlockPos(6, 1, 3));
		helper.assertTrue(mare.weightKg() == 408 && stallion.weightKg() == 522, Component.literal("mature weights 408 and 522 kg"));
		mare.setFitnessForTest(0.6);
		mare.startHeatForDisplay();
		mare.sureConceptionForTest();
		helper.assertTrue(mare.conceive(stallion.genome(), stallion.genes(), level.getGameTime()), Component.literal("she conceives"));
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		mare.saveWithoutId(out);
		HorseEntity b = FarmingsEntities.HORSE.create(level, EntitySpawnReason.LOAD);
		b.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		helper.assertTrue(b.breed() == HorseBreed.ARABIAN && b.genome().equals(mare.genome()) && b.genes().equals(mare.genes()) && b.pregnant()
				&& Math.abs(b.fitness() - 0.6) < 1e-9 && b.isTamed() && !b.isWild(), Component.literal("breed, genome, genes, fitness, pregnancy survive"));
		for (Trait t : Species.HORSE.traits()) {
			helper.assertTrue(b.score(t) == mare.score(t), Component.literal("score " + t.id + " survives"));
		}
		helper.succeed();
	}

	/** A foal, a filly at weaning; shears make a colt a gelding; a 3-year male is a stallion; under 2 years nobody rides. */
	static void horseLifecycle(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		HorseEntity foal = horse(helper, HorseBreed.QUARTER_HORSE, Sex.FEMALE, 11L, 30, new BlockPos(2, 1, 2));
		helper.assertTrue(foal.stage() == HorseStage.FOAL && foal.isBaby(), Component.literal("a 30-day horse is a foal"));
		foal.advanceForTest(150);
		foal.settle(helper.getLevel());
		helper.assertTrue(foal.stage() == HorseStage.FILLY && !foal.isBaby(), Component.literal("weaned she is a filly: " + foal.stage()));
		var player = helper.makeMockServerPlayerInLevel();
		foal.mobInteract(player, HAND);
		helper.assertTrue(!foal.isVehicle(), Component.literal("a yearling can't be ridden"));
		HorseEntity colt = horse(helper, HorseBreed.QUARTER_HORSE, Sex.MALE, 12L, 400, new BlockPos(5, 1, 2));
		helper.assertTrue(colt.stage() == HorseStage.COLT, Component.literal("a colt"));
		player.setItemInHand(HAND, new ItemStack(Items.SHEARS));
		colt.mobInteract(player, HAND);
		colt.advanceForTest(3 * 365);
		helper.assertTrue(colt.stage() == HorseStage.GELDING, Component.literal("he grows as a gelding: " + colt.stage()));
		helper.assertTrue(adult(helper, HorseBreed.ARABIAN, Sex.MALE, 13L, new BlockPos(5, 1, 5)).stage() == HorseStage.STALLION,
				Component.literal("a grown male is a stallion"));
		player.discard();
		helper.succeed();
	}

	/** In season a stallion serves a mare in heat and she foals one foal of a tenth her weight; out of season she doesn't cycle. */
	static void mareFoals(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		HorseEntity mare = adult(helper, HorseBreed.THOROUGHBRED, Sex.FEMALE, 21L, new BlockPos(4, 1, 4));
		HorseEntity stallion = adult(helper, HorseBreed.ARABIAN, Sex.MALE, 22L, new BlockPos(5, 1, 4));
		mare.setNoAi(true);
		stallion.setNoAi(true);
		mare.forceDayOfYearForTest(0);     // 1 January
		mare.settle(level);
		helper.assertTrue(!mare.inHeat() && !mare.pregnant(), Component.literal("no heat in January"));
		mare.forceDayOfYearForTest(120);     // 1 May
		mare.sureConceptionForTest();
		mare.settle(level);
		mare.startHeatForDisplay();
		mare.settle(level);
		helper.assertTrue(mare.pregnant() && Math.abs(mare.daysToFoaling() - 340) < 0.1, Component.literal("served, due in 340 days: " + mare.readout()));
		mare.makeDueNow();
		mare.settle(level);
		List<HorseEntity> foals = level.getEntitiesOfClass(HorseEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12)).stream()
				.filter(h -> h != mare && h != stallion).toList();
		helper.assertTrue(foals.size() == 1, Component.literal("one foal, got " + foals.size()));
		HorseEntity f = foals.get(0);
		helper.assertTrue(f.stage() == HorseStage.FOAL && Math.abs(f.weightKg() - 52.2) < 1e-6, Component.literal("a foal of 52.2 kg: " + f.weightKg()));
		helper.assertTrue(f.composition().equals(java.util.Map.of("TB", 5000, "ARA", 5000)) && f.damId().equals(mare.herdId()),
				Component.literal("a half-bred foal out of her"));
		helper.assertTrue(mare.nursing() && mare.stage() == HorseStage.MARE, Component.literal("a mare with foal at foot"));
		helper.succeed();
	}

	/** Plains spawn Farmings horses; a wild group is one breed with one stallion; a wild horse throws you, a tame one takes a saddle. */
	static void wildHorseReplacesVanilla(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		var plains = TamingGameTests.creatures(level, net.minecraft.world.level.biome.Biomes.PLAINS);
		var types = plains.stream().map(w -> w.value().type()).toList();
		helper.assertTrue(!types.contains(net.minecraft.world.entity.EntityTypes.HORSE) && types.contains(FarmingsEntities.HORSE),
				Component.literal("plains have Farmings horses: " + types));
		HorseEntity a = FarmingsEntities.HORSE.create(level, EntitySpawnReason.NATURAL);
		BlockPos p = helper.absolutePos(new BlockPos(2, 1, 2));
		a.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
		var data = a.finalizeSpawn(level, level.getCurrentDifficultyAt(a.blockPosition()), EntitySpawnReason.NATURAL, null);
		HorseEntity b = FarmingsEntities.HORSE.create(level, EntitySpawnReason.NATURAL);
		b.snapTo(a.getX() + 2, a.getY(), a.getZ(), 0, 0);
		b.finalizeSpawn(level, level.getCurrentDifficultyAt(b.blockPosition()), EntitySpawnReason.NATURAL, data);
		level.addFreshEntity(a);
		level.addFreshEntity(b);
		helper.assertTrue(a.isWild() && b.isWild() && a.breed() == b.breed() && a.sex() == Sex.MALE && b.sex() == Sex.FEMALE
				&& a.breed() != HorseBreed.ARABIAN, Component.literal("a wild plains group: one breed, one stallion"));
		var player = helper.makeMockServerPlayerInLevel();
		a.mobInteract(player, HAND);
		helper.assertTrue(!a.isVehicle() && !a.isTamed(), Component.literal("the wild horse won't be ridden"));
		ItemStack saddle = new ItemStack(Items.SADDLE);
		player.setItemInHand(HAND, saddle);
		a.mobInteract(player, HAND);
		helper.assertTrue(a.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), Component.literal("nor saddled"));
		a.tame(player.getUUID());
		helper.assertTrue(a.isTamed(), Component.literal("tamed in the corral, it is a herd horse"));
		player.setItemInHand(HAND, new ItemStack(Items.SADDLE));
		a.mobInteract(player, HAND);
		helper.assertTrue(!a.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), Component.literal("it takes a saddle"));
		player.setItemInHand(HAND, ItemStack.EMPTY);
		a.mobInteract(player, HAND);
		helper.assertTrue(player.getVehicle() == a, Component.literal("and is ridden"));
		player.stopRiding();
		player.discard();
		helper.succeed();
	}

	/**
	 * Galloping counts: a ridden horse moved at 50 m/s (well over half its pace) for 1 km builds 0.02 fitness and syncs its ridden speed
	 * from the distance galloped; 30 s at a walk resets that distance.
	 */
	static void horseGallopBuildsFitness(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		HorseEntity h = adult(helper, HorseBreed.THOROUGHBRED, Sex.FEMALE, 31L, new BlockPos(3, 1, 3));
		h.setNoAi(true);
		h.setNoGravity(true);
		var player = helper.makeMockServerPlayerInLevel();
		player.startRiding(h);
		double x0 = h.getX();
		int[] tick = {0};
		double[] paceSeen = {0};
		helper.succeedWhen(() -> {
			int t = tick[0]++;
			if (t < 420) {
				h.setPos(x0 + (t % 2 == 0 ? 2.5 : 0), h.getY(), h.getZ());
				if (t == 410) {
					paceSeen[0] = h.syncedPace();
				}
			}
			helper.assertTrue(t >= 420 + 620, Component.literal("still running"));
			Gallop.Runner r = h.runner();
			helper.assertTrue(Math.abs(h.fitness() - 0.02) < 0.002, Component.literal("1 km builds 0.02 fitness: " + h.fitness()));
			double expect = new Gallop.Runner(h.composition(), h.genes().count(HorseLocus.MSTN), h.score(Trait.SPEED), h.score(Trait.STAMINA),
					h.ageYears(), 0.02).speedAt(1000);
			helper.assertTrue(paceSeen[0] > 0 && Math.abs(paceSeen[0] - expect) / expect < 0.02 && paceSeen[0] < r.pace(402) * 1.2,
					Component.literal("ridden pace after 1 km " + paceSeen[0] + " vs " + expect));
			helper.assertTrue(h.gallopedM() == 0, Component.literal("30 s at rest resets the distance: " + h.gallopedM()));
			player.stopRiding();
			player.discard();
		});
	}

	/** A stallion's straw keeps (as a bull's) and gets a mare in heat in foal. */
	static void stallionStrawInseminatesMare(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		HorseEntity stallion = adult(helper, HorseBreed.QUARTER_HORSE, Sex.MALE, 41L, new BlockPos(2, 1, 2));
		HorseEntity mare = adult(helper, HorseBreed.QUARTER_HORSE, Sex.FEMALE, 42L, new BlockPos(5, 1, 2));
		stallion.setNoAi(true);
		mare.setNoAi(true);
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack empty = new ItemStack(FarmingsItems.EMPTY_STRAW, 1);
		helper.assertTrue(empty.getItem().interactLivingEntity(empty, player, stallion, HAND).consumesAction(), Component.literal("the stallion fills a straw"));
		ItemStack straw = player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(FarmingsItems.SEMEN_STRAW)).findFirst().orElseThrow();
		helper.assertTrue(!dev.farmings.item.StrawItem.stale(straw, helper.getLevel().getGameTime() + 10_000_000L), Component.literal("it keeps"));
		mare.forceDayOfYearForTest(150);
		mare.startHeatForDisplay();
		mare.sureConceptionForTest();
		helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, mare, HAND).consumesAction() && mare.pregnant(), Component.literal("the mare takes it"));
		helper.succeed();
	}

	/** The MSTN gene through the entity: a C/C horse wins the quarter mile and a T/T horse the mile and a half, all else equal. */
	static void sprinterAndStayer(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		HorseEntity cc = adult(helper, HorseBreed.THOROUGHBRED, Sex.MALE, 51L, new BlockPos(2, 1, 2));
		HorseEntity tt = adult(helper, HorseBreed.THOROUGHBRED, Sex.MALE, 51L, new BlockPos(5, 1, 2));
		HorseGenes g = cc.genes();
		cc.setGenesForTest(g.with(HorseLocus.MSTN, 1, 1));
		tt.setGenesForTest(g.with(HorseLocus.MSTN, 0, 0));
		for (Trait t : Species.HORSE.traits()) {
			tt.setScoreForTest(t, cc.score(t));
		}
		helper.assertTrue(cc.runner().time(402) < tt.runner().time(402) && tt.runner().time(2414) < cc.runner().time(2414),
				Component.literal(String.format("402 m: %.2f vs %.2f s; 2,414 m: %.2f vs %.2f s", cc.runner().time(402), tt.runner().time(402),
						cc.runner().time(2414), tt.runner().time(2414))));
		helper.assertTrue(cc.readout().contains("MSTN C/C") && tt.readout().contains("MSTN T/T"), Component.literal(cc.readout()));
		helper.assertTrue(Equine.dryMatterKg(cc.weightKg()) == cc.dailyDryMatterKg(), Component.literal("2.25% of body weight in dry matter"));
		helper.succeed();
	}
}
