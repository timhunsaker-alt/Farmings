package dev.farmings.shed;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A shed floor cell (sheep pen, lambing pen, pasture gate): 2/16 high, walked on (docs/multiblocks/sheep-shed.md). */
public class ShedFloorBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

	public ShedFloorBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return true;
	}
}
