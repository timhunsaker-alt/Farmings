package dev.farmings.crops;

import dev.farmings.farm.crops.FieldCrop;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Phase 3 crop (docs/crops.md). It grows on the 15:1 clock, not on random ticks: when it is planted it schedules its next stage, and
 * each stage schedules the next, so growth carries on across chunk reloads and is the same for every block. Bone meal (and Farmings
 * fertilizer) moves it on as vanilla bone meal does.
 */
public class FieldCropBlock extends CropBlock {
	protected final FieldCrop crop;
	private final Supplier<ItemLike> seed;

	public FieldCropBlock(FieldCrop crop, Supplier<ItemLike> seed, Properties properties) {
		super(properties);
		this.crop = crop;
		this.seed = seed;
	}

	public FieldCrop crop() {
		return crop;
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return seed.get();
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return false;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
	}

	/** Ticks to the next stage from this state. */
	protected long nextStageTicks(BlockState state) {
		return crop.stageTicks();
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		super.onPlace(state, level, pos, old, moved);
		if (!level.isClientSide() && !isMaxAge(state) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
			level.scheduleTick(pos, this, (int) Math.min(Integer.MAX_VALUE, nextStageTicks(state)));
		}
	}

	/** One stage on the clock. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!canSurvive(state, level, pos)) {
			level.destroyBlock(pos, true);
			return;
		}
		if (!isMaxAge(state)) {
			BlockState next = state.setValue(AGE, getAge(state) + 1);
			level.setBlock(pos, next, 2);
			if (!isMaxAge(next)) {
				level.scheduleTick(pos, this, (int) Math.min(Integer.MAX_VALUE, nextStageTicks(next)));
			} else {
				ripened(level, pos, next);
			}
		}
	}

	/** Called when it reaches its last stage. */
	protected void ripened(ServerLevel level, BlockPos pos, BlockState state) {
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		super.performBonemeal(level, random, pos, state, source);
		BlockState now = level.getBlockState(pos);
		if (now.getBlock() == this && isMaxAge(now)) {
			ripened(level, pos, now);
		}
	}

	/** For game tests: run the next stage now. */
	public void stageForTest(ServerLevel level, BlockPos pos) {
		tick(level.getBlockState(pos), level, pos, level.getRandom());
	}
}
