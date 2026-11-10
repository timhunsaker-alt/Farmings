package dev.farmings.process.wool;

import dev.farmings.husbandry.Flock;

/**
 * The wool mill's rules (docs/multiblocks/wool-mill.md), in kilograms. Plain Java. Sources are in the doc; values without one are marked
 * proposed here and there.
 */
public final class WoolProcess {
	/** Scouring water, 10-20 L per kg of greasy wool (US patent 4168143); the middle. */
	public static final double WATER_L_PER_GREASY_KG = 15;
	/** Wool grease is about 15% of greasy wool (an Australian scour: 150 kg lanolin per tonne). */
	public static final double GREASE_SHARE = 0.15;
	/** Share of that grease the bowl's centrifuge recovers as lanolin *(proposed; no figure found for water scouring)*. */
	public static final double LANOLIN_RECOVERY = 0.30;
	/** One lanolin item, kg *(proposed)*. */
	public static final double LANOLIN_ITEM_KG = 0.1;
	/** Clean yield used for loose (shed) wool, which carries no breed figure *(proposed)*. */
	public static final double LOOSE_WOOL_YIELD = 0.50;
	/** Loose wool from one shed coat, kg (Dorper 0.5-1 kg, sheep.md). */
	public static final double LOOSE_WOOL_KG = 0.75;
	/** Carding waste: short fibre and noil lost at the card *(proposed)*. */
	public static final double CARDING_LOSS = 0.05;
	/** One yarn item, kg: a 100 g skein *(proposed)*. */
	public static final double YARN_ITEM_KG = 0.1;
	/** Yarn items a loom weaves into one cloth *(proposed)*. */
	public static final int YARN_PER_CLOTH = 4;
	/** Clean wool in one felt *(proposed)*. */
	public static final double FELT_ITEM_KG = 0.25;
	/** Ticks one batch takes in any machine *(proposed)*. */
	public static final int BATCH_TICKS = 100;

	private WoolProcess() {
	}

	public record Scoured(double cleanKg, double lanolinKg, double waterL) {
	}

	public static Scoured scour(double greasyKg, double cleanYield) {
		double y = Math.max(0, Math.min(1, cleanYield));
		return new Scoured(greasyKg * y, greasyKg * GREASE_SHARE * LANOLIN_RECOVERY, greasyKg * WATER_L_PER_GREASY_KG);
	}

	public static double card(double cleanKg) {
		return cleanKg * (1 - CARDING_LOSS);
	}

	/**
	 * Extra yarn from finer wool: a finer fibre spins a finer, longer yarn from the same weight, so a fine fleece makes more skeins
	 * *(proposed shares)*.
	 */
	public static double spinBonus(Flock.Grade grade) {
		return switch (grade) {
			case FINE -> 0.25;
			case HALF_BLOOD -> 0.15;
			case THREE_EIGHTHS -> 0.10;
			case QUARTER -> 0.05;
			case COARSE, HAIR -> 0.0;
		};
	}

	/** Yarn (in items, fractional) spun from {@code sliverKg} of one grade. */
	public static double yarnItems(double sliverKg, Flock.Grade grade) {
		return sliverKg / YARN_ITEM_KG * (1 + spinBonus(grade));
	}
}
