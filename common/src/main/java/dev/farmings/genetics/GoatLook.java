package dev.farmings.genetics;

import java.util.Locale;

/**
 * What a goat shows, from its {@link GoatGenes} and sex (docs/animals/goats.md, "Coat genes").
 *
 * <ul>
 *   <li>Colour: A^Wt is white; bezoar, badgerface and swiss markings are their patterns; nonagouti a/a is black, or brown when b/b.</li>
 *   <li>Pattern: the EDNRA white body: 2 the Boer white body with a red head and neck, 1 a white belt and patches with a red head, 0
 *       plain *(the red and the part look are proposed)*.</li>
 *   <li>Horns: polled if it carries P; a P/P doe is intersex.</li>
 *   <li>Wattles if it carries W. A beard on every adult buck and on half the does by seed *(proposed: inheritance unknown)*.</li>
 * </ul>
 */
public record GoatLook(Colour colour, int pattern, boolean polled, boolean wattles, boolean beard, boolean intersex, int seed) {
	public enum Colour { WHITE, BEZOAR, BADGERFACE, SWISS, BLACK, BROWN }

	public static GoatLook of(GoatGenes g, Sex sex) {
		Colour c = switch (g.expressed(GoatLocus.AGOUTI)) {
			case 0 -> Colour.WHITE;
			case 1 -> Colour.BEZOAR;
			case 2 -> Colour.BADGERFACE;
			case 3 -> Colour.SWISS;
			default -> g.expressed(GoatLocus.BROWN) == 1 ? Colour.BROWN : Colour.BLACK;
		};
		int seed = java.util.Arrays.hashCode(g.save());
		boolean polled = g.expressed(GoatLocus.POLLED) == 1;
		boolean intersex = sex == Sex.FEMALE && g.polledHomozygous();
		boolean beard = sex == Sex.MALE || (seed & 1) == 0;
		return new GoatLook(c, g.expressed(GoatLocus.RED_HEAD), polled, g.expressed(GoatLocus.WATTLES) == 1, beard, intersex, seed);
	}

	/** A P/P buck is sterile half the time, by his genes *(proposed share; OMIA: "Some XY goats that are homozygous ... are sterile")*. */
	public static boolean sterileBuck(GoatGenes g) {
		return g.polledHomozygous() && (java.util.Arrays.hashCode(g.save()) >> 1 & 1) == 0;
	}

	public String describe() {
		StringBuilder b = new StringBuilder(colour.name().toLowerCase(Locale.ROOT));
		if (pattern == 2) {
			b.append(" body, red head");
		} else if (pattern == 1) {
			b.append(", white belt, red head");
		}
		b.append(polled ? ", polled" : ", horned");
		if (wattles) {
			b.append(", wattles");
		}
		if (intersex) {
			b.append(", INTERSEX (polled/polled)");
		}
		return b.toString();
	}

	/** Packs for syncing: colour 3 bits, pattern 2, polled 1, wattles 1, beard 1, intersex 1, seed 32. */
	public long pack() {
		long v = colour.ordinal();
		v |= (long) pattern << 3;
		v |= (polled ? 1L : 0L) << 5;
		v |= (wattles ? 1L : 0L) << 6;
		v |= (beard ? 1L : 0L) << 7;
		v |= (intersex ? 1L : 0L) << 8;
		v |= (seed & 0xFFFFFFFFL) << 9;
		return v;
	}

	public static GoatLook unpack(long v) {
		return new GoatLook(Colour.values()[(int) (v & 7) % Colour.values().length], (int) (v >> 3 & 3), (v >> 5 & 1) == 1, (v >> 6 & 1) == 1,
				(v >> 7 & 1) == 1, (v >> 8 & 1) == 1, (int) (v >> 9 & 0xFFFFFFFFL));
	}
}
