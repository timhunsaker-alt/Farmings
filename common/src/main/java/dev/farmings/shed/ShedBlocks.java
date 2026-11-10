package dev.farmings.shed;

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

/** The sheep shed's own blocks (docs/multiblocks/sheep-shed.md); its hay racks and troughs are the barn's parts. */
public final class ShedBlocks {
	public static final Block SHED_CONTROLLER = block("shed_controller", p -> new ShedControllerBlock(p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD)));
	public static final Block SHEEP_PEN = block("sheep_pen", p -> new ShedFloorBlock(p.mapColor(MapColor.COLOR_YELLOW).strength(0.6F).sound(SoundType.GRASS).noOcclusion()));
	public static final Block LAMBING_PEN = block("lambing_pen", p -> new ShedFloorBlock(p.mapColor(MapColor.COLOR_YELLOW).strength(0.6F).sound(SoundType.GRASS).noOcclusion()));
	public static final Block PASTURE_GATE = block("pasture_gate", p -> new ShedFloorBlock(p.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion()));
	public static final BlockEntityType<ShedControllerEntity> SHED_CONTROLLER_ENTITY = new BlockEntityType<>(ShedControllerEntity::new, Set.of(SHED_CONTROLLER));
	public static final Block[] ALL = {SHED_CONTROLLER, SHEEP_PEN, LAMBING_PEN, PASTURE_GATE};
	public static final String[] NAMES = {"shed_controller", "sheep_pen", "lambing_pen", "pasture_gate"};

	private ShedBlocks() {
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
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("shed_controller"), SHED_CONTROLLER_ENTITY);
	}
}
