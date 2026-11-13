package dev.farmings.process.creamery;

import dev.farmings.husbandry.TimeScale;
import java.util.ArrayList;
import java.util.List;

/**
 * What a creamery holds and does (docs/multiblocks/creamery.md). The controller calls {@link #step} once a second with which machines have
 * power. Plain Java; the Minecraft side only stores and shows this.
 */
public final class Creamery {
	public enum Mode {
		BOTTLE, CREAM, BUTTER, CHEESE;

		public Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	public enum Phase {
		IDLE, HEATING, HOLDING, COOLING, DONE
	}

	/** The bottler's products, in the order a glass bottle is filled. */
	public enum Bottle {
		MILK, CREAM, SKIM, BUTTERMILK, WHEY
	}

	public record Power(boolean heater, boolean separator, boolean churn, boolean vat) {
		public static final Power ALL = new Power(true, true, true, true);
	}

	public static final double TANK_L = 1000;
	public static final double BATCH_L = 200;
	/** Each store holds this much before the line backs up *(proposed)*. */
	public static final double STORE_L = 1000;
	public static final double STORE_BUTTER_KG = 500;
	public static final int SHELF_WHEELS = 64;
	public static final double BUTTER_ITEM_KG = 0.5;
	public static final double WHEEL_KG = 10;
	/** Holstein milk when nothing else is known (USDA AIPL, cattle.md). */
	public static final double DEFAULT_FAT = 3.72;
	public static final double DEFAULT_PROTEIN = 3.06;
	/** Heat-up, hold at 63 C (FDA PMO: 30 min), cool-down: 30 real minutes each, scaled 15:1. */
	public static final long PHASE_TICKS = TimeScale.realDaysToTicks(30.0 / (24 * 60));
	/** Pressing a wheel: 12 hours real *(proposed)*. */
	public static final long PRESS_TICKS = TimeScale.realDaysToTicks(0.5);
	/** Aging: 60 days real, 4 game days. */
	public static final long AGE_TICKS = TimeScale.realDaysToTicks(60);

	private double rawL;
	private double rawFat = DEFAULT_FAT;
	private double rawProtein = DEFAULT_PROTEIN;
	private Mode mode = Mode.BOTTLE;
	private Phase phase = Phase.IDLE;
	private long phaseTicks;
	private double batchL;
	private double batchFat;
	private double batchProtein;
	private final double[] bottles = new double[Bottle.values().length];
	private double butterKg;
	private double curdKg;
	private long pressing = -1;
	/** Wheels in the press now; a press takes every whole wheel of curd at once. */
	private int inPress;
	private final List<Long> aging = new ArrayList<>();
	private int agedWheels;
	private String fault = "";

	// ---- milk in ----

	public double rawL() {
		return rawL;
	}

	public double rawFat() {
		return rawFat;
	}

	public double room() {
		return TANK_L - rawL;
	}

	/** The milk the creamery is working (docs/multiblocks/creamery.md, "Sheep milk", "Goat milk and chèvre"). */
	private dev.farmings.husbandry.parlor.MilkKind kind = dev.farmings.husbandry.parlor.MilkKind.COW;

	public dev.farmings.husbandry.parlor.MilkKind kind() {
		return kind;
	}

	public void setKind(dev.farmings.husbandry.parlor.MilkKind kind) {
		this.kind = kind;
	}

	/** Nothing of any milk in it: raw milk, a batch, stores, butter, curd, wheels. Only then can it change milk kind. */
	public boolean empty() {
		double stores = 0;
		for (double b : bottles) {
			stores += b;
		}
		return rawL <= 1e-6 && batchL <= 1e-6 && stores <= 1e-6 && butterKg <= 1e-6 && curdKg <= 1e-6 && inPress == 0 && aging.isEmpty() && agedWheels == 0;
	}

	/** True if milk of this kind can go in now: the same kind, or an empty creamery. Kinds are never mixed. */
	public boolean accepts(dev.farmings.husbandry.parlor.MilkKind k) {
		return kind == k || empty();
	}

	/** Adds raw milk of one kind; refused (0) if the creamery holds another kind. */
	public double addMilk(double litres, double fat, double protein, dev.farmings.husbandry.parlor.MilkKind k) {
		if (!accepts(k)) {
			return 0;
		}
		kind = k;
		return addRaw(litres, fat, protein);
	}

	/** Adds raw cow milk (a bucket) with its fat and protein, mixed by volume; returns the litres that fit. */
	public double addMilk(double litres, double fat, double protein) {
		return addMilk(litres, fat, protein, dev.farmings.husbandry.parlor.MilkKind.COW);
	}

	private double addRaw(double litres, double fat, double protein) {
		double in = Math.max(0, Math.min(litres, room()));
		if (in <= 0) {
			return 0;
		}
		rawFat = (rawL * rawFat + in * fat) / (rawL + in);
		rawProtein = (rawL * rawProtein + in * protein) / (rawL + in);
		rawL += in;
		return in;
	}

	// ---- mode ----

	public Mode mode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public Phase phase() {
		return phase;
	}

	public String fault() {
		return fault;
	}

	// ---- running ----

	/** One step of {@code ticks}: pasteurizer phases, then the batch to its product, the press and the shelf. */
	public void step(long ticks, Power power) {
		fault = "";
		age(ticks);
		if (phase == Phase.IDLE && rawL >= 1) {
			batchL = Math.min(BATCH_L, rawL);
			batchFat = rawFat;
			batchProtein = rawProtein;
			rawL -= batchL;
			phase = Phase.HEATING;
			phaseTicks = 0;
		}
		if (phase == Phase.HEATING || phase == Phase.HOLDING || phase == Phase.COOLING) {
			if (!power.heater()) {
				fault = "NO POWER: batch pasteurizer";
			} else {
				phaseTicks += ticks;
				while (phaseTicks >= PHASE_TICKS && phase != Phase.DONE) {
					phaseTicks -= PHASE_TICKS;
					phase = phase == Phase.HEATING ? Phase.HOLDING : phase == Phase.HOLDING ? Phase.COOLING : Phase.DONE;
				}
			}
		}
		if (phase == Phase.DONE && route(power)) {
			phase = Phase.IDLE;
			batchL = 0;
			phaseTicks = 0;
		}
	}

	/** Sends the pasteurized batch on by mode; false (with a fault) if the next machine has no power or its store is full. */
	private boolean route(Power power) {
		switch (mode) {
			case BOTTLE -> {
				if (bottles[Bottle.MILK.ordinal()] + batchL > STORE_L) {
					fault = "BOTTLER FULL";
					return false;
				}
				bottles[Bottle.MILK.ordinal()] += batchL;
				return true;
			}
			case CREAM -> {
				if (!power.separator()) {
					fault = "NO POWER: cream separator";
					return false;
				}
				Dairy.Split s = Dairy.separate(batchL, batchFat);
				if (bottles[Bottle.CREAM.ordinal()] + s.creamL() > STORE_L || bottles[Bottle.SKIM.ordinal()] + s.skimL() > STORE_L) {
					fault = "BOTTLER FULL";
					return false;
				}
				bottles[Bottle.CREAM.ordinal()] += s.creamL();
				bottles[Bottle.SKIM.ordinal()] += s.skimL();
				return true;
			}
			case BUTTER -> {
				if (!power.separator()) {
					fault = "NO POWER: cream separator";
					return false;
				}
				if (!power.churn()) {
					fault = "NO POWER: butter churn";
					return false;
				}
				Dairy.Split s = Dairy.separate(batchL, batchFat);
				Dairy.Churned c = Dairy.churn(s.creamL(), Dairy.CREAM_FAT);
				if (butterKg + c.butterKg() > STORE_BUTTER_KG || bottles[Bottle.SKIM.ordinal()] + s.skimL() > STORE_L
						|| bottles[Bottle.BUTTERMILK.ordinal()] + c.buttermilkL() > STORE_L) {
					fault = "CHURN OR BOTTLER FULL";
					return false;
				}
				butterKg += c.butterKg();
				bottles[Bottle.SKIM.ordinal()] += s.skimL();
				bottles[Bottle.BUTTERMILK.ordinal()] += c.buttermilkL();
				return true;
			}
			case CHEESE -> {
				if (!power.vat()) {
					fault = "NO POWER: cheese vat";
					return false;
				}
				if (aging.size() + agedWheels >= SHELF_WHEELS) {
					fault = "SHELF FULL";
					return false;
				}
				double cheese = Dairy.cheeseKg(batchL, batchFat, batchProtein);
				double whey = Dairy.wheyL(batchL, cheese);
				if (bottles[Bottle.WHEY.ordinal()] + whey > STORE_L) {
					fault = "BOTTLER FULL";
					return false;
				}
				curdKg += cheese;
				bottles[Bottle.WHEY.ordinal()] += whey;
				return true;
			}
		}
		return false;
	}

	/** The press makes a wheel from each 10 kg of curd (all whole wheels at once); each wheel then ages on the shelf. */
	private void age(long ticks) {
		if (pressing < 0 && curdKg >= WHEEL_KG) {
			int n = (int) Math.min(Math.floor(curdKg / WHEEL_KG + 1e-9), SHELF_WHEELS - aging.size() - agedWheels);
			if (n > 0) {
				curdKg -= n * WHEEL_KG;
				inPress = n;
				pressing = 0;
			}
		}
		if (pressing >= 0) {
			pressing += ticks;
			if (pressing >= PRESS_TICKS) {
				pressing = -1;
				for (int i = 0; i < inPress; i++) {
					aging.add(0L);
				}
				inPress = 0;
			}
		}
		for (int i = aging.size() - 1; i >= 0; i--) {
			long a = aging.get(i) + ticks;
			if (a >= AGE_TICKS) {
				aging.remove(i);
				agedWheels++;
			} else {
				aging.set(i, a);
			}
		}
	}

	// ---- products out ----

	public double bottled(Bottle b) {
		return bottles[b.ordinal()];
	}

	/** Fills one glass bottle from the first product with a litre; null if none has. */
	public Bottle takeBottle() {
		for (Bottle b : Bottle.values()) {
			if (bottles[b.ordinal()] >= 1) {
				bottles[b.ordinal()] -= 1;
				return b;
			}
		}
		return null;
	}

	/** Puts back one litre a hopper took but could not hold. */
	public void returnBottle(Bottle b) {
		bottles[b.ordinal()] += 1;
	}

	/** What a glass bottle would be filled with now, or null. */
	public Bottle nextBottle() {
		for (Bottle b : Bottle.values()) {
			if (bottles[b.ordinal()] >= 1) {
				return b;
			}
		}
		return null;
	}

	public void returnButter(int blocks) {
		butterKg += blocks * BUTTER_ITEM_KG;
	}

	public void returnWheels(int n) {
		agedWheels += n;
	}

	public double butterKg() {
		return butterKg;
	}

	/** Takes whole butter blocks (0.5 kg each); returns how many. */
	public int takeButter(int max) {
		int n = (int) Math.min(max, Math.floor(butterKg / BUTTER_ITEM_KG + 1e-9));
		butterKg -= n * BUTTER_ITEM_KG;
		return n;
	}

	public double curdKg() {
		return curdKg;
	}

	public boolean pressingWheel() {
		return pressing >= 0;
	}

	public int agingWheels() {
		return aging.size();
	}

	public int agedWheels() {
		return agedWheels;
	}

	public int takeWheels(int max) {
		int n = Math.min(max, agedWheels);
		agedWheels -= n;
		return n;
	}

	// ---- saving: a flat array of numbers, so the Minecraft side need not know the fields ----

	/** Marks the save layout with whey (version 2); a first value of -2 cannot be a raw-milk volume, so older saves still read. */
	private static final double SAVE_V2 = -2;

	public double[] save() {
		double[] head = {SAVE_V2, rawL, rawFat, rawProtein, mode.ordinal(), phase.ordinal(), phaseTicks, batchL, batchFat, batchProtein,
				bottles[0], bottles[1], bottles[2], bottles[3], bottles[4], butterKg, curdKg, pressing, agedWheels, inPress, aging.size()};
		double[] out = new double[head.length + aging.size()];
		System.arraycopy(head, 0, out, 0, head.length);
		for (int i = 0; i < aging.size(); i++) {
			out[head.length + i] = aging.get(i);
		}
		return out;
	}

	public void load(double[] d) {
		if (d.length > 0 && d[0] == SAVE_V2) {
			// version 2: the old layout with a leading marker and whey after buttermilk
			if (d.length < 21) {
				return;
			}
			double[] old = new double[d.length - 2];
			System.arraycopy(d, 1, old, 0, 13);
			System.arraycopy(d, 15, old, 13, d.length - 15);
			load(old);
			bottles[Bottle.WHEY.ordinal()] = d[14];
			return;
		}
		if (d.length < 19) {
			return;
		}
		rawL = d[0];
		rawFat = d[1];
		rawProtein = d[2];
		mode = Mode.values()[(int) d[3] % Mode.values().length];
		phase = Phase.values()[(int) d[4] % Phase.values().length];
		phaseTicks = (long) d[5];
		batchL = d[6];
		batchFat = d[7];
		batchProtein = d[8];
		for (int i = 0; i < 4; i++) {
			bottles[i] = d[9 + i];
		}
		bottles[Bottle.WHEY.ordinal()] = 0;
		butterKg = d[13];
		curdKg = d[14];
		pressing = (long) d[15];
		agedWheels = (int) d[16];
		inPress = (int) d[17];
		int n = (int) d[18];
		aging.clear();
		for (int i = 0; i < n && 19 + i < d.length; i++) {
			aging.add((long) d[19 + i]);
		}
	}

	public String summary() {
		return String.format(java.util.Locale.ROOT, "%s, raw %.0f L (%.2f%% fat), pasteurizer %s; milk %.0f L, cream %.0f L, skim %.0f L, buttermilk %.0f L, whey %.0f L, butter %.1f kg, curd %.1f kg, wheels aging %d, aged %d",
				mode, rawL, rawFat, phase.name().toLowerCase(java.util.Locale.ROOT), bottles[0], bottles[1], bottles[2], bottles[3], bottles[4], butterKg, curdKg,
				aging.size() + inPress, agedWheels);
	}
}
