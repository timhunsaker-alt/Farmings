package dev.farmings.husbandry.parlor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BulkTankTest {
	@Test
	void freshMilkMixesByVolume() {
		BulkTank t = new BulkTank();
		t.add(100, 4);
		t.add(100, 34);
		assertEquals(19, t.tempC(), 1e-9);
		assertEquals(200, t.litres(), 1e-9);
	}

	@Test
	void chillerCoolsAtTheScaledRateAndStopsAtFourC() {
		BulkTank t = new BulkTank();
		t.add(50, 35);
		t.cool(14_400, true);
		assertEquals(4, t.tempC(), 1e-9);
		t.add(50, 35);
		t.cool(1000, false);
		assertEquals(19.5, t.tempC(), 1e-9);
		t.cool(100_000, true);
		assertEquals(4, t.tempC(), 1e-9);
	}

	@Test
	void fullAndWarmAndBucket() {
		BulkTank t = new BulkTank();
		assertEquals(1000, t.add(2000, 4), 1e-9);
		assertEquals(0, t.add(5, 4), 1e-9);
		assertFalse(t.warm());
		assertEquals(10, t.take(BulkTank.BUCKET_L), 1e-9);
		t.add(10, 700);
		assertTrue(t.warm());
	}

	@Test
	void warmMilkOnlyBlocksANewSession() {
		BulkTank t = new BulkTank();
		t.add(40, 35);
		assertTrue(t.warm());
		assertFalse(t.blocksMilking());              // the session is under way
		t.cool(5_999, false);
		assertFalse(t.blocksMilking());
		t.add(40, 35);                               // another cow: the session continues
		t.cool(5_999, false);
		assertFalse(t.blocksMilking());
		t.cool(1, false);
		assertTrue(t.blocksMilking());               // no milk for a whole session and still warm
		t.cool(14_400, true);
		assertFalse(t.blocksMilking());
	}
}
