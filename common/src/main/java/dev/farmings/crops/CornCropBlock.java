package dev.farmings.crops;

import dev.farmings.farm.crops.FieldCrop;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Corn (docs/crops.md): two blocks tall when grown (drawn tall from one block). At stage 7 (100 days) it is ready to chop for silage; 14
 * days later it is ripe for grain (114-day relative maturity) and turns dry.
 */
public class CornCropBlock extends FieldCropBlock {
	public static final BooleanProperty RIPE = BooleanProperty.create("ripe");

	public CornCropBlock(Supplier<ItemLike> seed, Properties properties) {
		super(FieldCrop.CORN, seed, properties);
		registerDefaultState(defaultBlockState().setValue(RIPE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(RIPE);
	}

	@Override
	protected void ripened(ServerLevel level, BlockPos pos, BlockState state) {
		if (!state.getValue(RIPE)) {
			level.scheduleTick(pos, this, (int) FieldCrop.cornRipeningTicks());
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (isMaxAge(state) && !state.getValue(RIPE) && canSurvive(state, level, pos)) {
			level.setBlock(pos, state.setValue(RIPE, true), 2);
			return;
		}
		super.tick(state, level, pos, random);
	}

	/** Ready for the forage chopper: grown (stage 7). */
	public static boolean silageReady(BlockState state) {
		return state.getBlock() instanceof CornCropBlock c && c.isMaxAge(state);
	}

	/** Ready for the combine: ripe for grain. */
	public static boolean grainReady(BlockState state) {
		return state.getBlock() instanceof CornCropBlock && state.getValue(RIPE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Block.box(2, 0, 2, 14, Math.min(16, 4 + 2 * getAge(state)), 14);
	}
}
