package dev.farmings.process.coldroom;

import java.util.List;

/**
 * A look at a walk-in fridge from its evaporator.
 *
 * @param problem    empty when formed, else the first thing wrong
 * @param min        the inside's lowest corner (x, y, z), or null
 * @param max        the inside's highest corner, or null
 * @param inside     every inside cell, as {x, y, z}
 * @param condenser  the condensing unit's cell, or null
 */
public record ColdRoomCheck(String problem, int[] min, int[] max, List<int[]> inside, int[] condenser) {
	public boolean formed() {
		return problem.isEmpty();
	}

	public int[] size() {
		return min == null ? new int[] {0, 0, 0} : new int[] {max[0] - min[0] + 1, max[1] - min[1] + 1, max[2] - min[2] + 1};
	}

	/** Inside volume in cubic metres (1 cell = 1 m). */
	public int volumeM3() {
		int[] s = size();
		return s[0] * s[1] * s[2];
	}

	public String status() {
		if (!formed()) {
			return problem;
		}
		int[] s = size();
		return "FORMED, " + s[0] + " x " + s[1] + " x " + s[2] + " inside (" + volumeM3() + " m3)";
	}
}
