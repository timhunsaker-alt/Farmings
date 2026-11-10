package dev.farmings.client;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;

/** What the sheep's skin and fleece models share: the walking pose and the body shape from the breed shares, so the two line up. */
abstract class SheepBaseModel extends QuadrupedModel<SheepRenderState> {
	SheepBaseModel(ModelPart root) {
		super(root);
	}

	/**
	 * Meat blood is wider and deeper with shorter legs, dairy blood a little longer in the leg and narrower, a ram a little wider. Drawn,
	 * not measured *(proposed)*. The shift keeps the hooves on the ground.
	 */
	void shape(SheepRenderState s) {
		float width = 1.0F + 0.15F * s.meat - 0.06F * s.dairy + (s.ram ? 0.05F : 0.0F);
		float depth = 1.0F + 0.12F * s.meat;
		float legLength = 1.0F - 0.10F * s.meat + 0.10F * s.dairy;
		float shift = 12.0F * (1.0F - legLength);
		body.xScale *= width;
		body.zScale *= depth;
		body.y += shift;
		head.y += shift;
		for (ModelPart leg : new ModelPart[] {rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg}) {
			leg.yScale *= legLength;
			leg.y += shift;
			leg.x *= width;
		}
	}
}
