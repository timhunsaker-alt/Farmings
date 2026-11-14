package dev.farmings.husbandry;

import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Goat rules in real days (docs/animals/goats.md), beside {@link Flock} for sheep. Nothing here knows about Minecraft; the goat entity asks
 * these and {@link TimeScale} converts to ticks. Crosses take the breed-weighted average (composition in basis points).
 */
public final class Herd {
	/** Oestrous cycle 21 days (Merck; NC State 18-22). */
	public static final double CYCLE_DAYS = 21;
	/** Standing heat, 36 hours (Merck). */
	public static final double HEAT_DAYS = 1.5;
	/** Gestation, about 150 days (Langston University), every breed. */
	public static final double GESTATION_DAYS = 150;
	/** First breeding not before 7 months (the middle of Merck's 6-8 month puberty)... */
	public static final double FIRST_BREEDING_AGE_DAYS = 7 * 365.0 / 12.0;
	/** ...and at 60% of mature weight (Merck). */
	public static final double FIRST_BREEDING_FRACTION = 0.60;
	/** A doe cycles again one cycle after kidding *(proposed: as sheep wait)*. */
	public static final double WAIT_AFTER_KIDDING_DAYS = CYCLE_DAYS;
	/** Mature weight at 24 months *(proposed: the sheep shape)*. */
	public static final double MATURITY_DAYS = 730;
	/** A trait at 0 or 1000 moves milk by this share *(proposed: as sheep)*. */
	public static final double TRAIT_SWING = 0.20;
	/** fertility at 0 or 1000 moves the litter mean by this much *(proposed: as sheep)*. */
	public static final double LITTER_SWING = 0.2;
	/** A first kidding's mean is the later mean x 1.37 / 1.56, the Saanen ratio (TO VERIFY, Togo study) *(derived)*. */
	public static final double FIRST_KIDDING_RATIO = 1.37 / 1.56;

	private Herd() {
	}

	static double weighted(Map<String, Integer> compositionBp, ToDoubleFunction<GoatBreed> f) {
		double sum = 0;
		double weight = 0;
		for (Map.Entry<String, Integer> e : compositionBp.entrySet()) {
			if (!GoatBreed.isGoat(e.getKey())) {
				continue;
			}
			sum += f.applyAsDouble(GoatBreed.byCode(e.getKey())) * e.getValue();
			weight += e.getValue();
		}
		return weight == 0 ? 0 : sum / weight;
	}

	/** The breed this composition holds most of (ties to the first in breed order). */
	public static GoatBreed topBreed(Map<String, Integer> c) {
		GoatBreed best = GoatBreed.SAANEN;
		int most = -1;
		for (GoatBreed b : GoatBreed.values()) {
			int bp = c.getOrDefault(b.code, 0);
			if (bp > most) {
				most = bp;
				best = b;
			}
		}
		return best;
	}

	public static double matureWeightKg(Map<String, Integer> c, boolean male) {
		return weighted(c, b -> male ? b.buckKg : b.doeKg);
	}

	/** Share of the genome that is Saanen or Nubian, 0-1: only these does give parlor milk, in proportion. */
	public static double dairyShare(Map<String, Integer> c) {
		double s = 0;
		for (GoatBreed b : GoatBreed.values()) {
			if (b.dairy()) {
				s += c.getOrDefault(b.code, 0) / 10_000.0;
			}
		}
		return s;
	}

	// ---- growth ----

	/** Target weight: birth weight, a straight line to 60% of mature at 7 months, then to mature at 24 months. */
	public static double targetKg(Map<String, Integer> c, boolean male, double ageDays) {
		double mature = matureWeightKg(c, male);
		double birth = weighted(c, b -> b.birthKg);
		double at7 = mature * FIRST_BREEDING_FRACTION;
		if (ageDays <= 0) {
			return birth;
		}
		if (ageDays >= MATURITY_DAYS) {
			return mature;
		}
		if (ageDays <= FIRST_BREEDING_AGE_DAYS) {
			return birth + (at7 - birth) * ageDays / FIRST_BREEDING_AGE_DAYS;
		}
		return at7 + (mature - at7) * (ageDays - FIRST_BREEDING_AGE_DAYS) / (MATURITY_DAYS - FIRST_BREEDING_AGE_DAYS);
	}

	public static double gainKg(Map<String, Integer> c, boolean male, double ageDays, double days, double fed) {
		if (days <= 0) {
			return 0;
		}
		return Math.max(0, targetKg(c, male, ageDays + days) - targetKg(c, male, ageDays)) * Math.max(0, Math.min(1, fed));
	}

	/** A kid's birth weight: the breed weight, 92% for a twin and 87% for a triplet (Mellado et al., Anim. Prod. Sci. 51:144). */
	public static double birthKg(Map<String, Integer> c, int litter) {
		double f = litter <= 1 ? 1.0 : litter == 2 ? 0.92 : 0.87;
		return weighted(c, b -> b.birthKg) * f;
	}

	public static boolean canFirstBreed(Map<String, Integer> c, boolean male, double ageDays, double weightKg) {
		return ageDays >= FIRST_BREEDING_AGE_DAYS && weightKg >= matureWeightKg(c, male) * FIRST_BREEDING_FRACTION;
	}

	// ---- kidding ----

	/** The goats.md odds by mean: Saanen first kidding 1.37 (65/33/2 proposed), later 1.56 (50/44/6 proposed), Boer 1.64 (FAO 43/50/7). */
	private static final double[][] LITTER_TABLE = {
			{1.0, 1.00, 0.00, 0.00},
			{1.37, 0.65, 0.33, 0.02},
			{1.56, 0.50, 0.44, 0.06},
			{1.64, 0.43, 0.50, 0.07},
			{2.0, 0.25, 0.50, 0.25},     // a fertile cross's ceiling (proposed)
	};

	public static Flock.Litter litterFor(double mean) {
		double m = Math.max(1.0, Math.min(2.0, mean));
		for (int i = 1; i < LITTER_TABLE.length; i++) {
			double[] lo = LITTER_TABLE[i - 1];
			double[] hi = LITTER_TABLE[i];
			if (m <= hi[0]) {
				double t = (m - lo[0]) / (hi[0] - lo[0]);
				return new Flock.Litter(lo[1] + t * (hi[1] - lo[1]), lo[2] + t * (hi[2] - lo[2]), lo[3] + t * (hi[3] - lo[3]));
			}
		}
		double[] last = LITTER_TABLE[LITTER_TABLE.length - 1];
		return new Flock.Litter(last[1], last[2], last[3]);
	}

	/** A doe's litter odds: the breed-weighted mean (lower at a first kidding), moved up to ±0.2 by her fertility score (0-1000). */
	public static Flock.Litter litter(Map<String, Integer> c, boolean firstKidding, int fertilityScore) {
		double mean = weighted(c, b -> b.litterMean) * (firstKidding ? FIRST_KIDDING_RATIO : 1) + (fertilityScore - 500) / 500.0 * LITTER_SWING;
		return litterFor(mean);
	}

	/** Chance a kid is stillborn: 6% (German dairy goat farms, "a mean stillbirth rate of 6.4%"), a single 0.8x and a twin or triplet 1.2x *(proposed)*. */
	public static double stillbirthRate(int litter) {
		return 0.06 * (litter <= 1 ? 0.8 : 1.2);
	}

	// ---- the breeding season ----

	/** Whether a doe of this breed cycles on this day of the year (the sheep calendar, Flock.dayOfYear). */
	public static boolean inSeason(GoatBreed b, int dayOfYear) {
		if (b.seasonStart < 0) {
			return true;
		}
		return b.seasonStart <= b.seasonEnd ? dayOfYear >= b.seasonStart && dayOfYear <= b.seasonEnd : dayOfYear >= b.seasonStart || dayOfYear <= b.seasonEnd;
	}

	// ---- milk ----

	/** Lactation length, 284 days (MSU P4120). */
	public static final int LACTATION_DAYS = 284;
	/** Wood's shape for dairy goats, t in days (Siqueira et al., LRRD 29(7)): peak at b / c = 40 days. */
	public static final double WOOD_B = 0.20;
	public static final double WOOD_C = 0.005;
	/** First lactation x0.85 *(proposed: the cattle parity factor)*. */
	public static final double FIRST_LACTATION = 0.85;
	private static final double CURVE_SUM;

	static {
		double sum = 0;
		for (int t = 1; t <= LACTATION_DAYS; t++) {
			sum += Math.pow(t, WOOD_B) * Math.exp(-WOOD_C * t);
		}
		CURVE_SUM = sum;
	}

	/** A pure doe of this breed on a day in milk (1-based), kg; the curve's a is set so the lactation sums to the breed's total. */
	public static double milkKg(GoatBreed b, int daysInMilk) {
		if (daysInMilk < 1 || daysInMilk > LACTATION_DAYS || b.lactationKg <= 0) {
			return 0;
		}
		return b.lactationKg / CURVE_SUM * Math.pow(daysInMilk, WOOD_B) * Math.exp(-WOOD_C * daysInMilk);
	}

	/** A doe's milk on a day in milk: each dairy breed's curve by its share, her milk_yield factor, and the first-lactation factor. */
	public static double milkKg(Map<String, Integer> c, int daysInMilk, int milkYieldScore, boolean firstLactation) {
		double kg = 0;
		for (GoatBreed b : GoatBreed.values()) {
			kg += milkKg(b, daysInMilk) * c.getOrDefault(b.code, 0) / 10_000.0;
		}
		return kg * Flock.factor(milkYieldScore) * (firstLactation ? FIRST_LACTATION : 1);
	}

	/** Fat % of a doe's milk: the dairy breeds' figures weighted by their shares. */
	public static double fatPct(Map<String, Integer> c) {
		return dairyWeighted(c, b -> b.fatPct);
	}

	public static double proteinPct(Map<String, Integer> c) {
		return dairyWeighted(c, b -> b.proteinPct);
	}

	private static double dairyWeighted(Map<String, Integer> c, ToDoubleFunction<GoatBreed> f) {
		double sum = 0;
		double w = 0;
		for (GoatBreed b : GoatBreed.values()) {
			int bp = c.getOrDefault(b.code, 0);
			if (b.dairy() && bp > 0) {
				sum += f.applyAsDouble(b) * bp;
				w += bp;
			}
		}
		return w == 0 ? GoatBreed.SAANEN.fatPct : sum / w;
	}

	// ---- feed, water, housing ----

	/** Dry matter share of body weight: 5% lactating (Shur-Gain, a feed company), 2.75% otherwise (Penn State, 2.5-3.0% for market goats). */
	public static double intakeKgDm(double weightKg, boolean lactating) {
		return weightKg * (lactating ? 0.05 : 0.0275);
	}

	/** Water 2-3 times dry matter, 4-5 times when lactating (Merck): the middles. */
	public static double waterLPerKgDm(boolean lactating) {
		return lactating ? 4.5 : 2.5;
	}

	/** Pen floor blocks per adult goat: Purdue's 1.9-2.3 m², 2 blocks. */
	public static final double PEN_M2_PER_GOAT = 2.0;
	/** Up to half the day's dry matter browsed from leaves beside the pasture spot *(proposed; Peischel: 40-60% browse)*. */
	public static final double BROWSE_SHARE = 0.5;
	/** Manure dry matter, 1.3% of live weight a day (NRCS via Univ. of Kentucky: 13 lb per 1,000 lb). */
	public static final double MANURE_DM_SHARE = 0.013;
	/** Fresh goat manure is 64% moisture (Utah State Extension, Table 1). */
	public static final double MANURE_MOISTURE = 0.64;

	/** Fresh manure a day, kg: 1.3% of live weight as dry matter, at 64% moisture. 60 kg gives 2.2 kg *(derived)*. */
	public static double manureKgPerDay(double weightKg) {
		return weightKg * MANURE_DM_SHARE / (1 - MANURE_MOISTURE);
	}

	// ---- meat ----

	/** Kid meat from goats under 9 months ("kid under 9 months", Texas A&M), chevon after. */
	public static final double KID_MEAT_DAYS = 270;

	/** Meat items: live weight x the breed dressing (kid or mature) x 70% retail cuts / 2.5 kg an item (as sheep). */
	public static int meatItems(Map<String, Integer> c, double liveWeightKg, double ageDays) {
		double dressing = weighted(c, b -> ageDays < KID_MEAT_DAYS ? b.dressingKid : b.dressingMature);
		return (int) Math.floor(liveWeightKg * dressing * Flock.RETAIL_SHARE / Flock.KG_PER_ITEM);
	}
}
