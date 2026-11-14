package dev.farmings.feed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.husbandry.Stage;
import org.junit.jupiter.api.Test;

class IntakeTest {
	@Test
	void aFullDayOfFeedUsesTheDailyIntakeAndTheUcDavisWater() {
		Intake.Result r = Intake.step(20, 0, Intake.TICKS_PER_DAY, 1000, 1000);
		assertEquals(20, r.eatenKg(), 1e-9);
		assertEquals(120, r.drunkL(), 1e-9);              // 6 L per kg dry matter
		assertEquals(1.0, r.fed(), 1e-9);
		assertEquals(1.0, r.watered(), 1e-9);
	}

	@Test
	void milkAddsOneLitreOfWaterPerLitre() {
		Intake.Result r = Intake.step(20, 30, Intake.TICKS_PER_DAY, 1000, 1000);
		assertEquals(150, r.drunkL(), 1e-9);
	}

	@Test
	void manureFollowsWhatWasEaten() {
		Intake.Result r = Intake.step(20, 0, Intake.TICKS_PER_DAY, 1000, 1000);
		assertEquals(44.6, r.manureKg(), 1e-9, "the Florida figure: 44.6 kg from 20 kg of dry matter");
		Intake.Result half = Intake.step(20, 0, Intake.TICKS_PER_DAY, 10, 1000);
		assertEquals(0.5, half.fed(), 1e-9);
		assertEquals(22.3, half.manureKg(), 1e-9);
	}

	@Test
	void manureFollowsTheD384TableByStage() {
		assertEquals(67, Intake.manureKgPerDay(Stage.LACTATING_COW).getAsDouble(), 1e-9, "D384.2: 67 kg at 75 lb of milk");
		assertEquals(39, Intake.manureKgPerDay(Stage.DRY_COW).getAsDouble(), 1e-9);
		assertEquals(24, Intake.manureKgPerDay(Stage.HEIFER).getAsDouble(), 1e-9);
		assertTrue(Intake.manureKgPerDay(Stage.BULL).isEmpty());
		Intake.Result r = Intake.step(20, 0, Intake.TICKS_PER_DAY, 1000, 1000, 67);
		assertEquals(67, r.manureKg(), 1e-9);
		Intake.Result half = Intake.step(20, 0, Intake.TICKS_PER_DAY, 10, 1000, 67);
		assertEquals(33.5, half.manureKg(), 1e-9, "half fed, half the manure");
	}

	@Test
	void aShortfallIsReportedAndNeverFatal() {
		Intake.Result r = Intake.step(20, 0, Intake.TICKS_PER_DAY, 0, 0);
		assertEquals(0, r.eatenKg());
		assertEquals(0, r.fed());
		assertEquals(0, r.watered());
		assertEquals(0, r.manureKg());
	}

	@Test
	void aShortStepTakesAShareOfTheDay() {
		Intake.Result r = Intake.step(24, 0, 20, 1000, 1000);       // 20 ticks of a 24 000 tick day
		assertEquals(24 * 20.0 / 24000, r.eatenKg(), 1e-9);
		assertTrue(r.drunkL() > 0);
	}

	@Test
	void aCalfWantsNothingAndIsNeverShort() {
		Intake.Result r = Intake.step(0, 0, 20, 0, 0);
		assertEquals(1.0, r.fed());
		assertEquals(1.0, r.watered());
	}

	@Test
	void everyStageButCalvesHasAnIntake() {
		assertTrue(Hay.dryMatterIntakeOrStandIn(Stage.CALF, 100).isEmpty());
		assertTrue(Hay.dryMatterIntakeOrStandIn(Stage.BULL_CALF, 100).isEmpty());
		assertEquals(0.0325 * 650, Hay.dryMatterIntakeOrStandIn(Stage.LACTATING_COW, 650).getAsDouble(), 1e-9);
		assertEquals(Hay.DRY_INTAKE_FRACTION * 650, Hay.dryMatterIntakeOrStandIn(Stage.DRY_COW, 650).getAsDouble(), 1e-9);
		assertEquals(Hay.DRY_INTAKE_FRACTION * 800, Hay.dryMatterIntakeOrStandIn(Stage.BULL, 800).getAsDouble(), 1e-9, "stand-in: the dry cow's rate");
		assertEquals(Hay.DRY_INTAKE_FRACTION * 400, Hay.dryMatterIntakeOrStandIn(Stage.HEIFER, 400).getAsDouble(), 1e-9);
	}
}
