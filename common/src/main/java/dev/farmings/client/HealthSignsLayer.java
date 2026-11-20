package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.farmings.Farmings;
import dev.farmings.health.HealthSigns;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/**
 * Draws the signs of sickness that are things on the animal, not poses (docs/animal-health.md): a red band round each hind leg while a
 * drug withdrawal runs, and the raised red-purple diamond patches of swine erysipelas on a pig's flanks. Both are small cubes placed from
 * the leg's and the body's own cube, so they fit every species and every body shape. Textures from Ludo swatches (tools/process_art.py).
 */
public class HealthSignsLayer<S extends LivingEntityRenderState & HealthSignsState, M extends EntityModel<? super S> & HealthSignsModel> extends RenderLayer<S, M> {
	private static final RenderType BAND = RenderTypes.entityCutout(Farmings.id("textures/entity/health/leg_band.png"));
	private static final RenderType MARKS = RenderTypes.entityCutout(Farmings.id("textures/entity/health/erysipelas.png"));
	/** The band: 2 px tall, 0.4 px proud of the leg, its middle 40% of the way up from the hoof, about the hock or the pastern's top. */
	private static final float BAND_PX = 2.0F;
	private static final float BAND_PROUD = 0.4F;
	private static final float BAND_UP = 0.4F;
	/** A diamond 2.6 px across its points, 0.3 px raised (proposed). */
	private static final float DIAMOND_PX = 2.6F;
	/** Where the diamonds sit on each flank: (along the body, up the side), as shares of the body cube. */
	private static final float[][] SPOTS = {{0.22F, 0.35F}, {0.42F, 0.7F}, {0.55F, 0.3F}, {0.75F, 0.6F}, {0.88F, 0.28F}};
	private static final ModelPart BAND_CUBE = cube(0, 0, 0, 16, 4, 16, 64, 20);
	private static final ModelPart DIAMOND = cube(-0.5F, -4, -4, 1, 8, 8, 32, 32);
	private static final RandomSource ONE = RandomSource.create(0);

	public HealthSignsLayer(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	private static ModelPart cube(float x, float y, float z, int w, int h, int d, int texW, int texH) {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("c", CubeListBuilder.create().texOffs(0, 0).addBox(x, y, z, w, h, d), PartPose.ZERO);
		return LayerDefinition.create(mesh, texW, texH).bakeRoot().getChild("c");
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		int signs = state.healthSigns();
		if (signs == 0 || state.isInvisible) {
			return;
		}
		M model = getParentModel();
		if (HealthSigns.has(signs, HealthSigns.BAND)) {
			for (ModelPart leg : model.bandLegs()) {
				if (leg == null || !leg.visible || leg.isEmpty()) {
					continue;
				}
				ModelPart.Cube c = leg.getRandomCube(ONE);
				float w = c.maxX - c.minX + 2 * BAND_PROUD;
				float d = c.maxZ - c.minZ + 2 * BAND_PROUD;
				float y = c.maxY - BAND_UP * (c.maxY - c.minY) - BAND_PX / 2;
				pose.pushPose();
				model.root().translateAndRotate(pose);
				leg.translateAndRotate(pose);
				pose.translate((c.minX - BAND_PROUD) / 16.0F, y / 16.0F, (c.minZ - BAND_PROUD) / 16.0F);
				pose.scale(w / 16.0F, BAND_PX / 4.0F, d / 16.0F);
				collector.submitModelPart(BAND_CUBE, pose, BAND, light, OverlayTexture.NO_OVERLAY, null);
				pose.popPose();
			}
		}
		ModelPart body = model.markBody();
		if (HealthSigns.has(signs, HealthSigns.DIAMONDS) && body != null && body.visible && !body.isEmpty()) {
			ModelPart.Cube c = body.getRandomCube(ONE);
			for (int side = 0; side < 2; side++) {
				float x = side == 0 ? c.minX - 0.15F : c.maxX + 0.15F;
				for (float[] s : SPOTS) {
					float y = c.minY + s[0] * (c.maxY - c.minY);
					float z = c.minZ + s[1] * (c.maxZ - c.minZ);
					pose.pushPose();
					model.root().translateAndRotate(pose);
					body.translateAndRotate(pose);
					pose.translate(x / 16.0F, y / 16.0F, z / 16.0F);
					pose.rotateDegrees(Axis.XP, 45);
					pose.scale(0.3F, DIAMOND_PX / 1.41F / 8.0F, DIAMOND_PX / 1.41F / 8.0F);
					collector.submitModelPart(DIAMOND, pose, MARKS, light, OverlayTexture.NO_OVERLAY, null);
					pose.popPose();
				}
			}
		}
	}
}
