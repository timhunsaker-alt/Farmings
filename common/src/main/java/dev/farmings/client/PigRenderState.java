package dev.farmings.client;

import dev.farmings.genetics.PigLook;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the pig renderer needs to know about one pig. */
public class PigRenderState extends LivingEntityRenderState {
	public PigLook look = PigRenderer.DEFAULT_LOOK;
	/** Duroc share, 0-1: drooping ears from 0.5. */
	public float droop;
	public float weightKg = 100;
}
