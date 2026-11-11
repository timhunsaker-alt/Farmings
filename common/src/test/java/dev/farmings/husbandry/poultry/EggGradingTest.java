package dev.farmings.husbandry.poultry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EggGradingTest {
	@Test
	void usdaSizesPerEgg() {
		assertEquals(56.70, EggGrading.Size.LARGE.minGrams(), 0.01);       // 24 oz a dozen
		assertEquals(70.87, EggGrading.Size.JUMBO.minGrams(), 0.01);
		assertEquals(EggGrading.Size.LARGE, EggGrading.size(58.5));          // a Leghorn at 32 weeks
		assertEquals(EggGrading.Size.MEDIUM, EggGrading.size(54.7));         // at 26 weeks
		assertEquals(EggGrading.Size.EXTRA_LARGE, EggGrading.size(64.0));
		assertEquals(EggGrading.Size.UNDERGRADE, EggGrading.size(10));       // a quail egg
	}

	@Test
	void gradeFallsWithAgeAndFloorEggs() {
		assertEquals(EggGrading.Grade.AA, EggGrading.grade(1, false));
		assertEquals(EggGrading.Grade.A, EggGrading.grade(10, false));
		assertEquals(EggGrading.Grade.B, EggGrading.grade(30, false));
		assertEquals(EggGrading.Grade.B, EggGrading.grade(1, true));
	}
}
