package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Roan, brindle and PMEL dilution (docs/GENETICS.md, "Coat genes, loose ends"). */
class CoatLooseEndsTest {
	private static VisualGenome with(String breed, Map<VisualLocus, int[]> set) {
		EnumMap<VisualLocus, int[]> m = new EnumMap<>(VisualGenome.founder(breed, 1L).alleles());
		m.putAll(set);
		return new VisualGenome(m);
	}

	private static VisualPhenotype look(VisualGenome g) {
		return VisualPhenotype.of(g, Map.of("HOL", 10_000));
	}

	@Test
	void roanIsIncomplete() {
		assertEquals(0, look(with("JER", Map.of(VisualLocus.ROAN, new int[] {0, 0}))).roan());
		assertEquals(1, look(with("JER", Map.of(VisualLocus.ROAN, new int[] {1, 0}))).roan());
		VisualPhenotype white = look(with("JER", Map.of(VisualLocus.ROAN, new int[] {1, 1})));
		assertEquals(2, white.roan());
		assertTrue(white.describe().startsWith("white"));
	}

	@Test
	void brindleShowsOnlyOnWildType() {
		Map<VisualLocus, int[]> abr = Map.of(VisualLocus.AGOUTI, new int[] {1, 0});
		assertTrue(look(with("JER", Map.of(VisualLocus.AGOUTI, new int[] {1, 0}, VisualLocus.EXTENSION, new int[] {2, 2}))).brindle(), "E+ shows it");
		assertFalse(look(with("JER", Map.of(VisualLocus.AGOUTI, new int[] {1, 1}, VisualLocus.EXTENSION, new int[] {0, 2}))).brindle(), "ED hides it");
		assertFalse(look(with("JER", Map.of(VisualLocus.AGOUTI, new int[] {1, 1}, VisualLocus.EXTENSION, new int[] {3, 3}))).brindle(), "e/e hides it");
		assertFalse(look(with("JER", Map.of(VisualLocus.AGOUTI, new int[] {0, 0}, VisualLocus.EXTENSION, new int[] {2, 2}))).brindle());
		assertNotNull(abr);
	}

	@Test
	void pmelAddsToDilution() {
		VisualGenome none = with("ANG", Map.of(VisualLocus.PMEL, new int[] {0, 0}, VisualLocus.DILUTION, new int[] {0, 0}));
		VisualGenome dun = with("ANG", Map.of(VisualLocus.PMEL, new int[] {1, 0}, VisualLocus.DILUTION, new int[] {0, 0}));
		VisualGenome silver = with("ANG", Map.of(VisualLocus.PMEL, new int[] {1, 1}, VisualLocus.DILUTION, new int[] {0, 0}));
		VisualGenome charolais = with("ANG", Map.of(VisualLocus.PMEL, new int[] {2, 2}, VisualLocus.DILUTION, new int[] {0, 0}));
		assertEquals(0, look(none).dilution());
		assertEquals(1, look(dun).dilution());
		assertEquals(2, look(silver).dilution());
		assertEquals(3, look(charolais).dilution(), "capped at strong");
	}

	@Test
	void sixBreedsCarryNoRoanOrBrindleAndWagyuSomeDh() {
		int dh = 0;
		for (long seed = 0; seed < 1000; seed++) {
			for (String b : new String[] {"HOL", "JER", "BSW", "GYR", "ANG", "WAG"}) {
				VisualGenome g = VisualGenome.founder(b, seed);
				assertEquals(0, g.sum(VisualLocus.ROAN), b);
				assertEquals(0, g.sum(VisualLocus.AGOUTI), b);
				if (!b.equals("WAG")) {
					assertEquals(0, g.sum(VisualLocus.PMEL), b);
				} else if (g.sum(VisualLocus.PMEL) > 0) {
					dh++;
				}
			}
		}
		assertTrue(dh > 120 && dh < 260, "about 19% of Wagyu founders carry Dh: " + dh);
	}

	@Test
	void olderSavesAndOldLooksStayTheSame() {
		VisualGenome g = VisualGenome.founder("HOL", 7L);
		int[] full = g.save();
		int[] old = java.util.Arrays.copyOf(full, (VisualLocus.EARS.ordinal() + 1) * 2);
		VisualGenome back = VisualGenome.load(old);
		assertNotNull(back);
		assertEquals(0, back.sum(VisualLocus.ROAN));
		assertEquals(g.expressed(VisualLocus.EXTENSION), back.expressed(VisualLocus.EXTENSION));
		// founders' existing loci are drawn first, so adding loci didn't change a founder's existing alleles
		VisualPhenotype p = look(g);
		assertEquals(p, VisualPhenotype.unpack(p.pack()));
		VisualPhenotype r = new VisualPhenotype(VisualPhenotype.Coat.WILD, 2, 1, 0, 0, true, 0, 0, 0, VisualPhenotype.BodyFamily.DAIRY, null, 0, 1, true);
		assertEquals(r, VisualPhenotype.unpack(r.pack()));
	}
}
