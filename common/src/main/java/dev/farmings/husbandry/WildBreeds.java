package dev.farmings.husbandry;

import dev.farmings.genetics.Species;
import dev.farmings.husbandry.poultry.FowlBreed;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Which breeds spawn wild in which vanilla biome (docs/taming.md, "Breed by biome"). The climates are sourced there; the mapping is a
 * game choice (proposed). Within a group each listed breed is equally likely.
 */
public final class WildBreeds {
	private WildBreeds() {
	}

	public enum Group {
		HOT_DRY(List.of(CattleBreed.GYR), List.of(SheepBreed.DORPER, SheepBreed.MERINO)),
		HOT_WET(List.of(CattleBreed.GYR), List.of(SheepBreed.DORPER)),
		MOUNTAIN(List.of(CattleBreed.BROWN_SWISS), List.of(SheepBreed.MERINO)),
		COLD(List.of(CattleBreed.ANGUS, CattleBreed.BROWN_SWISS), List.of(SheepBreed.SUFFOLK)),
		TEMPERATE(List.of(CattleBreed.HOLSTEIN, CattleBreed.JERSEY, CattleBreed.ANGUS), List.of(SheepBreed.SUFFOLK, SheepBreed.EAST_FRIESIAN)),
		WET_LOWLAND(List.of(CattleBreed.HOLSTEIN), List.of(SheepBreed.EAST_FRIESIAN)),
		CHERRY_GROVE(List.of(CattleBreed.WAGYU), List.of(SheepBreed.MERINO));

		public final List<CattleBreed> cattle;
		public final List<SheepBreed> sheep;

		Group(List<CattleBreed> cattle, List<SheepBreed> sheep) {
			this.cattle = cattle;
			this.sheep = sheep;
		}
	}

	private static final Map<String, Group> BIOMES = Map.ofEntries(
			Map.entry("savanna", Group.HOT_DRY), Map.entry("savanna_plateau", Group.HOT_DRY), Map.entry("windswept_savanna", Group.HOT_DRY),
			Map.entry("badlands", Group.HOT_DRY), Map.entry("eroded_badlands", Group.HOT_DRY), Map.entry("wooded_badlands", Group.HOT_DRY),
			Map.entry("jungle", Group.HOT_WET), Map.entry("sparse_jungle", Group.HOT_WET), Map.entry("bamboo_jungle", Group.HOT_WET),
			Map.entry("windswept_hills", Group.MOUNTAIN), Map.entry("windswept_gravelly_hills", Group.MOUNTAIN),
			Map.entry("windswept_forest", Group.MOUNTAIN), Map.entry("meadow", Group.MOUNTAIN),
			Map.entry("taiga", Group.COLD), Map.entry("snowy_taiga", Group.COLD), Map.entry("old_growth_pine_taiga", Group.COLD),
			Map.entry("old_growth_spruce_taiga", Group.COLD),
			Map.entry("swamp", Group.WET_LOWLAND),
			Map.entry("cherry_grove", Group.CHERRY_GROVE));

	/** The group for a biome path such as "plains" (namespace dropped). Biomes not listed, including modded ones, count as temperate. */
	public static Group group(String biome) {
		int colon = biome.indexOf(':');
		String path = colon >= 0 ? biome.substring(colon + 1) : biome;
		return BIOMES.getOrDefault(path, Group.TEMPERATE);
	}

	/** A cattle breed for the biome; roll is any non-negative int (a random draw). */
	public static CattleBreed cattle(String biome, int roll) {
		List<CattleBreed> l = group(biome).cattle;
		return l.get(Math.floorMod(roll, l.size()));
	}

	public static SheepBreed sheep(String biome, int roll) {
		List<SheepBreed> l = group(biome).sheep;
		return l.get(Math.floorMod(roll, l.size()));
	}

	// ---- wild fowl (docs/animals/poultry.md, "Wild fowl"; the mapping is proposed, the habitats are sourced there) ----

	private static final List<FowlBreed> JUNGLE = List.of(FowlBreed.SILKIE);
	private static final List<FowlBreed> FOREST = List.of(FowlBreed.BRONZE, FowlBreed.BOURBON_RED);
	private static final List<FowlBreed> TAIGA = List.of(FowlBreed.BRONZE);
	private static final List<FowlBreed> PLAINS = List.of(FowlBreed.BOBWHITE, FowlBreed.RHODE_ISLAND_RED, FowlBreed.PLYMOUTH_ROCK);
	private static final List<FowlBreed> SAVANNA = List.of(FowlBreed.COTURNIX);
	private static final List<FowlBreed> DRY = List.of(FowlBreed.LEGHORN);
	private static final List<FowlBreed> HILLS = List.of(FowlBreed.PLYMOUTH_ROCK, FowlBreed.BOURBON_RED);
	private static final List<FowlBreed> SWAMP = List.of(FowlBreed.MALLARD, FowlBreed.MUSCOVY);
	private static final List<FowlBreed> RIVER = List.of(FowlBreed.MALLARD, FowlBreed.PEKIN, FowlBreed.KHAKI_CAMPBELL);
	private static final List<FowlBreed> MANGROVE = List.of(FowlBreed.MUSCOVY);
	/** Biomes not listed, modded ones included, get the farmyard chickens of the plains. */
	private static final List<FowlBreed> OTHER = List.of(FowlBreed.RHODE_ISLAND_RED, FowlBreed.PLYMOUTH_ROCK);

	private static final Map<String, List<FowlBreed>> FOWL = Map.ofEntries(
			Map.entry("jungle", JUNGLE), Map.entry("sparse_jungle", JUNGLE), Map.entry("bamboo_jungle", JUNGLE),
			Map.entry("forest", FOREST), Map.entry("flower_forest", FOREST), Map.entry("birch_forest", FOREST),
			Map.entry("old_growth_birch_forest", FOREST), Map.entry("dark_forest", FOREST), Map.entry("dappled_forest", FOREST),
			Map.entry("taiga", TAIGA), Map.entry("snowy_taiga", TAIGA), Map.entry("old_growth_pine_taiga", TAIGA),
			Map.entry("old_growth_spruce_taiga", TAIGA),
			Map.entry("plains", PLAINS), Map.entry("sunflower_plains", PLAINS),
			Map.entry("savanna", SAVANNA), Map.entry("savanna_plateau", SAVANNA), Map.entry("windswept_savanna", SAVANNA),
			Map.entry("badlands", DRY), Map.entry("eroded_badlands", DRY), Map.entry("wooded_badlands", DRY),
			Map.entry("windswept_hills", HILLS), Map.entry("windswept_gravelly_hills", HILLS), Map.entry("windswept_forest", HILLS),
			Map.entry("swamp", SWAMP), Map.entry("river", RIVER), Map.entry("mangrove_swamp", MANGROVE));

	private static String path(String biome) {
		int colon = biome.indexOf(':');
		return colon >= 0 ? biome.substring(colon + 1) : biome;
	}

	/** The wild fowl breeds of a biome; each is equally likely. */
	public static List<FowlBreed> fowlBreeds(String biome) {
		return FOWL.getOrDefault(path(biome), OTHER);
	}

	/** A wild breed of this species for the biome, or null if the species doesn't live there. */
	public static FowlBreed fowl(Species species, String biome, int roll) {
		List<FowlBreed> l = new ArrayList<>();
		for (FowlBreed b : fowlBreeds(biome)) {
			if (b.species == species) {
				l.add(b);
			}
		}
		return l.isEmpty() ? null : l.get(Math.floorMod(roll, l.size()));
	}

	/**
	 * How a vanilla chicken entry's weight is shared between the species of the biome: by their share of its breeds, rounded, at least
	 * 1 each (proposed). Plains (one quail breed, two chicken breeds) turn a weight of 10 into quail 3 and chicken 7.
	 */
	public static Map<Species, Integer> fowlWeights(String biome, int weight) {
		List<FowlBreed> l = fowlBreeds(biome);
		Map<Species, Integer> count = new EnumMap<>(Species.class);
		for (FowlBreed b : l) {
			count.merge(b.species, 1, Integer::sum);
		}
		Map<Species, Integer> out = new EnumMap<>(Species.class);
		count.forEach((sp, n) -> out.put(sp, Math.max(1, (int) Math.round(weight * (double) n / l.size()))));
		return out;
	}

	/** Real days a wild animal must spend fed and watered in a corral before it is tamed (docs/taming.md: FutureBeef, 5-10 days). */
	public static final double SETTLE_DAYS = 7;
	/** How far a wild animal keeps from a player who holds no feed (proposed). */
	public static final double FLIGHT_BLOCKS = 6;
}
