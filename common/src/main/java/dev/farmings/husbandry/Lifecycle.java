package dev.farmings.husbandry;

import java.util.OptionalDouble;

/**
 * Cattle life-cycle rules in real days, with their sources in docs/animals/cattle.md. Nothing here knows about Minecraft; the
 * entity asks these functions what happens next and {@link TimeScale} converts to ticks.
 */
public final class Lifecycle {
	/** Heat cycle, about 21 days (range 17-24): LSU AgCenter. */
	public static final double HEAT_CYCLE_DAYS = 21;
	/** Standard lactation record, 305 days (Feedipedia; TO VERIFY in that file). */
	public static final double LACTATION_DAYS = 305;
	/** Conventional dry period, 60 days (Dairy Herd, Farmtario). */
	public static final double DRY_DAYS = 60;
	/** Calving interval target, 365 days. */
	public static final double CALVING_INTERVAL_DAYS = 365;
	/** First breeding at 15 real months (decided with Tim, 2026-11-07): 15 x 365 / 12 days. */
	public static final double FIRST_BREEDING_AGE_DAYS = 15 * 365.0 / 12.0;
	/** A heifer must also reach 55% of mature weight. */
	public static final double BREEDING_WEIGHT_FRACTION = 0.55;
	/** Puberty at about 40% of mature weight (KALRO). */
	public static final double PUBERTY_WEIGHT_FRACTION = 0.40;

	private Lifecycle() {
	}

	/**
	 * A heifer can be bred only when she is at least 15 months old AND has reached the breeding weight. An underfed heifer waits
	 * for the weight; a well-fed one is never bred earlier than 15 months.
	 */
	public static boolean canFirstBreed(CattleBreed breed, double ageRealDays, double weightKg) {
		return ageRealDays >= FIRST_BREEDING_AGE_DAYS && weightKg >= breed.breedingWeightKg();
	}

	/** The real day she calves, counted from the day she was bred. */
	public static double dueDay(CattleBreed breed, double bredDay) {
		return bredDay + breed.gestationDays;
	}

	/** The real day her lactation ends, counted from the day she calved. */
	public static double lactationEndDay(double calvingDay) {
		return calvingDay + LACTATION_DAYS;
	}

	public static Stage femaleStage(CattleBreed breed, double weightKg, boolean hasCalved, boolean pregnant, double daysSinceCalving,
			OptionalDouble daysToDue, boolean retired) {
		if (retired) {
			return Stage.RETIRED_COW;
		}
		if (!hasCalved) {
			if (weightKg < breed.pubertyWeightKg()) {
				return Stage.CALF;
			}
			return pregnant ? Stage.BRED_HEIFER : Stage.HEIFER;
		}
		boolean dueSoon = pregnant && daysToDue.isPresent() && daysToDue.getAsDouble() <= DRY_DAYS;
		if (dueSoon || daysSinceCalving >= LACTATION_DAYS) {
			return Stage.DRY_COW;
		}
		return Stage.LACTATING_COW;
	}

	public static Stage maleStage(CattleBreed breed, double weightKg, boolean castrated) {
		if (castrated) {
			return Stage.STEER;
		}
		return weightKg < breed.pubertyWeightKg() ? Stage.BULL_CALF : Stage.BULL;
	}

	/** A bull does not give milk and a steer does not breed. */
	public static boolean givesMilk(Stage stage) {
		return stage == Stage.LACTATING_COW;
	}

	public static boolean canBreed(Stage stage) {
		return stage == Stage.HEIFER || stage == Stage.BULL || stage == Stage.LACTATING_COW;
	}
}
