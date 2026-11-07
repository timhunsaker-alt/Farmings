package dev.farmings.fabric;

import dev.farmings.Farmings;
import dev.wildcatter.Wildcatter;
import net.fabricmc.api.ModInitializer;

/** The Fabric entry point. Wildcatter is a hard dependency and a compile dependency. */
public final class FarmingsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Farmings.register(Wildcatter.MOD_ID);
	}
}
