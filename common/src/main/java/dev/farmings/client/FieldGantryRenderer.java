package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.farmings.entity.FieldGantryEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;

/** Draws the gliding field gantry: each block it carries, at its offset from the moving entity (as vanilla draws a falling block). */
public class FieldGantryRenderer extends EntityRenderer<FieldGantryEntity, FieldGantryRenderer.State> {
	public static final class State extends EntityRenderState {
		final List<BlockPos> offsets = new ArrayList<>();
		final List<MovingBlockRenderState> blocks = new ArrayList<>();
	}

	public FieldGantryRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRender(FieldGantryEntity e, Frustum frustum, double x, double y, double z, float partial) {
		return true;     // a gantry spans the field; its own 1-block box would cull it too soon
	}

	@Override
	public void extractRenderState(FieldGantryEntity e, State s, float partial) {
		super.extractRenderState(e, s, partial);
		s.offsets.clear();
		s.blocks.clear();
		if (!(e.level() instanceof ClientLevel level)) {
			return;
		}
		for (FieldGantryEntity.Cell c : e.shownCells()) {
			if (c.state().getRenderShape() != RenderShape.MODEL) {
				continue;
			}
			MovingBlockRenderState b = new MovingBlockRenderState();
			BlockPos at = BlockPos.containing(s.x + c.offset().getX() + 0.5, s.y + c.offset().getY() + 0.5, s.z + c.offset().getZ() + 0.5);
			b.blockPos = at;
			b.randomSeedPos = e.cab().offset(c.offset());
			b.blockState = c.state();
			b.biome = level.getBiome(at);
			b.cardinalLighting = level.cardinalLighting();
			b.lightEngine = level.getLightEngine();
			s.offsets.add(c.offset());
			s.blocks.add(b);
		}
	}

	@Override
	public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < s.blocks.size(); i++) {
			BlockPos o = s.offsets.get(i);
			pose.pushPose();
			pose.translate(o.getX(), o.getY(), o.getZ());
			collector.submitMovingBlock(pose, s.blocks.get(i), s.outlineColor);
			pose.popPose();
		}
		super.submit(s, pose, collector, camera);
	}
}
