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
	/** A belt cell's slope (docs/conveyors.md, "Slopes"): flat, rising one block toward FACING, or falling one block into it. */
	public enum Slope implements net.minecraft.util.StringRepresentable {
		FLAT, UP, DOWN;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final EnumProperty<Slope> SLOPE = EnumProperty.create("slope", Slope.class);
	private static final VoxelShape SLOPED = Block.box(0, 0, 0, 16, 8, 16);
	private static final VoxelShape BELT = Block.box(0, 0, 0, 16, 4, 16);
	private static final VoxelShape AUGER = Block.box(4, 4, 4, 12, 12, 12);
	public final ConveyorKind kind;

	public ConveyorPieceBlock(ConveyorKind kind, Properties properties) {
		super(properties);
		this.kind = kind;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SLOPE, Slope.FLAT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SLOPE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction d = kind == ConveyorKind.BELT ? ctx.getHorizontalDirection() : ctx.getNearestLookingDirection();
		return defaultBlockState().setValue(FACING, d);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return kind == ConveyorKind.BELT ? (state.getValue(SLOPE) == Slope.FLAT ? BELT : SLOPED) : AUGER;
	}

	/**
	 * A belt placed one block above the cell behind it makes that cell rise to it; placed one block below, it falls into it (docs/conveyors.md,
	 * "Slopes"). Augers run straight and are never sloped.
	 */
	@Override
	public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity by,
			net.minecraft.world.item.ItemStack stack) {
		super.setPlacedBy(level, pos, state, by, stack);
		if (kind != ConveyorKind.BELT || level.isClientSide()) {
			return;
		}
		Direction d = state.getValue(FACING);
		BlockPos lowPrev = pos.relative(d.getOpposite()).below();
		BlockState lp = level.getBlockState(lowPrev);
		if (lp.getBlock() == this && lp.getValue(FACING) == d && lp.getValue(SLOPE) == Slope.FLAT) {
			level.setBlock(lowPrev, lp.setValue(SLOPE, Slope.UP), Block.UPDATE_ALL);
		}
		BlockState hp = level.getBlockState(pos.relative(d.getOpposite()).above());
		if (hp.getBlock() == this && hp.getValue(FACING) == d) {
			level.setBlock(pos, state.setValue(SLOPE, Slope.DOWN), Block.UPDATE_ALL);
		}
	}

	/** Metres of belt in a cell: 1 flat, √2 on a slope (one block up over one along; derived). */
	public static double length(BlockState s) {
		return s.getBlock() instanceof ConveyorPieceBlock && s.getValue(SLOPE) != Slope.FLAT ? Math.sqrt(2) : 1.0;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConveyorCellEntity(pos, state);
	}
}
