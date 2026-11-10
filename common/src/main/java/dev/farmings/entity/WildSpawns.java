package dev.farmings.entity;

import dev.farmings.FarmingsEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import org.jspecify.annotations.Nullable;

/**
 * Farmings animals replace vanilla cows and sheep in the world's spawn lists (docs/taming.md, "Spawns"). Each vanilla entry is swapped
 * for the Farmings species with the same weight and group size, so every biome that had cows or sheep, modded ones too, keeps its numbers.
 * Both loaders call this from their biome modification hooks.
 */
public final class WildSpawns {
	private WildSpawns() {
	}

	/** The Farmings entry that replaces a vanilla one, or null if the entry is not a cow or sheep. */
	public static @Nullable Weighted<SpawnerData> replacement(Weighted<SpawnerData> entry) {
		EntityType<?> t = entry.value().type();
		if (t == net.minecraft.world.entity.EntityTypes.COW) {
			return new Weighted<>(new SpawnerData(FarmingsEntities.CATTLE, entry.value().count()), entry.weight());
		}
		if (t == net.minecraft.world.entity.EntityTypes.SHEEP) {
			return new Weighted<>(new SpawnerData(FarmingsEntities.SHEEP, entry.value().count()), entry.weight());
		}
		return null;
	}

	public static boolean replaced(EntityType<?> t) {
		return t == net.minecraft.world.entity.EntityTypes.COW || t == net.minecraft.world.entity.EntityTypes.SHEEP;
	}

	/** Entries added outright: Wagyu in the cherry grove, as the sheep there (weight 2, groups of 2-4). */
	public static List<Weighted<SpawnerData>> extra(String biome) {
		List<Weighted<SpawnerData>> l = new ArrayList<>();
		if (biome.equals("minecraft:cherry_grove")) {
			l.add(new Weighted<>(new SpawnerData(FarmingsEntities.CATTLE, UniformInt.of(2, 4)), 2));
		}
		return l;
	}
}
