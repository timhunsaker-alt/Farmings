package dev.farmings.farm.silo;

/**
 * A tower silo or grain bin (docs/multiblocks/silo.md): a square ring of wall, 3 x 3 or 5 x 5 with a hollow middle, stacked layer on layer,
 * capped by a roof over the middle. Found from a part beside the bottom ring. Plain Java over a {@link Grid}.
 */
public final class SiloLayout {
	public enum Cell { WALL, ROOF, EMPTY, OTHER }

	public interface Grid {
		Cell at(int x, int y, int z);
	}

	/** A found tower: centre column, bottom layer, half width (1 or 2), height in layers; or a problem. */
	public record Tower(String problem, int cx, int y0, int cz, int half, int height) {
		public boolean formed() {
			return problem.isEmpty();
		}

		/** Hollow cells in one layer: 1 for 3 x 3, 9 for 5 x 5. */
		public int interior() {
			int w = 2 * half - 1;
			return w * w;
		}

		/** Hollow volume, m3. */
		public int volume() {
			return interior() * height;
		}
	}

	private SiloLayout() {
	}

	private static boolean ring(Grid g, int cx, int y, int cz, int half) {
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				boolean edge = Math.abs(dx) == half || Math.abs(dz) == half;
				Cell c = g.at(cx + dx, y, cz + dz);
				if (edge ? c != Cell.WALL : c != Cell.EMPTY) {
					return false;
				}
			}
		}
		return true;
	}

	/** The tower whose bottom ring holds the wall cell (wx, y, wz), at most {@code maxHeight} layers. */
	public static Tower find(Grid g, int wx, int y, int wz, int maxHeight) {
		for (int half = 1; half <= 2; half++) {
			for (int cx = wx - half; cx <= wx + half; cx++) {
				for (int cz = wz - half; cz <= wz + half; cz++) {
					if (Math.max(Math.abs(wx - cx), Math.abs(wz - cz)) != half || !ring(g, cx, y, cz, half)) {
						continue;
					}
					if (ring(g, cx, y - 1, cz, half)) {
						continue;     // not the bottom ring
					}
					int h = 1;
					while (h < maxHeight && ring(g, cx, y + h, cz, half)) {
						h++;
					}
					if (g.at(cx, y + h, cz) != Cell.ROOF) {
						return new Tower("NO ROOF over the top of the ring", cx, y, cz, half, h);
					}
					return new Tower("", cx, y, cz, half, h);
				}
			}
		}
		return new Tower("NOT A RING: a 3 x 3 or 5 x 5 ring of wall with a hollow middle", wx, y, wz, 1, 0);
	}
}
