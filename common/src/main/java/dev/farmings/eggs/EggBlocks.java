package dev.farmings.eggs;

import dev.farmings.Farmings;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The egg grading table (docs/eggs.md). */
public final class EggBlocks {
	public static final Block EGG_GRADER = new EggGraderBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Farmings.id("egg_grader")))
			.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion());
	public static final BlockEntityType<EggGraderEntity> EGG_GRADER_ENTITY = new BlockEntityType<>(EggGraderEntity::new, Set.of(EGG_GRADER));

	private EggBlocks() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, Farmings.id("egg_grader"), EGG_GRADER);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Farmings.id("egg_grader"));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(EGG_GRADER, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("egg_grader"), EGG_GRADER_ENTITY);
	}
}
