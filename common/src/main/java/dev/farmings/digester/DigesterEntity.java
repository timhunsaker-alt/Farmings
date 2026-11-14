package dev.farmings.digester;

import dev.farmings.farm.digester.Digester;
import dev.farmings.husbandry.TimeScale;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The digester tank (docs/multiblocks/digester.md): a heated, stirred complete-mix tank whose 6.5 kW mixer ("a power rating of 6.5kW", a trade example) runs on Wildcatter
 * 480 V. Fed from its feed pit, it fills the gas holder on top and the digestate tank beside it, on the 15:1 clock.
 */
public class DigesterEntity extends BlockEntity implements CircuitPart, Inspectable {
	public static final double WATTS = 6500;
	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, WATTS);
	private final Digester digester = new Digester();
	private boolean forced;

	public DigesterEntity(BlockPos pos, BlockState state) {
		super(DigesterBlocks.TANK_ENTITY, pos, state);
	}

	public Digester digester() {
		return digester;
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	/** A gas holder on the tank's roof: above any of its cells. */
	public boolean hasHolder() {
		return touching(Direction.UP, DigesterBlocks.GAS_HOLDER);
	}

	/** A digestate tank against any of its sides. */
	public boolean hasDigestateTank() {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (touching(d, DigesterBlocks.DIGESTATE_TANK)) {
				return true;
			}
		}
		return false;
	}

	private boolean touching(Direction d, net.minecraft.world.level.block.Block b) {
		if (level == null) {
			return false;
		}
		java.util.List<BlockPos> cells = dev.wildcatter.multiblock.BigParts.cellsAt(level, worldPosition);
		for (BlockPos c : cells) {
			BlockPos n = c.relative(d);
			if (!cells.contains(n) && level.getBlockState(n).getBlock() == b) {
				return true;
			}
		}
		return false;
	}

	public void serverTick(ServerLevel level) {
		if (level.getGameTime() % 20 == 0) {
			step(TimeScale.ticksToRealDays(20));
		}
	}

	public void step(double days) {
		digester.step(days, powered() && hasHolder() && hasDigestateTank());
		setChanged();
	}

	public String readout() {
		StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "Digester: %.0f kg of manure in, gas holder %.1f of %.0f m3, digestate %.0f kg, mixer %s",
				digester.inTankKg(), digester.gasM3(), Digester.HOLDER_M3, digester.digestateKg(), powered() ? "on (35-40 C)" : "NO POWER"));
		if (!hasHolder()) {
			b.append(", NO GAS HOLDER on top");
		}
		if (!hasDigestateTank()) {
			b.append(", NO DIGESTATE TANK beside it");
		}
		return b.toString();
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
		motor.apply(circuit, ticks, digester.inTankKg() > 0);
	}

	@Override
	public Component inspect() {
		return Component.literal(readout() + "; " + motor.describe());
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		var l = out.list("Digester", com.mojang.serialization.Codec.DOUBLE);
		for (double v : digester.save()) {
			l.add(v);
		}
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		java.util.List<Double> v = new java.util.ArrayList<>();
		in.listOrEmpty("Digester", com.mojang.serialization.Codec.DOUBLE).forEach(v::add);
		digester.load(v.stream().mapToDouble(Double::doubleValue).toArray());
	}
}
