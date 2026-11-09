package dev.farmings.husbandry.parlor;

import static org.junit.jupiter.api.Assertions.*;

import dev.farmings.husbandry.barn.Orientation;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ParlorLayoutTest {
	private static final int CY = 1;

	/** A drawn parlor with n stalls: controller at (0,1,0), pump at x = -(n+1). */
	private static Map<String, ParlorPart> parlor(int n) {
		Map<String, ParlorPart> m = new HashMap<>();
		m.put(k(0, CY, 0), ParlorPart.CONTROLLER);
		for (int dx = 1; dx <= 2; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				for (int y = 0; y <= 1; y++) {
					m.put(k(dx, y, dz), ParlorPart.BULK_TANK);
				}
			}
		}
		m.put(k(-(n + 1), 0, 0), ParlorPart.VACUUM_PUMP);
		for (int x = -n; x <= 0; x++) {
			m.put(k(x, 0, 0), ParlorPart.MILK_LINE);
		}
		for (int x = -n; x <= -1; x++) {
			m.put(k(x, 1, 0), ParlorPart.MILKING_UNIT);
			m.put(k(x, 0, 1), ParlorPart.PARLOR_STALL);
			m.put(k(x, 0, 2), ParlorPart.PARLOR_STALL);
		}
		return m;
	}

	private static String k(int x, int y, int z) {
		return x + "," + y + "," + z;
	}

	private static ParlorCheck run(Map<String, ParlorPart> m) {
		return ParlorLayout.check((x, y, z) -> m.get(k(x, y, z)), 0, CY, 0, new Orientation(0, false));
	}

	@Test
	void aWholeParlorForms() {
		for (int n : new int[] {2, 6, 12}) {
			ParlorCheck c = run(parlor(n));
			assertTrue(c.formed(), c.problems().toString());
			assertEquals(n, c.stalls());
		}
	}

	@Test
	void missingPartsAreNamed() {
		Map<String, ParlorPart> m = parlor(4);
		m.remove(k(-2, 1, 0));
		assertEquals("Stall 3 has no milking unit", run(m).status());
		m = parlor(4);
		m.remove(k(-3, 0, 2));
		assertEquals("Stall 2 has no parlor stall", run(m).status());
		m = parlor(4);
		m.remove(k(-1, 0, 0));
		assertTrue(run(m).status().startsWith("Milk line missing"));
		m = parlor(4);
		m.remove(k(2, 1, 1));
		assertTrue(run(m).status().contains("bulk tank"));
		m = parlor(4);
		m.remove(k(-5, 0, 0));
		assertTrue(run(m).status().contains("vacuum pump"));
	}

	@Test
	void tooFewStallsDoNotForm() {
		assertFalse(run(parlor(1)).formed());
	}

	@Test
	void findTriesTheOtherWaysRound() {
		Map<String, ParlorPart> m = parlor(3);
		Orientation o = new Orientation(1, true);   // the world is the drawing turned and mirrored
		ParlorCheck c = ParlorLayout.find((x, y, z) -> {
			for (int ax = -6; ax <= 6; ax++) {
				for (int az = -6; az <= 6; az++) {
					int[] w = o.world(ax, az);
					if (w[0] == x && w[1] == z) {
						return m.get(k(ax, y, az));
					}
				}
			}
			return null;
		}, 0, CY, 0);
		assertTrue(c.formed(), c.problems().toString());
	}
}
