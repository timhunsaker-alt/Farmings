package dev.farmings.genetics;

/** Pig coat loci (docs/animals/pigs.md, "Coat genes"). Sources in the doc: OMIA, Pielberg et al. 2002, Lü et al. 2016. */
public enum PigLocus {
	/** KIT: 0 i coloured, 1 I dominant white (the Large White's duplication). */
	KIT("kit", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, new String[] {"i", "I"}),
	/** MC1R: 0 e red (Duroc), 1 E+ wild, 2 E^P black spotting (Berkshire), 3 E^D2 dominant black; the higher shows. */
	MC1R("mc1r", VisualLocus.Mode.DOMINANT_HIGH, 0, 3, new String[] {"e", "E+", "EP", "ED2"});

	public final String id;
	final VisualLocus.Mode mode;
	public final int min;
	public final int max;
	private final String[] labels;

	PigLocus(String id, VisualLocus.Mode mode, int min, int max, String[] labels) {
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
		return mode == VisualLocus.Mode.DOMINANT_LOW ? Math.min(a, b) : Math.max(a, b);
	}

	public String label(int allele) {
		return labels[clamp(allele)];
	}

	/** Founders (pigs.md): Large White I/I with a hidden E+ *(proposed)*; Duroc i/i e/e; Berkshire i/i E^P/E^P. */
	static int[] founder(String breed, PigLocus l) {
		return switch (l) {
			case KIT -> breed.equals("LWH") ? new int[] {1, 1} : new int[] {0, 1};
			case MC1R -> switch (breed) {
				case "DUR" -> new int[] {0, 1};
				case "BRK" -> new int[] {2, 1};
				default -> new int[] {1, 1};
			};
		};
	}
}
