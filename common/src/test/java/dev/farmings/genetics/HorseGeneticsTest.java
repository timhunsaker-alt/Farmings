package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Horse MSTN and coat genes against docs/animals/horses.md. */
class HorseGeneticsTest {
	private static HorseGenes blank() {
		return HorseGenes.founder("TB", 1).with(HorseLocus.E, 1, 1).with(HorseLocus.A, 1, 1).with(HorseLocus.CR, 0, 0).with(HorseLocus.D, 0, 0)
				.with(HorseLocus.G, 0, 0).with(HorseLocus.RN, 0, 0).with(HorseLocus.SB1, 0, 0);
	}

	@Test
	void baseColours() {
		assertEquals("bay", HorseLook.of(blank(), 5).colourName());
		assertEquals("black", HorseLook.of(blank().with(HorseLocus.A, 0, 0), 5).colourName());
		assertEquals("chestnut", HorseLook.of(blank().with(HorseLocus.E, 0, 0), 5).colourName());
	}

	@Test
	void creamAndDun() {
		HorseGenes chestnut = blank().with(HorseLocus.E, 0, 0);
		assertEquals("palomino", HorseLook.of(chestnut.with(HorseLocus.CR, 1, 0), 5).colourName());
		assertEquals("cremello", HorseLook.of(chestnut.with(HorseLocus.CR, 1, 1), 5).colourName());
		assertEquals("buckskin", HorseLook.of(blank().with(HorseLocus.CR, 0, 1), 5).colourName());
		assertEquals("grullo", HorseLook.of(blank().with(HorseLocus.A, 0, 0).with(HorseLocus.D, 1, 0), 5).colourName());
	}

	@Test
	void greyComesWithAge() {
		HorseGenes g = blank().with(HorseLocus.G, 1, 0);
		assertEquals(0, HorseLook.of(g, 0.5).grey());
		assertEquals(3, HorseLook.of(g, 7).grey());
		assertTrue(HorseLook.of(g, 7).describe().startsWith("grey"));
	}

	@Test
	void packRoundTrips() {
		HorseLook l = HorseLook.of(blank().with(HorseLocus.SB1, 1, 1).with(HorseLocus.RN, 1, 0).with(HorseLocus.D, 0, 1), 2);
		assertEquals(l, HorseLook.unpack(l.pack()));
	}

	@Test
	void foundersFollowTheBreedShares() {
		int cQh = 0;
		int cAra = 0;
		int greyAra = 0;
		for (int i = 0; i < 2000; i++) {
			cQh += HorseGenes.founder("QH", i).count(HorseLocus.MSTN);
			HorseGenes a = HorseGenes.founder("ARA", i);
			cAra += a.count(HorseLocus.MSTN);
			greyAra += a.has(HorseLocus.G) ? 1 : 0;
		}
		assertEquals(0.90, cQh / 4000.0, 0.03);
		assertEquals(0.05, cAra / 4000.0, 0.02);
		assertEquals(0.30, greyAra / 2000.0, 0.04);
	}

	@Test
	void inheritanceTakesOneAlleleFromEachParent() {
		HorseGenes dam = blank().with(HorseLocus.MSTN, 1, 1);
		HorseGenes sire = blank().with(HorseLocus.MSTN, 0, 0);
		for (long s = 0; s < 20; s++) {
			assertEquals("C/T", HorseGenes.inherit(dam, sire, s).mstn());
		}
	}

	@Test
	void horseGenomeCarriesSpeedAndStamina() {
		Founders.Result f = Founders.create("ARA", Sex.FEMALE, 7, Ruleset.defaults());
		assertEquals(Species.HORSE, f.genome().species());
		assertTrue(f.phenotype().score(Trait.STAMINA) > f.phenotype().score(Trait.SPEED) - 400);
		assertEquals(f.genome(), Genome.decode(f.genome().encode()));
	}
}
