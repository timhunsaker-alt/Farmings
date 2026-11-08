package dev.farmings.husbandry;

/**
 * How a calf grows to her mature weight (docs/animals/growth.md, approved by Tim 2026-11-08). One target curve of weight by age: straight
 * lines from birth to 15 months to 48 months. Actual gain is the curve's gain times how well she was fed, so an underfed heifer grows
 * slower and never catches up. Plain Java.
 */
public final class Growth {
	/** Birth weight as a fraction of mature weight *(proposed, unverified)*. Shared with {@link Breeding#BIRTH_WEIGHT_FRACTION}. */
	public static final double BIRTH_FRACTION = Breeding.BIRTH_WEIGHT_FRACTION;
	/** About 15 months, in real days (30.4 days a month). */
	public static final double FIFTEEN_MONTHS_DAYS = 456;
	/** Mature weight is reached at 48 months *(proposed, no source)*. */
	public static final double MATURITY_DAYS = 1460;
	/** Stand-in 15-month fraction for breeds without a sourced weight (Holstein's). */
	public static final double STAND_IN_FRACTION_AT_15_MONTHS = 0.60;

	private Growth() {
	}

	/** Sourced weight at about 15 months (Duplessis and colleagues, via Dairy Producer), or 0 when the breed has none. */
	static double sourcedWeightAt15MonthsKg(CattleBreed breed) {
		return switch (breed) {
			case HOLSTEIN -> 425;
			case BROWN_SWISS -> 379;
			case JERSEY -> 297;
			default -> 0;
		};
	}

	public static double weightAt15MonthsKg(CattleBreed breed) {
		double sourced = sourcedWeightAt15MonthsKg(breed);
		return sourced > 0 ? sourced : breed.matureWeightKg * STAND_IN_FRACTION_AT_15_MONTHS;
	}

	/** The target weight at an age in real days: the unstressed, well-fed curve. */
	public static double targetKg(CattleBreed breed, double ageDays) {
		double mature = breed.matureWeightKg;
		double birth = mature * BIRTH_FRACTION;
		double at15 = weightAt15MonthsKg(breed);
		if (ageDays <= 0) {
			return birth;
		}
		if (ageDays >= MATURITY_DAYS) {
			return mature;
		}
		if (ageDays <= FIFTEEN_MONTHS_DAYS) {
			return birth + (at15 - birth) * (ageDays / FIFTEEN_MONTHS_DAYS);
		}
		return at15 + (mature - at15) * ((ageDays - FIFTEEN_MONTHS_DAYS) / (MATURITY_DAYS - FIFTEEN_MONTHS_DAYS));
	}

	/**
	 * Weight gained over {@code days} real days starting at {@code ageDays}: the target curve's gain over that span (so a long step across a
	 * bend is right) times the fed fraction (0 to 1). She never gains negative weight.
	 */
	public static double gainKg(CattleBreed breed, double ageDays, double days, double fed) {
		if (days <= 0) {
			return 0;
		}
		double f = Math.max(0, Math.min(1, fed));
		return Math.max(0, targetKg(breed, ageDays + days) - targetKg(breed, ageDays)) * f;
	}
}
