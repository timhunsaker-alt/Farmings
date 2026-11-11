package dev.farmings.gantry;

import dev.farmings.farm.gantry.GantryTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A gantry part with no state of its own (field-gantry.md): a field rail, an end truck, a bridge beam cell (which shows the tool hanging
 * under it) or the headland stop. AXIS is the beam's direction across the field.
 */
public class GantryPartBlock extends Block {
	public enum Part { RAIL, TRUCK, BEAM, STOP }

	public enum Tool implements net.minecraft.util.StringRepresentable {
		NONE, SEED_DRILL, HARVESTER, MOWER, RAKE, BALER, SPREADER;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}

		public static Tool of(GantryTool t) {
			return t == null ? NONE : valueOf(t.name());
		}
	}

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	public static final EnumProperty<Tool> TOOL = EnumProperty.create("tool", Tool.class);
	private static final VoxelShape RAIL = Block.box(0, 0, 0, 16, 3, 16);
	public final Part part;

	public GantryPartBlock(Part part, Properties properties) {
		super(properties);
		this.part = part;
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(TOOL, Tool.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, TOOL);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getAxis());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return part == Part.RAIL ? RAIL : super.getShape(state, level, pos, ctx);
	}
}
