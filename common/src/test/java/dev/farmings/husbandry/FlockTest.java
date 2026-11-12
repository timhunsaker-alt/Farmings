package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FlockTest {
	static Map<String, Integer> pure(SheepBreed b) {
		return Map.of(b.code, 10_000);
	}

	@Test
	void litterOddsGiveTheBreedMean() {
		for (SheepBreed b : SheepBreed.values()) {
			Flock.Litter l = Flock.litter(pure(b), 500);
			assertEquals(b.litterMean, l.mean(), 1e-9, b.name());
			assertEquals(1.0, l.single() + l.twins() + l.triplets(), 1e-9);
		}
		assertEquals(2, Flock.litterFor(1.5).roll(0.7));
		assertEquals(1, Flock.litterFor(1.0).roll(0.99));
	}

	@Test
	void fertilityMovesTheLitter() {
		assertTrue(Flock.litter(pure(SheepBreed.SUFFOLK), 1000).mean() > Flock.litter(pure(SheepBreed.SUFFOLK), 0).mean());
		assertEquals(2.0, Flock.litter(pure(SheepBreed.SUFFOLK), 1000).mean(), 1e-9);
	}

	@Test
	void aYearOfWoolIsTheBreedFleece() {
		assertEquals(5.0, Flock.fleeceKg(pure(SheepBreed.MERINO), 365, 500, 1.0), 1e-9);
		assertEquals(2.5, Flock.fleeceKg(pure(SheepBreed.MERINO), 182.5, 500, 1.0), 1e-9);
		assertEquals(6.0, Flock.fleeceKg(pure(SheepBreed.MERINO), 365, 1000, 1.0), 1e-9);
		assertEquals(10.0, Flock.fleeceKg(pure(SheepBreed.MERINO), 2000, 500, 1.0), 1e-9, "capped at two years");
		assertEquals(0, Flock.fleeceKg(pure(SheepBreed.DORPER), 365, 500, 0.0), "a hair sheep has no fleece to shear");
	}

	@Test
	void micronAndGradeFollowTheBreedAndFleeceQuality() {
		assertEquals(Flock.Grade.FINE, Flock.grade(Flock.micron(pure(SheepBreed.MERINO), 500)));
		assertEquals(Flock.Grade.QUARTER, Flock.grade(Flock.micron(pure(SheepBreed.SUFFOLK), 500)));
		assertEquals(Flock.Grade.COARSE, Flock.grade(Flock.micron(pure(SheepBreed.EAST_FRIESIAN), 500)));
		assertEquals(Flock.Grade.HAIR, Flock.grade(Flock.micron(pure(SheepBreed.DORPER), 500)));
		// a Merino x Dorper cross takes its micron from the Merino half only
		assertEquals(21, Flock.micron(Map.of("MER", 5000, "DOR", 5000), 500), 1e-9);
		assertTrue(Flock.micron(pure(SheepBreed.MERINO), 1000) < Flock.micron(pure(SheepBreed.MERINO), 0));
		assertEquals(Flock.Grade.HALF_BLOOD, Flock.Grade.FINE.lower());
	}

	@Test
	void onlyEastFriesianBloodGivesParlorMilk() {
		double total = 0;
		for (int d = 1; d <= Flock.LACTATION_DAYS; d++) {
			total += Flock.milkKg(pure(SheepBreed.EAST_FRIESIAN), d, 500);
		}
		assertEquals(365, total, 1e-6);
		assertEquals(0, Flock.milkKg(pure(SheepBreed.SUFFOLK), 30, 500));
		assertEquals(Flock.milkKg(30) / 2, Flock.milkKg(Map.of("EFR", 5000, "SUF", 5000), 30, 500), 1e-9);
	}

	@Test
	void eweLambsAreBredBySevenMonthsAndWeight() {
		Map<String, Integer> suf = pure(SheepBreed.SUFFOLK);
		assertFalse(Flock.canFirstBreed(suf, 150, 80));
		assertFalse(Flock.canFirstBreed(suf, 240, 50));
		assertTrue(Flock.canFirstBreed(suf, 240, 60));
		assertEquals(90 * 0.65, Flock.targetKg(suf, Flock.FIRST_BREEDING_AGE_DAYS), 1e-9);
		assertEquals(90, Flock.targetKg(suf, 800), 1e-9);
		assertEquals(50 * 0.70, Flock.targetKg(pure(SheepBreed.MERINO), Flock.FIRST_BREEDING_AGE_DAYS), 1e-9);
	}

	@Test
	void housingAndMeat() {
		assertEquals(2, Flock.penCapacity(3));
		assertEquals(6, Flock.penCapacity(9));
		assertEquals(12, Flock.meatItems(90));
		assertEquals(1.25, Flock.intakeKgDm(50, 0), 1e-9);
	}

	@org.junit.jupiter.api.Test
	void stillbirthRatesBySourceAndLitter() {
		org.junit.jupiter.api.Assertions.assertEquals(0.0904, Flock.stillbirthRate(true, 2) / 1.2, 0.0005);
		org.junit.jupiter.api.Assertions.assertEquals(0.00966, Flock.stillbirthRate(false, 1) / 0.8, 0.0001);
		org.junit.jupiter.api.Assertions.assertTrue(Flock.stillbirthRate(false, 3) > Flock.stillbirthRate(false, 1));
	}

	@org.junit.jupiter.api.Test
	void breedingSeasonsByBreed() {
		// a world starts on 1 October: every breed is in season
		org.junit.jupiter.api.Assertions.assertEquals("1 Oct", Flock.date(Flock.dayOfYear(0)));
		for (String b : new String[] {"SUF", "EFR", "MER", "DOR"}) {
			org.junit.jupiter.api.Assertions.assertTrue(Flock.inSeason(b, Flock.dayOfYear(0)), b);
		}
		int may = 135;     // 16 May: everyone out of season
		int feb = 40;      // 10 Feb: only the long-season breeds
		int sep5 = 247;
		org.junit.jupiter.api.Assertions.assertFalse(Flock.inSeason("SUF", may));
		org.junit.jupiter.api.Assertions.assertFalse(Flock.inSeason("MER", may));
		org.junit.jupiter.api.Assertions.assertFalse(Flock.inSeason("SUF", feb));
		org.junit.jupiter.api.Assertions.assertFalse(Flock.inSeason("EFR", feb));
		org.junit.jupiter.api.Assertions.assertTrue(Flock.inSeason("MER", feb));
		org.junit.jupiter.api.Assertions.assertTrue(Flock.inSeason("DOR", feb));
		org.junit.jupiter.api.Assertions.assertTrue(Flock.inSeason("SUF", sep5));
		org.junit.jupiter.api.Assertions.assertTrue(Flock.inSeason("SUF", 0), "the short season runs to 1 January");
		// a year is 365 real days
		org.junit.jupiter.api.Assertions.assertEquals(Flock.dayOfYear(10), Flock.dayOfYear(375));
	}
}
