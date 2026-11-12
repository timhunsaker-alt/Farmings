package dev.farmings.farm.silo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.husbandry.TimeScale;
import org.junit.jupiter.api.Test;

class SiloLayoutTest {
	/** A 3 x 3 ring around (0, 0) of {@code h} layers from y 0, with a roof at y h if {@code roof}. */
	static SiloLayout.Grid tower(int half, int h, boolean roof) {
		return (x, y, z) -> {
			if (y == h && x == 0 && z == 0) {
				return roof ? SiloLayout.Cell.ROOF : SiloLayout.Cell.EMPTY;
			}
			if (y < 0 || y >= h || Math.abs(x) > half || Math.abs(z) > half) {
				return SiloLayout.Cell.EMPTY;
			}
			return Math.abs(x) == half || Math.abs(z) == half ? SiloLayout.Cell.WALL : SiloLayout.Cell.EMPTY;
		};
	}

	@Test
	void findsATowerFromAWallCell() {
		SiloLayout.Tower t = SiloLayout.find(tower(1, 20, true), 1, 0, 0, 24);
		assertTrue(t.formed(), t.problem());
		assertEquals(20, t.height());
		assertEquals(20, t.volume());
		SiloLayout.Tower big = SiloLayout.find(tower(2, 10, true), -2, 0, 1, 24);
		assertTrue(big.formed(), big.problem());
		assertEquals(90, big.volume());
		assertTrue(SiloLayout.find(tower(1, 5, false), 1, 0, 1, 24).problem().startsWith("NO ROOF"));
	}

	@Test
	void silageFigures() {
		// a 3 x 3 x 20 silo: 20 m3 at 240 kg DM = 4.8 t DM (silo.md's example)
		assertEquals(4800, Silage.capacityKgDm(20), 1e-9);
		assertEquals(975, Silage.kept(1000), 1e-9);     // filling and effluent at fill; fermenting and storage come later
		assertEquals(0.025, Silage.lossBetween(0, Silage.FERMENT_DAYS), 1e-12);
		assertEquals(0.0125, Silage.lossBetween(0, Silage.FERMENT_DAYS / 2), 1e-12);
		assertEquals(0.043, Silage.lossBetween(Silage.FERMENT_DAYS, Silage.FERMENT_DAYS + 730), 0.001);
		assertEquals(0, Silage.lossBetween(5, 5), 0);
		assertEquals(1.4, TimeScale.realDaysToGameDays(Silage.FERMENT_DAYS), 1e-9);
		assertEquals(721, Silage.CORN_DENSITY, 1);
		assertEquals(412, Silage.OAT_DENSITY, 1);
		assertEquals(618, Silage.BARLEY_DENSITY, 1);
		assertEquals(165, Silage.unloadTicks());       // 25 kg at 3.02 kg/s: 8.3 s
	}
}
