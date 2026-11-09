package dev.farmings.creamery;

import dev.farmings.FarmingsBlocks;
import dev.farmings.parlor.ParlorPartBlock;
import dev.farmings.parlor.ParlorParts;
import dev.farmings.parlor.TankEntity;
import dev.farmings.process.creamery.Creamery;
import dev.farmings.process.creamery.CreameryCheck;
import dev.farmings.process.creamery.CreameryPart;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The creamery's controller (docs/multiblocks/creamery.md): checks the row once a second and, while it is formed, pulls milk from a piped
 * parlor bulk tank and runs the {@link Creamery} with the power its machines have.
 */
public class CreameryControllerEntity extends BlockEntity {
	public static final int CHECK_TICKS = 20;
	/** Milk moved from a piped bulk tank each second *(proposed)*. */
	public static final double PIPE_L_PER_SECOND = 50;
	public static final int PIPE_REACH = 64;

	private final Creamery creamery = new Creamery();
	private CreameryCheck check;
	private final Set<BlockPos> cells = new HashSet<>();

	public CreameryControllerEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.CREAMERY_CONTROLLER_ENTITY, pos, state);
	}

	public Creamery creamery() {
		return creamery;
	}

	public CreameryCheck check() {
		return check;
	}

	public boolean formed() {
		return check != null && check.formed();
	}

	public void refresh(Level level) {
		check = CreameryWorld.check(level, worldPosition);
		cells.clear();
		if (check.formed()) {
			for (var e : check.offsets().entrySet()) {
				CreameryParts kind = CreameryParts.of(e.getKey());
				int len = e.getKey().length();
				for (int dx = 0; dx < len; dx++) {
					for (int dy = 0; dy < 2; dy++) {
						for (int dz = 0; dz < 2; dz++) {
							BlockPos p = at(e.getValue() + dx, worldPosition.getY() + dy, dz);
							if (level.getBlockState(p).getBlock() instanceof CreameryPartBlock b && b.kind() == kind) {
								cells.add(p);
							}
						}
					}
				}
			}
		}
	}

	/** Whether this cell is one of this creamery's parts. */
	public boolean owns(BlockPos cell) {
		return cells.contains(cell);
	}

	private BlockPos at(int ax, int y, int az) {
		int[] w = check.orientation().world(ax, az);
		return new BlockPos(worldPosition.getX() + w[0], y, worldPosition.getZ() + w[1]);
	}

	public CreameryMachineEntity machine(Level level, CreameryPart part) {
		Integer x = check.offsets().get(part);
		return x != null && BigParts.entity(level, at(x, worldPosition.getY(), 0)) instanceof CreameryMachineEntity m ? m : null;
	}

	private boolean powered(Level level, CreameryPart part, boolean demand) {
		CreameryMachineEntity m = machine(level, part);
		if (m == null) {
			return false;
		}
		m.setDemand(demand);
		return m.powered();
	}

	public void serverTick(Level level) {
		long t = level.getGameTime() + worldPosition.hashCode();
		if (check == null || Math.floorMod(t, (long) CHECK_TICKS) == 0) {
			refresh(level);
		}
		if (formed() && Math.floorMod(t, (long) CHECK_TICKS) == 1) {
			run(level, CHECK_TICKS);
		}
	}

	/** One second (or {@code ticks}) of work. Tests call it directly. */
	public void run(Level level, int ticks) {
		if (!formed()) {
			return;
		}
		pullFromParlor(level, PIPE_L_PER_SECOND * ticks / 20.0);
		boolean busy = creamery.phase() != Creamery.Phase.IDLE || creamery.rawL() >= 1;
		Creamery.Mode mode = creamery.mode();
		Creamery.Power power = new Creamery.Power(
				powered(level, CreameryPart.BATCH_PASTEURIZER, busy),
				powered(level, CreameryPart.CREAM_SEPARATOR, busy && (mode == Creamery.Mode.CREAM || mode == Creamery.Mode.BUTTER)),
				powered(level, CreameryPart.BUTTER_CHURN, busy && mode == Creamery.Mode.BUTTER),
				powered(level, CreameryPart.CHEESE_VAT, busy && mode == Creamery.Mode.CHEESE));
		creamery.step(ticks, power);
		setChanged();
	}

	/** Follows milk_line blocks from the receiving tank to a parlor bulk tank and moves milk across, with its fat and protein. */
	public double pullFromParlor(Level level, double maxL) {
		if (creamery.room() <= 0) {
			return 0;
		}
		Integer tx = check.offsets().get(CreameryPart.RECEIVING_TANK);
		if (tx == null) {
			return 0;
		}
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		for (BlockPos c : cells) {
			if (level.getBlockState(c).getBlock() instanceof CreameryPartBlock b && b.kind() == CreameryParts.RECEIVING_TANK) {
				queue.add(c);
				seen.add(c);
			}
		}
		boolean first = true;
		int steps = 0;
		while (!queue.isEmpty() && steps++ < PIPE_REACH * 4) {
			BlockPos p = queue.poll();
			boolean fromTank = level.getBlockState(p).getBlock() instanceof CreameryPartBlock;
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (!seen.add(n)) {
					continue;
				}
				BlockState s = level.getBlockState(n);
				if (s.getBlock() instanceof ParlorPartBlock pb) {
					if (pb.kind() == ParlorParts.MILK_LINE) {
						queue.add(n);
					} else if (pb.kind() == ParlorParts.BULK_TANK && !fromTank && BigParts.entity(level, n) instanceof TankEntity tank) {
						double l = tank.tank().take(Math.min(maxL, creamery.room()));
						creamery.addMilk(l, tank.tank().fatPct(), tank.tank().proteinPct());
						tank.setChanged();
						return l;
					}
				}
			}
		}
		return 0;
	}

	public String readout() {
		if (check == null) {
			return "Creamery: not checked yet";
		}
		if (!check.formed()) {
			return "Creamery: " + check.status();
		}
		String f = creamery.fault();
		return "Creamery: " + creamery.summary() + (f.isEmpty() ? "" : ", " + f);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		double[] d = creamery.save();
		output.putInt("CreameryN", d.length);
		for (int i = 0; i < d.length; i++) {
			output.putDouble("Creamery" + i, d[i]);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		int n = input.getIntOr("CreameryN", 0);
		double[] d = new double[n];
		for (int i = 0; i < n; i++) {
			d[i] = input.getDoubleOr("Creamery" + i, 0);
		}
		creamery.load(d);
	}
}
