package dev.farmings.husbandry.barn;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the stalls are in a formed barn, in drawn cells from the controller (the pit towards +x, see {@link BarnLayout}). Stall i on a
 * side stands at x = rowStart + 1 + 2i (rowStart is the row's first divider, BarnCheck.rowStartX) (a bed, with a divider either side), across the barn at z = dir * 3 (the middle bed cell), her bunk and
 * feeder at z = dir * 5, and the alley cell in front of her at z = dir. dir is -1 for the north row and +1 for the south row.
 */
public final class BarnStalls {
	/** One stall: a bed with a bunk and a hay feeder at its head. */
	public record Stall(int id, int side, int index, int x, int bedZ, int bunkZ, int alleyZ) {
		public String name() {
			return "stall " + (index + 1) + (side < 0 ? " north" : " south");
		}
	}

	private BarnStalls() {
	}

	/** Every stall of a barn with {@code stallsPerSide} stalls a side, its rows starting at {@code rowStart}; the north row first. */
	public static List<Stall> of(int rowStart, int stallsPerSide) {
		List<Stall> out = new ArrayList<>();
		int id = 0;
		for (int dir = -1; dir <= 1; dir += 2) {
			for (int i = 0; i < stallsPerSide; i++) {
				out.add(new Stall(id++, dir, i, rowStart + 1 + 2 * i, dir * 3, dir * 5, dir));
			}
		}
		return out;
	}

	/** The free stall nearest the given drawn position (by distance along and across the barn), or null when every stall is taken. */
	public static Stall nearestFree(List<Stall> stalls, java.util.Set<Integer> taken, double x, double z) {
		Stall best = null;
		double bestD = Double.MAX_VALUE;
		for (Stall s : stalls) {
			if (taken.contains(s.id())) {
				continue;
			}
			double d = (s.x() - x) * (s.x() - x) + (s.bedZ() - z) * (s.bedZ() - z);
			if (d < bestD) {
				bestD = d;
				best = s;
			}
		}
		return best;
	}
}
