package dev.farmings.compost;

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

/** The compost windrow (docs/multiblocks/compost.md) and its products, and the digester's solids. */
public final class CompostBlocks {
	public static final Block COMPOST_PAD = new Block(props("compost_pad").mapColor(MapColor.STONE).strength(2.0F).sound(SoundType.STONE));
	public static final Block COMPOST_CONTROLLER = new CompostControllerBlock(false, props("compost_controller").mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD));
	public static final Block WINDROW_TURNER = new CompostControllerBlock(true, props("windrow_turner").mapColor(MapColor.COLOR_ORANGE).strength(3.0F).sound(SoundType.METAL));
	public static final BlockEntityType<CompostControllerEntity> CONTROLLER_ENTITY = new BlockEntityType<>(CompostControllerEntity::new, Set.of(COMPOST_CONTROLLER));
	public static final BlockEntityType<TurnerEntity> TURNER_ENTITY = new BlockEntityType<>(TurnerEntity::new, Set.of(WINDROW_TURNER));
	public static final Block[] ALL = {COMPOST_PAD, COMPOST_CONTROLLER, WINDROW_TURNER};
	/** Compost, 10 kg an item: one fertilizer use, bone meal's effect (waste.md). */
	public static final Item COMPOST = new BoneMealItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Farmings.id("compost"))));
	/** Separated digestate solids, 10 kg an item (digester.md): for compost. */
	public static final Item SEPARATED_SOLIDS = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Farmings.id("separated_solids"))));

	private CompostBlocks() {
	}

	private static BlockBehaviour.Properties props(String name) {
		return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Farmings.id(name)));
	}

	public static void register() {
		String[] names = {"compost_pad", "compost_controller", "windrow_turner"};
		for (int i = 0; i < ALL.length; i++) {
			Registry.register(BuiltInRegistries.BLOCK, Farmings.id(names[i]), ALL[i]);
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Farmings.id(names[i]));
			Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(ALL[i], new Item.Properties().setId(key).useBlockDescriptionPrefix()));
		}
		Registry.register(BuiltInRegistries.ITEM, Farmings.id("compost"), COMPOST);
		Registry.register(BuiltInRegistries.ITEM, Farmings.id("separated_solids"), SEPARATED_SOLIDS);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("compost_controller"), CONTROLLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("windrow_turner"), TURNER_ENTITY);
		java.util.function.Predicate<net.minecraft.world.item.ItemStack> before = dev.farmings.gantry.Fertilizers.more;
		dev.farmings.gantry.Fertilizers.more = s -> s.is(COMPOST) || before.test(s);
	}
}
