package dev.farmings.client;

import dev.farmings.genetics.PoultryLook;
import dev.farmings.genetics.Species;
import dev.farmings.husbandry.poultry.FowlBreed;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The bird model with its species' parts shown (docs/animals/poultry.md, "Movement and the model"): a chicken's comb and wattles (rose
 * and walnut combs drawn low, the Silkie's crest), the turkey tom's fan, snood and beard, the quail's short tail, the duck's bill, webbed
 * feet and the Muscovy's red face, the Pekin's upright stance. Proportions are drawn, not measured *(proposed)*.
 */
public class FowlModel extends EntityModel<FowlRenderState> {
	private final ModelPart head;
	private final ModelPart beak;
	private final ModelPart wattle;
	private final ModelPart snood;
	private final ModelPart comb;
	private final ModelPart crest;
	private final ModelPart bill;
	private final ModelPart caruncle;
	private final ModelPart knob;
	private final ModelPart casque;
	private final ModelPart neck;
	private final ModelPart beard;
	private final ModelPart body;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart tail;
	private final ModelPart fan;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightDuckLeg;
	private final ModelPart leftDuckLeg;

	public FowlModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		beak = head.getChild("beak");
		wattle = head.getChild("wattle");
		snood = head.getChild("snood");
		comb = head.getChild("comb");
		crest = head.getChild("crest");
		bill = head.getChild("bill");
		caruncle = head.getChild("caruncle");
		knob = head.getChild("knob");
		casque = head.getChild("casque");
		neck = root.getChild("neck");
		beard = root.getChild("beard");
		body = root.getChild("body");
		rightWing = root.getChild("right_wing");
		leftWing = root.getChild("left_wing");
		tail = root.getChild("tail");
		fan = root.getChild("fan");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
		rightDuckLeg = root.getChild("right_duck_leg");
		leftDuckLeg = root.getChild("left_duck_leg");
	}

	@Override
	public void setupAnim(FowlRenderState s) {
		super.setupAnim(s);
		boolean goose = s.species == Species.GOOSE;
		boolean duck = s.species == Species.DUCK || goose;     // a goose has the duck's bill and webbed feet
		boolean chicken = s.species == Species.CHICKEN;
		boolean turkey = s.species == Species.TURKEY;
		boolean grown = !s.young;
		// what each species has
		beak.visible = !duck;
		bill.visible = duck;
		caruncle.visible = duck && s.breed == FowlBreed.MUSCOVY && grown;
		knob.visible = goose && s.breed == FowlBreed.CHINESE && grown;
		boolean guinea = s.species == Species.GUINEA;
		casque.visible = guinea && grown;
		neck.visible = goose && grown;
		comb.visible = chicken && grown && !s.look.silky();
		crest.visible = chicken && s.look.silky();
		wattle.visible = (chicken || turkey || guinea) && grown;
		snood.visible = turkey && grown;
		beard.visible = turkey && grown && s.male;
		fan.visible = turkey && grown && s.male;
		tail.visible = !fan.visible;
		rightLeg.visible = leftLeg.visible = !duck;
		rightDuckLeg.visible = leftDuckLeg.visible = duck;
		// combs: a single comb stands tall, rose, pea and walnut sit low; a rooster's is bigger
		float combSize = s.male ? 1.3F : 1.0F;
		comb.yScale = (s.look.comb() == PoultryLook.Comb.SINGLE ? 1.0F : 0.45F) * combSize;
		comb.zScale = combSize;
		wattle.yScale = s.male ? 1.4F : 1.0F;
		tail.yScale = s.species == Species.QUAIL ? 0.4F : chicken && s.male ? 1.6F : 1.0F;
		tail.xRot = duck ? 0.2F : -0.5F;
		// head looks where the bird looks
		head.xRot = s.xRot * Mth.DEG_TO_RAD;
		head.yRot = s.yRot * Mth.DEG_TO_RAD;
		beard.yRot = head.yRot * 0.3F;
		// walking and flapping, as the vanilla chicken moves
		float walk = Mth.cos(s.walkAnimationPos * 0.6662F) * 1.4F * s.walkAnimationSpeed;
		rightLeg.xRot = rightDuckLeg.xRot = walk;
		leftLeg.xRot = leftDuckLeg.xRot = -walk;
		float flap = s.flapping ? (Mth.sin(s.ageInTicks * 1.3F) + 1.0F) * 0.7F : 0.0F;
		rightWing.zRot = flap;
		leftWing.zRot = -flap;
		// a broody hen settles low over her eggs
		if (s.broody) {
			body.y += 2.0F;
			head.y += 2.0F;
			rightWing.y += 2.0F;
			leftWing.y += 2.0F;
			tail.y += 2.0F;
			rightLeg.visible = leftLeg.visible = rightDuckLeg.visible = leftDuckLeg.visible = false;
		}
		// the Pekin stands upright
		if (s.breed == FowlBreed.PEKIN && grown) {
			body.xRot -= 0.45F;
			head.y -= 1.5F;
		}
		// a goose carries its head high on a long neck
		if (goose && grown) {
			head.y -= 5.0F;
			head.z -= 0.5F;
			head.xScale = head.yScale = head.zScale = 0.7F;
			body.xRot -= 0.15F;
		}
		// a guinea fowl: a small bare head on a round, hunched body
		if (guinea && grown) {
			head.xScale = head.yScale = head.zScale = 0.75F;
			head.y += 1.0F;
			body.xScale = 1.15F;
			tail.yScale = 0.6F;
		}
		// young birds: a big head on a small round body
		if (s.young) {
			head.xScale = head.yScale = head.zScale = 1.3F;
		}
	}
}
