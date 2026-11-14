package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** The horse rules and the speed model against docs/animals/horses.md. */
class GallopTest {
	private static Map<String, Integer> pure(HorseBreed b) {
		return Map.of(b.code, 10_000);
	}

	private static Gallop.Runner elite(HorseBreed b, int mstnC) {
		return new Gallop.Runner(pure(b), mstnC, 1000, 500, Gallop.PEAK_YEARS, 1);
	}

	@Test
	void recordsHitTheSourcedPaces() {
		assertEquals(19.54, Gallop.record(HorseBreed.THOROUGHBRED, 402), 1e-9);
		assertEquals(16.85, Gallop.record(HorseBreed.THOROUGHBRED, 2012), 1e-9);
		assertEquals(16.76, Gallop.record(HorseBreed.THOROUGHBRED, 2414), 1e-9);
		assertEquals(19.84, Gallop.record(HorseBreed.QUARTER_HORSE, 402), 1e-9);
		assertEquals(0.92 * 19.54, Gallop.record(HorseBreed.ARABIAN, 402), 1e-9);
		assertTrue(Gallop.record(HorseBreed.THOROUGHBRED, 4828) < 16.76 * 0.991);
	}

	@Test
	void anEliteHorseAtItsBestDistanceRunsItsRecord() {
		Gallop.Runner r = elite(HorseBreed.THOROUGHBRED, 1);
		assertEquals(Gallop.record(HorseBreed.THOROUGHBRED, 1830), r.pace(1830), 1e-9);
	}

	@Test
	void sprinterBeatsStayerShortAndLosesLong() {
		Gallop.Runner cc = elite(HorseBreed.THOROUGHBRED, 2);
		Gallop.Runner tt = elite(HorseBreed.THOROUGHBRED, 0);
		assertTrue(cc.time(402) < tt.time(402), "C/C wins the quarter mile");
		assertTrue(tt.time(2414) < cc.time(2414), "T/T wins the mile and a half");
	}

	@Test
	void quarterHorseWinsShortThoroughbredLong() {
		Gallop.Runner qh = elite(HorseBreed.QUARTER_HORSE, 2);
		Gallop.Runner tb = elite(HorseBreed.THOROUGHBRED, 1);
		assertTrue(qh.time(402) < tb.time(402));
		assertTrue(tb.time(2012) < qh.time(2012));
	}

	@Test
	void speedAtAddsUpToTheRaceTime() {
		Gallop.Runner r = new Gallop.Runner(pure(HorseBreed.ARABIAN), 1, 600, 700, 6, 0.5);
		for (int d : new int[] {402, 1207, 1609, 2414}) {
			double t = 0;
			for (int x = 0; x < d; x++) {
				t += 1 / r.speedAt(x);
			}
			assertEquals(r.time(d), t, 1e-6, "time over " + d);
		}
	}

	@Test
	void ageAndFitness() {
		assertEquals(1 - 0.021, Gallop.ageFactor(2), 1e-9);
		assertEquals(1, Gallop.ageFactor(4.45), 1e-9);
		assertEquals(1 - 0.0025 * 4, Gallop.ageFactor(8.45), 1e-9);
		assertEquals(0.95, Gallop.fitnessFactor(0), 1e-9);
		assertEquals(1, Gallop.fitnessFactor(1), 1e-9);
	}

	@Test
	void fitnessHeldThenLost() {
		assertEquals(0.8, Gallop.rested(0.8, 42), 1e-9);
		assertEquals(0.4, Gallop.rested(0.8, 63), 1e-9);
		assertEquals(0, Gallop.rested(0.8, 90), 1e-9);
		assertEquals(0.02, Gallop.gain(1, 0), 1e-9);
		assertEquals(0.01, Gallop.gain(5, 0.05), 1e-9, "capped at 0.06 a day");
	}

	@Test
	void growthAndSeason() {
		Map<String, Integer> c = pure(HorseBreed.THOROUGHBRED);
		assertEquals(45.4, Equine.targetKg(c, 0), 1e-9);
		assertEquals(0.64 * 454, Equine.targetKg(c, 365), 1e-9);
		assertEquals(454, Equine.targetKg(c, 2000), 1e-9);
		assertTrue(Equine.inSeason(105) && Equine.inSeason(303));
		assertTrue(!Equine.inSeason(104) && !Equine.inSeason(304));
		assertEquals(HorseStage.STALLION, HorseStage.male(3 * 365, false));
		assertEquals(HorseStage.COLT, HorseStage.male(2 * 365, false));
		assertEquals(HorseStage.FOAL, HorseStage.female(100, false));
	}
}
