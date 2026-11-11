package dev.farmings.husbandry.poultry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.genetics.Species;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LayingCycleTest {
	static double year(Species s, String code, int score, int fromDay) {
		double sum = 0;
		for (int d = fromDay; d < fromDay + 364; d++) {
			sum += LayingCycle.eggsPerDay(s, Map.of(code, 10_000), d, score);
		}
		return sum;
	}

	@Test
	void aLayingYearGivesTheBreedFigure() {
		assertEquals(235, year(Species.CHICKEN, "LEG", 500, 143), 1.0);
		assertEquals(100, year(Species.CHICKEN, "SLK", 500, 140), 1.0);
		assertEquals(250, year(Species.QUAIL, "COT", 500, 49), 1.0);
		assertEquals(75, year(Species.TURKEY, "BRZ", 500, 220), 1.5);
		assertEquals(35, year(Species.DUCK, "MAL", 500, 182), 1.0);
	}

	@Test
	void theTraitMovesItUpToTwentyPercent() {
		assertEquals(235 * 1.2, year(Species.CHICKEN, "LEG", 1000, 143), 1.5);
		assertEquals(235 * 0.8, year(Species.CHICKEN, "LEG", 0, 143), 1.5);
	}

	@Test
	void noEggsBeforeLayOrAfterRetirement() {
		Map<String, Integer> leg = Map.of("LEG", 10_000);
		assertEquals(0, LayingCycle.eggsPerDay(Species.CHICKEN, leg, 142, 500));
		assertTrue(LayingCycle.eggsPerDay(Species.CHICKEN, leg, 143, 500) > 0);
		assertEquals(0, LayingCycle.eggsPerDay(Species.CHICKEN, leg, 143 + 3 * 365, 500));
		assertTrue(LayingCycle.retired(leg, 143 + 3 * 365));
		for (int d = 0; d < 1000; d++) {
			assertTrue(LayingCycle.eggsPerDay(Species.QUAIL, Map.of("COT", 10_000), d, 1000) <= 1.0);
		}
	}

	@Test
	void turkeysRestAfterTwentyEightWeeks() {
		Map<String, Integer> brz = Map.of("BRZ", 10_000);
		assertTrue(LayingCycle.eggsPerDay(Species.TURKEY, brz, 220.5 + 27 * 7, 500) > 0);
		assertEquals(0, LayingCycle.eggsPerDay(Species.TURKEY, brz, 220.5 + 30 * 7, 500));
		assertTrue(LayingCycle.eggsPerDay(Species.TURKEY, brz, 220.5 + 53 * 7, 500) > 0);
	}

	@Test
	void eggsGetBiggerWithAge() {
		Map<String, Integer> leg = Map.of("LEG", 10_000);
		assertEquals(54.7, LayingCycle.eggGrams(leg, 143, 500), 0.05);
		assertEquals(58.5, LayingCycle.eggGrams(leg, 143 + (32 - 20.4) * 7, 500), 0.05);
		assertEquals(63.3, LayingCycle.eggGrams(leg, 143 + 60 * 7, 500), 0.05);
		assertEquals(58.5 * 1.1, LayingCycle.eggGrams(leg, 143 + (32 - 20.4) * 7, 1000), 0.05);
	}
}
