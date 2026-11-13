package dev.farmings.racing;

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
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The race track's blocks (docs/multiblocks/race-track.md): the surface, a rail, the start gate and the finish post. */
public final class RaceBlocks {
	public static final Block RACE_TRACK = block("race_track", p -> new Block(p.mapColor(MapColor.DIRT).strength(0.6F).sound(SoundType.GRAVEL)));
	public static final Block TRACK_RAIL = block("track_rail", p -> new FenceBlock(p.mapColor(MapColor.SNOW).strength(2.0F, 3.0F).sound(SoundType.WOOD)));
	public static final Block START_GATE = block("start_gate", p -> new StartGateBlock(p.mapColor(MapColor.COLOR_GREEN).strength(2.0F, 3.0F).sound(SoundType.METAL)));
	public static final Block FINISH_POST = block("finish_post", p -> new FinishPostBlock(p.mapColor(MapColor.COLOR_RED).strength(2.0F, 3.0F).sound(SoundType.WOOD)
			.noOcclusion()));
	public static final BlockEntityType<StartGateEntity> START_GATE_ENTITY = new BlockEntityType<>(StartGateEntity::new, Set.of(START_GATE));
	public static final Block[] ALL = {RACE_TRACK, TRACK_RAIL, START_GATE, FINISH_POST};
	public static final String[] NAMES = {"race_track", "track_rail", "start_gate", "finish_post"};

	private RaceBlocks() {
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
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("start_gate"), START_GATE_ENTITY);
	}
}
