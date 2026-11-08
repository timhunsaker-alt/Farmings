package dev.farmings.husbandry;

/**
 * 1 game day = 15 real days (decided with Tim, 2026-11-07). A game day is one Minecraft day, 24,000 ticks (20 minutes).
 * Durations are stored in real days and converted here. Output per game day (milk, manure) is one real day's output.
 */
public final class TimeScale {
	/** The single config value. */
	public static final int REAL_DAYS_PER_GAME_DAY = 15;
	public static final int TICKS_PER_GAME_DAY = 24_000;

	private TimeScale() {
	}

	public static double realDaysToGameDays(double realDays) {
		return realDays / REAL_DAYS_PER_GAME_DAY;
	}

	public static double gameDaysToRealDays(double gameDays) {
		return gameDays * REAL_DAYS_PER_GAME_DAY;
	}

	public static long realDaysToTicks(double realDays) {
		return Math.round(realDays / REAL_DAYS_PER_GAME_DAY * TICKS_PER_GAME_DAY);
	}

	public static double ticksToRealDays(long ticks) {
		return (double) ticks / TICKS_PER_GAME_DAY * REAL_DAYS_PER_GAME_DAY;
	}
}
