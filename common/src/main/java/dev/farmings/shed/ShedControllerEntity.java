package dev.farmings.shed;

import dev.farmings.barn.BarnPartBlock;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import dev.farmings.entity.SheepEntity;
import dev.farmings.husbandry.Flock;
import dev.farmings.husbandry.SheepStage;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.shed.PastureLayout;
import dev.farmings.husbandry.shed.ShedLayout;
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
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Runs a sheep shed (docs/multiblocks/sheep-shed.md): checks the layout, takes in sheep up to the pen's room, gives each a home cell
 * (a lambing pen for a ewe about to lamb), sends the flock out to pasture by day and home at dusk or in rain, and feeds and waters it:
 * grass on pasture, hay from the racks in the shed, water from the troughs. The layout itself is plain Java ({@link ShedLayout},
 * {@link PastureLayout}).
 */
public class ShedControllerEntity extends BlockEntity {
	/** How far the shed takes in sheep that have no shed *(proposed)*. */
	public static final double TAKE_IN_RADIUS = 48;
	/** A ewe due within this many real days goes to a lambing pen *(proposed)*. */
	public static final double JUG_BEFORE_DAYS = 3;
	/** She stays in the jug this long after lambing *(proposed)*. */
	public static final double JUG_AFTER_DAYS = 14;
	/** Lambs younger than this stay in with their dams *(proposed)*. */
	public static final double LAMBS_STAY_IN_DAYS = 14;
	/** A grazing sheep picks a new spot this often, in ticks *(proposed: 30 seconds)*. */
	public static final int REPICK_TICKS = 600;
	/** Fed and watered shares are running averages: each step moves them this far toward the step's share. */
	private static final double SMOOTHING = 0.05;

	private ShedLayout.@Nullable Check check;
	private PastureLayout.@Nullable Pasture pasture;
	private final Set<Long> pastureCells = new HashSet<>();
	private final List<BlockPos> racks = new ArrayList<>();
	private final List<BlockPos> troughs = new ArrayList<>();
	private final Map<UUID, Double> grazeCredit = new HashMap<>();
	private final Map<UUID, Long> lastPick = new HashMap<>();
	private boolean out;
	private int flock;
	private double grazedBlocks;

	public ShedControllerEntity(BlockPos pos, BlockState state) {
		super(ShedBlocks.SHED_CONTROLLER_ENTITY, pos, state);
	}

	public ShedLayout.@Nullable Check check() {
		return check;
	}

	public PastureLayout.@Nullable Pasture pasture() {
		return pasture;
	}

	/** Game tests: fix a dry day (true) or night (false) instead of reading the clock and weather; null reads them. Not saved. */
	private @Nullable Boolean forcedDay;

	public void forceDayForTest(@Nullable Boolean day) {
		this.forcedDay = day;
	}

	public boolean flockOut() {
		return out;
	}

	// ---- the layout ----

	private static ShedLayout.Cell cell(ServerLevel level, BlockPos p) {
		BlockState s = level.getBlockState(p);
		Block b = s.getBlock();
		if (b == ShedBlocks.SHEEP_PEN) {
			return ShedLayout.Cell.PEN;
		}
		if (b == ShedBlocks.LAMBING_PEN) {
			return ShedLayout.Cell.LAMBING_PEN;
		}
		if (b == ShedBlocks.PASTURE_GATE) {
			return ShedLayout.Cell.GATE;
		}
		if (b == ShedBlocks.SHED_CONTROLLER) {
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

	/** Checks the shed and its pasture again. */
	public void refresh(ServerLevel level) {
		check = ShedLayout.check((x, y, z) -> cell(level, new BlockPos(x, y, z)), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		racks.clear();
		troughs.clear();
		for (ShedLayout.Pos p : check.racks()) {
			BlockPos o = BigParts.owner(level, pos(p));
			if (!racks.contains(o)) {
				racks.add(o);
			}
		}
		for (ShedLayout.Pos p : check.troughs()) {
			BlockPos o = BigParts.owner(level, pos(p));
			if (!troughs.contains(o)) {
				troughs.add(o);
			}
		}
		pasture = null;
		pastureCells.clear();
		if (check.formed() && !check.gates().isEmpty()) {
			ShedLayout.Pos g = check.gates().get(0);
			pasture = PastureLayout.find(ground(level), g.x(), g.y(), g.z());
			if (pasture.found()) {
				for (PastureLayout.Spot s : pasture.spots()) {
					pastureCells.add(BlockPos.asLong(s.x(), s.y(), s.z()));
				}
			}
		}
	}

	/** Where a sheep can stand: the feet and head cells clear, the ground under them solid on top, no water. */
	private PastureLayout.Ground ground(ServerLevel level) {
		return new PastureLayout.Ground() {
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
				BlockPos p = new BlockPos(x, y, z);
				return level.getBlockState(p).getBlock() instanceof ShedFloorBlock || level.getBlockState(p).getBlock() == ShedBlocks.SHED_CONTROLLER;
			}
		};
	}

	private static boolean standable(ServerLevel level, BlockPos feet) {
		BlockState f = level.getBlockState(feet);
		BlockState h = level.getBlockState(feet.above());
		BlockState g = level.getBlockState(feet.below());
		if (!f.getFluidState().isEmpty() || !h.getFluidState().isEmpty() || !g.getFluidState().isEmpty()) {
			return false;
		}
		return f.getCollisionShape(level, feet).isEmpty() && h.getCollisionShape(level, feet.above()).isEmpty() && g.isFaceSturdy(level, feet.below(), Direction.UP);
	}

	// ---- the flock ----

	public List<SheepEntity> members(ServerLevel level) {
		return level.getEntitiesOfClass(SheepEntity.class, new AABB(worldPosition).inflate(TAKE_IN_RADIUS), s -> worldPosition.equals(s.shedController()));
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
		List<SheepEntity> members = members(level);
		flock = members.size();
		homes(members);
		long day = Math.floorMod(level.getOverworldClockTime(), TimeScale.TICKS_PER_GAME_DAY);
		boolean fine = forcedDay != null ? forcedDay : day < 12_000 && !level.isRaining();     // a test's forced day is a dry one
		out = pasture != null && pasture.found() && fine;
		pasture(level, members, now);
		tend(level, members, 20.0 / TimeScale.TICKS_PER_GAME_DAY);
	}

	private void takeIn(ServerLevel level) {
		int room = check.capacity() + check.lambing().size();
		int adults = 0;
		for (SheepEntity s : members(level)) {
			if (!s.stage().lamb()) {
				adults++;
			}
		}
		for (SheepEntity s : level.getEntitiesOfClass(SheepEntity.class, new AABB(worldPosition).inflate(TAKE_IN_RADIUS), s -> s.shedController() == null && !s.isWild())) {
			if (s.stage().lamb() || adults < room) {
				s.assignShed(worldPosition, null);
				if (!s.stage().lamb()) {
					adults++;
				}
			}
		}
	}

	private boolean needsJug(SheepEntity s) {
		double due = s.daysToLambing();
		if (due >= 0 && due <= JUG_BEFORE_DAYS) {
			return true;
		}
		return s.lambsNursing() > 0 && s.daysSinceLambing() < JUG_AFTER_DAYS;
	}

	/** Gives every member a home cell: a lambing pen for a ewe about to lamb or just lambed, else a pen cell; lambs go with their dams. */
	private void homes(List<SheepEntity> members) {
		List<BlockPos> pen = new ArrayList<>();
		for (ShedLayout.Pos p : check.pen()) {
			pen.add(pos(p));
		}
		Set<BlockPos> jugs = new HashSet<>();
		for (ShedLayout.Pos p : check.lambing()) {
			jugs.add(pos(p));
		}
		Set<BlockPos> jugTaken = new HashSet<>();
		for (SheepEntity s : members) {
			if (s.home() != null && jugs.contains(s.home()) && needsJug(s)) {
				jugTaken.add(s.home());
			}
		}
		Map<String, SheepEntity> dams = new HashMap<>();
		int i = 0;
		for (SheepEntity s : members) {
			if (s.stage().lamb()) {
				continue;
			}
			dams.put(s.herdId(), s);
			BlockPos home = s.home();
			if (needsJug(s)) {
				if (home == null || !jugs.contains(home)) {
					for (BlockPos j : jugs) {
						if (jugTaken.add(j)) {
							s.assignShed(worldPosition, j);
							break;
						}
					}
				}
			} else if (home == null || !pen.contains(home)) {
				s.assignShed(worldPosition, pen.get(i % pen.size()));
			}
			i++;
		}
		for (SheepEntity lamb : members) {
			if (lamb.stage().lamb()) {
				SheepEntity dam = dams.get(lamb.damId());
				lamb.assignShed(worldPosition, dam != null && dam.home() != null ? dam.home() : pen.get(0));
			}
		}
	}

	/** Out by day: each sheep goes to a pasture spot and picks another every 30 seconds; home at dusk or in rain. */
	private void pasture(ServerLevel level, List<SheepEntity> members, long now) {
		List<PastureLayout.Spot> spots = pasture != null && pasture.found() ? pasture.spots() : List.of();
		for (SheepEntity s : members) {
			if (s.reservedBy() != null) {
				continue;     // called in to be shorn
			}
			boolean stays = needsJug(s) || (s.stage().lamb() && s.ageRealDays() < LAMBS_STAY_IN_DAYS);
			if (!out || stays || spots.isEmpty()) {
				if (s.sentTo() != null) {
					s.sendTo(null);
				}
				continue;
			}
			Long last = lastPick.get(s.getUUID());
			if (s.sentTo() == null || last == null || now - last >= REPICK_TICKS) {
				PastureLayout.Spot spot = spots.get(level.getRandom().nextInt(spots.size()));
				s.sendTo(new BlockPos(spot.x(), spot.y(), spot.z()));
				lastPick.put(s.getUUID(), now);
			}
		}
	}

	private boolean onPasture(SheepEntity s) {
		BlockPos p = s.blockPosition();
		return pastureCells.contains(p.asLong()) || pastureCells.contains(p.above().asLong()) || pastureCells.contains(p.below().asLong());
	}

	/**
	 * One step of feeding and water, {@code dayShare} of a game day: each sheep needs that share of one real day's intake. On pasture it
	 * grazes (a grass block it stands by turns to dirt for 1.5 kg of dry matter), in the shed it eats from the racks; water comes from
	 * the troughs. The shares met move its fed and watered averages.
	 */
	public void tend(ServerLevel level, List<SheepEntity> members, double dayShare) {
		for (SheepEntity s : members) {
			double need = s.dryMatterIntakeKgPerDay() * dayShare;
			if (need <= 0) {
				continue;
			}
			double got;
			if (onPasture(s)) {
				got = graze(level, s, need);
			} else {
				got = 0;
				for (BlockPos r : racks) {
					if (got >= need) {
						break;
					}
					if (level.getBlockEntity(r) instanceof FeederEntity f) {
						got += f.take(need - got);
					}
				}
			}
			double water = need * Flock.WATER_L_PER_KG_DM;
			double drunk = 0;
			for (BlockPos t : troughs) {
				if (drunk >= water) {
					break;
				}
				if (level.getBlockEntity(t) instanceof TroughEntity tr) {
					drunk += tr.take(water - drunk);
				}
			}
			double fed = s.lastFed() + SMOOTHING * (Math.min(1, got / need) - s.lastFed());
			double watered = s.lastWatered() + SMOOTHING * (Math.min(1, drunk / water) - s.lastWatered());
			s.recordTending(fed, watered);
		}
	}

	private double graze(ServerLevel level, SheepEntity s, double need) {
		double credit = grazeCredit.getOrDefault(s.getUUID(), 0.0);
		while (credit < need) {
			BlockPos grass = nearestGrass(level, s.blockPosition());
			if (grass == null) {
				break;
			}
			level.setBlockAndUpdate(grass, Blocks.DIRT.defaultBlockState());
			grazedBlocks++;
			credit += Flock.GRASS_BLOCK_KG_DM;
		}
		double got = Math.min(credit, need);
		grazeCredit.put(s.getUUID(), credit - got);
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

	public double grazedBlocks() {
		return grazedBlocks;
	}

	public String readout() {
		if (check == null) {
			return "Sheep shed: not checked yet";
		}
		if (!check.formed()) {
			return "Sheep shed: " + check.status();
		}
		StringBuilder b = new StringBuilder("Sheep shed: FORMED");
		b.append(String.format(Locale.ROOT, ", pen %d cells (room for %d), %d lambing pen%s", check.pen().size(), check.capacity(), check.lambing().size(),
				check.lambing().size() == 1 ? "" : "s"));
		if (check.lambing().size() < check.lambingWanted()) {
			b.append(" (").append(check.lambingWanted()).append(" wanted for a full shed)");
		}
		double hay = 0;
		for (BlockPos r : racks) {
			if (level != null && level.getBlockEntity(r) instanceof FeederEntity f) {
				hay += f.dryMatterKg();
			}
		}
		double water = 0;
		for (BlockPos t : troughs) {
			if (level != null && level.getBlockEntity(t) instanceof TroughEntity tr) {
				water += tr.litres();
			}
		}
		b.append(String.format(Locale.ROOT, ", %d hay rack%s (%.0f kg DM), %d trough%s (%.0f L)", racks.size(), racks.size() == 1 ? "" : "s", hay,
				troughs.size(), troughs.size() == 1 ? "" : "s", water));
		if (check.gates().isEmpty()) {
			b.append(", no pasture gate");
		} else if (pasture != null && !pasture.found()) {
			b.append(", pasture: ").append(pasture.problem());
		} else if (pasture != null) {
			b.append(String.format(Locale.ROOT, ", pasture %d blocks (%d grass)", pasture.spots().size(), pasture.grass()));
		}
		b.append(", flock ").append(flock).append(out ? ", out to pasture" : ", in");
		return b.toString();
	}
}
