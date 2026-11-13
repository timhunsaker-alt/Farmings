package dev.farmings.stable;

import dev.farmings.barn.BarnPartBlock;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import dev.farmings.entity.HorseEntity;
import dev.farmings.husbandry.Flock;
import dev.farmings.husbandry.Hygiene;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.shed.PastureLayout;
import dev.farmings.husbandry.shed.ShedBedding;
import dev.farmings.husbandry.shed.ShedLayout;
import dev.farmings.shed.ShedBlocks;
import dev.farmings.shed.ShedControllerEntity;
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
 * The stable (docs/multiblocks/stable.md). The layout is the sheep shed's check (`ShedLayout`) with the stable's cells: stall floor for the
 * pen, the barn's hay feeder as the hay rack and its trough, the shed's pasture gate as the paddock gate. Every 12 blocks of stall floor
 * is one stall. Once a second it takes in tame horses, gives each a stall (a foal stays with its dam), sends them to the paddock by day,
 * feeds and waters them, and beds them on straw that fills with their manure.
 */
public class StableControllerEntity extends BlockEntity implements net.minecraft.world.WorldlyContainer {
	/** A stable takes in tame, unstabled horses within this many blocks *(proposed, as the shed)*. */
	public static final double TAKE_IN_RADIUS = 48;
	/** A stall is 3 x 4 blocks of stall floor, 12 m² ("A 12-foot x 12-foot stall", Penn State, TO VERIFY: 13.4 m²). */
	public static final int STALL_BLOCKS = 12;
	private static final double SMOOTHING = 0.2;
	private static final long REPICK_TICKS = 600;
	private static final int[] SLOTS = {0, 1};

	private ShedLayout.@Nullable Check check;
	private PastureLayout.@Nullable Pasture paddock;
	private final Set<Long> paddockCells = new HashSet<>();
	private final List<BlockPos> racks = new ArrayList<>();
	private final List<BlockPos> troughs = new ArrayList<>();
	private final Map<UUID, Double> grazeCredit = new HashMap<>();
	private final Map<UUID, Long> lastPick = new HashMap<>();
	private final ShedBedding bedding = new ShedBedding();
	private boolean dirty;
	private int herd;
	private boolean out;
	private double grazedBlocks;
	private @Nullable Boolean forcedDay;

	public StableControllerEntity(BlockPos pos, BlockState state) {
		super(StableBlocks.STABLE_CONTROLLER_ENTITY, pos, state);
	}

	public ShedLayout.@Nullable Check check() {
		return check;
	}

	public PastureLayout.@Nullable Pasture paddock() {
		return paddock;
	}

	public void forceDayForTest(@Nullable Boolean day) {
		this.forcedDay = day;
	}

	public double grazedBlocks() {
		return grazedBlocks;
	}

	/** Stalls: every 12 blocks of stall floor. */
	public int stalls() {
		return check == null ? 0 : check.pen().size() / STALL_BLOCKS;
	}

	public boolean formed() {
		return check != null && check.formed() && stalls() > 0;
	}

	// ---- the layout ----

	private static ShedLayout.Cell cell(ServerLevel level, BlockPos p) {
		Block b = level.getBlockState(p).getBlock();
		if (b == StableBlocks.HORSE_STALL) {
			return ShedLayout.Cell.PEN;
		}
		if (b == ShedBlocks.PASTURE_GATE) {
			return ShedLayout.Cell.GATE;
		}
		if (b == StableBlocks.STABLE_CONTROLLER) {
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

	/** The shed check's words for a stable. */
	public String status() {
		if (check == null) {
			return "not checked yet";
		}
		if (check.formed() && stalls() == 0) {
			return "A stall needs " + STALL_BLOCKS + " blocks of stall floor (3 x 4), has " + check.pen().size();
		}
		return check.status().replace("sheep pen floor", "stall floor").replace("sheep pen cells (room for 2 sheep)", "stall floor blocks")
				.replace("the pen", "the stalls").replace("The pen", "The stalls");
	}

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
		paddock = null;
		paddockCells.clear();
		if (check.formed() && !check.gates().isEmpty()) {
			ShedLayout.Pos g = check.gates().get(0);
			paddock = PastureLayout.find(new PastureLayout.Ground() {
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
					return b == StableBlocks.HORSE_STALL || b == ShedBlocks.PASTURE_GATE || b == StableBlocks.STABLE_CONTROLLER;
				}
			}, g.x(), g.y(), g.z());
			if (paddock.found()) {
				for (PastureLayout.Spot s : paddock.spots()) {
					paddockCells.add(BlockPos.asLong(s.x(), s.y(), s.z()));
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
				&& g.isFaceSturdy(level, feet.below(), Direction.UP);
	}

	// ---- the horses ----

	public List<HorseEntity> members(ServerLevel level) {
		return level.getEntitiesOfClass(HorseEntity.class, new AABB(worldPosition).inflate(TAKE_IN_RADIUS), h -> worldPosition.equals(h.place().shedController()));
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (check == null || now % 100 == 0) {
			refresh(level);
		}
		if (now % 20 != 0 || !formed()) {
			return;
		}
		takeIn(level);
		List<HorseEntity> members = members(level);
		herd = members.size();
		homes(members);
		long day = Math.floorMod(level.getOverworldClockTime(), TimeScale.TICKS_PER_GAME_DAY);
		boolean fine = forcedDay != null ? forcedDay : day < 12_000 && !level.isRaining();
		out = paddock != null && paddock.found() && fine;
		toPaddock(level, members, now);
		tend(level, members, 20.0 / TimeScale.TICKS_PER_GAME_DAY);
	}

	private void takeIn(ServerLevel level) {
		int used = 0;
		for (HorseEntity h : members(level)) {
			used += h.stage().foal() ? 0 : 1;
		}
		for (HorseEntity h : level.getEntitiesOfClass(HorseEntity.class, new AABB(worldPosition).inflate(TAKE_IN_RADIUS),
				h -> h.place().shedController() == null && !h.isWild() && !h.isVehicle())) {
			int need = h.stage().foal() ? 0 : 1;
			if (used + need <= stalls()) {
				h.place().assignShed(worldPosition, null);
				used += need;
			}
		}
	}

	/** The home cell of stall {@code k}: the middle of its 12 blocks. */
	private BlockPos stallCell(int k) {
		List<ShedLayout.Pos> pen = check.pen();
		return pos(pen.get(Math.min(pen.size() - 1, k * STALL_BLOCKS + STALL_BLOCKS / 2)));
	}

	/** One stall a horse; a foal goes where its dam is. */
	private void homes(List<HorseEntity> members) {
		Set<BlockPos> taken = new HashSet<>();
		List<BlockPos> stallCells = new ArrayList<>();
		for (int k = 0; k < stalls(); k++) {
			stallCells.add(stallCell(k));
		}
		for (HorseEntity h : members) {
			BlockPos home = h.place().home();
			if (!h.stage().foal() && home != null && stallCells.contains(home) && !taken.contains(home)) {
				taken.add(home);
			}
		}
		Map<String, HorseEntity> dams = new HashMap<>();
		for (HorseEntity h : members) {
			if (h.stage().foal()) {
				continue;
			}
			dams.put(h.herdId(), h);
			BlockPos home = h.place().home();
			if (home == null || !stallCells.contains(home) || !taken.contains(home)) {
				for (BlockPos c : stallCells) {
					if (taken.add(c)) {
						h.place().assignShed(worldPosition, c);
						break;
					}
				}
			}
		}
		for (HorseEntity foal : members) {
			if (foal.stage().foal()) {
				HorseEntity dam = dams.get(foal.damId());
				foal.place().assignShed(worldPosition, dam != null && dam.place().home() != null ? dam.place().home() : stallCells.get(0));
			}
		}
	}

	private void toPaddock(ServerLevel level, List<HorseEntity> members, long now) {
		List<PastureLayout.Spot> spots = paddock != null && paddock.found() ? paddock.spots() : List.of();
		for (HorseEntity h : members) {
			if (!out || spots.isEmpty() || h.isVehicle()) {
				if (h.place().sentTo() != null) {
					h.place().sendTo(null);
				}
				continue;
			}
			Long last = lastPick.get(h.getUUID());
			if (h.place().sentTo() == null || last == null || now - last >= REPICK_TICKS) {
				PastureLayout.Spot s = spots.get(level.getRandom().nextInt(spots.size()));
				h.place().sendTo(new BlockPos(s.x(), s.y(), s.z()));
				lastPick.put(h.getUUID(), now);
			}
		}
	}

	private boolean onPaddock(HorseEntity h) {
		BlockPos b = h.blockPosition();
		return paddockCells.contains(b.asLong()) || paddockCells.contains(b.above().asLong()) || paddockCells.contains(b.below().asLong());
	}

	/**
	 * One step of {@code dayShare} of a game day, each horse taking that share of one real day's intake (the shed's rule): grazing in the
	 * paddock, then hay from the racks; water from the troughs; manure into the
	 * bedding of horses indoors; the dirty-bedding health cost. Foals nurse until weaning; a ridden horse is out working and is skipped.
	 */
	public void tend(ServerLevel level, List<HorseEntity> members, double dayShare) {
		int housed = 0;
		for (HorseEntity h : members) {
			if (h.isVehicle() || h.stage().foal()) {
				continue;
			}
			double need = h.dailyDryMatterKg() * dayShare;
			double got = 0;
			boolean grazing = onPaddock(h);
			if (grazing) {
				got += graze(level, h, need);
			}
			for (BlockPos r : racks) {
				if (got >= need - 1e-12) {
					break;
				}
				if (level.getBlockEntity(r) instanceof FeederEntity fe) {
					got += fe.take(need - got);
				}
			}
			double water = h.dailyWaterL() * dayShare;
			double drunk = 0;
			for (BlockPos t : troughs) {
				if (drunk >= water) {
					break;
				}
				if (level.getBlockEntity(t) instanceof TroughEntity tr) {
					drunk += tr.take(water - drunk);
				}
			}
			double fed = h.lastFed() + SMOOTHING * (Math.min(1, need <= 0 ? 1 : got / need) - h.lastFed());
			double watered = h.lastWatered() + SMOOTHING * (Math.min(1, water <= 0 ? 1 : drunk / water) - h.lastWatered());
			h.recordTending(fed, watered);
			if (!grazing) {
				bedding.housedKg(h.manureKgPerDay(), dayShare);
				housed++;
			}
		}
		dirty = bedding.dirty(housed > 0);
		if (dirty) {
			for (HorseEntity h : members) {
				if (!h.isVehicle() && h.getHealth() > Hygiene.HEALTH_FLOOR) {
					h.setHealth((float) Math.max(Hygiene.HEALTH_FLOOR, h.getHealth() - Hygiene.HEALTH_LOSS_PER_GAME_DAY * dayShare));
				}
			}
		}
		setChanged();
	}

	/** Grazing: 1.5 kg DM a grass block near the horse, which is grazed bare to dirt (stable.md, as sheep). Returns kg DM got. */
	private double graze(ServerLevel level, HorseEntity h, double need) {
		double credit = grazeCredit.getOrDefault(h.getUUID(), 0.0);
		while (credit < need) {
			BlockPos grass = nearestGrass(level, h.blockPosition());
			if (grass == null) {
				break;
			}
			level.setBlockAndUpdate(grass, Blocks.DIRT.defaultBlockState());
			grazedBlocks++;
			credit += Flock.GRASS_BLOCK_KG_DM;
		}
		double got = Math.min(credit, need);
		grazeCredit.put(h.getUUID(), credit - got);
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
						if (paddockCells.contains(feet.asLong()) && level.getBlockState(feet.below()).is(Blocks.GRASS_BLOCK)) {
							return feet.below();
						}
					}
				}
			}
		}
		return null;
	}

	public String readout(ServerLevel level) {
		if (check == null) {
			return "Stable: not checked yet";
		}
		if (!formed()) {
			return "Stable: " + status();
		}
		StringBuilder b = new StringBuilder("Stable: FORMED");
		b.append(String.format(Locale.ROOT, ", %d stall%s, %d hay rack%s, %d trough%s", stalls(), stalls() == 1 ? "" : "s", racks.size(),
				racks.size() == 1 ? "" : "s", troughs.size(), troughs.size() == 1 ? "" : "s"));
		if (paddock != null && paddock.found()) {
			b.append(String.format(Locale.ROOT, ", paddock %d blocks (%d grass)", paddock.spots().size(), paddock.grass()));
		}
		b.append(String.format(Locale.ROOT, ", bedding: %.0f kg straw, %d of %d manure items, %s", bedding.strawKg(), bedding.manureItems(),
				ShedBedding.MAX_ITEMS, dirty ? "DIRTY" : "CLEAN"));
		b.append(", horses ").append(herd).append(out ? ", out in the paddock" : ", in");
		for (HorseEntity h : members(level)) {
			b.append(String.format(Locale.ROOT, "\n  %s %s %s: fitness %.0f%%, fed %.0f%%, watered %.0f%%", h.herdId(), h.breedLabel(), h.stage().label(),
					h.fitness() * 100, h.lastFed() * 100, h.lastWatered() * 100));
		}
		return b.toString();
	}

	// ---- bedding: straw in by hand or hopper, manure out by a hopper below (as the sheep shed) ----

	public ShedBedding bedding() {
		return bedding;
	}

	public boolean beddingDirty() {
		return dirty;
	}

	public boolean addStraw(ItemStack stack) {
		double kg = ShedControllerEntity.strawKg(stack);
		if (kg <= 0 || !bedding.hasRoomFor(kg)) {
			return false;
		}
		bedding.addStraw(kg);
		setChanged();
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == 1 && side != Direction.DOWN && ShedControllerEntity.strawKg(stack) > 0 && bedding.hasRoomFor(ShedControllerEntity.strawKg(stack));
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == 0 && side == Direction.DOWN;
	}

	@Override
	public int getContainerSize() {
		return 2;
	}

	@Override
	public boolean isEmpty() {
		return bedding.manureItems() == 0;
	}

	@Override
	public ItemStack getItem(int slot) {
		int n = bedding.manureItems();
		return slot == 0 && n > 0 ? new ItemStack(dev.farmings.FarmingsItems.MANURE, n) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		if (slot != 0) {
			return ItemStack.EMPTY;
		}
		int n = bedding.takeManure(count);
		setChanged();
		return n == 0 ? ItemStack.EMPTY : new ItemStack(dev.farmings.FarmingsItems.MANURE, n);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return removeItem(slot, ShedBedding.MAX_ITEMS);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot == 0) {
			bedding.setManureItems(stack.getCount());
		} else {
			for (int i = 0; i < stack.getCount(); i++) {
				addStraw(stack);
			}
		}
		setChanged();
	}

	@Override
	public boolean stillValid(net.minecraft.world.entity.player.Player player) {
		return true;
	}

	@Override
	public void clearContent() {
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putDouble("bedding_straw_kg", bedding.strawKg());
		out.putDouble("bedding_soiled_kg", bedding.soiledKg());
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		bedding.load(in.getDoubleOr("bedding_straw_kg", 0), in.getDoubleOr("bedding_soiled_kg", 0));
	}
}
