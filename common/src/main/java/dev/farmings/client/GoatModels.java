package dev.farmings.client;

import dev.farmings.Farmings;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The goat model (docs/animals/goats.md, "Coat genes"), drawn from scratch on its own 64x64 layout, keeping the vanilla quadruped's head,
 * body and leg places so the walk and hitbox fit.
 *
 * <p>Layout (u, v; tools/goat_textures.py paints the same):
 * <pre>
 *   head 5x5x7 (0,0)   erect ear 1x3x2 (24,0)   lop ear 1x4x2 (30,0)   horn 2x2x5 (38,0)   beard 2x4x1 (54,0)   wattle 1x3x1 (60,0)
 *   leg 3x12x3 (0,16)   body 8x16x6 (28,8)   udder 4x3x3 (0,34)   tail 2x4x1 (16,34)
 * </pre>
 */
public final class GoatModels {
	public static final ModelLayerLocation GOAT = new ModelLayerLocation(Farmings.id("goat"), "main");

	private GoatModels() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 5.0F, 7.0F),
				PartPose.offsetAndRotation(0.0F, 5.0F, -8.0F, 0.15F, 0.0F, 0.0F));
		// erect ears stand out from the poll; lop ears hang beside the cheek (Nubian, Boer)
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -3.0F, 0.0F, 1.0F, 3.0F, 2.0F),
				PartPose.offsetAndRotation(-2.5F, -3.0F, -1.0F, 0.0F, 0.0F, -0.9F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(0.0F, -3.0F, 0.0F, 1.0F, 3.0F, 2.0F),
				PartPose.offsetAndRotation(2.5F, -3.0F, -1.0F, 0.0F, 0.0F, 0.9F));
		head.addOrReplaceChild("right_lop", CubeListBuilder.create().texOffs(30, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 4.0F, 2.0F),
				PartPose.offsetAndRotation(-2.3F, -3.0F, -2.0F, 0.0F, 0.0F, 0.08F));
		head.addOrReplaceChild("left_lop", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 2.0F),
				PartPose.offsetAndRotation(2.3F, -3.0F, -2.0F, 0.0F, 0.0F, -0.08F));
		// horns sweep up and back from the poll
		head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(38, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F),
				PartPose.offsetAndRotation(-1.2F, -4.0F, -1.5F, 0.55F, -0.15F, 0.0F));
		head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(38, 0).mirror().addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F),
				PartPose.offsetAndRotation(1.2F, -4.0F, -1.5F, 0.55F, 0.15F, 0.0F));
		head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(54, 0).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 4.0F, 1.0F),
				PartPose.offset(0.0F, 1.0F, -4.5F));
		head.addOrReplaceChild("right_wattle", CubeListBuilder.create().texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offset(-1.5F, 1.0F, -1.0F));
		head.addOrReplaceChild("left_wattle", CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offset(1.5F, 1.0F, -1.0F));
		// the body lies on its side as the sheep's does: in its own frame +y runs to the rump, z -7 is the belly and z -1 the back
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 8).addBox(-4.0F, -10.0F, -7.0F, 8.0F, 16.0F, 6.0F),
				PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
		body.addOrReplaceChild("udder", CubeListBuilder.create().texOffs(0, 34).addBox(-2.0F, 1.0F, -9.0F, 4.0F, 3.0F, 3.0F), PartPose.ZERO);
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 34).addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 4.0F, 8.0F, -0.5F, 0.0F, 0.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F);
		CubeListBuilder legMirror = CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.5F, 12.0F, 7.0F));
		root.addOrReplaceChild("left_hind_leg", legMirror, PartPose.offset(2.5F, 12.0F, 7.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.5F, 12.0F, -5.0F));
		root.addOrReplaceChild("left_front_leg", legMirror, PartPose.offset(2.5F, 12.0F, -5.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}
}
