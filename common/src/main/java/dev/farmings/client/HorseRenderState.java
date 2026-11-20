package dev.farmings.client;

import dev.farmings.genetics.HorseLook;
import net.minecraft.client.renderer.entity.state.EquineRenderState;

/** What the horse renderer needs: the look from the genes, and the weight against the breed's mature weight for size. */
public class HorseRenderState extends EquineRenderState implements HealthSignsState {
	/** The signs of sickness (dev.farmings.health.HealthSigns flags). */
	public int healthSigns;

	@Override
	public int healthSigns() {
		return healthSigns;
	}

	public HorseLook look = HorseRenderer.DEFAULT_LOOK;
	public float weightKg = 500;
	public float matureKg = 500;
}
