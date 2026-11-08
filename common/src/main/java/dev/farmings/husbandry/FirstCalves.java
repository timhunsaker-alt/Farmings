package dev.farmings.husbandry;

import dev.farmings.genetics.Sex;

/**
 * The tutorial rule carried over from FarmChain (decided with Tim, 2026-11-07): a player's first two calves are heifers and the
 * third is a bull. After that the sex is whatever the genes roll.
 */
public final class FirstCalves {
	private FirstCalves() {
	}

	/** @param calvesSoFar how many calves the player has had born before this one. Returns null when the roll decides. */
	public static Sex forcedSex(int calvesSoFar) {
		return switch (calvesSoFar) {
			case 0, 1 -> Sex.FEMALE;
			case 2 -> Sex.MALE;
			default -> null;
		};
	}
}
