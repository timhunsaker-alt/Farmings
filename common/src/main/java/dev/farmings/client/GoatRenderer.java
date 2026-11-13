package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.farmings.Farmings;
import dev.farmings.entity.GoatEntity;
import dev.farmings.genetics.GoatLook;
import dev.farmings.husbandry.GoatStage;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/**
 * Goats drawn from their coat genes (docs/animals/goats.md, "Coat genes"):
 * <ol>
 *   <li>the coat, tinted by the agouti colour (white, bezoar, badgerface, swiss, black, brown);</li>
 *   <li>the agouti pattern's dark or white markings, untinted;</li>
 *   <li>the EDNRA white body: the Boer's white body and red head, or a belt and patches;</li>
 *   <li>details never tinted: eyes, nose, hooves, horns, udder.</li>
 * </ol>
 * The textures are drawn by tools/goat_textures.py.
 */
public class GoatRenderer extends MobRenderer<GoatEntity, GoatRenderState, GoatModel> {
	static final GoatLook DEFAULT_LOOK = new GoatLook(GoatLook.Colour.WHITE, 0, false, false, false, false, 0);
	private static final String DIR = "textures/entity/goat/";
	private static final Identifier SKIN = Farmings.id(DIR + "skin.png");
	private static final Identifier DETAIL = Farmings.id(DIR + "detail.png");

	public GoatRenderer(EntityRendererProvider.Context context) {
		super(context, new GoatModel(context.bakeLayer(GoatModels.GOAT)), 0.6F);
		addLayer(new Markings(this));
	}

	@Override
	public Identifier getTextureLocation(GoatRenderState state) {
		return SKIN;
	}

	/** Coat colours *(drawn, not measured)*: white, the tans of bezoar and badgerface, the swiss brown, black and chocolate. */
	static int coatRgb(GoatLook.Colour c) {
		return switch (c) {
			case WHITE -> 0xF2EDE4;
			case BEZOAR -> 0xA87A48;
			case BADGERFACE -> 0xC09A68;
			case SWISS -> 0x7A5436;
			case BLACK -> 0x2C2826;
			case BROWN -> 0x5C3C28;
		};
	}

	@Override
	protected int getModelTint(GoatRenderState state) {
		return 0xFF000000 | coatRgb(state.look.colour());
	}

	@Override
	protected void scale(GoatRenderState state, PoseStack pose) {
		super.scale(state, pose);
		if (state.isBaby) {
			pose.scale(0.5F, 0.5F, 0.5F);
		}
	}

	@Override
	public GoatRenderState createRenderState() {
		return new GoatRenderState();
	}

	@Override
	public void extractRenderState(GoatEntity entity, GoatRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		GoatLook look = entity.syncedLook();
		state.look = look == null ? DEFAULT_LOOK : look;
		state.dairy = entity.syncedDairy();
		state.meat = entity.syncedMeat();
		state.lop = entity.syncedLop();
		state.udder = entity.syncedUdder();
		state.buck = entity.syncedStage() == GoatStage.BUCK;
	}

	private static final class Markings extends RenderLayer<GoatRenderState, GoatModel> {
		Markings(RenderLayerParent<GoatRenderState, GoatModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, GoatRenderState s, float yRot, float xRot) {
			int order = 1;
			String pattern = switch (s.look.colour()) {
				case BEZOAR -> "pattern_bezoar.png";
				case BADGERFACE -> "pattern_badgerface.png";
				case SWISS -> "pattern_swiss.png";
				default -> null;
			};
			if (pattern != null) {
				renderColoredCutoutModel(getParentModel(), Farmings.id(DIR + pattern), pose, collector, light, s, -1, order++);
			}
			if (s.look.pattern() > 0) {
				renderColoredCutoutModel(getParentModel(), Farmings.id(DIR + "redhead_" + Math.min(2, s.look.pattern()) + ".png"), pose, collector, light, s,
						-1, order++);
			}
			renderColoredCutoutModel(getParentModel(), DETAIL, pose, collector, light, s, -1, order);
		}
	}
}
