package dev.farmings.husbandry.shearing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whether a shearing shed forms (docs/multiblocks/shearing-shed.md): a shearing stand with a catching pen beside it that opens onto a sheep
 * shed's pen, and a wool table beside the stand; a wool press beside the table bales the fleeces. Plain Java over a {@link Grid}.
 */
public final class ShearingLayout {
	/** Largest catching pen, in cells *(proposed)*. */
	public static final int MAX_CATCHING = 16;
	/** A sheep is shorn in this many ticks: real shearing takes 2-3 minutes (Wikipedia, sheep shearer); 5 seconds is a game size *(proposed)*. */
	public static final int SHEAR_TICKS = 100;
	/** Share of a fleece taken off as skirtings (belly and edge wool) at the wool table *(proposed; no figure found)*. */
	public static final double SKIRTING_SHARE = 0.12;
	/** Fleeces of one grade and colour in a bale *(proposed)*. */
	public static final int BALE_FLEECES = 8;
	/** The shearing machine's motor, watts *(proposed; no rating found)*. */
	public static final double MOTOR_WATTS = 500;

	public enum Cell { STAND, CATCHING_PEN, SHED_FLOOR, WOOL_TABLE, WOOL_PRESS, OTHER }

	public interface Grid {
		Cell at(int x, int y, int z);
	}

	public record Pos(int x, int y, int z) {
		Pos offset(int dx, int dz) {
			return new Pos(x + dx, y, z + dz);
		}
	}

	/**
	 * @param standPoint the catching-pen cell beside the stand, where a sheep is shorn
	 * @param shedFloor  a sheep-shed pen cell the catching pen opens onto
	 * @param press      null when there is no press
	 */
	public record Check(String problem, List<Pos> catching, Pos standPoint, Pos shedFloor, Pos table, Pos press) {
		public boolean formed() {
			return problem.isEmpty();
		}

		public String status() {
			return formed() ? "FORMED" : problem;
		}
	}

	private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private ShearingLayout() {
	}

	public static Check check(Grid grid, int sx, int sy, int sz) {
		Pos stand = new Pos(sx, sy, sz);
		Pos standPoint = null;
		Pos table = null;
		ArrayDeque<Pos> queue = new ArrayDeque<>();
		Set<Pos> seen = new HashSet<>();
		for (int[] s : SIDES) {
			Pos n = stand.offset(s[0], s[1]);
			Cell c = grid.at(n.x(), n.y(), n.z());
			if (c == Cell.CATCHING_PEN) {
				if (standPoint == null) {
					standPoint = n;
				}
				if (seen.add(n)) {
					queue.add(n);
				}
			} else if (c == Cell.WOOL_TABLE && table == null) {
				table = n;
			}
		}
		if (standPoint == null) {
			return new Check("No catching pen beside the shearing stand", List.of(), null, null, table, null);
		}
		List<Pos> catching = new ArrayList<>();
		Pos shedFloor = null;
		while (!queue.isEmpty() && catching.size() <= MAX_CATCHING) {
			Pos p = queue.poll();
			catching.add(p);
			for (int[] s : SIDES) {
				Pos n = p.offset(s[0], s[1]);
				Cell c = grid.at(n.x(), n.y(), n.z());
				if (c == Cell.CATCHING_PEN && seen.add(n)) {
					queue.add(n);
				} else if (c == Cell.SHED_FLOOR && shedFloor == null) {
					shedFloor = n;
				}
			}
		}
		if (catching.size() > MAX_CATCHING) {
			return new Check("The catching pen is larger than " + MAX_CATCHING + " cells", List.copyOf(catching), standPoint, shedFloor, table, null);
		}
		if (shedFloor == null) {
			return new Check("The catching pen does not open onto a sheep shed's pen", List.copyOf(catching), standPoint, null, table, null);
		}
		if (table == null) {
			return new Check("No wool table beside the shearing stand", List.copyOf(catching), standPoint, shedFloor, null, null);
		}
		Pos press = null;
		for (int[] s : SIDES) {
			Pos n = table.offset(s[0], s[1]);
			if (grid.at(n.x(), n.y(), n.z()) == Cell.WOOL_PRESS) {
				press = n;
				break;
			}
		}
		return new Check("", List.copyOf(catching), standPoint, shedFloor, table, press);
	}
}
