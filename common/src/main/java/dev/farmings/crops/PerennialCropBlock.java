package dev.farmings.crops;

import dev.farmings.farm.crops.FieldCrop;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Alfalfa, timothy, orchardgrass (docs/crops.md): cut, they drop back to stage 3 and regrow; after their stand's cuts they die. */
public class PerennialCropBlock extends FieldCropBlock {
	public static final IntegerProperty CUTS = IntegerProperty.create("cuts", 0, 31);
	/** The cut hay lying on the stubble: 0 none, 1 a mown swath (drying), 2 raked into a windrow (field-gantry.md). */
	public static final IntegerProperty SWATH = IntegerProperty.create("swath", 0, 2);

	public PerennialCropBlock(FieldCrop crop, Supplier<ItemLike> seed, Properties properties) {
		super(crop, seed, properties);
		registerDefaultState(defaultBlockState().setValue(CUTS, 0).setValue(SWATH, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CUTS, SWATH);
	}

	@Override
	protected long nextStageTicks(BlockState state) {
		return state.getValue(CUTS) > 0 ? crop.regrowthStageTicks() : crop.stageTicks();
	}

	/**
	 * Cuts a ripe plant: it drops to its regrowth stage with the cut lying on it as a swath, or dies at the end of its stand (its last swath
	 * is lost with it, proposed). Returns the dry matter cut, kg, or 0 if it was not ripe.
	 */
	public double cut(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() != this || !isMaxAge(state)) {
			return 0;
		}
		int cuts = state.getValue(CUTS) + 1;
		if (cuts >= crop.standCuts) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		} else {
			BlockState next = state.setValue(AGE, FieldCrop.REGROWTH_STAGE).setValue(CUTS, Math.min(31, cuts)).setValue(SWATH, 1);
			level.setBlock(pos, next, 3);
			level.scheduleTick(pos, this, (int) crop.regrowthStageTicks());
		}
		return crop.yieldKg;
	}

	/** A hoe rakes the swath, then gathers the dry windrow (field-gantry.md, "Hand raking"). */
	@Override
	protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, net.minecraft.world.level.Level level,
			BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		net.minecraft.world.InteractionResult r = dev.farmings.gantry.HandRake.use(stack, state, level, pos, player, hand);
		return r == net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND ? super.useItemOn(stack, state, level, pos, player, hand, hit) : r;
	}
}
