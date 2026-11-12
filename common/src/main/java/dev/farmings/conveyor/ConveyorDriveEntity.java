package dev.farmings.conveyor;

import dev.farmings.farm.conveyor.Travel;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The head drive of a belt or auger run (docs/conveyors.md). It finds its run (the cells behind it with the same facing), holds the run's
 * load as parcels ({@link Travel}), moves them at the run's speed while powered, picks up items that land on a belt, and delivers what
 * reaches the head into the next run, a Farmings machine ({@link ItemSink}) or any container. A blocked head stops the run.
 */
public class ConveyorDriveEntity extends BlockEntity implements CircuitPart, Inspectable {
	/** Parcels a run holds at most: one a metre, plus the head (proposed). */
	private final Travel<ItemStack> travel = new Travel<>();
	private final List<BlockPos> cells = new ArrayList<>();
	private Motor motor = new Motor(true, Voltage.INDUSTRIAL, 7500);
	private double ratedWatts = 7500;
	private boolean forced;
	private boolean blocked;
	private int delivered;

	public ConveyorDriveEntity(BlockPos pos, BlockState state) {
		super(ConveyorBlocks.DRIVE_ENTITY, pos, state);
	}

	public ConveyorKind kind() {
		return getBlockState().getBlock() instanceof ConveyorDriveBlock d ? d.kind : ConveyorKind.BELT;
	}

	private Direction facing() {
		return getBlockState().getValue(ConveyorDriveBlock.FACING);
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	public int length() {
		return cells.size();
	}

	public int delivered() {
		return delivered;
	}

	public boolean blocked() {
		return blocked;
	}

	public List<Travel.Parcel<ItemStack>> parcels() {
		return travel.parcels();
	}

	public boolean hasRoom() {
		return travel.size() < Math.max(2, cells.size() + 1);
	}

	private static boolean cellOf(BlockState s, ConveyorKind kind, Direction d) {
		return s.getBlock() instanceof ConveyorPieceBlock q && q.kind == kind && s.getValue(ConveyorPieceBlock.FACING) == d;
	}

	private static ConveyorPieceBlock.Slope slope(BlockState s) {
		return s.getBlock() instanceof ConveyorPieceBlock ? s.getValue(ConveyorPieceBlock.SLOPE) : ConveyorPieceBlock.Slope.FLAT;
	}

	/** The drive of the run a cell belongs to: walk along the cell's facing (up and down its slopes) to the drive. */
	public static @Nullable ConveyorDriveEntity driveOf(Level level, BlockPos cell) {
		BlockState s = level.getBlockState(cell);
		if (!(s.getBlock() instanceof ConveyorPieceBlock piece)) {
			return null;
		}
		Direction d = s.getValue(ConveyorPieceBlock.FACING);
		BlockPos p = cell;
		BlockState at = s;
		for (int i = 0; i < 256; i++) {
			BlockPos next = slope(at) == ConveyorPieceBlock.Slope.UP ? p.relative(d).above() : p.relative(d);
			BlockState n = level.getBlockState(next);
			if (n.getBlock() instanceof ConveyorDriveBlock drive && drive.kind == piece.kind && n.getValue(ConveyorDriveBlock.FACING) == d) {
				return level.getBlockEntity(next) instanceof ConveyorDriveEntity e ? e : null;
			}
			if (!cellOf(n, piece.kind, d) || slope(n) == ConveyorPieceBlock.Slope.DOWN) {
				BlockState below = level.getBlockState(next.below());
				if (slope(at) != ConveyorPieceBlock.Slope.UP && cellOf(below, piece.kind, d) && slope(below) == ConveyorPieceBlock.Slope.DOWN) {
					next = next.below();
					n = below;
				} else {
					return null;
				}
			}
			p = next;
			at = n;
		}
		return null;
	}

	/** The cell before {@code p} on a run going {@code d}, or null: level, one up into a falling cell, or one down from a rising one. */
	private @Nullable BlockPos previous(BlockPos p, BlockState atP, Direction d) {
		if (slope(atP) == ConveyorPieceBlock.Slope.DOWN) {
			BlockPos up = p.relative(d.getOpposite()).above();
			return cellOf(level.getBlockState(up), kind(), d) ? up : null;
		}
		BlockPos level0 = p.relative(d.getOpposite());
		BlockState s = level.getBlockState(level0);
		if (cellOf(s, kind(), d) && slope(s) != ConveyorPieceBlock.Slope.UP) {
			return level0;
		}
		BlockPos low = level0.below();
		BlockState l = level.getBlockState(low);
		return cellOf(l, kind(), d) && slope(l) == ConveyorPieceBlock.Slope.UP ? low : null;
	}

	/** Metres from the head end to the far edge of each cell, nearest first. */
	private final List<Double> along = new ArrayList<>();
	/** Two slopes closer than 3 cells: a run steeper than 1 in 3 doesn't move (docs/conveyors.md). */
	private boolean tooSteep;

	public boolean tooSteep() {
		return tooSteep;
	}

	public List<BlockPos> cells() {
		return List.copyOf(cells);
	}

	private double toGoAt(int i) {
		return i < 0 || along.isEmpty() ? 0 : along.get(Math.min(i, along.size() - 1));
	}

	/** Finds the run: the cells behind the drive with its kind and facing, nearest first, following its slopes. */
	public void refresh() {
		cells.clear();
		along.clear();
		tooSteep = false;
		if (level == null) {
			return;
		}
		Direction d = facing();
		BlockPos p = worldPosition;
		BlockState at = level.getBlockState(p);
		double metres = 0;
		int lastSlope = -100;
		for (int i = 0; i < 256; i++) {
			BlockPos prev = previous(p, at, d);
			if (prev == null) {
				break;
			}
			p = prev;
			at = level.getBlockState(p);
			if (slope(at) != ConveyorPieceBlock.Slope.FLAT) {
				if (cells.size() - lastSlope < 3) {
					tooSteep = true;
				}
				lastSlope = cells.size();
			}
			cells.add(p.immutable());
			metres += ConveyorPieceBlock.length(at);
			along.add(metres);
		}
		double w = kind().watts(Math.max(1, cells.size()));
		if (w != ratedWatts) {
			ratedWatts = w;
			motor = new Motor(true, Voltage.INDUSTRIAL, w);
		}
	}

	/** Puts a stack on the run at a cell (or the tail if the cell is not on it). */
	public void putOn(ItemStack stack, BlockPos at) {
		if (stack.isEmpty()) {
			return;
		}
		int i = cells.indexOf(at);
		double toGo = toGoAt(i < 0 ? cells.size() - 1 : i);
		travel.add(stack, toGo);
		setChanged();
	}

	/** Puts a stack on the tail end, as a machine feeding the run does. False if the run is full or does not carry it. */
	public boolean offer(ItemStack stack) {
		if (!kind().carries(stack) || !hasRoom()) {
			return false;
		}
		travel.add(stack.copy(), toGoAt(cells.size() - 1));
		setChanged();
		return true;
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (now % 40 == 0 || cells.isEmpty()) {
			refresh();
		}
		if (now % 10 == 0 && (!travel.isEmpty() || sentLoad)) {
			// the client draws the load (docs/conveyors.md, "Items drawn"): send it twice a second while there is any
			sentLoad = !travel.isEmpty();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
		if (cells.isEmpty() || tooSteep || !powered()) {
			return;
		}
		if (kind() == ConveyorKind.BELT && now % 5 == 0) {
			pickUp(level);
		}
		travel.advance(kind().metresPerSecond / 20.0);
		ItemStack ready = travel.takeReady();
		if (ready != null) {
			ItemStack left = deliver(level, ready);
			blocked = !left.isEmpty();
			if (blocked) {
				travel.putBackReady(left);
			} else {
				delivered += ready.getCount();
			}
			setChanged();
		}
	}

	/** Items lying on a belt's cells ride on. */
	private void pickUp(ServerLevel level) {
		for (int i = 0; i < cells.size() && hasRoom(); i++) {
			BlockPos c = cells.get(i);
			for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(c).deflate(0.05, 0, 0.05).setMaxY(c.getY() + 0.6))) {
				if (!hasRoom()) {
					break;
				}
				travel.add(e.getItem().copy(), toGoAt(i));
				e.discard();
			}
		}
	}

	/** Hands a stack to whatever the head faces; returns what it could not take. */
	private ItemStack deliver(ServerLevel level, ItemStack stack) {
		BlockPos out = worldPosition.relative(facing());
		BlockEntity be = dev.wildcatter.multiblock.BigParts.entity(level, out);
		if (level.getBlockState(out).getBlock() instanceof ConveyorPieceBlock) {
			ConveyorDriveEntity next = driveOf(level, out);
			if (next != null && next.kind().carries(stack) && next.hasRoom()) {
				next.putOn(stack, out);
				return ItemStack.EMPTY;
			}
			return stack;
		}
		if (be instanceof ItemSink sink) {
			return sink.accept(stack);
		}
		if (be instanceof Container c) {
			return HopperBlockEntity.addItem(null, c, stack, facing().getOpposite());
		}
		return stack;
	}

	public String readout() {
		String what = kind() == ConveyorKind.BELT ? "Belt drive" : "Auger drive";
		return String.format(Locale.ROOT, "%s (%.1f kW): %s, run %d cells, %.1f m at %.1f m/s, %d parcel%s on it, %d delivered%s", what, ratedWatts / 1000,
				tooSteep ? "TOO STEEP (at most one slope in 3 cells)" : powered() ? "running" : "NO POWER", cells.size(), toGoAt(cells.size() - 1),
				kind().metresPerSecond, travel.size(), travel.size() == 1 ? "" : "s", delivered, blocked ? ", HEAD BLOCKED" : "");
	}

	// ---- Wildcatter ----

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
		motor.apply(circuit, ticks, !cells.isEmpty());
	}

	@Override
	public Component inspect() {
		return Component.literal(readout() + "; " + motor.describe());
	}

	// ---- saving: the load on the run ----

	private boolean sentLoad;
	/** Client side: the game tick the load was last sent, to move it on smoothly between updates. */
	private long syncTick;
	private boolean syncPowered;

	public long syncTick() {
		return syncTick;
	}

	public boolean syncPowered() {
		return syncPowered;
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		var list = out.childrenList("Load");
		for (Travel.Parcel<ItemStack> p : travel.parcels()) {
			var o = list.addChild();
			o.store("Item", ItemStack.CODEC, p.load());
			o.putDouble("ToGo", p.toGo());
		}
		out.putInt("Delivered", delivered);
		out.putBoolean("Powered", powered() && !tooSteep && !cells.isEmpty());
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		travel.clear();
		for (ValueInput o : in.childrenListOrEmpty("Load")) {
			o.read("Item", ItemStack.CODEC).ifPresent(st -> travel.add(st, o.getDoubleOr("ToGo", 0)));
		}
		delivered = in.getIntOr("Delivered", 0);
		syncPowered = in.getBooleanOr("Powered", false);
		syncTick = level != null ? level.getGameTime() : 0;
	}
}
