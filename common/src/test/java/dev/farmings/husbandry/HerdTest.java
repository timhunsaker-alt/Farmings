package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The goat rules against docs/animals/goats.md. */
class HerdTest {
	private static Map<String, Integer> pure(GoatBreed b) {
		return Map.of(b.code, 10_000);
	}

	@Test
	void lactationsSumToTheAdgaTotals() {
		for (GoatBreed b : GoatBreed.values()) {
			double sum = 0;
			for (int d = 1; d <= Herd.LACTATION_DAYS; d++) {
				sum += Herd.milkKg(b, d);
			}
			assertEquals(b.lactationKg, sum, 1e-6, b.name());
		}
		assertEquals(0, Herd.milkKg(GoatBreed.SAANEN, Herd.LACTATION_DAYS + 1));
	}

	@Test
	void milkPeaksAtFortyDays() {
		double peak = 0;
		int at = 0;
		for (int d = 1; d <= Herd.LACTATION_DAYS; d++) {
			if (Herd.milkKg(GoatBreed.SAANEN, d) > peak) {
				peak = Herd.milkKg(GoatBreed.SAANEN, d);
				at = d;
			}
		}
		assertEquals(40, at);
	}

	@Test
	void boerGivesNoParlorMilkAndCrossesGiveTheirShare() {
		assertEquals(0, Herd.milkKg(pure(GoatBreed.BOER), 40, 500, false));
		Map<String, Integer> half = Map.of("SAA", 5_000, "BOE", 5_000);
		assertEquals(Herd.milkKg(GoatBreed.SAANEN, 40) / 2, Herd.milkKg(half, 40, 500, false), 1e-9);
		assertEquals(0.85 * Herd.milkKg(GoatBreed.SAANEN, 40), Herd.milkKg(pure(GoatBreed.SAANEN), 40, 500, true), 1e-9);
		assertEquals(4.6, Herd.fatPct(pure(GoatBreed.NUBIAN)), 1e-9);
		assertEquals(3.2, Herd.fatPct(half), 1e-9);
	}

	@Test
	void litterOddsGiveTheSourcedMeans() {
		assertEquals(1.64, Herd.litter(pure(GoatBreed.BOER), false, 500).mean(), 1e-9);
		assertEquals(1.56, Herd.litter(pure(GoatBreed.SAANEN), false, 500).mean(), 1e-9);
		assertEquals(1.37, Herd.litter(pure(GoatBreed.SAANEN), true, 500).mean(), 1e-9);
		Flock.Litter boer = Herd.litterFor(1.64);
		assertEquals(0.43, boer.single(), 1e-9);
		assertEquals(0.50, boer.twins(), 1e-9);
	}

	@Test
	void firstBreedingAtSevenMonthsAndSixtyPercent() {
		Map<String, Integer> c = pure(GoatBreed.SAANEN);
		assertFalse(Herd.canFirstBreed(c, false, 200, 50));
		assertFalse(Herd.canFirstBreed(c, false, 220, 30));
		assertTrue(Herd.canFirstBreed(c, false, 220, 61 * 0.6));
		assertEquals(61 * 0.6, Herd.targetKg(c, false, Herd.FIRST_BREEDING_AGE_DAYS), 1e-9);
		assertEquals(130, Herd.targetKg(pure(GoatBreed.BOER), true, 800), 1e-9);
		double boerAt90 = Herd.targetKg(pure(GoatBreed.BOER), false, 90);
		assertTrue(boerAt90 > 16 && boerAt90 < 23, "Penn State: 35-50 lb at 90 days, " + boerAt90);
	}

	@Test
	void seasonsByBreed() {
		assertTrue(Herd.inSeason(GoatBreed.SAANEN, 273));      // 1 Oct
		assertFalse(Herd.inSeason(GoatBreed.SAANEN, 150));     // 31 May
		assertTrue(Herd.inSeason(GoatBreed.NUBIAN, 215));      // 4 Aug
		assertFalse(Herd.inSeason(GoatBreed.SAANEN, 215));
		assertTrue(Herd.inSeason(GoatBreed.BOER, 150));
	}

	@Test
	void intakeWaterManureAndMeat() {
		assertEquals(3.05, Herd.intakeKgDm(61, true), 1e-9);
		assertEquals(61 * 0.0275, Herd.intakeKgDm(61, false), 1e-9);
		assertEquals(4.5, Herd.waterLPerKgDm(true), 1e-9);
		assertEquals(60 * 0.013 / 0.36, Herd.manureKgPerDay(60), 1e-9);
		// a 130 kg Boer buck at 3 years: 130 x 0.58 x 0.70 / 2.5 = 21 items
		assertEquals(21, Herd.meatItems(pure(GoatBreed.BOER), 130, 1000));
		assertEquals(0.06 * 1.2, Herd.stillbirthRate(2), 1e-12);
	}
}
