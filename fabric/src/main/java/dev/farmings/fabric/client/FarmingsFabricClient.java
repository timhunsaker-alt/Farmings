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
		dev.farmings.Farmings.clientTime = () -> {
			var mc = net.minecraft.client.Minecraft.getInstance();
			return mc.level == null ? 0L : mc.level.getGameTime();
		};
		dev.farmings.Farmings.openManual = () -> net.minecraft.client.Minecraft.getInstance().setScreenAndShow(new dev.farmings.client.manual.ManualScreen());
		ModelLayerRegistry.registerModelLayer(CattleModels.CATTLE, CattleModels::createLayer);
		EntityRendererRegistry.register(FarmingsEntities.CATTLE, CattleRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.farmings.client.SheepModels.SHEEP, dev.farmings.client.SheepModels::createLayer);
		ModelLayerRegistry.registerModelLayer(dev.farmings.client.GoatModels.GOAT, dev.farmings.client.GoatModels::createLayer);
		ModelLayerRegistry.registerModelLayer(dev.farmings.client.PigModels.PIG, dev.farmings.client.PigModels::createLayer);
		ModelLayerRegistry.registerModelLayer(dev.farmings.client.SheepModels.FLEECE, dev.farmings.client.SheepModels::createFleeceLayer);
		EntityRendererRegistry.register(FarmingsEntities.SHEEP, dev.farmings.client.SheepRenderer::new);
		EntityRendererRegistry.register(FarmingsEntities.GOAT, dev.farmings.client.GoatRenderer::new);
		EntityRendererRegistry.register(FarmingsEntities.PIG, dev.farmings.client.PigRenderer::new);
		EntityRendererRegistry.register(FarmingsEntities.HORSE, dev.farmings.client.HorseRenderer::new);
		EntityRendererRegistry.register(FarmingsEntities.ROTTEN_EGG, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
		EntityRendererRegistry.register(FarmingsEntities.SHEEP_DOG, dev.farmings.client.SheepDogRenderer::new);
		EntityRendererRegistry.register(FarmingsEntities.FIELD_GANTRY, dev.farmings.client.FieldGantryRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(dev.farmings.conveyor.ConveyorBlocks.DRIVE_ENTITY, dev.farmings.client.BeltItemsRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.farmings.client.FowlModels.FOWL, dev.farmings.client.FowlModels::createLayer);
		for (var type : FarmingsEntities.FOWL) {
			EntityRendererRegistry.register(type, dev.farmings.client.FowlRenderer::new);
		}
	}
}
