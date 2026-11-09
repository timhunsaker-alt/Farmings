package dev.farmings.husbandry.barn;

import java.util.ArrayList;
import java.util.List;

/**
 * The forming rules of the freestall barn (docs/multiblocks/barn.md), on a drawn barn with the pit at the east end. Plain Java:
 * {@link #check} asks a {@link BarnGrid} what stands in each cell, so it is tested without Minecraft.
 *
 * <p>The barn, from the pit end, along its length (x) and across it (z), with the controller at (cx, cy, cz) and floor level cy-1:
 * <pre>
 *   z-5   feed bunk (floor) and hay feeder (above), one per stall
 *   z-4..z-2  stall beds (3 long) and, 2 high, the dividers between them
 *   z-1..z+1  the alley: floor cells, the rail down its middle, the scraper (3 wide) at y = cy
 *   z+2..z+4  stall beds and dividers
 *   z+5   feed bunk and hay feeder
 * </pre>
 * Along x, a stall row is divider, bed, divider, bed ... divider, so n stalls take 2n+1 cells. (The spec's first sketch showed the
 * beds shoulder to shoulder; that left no cell for a divider between two beds, so a stall takes two cells.) The alley is twice as long
 * as a stall row (Tim, 2026-11-09): 2(2n+1) cells from the drive to the cell before the pit, with n open cells at the drive end, the
 * stall row, then n+1 open cells at the pit end (the odd cell goes to the pit end, proposed).
 */
public final class BarnLayout {
	public static final int MIN_STALLS_PER_SIDE = 2;
	/** How far from the controller the drive is looked for: 12 stalls a side (the spec's upper bound) take a 50-cell alley. */
	public static final int MAX_LENGTH = 50;
	private static final int PIT_LENGTH = 2;

	private BarnLayout() {
	}

	/** Tries all eight ways the barn could lie and returns the one that forms, or the one with the fewest problems. */
	public static BarnCheck find(BarnGrid world, int controllerX, int controllerY, int controllerZ) {
		BarnCheck best = null;
		for (Orientation o : Orientation.ALL) {
			BarnGrid drawn = (ax, y, az) -> {
				int[] w = o.world(ax, az);
				return world.at(controllerX + w[0], y, controllerZ + w[1]);
			};
			BarnCheck c = check(drawn, 0, controllerY, 0, o);
			if (c.formed()) {
				return c;
			}
			if (best == null || score(c) < score(best)) {
				best = c;
			}
		}
		return best;
	}

	private static int score(BarnCheck c) {
		return c.problems().size();
	}

	/** Checks a barn drawn with its controller at (cx, cy, cz) and its pit end towards +x. */
	public static BarnCheck check(BarnGrid g, int cx, int cy, int cz, Orientation orientation) {
		List<String> problems = new ArrayList<>();
		int fl = cy - 1;

		if (g.at(cx, cy, cz) != BarnPart.CONTROLLER) {
			problems.add("No barn controller here");
			return new BarnCheck(false, 0, problems, orientation, BarnCheck.NO_DRIVE);
		}
		// the pit: its top layer is 2 long (along the barn) and 3 wide (across), the controller on its near column
		for (int dx = 0; dx < PIT_LENGTH; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (g.at(cx + dx, fl, cz + dz) != BarnPart.COLLECTION_PIT) {
					problems.add("The collection pit is missing under and beside the controller");
					dx = PIT_LENGTH;
					break;
				}
			}
		}

		// the drive: the first scraper drive along the rail line, going away from the pit
		int xd = Integer.MIN_VALUE;
		for (int x = cx - 1; x >= cx - MAX_LENGTH; x--) {
			if (g.at(x, cy, cz) == BarnPart.SCRAPER_DRIVE) {
				xd = x;
				break;
			}
		}
		if (xd == Integer.MIN_VALUE) {
			problems.add("No scraper drive at the far end of the alley");
			return new BarnCheck(false, 0, problems, orientation, BarnCheck.NO_DRIVE);
		}
		int length = cx - xd;                       // cells from the drive to the one before the pit
		if (length % 4 != 2) {
			// an odd length is most likely a barn built before the alley was doubled: the alley only as long as its 2n+1 stall row
			int n = Math.max(MIN_STALLS_PER_SIDE, length % 2 == 1 ? (length - 1) / 2 : (int) Math.round((length / 2.0 - 1) / 2));
			problems.add("The alley is " + length + " cells long; it must be twice a stall row, 2 x (2n+1): "
					+ alleyLength(n) + " for " + n + " stalls a side");
			return new BarnCheck(false, 0, problems, orientation, xd - cx);
		}
		int stalls = (length / 2 - 1) / 2;
		if (stalls < MIN_STALLS_PER_SIDE) {
			problems.add("A barn needs at least " + MIN_STALLS_PER_SIDE + " stalls a side; this one has " + stalls);
			return new BarnCheck(false, stalls, problems, orientation, xd - cx);
		}

		// the alley: rail down the middle, floor either side
		for (int x = xd; x < cx; x++) {
			if (g.at(x, fl, cz) != BarnPart.SCRAPER_RAIL) {
				problems.add("Scraper rail missing " + (x - xd) + " cells from the drive");
				break;
			}
		}
		for (int x = xd; x < cx; x++) {
			if (g.at(x, fl, cz - 1) != BarnPart.ALLEY_FLOOR || g.at(x, fl, cz + 1) != BarnPart.ALLEY_FLOOR) {
				problems.add("Alley floor missing " + (x - xd) + " cells from the drive");
				break;
			}
		}
		boolean scraper = false;
		for (int x = xd + 1; x < cx && !scraper; x++) {
			scraper = g.at(x, cy, cz - 1) == BarnPart.MANURE_SCRAPER && g.at(x, cy, cz) == BarnPart.MANURE_SCRAPER && g.at(x, cy, cz + 1) == BarnPart.MANURE_SCRAPER;
		}
		if (!scraper) {
			problems.add("No manure scraper on the rail");
		}

		// the two rows of stalls, after the drive-end open stretch
		int row = xd + stalls;
		for (int side = 0; side < 2; side++) {
			String rowName = side == 0 ? "north" : "south";
			int dir = side == 0 ? -1 : 1;           // -z is the north row
			int bedNear = cz + dir * 2;             // the bed cells are 3 long: 2..4 from the centre
			for (int j = 0; j <= stalls; j++) {
				int x = row + 2 * j;
				if (!column(g, x, fl, bedNear, dir, 3, 2, BarnPart.STALL_DIVIDER)) {
					int stall = Math.min(j + 1, stalls);
					problems.add("Stall " + stall + " " + rowName + " has no divider on its " + (j < stalls ? "near" : "far") + " side");
				}
			}
			for (int i = 0; i < stalls; i++) {
				int x = row + 1 + 2 * i;
				if (!column(g, x, fl, bedNear, dir, 3, 1, BarnPart.STALL_BED)) {
					problems.add("Stall " + (i + 1) + " " + rowName + " has no bed");
				}
				int bunkZ = cz + dir * 5;
				if (g.at(x, fl, bunkZ) != BarnPart.FEED_BUNK) {
					problems.add("Stall " + (i + 1) + " " + rowName + " has no feed bunk");
				}
				if (g.at(x, fl + 1, bunkZ) != BarnPart.HAY_FEEDER) {
					problems.add("Stall " + (i + 1) + " " + rowName + " has no hay feeder");
				}
			}
			boolean trough = false;
			for (int x = xd - 1; x <= cx + 4 && !trough; x++) {
				for (int k = 2; k <= 4 && !trough; k++) {
					trough = g.at(x, fl, cz + dir * k) == BarnPart.WATER_TROUGH;
				}
			}
			if (!trough) {
				problems.add("The " + rowName + " row has no water trough");
			}
		}
		return new BarnCheck(problems.isEmpty(), stalls, problems, orientation, xd - cx);
	}

	/** The alley length (drive to the cell before the pit) for {@code stallsPerSide} stalls a side: twice the 2n+1 stall row. */
	public static int alleyLength(int stallsPerSide) {
		return 2 * (2 * stallsPerSide + 1);
	}

	/** Whether {@code len} cells across (starting {@code dir * k} from the centre line) are {@code part}, {@code high} layers up. */
	private static boolean column(BarnGrid g, int x, int fl, int firstZ, int dir, int len, int high, BarnPart part) {
		for (int h = 0; h < high; h++) {
			for (int k = 0; k < len; k++) {
				if (g.at(x, fl + h, firstZ + dir * k) != part) {
					return false;
				}
			}
		}
		return true;
	}
}
