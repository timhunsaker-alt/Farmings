package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.*;

import dev.farmings.genetics.Sex;
import org.junit.jupiter.api.Test;

class BreedingTest {
	@Test
	void aHeiferCyclesOnlyAfterTheBreedingRules() {
		CattleBreed h = CattleBreed.HOLSTEIN;
		assertFalse(Breeding.cycles(h, Stage.HEIFER, false, 300, 400, 0));
		assertFalse(Breeding.cycles(h, Stage.HEIFER, false, 500, 300, 0));
		assertTrue(Breeding.cycles(h, Stage.HEIFER, false, 500, 400, 0));
	}

	@Test
	void aCowWaitsSixtyDaysAndNeverCyclesInCalf() {
		CattleBreed h = CattleBreed.HOLSTEIN;
		assertFalse(Breeding.cycles(h, Stage.LACTATING_COW, false, 900, 650, 59));
		assertTrue(Breeding.cycles(h, Stage.LACTATING_COW, false, 900, 650, 60));
		assertFalse(Breeding.cycles(h, Stage.LACTATING_COW, true, 900, 650, 100));
		assertFalse(Breeding.cycles(h, Stage.DRY_COW, false, 900, 650, 400));
		assertFalse(Breeding.cycles(h, Stage.CALF, false, 100, 100, 0));
	}

	@Test
	void heatWindowIsThreeQuartersOfARealDayEveryTwentyOne() {
		assertEquals(1200, Breeding.heatTicks());
		assertEquals(33_600, Breeding.cycleTicks());
		assertFalse(Breeding.inHeat(99, 100));
		assertTrue(Breeding.inHeat(100, 100));
		assertTrue(Breeding.inHeat(1299, 100));
		assertFalse(Breeding.inHeat(1300, 100));
		assertFalse(Breeding.inHeat(5, -1));
		assertEquals(100 + 33_600, Breeding.nextHeatStart(1300, 100));
		assertEquals(100, Breeding.nextHeatStart(500, 100));
	}

	@Test
	void firstTwoCalvesAreHeifersThenABullThenFree() {
		assertEquals(Sex.FEMALE, Breeding.forcedSex(0));
		assertEquals(Sex.FEMALE, Breeding.forcedSex(1));
		assertEquals(Sex.MALE, Breeding.forcedSex(2));
		assertNull(Breeding.forcedSex(3));
		assertNull(Breeding.forcedSex(40));
	}

	@Test
	void twinAndStillbirthRatesFollowParity() {
		assertTrue(Breeding.twins(0.03, false));
		assertFalse(Breeding.twins(0.03, true));
		assertTrue(Breeding.stillborn(0.10, true));
		assertFalse(Breeding.stillborn(0.10, false));
		assertTrue(Breeding.freemartin(0.5));
		assertFalse(Breeding.freemartin(0.95));
	}
}
