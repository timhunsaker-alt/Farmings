package dev.farmings.genetics;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tuning values for heterosis, inbreeding and sampling, in basis points. These are FarmChain's published defaults
 * (BREED-SPEC 8.4, packages/farm/src/domain/breed-genome.ts). FarmChain calls them proposals, not biological constants.
 */
public record Ruleset(String version, Map<Trait, Integer> heterosisMaxBp, Map<Trait, Integer> inbreedingPenaltyMaxBp, int samplingMagnitude) {
	public static final String VERSION_1 = "farmings.ruleset.1";

	public static Ruleset defaults() {
		Map<Trait, Integer> het = new EnumMap<>(Trait.class);
		het.put(Trait.MILK_YIELD, 200);
		het.put(Trait.BUTTERFAT, 0);
		het.put(Trait.PROTEIN, 0);
		het.put(Trait.FEED_EFFICIENCY, 200);
		het.put(Trait.HEALTH, 600);
		het.put(Trait.FERTILITY, 800);
		het.put(Trait.LONGEVITY, 600);
		het.put(Trait.TEMPERAMENT, 100);
		// Sheep traits (proposed, 2026-11-10: wool yield like milk yield, fleece quality like the milk components, hardiness a
		// fitness trait like health).
		het.put(Trait.WOOL_YIELD, 200);
		het.put(Trait.FLEECE_QUALITY, 0);
		het.put(Trait.HARDINESS, 600);
		// Poultry traits (proposed, 2026-11-11: egg production like milk yield, egg weight like the milk components, broodiness like
		// temperament).
		het.put(Trait.EGG_PRODUCTION, 200);
		het.put(Trait.EGG_WEIGHT, 0);
		het.put(Trait.BROODINESS, 100);
		// Racing speed and stamina (horses.md, proposed: racing speed is weakly heritable, so a small heterosis as broodiness).
		het.put(Trait.SPEED, 100);
		het.put(Trait.STAMINA, 100);
		Map<Trait, Integer> inbreeding = new EnumMap<>(Trait.class);
		for (Trait t : Trait.values()) {
			inbreeding.put(t, 500);
		}
		return new Ruleset(VERSION_1, het, inbreeding, 30);
	}
}
