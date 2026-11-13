package dev.farmings.husbandry.poultry;

import dev.farmings.genetics.Species;
import java.util.Map;

/**
 * How many eggs a hen lays (docs/animals/poultry.md, "Laying"). The shape is the Hy-Line W-36 Leghorn curve by weeks since the first
 * egg: 50% at the start, 96% (the middle of the 95-97% peak) from 7 to 11.6 weeks, then 93, 89, 86, 82, 77 and 72% ten weeks apart.
 * Past the last point it falls 0.5 points a week (proposed). The shape is scaled so a laying year gives the breed's eggs a year, and
 * no hen lays more than one egg a day. Turkeys lay for 28 weeks, then rest until a year from their first egg.
 */
public final class LayingCycle {
	private LayingCycle() {
	}

	private static final double[][] CURVE = {{0, 0.50}, {7, 0.96}, {11.6, 0.96}, {19.6, 0.93}, {29.6, 0.89}, {39.6, 0.86}, {49.6, 0.82},
			{59.6, 0.77}, {69.6, 0.72}};
	/** Weeks a turkey lays each year (The Poultry Site: about 28). */
	public static final double TURKEY_LAYING_WEEKS = 28;

	/** The Hy-Line shape at a week since the first egg. */
	static double shape(double week) {
		if (week < 0) {
			return 0;
		}
		for (int i = 1; i < CURVE.length; i++) {
			if (week <= CURVE[i][0]) {
				double[] a = CURVE[i - 1];
				double[] b = CURVE[i];
				return a[1] + (b[1] - a[1]) * (week - a[0]) / (b[0] - a[0]);
			}
		}
		double[] last = CURVE[CURVE.length - 1];
		return Math.max(0, last[1] - 0.005 * (week - last[0]));
	}

	/** Sum of daily shape values over a laying season of this many weeks, from its start. */
	private static double seasonSum(double weeks) {
		double s = 0;
		for (int d = 0; d < weeks * 7; d++) {
			s += shape(d / 7.0);
		}
		return s;
	}

	private static final double YEAR_SUM = seasonSum(52);
	private static final double TURKEY_SUM = seasonSum(TURKEY_LAYING_WEEKS);

	/**
	 * Eggs a hen lays on one real day of her life (0-1): one real day's lay, given each game day.
	 *
	 * @param ageDays        her age in real days
	 * @param eggScore       her egg_production score, 0-1000
	 */
	public static double eggsPerDay(Species species, Map<String, Integer> c, double ageDays, int eggScore) {
		return eggsPerDay(species, c, ageDays, eggScore, -1);
	}

	/**
	 * The same on a day of the year: a seasonal bird (geese, guinea fowl; poultry.md, "Laying season") lays its year's eggs evenly over its
	 * season and none outside it, up to one a day. {@code dayOfYear} -1 ignores the season.
	 */
	public static double eggsPerDay(Species species, Map<String, Integer> c, double ageDays, int eggScore, int dayOfYear) {
		FowlBreed top = top(c);
		if (top != null && top.seasonal() && dayOfYear >= 0) {
			if (ageDays < Fowl.mean(c, b -> b.firstEggDays) || retired(c, ageDays) || !top.inLay(dayOfYear)) {
				return 0;
			}
			return Math.min(1.0, Fowl.mean(c, b -> b.eggsPerYear) / top.seasonDays() * Fowl.factor(eggScore, Fowl.EGG_RATE_SWING));
		}
		double first = Fowl.mean(c, b -> b.firstEggDays);
		double since = ageDays - first;
		if (since < 0 || since >= Fowl.LAYING_YEARS * 365) {
			return 0;
		}
		double perYear = Fowl.mean(c, b -> b.eggsPerYear);
		double rate;
		if (species == Species.TURKEY) {
			double week = since / 7.0 % 52;
			rate = week >= TURKEY_LAYING_WEEKS ? 0 : shape(week) * perYear / TURKEY_SUM;
		} else {
			rate = shape(since / 7.0) * perYear / YEAR_SUM;
		}
		return Math.min(1.0, rate * Fowl.factor(eggScore, Fowl.EGG_RATE_SWING));
	}

	/** The breed a composition holds most of, or null. */
	public static FowlBreed top(Map<String, Integer> c) {
		FowlBreed best = null;
		int most = -1;
		for (Map.Entry<String, Integer> e : c.entrySet()) {
			FowlBreed b = FowlBreed.byCode(e.getKey());
			if (b != null && e.getValue() > most) {
				most = e.getValue();
				best = b;
			}
		}
		return best;
	}

	/** Retired: past three laying years. */
	public static boolean retired(Map<String, Integer> c, double ageDays) {
		return ageDays - Fowl.mean(c, b -> b.firstEggDays) >= Fowl.LAYING_YEARS * 365;
	}

	/**
	 * Egg weight in grams: the breed figure, following the Hy-Line rise with age (54.7 g at 26 weeks, 58.5 at 32, 63.3 at 70) scaled to
	 * the breed, moved up to ±10% by egg_weight.
	 */
	public static double eggGrams(Map<String, Integer> c, double ageDays, int weightScore) {
		double base = Fowl.mean(c, b -> b.eggGrams);
		double week = (ageDays - Fowl.mean(c, b -> b.firstEggDays)) / 7.0 + 20.4;   // Hy-Line weeks of age (first egg at 143 days)
		double hy;
		if (week <= 26) {
			hy = 54.7;
		} else if (week <= 32) {
			hy = 54.7 + (58.5 - 54.7) * (week - 26) / 6;
		} else if (week <= 70) {
			hy = 58.5 + (63.3 - 58.5) * (week - 32) / 38;
		} else {
			hy = 63.3;
		}
		return base * hy / 58.5 * Fowl.factor(weightScore, Fowl.EGG_WEIGHT_SWING);
	}
}
