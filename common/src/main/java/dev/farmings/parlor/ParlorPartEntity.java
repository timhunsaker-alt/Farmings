package dev.farmings.parlor;

import dev.farmings.FarmingsBlocks;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The main cell of a plain parlor part: it only remembers that the part has grown into its cells. */
public class ParlorPartEntity extends BlockEntity implements BigPartEntity {
	private boolean grown;

	public ParlorPartEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.PARLOR_PART_ENTITY, pos, state);
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
	}
}
