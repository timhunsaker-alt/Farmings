package dev.farmings.client;

import dev.farmings.Farmings;
import dev.farmings.entity.SheepDogEntity;
import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;

/**
 * The Border Collie (docs/animals/sheep.md, "Sheep dog"), drawn on the vanilla wolf's frame with its own black-and-white coat
 * (tools/dog_texture.py paints it from scratch on the wolf's texture layout). Lying down is drawn as the wolf's sit.
 */
public class SheepDogRenderer extends MobRenderer<SheepDogEntity, WolfRenderState, WolfModel> {
	private static final Identifier TEXTURE = Farmings.id("textures/entity/sheep_dog.png");

	public SheepDogRenderer(EntityRendererProvider.Context context) {
		super(context, new AdultWolfModel(context.bakeLayer(ModelLayers.WOLF)), 0.5F);
	}

	@Override
	public Identifier getTextureLocation(WolfRenderState state) {
		return TEXTURE;
	}

	@Override
	public WolfRenderState createRenderState() {
		return new WolfRenderState();
	}

	@Override
	public void extractRenderState(SheepDogEntity entity, WolfRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isSitting = entity.sitting();
		state.tailAngle = (float) (Math.PI / 5);
		state.texture = TEXTURE;
		state.wetShade = 1.0F;
	}
}
