package dev.farmings.gantry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The field cable junction: wire it to Wildcatter 480 V; set it beside the home end of a field rail. */
public class FieldJunctionBlock extends BaseEntityBlock {
	public FieldJunctionBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FieldJunctionEntity(pos, state);
	}
}
