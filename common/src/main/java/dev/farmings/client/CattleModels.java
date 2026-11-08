package dev.farmings.client;

import dev.farmings.Farmings;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** The Gyr's own model: the vanilla cow with a hump on the shoulders. Every other breed uses the vanilla cow model. */
public final class CattleModels {
	public static final ModelLayerLocation GYR = new ModelLayerLocation(Farmings.id("gyr_cattle"), "main");

	private CattleModels() {
	}

	public static LayerDefinition createGyrLayer() {
		MeshDefinition mesh = CowModel.createBaseCowModel();
		// The body part is turned on its side, so "up" is +z here: a 6 wide, 7 long, 3 tall box over the shoulders, texture at (0, 40).
		mesh.getRoot().getChild("body").addOrReplaceChild("hump", CubeListBuilder.create().texOffs(0, 40).addBox(-3.0F, -9.0F, 3.0F, 6.0F, 7.0F, 3.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}
}
