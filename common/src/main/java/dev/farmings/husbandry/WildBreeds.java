package dev.farmings.husbandry;

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

	/** Real days a wild animal must spend fed and watered in a corral before it is tamed (docs/taming.md: FutureBeef, 5-10 days). */
	public static final double SETTLE_DAYS = 7;
	/** How far a wild animal keeps from a player who holds no feed (proposed). */
	public static final double FLIGHT_BLOCKS = 6;
}
