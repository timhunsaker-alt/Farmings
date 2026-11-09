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
	void cheeseLeavesWhey() {
		Creamery k = new Creamery();
		k.setMode(Creamery.Mode.CHEESE);
		k.addMilk(200, 3.72, 3.06);
		run(k, Creamery.PHASE_TICKS * 3 + 40, Creamery.Power.ALL);
		double cheese = Dairy.cheeseKg(200, 3.72, 3.06);
		double whey = k.bottled(Creamery.Bottle.WHEY);
		assertEquals((200 * 1.03 - cheese) / 1.03, whey, 1e-6, "milk less cheese, by mass");
		assertTrue(whey > 175 && whey < 185, "about 180 L of whey from 200 L, got " + whey);
		assertEquals(9.0, (200 * 1.03 - cheese) / cheese, 0.6, "about 9 kg of whey per kg of cheddar");
		assertEquals(Creamery.Bottle.WHEY, k.nextBottle());
		assertEquals(Creamery.Bottle.WHEY, k.takeBottle());
		k.returnBottle(Creamery.Bottle.WHEY);
		assertEquals(whey, k.bottled(Creamery.Bottle.WHEY), 1e-9);
	}

	@Test
	void anOlderSaveWithoutWheyStillLoads() {
		Creamery c = new Creamery();
		c.setMode(Creamery.Mode.BOTTLE);
		c.addMilk(300, 3.9, 3.1);
		run(c, Creamery.PHASE_TICKS * 3 + 40, Creamery.Power.ALL);
		double[] v2 = c.save();
		// the layout before whey: no marker, and no whey after buttermilk
		double[] v1 = new double[v2.length - 2];
		System.arraycopy(v2, 1, v1, 0, 13);
		System.arraycopy(v2, 15, v1, 13, v2.length - 15);
		Creamery d = new Creamery();
		d.load(v1);
		assertArrayEquals(c.save(), d.save());
		assertEquals(c.bottled(Creamery.Bottle.MILK), d.bottled(Creamery.Bottle.MILK), 1e-9);
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
