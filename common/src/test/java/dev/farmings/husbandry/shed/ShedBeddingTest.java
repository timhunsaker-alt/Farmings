package dev.farmings.husbandry.shed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShedBeddingTest {
	@Test
	void manureRatesFollowTheUkTable() {
		assertEquals(5.0, ShedBedding.manureKgPerDay(80, false));
		assertEquals(3.3, ShedBedding.manureKgPerDay(55, false));
		assertEquals(1.8, ShedBedding.manureKgPerDay(30, true));
	}

	@Test
	void aDayHousedUsesStrawAndSoilsIt() {
		ShedBedding b = new ShedBedding();
		b.addStraw(ShedBedding.LOOSE_STRAW_KG);
		b.housed(80, false, 1.0);
		assertEquals(9.0, b.strawKg(), 1e-9);
		assertEquals(6.0, b.soiledKg(), 1e-9);     // 5 kg manure + 1 kg straw
		assertFalse(b.dirty(true));
	}

	@Test
	void noStrawOrAFullStoreIsDirty() {
		ShedBedding b = new ShedBedding();
		assertTrue(b.dirty(true), "housed sheep with no straw");
		assertFalse(b.dirty(false), "an empty shed isn't dirty");
		b.addStraw(1000);
		for (int d = 0; d < 1000; d++) {
			b.housed(80, false, 1.0);
		}
		assertEquals(ShedBedding.MAX_ITEMS, b.manureItems());
		assertTrue(b.dirty(true), "a full store");
		assertEquals(10, b.takeManure(10));
		assertTrue(b.dirty(true), "mucked out but the straw is all used");
		b.addStraw(ShedBedding.LOOSE_STRAW_KG);
		assertFalse(b.dirty(true), "fresh straw re-beds it");
	}

	@Test
	void strawStoreCaps() {
		ShedBedding b = new ShedBedding();
		assertTrue(b.hasRoomFor(ShedBedding.STRAW_BALE_KG));
		b.addStraw(ShedBedding.STRAW_BALE_KG * 2);
		assertFalse(b.hasRoomFor(ShedBedding.STRAW_BALE_KG));
		assertEquals(ShedBedding.MAX_STRAW_KG - 908, b.addStraw(500), 1e-9);
	}
}
