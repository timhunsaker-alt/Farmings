package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HygieneTest {
	@Test
	void oneDaysManureIsNormal() {
		assertEquals(0, Hygiene.dirtyFraction(0));
		assertEquals(0, Hygiene.dirtyFraction(1));
		assertEquals(0.5, Hygiene.dirtyFraction(3), 1e-9);
		assertEquals(1, Hygiene.dirtyFraction(9));
	}

	@Test
	void fullyDirtyCostsTenPercentMilk() {
		assertEquals(0.9, Hygiene.milkFactor(1), 1e-9);
		assertEquals(1.0, Hygiene.milkFactor(0), 1e-9);
	}

	@Test
	void healthNeverFallsBelowTheFloor() {
		assertEquals(1.0, Hygiene.healthLoss(1, 10, TimeScale.TICKS_PER_GAME_DAY), 1e-9);
		assertEquals(0.5, Hygiene.healthLoss(1, 2.5, TimeScale.TICKS_PER_GAME_DAY * 10L), 1e-9);
		assertEquals(0, Hygiene.healthLoss(1, 2, TimeScale.TICKS_PER_GAME_DAY));
		assertEquals(0, Hygiene.healthLoss(0.25, 10, TimeScale.TICKS_PER_GAME_DAY));
	}
}
