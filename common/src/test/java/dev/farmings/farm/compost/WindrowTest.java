package dev.farmings.farm.compost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WindrowTest {
	@Test
	void manureAloneIsTooWetAndShortOfCarbon() {
		Windrow w = new Windrow();
		w.add(500, 20, 0.87);
		assertEquals("TOO WET", w.problem());
		assertEquals(20, w.cn(), 1e-9);
	}

	@Test
	void manurePlusStrawStartsAndFinishesOnTime() {
		Windrow w = new Windrow();
		// manure as excreted is too wet, and enough straw to dry it gives too much carbon (C:N 44 at 200 kg); poultry litter fixes both
		w.add(500, 20, 0.87);      // 10 manure items
		w.add(200, 80, 0.10);      // straw
		w.add(100, 10.5, 0.30);    // 10 poultry litter items
		assertTrue(w.moisture() >= 0.40 && w.moisture() <= 0.65, "moisture " + w.moisture());
		assertTrue(w.cn() >= 25 && w.cn() <= 40, "C:N " + w.cn());
		assertTrue(w.start());
		assertFalse(w.add(10, 20, 0.5), "nothing goes on a started pile");
		w.age(100);
		assertEquals(Windrow.TURN_EVERY_DAYS, w.ageDays(), 1e-9);     // waits for its first turn
		for (int i = 0; i < 5; i++) {
			assertTrue(w.turnDue());
			w.turn();
			w.age(Windrow.TURN_EVERY_DAYS);
		}
		assertTrue(w.done());
		assertEquals(800 * 0.425, w.takeCompost(), 1e-9);
	}
}
