package dev.farmings.husbandry.parlor;

import dev.farmings.husbandry.barn.Orientation;
import java.util.List;

/**
 * The result of looking at a parlor from its controller.
 *
 * @param formed      every rule in the spec holds
 * @param stalls      cows milked at once (0 when the length could not be read)
 * @param problems    what is missing or wrong, most important first; empty when formed
 * @param orientation how the parlor lies in the world
 * @param pumpX       where the vacuum pump stands, in drawn cells from the controller (negative: away from the tank); {@link #NO_PUMP} if none
 */
public record ParlorCheck(boolean formed, int stalls, List<String> problems, Orientation orientation, int pumpX) {
	public static final int NO_PUMP = Integer.MIN_VALUE;

	public String status() {
		return formed ? "FORMED" : problems.get(0);
	}
}
