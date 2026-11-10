package dev.farmings.husbandry;

import java.util.Map;

/**
 * Sheep rules in real days (docs/animals/sheep.md). Nothing here knows about Minecraft; the sheep entity asks these functions and
 * {@link TimeScale} converts to ticks. Crosses use the breed-weighted average of the breed figures (composition in basis points).
 */
public final class Flock {
	/** Oestrous cycle, about 17 days (WikiVet; UMD sheep reproduction guide). */
	public static final double CYCLE_DAYS = 17;
	/** Gestation, 147 days (142-152), every breed (same sources). */
	public static final double GESTATION_DAYS = 147;
	/** A ewe lamb is first bred at 7 months at the earliest (SDSU: 7-10 months). */
	public static final double FIRST_BREEDING_AGE_DAYS = 7 * 365.0 / 12.0;
	/** Heat lasts about a day *(proposed; the cattle heat is 0.75 days)*. */
	public static final double HEAT_DAYS = 1.0;
	/** A ewe can be bred again this long after lambing *(proposed)*. */
	public static final double WAIT_AFTER_LAMBING_DAYS = 45;
	/** Birth weight as a share of mature weight *(proposed)*. */
	public static final double BIRTH_FRACTION = 0.08;
	/** Mature weight reached at 24 months *(proposed; a straight line from the first-breeding point, as cattle growth)*. */
	public static final double MATURITY_DAYS = 730;
	/** A fleece counts as ready at 300 days of growth *(proposed)*. */
	public static final double FLEECE_READY_DAYS = 300;
	/** Fleece stops growing at two years without shearing *(proposed)*. */
	public static final double FLEECE_MAX_DAYS = 730;
	/** A trait at 0 or 1000 moves fleece weight and milk by this share *(proposed)*. */
	public static final double TRAIT_SWING = 0.20;
	/** fleece_quality at 0 or 1000 moves the micron by this much *(proposed)*. */
	public static final double MICRON_SWING = 2.0;
	/** fertility at 0 or 1000 moves the litter mean by this much *(proposed)*. */
	public static final double LITTER_SWING = 0.2;

	private Flock() {
	}

	// ---- breed-weighted figures ----

	/** A figure averaged over the breed composition. When {@code woolOnly}, hair breeds are left out (micron, yield, staple). */
	static double weighted(Map<String, Integer> compositionBp, java.util.function.ToDoubleFunction<SheepBreed> f, boolean woolOnly) {
		double sum = 0;
		double weight = 0;
		for (Map.Entry<String, Integer> e : compositionBp.entrySet()) {
			if (!SheepBreed.isSheep(e.getKey())) {
				continue;
			}
			SheepBreed b = SheepBreed.byCode(e.getKey());
			if (woolOnly && b.hair()) {
				continue;
			}
			sum += f.applyAsDouble(b) * e.getValue();
			weight += e.getValue();
		}
		return weight == 0 ? 0 : sum / weight;
	}

	public static double matureWeightKg(Map<String, Integer> c) {
		return weighted(c, b -> b.matureWeightKg, false);
	}

	/** The share of mature weight a ewe lamb needs: 70% if mostly Merino, else 65% (the breed-weighted figure). */
	public static double firstBreedingFraction(Map<String, Integer> c) {
		return weighted(c, b -> b.firstBreedingFraction, false);
	}

	/** Share of the genome that is East Friesian, 0-1: only these ewes give parlor milk, in proportion. */
	public static double dairyShare(Map<String, Integer> c) {
		return c.getOrDefault(SheepBreed.EAST_FRIESIAN.code, 0) / 10_000.0;
	}

	// ---- growth ----

	/** Target weight at an age: birth, then a straight line to the first-breeding weight at 7 months, then to mature at 24 months. */
	public static double targetKg(Map<String, Integer> c, double ageDays) {
		double mature = matureWeightKg(c);
		double birth = mature * BIRTH_FRACTION;
		double at7 = mature * firstBreedingFraction(c);
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

	public static double gainKg(Map<String, Integer> c, double ageDays, double days, double fed) {
		if (days <= 0) {
			return 0;
		}
		return Math.max(0, targetKg(c, ageDays + days) - targetKg(c, ageDays)) * Math.max(0, Math.min(1, fed));
	}

	public static boolean canFirstBreed(Map<String, Integer> c, double ageDays, double weightKg) {
		return ageDays >= FIRST_BREEDING_AGE_DAYS && weightKg >= matureWeightKg(c) * firstBreedingFraction(c);
	}

	// ---- lambing ----

	/** Odds of a single, twins or triplets. */
	public record Litter(double single, double twins, double triplets) {
		public double mean() {
			return single + 2 * twins + 3 * triplets;
		}

		/** 1, 2 or 3 lambs for a roll in [0, 1). */
		public int roll(double r) {
			return r < single ? 1 : r < single + twins ? 2 : 3;
		}
	}

	/** The sheep.md table, by mean: the four breeds plus the ends (1.0 all singles; 2.5 proposed). */
	private static final double[][] LITTER_TABLE = {
			{1.0, 1.00, 0.00, 0.00},
			{1.25, 0.75, 0.25, 0.00},
			{1.5, 0.50, 0.50, 0.00},
			{1.8, 0.30, 0.60, 0.10},
			{2.2, 0.15, 0.50, 0.35},
			{2.5, 0.05, 0.40, 0.55},
	};

	/** Litter odds for a mean, read off the table by straight lines between its rows (clamped to 1.0-2.5). */
	public static Litter litterFor(double mean) {
		double m = Math.max(1.0, Math.min(2.5, mean));
		for (int i = 1; i < LITTER_TABLE.length; i++) {
			double[] lo = LITTER_TABLE[i - 1];
			double[] hi = LITTER_TABLE[i];
			if (m <= hi[0]) {
				double t = (m - lo[0]) / (hi[0] - lo[0]);
				return new Litter(lo[1] + t * (hi[1] - lo[1]), lo[2] + t * (hi[2] - lo[2]), lo[3] + t * (hi[3] - lo[3]));
			}
		}
		double[] last = LITTER_TABLE[LITTER_TABLE.length - 1];
		return new Litter(last[1], last[2], last[3]);
	}

	/** A ewe's litter odds: the breed-weighted mean, moved up to ±0.2 by her fertility score (0-1000). */
	public static Litter litter(Map<String, Integer> c, int fertilityScore) {
		double mean = weighted(c, b -> b.litterMean, false) + (fertilityScore - 500) / 500.0 * LITTER_SWING;
		return litterFor(mean);
	}

	// ---- wool ----

	public enum Grade {
		FINE("fine"),
		HALF_BLOOD("half blood"),
		THREE_EIGHTHS("three-eighths blood"),
		QUARTER("quarter blood"),
		COARSE("low quarter and coarser"),
		HAIR("hair");

		public final String label;

		Grade(String label) {
			this.label = label;
		}

		/** One grade lower (skirtings); hair stays hair, coarse stays coarse. */
		public Grade lower() {
			return this == HAIR || this == COARSE ? this : values()[ordinal() + 1];
		}
	}

	/** US blood grades by micron (sheep.md, TO VERIFY the boundaries against the ASI table). 0 microns means hair. */
	public static Grade grade(double micron) {
		if (micron <= 0) {
			return Grade.HAIR;
		}
		if (micron < 22.0) {
			return Grade.FINE;
		}
		if (micron < 25.0) {
			return Grade.HALF_BLOOD;
		}
		if (micron < 28.0) {
			return Grade.THREE_EIGHTHS;
		}
		if (micron < 31.0) {
			return Grade.QUARTER;
		}
		return Grade.COARSE;
	}

	/** Mean micron of a wool sheep: the wool breeds' weighted micron, moved by fleece_quality (higher quality, finer). 0 if all hair. */
	public static double micron(Map<String, Integer> c, int fleeceQualityScore) {
		double base = weighted(c, b -> b.micron, true);
		if (base <= 0) {
			return 0;
		}
		return Math.max(12, base - (fleeceQualityScore - 500) / 500.0 * MICRON_SWING);
	}

	public static double cleanYield(Map<String, Integer> c) {
		return weighted(c, b -> b.cleanYield, true);
	}

	/**
	 * Greasy fleece weight after {@code growthDays} of growth: the breed-weighted annual fleece times the share of a year (capped at two
	 * years), times the wool_yield factor (±20%), times the share of the coat that is wool (1, 0.5 or 0, from the coat gene).
	 */
	public static double fleeceKg(Map<String, Integer> c, double growthDays, int woolYieldScore, double fleeceShare) {
		double days = Math.max(0, Math.min(FLEECE_MAX_DAYS, growthDays));
		double annual = weighted(c, b -> b.fleeceKg, false);
		return annual * days / 365.0 * factor(woolYieldScore) * fleeceShare;
	}

	/** Staple length in cm after {@code growthDays}: the wool breeds' weighted yearly growth, capped at two years. */
	public static double stapleCm(Map<String, Integer> c, double growthDays) {
		return weighted(c, b -> b.stapleCmPerYear, true) * Math.max(0, Math.min(FLEECE_MAX_DAYS, growthDays)) / 365.0;
	}

	/** A 0-1000 score as a multiplier: 500 is 1.0, 0 is 0.8, 1000 is 1.2. */
	public static double factor(int score) {
		return 1.0 + (score - 500) / 500.0 * TRAIT_SWING;
	}

	// ---- milk (East Friesian) ----

	/** Lactation length, 221 days, and total, 365 kg (Croatian East Friesian study). */
	public static final int LACTATION_DAYS = 221;
	public static final double LACTATION_KG = 365;
	/** Fat, protein and lactose of East Friesian milk, % (same study). */
	public static final double FAT_PCT = 5.68;
	public static final double PROTEIN_PCT = 4.92;
	public static final double LACTOSE_PCT = 4.59;
	/** Wood's shape borrowed from the Jersey curve *(proposed)*. */
	private static final double WOOD_B = 0.18;
	private static final double WOOD_C = 0.0030;
	private static final double WOOD_SCALE;

	static {
		double sum = 0;
		for (int t = 1; t <= LACTATION_DAYS; t++) {
			sum += Math.pow(t, WOOD_B) * Math.exp(-WOOD_C * t);
		}
		WOOD_SCALE = LACTATION_KG / sum;
	}

	/** A pure East Friesian's milk on a day in milk (1-based), kg; 0 outside the lactation. */
	public static double milkKg(int daysInMilk) {
		if (daysInMilk < 1 || daysInMilk > LACTATION_DAYS) {
			return 0;
		}
		return WOOD_SCALE * Math.pow(daysInMilk, WOOD_B) * Math.exp(-WOOD_C * daysInMilk);
	}

	/** A ewe's milk on a day in milk: the pure curve times her East Friesian share and her milk_yield factor. */
	public static double milkKg(Map<String, Integer> c, int daysInMilk, int milkYieldScore) {
		return milkKg(daysInMilk) * dairyShare(c) * factor(milkYieldScore);
	}

	// ---- feed and water ----

	/** Dry matter intake share of body weight: 2.5% maintenance, 3.5% nursing one lamb, 4.5% nursing more (Oregon State ANS 312). */
	public static double intakeKgDm(double weightKg, int lambsNursing) {
		double share = lambsNursing <= 0 ? 0.025 : lambsNursing == 1 ? 0.035 : 0.045;
		return weightKg * share;
	}

	/** 1.0-1.5 gal per 4 lb DM (SDSU), 2.1-3.1 L per kg: the middle, 2.6 L per kg DM. */
	public static final double WATER_L_PER_KG_DM = 2.6;
	/** A grass block grazed bare gives 1.5 kg of dry matter *(proposed, scaled; see sheep.md)*. */
	public static final double GRASS_BLOCK_KG_DM = 1.5;

	// ---- housing ----

	/** Pen floor blocks per adult sheep: 1.5 m² for a pregnant or lactating ewe (OMAFRA 1.4-1.5), so 3 blocks hold 2 sheep. */
	public static final double PEN_M2_PER_SHEEP = 1.5;
	/** Sheep served per hay-rack block: 50 cm of feeder each (Penn State, 9-20 in), rounded to a block *(proposed)*. */
	public static final int SHEEP_PER_RACK = 2;

	public static int penCapacity(int penBlocks) {
		return (int) Math.floor(penBlocks / PEN_M2_PER_SHEEP);
	}

	// ---- meat ----

	/** Dressing about 50% of live weight (Montana State); bone-in retail cuts 65-75% of the carcass, 70% used (UW-Madison). */
	public static final double DRESSING = 0.50;
	public static final double RETAIL_SHARE = 0.70;
	/** One meat item per 2.5 kg of retail meat *(proposed: beef uses 10 kg; a smaller animal gets smaller items so a lamb drops a few)*. */
	public static final double KG_PER_ITEM = 2.5;

	public static int meatItems(double liveWeightKg) {
		return (int) Math.floor(liveWeightKg * DRESSING * RETAIL_SHARE / KG_PER_ITEM);
	}
}
