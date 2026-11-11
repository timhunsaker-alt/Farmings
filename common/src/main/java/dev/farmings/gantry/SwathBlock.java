package dev.farmings.gantry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Straw the combine leaves on the stubble (field-gantry.md): a thin layer, raked into a windrow, then baled. */
public class SwathBlock extends Block {
	public enum Kind implements net.minecraft.util.StringRepresentable {
		OAT_STRAW, BARLEY_STRAW;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
	public static final BooleanProperty RAKED = BooleanProperty.create("raked");
	private static final VoxelShape FLAT = Block.box(0, 0, 0, 16, 2, 16);
	private static final VoxelShape ROW = Block.box(4, 0, 0, 12, 5, 16);

	public SwathBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(KIND, Kind.OAT_STRAW).setValue(RAKED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(KIND, RAKED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(RAKED) ? ROW : FLAT;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return net.minecraft.world.phys.shapes.Shapes.empty();
	}
}
