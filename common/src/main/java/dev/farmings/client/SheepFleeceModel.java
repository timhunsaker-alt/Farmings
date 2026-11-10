package dev.farmings.client;

import net.minecraft.client.model.geom.ModelPart;

/**
 * The fleece over the skin model. The body is built at its thickest ({@link SheepModels#MAX_WOOL}) and scaled to the wool the sheep
 * has now, so a just-shorn sheep shows only a thin coat and a full fleece stands well out from the body. The poll cap shows only on a
 * fine-wool head (white face, crimp 2 or more: Merino-like), the leg wool only on a sheep that is not of meat type.
 */
public class SheepFleeceModel extends SheepBaseModel {
	public SheepFleeceModel(ModelPart root) {
		super(root);
	}

	/** Wool thickness in model pixels for a fleece of {@code years} growth: 2 at a year, the full inflation at two *(drawn)*. */
	static float thickness(float years) {
		float y = Math.max(0.0F, years);
		return 0.25F + 1.75F * Math.min(1.0F, y) + (SheepModels.MAX_WOOL - 2.0F) * Math.max(0.0F, Math.min(1.0F, y - 1.0F));
	}

	@Override
	public void setupAnim(SheepRenderState s) {
		super.setupAnim(s);
		float t = thickness(s.fleece);
		float m = SheepModels.MAX_WOOL;
		// the body box is 8 x 16 x 6 (x, along, up): each side grows by t instead of the built-in m
		body.xScale = (8.0F + 2 * t) / (8.0F + 2 * m);
		body.yScale = (16.0F + 2 * t) / (16.0F + 2 * m);
		body.zScale = (6.0F + 2 * t) / (6.0F + 2 * m);
		head.visible = s.look.head() == 0 && s.look.crimp() >= 2 && s.fleece > 0.2F;
		boolean legWool = s.meat < 0.5F && s.fleece > 0.2F;
		rightHindLeg.visible = leftHindLeg.visible = rightFrontLeg.visible = leftFrontLeg.visible = legWool;
		shape(s);
		if (s.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.35F;
		}
	}
}
