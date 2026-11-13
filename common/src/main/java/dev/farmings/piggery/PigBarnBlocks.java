package dev.farmings.piggery;

import dev.farmings.Farmings;
import dev.farmings.shed.ShedFloorBlock;
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

/**
 * The pig barn's own blocks (docs/multiblocks/pig-barn.md). Its feeders and drinkers are the barn's hay feeder (it takes grain, meal and
 * rations) and water trough; its pasture gate is the sheep shed's.
 */
public final class PigBarnBlocks {
	public static final Block PIG_BARN_CONTROLLER = block("pig_barn_controller", p -> new PigBarnControllerBlock(p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD)));
	public static final Block SLATTED_FLOOR = block("slatted_floor", p -> new ShedFloorBlock(p.mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noOcclusion()));
	public static final Block FARROWING_CRATE = block("farrowing_crate", p -> new ShedFloorBlock(p.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()));
	public static final Block HEAT_LAMP = block("heat_lamp", p -> new HeatLampBlock(p.mapColor(MapColor.COLOR_RED).strength(0.5F).sound(SoundType.GLASS).noOcclusion()
			.lightLevel(s -> 9)));
	public static final Block SLURRY_PUMP = block("slurry_pump", p -> new SlurryPumpBlock(p.mapColor(MapColor.METAL).strength(2.0F, 3.0F).sound(SoundType.METAL)));
	public static final Block SLURRY_PIPE = block("slurry_pipe", p -> new Block(p.mapColor(MapColor.COLOR_GRAY).strength(1.0F).sound(SoundType.METAL)));
	public static final Block SLURRY_STORE = block("slurry_store", p -> new SlurryStoreBlock(p.mapColor(MapColor.METAL).strength(2.0F, 3.0F).sound(SoundType.METAL)));
	public static final BlockEntityType<PigBarnControllerEntity> PIG_BARN_CONTROLLER_ENTITY = new BlockEntityType<>(PigBarnControllerEntity::new, Set.of(PIG_BARN_CONTROLLER));
	public static final BlockEntityType<HeatLampEntity> HEAT_LAMP_ENTITY = new BlockEntityType<>(HeatLampEntity::new, Set.of(HEAT_LAMP));
	public static final BlockEntityType<SlurryPumpEntity> SLURRY_PUMP_ENTITY = new BlockEntityType<>(SlurryPumpEntity::new, Set.of(SLURRY_PUMP));
	public static final BlockEntityType<SlurryStoreEntity> SLURRY_STORE_ENTITY = new BlockEntityType<>(SlurryStoreEntity::new, Set.of(SLURRY_STORE));
	public static final Block[] ALL = {PIG_BARN_CONTROLLER, SLATTED_FLOOR, FARROWING_CRATE, HEAT_LAMP, SLURRY_PUMP, SLURRY_PIPE, SLURRY_STORE};
	public static final String[] NAMES = {"pig_barn_controller", "slatted_floor", "farrowing_crate", "heat_lamp", "slurry_pump", "slurry_pipe", "slurry_store"};

	private PigBarnBlocks() {
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
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("pig_barn_controller"), PIG_BARN_CONTROLLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("heat_lamp"), HEAT_LAMP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("slurry_pump"), SLURRY_PUMP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("slurry_store"), SLURRY_STORE_ENTITY);
	}
}
