package dev.farmings.husbandry.barn;

/**
 * The barn is drawn once, with its pit at the east end, and may stand any of four ways round and mirrored. {@code turns} is how far
 * the drawing is turned clockwise (0 pit east, 1 pit south, 2 pit west, 3 pit north); {@code mirror} swaps the two stall rows.
 */
public record Orientation(int turns, boolean mirror) {
	/** The world offset (dx, dz) of a drawn offset (ax along the barn towards the pit, az across it, positive to the south side). */
	public int[] world(int ax, int az) {
		int z = mirror ? -az : az;
		int dx = ax;
		int dz = z;
		for (int i = 0; i < Math.floorMod(turns, 4); i++) {
			int t = dx;
			dx = -dz;
			dz = t;
		}
		return new int[] {dx, dz};
	}

	public static final Orientation[] ALL = {
			new Orientation(0, false), new Orientation(1, false), new Orientation(2, false), new Orientation(3, false),
			new Orientation(0, true), new Orientation(1, true), new Orientation(2, true), new Orientation(3, true)};
}
