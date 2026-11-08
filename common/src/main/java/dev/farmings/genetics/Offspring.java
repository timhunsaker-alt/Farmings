package dev.farmings.genetics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Makes one calf from a dam and a sire, deterministically (FarmChain create-offspring.ts). Random numbers are used in a fixed
 * order: sex, then one dam allele and one sire allele per core locus in sorted locus-id order, then one sampling value per trait.
 * The same seed and parents always give the same calf.
 */
public final class Offspring {
	public record Result(Sex sex, Genome genome, Phenotype phenotype) {
	}

	private Offspring() {
	}

	/**
	 * @param forcedSex when not null, the calf has this sex whatever the roll (the tutorial rule: a player's first two
	 *                  calves are heifers, the third a bull). The roll is still consumed so every other result is unchanged.
	 */
	public static Result create(Genome dam, Genome sire, String eventId, String worldKey, Ruleset rules, Sex forcedSex) {
		long seed = BreedingSeed.seed(worldKey, eventId, dam.hash(), sire.hash(), rules.version());
		return create(dam, sire, seed, rules, forcedSex);
	}

	public static Result create(Genome dam, Genome sire, long seed, Ruleset rules, Sex forcedSex) {
		Rng rng = new Rng(seed);

		Sex rolled = rng.next() < 0.5 ? Sex.FEMALE : Sex.MALE;
		Sex sex = forcedSex != null ? forcedSex : rolled;

		Map<String, Integer> composition = BreedComposition.average(dam.breedCompositionBp(), sire.breedCompositionBp());

		List<Trait> order = new ArrayList<>(List.of(Trait.values()));
		order.sort(Comparator.comparing(t -> t.id));
		Map<Trait, AllelePair> core = new EnumMap<>(Trait.class);
		for (Trait t : order) {
			AllelePair d = dam.coreGenome().get(t);
			AllelePair s = sire.coreGenome().get(t);
			if (d == null || s == null) {
				continue;
			}
			String fromDam = rng.next() < 0.5 ? d.first() : d.second();
			String fromSire = rng.next() < 0.5 ? s.first() : s.second();
			core.put(t, new AllelePair(fromDam, fromSire));
		}

		int het = Heterosis.coefficientBp(dam.breedCompositionBp(), sire.breedCompositionBp());
		int inbr = Inbreeding.relatednessBp(dam.coreGenome(), sire.coreGenome());

		Genome genome = new Genome(Genome.SCHEMA_V1, Math.max(dam.generation(), sire.generation()) + 1, composition, core, rules.version());
		Phenotype phenotype = Phenotype.express(genome, het, inbr, rules, rng);
		return new Result(sex, genome, phenotype);
	}
}
