package dev.farmings.husbandry.lane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.husbandry.lane.LanePath.Cell;
import dev.farmings.husbandry.lane.LanePath.Pos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LanePathTest {
	/**
	 * Three parlor stalls along x 0..2 at z 0..1, a 2-row pen at z 2..3, a lane from (0, 4) south to (0, 9), then a short alley at z 10
	 * running east with the barn controller one up beside its end.
	 */
	static final class Drawing {
		final Map<Pos, Cell> cells = new HashMap<>();
		final List<Pos> stalls = new ArrayList<>();

		Drawing() {
			for (int x = 0; x < 3; x++) {
				for (int z = 0; z <= 1; z++) {
					set(x, 0, z, Cell.STALL);
					stalls.add(new Pos(x, 0, z));
				}
				set(x, 0, 2, Cell.PEN);
				set(x, 0, 3, Cell.PEN);
			}
			for (int z = 4; z <= 9; z++) {
				set(0, 0, z, Cell.LANE);
			}
			for (int x = -2; x <= 4; x++) {
				set(x, 0, 10, Cell.ALLEY);
			}
			set(5, 1, 10, Cell.BARN_CONTROLLER);
		}

		void set(int x, int y, int z, Cell c) {
			cells.put(new Pos(x, y, z), c);
		}

		LanePath.Link find() {
			return LanePath.find((x, y, z) -> cells.getOrDefault(new Pos(x, y, z), Cell.OTHER), stalls, 3);
		}
	}

	@Test
	void penLaneAndBarnLink() {
		LanePath.Link l = new Drawing().find();
		assertTrue(l.linked(), l.problem());
		assertEquals(6, l.pen().size());
		assertEquals(4, l.penCapacity(), "6 m2 at 1.39 m2 a cow");
		assertEquals(new Pos(5, 1, 10), l.barnController());
		// the alley exit, six lane cells from the barn end, then the pen cell
		assertEquals(8, l.route().size(), l.route().toString());
		assertEquals(new Pos(0, 0, 10), l.route().get(0));
		assertEquals(new Pos(0, 0, 9), l.route().get(1));
		assertEquals(new Pos(0, 0, 4), l.route().get(6));
		assertEquals(new Pos(0, 0, 3), l.route().get(7));
	}

	@Test
	void penSizeFollows15SquareFeetACow() {
		assertEquals(3, LanePath.penCellsNeeded(2));
		assertEquals(5, LanePath.penCellsNeeded(3));
		assertEquals(17, LanePath.penCellsNeeded(12));
		Drawing d = new Drawing();
		d.cells.remove(new Pos(1, 0, 3));
		d.cells.remove(new Pos(2, 0, 3));
		assertEquals("The holding pen has 4 cells; 3 stalls need 5 (15 sq ft a cow)", d.find().problem());
	}

	@Test
	void eachMissingPieceIsNamed() {
		Drawing d = new Drawing();
		for (int x = 0; x < 3; x++) {
			d.cells.remove(new Pos(x, 0, 2));
		}
		assertEquals("No holding pen beside the parlor stalls", d.find().problem());
		d = new Drawing();
		d.cells.remove(new Pos(0, 0, 4));
		assertEquals("No cow lane leaves the holding pen", d.find().problem());
		d = new Drawing();
		d.cells.remove(new Pos(0, 0, 7));
		assertTrue(d.find().problem().startsWith("The cow lane does not reach a barn alley"), d.find().problem());
		d = new Drawing();
		d.cells.remove(new Pos(5, 1, 10));
		assertEquals("The cow lane reaches an alley with no barn controller", d.find().problem());
	}

	@Test
	void theLaneMayStepOneBlockAndTakesTheShortestWay() {
		Drawing d = new Drawing();
		// lift the lane's last three cells and the alley one block, like a barn built on a rise
		for (int z = 7; z <= 9; z++) {
			d.cells.remove(new Pos(0, 0, z));
			d.set(0, 1, z, Cell.LANE);
		}
		for (int x = -2; x <= 4; x++) {
			d.cells.remove(new Pos(x, 0, 10));
			d.set(x, 1, 10, Cell.ALLEY);
		}
		d.cells.remove(new Pos(5, 1, 10));
		d.set(5, 2, 10, Cell.BARN_CONTROLLER);
		// and a longer second lane that should not be chosen
		for (int z = 4; z <= 9; z++) {
			d.set(2, 0, z, Cell.LANE);
		}
		d.set(2, 0, 9, Cell.LANE);
		LanePath.Link l = d.find();
		assertTrue(l.linked(), l.problem());
		assertEquals(8, l.route().size());
	}

	@Test
	void aLaneLongerThan64CellsDoesNotLink() {
		Drawing d = new Drawing();
		d.cells.keySet().removeIf(p -> d.cells.get(p) == Cell.LANE || d.cells.get(p) == Cell.ALLEY || d.cells.get(p) == Cell.BARN_CONTROLLER);
		for (int z = 4; z <= 4 + 65; z++) {
			d.set(0, 0, z, Cell.LANE);
		}
		d.set(0, 0, 70, Cell.ALLEY);
		d.set(1, 1, 70, Cell.BARN_CONTROLLER);
		assertFalse(d.find().linked());
	}

	@Test
	void aCowStartsFromTheNearestWaypoint() {
		List<Pos> route = new Drawing().find().route();
		assertEquals(0, LanePath.startIndex(route, 0.5, 10.5));
		assertEquals(7, LanePath.startIndex(route, 1.5, 3.2));
		assertEquals(4, LanePath.startIndex(route, 0.4, 6.6));
	}
}
