package dev.farmings.farm.crops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.farmings.husbandry.TimeScale;
import org.junit.jupiter.api.Test;

class FieldCropTest {
	@Test
	void growsOnTheCattleClock() {
		// alfalfa: 65 days to first cut is 4.33 game days; 32.5 days of regrowth 2.17
		assertEquals(65.0 / 15, TimeScale.ticksToRealDays(FieldCrop.ALFALFA.stageTicks() * FieldCrop.MAX_STAGE) / 15, 0.001);
		assertEquals(32.5, TimeScale.ticksToRealDays(FieldCrop.ALFALFA.regrowthStageTicks() * 4), 0.01);
		assertEquals(14.0, TimeScale.ticksToRealDays(FieldCrop.cornRipeningTicks()), 1e-9);
	}

	@Test
	void yieldsAreTenTimesTheRealSquareMetre() {
		// 7.97 t DM/ha a year in 4 cuts, x10: 2.0 kg DM a block a cut, 2.25 kg of hay at 89% DM
		assertEquals(2.0, FieldCrop.ALFALFA.yieldKg, 0.01);
		assertEquals(2.25, FieldCrop.ALFALFA.hayKg(), 0.01);
		// corn: 17.6 kg DM is 50.3 kg chopped at 35% DM
		assertEquals(50.3, FieldCrop.CORN.choppedKg(), 0.1);
		assertEquals(4.55, FieldCrop.OATS.strawKg(), 1e-9);
		// a cow eats about 8 t DM a year (22 kg a day); alfalfa's 4 cuts give 8 kg DM a block a year: about 1,000 blocks, 32 x 32
		double blocks = 8000 / (FieldCrop.ALFALFA.yieldKg * 4);
		assertEquals(1000, blocks, 1);
	}
}
