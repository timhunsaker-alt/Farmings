package dev.farmings.husbandry;

import java.util.Map;
import java.util.function.ToDoubleFunction;

/** Horse rules in real days (docs/animals/horses.md), beside {@link Swine} and {@link Herd}. Plain Java. Speed is in {@link Gallop}. */
public final class Equine {
	/** "a mare will have a 21-day estrous cycle" (extension.org). */
	public static final double CYCLE_DAYS = 21;
	/** "usually 4 to 7 days" (extension.org): 5.5. */
	public static final double HEAT_DAYS = 5.5;
	/** "338 to 343 days" (AAEP, TO VERIFY): 340. */
	public static final double GESTATION_DAYS = 340;
	/** "An overall mean per cycle pregnancy rate of 59.9%" (Morris and Allen 2002, Equine Vet. J. 34:51). */
	public static final double CONCEPTION = 0.60;
	/** The foal heat, "six to nine days after foaling" (eXtension, horses): 8. */
	public static final double FOAL_HEAT_DAYS = 8;
	/** First breeding at 3 years *(proposed: NOT FOUND as a sourced age)*; a colt is a stallion from then. */
	public static final double FIRST_BREEDING_DAYS = 3 * 365;
	/** "the usual age for weaning is between 4 and 6 months" (eXtension, weaning): 150 days. */
	public static final double WEANING_DAYS = 150;
	/** A foal weighs "about 10% of the mare's weight" (Horse Canada). */
	public static final double BIRTH_SHARE = 0.10;
	/**
	 * The breeding season as days of the year (0 = 1 Jan): from 15 April, "The natural breeding season typically begins around mid-April"
	 * (eXtension, mare seasonality); to 31 October *(proposed: the end is NOT FOUND in a primary source)*.
	 */
	public static final int SEASON_FIRST = 105;
	public static final int SEASON_LAST = 303;
	/** A stallion serves a mare this far away: the same paddock or stable *(proposed, as sheep)*. */
	public static final double SERVICE_RANGE = 8.0;
	/** Foals and yearlings can't be ridden: under 2 years (horses.md, "Age"). */
	public static final double RIDING_AGE_DAYS = 2 * 365;
	/** "2–2.5%– body weight" of dry matter a day (MSD): 2.25%. */
	public static final double DM_SHARE = 0.0225;
	/** "21–29 L of water per day" for a 500 kg horse (MSD): 0.05 L a kg. */
	public static final double WATER_L_PER_KG = 0.05;
	/** "around 51 pounds of total raw waste per day" for a 1,000-pound horse (Penn State): 5.1% of body weight. */
	public static final double MANURE_SHARE = 0.051;

	/** Share of mature weight by age in days: MSD's growth table, 43% at 6 months ... 86% at 24; mature at 5 years *(proposed)*. */
	private static final double[][] GROWTH = {{0, BIRTH_SHARE}, {182.5, 0.43}, {365, 0.64}, {547.5, 0.77}, {730, 0.86}, {5 * 365, 1.0}};

	private Equine() {
	}

	static double weighted(Map<String, Integer> c, ToDoubleFunction<HorseBreed> f) {
		double sum = 0;
		double w = 0;
		for (Map.Entry<String, Integer> e : c.entrySet()) {
			if (HorseBreed.isHorse(e.getKey())) {
				sum += f.applyAsDouble(HorseBreed.byCode(e.getKey())) * e.getValue();
				w += e.getValue();
			}
		}
		return w == 0 ? f.applyAsDouble(HorseBreed.THOROUGHBRED) : sum / w;
	}

	public static HorseBreed topBreed(Map<String, Integer> c) {
		HorseBreed best = HorseBreed.THOROUGHBRED;
		int most = -1;
		for (HorseBreed b : HorseBreed.values()) {
			int bp = c.getOrDefault(b.code, 0);
			if (bp > most) {
				most = bp;
				best = b;
			}
		}
		return best;
	}

	public static double matureKg(Map<String, Integer> c) {
		return weighted(c, b -> b.matureKg);
	}

	/** Share of mature weight at an age, a straight line between the table's points. */
	public static double growthShare(double ageDays) {
		if (ageDays <= 0) {
			return GROWTH[0][1];
		}
		for (int i = 1; i < GROWTH.length; i++) {
			if (ageDays <= GROWTH[i][0]) {
				double f = (ageDays - GROWTH[i - 1][0]) / (GROWTH[i][0] - GROWTH[i - 1][0]);
				return GROWTH[i - 1][1] + f * (GROWTH[i][1] - GROWTH[i - 1][1]);
			}
		}
		return 1.0;
	}

	public static double targetKg(Map<String, Integer> c, double ageDays) {
		return matureKg(c) * growthShare(ageDays);
	}

	/** Weight gained over {@code span} days from {@code ageDays}, by the fed share. */
	public static double gainKg(Map<String, Integer> c, double ageDays, double span, double fed) {
		return Math.max(0, targetKg(c, ageDays + span) - targetKg(c, Math.max(0, ageDays))) * Math.max(0, Math.min(1, fed));
	}

	public static double dryMatterKg(double kg) {
		return DM_SHARE * kg;
	}

	/** Water a day; doubled on a day of work *(proposed)*. */
	public static double waterL(double kg, boolean worked) {
		return WATER_L_PER_KG * kg * (worked ? 2 : 1);
	}

	public static double manureKg(double kg) {
		return MANURE_SHARE * kg;
	}

	/** A mare cycles from 15 April to 31 October. */
	public static boolean inSeason(int dayOfYear) {
		return dayOfYear >= SEASON_FIRST && dayOfYear <= SEASON_LAST;
	}
}
