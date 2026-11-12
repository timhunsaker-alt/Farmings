package dev.farmings.genetics;

/**
 * Plumage, comb and shell loci for the four poultry species (docs/animals/poultry.md, "Plumage and shell genes"). One enum serves all
 * four; a locus a species does not use stays at its founder value and is ignored by {@link PoultryLook}. The dominance orders are the
 * usual poultry-genetics loci, cited nowhere yet: they are *(proposed)* until a source is added (the doc marks them TO VERIFY).
 *
 * <p>Sex-linked loci sit on the Z chromosome. A male (ZZ) carries two alleles; a female (ZW) carries one, in slot 0, and slot 1 is unused.
 */
public enum PoultryLocus {
	/** Chicken dominant white (PMEL17): 0 i, 1 I (dominant). Leghorn is I/I. */
	DOMINANT_WHITE("dominant_white", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, false, false, new String[] {"i", "I"}),
	/** Chicken extension (MC1R): 0 e+ wild type, 1 red, 2 E black; the higher allele shows. Red stands for the Rhode Island Red's set. */
	EXTENSION("extension", VisualLocus.Mode.DOMINANT_HIGH, 0, 2, false, false, new String[] {"e+", "eR", "E"}),
	/** Chicken barring: 0 b, 1 B (dominant), sex-linked. Plymouth Rock is barred. */
	BARRING("barring", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, false, true, new String[] {"b", "B"}),
	/** Silkie feathering: 0 H normal, 1 h silky (recessive). */
	SILKY("silky", VisualLocus.Mode.DOMINANT_LOW, 0, 1, false, false, new String[] {"H", "h"}),
	/** Rose comb: 0 r, 1 R (dominant). With pea it makes a walnut comb (the Silkie). */
	ROSE("rose_comb", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, false, false, new String[] {"r", "R"}),
	/** Pea comb: 0 p, 1 P (dominant). */
	PEA("pea_comb", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, false, false, new String[] {"p", "P"}),
	/** Recessive white, chicken, turkey and duck: 0 C coloured, 1 c (c/c is white: Silkie, Broad Breasted White, Pekin). */
	RECESSIVE_WHITE("recessive_white", VisualLocus.Mode.DOMINANT_LOW, 0, 1, false, false, new String[] {"C", "c"}),
	/** Turkey black: 0 b+ bronze, 1 B black (dominant). */
	TURKEY_BLACK("turkey_black", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, false, false, new String[] {"b+", "B"}),
	/** Turkey red: 0 R+, 1 r (recessive). Bourbon Red is r/r on bronze. */
	TURKEY_RED("turkey_red", VisualLocus.Mode.DOMINANT_LOW, 0, 1, false, false, new String[] {"R+", "r"}),
	/** Duck pattern: 0 m^d dusky, 1 M+ mallard, 2 M^R restricted; the higher allele shows. */
	DUCK_PATTERN("duck_pattern", VisualLocus.Mode.DOMINANT_HIGH, 0, 2, false, false, new String[] {"md", "M+", "MR"}),
	/** Duck brown dilution: 0 D+, 1 d (recessive), sex-linked. Dusky with d is the Khaki Campbell's khaki. */
	DUCK_BROWN("duck_brown", VisualLocus.Mode.DOMINANT_LOW, 0, 1, false, true, new String[] {"D+", "d"}),
	/** Shell shade, 0 white to 1000 dark brown: polygenic, so a blend of the two values *(proposed)*. */
	SHELL("shell_shade", VisualLocus.Mode.MEAN, 0, 1000, true, false, null),
	// Colour mutants (poultry.md, "Colour mutants", 2026-11-11). Added after SHELL so older birds' alleles and draws stay as they were.
	/** Muscovy blue: 0 +, 1 Bl; incomplete: one is blue, two are silver (TO VERIFY: breeders). Read by count, see {@link PoultryGenes#count}. */
	MUSCOVY_BLUE("muscovy_blue", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, false, false, new String[] {"+", "Bl"}),
	/** Muscovy lavender: 0 +, 1 lav, autosomal recessive (OMIA 002249). */
	MUSCOVY_LAVENDER("muscovy_lavender", VisualLocus.Mode.DOMINANT_LOW, 0, 1, false, false, new String[] {"+", "lav"}),
	/** Muscovy chocolate: 0 +, 1 choc, sex-linked recessive (TO VERIFY: breeders). */
	MUSCOVY_CHOCOLATE("muscovy_chocolate", VisualLocus.Mode.DOMINANT_LOW, 0, 1, false, true, new String[] {"+", "choc"}),
	/** Coturnix fawn (Y, ASIP): 0 +, 1 Y; incomplete: one is Italian, two are Golden. */
	QUAIL_FAWN("quail_fawn", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, false, false, new String[] {"+", "Y"}),
	/** Coturnix extended brown (E, MC1R): 0 +, 1 E; incomplete: one is Rosetta, two are Tibetan. */
	QUAIL_BROWN("quail_brown", VisualLocus.Mode.DOMINANT_HIGH, 0, 1, false, false, new String[] {"+", "E"}),
	/** Coturnix dotted white (S, EDNRB2): 0 +, 1 s, recessive; s/s is English White. */
	QUAIL_WHITE("quail_white", VisualLocus.Mode.DOMINANT_LOW, 0, 1, false, false, new String[] {"+", "s"});

	/** Herd founders of the breed carry each colour mutant at this share *(proposed)*; other breeds never. */
	static final int MUTANT_PERCENT = 5;

	public final String id;
	final VisualLocus.Mode mode;
	public final int min;
	public final int max;
	final boolean range;
	/** On the Z chromosome: a female has one allele, from her sire. */
	public final boolean sexLinked;
	private final String[] labels;

	PoultryLocus(String id, VisualLocus.Mode mode, int min, int max, boolean range, boolean sexLinked, String[] labels) {
		this.id = id;
		this.mode = mode;
		this.min = min;
		this.max = max;
		this.range = range;
		this.sexLinked = sexLinked;
		this.labels = labels;
	}

	int clamp(int v) {
		return Math.max(min, Math.min(max, v));
	}

	/** What a pair shows; a female shows her one Z allele at a sex-linked locus. */
	public int express(int a, int b, Sex sex) {
		if (sexLinked && sex == Sex.FEMALE) {
			return a;
		}
		return switch (mode) {
			case DOMINANT_LOW -> Math.min(a, b);
			case DOMINANT_HIGH -> Math.max(a, b);
			case MEAN -> (int) Math.floor((a + b) / 2.0 + 0.5);
			case LAYERED -> throw new IllegalStateException("no layered poultry locus");
		};
	}

	public String label(int allele) {
		return labels == null ? Integer.toString(allele) : labels[clamp(allele)];
	}

	/**
	 * Founder alleles per breed: a weighted list {value, weight, ...}, or {low, high} for a range locus. From the breed colours in
	 * docs/animals/poultry.md; the shell ranges are *(proposed)* shades for white, cream, tinted and brown shells.
	 */
	static int[] founder(String breed, PoultryLocus l) {
		return switch (l) {
			case DOMINANT_WHITE -> breed.equals("LEG") ? new int[] {1, 1} : new int[] {0, 1};
			case EXTENSION -> switch (breed) {
				case "RIR" -> new int[] {1, 1};
				case "PLR" -> new int[] {2, 1};
				default -> new int[] {0, 1};
			};
			case BARRING -> breed.equals("PLR") ? new int[] {1, 1} : new int[] {0, 1};
			case SILKY -> breed.equals("SLK") ? new int[] {1, 1} : new int[] {0, 1};
			case ROSE, PEA -> breed.equals("SLK") ? new int[] {1, 1} : new int[] {0, 1};
			case RECESSIVE_WHITE -> switch (breed) {
				case "SLK", "BBW", "PEK" -> new int[] {1, 1};
				default -> new int[] {0, 1};
			};
			case TURKEY_BLACK -> new int[] {0, 1};
			case TURKEY_RED -> breed.equals("BRD") ? new int[] {1, 1} : new int[] {0, 1};
			case DUCK_PATTERN -> breed.equals("KHC") ? new int[] {0, 1} : new int[] {1, 1};
			case DUCK_BROWN -> breed.equals("KHC") ? new int[] {1, 1} : new int[] {0, 1};
			case SHELL -> switch (breed) {
				case "LEG", "PEK", "BOB", "MUS" -> new int[] {0, 60};
				case "SLK", "MAL" -> new int[] {150, 300};
				case "KHC" -> new int[] {100, 200};
				case "RIR" -> new int[] {600, 800};
				case "PLR" -> new int[] {550, 750};
				case "COT" -> new int[] {450, 650};
				default -> new int[] {400, 650};   // turkeys
			};
			case MUSCOVY_BLUE, MUSCOVY_LAVENDER, MUSCOVY_CHOCOLATE -> breed.equals("MUS") ? new int[] {0, 100 - MUTANT_PERCENT, 1, MUTANT_PERCENT} : new int[] {0, 1};
			case QUAIL_FAWN, QUAIL_BROWN, QUAIL_WHITE -> breed.equals("COT") ? new int[] {0, 100 - MUTANT_PERCENT, 1, MUTANT_PERCENT} : new int[] {0, 1};
		};
	}
}
