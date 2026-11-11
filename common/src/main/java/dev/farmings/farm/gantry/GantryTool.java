package dev.farmings.farm.gantry;

/**
 * The field gantry's tool heads (docs/multiblocks/field-gantry.md). Speeds are ASAE D497.5's typical field speeds; power uses its rotary
 * power parameters with the span as the working width. Plain Java.
 */
public enum GantryTool {
	/** Grain drill, 8.0 km/h. Draft 300 N a row (2.4-3.7 m drills), rows at 0.19 m (proposed): 3.5 kW per metre at 8 km/h (derived). */
	SEED_DRILL("seed drill", 8.0),
	/** Forage harvester, 5.0 km/h: 6.0 kW + 3.3 kWh per tonne of corn silage (4.0 for wilted alfalfa). Also combines grain. */
	HARVESTER("combine / forage chopper", 5.0),
	/** Disk mower-conditioner, 11 km/h: 8.0 kW per metre. */
	MOWER("mower", 11.0),
	/** Rake, 8 km/h and 2 kW per metre (both proposed: not in D497). */
	RAKE("rake", 8.0),
	/** Variable-chamber round baler, 8 km/h: 4.0 kW + 1.1 kWh per tonne. */
	BALER("round baler", 8.0),
	/** Spreader, 8 km/h and 2 kW per metre (proposed). */
	SPREADER("spreader", 8.0);

	public final String label;
	public final double kmPerHour;

	GantryTool(String label, double kmPerHour) {
		this.label = label;
		this.kmPerHour = kmPerHour;
	}

	public double metresPerSecond() {
		return kmPerHour / 3.6;
	}

	/** Ticks to cross one block at this tool's speed. */
	public int ticksPerBlock() {
		return Math.max(1, (int) Math.round(20.0 / metresPerSecond()));
	}

	/** Drive motors: two end trucks at 0.75 kW each (the middle of a linear-move irrigator's 0.5-1.5 hp a tower, rounded). */
	public static final double TRUCK_WATTS = 2 * 750;

	/**
	 * Watts at work across {@code spanMetres}, for tools whose power depends on throughput given {@code realKgPerSquareMetre} (the real,
	 * unscaled crop mass: x10 yields are a game choice, not more crop to push through the machine).
	 */
	public double watts(int spanMetres, double realKgPerSquareMetre) {
		double tonnesPerHour = spanMetres * metresPerSecond() * realKgPerSquareMetre * 3.6;
		double tool = switch (this) {
			case SEED_DRILL -> 300.0 * (spanMetres / 0.19) * metresPerSecond();
			case HARVESTER -> 6000 + 3300 * tonnesPerHour;
			case MOWER -> 8000.0 * spanMetres;
			case RAKE, SPREADER -> 2000.0 * spanMetres;
			case BALER -> 4000 + 1100 * tonnesPerHour;
		};
		return TRUCK_WATTS + tool;
	}

	/** The gantry's bin, kg (proposed). */
	public static final double BIN_KG = 4000;
	/** Spans the beam may have between its rails (proposed: up to the 10 m research gantries). */
	public static final int MIN_SPAN = 4;
	public static final int MAX_SPAN = 12;
	/** Rail lines run at most this far (proposed). */
	public static final int MAX_RAIL = 256;
	/** A swath can be raked after 1 real day of drying and baled after 1.5 (crops.md, proposed from 12-24 h and about 38 h). */
	public static final double RAKE_AFTER_DAYS = 1.0;
	public static final double BALE_AFTER_DAYS = 1.5;
	/** A round bale, kg (5 x 5 ft, UF/IFAS AN326). */
	public static final double BALE_KG = 454;
}
