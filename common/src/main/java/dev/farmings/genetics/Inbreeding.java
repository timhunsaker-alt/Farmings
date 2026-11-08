package dev.farmings.genetics;

import java.util.Map;

/**
 * Inbreeding depression (BREED-SPEC 8.4): the more core alleles the parents share, the larger the penalty. It never
 * becomes a bonus.
 */
public final class Inbreeding {
	private Inbreeding() {
	}

	/** 0 when the parents share no core alleles, 10,000 when every one is shared. */
	public static int relatednessBp(Map<Trait, AllelePair> dam, Map<Trait, AllelePair> sire) {
		int shared = 0;
		int total = 0;
		for (Trait t : Trait.values()) {
			AllelePair d = dam.get(t);
			AllelePair s = sire.get(t);
			if (d == null || s == null) {
				continue;
			}
			total += 2;
			if (d.contains(s.first())) {
				shared++;
			}
			if (d.contains(s.second())) {
				shared++;
			}
		}
		if (total == 0) {
			return 0;
		}
		return Math.max(0, Math.min(10_000, (int) Math.round((double) shared / total * 10_000.0)));
	}

	public static int perTraitPenaltyBp(int relatednessBp, Trait trait, Ruleset rules) {
		int r = Math.max(0, Math.min(10_000, relatednessBp));
		return Math.max(0, (int) Math.round((double) r * rules.inbreedingPenaltyMaxBp().getOrDefault(trait, 0) / 10_000.0));
	}
}
