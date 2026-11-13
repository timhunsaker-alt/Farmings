package dev.farmings.client;

import dev.farmings.Farmings;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The pig model (docs/animals/pigs.md), drawn from scratch on its own 64x64 layout; the quadruped's head, body and leg places keep the walk.
 *
 * <pre>
 *   head 8x7x6 (0,0)   snout 4x3x2 (28,0)   erect ear 2x3x1 (40,0)   drooping ear 3x4x1 (48,0)   tail 1x3x1 (58,0)
 *   leg 4x6x4 (0,16)   body 10x16x8 (24,14)
 * </pre>
 */
public final class PigModels {
	public static final ModelLayerLocation PIG = new ModelLayerLocation(Farmings.id("pig"), "main");

	private PigModels() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 7.0F, 6.0F),
				PartPose.offset(0.0F, 13.0F, -7.0F));
		head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(28, 0).addBox(-2.0F, -0.5F, -8.0F, 4.0F, 3.0F, 2.0F), PartPose.ZERO);
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 1.0F),
				PartPose.offsetAndRotation(-2.5F, -4.0F, -2.0F, -0.2F, 0.0F, -0.3F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 1.0F),
				PartPose.offsetAndRotation(2.5F, -4.0F, -2.0F, -0.2F, 0.0F, 0.3F));
		head.addOrReplaceChild("right_droop", CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(-2.5F, -4.0F, -3.0F, -1.1F, 0.0F, -0.2F));
		head.addOrReplaceChild("left_droop", CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(2.5F, -4.0F, -3.0F, -1.1F, 0.0F, 0.2F));
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 14).addBox(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F),
				PartPose.offsetAndRotation(0.0F, 11.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(58, 0).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 9.0F, 9.0F, -0.6F, 0.0F, 0.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F);
		CubeListBuilder legMirror = CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.0F, 18.0F, 7.0F));
		root.addOrReplaceChild("left_hind_leg", legMirror, PartPose.offset(3.0F, 18.0F, 7.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.0F, 18.0F, -5.0F));
		root.addOrReplaceChild("left_front_leg", legMirror, PartPose.offset(3.0F, 18.0F, -5.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}
}
