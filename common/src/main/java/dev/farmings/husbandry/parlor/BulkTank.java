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
	/** Above this the tank is warm and milking stops *(proposed)*. */
	public static final double WARM_C = 10;
	/** Milk density, kg per litre. */
	public static final double KG_PER_L = 1.03;
	/** A manufacturer spec cools 35 C to 4 C in 3 h; scaled 15:1 (3 h real = 14,400 ticks). */
	public static final double COOL_C_PER_TICK = (MILK_IN_C - COLD_C) / 14_400.0;
	/** One bucket takes this many litres *(proposed)*. */
	public static final double BUCKET_L = 10;

	private double litres;
	private double tempC = COLD_C;

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

	/** Adds fresh milk at {@code inC}; returns the litres that fit. */
	public double add(double addL, double inC) {
		double in = Math.max(0, Math.min(addL, room()));
		if (in <= 0) {
			return 0;
		}
		tempC = (litres * tempC + in * inC) / (litres + in);
		litres += in;
		return in;
	}

	/** The chiller's work for {@code ticks}: down towards 4 C when running, nothing otherwise. */
	public void cool(int ticks, boolean running) {
		if (running && litres > 0 && tempC > COLD_C) {
			tempC = Math.max(COLD_C, tempC - COOL_C_PER_TICK * ticks);
		}
	}

	public double take(double wantL) {
		double out = Math.max(0, Math.min(wantL, litres));
		litres -= out;
		return out;
	}

	public void load(double litres, double tempC) {
		this.litres = Math.max(0, Math.min(CAPACITY_L, litres));
		this.tempC = tempC;
	}
}
