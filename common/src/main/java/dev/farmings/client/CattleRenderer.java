package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.farmings.Farmings;
import dev.farmings.entity.CattleEntity;
import dev.farmings.genetics.VisualPhenotype;
import dev.farmings.husbandry.Stage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/**
 * Cattle drawn from their coat genes (docs/animals/cattle.md, "Models and coats"), in three passes over {@link CattleModel}:
 * <ol>
 *   <li>the hair texture, tinted by the coat colour the genes give (black, red or wild type, diluted);</li>
 *   <li>white markings: one mask per pattern the animal shows (patches, speckles, colour-sided) at her white coverage, and a face
 *       marking; which of the drawn shapes she gets is fixed by her marking seed;</li>
 *   <li>details that are never tinted: eyes, muzzle, horns, hooves, udder skin.</li>
 * </ol>
 * The textures are drawn by tools/cattle_textures.py.
 */
public class CattleRenderer extends MobRenderer<CattleEntity, CattleRenderState, CowModel> {
	/** Shown for a moment before the server sends an animal's looks: a plain black-and-white dairy cow. */
	static final VisualPhenotype DEFAULT_LOOK = new VisualPhenotype(VisualPhenotype.Coat.BLACK, 0, 1 << 3, 600, 0, true, 0, 0, 0,
			VisualPhenotype.BodyFamily.DAIRY, null, 0);
	public static final int PATCH_SHAPES = 8;
	public static final int SPECKLE_SHAPES = 4;
	private static final Identifier BASE = Farmings.id("textures/entity/cattle/coat.png");
	private static final Identifier DETAIL = Farmings.id("textures/entity/cattle/detail.png");

	public CattleRenderer(EntityRendererProvider.Context context) {
		super(context, new CattleModel(context.bakeLayer(CattleModels.CATTLE)), 0.7F);
		addLayer(new Markings(this));
	}

	@Override
	public Identifier getTextureLocation(CattleRenderState state) {
		return BASE;
	}

	@Override
	protected int getModelTint(CattleRenderState state) {
		return 0xFF000000 | state.look.coatRgb();
	}

	/** A calf is drawn at half size: the vanilla cow gets that from a separate baby model, which this single model replaces. */
	@Override
	protected void scale(CattleRenderState state, PoseStack pose) {
		super.scale(state, pose);
		if (state.isBaby) {
			pose.scale(0.5F, 0.5F, 0.5F);
		}
	}

	@Override
	public CattleRenderState createRenderState() {
		return new CattleRenderState();
	}

	@Override
	public void extractRenderState(CattleEntity entity, CattleRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.breed = entity.syncedBreed();
		VisualPhenotype look = entity.syncedLook();
		state.look = look == null ? DEFAULT_LOOK : look;
		state.markings = entity.syncedMarkings();
		Stage stage = entity.syncedStage();
		state.udder = stage == Stage.BRED_HEIFER || stage == Stage.LACTATING_COW || stage == Stage.DRY_COW || stage == Stage.RETIRED_COW;
		state.bull = stage == Stage.BULL;
		state.beef = share(state.look, VisualPhenotype.BodyFamily.BEEF);
		state.zebu = share(state.look, VisualPhenotype.BodyFamily.ZEBU);
	}

	/** How much of the animal is of one body family: all of it for the main family less the blended share, or the blended share. */
	static float share(VisualPhenotype look, VisualPhenotype.BodyFamily family) {
		float second = look.secondary() == null ? 0.0F : look.secondaryPct() / 100.0F;
		if (look.body() == family) {
			return 1.0F - second;
		}
		return look.secondary() == family ? second : 0.0F;
	}

	/** The marking textures for this look, in drawing order: brindle stripes, roan, then the white patterns and the face. */
	public static List<Identifier> markings(VisualPhenotype look, int seed) {
		List<Identifier> out = new ArrayList<>();
		if (look.brindle()) {
			out.add(Farmings.id("textures/entity/cattle/brindle.png"));
		}
		if (look.roan() > 0) {
			out.add(Farmings.id("textures/entity/cattle/white/roan_" + Math.min(2, look.roan()) + ".png"));
		}
		int level = Math.max(1, Math.min(4, Math.round(look.white() / 250.0F)));
		int shape = Math.floorMod(seed, PATCH_SHAPES);
		if (look.shows(1)) {
			out.add(Farmings.id("textures/entity/cattle/white/sided_" + level + ".png"));
		}
		if (look.shows(2)) {
			out.add(Farmings.id("textures/entity/cattle/white/speckle_" + level + "_" + Math.floorMod(seed >> 3, SPECKLE_SHAPES) + ".png"));
		}
		if (look.shows(3)) {
			out.add(Farmings.id("textures/entity/cattle/white/patch_" + level + "_" + shape + ".png"));
		}
		if (look.face() > 0) {
			out.add(Farmings.id("textures/entity/cattle/white/face_" + look.face() + ".png"));
		}
		return out;
	}

	private static final class Markings extends RenderLayer<CattleRenderState, CowModel> {
		Markings(RenderLayerParent<CattleRenderState, CowModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, CattleRenderState state, float yRot, float xRot) {
			int order = 1;
			for (Identifier mask : markings(state.look, state.markings)) {
				renderColoredCutoutModel(getParentModel(), mask, pose, collector, light, state, -1, order++);
			}
			renderColoredCutoutModel(getParentModel(), DETAIL, pose, collector, light, state, -1, order);
		}
	}
}
