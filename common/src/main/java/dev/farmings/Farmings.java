package dev.farmings;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod, the same on every loader. Each loader's entry point calls {@link #register(String)} while the registries are open.
 * Registers the animals; blocks, items and machines follow with the barn.
 */
public final class Farmings {
	public static final String MOD_ID = "farmings";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Farmings() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** {@code requiredMod} is Wildcatter's mod id, read off the Wildcatter jar on the compile classpath. */
	public static void register(String requiredMod) {
		FarmingsBlocks.register();
		FarmingsEntities.register();
		LOGGER.info("Farmings loaded; requires {}", requiredMod);
	}
}
