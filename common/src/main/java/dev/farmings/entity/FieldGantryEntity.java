package dev.farmings.entity;

import dev.farmings.FarmingsEntities;
import dev.farmings.gantry.GantryCabEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The field gantry while it moves (docs/multiblocks/field-gantry.md, "The lift"), Tailings' TBM pattern: the gantry's trucks, beam and cab
 * lift out of the world into this one entity, which glides along the rails at the tool's pace. At each cell it sets its blocks down,
 * the cab works one step of its program (the row it is over, then the move to the next cell, carrying its state as before), and if it
 * moves on it lifts again; all in one tick, so the world never shows the gantry in two places. When it stops (parked, unloading, out of
 * power, or at the end of the rails) its blocks stay down and the cab ticks as a block again. Saved either way.
 */
public class FieldGantryEntity extends Entity {
	private static final EntityDataAccessor<String> DATA_CELLS = SynchedEntityData.defineId(FieldGantryEntity.class, EntityDataSerializers.STRING);

	/** The blocks carried: offset from the cab's cell and the block state. */
	public record Cell(BlockPos offset, BlockState state) {
	}

	private final List<Cell> cells = new ArrayList<>();
	private @Nullable CompoundTag cabData;
	private BlockPos cab = BlockPos.ZERO;
	private Direction dir = Direction.NORTH;
	private int ticksPerStep = 9;
	private int elapsed;
	/** Client: the cells decoded from the synced string, and the string they came from. */
	private String decoded = "";
	private final List<Cell> shown = new ArrayList<>();

	public FieldGantryEntity(EntityType<?> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_CELLS, "");
	}

	/** Lifts the gantry whose cab is {@code c} into a new entity gliding toward its next cell. */
	public static @Nullable FieldGantryEntity lift(ServerLevel level, GantryCabEntity c, GantryCabEntity.Frame f) {
		Direction d = c.nextMove(f);
		if (d == null) {
			return null;
		}
		FieldGantryEntity e = new FieldGantryEntity(FarmingsEntities.FIELD_GANTRY, level);
		e.take(level, c, f, d);
		level.addFreshEntity(e);
		return e;
	}

	/** Takes the gantry's blocks and the cab's state out of the world, with nothing dropped. */
	private void take(ServerLevel level, GantryCabEntity c, GantryCabEntity.Frame f, Direction d) {
		cab = c.getBlockPos();
		dir = d;
		ticksPerStep = Math.max(1, c.ticksPerStep());
		elapsed = 0;
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		c.saveCustomOnlyFor(out);
		cabData = out.buildResult();
		cells.clear();
		for (BlockPos p : c.parts(f)) {
			cells.add(new Cell(p.subtract(cab), level.getBlockState(p)));
		}
		int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		for (Cell cell : cells) {
			BlockPos at = cab.offset(cell.offset());
			level.removeBlockEntity(at);
			level.setBlock(at, Blocks.AIR.defaultBlockState(), quiet);
		}
		entityData.set(DATA_CELLS, encode(cells));
		setPos(cab.getX(), cab.getY(), cab.getZ());
	}

	/** Puts the blocks back at the cab's cell and its state into the cab. Returns the cab, or null if it could not. */
	private @Nullable GantryCabEntity setDown(ServerLevel level) {
		int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		for (Cell cell : cells) {
			level.setBlock(cab.offset(cell.offset()), cell.state(), quiet);
		}
		if (level.getBlockEntity(cab) instanceof GantryCabEntity c) {
			if (cabData != null) {
				c.loadState(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), cabData));
			}
			c.catchUp(elapsed, level.isRaining());
			c.setChanged();
			return c;
		}
		return null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (cells.isEmpty()) {
			discard();
			return;
		}
		elapsed++;
		double frac = Math.min(1.0, elapsed / (double) ticksPerStep);
		setPos(cab.getX() + dir.getStepX() * frac, cab.getY(), cab.getZ() + dir.getStepZ() * frac);
		if (elapsed < ticksPerStep) {
			return;
		}
		// a cell reached: set down, one step of the cab's program, and lift again if it moves on
		GantryCabEntity c = setDown(level);
		cells.clear();
		if (c == null) {
			discard();
			return;
		}
		GantryCabEntity.Frame f = c.frame();
		if (f == null || !c.poweredNow()) {
			discard();     // it stays down where it is; the cab's readout says why
			return;
		}
		c.runStep(level, f);
		GantryCabEntity now = c.movedTo() != null && level.getBlockEntity(c.movedTo()) instanceof GantryCabEntity next ? next : c;
		GantryCabEntity.Frame nf = now.frame();
		if (nf == null || now.nextMove(nf) == null) {
			now.resetCooldown();
			discard();
			return;
		}
		take(level, now, nf, now.nextMove(nf));
	}

	public BlockPos cab() {
		return cab;
	}

	public List<Cell> cells() {
		return List.copyOf(cells);
	}

	/** Client: the carried blocks, decoded from the synced data. */
	public List<Cell> shownCells() {
		String s = entityData.get(DATA_CELLS);
		if (!s.equals(decoded)) {
			decoded = s;
			shown.clear();
			shown.addAll(decode(s));
		}
		return shown;
	}

	private static String encode(List<Cell> cells) {
		StringBuilder b = new StringBuilder();
		for (Cell c : cells) {
			if (b.length() > 0) {
				b.append(';');
			}
			b.append(c.offset().getX()).append(',').append(c.offset().getY()).append(',').append(c.offset().getZ()).append(',').append(Block.getId(c.state()));
		}
		return b.toString();
	}

	private static List<Cell> decode(String s) {
		List<Cell> out = new ArrayList<>();
		if (s.isEmpty()) {
			return out;
		}
		for (String part : s.split(";")) {
			String[] v = part.split(",");
			if (v.length == 4) {
				out.add(new Cell(new BlockPos(Integer.parseInt(v[0]), Integer.parseInt(v[1]), Integer.parseInt(v[2])), Block.stateById(Integer.parseInt(v[3]))));
			}
		}
		return out;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		out.putLong("Cab", cab.asLong());
		out.putString("Dir", dir.getName());
		out.putInt("TicksPerStep", ticksPerStep);
		out.putInt("Elapsed", elapsed);
		var list = out.childrenList("Cells");
		for (Cell c : cells) {
			var o = list.addChild();
			o.putLong("Offset", c.offset().asLong());
			o.store("State", BlockState.CODEC, c.state());
		}
		if (cabData != null) {
			out.store("CabData", CompoundTag.CODEC, cabData);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		cab = BlockPos.of(in.getLongOr("Cab", 0L));
		Direction d = Direction.byName(in.getStringOr("Dir", "north"));
		dir = d == null ? Direction.NORTH : d;
		ticksPerStep = Math.max(1, in.getIntOr("TicksPerStep", 9));
		elapsed = in.getIntOr("Elapsed", 0);
		cells.clear();
		for (ValueInput o : in.childrenListOrEmpty("Cells")) {
			BlockState st = o.read("State", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
			cells.add(new Cell(BlockPos.of(o.getLongOr("Offset", 0L)), st));
		}
		cabData = in.read("CabData", CompoundTag.CODEC).orElse(null);
		entityData.set(DATA_CELLS, encode(cells));
	}
}
