package dev.farmings.client;

import dev.farmings.genetics.VisualPhenotype;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * The cattle model with the coat genes applied (CattleModels has the parts). Each frame, after the vanilla cow's walking pose, it shows the
 * parts this animal has and shapes the body:
 * <ul>
 *   <li>horns if horned (not on calves); long drooping ears for ear length 2-3; a hump of the gene's size; a dewlap sized by the gene;</li>
 *   <li>an udder on a cow that has been bred; a crest on a bull;</li>
 *   <li>beef blood makes the body wider and deeper and the legs shorter and thicker; zebu blood makes the legs a little longer. The shift
 *       keeps the hooves on the ground. These proportions are drawn, not measured *(proposed)*.</li>
 * </ul>
 */
public class CattleModel extends CowModel {
	private final ModelPart rightHorn;
	private final ModelPart leftHorn;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart rightLongEar;
	private final ModelPart leftLongEar;
	private final ModelPart udder;
	private final ModelPart crest;
	private final ModelPart dewlap;
	private final ModelPart[] humps;

	public CattleModel(ModelPart root) {
		super(root);
		ModelPart head = root.getChild("head");
		ModelPart body = root.getChild("body");
		this.rightHorn = head.getChild("right_horn");
		this.leftHorn = head.getChild("left_horn");
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.rightLongEar = head.getChild("right_long_ear");
		this.leftLongEar = head.getChild("left_long_ear");
		this.udder = body.getChild("udder");
		this.crest = body.getChild("crest");
		this.dewlap = body.getChild("dewlap");
		this.humps = new ModelPart[] {body.getChild("hump_small"), body.getChild("hump_medium"), body.getChild("hump_large")};
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		if (!(state instanceof CattleRenderState c)) {
			return;
		}
		VisualPhenotype look = c.look;
		boolean calf = c.isBaby;
		rightHorn.visible = leftHorn.visible = look.horned() && !calf;
		boolean longEars = look.ears() >= 2;
		rightLongEar.visible = leftLongEar.visible = longEars;
		rightEar.visible = leftEar.visible = !longEars;
		for (int i = 0; i < humps.length; i++) {
			humps[i].visible = !calf && look.hump() == i + 1;
		}
		dewlap.visible = !calf && look.dewlap() > 0;
		dewlap.yScale = dewlap.zScale = 0.5F + 0.25F * look.dewlap();
		udder.visible = c.udder;
		udder.xScale = udder.yScale = udder.zScale = c.beef > 0.5F ? 0.6F : 1.0F;
		crest.visible = c.bull;

		float b = c.beef;
		float z = c.zebu;
		float width = 0.95F + 0.25F * b + (c.bull ? 0.06F : 0.0F);
		float depth = 1.0F + 0.15F * b + (c.bull ? 0.05F : 0.0F);
		body.xScale = width;
		body.zScale = depth;
		float legLength = 1.0F - 0.18F * b + 0.08F * z;
		float legThick = 1.0F + 0.15F * b;
		float shift = 12.0F * (1.0F - legLength);
		body.y += shift;
		head.y += shift;
		for (ModelPart leg : new ModelPart[] {rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg}) {
			leg.yScale = legLength;
			leg.xScale = leg.zScale = legThick;
			leg.y += shift;
			leg.x *= width;
		}
		if (calf) {
			head.xScale = head.yScale = head.zScale = 1.35F;
		} else if (c.bull) {
			head.xScale = head.yScale = head.zScale = 1.08F;
		}
	}
}
