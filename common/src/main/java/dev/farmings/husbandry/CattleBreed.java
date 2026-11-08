package dev.farmings.husbandry;

/**
 * The six starting cattle breeds (docs/animals/cattle.md). Mature cow weight and gestation are real figures with sources there.
 * Entries marked stand-in have no good source yet and are replaced when one is found.
 */
public enum CattleBreed {
	// Duplessis et al. 2014 (Better Farming): about 710.
	HOLSTEIN("HOL", Use.DAIRY, 710, 283),
	// Same source: about 470.
	JERSEY("JER", Use.DAIRY, 470, 283),
	// Stand-in: breed lists say about 600-612 kg (secondary, TO VERIFY); the midpoint is used.
	BROWN_SWISS("BSW", Use.DAIRY, 606, 283),
	// Stand-in: 385 kg from the Oklahoma State breed profile (Indian figures; Brazilian dairy strain TO VERIFY). Gyr gestation
	// was not found, so 283 is used as a stand-in.
	GYR("GYR", Use.DAIRY, 385, 283),
	// Stand-in: about 550 kg from breed lists (TO VERIFY). Gestation about 280 days (USDA ARS Lincoln, 10,000+ records).
	ANGUS("ANG", Use.BEEF, 550, 280),
	// Motoyama et al. 2014: about 487 kg. Gestation 283.8 days in a Brazilian herd (286 for Wagyu-sired calves); 284 rounded.
	WAGYU("WAG", Use.BEEF, 487, 284);

	public enum Use {
		DAIRY,
		BEEF
	}

	public final String code;
	public final Use use;
	public final double matureWeightKg;
	public final int gestationDays;

	CattleBreed(String code, Use use, double matureWeightKg, int gestationDays) {
		this.code = code;
		this.use = use;
		this.matureWeightKg = matureWeightKg;
		this.gestationDays = gestationDays;
	}

	/** First breeding weight: 55% of mature weight (Wynnstay, Engormix). Holstein comes to about 390 kg, Jersey about 260 kg. */
	public double breedingWeightKg() {
		return matureWeightKg * Lifecycle.BREEDING_WEIGHT_FRACTION;
	}

	/** Puberty at about 40% of mature weight (KALRO): weight, not age. */
	public double pubertyWeightKg() {
		return matureWeightKg * Lifecycle.PUBERTY_WEIGHT_FRACTION;
	}

	public static CattleBreed byCode(String code) {
		for (CattleBreed b : values()) {
			if (b.code.equals(code)) {
				return b;
			}
		}
		throw new IllegalArgumentException("unknown cattle breed " + code);
	}
}
