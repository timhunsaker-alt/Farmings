package dev.farmings.client;

import dev.farmings.genetics.VisualPhenotype;
import dev.farmings.husbandry.CattleBreed;
import net.minecraft.client.renderer.entity.state.CowRenderState;

/** What the renderer needs to know about one animal. */
public class CattleRenderState extends CowRenderState {
	public CattleBreed breed = CattleBreed.HOLSTEIN;
	public VisualPhenotype look = CattleRenderer.DEFAULT_LOOK;
	public int markings;
	public boolean udder;
	public boolean bull;
	/** Share of beef and zebu blood, 0-1, from the body families (VisualPhenotype.body and secondary). */
	public float beef;
	public float zebu;
}
