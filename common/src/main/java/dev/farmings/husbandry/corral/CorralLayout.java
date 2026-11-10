package dev.farmings.husbandry.corral;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The taming corral (docs/taming.md): the walkable floor reached from the corral post, stepping at most one block up or down, ending at
 * fences, walls and corral gates. The gates, bait troughs and water troughs touching that floor are the corral's parts. Plain Java over
 * a {@link Ground}.
 */
public final class CorralLayout {
	/** Largest corral, in cells (proposed); a bigger flood means the corral is not closed in. */
	public static final int MAX_CELLS = 1024;

	public enum Part {
		NONE,
		GATE,
		BAIT,
		TROUGH
	}

	public record Pos(int x, int y, int z) {
	}

	public interface Ground {
		/** Where an animal stands in column (x, z) within one block of {@code nearY}, or null if it cannot stand there. */
		Pos at(int x, int z, int nearY);

		/** The corral part at a cell that is not floor. */
		Part part(int x, int y, int z);
	}

	public record Corral(String problem, List<Pos> floor, List<Pos> gates, List<Pos> baits, List<Pos> troughs) {
		public boolean formed() {
			return problem.isEmpty();
		}
	}

	private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private CorralLayout() {
	}

	/** The corral around a post at (px, py, pz). */
	public static Corral check(Ground ground, int px, int py, int pz) {
		Map<Long, Pos> seen = new HashMap<>();
		ArrayDeque<Pos> queue = new ArrayDeque<>();
		Set<Pos> gates = new LinkedHashSet<>();
		Set<Pos> baits = new LinkedHashSet<>();
		Set<Pos> troughs = new LinkedHashSet<>();
		visit(ground, px, py, pz, seen, queue, gates, baits, troughs, true);
		if (queue.isEmpty()) {
			return new Corral("NO FLOOR: no open ground beside the post", List.of(), List.of(), List.of(), List.of());
		}
		List<Pos> floor = new ArrayList<>();
		while (!queue.isEmpty()) {
			Pos p = queue.poll();
			floor.add(p);
			if (floor.size() > MAX_CELLS) {
				return new Corral("NOT CLOSED IN (over " + MAX_CELLS + " blocks)", List.of(), List.of(), List.of(), List.of());
			}
			visit(ground, p.x(), p.y(), p.z(), seen, queue, gates, baits, troughs, false);
		}
		String problem = gates.isEmpty() ? "NO GATE: set a corral gate in the fence" : baits.isEmpty() ? "NO BAIT TROUGH inside the corral"
				: troughs.isEmpty() ? "NO WATER: put a water trough inside the corral" : "";
		return new Corral(problem, List.copyOf(floor), List.copyOf(gates), List.copyOf(baits), List.copyOf(troughs));
	}

	private static void visit(Ground ground, int x0, int y0, int z0, Map<Long, Pos> seen, ArrayDeque<Pos> queue, Set<Pos> gates, Set<Pos> baits,
			Set<Pos> troughs, boolean fromPost) {
		for (int[] s : SIDES) {
			int x = x0 + s[0];
			int z = z0 + s[1];
			Part part = ground.part(x, y0, z);
			if (part != Part.NONE) {
				(part == Part.GATE ? gates : part == Part.BAIT ? baits : troughs).add(new Pos(x, y0, z));
				continue;
			}
			if (seen.containsKey(key(x, z))) {
				continue;
			}
			Pos n = ground.at(x, z, y0);
			if (n != null && Math.abs(n.y() - y0) <= 1) {
				seen.put(key(x, z), n);
				queue.add(n);
			}
		}
	}

	private static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}
}
