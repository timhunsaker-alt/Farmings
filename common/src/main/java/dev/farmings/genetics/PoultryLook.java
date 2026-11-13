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
 *   <li>Duck, Muscovy: two blue is silver, one blue, lavender, chocolate, else the wild-type Muscovy.</li>
 *   <li>Quail: Bobwhite shows its wild type. Coturnix: s/s English White, then Tibetan (E/E), Golden (Y/Y), Rosetta (E/+), Italian (Y/+),
 *       else the wild type.</li>
 * </ul>
 */
public record PoultryLook(Plumage plumage, Comb comb, boolean silky, int shell, int seed) {
	public enum Plumage {
		WHITE, WILD, RED, BLACK, BARRED, BRONZE, BOURBON, MALLARD, RESTRICTED, DUSKY, KHAKI, MUSCOVY, COTURNIX, BOBWHITE,
		// colour mutants (2026-11-11)
		MUSCOVY_BLUE, MUSCOVY_SILVER, MUSCOVY_LAVENDER, MUSCOVY_CHOCOLATE, ITALIAN, GOLDEN, ROSETTA, TIBETAN, ENGLISH_WHITE,
		// geese (2026-11-13)
		GOOSE_GREY, GOOSE_PALE, GOOSE_WHITE, GOOSE_SADDLEBACK, CHINESE_BROWN, CHINESE_WHITE,
		// guinea fowl (2026-11-13)
		PEARL, LAVENDER, GUINEA_WHITE
	}

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
					int blue = g.count(PoultryLocus.MUSCOVY_BLUE, sex);
					yield blue == 2 ? Plumage.MUSCOVY_SILVER : blue == 1 ? Plumage.MUSCOVY_BLUE
							: g.expressed(PoultryLocus.MUSCOVY_LAVENDER, sex) == 1 ? Plumage.MUSCOVY_LAVENDER
							: g.expressed(PoultryLocus.MUSCOVY_CHOCOLATE, sex) == 1 ? Plumage.MUSCOVY_CHOCOLATE : Plumage.MUSCOVY;
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
			case QUAIL -> {
				if (breedCode.equals("BOB")) {
					yield Plumage.BOBWHITE;
				}
				int brown = g.count(PoultryLocus.QUAIL_BROWN, sex);
				int fawn = g.count(PoultryLocus.QUAIL_FAWN, sex);
				yield g.expressed(PoultryLocus.QUAIL_WHITE, sex) == 1 ? Plumage.ENGLISH_WHITE
						: brown == 2 ? Plumage.TIBETAN : fawn == 2 ? Plumage.GOLDEN : brown == 1 ? Plumage.ROSETTA : fawn == 1 ? Plumage.ITALIAN
						: Plumage.COTURNIX;
			}
			case GOOSE -> {
				if (breedCode.equals("CHI")) {
					yield g.expressed(PoultryLocus.GOOSE_WHITE, sex) == 1 ? Plumage.CHINESE_WHITE : Plumage.CHINESE_BROWN;
				}
				int dil = g.count(PoultryLocus.GOOSE_DILUTION, sex);
				boolean spotted = g.expressed(PoultryLocus.GOOSE_SPOTTING, sex) == 1;
				yield spotted ? (dil >= 1 ? Plumage.GOOSE_WHITE : Plumage.GOOSE_SADDLEBACK)
						: dil == 2 ? Plumage.GOOSE_WHITE : dil == 1 ? Plumage.GOOSE_PALE : Plumage.GOOSE_GREY;
			}
			case GUINEA -> g.expressed(PoultryLocus.GUINEA_WHITE, sex) == 1 ? Plumage.GUINEA_WHITE
					: g.expressed(PoultryLocus.GUINEA_LAVENDER, sex) == 1 ? Plumage.LAVENDER : Plumage.PEARL;
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

	/** Packs the look for syncing to clients: plumage 6 bits (widened from 5 for the geese and guinea fowl), comb 2, silky 1, shell 10, seed 32. */
	public long pack() {
		long v = plumage.ordinal();
		v |= (long) comb.ordinal() << 6;
		v |= (silky ? 1L : 0L) << 8;
		v |= (long) (shell & 0x3FF) << 9;
		v |= (seed & 0xFFFFFFFFL) << 19;
		return v;
	}

	public static PoultryLook unpack(long v) {
		return new PoultryLook(Plumage.values()[(int) (v & 0x3F) % Plumage.values().length], Comb.values()[(int) (v >> 6 & 3)], (v >> 8 & 1) == 1,
				(int) (v >> 9 & 0x3FF), (int) (v >> 19 & 0xFFFFFFFFL));
	}
}
