package dev.farmings.process.creamery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.husbandry.parlor.MilkKind;
import org.junit.jupiter.api.Test;

/** The chèvre mode (docs/multiblocks/creamery.md, "The chèvre mode"). */
class ChevreTest {
	private static void run(Creamery c, long ticks) {
		for (long t = 0; t < ticks; t += 20) {
			c.step(20, Creamery.Power.ALL);
		}
	}

	@Test
	void goatMilkSetsAndDrainsIntoChevreAndWhey() {
		Creamery c = new Creamery();
		c.setMode(Creamery.Mode.CHEVRE);
		assertEquals(200, c.addMilk(200, 3.2, 2.8, MilkKind.GOAT), 1e-9);
		run(c, Creamery.PHASE_TICKS * 3 + 40);
		assertTrue(c.chevreDraining(), "the batch is in the vat: " + c.summary());
		assertEquals(0, c.chevreKg(), 1e-9);
		run(c, Creamery.CHEVRE_TICKS + 40);
		double milkKg = 200 * Dairy.MILK_KG_PER_L;
		assertEquals(milkKg * 0.17, c.chevreKg(), 1e-6);
		assertEquals((milkKg - milkKg * 0.17) / Dairy.MILK_KG_PER_L, c.bottled(Creamery.Bottle.WHEY), 1e-6);
		assertEquals(140, c.takeChevre(1000), "35.02 kg makes 140 logs of 0.25 kg");
	}

	@Test
	void chevreNeedsGoatMilkAndGoatMilkMakesNoCheddar() {
		Creamery cow = new Creamery();
		cow.setMode(Creamery.Mode.CHEVRE);
		cow.addMilk(200, 3.72, 3.06);
		run(cow, Creamery.PHASE_TICKS * 3 + 40);
		assertEquals("CHEVRE NEEDS GOAT MILK", cow.fault());
		Creamery goat = new Creamery();
		goat.setMode(Creamery.Mode.CHEESE);
		goat.addMilk(200, 3.2, 2.8, MilkKind.GOAT);
		run(goat, Creamery.PHASE_TICKS * 3 + 40);
		assertEquals("GOAT MILK: USE CHEVRE", goat.fault());
	}

	@Test
	void theVatSurvivesSavingAndOldSavesStillLoad() {
		Creamery c = new Creamery();
		c.setMode(Creamery.Mode.CHEVRE);
		c.addMilk(200, 3.2, 2.8, MilkKind.GOAT);
		run(c, Creamery.PHASE_TICKS * 3 + 40 + Creamery.CHEVRE_TICKS / 2);
		Creamery d = new Creamery();
		d.load(c.save());
		assertTrue(d.chevreDraining());
		assertEquals(Creamery.Mode.CHEVRE, d.mode());
		run(d, Creamery.CHEVRE_TICKS);
		assertEquals(200 * Dairy.MILK_KG_PER_L * 0.17, d.chevreKg(), 1e-6);
	}
}
