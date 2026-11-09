package dev.farmings.process.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.process.food.Spoilage.Food;
import org.junit.jupiter.api.Test;

class SpoilageTest {
	@Test
	void milkKeepsADayOutOfTheCold() {
		long made = 10_000;
		long at = Spoilage.spoilsAt(Food.MILK, made);
		assertEquals(0, at % Spoilage.STEP, "rounded to a step so stacks stay alike");
		assertTrue(at - made >= 24_000 && at - made < 24_000 + Spoilage.STEP);
		assertFalse(Spoilage.spoiled(at, at - 1));
		assertTrue(Spoilage.spoiled(at, at));
	}

	@Test
	void aFridgeStretchesMilkToSevenDays() {
		// kept cold the whole time, milk lasts its fridge life: 7 days
		long now = 0;
		long at = Spoilage.spoilsAt(Food.MILK, now);
		while (!Spoilage.spoiled(at, now)) {
			now += Spoilage.STEP;
			at += Spoilage.coldCredit(Food.MILK, Spoilage.STEP);
		}
		assertEquals(7 * 24_000, now, 2 * Spoilage.STEP);
	}

	@Test
	void butterAndCheddarFollowFoodKeeper() {
		assertEquals(45 * 24_000, Food.BUTTER.fridgeTicks());
		assertEquals(36_000, Food.BUTTER.roomTicks(), "1-2 days on the counter");
		assertEquals(180 * 24_000, Food.CHEDDAR_WHEEL.fridgeTicks(), "hard cheese, 6 months");
		assertEquals(96_000, Food.RAW_STEAK.fridgeTicks(), "3-5 days");
	}

	@Test
	void foodWithAShorterFridgeLifeGetsNoCredit() {
		for (Food f : Food.values()) {
			assertTrue(Spoilage.coldCredit(f, 1200) >= 0);
		}
		assertEquals(0.5, Spoilage.left(Food.MILK, 12_000, 0), 1e-9);
	}
}
