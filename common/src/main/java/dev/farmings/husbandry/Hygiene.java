package dev.farmings.husbandry;

/**
 * The dirty-stall rule (docs/multiblocks/barn.md, docs/waste.md). No source gives the size of the effect, so every value here is
 * proposed (Tim chose "build, marked proposed" on 2026-11-09). Uncollected manure in the alley in front of her stall lowers her milk
 * and slowly her health. She never dies from it.
 */
public final class Hygiene {
	/**
	 * One day's manure in front of her is normal: the scraper passes once a game day. Each item beyond that adds a quarter, so five
	 * items (four days uncollected) is fully dirty (proposed).
	 */
	public static final int NORMAL_ITEMS = 1;
	public static final int FULLY_DIRTY_ITEMS = 5;
	/** Milk lost when fully dirty (proposed: 10%). */
	public static final double MAX_MILK_LOSS = 0.10;
	/** Health lost per game day while at least half dirty (proposed: 1 point of 10). */
	public static final double HEALTH_LOSS_PER_GAME_DAY = 1.0;
	/** Health never falls below this from dirt (proposed: 2 of 10), so she never dies. */
	public static final double HEALTH_FLOOR = 2.0;

	private Hygiene() {
	}

	/** 0 (clean) to 1 (fully dirty) from the manure items lying in front of her stall. */
	public static double dirtyFraction(int manureItems) {
		return Math.max(0, Math.min(1, (double) (manureItems - NORMAL_ITEMS) / (FULLY_DIRTY_ITEMS - NORMAL_ITEMS)));
	}

	public static double milkFactor(double dirty) {
		return 1 - MAX_MILK_LOSS * Math.max(0, Math.min(1, dirty));
	}

	/** Health lost over {@code ticks} at this dirt level, never taking her below the floor. */
	public static double healthLoss(double dirty, double health, long ticks) {
		if (dirty < 0.5 || health <= HEALTH_FLOOR) {
			return 0;
		}
		double loss = HEALTH_LOSS_PER_GAME_DAY * ticks / (double) TimeScale.TICKS_PER_GAME_DAY;
		return Math.min(loss, health - HEALTH_FLOOR);
	}
}
