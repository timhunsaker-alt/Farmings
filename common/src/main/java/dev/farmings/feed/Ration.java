package dev.farmings.feed;

/**
 * Rations and what they do (docs/multiblocks/feed-mill.md, "What a ration does"). A ration's analysis is its dry-matter-weighted crude
 * protein, NDF, TDN and calcium (% of DM), its forage share and its premix share. Each animal's score (0-1) is the mean of its target
 * scores; the score's shape is proposed, the targets and end points are sourced in the spec. Plain Java.
 */
public final class Ration {
	private Ration() {
	}

	/** Percentages of dry matter; forage and premix as shares (0-1). */
	public record Analysis(double cp, double ndf, double tdn, double forage, double ca, double premix) {
		/** A ration that meets every target: what an animal is assumed to eat before any feeder has fed it (proposed). */
		public static final Analysis BALANCED = new Analysis(16.5, 28, 72, 0.5, 3.5, 0.014);

		/** This and another, mixed by dry matter. */
		public Analysis mix(double kgThis, Analysis other, double kgOther) {
			double t = kgThis + kgOther;
			if (t <= 0) {
				return other;
			}
			double a = kgThis / t;
			double b = kgOther / t;
			return new Analysis(cp * a + other.cp * b, ndf * a + other.ndf * b, tdn * a + other.tdn * b, forage * a + other.forage * b,
					ca * a + other.ca * b, premix * a + other.premix * b);
		}

		/** Moves this a share of the way toward another (the animal's running average of what it eats). */
		public Analysis toward(Analysis other, double share) {
			return mix(1 - share, other, share);
		}
	}

	/** 1 inside [lo, hi], falling straight to 0 at {@code zeroLo} below and {@code zeroHi} above (proposed shape). */
	static double band(double v, double zeroLo, double lo, double hi, double zeroHi) {
		if (v >= lo && v <= hi) {
			return 1;
		}
		if (v < lo) {
			return clamp((v - zeroLo) / (lo - zeroLo));
		}
		return clamp((zeroHi - v) / (zeroHi - hi));
	}

	static double clamp(double v) {
		return Math.max(0, Math.min(1, v));
	}

	/** Premix counts as fed at 1% of DM or more: 0.265 kg in a cow's 22 kg of DM is 1.2% (derived). */
	public static final double PREMIX_SHARE = 0.01;

	/** Cow in milk: CP 15-18%, NDF 25-30%, forage 40% or more, premix fed. */
	public static double dairyScore(Analysis a) {
		double s = (band(a.cp, 8, 15, 18, 25) + band(a.ndf, 15, 25, 30, 60) + clamp(a.forage / 0.4)) / 3;
		return a.premix >= PREMIX_SHARE ? s : s * 0.9;
	}

	/** Growing cattle: CP 12% or more, TDN 65% or more (proposed targets). */
	public static double growingScore(Analysis a) {
		return (clamp((a.cp - 6) / (12 - 6)) + clamp((a.tdn - 50) / (65 - 50))) / 2;
	}

	/** Lambs: TDN 66% or more, CP 15% or more (NRC 2007 early lactation twins, as the lamb's dam's milk-and-creep diet). */
	public static double lambScore(Analysis a) {
		return (clamp((a.cp - 8) / (15 - 8)) + clamp((a.tdn - 50) / (66 - 50))) / 2;
	}

	/**
	 * Ewes and does in milk: CP 11.6% or more, the low end of NRC 2007's 11.6-15.5% for early lactation (Penn State). The score falls
	 * straight to 0 at CP 8% *(proposed shape, as the lamb's)*.
	 */
	public static double eweScore(Analysis a) {
		return clamp((a.cp - 8) / (11.6 - 8));
	}

	/** Laying hens: CP 16% or more, Ca 3-4% (Alabama ANR-2913). */
	public static double layerScore(Analysis a) {
		return (clamp((a.cp - 9) / (16 - 9)) + clamp(a.ca / 3.0)) / 2;
	}

	/** Milk x (0.67 + 0.33 x score): "milk production (29.6 vs. 44.1 kg/d)" on pasture and TMR, 29.6 / 44.1 = 0.67 (Kolver & Muller 1998). */
	public static double milkFactor(Analysis a) {
		return 0.67 + 0.33 * dairyScore(a);
	}

	/**
	 * Growth x (0.29 + 0.71 x score): Penn State backgrounding, "Expected daily gain (pounds) 0.5 0.5 0.75 1.0 1.0 1.0 1.25 1.75", so the
	 * worst ration gains 0.5 / 1.75 = 0.29 of the best *(derived)*.
	 */
	public static double growthFactor(Analysis a) {
		return GROWTH_FLOOR + (1 - GROWTH_FLOOR) * growingScore(a);
	}

	/** The growth factor at a score of 0: 0.5 / 1.75 lb a day (Penn State backgrounding) *(derived)*. */
	public static final double GROWTH_FLOOR = 0.5 / 1.75;

	/**
	 * Ewe and doe milk x (0.83 + 0.17 x score): a low-protein diet gave "a decrease (P< 0·01) in daily milk yield of 0·53 kg (17%)"
	 * (Robinson et al. 1979, Anim. Prod.), so 0.83 at worst *(derived)*.
	 */
	public static double eweMilkFactor(Analysis a) {
		return EWE_MILK_FLOOR + (1 - EWE_MILK_FLOOR) * eweScore(a);
	}

	/** The ewe and doe milk factor at a score of 0: 17% less milk (Robinson et al. 1979) *(derived)*. */
	public static final double EWE_MILK_FLOOR = 0.83;

	/** Lamb growth x (0.8 + 0.2 x score): grain- and creep-fed lambs gain 20-30% more (UMN). */
	public static double lambFactor(Analysis a) {
		return 0.8 + 0.2 * lambScore(a);
	}

	/** Eggs x (0.75 + 0.25 x score) (proposed floor; Alabama ANR-2913: on grain alone production suffers). */
	public static double eggFactor(Analysis a) {
		return 0.75 + 0.25 * layerScore(a);
	}

	/** Each feeding step moves an animal's running analysis this far toward what it ate (as the fed share, proposed). */
	public static final double SMOOTHING = 0.05;
}
