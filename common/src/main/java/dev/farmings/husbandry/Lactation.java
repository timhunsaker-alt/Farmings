package dev.farmings.husbandry;

/**
 * The lactation curve (docs/animals/cattle.md, Milk). Wood's model: yield on day t is a * t^b * e^(-c*t) kg. The shape (b, c)
 * is from the ADSA 2019 abstract M128 (Wisconsin test-day data, later lactations, midpoints of the ranges). The scale a is not
 * taken from that paper: it is solved so the 305-day total equals the USDA/CDCB national standardized average for 2019
 * calvings (Table 7B), which Tim chose on 2026-11-08. Nothing here knows about Minecraft.
 */
public final class Lactation {
	private static final double LB_TO_KG = 0.45359237;
	public static final int DAYS = 305;

	private Lactation() {
	}

	/** Shape and total for one breed. {@code total305Kg} is 0 for breeds that are not milked. */
	public record Curve(double b, double c, double total305Kg, boolean standIn) {
		/** The Wood scale a that makes the 305-day sum equal the total. */
		public double scale() {
			if (total305Kg <= 0) {
				return 0;
			}
			double sum = 0;
			for (int t = 1; t <= DAYS; t++) {
				sum += Math.pow(t, b) * Math.exp(-c * t);
			}
			return total305Kg / sum;
		}

		/** Kilograms of milk a day on day {@code daysInMilk} (1-based); 0 outside 1..305. */
		public double yieldKg(int daysInMilk) {
			if (daysInMilk < 1 || daysInMilk > DAYS) {
				return 0;
			}
			return scale() * Math.pow(daysInMilk, b) * Math.exp(-c * daysInMilk);
		}

		/** Day of peak yield: b / c. */
		public double peakDay() {
			return b / c;
		}
	}

	// Holstein 27,686 lb (USDA 2019 calvings). Shape: ADSA M128, b 0.23-0.24, c 0.0032-0.0036.
	private static final Curve HOLSTEIN = new Curve(0.235, 0.0034, 27686 * LB_TO_KG, false);
	// Jersey 20,740 lb. Shape: ADSA M128, b 0.17-0.19, c 0.0028-0.0032.
	private static final Curve JERSEY = new Curve(0.18, 0.0030, 20740 * LB_TO_KG, false);
	// Brown Swiss 22,737 lb (USDA). No shape found: Holstein's is used as a stand-in.
	private static final Curve BROWN_SWISS = new Curve(0.235, 0.0034, 22737 * LB_TO_KG, true);
	// Gyr: 1,225-2,268 kg in India (IJAnS review); the midpoint 1,746.5 kg and Jersey's shape are stand-ins (proposed).
	private static final Curve GYR = new Curve(0.18, 0.0030, (1225 + 2268) / 2.0, true);
	private static final Curve NONE = new Curve(0, 0, 0, false);

	public static Curve of(CattleBreed breed) {
		return switch (breed) {
			case HOLSTEIN -> HOLSTEIN;
			case JERSEY -> JERSEY;
			case BROWN_SWISS -> BROWN_SWISS;
			case GYR -> GYR;
			case ANGUS, WAGYU -> NONE;
		};
	}

	/** Milk for one real day for a cow of this breed, times a yield factor (1.0 = breed average). Beef breeds give none. */
	public static double dailyKg(CattleBreed breed, int daysInMilk, double yieldFactor) {
		return of(breed).yieldKg(daysInMilk) * yieldFactor;
	}

	/** Yield factor limits (proposed): a cow gives between half and one and a half times her breed's average. */
	public static final double MIN_FACTOR = 0.5;
	public static final double MAX_FACTOR = 1.5;

	/**
	 * The yield factor from the genome (proposed, decided as a default with Tim 2026-11-08): her milk-yield score over the breed's
	 * baseline score (the 0-100 baseline times 10, on the 0-1000 scale), kept between {@link #MIN_FACTOR} and {@link #MAX_FACTOR}.
	 * A breed with no milk baseline gives 1.
	 */
	public static double yieldFactor(CattleBreed breed, int milkYieldScore) {
		if (breed.use == CattleBreed.Use.BEEF) {
			return 1;   // beef cows are not milked; their curve is zero anyway
		}
		int baseline = dev.farmings.genetics.Breeds.known(breed.code)
				? dev.farmings.genetics.Breeds.baseline(breed.code, dev.farmings.genetics.Trait.MILK_YIELD) * 10 : 0;
		if (baseline <= 0) {
			return 1;
		}
		return Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, (double) milkYieldScore / baseline));
	}

	/**
	 * First lactation: about 15% less than later ones (Wisconsin Wood's fits, docs/animals/cattle.md). Later lactations give the full
	 * curve, which is fitted to later lactations.
	 */
	public static final double FIRST_LACTATION_FACTOR = 0.85;

	public static double parityFactor(int lactation) {
		return lactation <= 1 ? FIRST_LACTATION_FACTOR : 1.0;
	}

	/** Which real day of the lactation it is, counting the calving day as 1. */
	public static int daysInMilk(double realDaysSinceCalving) {
		return (int) Math.floor(realDaysSinceCalving) + 1;
	}

	/**
	 * Milk made in one real day (the output of one game day, decided with Tim): the curve times the genome factor times the less of
	 * the fed and watered fractions of her last tending (proposed: milk falls in step with the shortfall; she never dies).
	 */
	public static double producedKg(CattleBreed breed, int daysInMilk, int milkYieldScore, double fed, double watered) {
		return producedKg(breed, daysInMilk, milkYieldScore, fed, watered, 2, 0);
	}

	/** As above, for her {@code lactation}-th lactation (1 = first) and a stall {@code dirty} fraction 0..1 ({@link Hygiene}). */
	public static double producedKg(CattleBreed breed, int daysInMilk, int milkYieldScore, double fed, double watered, int lactation, double dirty) {
		double shortfall = Math.max(0, Math.min(1, Math.min(fed, watered)));
		return dailyKg(breed, daysInMilk, yieldFactor(breed, milkYieldScore)) * shortfall * parityFactor(lactation) * Hygiene.milkFactor(dirty);
	}
}
