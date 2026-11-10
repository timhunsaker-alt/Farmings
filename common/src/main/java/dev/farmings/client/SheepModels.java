package dev.farmings.client;

import dev.farmings.Farmings;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The sheep models (docs/animals/sheep.md, "Coat genes"): a skin model and a fleece model drawn over it. Both keep the vanilla sheep's
 * head, body and leg places, so the walk and hitbox fit; everything is drawn from scratch on its own 64x64 layouts.
 *
 * <p>Skin layout (u, v; tools/sheep_textures.py paints the same):
 * <pre>
 *   head 6x6x8 (0,0)   ear 3x1x2 (28,0)   horn base 2x2x5 (40,0)   horn curl 2x4x2 (54,0)
 *   leg 4x12x4 (0,16)   body 8x16x6 (28,8)   udder 4x3x3 (0,34)   tail 1x8x1 (16,34)
 * </pre>
 * Fleece layout: poll cap 6x3x6 (0,0), body 8x16x6 (28,8) inflated, leg wool 4x6x4 (0,16) inflated. The fleece body is drawn at its
 * thickest (two years of wool) and scaled down to the sheep's fleece length in {@link SheepFleeceModel}.
 */
public final class SheepModels {
	public static final ModelLayerLocation SHEEP = new ModelLayerLocation(Farmings.id("sheep"), "main");
	public static final ModelLayerLocation FLEECE = new ModelLayerLocation(Farmings.id("sheep"), "fleece");
	/** The fleece body's inflation at its thickest, in model pixels. */
	public static final float MAX_WOOL = 2.8F;

	private SheepModels() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -6.0F, 6.0F, 6.0F, 8.0F),
				PartPose.offset(0.0F, 6.0F, -8.0F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(28, 0).addBox(-3.0F, 0.0F, 0.0F, 3.0F, 1.0F, 2.0F),
				PartPose.offsetAndRotation(-3.0F, -3.0F, -3.0F, 0.0F, 0.0F, 0.35F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 2.0F),
				PartPose.offsetAndRotation(3.0F, -3.0F, -3.0F, 0.0F, 0.0F, -0.35F));
		// a ram's horn: a base running back from the poll, then a curl hanging down beside the cheek
		PartDefinition rh = head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F),
				PartPose.offsetAndRotation(-2.5F, -3.5F, -2.0F, 0.15F, -0.35F, 0.0F));
		rh.addOrReplaceChild("curl", CubeListBuilder.create().texOffs(54, 0).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 4.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, 0.5F, 3.5F, -0.6F, 0.0F, 0.0F));
		PartDefinition lh = head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(0.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F),
				PartPose.offsetAndRotation(2.5F, -3.5F, -2.0F, 0.15F, 0.35F, 0.0F));
		lh.addOrReplaceChild("curl", CubeListBuilder.create().texOffs(54, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 4.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, 0.5F, 3.5F, -0.6F, 0.0F, 0.0F));
		// the body lies on its side as the cow's does: in its own frame +y runs to the rump, z -7 is the belly and z -1 the back
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 8).addBox(-4.0F, -10.0F, -7.0F, 8.0F, 16.0F, 6.0F),
				PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
		body.addOrReplaceChild("udder", CubeListBuilder.create().texOffs(0, 34).addBox(-2.0F, 1.0F, -9.0F, 4.0F, 3.0F, 3.0F), PartPose.ZERO);
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 34).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 6.5F, 8.0F, 0.25F, 0.0F, 0.0F));
		legs(root, CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
				CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	public static LayerDefinition createFleeceLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -4.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.7F)),
				PartPose.offset(0.0F, 6.0F, -8.0F));
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 8).addBox(-4.0F, -10.0F, -7.0F, 8.0F, 16.0F, 6.0F, new CubeDeformation(MAX_WOOL)),
				PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.6F));
		CubeListBuilder legMirror = CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.6F));
		legs(root, leg, legMirror);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static void legs(PartDefinition root, CubeListBuilder leg, CubeListBuilder legMirror) {
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.0F, 12.0F, 7.0F));
		root.addOrReplaceChild("left_hind_leg", legMirror, PartPose.offset(3.0F, 12.0F, 7.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.0F, 12.0F, -5.0F));
		root.addOrReplaceChild("left_front_leg", legMirror, PartPose.offset(3.0F, 12.0F, -5.0F));
	}
}
