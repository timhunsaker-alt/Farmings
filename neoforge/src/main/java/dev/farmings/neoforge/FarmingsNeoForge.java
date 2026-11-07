package dev.farmings.neoforge;

import dev.farmings.Farmings;
import dev.wildcatter.Wildcatter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge entry point. Wildcatter is a hard dependency and a compile dependency. */
@Mod(Farmings.MOD_ID)
public final class FarmingsNeoForge {
	private static boolean registered;

	public FarmingsNeoForge(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> {
			if (!registered) {
				registered = true;
				Farmings.register(Wildcatter.MOD_ID);
			}
		});
	}
}
