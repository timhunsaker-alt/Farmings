package dev.farmings.farm.digester;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DigesterTest {
	@Test
	void aTonneGives23CubicMetresOverTwentyDays() {
		Digester d = new Digester();
		d.feed(1000);
		d.step(10, true);
		assertEquals(23 * 0.5 * 0.5, d.gasM3(), 1e-9);     // half the gas so far, half of that to the holder
		assertEquals(0, d.digestateKg(), 1e-9);
		d.step(10, true);
		assertEquals(23 * 0.5, d.gasM3(), 1e-9);
		assertEquals(1000 * (1 - 0.023 * 1.2), d.digestateKg(), 1e-9);
		assertEquals(0, d.inTankKg(), 1e-9);
	}

	@Test
	void noPowerNoDigestion() {
		Digester d = new Digester();
		d.feed(1000);
		d.step(30, false);
		assertEquals(0, d.gasM3(), 1e-9);
		assertEquals(1000, d.inTankKg(), 1e-9);
	}

	@Test
	void savesItsBatches() {
		Digester d = new Digester();
		d.feed(500);
		d.step(5, true);
		Digester e = new Digester();
		e.load(d.save());
		e.step(15, true);
		assertEquals(500 * (1 - 0.023 * 1.2), e.digestateKg(), 1e-9);
	}
}
