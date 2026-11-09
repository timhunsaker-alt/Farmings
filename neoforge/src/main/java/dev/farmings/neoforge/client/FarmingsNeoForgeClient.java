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
				event -> event.registerLayerDefinition(CattleModels.CATTLE, CattleModels::createLayer));
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
				event -> event.registerEntityRenderer(FarmingsEntities.CATTLE, CattleRenderer::new));
	}
}
