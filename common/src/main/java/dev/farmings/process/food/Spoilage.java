package dev.farmings.process.food;

/**
 * How long Farmings food keeps (docs/food.md). Shelf lives are real days played as game days (1 real day = 1 game day = 24,000 ticks,
 * Tim 2026-11-09).
 * <ul>
 *   <li>Cold storage (a powered walk-in fridge) uses the USDA FoodKeeper refrigerator times, the middle of each range: milk and skim 7 days
 *       (Wyoming Dept. of Health FoodKeeper chart), cream 3.5 (half and half 3-4 days, FoodKeeper via American Dairy Association),
 *       buttermilk 10.5 (1-2 weeks), butter 45 (1-2 months), a whole cheddar 180 (hard cheese 6 months), cut cheddar 24.5 (3-4 weeks after
 *       opening), raw steak 4 (3-5 days, USDA FSIS), cooked beef 3.5 (3-4 days). Whey has no FoodKeeper entry: milk's 7 *(proposed)*.</li>
 *   <li>Room temperature: butter 1.5 days (FoodKeeper 1-2 days on the counter). Milk, cream, skim, buttermilk, whey and steaks keep 1 game
 *       day out of the cold *(proposed, Tim 2026-11-09; the only sourced figure is the USDA 2-hour safety rule)*. Cheddar: a whole wheel
 *       14 days, cut cheddar 1 day *(proposed)*.</li>
 * </ul>
 *
 * <p>Each food item carries the tick it would spoil at room temperature. Out of the cold that tick just comes; in a powered fridge it is
 * pushed later, by the share of the time the fridge saves ({@link #coldCredit}). Items made together carry the same tick and stack.
 */
public final class Spoilage {
	public static final long TICKS_PER_DAY = 24_000;
	/** Spoil ticks are rounded up to this, and a fridge credits its food once per this many ticks, so stacks stay alike *(proposed)*. */
	public static final long STEP = 1_200;

	public enum Spoiled { BOTTLE, BUTTER, CHEESE, STEAK, MUTTON, EGG, POULTRY }

	public enum Food {
		MILK(7, 1, Spoiled.BOTTLE),
		CREAM(3.5, 1, Spoiled.BOTTLE),
		SKIM(7, 1, Spoiled.BOTTLE),
		BUTTERMILK(10.5, 1, Spoiled.BOTTLE),
		WHEY(7, 1, Spoiled.BOTTLE),
		BUTTER(45, 1.5, Spoiled.BUTTER),
		CHEDDAR_WHEEL(180, 14, Spoiled.CHEESE),
		CHEDDAR(24.5, 1, Spoiled.CHEESE),
		RAW_STEAK(4, 1, Spoiled.STEAK),
		COOKED_STEAK(3.5, 1, Spoiled.STEAK),
		// Lamb chops: raw 3-5 days, cooked 3-4 days in the fridge (USDA FoodKeeper, lamb); the middles. Room 1 day as all (proposed).
		RAW_LAMB(4, 1, Spoiled.MUTTON),
		COOKED_LAMB(3.5, 1, Spoiled.MUTTON),
		// Eggs: "in the refrigerator (40 F). For best quality, use within 5 weeks" (USDA AMS HG 264); 7 days out of the cold (proposed).
		EGG(35, 7, Spoiled.EGG),
		// Poultry: raw 1-2 days, cooked 3-4 days in the fridge (USDA FSIS via FoodSafety.gov, TO VERIFY); the middles. Room 1 day (proposed).
		RAW_POULTRY(1.5, 1, Spoiled.POULTRY),
		COOKED_POULTRY(3.5, 1, Spoiled.POULTRY);

		public final double fridgeDays;
		public final double roomDays;
		public final Spoiled spoiled;

		Food(double fridgeDays, double roomDays, Spoiled spoiled) {
			this.fridgeDays = fridgeDays;
			this.roomDays = roomDays;
			this.spoiled = spoiled;
		}

		public long roomTicks() {
			return Math.round(roomDays * TICKS_PER_DAY);
		}

		public long fridgeTicks() {
			return Math.round(fridgeDays * TICKS_PER_DAY);
		}
	}

	private Spoilage() {
	}

	/** The tick food made (or taken out of the cold) at {@code now} spoils at room temperature, rounded up to a {@link #STEP}. */
	public static long spoilsAt(Food food, long now) {
		long t = now + food.roomTicks();
		return Math.floorDiv(t + STEP - 1, STEP) * STEP;
	}

	/**
	 * How far a fridge pushes the spoil tick for {@code ticks} kept cold: in that time the food used ticks / fridge life of its life, which
	 * at room temperature is ticks x room / fridge; the rest of the ticks are saved. Never negative.
	 */
	public static long coldCredit(Food food, long ticks) {
		double saved = ticks * (1.0 - (double) food.roomTicks() / food.fridgeTicks());
		return Math.max(0, Math.round(saved));
	}

	/**
	 * A food's life at {@code tempC} in ticks (docs/multiblocks/cold-room.md, "Spoilage by temperature"): its fridge life at 4 °C or
	 * colder, its room life at 20 °C or warmer, and log-linear between, so each food has its own Q10, (fridge / room)^(10/16) *(derived)*.
	 */
	public static double lifeAt(Food food, double tempC) {
		if (tempC <= dev.farmings.process.coldroom.ColdRoomTemp.FRIDGE_C) {
			return food.fridgeTicks();
		}
		if (tempC >= dev.farmings.process.coldroom.ColdRoomTemp.OUTSIDE_C) {
			return food.roomTicks();
		}
		double f = (tempC - dev.farmings.process.coldroom.ColdRoomTemp.FRIDGE_C)
				/ (dev.farmings.process.coldroom.ColdRoomTemp.OUTSIDE_C - dev.farmings.process.coldroom.ColdRoomTemp.FRIDGE_C);
		return Math.exp(Math.log(food.fridgeTicks()) + f * (Math.log(food.roomTicks()) - Math.log(food.fridgeTicks())));
	}

	/** The cold credit for {@code ticks} kept at {@code tempC}: the share of the time the cold saved. 0 at 20 °C, full fridge credit at 4. */
	public static long coldCredit(Food food, long ticks, double tempC) {
		double saved = ticks * (1.0 - food.roomTicks() / lifeAt(food, tempC));
		return Math.max(0, Math.round(saved));
	}

	public static boolean spoiled(long spoilsAt, long now) {
		return now >= spoilsAt;
	}

	/** How much of its room-temperature life is left, 0-1 (for the item's bar). */
	public static double left(Food food, long spoilsAt, long now) {
		return Math.max(0, Math.min(1, (spoilsAt - now) / (double) food.roomTicks()));
	}
}
