package dev.farmings.creamery;

import dev.farmings.FarmingsBlocks;
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

/** A powered creamery part (pasteurizer heater, separator, churn, vat agitator): one three-phase load on Wildcatter's industrial voltage. */
public class CreameryMachineEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	private final CreameryParts kind;
	private final Motor motor;
	private boolean demand;
	private boolean forced;
	private boolean grown;

	public CreameryMachineEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.CREAMERY_MACHINE_ENTITY, pos, state);
		this.kind = state.getBlock() instanceof CreameryPartBlock b ? b.kind() : CreameryParts.CREAM_SEPARATOR;
		this.motor = new Motor(true, Voltage.INDUSTRIAL, Math.max(1, kind.watts));
	}

	public CreameryParts kind() {
		return kind;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	public void setDemand(boolean demand) {
		this.demand = demand;
	}

	/** For tests: run as if fed, without building a grid. */
	public void forceRunning(boolean on) {
		forced = on;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL && role != Conductor.N && role != Conductor.G;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, demand);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("%s (%.2f kW): %s", kind.id.replace('_', ' '), kind.watts / 1000.0, motor.describe()));
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
