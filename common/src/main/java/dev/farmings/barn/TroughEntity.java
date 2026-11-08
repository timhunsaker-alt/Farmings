package dev.farmings.barn;

import dev.farmings.FarmingsBlocks;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A water trough's water, in litres. At one cell to the metre a water bucket (one block of water) is 1,000 litres, and the trough's
 * capacity is one bucket *(proposed)*. A Wildcatter pipe feed comes later (decided with Tim: water from both a pipe and a bucket).
 */
public class TroughEntity extends BlockEntity implements BigPartEntity {
	public static final double BUCKET_LITRES = 1000;
	public static final double CAPACITY_LITRES = BUCKET_LITRES;

	private double litres;
	private boolean grown;

	public TroughEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.TROUGH_ENTITY, pos, state);
	}

	public double litres() {
		return litres;
	}

	public double room() {
		return CAPACITY_LITRES - litres;
	}

	/** Pours a bucket in, up to the capacity; what does not fit is spilled. Returns false when the trough is already full. */
	public boolean pourBucket() {
		if (room() <= 1e-9) {
			return false;
		}
		litres = Math.min(CAPACITY_LITRES, litres + BUCKET_LITRES);
		setChanged();
		return true;
	}

	/** Takes up to {@code l} litres; returns how much was there. */
	public double take(double l) {
		double taken = Math.max(0, Math.min(l, litres));
		if (taken > 0) {
			litres -= taken;
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
		output.putDouble("litres", litres);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		litres = Math.max(0, Math.min(CAPACITY_LITRES, input.getDoubleOr("litres", 0)));
	}
}
