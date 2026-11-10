package dev.farmings.corral;

import dev.farmings.entity.Wild;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A one-way gate in the corral fence (docs/taming.md). FACING points into the corral: an animal passes from outside to inside, and from
 * inside the gate is a closed fence. A player opens it both ways with a right-click and closes it with another.
 */
public class CorralGateBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	private static final VoxelShape CLOSED = Block.box(0, 0, 0, 16, 24, 16);
	private static final VoxelShape OUTLINE_X = Block.box(6, 0, 0, 10, 16, 16);
	private static final VoxelShape OUTLINE_Z = Block.box(0, 0, 6, 16, 16, 10);

	public CorralGateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN);
	}

	/** Placed from outside, looking in: the gate faces the way the player looks. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(FACING).getAxis() == Direction.Axis.X ? OUTLINE_X : OUTLINE_Z;
	}

	/**
	 * Animals: open while any part of the animal is still on the outside of the gate's inner face, closed once it is wholly inside.
	 * Players and other mobs: closed unless opened. With no entity (path finding, the corral check): open, so an outside animal's path runs
	 * through it.
	 */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		if (state.getValue(OPEN)) {
			return Shapes.empty();
		}
		Entity e = ctx instanceof EntityCollisionContext ec ? ec.getEntity() : null;
		if (e == null) {
			return Shapes.empty();
		}
		if (e instanceof Wild.Animal) {
			return whollyInside(e.getBoundingBox(), pos, state.getValue(FACING)) ? CLOSED : Shapes.empty();
		}
		return CLOSED;
	}

	/** Whether the box is wholly past the gate's inner face. */
	public static boolean whollyInside(AABB box, BlockPos pos, Direction facing) {
		return switch (facing) {
			case SOUTH -> box.minZ >= pos.getZ() + 1 - 1e-7;
			case NORTH -> box.maxZ <= pos.getZ() + 1e-7;
			case EAST -> box.minX >= pos.getX() + 1 - 1e-7;
			case WEST -> box.maxX <= pos.getX() + 1e-7;
			default -> false;
		};
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return type == PathComputationType.LAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			level.setBlock(pos, state.cycle(OPEN), Block.UPDATE_ALL);
		}
		return InteractionResult.SUCCESS;
	}
}
