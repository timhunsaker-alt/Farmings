package dev.farmings.client;

import dev.farmings.health.HealthSigns;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The poses of sickness, applied after a model's own walk (docs/animal-health.md, "What the player sees"): a sick animal stands and walks
 * with its head low; a lame one limps on its right fore, a short stiff stride on that leg and a longer one on the sound leg, the head
 * nodding up as the lame foot lands (the head bob a stockperson looks for), the foot held up a little at rest. Drawn, not measured.
 */
public final class HealthSignsPose {
	/** Head low: dropped 26 degrees and 1.5 px (proposed). */
	public static final float HEAD_DROP = 0.45F;
	public static final float HEAD_SINK = 1.5F;

	private HealthSignsPose() {
	}

	/** A four-legged Farmings model: cattle, sheep, goats and pigs. */
	public static void quadruped(int signs, ModelPart head, ModelPart lameLeg, ModelPart soundLeg, float pos, float speed) {
		if (HealthSigns.has(signs, HealthSigns.SICK)) {
			head.xRot += HEAD_DROP;
			head.y += HEAD_SINK;
		}
		if (HealthSigns.has(signs, HealthSigns.LAME)) {
			limp(head, lameLeg, soundLeg, pos, speed);
		}
	}

	static void limp(ModelPart head, ModelPart lameLeg, ModelPart soundLeg, float pos, float speed) {
		float walking = Math.min(1.0F, speed * 2.0F);
		// the lame leg: a short, stiff stride; the sound one reaches further to carry the weight
		lameLeg.xRot = lameLeg.xRot * 0.35F - 0.12F * (1.0F - walking);
		soundLeg.xRot *= 1.25F;
		// the foot lifted off the ground at rest, and through its stance when walking
		float stance = Math.max(0.0F, Mth.cos(pos * 0.6662F));
		lameLeg.y -= 0.8F * (1.0F - walking) + 0.9F * walking * stance;
		// the head bob: up as the lame foot lands, down onto the sound one
		head.y -= 2.0F * walking * stance;
		head.y += 0.8F * walking * (1.0F - stance);
	}
}
