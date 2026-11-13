package dev.farmings.husbandry;

import java.util.Map;

/**
 * How fast a horse runs (docs/animals/horses.md, "Speed: genes, traits, age and fitness"). Plain Java; distances in metres (1 block = 1 m),
 * paces in metres a second, in real time (races are not on the 15:1 clock).
 *
 * <p>{@link #pace} is a horse's best <em>average</em> pace over a run of a given length, as a record is. A horse in the middle of a run
 * goes at {@link #speedAt}: the speed that makes its time over every distance come out at that distance over its pace, so a race of any
 * length takes {@link #time} however it is cut up.
 */
public final class Gallop {
	/** Below this the record curve is flat: 402 m, the Quarter Horse's quarter mile. */
	public static final double SHORTEST_M = 402;
	/** Thoroughbred record paces: 402 m at 19.66 m/s (Winning Brew, TO VERIFY); 2,012 m at 16.85 and 2,414 m at 16.76 (Secretariat). */
	private static final double[][] TB = {{402, 19.66}, {2012, 16.85}, {2414, 16.76}};
	/** Beyond 2,414 m the Thoroughbred curve falls 1% a doubling *(proposed)*. */
	private static final double FALL_PER_DOUBLING = 0.01;
	/** Quarter Horse: 402 m at 19.84 m/s (First Moonflash 2009, TO VERIFY); its curve falls 25% faster than the Thoroughbred's *(proposed)*. */
	private static final double QH_START = 19.84;
	private static final double QH_FALL = 1.25;
	/** Arabian: 92% of the Thoroughbred, its curve falling 40% slower *(proposed)*. */
	private static final double ARA_SHARE = 0.92;
	private static final double ARA_FALL = 0.6;
	/** MSTN best distances, C/C 6.2, C/T 9.1, T/T 10.5 furlongs (Hill et al. 2010): metres. */
	public static final double BEST_CC = 1250;
	public static final double BEST_CT = 1830;
	public static final double BEST_TT = 2110;
	/** Away from its best distance a horse runs 3% slower per natural-log unit of the distance ratio *(proposed)*. */
	public static final double MSTN_PER_LN = 0.03;
	/** SPEED at 0 or 1000 moves pace 3% either side of the middle *(proposed)*. */
	public static final double SPEED_SWING = 0.03;
	/** STAMINA at 0 or 1000 steepens or flattens the fall with distance by 20% *(proposed)*. */
	public static final double STAMINA_SWING = 0.20;
	/** "peak racing age is 4.45 years" (Gramm & Marksteiner 2010). */
	public static final double PEAK_YEARS = 4.45;
	/** 2.1% slower at 2 years and 0.25% slower a year after the peak (derived from Gramm & Marksteiner 2010, horses.md). */
	public static final double AT_TWO = 0.021;
	public static final double PER_YEAR_PAST_PEAK = 0.0025;
	/** An untrained horse runs 5% slower *(proposed: NOT FOUND as a percentage)*. */
	public static final double UNFIT = 0.05;

	private Gallop() {
	}

	private static double tb(double d) {
		if (d <= TB[0][0]) {
			return TB[0][1];
		}
		for (int i = 1; i < TB.length; i++) {
			if (d <= TB[i][0]) {
				double f = Math.log(d / TB[i - 1][0]) / Math.log(TB[i][0] / TB[i - 1][0]);
				return TB[i - 1][1] + f * (TB[i][1] - TB[i - 1][1]);
			}
		}
		double last = TB[TB.length - 1][1];
		return last * (1 - FALL_PER_DOUBLING * Math.log(d / TB[TB.length - 1][0]) / Math.log(2));
	}

	/** A breed's record pace over {@code d} metres. */
	public static double record(HorseBreed b, double d) {
		double fall = TB[0][1] - tb(d);
		return switch (b) {
			case THOROUGHBRED -> tb(d);
			case QUARTER_HORSE -> QH_START - QH_FALL * fall;
			case ARABIAN -> ARA_SHARE * (TB[0][1] - ARA_FALL * fall);
		};
	}

	/** The best distance of an MSTN genotype by its count of C alleles (2 C/C sprinter, 1 C/T, 0 T/T stayer). */
	public static double bestDistance(int cAlleles) {
		return cAlleles >= 2 ? BEST_CC : cAlleles == 1 ? BEST_CT : BEST_TT;
	}

	/** The MSTN factor over {@code d} metres. */
	public static double mstn(int cAlleles, double d) {
		return 1 - MSTN_PER_LN * Math.abs(Math.log(Math.max(SHORTEST_M, d) / bestDistance(cAlleles)));
	}

	/** SPEED score 0-1000 as a factor: 1.0 at 1000, 0.97 at 500, 0.94 at 0. */
	public static double speedFactor(int score) {
		return 1 - 2 * SPEED_SWING * (1 - clamp(score) / 1000.0);
	}

	/** STAMINA score 0-1000 as a multiplier on the fall with distance: 0.8 at 1000, 1.0 at 500, 1.2 at 0. */
	public static double staminaFall(int score) {
		return 1 + STAMINA_SWING * (1 - 2 * clamp(score) / 1000.0);
	}

	private static int clamp(int score) {
		return Math.max(0, Math.min(1000, score));
	}

	/** Age as a factor: 2.1% slower at 2 years, straight up to 1 at 4.45, then 0.25% slower a year. */
	public static double ageFactor(double years) {
		if (years <= PEAK_YEARS) {
			double y = Math.max(2, years);
			return 1 - AT_TWO * (PEAK_YEARS - y) / (PEAK_YEARS - 2);
		}
		return 1 - PER_YEAR_PAST_PEAK * (years - PEAK_YEARS);
	}

	/** Fitness 0-1 as a factor: an untrained horse 5% slower. */
	public static double fitnessFactor(double fitness) {
		return 1 - UNFIT * (1 - Math.max(0, Math.min(1, fitness)));
	}

	/** What a horse is, for its pace: breed shares, MSTN C count, SPEED and STAMINA scores, age in years and fitness 0-1. */
	public record Runner(Map<String, Integer> composition, int mstnC, int speed, int stamina, double ageYears, double fitness) {
		/** The best average pace over a run of {@code d} metres. */
		public double pace(double d) {
			double dd = Math.max(SHORTEST_M, d);
			double k = staminaFall(stamina);
			double p = Equine.weighted(composition, b -> {
				double start = record(b, SHORTEST_M);
				return start - k * (start - record(b, dd));
			});
			return p * mstn(mstnC, dd) * speedFactor(speed) * ageFactor(ageYears) * fitnessFactor(fitness);
		}

		/** Seconds to run {@code d} metres from a standing start. */
		public double time(double d) {
			return d <= 0 ? 0 : d / pace(d);
		}

		/** The speed, m/s, {@code x} metres into a run: one metre over the time that metre takes. */
		public double speedAt(double x) {
			double from = Math.max(0, x);
			double dt = time(from + 1) - time(from);
			return dt <= 0 ? pace(from + 1) : 1 / dt;
		}
	}

	// ---- fitness (horses.md, "Fitness (conditioning)") ----

	/** Each kilometre run at over half the horse's pace adds 0.02 fitness, at most 0.06 a real day *(proposed)*. */
	public static final double FITNESS_PER_KM = 0.02;
	public static final double FITNESS_PER_DAY = 0.06;
	/** Rest keeps fitness 42 real days, then it falls to nothing over the next 42 ("6 wk", TO VERIFY). */
	public static final double HELD_DAYS = 42;
	public static final double LOST_OVER_DAYS = 42;
	/** Walking this long resets the distance galloped *(proposed)*. */
	public static final double REST_SECONDS = 30;

	/** Fitness left {@code daysSinceWork} real days after the last work. */
	public static double rested(double fitness, double daysSinceWork) {
		double past = daysSinceWork - HELD_DAYS;
		return past <= 0 ? fitness : Math.max(0, fitness * (1 - past / LOST_OVER_DAYS));
	}

	/** Fitness gained for {@code km} galloped, given what has been gained already today. */
	public static double gain(double km, double gainedToday) {
		return Math.max(0, Math.min(km * FITNESS_PER_KM, FITNESS_PER_DAY - gainedToday));
	}
}
