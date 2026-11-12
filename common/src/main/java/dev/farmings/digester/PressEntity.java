package dev.farmings.digester;

import dev.farmings.compost.CompostBlocks;
import dev.farmings.farm.digester.Digester;
import dev.farmings.silo.Towers;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The screw-press separator (digester.md): beside a digestate tank, it presses the digester's digestate into separated solids (10%, as 10 kg
 * items) and liquid fertilizer (90%, as 100 L items), 100 kg a second while powered. 5.5 kW (proposed).
 */
public class PressEntity extends BlockEntity implements CircuitPart, Inspectable {
	public static final double WATTS = 5500;
	public static final double KG_PER_STEP = 100;
	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, WATTS);
	private boolean forced;
	private double solidsKg;
	private double liquidKg;
	private int solidsOut;
	private int liquidOut;

	public PressEntity(BlockPos pos, BlockState state) {
		super(DigesterBlocks.PRESS_ENTITY, pos, state);
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	public int solidsOut() {
		return solidsOut;
	}

	public int liquidOut() {
		return liquidOut;
	}

	private @Nullable DigesterEntity digester() {
		if (level == null) {
			return null;
		}
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos t = worldPosition.relative(d);
			if (level.getBlockState(t).getBlock() == DigesterBlocks.DIGESTATE_TANK) {
				for (Direction e : Direction.Plane.HORIZONTAL) {
					if (dev.wildcatter.multiblock.BigParts.entity(level, t.relative(e)) instanceof DigesterEntity dg) {
						return dg;
					}
				}
			}
		}
		return null;
	}

	public void serverTick(ServerLevel level) {
		if (level.getGameTime() % 20 != 0 || !powered()) {
			return;
		}
		DigesterEntity dg = digester();
		if (dg != null) {
			double kg = dg.digester().takeDigestate(KG_PER_STEP);
			solidsKg += kg * Digester.SOLIDS_SHARE;
			liquidKg += kg * (1 - Digester.SOLIDS_SHARE);
			dg.setChanged();
		}
		while (solidsKg >= 10 - 1e-9) {
			if (!Towers.handOn(level, worldPosition, new ItemStack(CompostBlocks.SEPARATED_SOLIDS), p -> false).isEmpty()) {
				break;
			}
			solidsKg -= 10;
			solidsOut++;
		}
		while (liquidKg >= 100 - 1e-9) {
			if (!Towers.handOn(level, worldPosition, new ItemStack(DigesterBlocks.LIQUID_DIGESTATE), p -> false).isEmpty()) {
				break;
			}
			liquidKg -= 100;
			liquidOut++;
		}
		setChanged();
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
		DigesterEntity dg = digester();
		motor.apply(circuit, ticks, dg != null && dg.digester().digestateKg() > 0);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format(Locale.ROOT, "Screw press (5.5 kW): %s, %d solids and %d liquid out; %s", powered() ? "on" : "NO POWER", solidsOut,
				liquidOut, motor.describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putDouble("SolidsKg", solidsKg);
		out.putDouble("LiquidKg", liquidKg);
		out.putInt("SolidsOut", solidsOut);
		out.putInt("LiquidOut", liquidOut);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		solidsKg = in.getDoubleOr("SolidsKg", 0);
		liquidKg = in.getDoubleOr("LiquidKg", 0);
		solidsOut = in.getIntOr("SolidsOut", 0);
		liquidOut = in.getIntOr("LiquidOut", 0);
	}
}
