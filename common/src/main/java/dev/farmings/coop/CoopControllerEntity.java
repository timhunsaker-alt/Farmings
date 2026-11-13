package dev.farmings.coop;

import dev.farmings.FarmingsItems;
import dev.farmings.barn.BarnPartBlock;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.TroughEntity;
import dev.farmings.entity.FowlEggs;
import dev.farmings.entity.FowlEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.coop.CoopLayout;
import dev.farmings.husbandry.poultry.Clutch;
import dev.farmings.husbandry.poultry.Fowl;
import dev.farmings.husbandry.poultry.FowlBreed;
import dev.farmings.husbandry.shed.PastureLayout;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.EnumMap;
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
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Runs a coop (docs/multiblocks/coop.md): checks the layout, takes in tame birds up to the floor's room, gives each a home cell, sends them
 * out to the run by day and in at dusk, feeds them grain and water, takes eggs into nest boxes, keeps broody hens on their clutches until
 * they hatch, checks the ducks' pond and gathers litter. The layout is plain Java ({@link CoopLayout}, {@link PastureLayout}).
 */
public class CoopControllerEntity extends BlockEntity implements WorldlyContainer {
	/** How far the coop takes in birds that have no coop (the shed's 48, proposed). */
	public static final double TAKE_IN_RADIUS = 48;
	/** A bird on the run picks a new spot this often, in ticks (the shed's 30 seconds). */
	public static final int REPICK_TICKS = 600;
	/** One litter item is 10 kg (proposed); the coop holds 64. */
	public static final double LITTER_ITEM_KG = 10;
	public static final int LITTER_MAX_ITEMS = 64;
	/** Daily health costs, as shares of a bird's health bar (proposed; the doc's points on a 100-point scale). */
	public static final double POND_LOSS = 0.05;
	public static final double LITTER_LOSS = 0.02;
	/** A broody hen or a mother warms her young within this reach (poultry.md). */
	public static final double MOTHER_REACH = 2.0;
	private static final double SMOOTHING = 0.05;
	private static final int[] SLOTS = {0};

	private CoopLayout.@Nullable Check check;
	private PastureLayout.@Nullable Pasture run;
	private String runProblem = "";
	private final Set<Long> runCells = new HashSet<>();
	private final List<BlockPos> nests = new ArrayList<>();
	private final List<BlockPos> perches = new ArrayList<>();
	private final List<BlockPos> feeders = new ArrayList<>();
	private final List<BlockPos> troughs = new ArrayList<>();
	private int pond;
	private final Map<UUID, Long> lastPick = new HashMap<>();
	private boolean out;
	private int flock;
	private double litterKg;
	/** Forage the run's birds have eaten past its regrowth, kg DM; each 1.5 kg bares one grass block (poultry.md, "Foraging on the run"). */
	private double runDrawnKg;
	private long lastDay = -1;
	private int hatched;
	private @Nullable Boolean forcedDay;

	public CoopControllerEntity(BlockPos pos, BlockState state) {
		super(CoopBlocks.COOP_CONTROLLER_ENTITY, pos, state);
	}

	public CoopLayout.@Nullable Check check() {
		return check;
	}

	public PastureLayout.@Nullable Pasture run() {
		return run;
	}

	public int pond() {
		return pond;
	}

	public boolean flockOut() {
		return out;
	}

	public double litterKg() {
		return litterKg;
	}

	public int hatched() {
		return hatched;
	}

	public List<BlockPos> nests() {
		return nests;
	}

	/** Game tests: fix a dry day (true) or night (false); null reads the clock and weather. Not saved. */
	public void forceDayForTest(@Nullable Boolean day) {
		this.forcedDay = day;
	}

	// ---- the layout ----

	private static CoopLayout.Cell cell(ServerLevel level, BlockPos p) {
		Block b = level.getBlockState(p).getBlock();
		if (b == CoopBlocks.COOP_FLOOR) {
			return CoopLayout.Cell.FLOOR;
		}
		if (b == CoopBlocks.POP_HOLE) {
			return CoopLayout.Cell.POP_HOLE;
		}
		if (b == CoopBlocks.COOP_CONTROLLER) {
			return CoopLayout.Cell.CONTROLLER;
		}
		if (b == CoopBlocks.NEST_BOX) {
			return CoopLayout.Cell.NEST;
		}
		if (b == CoopBlocks.PERCH) {
			return CoopLayout.Cell.PERCH;
		}
		if (b == CoopBlocks.GRAIN_FEEDER) {
			return CoopLayout.Cell.FEEDER;
		}
		if (b instanceof BarnPartBlock part && part.kind() == BarnParts.WATER_TROUGH) {
			return CoopLayout.Cell.TROUGH;
		}
		return CoopLayout.Cell.OTHER;
	}

	private static BlockPos pos(CoopLayout.Pos p) {
		return new BlockPos(p.x(), p.y(), p.z());
	}

	/** Checks the coop, its run and its pond again. */
	public void refresh(ServerLevel level) {
		check = CoopLayout.check((x, y, z) -> cell(level, new BlockPos(x, y, z)), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		nests.clear();
		perches.clear();
		feeders.clear();
		troughs.clear();
		check.nests().forEach(p -> nests.add(pos(p)));
		check.perches().forEach(p -> perches.add(pos(p)));
		check.feeders().forEach(p -> feeders.add(pos(p)));
		for (CoopLayout.Pos p : check.troughs()) {
			BlockPos o = BigParts.owner(level, pos(p));
			if (!troughs.contains(o)) {
				troughs.add(o);
			}
		}
		run = null;
		runProblem = "";
		runCells.clear();
		if (check.formed() && !check.popHoles().isEmpty()) {
			CoopLayout.Pos h = check.popHoles().get(0);
			PastureLayout.Pasture p = PastureLayout.find(ground(level), h.x(), h.y(), h.z());
			if (p.found() && p.spots().size() > CoopLayout.MAX_RUN) {
				runProblem = "the run is not fenced in (over " + CoopLayout.MAX_RUN + " blocks)";
			} else if (!p.found()) {
				runProblem = p.problem();
			} else {
				run = p;
				for (PastureLayout.Spot s : p.spots()) {
					runCells.add(BlockPos.asLong(s.x(), s.y(), s.z()));
				}
			}
		}
		pond = countPond(level);
	}

	/** Water source blocks in or beside the run; with no run, beside the coop floor (coop.md, "The duck pond"). */
	private int countPond(ServerLevel level) {
		Set<BlockPos> water = new HashSet<>();
		List<BlockPos> around = new ArrayList<>();
		if (run != null) {
			for (PastureLayout.Spot s : run.spots()) {
				around.add(new BlockPos(s.x(), s.y(), s.z()));
			}
		} else if (check != null) {
			check.floor().forEach(p -> around.add(pos(p)));
		}
		for (BlockPos a : around) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					for (int dy = -1; dy <= 0; dy++) {
						BlockPos q = a.offset(dx, dy, dz);
						if (level.getFluidState(q).isSource() && level.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER)) {
							water.add(q.immutable());
						}
					}
				}
			}
		}
		return water.size();
	}

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
				Block b = level.getBlockState(new BlockPos(x, y, z)).getBlock();
				return b == CoopBlocks.COOP_FLOOR || b == CoopBlocks.POP_HOLE || b == CoopBlocks.COOP_CONTROLLER;
			}
		};
	}

	private static boolean standable(ServerLevel level, BlockPos feet) {
		BlockState f = level.getBlockState(feet);
		BlockState g = level.getBlockState(feet.below());
		if (!f.getFluidState().isEmpty() || !g.getFluidState().isEmpty()) {
			return false;
		}
		return f.getCollisionShape(level, feet).isEmpty() && g.isFaceSturdy(level, feet.below(), Direction.UP);
	}

	// ---- the flock ----

	public List<FowlEntity> members(ServerLevel level) {
		return level.getEntitiesOfClass(FowlEntity.class, new AABB(worldPosition).inflate(TAKE_IN_RADIUS), f -> worldPosition.equals(f.coop()));
	}

	private static Map<Species, Integer> adults(List<FowlEntity> birds, boolean hensOnly) {
		Map<Species, Integer> m = new EnumMap<>(Species.class);
		for (FowlEntity f : birds) {
			if (!f.young() && (!hensOnly || f.sex() == Sex.FEMALE)) {
				m.merge(f.species(), 1, Integer::sum);
			}
		}
		return m;
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
		List<FowlEntity> members = members(level);
		flock = members.size();
		homes(members);
		long time = Math.floorMod(level.getOverworldClockTime(), TimeScale.TICKS_PER_GAME_DAY);
		boolean day = forcedDay != null ? forcedDay : time < 12_000;
		boolean rain = forcedDay == null && level.isRaining();
		toRun(level, members, now, day, rain);
		double dayShare = 20.0 / TimeScale.TICKS_PER_GAME_DAY;
		tend(level, members, dayShare);
		brood(level, members, 20);
		warmYoung(level, members, now);
		long d = now / TimeScale.TICKS_PER_GAME_DAY;
		if (lastDay < 0) {
			lastDay = d;
		} else if (d > lastDay) {
			lastDay = d;
			daily(level, members);
		}
	}

	private void takeIn(ServerLevel level) {
		List<FowlEntity> members = members(level);
		Map<Species, Integer> adults = adults(members, false);
		int floor = check.floor().size();
		for (FowlEntity f : level.getEntitiesOfClass(FowlEntity.class, new AABB(worldPosition).inflate(TAKE_IN_RADIUS), f -> f.coop() == null && !f.isWild())) {
			if (f.young()) {
				f.assignCoop(worldPosition, null);
			} else if (CoopLayout.room(adults, f.species(), floor)) {
				f.assignCoop(worldPosition, null);
				adults.merge(f.species(), 1, Integer::sum);
			}
		}
	}

	/** A home cell for each bird: a floor cell beside a perch for the roosters of the night (chickens, turkeys), else any floor cell. */
	private void homes(List<FowlEntity> members) {
		List<BlockPos> floor = new ArrayList<>();
		check.floor().forEach(p -> floor.add(pos(p)));
		List<BlockPos> roost = new ArrayList<>();
		for (BlockPos f : floor) {
			for (BlockPos p : perches) {
				if (Math.abs(p.getX() - f.getX()) + Math.abs(p.getZ() - f.getZ()) <= 1) {
					roost.add(f);
					break;
				}
			}
		}
		int i = 0;
		for (FowlEntity f : members) {
			boolean roosts = Fowl.birdsPerPerch(f.species()) > 0 && !roost.isEmpty();
			List<BlockPos> cells = roosts ? roost : floor;
			if (f.home() == null || !cells.contains(f.home())) {
				f.assignCoop(worldPosition, cells.get(i % cells.size()));
			}
			i++;
		}
	}

	/** Out by day in clear weather (ducks in rain too), only as many as the run holds; in at dusk. */
	private void toRun(ServerLevel level, List<FowlEntity> members, long now, boolean day, boolean rain) {
		List<PastureLayout.Spot> spots = run != null ? run.spots() : List.of();
		double room = spots.size();
		out = day && !spots.isEmpty();
		for (FowlEntity f : members) {
			boolean goes = out && !f.broody() && !f.young() && (!rain || f.species() == Species.DUCK || f.species() == Species.GOOSE);     // waterfowl go out in rain (proposed for geese)
			double need = Fowl.runCellsPerBird(f.species());
			if (goes && room >= need) {
				room -= need;
				Long last = lastPick.get(f.getUUID());
				if (f.sentTo() == null || last == null || now - last >= REPICK_TICKS) {
					PastureLayout.Spot s = spots.get(level.getRandom().nextInt(spots.size()));
					f.sendTo(new BlockPos(s.x(), s.y(), s.z()));
					lastPick.put(f.getUUID(), now);
				}
			} else if (f.sentTo() != null) {
				f.sendTo(null);
			}
		}
	}

	/** One step of grain and water ({@code dayShare} of a game day: that share of one real day's need), and the litter it leaves. */
	public void tend(ServerLevel level, List<FowlEntity> members, double dayShare) {
		double foraged = 0;
		for (FowlEntity f : members) {
			double need = Fowl.feedKgPerDay(f.species(), f.weightKg()) * dayShare;
			double got = 0;
			if (onGrassRun(level, f)) {
				got = need * Fowl.forageShare(f.species());     // geese two-thirds, the rest a fifth (poultry.md)
				foraged += got;
				f.recordRation(dev.farmings.feed.Feedstuff.PASTURE.analysis());
			}
			for (BlockPos b : feeders) {
				if (got >= need) {
					break;
				}
				if (level.getBlockEntity(b) instanceof GrainFeederEntity g) {
					double took = g.take(need - got);
					if (took > 0) {
						f.recordRation(g.analysis());
					}
					got += took;
				}
			}
			double water = Fowl.waterLPerDay(f.species(), f.weightKg()) * dayShare;
			double drunk = 0;
			for (BlockPos t : troughs) {
				if (drunk >= water) {
					break;
				}
				if (level.getBlockEntity(t) instanceof TroughEntity tr) {
					drunk += tr.take(water - drunk);
				}
			}
			double fed = f.lastFed() + SMOOTHING * (Math.min(1, need <= 0 ? 1 : got / need) - f.lastFed());
			double watered = f.lastWatered() + SMOOTHING * (Math.min(1, water <= 0 ? 1 : drunk / water) - f.lastWatered());
			f.recordTending(fed, watered);
			litterKg = Math.min(LITTER_ITEM_KG * LITTER_MAX_ITEMS, litterKg + f.weightKg() * Fowl.litterKgPerKgDay(f.species()) * dayShare);
		}
		wearRun(level, foraged, dayShare);
		setChanged();
	}

	/** A bird out on the run, standing on grass. */
	private boolean onGrassRun(ServerLevel level, FowlEntity f) {
		BlockPos p = f.blockPosition();
		for (BlockPos feet : new BlockPos[] {p, p.above(), p.below()}) {
			if (runCells.contains(feet.asLong())) {
				return level.getBlockState(feet.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
			}
		}
		return false;
	}

	/** The run regrows a little and wears by what was eaten; each 1.5 kg DM drawn bares one grass block, picked at random. */
	private void wearRun(ServerLevel level, double eatenKg, double dayShare) {
		if (run == null) {
			return;
		}
		List<BlockPos> grass = new java.util.ArrayList<>();
		for (PastureLayout.Spot s : run.spots()) {
			BlockPos below = new BlockPos(s.x(), s.y() - 1, s.z());
			if (level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
				grass.add(below);
			}
		}
		runDrawnKg = Fowl.runDrawn(runDrawnKg, eatenKg, grass.size(), dayShare);
		while (runDrawnKg >= dev.farmings.husbandry.Flock.GRASS_BLOCK_KG_DM && !grass.isEmpty()) {
			BlockPos b = grass.remove(level.getRandom().nextInt(grass.size()));
			level.setBlockAndUpdate(b, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
			runDrawnKg -= dev.farmings.husbandry.Flock.GRASS_BLOCK_KG_DM;
		}
	}

	public double runDrawnKg() {
		return runDrawnKg;
	}

	/** The nest box a broody hen sits by: the nest box beside her nest cell. */
	private @Nullable NestBoxEntity nestOf(ServerLevel level, FowlEntity hen) {
		BlockPos cell = hen.nest();
		if (cell == null) {
			return null;
		}
		for (BlockPos n : nests) {
			if (Math.abs(n.getX() - cell.getX()) + Math.abs(n.getZ() - cell.getZ()) == 1 && n.getY() == cell.getY()
					&& level.getBlockEntity(n) instanceof NestBoxEntity box) {
				return box;
			}
		}
		return null;
	}

	/** The floor cell beside a nest box, where its hen sits. */
	private @Nullable BlockPos sittingCell(BlockPos nest) {
		for (CoopLayout.Pos p : check.floor()) {
			if (Math.abs(p.x() - nest.getX()) + Math.abs(p.z() - nest.getZ()) == 1 && p.y() == nest.getY()) {
				return pos(p);
			}
		}
		return null;
	}

	/**
	 * Broody hens keep up to 12 fertile eggs of their species warm; each counts down its incubation days and hatches beside the nest. When
	 * her eggs are gone she stops sitting and mothers her young until they are feathered.
	 */
	public void brood(ServerLevel level, List<FowlEntity> members, long ticks) {
		double days = TimeScale.FOWL.ticksToRealDays(ticks);
		long now = level.getGameTime();
		for (FowlEntity hen : members) {
			if (!hen.broody()) {
				continue;
			}
			NestBoxEntity box = nestOf(level, hen);
			if (box == null) {
				hen.setBroody(false, null);
				continue;
			}
			int warmed = 0;
			boolean any = false;
			for (int i = 0; i < NestBoxEntity.SIZE; i++) {
				ItemStack egg = box.getItem(i);
				if (egg.isEmpty() || FowlEggs.species(egg) != hen.species() || !FowlEggs.fertile(egg)) {
					continue;
				}
				if (warmed >= Clutch.MAX_SITTING || !FowlEggs.hatchable(egg, now)) {
					continue;
				}
				any = true;
				warmed++;
				double warm = FowlEggs.warmDays(egg) + days;
				FowlEggs.setWarmDays(egg, warm);
				FowlBreed breed = FowlEntity.topBreed(dev.farmings.genetics.Genome.decode(FowlEggs.tag(egg).getStringOr(FowlEggs.DAM, "")).breedCompositionBp(),
						hen.species());
				double incubation = breed.incubationDays;
				if (warm >= incubation) {
					FowlEntity chick = FowlEggs.hatch(level, egg, hen.nest(), hen.owner());
					if (chick != null) {
						chick.assignCoop(worldPosition, hen.home());
						hatched++;
					}
					box.setItem(i, ItemStack.EMPTY);
				}
			}
			box.setChanged();
			if (!any) {
				hen.setBroody(false, null);
				hen.becomeMother(now + TimeScale.FOWL.realDaysToTicks(hen.breed().heatUntilDays));
			}
		}
	}

	/** Broody hens and mothers keep their species' young warm within reach. */
	private void warmYoung(ServerLevel level, List<FowlEntity> members, long now) {
		for (FowlEntity m : members) {
			if (!m.broody() && !m.mothering()) {
				continue;
			}
			for (FowlEntity y : level.getEntitiesOfClass(FowlEntity.class, m.getBoundingBox().inflate(MOTHER_REACH), b -> b.young() && b.species() == m.species())) {
				y.keepWarm(now + 40);
			}
		}
	}

	/** Once a game day: hens may go broody; ducks without their pond and birds on full litter lose a little health. */
	public void daily(ServerLevel level, List<FowlEntity> members) {
		int ducks = 0;
		for (FowlEntity f : members) {
			if (f.species() == Species.DUCK && !f.young()) {
				ducks++;
			}
		}
		boolean dry = pond < CoopLayout.pondWanted(ducks);
		boolean dirty = litterKg >= LITTER_ITEM_KG * LITTER_MAX_ITEMS - 1e-9;
		for (FowlEntity f : members) {
			if (dry && f.species() == Species.DUCK && !f.young()) {
				hurt(f, POND_LOSS);
			}
			if (dirty) {
				hurt(f, LITTER_LOSS);
			}
			if (f.sex() == Sex.FEMALE && f.laying() && !f.broody()) {
				for (BlockPos n : nests) {
					if (level.getBlockEntity(n) instanceof NestBoxEntity box) {
						double chance = Clutch.broodyChance(f.score(Trait.BROODINESS), box.count(f.species()));
						if (chance > 0 && level.getRandom().nextDouble() < chance) {
							BlockPos cell = sittingCell(n);
							if (cell != null) {
								f.setBroody(true, cell);
							}
							break;
						}
					}
				}
			}
		}
	}

	/** Takes a share of a bird's health bar, never killing it (a missed day never kills, the cattle rule). */
	static void hurt(FowlEntity f, double share) {
		f.setHealth((float) Math.max(1.0, f.getHealth() - share * f.getMaxHealth()));
	}

	/** Puts a laid egg in the nearest nest box of the hen's coop with room. */
	static boolean takeEgg(ServerLevel level, FowlEntity hen, ItemStack egg) {
		BlockPos c = hen.coop();
		if (c == null || !(level.getBlockEntity(c) instanceof CoopControllerEntity coop) || coop.check == null || !coop.check.formed()) {
			return false;
		}
		BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		for (BlockPos n : coop.nests) {
			if (level.getBlockEntity(n) instanceof NestBoxEntity box && !box.full()) {
				double d = n.distSqr(hen.blockPosition());
				if (d < bestD) {
					bestD = d;
					best = n;
				}
			}
		}
		return best != null && ((NestBoxEntity) level.getBlockEntity(best)).add(egg);
	}

	public String readout() {
		if (check == null) {
			return "Coop: not checked yet";
		}
		if (!check.formed()) {
			return "Coop: " + check.problem();
		}
		List<FowlEntity> members = level instanceof ServerLevel sl ? members(sl) : List.of();
		Map<Species, Integer> adults = adults(members, false);
		Map<Species, Integer> hens = adults(members.stream().filter(f -> f.sex() == Sex.FEMALE).toList(), false);
		StringBuilder b = new StringBuilder("Coop: FORMED");
		b.append(String.format(Locale.ROOT, ", floor %d cells (%.1f used)", check.floor().size(), CoopLayout.floorNeeded(adults)));
		b.append(String.format(Locale.ROOT, ", %d nest box%s", nests.size(), nests.size() == 1 ? "" : "es"));
		int wantN = CoopLayout.nestsWanted(hens);
		if (nests.size() < wantN) {
			b.append(" (").append(wantN).append(" wanted)");
		}
		int wantP = CoopLayout.perchesWanted(adults);
		b.append(String.format(Locale.ROOT, ", %d perch%s", perches.size(), perches.size() == 1 ? "" : "es"));
		if (perches.size() < wantP) {
			b.append(" (").append(wantP).append(" wanted; the rest sleep on the floor)");
		}
		double grain = 0;
		for (BlockPos f : feeders) {
			if (level != null && level.getBlockEntity(f) instanceof GrainFeederEntity g) {
				grain += g.kg();
			}
		}
		double water = 0;
		for (BlockPos t : troughs) {
			if (level != null && level.getBlockEntity(t) instanceof TroughEntity tr) {
				water += tr.litres();
			}
		}
		b.append(String.format(Locale.ROOT, ", grain %.1f kg, water %.0f L", grain, water));
		if (check.popHoles().isEmpty()) {
			b.append(", no pop hole (no run)");
		} else if (run == null) {
			b.append(", run: ").append(runProblem);
		} else {
			b.append(String.format(Locale.ROOT, ", run %d blocks (%.0f needed)", run.spots().size(), CoopLayout.runNeeded(adults)));
		}
		int ducks = adults.getOrDefault(Species.DUCK, 0);
		if (ducks > 0) {
			int want = CoopLayout.pondWanted(ducks);
			b.append(String.format(Locale.ROOT, ", pond %d water block%s%s", pond, pond == 1 ? "" : "s", pond < want ? " (" + want + " wanted: DUCKS DRY)" : ""));
		}
		b.append(String.format(Locale.ROOT, ", litter %.0f kg%s", litterKg, litterKg >= LITTER_ITEM_KG * LITTER_MAX_ITEMS - 1e-9 ? " (FULL)" : ""));
		long broody = members.stream().filter(FowlEntity::broody).count();
		b.append(", flock ").append(flock);
		if (broody > 0) {
			b.append(", ").append(broody).append(" broody");
		}
		b.append(out ? ", out on the run" : ", in");
		return b.toString();
	}

	// ---- a hopper below pulls the litter out ----

	private int litterItems() {
		return (int) Math.min(LITTER_MAX_ITEMS, Math.floor(litterKg / LITTER_ITEM_KG + 1e-9));
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return litterItems() == 0;
	}

	@Override
	public ItemStack getItem(int slot) {
		int n = litterItems();
		return n == 0 ? ItemStack.EMPTY : new ItemStack(FarmingsItems.POULTRY_LITTER, n);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		int n = Math.min(count, litterItems());
		if (n <= 0) {
			return ItemStack.EMPTY;
		}
		litterKg -= n * LITTER_ITEM_KG;
		setChanged();
		return new ItemStack(FarmingsItems.POULTRY_LITTER, n);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return removeItem(slot, LITTER_MAX_ITEMS);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		// the hopper hands back what it took less what it moved: keep the litter in step
		litterKg = Math.min(litterKg % LITTER_ITEM_KG + stack.getCount() * LITTER_ITEM_KG, LITTER_ITEM_KG * LITTER_MAX_ITEMS);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putDouble("litter_kg", litterKg);
		out.putDouble("run_drawn_kg", runDrawnKg);
		out.putInt("hatched", hatched);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		litterKg = in.getDoubleOr("litter_kg", 0);
		runDrawnKg = in.getDoubleOr("run_drawn_kg", 0);
		hatched = in.getIntOr("hatched", 0);
	}
}
