package dev.farmings.husbandry.lane;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How a parlor reaches its barn (docs/multiblocks/parlor.md, "Cow lane and holding pen"): a holding pen beside the parlor stalls, then a
 * run of cow lane, then a barn's scrape alley, whose barn controller stands beside the alley's pit end. Plain Java, in world cells, so it
 * is tested on a drawn grid without Minecraft.
 *
 * <ul>
 *   <li>The pen is every holding-pen cell joined (side by side, same level) to one that touches a parlor stall. It must give each parlor
 *       stall 15 sq ft (1.39 m2), the low end of the Dairyland Initiative's 15-20 sq ft per cow (Hoard's Dairyman, "Fit your facilities to
 *       your herd"), so one parlor turn of cows can wait in it.</li>
 *   <li>The lane is the shortest run of cow-lane cells from the pen to a cell beside a barn alley, up to {@link #MAX_LANE} cells
 *       (proposed). A lane may step up or down one block between cells.</li>
 *   <li>The barn is found by following the alley (floor and rail cells) to the cell beside its controller, one block up.</li>
 * </ul>
 */
public final class LanePath {
	public enum Cell { LANE, PEN, ALLEY, BARN_CONTROLLER, STALL, OTHER }

	@FunctionalInterface
	public interface Grid {
		Cell at(int x, int y, int z);
	}

	public record Pos(int x, int y, int z) {
		Pos offset(int dx, int dy, int dz) {
			return new Pos(x + dx, y + dy, z + dz);
		}
	}

	/**
	 * A found link.
	 *
	 * @param problem         empty when linked, else what is missing
	 * @param pen             the pen cells
	 * @param route           the way from the barn to the pen: the alley cell the lane leaves from, every lane cell in order, then
	 *                        the pen cell the lane enters
	 * @param barnController  the barn controller's cell, or null
	 */
	public record Link(String problem, List<Pos> pen, List<Pos> route, Pos barnController) {
		public boolean linked() {
			return problem.isEmpty();
		}

		/** How many cows the pen holds at 15 sq ft a cow. */
		public int penCapacity() {
			return capacity(pen.size());
		}
	}

	/** One cow's share of a holding pen: 15 sq ft in square metres. */
	public static final double M2_PER_COW = 15 * 0.3048 * 0.3048;
	public static final int MAX_LANE = 64;
	private static final int MAX_ALLEY = 400;
	private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private LanePath() {
	}

	/** Pen cells (1 m2 each) needed for {@code stalls} cows. */
	public static int penCellsNeeded(int stalls) {
		return (int) Math.ceil(stalls * M2_PER_COW - 1e-9);
	}

	public static int capacity(int penCells) {
		return (int) Math.floor(penCells / M2_PER_COW + 1e-9);
	}

	/** Finds the pen, lane and barn from the parlor's stall cells. */
	public static Link find(Grid g, List<Pos> stallCells, int stalls) {
		// the pen
		Set<Pos> pen = new LinkedHashSet<>();
		ArrayDeque<Pos> q = new ArrayDeque<>();
		for (Pos s : stallCells) {
			for (int[] d : SIDES) {
				Pos p = s.offset(d[0], 0, d[1]);
				if (g.at(p.x(), p.y(), p.z()) == Cell.PEN && pen.add(p)) {
					q.add(p);
				}
			}
		}
		if (pen.isEmpty()) {
			return fail("No holding pen beside the parlor stalls", pen);
		}
		while (!q.isEmpty() && pen.size() < 1000) {
			Pos p = q.poll();
			for (int[] d : SIDES) {
				Pos n = p.offset(d[0], 0, d[1]);
				if (!pen.contains(n) && g.at(n.x(), n.y(), n.z()) == Cell.PEN) {
					pen.add(n);
					q.add(n);
				}
			}
		}
		int need = penCellsNeeded(stalls);
		if (pen.size() < need) {
			return fail("The holding pen has " + pen.size() + " cells; " + stalls + " stalls need " + need + " (15 sq ft a cow)", pen);
		}
		// the lane: breadth first from the pen, so the route found is the shortest
		Map<Pos, Pos> from = new HashMap<>();
		Map<Pos, Integer> depth = new HashMap<>();
		q.clear();
		for (Pos p : pen) {
			for (Pos n : steps(p)) {
				if (!from.containsKey(n) && g.at(n.x(), n.y(), n.z()) == Cell.LANE) {
					from.put(n, p);
					depth.put(n, 1);
					q.add(n);
				}
			}
		}
		if (q.isEmpty()) {
			return fail("No cow lane leaves the holding pen", pen);
		}
		Pos end = null;
		Pos exit = null;
		while (!q.isEmpty() && end == null) {
			Pos p = q.poll();
			for (Pos n : steps(p)) {
				Cell c = g.at(n.x(), n.y(), n.z());
				if (c == Cell.ALLEY) {
					end = p;
					exit = n;
					break;
				}
				if (c == Cell.LANE && !from.containsKey(n) && depth.get(p) < MAX_LANE) {
					from.put(n, p);
					depth.put(n, depth.get(p) + 1);
					q.add(n);
				}
			}
		}
		if (end == null) {
			return fail("The cow lane does not reach a barn alley (" + MAX_LANE + " cells at most)", pen);
		}
		List<Pos> route = new ArrayList<>();
		route.add(exit);
		for (Pos p = end; p != null && !pen.contains(p); p = from.get(p)) {
			route.add(p);
		}
		// the pen cell the lane enters by
		Pos first = route.get(route.size() - 1);
		route.add(from.get(first));
		// the barn: follow the alley to the cell beside the controller, one up
		Pos barn = barnController(g, exit);
		if (barn == null) {
			return new Link("The cow lane reaches an alley with no barn controller", List.copyOf(pen), route, null);
		}
		return new Link("", List.copyOf(pen), Collections.unmodifiableList(route), barn);
	}

	private static Pos barnController(Grid g, Pos start) {
		Set<Pos> seen = new LinkedHashSet<>();
		ArrayDeque<Pos> q = new ArrayDeque<>();
		seen.add(start);
		q.add(start);
		while (!q.isEmpty() && seen.size() < MAX_ALLEY) {
			Pos p = q.poll();
			for (int[] d : SIDES) {
				Pos up = p.offset(d[0], 1, d[1]);
				if (g.at(up.x(), up.y(), up.z()) == Cell.BARN_CONTROLLER) {
					return up;
				}
				Pos n = p.offset(d[0], 0, d[1]);
				if (!seen.contains(n) && g.at(n.x(), n.y(), n.z()) == Cell.ALLEY) {
					seen.add(n);
					q.add(n);
				}
			}
		}
		return null;
	}

	/** The cells a cow can step to: the four sides, level or one block up or down. */
	private static List<Pos> steps(Pos p) {
		List<Pos> out = new ArrayList<>(12);
		for (int dy = 0; dy >= -1; dy--) {
			for (int[] d : SIDES) {
				out.add(p.offset(d[0], dy, d[1]));
			}
		}
		for (int[] d : SIDES) {
			out.add(p.offset(d[0], 1, d[1]));
		}
		return out;
	}

	private static Link fail(String problem, Set<Pos> pen) {
		return new Link(problem, List.copyOf(pen), List.of(), null);
	}

	/**
	 * Where a cow walking {@code route} should head first: the waypoint she is nearest to, so a cow already part way along, or already at
	 * the far end, does not walk back to the start.
	 */
	public static int startIndex(List<Pos> route, double x, double z) {
		int best = 0;
		double bestD = Double.MAX_VALUE;
		for (int i = 0; i < route.size(); i++) {
			double dx = route.get(i).x() + 0.5 - x;
			double dz = route.get(i).z() + 0.5 - z;
			double d = dx * dx + dz * dz;
			if (d < bestD) {
				bestD = d;
				best = i;
			}
		}
		return best;
	}
}
