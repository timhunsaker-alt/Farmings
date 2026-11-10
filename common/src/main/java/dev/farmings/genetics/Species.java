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
	CATTLE("farmings.cattle.genome.v1", EnumSet.of(Trait.MILK_YIELD, Trait.BUTTERFAT, Trait.PROTEIN, Trait.FEED_EFFICIENCY, Trait.HEALTH,
			Trait.FERTILITY, Trait.LONGEVITY, Trait.TEMPERAMENT)),
	SHEEP("farmings.sheep.genome.v1", EnumSet.of(Trait.MILK_YIELD, Trait.FEED_EFFICIENCY, Trait.HEALTH, Trait.FERTILITY, Trait.LONGEVITY,
			Trait.TEMPERAMENT, Trait.WOOL_YIELD, Trait.FLEECE_QUALITY, Trait.HARDINESS));

	public final String schema;
	private final List<Trait> traits;

	Species(String schema, EnumSet<Trait> traits) {
		this.schema = schema;
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
