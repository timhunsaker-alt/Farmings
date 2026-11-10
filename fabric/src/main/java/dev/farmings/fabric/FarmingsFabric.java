package dev.farmings.fabric;

import dev.farmings.Farmings;
import dev.farmings.FarmingsEntities;
import dev.farmings.entity.CattleEntity;
import dev.wildcatter.Wildcatter;
import dev.farmings.FarmingsBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/** The Fabric entry point. Wildcatter is a hard dependency and a compile dependency. */
public final class FarmingsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Farmings.register(Wildcatter.MOD_ID);
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Farmings.id("farmings"), FabricCreativeModeTab.builder()
				.title(FarmingsBlocks.tabTitle())
				.icon(FarmingsBlocks::tabIcon)
				.displayItems((parameters, output) -> FarmingsBlocks.creativeTab(output::accept))
				.build());
		FabricDefaultAttributeRegistry.register(FarmingsEntities.CATTLE, CattleEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(FarmingsEntities.SHEEP, dev.farmings.entity.SheepEntity.createAttributes());
		// wild animals replace vanilla cows and sheep, same weights and group sizes (docs/taming.md)
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.create(Farmings.id("wild_spawns")).add(net.fabricmc.fabric.api.biome.v1.ModificationPhase.REPLACEMENTS,
				sel -> true, (sel, ctx) -> {
					var spawns = ctx.getMobSpawnSettings();
					java.util.List<net.minecraft.util.random.Weighted<net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData>> add = new java.util.ArrayList<>();
					for (var w : spawns.getMobs(net.minecraft.world.entity.MobCategory.CREATURE)) {
						var r = dev.farmings.entity.WildSpawns.replacement(w);
						if (r != null) {
							add.add(r);
						}
					}
					add.addAll(dev.farmings.entity.WildSpawns.extra(sel.getBiomeKey().identifier().toString()));
					if (add.isEmpty()) {
						return;
					}
					spawns.removeSpawns((cat, data) -> dev.farmings.entity.WildSpawns.replaced(data.type()));
					for (var w : add) {
						spawns.addSpawn(net.minecraft.world.entity.MobCategory.CREATURE, w.value(), w.weight());
					}
				});
	}
}
