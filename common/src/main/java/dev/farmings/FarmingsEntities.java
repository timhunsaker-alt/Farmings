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

	/** A sheep: the vanilla sheep's size (0.9 x 1.3). */
	public static final EntityType<dev.farmings.entity.SheepEntity> SHEEP = EntityType.Builder.<dev.farmings.entity.SheepEntity>of(dev.farmings.entity.SheepEntity::new, MobCategory.CREATURE)
			.sized(0.9F, 1.3F)
			.eyeHeight(1.235F)
			.clientTrackingRange(10)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Farmings.id("sheep")));

	private FarmingsEntities() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("cattle"), CATTLE);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("sheep"), SHEEP);
	}
}
