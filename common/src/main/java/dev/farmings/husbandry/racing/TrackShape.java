package dev.farmings.husbandry.racing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A race track's shape (docs/multiblocks/race-track.md, "Forming"), from its surface cells. Plain Java.
 *
 * <p>A track must be a ring around its middle, an oval or rounded rectangle *(proposed: figure-eights don't form)*: the middle of the
 * cells is not a track cell, and every direction out from it crosses the track. The centre line is the middle of the cells in each of
 * {@link #SECTORS} directions, joined into a closed line run counter-clockwise seen from above, as American racing *(proposed)*. The lap
 * length is that line's length, 1 block = 1 m.
 */
public final class TrackShape {
	/** Directions the ring is cut into: 5° each. */
	public static final int SECTORS = 72;

	private final double cx;
	private final double cz;
	private final double[] px;
	private final double[] pz;
	private final double[] at;
	private final double lap;
	private final String problem;

	private TrackShape(double cx, double cz, double[] px, double[] pz, String problem) {
		this.cx = cx;
		this.cz = cz;
		this.px = px;
		this.pz = pz;
		this.problem = problem;
		this.at = new double[px.length + 1];
		for (int i = 0; i < px.length; i++) {
			int j = (i + 1) % px.length;
			at[i + 1] = at[i] + Math.hypot(px[j] - px[i], pz[j] - pz[i]);
		}
		this.lap = px.length == 0 ? 0 : at[px.length];
	}

	/** The shape of these surface cells ({x, z} block positions). */
	public static TrackShape of(Collection<int[]> cells) {
		if (cells.size() < SECTORS / 2) {
			return failed("The track is too small: " + cells.size() + " blocks");
		}
		double sx = 0;
		double sz = 0;
		java.util.Set<Long> set = new java.util.HashSet<>();
		for (int[] c : cells) {
			sx += c[0] + 0.5;
			sz += c[1] + 0.5;
			set.add(key(c[0], c[1]));
		}
		double cx = sx / cells.size();
		double cz = sz / cells.size();
		if (set.contains(key((int) Math.floor(cx), (int) Math.floor(cz)))) {
			return failed("The track must be a loop around an infield: its middle is track");
		}
		double[] sumX = new double[SECTORS];
		double[] sumZ = new double[SECTORS];
		int[] n = new int[SECTORS];
		for (int[] c : cells) {
			double x = c[0] + 0.5;
			double z = c[1] + 0.5;
			int s = sector(Math.atan2(z - cz, x - cx));
			sumX[s] += x;
			sumZ[s] += z;
			n[s]++;
		}
		List<double[]> pts = new ArrayList<>();
		// counter-clockwise from above is decreasing atan2(z, x) (z runs south)
		for (int k = SECTORS - 1; k >= 0; k--) {
			if (n[k] == 0) {
				return failed("The track doesn't close: there is a gap in the loop");
			}
			pts.add(new double[] {sumX[k] / n[k], sumZ[k] / n[k]});
		}
		double[] px = new double[pts.size()];
		double[] pz = new double[pts.size()];
		for (int i = 0; i < pts.size(); i++) {
			px[i] = pts.get(i)[0];
			pz[i] = pts.get(i)[1];
		}
		return new TrackShape(cx, cz, px, pz, "");
	}

	private static TrackShape failed(String why) {
		return new TrackShape(0, 0, new double[0], new double[0], why);
	}

	private static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}

	private static int sector(double angle) {
		int s = (int) Math.floor((angle + Math.PI) / (2 * Math.PI) * SECTORS);
		return Math.floorMod(s, SECTORS);
	}

	public boolean formed() {
		return problem.isEmpty();
	}

	public String problem() {
		return problem;
	}

	/** The lap length along the centre line, metres. */
	public double lap() {
		return lap;
	}

	/** Wraps an arc position into [0, lap). */
	public double wrap(double s) {
		return lap <= 0 ? 0 : ((s % lap) + lap) % lap;
	}

	/** The signed shortest way from arc position {@code from} to {@code to}, in (-lap/2, lap/2]. */
	public double delta(double from, double to) {
		double d = wrap(to) - wrap(from);
		if (d > lap / 2) {
			d -= lap;
		} else if (d <= -lap / 2) {
			d += lap;
		}
		return d;
	}

	private int segment(double s) {
		double w = wrap(s);
		int lo = 0;
		int hi = px.length - 1;
		while (lo < hi) {
			int mid = (lo + hi + 1) >>> 1;
			if (at[mid] <= w) {
				lo = mid;
			} else {
				hi = mid - 1;
			}
		}
		return lo;
	}

	/** The point {@code offset} metres out from the centre line (outward, away from the infield) at arc position {@code s}: {x, z}. */
	public double[] pointAt(double s, double offset) {
		int i = segment(s);
		int j = (i + 1) % px.length;
		double len = at[i + 1] - at[i];
		double f = len <= 0 ? 0 : (wrap(s) - at[i]) / len;
		double x = px[i] + f * (px[j] - px[i]);
		double z = pz[i] + f * (pz[j] - pz[i]);
		double[] n = normal(i);
		return new double[] {x + n[0] * offset, z + n[1] * offset};
	}

	/** The outward unit normal of segment {@code i}. */
	private double[] normal(int i) {
		int j = (i + 1) % px.length;
		double tx = px[j] - px[i];
		double tz = pz[j] - pz[i];
		double len = Math.hypot(tx, tz);
		if (len <= 0) {
			return new double[] {0, 0};
		}
		double nx = -tz / len;
		double nz = tx / len;
		double mx = (px[i] + px[j]) / 2 - cx;
		double mz = (pz[i] + pz[j]) / 2 - cz;
		return nx * mx + nz * mz >= 0 ? new double[] {nx, nz} : new double[] {-nx, -nz};
	}

	/** The arc position of the centre-line point nearest {x, z}. */
	public double project(double x, double z) {
		double best = Double.MAX_VALUE;
		double bestS = 0;
		for (int i = 0; i < px.length; i++) {
			int j = (i + 1) % px.length;
			double tx = px[j] - px[i];
			double tz = pz[j] - pz[i];
			double len2 = tx * tx + tz * tz;
			double f = len2 <= 0 ? 0 : Math.max(0, Math.min(1, ((x - px[i]) * tx + (z - pz[i]) * tz) / len2));
			double qx = px[i] + f * tx;
			double qz = pz[i] + f * tz;
			double d = (x - qx) * (x - qx) + (z - qz) * (z - qz);
			if (d < best) {
				best = d;
				bestS = at[i] + f * Math.sqrt(len2);
			}
		}
		return bestS;
	}

	/** How far {x, z} lies out from the centre line at its nearest point (negative toward the infield). */
	public double offset(double x, double z) {
		double s = project(x, z);
		double[] c = pointAt(s, 0);
		double[] n = normal(segment(s));
		return (x - c[0]) * n[0] + (z - c[1]) * n[1];
	}
}
