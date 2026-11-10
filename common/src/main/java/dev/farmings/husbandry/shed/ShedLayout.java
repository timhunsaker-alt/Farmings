package dev.farmings.husbandry.shed;

import dev.farmings.husbandry.Flock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whether a sheep shed forms, and what it holds (docs/multiblocks/sheep-shed.md). Plain Java over a {@link Grid}: the world side says
 * what each cell is; this finds the pen, its racks, troughs, lambing pens and gate, and names the first problem.
 */
public final class ShedLayout {
	/** Largest pen, in cells *(proposed)*. */
	public static final int MAX_CELLS = 256;
	/** A pen needs room for two sheep. */
	public static final int MIN_PEN_CELLS = 3;
	/** Lambing pens: about 10% of the flock (Penn State G5, a 100-ewe flock). */
	public static final double LAMBING_SHARE = 0.10;

	public enum Cell { PEN, LAMBING_PEN, GATE, CONTROLLER, HAY_RACK, TROUGH, OTHER }

	public interface Grid {
		Cell at(int x, int y, int z);
	}

	public record Pos(int x, int y, int z) {
		Pos offset(int dx, int dz) {
			return new Pos(x + dx, y, z + dz);
		}
	}

	public record Check(String problem, List<Pos> pen, List<Pos> lambing, List<Pos> gates, List<Pos> racks, List<Pos> troughs) {
		public boolean formed() {
			return problem.isEmpty();
		}

		/** Sheep the pen holds (lambing pens hold their ewe on top). */
		public int capacity() {
			return Flock.penCapacity(pen.size());
		}

		/** Lambing pens wanted for a full shed, at the 10% rule, rounded up. */
		public int lambingWanted() {
			return (int) Math.ceil(capacity() * LAMBING_SHARE);
		}

		public String status() {
			return formed() ? "FORMED" : problem;
		}
	}

	private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private ShedLayout() {
	}

	private static boolean floor(Cell c) {
		return c == Cell.PEN || c == Cell.LAMBING_PEN || c == Cell.GATE;
	}

	public static Check check(Grid grid, int cx, int cy, int cz) {
		List<Pos> pen = new ArrayList<>();
		List<Pos> lambing = new ArrayList<>();
		List<Pos> gates = new ArrayList<>();
		Set<Pos> racks = new HashSet<>();
		Set<Pos> troughs = new HashSet<>();
		Set<Pos> seen = new HashSet<>();
		ArrayDeque<Pos> queue = new ArrayDeque<>();
		Pos c = new Pos(cx, cy, cz);
		for (int[] s : SIDES) {
			Pos n = c.offset(s[0], s[1]);
			if (floor(grid.at(n.x(), n.y(), n.z())) && seen.add(n)) {
				queue.add(n);
			}
		}
		if (queue.isEmpty()) {
			return fail("No sheep pen floor beside the controller");
		}
		boolean tooBig = false;
		while (!queue.isEmpty()) {
			Pos p = queue.poll();
			switch (grid.at(p.x(), p.y(), p.z())) {
				case PEN -> pen.add(p);
				case LAMBING_PEN -> lambing.add(p);
				default -> gates.add(p);
			}
			if (seen.size() > MAX_CELLS) {
				tooBig = true;
				break;
			}
			for (int[] s : SIDES) {
				Pos n = p.offset(s[0], s[1]);
				Cell k = grid.at(n.x(), n.y(), n.z());
				if (floor(k) && grid.at(p.x(), p.y(), p.z()) != Cell.GATE && seen.add(n)) {
					queue.add(n);
				} else if (k == Cell.HAY_RACK) {
					racks.add(n);
				} else if (k == Cell.TROUGH) {
					troughs.add(n);
				}
			}
		}
		Comparator<Pos> order = Comparator.comparingInt(Pos::x).thenComparingInt(Pos::z);
		pen.sort(order);
		lambing.sort(order);
		gates.sort(order);
		List<Pos> r = new ArrayList<>(racks);
		r.sort(order);
		List<Pos> t = new ArrayList<>(troughs);
		t.sort(order);
		String problem = "";
		if (tooBig) {
			problem = "The pen is larger than " + MAX_CELLS + " cells";
		} else if (pen.size() < MIN_PEN_CELLS) {
			problem = "The pen needs at least " + MIN_PEN_CELLS + " sheep pen cells (room for 2 sheep), has " + pen.size();
		} else if (r.isEmpty()) {
			problem = "No hay rack beside the pen";
		} else if (t.isEmpty()) {
			problem = "No water trough beside the pen";
		}
		return new Check(problem, List.copyOf(pen), List.copyOf(lambing), List.copyOf(gates), List.copyOf(r), List.copyOf(t));
	}

	private static Check fail(String why) {
		return new Check(why, List.of(), List.of(), List.of(), List.of(), List.of());
	}
}
