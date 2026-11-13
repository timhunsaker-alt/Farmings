package dev.farmings.genetics;

import java.util.EnumSet;
import java.util.List;

/**
 * Which animal a genome belongs to, and so which core traits it carries. Cattle keep FarmChain's eight dairy loci (BREED-SPEC 8.1),
 * exactly as before sheep were added. Sheep take FarmChain's proposed sheep gene list (packages/genetics gene-catalog: wool_yield,
 * fleece_quality, feed_efficiency, health, fertility, longevity, temperament, hardiness) plus milk_yield for dairy sheep
 * (Farmings' addition, decided with Tim 2026-11-10). Traits are always walked in {@link Trait} order, so random draws keep a fixed order.
 */
public enum Species {
	CATTLE("farmings.cattle.genome.v1", false, EnumSet.of(Trait.MILK_YIELD, Trait.BUTTERFAT, Trait.PROTEIN, Trait.FEED_EFFICIENCY, Trait.HEALTH,
			Trait.FERTILITY, Trait.LONGEVITY, Trait.TEMPERAMENT)),
	SHEEP("farmings.sheep.genome.v1", false, EnumSet.of(Trait.MILK_YIELD, Trait.FEED_EFFICIENCY, Trait.HEALTH, Trait.FERTILITY, Trait.LONGEVITY,
			Trait.TEMPERAMENT, Trait.WOOL_YIELD, Trait.FLEECE_QUALITY, Trait.HARDINESS)),
	// Poultry (docs/animals/poultry.md): the shared fitness traits plus egg production, egg weight and broodiness.
	CHICKEN("farmings.chicken.genome.v1", true, fowlTraits()),
	TURKEY("farmings.turkey.genome.v1", true, fowlTraits()),
	QUAIL("farmings.quail.genome.v1", true, fowlTraits()),
	DUCK("farmings.duck.genome.v1", true, fowlTraits()),
	// Goats (docs/animals/goats.md), appended so no saved ordinal moves: the dairy loci plus the small-ruminant fitness traits
	// *(proposed list: FarmChain has no goat catalog)*.
	GOAT("farmings.goat.genome.v1", false, EnumSet.of(Trait.MILK_YIELD, Trait.BUTTERFAT, Trait.PROTEIN, Trait.FEED_EFFICIENCY, Trait.HEALTH,
			Trait.FERTILITY, Trait.LONGEVITY, Trait.TEMPERAMENT, Trait.HARDINESS)),
	// Geese (docs/animals/poultry.md, "Geese and guinea fowl"), appended: the poultry traits.
	GOOSE("farmings.goose.genome.v1", true, fowlTraits()),
	// Helmeted guinea fowl (poultry.md, "Geese and guinea fowl"), appended: the poultry traits.
	GUINEA("farmings.guinea.genome.v1", true, fowlTraits());

	private static EnumSet<Trait> fowlTraits() {
		return EnumSet.of(Trait.FEED_EFFICIENCY, Trait.HEALTH, Trait.FERTILITY, Trait.LONGEVITY, Trait.TEMPERAMENT, Trait.EGG_PRODUCTION,
				Trait.EGG_WEIGHT, Trait.BROODINESS);
	}

	/** True for the poultry species. Set on each constant, so a new mammal is never taken for a bird. */
	public boolean fowl() {
		return fowl;
	}

	public final String schema;
	private final boolean fowl;
	private final List<Trait> traits;

	Species(String schema, boolean fowl, EnumSet<Trait> traits) {
		this.schema = schema;
		this.fowl = fowl;
		this.traits = List.copyOf(traits);
	}

	/** This species' core traits in {@link Trait} order. */
	public List<Trait> traits() {
		return traits;
	}

	public boolean has(Trait t) {
		return traits.contains(t);
	}

	/** The species a genome schema names; throws on an unknown schema. */
	public static Species ofSchema(String schema) {
		for (Species s : values()) {
			if (s.schema.equals(schema)) {
				return s;
			}
		}
		throw new IllegalArgumentException("not a Farmings genome schema: " + schema);
	}
}
