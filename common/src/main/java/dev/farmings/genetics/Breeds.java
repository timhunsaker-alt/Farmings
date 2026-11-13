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
	private static final Map<String, Species> SPECIES = new LinkedHashMap<>();

	static {
		add("HOL", 95, 45, 50, 55, 45, 40, 50, 65);
		add("JER", 65, 95, 90, 90, 70, 80, 80, 75);
		add("BSW", 82, 70, 78, 75, 82, 70, 90, 88);
		add("GYR", 58, 78, 74, 86, 92, 88, 86, 72);
		// Proposed, no source (FarmChain has no beef breeds).
		add("ANG", 20, 50, 50, 75, 80, 80, 75, 70);
		add("WAG", 15, 55, 50, 55, 75, 70, 85, 85);
		// Sheep: all proposed (2026-11-10). FarmChain has a sheep gene list but no sheep breeds. Set from the direction of the real
		// differences in docs/animals/sheep.md: Merino the best fleece and fewest lambs, Suffolk a meat breed with a medium fleece,
		// Dorper a hardy hair sheep with almost no wool, East Friesian the milk breed with the most lambs.
		//      code   wool fleece eff  health fert long temp hardy milk
		sheep("MER", 95, 95, 60, 60, 45, 70, 70, 65, 20);
		sheep("SUF", 45, 45, 75, 70, 75, 60, 65, 60, 30);
		sheep("DOR", 10, 10, 85, 85, 70, 75, 75, 95, 30);
		sheep("EFR", 55, 50, 60, 55, 90, 60, 80, 45, 95);
		// Goat baselines (docs/animals/goats.md) *(proposed: 0-100 scores set from the breed table, as the sheep baselines)*:
		// milk, fat, protein, efficiency, health, fertility, longevity, temperament, hardiness.
		goat("SAA", 95, 40, 40, 60, 55, 70, 65, 80, 50);
		goat("NUB", 70, 85, 85, 60, 60, 70, 65, 70, 60);
		goat("BOE", 10, 50, 50, 80, 75, 80, 65, 75, 80);
		// Poultry: all proposed (2026-11-11), set from the direction of the real differences in docs/animals/poultry.md (eggs a
		// year, egg size, broodiness, the broad-breasted turkey's poor fertility, the Silkie's calm, the Mallard's wildness).
		//    species          code   eff health fert long temp eggs egg-wt broody
		fowl(Species.CHICKEN, "LEG", 85, 70, 75, 60, 45, 95, 70, 5);
		fowl(Species.CHICKEN, "RIR", 75, 85, 80, 70, 65, 85, 72, 60);
		fowl(Species.CHICKEN, "PLR", 70, 85, 75, 75, 75, 70, 72, 65);
		fowl(Species.CHICKEN, "SLK", 50, 65, 60, 80, 95, 30, 35, 98);
		fowl(Species.TURKEY, "BBW", 90, 50, 30, 40, 70, 70, 72, 10);
		fowl(Species.TURKEY, "BRZ", 65, 75, 70, 70, 60, 50, 72, 80);
		fowl(Species.TURKEY, "BRD", 60, 80, 70, 75, 65, 35, 72, 80);
		fowl(Species.QUAIL, "COT", 85, 70, 85, 40, 60, 90, 70, 5);
		fowl(Species.QUAIL, "BOB", 55, 70, 65, 50, 30, 45, 65, 70);
		fowl(Species.DUCK, "PEK", 85, 75, 70, 60, 70, 50, 70, 10);
		fowl(Species.DUCK, "KHC", 75, 80, 75, 65, 65, 90, 70, 15);
		fowl(Species.DUCK, "MAL", 50, 85, 70, 70, 25, 15, 60, 90);
		fowl(Species.DUCK, "MUS", 70, 85, 70, 75, 60, 35, 70, 90);
	}

	private static void fowl(Species species, String code, int efficiency, int health, int fertility, int longevity, int temperament, int eggs,
			int eggWeight, int broodiness) {
		Map<Trait, Integer> m = new EnumMap<>(Trait.class);
		m.put(Trait.FEED_EFFICIENCY, efficiency);
		m.put(Trait.HEALTH, health);
		m.put(Trait.FERTILITY, fertility);
		m.put(Trait.LONGEVITY, longevity);
		m.put(Trait.TEMPERAMENT, temperament);
		m.put(Trait.EGG_PRODUCTION, eggs);
		m.put(Trait.EGG_WEIGHT, eggWeight);
		m.put(Trait.BROODINESS, broodiness);
		BASELINES.put(code, m);
		SPECIES.put(code, species);
	}

	private static void sheep(String code, int wool, int fleece, int efficiency, int health, int fertility, int longevity, int temperament, int hardiness,
			int milk) {
		Map<Trait, Integer> m = new EnumMap<>(Trait.class);
		m.put(Trait.WOOL_YIELD, wool);
		m.put(Trait.FLEECE_QUALITY, fleece);
		m.put(Trait.FEED_EFFICIENCY, efficiency);
		m.put(Trait.HEALTH, health);
		m.put(Trait.FERTILITY, fertility);
		m.put(Trait.LONGEVITY, longevity);
		m.put(Trait.TEMPERAMENT, temperament);
		m.put(Trait.HARDINESS, hardiness);
		m.put(Trait.MILK_YIELD, milk);
		BASELINES.put(code, m);
		SPECIES.put(code, Species.SHEEP);
	}

	private static void goat(String code, int milk, int fat, int protein, int efficiency, int health, int fertility, int longevity, int temperament,
			int hardiness) {
		Map<Trait, Integer> m = new EnumMap<>(Trait.class);
		m.put(Trait.MILK_YIELD, milk);
		m.put(Trait.BUTTERFAT, fat);
		m.put(Trait.PROTEIN, protein);
		m.put(Trait.FEED_EFFICIENCY, efficiency);
		m.put(Trait.HEALTH, health);
		m.put(Trait.FERTILITY, fertility);
		m.put(Trait.LONGEVITY, longevity);
		m.put(Trait.TEMPERAMENT, temperament);
		m.put(Trait.HARDINESS, hardiness);
		BASELINES.put(code, m);
		SPECIES.put(code, Species.GOAT);
	}

	/** The species a breed code belongs to; throws on a code no species registered, so a new breed can't fall into cattle. */
	public static Species species(String code) {
		Species s = SPECIES.get(code);
		if (s == null) {
			throw new IllegalArgumentException("not a Farmings breed code: " + code);
		}
		return s;
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
		SPECIES.put(code, Species.CATTLE);
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
