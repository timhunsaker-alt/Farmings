package dev.farmings.feed;

import dev.farmings.husbandry.Stage;

/**
 * One tending step for one animal: how much she eats and drinks over a stretch of time, and the manure that comes of it
 * (docs/animals/cattle.md, docs/waste.md). Plain Java.
 *
 * <p>Output per game day is one real day's (decided with Tim), so a day's intake is spread over {@link #TICKS_PER_DAY} ticks.
 * Water follows the UC Davis rule: 6 L per kg of dry matter she should eat, plus 1 L per litre of milk. Manure follows ASAE D384.2
 * by stage ({@link #manureKgPerDay}), scaled by how much of her ration she actually ate. Stages without a D384.2 figure use the
 * Florida study instead: {@link #MANURE_KG_PER_KG_DM} kg per kg of dry matter eaten (44.6 kg from about 20 kg, waste.md).
 * Shortfalls are not fatal (the underfeeding rule: less milk, slower growth, lower health, never death); this step only reports them.
 */
public final class Intake {
	public static final int TICKS_PER_DAY = 24_000;
	/** 44.6 kg of raw manure a day from about 20 kg of dry matter (Florida lactating cow study, docs/waste.md). Stand-in for stages D384.2 lacks. */
	public static final double MANURE_KG_PER_KG_DM = 44.6 / 20.0;
	/** Lactating cow, 67 kg a day as excreted at 75 lb of milk ("60/67/74/81" kg at 50/75/100/125 lb; ASAE D384.2 via USDA NRCS, Table 4-5). */
	public static final double LACTATING_MANURE_KG = 67;
	/** Dry cow, 39 kg a day as excreted (ASAE D384.2 via USDA NRCS, Table 4-5). */
	public static final double DRY_MANURE_KG = 39;
	/** Heifer, 24 kg a day as excreted (ASAE D384.2 via USDA NRCS, Table 4-5). */
	public static final double HEIFER_MANURE_KG = 24;
	/** One manure item is 50 kg (decided with Tim). */
	public static final double MANURE_ITEM_KG = 50;

	private Intake() {
	}

	/**
	 * @param eatenKg    dry matter eaten
	 * @param drunkL     water drunk
	 * @param manureKg   manure made
	 * @param fed        eaten / wanted, 1 when she wanted nothing
	 * @param watered    drunk / wanted, 1 when she wanted nothing
	 */
	public record Result(double eatenKg, double drunkL, double manureKg, double fed, double watered) {
	}

	/**
	 * Manure as excreted, kg a day, on a full ration: the ASAE D384.2 dairy figures by stage, or empty for a stage the table does not
	 * give (bulls, steers, retired cows, calves).
	 */
	public static java.util.OptionalDouble manureKgPerDay(Stage stage) {
		return switch (stage) {
			case LACTATING_COW -> java.util.OptionalDouble.of(LACTATING_MANURE_KG);
			case DRY_COW -> java.util.OptionalDouble.of(DRY_MANURE_KG);
			case HEIFER, BRED_HEIFER -> java.util.OptionalDouble.of(HEIFER_MANURE_KG);
			default -> java.util.OptionalDouble.empty();
		};
	}

	/** As {@link #step(double, double, int, double, double, double)} with manure from dry matter eaten (the Florida stand-in). */
	public static Result step(double dmiKgPerDay, double milkLitresDay, int ticks, double hayKgAvailable, double waterLAvailable) {
		return step(dmiKgPerDay, milkLitresDay, ticks, hayKgAvailable, waterLAvailable, -1);
	}

	/**
	 * @param dmiKgPerDay    the dry matter she should eat in a day (0 for a calf)
	 * @param milkLitresDay  the milk she gives in a day (0 until milking exists)
	 * @param ticks          how long this step covers
	 * @param hayKgAvailable dry matter in her feeder
	 * @param waterLAvailable water in her trough
	 * @param manureKgPerDay manure on a full ration ({@link #manureKgPerDay(Stage)}), or negative to use the Florida stand-in
	 */
	public static Result step(double dmiKgPerDay, double milkLitresDay, int ticks, double hayKgAvailable, double waterLAvailable,
			double manureKgPerDay) {
		double share = (double) ticks / TICKS_PER_DAY;
		double wantKg = Math.max(0, dmiKgPerDay) * share;
		double wantL = Hay.waterLitresPerDay(Math.max(0, dmiKgPerDay), Math.max(0, milkLitresDay)) * share;
		double eaten = Math.min(wantKg, Math.max(0, hayKgAvailable));
		double drunk = Math.min(wantL, Math.max(0, waterLAvailable));
		double fed = wantKg <= 0 ? 1 : eaten / wantKg;
		double watered = wantL <= 0 ? 1 : drunk / wantL;
		double manure = manureKgPerDay < 0 ? eaten * MANURE_KG_PER_KG_DM : wantKg <= 0 ? 0 : manureKgPerDay * share * fed;
		return new Result(eaten, drunk, manure, fed, watered);
	}
}
