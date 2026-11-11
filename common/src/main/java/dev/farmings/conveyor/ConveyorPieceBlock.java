package dev.farmings.conveyor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * One cell of a belt or auger run (docs/conveyors.md). FACING is the way the load moves. A belt cell is a low walkable slab and takes items
 * that land on it; an auger cell is a tube in any direction. Each cell takes a hopper's items and hands them to its run's drive.
 */
public class ConveyorPieceBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	private static final VoxelShape BELT = Block.box(0, 0, 0, 16, 4, 16);
	private static final VoxelShape AUGER = Block.box(4, 4, 4, 12, 12, 12);
	public final ConveyorKind kind;

	public ConveyorPieceBlock(ConveyorKind kind, Properties properties) {
		super(properties);
		this.kind = kind;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction d = kind == ConveyorKind.BELT ? ctx.getHorizontalDirection() : ctx.getNearestLookingDirection();
		return defaultBlockState().setValue(FACING, d);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return kind == ConveyorKind.BELT ? BELT : AUGER;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConveyorCellEntity(pos, state);
	}
}
