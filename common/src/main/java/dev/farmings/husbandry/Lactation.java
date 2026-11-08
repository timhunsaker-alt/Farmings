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
}
