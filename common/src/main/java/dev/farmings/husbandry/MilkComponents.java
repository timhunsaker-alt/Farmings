package dev.farmings.husbandry;

/**
 * Fat and protein in a cow's milk (docs/animals/cattle.md, Milk; docs/multiblocks/creamery.md). Breed figures: USDA AIPL standardized
 * lactation averages (Holstein 3.72/3.06, Jersey 4.76/3.63, Brown Swiss 4.02/3.32). Gyr fat 4.55 (midpoint of 4.5-4.6, IJAnS review);
 * Gyr protein is not sourced, Brown Swiss's 3.32 stands in. Beef breeds are not milked; Holstein's figures stand in.
 */
public final class MilkComponents {
	/** Her butterfat score over her breed's baseline moves her fat by at most 20% either way *(proposed)*. */
	public static final double MIN_FACTOR = 0.8;
	public static final double MAX_FACTOR = 1.2;

	private MilkComponents() {
	}

	public static double breedFat(CattleBreed breed) {
		return switch (breed) {
			case JERSEY -> 4.76;
			case BROWN_SWISS -> 4.02;
			case GYR -> 4.55;
			default -> 3.72;
		};
	}

	public static double breedProtein(CattleBreed breed) {
		return switch (breed) {
			case JERSEY -> 3.63;
			case BROWN_SWISS, GYR -> 3.32;
			default -> 3.06;
		};
	}

	/** Her fat: the breed's, times her butterfat score over the breed baseline (both on the 0-1000 scale), clamped. */
	public static double fat(CattleBreed breed, int butterfatScore, int breedBaseline0to100) {
		if (breedBaseline0to100 <= 0) {
			return breedFat(breed);
		}
		double f = (double) butterfatScore / (breedBaseline0to100 * 10);
		return breedFat(breed) * Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, f));
	}
}
