package dev.farmings.husbandry;

import java.util.Locale;
import java.util.Map;

/**
 * A goat's stage (docs/animals/goats.md, "Sexes and stages"): kids until weaning at 90 days; a doeling becomes a doe by kidding; a buckling
 * becomes a buck once he could breed (7 months and 60% of mature weight); a castrated male is a wether.
 */
public enum GoatStage {
	KID,
	DOELING,
	DOE,
	RETIRED_DOE,
	BUCKLING,
	BUCK,
	WETHER;

	/** Dam-reared kids weaned at 90 days, the standard adjustment age (Penn State, goats.md). */
	public static final double WEANING_DAYS = 90;

	public static GoatStage female(double ageDays, boolean hasKidded, boolean retired) {
		if (retired) {
			return RETIRED_DOE;
		}
		if (ageDays < WEANING_DAYS) {
			return KID;
		}
		return hasKidded ? DOE : DOELING;
	}

	public static GoatStage male(Map<String, Integer> composition, double ageDays, double weightKg, boolean castrated) {
		if (ageDays < WEANING_DAYS) {
			return KID;
		}
		if (castrated) {
			return WETHER;
		}
		return Herd.canFirstBreed(composition, false, ageDays, weightKg) ? BUCK : BUCKLING;
	}

	public boolean kid() {
		return this == KID;
	}

	public String label() {
		return name().toLowerCase(Locale.ROOT).replace('_', ' ');
	}
}
