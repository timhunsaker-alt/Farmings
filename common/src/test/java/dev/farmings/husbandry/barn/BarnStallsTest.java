package dev.farmings.husbandry.barn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.farmings.husbandry.barn.BarnStalls.Stall;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BarnStallsTest {
	@Test
	void aFourStallBarnHasEightStallsOnTheBedsTheLayoutWants() {
		int driveX = -9;
		List<Stall> stalls = BarnStalls.of(driveX, 4);
		assertEquals(8, stalls.size());
		assertEquals(-8, stalls.get(0).x());
		assertEquals(-6, stalls.get(1).x());
		assertEquals(-2, stalls.get(3).x());
		assertEquals(-3, stalls.get(0).bedZ());
		assertEquals(3, stalls.get(4).bedZ());
		assertEquals(-5, stalls.get(0).bunkZ());
		assertEquals(1, stalls.get(7).alleyZ());
		assertEquals("stall 3 south", stalls.get(6).name());
	}

	@Test
	void everyStallStandsInsideTheCellsTheCheckRequires() {
		// the beds the check wants are at x = driveX + 1 + 2i, z = dir * (2..4); the standing spot z = dir*3 is the middle cell
		for (Stall s : BarnStalls.of(-7, 3)) {
			assertEquals(-7 + 1 + 2 * s.index(), s.x());
			assertEquals(3, Math.abs(s.bedZ()));
			assertEquals(5, Math.abs(s.bunkZ()));
		}
	}

	@Test
	void theNearestFreeStallIsChosenAndOneCowTakesOne() {
		List<Stall> stalls = BarnStalls.of(-5, 2);
		Set<Integer> taken = new HashSet<>();
		Stall first = BarnStalls.nearestFree(stalls, taken, -4, -1);
		assertEquals("stall 1 north", first.name());
		taken.add(first.id());
		Stall second = BarnStalls.nearestFree(stalls, taken, -4, -1);
		assertEquals("stall 2 north", second.name());
		taken.add(second.id());
		taken.add(BarnStalls.nearestFree(stalls, taken, -4, 1).id());
		taken.add(BarnStalls.nearestFree(stalls, taken, -4, 1).id());
		assertEquals(4, taken.size());
		assertNull(BarnStalls.nearestFree(stalls, taken, 0, 0), "every stall taken");
		assertNotNull(BarnStalls.nearestFree(stalls, new HashSet<>(), 0, 0));
	}
}
