package dev.farmings.client;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * The goat model with the genes applied each frame: lop or erect ears by the Nubian and Boer share, horns unless polled (a buck's
 * larger), a beard and wattles when the look has them, an udder on a doe in kid or in milk, and the body shape: Boer blood wider and
 * deeper with shorter legs, dairy blood leaner and longer in the leg. Proportions are drawn, not measured *(proposed)*.
 */
public class GoatModel extends QuadrupedModel<GoatRenderState> {
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart rightLop;
	private final ModelPart leftLop;
	private final ModelPart rightHorn;
	private final ModelPart leftHorn;
	private final ModelPart beard;
	private final ModelPart rightWattle;
	private final ModelPart leftWattle;
	private final ModelPart udder;

	public GoatModel(ModelPart root) {
		super(root);
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.rightLop = head.getChild("right_lop");
		this.leftLop = head.getChild("left_lop");
		this.rightHorn = head.getChild("right_horn");
		this.leftHorn = head.getChild("left_horn");
		this.beard = head.getChild("beard");
		this.rightWattle = head.getChild("right_wattle");
		this.leftWattle = head.getChild("left_wattle");
		this.udder = body.getChild("udder");
	}

	@Override
	public void setupAnim(GoatRenderState s) {
		super.setupAnim(s);
		boolean kid = s.isBaby;
		boolean lop = s.lop >= 0.5F;
		rightLop.visible = leftLop.visible = lop;
		rightEar.visible = leftEar.visible = !lop;
		rightHorn.visible = leftHorn.visible = !kid && !s.look.polled();
		float horn = s.buck ? 1.0F : 0.65F;
		rightHorn.xScale = rightHorn.yScale = rightHorn.zScale = horn;
		leftHorn.xScale = leftHorn.yScale = leftHorn.zScale = horn;
		beard.visible = !kid && s.look.beard();
		beard.yScale = s.buck ? 1.3F : 0.8F;
		rightWattle.visible = leftWattle.visible = s.look.wattles();
		udder.visible = s.udder && !kid;
		udder.xScale = udder.yScale = udder.zScale = 1.0F + 0.3F * s.dairy;
		float width = 1.0F + 0.18F * s.meat - 0.08F * s.dairy + (s.buck ? 0.06F : 0.0F);
		float depth = 1.0F + 0.14F * s.meat;
		float legLength = 1.0F - 0.10F * s.meat + 0.08F * s.dairy;
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
		if (kid) {
			head.xScale = head.yScale = head.zScale = 1.35F;
		}
	}
}
