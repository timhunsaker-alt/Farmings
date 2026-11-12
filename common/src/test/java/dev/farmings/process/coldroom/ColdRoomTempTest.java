package dev.farmings.process.coldroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.process.food.Spoilage;
import org.junit.jupiter.api.Test;

class ColdRoomTempTest {
	@Test
	void shutRoomWithNoPowerWarmsTwoToFiveInFourHours() {
		double t = ColdRoomTemp.step(2.0, 4 * 3600, false, 0, 3, 2, 3);
		assertEquals(5.0, t, 1e-9);
		// and the same for a big room: the mass scales with the walls
		assertEquals(5.0, ColdRoomTemp.step(2.0, 4 * 3600, false, 0, 10, 4, 10), 1e-9);
	}

	@Test
	void poweredRoomPullsDownAndHoldsTheSetPoint() {
		double cooling = ColdRoomLoad.coolingWatts(18);
		double t = 20;
		int steps = 0;
		while (t > ColdRoomTemp.SET_POINT_C + 1e-9 && steps < 100) {
			t = ColdRoomTemp.step(t, 4320, true, cooling, 3, 2, 3);
			steps++;
		}
		assertEquals(ColdRoomTemp.SET_POINT_C, t, 1e-9);
		assertTrue(steps <= 5, "a small room is cold within a few steps: " + steps);
		assertEquals(ColdRoomTemp.SET_POINT_C, ColdRoomTemp.step(t, 4320, true, cooling, 3, 2, 3), 1e-9);
	}

	@Test
	void openingTheDoorLetsWarmthIn() {
		double t = ColdRoomTemp.doorOpened(2.0, 3, 2, 3);
		assertTrue(t > 2.0 && t < 3.0, "one air change warms the room about half a degree: " + t);
		assertEquals(20.0, ColdRoomTemp.doorOpened(20.0, 3, 2, 3), 1e-9);
	}

	@Test
	void spoilageFollowsTemperature() {
		Spoilage.Food milk = Spoilage.Food.MILK;
		assertEquals(milk.fridgeTicks(), Spoilage.lifeAt(milk, 2.0), 1e-6);
		assertEquals(milk.roomTicks(), Spoilage.lifeAt(milk, 25.0), 1e-6);
		double mid = Spoilage.lifeAt(milk, 12.0);
		assertEquals(Math.sqrt((double) milk.fridgeTicks() * milk.roomTicks()), mid, 1e-3, "half way is the geometric mean");
		assertEquals(Spoilage.coldCredit(milk, 1200), Spoilage.coldCredit(milk, 1200, 3.0));
		assertEquals(0, Spoilage.coldCredit(milk, 1200, 20.0));
		assertTrue(Spoilage.coldCredit(milk, 1200, 10.0) > 0 && Spoilage.coldCredit(milk, 1200, 10.0) < Spoilage.coldCredit(milk, 1200));
	}
}
