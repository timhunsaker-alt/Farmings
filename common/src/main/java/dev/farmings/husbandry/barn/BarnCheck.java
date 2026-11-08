package dev.farmings.husbandry.barn;

import java.util.List;

/**
 * The result of looking at a barn from its controller.
 *
 * @param formed         every rule in the spec holds
 * @param stallsPerSide  stalls along each row (0 when the length could not be read)
 * @param problems       what is missing or wrong, most important first; empty when formed
 * @param orientation    how the barn lies in the world (which way the pit end points, and whether it is mirrored)
 * @param driveX         where the scraper drive stands along the barn, in drawn cells from the controller (negative: away from the pit);
 *                       {@link #NO_DRIVE} when none was found
 */
public record BarnCheck(boolean formed, int stallsPerSide, List<String> problems, Orientation orientation, int driveX) {
	public static final int NO_DRIVE = Integer.MIN_VALUE;

	/** The first problem, as the controller shows it, or "FORMED". */
	public String status() {
		return formed ? "FORMED" : problems.get(0);
	}

	public int stalls() {
		return stallsPerSide * 2;
	}
}
