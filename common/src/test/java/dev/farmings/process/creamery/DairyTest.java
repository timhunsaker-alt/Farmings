package dev.farmings.process.creamery;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DairyTest {
	@Test
	void separatorKeepsTheFat() {
		Dairy.Split s = Dairy.separate(100, 3.72);
		assertEquals(100, s.creamL() + s.skimL(), 1e-9);
		assertEquals(100 * 3.72, s.creamL() * 36 + s.skimL() * 0.1, 1e-9);
		assertEquals(10.08, s.creamL(), 0.01);
	}

	@Test
	void butterIsEightyPercentFat() {
		Dairy.Churned c = Dairy.churn(10, 36);
		assertEquals(10 * 0.36 * 0.989 / 0.80, c.butterKg(), 1e-9);
		assertEquals(10 - c.butterKg(), c.buttermilkL(), 1e-9);
	}

	@Test
	void vanSlykeGivesAboutTenKgPerHundred() {
		double y = Dairy.vanSlykePer100(3.72, 3.06);
		assertEquals((0.93 * 3.72 + 0.78 * 3.06 - 0.1) * 1.09 / 0.62, y, 1e-9);
		assertTrue(y > 9.5 && y < 10.5, "Holstein milk: about 10 kg per 100 kg, got " + y);
		assertTrue(Dairy.vanSlykePer100(4.76, 3.63) > y, "Jersey milk makes more cheese");
	}
}
