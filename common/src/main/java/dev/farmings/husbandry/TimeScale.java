package dev.farmings.husbandry;

import dev.farmings.genetics.Species;

/**
 * 1 game day = 15 real days for cattle and sheep (decided with Tim, 2026-11-07), and 3 real days for poultry (Tim, 2026-11-10;
 * docs/animals/poultry.md). A game day is one Minecraft day, 24,000 ticks (20 minutes). Durations are stored in real days and converted
 * here. Output per game day (milk, manure, eggs) is one real day's output, whatever the scale.
 *
 * <p>The static methods are the cattle and sheep scale; species-aware code uses {@link #of(Species)}.
 */
public final class TimeScale {
	/** The cattle and sheep config value. */
	public static final int REAL_DAYS_PER_GAME_DAY = 15;
	/** The poultry config value. */
	public static final int FOWL_REAL_DAYS_PER_GAME_DAY = 3;
	public static final int TICKS_PER_GAME_DAY = 24_000;

	/** One species' scale. */
	public record Scale(int realDaysPerGameDay) {
		public double realDaysToGameDays(double realDays) {
			return realDays / realDaysPerGameDay;
		}

		public double gameDaysToRealDays(double gameDays) {
			return gameDays * realDaysPerGameDay;
		}

		public long realDaysToTicks(double realDays) {
			return Math.round(realDays / realDaysPerGameDay * TICKS_PER_GAME_DAY);
		}

		public double ticksToRealDays(long ticks) {
			return (double) ticks / TICKS_PER_GAME_DAY * realDaysPerGameDay;
		}
	}

	public static final Scale LIVESTOCK = new Scale(REAL_DAYS_PER_GAME_DAY);
	public static final Scale FOWL = new Scale(FOWL_REAL_DAYS_PER_GAME_DAY);

	private TimeScale() {
	}

	/** The scale an animal of this species lives on. */
	public static Scale of(Species species) {
		return switch (species) {
			case CATTLE, SHEEP -> LIVESTOCK;
			default -> FOWL;
		};
	}

	public static double realDaysToGameDays(double realDays) {
		return LIVESTOCK.realDaysToGameDays(realDays);
	}

	public static double gameDaysToRealDays(double gameDays) {
		return LIVESTOCK.gameDaysToRealDays(gameDays);
	}

	public static long realDaysToTicks(double realDays) {
		return LIVESTOCK.realDaysToTicks(realDays);
	}

	public static double ticksToRealDays(long ticks) {
		return LIVESTOCK.ticksToRealDays(ticks);
	}
}
