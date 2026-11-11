package dev.farmings.husbandry.poultry;

import dev.farmings.genetics.Species;

/**
 * Fertile eggs, incubation and broodiness (docs/animals/poultry.md, "Mating and fertile eggs", "Incubation and hatching";
 * docs/multiblocks/coop.md, "Broody hens"). Days are real days.
 */
public final class Clutch {
	private Clutch() {
	}

	/** A broody hen sits on at most this many eggs (a wild turkey's clutch is 10-14; proposed for all). */
	public static final int MAX_SITTING = 12;
	/** Eggs a nest needs before its hen may go broody (proposed). */
	public static final int BROODY_MIN_EGGS = 6;
	/** The daily broody chance at the top score (proposed). */
	public static final double BROODY_DAILY_AT_MAX = 0.1;

	public enum Stage {
		/** Warm and turned. */
		SETTING,
		/** The last days: no turning, high humidity. */
		LOCKDOWN,
		HATCHING
	}

	/** Whether a hen last mated {@code sinceMatingDays} ago lays fertile eggs. */
	public static boolean fertile(Species s, double sinceMatingDays) {
		return sinceMatingDays >= 0 && sinceMatingDays <= Fowl.fertileDays(s);
	}

	/** Can an egg still be started, given the days it has waited unwarmed? */
	public static boolean hatchable(double daysWaited) {
		return daysWaited <= Fowl.HATCHABLE_DAYS;
	}

	public static Stage stage(FowlBreed b, double daysWarm) {
		if (daysWarm >= b.incubationDays) {
			return Stage.HATCHING;
		}
		return daysWarm >= b.stopTurningDay ? Stage.LOCKDOWN : Stage.SETTING;
	}

	/** The chance a hen goes broody on one game day, from her broodiness score (0-1000), if her nest holds enough eggs. */
	public static double broodyChance(int broodinessScore, int eggsInNest) {
		if (eggsInNest < BROODY_MIN_EGGS) {
			return 0;
		}
		return Math.max(0, Math.min(1000, broodinessScore)) / 1000.0 * BROODY_DAILY_AT_MAX;
	}
}
