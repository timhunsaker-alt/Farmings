package dev.farmings.barn;

import dev.farmings.FarmingsBlocks;
import dev.farmings.feed.Hay;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A hay feeder's contents, as kilograms of dry matter: up to 4 hay blocks of 85 kg each (docs/multiblocks/barn.md). */
public class FeederEntity extends BlockEntity implements BigPartEntity {
	public static final int MAX_BLOCKS = 4;
	public static final double CAPACITY_KG = MAX_BLOCKS * Hay.BLOCK_FRESH_KG * Hay.DRY_MATTER_FRACTION;

	private double dryMatterKg;
	private boolean grown;

	public FeederEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.FEEDER_ENTITY, pos, state);
	}

	public double dryMatterKg() {
		return dryMatterKg;
	}

	/** Hay blocks' worth, rounded down, for the readout. */
	public double blocks() {
		return dryMatterKg / Hay.blockDryMatterKg();
	}

	/** Puts one hay block in if there is room for it; returns whether it went in. */
	public boolean addBlock() {
		if (dryMatterKg + Hay.blockDryMatterKg() > CAPACITY_KG + 1e-9) {
			return false;
		}
		dryMatterKg += Hay.blockDryMatterKg();
		setChanged();
		return true;
	}

	/** Takes up to {@code kg} of dry matter; returns how much was there. */
	public double take(double kg) {
		double taken = Math.max(0, Math.min(kg, dryMatterKg));
		if (taken > 0) {
			dryMatterKg -= taken;
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
		output.putDouble("dry_matter_kg", dryMatterKg);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		dryMatterKg = Math.max(0, Math.min(CAPACITY_KG, input.getDoubleOr("dry_matter_kg", 0)));
	}
}
