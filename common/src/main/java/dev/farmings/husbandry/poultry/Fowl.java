package dev.farmings.husbandry.poultry;

import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import java.util.Map;

/**
 * Poultry rules shared by every bird (docs/animals/poultry.md). Crosses take the breed-weighted mean of the {@link FowlBreed} figures,
 * by breed composition in basis points.
 */
public final class Fowl {
	private Fowl() {
	}

	/** A 0-1000 trait score as a multiplier with the given swing: 500 is 1.0 (the sheep rule). */
	public static double factor(int score, double swing) {
		return 1.0 + (score - 500) / 500.0 * swing;
	}

	/** egg_production moves the laying rate by up to ±20% (proposed, as wool_yield). */
	public static final double EGG_RATE_SWING = 0.20;
	/** egg_weight moves egg weight by up to ±10% (proposed). */
	public static final double EGG_WEIGHT_SWING = 0.10;
	/** A hatchling weighs two thirds of its egg (proposed). */
	public static final double HATCH_FRACTION_OF_EGG = 2.0 / 3.0;
	/** A hen lays for 3 years, then retires (proposed). */
	public static final double LAYING_YEARS = 3;
	/** A cold day costs a young bird this much health on the cattle scale (proposed). */
	public static final double COLD_HEALTH_LOSS = 10;
	/** Litter, kg per kg of bird a day: the MWPS-18 layer, 0.068 kg / 1.36 kg. */
	public static final double LITTER_KG_PER_KG_DAY = 0.068 / 1.36;
	/** Ducks: 0.44 lb a day for a 4 lb duck (MWPS-18, Table 6, Duck row; sourcing pass 2026-11-12). */
	public static final double DUCK_LITTER_KG_PER_KG_DAY = 0.44 / 4.0;

	/** Litter rate; geese take the duck's (waterfowl, proposed: no goose row in MWPS-18). */
	public static double litterKgPerKgDay(Species s) {
		return s == Species.DUCK || s == Species.GOOSE ? DUCK_LITTER_KG_PER_KG_DAY : LITTER_KG_PER_KG_DAY;
	}
	/** A fertile egg not started within this many real days no longer hatches (proposed). */
	public static final double HATCHABLE_DAYS = 7;
	/** One grain item is 100 g of feed (proposed). */
	public static final double GRAIN_ITEM_KG = 0.1;
	/** Feed scales with body weight to this power (proposed: metabolic scaling). */
	public static final double FEED_EXPONENT = 0.75;

	public static double mean(Map<String, Integer> c, java.util.function.ToDoubleFunction<FowlBreed> f) {
		double sum = 0;
		int total = 0;
		for (Map.Entry<String, Integer> e : c.entrySet()) {
			FowlBreed b = FowlBreed.byCode(e.getKey());
			if (b != null) {
				sum += f.applyAsDouble(b) * e.getValue();
				total += e.getValue();
			}
		}
		return total == 0 ? 0 : sum / total;
	}

	public static double matureKg(Map<String, Integer> c, Sex sex) {
		return mean(c, b -> sex == Sex.MALE ? b.maleKg : b.femaleKg);
	}

	/**
	 * Body weight by age: from the hatchling (two thirds of the egg) straight up to the mature weight at the first-egg age
	 * (proposed linear growth; no growth curve was looked up).
	 */
	public static double weightKg(Map<String, Integer> c, Sex sex, double ageDays) {
		double hatch = mean(c, b -> b.eggGrams) / 1000.0 * HATCH_FRACTION_OF_EGG;
		double mature = matureKg(c, sex);
		double t = Math.min(1, Math.max(0, ageDays / mean(c, b -> b.firstEggDays)));
		return hatch + (mature - hatch) * t;
	}

	/** Still needs heat (a brooder or a mother) at this age. */
	public static boolean needsHeat(Map<String, Integer> c, double ageDays) {
		return ageDays < mean(c, b -> b.heatUntilDays);
	}

	public static boolean adult(Map<String, Integer> c, double ageDays) {
		return ageDays >= mean(c, b -> b.firstEggDays);
	}

	/** Species anchors for daily feed: grams a day at a body weight (docs/animals/poultry.md, "Feed and water"). */
	static double[] feedAnchor(Species s) {
		return switch (s) {
			case QUAIL -> new double[] {16, 0.14};      // NSW DPI, 14-18 g
			case DUCK -> new double[] {218, 3.9};       // breeder ducks, 48 lb per 100 a day
			case GOOSE -> new double[] {320, 6.75};     // FAO: 320 g a day for breeder geese of 6.5-7.0 kg
			case GUINEA -> new double[] {85, 1.5};      // "(84.5g/fowl/day)" (Konlan et al. 2011, J. World's Poult. Res. 1(1):1-3, Ghana)
			default -> new double[] {100, 2.0};          // laying hen (Hy-Line 99.6 g); turkeys use it too (proposed)
		};
	}

	/** One real day's feed in kg, as fed. */
	public static double feedKgPerDay(Species s, double weightKg) {
		double[] a = feedAnchor(s);
		return a[0] / 1000.0 * Math.pow(weightKg / a[1], FEED_EXPONENT);
	}

	/**
	 * One real day's water in litres: 2× the feed, 2.3× for quail (water "59.67 ± 7.17 ml/bird/day" against feed "26.32 ± 0.05 g/bird/day",
	 * Arch Razi Inst 2024), and 3× for ducks and geese (proposed for geese: adult goose water NOT FOUND).
	 */
	public static double waterLPerDay(Species s, double weightKg) {
		double ratio = switch (s) {
			case DUCK, GOOSE -> 3.0;
			case QUAIL -> QUAIL_WATER_RATIO;
			default -> 2.0;
		};
		return feedKgPerDay(s, weightKg) * ratio;
	}

	/** Quail drink about 2.3× their feed: 59.67 / 26.32 = 2.27, rounded (Arch Razi Inst 2024). */
	public static final double QUAIL_WATER_RATIO = 2.3;

	/** Females one adult male serves in a coop. */
	/**
	 * Share of the day's feed a bird on a grass run takes as forage and insects (poultry.md, "Foraging on the run"): "pastured poultry
	 * eat 5-20% (of their diet) from pasture" (Mattocks 2002, NCAT/ATTRA IP453); layers reach 20% (Agrarforschung Schweiz 2024). The upper
	 * end, for every species *(proposed)*.
	 */
	public static final double FORAGE_SHARE = 0.20;
	/**
	 * Geese take two-thirds of their feed off a grassy run (poultry.md, "Grazing (geese)"): FAO holds pasture geese to 0.5-1.0 kg of grain
	 * a week, about 107 g a day against a full 320 g *(derived)*.
	 */
	public static final double GOOSE_FORAGE_SHARE = 2.0 / 3.0;

	/** The share of a bird's day it takes as forage on a grassy run. */
	public static double forageShare(Species s) {
		return s == Species.GOOSE ? GOOSE_FORAGE_SHARE : FORAGE_SHARE;
	}
	/**
	 * Forage a run's grass block regrows a day, kg DM: 50 hens an acre for good x 25 g / 4,047 m². The 50 is *(derived: half Dryden's 100,
	 * for a run grazed all year without rest)*: Dryden 1916 gives "Maximum number of fowls per acre : 100 laying hens" only with night
	 * droppings put elsewhere, two yards cropped in turn and at least 6 months' rest a year.
	 */
	public static final double RUN_REGROWTH_KG_PER_BLOCK_DAY = 50 * 0.025 / 4047.0;

	/**
	 * How much forage is drawn down after one step: {@code drawn} kg DM before, plus what the birds ate, less what {@code grassBlocks}
	 * regrew in {@code dayShare} of a day. Never below 0.
	 */
	public static double runDrawn(double drawn, double eatenKg, int grassBlocks, double dayShare) {
		return Math.max(0, drawn + eatenKg - grassBlocks * RUN_REGROWTH_KG_PER_BLOCK_DAY * dayShare);
	}

	/** Males serve at most this many hens at once (poultry.md, "Hens per male"): chicken 10 (UC ANR), quail 3 (NSW DPI), duck 5 (1 to 5-8 for meat ducks, Narushin 2026 quoting "Poultry 2024"), turkey 10 (proposed). */
	public static int hensPerMale(Species s) {
		return switch (s) {
			case QUAIL -> 3;
			case DUCK -> 5;
			case GOOSE -> 4;      // FAO: "one male for every four females"
			case GUINEA -> 5;     // UKY: "one male for each four to five females"
			default -> 10;
		};
	}

	/** Real days a mated hen keeps laying fertile eggs (MSU: up to 4 weeks for chickens; 3 for turkeys; others proposed). */
	public static double fertileDays(Species s) {
		// quail "can store sperm for up to 11 days" (PMC4519265; sourcing pass 2026-11-12)
		return switch (s) {
			case CHICKEN -> 28;
			case QUAIL -> 11;
			case GOOSE -> 14;     // NOT FOUND for geese; proposed (forum figures 10-14)
			default -> 21;
		};
	}

	/** Birds one coop floor block holds (docs/animals/poultry.md, "Housing"). */
	public static double birdsPerFloorBlock(Species s) {
		return switch (s) {
			case CHICKEN -> 3;
			case TURKEY -> 1;
			case QUAIL -> 16;
			case GOOSE -> 1;      // FAO: 1.0 m² a goose on deep litter
			case GUINEA -> 4;     // UKY: 2-3 sq ft a guinea
			default -> 2;
		};
	}

	/** Run cells one bird needs. */
	public static double runCellsPerBird(Species s) {
		return switch (s) {
			case CHICKEN -> 1;
			case QUAIL -> 1.0 / 8;
			case GOOSE, GUINEA -> 1;     // FAO: at least 1.0 m² a goose in a run; guinea fowl as hens (proposed)
			default -> 2;
		};
	}

	public static int hensPerNest(Species s) {
		return s == Species.QUAIL ? 8 : 4;
	}

	/** Birds one perch block holds; 0 for birds that don't perch. */
	public static int birdsPerPerch(Species s) {
		return switch (s) {
			case CHICKEN, GUINEA -> 3;     // guinea fowl roost high (UKY); as hens (proposed)
			case TURKEY -> 1;
			default -> 0;
		};
	}

	/** Meat items a bird gives: live weight × dressing, one item per 0.5 kg, at least one (proposed). */
	public static int meatItems(Map<String, Integer> c, double liveKg) {
		return Math.max(1, (int) Math.floor(liveKg * mean(c, b -> b.dressing) / 0.5));
	}
}
