package dev.farmings.process.wool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.husbandry.Flock;
import org.junit.jupiter.api.Test;

class WoolProcessTest {
	@Test
	void aMerinoFleeceScoursToHalfItsWeightAndSomeLanolin() {
		WoolProcess.Scoured s = WoolProcess.scour(5.0, 0.50);
		assertEquals(2.5, s.cleanKg(), 1e-9);
		assertEquals(5.0 * 0.15 * 0.30, s.lanolinKg(), 1e-9);
		assertEquals(75, s.waterL(), 1e-9);
	}

	@Test
	void fineWoolSpinsMoreYarnThanCoarse() {
		double sliver = WoolProcess.card(2.5);
		assertEquals(2.375, sliver, 1e-9);
		assertTrue(WoolProcess.yarnItems(sliver, Flock.Grade.FINE) > WoolProcess.yarnItems(sliver, Flock.Grade.COARSE));
		assertEquals(23.75, WoolProcess.yarnItems(sliver, Flock.Grade.COARSE), 1e-9);
	}
}
