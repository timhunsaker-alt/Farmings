package dev.farmings.genetics;

import java.util.Locale;

/**
 * What a bird shows, worked out from its {@link PoultryGenes}, species and sex (docs/animals/poultry.md, "Plumage and shell genes").
 *
 * <ul>
 *   <li>Chicken: dominant white or c/c is white; otherwise black E with barring is barred, black E is black, red is red, e+ is the wild
 *       junglefowl look. Rose and pea combs; both make a walnut comb. Silky feathering is h/h.</li>
 *   <li>Turkey: c/c white; black B; otherwise r/r is Bourbon red, else bronze.</li>
 *   <li>Duck: c/c white; restricted M^R; mallard M+; dusky m^d, khaki when also brown-diluted. The Muscovy has its own look.</li>
 *   <li>Quail: each species shows its wild type.</li>
 * </ul>
 */
public record PoultryLook(Plumage plumage, Comb comb, boolean silky, int shell, int seed) {
	public enum Plumage { WHITE, WILD, RED, BLACK, BARRED, BRONZE, BOURBON, MALLARD, RESTRICTED, DUSKY, KHAKI, MUSCOVY, COTURNIX, BOBWHITE }

	public enum Comb { SINGLE, ROSE, PEA, WALNUT }

	public static PoultryLook of(Species species, String breedCode, PoultryGenes g, Sex sex) {
		boolean white = g.expressed(PoultryLocus.RECESSIVE_WHITE, sex) == 1;
		Plumage p = switch (species) {
			case CHICKEN -> {
				if (white || g.expressed(PoultryLocus.DOMINANT_WHITE, sex) == 1) {
					yield Plumage.WHITE;
				}
				int e = g.expressed(PoultryLocus.EXTENSION, sex);
				if (e == 2) {
					yield g.expressed(PoultryLocus.BARRING, sex) == 1 ? Plumage.BARRED : Plumage.BLACK;
				}
				yield e == 1 ? Plumage.RED : Plumage.WILD;
			}
			case TURKEY -> white ? Plumage.WHITE
					: g.expressed(PoultryLocus.TURKEY_BLACK, sex) == 1 ? Plumage.BLACK
					: g.expressed(PoultryLocus.TURKEY_RED, sex) == 1 ? Plumage.BOURBON : Plumage.BRONZE;
			case DUCK -> {
				if (breedCode.equals("MUS")) {
					yield Plumage.MUSCOVY;
				}
				if (white) {
					yield Plumage.WHITE;
				}
				yield switch (g.expressed(PoultryLocus.DUCK_PATTERN, sex)) {
					case 2 -> Plumage.RESTRICTED;
					case 1 -> Plumage.MALLARD;
					default -> g.expressed(PoultryLocus.DUCK_BROWN, sex) == 1 ? Plumage.KHAKI : Plumage.DUSKY;
				};
			}
			case QUAIL -> breedCode.equals("BOB") ? Plumage.BOBWHITE : Plumage.COTURNIX;
			default -> throw new IllegalArgumentException("not a bird: " + species);
		};
		Comb comb = Comb.SINGLE;
		boolean silky = false;
		if (species == Species.CHICKEN) {
			boolean rose = g.expressed(PoultryLocus.ROSE, sex) == 1;
			boolean pea = g.expressed(PoultryLocus.PEA, sex) == 1;
			comb = rose && pea ? Comb.WALNUT : rose ? Comb.ROSE : pea ? Comb.PEA : Comb.SINGLE;
			silky = g.expressed(PoultryLocus.SILKY, sex) == 1;
		}
		return new PoultryLook(p, comb, silky, g.expressed(PoultryLocus.SHELL, sex), java.util.Arrays.hashCode(g.save()));
	}

	public String describe() {
		StringBuilder b = new StringBuilder(plumage.name().toLowerCase(Locale.ROOT));
		if (comb != Comb.SINGLE) {
			b.append(", ").append(comb.name().toLowerCase(Locale.ROOT)).append(" comb");
		}
		if (silky) {
			b.append(", silky");
		}
		return b.toString();
	}

	/** The shell colour word for a shade (proposed bands). */
	public static String shellColour(int shade) {
		return shade < 100 ? "white" : shade < 350 ? "cream" : shade < 550 ? "tinted" : "brown";
	}

	/** Packs the look for syncing to clients: plumage 4 bits, comb 2, silky 1, shell 10, seed 32. */
	public long pack() {
		long v = plumage.ordinal();
		v |= (long) comb.ordinal() << 4;
		v |= (silky ? 1L : 0L) << 6;
		v |= (long) (shell & 0x3FF) << 7;
		v |= (seed & 0xFFFFFFFFL) << 17;
		return v;
	}

	public static PoultryLook unpack(long v) {
		return new PoultryLook(Plumage.values()[(int) (v & 0xF)], Comb.values()[(int) (v >> 4 & 3)], (v >> 6 & 1) == 1, (int) (v >> 7 & 0x3FF),
				(int) (v >> 17 & 0xFFFFFFFFL));
	}
}
