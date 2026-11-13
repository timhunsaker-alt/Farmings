package dev.farmings.entity;

import dev.farmings.FarmingsEntities;
import dev.farmings.genetics.Species;
import dev.farmings.husbandry.WildBreeds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

/**
 * Farmings animals replace vanilla cows, sheep, goats and chickens in the world's spawn lists (docs/taming.md, "Spawns"; docs/animals/poultry.md,
 * "Wild fowl"). Each vanilla entry is swapped for the Farmings species with the same weight and group size, so every biome that had them,
 * modded ones too, keeps its numbers. A chicken entry is shared between the bird species of the biome. Both loaders call this from their
 * biome modification hooks.
 */
public final class WildSpawns {
	private WildSpawns() {
	}

	/** The Farmings entries that replace a vanilla one in a biome; empty if the entry is not a cow, sheep or chicken. */
	public static List<Weighted<SpawnerData>> replacements(Weighted<SpawnerData> entry, String biome) {
		EntityType<?> t = entry.value().type();
		List<Weighted<SpawnerData>> out = new ArrayList<>();
		if (t == net.minecraft.world.entity.EntityTypes.COW) {
			out.add(new Weighted<>(new SpawnerData(FarmingsEntities.CATTLE, entry.value().count()), entry.weight()));
		} else if (t == net.minecraft.world.entity.EntityTypes.SHEEP) {
			out.add(new Weighted<>(new SpawnerData(FarmingsEntities.SHEEP, entry.value().count()), entry.weight()));
		} else if (t == net.minecraft.world.entity.EntityTypes.PIG) {
			out.add(new Weighted<>(new SpawnerData(FarmingsEntities.PIG, entry.value().count()), entry.weight()));     // pigs.md, "Wild pigs"
		} else if (t == net.minecraft.world.entity.EntityTypes.GOAT) {
			out.add(new Weighted<>(new SpawnerData(FarmingsEntities.GOAT, entry.value().count()), entry.weight()));     // goats.md, "Wild goats"
		} else if (t == net.minecraft.world.entity.EntityTypes.CHICKEN) {
			for (Map.Entry<Species, Integer> e : WildBreeds.fowlWeights(biome, entry.weight()).entrySet()) {
				out.add(new Weighted<>(new SpawnerData(FarmingsEntities.fowl(e.getKey()), entry.value().count()), e.getValue()));
			}
		}
		return out;
	}

	public static boolean replaced(EntityType<?> t) {
		return t == net.minecraft.world.entity.EntityTypes.COW || t == net.minecraft.world.entity.EntityTypes.SHEEP
				|| t == net.minecraft.world.entity.EntityTypes.CHICKEN || t == net.minecraft.world.entity.EntityTypes.GOAT
				|| t == net.minecraft.world.entity.EntityTypes.PIG;
	}

	/**
	 * Entries added outright: Wagyu in the cherry grove, as the sheep there (weight 2, groups of 2-4); ducks on rivers and Muscovies in
	 * mangrove swamps, as a chicken entry (weight 10, groups of 4; poultry.md).
	 */
	public static List<Weighted<SpawnerData>> extra(String biome) {
		List<Weighted<SpawnerData>> l = new ArrayList<>();
		if (biome.equals("minecraft:cherry_grove")) {
			l.add(new Weighted<>(new SpawnerData(FarmingsEntities.CATTLE, UniformInt.of(2, 4)), 2));
		}
		// sheep dogs, in pairs in the sheep biomes (docs/taming.md, "the sheep dog"; proposed weight)
		if (biome.equals("minecraft:plains") || biome.equals("minecraft:meadow") || biome.equals("minecraft:windswept_hills")) {
			l.add(new Weighted<>(new SpawnerData(FarmingsEntities.SHEEP_DOG, ConstantInt.of(2)), 2));
		}
		// wild geese in groups of 4 (poultry.md, "Wild geese and guinea fowl"; weight 6, proposed)
		if (!WildBreeds.geese(biome).isEmpty()) {
			l.add(new Weighted<>(new SpawnerData(FarmingsEntities.GOOSE, ConstantInt.of(4)), 6));
		}
		// wild guinea fowl in the savannas, groups of 4 (weight 6, proposed)
		if (WildBreeds.guineaFowl(biome)) {
			l.add(new Weighted<>(new SpawnerData(FarmingsEntities.GUINEA, ConstantInt.of(4)), 6));
		}
		if (biome.equals("minecraft:river") || biome.equals("minecraft:mangrove_swamp")) {
			l.add(new Weighted<>(new SpawnerData(FarmingsEntities.DUCK, ConstantInt.of(4)), 10));
		}
		return l;
	}
}
