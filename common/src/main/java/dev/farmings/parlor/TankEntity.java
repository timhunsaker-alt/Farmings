package dev.farmings.parlor;

import dev.farmings.FarmingsBlocks;
import dev.farmings.husbandry.parlor.BulkTank;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The bulk tank: its milk ({@link BulkTank}) and a 2.2 kW three-phase chiller (approved by Tim 2026-11-08; a suggestion, not a source). */
public class TankEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	public static final int CHILLER_WATTS = 2_600;     // Mueller Model R, 1,000 L (sourcing pass 2026-11-12)

	private final BulkTank tank = new BulkTank();
	private final Motor chiller = new Motor(true, Voltage.INDUSTRIAL, CHILLER_WATTS);
	private boolean forced;
	private boolean grown;

	public TankEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.TANK_ENTITY, pos, state);
	}

	public BulkTank tank() {
		return tank;
	}

	public boolean chilling() {
		return forced || chiller.output() > 0;
	}

	/** For tests: run the chiller as if fed. */
	public void forceChilling(boolean on) {
		forced = on;
	}

	/** The controller's once-a-second step: the chiller works while milk above 4 C is in the tank. */
	public void step(int ticks) {
		tank.cool(ticks, chilling());
		setChanged();
	}

	public boolean wantsCooling() {
		return tank.litres() > 0 && tank.tempC() > BulkTank.COLD_C;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL && role != Conductor.N && role != Conductor.G;
	}

	@Override
	public void build(Circuit circuit) {
		chiller.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		chiller.apply(circuit, ticks, wantsCooling());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Bulk tank: %.0f / %.0f L at %.1f C, %.2f%% fat; chiller (2.2 kW): %s", tank.litres(), BulkTank.CAPACITY_L, tank.tempC(), tank.fatPct(), chiller.describe()));
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
		output.putDouble("Litres", tank.litres());
		output.putDouble("TempC", tank.tempC());
		output.putLong("SinceAdd", tank.sinceAddTicks());
		output.putDouble("FatPct", tank.fatPct());
		output.putDouble("ProteinPct", tank.proteinPct());
		output.putString("Milk", tank.kind().name());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		tank.setKind(dev.farmings.husbandry.parlor.MilkKind.load(input.getStringOr("Milk", ""), input.getBooleanOr("SheepMilk", false)));
		grown = input.getBooleanOr("grown", false);
		tank.load(input.getDoubleOr("Litres", 0), input.getDoubleOr("TempC", BulkTank.COLD_C), input.getLongOr("SinceAdd", Long.MAX_VALUE / 2));
		tank.loadComponents(input.getDoubleOr("FatPct", 3.72), input.getDoubleOr("ProteinPct", 3.06));
	}
}
