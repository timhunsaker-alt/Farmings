package dev.farmings.barn;

import dev.farmings.FarmingsBlocks;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The main cell of a barn part: it only remembers that the part has grown into its cells (Wildcatter's big-part rule). */
public class BarnPartEntity extends BlockEntity implements BigPartEntity {
	private boolean grown;

	public BarnPartEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.PART_ENTITY, pos, state);
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
