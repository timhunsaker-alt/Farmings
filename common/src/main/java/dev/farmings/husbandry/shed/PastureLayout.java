package dev.farmings.husbandry.shed;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The pasture behind a shed's gate (docs/multiblocks/sheep-shed.md): the walkable ground reached from the gate, stepping at most one
 * block up or down, ending at fences, walls, water or anything a sheep cannot walk on. Plain Java over a {@link Ground}.
 */
public final class PastureLayout {
	/** Largest pasture, in cells *(proposed)*; a bigger flood means the pasture is not fenced in. */
	public static final int MAX_CELLS = 4096;

	/** One walkable cell: where a sheep stands (y) and whether the block under it is grass. */
	public record Spot(int x, int y, int z, boolean grass) {
	}

	public interface Ground {
		/** The walkable spot at column (x, z) within one block of {@code nearY}, or null if a sheep cannot stand there. */
		Spot at(int x, int z, int nearY);

		/** True for cells that belong to the shed (pen floor, gate): the flood does not run back into the pen. */
		boolean shed(int x, int y, int z);
	}

	public record Pasture(String problem, List<Spot> spots) {
		public boolean found() {
			return problem.isEmpty();
		}

		public int grass() {
			int n = 0;
			for (Spot s : spots) {
				if (s.grass()) {
					n++;
				}
			}
			return n;
		}
	}

	private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private PastureLayout() {
	}

	/** The pasture through a gate at (gx, gy, gz). */
	public static Pasture find(Ground ground, int gx, int gy, int gz) {
		Map<Long, Spot> seen = new HashMap<>();
		ArrayDeque<Spot> queue = new ArrayDeque<>();
		for (int[] s : SIDES) {
			int x = gx + s[0];
			int z = gz + s[1];
			Spot sp = ground.at(x, z, gy);
			if (sp != null && !ground.shed(x, sp.y(), z) && seen.putIfAbsent(key(x, z), sp) == null) {
				queue.add(sp);
			}
		}
		if (queue.isEmpty()) {
			return new Pasture("No open ground past the pasture gate", List.of());
		}
		List<Spot> out = new ArrayList<>();
		while (!queue.isEmpty()) {
			Spot p = queue.poll();
			out.add(p);
			if (out.size() > MAX_CELLS) {
				return new Pasture("The pasture is not fenced in (over " + MAX_CELLS + " blocks)", List.of());
			}
			for (int[] s : SIDES) {
				int x = p.x() + s[0];
				int z = p.z() + s[1];
				if (seen.containsKey(key(x, z))) {
					continue;
				}
				Spot n = ground.at(x, z, p.y());
				if (n != null && Math.abs(n.y() - p.y()) <= 1 && !ground.shed(x, n.y(), z)) {
					seen.put(key(x, z), n);
					queue.add(n);
				}
			}
		}
		if (out.stream().noneMatch(Spot::grass)) {
			return new Pasture("The pasture has no grass", List.copyOf(out));
		}
		return new Pasture("", List.copyOf(out));
	}

	private static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}
}
