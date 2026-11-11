package dev.farmings.digester;

import dev.farmings.Farmings;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The methane digester and separator (docs/multiblocks/digester.md). */
public final class DigesterBlocks {
	public static final Block DIGESTER_TANK = new DigesterBlock(DigesterBlock.Kind.TANK, props("digester_tank").mapColor(MapColor.COLOR_GREEN).strength(3.0F).sound(SoundType.METAL));
	public static final Block FEED_PIT = new DigesterBlock(DigesterBlock.Kind.FEED_PIT, props("digester_feed_pit").mapColor(MapColor.STONE).strength(3.0F).sound(SoundType.STONE));
	public static final Block GAS_HOLDER = new DigesterBlock(DigesterBlock.Kind.HOLDER, props("gas_holder").mapColor(MapColor.COLOR_LIGHT_GRAY).strength(2.0F).sound(SoundType.WOOL));
	public static final Block DIGESTATE_TANK = new Block(props("digestate_tank").mapColor(MapColor.COLOR_GREEN).strength(3.0F).sound(SoundType.METAL));
	public static final Block SCREW_PRESS = new DigesterBlock(DigesterBlock.Kind.PRESS, props("screw_press").mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL));
	public static final BlockEntityType<DigesterEntity> TANK_ENTITY = new BlockEntityType<>(DigesterEntity::new, Set.of(DIGESTER_TANK));
	public static final BlockEntityType<FeedPitEntity> FEED_PIT_ENTITY = new BlockEntityType<>(FeedPitEntity::new, Set.of(FEED_PIT));
	public static final BlockEntityType<GasHolderEntity> HOLDER_ENTITY = new BlockEntityType<>(GasHolderEntity::new, Set.of(GAS_HOLDER));
	public static final BlockEntityType<PressEntity> PRESS_ENTITY = new BlockEntityType<>(PressEntity::new, Set.of(SCREW_PRESS));
	public static final Block[] ALL = {DIGESTER_TANK, FEED_PIT, GAS_HOLDER, DIGESTATE_TANK, SCREW_PRESS};
	public static final String[] NAMES = {"digester_tank", "digester_feed_pit", "gas_holder", "digestate_tank", "screw_press"};
	/** Liquid digestate, 100 L an item: one fertilizer use (digester.md). */
	public static final Item LIQUID_DIGESTATE = new BoneMealItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Farmings.id("liquid_digestate"))));

	private DigesterBlocks() {
	}

	private static BlockBehaviour.Properties props(String name) {
		return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Farmings.id(name)));
	}

	public static void register() {
		for (int i = 0; i < ALL.length; i++) {
			Registry.register(BuiltInRegistries.BLOCK, Farmings.id(NAMES[i]), ALL[i]);
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Farmings.id(NAMES[i]));
			Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(ALL[i], new Item.Properties().setId(key).useBlockDescriptionPrefix()));
		}
		Registry.register(BuiltInRegistries.ITEM, Farmings.id("liquid_digestate"), LIQUID_DIGESTATE);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("digester_tank"), TANK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("digester_feed_pit"), FEED_PIT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("gas_holder"), HOLDER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("screw_press"), PRESS_ENTITY);
		java.util.function.Predicate<net.minecraft.world.item.ItemStack> before = dev.farmings.gantry.Fertilizers.more;
		dev.farmings.gantry.Fertilizers.more = s -> s.is(LIQUID_DIGESTATE) || before.test(s);
	}
}
