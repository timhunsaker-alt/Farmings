package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The pig rules against docs/animals/pigs.md. */
class SwineTest {
	private static Map<String, Integer> pure(PigBreed b) {
		return Map.of(b.code, 10_000);
	}

	@Test
	void growthHitsTheSourcedPoints() {
		Map<String, Integer> c = pure(PigBreed.LARGE_WHITE);
		assertEquals(1.5, Swine.targetKg(c, false, 0), 1e-9);
		assertEquals(5.7, Swine.targetKg(c, false, 21), 1e-9);
		assertEquals(120, Swine.targetKg(c, false, 186), 1e-9);
		assertEquals(0.8, Swine.targetKg(c, false, 126) - Swine.targetKg(c, false, 125), 1e-9, "finishing ADG 0.80");
		assertEquals(280, Swine.targetKg(c, false, 800), 1e-9);
	}

	@Test
	void firstBreedingAt210DaysAnd136Kg() {
		assertFalse(Swine.canFirstBreed(200, 150));
		assertFalse(Swine.canFirstBreed(220, 130));
		assertTrue(Swine.canFirstBreed(210, 136));
	}

	@Test
	void littersByBreedAndParity() {
		assertEquals(11.6, Swine.litterMean(pure(PigBreed.LARGE_WHITE), true, 500), 1e-9);
		assertEquals(13.7, Swine.litterMean(pure(PigBreed.LARGE_WHITE), false, 500), 1e-9);
		assertEquals(14, Swine.litter(pure(PigBreed.LARGE_WHITE), false, 500, 0.0));
		assertEquals(1, Swine.litter(pure(PigBreed.BERKSHIRE), false, 500, -10));
		assertEquals(20, Swine.litter(pure(PigBreed.LARGE_WHITE), false, 500, 10));
		assertEquals(0.1509, Swine.stillbornRate(pure(PigBreed.BERKSHIRE)), 1e-9);
	}

	@Test
	void feedWaterManureAndSpace() {
		assertEquals(2.2, Swine.feedKgPerDay(80, false, false), 1e-9);
		assertEquals(6.5, Swine.feedKgPerDay(280, true, true), 1e-9);
		assertEquals(4.9, Swine.manureKgPerDay(100, false, false, false), 1e-9);
		assertEquals(1.4, Swine.manureKgPerDay(18, false, false, false), 0.05, "the nursery row");
		assertEquals(10.6, Swine.manureKgPerDay(280, true, false, true), 1e-9);
		assertEquals(0.5, Swine.penBlocks(30, false), 1e-9);
		assertEquals(2, Swine.penBlocks(280, true), 1e-9);
	}

	@Test
	void aMarketPigsCuts() {
		Swine.Cuts c = Swine.cuts(120);
		// carcass 84 kg: hams 15.8%, chops 14.6% at 0.5 kg, bellies 12.6% at 2 kg, shoulders 24.4% at 2 kg, fat 15% at 1 kg
		assertEquals(2, c.hams());
		assertEquals(84 * 0.158 / 2, c.hamKg(), 1e-9);
		assertEquals(24, c.chops());
		assertEquals(5, c.bellies());
		assertEquals(10, c.shoulders());
		assertEquals(12, c.fat());
	}
}
