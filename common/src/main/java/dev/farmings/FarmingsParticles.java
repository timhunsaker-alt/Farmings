package dev.farmings;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The mod's own particles. A drop of whey ({@code client/WheyDripParticle}): whey running out of the cheese press's mold while it presses
 * (docs/cheese-armor.md), thin, translucent and a pale yellow-green, as real whey is; vanilla's drips are water blue. Sent by the server.
 */
public final class FarmingsParticles {
	public static final SimpleParticleType WHEY_DRIP = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Farmings.id("whey_drip"),
			new SimpleParticleType(false) {
			});

	private FarmingsParticles() {
	}

	/** Touch this class early so the particles are registered. */
	public static void register() {
	}
}
