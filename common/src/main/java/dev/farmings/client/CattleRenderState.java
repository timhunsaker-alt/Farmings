package dev.farmings.client;

import dev.farmings.husbandry.CattleBreed;
import net.minecraft.client.renderer.entity.state.CowRenderState;

/** What the renderer needs to know about one animal. */
public class CattleRenderState extends CowRenderState {
	public CattleBreed breed = CattleBreed.HOLSTEIN;
	public boolean udder;
}
