package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import org.junit.jupiter.api.Test;

/** Muscovy and Coturnix colour mutants (docs/animals/poultry.md, "Colour mutants"). */
class PoultryMutantTest {
	private static PoultryGenes with(String breed, Sex sex, PoultryLocus l, int a, int b) {
		PoultryGenes base = PoultryGenes.founder(breed, sex, 1L);
		EnumMap<PoultryLocus, int[]> m = new EnumMap<>(PoultryLocus.class);
		for (PoultryLocus k : PoultryLocus.values()) {
			m.put(k, k == l ? new int[] {a, b} : new int[] {0, 0});
		}
		m.put(PoultryLocus.SHELL, base.alleles().get(PoultryLocus.SHELL));
		return new PoultryGenes(m);
	}

	private static PoultryLook.Plumage look(String breed, Species sp, Sex sex, PoultryLocus l, int a, int b) {
		return PoultryLook.of(sp, breed, with(breed, sex, l, a, b), sex).plumage();
	}

	@Test
	void muscovyBlueIsIncompleteAndLavenderRecessive() {
		assertEquals(PoultryLook.Plumage.MUSCOVY, look("MUS", Species.DUCK, Sex.MALE, PoultryLocus.MUSCOVY_BLUE, 0, 0));
		assertEquals(PoultryLook.Plumage.MUSCOVY_BLUE, look("MUS", Species.DUCK, Sex.MALE, PoultryLocus.MUSCOVY_BLUE, 1, 0));
		assertEquals(PoultryLook.Plumage.MUSCOVY_SILVER, look("MUS", Species.DUCK, Sex.MALE, PoultryLocus.MUSCOVY_BLUE, 1, 1));
		assertEquals(PoultryLook.Plumage.MUSCOVY, look("MUS", Species.DUCK, Sex.MALE, PoultryLocus.MUSCOVY_LAVENDER, 1, 0));
		assertEquals(PoultryLook.Plumage.MUSCOVY_LAVENDER, look("MUS", Species.DUCK, Sex.MALE, PoultryLocus.MUSCOVY_LAVENDER, 1, 1));
	}

	@Test
	void chocolateIsSexLinked() {
		// a hen shows her one Z; a drake needs two
		assertEquals(PoultryLook.Plumage.MUSCOVY_CHOCOLATE, look("MUS", Species.DUCK, Sex.FEMALE, PoultryLocus.MUSCOVY_CHOCOLATE, 1, 0));
		assertEquals(PoultryLook.Plumage.MUSCOVY, look("MUS", Species.DUCK, Sex.MALE, PoultryLocus.MUSCOVY_CHOCOLATE, 1, 0));
		assertEquals(PoultryLook.Plumage.MUSCOVY_CHOCOLATE, look("MUS", Species.DUCK, Sex.MALE, PoultryLocus.MUSCOVY_CHOCOLATE, 1, 1));
	}

	@Test
	void coturnixMutants() {
		assertEquals(PoultryLook.Plumage.ITALIAN, look("COT", Species.QUAIL, Sex.FEMALE, PoultryLocus.QUAIL_FAWN, 1, 0));
		assertEquals(PoultryLook.Plumage.GOLDEN, look("COT", Species.QUAIL, Sex.FEMALE, PoultryLocus.QUAIL_FAWN, 1, 1));
		assertEquals(PoultryLook.Plumage.ROSETTA, look("COT", Species.QUAIL, Sex.FEMALE, PoultryLocus.QUAIL_BROWN, 0, 1));
		assertEquals(PoultryLook.Plumage.TIBETAN, look("COT", Species.QUAIL, Sex.FEMALE, PoultryLocus.QUAIL_BROWN, 1, 1));
		assertEquals(PoultryLook.Plumage.COTURNIX, look("COT", Species.QUAIL, Sex.FEMALE, PoultryLocus.QUAIL_WHITE, 1, 0));
		assertEquals(PoultryLook.Plumage.ENGLISH_WHITE, look("COT", Species.QUAIL, Sex.FEMALE, PoultryLocus.QUAIL_WHITE, 1, 1));
		// Bobwhite never shows a Coturnix mutant
		assertEquals(PoultryLook.Plumage.BOBWHITE, look("BOB", Species.QUAIL, Sex.FEMALE, PoultryLocus.QUAIL_FAWN, 1, 1));
	}

	@Test
	void twoBlueParentsGiveAllThreeKinds() {
		PoultryGenes dam = with("MUS", Sex.FEMALE, PoultryLocus.MUSCOVY_BLUE, 1, 0);
		PoultryGenes sire = with("MUS", Sex.MALE, PoultryLocus.MUSCOVY_BLUE, 0, 1);
		int[] seen = new int[3];
		for (long seed = 0; seed < 400; seed++) {
			seen[PoultryGenes.inherit(dam, sire, Sex.MALE, seed).count(PoultryLocus.MUSCOVY_BLUE, Sex.MALE)]++;
		}
		// about 1:2:1
		assertTrue(seen[0] > 60 && seen[1] > 140 && seen[2] > 60, java.util.Arrays.toString(seen));
	}

	@Test
	void foundersCarryMutantsRarely() {
		int carriers = 0;
		for (long seed = 0; seed < 1000; seed++) {
			if (PoultryGenes.founder("MUS", Sex.MALE, seed).count(PoultryLocus.MUSCOVY_BLUE, Sex.MALE) > 0) {
				carriers++;
			}
			assertEquals(0, PoultryGenes.founder("PEK", Sex.MALE, seed).count(PoultryLocus.MUSCOVY_BLUE, Sex.MALE));
		}
		// 5% an allele, two draws: about 9.75% carry one
		assertTrue(carriers > 60 && carriers < 140, "carriers " + carriers);
		int silver = 0;
		for (long seed = 0; seed < 4000; seed++) {
			if (PoultryLook.of(Species.DUCK, "MUS", PoultryGenes.founder("MUS", Sex.FEMALE, seed), Sex.FEMALE).plumage() == PoultryLook.Plumage.MUSCOVY_SILVER) {
				silver++;
			}
		}
		// 0.25% expected: the two draws are independent
		assertTrue(silver < 40, "silver founders " + silver + " of 4000");
	}

	@Test
	void oldSavesLoadWithWildTypeMutants() {
		int[] full = PoultryGenes.founder("COT", Sex.FEMALE, 4L).save();
		int[] old = java.util.Arrays.copyOf(full, (PoultryLocus.SHELL.ordinal() + 1) * 2);
		PoultryGenes g = PoultryGenes.load(old);
		assertNotNull(g);
		assertEquals(0, g.count(PoultryLocus.QUAIL_FAWN, Sex.FEMALE));
		assertEquals(full[PoultryLocus.SHELL.ordinal() * 2], g.alleles().get(PoultryLocus.SHELL)[0]);
	}

	@Test
	void mutantLooksSurviveSyncPacking() {
		for (PoultryLook.Plumage p : PoultryLook.Plumage.values()) {
			PoultryLook l = new PoultryLook(p, PoultryLook.Comb.WALNUT, true, 777, -123456);
			assertEquals(l, PoultryLook.unpack(l.pack()));
		}
	}
}
