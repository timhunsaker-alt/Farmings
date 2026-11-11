package dev.farmings.gantry;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The fixed cable junction at the rails' home end (field-gantry.md): a Wildcatter 480 V load standing for the gantry's motors at the end
 * of its trailing cable, as the TBM's cable junction does. The gantry sets the demand; the junction says whether power is there.
 */
public class FieldJunctionEntity extends BlockEntity implements CircuitPart, Inspectable {
	private Motor motor = new Motor(true, Voltage.INDUSTRIAL, 1500);
	private double watts = 1500;
	private boolean demand;
	private boolean forced;

	public FieldJunctionEntity(BlockPos pos, BlockState state) {
		super(GantryBlocks.JUNCTION_ENTITY, pos, state);
	}

	public void demand(double w, boolean on) {
		if (Math.abs(w - watts) > 1) {
			watts = w;
			motor = new Motor(true, Voltage.INDUSTRIAL, Math.max(1, w));
		}
		demand = on;
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL;
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
		return Component.literal(String.format(Locale.ROOT, "Field cable junction (%.1f kW for the gantry): %s", watts / 1000, motor.describe()));
	}
}
