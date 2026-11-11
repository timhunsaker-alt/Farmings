package dev.farmings.crops;

import dev.farmings.farm.crops.FieldCrop;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where the new seeds come from (docs/crops.md, "Seeds"): grass a player breaks without shears drops one, at vanilla's wheat-seed odds
 * (12.5%, the short-grass loot table), chosen by biome (proposed). Vanilla's own wheat-seed drop is untouched. Both loaders call this from
 * their block-break events.
 */
public final class GrassSeeds {
	private GrassSeeds() {
	}

	public static final double CHANCE = 0.125;

	private static final Map<String, List<FieldCrop>> BY_BIOME = Map.of(
			"plains", List.of(FieldCrop.CORN, FieldCrop.ALFALFA, FieldCrop.OATS),
			"sunflower_plains", List.of(FieldCrop.CORN, FieldCrop.ALFALFA, FieldCrop.OATS),
			"savanna", List.of(FieldCrop.CORN, FieldCrop.ALFALFA),
			"meadow", List.of(FieldCrop.TIMOTHY, FieldCrop.ORCHARDGRASS),
			"forest", List.of(FieldCrop.TIMOTHY, FieldCrop.ORCHARDGRASS),
			"birch_forest", List.of(FieldCrop.TIMOTHY, FieldCrop.ORCHARDGRASS),
			"taiga", List.of(FieldCrop.OATS, FieldCrop.BARLEY));

	/** The seed crops of a biome path such as "plains"; empty for the rest. */
	public static List<FieldCrop> crops(String biomePath) {
		return BY_BIOME.getOrDefault(biomePath, List.of());
	}

	public static void onBreak(ServerLevel level, Player player, BlockPos pos, BlockState state) {
		Block b = state.getBlock();
		if (b != Blocks.SHORT_GRASS && b != Blocks.TALL_GRASS || player.getMainHandItem().is(Items.SHEARS) || player.isCreative()) {
			return;
		}
		String biome = level.getBiome(pos).unwrapKey().map(k -> k.identifier().getPath()).orElse("");
		List<FieldCrop> l = crops(biome);
		if (l.isEmpty() || level.getRandom().nextDouble() >= CHANCE) {
			return;
		}
		FieldCrop c = l.get(level.getRandom().nextInt(l.size()));
		Block.popResource(level, pos, new ItemStack(FieldCrops.seed(c)));
	}
}
