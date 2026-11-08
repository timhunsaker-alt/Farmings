package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LactationTest {
	private static double total(CattleBreed b) {
		double s = 0;
		for (int t = 1; t <= Lactation.DAYS; t++) {
			s += Lactation.dailyKg(b, t, 1.0);
		}
		return s;
	}

	@Test
	void holsteinTotalMatchesUsda() {
		assertEquals(27686 * 0.45359237, total(CattleBreed.HOLSTEIN), 0.01);
	}

	@Test
	void jerseyAndBrownSwissTotalsMatchUsda() {
		assertEquals(20740 * 0.45359237, total(CattleBreed.JERSEY), 0.01);
		assertEquals(22737 * 0.45359237, total(CattleBreed.BROWN_SWISS), 0.01);
	}

	@Test
	void peaksAtTheShapeDay() {
		assertEquals(69, Lactation.of(CattleBreed.HOLSTEIN).peakDay(), 0.5);
		assertEquals(60, Lactation.of(CattleBreed.JERSEY).peakDay(), 0.5);
		assertTrue(Lactation.dailyKg(CattleBreed.HOLSTEIN, 69, 1) > Lactation.dailyKg(CattleBreed.HOLSTEIN, 10, 1));
		assertTrue(Lactation.dailyKg(CattleBreed.HOLSTEIN, 69, 1) > Lactation.dailyKg(CattleBreed.HOLSTEIN, 300, 1));
	}

	@Test
	void beefBreedsAndDaysOutsideTheLactationGiveNothing() {
		assertEquals(0, Lactation.dailyKg(CattleBreed.ANGUS, 60, 1));
		assertEquals(0, Lactation.dailyKg(CattleBreed.WAGYU, 60, 1));
		assertEquals(0, Lactation.dailyKg(CattleBreed.HOLSTEIN, 0, 1));
		assertEquals(0, Lactation.dailyKg(CattleBreed.HOLSTEIN, 306, 1));
	}

	@Test
	void standInsAreFlagged() {
		assertFalse(Lactation.of(CattleBreed.HOLSTEIN).standIn());
		assertTrue(Lactation.of(CattleBreed.BROWN_SWISS).standIn());
		assertTrue(Lactation.of(CattleBreed.GYR).standIn());
	}

	@Test
	void yieldFactorScalesTheDay() {
		assertEquals(1.1 * Lactation.dailyKg(CattleBreed.JERSEY, 100, 1), Lactation.dailyKg(CattleBreed.JERSEY, 100, 1.1), 1e-9);
	}

	@Test
	void factorFollowsTheGenomeScoreAndIsClamped() {
		// Holstein baseline 95 -> 950.
		assertEquals(1.0, Lactation.yieldFactor(CattleBreed.HOLSTEIN, 950), 1e-9);
		assertEquals(1.1, Lactation.yieldFactor(CattleBreed.HOLSTEIN, 1045), 1e-9);
		assertEquals(Lactation.MAX_FACTOR, Lactation.yieldFactor(CattleBreed.HOLSTEIN, 1000 * 3), 1e-9);
		assertEquals(Lactation.MIN_FACTOR, Lactation.yieldFactor(CattleBreed.HOLSTEIN, 10), 1e-9);
		assertEquals(1.0, Lactation.yieldFactor(CattleBreed.ANGUS, 500), 1e-9);
	}

	@Test
	void shortfallCutsMilkInStep() {
		double full = Lactation.producedKg(CattleBreed.HOLSTEIN, 69, 950, 1, 1);
		assertEquals(full / 2, Lactation.producedKg(CattleBreed.HOLSTEIN, 69, 950, 0.5, 1), 1e-9);
		assertEquals(full / 4, Lactation.producedKg(CattleBreed.HOLSTEIN, 69, 950, 1, 0.25), 1e-9);
		assertEquals(0, Lactation.producedKg(CattleBreed.HOLSTEIN, 69, 950, 0, 1), 1e-9);
	}

	@Test
	void daysInMilkCountsTheCalvingDayAsOne() {
		assertEquals(1, Lactation.daysInMilk(0));
		assertEquals(1, Lactation.daysInMilk(0.99));
		assertEquals(70, Lactation.daysInMilk(69.2));
	}
}
