package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class GeneticsTest {
	static final Ruleset RULES = Ruleset.defaults();

	/** The parents used to take golden values from FarmChain's own TypeScript (see PROGRESS.md). */
	static Genome parent(Map<String, Integer> composition, int offsetA, int offsetB) {
		Map<Trait, AllelePair> core = new EnumMap<>(Trait.class);
		Trait[] traits = Trait.values();
		for (int i = 0; i < traits.length; i++) {
			core.put(traits[i], new AllelePair(AlleleRegistry.alleleId(traits[i], (i + offsetA) % 7), AlleleRegistry.alleleId(traits[i], (i + offsetB) % 7)));
		}
		return new Genome(Genome.SCHEMA_V1, 0, composition, core, RULES.version());
	}

	static Map<String, Integer> comp(Object... kv) {
		Map<String, Integer> m = new TreeMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put((String) kv[i], (Integer) kv[i + 1]);
		}
		return m;
	}

	static Genome dam(Map<String, Integer> c) {
		return parent(c, 0, 3);
	}

	static Genome sire(Map<String, Integer> c) {
		return parent(c, 1, 5);
	}

	// --- parity with FarmChain ---

	@Test
	void rngMatchesFarmChain() {
		Rng r = new Rng(12345);
		assertEquals(0.764439231018, r.next(), 1e-12);
		assertEquals(0.142752525629, r.next(), 1e-12);
		assertEquals(0.684745260514, r.next(), 1e-12);
		assertEquals(516375437L, r.nextUint32());
	}

	@Test
	void breedingSeedMatchesFarmChain() {
		assertEquals(801461197L, BreedingSeed.seed("world-key", "calf-1", "aa", "bb", "v1"));
	}

	@Test
	void compositionAveragingMatchesFarmChain() {
		assertEquals(comp("HOL", 7500, "JER", 2500), BreedComposition.average(comp("HOL", 10000), comp("JER", 5001, "HOL", 4999)));
	}

	@Test
	void calfMatchesFarmChainForFixedSeed() {
		Offspring.Result r = Offspring.create(dam(comp("HOL", 10000)), sire(comp("JER", 10000)), 987654321L, RULES, null);
		assertEquals(Sex.FEMALE, r.sex());
		assertEquals(comp("HOL", 5000, "JER", 5000), r.genome().breedCompositionBp());
		assertEquals(new AllelePair("B_A4", "B_A2"), r.genome().coreGenome().get(Trait.BUTTERFAT));
		assertEquals(new AllelePair("MY_A0", "MY_A1"), r.genome().coreGenome().get(Trait.MILK_YIELD));
		assertEquals(new AllelePair("L_A6", "L_A0"), r.genome().coreGenome().get(Trait.LONGEVITY));
		int[] expected = {779, 696, 688, 756, 626, 647, 679, 680}; // milk, fat, protein, efficiency, health, fertility, longevity, temperament
		for (Trait t : Trait.values()) {
			assertEquals(expected[t.ordinal()], r.phenotype().score(t), t.id);
		}
		assertEquals(10000, r.phenotype().heterosisBp());
		assertEquals(0, r.phenotype().inbreedingBp());
	}

	@Test
	void backcrossCalfMatchesFarmChain() {
		Offspring.Result r = Offspring.create(dam(comp("HOL", 7500, "JER", 2500)), sire(comp("BSW", 10000)), 42L, RULES, null);
		assertEquals(Sex.FEMALE, r.sex());
		assertEquals(comp("BSW", 5000, "HOL", 3750, "JER", 1250), r.genome().breedCompositionBp());
		int[] expected = {846, 638, 685, 717, 686, 635, 780, 750};
		for (Trait t : Trait.values()) {
			assertEquals(expected[t.ordinal()], r.phenotype().score(t), t.id);
		}
	}

	@Test
	void sameCompositionParentsGiveNoHeterosisAndHighSeedWorks() {
		Offspring.Result r = Offspring.create(dam(comp("HOL", 5000, "BSW", 5000)), sire(comp("HOL", 5000, "BSW", 5000)), 4000000000L, RULES, null);
		assertEquals(Sex.MALE, r.sex());
		assertEquals(0, r.phenotype().heterosisBp());
		int[] expected = {871, 561, 631, 651, 639, 550, 699, 750};
		for (Trait t : Trait.values()) {
			assertEquals(expected[t.ordinal()], r.phenotype().score(t), t.id);
		}
	}

	// --- engine properties ---

	@Test
	void compositionAlwaysSumsToTotal() {
		Map<String, Integer> f1 = BreedComposition.average(comp("HOL", 10000), comp("JER", 10000));
		Map<String, Integer> back = BreedComposition.average(f1, comp("HOL", 10000));
		Map<String, Integer> three = BreedComposition.average(back, comp("BSW", 10000));
		Map<String, Integer> odd = BreedComposition.average(comp("HOL", 3333, "JER", 3333, "BSW", 3334), comp("GYR", 10000));
		for (Map<String, Integer> m : java.util.List.of(f1, back, three, odd)) {
			assertTrue(BreedComposition.sumsToTotal(m), m.toString());
		}
	}

	@Test
	void sameEventAndParentsGiveSameCalf() {
		Genome d = dam(comp("HOL", 10000));
		Genome s = sire(comp("JER", 10000));
		Offspring.Result a = Offspring.create(d, s, "calf-1", "world", RULES, null);
		Offspring.Result b = Offspring.create(d, s, "calf-1", "world", RULES, null);
		assertEquals(a.genome().hash(), b.genome().hash());
		assertEquals(a.phenotype(), b.phenotype());
		assertEquals(a.sex(), b.sex());
	}

	@Test
	void differentEventOrWorldCanGiveDifferentCalves() {
		Genome d = dam(comp("HOL", 10000));
		Genome s = sire(comp("JER", 10000));
		long distinct = java.util.stream.IntStream.range(0, 30)
				.mapToObj(i -> Offspring.create(d, s, "calf-" + i, "world", RULES, null).genome().hash()).distinct().count();
		assertTrue(distinct > 1);
		assertNotEquals(BreedingSeed.seed("world-a", "calf-1", "x", "y", "v"), BreedingSeed.seed("world-b", "calf-1", "x", "y", "v"));
	}

	@Test
	void everyCalfAlleleComesFromTheMatchingParent() {
		Genome d = dam(comp("HOL", 10000));
		Genome s = sire(comp("JER", 10000));
		for (int i = 0; i < 50; i++) {
			Genome c = Offspring.create(d, s, "c" + i, "w", RULES, null).genome();
			for (Trait t : Trait.values()) {
				assertTrue(d.coreGenome().get(t).contains(c.coreGenome().get(t).first()));
				assertTrue(s.coreGenome().get(t).contains(c.coreGenome().get(t).second()));
			}
			assertEquals(1, c.generation());
		}
	}

	@Test
	void forcedSexOverridesTheRollButNothingElse() {
		Genome d = dam(comp("HOL", 10000));
		Genome s = sire(comp("JER", 10000));
		for (int i = 0; i < 20; i++) {
			Offspring.Result free = Offspring.create(d, s, "c" + i, "w", RULES, null);
			Offspring.Result girl = Offspring.create(d, s, "c" + i, "w", RULES, Sex.FEMALE);
			Offspring.Result boy = Offspring.create(d, s, "c" + i, "w", RULES, Sex.MALE);
			assertEquals(Sex.FEMALE, girl.sex());
			assertEquals(Sex.MALE, boy.sex());
			assertEquals(free.genome().hash(), girl.genome().hash());
			assertEquals(free.phenotype(), boy.phenotype());
		}
	}

	@Test
	void heterosisIsZeroForIdenticalAndFullForUnrelatedPureBreeds() {
		assertEquals(0, Heterosis.coefficientBp(comp("HOL", 5000, "JER", 5000), comp("HOL", 5000, "JER", 5000)));
		assertEquals(10000, Heterosis.coefficientBp(comp("HOL", 10000), comp("JER", 10000)));
		assertEquals(0, Heterosis.coefficientBp(comp("HOL", 10000), comp("HOL", 10000)));
	}

	@Test
	void fitnessTraitsGainMoreFromHeterosisThanProduction() {
		assertTrue(Heterosis.perTraitBonusBp(10000, Trait.FERTILITY, RULES) > Heterosis.perTraitBonusBp(10000, Trait.MILK_YIELD, RULES));
		assertEquals(0, Heterosis.perTraitBonusBp(10000, Trait.BUTTERFAT, RULES));
		assertEquals(0, Heterosis.perTraitBonusBp(0, Trait.FERTILITY, RULES));
	}

	@Test
	void inbreedingGrowsWithSharedAllelesAndIsNeverABonus() {
		Genome a = dam(comp("HOL", 10000));
		Genome same = dam(comp("HOL", 10000));
		assertEquals(10000, Inbreeding.relatednessBp(a.coreGenome(), same.coreGenome()));
		Genome other = parent(comp("HOL", 10000), 2, 6);
		assertTrue(Inbreeding.relatednessBp(a.coreGenome(), other.coreGenome()) < 10000);
		int last = -1;
		for (int r = 0; r <= 10000; r += 500) {
			int p = Inbreeding.perTraitPenaltyBp(r, Trait.HEALTH, RULES);
			assertTrue(p >= last && p >= 0);
			last = p;
		}
	}

	@Test
	void traitScoresStayInRange() {
		for (int i = 0; i < 200; i++) {
			Founders.Result f = Founders.create("HOL", Sex.FEMALE, i, RULES);
			Founders.Result g = Founders.create("JER", Sex.MALE, 1000 + i, RULES);
			Offspring.Result c = Offspring.create(f.genome(), g.genome(), "e" + i, "w", RULES, null);
			for (Trait t : Trait.values()) {
				assertTrue(c.phenotype().score(t) >= 0 && c.phenotype().score(t) <= 1000);
				assertTrue(f.phenotype().score(t) >= 0 && f.phenotype().score(t) <= 1000);
			}
		}
	}

	@Test
	void foundersAreDeterministicAndPurebred() {
		Founders.Result a = Founders.create("GYR", Sex.FEMALE, 7, RULES);
		Founders.Result b = Founders.create("GYR", Sex.FEMALE, 7, RULES);
		assertEquals(a.genome().hash(), b.genome().hash());
		assertEquals(comp("GYR", 10000), a.genome().breedCompositionBp());
		assertEquals(0, a.genome().generation());
	}

	@Test
	void foundersCentreOnTheirBreedBaseline() {
		long sum = 0;
		for (int i = 0; i < 400; i++) {
			sum += Founders.create("HOL", Sex.FEMALE, i, RULES).phenotype().score(Trait.MILK_YIELD);
		}
		double mean = sum / 400.0;
		assertTrue(Math.abs(mean - 950) < 25, "Holstein milk yield mean " + mean);
		long jer = 0;
		for (int i = 0; i < 400; i++) {
			jer += Founders.create("JER", Sex.FEMALE, i, RULES).phenotype().score(Trait.BUTTERFAT);
		}
		assertTrue(Math.abs(jer / 400.0 - 950) < 25);
	}

	@Test
	void hashIgnoresAlleleOrder() {
		Genome a = dam(comp("HOL", 10000));
		Map<Trait, AllelePair> flipped = new EnumMap<>(Trait.class);
		a.coreGenome().forEach((t, p) -> flipped.put(t, new AllelePair(p.second(), p.first())));
		Genome b = new Genome(a.schema(), a.generation(), a.breedCompositionBp(), flipped, a.rulesetVersion());
		assertEquals(a.hash(), b.hash());
		assertEquals(64, a.hash().length());
	}

	@Test
	void canonicalStringHasSortedKeys() {
		String c = dam(comp("JER", 4000, "HOL", 6000)).canonical();
		assertTrue(c.indexOf("\"HOL\"") < c.indexOf("\"JER\""));
		assertTrue(c.indexOf("\"butterfat\"") < c.indexOf("\"temperament\""));
		assertTrue(c.startsWith("{\"schema\":\"farmings.cattle.genome.v1\""));
	}

	@Test
	void angusAndWagyuHaveNoBaselinesYet() {
		assertTrue(Breeds.known("HOL") && Breeds.known("JER") && Breeds.known("BSW") && Breeds.known("GYR"));
		assertTrue(!Breeds.known("ANG") && !Breeds.known("WAG"));
	}

	@Test
	void savedGenomeComesBackExactly() {
		Offspring.Result r = Offspring.create(dam(comp("HOL", 7500, "JER", 2500)), sire(comp("BSW", 10000)), 42L, RULES, null);
		Genome back = Genome.decode(r.genome().encode());
		assertEquals(r.genome(), back);
		assertEquals(r.genome().hash(), back.hash());
		assertEquals(r.genome().coreGenome().get(Trait.BUTTERFAT), back.coreGenome().get(Trait.BUTTERFAT), "allele order kept");
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Genome.decode("garbage"));
	}
}
