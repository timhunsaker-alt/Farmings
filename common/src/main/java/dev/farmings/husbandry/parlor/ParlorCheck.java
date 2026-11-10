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
 * @param sheep       for each stall, true if it is a sheep stall (empty before the stalls are read)
 */
public record ParlorCheck(boolean formed, int stalls, List<String> problems, Orientation orientation, int pumpX, List<Boolean> sheep) {
	public static final int NO_PUMP = Integer.MIN_VALUE;

	public ParlorCheck(boolean formed, int stalls, List<String> problems, Orientation orientation, int pumpX) {
		this(formed, stalls, problems, orientation, pumpX, List.of());
	}

	/** True if stall {@code i} is a sheep stall. */
	public boolean sheepStall(int i) {
		return i >= 0 && i < sheep.size() && sheep.get(i);
	}

	public String status() {
		return formed ? "FORMED" : problems.get(0);
	}
}
