package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.farmings.Farmings;
import dev.farmings.entity.PigEntity;
import dev.farmings.genetics.PigLook;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Pigs drawn from their coat genes (pigs.md, "Coat genes"): one texture a colour, drawn by tools/pig_textures.py. The model is drawn at
 * a 100 kg pig and scaled by the cube root of the pig's weight *(proposed, as the birds)*.
 */
public class PigRenderer extends MobRenderer<PigEntity, PigRenderState, PigModel> {
	static final PigLook DEFAULT_LOOK = new PigLook(PigLook.Colour.WHITE, 0);

	public PigRenderer(EntityRendererProvider.Context context) {
		super(context, new PigModel(context.bakeLayer(PigModels.PIG)), 0.6F);
	}

	@Override
	public Identifier getTextureLocation(PigRenderState s) {
		return Farmings.id("textures/entity/pig/" + s.look.colour().name().toLowerCase(java.util.Locale.ROOT) + ".png");
	}

	@Override
	protected void scale(PigRenderState state, PoseStack pose) {
		super.scale(state, pose);
		float k = state.isBaby ? 0.5F : (float) Math.max(0.6, Math.min(1.45, Math.cbrt(Math.max(5, state.weightKg) / 100.0)));
		pose.scale(k, k, k);
	}

	@Override
	public PigRenderState createRenderState() {
		return new PigRenderState();
	}

	@Override
	public void extractRenderState(PigEntity entity, PigRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		PigLook look = entity.syncedLook();
		state.look = look == null ? DEFAULT_LOOK : look;
		state.droop = entity.syncedDroop();
		state.weightKg = entity.syncedKg();
	}
}
