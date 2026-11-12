package dev.farmings.process.coldroom;

/**
 * The cold room's air temperature (docs/multiblocks/cold-room.md, "the room's temperature"). Plain Java.
 * <ul>
 *   <li>Set point 2 °C *(proposed)*, under the 4 °C (40 °F) a fridge should be at most (USDA via Virginia Tech FST-518); outside 20 °C.</li>
 *   <li>Walls: U = 0.23 W/m²K over the inside surface (R-25, the legal minimum for walk-in cooler panels, 10 CFR 431.306, converted).</li>
 *   <li>A door opened swaps one room of air: 1.2 kJ/m³K x volume x (20 - T) *(proposed: no door-load figure found)*.</li>
 *   <li>Thermal mass: set so a shut room with no power warms from 2 to 5 °C in 4 hours (FST-518: a fridge "lasts ~4 hours"; derived):
 *       T(t) = 20 - 18 e^(-t UA / C), so C = UA x 4 h / ln(18 / 15).</li>
 *   <li>Cooling: with power, up to the load table's cooling (ColdRoomLoad), never below the set point.</li>
 * </ul>
 */
public final class ColdRoomTemp {
	public static final double SET_POINT_C = 2.0;
	public static final double OUTSIDE_C = 20.0;
	/** At or below this the food keeps its fridge life (FoodKeeper times are for 40 °F). */
	public static final double FRIDGE_C = 4.0;
	public static final double U_W_PER_M2K = 0.23;
	public static final double AIR_J_PER_M3K = 1200;
	public static final double WARM_UP_SECONDS = 4 * 3600;

	private ColdRoomTemp() {
	}

	/** Inside surface area of a w x h x l box, m². */
	public static double area(int w, int h, int l) {
		return 2.0 * (w * h + w * l + h * l);
	}

	/** Heat through the walls per kelvin, W/K. */
	public static double ua(int w, int h, int l) {
		return U_W_PER_M2K * area(w, h, l);
	}

	/** The room's thermal mass, J/K (see the class note). */
	public static double mass(int w, int h, int l) {
		return ua(w, h, l) * WARM_UP_SECONDS / Math.log(18.0 / 15.0);
	}

	/**
	 * The temperature after {@code seconds}: heat in through the walls, out by the unit when powered (at most {@code coolingW}), held at
	 * the set point. Stepped exactly for the walls (an exponential), so long steps stay right.
	 */
	public static double step(double tempC, double seconds, boolean powered, double coolingW, int w, int h, int l) {
		double ua = ua(w, h, l);
		double c = mass(w, h, l);
		// with the unit running flat out, the room heads for 20 - coolingW / UA; walls alone, for 20
		double target = powered ? OUTSIDE_C - coolingW / ua : OUTSIDE_C;
		double t = target + (tempC - target) * Math.exp(-seconds * ua / c);
		return powered ? Math.max(SET_POINT_C, t) : t;
	}

	/** The temperature after a door is opened once. */
	public static double doorOpened(double tempC, int w, int h, int l) {
		double volume = (double) w * h * l;
		return Math.min(OUTSIDE_C, tempC + AIR_J_PER_M3K * volume * (OUTSIDE_C - tempC) / mass(w, h, l));
	}
}
