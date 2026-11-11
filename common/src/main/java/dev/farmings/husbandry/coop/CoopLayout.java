package dev.farmings.husbandry.coop;

import dev.farmings.genetics.Species;
import dev.farmings.husbandry.poultry.Fowl;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Whether a coop forms, and what it holds (docs/multiblocks/coop.md). Plain Java over a {@link Grid}: the world side says what each cell
 * is; this finds the floor, its pop holes, nest boxes, perches, grain feeders and troughs, and names the first problem. The sheep shed's
 * {@code ShedLayout} is the pattern.
 */
public final class CoopLayout {
	/** Largest coop, in cells (proposed, the shed's cap). */
	public static final int MAX_CELLS = 256;
	/** Largest run, in cells (proposed). */
	public static final int MAX_RUN = 1024;
	/** Ducks need one water block in or beside the run per this many ducks (proposed). */
	public static final int DUCKS_PER_WATER = 4;

	public enum Cell { FLOOR, POP_HOLE, CONTROLLER, NEST, PERCH, FEEDER, TROUGH, OTHER }

	public interface Grid {
		Cell at(int x, int y, int z);
	}

	public record Pos(int x, int y, int z) {
		Pos offset(int dx, int dy, int dz) {
			return new Pos(x + dx, y + dy, z + dz);
		}
	}

	public record Check(String problem, List<Pos> floor, List<Pos> popHoles, List<Pos> nests, List<Pos> perches, List<Pos> feeders,
			List<Pos> troughs) {
		public boolean formed() {
			return problem.isEmpty();
		}
	}

	private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private CoopLayout() {
	}

	private static boolean floor(Cell c) {
		return c == Cell.FLOOR || c == Cell.POP_HOLE;
	}

	public static Check check(Grid grid, int cx, int cy, int cz) {
		List<Pos> floor = new ArrayList<>();
		List<Pos> holes = new ArrayList<>();
		Set<Pos> nests = new LinkedHashSet<>();
		Set<Pos> perches = new LinkedHashSet<>();
		Set<Pos> feeders = new LinkedHashSet<>();
		Set<Pos> troughs = new LinkedHashSet<>();
		Set<Pos> seen = new LinkedHashSet<>();
		ArrayDeque<Pos> queue = new ArrayDeque<>();
		Pos c = new Pos(cx, cy, cz);
		for (int[] s : SIDES) {
			Pos n = c.offset(s[0], 0, s[1]);
			if (floor(grid.at(n.x(), n.y(), n.z())) && seen.add(n)) {
				queue.add(n);
			}
		}
		if (queue.isEmpty()) {
			return fail("No coop floor beside the controller");
		}
		boolean tooBig = false;
		while (!queue.isEmpty()) {
			Pos p = queue.poll();
			Cell here = grid.at(p.x(), p.y(), p.z());
			(here == Cell.POP_HOLE ? holes : floor).add(p);
			if (seen.size() > MAX_CELLS) {
				tooBig = true;
				break;
			}
			if (grid.at(p.x(), p.y() + 1, p.z()) == Cell.PERCH) {
				perches.add(p.offset(0, 1, 0));
			}
			for (int[] s : SIDES) {
				for (int dy = 0; dy <= 1; dy++) {
					Pos n = p.offset(s[0], dy, s[1]);
					Cell k = grid.at(n.x(), n.y(), n.z());
					if (dy == 0 && floor(k) && here != Cell.POP_HOLE && seen.add(n)) {
						queue.add(n);
					} else if (k == Cell.PERCH) {
						perches.add(n);
					} else if (dy == 0 && k == Cell.NEST) {
						nests.add(n);
					} else if (dy == 0 && k == Cell.FEEDER) {
						feeders.add(n);
					} else if (dy == 0 && k == Cell.TROUGH) {
						troughs.add(n);
					}
				}
			}
		}
		Comparator<Pos> order = Comparator.comparingInt(Pos::x).thenComparingInt(Pos::z).thenComparingInt(Pos::y);
		floor.sort(order);
		holes.sort(order);
		String problem = "";
		if (tooBig) {
			problem = "The coop is larger than " + MAX_CELLS + " cells";
		} else if (floor.isEmpty()) {
			problem = "No coop floor beside the controller";
		} else if (nests.isEmpty()) {
			problem = "No nest box beside the coop floor";
		} else if (feeders.isEmpty()) {
			problem = "No grain feeder beside the coop floor";
		} else if (troughs.isEmpty()) {
			problem = "No water trough beside the coop floor";
		}
		return new Check(problem, List.copyOf(floor), List.copyOf(holes), sorted(nests, order), sorted(perches, order), sorted(feeders, order),
				sorted(troughs, order));
	}

	private static List<Pos> sorted(Set<Pos> s, Comparator<Pos> order) {
		List<Pos> l = new ArrayList<>(s);
		l.sort(order);
		return List.copyOf(l);
	}

	private static Check fail(String why) {
		return new Check(why, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
	}

	/** Floor cells a mix of birds needs (poultry.md, "Housing"): each bird its share of a block. Young birds count as nothing. */
	public static double floorNeeded(Map<Species, Integer> adults) {
		double cells = 0;
		for (Map.Entry<Species, Integer> e : adults.entrySet()) {
			cells += e.getValue() / Fowl.birdsPerFloorBlock(e.getKey());
		}
		return cells;
	}

	/** Can one more adult of this species fit? */
	public static boolean room(Map<Species, Integer> adults, Species more, int floorCells) {
		return floorNeeded(adults) + 1.0 / Fowl.birdsPerFloorBlock(more) <= floorCells + 1e-9;
	}

	/** Nest boxes the hens want: one per 4 hens (8 for quail). */
	public static int nestsWanted(Map<Species, Integer> hens) {
		double n = 0;
		for (Map.Entry<Species, Integer> e : hens.entrySet()) {
			n += (double) e.getValue() / Fowl.hensPerNest(e.getKey());
		}
		return (int) Math.ceil(n);
	}

	/** Perch blocks the roosting birds want (chickens 3 a block, turkeys 1). */
	public static int perchesWanted(Map<Species, Integer> adults) {
		double n = 0;
		for (Map.Entry<Species, Integer> e : adults.entrySet()) {
			int per = Fowl.birdsPerPerch(e.getKey());
			if (per > 0) {
				n += (double) e.getValue() / per;
			}
		}
		return (int) Math.ceil(n);
	}

	/** Run cells the birds that go out need (poultry.md): only as many birds go out as fit. */
	public static double runNeeded(Map<Species, Integer> adults) {
		double cells = 0;
		for (Map.Entry<Species, Integer> e : adults.entrySet()) {
			cells += e.getValue() * Fowl.runCellsPerBird(e.getKey());
		}
		return cells;
	}

	/** Water blocks the ducks need: one per 4 ducks, rounded up. */
	public static int pondWanted(int ducks) {
		return (ducks + DUCKS_PER_WATER - 1) / DUCKS_PER_WATER;
	}
}
