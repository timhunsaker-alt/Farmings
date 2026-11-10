package dev.farmings.process.coldroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ColdRoomLayoutTest {
	/** A panel box with inside (0..w-1, 0..h-1, 0..l-1), the evaporator in the west wall facing east, a door in the south wall, a condenser outside the west wall. */
	static final class Box {
		final Map<String, ColdRoomPart> cells = new HashMap<>();

		Box(int w, int h, int l) {
			for (int x = -1; x <= w; x++) {
				for (int y = -1; y <= h; y++) {
					for (int z = -1; z <= l; z++) {
						boolean in = x >= 0 && x < w && y >= 0 && y < h && z >= 0 && z < l;
						if (!in) {
							set(x, y, z, ColdRoomPart.PANEL);
						}
					}
				}
			}
			set(-1, 0, 0, ColdRoomPart.EVAPORATOR);
			set(0, 0, l, ColdRoomPart.DOOR);
			set(-2, 0, 0, ColdRoomPart.CONDENSER);
		}

		void set(int x, int y, int z, ColdRoomPart p) {
			cells.put(x + "," + y + "," + z, p);
		}

		ColdRoomCheck check() {
			return ColdRoomLayout.check((x, y, z) -> cells.getOrDefault(x + "," + y + "," + z, ColdRoomPart.OTHER), -1, 0, 0, 1, 0);
		}
	}

	@Test
	void boxesFromSmallestToLargestForm() {
		for (int[] s : new int[][] {{1, 1, 1}, {3, 2, 3}, {10, 4, 10}, {4, 3, 9}}) {
			ColdRoomCheck c = new Box(s[0], s[1], s[2]).check();
			assertTrue(c.formed(), c.problem());
			assertEquals(s[0] * s[1] * s[2], c.volumeM3());
		}
	}

	@Test
	void aGapIsNamedAndABigRoomIsRefused() {
		Box b = new Box(4, 2, 4);
		b.cells.remove("2,2,2");       // a hole in the ceiling
		ColdRoomCheck c = b.check();
		assertFalse(c.formed());
		assertTrue(c.problem().startsWith("The walls are not sealed"), c.problem());
		assertTrue(new Box(11, 2, 4).check().problem().startsWith("The walls are not sealed") || new Box(11, 2, 4).check().problem().startsWith("Too big"));
		assertFalse(new Box(4, 5, 4).check().formed());
	}

	@Test
	void doorAndCondenserAreNeeded() {
		Box b = new Box(3, 2, 3);
		b.set(0, 0, 3, ColdRoomPart.PANEL);
		assertEquals("The room needs a cold-room door in a wall", b.check().problem());
		b = new Box(3, 2, 3);
		b.cells.remove("-2,0,0");
		assertEquals("No condensing unit touching the walls outside", b.check().problem());
	}

	@Test
	void chestsInsideAreStillInside() {
		Box b = new Box(3, 2, 3);
		b.set(1, 0, 1, ColdRoomPart.OTHER);
		assertTrue(b.check().formed());
	}

	@Test
	void loadFollowsTheVendorTable() {
		assertEquals(4986, ColdRoomLoad.btuPerHour(6 * 6 * 8 / ColdRoomLoad.FT3_PER_M3), 1);
		assertEquals(6258, ColdRoomLoad.btuPerHour(8 * 8 * 8 / ColdRoomLoad.FT3_PER_M3), 1);
		assertEquals(8230, ColdRoomLoad.btuPerHour(10 * 10 * 8 / ColdRoomLoad.FT3_PER_M3), 1);
		assertTrue(ColdRoomLoad.btuPerHour(400) > ColdRoomLoad.btuPerHour(36));
		assertEquals(ColdRoomLoad.coolingWatts(36) / 2, ColdRoomLoad.electricWatts(36), 1e-9);
	}
}
