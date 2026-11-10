package dev.farmings.coldroom;

import dev.farmings.Farmings;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The walk-in fridge's blocks (docs/multiblocks/cold-room.md). */
public final class ColdRoomBlocks {
	public static final Block PANEL = block("cold_room_panel", p -> new Block(p.mapColor(MapColor.SNOW).strength(2.0F, 6.0F).sound(SoundType.METAL)));
	public static final Block DOOR = block("cold_room_door", p -> new ColdRoomDoorBlock(p.mapColor(MapColor.SNOW).strength(2.0F, 6.0F).noOcclusion()));
	public static final Block EVAPORATOR = block("evaporator", p -> new EvaporatorBlock(p.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL)));
	public static final Block CONDENSING_UNIT = block("condensing_unit", p -> new CondensingUnitBlock(p.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL)));
	public static final BlockEntityType<ColdRoomEntity> COLD_ROOM_ENTITY = new BlockEntityType<>(ColdRoomEntity::new, Set.of(EVAPORATOR));
	public static final BlockEntityType<CondenserEntity> CONDENSER_ENTITY = new BlockEntityType<>(CondenserEntity::new, Set.of(CONDENSING_UNIT));
	public static final Block[] ALL = {PANEL, DOOR, EVAPORATOR, CONDENSING_UNIT};
	public static final String[] NAMES = {"cold_room_panel", "cold_room_door", "evaporator", "condensing_unit"};

	private ColdRoomBlocks() {
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Farmings.id(name));
		return factory.apply(BlockBehaviour.Properties.of().setId(key));
	}

	public static void register() {
		for (int i = 0; i < ALL.length; i++) {
			Registry.register(BuiltInRegistries.BLOCK, Farmings.id(NAMES[i]), ALL[i]);
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Farmings.id(NAMES[i]));
			Item.Properties props = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
			Registry.register(BuiltInRegistries.ITEM, itemKey, ALL[i] == DOOR ? new DoubleHighBlockItem(ALL[i], props) : new BlockItem(ALL[i], props));
		}
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("cold_room"), COLD_ROOM_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("condensing_unit"), CONDENSER_ENTITY);
	}
}
