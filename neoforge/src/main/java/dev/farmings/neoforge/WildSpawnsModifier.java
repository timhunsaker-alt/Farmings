package dev.farmings.neoforge;

import com.mojang.serialization.MapCodec;
import dev.farmings.entity.WildSpawns;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.MobSpawnSettingsBuilder;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/** Swaps vanilla cows, sheep and chickens for Farmings animals in every biome, keeping weights and group sizes (docs/taming.md). */
public record WildSpawnsModifier() implements BiomeModifier {
	public static final MapCodec<WildSpawnsModifier> CODEC = MapCodec.unit(WildSpawnsModifier::new);

	@Override
	public void modify(RegistryAccess registries, Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		if (phase != Phase.REMOVE) {
			return;
		}
		MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();
		List<Weighted<SpawnerData>> add = new ArrayList<>();
		String name = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
		spawns.removeSpawns(w -> {
			List<Weighted<SpawnerData>> r = WildSpawns.replacements(w, name);
			add.addAll(r);
			return !r.isEmpty();
		});
		add.addAll(WildSpawns.extra(name));
		for (Weighted<SpawnerData> w : add) {
			spawns.addSpawn(w.value().type(), w.weight(), w.value().count());
		}
	}

	@Override
	public MapCodec<? extends BiomeModifier> codec() {
		return CODEC;
	}
}
