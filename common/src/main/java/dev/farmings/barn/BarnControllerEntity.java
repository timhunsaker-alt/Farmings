package dev.farmings.barn;

import dev.farmings.FarmingsBlocks;
import dev.farmings.FarmingsItems;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.barn.BarnCheck;
import dev.farmings.husbandry.barn.BarnPart;
import dev.farmings.husbandry.barn.Orientation;
import dev.farmings.husbandry.barn.ScraperPass;
import dev.wildcatter.multiblock.BigParts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The barn's controller: it checks the structure once a second and, while it is formed, runs the manure scraper (docs/multiblocks/barn.md).
 * One pass starts each game day, or when a redstone signal reaches the controller. The blade moves one cell at a time, pushes every
 * manure item in front of it along the alley, empties them into the collection pit at the end, and returns. It only moves while the
 * drive's motor is running, and it stops with NO POWER, PIT FULL, BLOCKED or NO SCRAPER.
 */
public class BarnControllerEntity extends BlockEntity {
	/** How often the structure is checked, in ticks (Wildcatter's STRUCTURE_CHECK_TICKS). */
	public static final int CHECK_TICKS = 20;
	/** One pass per game day *(proposed; real scrapers run several times a day)*. */
	public static final int TICKS_BETWEEN_PASSES = TimeScale.TICKS_PER_GAME_DAY;

	private BarnCheck check;
	private final ScraperPass pass = new ScraperPass();
	private int sinceLastPass;
	private boolean signalWas;

	public BarnControllerEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.CONTROLLER_ENTITY, pos, state);
	}

	// ---- the structure -------------------------------------------------------------------------------------------------

	public void refresh(Level level) {
		check = BarnWorld.check(level, worldPosition);
	}

	/** The last check, or null before the first one. */
	public BarnCheck check() {
		return check;
	}

	public boolean formed() {
		return check != null && check.formed();
	}

	public ScraperPass pass() {
		return pass;
	}

	// ---- positions: the barn is drawn with its controller at 0, the pit towards +x, and may lie any of eight ways ---------

	private BlockPos at(int ax, int y, int az) {
		int[] w = check.orientation().world(ax, az);
		return new BlockPos(worldPosition.getX() + w[0], y, worldPosition.getZ() + w[1]);
	}

	private int floorY() {
		return worldPosition.getY() - 1;
	}

	private int levelY() {
		return worldPosition.getY();
	}

	private int restX() {
		return check.driveX() + 1;
	}

	private static final int PIT_END_X = -1;

	/** Where the blade is: its drawn x along the alley, or null if there is none on the rail. */
	private Integer bladeAt(Level level) {
		for (int ax = restX(); ax <= PIT_END_X; ax++) {
			if (isBlade(level, ax, 0) && isBlade(level, ax, -1) && isBlade(level, ax, 1)) {
				return ax;
			}
		}
		return null;
	}

	private boolean isBlade(Level level, int ax, int az) {
		return BarnWorld.partOf(level.getBlockState(at(ax, levelY(), az))) == BarnPart.MANURE_SCRAPER;
	}

	private boolean cellFree(Level level, int ax) {
		if (ax < restX() || ax > PIT_END_X) {
			return false;
		}
		for (int az = -1; az <= 1; az++) {
			if (!level.getBlockState(at(ax, levelY(), az)).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** The drive's motor, or null. */
	public DriveEntity drive(Level level) {
		BlockPos pos = at(check.driveX(), levelY(), 0);
		return BigParts.entity(level, pos) instanceof DriveEntity d ? d : null;
	}

	/** The pit's store, or null. */
	public PitEntity pit(Level level) {
		return BigParts.entity(level, worldPosition.below()) instanceof PitEntity p ? p : null;
	}

	private ScraperPass.Look look(Level level) {
		Integer blade = bladeAt(level);
		DriveEntity drive = drive(level);
		PitEntity pit = pit(level);
		int x = blade == null ? 0 : blade;
		return new ScraperPass.Look(blade != null, blade != null && x == PIT_END_X, blade != null && x == restX(), drive != null && drive.powered(),
				blade != null && cellFree(level, x + 1), blade != null && cellFree(level, x - 1), pit != null && pit.room() > 0);
	}

	// ---- running -------------------------------------------------------------------------------------------------------

	public void serverTick(Level level) {
		if (check == null || (level.getGameTime() + worldPosition.hashCode()) % CHECK_TICKS == 0) {
			refresh(level);
		}
		if (!formed()) {
			return;
		}
		boolean signal = level.hasNeighborSignal(worldPosition);
		if (!pass.running()) {
			sinceLastPass++;
			boolean wanted = (signal && !signalWas) || sinceLastPass >= TICKS_BETWEEN_PASSES;
			if (wanted && pass.start(look(level))) {
				sinceLastPass = 0;
			}
			if (!pass.running() && level.getGameTime() % CHECK_TICKS == 0) {
				pass.tick(look(level));      // keeps NO SCRAPER and PIT FULL current while idle
			}
		}
		signalWas = signal;
		DriveEntity drive = drive(level);
		if (drive != null) {
			drive.setDemand(pass.running());
		}
		if (pass.running()) {
			act(level, pass.tick(look(level)));
		}
	}

	/** Starts a pass now, as a redstone signal would. */
	public boolean startPass(Level level) {
		if (!formed()) {
			return false;
		}
		boolean started = pass.start(look(level));
		if (started) {
			sinceLastPass = 0;
		}
		return started;
	}

	private void act(Level level, ScraperPass.Action action) {
		Integer blade = bladeAt(level);
		if (blade == null) {
			return;
		}
		switch (action) {
			case MOVE_TO_PIT -> {
				pushManure(level, blade + 1);
				moveBlade(level, blade, blade + 1);
			}
			case MOVE_TO_DRIVE -> moveBlade(level, blade, blade - 1);
			case DUMP -> dump(level);
			default -> {
			}
		}
		setChanged();
	}

	/** The blade is entering cell {@code ax}: manure there is pushed one cell on, ahead of the blade (into the pit from the last cell). */
	private void pushManure(Level level, int ax) {
		AABB cell = cellBox(ax, ax, -1, 1);
		Vec3 ahead = Vec3.atCenterOf(at(ax + 1, floorY(), 0)).subtract(Vec3.atCenterOf(at(ax, floorY(), 0)));
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, cell, i -> i.getItem().is(FarmingsItems.MANURE))) {
			e.setPos(e.getX() + ahead.x, e.getY(), e.getZ() + ahead.z);
			e.setDeltaMovement(Vec3.ZERO);
		}
	}

	/** Every manure item in the pit's top cells goes into the pit, as many as fit. */
	private void dump(Level level) {
		PitEntity pit = pit(level);
		if (pit == null) {
			return;
		}
		AABB top = cellBox(0, 1, -1, 1);
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, top, i -> i.getItem().is(FarmingsItems.MANURE))) {
			int taken = pit.add(e.getItem().getCount());
			if (taken >= e.getItem().getCount()) {
				e.discard();
			} else {
				e.getItem().shrink(taken);
			}
		}
	}

	/** The world box of drawn cells ax0..ax1 along the barn and az0..az1 across, from the floor up to two blocks. */
	private AABB cellBox(int ax0, int ax1, int az0, int az1) {
		BlockPos a = at(ax0, floorY(), az0);
		BlockPos b = at(ax1, floorY(), az1);
		return new AABB(Math.min(a.getX(), b.getX()), floorY(), Math.min(a.getZ(), b.getZ()),
				Math.max(a.getX(), b.getX()) + 1, floorY() + 2, Math.max(a.getZ(), b.getZ()) + 1);
	}

	/** Moves the whole blade one cell: its cells go and the same part is placed one cell along. */
	private void moveBlade(Level level, int from, int to) {
		BlockPos oldPos = at(from, levelY(), 0);
		BlockState state = level.getBlockState(oldPos);
		BlockPos main = BigParts.main(oldPos, state);
		BlockPos shift = at(to, levelY(), 0).subtract(oldPos);
		List<BlockPos> cells = BigParts.cells(main, state);
		for (int i = cells.size() - 1; i >= 0; i--) {
			level.setBlock(cells.get(i), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
		}
		BlockPos newMain = main.offset(shift);
		BlockState placed = state.setValue(((dev.wildcatter.multiblock.BigPart) state.getBlock()).piece(), 0);
		level.setBlock(newMain, placed, Block.UPDATE_ALL);
		BigParts.grow(level, newMain, placed);
	}

	// ---- reading and saving --------------------------------------------------------------------------------------------

	/** What the controller shows when read. */
	public String readout() {
		if (check == null) {
			return "Barn: not checked yet";
		}
		if (!check.formed()) {
			return "Barn: " + check.status();
		}
		String scraper = pass.running() ? "scraper " + pass.fault().label : (pass.fault() == ScraperPass.Fault.NONE ? "scraper idle" : "scraper " + pass.fault().label);
		return "Barn: FORMED, " + check.stallsPerSide() + " stalls a side (" + check.stalls() + " cows), " + scraper;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putIntArray("Pass", pass.save());
		output.putInt("SinceLastPass", sinceLastPass);
		output.putBoolean("Signal", signalWas);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		input.getIntArray("Pass").ifPresent(pass::load);
		sinceLastPass = input.getIntOr("SinceLastPass", 0);
		signalWas = input.getBooleanOr("Signal", false);
	}
}
