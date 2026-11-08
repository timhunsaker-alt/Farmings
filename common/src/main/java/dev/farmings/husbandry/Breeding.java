package dev.farmings.husbandry;

import dev.farmings.genetics.Sex;

/**
 * Heat, conception and calf-sex rules (docs/animals/breeding.md, approved by Tim 2026-11-08). Plain Java: times are game ticks.
 */
public final class Breeding {
	/** Heat lasts 0.75 real day (proposed; sources disagree between about 7 h and 24 h). */
	public static final double HEAT_LENGTH_REAL_DAYS = 0.75;
	/** A cow waits 60 real days after calving before her first heat counts (proposed, no source). */
	public static final double WAIT_AFTER_CALVING_DAYS = 60;

	private Breeding() {
	}

	public static long cycleTicks() {
		return TimeScale.realDaysToTicks(Lifecycle.HEAT_CYCLE_DAYS);
	}

	public static long heatTicks() {
		return TimeScale.realDaysToTicks(HEAT_LENGTH_REAL_DAYS);
	}

	/** Can this female come into heat? A heifer that may be bred, or a lactating cow past the waiting period; never one in calf. */
	public static boolean cycles(CattleBreed breed, Stage stage, boolean inCalf, double ageRealDays, double weightKg, double daysSinceCalving) {
		if (inCalf) {
			return false;
		}
		return switch (stage) {
			case HEIFER -> Lifecycle.canFirstBreed(breed, ageRealDays, weightKg);
			case LACTATING_COW -> daysSinceCalving >= WAIT_AFTER_CALVING_DAYS;
			default -> false;
		};
	}

	/** True while the heat window that began at {@code heatStart} is open. */
	public static boolean inHeat(long now, long heatStart) {
		return heatStart >= 0 && now >= heatStart && now < heatStart + heatTicks();
	}

	/** The start of the first heat window at or after {@code now}, stepping the 21-day cycle from {@code heatStart}. */
	public static long nextHeatStart(long now, long heatStart) {
		if (heatStart < 0) {
			return -1;
		}
		long cycle = cycleTicks();
		long start = heatStart;
		while (start + heatTicks() <= now) {
			start += cycle;
		}
		return start;
	}

	/**
	 * The sex a player's next calf must have, or null for an ordinary 50/50 roll. The first two are heifers and the third a bull
	 * (the tutorial rule, per player, decided 2026-11-08).
	 */
	public static Sex forcedSex(int calvesAlreadyBorn) {
		return switch (calvesAlreadyBorn) {
			case 0, 1 -> Sex.FEMALE;
			case 2 -> Sex.MALE;
			default -> null;
		};
	}

	/** Birth weight as a share of the dam's mature weight (proposed; about 6%, so a Holstein calf is about 43 kg, unverified). */
	public static final double BIRTH_WEIGHT_FRACTION = 0.06;
}
