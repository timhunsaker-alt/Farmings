package dev.farmings.conveyor;

import dev.farmings.Farmings;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Belts and augers (docs/conveyors.md). */
public final class ConveyorBlocks {
	public static final Block FARM_BELT = block("farm_belt", p -> new ConveyorPieceBlock(ConveyorKind.BELT, p.mapColor(MapColor.COLOR_BLACK).strength(1.5F).sound(SoundType.METAL).noOcclusion()));
	public static final Block FARM_BELT_DRIVE = block("farm_belt_drive", p -> new ConveyorDriveBlock(ConveyorKind.BELT, p.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL)));
	public static final Block GRAIN_AUGER = block("grain_auger", p -> new ConveyorPieceBlock(ConveyorKind.AUGER, p.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()));
	public static final Block GRAIN_AUGER_DRIVE = block("grain_auger_drive", p -> new ConveyorDriveBlock(ConveyorKind.AUGER, p.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL)));
	public static final BlockEntityType<ConveyorCellEntity> CELL_ENTITY = new BlockEntityType<>(ConveyorCellEntity::new, Set.of(FARM_BELT, GRAIN_AUGER));
	public static final BlockEntityType<ConveyorDriveEntity> DRIVE_ENTITY = new BlockEntityType<>(ConveyorDriveEntity::new, Set.of(FARM_BELT_DRIVE, GRAIN_AUGER_DRIVE));
	public static final Block[] ALL = {FARM_BELT, FARM_BELT_DRIVE, GRAIN_AUGER, GRAIN_AUGER_DRIVE};
	public static final String[] NAMES = {"farm_belt", "farm_belt_drive", "grain_auger", "grain_auger_drive"};

	private ConveyorBlocks() {
	}

	/** Meal from the mill rides augers too (feed-mill.md); set once the mill's items exist. */
	public static java.util.function.Predicate<ItemStack> meal = s -> false;

	static boolean isMeal(ItemStack s) {
		return meal.test(s);
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
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("conveyor_cell"), CELL_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("conveyor_drive"), DRIVE_ENTITY);
	}
}
