package dev.farmings.genetics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The shared pool of core-trait alleles: seven per trait, MY_A0 to MY_A6 and so on, with the effect ladder
 * -45, -30, -15, 0, 15, 30, 45 on the 0-1000 scale (FarmChain breed-genome.ts).
 */
public final class AlleleRegistry {
	public static final int[] EFFECTS = {-45, -30, -15, 0, 15, 30, 45};

	private static final Map<String, Integer> EFFECT_BY_ID = new HashMap<>();

	static {
		for (Trait t : Trait.values()) {
			for (int i = 0; i < EFFECTS.length; i++) {
				EFFECT_BY_ID.put(alleleId(t, i), EFFECTS[i]);
			}
		}
	}

	private AlleleRegistry() {
	}

	public static String alleleId(Trait trait, int index) {
		return trait.code + "_A" + index;
	}

	public static List<String> pool(Trait trait) {
		List<String> ids = new ArrayList<>();
		for (int i = 0; i < EFFECTS.length; i++) {
			ids.add(alleleId(trait, i));
		}
		return ids;
	}

	/** The effect of an allele; 0 for an id that is not in the pool (as in FarmChain). */
	public static int effect(String alleleId) {
		return EFFECT_BY_ID.getOrDefault(alleleId, 0);
	}
}
