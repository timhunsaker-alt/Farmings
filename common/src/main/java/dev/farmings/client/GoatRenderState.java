package dev.farmings.client;

import dev.farmings.genetics.GoatLook;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the goat renderer needs to know about one goat. */
public class GoatRenderState extends LivingEntityRenderState {
	public GoatLook look = GoatRenderer.DEFAULT_LOOK;
	/** Shares of dairy (Saanen, Nubian), meat (Boer) and lop-eared (Nubian, Boer) blood, 0-1. */
	public float dairy;
	public float meat;
	public float lop;
	public boolean udder;
	public boolean buck;
}
