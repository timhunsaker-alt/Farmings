package dev.farmings.client;

import net.minecraft.client.model.geom.ModelPart;

/**
 * The sheep's skin model with the genes applied each frame (after the walking pose): horns for a horned sheep (scurs drawn as short
 * horns), an udder on a ewe that is in lamb or has lambed, the East Friesian's long bare "rat tail" (others a short docked tail), and the
 * body shape: meat blood is wider and deeper with shorter legs, dairy blood a little longer in the leg and narrower, a ram heavier in the
 * head. These proportions are drawn, not measured *(proposed)*.
 */
public class SheepModel extends SheepBaseModel {
	private final ModelPart rightHorn;
	private final ModelPart leftHorn;
	private final ModelPart udder;
	private final ModelPart tail;

	public SheepModel(ModelPart root) {
		super(root);
		this.rightHorn = head.getChild("right_horn");
		this.leftHorn = head.getChild("left_horn");
		this.udder = body.getChild("udder");
		this.tail = root.getChild("tail");
	}

	@Override
	public void setupAnim(SheepRenderState s) {
		super.setupAnim(s);
		boolean lamb = s.isBaby;
		rightHorn.visible = leftHorn.visible = !lamb && s.look.horns() > 0;
		float hornSize = s.look.horns() == 2 ? (s.ram ? 1.15F : 0.8F) : 0.55F;
		rightHorn.xScale = rightHorn.yScale = rightHorn.zScale = hornSize;
		leftHorn.xScale = leftHorn.yScale = leftHorn.zScale = hornSize;
		udder.visible = s.udder && !lamb;
		tail.yScale = s.dairy >= 0.5F ? 1.0F : 0.4F;
		shape(s);
		if (lamb) {
			head.xScale = head.yScale = head.zScale = 1.35F;
		} else if (s.ram) {
			head.xScale = head.yScale = head.zScale = 1.08F;
		}
	}
}
