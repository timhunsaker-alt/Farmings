package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.parlor.BulkTank;
import dev.farmings.husbandry.parlor.MilkKind;
import org.junit.jupiter.api.Test;

/** The new-animals groundwork (docs/animals/goats.md): a new species is never silently taken for another. */
class SpeciesGroundworkTest {
	@Test
	void mammalsAreNotBirdsAndRunOnTheLivestockClock() {
		for (Species s : Species.values()) {
			assertSame(s.fowl() ? TimeScale.FOWL : TimeScale.LIVESTOCK, TimeScale.of(s), s.name());
		}
		assertFalse(Species.CATTLE.fowl());
		assertFalse(Species.SHEEP.fowl());
		assertTrue(Species.DUCK.fowl());
	}

	@Test
	void anUnknownBreedCodeIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> Breeds.species("ZZZ"));
		assertEquals(Species.CATTLE, Breeds.species("HOL"));
		assertEquals(Species.SHEEP, Breeds.species("MER"));
	}

	@Test
	void milkKindLoadsOldSavesAndNames() {
		assertEquals(MilkKind.SHEEP, MilkKind.load("", true));
		assertEquals(MilkKind.COW, MilkKind.load("", false));
		assertEquals(MilkKind.GOAT, MilkKind.load("GOAT", false));
		assertEquals("TANK HOLDS GOAT MILK", MilkKind.GOAT.holdsFault());
	}

	@Test
	void aTankHoldsOneKindAndChangesOnlyWhenEmpty() {
		BulkTank t = new BulkTank();
		assertEquals(10, t.add(10, 35, 3.2, 2.8, MilkKind.GOAT), 1e-9);
		assertTrue(t.holds(MilkKind.GOAT));
		assertEquals(0, t.add(10, 35, 5.68, 4.92, MilkKind.SHEEP), 1e-9);
		t.take(10);
		assertEquals(MilkKind.COW, t.kind(), "an empty tank reads as cow and takes any kind");
		assertTrue(t.accepts(MilkKind.SHEEP));
	}
}
