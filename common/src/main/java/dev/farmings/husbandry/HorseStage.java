package dev.farmings.husbandry;

import java.util.Locale;

/**
 * A horse's stage (docs/animals/horses.md, "Reproduction"): a foal to weaning at 150 days; a filly becomes a mare by foaling; a colt
 * becomes a stallion at 3 years, or a gelding when cut.
 */
public enum HorseStage {
	FOAL,
	FILLY,
	MARE,
	COLT,
	STALLION,
	GELDING;

	public static HorseStage female(double ageDays, boolean hasFoaled) {
		if (ageDays < Equine.WEANING_DAYS) {
			return FOAL;
		}
		return hasFoaled ? MARE : FILLY;
	}

	public static HorseStage male(double ageDays, boolean castrated) {
		if (ageDays < Equine.WEANING_DAYS) {
			return FOAL;
		}
		if (castrated) {
			return GELDING;
		}
		return ageDays >= Equine.FIRST_BREEDING_DAYS ? STALLION : COLT;
	}

	public boolean foal() {
		return this == FOAL;
	}

	public String label() {
		return name().toLowerCase(Locale.ROOT);
	}
}
