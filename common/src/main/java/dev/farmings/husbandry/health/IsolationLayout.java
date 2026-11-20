package dev.farmings.husbandry.health;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whether an isolation pen forms (docs/multiblocks/isolation-pen.md): the pen cells joined to the controller, 2 to 64 of them, a feeder
 * and a trough beside it, and no other housing within 8 blocks ("no over-the-fence contact"). Plain Java over a {@link Grid}.
 */
public final class IsolationLayout {
	public static final int MIN_CELLS = 2;
	public static final int MAX_CELLS = 64;
	/** No cell of other housing within this many blocks of a pen cell (proposed). */
	public static final int CLEARANCE = 8;

	public enum Cell { PEN, CONTROLLER, FEEDER, TROUGH, HOUSING, OTHER }

	public interface Grid {
		Cell at(int x, int y, int z);
	}

	public record Pos(int x, int y, int z) {
	}

	public record Check(String problem, List<Pos> pen, List<Pos> feeders, List<Pos> troughs) {
		public boolean formed() {
			return problem.isEmpty();
		}
	}

	private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private IsolationLayout() {
	}

	public static Check check(Grid g, int cx, int cy, int cz) {
		List<Pos> pen = new ArrayList<>();
		Set<Pos> feeders = new HashSet<>();
		Set<Pos> troughs = new HashSet<>();
		Set<Pos> seen = new HashSet<>();
		ArrayDeque<Pos> q = new ArrayDeque<>();
		for (int[] s : SIDES) {
			Pos n = new Pos(cx + s[0], cy, cz + s[1]);
			if (g.at(n.x(), n.y(), n.z()) == Cell.PEN && seen.add(n)) {
				q.add(n);
			}
		}
		if (q.isEmpty()) {
			return new Check("No isolation pen floor beside the controller", List.of(), List.of(), List.of());
		}
		while (!q.isEmpty()) {
			Pos p = q.poll();
			pen.add(p);
			if (pen.size() > MAX_CELLS) {
				return new Check("The pen is larger than " + MAX_CELLS + " cells", pen, List.of(), List.of());
			}
			for (int[] s : SIDES) {
				Pos n = new Pos(p.x() + s[0], p.y(), p.z() + s[1]);
				Cell k = g.at(n.x(), n.y(), n.z());
				if (k == Cell.PEN && seen.add(n)) {
					q.add(n);
				} else if (k == Cell.FEEDER) {
					feeders.add(n);
				} else if (k == Cell.TROUGH) {
					troughs.add(n);
				}
			}
		}
		if (pen.size() < MIN_CELLS) {
			return new Check("The pen needs at least " + MIN_CELLS + " cells, has " + pen.size(), pen, List.of(), List.of());
		}
		if (feeders.isEmpty()) {
			return new Check("NO FEEDER beside the pen", pen, List.of(), List.of(troughs.toArray(new Pos[0])));
		}
		if (troughs.isEmpty()) {
			return new Check("NO WATER: no trough beside the pen", pen, List.copyOf(feeders), List.of());
		}
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		for (Pos p : pen) {
			minX = Math.min(minX, p.x());
			maxX = Math.max(maxX, p.x());
			minZ = Math.min(minZ, p.z());
			maxZ = Math.max(maxZ, p.z());
		}
		for (int x = minX - CLEARANCE; x <= maxX + CLEARANCE; x++) {
			for (int z = minZ - CLEARANCE; z <= maxZ + CLEARANCE; z++) {
				for (int y = cy - 2; y <= cy + 2; y++) {
					if (g.at(x, y, z) == Cell.HOUSING && near(pen, x, z)) {
						return new Check("TOO CLOSE TO OTHER HOUSING AT " + x + " " + y + " " + z + " (" + CLEARANCE + " blocks needed)", pen, List.copyOf(feeders),
								List.copyOf(troughs));
					}
				}
			}
		}
		return new Check("", pen, List.copyOf(feeders), List.copyOf(troughs));
	}

	private static boolean near(List<Pos> pen, int x, int z) {
		for (Pos p : pen) {
			if (Math.abs(p.x() - x) <= CLEARANCE && Math.abs(p.z() - z) <= CLEARANCE) {
				return true;
			}
		}
		return false;
	}
}
