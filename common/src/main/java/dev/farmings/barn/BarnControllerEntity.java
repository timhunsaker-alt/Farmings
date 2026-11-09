package dev.farmings.barn;

import dev.farmings.FarmingsBlocks;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.CattleEntity;
import dev.farmings.feed.Hay;
import dev.farmings.feed.Intake;
import dev.farmings.husbandry.Stage;
import dev.farmings.husbandry.barn.BarnStalls;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
		housed = stallsTaken(level);
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
		if (check == null || Math.floorMod(level.getGameTime() + worldPosition.hashCode(), (long) CHECK_TICKS) == 0) {
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
		if (Math.floorMod(level.getGameTime() + worldPosition.hashCode(), (long) CHECK_TICKS) == 1) {
			tend(level, CHECK_TICKS);
		}
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

	// ---- the herd ---------------------------------------------------------------------------------------------------------

	/** How far a housed cow may be (out on the cow lane, in the parlor) and still keep her stall: the lane's 64 cells plus a parlor. */
	private static final int AWAY_RADIUS = 96;

	private BlockPos standPos(BarnStalls.Stall s) {
		return at(s.x(), floorY(), s.bedZ());
	}

	private BlockPos bunkPos(BarnStalls.Stall s) {
		return at(s.x(), floorY(), s.bunkZ());
	}

	/** The cattle inside the barn's walls that live in stalls: not calves. */
	private List<CattleEntity> herd(Level level) {
		BlockPos a = at(check.driveX() - 1, floorY() - 1, -6);
		BlockPos b = at(3, floorY() + 4, 6);
		AABB box = new AABB(Math.min(a.getX(), b.getX()), floorY() - 1, Math.min(a.getZ(), b.getZ()),
				Math.max(a.getX(), b.getX()) + 1, floorY() + 4, Math.max(a.getZ(), b.getZ()) + 1);
		return level.getEntitiesOfClass(CattleEntity.class, box, c -> !c.isBaby() && c.isAlive());
	}

	/** The cows in stalls: how many stalls there are, and how many are taken. */
	public int stallsTaken(Level level) {
		if (!formed()) {
			return 0;
		}
		int n = 0;
		for (CattleEntity c : herd(level)) {
			if (worldPosition.equals(c.stallController())) {
				n++;
			}
		}
		return n;
	}

	/**
	 * Tends the herd for {@code ticks} of time: gives each unhoused cow the nearest free stall (one cow a stall), and each housed cow eats from
	 * the hay feeder at her stall and drinks from a trough in her row, in the amounts of {@link Intake}. Manure drops in the alley in front of her
	 * stall once she has made 50 kg. Run once a second by the controller; tests give it a longer stretch to stand for a day.
	 */
	public void tend(Level level, int ticks) {
		if (!formed()) {
			return;
		}
		List<BarnStalls.Stall> stalls = BarnStalls.of(check.rowStartX(), check.stallsPerSide());
		List<CattleEntity> herd = herd(level);
		Set<Integer> taken = new HashSet<>();
		List<CattleEntity> homeless = new ArrayList<>();
		for (CattleEntity cow : herd) {
			BarnStalls.Stall mine = null;
			if (worldPosition.equals(cow.stallController()) && cow.stallStand() != null) {
				for (BarnStalls.Stall s : stalls) {
					if (standPos(s).equals(cow.stallStand()) && !taken.contains(s.id())) {
						mine = s;
					}
				}
			}
			if (mine == null) {
				if (cow.stallController() != null && worldPosition.equals(cow.stallController())) {
					cow.clearStall();
				}
				if (cow.stallController() == null) {
					homeless.add(cow);
				}
			} else {
				taken.add(mine.id());
			}
		}
		// her stall stays hers while she is out on the cow lane or in the parlor (docs/multiblocks/parlor.md)
		for (CattleEntity cow : level.getEntitiesOfClass(CattleEntity.class, new AABB(worldPosition).inflate(AWAY_RADIUS, 8, AWAY_RADIUS),
				c -> c.isAlive() && worldPosition.equals(c.stallController()) && !herd.contains(c))) {
			for (BarnStalls.Stall s : stalls) {
				if (standPos(s).equals(cow.stallStand())) {
					taken.add(s.id());
				}
			}
		}
		for (CattleEntity cow : homeless) {
			BarnStalls.Stall best = null;
			double bestD = Double.MAX_VALUE;
			for (BarnStalls.Stall s : stalls) {
				if (taken.contains(s.id())) {
					continue;
				}
				BlockPos p = standPos(s);
				double d = cow.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
				if (d < bestD) {
					bestD = d;
					best = s;
				}
			}
			if (best != null) {
				taken.add(best.id());
				cow.assignStall(worldPosition, standPos(best), bunkPos(best));
			}
		}
		for (CattleEntity cow : herd) {
			if (!worldPosition.equals(cow.stallController())) {
				continue;
			}
			for (BarnStalls.Stall s : stalls) {
				if (standPos(s).equals(cow.stallStand())) {
					feed(level, cow, s, ticks);
				}
			}
		}
		serve(level, herd);
		setChanged();
	}

	private void feed(Level level, CattleEntity cow, BarnStalls.Stall s, int ticks) {
		double dmi = cow.dryMatterIntakeKgPerDay();
		BlockPos feederPos = at(s.x(), floorY() + 1, s.bunkZ());
		FeederEntity feeder = BigParts.entity(level, feederPos) instanceof FeederEntity f ? f : null;
		TroughEntity trough = troughFor(level, s);
		double hay = feeder == null ? 0 : feeder.dryMatterKg();
		double water = trough == null ? 0 : trough.litres();
		Intake.Result r = Intake.step(dmi, 0, ticks, hay, water);
		if (feeder != null) {
			feeder.take(r.eatenKg());
		}
		if (trough != null) {
			trough.take(r.drunkL());
		}
		cow.recordTending(r.eatenKg(), r.drunkL(), r.fed(), r.watered());
		cow.applyDirt(dev.farmings.husbandry.Hygiene.dirtyFraction(manureInFront(level, s)), ticks);
		int items = cow.collectManureItems(r.manureKg());
		if (items > 0) {
			Vec3 spot = Vec3.atCenterOf(at(s.x(), floorY(), s.alleyZ()));
			net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(FarmingsItems.MANURE, items);
			ItemEntity e = new ItemEntity(level, spot.x, floorY() + 0.3, spot.z, stack);
			e.setDeltaMovement(Vec3.ZERO);
			level.addFreshEntity(e);
		}
	}

	/** The alley cell in front of this cow's stall in this barn, or null if she is not housed here. */
	public @org.jspecify.annotations.Nullable BlockPos alleyInFrontOf(CattleEntity cow) {
		if (!formed() || !worldPosition.equals(cow.stallController())) {
			return null;
		}
		for (BarnStalls.Stall s : BarnStalls.of(check.rowStartX(), check.stallsPerSide())) {
			if (standPos(s).equals(cow.stallStand())) {
				return at(s.x(), floorY(), s.alleyZ());
			}
		}
		return null;
	}

	/** Manure items lying in the alley cell in front of her stall: what makes her stall dirty (Hygiene). */
	private int manureInFront(Level level, BarnStalls.Stall s) {
		BlockPos cell = at(s.x(), floorY(), s.alleyZ());
		int n = 0;
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(cell).inflate(0.1, 1.0, 0.1))) {
			if (e.getItem().is(FarmingsItems.MANURE)) {
				n += e.getItem().getCount();
			}
		}
		return n;
	}

	/** A trough in her row: the first with water in it, or else the first there is. */
	private TroughEntity troughFor(Level level, BarnStalls.Stall s) {
		TroughEntity first = null;
		for (int ax = check.driveX() - 3; ax <= 4; ax++) {
			for (int k = 2; k <= 4; k++) {
				BlockPos p = at(ax, floorY(), s.side() * k);
				if (BigParts.entity(level, p) instanceof TroughEntity t) {
					if (first == null) {
						first = t;
					}
					if (t.litres() > 0) {
						return t;
					}
				}
			}
		}
		return first;
	}

	// ---- reading and saving --------------------------------------------------------------------------------------------

	private int housed;
	private java.util.UUID owner;

	/** The first player to read the controller owns the barn: their calves count toward the first-two-heifers rule. */
	public void claim(java.util.UUID player) {
		if (owner == null) {
			owner = player;
			setChanged();
		}
	}

	/** Natural service: a housed bull serves every housed cow that is in heat (breeding.md). */
	private void serve(Level level, List<CattleEntity> herd) {
		CattleEntity bull = null;
		for (CattleEntity c : herd) {
			if (c.stage() == Stage.BULL && c.genome() != null && worldPosition.equals(c.stallController())) {
				bull = c;
				break;
			}
		}
		for (CattleEntity c : herd) {
			if (owner != null && c.owner() == null) {
				c.setOwner(owner);
			}
			if (bull != null && c != bull && worldPosition.equals(c.stallController()) && c.canConceive()) {
				c.conceive(bull.genome(), bull.visualGenome(), level.getGameTime());
			}
		}
	}

	/** What the controller shows when read. */
	public String readout() {
		if (check == null) {
			return "Barn: not checked yet";
		}
		if (!check.formed()) {
			return "Barn: " + check.status();
		}
		String scraper = pass.running() ? "scraper " + pass.fault().label : (pass.fault() == ScraperPass.Fault.NONE ? "scraper idle" : "scraper " + pass.fault().label);
		return "Barn: FORMED, " + check.stallsPerSide() + " stalls a side (" + check.stalls() + " cows), " + scraper + ", " + housed + " housed";
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putIntArray("Pass", pass.save());
		output.putInt("SinceLastPass", sinceLastPass);
		if (owner != null) {
			output.putString("Owner", owner.toString());
		}
		output.putBoolean("Signal", signalWas);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		input.getIntArray("Pass").ifPresent(pass::load);
		sinceLastPass = input.getIntOr("SinceLastPass", 0);
		String own = input.getStringOr("Owner", "");
		owner = own.isEmpty() ? null : java.util.UUID.fromString(own);
		signalWas = input.getBooleanOr("Signal", false);
	}
}
