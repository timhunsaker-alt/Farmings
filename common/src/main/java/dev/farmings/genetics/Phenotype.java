package dev.farmings.genetics;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The expressed trait scores (0-1000) plus the hybrid-vigor and inbreeding measures that went into them, in basis points.
 * Computed once when the animal is born and stored on it (FarmChain does the same).
 */
public record Phenotype(Map<Trait, Integer> traitScores, int heterosisBp, int inbreedingBp) {
	public Phenotype {
		traitScores = Collections.unmodifiableMap(new EnumMap<>(traitScores));
	}

	public int score(Trait trait) {
		return traitScores.getOrDefault(trait, 0);
	}

	/**
	 * Trait expression (BREED-SPEC 8.5): weighted breed baseline + mean allele effect + sampling, then heterosis, minus the
	 * inbreeding penalty, clamped to 0-1000. Consumes one RNG value per trait, in {@link Trait} order.
	 */
	public static Phenotype express(Genome genome, int heterosisBp, int inbreedingBp, Ruleset rules, Rng rng) {
		Map<Trait, Integer> scores = new EnumMap<>(Trait.class);
		for (Trait trait : genome.species().traits()) {
			double acc = 0;
			for (Map.Entry<String, Integer> e : genome.breedCompositionBp().entrySet()) {
				acc += (double) e.getValue() * Breeds.baseline(e.getKey(), trait);
			}
			long baseline = Math.round(acc / 10_000.0 * 10.0);

			AllelePair pair = genome.coreGenome().get(trait);
			int effA = pair == null ? 0 : AlleleRegistry.effect(pair.first());
			int effB = pair == null ? 0 : AlleleRegistry.effect(pair.second());
			long alleleEffect = Math.round((effA + effB) / 2.0);

			long sample = Math.round((rng.next() * 2 - 1) * rules.samplingMagnitude());
			long raw = baseline + alleleEffect + sample;

			long hetBonus = Math.round(raw * (double) Heterosis.perTraitBonusBp(heterosisBp, trait, rules) / 10_000.0);
			long inbrPenalty = Math.round(raw * (double) Inbreeding.perTraitPenaltyBp(inbreedingBp, trait, rules) / 10_000.0);

			long value = Math.max(0, Math.min(1000, raw + hetBonus - inbrPenalty));
			scores.put(trait, (int) value);
		}
		return new Phenotype(scores, heterosisBp, inbreedingBp);
	}
}
