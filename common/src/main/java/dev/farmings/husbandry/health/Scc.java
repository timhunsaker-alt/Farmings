package dev.farmings.husbandry.health;

/**
 * Somatic cell counts and the California Mastitis Test (docs/animal-health.md, "Mastitis"). Subclinical is "above 200,000 cells per mL"
 * (University of Kentucky); the CMT bands are Kentucky's table (TO VERIFY: sources differ on the trace band). The Grade "A" limit for
 * individual producer milk is 750,000/mL, 1,500,000/mL for goat milk (PMO 2013, Section 7).
 */
public final class Scc {
	public static final int SUBCLINICAL = 200_000;
	public static final int TANK_LIMIT = 750_000;
	public static final int GOAT_TANK_LIMIT = 1_500_000;
	/** A healthy cow's count (proposed: inside the usual 50,000-150,000). */
	public static final int HEALTHY = 100_000;
	public static final int SUBCLINICAL_LEVEL = 600_000;
	public static final int CLINICAL_LEVEL = 3_000_000;
	public static final int CHRONIC_LEVEL = 700_000;

	private Scc() {
	}

	/** The CMT score for a count: N, T, 1, 2 or 3. */
	public static String cmt(int scc) {
		if (scc < 200_000) {
			return "N";
		}
		if (scc < 400_000) {
			return "T";
		}
		if (scc < 1_200_000) {
			return "1";
		}
		if (scc < 5_000_000) {
			return "2";
		}
		return "3";
	}

	/** The band a CMT score stands for, for the message. */
	public static String band(String cmt) {
		return switch (cmt) {
			case "N" -> "under 200,000 cells/mL";
			case "T" -> "200,000-400,000 cells/mL";
			case "1" -> "400,000-1,200,000 cells/mL";
			case "2" -> "1,200,000-5,000,000 cells/mL";
			default -> "over 5,000,000 cells/mL";
		};
	}

	/** The limit a tank's milk is held to: goat milk's is higher. */
	public static int tankLimit(boolean goat) {
		return goat ? GOAT_TANK_LIMIT : TANK_LIMIT;
	}

	/** Volume-weighted mix of a tank's count with milk added at another count. */
	public static double mix(double tankScc, double tankL, double addScc, double addL) {
		double total = tankL + addL;
		return total <= 0 ? 0 : (tankScc * tankL + addScc * addL) / total;
	}
}
