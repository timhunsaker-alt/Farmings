package dev.farmings.fabric.client;

import dev.farmings.FarmingsEntities;
import dev.farmings.client.CattleModels;
import dev.farmings.client.CattleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** The Fabric client entry point. */
public final class FarmingsFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(CattleModels.GYR, CattleModels::createGyrLayer);
		EntityRendererRegistry.register(FarmingsEntities.CATTLE, CattleRenderer::new);
	}
}
