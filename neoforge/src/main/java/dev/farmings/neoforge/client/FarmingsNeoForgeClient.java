package dev.farmings.neoforge.client;

import dev.farmings.FarmingsEntities;
import dev.farmings.client.CattleRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The NeoForge client side. */
public final class FarmingsNeoForgeClient {
	private FarmingsNeoForgeClient() {
	}

	public static void init(IEventBus modBus) {
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
				event -> event.registerEntityRenderer(FarmingsEntities.CATTLE, CattleRenderer::new));
	}
}
