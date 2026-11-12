package dev.farmings.farm.conveyor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TravelTest {
	@Test
	void parcelsRideToTheHeadInOrder() {
		Travel<String> t = new Travel<>();
		t.add("far", 20);
		t.add("near", 5);
		assertNull(t.peekReady());
		t.advance(5);
		assertEquals("near", t.takeReady());
		assertNull(t.takeReady());
		t.advance(14.9);
		assertNull(t.peekReady());
		t.advance(0.1);
		assertEquals("far", t.takeReady());
	}

	@Test
	void aBlockedHeadHoldsTheLoad() {
		Travel<String> t = new Travel<>();
		t.add("a", 0);
		String a = t.takeReady();
		t.putBackReady(a);
		t.advance(10);
		assertEquals("a", t.peekReady());
		assertEquals(1, t.size());
	}

	@Test
	void powerAndTransit() {
		assertEquals(10.0, Travel.transitSeconds(20, 2.0), 1e-9);     // a 20 m belt at 2 m/s
		assertEquals(7500, Travel.beltWatts(30), 1e-9);
		assertEquals(15000, Travel.beltWatts(31), 1e-9);
		assertEquals(3700, Travel.augerWatts(9), 1e-9);     // one 3.7 kW drive per 9 m (AGI Westfield)
		assertEquals(7400, Travel.augerWatts(10), 1e-9);
	}
}
