package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.farmings.genetics.Species;
import org.junit.jupiter.api.Test;

class TimeScaleTest {
	@Test
	void cattleAndSheepStayAtFifteen() {
		assertSame(TimeScale.LIVESTOCK, TimeScale.of(Species.CATTLE));
		assertSame(TimeScale.LIVESTOCK, TimeScale.of(Species.SHEEP));
		assertEquals(9.8, TimeScale.of(Species.SHEEP).realDaysToGameDays(147), 1e-9);
		assertEquals(TimeScale.realDaysToTicks(283), TimeScale.LIVESTOCK.realDaysToTicks(283));
	}

	@Test
	void fowlRunAtThree() {
		// poultry.md: a chicken's 21 days of incubation is 7 game days; the corral's 7 settling days about 2.3
		assertEquals(7.0, TimeScale.FOWL.realDaysToGameDays(21), 1e-9);
		assertEquals(56_000, TimeScale.FOWL.realDaysToTicks(7));
		assertEquals(3.0, TimeScale.FOWL.ticksToRealDays(TimeScale.TICKS_PER_GAME_DAY), 1e-9);
	}
}
