package dev.farmings.neoforge.client;

import dev.farmings.FarmingsEntities;
import dev.farmings.client.CattleModels;
import dev.farmings.client.CattleRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The NeoForge client side. */
public final class FarmingsNeoForgeClient {
	private FarmingsNeoForgeClient() {
	}

	public static void init(IEventBus modBus) {
		dev.farmings.Farmings.clientTime = () -> {
			var mc = net.minecraft.client.Minecraft.getInstance();
			return mc.level == null ? 0L : mc.level.getGameTime();
		};
		modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class,
				event -> {
					event.registerLayerDefinition(CattleModels.CATTLE, CattleModels::createLayer);
					event.registerLayerDefinition(dev.farmings.client.SheepModels.SHEEP, dev.farmings.client.SheepModels::createLayer);
					event.registerLayerDefinition(dev.farmings.client.SheepModels.FLEECE, dev.farmings.client.SheepModels::createFleeceLayer);
					event.registerLayerDefinition(dev.farmings.client.FowlModels.FOWL, dev.farmings.client.FowlModels::createLayer);
				});
		// Wildcatter 0.6.0 registers its PLC payload both ways with one handler; this NeoForge (26.3.0.101) also wants a client handler for a
		// clientbound payload, or the client refuses to start ("missing client-side handlers: [wildcatter:plc]"). Found 2026-11-12 on the first
		// NeoForge client launch. The fix belongs in Wildcatter; until then this registers the same client handler from here.
		modBus.addListener(net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent.class,
				event -> event.register(dev.wildcatter.control.PlcNet.Payload.TYPE,
						(payload, context) -> context.enqueueWork(() -> dev.wildcatter.control.PlcNet.client.accept(payload.tag()))));
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
				event -> {
					event.registerEntityRenderer(FarmingsEntities.CATTLE, CattleRenderer::new);
					event.registerEntityRenderer(FarmingsEntities.SHEEP, dev.farmings.client.SheepRenderer::new);
					event.registerEntityRenderer(FarmingsEntities.ROTTEN_EGG, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
					event.registerEntityRenderer(FarmingsEntities.SHEEP_DOG, dev.farmings.client.SheepDogRenderer::new);
					event.registerEntityRenderer(FarmingsEntities.FIELD_GANTRY, dev.farmings.client.FieldGantryRenderer::new);
					event.registerBlockEntityRenderer(dev.farmings.conveyor.ConveyorBlocks.DRIVE_ENTITY, dev.farmings.client.BeltItemsRenderer::new);
					for (var type : FarmingsEntities.FOWL) {
						event.registerEntityRenderer(type, dev.farmings.client.FowlRenderer::new);
					}
				});
	}
}
