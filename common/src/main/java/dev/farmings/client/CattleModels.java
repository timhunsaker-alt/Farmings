package dev.farmings.client;

import dev.farmings.Farmings;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The one cattle model (docs/animals/cattle.md, "Models and coats"). It keeps the vanilla cow's head, body and legs where they are (so the
 * walking animation and hitbox fit), on its own 64x64 texture layout, and adds parts the coat genes turn on: horns, short or long ears, a
 * hump in three sizes, a dewlap, an udder and a bull's crest. Body types (dairy, beef, zebu) and crosses between them are made by scaling
 * these parts in {@link CattleModel#setupAnim}, not by separate models, so a cross can sit between its parents.
 *
 * <p>Texture layout (u, v of each box; tools/cattle_textures.py paints the same layout):
 * <pre>
 *   head 8x8x6 (0,0)   muzzle 6x3x1 (28,0)   horns 1x3x1 (44,0) (48,0)   short ear 3x1x2 (52,0)   long ear 1x5x2 (52,4)
 *   legs 4x12x4 (0,16)   body 12x18x10 (18,14)
 *   udder 4x6x1 (0,42)   crest 6x8x2 (10,42)   dewlap 1x6x4 (48,42)
 *   hump: large 6x7x3 (0,54), medium 6x5x2 (18,54), small 5x3x2 (34,54)
 * </pre>
 */
public final class CattleModels {
	public static final ModelLayerLocation CATTLE = new ModelLayerLocation(Farmings.id("cattle"), "main");

	private CattleModels() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
				.texOffs(28, 0).addBox(-3.0F, 1.0F, -7.0F, 6.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 4.0F, -8.0F));
		head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offsetAndRotation(-4.0F, -3.0F, -4.5F, 0.0F, 0.0F, -0.45F));
		head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(48, 0).addBox(0.0F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offsetAndRotation(4.0F, -3.0F, -4.5F, 0.0F, 0.0F, 0.45F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(52, 0).addBox(-3.0F, 0.0F, 0.0F, 3.0F, 1.0F, 2.0F), PartPose.offset(-4.0F, -3.0F, -3.0F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 2.0F), PartPose.offset(4.0F, -3.0F, -3.0F));
		head.addOrReplaceChild("right_long_ear", CubeListBuilder.create().texOffs(52, 4).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 5.0F, 2.0F),
				PartPose.offsetAndRotation(-4.0F, -2.0F, -4.0F, 0.0F, 0.0F, 0.25F));
		head.addOrReplaceChild("left_long_ear", CubeListBuilder.create().texOffs(52, 4).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 5.0F, 2.0F),
				PartPose.offsetAndRotation(4.0F, -2.0F, -4.0F, 0.0F, 0.0F, -0.25F));
		// the body lies on its side (rotated a quarter turn), so in its own frame +y runs to the rump and +z is up (z 3 is the back, -7 the belly)
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 14).addBox(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F),
				PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
		body.addOrReplaceChild("udder", CubeListBuilder.create().texOffs(0, 42).addBox(-2.0F, 2.0F, -8.0F, 4.0F, 6.0F, 1.0F), PartPose.ZERO);
		body.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(10, 42).addBox(-3.0F, -10.0F, 3.0F, 6.0F, 8.0F, 2.0F), PartPose.ZERO);
		body.addOrReplaceChild("dewlap", CubeListBuilder.create().texOffs(48, 42).addBox(-0.5F, -10.0F, -11.0F, 1.0F, 6.0F, 4.0F), PartPose.ZERO);
		body.addOrReplaceChild("hump_large", CubeListBuilder.create().texOffs(0, 54).addBox(-3.0F, -9.0F, 3.0F, 6.0F, 7.0F, 3.0F), PartPose.ZERO);
		body.addOrReplaceChild("hump_medium", CubeListBuilder.create().texOffs(18, 54).addBox(-3.0F, -8.0F, 3.0F, 6.0F, 5.0F, 2.0F), PartPose.ZERO);
		body.addOrReplaceChild("hump_small", CubeListBuilder.create().texOffs(34, 54).addBox(-2.5F, -7.0F, 3.0F, 5.0F, 3.0F, 2.0F), PartPose.ZERO);
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
		CubeListBuilder legMirror = CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-4.0F, 12.0F, 7.0F));
		root.addOrReplaceChild("left_hind_leg", legMirror, PartPose.offset(4.0F, 12.0F, 7.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-4.0F, 12.0F, -5.0F));
		root.addOrReplaceChild("left_front_leg", legMirror, PartPose.offset(4.0F, 12.0F, -5.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}
}
