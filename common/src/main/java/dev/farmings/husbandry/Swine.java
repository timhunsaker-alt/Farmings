package dev.farmings.husbandry;

import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Pig rules in real days (docs/animals/pigs.md), beside {@link Flock} and {@link Herd}. Plain Java. */
public final class Swine {
	/** "normally 21 days but can range from 18 to 24 days" (UW A3858-03). */
	public static final double CYCLE_DAYS = 21;
	/** Gilts 1-2 days, sows 2-3 days (MSD): 2. */
	public static final double HEAT_DAYS = 2;
	/** "114 days (3 months, 3 weeks, and 3 days)" (UW). */
	public static final double GESTATION_DAYS = 114;
	/** "have a 300 lb gilt at 210 days of age" (K-State MF2302). */
	public static final double FIRST_BREEDING_DAYS = 210;
	public static final double FIRST_BREEDING_KG = 136;
	/** Back in heat 4-6 days after weaning (UW): 5. */
	public static final double WEAN_TO_HEAT_DAYS = 5;
	/** "Average age at weaning 20.99" (PigCHAMP 2024). */
	public static final double WEANING_DAYS = 21;
	/** "Farrowing rate 83.81" (PigCHAMP 2024), as the chance a service conceives *(proposed use)*. */
	public static final double CONCEPTION = 0.84;
	/** A boar serves sows this far away: the same pen *(proposed, as sheep)*. */
	public static final double SERVICE_RANGE = 8.0;
	/** fertility at 0 or 1000 moves the litter mean by this many piglets *(proposed)*. */
	public static final double LITTER_SWING = 1.0;
	/** The litter roll's spread around its mean *(proposed)*. */
	public static final double LITTER_SD = 2.5;
	/** Dressing, "250 lb x 70%" (UW). */
	public static final double DRESSING = 0.70;

	private Swine() {
	}

	static double weighted(Map<String, Integer> c, ToDoubleFunction<PigBreed> f) {
		double sum = 0;
		double w = 0;
		for (Map.Entry<String, Integer> e : c.entrySet()) {
			if (PigBreed.isPig(e.getKey())) {
				sum += f.applyAsDouble(PigBreed.byCode(e.getKey())) * e.getValue();
				w += e.getValue();
			}
		}
		return w == 0 ? 0 : sum / w;
	}

	public static PigBreed topBreed(Map<String, Integer> c) {
		PigBreed best = PigBreed.LARGE_WHITE;
		int most = -1;
		for (PigBreed b : PigBreed.values()) {
			int bp = c.getOrDefault(b.code, 0);
			if (bp > most) {
				most = bp;
				best = b;
			}
		}
		return best;
	}

	public static double matureKg(Map<String, Integer> c, boolean male) {
		return weighted(c, b -> male ? b.boarKg : b.sowKg);
	}

	/** Growth points (age days, kg): birth 1.5 kg (UW), weaning 5.7 kg at 21 d, 24 kg at 66 d and 120 kg at 186 d (Knauer & Hostetler 2013). */
	private static final double[][] GROWTH = {{0, 1.5}, {21, 5.7}, {66, 24}, {186, 120}};
	/** Mature weight at two years *(proposed)*. */
	public static final double MATURITY_DAYS = 730;

	public static double targetKg(Map<String, Integer> c, boolean male, double ageDays) {
		if (ageDays <= 0) {
			return GROWTH[0][1];
		}
		for (int i = 1; i < GROWTH.length; i++) {
			if (ageDays <= GROWTH[i][0]) {
				double[] a = GROWTH[i - 1];
				double[] b = GROWTH[i];
				return a[1] + (b[1] - a[1]) * (ageDays - a[0]) / (b[0] - a[0]);
			}
		}
		double mature = Math.max(120, matureKg(c, male));
		if (ageDays >= MATURITY_DAYS) {
			return mature;
		}
		double[] last = GROWTH[GROWTH.length - 1];
		return last[1] + (mature - last[1]) * (ageDays - last[0]) / (MATURITY_DAYS - last[0]);
	}

	public static double gainKg(Map<String, Integer> c, boolean male, double ageDays, double days, double fed) {
		if (days <= 0) {
			return 0;
		}
		return Math.max(0, targetKg(c, male, ageDays + days) - targetKg(c, male, ageDays)) * Math.max(0, Math.min(1, fed));
	}

	public static boolean canFirstBreed(double ageDays, double weightKg) {
		return ageDays >= FIRST_BREEDING_DAYS && weightKg >= FIRST_BREEDING_KG;
	}

	// ---- farrowing ----

	/** The mean total born: the breed figure at a first or later litter, moved by fertility. */
	public static double litterMean(Map<String, Integer> c, boolean first, int fertilityScore) {
		return weighted(c, b -> first ? b.litterFirst : b.litterLater) + (fertilityScore - 500) / 500.0 * LITTER_SWING;
	}

	/** Total born for a normal deviate {@code z}: mean + 2.5 z, rounded, 1 to 20. */
	public static int litter(Map<String, Integer> c, boolean first, int fertilityScore, double z) {
		return (int) Math.max(1, Math.min(20, Math.round(litterMean(c, first, fertilityScore) + LITTER_SD * z)));
	}

	public static double stillbornRate(Map<String, Integer> c) {
		return weighted(c, b -> b.stillbornRate);
	}

	// ---- feed, water, manure ----

	/** Feed as fed, kg a day: finishers 2.2 at 80 kg (Knauer & Hostetler 2013, derived) by weight^0.75; gestating sows and boars 2.34; lactating 6.5. */
	public static double feedKgPerDay(double weightKg, boolean adultBreeder, boolean lactating) {
		if (lactating) {
			return 6.5;
		}
		if (adultBreeder) {
			return 2.34;
		}
		return 2.2 * Math.pow(weightKg / 80.0, 0.75);
	}

	/**
	 * Water, L a day (NDSU, livestock water requirements, gal/head/day): finishers "Finishing (100-250 lbs.)" "2 to 3", 9.5 L at 80 kg;
	 * "Pregnant sows" "3 to 6", 17 L (also boars, *proposed*); "Lactating sows" "5 to 8", 25 L.
	 */
	public static double waterLPerDay(double weightKg, boolean adultBreeder, boolean lactating) {
		if (lactating) {
			return 25;
		}
		if (adultBreeder) {
			return 17;
		}
		return 9.5 * Math.pow(weightKg / 80.0, 0.75);
	}

	/** Manure as excreted, kg a day (MWPS-18 Table 6): growing 4.9 at 100 kg by weight^0.75 (1.4 at 18 kg); sow 4.1; lactating 10.6; boar 2.8. */
	public static double manureKgPerDay(double weightKg, boolean sow, boolean boar, boolean lactating) {
		if (lactating) {
			return 10.6;
		}
		if (sow) {
			return 4.1;
		}
		if (boar) {
			return 2.8;
		}
		return 4.9 * Math.pow(weightKg / 100.0, 0.75);
	}

	/** Share of the day's feed a pig takes rooting on pasture: growing pigs 15% (NC State CEFS, 10-20%), sows 50% (Utah State). */
	public static double forageShare(boolean adultBreeder) {
		return adultBreeder ? 0.5 : 0.15;
	}

	/** Pen blocks a pig takes: sows and boars 2 (16 sq ft), pigs of 45 kg or more 1 (8 sq ft), smaller 0.5 (PIH-55). */
	public static double penBlocks(double weightKg, boolean adultBreeder) {
		return adultBreeder ? 2 : weightKg >= 45 ? 1 : 0.5;
	}

	// ---- meat ----

	/** The cuts of a pig, item counts (pigs.md, "Meat"; K-State MF3396 shares of the hot carcass, proposed item sizes). */
	public record Cuts(int hams, double hamKg, int chops, int bellies, int shoulders, int fat) {
	}

	public static Cuts cuts(double liveKg) {
		double carcass = liveKg * DRESSING;
		if (liveKg < 30) {
			return new Cuts(0, 0, Math.max(1, (int) Math.floor(carcass * 0.5 / 0.5)), 0, 0, 0);
		}
		return new Cuts(2, carcass * 0.158 / 2, (int) Math.floor(carcass * 0.146 / 0.5), (int) Math.floor(carcass * 0.126 / 2),
				(int) Math.floor(carcass * (0.086 + 0.080 + 0.078) / 2), (int) Math.floor(carcass * 0.15 / 1));
	}
}
