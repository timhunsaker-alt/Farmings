package dev.farmings.health;

import dev.farmings.Farmings;
import dev.farmings.genetics.Species;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

/** Animal health's sounds (docs/animal-health.md, "Sounds and art"): a cough per species, the footbath, the CMT paddle, the vet kit. */
public final class HealthSounds {
	public static final SoundEvent CATTLE_COUGH = event("entity.cattle.cough");
	public static final SoundEvent SHEEP_COUGH = event("entity.sheep.cough");
	public static final SoundEvent PIG_COUGH = event("entity.pig.cough");
	public static final SoundEvent HORSE_COUGH = event("entity.horse.cough");
	public static final SoundEvent FOOTBATH = event("block.footbath.splash");
	public static final SoundEvent CMT = event("item.cmt_paddle.swirl");
	public static final SoundEvent VET_KIT = event("item.vet_kit.open");
	private static final SoundEvent[] ALL = {CATTLE_COUGH, SHEEP_COUGH, PIG_COUGH, HORSE_COUGH, FOOTBATH, CMT, VET_KIT};

	private HealthSounds() {
	}

	private static SoundEvent event(String name) {
		return SoundEvent.createVariableRangeEvent(Farmings.id(name));
	}

	public static void register() {
		for (SoundEvent e : ALL) {
			Registry.register(BuiltInRegistries.SOUND_EVENT, e.location(), e);
		}
	}

	/** The cough for a species; birds have none. */
	public static @Nullable SoundEvent cough(Species s) {
		return switch (s) {
			case CATTLE -> CATTLE_COUGH;
			case SHEEP, GOAT -> SHEEP_COUGH;
			case PIG -> PIG_COUGH;
			case HORSE -> HORSE_COUGH;
			default -> null;
		};
	}
}
