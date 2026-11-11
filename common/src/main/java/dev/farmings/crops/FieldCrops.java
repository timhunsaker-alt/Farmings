package dev.farmings.crops;

import dev.farmings.Farmings;
import dev.farmings.farm.crops.FieldCrop;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** The Phase 3 crops' blocks, seeds and products (docs/crops.md). */
public final class FieldCrops {
	private FieldCrops() {
	}

	private static BlockBehaviour.Properties cropProps(String name) {
		return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Farmings.id(name))).mapColor(MapColor.PLANT).noCollision()
				.instabreak().sound(SoundType.CROP).pushReaction(PushReaction.POPPED).noOcclusion();
	}

	public static final Map<FieldCrop, Block> BLOCKS = new LinkedHashMap<>();
	public static final Map<FieldCrop, Item> SEEDS = new LinkedHashMap<>();

	public static final Block ALFALFA = crop(FieldCrop.ALFALFA);
	public static final Block TIMOTHY = crop(FieldCrop.TIMOTHY);
	public static final Block ORCHARDGRASS = crop(FieldCrop.ORCHARDGRASS);
	public static final Block CORN = crop(FieldCrop.CORN);
	public static final Block OATS = crop(FieldCrop.OATS);
	public static final Block BARLEY = crop(FieldCrop.BARLEY);

	private static Block crop(FieldCrop c) {
		String name = c.id;
		Block b = switch (c.kind) {
			case PERENNIAL -> new PerennialCropBlock(c, () -> SEEDS.get(c), cropProps(name));
			case CORN -> new CornCropBlock(() -> SEEDS.get(c), cropProps(name));
			default -> new FieldCropBlock(c, () -> SEEDS.get(c), cropProps(name));
		};
		BLOCKS.put(c, b);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Farmings.id(c.id + "_seeds"));
		SEEDS.put(c, new BlockItem(b, new Item.Properties().setId(key).useItemDescriptionPrefix()));
		return b;
	}

	// products (docs/crops.md, "Products"); masses are in the items' names' docs
	public static final Map<String, Item> PRODUCTS = new LinkedHashMap<>();
	/** Loose hay, 10 kg a handful (proposed). */
	public static final Item ALFALFA_HAY = product("alfalfa_hay", Item::new);
	public static final Item GRASS_HAY = product("grass_hay", Item::new);
	/** Loose straw, 10 kg (proposed). */
	public static final Item STRAW = product("straw", Item::new);
	/** Round bales, 454 kg (5 x 5 ft, UF/IFAS AN326). */
	public static final Item ALFALFA_BALE = product("alfalfa_bale", p -> new Item(p.stacksTo(16)));
	public static final Item GRASS_BALE = product("grass_bale", p -> new Item(p.stacksTo(16)));
	public static final Item STRAW_BALE = product("straw_bale", p -> new Item(p.stacksTo(16)));
	/** Grain, 1 kg an item (proposed). */
	public static final Item CORN_GRAIN = product("corn_grain", Item::new);
	public static final Item OAT_GRAIN = product("oat_grain", Item::new);
	public static final Item BARLEY_GRAIN = product("barley_grain", Item::new);
	/** Chopped forage and silage, 25 kg as-fed (proposed). */
	public static final Item CHOPPED_CORN = product("chopped_corn", Item::new);
	public static final Item CHOPPED_ALFALFA = product("chopped_alfalfa", Item::new);
	public static final Item CORN_SILAGE = product("corn_silage", Item::new);
	public static final Item HAYLAGE = product("haylage", Item::new);

	private static Item product(String name, Function<Item.Properties, Item> f) {
		Item i = f.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Farmings.id(name))));
		PRODUCTS.put(name, i);
		return i;
	}

	public static Block block(FieldCrop c) {
		return BLOCKS.get(c);
	}

	public static Item seed(FieldCrop c) {
		return SEEDS.get(c);
	}

	public static void register() {
		for (FieldCrop c : FieldCrop.values()) {
			Registry.register(BuiltInRegistries.BLOCK, Farmings.id(c.id), BLOCKS.get(c));
			Registry.register(BuiltInRegistries.ITEM, Farmings.id(c.id + "_seeds"), SEEDS.get(c));
		}
		PRODUCTS.forEach((n, i) -> Registry.register(BuiltInRegistries.ITEM, Farmings.id(n), i));
	}
}
