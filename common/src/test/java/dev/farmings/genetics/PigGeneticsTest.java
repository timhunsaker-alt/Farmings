package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Pig coat genes (docs/animals/pigs.md, "Coat genes"). */
class PigGeneticsTest {
	private static PigGenes g(int[] kit, int[] mc1r) {
		Map<PigLocus, int[]> m = new EnumMap<>(PigLocus.class);
		m.put(PigLocus.KIT, kit);
		m.put(PigLocus.MC1R, mc1r);
		return new PigGenes(m);
	}

	@Test
	void foundersLookLikeTheirBreed() {
		for (long seed = 1; seed < 20; seed++) {
			assertEquals(PigLook.Colour.WHITE, PigLook.of(PigGenes.founder("LWH", seed)).colour());
			assertEquals(PigLook.Colour.RED, PigLook.of(PigGenes.founder("DUR", seed)).colour());
			assertEquals(PigLook.Colour.BERKSHIRE, PigLook.of(PigGenes.founder("BRK", seed)).colour());
		}
		assertEquals(Species.PIG, Founders.create("DUR", Sex.MALE, 3L, Ruleset.defaults()).genome().species());
	}

	@Test
	void dominantWhiteAndTheMc1rOrder() {
		assertEquals(PigLook.Colour.WHITE, PigLook.of(g(new int[] {1, 0}, new int[] {0, 0})).colour(), "one I is white");
		assertEquals(PigLook.Colour.BLACK, PigLook.of(g(new int[] {0, 0}, new int[] {3, 0})).colour(), "E^D2 over red");
		assertEquals(PigLook.Colour.SPOTTED, PigLook.of(g(new int[] {0, 0}, new int[] {2, 0})).colour(), "E^P with red: black spots");
		assertEquals(PigLook.Colour.RED, PigLook.of(g(new int[] {0, 0}, new int[] {0, 0})).colour());
	}

	@Test
	void looksPackAndGenesSave() {
		PigGenes x = PigGenes.founder("BRK", 9L);
		assertEquals(x, PigGenes.load(x.save()));
		PigLook l = PigLook.of(x);
		assertEquals(l, PigLook.unpack(l.pack()));
	}
}
