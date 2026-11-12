package dev.farmings.husbandry.poultry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.genetics.Breeds;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FowlTest {
	static Map<String, Integer> pure(String code) {
		return Map.of(code, 10_000);
	}

	@Test
	void everyBreedHasGeneticsAndTheRightSpecies() {
		for (FowlBreed b : FowlBreed.values()) {
			assertTrue(Breeds.known(b.code), b.code);
			assertEquals(b.species, Breeds.species(b.code), b.code);
			assertNotNull(FowlBreed.byCode(b.code));
			assertTrue(b.stopTurningDay < b.incubationDays, b.code);
		}
	}

	@Test
	void growsFromHatchlingToMatureWeightByFirstEgg() {
		assertEquals(0.0585 * 2 / 3, Fowl.weightKg(pure("LEG"), Sex.FEMALE, 0), 1e-9);
		assertEquals(2.04, Fowl.weightKg(pure("LEG"), Sex.FEMALE, 143), 1e-9);
		assertEquals(16.56, Fowl.weightKg(pure("BRZ"), Sex.MALE, 400), 1e-9);
		assertEquals((3.86 + 2.72) / 2, Fowl.matureKg(Map.of("RIR", 5000, "LEG", 5000), Sex.MALE), 1e-9);
	}

	@Test
	void youngNeedHeatUntilFeathered() {
		assertTrue(Fowl.needsHeat(pure("LEG"), 41));
		assertFalse(Fowl.needsHeat(pure("LEG"), 42));
		assertFalse(Fowl.needsHeat(pure("COT"), 25));
	}

	@Test
	void feedFromTheSpeciesAnchors() {
		assertEquals(0.100, Fowl.feedKgPerDay(Species.CHICKEN, 2.0), 1e-9);
		assertEquals(0.016, Fowl.feedKgPerDay(Species.QUAIL, 0.14), 1e-9);
		assertEquals(0.218, Fowl.feedKgPerDay(Species.DUCK, 3.9), 1e-9);
		assertEquals(0.200, Fowl.waterLPerDay(Species.CHICKEN, 2.0), 1e-9);
		assertEquals(0.654, Fowl.waterLPerDay(Species.DUCK, 3.9), 1e-9);
		double turkeyHen = Fowl.feedKgPerDay(Species.TURKEY, 8.2);
		assertTrue(turkeyHen > 0.28 && turkeyHen < 0.30, "about 290 g for an 8.2 kg hen: " + turkeyHen);
	}

	@Test
	void meatFromDressedWeight() {
		assertEquals(4, Fowl.meatItems(pure("PLR"), 3.40));
		assertEquals(26, Fowl.meatItems(pure("BRZ"), 16.56));
		assertEquals(1, Fowl.meatItems(pure("COT"), 0.14));
	}

	@Test
	void runRegrowsSlowlyAndWearsUnderLoad() {
		// 50 hens an acre is sustainable: 25 g each over 4,047 m2 regrows exactly
		org.junit.jupiter.api.Assertions.assertEquals(0.0, Fowl.runDrawn(0, 50 * 0.025, 4047, 1.0), 1e-9);
		// 10 hens on an 8 x 6 run: about 235 g a day more than it regrows
		double eaten = 10 * 0.025;
		double drawn = Fowl.runDrawn(0, eaten, 48, 1.0);
		org.junit.jupiter.api.Assertions.assertEquals(0.235, drawn, 0.001);
		// regrowth never takes it below zero
		org.junit.jupiter.api.Assertions.assertEquals(0.0, Fowl.runDrawn(0.001, 0, 48, 1.0), 1e-12);
		org.junit.jupiter.api.Assertions.assertEquals(10, Fowl.hensPerMale(dev.farmings.genetics.Species.CHICKEN));
		org.junit.jupiter.api.Assertions.assertEquals(3, Fowl.hensPerMale(dev.farmings.genetics.Species.QUAIL));
		org.junit.jupiter.api.Assertions.assertEquals(5, Fowl.hensPerMale(dev.farmings.genetics.Species.DUCK));
	}
}
