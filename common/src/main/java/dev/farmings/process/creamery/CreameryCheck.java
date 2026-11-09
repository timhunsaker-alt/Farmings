package dev.farmings.process.creamery;

import dev.farmings.husbandry.barn.Orientation;
import java.util.List;
import java.util.Map;

/** The result of checking a creamery: formed or not, the problems, its orientation, and the row offset of each part's first cell. */
public record CreameryCheck(boolean formed, List<String> problems, Orientation orientation, Map<CreameryPart, Integer> offsets) {
	public String status() {
		return formed ? "FORMED" : problems.isEmpty() ? "not formed" : problems.get(0);
	}
}
