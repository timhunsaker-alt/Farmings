package dev.farmings.husbandry;

import java.util.Map;

/**
 * A sheep's stage (docs/animals/sheep.md, "Sexes and stages"). Lambs are lambs until weaning at 120 days *(proposed)*; a ewe lamb
 * becomes a ewe by lambing; a ram lamb becomes a ram once he could breed (7 months and the first-breeding weight); a castrated male is a
 * wether.
 */
public enum SheepStage {
	LAMB,
	EWE_LAMB,
	EWE,
	RETIRED_EWE,
	RAM_LAMB,
	RAM,
	WETHER;

	/** Weaning age *(proposed: lambs are commonly weaned at 60-120 days; no single source)*. */
	public static final double WEANING_DAYS = 120;

	public static SheepStage female(double ageDays, boolean hasLambed, boolean retired) {
		if (retired) {
			return RETIRED_EWE;
		}
		if (ageDays < WEANING_DAYS) {
			return LAMB;
		}
		return hasLambed ? EWE : EWE_LAMB;
	}

	public static SheepStage male(Map<String, Integer> composition, double ageDays, double weightKg, boolean castrated) {
		if (ageDays < WEANING_DAYS) {
			return LAMB;
		}
		if (castrated) {
			return WETHER;
		}
		return Flock.canFirstBreed(composition, ageDays, weightKg) ? RAM : RAM_LAMB;
	}

	public boolean lamb() {
		return this == LAMB;
	}

	public String label() {
		return name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
	}
}
