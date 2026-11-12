package dev.farmings;

import dev.farmings.entity.CattleEntity;
import dev.farmings.entity.FowlEntity;
import dev.farmings.genetics.Species;
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

	// Poultry (docs/animals/poultry.md). Sizes: the vanilla chicken's 0.4 x 0.7, scaled for each species (proposed).
	public static final EntityType<FowlEntity> CHICKEN = fowl("chicken", 0.4F, 0.7F);
	public static final EntityType<FowlEntity> TURKEY = fowl("turkey", 0.75F, 1.2F);
	public static final EntityType<FowlEntity> QUAIL = fowl("quail", 0.3F, 0.4F);
	public static final EntityType<FowlEntity> DUCK = fowl("duck", 0.45F, 0.7F);

	private static EntityType<FowlEntity> fowl(String name, float width, float height) {
		return EntityType.Builder.<FowlEntity>of(FowlEntity::new, MobCategory.CREATURE)
				.sized(width, height)
				.eyeHeight(height * 0.85F)
				.clientTrackingRange(10)
				.build(ResourceKey.create(Registries.ENTITY_TYPE, Farmings.id(name)));
	}

	public static EntityType<FowlEntity> fowl(Species s) {
		return switch (s) {
			case TURKEY -> TURKEY;
			case QUAIL -> QUAIL;
			case DUCK -> DUCK;
			default -> CHICKEN;
		};
	}

	/** The species a poultry entity type is. Called from the entity constructor, so it compares by identity once the types exist. */
	public static Species speciesOf(EntityType<?> type) {
		if (type == TURKEY) {
			return Species.TURKEY;
		}
		if (type == QUAIL) {
			return Species.QUAIL;
		}
		if (type == DUCK) {
			return Species.DUCK;
		}
		return Species.CHICKEN;
	}

	/** A Border Collie (docs/animals/sheep.md, "Sheep dog"): 53 cm at the withers (KC standard), drawn on the wolf's frame. */
	public static final EntityType<dev.farmings.entity.SheepDogEntity> SHEEP_DOG = EntityType.Builder.<dev.farmings.entity.SheepDogEntity>of(dev.farmings.entity.SheepDogEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 0.85F)
			.eyeHeight(0.68F)
			.clientTrackingRange(10)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Farmings.id("sheep_dog")));

	/** The field gantry while it glides along its rails (docs/multiblocks/field-gantry.md, "The lift"). */
	public static final EntityType<dev.farmings.entity.FieldGantryEntity> FIELD_GANTRY = EntityType.Builder.<dev.farmings.entity.FieldGantryEntity>of(dev.farmings.entity.FieldGantryEntity::new, MobCategory.MISC)
			.noLootTable()
			.sized(1.0F, 1.0F)
			.clientTrackingRange(16)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Farmings.id("field_gantry")));

	/** A thrown rotten egg (docs/eggs.md): a snowball's size and tracking. */
	public static final EntityType<dev.farmings.entity.ThrownRottenEgg> ROTTEN_EGG = EntityType.Builder.<dev.farmings.entity.ThrownRottenEgg>of(dev.farmings.entity.ThrownRottenEgg::new, MobCategory.MISC)
			.noLootTable()
			.sized(0.25F, 0.25F)
			.clientTrackingRange(4)
			.updateInterval(10)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Farmings.id("rotten_egg")));

	public static final java.util.List<EntityType<FowlEntity>> FOWL = java.util.List.of(CHICKEN, TURKEY, QUAIL, DUCK);

	private FarmingsEntities() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("cattle"), CATTLE);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("sheep"), SHEEP);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("chicken"), CHICKEN);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("turkey"), TURKEY);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("quail"), QUAIL);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("duck"), DUCK);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("rotten_egg"), ROTTEN_EGG);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("sheep_dog"), SHEEP_DOG);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Farmings.id("field_gantry"), FIELD_GANTRY);
	}
}
