package dev.farmings.feed;

/**
 * One tending step for one animal: how much she eats and drinks over a stretch of time, and the manure that comes of it
 * (docs/animals/cattle.md, docs/waste.md). Plain Java.
 *
 * <p>Output per game day is one real day's (decided with Tim), so a day's intake is spread over {@link #TICKS_PER_DAY} ticks.
 * Water follows the UC Davis rule: 6 L per kg of dry matter she should eat, plus 1 L per litre of milk. Manure follows what she
 * actually ate: {@link #MANURE_KG_PER_KG_DM} kg per kg of dry matter, from the Florida study in waste.md (44.6 kg from about 20 kg).
 * Shortfalls are not fatal (the underfeeding rule: less milk, slower growth, lower health, never death); this step only reports them.
 */
public final class Intake {
	public static final int TICKS_PER_DAY = 24_000;
	/** 44.6 kg of raw manure a day from about 20 kg of dry matter (Florida lactating cow study, docs/waste.md). */
	public static final double MANURE_KG_PER_KG_DM = 44.6 / 20.0;
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
	 * @param dmiKgPerDay    the dry matter she should eat in a day (0 for a calf)
	 * @param milkLitresDay  the milk she gives in a day (0 until milking exists)
	 * @param ticks          how long this step covers
	 * @param hayKgAvailable dry matter in her feeder
	 * @param waterLAvailable water in her trough
	 */
	public static Result step(double dmiKgPerDay, double milkLitresDay, int ticks, double hayKgAvailable, double waterLAvailable) {
		double share = (double) ticks / TICKS_PER_DAY;
		double wantKg = Math.max(0, dmiKgPerDay) * share;
		double wantL = Hay.waterLitresPerDay(Math.max(0, dmiKgPerDay), Math.max(0, milkLitresDay)) * share;
		double eaten = Math.min(wantKg, Math.max(0, hayKgAvailable));
		double drunk = Math.min(wantL, Math.max(0, waterLAvailable));
		double fed = wantKg <= 0 ? 1 : eaten / wantKg;
		double watered = wantL <= 0 ? 1 : drunk / wantL;
		return new Result(eaten, drunk, eaten * MANURE_KG_PER_KG_DM, fed, watered);
	}
}
