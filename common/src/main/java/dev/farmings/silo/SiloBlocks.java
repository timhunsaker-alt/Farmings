package dev.farmings.silo;

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

/** The tower silo and the grain bin (docs/multiblocks/silo.md). */
public final class SiloBlocks {
	public static final Block SILO_STAVE = block("silo_stave", p -> new Block(p.mapColor(MapColor.STONE).strength(3.0F, 6.0F).sound(SoundType.STONE)));
	public static final Block SILO_ROOF = block("silo_roof", p -> new Block(p.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL)));
	public static final Block FORAGE_BLOWER = block("forage_blower", p -> new SiloMachineBlock(SiloMachineBlock.Kind.BLOWER, p.mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.METAL)));
	public static final Block SILO_UNLOADER = block("silo_unloader", p -> new SiloMachineBlock(SiloMachineBlock.Kind.UNLOADER, p.mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.METAL)));
	public static final Block BIN_WALL = block("grain_bin_wall", p -> new Block(p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)));
	public static final Block BIN_ROOF = block("grain_bin_roof", p -> new SiloMachineBlock(SiloMachineBlock.Kind.ROOF, p.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL)));
	public static final Block GRAIN_BIN_HOPPER = block("grain_bin_hopper", p -> new SiloMachineBlock(SiloMachineBlock.Kind.HOPPER, p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)));
	public static final BlockEntityType<BlowerEntity> BLOWER_ENTITY = new BlockEntityType<>(BlowerEntity::new, Set.of(FORAGE_BLOWER));
	public static final BlockEntityType<SiloEntity> UNLOADER_ENTITY = new BlockEntityType<>(SiloEntity::new, Set.of(SILO_UNLOADER));
	public static final BlockEntityType<GrainBinEntity> HOPPER_ENTITY = new BlockEntityType<>(GrainBinEntity::new, Set.of(GRAIN_BIN_HOPPER));
	public static final BlockEntityType<BinRoofEntity> ROOF_ENTITY = new BlockEntityType<>(BinRoofEntity::new, Set.of(BIN_ROOF));
	public static final Block[] ALL = {SILO_STAVE, SILO_ROOF, FORAGE_BLOWER, SILO_UNLOADER, BIN_WALL, BIN_ROOF, GRAIN_BIN_HOPPER};
	public static final String[] NAMES = {"silo_stave", "silo_roof", "forage_blower", "silo_unloader", "grain_bin_wall", "grain_bin_roof", "grain_bin_hopper"};

	private SiloBlocks() {
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		return factory.apply(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Farmings.id(name))));
	}

	public static void register() {
		for (int i = 0; i < ALL.length; i++) {
			Registry.register(BuiltInRegistries.BLOCK, Farmings.id(NAMES[i]), ALL[i]);
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Farmings.id(NAMES[i]));
			Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(ALL[i], new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		}
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("forage_blower"), BLOWER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("silo_unloader"), UNLOADER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("grain_bin_hopper"), HOPPER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("grain_bin_roof"), ROOF_ENTITY);
	}
}
