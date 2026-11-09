package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VisualGenomeTest {
	private static VisualGenome with(VisualGenome base, VisualLocus l, int a, int b) {
		EnumMap<VisualLocus, int[]> m = new EnumMap<>(base.alleles());
		m.put(l, new int[] {a, b});
		return new VisualGenome(m);
	}

	private static final VisualGenome HOL = VisualGenome.founder("HOL", 1L);

	@Test
	void blackOverRedGivesAllBlackCalves() {
		VisualGenome black = with(HOL, VisualLocus.EXTENSION, 0, 0);
		VisualGenome red = with(HOL, VisualLocus.EXTENSION, 3, 3);
		for (long s = 0; s < 200; s++) {
			VisualGenome calf = VisualGenome.inherit(black, red, s);
			assertEquals(0, calf.expressed(VisualLocus.EXTENSION));
			assertEquals(VisualPhenotype.Coat.BLACK, VisualPhenotype.of(calf, Map.of("HOL", 10000)).coat());
		}
	}

	@Test
	void redTimesRedIsRed() {
		VisualGenome red = with(HOL, VisualLocus.EXTENSION, 3, 3);
		for (long s = 0; s < 200; s++) {
			assertEquals(VisualPhenotype.Coat.RED, VisualPhenotype.of(VisualGenome.inherit(red, red, s), Map.of("HOL", 10000)).coat());
		}
	}

	@Test
	void twoRedCarriersGiveAQuarterRed() {
		VisualGenome carrier = with(HOL, VisualLocus.EXTENSION, 0, 3);
		int red = 0;
		int n = 8000;
		for (long s = 0; s < n; s++) {
			red += VisualGenome.inherit(carrier, carrier, s * 7919).expressed(VisualLocus.EXTENSION) == 3 ? 1 : 0;
		}
		assertEquals(0.25, red / (double) n, 0.02);
	}

	@Test
	void polledIsDominant() {
		VisualGenome polled = with(HOL, VisualLocus.HORNS, 0, 0);
		VisualGenome horned = with(HOL, VisualLocus.HORNS, 1, 1);
		VisualGenome het = with(HOL, VisualLocus.HORNS, 0, 1);
		int hornedCalves = 0;
		for (long s = 0; s < 4000; s++) {
			assertEquals(0, VisualGenome.inherit(polled, horned, s).expressed(VisualLocus.HORNS));
			hornedCalves += VisualGenome.inherit(het, het, s * 31).expressed(VisualLocus.HORNS);
		}
		assertEquals(0.25, hornedCalves / 4000.0, 0.025);
	}

	@Test
	void theDominanceOrderIsEdEbrEplusE() {
		assertEquals(0, VisualLocus.EXTENSION.express(0, 1));
		assertEquals(1, VisualLocus.EXTENSION.express(1, 2));
		assertEquals(2, VisualLocus.EXTENSION.express(3, 2));
		assertEquals(3, VisualLocus.EXTENSION.express(3, 3));
	}

	@Test
	void gradedLociBlendAndPatternsLayer() {
		assertEquals(2, VisualLocus.HUMP.express(3, 0), "a Gyr x Holstein hump lands between");
		assertEquals(500, VisualLocus.WHITE.express(800, 200));
		assertEquals(1, VisualLocus.PATTERN.express(0, 3), "solid over patch is solid (proposed)");
		assertEquals((1 << 2) | (1 << 3), VisualLocus.PATTERN.express(2, 3), "speckled and patch layer");
	}

	@Test
	void breedFoundersLookLikeTheirBreeds() {
		for (long s = 0; s < 50; s++) {
			VisualPhenotype hol = VisualPhenotype.of(VisualGenome.founder("HOL", s), Map.of("HOL", 10000));
			assertTrue(hol.shows(3), "Holsteins are patched");
			assertTrue(hol.white() >= 450 && hol.white() <= 900);
			VisualPhenotype ang = VisualPhenotype.of(VisualGenome.founder("ANG", s), Map.of("ANG", 10000));
			assertTrue(!ang.horned() && ang.solid(), "Angus are polled and solid");
			assertEquals(VisualPhenotype.BodyFamily.BEEF, ang.body());
			VisualPhenotype gyr = VisualPhenotype.of(VisualGenome.founder("GYR", s), Map.of("GYR", 10000));
			assertEquals(3, gyr.hump());
			assertEquals(VisualPhenotype.BodyFamily.ZEBU, gyr.body());
			assertTrue(gyr.horned() && gyr.shows(2));
			VisualPhenotype wag = VisualPhenotype.of(VisualGenome.founder("WAG", s), Map.of("WAG", 10000));
			assertTrue(wag.horned() && wag.coat() == VisualPhenotype.Coat.BLACK);
		}
	}

	@Test
	void anF1BlendsTheSecondBodyFamilyAtAQuarterOrMore() {
		VisualGenome hol = VisualGenome.founder("HOL", 5L);
		VisualGenome ang = VisualGenome.founder("ANG", 6L);
		VisualPhenotype f1 = VisualPhenotype.of(VisualGenome.inherit(hol, ang, 9L), Map.of("HOL", 5000, "ANG", 5000));
		assertNotNull(f1.secondary());
		assertEquals(50, f1.secondaryPct());
		VisualPhenotype back = VisualPhenotype.of(hol, Map.of("HOL", 8750, "ANG", 1250));
		assertNull(back.secondary(), "12.5% does not blend");
		assertEquals(VisualPhenotype.BodyFamily.DAIRY, back.body());
	}

	@Test
	void packingRoundTrips() {
		String[] breeds = {"HOL", "JER", "BSW", "GYR", "ANG", "WAG"};
		for (long s = 0; s < 60; s++) {
			String a = breeds[(int) (s % 6)];
			String b = breeds[(int) ((s / 6) % 6)];
			VisualGenome calf = VisualGenome.inherit(VisualGenome.founder(a, s), VisualGenome.founder(b, s + 100), s);
			VisualPhenotype p = VisualPhenotype.of(calf, a.equals(b) ? Map.of(a, 10000) : Map.of(a, 5000, b, 5000));
			assertEquals(p, VisualPhenotype.unpack(p.pack()), a + " x " + b);
		}
	}

	@Test
	void savingAndFallbackAreStable() {
		VisualGenome g = VisualGenome.founder("JER", 77L);
		assertEquals(g, VisualGenome.load(g.save()));
		assertNull(VisualGenome.load(new int[3]));
		assertEquals(g, VisualGenome.founder("JER", 77L), "the same seed gives the same founder");
		Genome genome = Founders.create("GYR", Sex.FEMALE, 4L, Ruleset.defaults()).genome();
		assertEquals(VisualGenome.fallback(genome), VisualGenome.fallback(genome));
		assertEquals(3, VisualGenome.fallback(genome).expressed(VisualLocus.HUMP), "a pure Gyr's fallback coat is a Gyr's");
	}
}
