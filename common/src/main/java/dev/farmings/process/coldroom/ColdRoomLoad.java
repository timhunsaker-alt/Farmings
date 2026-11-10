package dev.farmings.process.coldroom;

/**
 * The refrigeration a walk-in fridge needs, from its inside volume (docs/multiblocks/cold-room.md). A vendor's walk-in cooler load table at
 * 35 F, typical use (Atlantic Series, walk-in cooler BTU calculator): 6 x 6 x 8 ft 4,986 BTU/h, 8 x 8 x 8 ft 6,258, 10 x 10 x 8 ft 8,230.
 * Between those it is interpolated by volume, outside them extended along the nearest two (load per cubic foot falls as boxes grow,
 * because wall area grows slower than volume). The condensing unit's electric draw is the cooling over a coefficient of performance of 2
 * *(proposed, no source)*.
 */
public final class ColdRoomLoad {
	public static final double FT3_PER_M3 = 35.3147;
	public static final double W_PER_BTU_H = 0.29307;
	public static final double COP = 2.0;
	private static final double[] FT3 = {6 * 6 * 8, 8 * 8 * 8, 10 * 10 * 8};
	private static final double[] BTU_H = {4986, 6258, 8230};

	private ColdRoomLoad() {
	}

	public static double btuPerHour(double insideM3) {
		double v = insideM3 * FT3_PER_M3;
		int i = v <= FT3[1] ? 0 : 1;
		double t = (v - FT3[i]) / (FT3[i + 1] - FT3[i]);
		return Math.max(BTU_H[0] * 0.5, BTU_H[i] + t * (BTU_H[i + 1] - BTU_H[i]));
	}

	public static double coolingWatts(double insideM3) {
		return btuPerHour(insideM3) * W_PER_BTU_H;
	}

	/** What the condensing unit draws from the grid. */
	public static double electricWatts(double insideM3) {
		return coolingWatts(insideM3) / COP;
	}
}
