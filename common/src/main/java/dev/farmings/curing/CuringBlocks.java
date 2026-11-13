package dev.farmings.curing;

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

/** Curing, smoking, lard and sausage (docs/multiblocks/curing-room.md). */
public final class CuringBlocks {
	public static final Block CURING_RACK = block("curing_rack", p -> new CuringMachineBlock(p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion(), 0));
	public static final Block SMOKEHOUSE = block("smokehouse", p -> new CuringMachineBlock(p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD), 1));
	public static final Block LARD_KETTLE = block("lard_kettle", p -> new CuringMachineBlock(p.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion(), 2));
	public static final Block MEAT_GRINDER = block("meat_grinder", p -> new CuringMachineBlock(p.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion(), 3));
	public static final BlockEntityType<CuringRackEntity> CURING_RACK_ENTITY = new BlockEntityType<>(CuringRackEntity::new, Set.of(CURING_RACK));
	public static final BlockEntityType<SmokehouseEntity> SMOKEHOUSE_ENTITY = new BlockEntityType<>(SmokehouseEntity::new, Set.of(SMOKEHOUSE));
	public static final BlockEntityType<LardKettleEntity> LARD_KETTLE_ENTITY = new BlockEntityType<>(LardKettleEntity::new, Set.of(LARD_KETTLE));
	public static final BlockEntityType<MeatGrinderEntity> MEAT_GRINDER_ENTITY = new BlockEntityType<>(MeatGrinderEntity::new, Set.of(MEAT_GRINDER));
	public static final Block[] ALL = {CURING_RACK, SMOKEHOUSE, LARD_KETTLE, MEAT_GRINDER};
	public static final String[] NAMES = {"curing_rack", "smokehouse", "lard_kettle", "meat_grinder"};

	private CuringBlocks() {
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
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("curing_rack"), CURING_RACK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("smokehouse"), SMOKEHOUSE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("lard_kettle"), LARD_KETTLE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("meat_grinder"), MEAT_GRINDER_ENTITY);
	}
}
