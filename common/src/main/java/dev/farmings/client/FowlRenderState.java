package dev.farmings.client;

import dev.farmings.genetics.PoultryLook;
import dev.farmings.genetics.Species;
import dev.farmings.husbandry.poultry.FowlBreed;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the bird renderer needs to know about one bird. */
public class FowlRenderState extends LivingEntityRenderState {
	public Species species = Species.CHICKEN;
	public FowlBreed breed = FowlBreed.LEGHORN;
	public PoultryLook look = FowlRenderer.DEFAULT_LOOK;
	public boolean male;
	public boolean young;
	public boolean broody;
	public boolean flapping;
	public float weightKg = 2.0F;
}
