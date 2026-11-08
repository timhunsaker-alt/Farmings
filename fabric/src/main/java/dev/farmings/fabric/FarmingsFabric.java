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
	}
}
