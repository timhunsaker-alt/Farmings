package dev.farmings.husbandry.parlor;

import dev.farmings.husbandry.barn.Orientation;
import java.util.ArrayList;
import java.util.List;

/**
 * The forming rules of the milking parlor (docs/multiblocks/parlor.md), drawn with the tank at the east end. The layout is a proposal
 * (the approved spec listed the parts but no geometry). The controller stands at (cx, cy, cz), floor level cy-1:
 * <pre>
 *   floor: vacuum pump at the far end, then the milk line, one cell per stall, running to the controller
 *   level: a milking unit above the line at each stall; the controller at the tank end
 *   z+1..z+2: a parlor stall under each unit (1 wide, 2 long), one cow each
 *   x+1..x+2, z..z+1, two high: the bulk tank, beside the controller
 * </pre>
 * Plain Java: {@link #check} asks a {@link ParlorGrid} what stands in each cell.
 */
public final class ParlorLayout {
	public static final int MIN_STALLS = 2;
	public static final int MAX_STALLS = 12;

	private ParlorLayout() {
	}

	/** Tries all eight ways the parlor could lie and returns the one that forms, or the one with the fewest problems. */
	public static ParlorCheck find(ParlorGrid world, int controllerX, int controllerY, int controllerZ) {
		ParlorCheck best = null;
		for (Orientation o : Orientation.ALL) {
			ParlorGrid drawn = (ax, y, az) -> {
				int[] w = o.world(ax, az);
				return world.at(controllerX + w[0], y, controllerZ + w[1]);
			};
			ParlorCheck c = check(drawn, 0, controllerY, 0, o);
			if (c.formed()) {
				return c;
			}
			if (best == null || c.problems().size() < best.problems().size()) {
				best = c;
			}
		}
		return best;
	}

	public static ParlorCheck check(ParlorGrid g, int cx, int cy, int cz, Orientation orientation) {
		List<String> problems = new ArrayList<>();
		int fl = cy - 1;
		if (g.at(cx, cy, cz) != ParlorPart.CONTROLLER) {
			problems.add("No parlor controller here");
			return new ParlorCheck(false, 0, problems, orientation, ParlorCheck.NO_PUMP);
		}
		// the bulk tank: 2 x 2 x 2 beside the controller, on the tank side
		boolean tank = true;
		for (int dx = 1; dx <= 2; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				for (int y = fl; y <= cy; y++) {
					tank &= g.at(cx + dx, y, cz + dz) == ParlorPart.BULK_TANK;
				}
			}
		}
		if (!tank) {
			problems.add("The bulk tank is missing beside the controller");
		}
		// the vacuum pump: the first one along the floor line, going away from the tank
		int xp = Integer.MIN_VALUE;
		for (int x = cx - 1; x >= cx - MAX_STALLS - 1; x--) {
			if (g.at(x, fl, cz) == ParlorPart.VACUUM_PUMP) {
				xp = x;
				break;
			}
		}
		if (xp == Integer.MIN_VALUE) {
			problems.add("No vacuum pump at the far end of the milk line");
			return new ParlorCheck(false, 0, problems, orientation, ParlorCheck.NO_PUMP);
		}
		int stalls = cx - xp - 1;
		if (stalls < MIN_STALLS) {
			problems.add("A parlor needs at least " + MIN_STALLS + " stalls; this one has " + stalls);
			return new ParlorCheck(false, stalls, problems, orientation, xp - cx);
		}
		for (int x = xp + 1; x <= cx; x++) {
			if (g.at(x, fl, cz) != ParlorPart.MILK_LINE) {
				problems.add("Milk line missing " + (x - xp) + " cells from the pump");
				break;
			}
		}
		List<Boolean> sheep = new ArrayList<>();
		for (int i = 0; i < stalls; i++) {
			int x = xp + 1 + i;
			if (g.at(x, cy, cz) != ParlorPart.MILKING_UNIT) {
				problems.add("Stall " + (i + 1) + " has no milking unit");
			}
			// a cow stall is two parlor-stall cells; a sheep stall is one sheep-stall cell (sheep.md: dairy ewes)
			boolean cow = g.at(x, fl, cz + 1) == ParlorPart.PARLOR_STALL && g.at(x, fl, cz + 2) == ParlorPart.PARLOR_STALL;
			boolean ewe = g.at(x, fl, cz + 1) == ParlorPart.SHEEP_STALL;
			if (!cow && !ewe) {
				problems.add("Stall " + (i + 1) + " has no parlor stall");
			}
			sheep.add(ewe);
		}
		return new ParlorCheck(problems.isEmpty(), stalls, problems, orientation, xp - cx, List.copyOf(sheep));
	}
}
