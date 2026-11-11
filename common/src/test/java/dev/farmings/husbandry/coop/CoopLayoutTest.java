package dev.farmings.husbandry.coop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.farmings.genetics.Species;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoopLayoutTest {
	/** '.' floor, 'H' pop hole, 'C' controller, 'N' nest, 'P' perch (beside), 'F' feeder, 'W' trough; anything else is OTHER. y = 0. */
	static CoopLayout.Grid grid(String... rows) {
		Map<Long, Character> m = new HashMap<>();
		for (int z = 0; z < rows.length; z++) {
			for (int x = 0; x < rows[z].length(); x++) {
				m.put(((long) x << 32) ^ z, rows[z].charAt(x));
			}
		}
		return (x, y, z) -> {
			if (y != 0) {
				return CoopLayout.Cell.OTHER;
			}
			return switch (m.getOrDefault(((long) x << 32) ^ (z & 0xFFFFFFFFL), '#')) {
				case '.' -> CoopLayout.Cell.FLOOR;
				case 'H' -> CoopLayout.Cell.POP_HOLE;
				case 'C' -> CoopLayout.Cell.CONTROLLER;
				case 'N' -> CoopLayout.Cell.NEST;
				case 'P' -> CoopLayout.Cell.PERCH;
				case 'F' -> CoopLayout.Cell.FEEDER;
				case 'W' -> CoopLayout.Cell.TROUGH;
				default -> CoopLayout.Cell.OTHER;
			};
		};
	}

	static final String[] GOOD = {
			"#NNP#",
			"C...H",
			"#F.W#"};

	@Test
	void formsWithEveryPart() {
		CoopLayout.Check c = CoopLayout.check(grid(GOOD), 0, 0, 1);
		assertTrue(c.formed(), c.problem());
		assertEquals(4, c.floor().size());
		assertEquals(1, c.popHoles().size());
		assertEquals(2, c.nests().size());
		assertEquals(1, c.perches().size());
		assertEquals(1, c.feeders().size());
		assertEquals(1, c.troughs().size());
	}

	@Test
	void namesWhatIsMissing() {
		assertTrue(CoopLayout.check(grid("#..P#", "C...H", "#F.W#"), 0, 0, 1).problem().startsWith("No nest box"));
		assertTrue(CoopLayout.check(grid("#NNP#", "C...H", "#..W#"), 0, 0, 1).problem().startsWith("No grain feeder"));
		assertTrue(CoopLayout.check(grid("#NNP#", "C...H", "#F..#"), 0, 0, 1).problem().startsWith("No water trough"));
		assertTrue(CoopLayout.check(grid("#NNP#", "C#..H", "#F.W#"), 0, 0, 1).problem().startsWith("No coop floor"));
	}

	@Test
	void capacityFollowsTheHousingTable() {
		// 1 floor block holds 3 chickens, 1 turkey, 16 quail or 2 ducks
		assertTrue(CoopLayout.room(Map.of(Species.CHICKEN, 2), Species.CHICKEN, 1));
		assertFalse(CoopLayout.room(Map.of(Species.CHICKEN, 3), Species.CHICKEN, 1));
		assertFalse(CoopLayout.room(Map.of(Species.CHICKEN, 3), Species.TURKEY, 1));
		assertTrue(CoopLayout.room(Map.of(Species.QUAIL, 15), Species.QUAIL, 1));
		assertEquals(2.0, CoopLayout.floorNeeded(Map.of(Species.DUCK, 2, Species.TURKEY, 1)), 1e-9);
		assertEquals(3, CoopLayout.nestsWanted(Map.of(Species.CHICKEN, 9)));
		assertEquals(1, CoopLayout.nestsWanted(Map.of(Species.QUAIL, 8)));
		assertEquals(4, CoopLayout.perchesWanted(Map.of(Species.CHICKEN, 6, Species.TURKEY, 2, Species.DUCK, 5)));
		assertEquals(2, CoopLayout.pondWanted(5));
		assertEquals(4.0, CoopLayout.runNeeded(Map.of(Species.CHICKEN, 3, Species.QUAIL, 8)), 1e-9);
	}
}
