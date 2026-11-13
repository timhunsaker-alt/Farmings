package dev.farmings.piggery;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A slurry store, 100 m³ *(proposed)*, standing in for a lagoon (pig-barn.md). Slurry is about 1 kg a litre *(proposed)*. */
public class SlurryStoreEntity extends BlockEntity {
	public static final double CAPACITY_KG = 100_000;
	private double kg;

	public SlurryStoreEntity(BlockPos pos, BlockState state) {
		super(PigBarnBlocks.SLURRY_STORE_ENTITY, pos, state);
	}

	public double kg() {
		return kg;
	}

	/** Takes in what fits; returns kg taken. */
	public double add(double in) {
		double t = Math.max(0, Math.min(in, CAPACITY_KG - kg));
		kg += t;
		setChanged();
		return t;
	}

	public void serverTick(ServerLevel level) {
	}

	public String readout() {
		return String.format(Locale.ROOT, "Slurry store: %.1f of %.0f m³", kg / 1000, CAPACITY_KG / 1000);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putDouble("Kg", kg);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		kg = in.getDoubleOr("Kg", 0);
	}
}
