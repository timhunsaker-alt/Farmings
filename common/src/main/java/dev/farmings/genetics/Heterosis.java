package dev.farmings.genetics;

import java.util.Map;

/**
 * Hybrid vigor (BREED-SPEC 8.4 as FarmChain implements it): the coefficient is how far apart the parents' breed compositions
 * are. 0 for identical compositions, 10,000 for two unrelated pure breeds. Fitness traits gain most.
 */
public final class Heterosis {
	private Heterosis() {
	}

	public static int coefficientBp(Map<String, Integer> dam, Map<String, Integer> sire) {
		int overlap = 0;
		for (Map.Entry<String, Integer> e : dam.entrySet()) {
			overlap += Math.min(e.getValue(), sire.getOrDefault(e.getKey(), 0));
		}
		return Math.max(0, Math.min(10_000, 10_000 - overlap));
	}

	public static int perTraitBonusBp(int coefficientBp, Trait trait, Ruleset rules) {
		int c = Math.max(0, Math.min(10_000, coefficientBp));
		return (int) Math.round((double) c * rules.heterosisMaxBp().getOrDefault(trait, 0) / 10_000.0);
	}
}
