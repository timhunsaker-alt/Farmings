package dev.farmings.health;

import dev.farmings.genetics.Species;
import dev.farmings.husbandry.health.Disease;
import dev.farmings.husbandry.health.HealthRecord;
import dev.farmings.husbandry.health.HealthRecord.Stage;

/**
 * The signs of sickness an animal shows without a kit (docs/animal-health.md, "What the player sees"), as flags the server syncs to the
 * client, which draws them: head low when sick, a limp when lame, a red band on the hind legs under a withdrawal, and erysipelas' diamond
 * marks on a pig. Nothing else is drawn (no gore).
 */
public final class HealthSigns {
	public static final int SICK = 1;
	public static final int LAME = 2;
	public static final int BAND = 4;
	public static final int DIAMONDS = 8;

	private HealthSigns() {
	}

	public static int of(Species sp, HealthRecord r, long now) {
		int f = 0;
		if (r.sick()) {
			f |= SICK;
		}
		if (r.lame()) {
			f |= LAME;
		}
		if (now < r.milkUntil() || now < r.meatUntil()) {
			f |= BAND;
		}
		HealthRecord.Case c = r.caseOf(Disease.ERYSIPELAS);
		if (sp == Species.PIG && c != null && (c.stage == Stage.SICK || c.stage == Stage.CHRONIC)) {
			f |= DIAMONDS;
		}
		return f;
	}

	public static boolean has(int flags, int sign) {
		return (flags & sign) != 0;
	}
}
