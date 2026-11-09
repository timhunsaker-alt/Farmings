package dev.farmings.genetics;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Breed baseline scores on FarmChain's 0-100 game-balance scale (BREED-SPEC 6.1). They are balance targets, not real-world
 * breed averages. Holstein, Jersey, Brown Swiss and Gyr come from FarmChain. Angus and Wagyu are not in FarmChain: their scores are
 * proposed (2026-11-09, docs/animals/cattle.md), set from the direction of the real differences: little milk (beef cows only feed
 * their calf), Angus more feed-efficient and fertile, Wagyu longer-lived and calmer. Their beef traits are in husbandry.Carcass.
 */
public final class Breeds {
	private static final Map<String, Map<Trait, Integer>> BASELINES = new LinkedHashMap<>();

	static {
		add("HOL", 95, 45, 50, 55, 45, 40, 50, 65);
		add("JER", 65, 95, 90, 90, 70, 80, 80, 75);
		add("BSW", 82, 70, 78, 75, 82, 70, 90, 88);
		add("GYR", 58, 78, 74, 86, 92, 88, 86, 72);
		// Proposed, no source (FarmChain has no beef breeds).
		add("ANG", 20, 50, 50, 75, 80, 80, 75, 70);
		add("WAG", 15, 55, 50, 55, 75, 70, 85, 85);
	}

	private Breeds() {
	}

	private static void add(String code, int milk, int fat, int protein, int efficiency, int health, int fertility, int longevity, int temperament) {
		Map<Trait, Integer> m = new EnumMap<>(Trait.class);
		m.put(Trait.MILK_YIELD, milk);
		m.put(Trait.BUTTERFAT, fat);
		m.put(Trait.PROTEIN, protein);
		m.put(Trait.FEED_EFFICIENCY, efficiency);
		m.put(Trait.HEALTH, health);
		m.put(Trait.FERTILITY, fertility);
		m.put(Trait.LONGEVITY, longevity);
		m.put(Trait.TEMPERAMENT, temperament);
		BASELINES.put(code, m);
	}

	public static boolean known(String code) {
		return BASELINES.containsKey(code);
	}

	/** The 0-100 baseline for a trait, or 0 for a breed with no baseline (as in FarmChain). */
	public static int baseline(String code, Trait trait) {
		Map<Trait, Integer> m = BASELINES.get(code);
		return m == null ? 0 : m.getOrDefault(trait, 0);
	}

	public static Iterable<String> codes() {
		return BASELINES.keySet();
	}
}
