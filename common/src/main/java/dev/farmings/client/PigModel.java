package dev.farmings.client;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;

/** The pig model: drooping ears on Duroc blood, erect otherwise (Purdue breed ID; pigs.md); a piglet's big head. */
public class PigModel extends QuadrupedModel<PigRenderState> {
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart rightDroop;
	private final ModelPart leftDroop;

	public PigModel(ModelPart root) {
		super(root);
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.rightDroop = head.getChild("right_droop");
		this.leftDroop = head.getChild("left_droop");
	}

	@Override
	public void setupAnim(PigRenderState s) {
		super.setupAnim(s);
		boolean droop = s.droop >= 0.5F;
		rightDroop.visible = leftDroop.visible = droop;
		rightEar.visible = leftEar.visible = !droop;
		if (s.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.4F;
		}
	}
}
