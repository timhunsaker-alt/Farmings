package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.farmings.entity.CattleEntity;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/** A vanilla cow's model and texture, for every breed, until the Ludo models are in. Listed as a placeholder in PROGRESS.md. */
public class CattleRenderer extends MobRenderer<CattleEntity, CowRenderState, CowModel> {
	private static final Identifier ADULT = Identifier.withDefaultNamespace("textures/entity/cow/cow_temperate.png");
	private static final Identifier CALF = Identifier.withDefaultNamespace("textures/entity/cow/cow_temperate_baby.png");

	private final CowModel adultModel;
	private final CowModel calfModel;

	public CattleRenderer(EntityRendererProvider.Context context) {
		super(context, new CowModel(context.bakeLayer(ModelLayers.COW)), 0.7F);
		this.adultModel = new CowModel(context.bakeLayer(ModelLayers.COW));
		this.calfModel = new CowModel(context.bakeLayer(ModelLayers.COW_BABY));
	}

	@Override
	public Identifier getTextureLocation(CowRenderState state) {
		return state.isBaby ? CALF : ADULT;
	}

	@Override
	public CowRenderState createRenderState() {
		return new CowRenderState();
	}

	@Override
	public void submit(CowRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		this.model = state.isBaby ? calfModel : adultModel;
		super.submit(state, poseStack, collector, camera);
	}
}
