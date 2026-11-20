package dev.farmings.client;

import net.minecraft.client.model.geom.ModelPart;

/** A Farmings animal model the signs layer can draw on: the legs that take the red band, and the body that takes erysipelas' marks. */
public interface HealthSignsModel {
	/** The hind legs (a withdrawal band goes round each, as dairies band both hind legs so it shows from either side). */
	ModelPart[] bandLegs();

	/** The body; null where no marks are drawn. */
	default ModelPart markBody() {
		return null;
	}
}
