package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.farmings.Farmings;
import dev.farmings.entity.CattleEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.husbandry.Stage;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * The vanilla cow model with a coat texture for each breed (tools/cattle_textures.py draws them), and the Gyr's own model with a
 * hump. Dairy cows that have bred show an udder; calves, heifers, bulls and beef cattle do not. Polled breeds have no horns because
 * their texture leaves the horns transparent.
 */
public class CattleRenderer extends MobRenderer<CattleEntity, CattleRenderState, CowModel> {
	private final CowModel adultModel;
	private final CowModel gyrModel;
	private final CowModel calfModel;
	private final Map<CattleBreed, Identifier> withUdder = new EnumMap<>(CattleBreed.class);
	private final Map<CattleBreed, Identifier> withoutUdder = new EnumMap<>(CattleBreed.class);

	public CattleRenderer(EntityRendererProvider.Context context) {
		super(context, new CowModel(context.bakeLayer(ModelLayers.COW)), 0.7F);
		this.adultModel = new CowModel(context.bakeLayer(ModelLayers.COW));
		this.gyrModel = new CowModel(context.bakeLayer(CattleModels.GYR));
		this.calfModel = new CowModel(context.bakeLayer(ModelLayers.COW_BABY));
		for (CattleBreed breed : CattleBreed.values()) {
			String base = "textures/entity/cattle/" + breed.assetName();
			withUdder.put(breed, Farmings.id(base + ".png"));
			// Beef breeds have one texture (no udder); dairy breeds have a second one without.
			withoutUdder.put(breed, breed.use == CattleBreed.Use.DAIRY ? Farmings.id(base + "_no_udder.png") : Farmings.id(base + ".png"));
		}
	}

	@Override
	public Identifier getTextureLocation(CattleRenderState state) {
		return (state.udder ? withUdder : withoutUdder).get(state.breed);
	}

	@Override
	public CattleRenderState createRenderState() {
		return new CattleRenderState();
	}

	@Override
	public void extractRenderState(CattleEntity entity, CattleRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.breed = entity.syncedBreed();
		Stage stage = entity.syncedStage();
		state.udder = state.breed.use == CattleBreed.Use.DAIRY
				&& (stage == Stage.BRED_HEIFER || stage == Stage.LACTATING_COW || stage == Stage.DRY_COW || stage == Stage.RETIRED_COW);
	}

	@Override
	public void submit(CattleRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		this.model = state.isBaby ? calfModel : (state.breed == CattleBreed.GYR ? gyrModel : adultModel);
		super.submit(state, poseStack, collector, camera);
	}
}
