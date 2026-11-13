package dev.farmings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.farmings.Farmings;
import dev.farmings.entity.HorseEntity;
import dev.farmings.genetics.HorseLook;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;

/**
 * Horses drawn from their coat genes (horses.md, "Coat colour genes") on vanilla's horse model, so vanilla saddles and armour fit: the
 * base coat by Extension, Agouti, Cream and Dun, with roan, sabino, face and leg markings and grey drawn over it (textures by
 * tools/horse_textures.py). Foals are the adult model scaled by weight, as the other Farmings animals *(proposed)*.
 */
public class HorseRenderer extends AbstractHorseRenderer<HorseEntity, HorseRenderState, HorseModel> {
	static final HorseLook DEFAULT_LOOK = new HorseLook(HorseLook.Base.BAY, 0, false, false, 0, 0, 0, false, 0);
	private static final String DIR = "textures/entity/horse/";

	public HorseRenderer(EntityRendererProvider.Context context) {
		super(context, new HorseModel(context.bakeLayer(ModelLayers.HORSE)), new HorseModel(context.bakeLayer(ModelLayers.HORSE)));
		addLayer(new Markings(this));
		addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_BODY, s -> s.bodyArmorItem,
				new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR)), null, 2));
		addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_SADDLE, s -> s.saddle,
				new EquineSaddleModel(context.bakeLayer(ModelLayers.HORSE_SADDLE)), null, 2));
	}

	@Override
	public Identifier getTextureLocation(HorseRenderState s) {
		return Farmings.id(DIR + s.look.texture() + ".png");
	}

	/** Size by weight against the breed's mature weight, a cube root *(proposed)*: a foal is drawn small, a grown horse at vanilla's size. */
	@Override
	protected void scale(HorseRenderState state, PoseStack pose) {
		super.scale(state, pose);
		float share = state.matureKg <= 0 ? 1 : state.weightKg / state.matureKg;
		float k = (float) Math.max(0.4, Math.min(1.05, Math.cbrt(Math.max(0.05, share))));
		pose.scale(k, k, k);
	}

	@Override
	public HorseRenderState createRenderState() {
		return new HorseRenderState();
	}

	@Override
	public void extractRenderState(HorseEntity entity, HorseRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		HorseLook look = entity.syncedLook();
		state.look = look == null ? DEFAULT_LOOK : look;
		state.weightKg = entity.syncedKg();
		state.matureKg = entity.syncedMatureKg();
	}

	private static final class Markings extends RenderLayer<HorseRenderState, HorseModel> {
		Markings(RenderLayerParent<HorseRenderState, HorseModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, HorseRenderState s, float yRot, float xRot) {
			int order = 1;
			HorseLook l = s.look;
			if (l.roan()) {
				order = draw("roan", pose, collector, light, s, order);
			}
			if (l.sabino() > 0) {
				order = draw("sabino_" + Math.min(2, l.sabino()), pose, collector, light, s, order);
			}
			if (l.face() > 0) {
				order = draw("face_" + Math.min(2, l.face()), pose, collector, light, s, order);
			}
			if (l.socks()) {
				order = draw("socks", pose, collector, light, s, order);
			}
			if (l.grey() > 0) {
				draw("grey_" + Math.min(3, l.grey()), pose, collector, light, s, order);
			}
		}

		private int draw(String name, PoseStack pose, SubmitNodeCollector collector, int light, HorseRenderState s, int order) {
			renderColoredCutoutModel(getParentModel(), Farmings.id(DIR + name + ".png"), pose, collector, light, s, -1, order);
			return order + 1;
		}
	}
}
