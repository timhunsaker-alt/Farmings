package dev.farmings.gantry;

import dev.farmings.Farmings;
import dev.farmings.farm.gantry.GantryTool;
import java.util.EnumMap;
import java.util.Map;
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
import org.jspecify.annotations.Nullable;

/** The field gantry's blocks and tool heads (docs/multiblocks/field-gantry.md). */
public final class GantryBlocks {
	public static final Block FIELD_RAIL = block("field_rail", p -> new GantryPartBlock(GantryPartBlock.Part.RAIL, p.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion()));
	public static final Block GANTRY_TRUCK = block("gantry_truck", p -> new GantryPartBlock(GantryPartBlock.Part.TRUCK, p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)));
	public static final Block GANTRY_BEAM = block("gantry_beam", p -> new GantryPartBlock(GantryPartBlock.Part.BEAM, p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).noOcclusion()));
	public static final Block HEADLAND_STOP = block("headland_stop", p -> new GantryPartBlock(GantryPartBlock.Part.STOP, p.mapColor(MapColor.COLOR_YELLOW).strength(2.0F).sound(SoundType.METAL)));
	public static final Block GANTRY_CAB = block("gantry_cab", p -> new GantryCabBlock(p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).noOcclusion()));
	public static final Block FIELD_JUNCTION = block("field_junction", p -> new FieldJunctionBlock(p.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL)));
	public static final Block SWATH = block("swath", p -> new SwathBlock(p.mapColor(MapColor.COLOR_YELLOW).instabreak().sound(SoundType.GRASS).noOcclusion()));
	public static final BlockEntityType<GantryCabEntity> CAB_ENTITY = new BlockEntityType<>(GantryCabEntity::new, Set.of(GANTRY_CAB));
	public static final BlockEntityType<FieldJunctionEntity> JUNCTION_ENTITY = new BlockEntityType<>(FieldJunctionEntity::new, Set.of(FIELD_JUNCTION));
	public static final Block[] ALL = {FIELD_RAIL, GANTRY_TRUCK, GANTRY_BEAM, HEADLAND_STOP, GANTRY_CAB, FIELD_JUNCTION};
	public static final String[] NAMES = {"field_rail", "gantry_truck", "gantry_beam", "headland_stop", "gantry_cab", "field_junction"};
	public static final Map<GantryTool, Item> TOOLS = new EnumMap<>(GantryTool.class);

	static {
		for (GantryTool t : GantryTool.values()) {
			String name = "tool_" + t.name().toLowerCase(java.util.Locale.ROOT);
			TOOLS.put(t, new Item(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, Farmings.id(name)))));
		}
	}

	private GantryBlocks() {
	}

	public static Item toolItem(GantryTool t) {
		return TOOLS.get(t);
	}

	public static @Nullable GantryTool toolOf(ItemStack s) {
		for (Map.Entry<GantryTool, Item> e : TOOLS.entrySet()) {
			if (s.is(e.getValue())) {
				return e.getKey();
			}
		}
		return null;
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
		Registry.register(BuiltInRegistries.BLOCK, Farmings.id("swath"), SWATH);
		TOOLS.forEach((t, item) -> Registry.register(BuiltInRegistries.ITEM, Farmings.id("tool_" + t.name().toLowerCase(java.util.Locale.ROOT)), item));
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("gantry_cab"), CAB_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("field_junction"), JUNCTION_ENTITY);
	}
}
