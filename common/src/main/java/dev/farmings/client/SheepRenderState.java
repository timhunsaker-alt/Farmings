package dev.farmings.client;

import dev.farmings.genetics.SheepLook;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the sheep renderer needs to know about one sheep. */
public class SheepRenderState extends LivingEntityRenderState {
	public SheepLook look = SheepRenderer.DEFAULT_LOOK;
	/** Fleece growth as a share of a year, 0-2. */
	public float fleece = 1.0F;
	/** Shares of dairy (East Friesian) and meat (Suffolk, Dorper) blood, 0-1. */
	public float dairy;
	public float meat;
	public boolean udder;
	public boolean ram;
}
