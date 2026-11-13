package dev.farmings.genetics;

/**
 * Goat coat, horn and wattle loci (docs/animals/goats.md, "Coat genes"). Small integer alleles; {@link #express} turns a pair into what
 * the goat shows. Sources are in the doc: OMIA for agouti, the EDNRA copy-number red head, polled and wattles.
 */
public enum GoatLocus {
	/**
	 * ASIP agouti: 0 white/tan A^Wt (top dominant), 1 bezoar A^bz, 2 badgerface A^b, 3 swiss markings A^sm, 4 nonagouti a (recessive).
	 * The middle alleles are codominant in goats; the game shows the lower-numbered one *(proposed)*.
	 */
	AGOUTI("agouti", VisualLocus.Mode.DOMINANT_LOW, 0, 4, new String[] {"Awt", "Abz", "Ab", "Asm", "a"}),
	/** TYRP1 brown: 0 black B+, 1 brown b (recessive) *(proposed for goats, as sheep)*. */
	BROWN("brown", VisualLocus.Mode.DOMINANT_LOW, 0, 1, new String[] {"B+", "b"}),
	/** EDNRA copy number, incomplete dominance: 0 pigmented, 2 white body (Boer); 0/2 shows 1, a belt and patches. */
	RED_HEAD("red_head", VisualLocus.Mode.MEAN, 0, 2, new String[] {"P", "part", "W"}),
	/** Polled (the PIS deletion): 0 horned, 1 polled, dominant. P/P does are intersex. */
	POLLED("polled", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, new String[] {"p", "P"}),
	/** Wattles: 0 none, 1 wattles, dominant. */
	WATTLES("wattles", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, new String[] {"w", "W"});

	public final String id;
	final VisualLocus.Mode mode;
	public final int min;
	public final int max;
	private final String[] labels;

	GoatLocus(String id, VisualLocus.Mode mode, int min, int max, String[] labels) {
		this.id = id;
		this.mode = mode;
		this.min = min;
		this.max = max;
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
			case LAYERED -> throw new IllegalStateException("no layered goat locus");
		};
	}

	public String label(int allele) {
		return labels[clamp(allele)];
	}

	/**
	 * Founder alleles per breed, a weighted list {value, weight, ...} (goats.md, "Founders"): Saanen A^Wt and pigmented; Nubian any agouti
	 * colour and some brown; Boer white body (W/W), horned. Polled at 15% and wattles at 30% of alleles in the dairy breeds *(proposed)*.
	 */
	static int[] founder(String breed, GoatLocus l) {
		boolean boer = breed.equals("BOE");
		boolean nubian = breed.equals("NUB");
		return switch (l) {
			case AGOUTI -> nubian ? new int[] {1, 30, 2, 25, 3, 25, 4, 20} : new int[] {0, 1};
			case BROWN -> nubian ? new int[] {0, 80, 1, 20} : new int[] {0, 1};
			case RED_HEAD -> boer ? new int[] {2, 1} : new int[] {0, 1};
			case POLLED -> boer ? new int[] {0, 1} : new int[] {0, 85, 1, 15};
			case WATTLES -> new int[] {0, 70, 1, 30};
		};
	}
}
