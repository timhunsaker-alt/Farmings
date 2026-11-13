package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Goat coat genes (docs/animals/goats.md, "Coat genes"). */
class GoatGeneticsTest {
	private static GoatGenes genes(int[] agouti, int[] red, int[] polled) {
		Map<GoatLocus, int[]> m = new EnumMap<>(GoatLocus.class);
		m.put(GoatLocus.AGOUTI, agouti);
		m.put(GoatLocus.RED_HEAD, red);
		m.put(GoatLocus.POLLED, polled);
		return new GoatGenes(m);
	}

	@Test
	void foundersLookLikeTheirBreed() {
		for (long seed = 1; seed < 40; seed++) {
			GoatLook saanen = GoatLook.of(GoatGenes.founder("SAA", seed), Sex.FEMALE);
			assertEquals(GoatLook.Colour.WHITE, saanen.colour());
			assertEquals(0, saanen.pattern());
			GoatLook boer = GoatLook.of(GoatGenes.founder("BOE", seed), Sex.MALE);
			assertEquals(2, boer.pattern(), "Boer: white body, red head");
			assertFalse(boer.polled());
			assertFalse(GoatGenes.founder("SAA", seed).polledHomozygous(), "no founder is intersex");
		}
		Founders.Result f = Founders.create("NUB", Sex.FEMALE, 3L, Ruleset.defaults());
		assertEquals(Species.GOAT, f.genome().species());
	}

	@Test
	void agoutiWhiteIsOnTopAndNonagoutiRecessive() {
		assertEquals(GoatLook.Colour.WHITE, GoatLook.of(genes(new int[] {0, 4}, new int[] {0, 0}, new int[] {0, 0}), Sex.FEMALE).colour());
		assertEquals(GoatLook.Colour.BEZOAR, GoatLook.of(genes(new int[] {1, 4}, new int[] {0, 0}, new int[] {0, 0}), Sex.FEMALE).colour());
		assertEquals(GoatLook.Colour.BLACK, GoatLook.of(genes(new int[] {4, 4}, new int[] {0, 0}, new int[] {0, 0}), Sex.FEMALE).colour());
	}

	@Test
	void redHeadIsIncompletelyDominant() {
		assertEquals(1, GoatLook.of(genes(new int[] {0, 0}, new int[] {0, 2}, new int[] {0, 0}), Sex.FEMALE).pattern());
		assertEquals(2, GoatLook.of(genes(new int[] {0, 0}, new int[] {2, 2}, new int[] {0, 0}), Sex.FEMALE).pattern());
	}

	@Test
	void polledPolledDoesAreIntersex() {
		GoatGenes pp = genes(new int[] {0, 0}, new int[] {0, 0}, new int[] {1, 1});
		assertTrue(GoatLook.of(pp, Sex.FEMALE).intersex());
		assertFalse(GoatLook.of(pp, Sex.MALE).intersex());
		assertFalse(GoatLook.of(genes(new int[] {0, 0}, new int[] {0, 0}, new int[] {1, 0}), Sex.FEMALE).intersex());
		assertTrue(GoatLook.of(genes(new int[] {0, 0}, new int[] {0, 0}, new int[] {1, 0}), Sex.FEMALE).polled());
	}

	@Test
	void looksAndGenesSurviveSavingAndPacking() {
		GoatGenes g = GoatGenes.founder("NUB", 77L);
		assertEquals(g, GoatGenes.load(g.save()));
		GoatLook l = GoatLook.of(g, Sex.MALE);
		assertEquals(l, GoatLook.unpack(l.pack()));
	}
}
