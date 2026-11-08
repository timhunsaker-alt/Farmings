package dev.farmings.fabric;

import dev.farmings.Farmings;
import dev.farmings.FarmingsEntities;
import dev.farmings.entity.CattleEntity;
import dev.wildcatter.Wildcatter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/** The Fabric entry point. Wildcatter is a hard dependency and a compile dependency. */
public final class FarmingsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Farmings.register(Wildcatter.MOD_ID);
		FabricDefaultAttributeRegistry.register(FarmingsEntities.CATTLE, CattleEntity.createAttributes());
	}
}
