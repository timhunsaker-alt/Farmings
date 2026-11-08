package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.genetics.Sex;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class LifecycleTest {
	@Test
	void timeScaleIsFifteenToOne() {
		assertEquals(24_000, TimeScale.TICKS_PER_GAME_DAY);
		assertEquals(18.87, TimeScale.realDaysToGameDays(283), 0.01);
		assertEquals(20.33, TimeScale.realDaysToGameDays(Lifecycle.LACTATION_DAYS), 0.01);
		assertEquals(4.0, TimeScale.realDaysToGameDays(Lifecycle.DRY_DAYS), 1e-9);
		assertEquals(1.4, TimeScale.realDaysToGameDays(Lifecycle.HEAT_CYCLE_DAYS), 1e-9);
		assertEquals(24.33, TimeScale.realDaysToGameDays(Lifecycle.CALVING_INTERVAL_DAYS), 0.01);
		assertEquals(30.42, TimeScale.realDaysToGameDays(Lifecycle.FIRST_BREEDING_AGE_DAYS), 0.01);
		assertEquals(24_000, TimeScale.realDaysToTicks(15));
		assertEquals(15.0, TimeScale.ticksToRealDays(24_000), 1e-9);
	}

	@Test
	void breedingWeightsMatchTheDocument() {
		assertEquals(390, CattleBreed.HOLSTEIN.breedingWeightKg(), 1.0);
		assertEquals(260, CattleBreed.JERSEY.breedingWeightKg(), 2.0);
	}

	@Test
	void firstBreedingNeedsFifteenMonthsAndTheWeight() {
		CattleBreed h = CattleBreed.HOLSTEIN;
		double day = Lifecycle.FIRST_BREEDING_AGE_DAYS;
		assertTrue(Lifecycle.canFirstBreed(h, day, 400));
		assertFalse(Lifecycle.canFirstBreed(h, day - 1, 600), "well fed but too young");
		assertFalse(Lifecycle.canFirstBreed(h, day + 100, 300), "old enough but underweight");
	}

	@Test
	void gestationIsPerBreed() {
		assertEquals(283, Lifecycle.dueDay(CattleBreed.HOLSTEIN, 0));
		assertEquals(280, Lifecycle.dueDay(CattleBreed.ANGUS, 0));
		assertEquals(400 + 284, Lifecycle.dueDay(CattleBreed.WAGYU, 400));
	}

	@Test
	void lactationAndDryPeriodAddToTheCalvingInterval() {
		assertEquals(Lifecycle.CALVING_INTERVAL_DAYS, Lifecycle.LACTATION_DAYS + Lifecycle.DRY_DAYS);
		assertEquals(305, Lifecycle.lactationEndDay(0));
	}

	@Test
	void femaleStages() {
		CattleBreed h = CattleBreed.HOLSTEIN;
		OptionalDouble none = OptionalDouble.empty();
		assertEquals(Stage.CALF, Lifecycle.femaleStage(h, 100, false, false, 0, none, false));
		assertEquals(Stage.HEIFER, Lifecycle.femaleStage(h, 400, false, false, 0, none, false));
		assertEquals(Stage.BRED_HEIFER, Lifecycle.femaleStage(h, 450, false, true, 0, OptionalDouble.of(200), false));
		assertEquals(Stage.LACTATING_COW, Lifecycle.femaleStage(h, 650, true, false, 10, none, false));
		assertEquals(Stage.LACTATING_COW, Lifecycle.femaleStage(h, 650, true, true, 100, OptionalDouble.of(150), false));
		assertEquals(Stage.DRY_COW, Lifecycle.femaleStage(h, 650, true, true, 250, OptionalDouble.of(60), false), "dried off 60 days before due");
		assertEquals(Stage.DRY_COW, Lifecycle.femaleStage(h, 650, true, false, 305, none, false));
		assertEquals(Stage.RETIRED_COW, Lifecycle.femaleStage(h, 650, true, false, 10, none, true));
	}

	@Test
	void maleStagesAndWhoMilksOrBreeds() {
		CattleBreed a = CattleBreed.ANGUS;
		assertEquals(Stage.BULL_CALF, Lifecycle.maleStage(a, 100, false));
		assertEquals(Stage.BULL, Lifecycle.maleStage(a, 500, false));
		assertEquals(Stage.STEER, Lifecycle.maleStage(a, 500, true));
		assertTrue(Lifecycle.givesMilk(Stage.LACTATING_COW));
		assertFalse(Lifecycle.givesMilk(Stage.BULL));
		assertFalse(Lifecycle.givesMilk(Stage.DRY_COW));
		assertFalse(Lifecycle.canBreed(Stage.STEER));
		assertTrue(Lifecycle.canBreed(Stage.BULL));
		assertFalse(Lifecycle.canBreed(Stage.CALF));
	}

	@Test
	void firstThreeCalvesFollowTheTutorialRule() {
		assertEquals(Sex.FEMALE, FirstCalves.forcedSex(0));
		assertEquals(Sex.FEMALE, FirstCalves.forcedSex(1));
		assertEquals(Sex.MALE, FirstCalves.forcedSex(2));
		assertNull(FirstCalves.forcedSex(3));
	}

	@Test
	void breedLookup() {
		assertEquals(CattleBreed.GYR, CattleBreed.byCode("GYR"));
		assertEquals(CattleBreed.Use.BEEF, CattleBreed.WAGYU.use);
	}
}
