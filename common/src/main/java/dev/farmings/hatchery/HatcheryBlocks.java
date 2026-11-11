package dev.farmings.hatchery;

import dev.farmings.Farmings;
import java.util.Set;
import java.util.function.Function;
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

/** The hatchery's machines (docs/multiblocks/hatchery.md): the incubator and the brooder, both on Wildcatter low-voltage power. */
public final class HatcheryBlocks {
	public static final Block INCUBATOR = block("incubator", p -> new IncubatorBlock(p.mapColor(MapColor.METAL).strength(2.0F, 3.0F).sound(SoundType.METAL).noOcclusion()));
	public static final Block BROODER = block("brooder", p -> new BrooderBlock(p.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()
			.lightLevel(s -> 10)));
	public static final BlockEntityType<IncubatorEntity> INCUBATOR_ENTITY = new BlockEntityType<>(IncubatorEntity::new, Set.of(INCUBATOR));
	public static final BlockEntityType<BrooderEntity> BROODER_ENTITY = new BlockEntityType<>(BrooderEntity::new, Set.of(BROODER));
	public static final Block[] ALL = {INCUBATOR, BROODER};
	public static final String[] NAMES = {"incubator", "brooder"};

	private HatcheryBlocks() {
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Farmings.id(name));
		return factory.apply(BlockBehaviour.Properties.of().setId(key));
	}

	public static void register() {
		for (int i = 0; i < ALL.length; i++) {
			Registry.register(BuiltInRegistries.BLOCK, Farmings.id(NAMES[i]), ALL[i]);
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Farmings.id(NAMES[i]));
			Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(ALL[i], new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		}
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("incubator"), INCUBATOR_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("brooder"), BROODER_ENTITY);
	}
}
