package dev.farmings.barn;

import dev.farmings.FarmingsBlocks;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The collection pit's store of manure items. 64 an item cell and 6 cells in its top layer, so 384 (docs/multiblocks/barn.md). */
public class PitEntity extends BlockEntity implements BigPartEntity {
	public static final int CAPACITY = 64 * 6;

	private int manure;
	private boolean grown;

	public PitEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.PIT_ENTITY, pos, state);
	}

	public int manure() {
		return manure;
	}

	public int room() {
		return CAPACITY - manure;
	}

	/** Adds up to {@code count} manure; returns how many fit. */
	public int add(int count) {
		int taken = Math.max(0, Math.min(count, room()));
		if (taken > 0) {
			manure += taken;
			setChanged();
		}
		return taken;
	}

	/** Takes up to {@code count} manure; returns how many were there. */
	public int take(int count) {
		int taken = Math.max(0, Math.min(count, manure));
		if (taken > 0) {
			manure -= taken;
			setChanged();
		}
		return taken;
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
		output.putInt("manure", manure);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		manure = Math.max(0, Math.min(CAPACITY, input.getIntOr("manure", 0)));
	}
}
