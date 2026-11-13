package dev.farmings.piggery;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The slurry pump (pig-barn.md): beside the pig barn's controller, on Wildcatter power (1.5 kW, proposed), it moves slurry from the pit
 * along slurry pipes to a digester feed pit or a slurry store, 20 kg a second *(proposed)*.
 */
public class SlurryPumpEntity extends BlockEntity implements CircuitPart, Inspectable {
	public static final double WATTS = 1500;
	public static final double KG_PER_SECOND = 20;
	/** Longest pipe run *(proposed, as the milk line)*. */
	public static final int MAX_PIPE = 64;
	private final Motor motor = new Motor(false, Voltage.LOW, WATTS);
	private boolean forced;
	private boolean demand;
	private double movedKg;
	private String status = "idle";

	public SlurryPumpEntity(BlockPos pos, BlockState state) {
		super(PigBarnBlocks.SLURRY_PUMP_ENTITY, pos, state);
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	public double movedKg() {
		return movedKg;
	}

	private @Nullable PigBarnControllerEntity barn(ServerLevel level) {
		for (Direction d : Direction.values()) {
			if (level.getBlockEntity(worldPosition.relative(d)) instanceof PigBarnControllerEntity b) {
				return b;
			}
		}
		return null;
	}

	/** The end of the pipe run: a digester feed pit or a slurry store reached along slurry pipes (or touching the pump). */
	private @Nullable BlockEntity sink(ServerLevel level) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(worldPosition);
		seen.add(worldPosition);
		while (!queue.isEmpty() && seen.size() <= MAX_PIPE) {
			BlockPos p = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (!seen.add(n)) {
					continue;
				}
				BlockEntity e = level.getBlockEntity(n);
				if (e instanceof SlurryStoreEntity || e instanceof dev.farmings.digester.FeedPitEntity) {
					return e;
				}
				if (level.getBlockState(n).is(PigBarnBlocks.SLURRY_PIPE)) {
					queue.add(n);
				}
			}
		}
		return null;
	}

	public void serverTick(ServerLevel level) {
		if (level.getGameTime() % 20 != 0) {
			return;
		}
		PigBarnControllerEntity b = barn(level);
		demand = b != null && b.pitKg() > 1;
		if (b == null) {
			status = "NO PIG BARN BESIDE IT";
			return;
		}
		BlockEntity s = sink(level);
		if (s == null) {
			status = "NO PIPE TO A FEED PIT OR STORE";
			return;
		}
		if (!powered()) {
			status = demand ? "NO POWER" : "idle";
			return;
		}
		double want = Math.min(KG_PER_SECOND, b.pitKg());
		double took = 0;
		if (s instanceof SlurryStoreEntity store) {
			took = store.add(want);
		} else if (s instanceof dev.farmings.digester.FeedPitEntity pit) {
			took = pit.feedSlurry(want);
		}
		b.takeSlurry(took);
		movedKg += took;
		status = took > 0 ? "pumping" : want > 0 ? "SINK FULL OR NO TANK" : "idle";
		setChanged();
	}

	public String readout() {
		return String.format(Locale.ROOT, "Slurry pump (%.1f kW): %s, %.0f kg moved", WATTS / 1000, status, movedKg);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.LOW;
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
		return Component.literal(readout() + "; " + motor.describe());
	}
}
