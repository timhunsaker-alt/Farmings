package dev.farmings.corral;

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

/** The taming corral's blocks (docs/taming.md); its water trough is the barn's. */
public final class CorralBlocks {
	public static final Block CORRAL_POST = block("corral_post", p -> new CorralPostBlock(p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion()));
	public static final Block CORRAL_GATE = block("corral_gate", p -> new CorralGateBlock(p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion()));
	public static final Block BAIT_TROUGH = block("bait_trough", p -> new BaitTroughBlock(p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
	public static final BlockEntityType<CorralPostEntity> CORRAL_POST_ENTITY = new BlockEntityType<>(CorralPostEntity::new, Set.of(CORRAL_POST));
	public static final BlockEntityType<BaitTroughEntity> BAIT_TROUGH_ENTITY = new BlockEntityType<>(BaitTroughEntity::new, Set.of(BAIT_TROUGH));
	public static final Block[] ALL = {CORRAL_POST, CORRAL_GATE, BAIT_TROUGH};
	public static final String[] NAMES = {"corral_post", "corral_gate", "bait_trough"};

	private CorralBlocks() {
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
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("corral_post"), CORRAL_POST_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("bait_trough"), BAIT_TROUGH_ENTITY);
	}
}
