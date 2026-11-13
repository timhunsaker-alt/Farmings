package dev.farmings.husbandry.shed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PenRunsTest {
	@Test
	void cellsSideBySideAreOneRunAndAGapStartsAnother() {
		List<ShedLayout.Pos> cells = List.of(new ShedLayout.Pos(0, 0, 0), new ShedLayout.Pos(1, 0, 0), new ShedLayout.Pos(3, 0, 0),
				new ShedLayout.Pos(3, 1, 1), new ShedLayout.Pos(1, 0, 1), new ShedLayout.Pos(5, 3, 0));
		assertArrayEquals(new int[] {0, 0, 1, 1, 0, 2}, PenRuns.of(cells));
	}
}
