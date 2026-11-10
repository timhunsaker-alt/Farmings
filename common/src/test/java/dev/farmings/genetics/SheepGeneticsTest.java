package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SheepGeneticsTest {
	static final Ruleset RULES = Ruleset.defaults();

	@Test
	void sheepFoundersCarryTheSheepTraits() {
		Founders.Result m = Founders.create("MER", Sex.FEMALE, 5L, RULES);
		assertEquals(Species.SHEEP, m.genome().species());
		assertEquals(Genome.SCHEMA_SHEEP_V1, m.genome().schema());
		assertEquals(Species.SHEEP.traits().size(), m.genome().coreGenome().size());
		assertTrue(m.genome().coreGenome().containsKey(Trait.WOOL_YIELD));
		assertFalse(m.genome().coreGenome().containsKey(Trait.BUTTERFAT));
		assertTrue(m.phenotype().score(Trait.WOOL_YIELD) > 800, "a Merino's wool score sits near its 95 baseline");
		Founders.Result d = Founders.create("DOR", Sex.FEMALE, 5L, RULES);
		assertTrue(d.phenotype().score(Trait.WOOL_YIELD) < 250);
		assertTrue(d.phenotype().score(Trait.HARDINESS) > 800);
	}

	@Test
	void sheepGenomesRoundTripAndAreDeterministic() {
		Founders.Result dam = Founders.create("EFR", Sex.FEMALE, 1L, RULES);
		Founders.Result ram = Founders.create("SUF", Sex.MALE, 2L, RULES);
		Offspring.Result a = Offspring.create(dam.genome(), ram.genome(), "lamb-1", "w", RULES, null);
		Offspring.Result b = Offspring.create(dam.genome(), ram.genome(), "lamb-1", "w", RULES, null);
		assertEquals(a.genome().hash(), b.genome().hash());
		assertEquals(Species.SHEEP, a.genome().species());
		assertEquals(Map.of("EFR", 5000, "SUF", 5000), a.genome().breedCompositionBp());
		assertEquals(a.genome(), Genome.decode(a.genome().encode()));
	}

	@Test
	void cattleAndSheepCannotBreed() {
		Founders.Result cow = Founders.create("HOL", Sex.FEMALE, 1L, RULES);
		Founders.Result ram = Founders.create("MER", Sex.MALE, 2L, RULES);
		assertThrows(IllegalArgumentException.class, () -> Offspring.create(cow.genome(), ram.genome(), "x", "w", RULES, null));
	}

	static SheepGenes genes(SheepLocus l, int a, int b) {
		Map<SheepLocus, int[]> m = new EnumMap<>(SheepLocus.class);
		m.put(l, new int[] {a, b});
		return new SheepGenes(m);
	}

	@Test
	void whiteIsDominantAndBlackOrBrownAreRecessive() {
		assertEquals(SheepLook.Colour.WHITE, SheepLook.of(genes(SheepLocus.AGOUTI, 0, 1), Sex.FEMALE).colour());
		Map<SheepLocus, int[]> m = new EnumMap<>(SheepLocus.class);
		m.put(SheepLocus.AGOUTI, new int[] {1, 1});
		assertEquals(SheepLook.Colour.BLACK, SheepLook.of(new SheepGenes(m), Sex.FEMALE).colour());
		m.put(SheepLocus.BROWN, new int[] {1, 1});
		assertEquals(SheepLook.Colour.BROWN, SheepLook.of(new SheepGenes(m), Sex.FEMALE).colour());
		m.put(SheepLocus.EXTENSION, new int[] {0, 1});
		assertEquals(SheepLook.Colour.BLACK, SheepLook.of(new SheepGenes(m), Sex.FEMALE).colour(), "dominant black masks agouti");
	}

	@Test
	void hornsAreSexInfluenced() {
		assertEquals(2, SheepLook.of(genes(SheepLocus.HORNS, 1, 1), Sex.FEMALE).horns());
		assertEquals(1, SheepLook.of(genes(SheepLocus.HORNS, 0, 1), Sex.MALE).horns(), "a P/H ram grows scurs");
		assertEquals(0, SheepLook.of(genes(SheepLocus.HORNS, 0, 1), Sex.FEMALE).horns(), "a P/H ewe is polled");
		assertEquals(0, SheepLook.of(genes(SheepLocus.HORNS, 0, 0), Sex.MALE).horns());
	}

	@Test
	void foundersLookLikeTheirBreed() {
		for (long seed = 0; seed < 40; seed++) {
			SheepLook suf = SheepLook.of(SheepGenes.founder("SUF", seed), Sex.FEMALE);
			assertEquals(2, suf.head(), "Suffolk black head");
			assertEquals(0, suf.horns());
			SheepLook dor = SheepLook.of(SheepGenes.founder("DOR", seed), Sex.FEMALE);
			assertTrue(dor.sheds());
			assertEquals(SheepLook.Colour.WHITE, dor.colour());
			assertEquals(2, SheepLook.of(SheepGenes.founder("MER", seed), Sex.MALE).horns(), "Merino rams horned");
		}
	}

	@Test
	void aMerinoByDorperCrossHasPartHairAndAMottledHeadOnlyFromSuffolkOrDorper() {
		SheepGenes lamb = SheepGenes.inherit(SheepGenes.founder("MER", 1), SheepGenes.founder("DOR", 2), 77L);
		SheepLook look = SheepLook.of(lamb, Sex.FEMALE);
		assertEquals(1, look.coat());
		assertEquals(1, look.head());
		assertEquals(0.5, look.fleeceShare());
	}

	@Test
	void recessiveColourAppearsInAboutAQuarterOfCarrierMatings() {
		Map<SheepLocus, int[]> carrier = new EnumMap<>(SheepLocus.class);
		carrier.put(SheepLocus.AGOUTI, new int[] {0, 1});
		SheepGenes c = new SheepGenes(carrier);
		int black = 0;
		int n = 4000;
		for (int i = 0; i < n; i++) {
			if (SheepLook.of(SheepGenes.inherit(c, c, i * 7919L), Sex.FEMALE).colour() != SheepLook.Colour.WHITE) {
				black++;
			}
		}
		assertEquals(0.25, black / (double) n, 0.03);
	}

	@Test
	void lookPacksAndSavesExactly() {
		SheepGenes g = SheepGenes.inherit(SheepGenes.founder("SUF", 3), SheepGenes.founder("EFR", 4), 9L);
		assertEquals(g, SheepGenes.load(g.save()));
		SheepLook look = SheepLook.of(g, Sex.MALE);
		assertEquals(look, SheepLook.unpack(look.pack()));
	}
}
