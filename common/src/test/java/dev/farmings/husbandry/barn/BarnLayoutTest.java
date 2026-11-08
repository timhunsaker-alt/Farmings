package dev.farmings.husbandry.barn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BarnLayoutTest {
	/** A barn drawn with the controller at (0, 1, 0), floor at y 0, the pit end towards +x and the drive at the far end. */
	static final class Drawing {
		final Map<String, BarnPart> cells = new HashMap<>();
		final int stalls;
		final int xd;

		Drawing(int stalls) {
			this.stalls = stalls;
			this.xd = -(2 * stalls + 1);
			set(0, 1, 0, BarnPart.CONTROLLER);
			for (int x = 0; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					set(x, 0, z, BarnPart.COLLECTION_PIT);
					set(x, -1, z, BarnPart.COLLECTION_PIT);
				}
			}
			set(xd, 1, 0, BarnPart.SCRAPER_DRIVE);
			for (int x = xd; x < 0; x++) {
				set(x, 0, 0, BarnPart.SCRAPER_RAIL);
				set(x, 0, -1, BarnPart.ALLEY_FLOOR);
				set(x, 0, 1, BarnPart.ALLEY_FLOOR);
			}
			for (int z = -1; z <= 1; z++) {
				set(xd + 2, 1, z, BarnPart.MANURE_SCRAPER);
			}
			for (int dir = -1; dir <= 1; dir += 2) {
				for (int j = 0; j <= stalls; j++) {
					for (int k = 2; k <= 4; k++) {
						set(xd + 2 * j, 0, dir * k, BarnPart.STALL_DIVIDER);
						set(xd + 2 * j, 1, dir * k, BarnPart.STALL_DIVIDER);
					}
				}
				for (int i = 0; i < stalls; i++) {
					int x = xd + 1 + 2 * i;
					for (int k = 2; k <= 4; k++) {
						set(x, 0, dir * k, BarnPart.STALL_BED);
					}
					set(x, 0, dir * 5, BarnPart.FEED_BUNK);
					set(x, 1, dir * 5, BarnPart.HAY_FEEDER);
				}
				set(1, 0, dir * 2, BarnPart.WATER_TROUGH);   // beside the pit end, in line with the first bed row cell
				set(2, 0, dir * 2, BarnPart.WATER_TROUGH);
			}
		}

		void set(int x, int y, int z, BarnPart p) {
			cells.put(x + "," + y + "," + z, p);
		}

		void remove(int x, int y, int z) {
			cells.remove(x + "," + y + "," + z);
		}

		BarnGrid grid() {
			return (x, y, z) -> cells.get(x + "," + y + "," + z);
		}

		BarnCheck check() {
			return BarnLayout.check(grid(), 0, 1, 0, new Orientation(0, false));
		}
	}

	@Test
	void aPerfectBarnForms() {
		for (int n = 2; n <= 12; n++) {
			BarnCheck c = new Drawing(n).check();
			assertTrue(c.formed(), n + " stalls: " + c.problems());
			assertEquals(n, c.stallsPerSide());
			assertEquals(2 * n, c.stalls());
			assertEquals("FORMED", c.status());
		}
	}

	@Test
	void oneStallASideIsTooSmall() {
		BarnCheck c = new Drawing(1).check();
		assertFalse(c.formed());
		assertTrue(c.status().contains("at least 2 stalls"), c.status());
	}

	@Test
	void aMissingDividerNamesTheStall() {
		Drawing d = new Drawing(4);
		// the divider between stall 2 and stall 3 on the north side: x = xd + 4
		for (int k = 2; k <= 4; k++) {
			d.remove(d.xd + 4, 0, -k);
			d.remove(d.xd + 4, 1, -k);
		}
		BarnCheck c = d.check();
		assertFalse(c.formed());
		assertEquals(1, c.problems().size(), c.problems().toString());
		assertEquals("Stall 3 north has no divider on its near side", c.status());
	}

	@Test
	void aDividerThatIsOnlyOneHighIsNotEnough() {
		Drawing d = new Drawing(3);
		for (int k = 2; k <= 4; k++) {
			d.remove(d.xd, 1, k);
		}
		assertTrue(d.check().status().contains("Stall 1 south has no divider"));
	}

	@Test
	void theFarDividerBelongsToTheLastStall() {
		Drawing d = new Drawing(3);
		for (int k = 2; k <= 4; k++) {
			d.remove(d.xd + 6, 0, k);
			d.remove(d.xd + 6, 1, k);
		}
		assertEquals("Stall 3 south has no divider on its far side", d.check().status());
	}

	@Test
	void everyStallNeedsABedABunkAndAFeeder() {
		Drawing d = new Drawing(3);
		d.remove(d.xd + 3, 0, -3);                  // a cell of the bed of stall 2 north
		assertEquals("Stall 2 north has no bed", d.check().status());
		d = new Drawing(3);
		d.remove(d.xd + 1, 0, 5);
		assertEquals("Stall 1 south has no feed bunk", d.check().status());
		d = new Drawing(3);
		d.remove(d.xd + 5, 1, -5);
		assertEquals("Stall 3 north has no hay feeder", d.check().status());
	}

	@Test
	void theAlleyNeedsItsFloorRailScraperDriveAndPit() {
		Drawing d = new Drawing(3);
		d.remove(d.xd + 2, 0, 0);
		assertEquals("Scraper rail missing 2 cells from the drive", d.check().status());
		d = new Drawing(3);
		d.remove(d.xd + 4, 0, 1);
		assertEquals("Alley floor missing 4 cells from the drive", d.check().status());
		d = new Drawing(3);
		d.remove(d.xd + 2, 1, 1);
		assertEquals("No manure scraper on the rail", d.check().status());
		d = new Drawing(3);
		d.remove(d.xd, 1, 0);
		assertTrue(d.check().status().contains("No scraper drive"), d.check().status());
		d = new Drawing(3);
		d.remove(1, 0, 1);
		assertTrue(d.check().status().contains("collection pit"), d.check().status());
	}

	@Test
	void eachRowNeedsAWaterTrough() {
		Drawing d = new Drawing(3);
		d.remove(1, 0, -2);
		d.remove(2, 0, -2);
		assertEquals("The north row has no water trough", d.check().status());
		d = new Drawing(3);
		d.remove(1, 0, 2);
		d.remove(2, 0, 2);
		assertEquals("The south row has no water trough", d.check().status());
	}

	@Test
	void anEvenAlleyIsRejected() {
		Drawing d = new Drawing(3);
		d.remove(d.xd, 1, 0);
		d.set(d.xd + 1, 1, 0, BarnPart.SCRAPER_DRIVE);
		assertTrue(d.check().status().contains("2n+1"), d.check().status());
	}

	@Test
	void noControllerNoBarn() {
		Drawing d = new Drawing(3);
		d.remove(0, 1, 0);
		assertEquals("No barn controller here", d.check().status());
	}

	@Test
	void theBarnFormsInAllEightOrientations() {
		for (Orientation o : Orientation.ALL) {
			Drawing d = new Drawing(4);
			// put the drawing in the world turned by o, controller at (40, 1, 40), and ask from the controller
			BarnGrid world = (x, y, z) -> {
				for (int ax = -30; ax <= 5; ax++) {
					for (int az = -7; az <= 7; az++) {
						int[] w = o.world(ax, az);
						if (40 + w[0] == x && 40 + w[1] == z) {
							return d.cells.get(ax + "," + y + "," + az);
						}
					}
				}
				return null;
			};
			BarnCheck c = BarnLayout.find(world, 40, 1, 40);
			assertTrue(c.formed(), o + ": " + c.problems());
			assertEquals(4, c.stallsPerSide());
		}
	}

	@Test
	void whenNothingFormsTheClosestReadingNamesTheProblem() {
		Drawing d = new Drawing(4);
		for (int k = 2; k <= 4; k++) {
			d.remove(d.xd + 4, 0, -k);
			d.remove(d.xd + 4, 1, -k);
		}
		BarnCheck c = BarnLayout.find(d.grid(), 0, 1, 0);
		assertFalse(c.formed());
		assertEquals("Stall 3 north has no divider on its near side", c.status());
		assertEquals(new Orientation(0, false), c.orientation());
	}
}
