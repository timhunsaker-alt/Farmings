package dev.farmings.husbandry.poultry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.genetics.Species;
import dev.farmings.husbandry.TimeScale;
import org.junit.jupiter.api.Test;

class ClutchTest {
	@Test
	void fertileForWeeksAfterMating() {
		assertTrue(Clutch.fertile(Species.CHICKEN, 28));
		assertFalse(Clutch.fertile(Species.CHICKEN, 28.5));
		assertFalse(Clutch.fertile(Species.TURKEY, 22));
	}

	@Test
	void incubationStages() {
		assertEquals(Clutch.Stage.SETTING, Clutch.stage(FowlBreed.LEGHORN, 17.9));
		assertEquals(Clutch.Stage.LOCKDOWN, Clutch.stage(FowlBreed.LEGHORN, 18));
		assertEquals(Clutch.Stage.HATCHING, Clutch.stage(FowlBreed.LEGHORN, 21));
		assertEquals(Clutch.Stage.LOCKDOWN, Clutch.stage(FowlBreed.MUSCOVY, 32));
		assertEquals(7.0, TimeScale.FOWL.realDaysToGameDays(FowlBreed.LEGHORN.incubationDays), 1e-9);
		assertTrue(Clutch.hatchable(7));
		assertFalse(Clutch.hatchable(7.5));
	}

	@Test
	void broodinessNeedsAClutch() {
		assertEquals(0.098, Clutch.broodyChance(980, 6), 1e-9);
		assertEquals(0, Clutch.broodyChance(980, 5));
		assertEquals(0.005, Clutch.broodyChance(50, 12), 1e-9);
	}
}
