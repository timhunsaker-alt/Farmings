package dev.farmings.husbandry.racing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The track's shape against docs/multiblocks/race-track.md. */
class TrackShapeTest {
	/** A rectangular ring, outer w x h, {@code width} blocks wide. */
	static List<int[]> ring(int w, int h, int width) {
		List<int[]> cells = new ArrayList<>();
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < h; z++) {
				if (x < width || x >= w - width || z < width || z >= h - width) {
					cells.add(new int[] {x, z});
				}
			}
		}
		return cells;
	}

	/** An elliptical ring: semi-axes a and b at the centre line, {@code width} wide. */
	static List<int[]> oval(double a, double b, double width) {
		List<int[]> cells = new ArrayList<>();
		for (int x = (int) -(a + width); x <= a + width; x++) {
			for (int z = (int) -(b + width); z <= b + width; z++) {
				double cx = x + 0.5;
				double cz = z + 0.5;
				double r = Math.hypot(cx / a, cz / b);
				double along = Math.hypot(cx, cz);
				double rc = along / Math.max(1e-9, r);
				if (Math.abs(along - rc) <= width / 2) {
					cells.add(new int[] {x, z});
				}
			}
		}
		return cells;
	}

	@Test
	void anOvalsLapIsItsCentreLine() {
		double a = 40;
		double b = 20;
		TrackShape t = TrackShape.of(oval(a, b, 4));
		assertTrue(t.formed(), t.problem());
		// Ramanujan's ellipse perimeter
		double h = Math.pow(a - b, 2) / Math.pow(a + b, 2);
		double p = Math.PI * (a + b) * (1 + 3 * h / (10 + Math.sqrt(4 - 3 * h)));
		assertEquals(p, t.lap(), p * 0.03);
	}

	@Test
	void aSolidBlockOrAnOpenLineDoesntForm() {
		List<int[]> solid = new ArrayList<>();
		for (int x = 0; x < 20; x++) {
			for (int z = 0; z < 20; z++) {
				solid.add(new int[] {x, z});
			}
		}
		assertFalse(TrackShape.of(solid).formed());
		List<int[]> open = ring(30, 20, 3);
		open.removeIf(c -> c[0] >= 12 && c[0] <= 16 && c[1] < 3);
		assertFalse(TrackShape.of(open).formed());
	}

	@Test
	void projectAndPointAtAgree() {
		TrackShape t = TrackShape.of(ring(40, 24, 4));
		assertTrue(t.formed(), t.problem());
		for (double s = 0; s < t.lap(); s += 7.3) {
			double[] p = t.pointAt(s, 0);
			assertEquals(0, t.delta(s, t.project(p[0], p[1])), 0.6);
			double[] out = t.pointAt(s, 1.5);
			assertTrue(t.offset(out[0], out[1]) > 0.5, "outward is positive");
		}
		assertEquals(-2, t.delta(1, t.lap() - 1), 1e-9);
	}

	@Test
	void runsCounterClockwiseFromAbove() {
		TrackShape t = TrackShape.of(oval(30, 15, 4));
		// at the east end (x > 0) a counter-clockwise run seen from above heads north (z falling)
		double s = t.project(30, 0);
		double[] a = t.pointAt(s, 0);
		double[] b = t.pointAt(s + 2, 0);
		assertTrue(b[1] < a[1], "heading north at the east end");
	}
}
