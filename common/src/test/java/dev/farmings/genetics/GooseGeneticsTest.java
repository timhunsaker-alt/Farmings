package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.husbandry.poultry.FowlBreed;
import dev.farmings.husbandry.poultry.LayingCycle;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Goose plumage and laying (docs/animals/poultry.md, "Geese and guinea fowl"). */
class GooseGeneticsTest {
	private static PoultryGenes genes(int[] dilution, int[] spotting) {
		Map<PoultryLocus, int[]> m = new EnumMap<>(PoultryLocus.class);
		m.put(PoultryLocus.GOOSE_DILUTION, dilution);
		m.put(PoultryLocus.GOOSE_SPOTTING, spotting);
		return new PoultryGenes(m);
	}

	@Test
	void dilutionIsSexLinkedLikeThePilgrim() {
		// a gander with two Sd is white, a goose with her one Sd pale grey (the Pilgrim's autosexing)
		assertEquals(PoultryLook.Plumage.GOOSE_WHITE, PoultryLook.of(Species.GOOSE, "TOU", genes(new int[] {1, 1}, new int[] {0, 0}), Sex.MALE).plumage());
		assertEquals(PoultryLook.Plumage.GOOSE_PALE, PoultryLook.of(Species.GOOSE, "TOU", genes(new int[] {1, 0}, new int[] {0, 0}), Sex.FEMALE).plumage());
		assertEquals(PoultryLook.Plumage.GOOSE_GREY, PoultryLook.of(Species.GOOSE, "TOU", genes(new int[] {0, 0}, new int[] {0, 0}), Sex.MALE).plumage());
	}

	@Test
	void spottingAloneIsSaddlebackAndWithDilutionWhite() {
		assertEquals(PoultryLook.Plumage.GOOSE_SADDLEBACK, PoultryLook.of(Species.GOOSE, "TOU", genes(new int[] {0, 0}, new int[] {1, 1}), Sex.MALE).plumage());
		assertEquals(PoultryLook.Plumage.GOOSE_WHITE, PoultryLook.of(Species.GOOSE, "EMB", genes(new int[] {1, 0}, new int[] {1, 1}), Sex.FEMALE).plumage());
		assertEquals(PoultryLook.Plumage.GOOSE_GREY, PoultryLook.of(Species.GOOSE, "TOU", genes(new int[] {0, 0}, new int[] {1, 0}), Sex.MALE).plumage(),
				"spotting is recessive");
	}

	@Test
	void embdenFoundersAreWhiteAndLooksPackInSixBits() {
		for (long seed = 1; seed < 20; seed++) {
			for (Sex sex : Sex.values()) {
				PoultryLook l = PoultryLook.of(Species.GOOSE, "EMB", PoultryGenes.founder("EMB", sex, seed), sex);
				assertEquals(PoultryLook.Plumage.GOOSE_WHITE, l.plumage());
				assertEquals(l, PoultryLook.unpack(l.pack()));
			}
		}
		assertTrue(PoultryLook.Plumage.values().length <= 64);
	}

	@Test
	void geeseLayOnlyInTheirSeason() {
		Map<String, Integer> emb = Map.of("EMB", 10_000);
		assertEquals(121, FowlBreed.EMBDEN.seasonDays());
		assertEquals(0, LayingCycle.eggsPerDay(Species.GOOSE, emb, 400, 500, 273), 1e-12);
		assertEquals(40.0 / 121, LayingCycle.eggsPerDay(Species.GOOSE, emb, 400, 500, 100), 1e-12);
		assertEquals(0, LayingCycle.eggsPerDay(Species.GOOSE, emb, 300, 500, 100), 1e-12, "not before a year old");
		assertFalse(FowlBreed.CHINESE.inLay(200));
		assertTrue(FowlBreed.CHINESE.inLay(31) && FowlBreed.CHINESE.inLay(180));
	}
}
