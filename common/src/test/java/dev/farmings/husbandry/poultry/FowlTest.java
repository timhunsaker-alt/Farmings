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
}
