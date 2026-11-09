package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CarcassTest {
	@Test
	void angusCarcassFromPennStateFigures() {
		Carcass.Yield y = Carcass.of(550, Map.of("ANG", 10_000));
		assertEquals(550 * 0.63, y.carcassKg(), 1e-9);
		assertEquals(3, y.steakItems());          // 34.65 kg of steak
		assertEquals(13, y.beefItems());          // 173.25 - 34.65 = 138.6 kg
		assertEquals(Carcass.Grade.CHOICE, y.grade());
	}

	@Test
	void wagyuDropMoreSteaksThanAngus() {
		Carcass.Yield w = Carcass.of(487, Map.of("WAG", 10_000));
		Carcass.Yield a = Carcass.of(550, Map.of("ANG", 10_000));
		assertTrue(w.steakItems() > a.steakItems(), w + " vs " + a);
		assertEquals(Carcass.Grade.PRIME, w.grade());
	}

	@Test
	void dairyDressesLighter() {
		assertEquals(0.58, Carcass.dressing(Map.of("HOL", 10_000)), 1e-9);
		assertEquals((0.58 + 0.63) / 2, Carcass.dressing(Map.of("HOL", 5_000, "ANG", 5_000)), 1e-9);
	}

	@Test
	void crossMarblingIsTheAverage() {
		assertEquals(684, Carcass.marbling(Map.of("ANG", 5_000, "WAG", 5_000)));
		assertEquals(Carcass.Grade.SELECT, Carcass.grade(399));
	}
}
