package dev.farmings.genetics;

/**
 * The coat and body-shape loci the renderer can show, from FarmChain's visual genome (BREED-SPEC §7.3), each with FarmChain's inheritance
 * mode (§7.2). A locus holds small integer alleles; {@link #express} turns a pair into what the animal shows.
 */
public enum VisualLocus {
	/** MC1R extension: 0 ED (dominant black), 1 EBR (Holstein black/red), 2 E+ (wild type), 3 e (recessive red). ED > EBR > E+ > e (UC Davis VGL). */
	EXTENSION("pigment_extension", Mode.DOMINANT_LOW, 0, 3, false, new String[] {"ED", "EBR", "E+", "e"}),
	/** Coat dilution, none to strong (0-3); semi-dominant (FarmChain §7.2: incomplete dominance). */
	DILUTION("dilution", Mode.MEAN, 0, 3, false, new String[] {"none", "light", "medium", "strong"}),
	/**
	 * Pattern family: 0 solid, 1 colour-sided, 2 speckled, 3 patch (piebald). Two different patterns layer (FarmChain §7.2: codominant /
	 * layered); solid over any pattern is solid *(proposed: real piebald spotting is recessive, which this approximates)*.
	 */
	PATTERN("pattern_family", Mode.LAYERED, 0, 3, false, new String[] {"solid", "color-sided", "speckled", "patch"}),
	/** How much of the coat is white, 0-1000 (FarmChain §7.2: polygenic blend). */
	WHITE("white_coverage", Mode.MEAN, 0, 1000, true, null),
	/** Face white: 0 none, 1 star, 2 blaze, 3 white face; the whiter allele shows *(proposed)*. */
	FACE("face_white", Mode.DOMINANT_HIGH, 0, 3, false, new String[] {"none", "star", "blaze", "white face"}),
	/** 0 polled, 1 horned: polled is dominant (FarmChain §7.2: Mendelian; UC Davis VGL, Polled test). */
	HORNS("horn_status", Mode.DOMINANT_LOW, 0, 1, false, new String[] {"polled", "horned"}),
	/** Zebu hump, 0-3 (FarmChain §7.2: incomplete dominance). */
	HUMP("zebu_hump", Mode.MEAN, 0, 3, false, null),
	/** Dewlap and loose skin, 0-3 (incomplete dominance). */
	DEWLAP("dewlap_loose_skin", Mode.MEAN, 0, 3, false, null),
	/** Ear length, 0-3 (FarmChain §7.2: incomplete dominance). */
	EARS("ear_length_shape", Mode.MEAN, 0, 3, false, null),
	// Loose ends, 2026-11-11 (docs/GENETICS.md): appended so older animals' alleles and draws stay as they were.
	/** Roan (KITLG): 0 +, 1 R; incomplete dominance (OMIA 001216): R/+ roan, R/R white. Read by count. */
	ROAN("roan", Mode.DOMINANT_HIGH, 0, 1, false, new String[] {"+", "R"}),
	/** Agouti (ASIP): 0 A+, 1 Abr; brindle shows only on E+ (AHCA). */
	AGOUTI("agouti", Mode.DOMINANT_HIGH, 0, 1, false, new String[] {"A+", "Abr"}),
	/** PMEL dilution: 0 +, 1 Dh (Highland), 2 Dc (Charolais); semi-dominant, strength added up (OMIA 001545; AHCA). */
	PMEL("pmel_dilution", Mode.DOMINANT_HIGH, 0, 2, false, new String[] {"+", "Dh", "Dc"});

	enum Mode { DOMINANT_LOW, DOMINANT_HIGH, MEAN, LAYERED }

	public final String id;
	final Mode mode;
	public final int min;
	public final int max;
	/** Founder alleles are drawn from a range (two values), not a weighted list. */
	final boolean range;
	private final String[] labels;

	VisualLocus(String id, Mode mode, int min, int max, boolean range, String[] labels) {
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

	/**
	 * What a pair shows: the lower or higher allele for a dominance order, the mean (rounded half up) for a blend, and for the layered
	 * pattern a bit mask of the patterns shown (bit 0 alone for solid).
	 */
	public int express(int a, int b) {
		return switch (mode) {
			case DOMINANT_LOW -> Math.min(a, b);
			case DOMINANT_HIGH -> Math.max(a, b);
			case MEAN -> (int) Math.floor((a + b) / 2.0 + 0.5);
			case LAYERED -> a == 0 || b == 0 ? 1 : (1 << a) | (1 << b);
		};
	}

	public String label(int allele) {
		return labels == null ? Integer.toString(allele) : labels[clamp(allele)];
	}
}
