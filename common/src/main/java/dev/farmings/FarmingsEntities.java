package dev.farmings;

import dev.farmings.entity.CattleEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** The animals. Registered with the other content while the registries are open. */
public final class FarmingsEntities {
	/** Same size as a vanilla cow until the Ludo models are in. */
	public static final EntityType<CattleEntity> CATTLE = EntityType.Builder.<CattleEntity>of(CattleEntity::new, MobCategory.CREATURE)
			.sized(0.9F, 1.4F)
			.eyeHeight(1.3F)
			.clientTrackingRange(10)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Farmings.id("cattle")));

	private FarmingsEntities() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("cattle"), CATTLE);
	}
}
