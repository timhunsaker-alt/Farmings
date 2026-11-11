package dev.farmings.farm.compost;

/**
 * A compost windrow (docs/multiblocks/compost.md). Inputs are blended by mass: moisture by water over wet mass, and C:N assuming every
 * input's dry matter holds the same share of carbon, so nitrogen is dry mass over C:N (proposed: the spec's sources give C:N, not carbon).
 * A started pile composts while it is turned on time: once every 11.2 real days (8 weeks over 5 turns), and a pile that misses a turn
 * waits for it. It is done after 56 days and 5 turns, weighing 42.5% of what went in (NDSU: 50-65% less). Plain Java.
 */
public final class Windrow {
	public static final double MIN_CN = 20;
	public static final double MAX_CN = 40;
	public static final double MIN_MOISTURE = 0.40;
	public static final double MAX_MOISTURE = 0.65;
	public static final double DAYS = 56;
	public static final int TURNS = 5;
	public static final double TURN_EVERY_DAYS = DAYS / TURNS;
	public static final double KEPT = 1 - 0.575;
	/** Wet kg a pad cell holds (proposed). */
	public static final double KG_PER_CELL = 2000;

	private double wet;
	private double water;
	private double dry;
	private double nitrogen;
	private boolean started;
	private double age;
	private int turns;

	public double wetKg() {
		return wet;
	}

	public double moisture() {
		return wet <= 0 ? 0 : water / wet;
	}

	/** C:N of the blend; 0 for an empty pile or plain water. */
	public double cn() {
		return nitrogen <= 0 ? 0 : dry / nitrogen;
	}

	public boolean started() {
		return started;
	}

	public double ageDays() {
		return age;
	}

	public int turns() {
		return turns;
	}

	public boolean done() {
		return started && age >= DAYS && turns >= TURNS;
	}

	/** Adds {@code kg} of an input with this C:N (0 for water) and moisture (0-1). Not after the pile has started. */
	public boolean add(double kg, double cn, double moisture) {
		if (started || kg <= 0) {
			return false;
		}
		wet += kg;
		water += kg * moisture;
		double d = kg * (1 - moisture);
		if (cn > 0) {
			dry += d;
			nitrogen += d / cn;
		}
		return true;
	}

	/** What stops the pile from starting, or "" if it can. */
	public String problem() {
		if (wet <= 0) {
			return "EMPTY";
		}
		double m = moisture();
		if (m > MAX_MOISTURE) {
			return "TOO WET";
		}
		if (m < MIN_MOISTURE) {
			return "TOO DRY";
		}
		double c = cn();
		if (c > MAX_CN) {
			return "NEEDS NITROGEN";
		}
		if (c < MIN_CN) {
			return "NEEDS CARBON";
		}
		return "";
	}

	public boolean start() {
		if (started || !problem().isEmpty()) {
			return false;
		}
		started = true;
		return true;
	}

	/** Whether a turn is due (the pile has aged to its next turn). */
	public boolean turnDue() {
		return started && turns < TURNS && age >= (turns + 1) * TURN_EVERY_DAYS - 1e-9;
	}

	public void turn() {
		if (turnDue()) {
			turns++;
		}
	}

	/** {@code days} pass; the pile ages only up to its next due turn. */
	public void age(double days) {
		if (!started) {
			return;
		}
		double cap = turns < TURNS ? (turns + 1) * TURN_EVERY_DAYS : Double.MAX_VALUE;
		age = Math.min(cap, age + days);
	}

	/** Finished compost, kg; the pile is emptied. */
	public double takeCompost() {
		if (!done()) {
			return 0;
		}
		double kg = wet * KEPT;
		wet = water = dry = nitrogen = age = 0;
		turns = 0;
		started = false;
		return kg;
	}

	public double[] save() {
		return new double[] {wet, water, dry, nitrogen, started ? 1 : 0, age, turns};
	}

	public void load(double[] v) {
		if (v == null || v.length < 7) {
			return;
		}
		wet = v[0];
		water = v[1];
		dry = v[2];
		nitrogen = v[3];
		started = v[4] > 0;
		age = v[5];
		turns = (int) v[6];
	}
}
