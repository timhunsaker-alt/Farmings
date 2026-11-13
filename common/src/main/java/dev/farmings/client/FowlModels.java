package dev.farmings.client;

import dev.farmings.Farmings;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * One bird model for all four species (docs/animals/poultry.md, "Movement and the model"), drawn from scratch: every species' parts are in
 * it and {@link FowlModel} shows the ones a bird has. Drawn at chicken size; the renderer scales by species and weight.
 *
 * <p>Layout on a 64x64 texture (u, v; tools/poultry_textures.py paints the same):
 * <pre>
 *   head 4x6x3 (0,0)    beak 4x2x2 (14,0)   wattle 2x2x2 (14,4)  snood 1x3x1 (22,4)  comb 1x3x4 (26,0)  crest 5x3x5 (36,0)
 *   beard 1x3x1 (56,0)  body 6x8x6 (0,9)    wing 1x4x6 (24,13)   leg 3x5x3 (38,13)   tail 4x4x2 (50,8)
 *   fan 12x10x1 (0,24)  bill 4x1x3 (26,24)  caruncle 4x2x1 (40,24)  web foot 3x1x3 (52,24)  duck leg 2x4x2 (0,36)
 *   goose neck 2x6x2 (16,36)  Chinese goose knob 2x2x2 (28,36)
 * </pre>
 */
public final class FowlModels {
	public static final ModelLayerLocation FOWL = new ModelLayerLocation(Farmings.id("fowl"), "main");

	private FowlModels() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 3.0F),
				PartPose.offset(0.0F, 15.0F, -4.0F));
		head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(14, 0).addBox(-2.0F, -4.0F, -4.0F, 4.0F, 2.0F, 2.0F), PartPose.ZERO);
		head.addOrReplaceChild("wattle", CubeListBuilder.create().texOffs(14, 4).addBox(-1.0F, -2.0F, -3.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
		head.addOrReplaceChild("snood", CubeListBuilder.create().texOffs(22, 4).addBox(-0.5F, -4.0F, -4.5F, 1.0F, 3.0F, 1.0F), PartPose.ZERO);
		head.addOrReplaceChild("comb", CubeListBuilder.create().texOffs(26, 0).addBox(-0.5F, -9.0F, -2.0F, 1.0F, 3.0F, 4.0F), PartPose.ZERO);
		head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(36, 0).addBox(-2.5F, -8.5F, -2.0F, 5.0F, 3.0F, 5.0F), PartPose.ZERO);
		head.addOrReplaceChild("bill", CubeListBuilder.create().texOffs(26, 24).addBox(-2.0F, -3.0F, -5.0F, 4.0F, 1.0F, 3.0F), PartPose.ZERO);
		head.addOrReplaceChild("caruncle", CubeListBuilder.create().texOffs(40, 24).addBox(-2.0F, -5.0F, -2.5F, 4.0F, 2.0F, 1.0F), PartPose.ZERO);
		head.addOrReplaceChild("knob", CubeListBuilder.create().texOffs(28, 36).addBox(-1.0F, -5.0F, -3.5F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
		root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(16, 36).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, 15.0F, -3.0F, 0.15F, 0.0F, 0.0F));
		root.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offset(0.0F, 15.0F, -4.5F));
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 9).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 8.0F, 6.0F),
				PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
		root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(24, 13).addBox(0.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F),
				PartPose.offset(-4.0F, 13.0F, 0.0F));
		root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(24, 13).mirror().addBox(-1.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F),
				PartPose.offset(4.0F, 13.0F, 0.0F));
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(50, 8).addBox(-2.0F, -4.0F, 0.0F, 4.0F, 4.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, 15.0F, 3.5F, -0.5F, 0.0F, 0.0F));
		root.addOrReplaceChild("fan", CubeListBuilder.create().texOffs(0, 24).addBox(-6.0F, -10.0F, 0.0F, 12.0F, 10.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 15.0F, 4.0F, -0.15F, 0.0F, 0.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(38, 13).addBox(-1.0F, 0.0F, -3.0F, 3.0F, 5.0F, 3.0F);
		root.addOrReplaceChild("right_leg", leg, PartPose.offset(-2.0F, 19.0F, 1.0F));
		root.addOrReplaceChild("left_leg", leg, PartPose.offset(1.0F, 19.0F, 1.0F));
		PartDefinition rd = root.addOrReplaceChild("right_duck_leg", CubeListBuilder.create().texOffs(0, 36).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
				PartPose.offset(-1.5F, 19.0F, 1.0F));
		rd.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(52, 24).addBox(-1.5F, 4.0F, -3.0F, 3.0F, 1.0F, 3.0F), PartPose.ZERO);
		PartDefinition ld = root.addOrReplaceChild("left_duck_leg", CubeListBuilder.create().texOffs(0, 36).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
				PartPose.offset(1.5F, 19.0F, 1.0F));
		ld.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(52, 24).addBox(-1.5F, 4.0F, -3.0F, 3.0F, 1.0F, 3.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}
}
