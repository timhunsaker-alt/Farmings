package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PoultryGeneticsTest {
	static final Ruleset RULES = Ruleset.defaults();

	@Test
	void everyBreedBelongsToItsSpecies() {
		Map<String, Species> want = Map.ofEntries(Map.entry("LEG", Species.CHICKEN), Map.entry("RIR", Species.CHICKEN),
				Map.entry("PLR", Species.CHICKEN), Map.entry("SLK", Species.CHICKEN), Map.entry("BBW", Species.TURKEY),
				Map.entry("BRZ", Species.TURKEY), Map.entry("BRD", Species.TURKEY), Map.entry("COT", Species.QUAIL),
				Map.entry("BOB", Species.QUAIL), Map.entry("PEK", Species.DUCK), Map.entry("KHC", Species.DUCK), Map.entry("MAL", Species.DUCK),
				Map.entry("MUS", Species.DUCK));
		want.forEach((code, sp) -> {
			Founders.Result r = Founders.create(code, Sex.FEMALE, 3L, RULES);
			assertEquals(sp, r.genome().species(), code);
			assertTrue(sp.fowl());
			assertTrue(r.genome().coreGenome().containsKey(Trait.EGG_PRODUCTION));
			assertFalse(r.genome().coreGenome().containsKey(Trait.MILK_YIELD));
			assertEquals(r.genome(), Genome.decode(r.genome().encode()), code);
		});
		assertFalse(Species.CATTLE.fowl());
		assertTrue(Founders.create("LEG", Sex.FEMALE, 3L, RULES).phenotype().score(Trait.EGG_PRODUCTION)
				> Founders.create("SLK", Sex.FEMALE, 3L, RULES).phenotype().score(Trait.EGG_PRODUCTION));
		assertTrue(Founders.create("SLK", Sex.FEMALE, 3L, RULES).phenotype().score(Trait.BROODINESS) > 800);
	}

	@Test
	void crossesStayWithinASpecies() {
		Founders.Result hen = Founders.create("RIR", Sex.FEMALE, 1L, RULES);
		Founders.Result cock = Founders.create("LEG", Sex.MALE, 2L, RULES);
		Offspring.Result a = Offspring.create(hen.genome(), cock.genome(), "chick-1", "w", RULES, null);
		assertEquals(Species.CHICKEN, a.genome().species());
		assertEquals(Map.of("RIR", 5000, "LEG", 5000), a.genome().breedCompositionBp());
		Founders.Result drake = Founders.create("PEK", Sex.MALE, 2L, RULES);
		assertThrows(IllegalArgumentException.class, () -> Offspring.create(hen.genome(), drake.genome(), "x", "w", RULES, null));
	}

	static PoultryLook look(String breed, Sex sex) {
		Species sp = Breeds.species(breed);
		return PoultryLook.of(sp, breed, PoultryGenes.founder(breed, sex, 9L), sex);
	}

	@Test
	void foundersLookLikeTheirBreed() {
		assertEquals(PoultryLook.Plumage.WHITE, look("LEG", Sex.FEMALE).plumage());
		assertEquals(PoultryLook.Plumage.RED, look("RIR", Sex.FEMALE).plumage());
		assertEquals(PoultryLook.Plumage.BARRED, look("PLR", Sex.FEMALE).plumage());
		assertEquals(PoultryLook.Plumage.BARRED, look("PLR", Sex.MALE).plumage());
		PoultryLook silkie = look("SLK", Sex.FEMALE);
		assertEquals(PoultryLook.Plumage.WHITE, silkie.plumage());
		assertTrue(silkie.silky());
		assertEquals(PoultryLook.Comb.WALNUT, silkie.comb());
		assertEquals(PoultryLook.Plumage.WHITE, look("BBW", Sex.MALE).plumage());
		assertEquals(PoultryLook.Plumage.BRONZE, look("BRZ", Sex.MALE).plumage());
		assertEquals(PoultryLook.Plumage.BOURBON, look("BRD", Sex.FEMALE).plumage());
		assertEquals(PoultryLook.Plumage.COTURNIX, look("COT", Sex.FEMALE).plumage());
		assertEquals(PoultryLook.Plumage.BOBWHITE, look("BOB", Sex.FEMALE).plumage());
		assertEquals(PoultryLook.Plumage.WHITE, look("PEK", Sex.FEMALE).plumage());
		assertEquals(PoultryLook.Plumage.KHAKI, look("KHC", Sex.FEMALE).plumage());
		assertEquals(PoultryLook.Plumage.KHAKI, look("KHC", Sex.MALE).plumage());
		assertEquals(PoultryLook.Plumage.MALLARD, look("MAL", Sex.MALE).plumage());
		assertEquals(PoultryLook.Plumage.MUSCOVY, look("MUS", Sex.FEMALE).plumage());
		assertEquals("white", PoultryLook.shellColour(look("LEG", Sex.FEMALE).shell()));
		assertEquals("brown", PoultryLook.shellColour(look("RIR", Sex.FEMALE).shell()));
	}

	@Test
	void barringIsSexLinked() {
		// A non-barred rooster over a barred hen: sons are barred, daughters are not (the hen's Z goes to her sons only).
		PoultryGenes cock = PoultryGenes.founder("RIR", Sex.MALE, 1L);
		PoultryGenes hen = PoultryGenes.founder("PLR", Sex.FEMALE, 2L);
		for (long seed = 0; seed < 20; seed++) {
			PoultryGenes son = PoultryGenes.inherit(hen, cock, Sex.MALE, seed);
			PoultryGenes daughter = PoultryGenes.inherit(hen, cock, Sex.FEMALE, seed);
			assertEquals(1, son.expressed(PoultryLocus.BARRING, Sex.MALE));
			assertEquals(0, daughter.expressed(PoultryLocus.BARRING, Sex.FEMALE));
		}
	}

	@Test
	void looksAndGenesSurviveSaving() {
		PoultryGenes g = PoultryGenes.founder("SLK", Sex.MALE, 4L);
		assertEquals(g, PoultryGenes.load(g.save()));
		PoultryLook l = PoultryLook.of(Species.CHICKEN, "SLK", g, Sex.MALE);
		assertEquals(l, PoultryLook.unpack(l.pack()));
	}
}
