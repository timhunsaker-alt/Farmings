package dev.farmings.husbandry.corral;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CorralLayoutTest {
	/** A flat map: '.' floor, '#' fence, 'G' gate, 'B' bait trough, 'W' water trough, 'P' the post; outside the map is open floor. */
	static CorralLayout.Ground ground(String... rows) {
		Map<Long, Character> m = new HashMap<>();
		for (int z = 0; z < rows.length; z++) {
			for (int x = 0; x < rows[z].length(); x++) {
				m.put(((long) x << 32) ^ z, rows[z].charAt(x));
			}
		}
		return new CorralLayout.Ground() {
			char c(int x, int z) {
				return m.getOrDefault(((long) x << 32) ^ (z & 0xFFFFFFFFL), '.');
			}

			@Override
			public CorralLayout.Pos at(int x, int z, int nearY) {
				return c(x, z) == '.' ? new CorralLayout.Pos(x, 64, z) : null;
			}

			@Override
			public CorralLayout.Part part(int x, int y, int z) {
				return switch (c(x, z)) {
					case 'G' -> CorralLayout.Part.GATE;
					case 'B' -> CorralLayout.Part.BAIT;
					case 'W' -> CorralLayout.Part.TROUGH;
					default -> CorralLayout.Part.NONE;
				};
			}
		};
	}

	static final String[] GOOD = {
			"#######",
			"#P....#",
			"#..B..G",
			"#....W#",
			"#######"};

	static CorralLayout.Corral check(String... rows) {
		return CorralLayout.check(ground(rows), 1, 64, 1);
	}

	@Test
	void formsWithGateBaitAndWater() {
		CorralLayout.Corral c = check(GOOD);
		assertTrue(c.formed(), c.problem());
		assertEquals(12, c.floor().size());
		assertEquals(1, c.gates().size());
		assertEquals(1, c.baits().size());
		assertEquals(1, c.troughs().size());
	}

	@Test
	void aGapLeaks() {
		CorralLayout.Corral c = check("#######", "#P.....", "#..B..G", "#....W#", "#######");
		assertTrue(c.problem().startsWith("NOT CLOSED IN"), c.problem());
	}

	@Test
	void eachMissingPartIsNamed() {
		assertTrue(check("#######", "#P....#", "#..B..#", "#....W#", "#######").problem().startsWith("NO GATE"));
		assertTrue(check("#######", "#P....#", "#.....G", "#....W#", "#######").problem().startsWith("NO BAIT"));
		assertTrue(check("#######", "#P....#", "#..B..G", "#.....#", "#######").problem().startsWith("NO WATER"));
	}

	@Test
	void aPostWithNoFloorIsNamed() {
		assertTrue(check("###", "#P#", "###").problem().startsWith("NO FLOOR"));
	}
}
