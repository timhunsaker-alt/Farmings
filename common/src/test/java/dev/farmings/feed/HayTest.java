package dev.farmings.feed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.husbandry.CattleBreed;
import dev.farmings.husbandry.Stage;
import org.junit.jupiter.api.Test;

class HayTest {
	@Test
	void aBlockHoldsEightyFiveKgOfDryMatter() {
		assertEquals(85.0, Hay.blockDryMatterKg(), 1e-9);
	}

	@Test
	void aLactatingHolsteinEatsAboutAQuarterBlockADay() {
		double dmi = Hay.dryMatterIntakeKg(Stage.LACTATING_COW, CattleBreed.HOLSTEIN.matureWeightKg).getAsDouble();
		assertEquals(23.08, dmi, 0.01);     // 3.25% of 710 kg (FAS Scotland, 3-3.5%)
		double blocks = Hay.blocksPerDay(dmi);
		assertTrue(blocks > 0.27 && blocks < 0.28, "blocks/day " + blocks);
	}

	@Test
	void dryCowsEatLessAndOtherStagesHaveNoFigureYet() {
		double lact = Hay.dryMatterIntakeKg(Stage.LACTATING_COW, 650).getAsDouble();
		double dry = Hay.dryMatterIntakeKg(Stage.DRY_COW, 650).getAsDouble();
		assertTrue(dry < lact);
		assertTrue(Hay.dryMatterIntakeKg(Stage.CALF, 100).isEmpty());
		assertTrue(Hay.dryMatterIntakeKg(Stage.BULL, 800).isEmpty());
	}

	@Test
	void waterFollowsTheUcDavisRule() {
		assertEquals(6 * 20 + 30, Hay.waterLitresPerDay(20, 30), 1e-9);
		assertEquals(6 * 20, Hay.waterLitresPerDay(20, 0), 1e-9);
	}

	@Test
	void aBarnOfEightHasHayForAboutEightGameDays() {
		// barn.md: a feeder holds 4 blocks a cell, 8 cows have 16 blocks. At about 0.271 block per cow a day that is 16 / (8 x 0.271).
		double perCow = Hay.blocksPerDay(Hay.dryMatterIntakeKg(Stage.LACTATING_COW, CattleBreed.HOLSTEIN.matureWeightKg).getAsDouble());
		double days = 16 / (8 * perCow);
		assertEquals(7.4, days, 0.2);
	}
}
