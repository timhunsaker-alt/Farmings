package dev.farmings.piggery;

import dev.farmings.barn.BarnPartBlock;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import dev.farmings.entity.PigEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.Swine;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.shed.PastureLayout;
import dev.farmings.husbandry.shed.ShedLayout;
import dev.farmings.shed.ShedBlocks;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The pig barn (docs/multiblocks/pig-barn.md). The layout is the sheep shed's check (`ShedLayout`) with the barn's cells: slatted floor for
 * pens, farrowing crates for lambing pens, the barn's hay feeder as the pig feeder and its trough as the drinker, the shed's pasture gate.
 * Once a second it takes in tame pigs, gives each a home (a crate for a sow about to farrow or nursing, else a pen cell; piglets with their
 * dam), sends them to pasture by day, feeds and waters them, takes their manure into the pit under the slats, and keeps piglets warm under
 * a powered heat lamp.
 */
public class PigBarnControllerEntity extends BlockEntity {
	/** A barn takes in tame, unhoused pigs within this many blocks *(proposed, as the shed)*. */
	public static final double TAKE_IN_RADIUS = 48;
	/** A sow goes to a crate this many real days before she is due (pig-barn.md, proposed) and stays until weaning. */
	public static final double CRATE_BEFORE_DAYS = 5;
	/** Pit room a pen block, kg (0.5 m³ at about 1 kg a litre, proposed). */
	public static final double PIT_KG_PER_BLOCK = 500;
	/** Feed as fed is about 88% dry matter (grain, proposed): the feeders hold dry matter. */
	public static final double DM_SHARE = 0.88;
	private static final double SMOOTHING = 0.2;
	private static final long REPICK_TICKS = 600;

	private ShedLayout.@Nullable Check check;
	private PastureLayout.@Nullable Pasture pasture;
	private final Set<Long> pastureCells = new HashSet<>();
	private final List<BlockPos> feeders = new ArrayList<>();
	private final List<BlockPos> drinkers = new ArrayList<>();
	private final Map<UUID, Double> rootCredit = new HashMap<>();
	private final Map<UUID, Long> lastPick = new HashMap<>();
	private double pitKg;
	private int herd;
	private boolean out;
	private int rooted;
	private @Nullable Boolean forcedDay;

	public PigBarnControllerEntity(BlockPos pos, BlockState state) {
		super(PigBarnBlocks.PIG_BARN_CONTROLLER_ENTITY, pos, state);
	}

	public ShedLayout.@Nullable Check check() {
		return check;
	}

	public PastureLayout.@Nullable Pasture pasture() {
		return pasture;
	}

	public void forceDayForTest(@Nullable Boolean day) {
		this.forcedDay = day;
	}

	public double pitKg() {
		return pitKg;
	}

	public double pitCapacityKg() {
		return check == null ? 0 : check.pen().size() * PIT_KG_PER_BLOCK;
	}

	public void takeSlurry(double kg) {
		pitKg = Math.max(0, pitKg - kg);
		setChanged();
	}

	public void addSlurryForTest(double kg) {
		pitKg += kg;
	}

	public int rooted() {
		return rooted;
	}

	public boolean dirty() {
		return check != null && pitKg >= pitCapacityKg() - 1e-6 && pitCapacityKg() > 0;
	}

	// ---- the layout ----

	private static ShedLayout.Cell cell(ServerLevel level, BlockPos p) {
		Block b = level.getBlockState(p).getBlock();
		if (b == PigBarnBlocks.SLATTED_FLOOR) {
			return ShedLayout.Cell.PEN;
		}
		if (b == PigBarnBlocks.FARROWING_CRATE) {
			return ShedLayout.Cell.LAMBING_PEN;
		}
		if (b == ShedBlocks.PASTURE_GATE) {
			return ShedLayout.Cell.GATE;
		}
		if (b == PigBarnBlocks.PIG_BARN_CONTROLLER) {
			return ShedLayout.Cell.CONTROLLER;
		}
		if (b instanceof BarnPartBlock part) {
			if (part.kind() == BarnParts.HAY_FEEDER) {
				return ShedLayout.Cell.HAY_RACK;
			}
			if (part.kind() == BarnParts.WATER_TROUGH) {
				return ShedLayout.Cell.TROUGH;
			}
		}
		return ShedLayout.Cell.OTHER;
	}

	private static BlockPos pos(ShedLayout.Pos p) {
		return new BlockPos(p.x(), p.y(), p.z());
	}

	/** The shed check's words for a pig barn. */
	public String status() {
		if (check == null) {
			return "not checked yet";
		}
		return check.status().replace("sheep pen floor", "slatted floor").replace("sheep pen cells (room for 2 sheep)", "slatted floor cells")
				.replace("hay rack", "feeder").replace("water trough", "drinker (trough)");
	}

	public void refresh(ServerLevel level) {
		check = ShedLayout.check((x, y, z) -> cell(level, new BlockPos(x, y, z)), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		feeders.clear();
		drinkers.clear();
		for (ShedLayout.Pos p : check.racks()) {
			BlockPos o = BigParts.owner(level, pos(p));
			if (!feeders.contains(o)) {
				feeders.add(o);
			}
		}
		for (ShedLayout.Pos p : check.troughs()) {
			BlockPos o = BigParts.owner(level, pos(p));
			if (!drinkers.contains(o)) {
				drinkers.add(o);
			}
		}
		pasture = null;
		pastureCells.clear();
		if (check.formed() && !check.gates().isEmpty()) {
			ShedLayout.Pos g = check.gates().get(0);
			pasture = PastureLayout.find(new PastureLayout.Ground() {
				@Override
				public PastureLayout.@Nullable Spot at(int x, int z, int nearY) {
					for (int dy : new int[] {0, 1, -1}) {
						BlockPos feet = new BlockPos(x, nearY + dy, z);
						if (standable(level, feet)) {
							return new PastureLayout.Spot(x, feet.getY(), z, level.getBlockState(feet.below()).is(Blocks.GRASS_BLOCK));
						}
					}
					return null;
				}

				@Override
				public boolean shed(int x, int y, int z) {
					Block b = level.getBlockState(new BlockPos(x, y, z)).getBlock();
					return b == PigBarnBlocks.SLATTED_FLOOR || b == PigBarnBlocks.FARROWING_CRATE || b == ShedBlocks.PASTURE_GATE || b == PigBarnBlocks.PIG_BARN_CONTROLLER;
				}
			}, g.x(), g.y(), g.z());
			if (pasture.found()) {
				for (PastureLayout.Spot s : pasture.spots()) {
					pastureCells.add(BlockPos.asLong(s.x(), s.y(), s.z()));
				}
			}
		}
	}

	private static boolean standable(ServerLevel level, BlockPos feet) {
		BlockState f = level.getBlockState(feet);
		BlockState h = level.getBlockState(feet.above());
		BlockState g = level.getBlockState(feet.below());
		if (!f.getFluidState().isEmpty() || !h.getFluidState().isEmpty() || !g.getFluidState().isEmpty()) {
			return false;
		}
		return f.getCollisionShape(level, feet).isEmpty() && h.getCollisionShape(level, feet.above()).isEmpty()
				&& g.isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP);
	}

	// ---- the herd ----

	public List<PigEntity> members(ServerLevel level) {
		return level.getEntitiesOfClass(PigEntity.class, new AABB(worldPosition).inflate(TAKE_IN_RADIUS), p -> worldPosition.equals(p.place().shedController()));
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (check == null || now % 100 == 0) {
			refresh(level);
		}
		if (now % 20 != 0 || check == null || !check.formed()) {
			return;
		}
		takeIn(level);
		List<PigEntity> members = members(level);
		herd = members.size();
		homes(members);
		long day = Math.floorMod(level.getOverworldClockTime(), TimeScale.TICKS_PER_GAME_DAY);
		boolean fine = forcedDay != null ? forcedDay : day < 12_000 && !level.isRaining();
		out = pasture != null && pasture.found() && fine;
		toPasture(level, members, now);
		tend(level, members, 20.0 / TimeScale.TICKS_PER_GAME_DAY);
	}

	private static boolean crated(PigEntity p) {
		double due = p.daysToFarrowing();
		return p.sex() == Sex.FEMALE && (due >= 0 && due <= CRATE_BEFORE_DAYS || p.pigletsNursing() > 0);
	}

	private double space(PigEntity p) {
		return p.stage().piglet() || crated(p) ? 0 : Swine.penBlocks(p.weightKg(), p.adultBreeder());
	}

	private void takeIn(ServerLevel level) {
		double used = 0;
		for (PigEntity p : members(level)) {
			used += space(p);
		}
		double room = check.pen().size();
		for (PigEntity p : level.getEntitiesOfClass(PigEntity.class, new AABB(worldPosition).inflate(TAKE_IN_RADIUS),
				p -> p.place().shedController() == null && !p.isWild())) {
			double need = space(p);
			if (used + need <= room + 1e-9) {
				p.place().assignShed(worldPosition, null);
				used += need;
			}
		}
	}

	/** A crate for a sow due within 5 days or nursing; a pen cell for the rest; piglets go where their dam is. */
	private void homes(List<PigEntity> members) {
		List<BlockPos> pen = new ArrayList<>();
		for (ShedLayout.Pos p : check.pen()) {
			pen.add(pos(p));
		}
		Set<BlockPos> crates = new HashSet<>();
		for (ShedLayout.Pos p : check.lambing()) {
			crates.add(pos(p));
		}
		Set<BlockPos> taken = new HashSet<>();
		for (PigEntity p : members) {
			BlockPos h = p.place().home();
			if (h != null && crates.contains(h) && crated(p)) {
				taken.add(h);
			}
		}
		Map<String, PigEntity> dams = new HashMap<>();
		int i = 0;
		for (PigEntity p : members) {
			if (p.stage().piglet()) {
				continue;
			}
			dams.put(p.herdId(), p);
			BlockPos h = p.place().home();
			if (crated(p)) {
				if (h == null || !crates.contains(h)) {
					for (BlockPos c : crates) {
						if (taken.add(c)) {
							p.place().assignShed(worldPosition, c);
							break;
						}
					}
				}
			} else if (h == null || !pen.contains(h)) {
				p.place().assignShed(worldPosition, pen.get(i % pen.size()));
			}
			i++;
		}
		for (PigEntity piglet : members) {
			if (piglet.stage().piglet()) {
				PigEntity dam = dams.get(piglet.damId());
				piglet.place().assignShed(worldPosition, dam != null && dam.place().home() != null ? dam.place().home() : pen.get(0));
			}
		}
	}

	private void toPasture(ServerLevel level, List<PigEntity> members, long now) {
		List<PastureLayout.Spot> spots = pasture != null && pasture.found() ? pasture.spots() : List.of();
		for (PigEntity p : members) {
			if (!out || spots.isEmpty() || crated(p) || p.stage().piglet()) {
				if (p.place().sentTo() != null) {
					p.place().sendTo(null);
				}
				continue;
			}
			Long last = lastPick.get(p.getUUID());
			if (p.place().sentTo() == null || last == null || now - last >= REPICK_TICKS) {
				PastureLayout.Spot s = spots.get(level.getRandom().nextInt(spots.size()));
				p.place().sendTo(new BlockPos(s.x(), s.y(), s.z()));
				lastPick.put(p.getUUID(), now);
			}
		}
	}

	private boolean onPasture(PigEntity p) {
		BlockPos b = p.blockPosition();
		return pastureCells.contains(b.asLong()) || pastureCells.contains(b.above().asLong()) || pastureCells.contains(b.below().asLong());
	}

	/** A powered heat lamp within 3 blocks above the cell (the crate's creep). */
	private boolean warmAt(ServerLevel level, BlockPos cell) {
		for (int dy = 1; dy <= 3; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (level.getBlockEntity(cell.offset(dx, dy, dz)) instanceof HeatLampEntity lamp && lamp.powered()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * One step of {@code dayShare} of a game day: feed (rooting on pasture first), water, manure into the pit, warmth for piglets, and the
	 * dirty-pit health cost (the barn's rule).
	 */
	public void tend(ServerLevel level, List<PigEntity> members, double dayShare) {
		boolean dirty = dirty();
		for (PigEntity p : members) {
			if (p.stage().piglet()) {
				BlockPos h = p.place().home();
				if (h == null || !warmAt(level, h)) {
					// a newborn needs 32-35 °C (Smith et al. 2019): without a lamp it loses health as a cold chick does (proposed)
					p.setHealth((float) Math.max(1.0, p.getHealth() - dev.farmings.husbandry.poultry.Fowl.COLD_HEALTH_LOSS / 100.0 * p.getMaxHealth() * dayShare));
				}
				continue;
			}
			double needDm = p.feedKgPerDay() * DM_SHARE * dayShare;
			double got = 0;
			if (onPasture(p)) {
				got += root(level, p, needDm * Swine.forageShare(p.adultBreeder()));
				if (got > 0) {
					p.recordRation(dev.farmings.feed.Feedstuff.PASTURE.analysis());
				}
			}
			for (BlockPos f : feeders) {
				if (got >= needDm - 1e-12) {
					break;
				}
				if (level.getBlockEntity(f) instanceof FeederEntity fe) {
					double took = fe.take(needDm - got);
					if (took > 0) {
						p.recordRation(fe.analysis());
					}
					got += took;
				}
			}
			double water = p.dailyWaterL() * dayShare;
			double drunk = 0;
			for (BlockPos t : drinkers) {
				if (drunk >= water) {
					break;
				}
				if (level.getBlockEntity(t) instanceof TroughEntity tr) {
					drunk += tr.take(water - drunk);
				}
			}
			double fed = p.lastFed() + SMOOTHING * (Math.min(1, needDm <= 0 ? 1 : got / needDm) - p.lastFed());
			double watered = p.lastWatered() + SMOOTHING * (Math.min(1, water <= 0 ? 1 : drunk / water) - p.lastWatered());
			p.recordTending(fed, watered);
			if (!onPasture(p)) {
				pitKg = Math.min(pitCapacityKg(), pitKg + p.manureKgPerDay() * dayShare);
			}
			if (dirty && p.getHealth() > dev.farmings.husbandry.Hygiene.HEALTH_FLOOR) {
				p.setHealth((float) Math.max(dev.farmings.husbandry.Hygiene.HEALTH_FLOOR, p.getHealth() - dev.farmings.husbandry.Hygiene.HEALTH_LOSS_PER_GAME_DAY * dayShare));
			}
		}
		setChanged();
	}

	/** Rooting: forage comes off grass blocks near the pig at 1.5 kg DM a block, which turns to dirt (pigs.md). Returns kg DM got. */
	private double root(ServerLevel level, PigEntity p, double need) {
		double credit = rootCredit.getOrDefault(p.getUUID(), 0.0);
		while (credit < need) {
			BlockPos grass = nearestGrass(level, p.blockPosition());
			if (grass == null) {
				break;
			}
			level.setBlockAndUpdate(grass, Blocks.DIRT.defaultBlockState());
			rooted++;
			credit += dev.farmings.husbandry.Flock.GRASS_BLOCK_KG_DM;
		}
		double got = Math.min(credit, need);
		rootCredit.put(p.getUUID(), credit - got);
		return got;
	}

	private @Nullable BlockPos nearestGrass(ServerLevel level, BlockPos at) {
		for (int r = 0; r <= 3; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					for (int dy = -1; dy <= 1; dy++) {
						BlockPos feet = at.offset(dx, dy, dz);
						if (pastureCells.contains(feet.asLong()) && level.getBlockState(feet.below()).is(Blocks.GRASS_BLOCK)) {
							return feet.below();
						}
					}
				}
			}
		}
		return null;
	}

	public String readout() {
		if (check == null) {
			return "Pig barn: not checked yet";
		}
		if (!check.formed()) {
			return "Pig barn: " + status();
		}
		StringBuilder b = new StringBuilder("Pig barn: FORMED");
		b.append(String.format(Locale.ROOT, ", pens %d blocks, %d farrowing crate%s, %d feeder%s (room for %d), %d drinker%s (room for %d)",
				check.pen().size(), check.lambing().size(), check.lambing().size() == 1 ? "" : "s", feeders.size(), feeders.size() == 1 ? "" : "s",
				feeders.size() * 5, drinkers.size(), drinkers.size() == 1 ? "" : "s", drinkers.size() * 12));
		if (pasture != null && pasture.found()) {
			b.append(String.format(Locale.ROOT, ", pasture %d blocks (%d grass)", pasture.spots().size(), pasture.grass()));
		}
		b.append(String.format(Locale.ROOT, ", pit %.1f of %.1f m³%s", pitKg / 1000, pitCapacityKg() / 1000, dirty() ? " FULL: DIRTY" : ""));
		b.append(", herd ").append(herd).append(out ? ", out to pasture" : ", in");
		return b.toString();
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putDouble("PitKg", pitKg);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		pitKg = in.getDoubleOr("PitKg", 0);
	}
}
