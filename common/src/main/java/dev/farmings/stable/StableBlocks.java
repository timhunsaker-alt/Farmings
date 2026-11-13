package dev.farmings.stable;

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
 * The stable's own blocks (docs/multiblocks/stable.md): the controller and the stall floor. Its hay racks and troughs are the barn's hay
 * feeder and water trough; its paddock gate is the sheep shed's pasture gate.
 */
public final class StableBlocks {
	public static final Block STABLE_CONTROLLER = block("stable_controller", p -> new StableControllerBlock(p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD)));
	public static final Block HORSE_STALL = block("horse_stall", p -> new ShedFloorBlock(p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
	public static final BlockEntityType<StableControllerEntity> STABLE_CONTROLLER_ENTITY = new BlockEntityType<>(StableControllerEntity::new, Set.of(STABLE_CONTROLLER));
	public static final Block[] ALL = {STABLE_CONTROLLER, HORSE_STALL};
	public static final String[] NAMES = {"stable_controller", "horse_stall"};

	private StableBlocks() {
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
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("stable_controller"), STABLE_CONTROLLER_ENTITY);
	}
}
