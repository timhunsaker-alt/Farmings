package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.farmings.Farmings;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.SheepLook;
import dev.farmings.husbandry.SheepStage;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/**
 * Sheep drawn from their coat genes (docs/animals/sheep.md, "Coat genes"):
 * <ol>
 *   <li>the skin and short hair, tinted by the colour the genes give (white, black, brown);</li>
 *   <li>the head and leg colour (black or mottled) on a white sheep, and white spots on a spotted coloured sheep; untinted;</li>
 *   <li>details never tinted: eyes, nose, hooves, horns, udder;</li>
 *   <li>the fleece model, tinted by the wool colour, with the crimp texture of the crimp gene and the same spots.</li>
 * </ol>
 * The textures are drawn by tools/sheep_textures.py.
 */
public class SheepRenderer extends MobRenderer<SheepEntity, SheepRenderState, SheepModel> {
	static final SheepLook DEFAULT_LOOK = new SheepLook(SheepLook.Colour.WHITE, 0, false, 0, 0, 0, 1, 0);
	public static final int SPOT_SHAPES = 4;
	private static final String DIR = "textures/entity/sheep/";
	private static final Identifier SKIN = Farmings.id(DIR + "skin.png");
	private static final Identifier DETAIL = Farmings.id(DIR + "detail.png");

	public SheepRenderer(EntityRendererProvider.Context context) {
		super(context, new SheepModel(context.bakeLayer(SheepModels.SHEEP)), 0.7F);
		addLayer(new Markings(this));
		addLayer(new Fleece(this, new SheepFleeceModel(context.bakeLayer(SheepModels.FLEECE))));
	}

	@Override
	public Identifier getTextureLocation(SheepRenderState state) {
		return SKIN;
	}

	/** Hair and skin colours *(drawn, not measured)*. */
	static int skinRgb(SheepLook.Colour c) {
		return switch (c) {
			case WHITE -> 0xF0E4D8;
			case BLACK -> 0x2E2A28;
			case BROWN -> 0x6C4A33;
		};
	}

	/** Wool colours as they look on the sheep, greasy and a little dusty *(drawn)*. */
	static int woolRgb(SheepLook look) {
		return switch (look.colour()) {
			case WHITE -> 0xEDE3CC;
			case BLACK -> 0x34302D;
			case BROWN -> 0x74543C;
		};
	}

	@Override
	protected int getModelTint(SheepRenderState state) {
		return 0xFF000000 | skinRgb(state.look.colour());
	}

	@Override
	protected void scale(SheepRenderState state, PoseStack pose) {
		super.scale(state, pose);
		if (state.isBaby) {
			pose.scale(0.5F, 0.5F, 0.5F);
		}
	}

	@Override
	public SheepRenderState createRenderState() {
		return new SheepRenderState();
	}

	@Override
	public void extractRenderState(SheepEntity entity, SheepRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		SheepLook look = entity.syncedLook();
		state.look = look == null ? DEFAULT_LOOK : look;
		state.fleece = entity.syncedFleece();
		state.dairy = entity.syncedDairy();
		state.meat = entity.syncedMeat();
		state.udder = entity.syncedUdder();
		state.ram = entity.syncedStage() == SheepStage.RAM;
	}

	static Identifier spots(String what, int seed) {
		return Farmings.id(DIR + "spots_" + what + "_" + Math.floorMod(seed, SPOT_SHAPES) + ".png");
	}

	private static final class Markings extends RenderLayer<SheepRenderState, SheepModel> {
		Markings(RenderLayerParent<SheepRenderState, SheepModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, SheepRenderState s, float yRot, float xRot) {
			int order = 1;
			if (s.look.colour() == SheepLook.Colour.WHITE && s.look.head() > 0) {
				Identifier head = Farmings.id(DIR + (s.look.head() == 2 ? "head_black.png" : "head_mottled.png"));
				renderColoredCutoutModel(getParentModel(), head, pose, collector, light, s, -1, order++);
			}
			if (s.look.spotted()) {
				renderColoredCutoutModel(getParentModel(), spots("skin", s.look.seed()), pose, collector, light, s, -1, order++);
			}
			renderColoredCutoutModel(getParentModel(), DETAIL, pose, collector, light, s, -1, order);
		}
	}

	private static final class Fleece extends RenderLayer<SheepRenderState, SheepModel> {
		private final SheepFleeceModel model;

		Fleece(RenderLayerParent<SheepRenderState, SheepModel> parent, SheepFleeceModel model) {
			super(parent);
			this.model = model;
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, SheepRenderState s, float yRot, float xRot) {
			if (s.look.sheds() || s.fleece < 0.02F || s.isInvisible) {
				return;
			}
			Identifier tex = Farmings.id(DIR + "fleece_" + Math.max(0, Math.min(3, s.look.crimp())) + ".png");
			coloredCutoutModelCopyLayerRender(model, tex, pose, collector, light, s, 0xFF000000 | woolRgb(s.look), 10);
			if (s.look.spotted()) {
				coloredCutoutModelCopyLayerRender(model, spots("fleece", s.look.seed()), pose, collector, light, s, -1, 11);
			}
		}
	}
}
