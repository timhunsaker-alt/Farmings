package dev.farmings.husbandry;

import java.util.Locale;
import java.util.Map;

/** A pig's stage (docs/animals/pigs.md): piglet to weaning at 21 days; a gilt becomes a sow by farrowing; a boar once he could breed. */
public enum PigStage {
	PIGLET,
	GILT,
	SOW,
	YOUNG_BOAR,
	BOAR,
	BARROW;

	public static PigStage female(double ageDays, boolean hasFarrowed) {
		if (ageDays < Swine.WEANING_DAYS) {
			return PIGLET;
		}
		return hasFarrowed ? SOW : GILT;
	}

	public static PigStage male(Map<String, Integer> c, double ageDays, double weightKg, boolean castrated) {
		if (ageDays < Swine.WEANING_DAYS) {
			return PIGLET;
		}
		if (castrated) {
			return BARROW;
		}
		return Swine.canFirstBreed(ageDays, weightKg) ? BOAR : YOUNG_BOAR;
	}

	public boolean piglet() {
		return this == PIGLET;
	}

	public String label() {
		return name().toLowerCase(Locale.ROOT).replace('_', ' ');
	}
}
