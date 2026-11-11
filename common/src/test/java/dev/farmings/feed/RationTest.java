package dev.farmings.feed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RationTest {
	@Test
	void aBalancedTmrMeetsTheDairyTargets() {
		// 50% corn silage, 20% alfalfa hay, 28.6% corn grain, 1.4% premix, by DM
		FeedMix m = new FeedMix();
		m.add(50, Feedstuff.CORN_SILAGE.analysis());
		m.add(20, Feedstuff.ALFALFA_HAY.analysis());
		m.add(28.6, Feedstuff.CORN_GRAIN.analysis());
		m.add(1.4, Feedstuff.PREMIX.analysis());
		Ration.Analysis a = m.analysis();
		assertTrue(a.forage() >= 0.4 && a.premix() >= Ration.PREMIX_SHARE, a.toString());
		assertTrue(Ration.milkFactor(a) > 0.85, "a corn silage TMR scores well: " + Ration.milkFactor(a) + " " + a);
	}

	@Test
	void forageAloneScoresLow() {
		Ration.Analysis grass = Feedstuff.GRASS_HAY.analysis();
		// CP 10.5 (0.36 of the way to 15), NDF 65 (over the 60 zero), all forage: (0.36 + 0 + 1) / 3, no premix x0.9
		assertEquals((2.5 / 7 + 0 + 1) / 3 * 0.9, Ration.dairyScore(grass), 1e-9);
		assertEquals(0.67, Ration.milkFactor(new Ration.Analysis(5, 70, 40, 0, 0, 0)), 1e-9);
		assertEquals(1.0, Ration.milkFactor(Ration.Analysis.BALANCED), 1e-9);
	}

	@Test
	void hensNeedProteinAndCalcium() {
		assertEquals(0.75 + 0.25 * ((3.0 / 7) / 2), Ration.eggFactor(Feedstuff.MIXED_GRAIN.analysis()), 1e-9);
		FeedMix layer = new FeedMix();
		layer.add(72, Feedstuff.CORN_GRAIN.analysis());
		layer.add(20, Feedstuff.FISH_MEAL.analysis());
		layer.add(8, Feedstuff.LIMESTONE.analysis());
		assertEquals(1.0, Ration.eggFactor(layer.analysis()), 1e-9);
	}

	@Test
	void runningAverageMovesTowardWhatIsEaten() {
		Ration.Analysis a = Ration.Analysis.BALANCED.toward(Feedstuff.STRAW.analysis(), 0.5);
		assertEquals((16.5 + 4) / 2, a.cp(), 1e-9);
	}
}
