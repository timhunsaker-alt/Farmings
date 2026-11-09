package dev.farmings.husbandry.parlor;

/**
 * The bulk tank's milk (docs/multiblocks/parlor.md): litres and temperature. Fresh milk comes in at body-warm and is mixed by volume; a
 * running chiller pulls the tank down to 4 C. Spoilage is not modelled yet. Plain Java.
 */
public final class BulkTank {
	/** A 2 x 2 x 2 tank holds 1,000 L (Tim, 2026-11-08; a suggestion, not a source). */
	public static final double CAPACITY_L = 1000;
	public static final double COLD_C = 4;
	/** Milk leaves the cow at about 35 C (manufacturer cooling figure in parlor.md). */
	public static final double MILK_IN_C = 35;
	/** Above this the tank is warm *(proposed)*. */
	public static final double WARM_C = 10;
	/**
	 * A milking session: milk keeps going in while the last of it went in less than this long ago, however warm the tank is, because a herd's
	 * warm milk all goes into the tank and is cooled after (Tim chose this 2026-11-09). About 2 h real (the milk must be cooled within 2 h),
	 * scaled 15:1 = 8 min = 9,600 ticks would be the plain scaling; 6,000 ticks (about 100 real minutes) is used *(proposed)*.
	 */
	public static final long SESSION_TICKS = 6_000;
	/** Milk density, kg per litre. */
	public static final double KG_PER_L = 1.03;
	/** A manufacturer spec cools 35 C to 4 C in 3 h; scaled 15:1 (3 h real = 14,400 ticks). */
	public static final double COOL_C_PER_TICK = (MILK_IN_C - COLD_C) / 14_400.0;
	/** One bucket takes this many litres *(proposed)*. */
	public static final double BUCKET_L = 10;

	private double litres;
	private double tempC = COLD_C;
	private long sinceAddTicks = Long.MAX_VALUE / 2;

	public double litres() {
		return litres;
	}

	public double tempC() {
		return tempC;
	}

	public double room() {
		return CAPACITY_L - litres;
	}

	public boolean warm() {
		return tempC > WARM_C;
	}

	/** Whether a cow's milk is refused now: the tank is warm and no milking session is under way. */
	public boolean blocksMilking() {
		return warm() && sinceAddTicks >= SESSION_TICKS;
	}

	/** Adds fresh milk at {@code inC}; returns the litres that fit. */
	public double add(double addL, double inC) {
		double in = Math.max(0, Math.min(addL, room()));
		if (in <= 0) {
			return 0;
		}
		tempC = (litres * tempC + in * inC) / (litres + in);
		sinceAddTicks = 0;
		litres += in;
		return in;
	}

	/** The chiller's work for {@code ticks}: down towards 4 C when running, nothing otherwise. */
	public void cool(int ticks, boolean running) {
		sinceAddTicks = Math.min(Long.MAX_VALUE / 2, sinceAddTicks + ticks);
		if (running && litres > 0 && tempC > COLD_C) {
			tempC = Math.max(COLD_C, tempC - COOL_C_PER_TICK * ticks);
		}
	}

	public double take(double wantL) {
		double out = Math.max(0, Math.min(wantL, litres));
		litres -= out;
		return out;
	}

	public long sinceAddTicks() {
		return sinceAddTicks;
	}

	public void load(double litres, double tempC, long sinceAddTicks) {
		this.litres = Math.max(0, Math.min(CAPACITY_L, litres));
		this.tempC = tempC;
		this.sinceAddTicks = sinceAddTicks;
	}
}
