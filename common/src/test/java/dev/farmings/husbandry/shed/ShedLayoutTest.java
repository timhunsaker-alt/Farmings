package dev.farmings.husbandry.shed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShedLayoutTest {
	final Map<String, ShedLayout.Cell> cells = new HashMap<>();

	void set(int x, int z, ShedLayout.Cell c) {
		cells.put(x + "," + z, c);
	}

	ShedLayout.Grid grid() {
		return (x, y, z) -> y == 0 ? cells.getOrDefault(x + "," + z, ShedLayout.Cell.OTHER) : ShedLayout.Cell.OTHER;
	}

	/** Controller at (0,0), a w x d pen east of it, a rack north of the pen, a trough south. */
	void shed(int w, int d) {
		set(0, 0, ShedLayout.Cell.CONTROLLER);
		for (int x = 1; x <= w; x++) {
			for (int z = 0; z < d; z++) {
				set(x, z, ShedLayout.Cell.PEN);
			}
		}
		set(1, -1, ShedLayout.Cell.HAY_RACK);
		set(2, d, ShedLayout.Cell.TROUGH);
	}

	@Test
	void aPenWithRackAndTroughForms() {
		shed(3, 3);
		ShedLayout.Check c = ShedLayout.check(grid(), 0, 0, 0);
		assertTrue(c.formed(), c.problem());
		assertEquals(9, c.pen().size());
		assertEquals(6, c.capacity());
		assertEquals(1, c.racks().size());
		assertEquals(1, c.troughs().size());
		assertEquals(1, c.lambingWanted());
	}

	@Test
	void problemsAreNamedInOrder() {
		assertEquals("No sheep pen floor beside the controller", ShedLayout.check(grid(), 0, 0, 0).status());
		shed(1, 2);
		assertTrue(ShedLayout.check(grid(), 0, 0, 0).status().startsWith("The pen needs at least 3"));
		cells.clear();
		shed(3, 3);
		cells.remove("1,-1");
		assertEquals("No hay rack beside the pen", ShedLayout.check(grid(), 0, 0, 0).status());
		set(1, -1, ShedLayout.Cell.HAY_RACK);
		cells.remove("2,3");
		assertEquals("No water trough beside the pen", ShedLayout.check(grid(), 0, 0, 0).status());
	}

	@Test
	void lambingPensAndGateArePartOfThePenButTheFloodStopsAtTheGate() {
		shed(3, 3);
		set(4, 1, ShedLayout.Cell.LAMBING_PEN);
		set(2, 3, ShedLayout.Cell.GATE);
		set(2, 4, ShedLayout.Cell.PEN);          // past the gate: not part of this pen
		cells.remove("2,3");
		set(3, 3, ShedLayout.Cell.TROUGH);
		set(2, 3, ShedLayout.Cell.GATE);
		ShedLayout.Check c = ShedLayout.check(grid(), 0, 0, 0);
		assertTrue(c.formed(), c.problem());
		assertEquals(1, c.lambing().size());
		assertEquals(1, c.gates().size());
		assertEquals(9, c.pen().size(), "the pen cell beyond the gate is not counted");
	}

	@Test
	void aHugePenIsRefused() {
		shed(20, 20);
		assertTrue(ShedLayout.check(grid(), 0, 0, 0).status().startsWith("The pen is larger"));
	}

	// ---- pasture ----

	static PastureLayout.Ground field(int w, int d, boolean fenced, int gateX, int gateZ) {
		return new PastureLayout.Ground() {
			@Override
			public PastureLayout.Spot at(int x, int z, int nearY) {
				if (x == gateX && z == gateZ) {
					return new PastureLayout.Spot(x, 0, z, false);
				}
				boolean inside = x >= 0 && x < w && z >= 1 && z <= d;
				if (inside) {
					return new PastureLayout.Spot(x, x == w - 1 ? 1 : 0, z, (x + z) % 3 != 0);   // a one-block rise at the far side
				}
				return fenced ? null : new PastureLayout.Spot(x, 0, z, true);
			}

			@Override
			public boolean shed(int x, int y, int z) {
				return z <= 0;
			}
		};
	}

	@Test
	void theFencedFieldPastTheGateIsThePasture() {
		PastureLayout.Pasture p = PastureLayout.find(field(10, 8, true, 3, 0), 3, 0, 0);
		assertTrue(p.found(), p.problem());
		assertEquals(80, p.spots().size());
		assertTrue(p.grass() > 40);
	}

	@Test
	void anOpenFieldIsNotFencedIn() {
		PastureLayout.Pasture p = PastureLayout.find(field(10, 8, false, 3, 0), 3, 0, 0);
		assertFalse(p.found());
		assertTrue(p.problem().contains("not fenced in"));
	}
}
