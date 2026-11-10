package dev.farmings.genetics;

/**
 * The core traits of every species (see {@link Species} for which species carries which), ported from FarmChain (BREED-SPEC 8.1
 * for cattle; the gene-catalog for sheep). Scores are integers on a 0-1000 scale.
 * The {@link #id} is FarmChain's locus name and is used in the canonical genome string.
 */
public enum Trait {
	MILK_YIELD("milk_yield", "MY", false),
	BUTTERFAT("butterfat", "B", false),
	PROTEIN("protein", "P", false),
	FEED_EFFICIENCY("feed_efficiency", "FE", false),
	HEALTH("health", "H", true),
	FERTILITY("fertility", "F", true),
	LONGEVITY("longevity", "L", true),
	TEMPERAMENT("temperament", "T", false),
	// Sheep (FarmChain's proposed sheep gene list; added 2026-11-10 after the cattle traits so cattle ordinals never move).
	WOOL_YIELD("wool_yield", "WY", false),
	FLEECE_QUALITY("fleece_quality", "FQ", false),
	HARDINESS("hardiness", "HD", true);

	public final String id;
	/** Short code used in allele ids (milk_yield -> MY, so MY_A0 ... MY_A6). */
	public final String code;
	/** Fitness traits get the largest heterosis benefit (BREED-SPEC 8.4). */
	public final boolean fitness;

	Trait(String id, String code, boolean fitness) {
		this.id = id;
		this.code = code;
		this.fitness = fitness;
	}
}
