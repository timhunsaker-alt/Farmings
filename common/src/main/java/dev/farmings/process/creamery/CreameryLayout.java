package dev.farmings.process.creamery;

import dev.farmings.husbandry.barn.Orientation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The creamery's forming rule (docs/multiblocks/creamery.md, proposed): the controller, then every other part once, in one straight row on
 * the controller's level starting beside it, in any order, with no gaps. Plain Java.
 */
public final class CreameryLayout {
	private static final Orientation[] FOUR = {Orientation.ALL[0], Orientation.ALL[1], Orientation.ALL[2], Orientation.ALL[3]};

	private CreameryLayout() {
	}

	public static CreameryCheck find(CreameryGrid world, int cx, int cy, int cz) {
		CreameryCheck best = null;
		for (Orientation o : FOUR) {
			CreameryGrid drawn = (ax, y, az) -> {
				int[] w = o.world(ax, az);
				return world.at(cx + w[0], y, cz + w[1]);
			};
			CreameryCheck c = check(drawn, cy, o);
			if (c.formed()) {
				return c;
			}
			if (best == null || c.offsets().size() > best.offsets().size()) {
				best = c;
			}
		}
		return best;
	}

	public static CreameryCheck check(CreameryGrid g, int cy, Orientation o) {
		List<String> problems = new ArrayList<>();
		Map<CreameryPart, Integer> offsets = new EnumMap<>(CreameryPart.class);
		if (g.at(0, cy, 0) != CreameryPart.CONTROLLER) {
			problems.add("No creamery controller here");
			return new CreameryCheck(false, problems, o, offsets);
		}
		int x = 1;
		while (x < 32) {
			CreameryPart p = g.at(x, cy, 0);
			if (p == null || p == CreameryPart.CONTROLLER) {
				break;
			}
			if (offsets.containsKey(p)) {
				problems.add("Two " + name(p) + "s in the row");
				break;
			}
			if (p.length() == 2 && g.at(x + 1, cy, 0) != p) {
				problems.add("The " + name(p) + " must lie along the row (2 cells long)");
				break;
			}
			offsets.put(p, x);
			x += p.length();
		}
		for (CreameryPart p : CreameryPart.values()) {
			if (p != CreameryPart.CONTROLLER && !offsets.containsKey(p)) {
				problems.add("The row has no " + name(p));
			}
		}
		return new CreameryCheck(problems.isEmpty(), problems, o, offsets);
	}

	public static String name(CreameryPart p) {
		return p.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
	}
}
