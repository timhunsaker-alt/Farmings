package dev.farmings.genetics;

/**
 * Sheep coat and horn loci (docs/animals/sheep.md, "Coat genes"). Each holds small integer alleles; {@link #express} turns a pair into
 * what the sheep shows. Dominance orders are from Sponenberg's sheep colour genetics notes (Cornell) and OMIA; modes marked proposed
 * in the doc have no source.
 */
public enum SheepLocus {
	/** ASIP agouti: 0 white/tan A^Wt (dominant), 1 non-agouti a (black or brown). */
	AGOUTI("agouti", VisualLocus.Mode.DOMINANT_LOW, 0, 1, false, new String[] {"Awt", "a"}),
	/** MC1R extension: 0 wild type E+, 1 dominant black E^D (dominant). E^D masks agouti. */
	EXTENSION("extension", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, false, new String[] {"E+", "ED"}),
	/** TYRP1 brown: 0 black B+, 1 brown b (moorit, recessive). */
	BROWN("brown", VisualLocus.Mode.DOMINANT_LOW, 0, 1, false, new String[] {"B+", "b"}),
	/** White spotting: 0 plain, 1 spotted; spotting is usually dominant (Sponenberg). */
	SPOTTING("spotting", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, false, new String[] {"s", "S"}),
	/** How much white a spotted sheep shows, 0-1000; a blend *(proposed)*. */
	WHITE("white_amount", VisualLocus.Mode.MEAN, 0, 1000, true, null),
	/** Head and leg colour: 0 white, 2 black (Suffolk, Dorper); a blend, so crosses show a mottled head *(proposed)*. */
	HEAD("head_colour", VisualLocus.Mode.MEAN, 0, 2, false, new String[] {"white", "mottled", "black"}),
	/** RXFP2 horns: 0 polled P (dominant), 1 horned H; sex-influenced, see {@link SheepLook}. */
	HORNS("horns", VisualLocus.Mode.DOMINANT_LOW, 0, 1, false, new String[] {"P", "H"}),
	/** Coat type: 0 wool, 2 hair that sheds; a blend, so a wool x hair cross sheds part of its coat *(proposed)*. */
	COAT("coat_type", VisualLocus.Mode.MEAN, 0, 2, false, new String[] {"wool", "part hair", "hair"}),
	/** Fleece crimp, 0-3; drawn only, a blend *(proposed)*. */
	CRIMP("crimp", VisualLocus.Mode.MEAN, 0, 3, false, null);

	public final String id;
	final VisualLocus.Mode mode;
	public final int min;
	public final int max;
	final boolean range;
	private final String[] labels;

	SheepLocus(String id, VisualLocus.Mode mode, int min, int max, boolean range, String[] labels) {
		this.id = id;
		this.mode = mode;
		this.min = min;
		this.max = max;
		this.range = range;
		this.labels = labels;
	}

	int clamp(int v) {
		return Math.max(min, Math.min(max, v));
	}

	public int express(int a, int b) {
		return switch (mode) {
			case DOMINANT_LOW -> Math.min(a, b);
			case DOMINANT_HIGH -> Math.max(a, b);
			case MEAN -> (int) Math.floor((a + b) / 2.0 + 0.5);
			case LAYERED -> throw new IllegalStateException("no layered sheep locus");
		};
	}

	public String label(int allele) {
		return labels == null ? Integer.toString(allele) : labels[clamp(allele)];
	}

	/**
	 * Founder alleles per breed: a weighted list {value, weight, ...} or, for a range locus, {low, high}. All four breeds are white
	 * (A^Wt); Suffolk and Dorper have black heads; Merino rams are horned (H/H); the others are polled; Dorper is a hair sheep
	 * (docs/animals/sheep.md). The 5% hidden non-agouti and brown alleles are *(proposed)*, so a coloured lamb turns up now and then.
	 */
	static int[] founder(String breed, SheepLocus l) {
		return switch (l) {
			case AGOUTI -> breed.equals("DOR") ? new int[] {0, 1} : new int[] {0, 95, 1, 5};
			case EXTENSION -> new int[] {0, 1};
			case BROWN -> breed.equals("DOR") ? new int[] {0, 1} : new int[] {0, 95, 1, 5};
			case SPOTTING -> new int[] {0, 1};
			case WHITE -> new int[] {200, 700};
			case HEAD -> breed.equals("SUF") || breed.equals("DOR") ? new int[] {2, 1} : new int[] {0, 1};
			case HORNS -> breed.equals("MER") ? new int[] {1, 1} : new int[] {0, 1};
			case COAT -> breed.equals("DOR") ? new int[] {2, 1} : new int[] {0, 1};
			case CRIMP -> switch (breed) {
				case "MER" -> new int[] {3, 1};
				case "DOR" -> new int[] {0, 1};
				default -> new int[] {1, 1};
			};
		};
	}
}
