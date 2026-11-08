package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GrowthTest {
	@Test
	void sourcedBreedsHitTheirFifteenMonthWeights() {
		assertEquals(425, Growth.targetKg(CattleBreed.HOLSTEIN, 456), 1e-9);
		assertEquals(297, Growth.targetKg(CattleBreed.JERSEY, 456), 1e-9);
		assertEquals(379, Growth.targetKg(CattleBreed.BROWN_SWISS, 456), 1e-9);
	}

	@Test
	void otherBreedsUseHolsteinsFraction() {
		assertEquals(550 * 0.60, Growth.targetKg(CattleBreed.ANGUS, 456), 1e-9);
	}

	@Test
	void birthAndMaturityAreTheEnds() {
		for (CattleBreed b : CattleBreed.values()) {
			assertEquals(b.matureWeightKg * 0.06, Growth.targetKg(b, 0), 1e-9);
			assertEquals(b.matureWeightKg, Growth.targetKg(b, 1460), 1e-9);
			assertEquals(b.matureWeightKg, Growth.targetKg(b, 3000), 1e-9);
		}
	}

	@Test
	void gainToFifteenMonthsMatchesTheAdvice() {
		double hol = Growth.gainKg(CattleBreed.HOLSTEIN, 0, 456, 1) / 456;
		double jer = Growth.gainKg(CattleBreed.JERSEY, 0, 456, 1) / 456;
		assertEquals(0.84, hol, 0.01);
		assertEquals(0.59, jer, 0.01);
	}

	@Test
	void gainIsTheSameInOneStepOrMany() {
		double one = Growth.gainKg(CattleBreed.HOLSTEIN, 400, 200, 1);
		double many = 0;
		for (int i = 0; i < 200; i++) {
			many += Growth.gainKg(CattleBreed.HOLSTEIN, 400 + i, 1, 1);
		}
		assertEquals(one, many, 1e-6);
	}

	@Test
	void underfedGrowsSlowerAndGrownStops() {
		double full = Growth.gainKg(CattleBreed.HOLSTEIN, 100, 100, 1);
		assertEquals(full / 2, Growth.gainKg(CattleBreed.HOLSTEIN, 100, 100, 0.5), 1e-9);
		assertEquals(0, Growth.gainKg(CattleBreed.HOLSTEIN, 100, 100, 0), 1e-9);
		assertEquals(0, Growth.gainKg(CattleBreed.HOLSTEIN, 1500, 100, 1), 1e-9);
		assertEquals(0, Growth.gainKg(CattleBreed.HOLSTEIN, 100, -5, 1), 1e-9);
	}
}
