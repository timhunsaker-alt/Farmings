package dev.farmings.feedmill;

import dev.farmings.Farmings;
import java.util.Set;
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

/** The feed mill's machines (docs/multiblocks/feed-mill.md). */
public final class FeedMillBlocks {
	public static final Block HAMMER_MILL = new FeedMachineBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Farmings.id("hammer_mill")))
			.mapColor(MapColor.COLOR_GREEN).strength(3.0F).sound(SoundType.METAL));
	public static final Block TMR_MIXER = new FeedMixerBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Farmings.id("tmr_mixer")))
			.mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.METAL));
	public static final BlockEntityType<FeedMachineEntity> MACHINE_ENTITY = new BlockEntityType<>(FeedMachineEntity::new, Set.of(HAMMER_MILL, TMR_MIXER));
	public static final Block[] ALL = {HAMMER_MILL, TMR_MIXER};

	private FeedMillBlocks() {
	}

	public static void register() {
		for (String n : new String[] {"hammer_mill", "tmr_mixer"}) {
			Block b = n.equals("hammer_mill") ? HAMMER_MILL : TMR_MIXER;
			Registry.register(BuiltInRegistries.BLOCK, Farmings.id(n), b);
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Farmings.id(n));
			Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(b, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
		}
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("feed_machine"), MACHINE_ENTITY);
		FeedItems.register();
	}
}
