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
		assertEquals(0.027 * 650, Hay.dryMatterIntakeOrStandIn(Stage.LACTATING_COW, 650).getAsDouble(), 1e-9);
		assertEquals(0.022 * 650, Hay.dryMatterIntakeOrStandIn(Stage.DRY_COW, 650).getAsDouble(), 1e-9);
		assertEquals(0.022 * 800, Hay.dryMatterIntakeOrStandIn(Stage.BULL, 800).getAsDouble(), 1e-9, "stand-in: the dry cow's rate");
		assertEquals(0.022 * 400, Hay.dryMatterIntakeOrStandIn(Stage.HEIFER, 400).getAsDouble(), 1e-9);
	}
}
