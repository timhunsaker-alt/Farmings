package dev.farmings.husbandry.shed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The connected runs of a shed's pen cells (docs/multiblocks/sheep-shed.md, "Goats"): each run holds one species. Cells touching side by
 * side, up to one block apart in height, are one run. Plain Java.
 */
public final class PenRuns {
	private PenRuns() {
	}

	/** The run number of each cell, in the cells' order; runs are numbered from 0 in order of their first cell. */
	public static int[] of(List<ShedLayout.Pos> cells) {
		int n = cells.size();
		int[] run = new int[n];
		java.util.Arrays.fill(run, -1);
		Map<Long, Integer> index = new HashMap<>();
		for (int i = 0; i < n; i++) {
			ShedLayout.Pos p = cells.get(i);
			index.put(key(p.x(), p.z()), i);
		}
		int next = 0;
		int[] stack = new int[n];
		for (int i = 0; i < n; i++) {
			if (run[i] >= 0) {
				continue;
			}
			int top = 0;
			stack[top++] = i;
			run[i] = next;
			while (top > 0) {
				ShedLayout.Pos p = cells.get(stack[--top]);
				int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
				for (int[] d : steps) {
					Integer j = index.get(key(p.x() + d[0], p.z() + d[1]));
					if (j != null && run[j] < 0 && Math.abs(cells.get(j).y() - p.y()) <= 1) {
						run[j] = next;
						stack[top++] = j;
					}
				}
			}
			next++;
		}
		return run;
	}

	private static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xffffffffL);
	}
}
