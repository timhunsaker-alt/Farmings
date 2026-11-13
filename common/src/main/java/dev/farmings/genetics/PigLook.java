package dev.farmings.genetics;

import java.util.Locale;

/**
 * What a pig shows (docs/animals/pigs.md, "Coat genes"): dominant white I is white; else MC1R: E^D2 black, E^P/E^P the Berkshire's black
 * with six white points *(proposed: the points' own gene is unsettled)*, E^P with another allele black spots on red or white, E+ the wild
 * brown, e/e red.
 */
public record PigLook(Colour colour, int seed) {
	public enum Colour { WHITE, BLACK, BERKSHIRE, SPOTTED, WILD, RED }

	public static PigLook of(PigGenes g) {
		int seed = java.util.Arrays.hashCode(g.save());
		if (g.expressed(PigLocus.KIT) == 1) {
			return new PigLook(Colour.WHITE, seed);
		}
		int[] m = g.pair(PigLocus.MC1R);
		Colour c = switch (g.expressed(PigLocus.MC1R)) {
			case 3 -> Colour.BLACK;
			case 2 -> m[0] == 2 && m[1] == 2 ? Colour.BERKSHIRE : Colour.SPOTTED;
			case 1 -> Colour.WILD;
			default -> Colour.RED;
		};
		return new PigLook(c, seed);
	}

	public String describe() {
		return switch (colour) {
			case BERKSHIRE -> "black, six white points";
			case SPOTTED -> "black spots";
			default -> colour.name().toLowerCase(Locale.ROOT);
		};
	}

	public long pack() {
		return colour.ordinal() | (seed & 0xFFFFFFFFL) << 3;
	}

	public static PigLook unpack(long v) {
		return new PigLook(Colour.values()[(int) (v & 7) % Colour.values().length], (int) (v >> 3 & 0xFFFFFFFFL));
	}
}
