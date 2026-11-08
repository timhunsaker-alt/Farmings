package dev.farmings.genetics;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Generation-0 animals: a purebred of one breed with alleles drawn from the shared pool. This is new in Farmings. FarmChain
 * seeds its founders elsewhere; here a founder's alleles are drawn uniformly from the seven in each trait's pool.
 */
public final class Founders {
	private Founders() {
	}

	public record Result(Sex sex, Genome genome, Phenotype phenotype) {
	}

	public static Result create(String breedCode, Sex sex, long seed, Ruleset rules) {
		if (!Breeds.known(breedCode)) {
			throw new IllegalArgumentException("no baseline scores for breed " + breedCode);
		}
		Rng rng = new Rng(seed);
		Map<Trait, AllelePair> core = new EnumMap<>(Trait.class);
		for (Trait t : Trait.values()) {
			List<String> pool = AlleleRegistry.pool(t);
			String a = pool.get((int) Math.floor(rng.next() * pool.size()));
			String b = pool.get((int) Math.floor(rng.next() * pool.size()));
			core.put(t, new AllelePair(a, b));
		}
		Genome genome = new Genome(Genome.SCHEMA_V1, 0, BreedComposition.purebred(breedCode), core, rules.version());
		Phenotype phenotype = Phenotype.express(genome, 0, 0, rules, rng);
		return new Result(sex, genome, phenotype);
	}
}
