package dev.farmings.process.creamery;

import static org.junit.jupiter.api.Assertions.*;

import dev.farmings.husbandry.barn.Orientation;
import org.junit.jupiter.api.Test;

class CreameryTest {
	private static void run(Creamery c, long ticks, Creamery.Power p) {
		for (long t = 0; t < ticks; t += 20) {
			c.step(20, p);
		}
	}

	@Test
	void bottleModePasteurizesInBatches() {
		Creamery c = new Creamery();
		c.addMilk(250, 3.72, 3.06);
		run(c, Creamery.PHASE_TICKS * 3 + 40, Creamery.Power.ALL);
		assertEquals(200, c.bottled(Creamery.Bottle.MILK), 1e-9);
		assertEquals(0, c.rawL(), 1e-9);   // the last 50 L is already in the next batch
		assertNotEquals(Creamery.Phase.IDLE, c.phase());
		assertEquals(Creamery.Bottle.MILK, c.takeBottle());
		assertEquals(199, c.bottled(Creamery.Bottle.MILK), 1e-9);
	}

	@Test
	void noHeaterNoBatch() {
		Creamery c = new Creamery();
		c.addMilk(100, 3.72, 3.06);
		run(c, 2000, new Creamery.Power(false, true, true, true));
		assertEquals(0, c.bottled(Creamery.Bottle.MILK));
		assertTrue(c.fault().startsWith("NO POWER"));
	}

	@Test
	void butterAndCheese() {
		Creamery c = new Creamery();
		c.setMode(Creamery.Mode.BUTTER);
		c.addMilk(200, 4.76, 3.63);
		run(c, Creamery.PHASE_TICKS * 3 + 40, Creamery.Power.ALL);
		assertTrue(c.butterKg() > 10 && c.butterKg() < 13, "about 11.6 kg of butter from 200 L Jersey milk, got " + c.butterKg());
		assertEquals(23, c.takeButter(64));
		Creamery k = new Creamery();
		k.setMode(Creamery.Mode.CHEESE);
		k.addMilk(200, 3.72, 3.06);
		run(k, Creamery.PHASE_TICKS * 3 + 40, Creamery.Power.ALL);
		run(k, Creamery.PRESS_TICKS + Creamery.AGE_TICKS + 40, Creamery.Power.ALL);
		assertEquals(2, k.agedWheels(), "about 20.5 kg of curd makes two 10 kg wheels");
		assertEquals(2, k.takeWheels(5));
	}

	@Test
	void saveAndLoad() {
		Creamery c = new Creamery();
		c.setMode(Creamery.Mode.CHEESE);
		c.addMilk(300, 3.9, 3.1);
		run(c, 400, Creamery.Power.ALL);
		Creamery d = new Creamery();
		d.load(c.save());
		assertArrayEquals(c.save(), d.save());
	}

	@Test
	void layoutAnyOrderNoGaps() {
		java.util.Map<Integer, CreameryPart> row = new java.util.HashMap<>();
		row.put(0, CreameryPart.CONTROLLER);
		int x = 1;
		for (CreameryPart p : new CreameryPart[] {CreameryPart.BOTTLER, CreameryPart.RECEIVING_TANK, CreameryPart.BATCH_PASTEURIZER,
				CreameryPart.CREAM_SEPARATOR, CreameryPart.BUTTER_CHURN, CreameryPart.CHEESE_VAT, CreameryPart.CHEESE_PRESS, CreameryPart.AGING_SHELF}) {
			for (int i = 0; i < p.length(); i++) {
				row.put(x++, p);
			}
		}
		CreameryGrid g = (gx, y, gz) -> y == 5 && gz == 0 ? row.get(gx) : null;
		assertTrue(CreameryLayout.check(g, 5, Orientation.ALL[0]).formed());
		row.remove(4);   // a gap in the pasteurizer's place
		CreameryCheck bad = CreameryLayout.check(g, 5, Orientation.ALL[0]);
		assertFalse(bad.formed());
		assertTrue(bad.status().contains("no"), bad.status());
	}
}
