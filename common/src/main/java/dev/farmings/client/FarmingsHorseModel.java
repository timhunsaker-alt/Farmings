package dev.farmings.client;

import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelPart;

/** Vanilla's horse model (so saddles and armour fit) with the signs of sickness: head low, the limp, the leg band (docs/animal-health.md). */
public class FarmingsHorseModel extends HorseModel implements HealthSignsModel {
	public FarmingsHorseModel(ModelPart root) {
		super(root);
	}

	@Override
	public void setupAnim(net.minecraft.client.renderer.entity.state.EquineRenderState state) {
		super.setupAnim(state);
		if (state instanceof HorseRenderState s) {
			HealthSignsPose.quadruped(s.healthSigns, headParts, rightFrontLeg, leftFrontLeg, s.walkAnimationPos, s.walkAnimationSpeed);
		}
	}

	@Override
	public ModelPart[] bandLegs() {
		return new ModelPart[] {rightHindLeg, leftHindLeg};
	}
}
