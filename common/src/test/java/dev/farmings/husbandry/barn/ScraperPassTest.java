package dev.farmings.husbandry.barn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.husbandry.barn.ScraperPass.Action;
import dev.farmings.husbandry.barn.ScraperPass.Fault;
import dev.farmings.husbandry.barn.ScraperPass.Look;
import dev.farmings.husbandry.barn.ScraperPass.Phase;
import org.junit.jupiter.api.Test;

class ScraperPassTest {
	/** A simulated alley: the blade at cell {@code at} of 0..last, the drive beside cell 0, the pit beyond cell {@code last}. */
	static final class Alley {
		final int last;
		int at;
		boolean powered = true;
		boolean pitRoom = true;
		boolean present = true;
		boolean blockedAhead;
		int blockedCell = -1;
		int dumps;

		Alley(int last) {
			this.last = last;
		}

		Look look() {
			boolean pitward = at < last && at + 1 != blockedCell;
			boolean driveward = at > 0 && at - 1 != blockedCell;
			return new Look(present, at == last, at == 0, powered, pitward, driveward, pitRoom);
		}

		Action step(ScraperPass pass) {
			Action a = pass.tick(look());
			switch (a) {
				case MOVE_TO_PIT -> at++;
				case MOVE_TO_DRIVE -> at--;
				case DUMP -> dumps++;
				default -> {
				}
			}
			return a;
		}

		int run(ScraperPass pass, int maxTicks) {
			for (int t = 0; t < maxTicks; t++) {
				step(pass);
				if (!pass.running()) {
					return t + 1;
				}
			}
			return maxTicks;
		}
	}

	@Test
	void aPassGoesToThePitDumpsAndComesBack() {
		Alley alley = new Alley(8);
		ScraperPass pass = new ScraperPass();
		assertTrue(pass.start(alley.look()));
		int ticks = alley.run(pass, 10_000);
		assertEquals(1, alley.dumps);
		assertEquals(0, alley.at, "back beside the drive");
		assertEquals(Phase.IDLE, pass.phase());
		// 8 cells there and 8 back, each a cell time, plus the tick that notices each end
		assertTrue(ticks >= 16 * ScraperPass.TICKS_PER_CELL && ticks <= 16 * (ScraperPass.TICKS_PER_CELL + 1) + 3, "ticks " + ticks);
	}

	@Test
	void idleDoesNothing() {
		Alley alley = new Alley(8);
		ScraperPass pass = new ScraperPass();
		for (int i = 0; i < 100; i++) {
			assertEquals(Action.NONE, alley.step(pass));
		}
		assertEquals(0, alley.at);
		assertEquals(Fault.NONE, pass.fault());
	}

	@Test
	void noPowerHoldsTheBladeAndSaysSo() {
		Alley alley = new Alley(8);
		ScraperPass pass = new ScraperPass();
		pass.start(alley.look());
		alley.run(pass, 3 * ScraperPass.TICKS_PER_CELL + 5);   // part way
		int where = alley.at;
		assertTrue(where > 0 && where < 8);
		alley.powered = false;
		for (int i = 0; i < 500; i++) {
			alley.step(pass);
		}
		assertEquals(where, alley.at, "it must not move without power");
		assertEquals(Fault.NO_POWER, pass.fault());
		alley.powered = true;
		alley.run(pass, 10_000);
		assertEquals(0, alley.at);
		assertEquals(1, alley.dumps);
	}

	@Test
	void aBlockOnTheRailStopsTheBladeUntilItIsCleared() {
		Alley alley = new Alley(8);
		alley.blockedCell = 4;
		ScraperPass pass = new ScraperPass();
		pass.start(alley.look());
		alley.run(pass, 5000);
		assertEquals(3, alley.at, "stops in front of the obstruction");
		assertEquals(Fault.BLOCKED, pass.fault());
		assertTrue(pass.running());
		alley.blockedCell = -1;
		alley.run(pass, 10_000);
		assertEquals(0, alley.at);
		assertEquals(1, alley.dumps);
		assertEquals(Fault.NONE, pass.fault());
	}

	@Test
	void aFullPitStopsTheStartOfAPass() {
		Alley alley = new Alley(8);
		alley.pitRoom = false;
		ScraperPass pass = new ScraperPass();
		assertFalse(pass.start(alley.look()));
		assertEquals(Fault.PIT_FULL, pass.fault());
		assertEquals(Phase.IDLE, pass.phase());
	}

	@Test
	void aPitThatFillsDuringThePassHoldsTheBladeAtTheEnd() {
		Alley alley = new Alley(6);
		ScraperPass pass = new ScraperPass();
		pass.start(alley.look());
		while (alley.at < 6) {
			alley.step(pass);
		}
		alley.pitRoom = false;      // the blade has just arrived and has not dumped yet
		alley.run(pass, 2000);
		assertEquals(6, alley.at);
		assertEquals(Fault.PIT_FULL, pass.fault());
		assertEquals(0, alley.dumps);
		alley.pitRoom = true;
		alley.run(pass, 10_000);
		assertEquals(1, alley.dumps);
		assertEquals(0, alley.at);
	}

	@Test
	void noBladeNoPass() {
		Alley alley = new Alley(8);
		alley.present = false;
		ScraperPass pass = new ScraperPass();
		assertFalse(pass.start(alley.look()));
		assertEquals(Fault.NO_SCRAPER, pass.fault());
		alley.present = true;
		alley.step(pass);
		assertEquals(Fault.NONE, pass.fault(), "the fault clears when the blade is back");
	}

	@Test
	void aPassSurvivesSaveAndLoad() {
		Alley alley = new Alley(8);
		ScraperPass pass = new ScraperPass();
		pass.start(alley.look());
		alley.run(pass, 3 * ScraperPass.TICKS_PER_CELL);
		ScraperPass loaded = new ScraperPass();
		loaded.load(pass.save());
		assertEquals(pass.phase(), loaded.phase());
		assertEquals(pass.fault(), loaded.fault());
		alley.run(loaded, 10_000);
		assertEquals(1, alley.dumps);
	}

	@Test
	void aSecondStartWhileRunningIsRefused() {
		Alley alley = new Alley(8);
		ScraperPass pass = new ScraperPass();
		assertTrue(pass.start(alley.look()));
		assertFalse(pass.start(alley.look()));
	}
}
