package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.farmings.Farmings;
import dev.farmings.entity.FowlEntity;
import dev.farmings.genetics.PoultryLook;
import dev.farmings.genetics.Species;
import java.util.Locale;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Birds drawn from their plumage genes (docs/animals/poultry.md, "Plumage and shell genes"). Each species and plumage has its own texture,
 * drawn by tools/poultry_textures.py; mallard-pattern and khaki drakes have their own head colours; young birds wear down. The model is
 * drawn at the size of a 2.5 kg chicken and scaled by the cube root of the bird's weight against its species' reference (proposed).
 */
public class FowlRenderer extends MobRenderer<FowlEntity, FowlRenderState, FowlModel> {
	static final PoultryLook DEFAULT_LOOK = new PoultryLook(PoultryLook.Plumage.WHITE, PoultryLook.Comb.SINGLE, false, 0, 0);
	private static final String DIR = "textures/entity/fowl/";

	public FowlRenderer(EntityRendererProvider.Context context) {
		super(context, new FowlModel(context.bakeLayer(FowlModels.FOWL)), 0.3F);
	}

	/** The texture file for a species, plumage, sex and age. */
	public static String textureName(Species species, PoultryLook.Plumage plumage, boolean male, boolean young) {
		String sp = species.name().toLowerCase(Locale.ROOT);
		if (young) {
			return sp + "_young";
		}
		boolean drakeHead = species == Species.DUCK && male
				&& (plumage == PoultryLook.Plumage.MALLARD || plumage == PoultryLook.Plumage.RESTRICTED || plumage == PoultryLook.Plumage.KHAKI);
		return sp + "_" + plumage.name().toLowerCase(Locale.ROOT) + (drakeHead ? "_male" : "");
	}

	@Override
	public Identifier getTextureLocation(FowlRenderState s) {
		return Farmings.id(DIR + textureName(s.species, s.look.plumage(), s.male, s.young) + ".png");
	}

	/** Weight (kg) the model is drawn at, and the model scale at that weight, per species *(proposed)*. */
	static float[] reference(Species s) {
		return switch (s) {
			case TURKEY -> new float[] {9.0F, 1.7F};
			case QUAIL -> new float[] {0.14F, 0.5F};
			case DUCK -> new float[] {3.0F, 1.05F};
			case GOOSE -> new float[] {9.0F, 1.55F};
			case GUINEA -> new float[] {1.5F, 0.95F};
			default -> new float[] {2.5F, 1.0F};
		};
	}

	@Override
	protected void scale(FowlRenderState state, PoseStack pose) {
		super.scale(state, pose);
		float[] ref = reference(state.species);
		float k = ref[1] * (float) Math.cbrt(Math.max(0.01F, state.weightKg) / ref[0]);
		k = Math.max(0.12F, Math.min(2.4F, k));
		pose.scale(k, k, k);
	}

	@Override
	public FowlRenderState createRenderState() {
		return new FowlRenderState();
	}

	@Override
	public void extractRenderState(FowlEntity entity, FowlRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		PoultryLook look = entity.syncedLook();
		state.look = look == null ? DEFAULT_LOOK : look;
		state.species = entity.species();
		state.breed = entity.syncedBreed();
		state.male = entity.syncedMale();
		state.young = entity.syncedYoung();
		state.broody = entity.syncedBroody();
		state.weightKg = entity.syncedSize();
		state.flapping = !entity.onGround() && !entity.isInWater();
	}
}
