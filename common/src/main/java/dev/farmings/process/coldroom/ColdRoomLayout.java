package dev.farmings.process.coldroom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The walk-in fridge's forming rule (docs/multiblocks/cold-room.md), plain Java on a {@link Grid}:
 * <ul>
 *   <li>The evaporator is part of a wall and faces in. The inside is every cell reached from the cell in front of it, through anything
 *       that is not wall (air, chests, barrels). If that reaches past the largest room, the walls have a gap.</li>
 *   <li>The inside must be a box: 1 x 1 x 1 to 10 x 4 x 10 cells (outside 3 x 3 x 3 to 12 x 6 x 12) *(proposed)*.</li>
 *   <li>Every cell of the six walls around it is an insulated panel, a cold-room door or the evaporator; at least one door.</li>
 *   <li>A condensing unit touches the walls from outside.</li>
 * </ul>
 */
public final class ColdRoomLayout {
	@FunctionalInterface
	public interface Grid {
		ColdRoomPart at(int x, int y, int z);
	}

	public static final int MAX_W = 10;
	public static final int MAX_H = 4;
	public static final int MAX_L = 10;
	private static final int[][] SIDES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	private ColdRoomLayout() {
	}

	/** Checks the room whose evaporator is at (ex, ey, ez), facing in along (fx, 0, fz). */
	public static ColdRoomCheck check(Grid g, int ex, int ey, int ez, int fx, int fz) {
		if (g.at(ex, ey, ez) != ColdRoomPart.EVAPORATOR) {
			return fail("No evaporator here");
		}
		int sx = ex + fx;
		int sz = ez + fz;
		if (g.at(sx, ey, sz).shell()) {
			return fail("The evaporator must face into the room");
		}
		// flood the inside; it may not spread further from the start than the largest room allows
		Set<Long> seen = new HashSet<>();
		List<int[]> inside = new ArrayList<>();
		ArrayDeque<int[]> q = new ArrayDeque<>();
		q.add(new int[] {sx, ey, sz});
		seen.add(key(sx, ey, sz));
		int maxCells = MAX_W * MAX_H * MAX_L;
		int[] lo = {sx, ey, sz};
		int[] hi = {sx, ey, sz};
		while (!q.isEmpty()) {
			int[] c = q.poll();
			inside.add(c);
			for (int i = 0; i < 3; i++) {
				lo[i] = Math.min(lo[i], c[i]);
				hi[i] = Math.max(hi[i], c[i]);
			}
			if (hi[0] - lo[0] >= Math.max(MAX_W, MAX_L) || hi[1] - lo[1] >= MAX_H || hi[2] - lo[2] >= Math.max(MAX_W, MAX_L) || inside.size() > maxCells) {
				return fail("The walls are not sealed: the inside runs out near " + c[0] + ", " + c[1] + ", " + c[2]);
			}
			for (int[] d : SIDES) {
				int x = c[0] + d[0];
				int y = c[1] + d[1];
				int z = c[2] + d[2];
				if (!g.at(x, y, z).shell() && seen.add(key(x, y, z))) {
					q.add(new int[] {x, y, z});
				}
			}
		}
		int w = hi[0] - lo[0] + 1;
		int h = hi[1] - lo[1] + 1;
		int l = hi[2] - lo[2] + 1;
		if (h > MAX_H || Math.min(w, l) > Math.min(MAX_W, MAX_L) || Math.max(w, l) > Math.max(MAX_W, MAX_L)) {
			return fail("Too big: the inside is " + w + " x " + h + " x " + l + "; at most " + MAX_W + " x " + MAX_H + " x " + MAX_L);
		}
		if (inside.size() != w * h * l) {
			return fail("The inside is not a box (" + inside.size() + " cells in a " + w + " x " + h + " x " + l + " space)");
		}
		// the six walls (no edges or corners needed: they do not touch the inside)
		int doors = 0;
		for (int x = lo[0]; x <= hi[0]; x++) {
			for (int y = lo[1]; y <= hi[1]; y++) {
				for (int z = lo[2]; z <= hi[2]; z++) {
					for (int[] d : SIDES) {
						int nx = x + d[0];
						int ny = y + d[1];
						int nz = z + d[2];
						if (nx >= lo[0] && nx <= hi[0] && ny >= lo[1] && ny <= hi[1] && nz >= lo[2] && nz <= hi[2]) {
							continue;
						}
						ColdRoomPart p = g.at(nx, ny, nz);
						if (p == ColdRoomPart.DOOR) {
							doors++;
						}
					}
				}
			}
		}
		if (doors == 0) {
			return fail("The room needs a cold-room door in a wall");
		}
		// a condensing unit touching the walls from outside
		int[] condenser = null;
		for (int x = lo[0] - 2; x <= hi[0] + 2 && condenser == null; x++) {
			for (int y = lo[1] - 2; y <= hi[1] + 2 && condenser == null; y++) {
				for (int z = lo[2] - 2; z <= hi[2] + 2 && condenser == null; z++) {
					if (g.at(x, y, z) != ColdRoomPart.CONDENSER) {
						continue;
					}
					for (int[] d : SIDES) {
						int nx = x + d[0];
						int ny = y + d[1];
						int nz = z + d[2];
						boolean wall = nx >= lo[0] - 1 && nx <= hi[0] + 1 && ny >= lo[1] - 1 && ny <= hi[1] + 1 && nz >= lo[2] - 1 && nz <= hi[2] + 1;
						if (wall && g.at(nx, ny, nz).shell()) {
							condenser = new int[] {x, y, z};
							break;
						}
					}
				}
			}
		}
		if (condenser == null) {
			return new ColdRoomCheck("No condensing unit touching the walls outside", lo, hi, inside, null);
		}
		return new ColdRoomCheck("", lo, hi, inside, condenser);
	}

	private static long key(int x, int y, int z) {
		return ((long) x & 0x3FFFFF) << 42 | ((long) y & 0xFFFFF) << 22 | ((long) z & 0x3FFFFF);
	}

	private static ColdRoomCheck fail(String problem) {
		return new ColdRoomCheck(problem, null, null, List.of(), null);
	}
}
