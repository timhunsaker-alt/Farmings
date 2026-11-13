package dev.farmings.process.curing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The curing rules (docs/multiblocks/curing-room.md). */
class CuringTest {
	@Test
	void aHamCuresEqualizesSmokesAndAges() {
		Curing.Piece ham = new Curing.Piece(Curing.Kind.HAM, 6.6);
		double cure = 2 * 6.6 * 2.20462;     // 2 days a pound: 29.1 days
		ham.step(cure - 0.01, true, false);
		assertEquals(Curing.Phase.CURE, ham.phase);
		ham.step(0.02, true, false);
		assertEquals(Curing.Phase.EQUALIZE, ham.phase);
		ham.step(14, false, false);
		assertEquals(Curing.Phase.SMOKE, ham.phase, "equalizing needs no cold");
		ham.step(10, false, false);
		assertEquals(Curing.Phase.SMOKE, ham.phase, "smoking waits for the smokehouse");
		ham.step(2, false, true);
		assertEquals(Curing.Phase.AGE, ham.phase);
		ham.step(90, false, false);
		assertEquals(Curing.Phase.DONE, ham.phase);
		assertEquals(6.6 * 0.775, ham.outKg(), 1e-9);
	}

	@Test
	void baconSkipsEqualizingAndAging() {
		Curing.Piece b = new Curing.Piece(Curing.Kind.BACON, 2);
		b.step(7, true, false);
		assertEquals(Curing.Phase.SMOKE, b.phase);
		b.step(0.125, true, true);
		assertEquals(Curing.Phase.DONE, b.phase);
		assertEquals(3, b.baconItems(), "2 kg x 90% = 1.8 kg: 3 items of 0.5 kg");
	}

	@Test
	void freshMeatLeftWarmSpoils() {
		Curing.Piece ham = new Curing.Piece(Curing.Kind.HAM, 6);
		ham.step(0.9, false, false);
		assertEquals(Curing.Phase.CURE, ham.phase);
		ham.step(0.2, false, false);
		assertEquals(Curing.Phase.SPOILED, ham.phase);
		assertEquals(0.25, Curing.sausageFatShare(), 1e-9);
	}
}
