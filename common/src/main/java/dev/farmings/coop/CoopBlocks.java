package dev.farmings.coop;

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

/** The coop's own blocks (docs/multiblocks/coop.md); its water trough is the barn's. */
public final class CoopBlocks {
	public static final Block COOP_CONTROLLER = block("coop_controller", p -> new CoopControllerBlock(p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD)));
	public static final Block COOP_FLOOR = block("coop_floor", p -> new ShedFloorBlock(p.mapColor(MapColor.COLOR_YELLOW).strength(0.6F).sound(SoundType.GRASS).noOcclusion()));
	public static final Block POP_HOLE = block("pop_hole", p -> new ShedFloorBlock(p.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion()));
	public static final Block NEST_BOX = block("nest_box", p -> new NestBoxBlock(p.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion()));
	public static final Block PERCH = block("perch", p -> new PerchBlock(p.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion()));
	public static final Block GRAIN_FEEDER = block("grain_feeder", p -> new GrainFeederBlock(p.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()));
	public static final BlockEntityType<CoopControllerEntity> COOP_CONTROLLER_ENTITY = new BlockEntityType<>(CoopControllerEntity::new, Set.of(COOP_CONTROLLER));
	public static final BlockEntityType<NestBoxEntity> NEST_BOX_ENTITY = new BlockEntityType<>(NestBoxEntity::new, Set.of(NEST_BOX));
	public static final BlockEntityType<GrainFeederEntity> GRAIN_FEEDER_ENTITY = new BlockEntityType<>(GrainFeederEntity::new, Set.of(GRAIN_FEEDER));
	public static final Block[] ALL = {COOP_CONTROLLER, COOP_FLOOR, POP_HOLE, NEST_BOX, PERCH, GRAIN_FEEDER};
	public static final String[] NAMES = {"coop_controller", "coop_floor", "pop_hole", "nest_box", "perch", "grain_feeder"};

	private CoopBlocks() {
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
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("coop_controller"), COOP_CONTROLLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("nest_box"), NEST_BOX_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("grain_feeder"), GRAIN_FEEDER_ENTITY);
		// hens in a coop lay into its nest boxes (FowlEntity's egg sink)
		dev.farmings.entity.FowlEntity.eggSink = CoopControllerEntity::takeEgg;
	}
}
