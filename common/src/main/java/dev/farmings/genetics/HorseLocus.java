package dev.farmings.genetics;

/**
 * Horse loci (docs/animals/horses.md, "Speed" and "Coat colour genes"): the MSTN speed gene and the coat genes. Allele 1 is the named
 * (dominant) allele at every coat locus. Sources in the doc: Hill et al. 2010; Univ. of Kentucky Gluck; PLoS Genet. 2013.
 */
public enum HorseLocus {
	/** MSTN g.66493737: 0 T, 1 C. Codominant: C/C sprinter, C/T middle distance, T/T stayer. */
	MSTN("mstn", new String[] {"T", "C"}),
	/** Extension (MC1R): 0 e, 1 E; e/e chestnut. */
	E("extension", new String[] {"e", "E"}),
	/** Agouti (ASIP): 0 a, 1 A; with E, A bay and a/a black. */
	A("agouti", new String[] {"a", "A"}),
	/** Cream (SLC45A2): 0 n, 1 Cr; one copy dilutes red, two dilute black too. */
	CR("cream", new String[] {"n", "Cr"}),
	/** Dun (TBX3): 0 nd, 1 D; dominant, no dosage effect. */
	D("dun", new String[] {"nd", "D"}),
	/** Grey (STX17): 0 g, 1 G; dominant, greys with age. */
	G("grey", new String[] {"g", "G"}),
	/** Roan (KIT): 0 rn, 1 Rn; dominant. */
	RN("roan", new String[] {"rn", "Rn"}),
	/** Sabino-1 (KIT): 0 n, 1 SB1; more white with two copies. */
	SB1("sabino", new String[] {"n", "SB1"});

	public final String id;
	private final String[] labels;

	HorseLocus(String id, String[] labels) {
		this.id = id;
		this.labels = labels;
	}

	int clamp(int v) {
		return Math.max(0, Math.min(1, v));
	}

	public String label(int allele) {
		return labels[clamp(allele)];
	}

	/**
	 * Founders' share of allele 1 by breed, per thousand (horses.md: MSTN from Hill et al. 2010 and the Thoroughbred's 45% proposed; coat
	 * shares proposed from "TB bay mostly with 15% e and 5% G; QH 40% e/e and a few Cr and D; Arabian 30% G, no Cr or D", each phenotype
	 * share turned into an allele share by Hardy-Weinberg, derived; roan and sabino proposed).
	 */
	static int founderShare(String breed, HorseLocus l) {
		return switch (breed) {
			case "QH" -> switch (l) {
				case MSTN -> 900;
				case E -> 368;     // e/e 40%: e at 0.632
				case A -> 600;
				case CR -> 50;
				case D -> 50;
				case G -> 0;
				case RN -> 30;
				case SB1 -> 30;
			};
			case "ARA" -> switch (l) {
				case MSTN -> 50;
				case E -> 550;
				case A -> 650;
				case CR, D, RN -> 0;
				case G -> 163;     // grey 30%: G at 0.163
				case SB1 -> 50;
			};
			default -> switch (l) {     // Thoroughbred
				case MSTN -> 450;
				case E -> 613;     // e/e 15%: e at 0.387
				case A -> 700;
				case G -> 25;      // grey 5%: G at 0.025
				case CR, D, RN, SB1 -> 0;
			};
		};
	}
}
