package dev.farmings.gantry;

import dev.farmings.conveyor.ConveyorDriveEntity;
import dev.farmings.conveyor.ConveyorPieceBlock;
import dev.farmings.conveyor.ItemSink;
import dev.farmings.crops.CornCropBlock;
import dev.farmings.crops.FieldCropBlock;
import dev.farmings.crops.FieldCrops;
import dev.farmings.crops.PerennialCropBlock;
import dev.farmings.farm.crops.FieldCrop;
import dev.farmings.farm.gantry.GantryTool;
import dev.farmings.husbandry.TimeScale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The field gantry's cab (docs/multiblocks/field-gantry.md): the controller. It finds its gantry (the beam under it, an end truck under
 * each end of the beam, a field rail under each truck), runs the program (work every row to the far end, unload at the headland stop,
 * come home), works the row under the beam with its tool, and steps one block along the rails at the tool's speed by moving its own
 * blocks, carrying its state with it. Power comes from the field junction at the rails' home end.
 */
public class GantryCabEntity extends BlockEntity implements WorldlyContainer {
	public enum Phase { PARKED, WORKING, TO_STOP, UNLOADING, RETURNING }

	private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
	private final NonNullList<ItemStack> inputs = NonNullList.withSize(9, ItemStack.EMPTY);
	private @Nullable GantryTool tool;
	private Phase phase = Phase.PARKED;
	private Direction forward = Direction.NORTH;
	private int step;
	private int target;
	private int resume = -1;
	private int cooldown;
	private @Nullable BlockPos junction;
	private final Map<String, Double> bin = new LinkedHashMap<>();
	private final List<ItemStack> extras = new ArrayList<>();
	/** Real days each mown or combined row has dried, by row (steps from home). */
	private final Map<Integer, Double> drying = new LinkedHashMap<>();
	private String fault = "";
	private int worked;
	private boolean forcedPower;

	public GantryCabEntity(BlockPos pos, BlockState state) {
		super(GantryBlocks.CAB_ENTITY, pos, state);
	}

	// ---- the structure ----

	/** The gantry's shape around the cab, or null with {@link #fault} set. */
	public record Frame(BlockPos e0, BlockPos e1, Direction across, int span) {
		BlockPos rail0() {
			return e0.below(2);
		}

		BlockPos rail1() {
			return e1.below(2);
		}

		List<BlockPos> row() {
			List<BlockPos> l = new ArrayList<>();
			for (int i = 1; i <= span; i++) {
				l.add(rail0().relative(across, i));
			}
			return l;
		}
	}

	private static boolean is(BlockState s, GantryPartBlock.Part part) {
		return s.getBlock() instanceof GantryPartBlock g && g.part == part;
	}

	public @Nullable Frame frame() {
		if (level == null) {
			return null;
		}
		BlockPos e0 = worldPosition.below();
		BlockState b0 = level.getBlockState(e0);
		if (!is(b0, GantryPartBlock.Part.BEAM)) {
			fault = "NO BEAM under the cab";
			return null;
		}
		Direction.Axis axis = b0.getValue(GantryPartBlock.AXIS);
		Direction across = null;
		BlockPos e1 = null;
		for (Direction d : new Direction[] {Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE),
				Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE)}) {
			BlockPos p = e0;
			int n = 0;
			while (n < GantryTool.MAX_SPAN + 2 && is(level.getBlockState(p.relative(d)), GantryPartBlock.Part.BEAM)) {
				p = p.relative(d);
				n++;
			}
			if (n > 0) {
				across = d;
				e1 = p;
				break;
			}
		}
		if (across == null) {
			fault = "the BEAM must run from the cab across the field";
			return null;
		}
		int span = e0.distManhattan(e1) - 1;
		if (span < GantryTool.MIN_SPAN || span > GantryTool.MAX_SPAN) {
			fault = "SPAN " + span + ": the beam must cross " + GantryTool.MIN_SPAN + " to " + GantryTool.MAX_SPAN + " blocks between its trucks";
			return null;
		}
		if (!is(level.getBlockState(e0.below()), GantryPartBlock.Part.TRUCK) || !is(level.getBlockState(e1.below()), GantryPartBlock.Part.TRUCK)) {
			fault = "NO TRUCK under each end of the beam";
			return null;
		}
		if (!is(level.getBlockState(e0.below(2)), GantryPartBlock.Part.RAIL) || !is(level.getBlockState(e1.below(2)), GantryPartBlock.Part.RAIL)) {
			fault = "the trucks must stand on FIELD RAIL";
			return null;
		}
		fault = "";
		return new Frame(e0.immutable(), e1.immutable(), across, span);
	}

	private boolean railAt(Frame f, Direction d) {
		return is(level.getBlockState(f.rail0().relative(d)), GantryPartBlock.Part.RAIL) && is(level.getBlockState(f.rail1().relative(d)), GantryPartBlock.Part.RAIL);
	}

	// ---- inputs, tool and program ----

	public @Nullable GantryTool tool() {
		return tool;
	}

	public Phase phase() {
		return phase;
	}

	public int step() {
		return step;
	}

	public String fault() {
		return fault;
	}

	public Map<String, Double> bin() {
		return bin;
	}

	public void forcePowerForTest(boolean on) {
		forcedPower = on;
	}

	/** Swaps the tool head while parked; returns the old head's item, or empty. */
	public ItemStack fitTool(@Nullable GantryTool t) {
		if (phase != Phase.PARKED) {
			return ItemStack.EMPTY;
		}
		ItemStack old = tool == null ? ItemStack.EMPTY : new ItemStack(GantryBlocks.toolItem(tool));
		tool = t;
		Frame f = frame();
		if (f != null) {
			for (BlockPos p : beamCells(f)) {
				BlockState s = level.getBlockState(p);
				level.setBlock(p, s.setValue(GantryPartBlock.TOOL, GantryPartBlock.Tool.of(t)), 3);
			}
		}
		setChanged();
		return old;
	}

	private List<BlockPos> beamCells(Frame f) {
		List<BlockPos> l = new ArrayList<>();
		for (int i = 0; i <= f.span() + 1; i++) {
			l.add(f.e0().relative(f.across(), i));
		}
		return l;
	}

	/** Starts the program from where it stands (its home row), or stops it. */
	public void toggle() {
		if (phase != Phase.PARKED) {
			phase = Phase.PARKED;
			setChanged();
			return;
		}
		Frame f = frame();
		if (f == null || tool == null) {
			if (tool == null) {
				fault = "NO TOOL: sneak-right-click the cab with a tool head";
			}
			return;
		}
		// forward: the way the rails run on from here (the longer way if both)
		Direction best = null;
		int bestLen = 0;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (d.getAxis() == f.across().getAxis()) {
				continue;
			}
			int n = 0;
			BlockPos a = f.rail0();
			BlockPos b = f.rail1();
			while (n < GantryTool.MAX_RAIL && is(level.getBlockState(a.relative(d)), GantryPartBlock.Part.RAIL)
					&& is(level.getBlockState(b.relative(d)), GantryPartBlock.Part.RAIL)) {
				a = a.relative(d);
				b = b.relative(d);
				n++;
			}
			if (n > bestLen) {
				bestLen = n;
				best = d;
			}
		}
		if (best == null) {
			fault = "NO RAIL ahead of the gantry";
			return;
		}
		forward = best;
		step = 0;
		resume = -1;
		phase = Phase.WORKING;
		junction = findJunction(f);
		fault = "";
		setChanged();
	}

	private @Nullable BlockPos findJunction(Frame f) {
		for (BlockPos p : BlockPos.betweenClosed(f.rail0().offset(-2, -1, -2), f.rail0().offset(2, 2, 2))) {
			if (level.getBlockState(p).getBlock() == GantryBlocks.FIELD_JUNCTION) {
				return p.immutable();
			}
		}
		return null;
	}

	private boolean powered(Frame f) {
		if (forcedPower) {
			return true;
		}
		if (junction == null || !(level.getBlockEntity(junction) instanceof FieldJunctionEntity j)) {
			fault = "NO JUNCTION: set a field cable junction beside the home end of a rail";
			return false;
		}
		double realKg = tool == GantryTool.HARVESTER ? FieldCrop.CORN.choppedKg() / 10 : tool == GantryTool.BALER ? FieldCrop.ALFALFA.hayKg() / 10 : 0;
		j.demand(tool == null ? GantryTool.TRUCK_WATTS : tool.watts(f.span(), realKg), phase != Phase.PARKED);
		if (!j.powered()) {
			fault = "NO POWER";
			return false;
		}
		return true;
	}

	// ---- the clock ----

	public void serverTick(ServerLevel level) {
		if (!level.isRaining()) {
			double day = TimeScale.ticksToRealDays(1);
			drying.replaceAll((k, v) -> v + day);
		}
		if (phase == Phase.PARKED) {
			return;
		}
		Frame f = frame();
		if (f == null) {
			phase = Phase.PARKED;
			return;
		}
		if (!powered(f)) {
			return;
		}
		if (--cooldown > 0) {
			return;
		}
		cooldown = tool == null ? 9 : tool.ticksPerBlock();
		runStep(level, f);
	}

	/** One step of the program: for game tests, a whole step at once. */
	public void runStep(ServerLevel level, Frame f) {
		switch (phase) {
			case WORKING -> {
				work(level, f);
				if (binKg() >= GantryTool.BIN_KG) {
					resume = step;
					phase = Phase.TO_STOP;
				} else if (!move(level, f, forward)) {
					phase = Phase.UNLOADING;
				}
			}
			case TO_STOP -> {
				if (!move(level, f, forward)) {
					phase = Phase.UNLOADING;
				}
			}
			case UNLOADING -> {
				if (unload(level, f)) {
					target = resume >= 0 ? resume : 0;
					phase = Phase.RETURNING;
				}
			}
			case RETURNING -> {
				if (step == target) {
					phase = resume >= 0 ? Phase.WORKING : Phase.PARKED;
					resume = -1;
				} else {
					move(level, f, forward.getOpposite());
				}
			}
			default -> {
			}
		}
		setChanged();
	}

	/** For game tests: run the program until it parks or {@code max} steps. */
	public void runForTest(ServerLevel level, int max) {
		GantryCabEntity cab = this;
		for (int i = 0; i < max && cab.phase != Phase.PARKED; i++) {
			Frame f = cab.frame();
			if (f == null) {
				return;
			}
			cab.runStep(level, f);
			if (cab.movedTo != null && level.getBlockEntity(cab.movedTo) instanceof GantryCabEntity next && next != cab) {
				cab = next;
			}
		}
	}

	private @Nullable BlockPos movedTo;

	/** For game tests: the cab where it now stands after moves. */
	public @Nullable BlockPos movedTo() {
		return movedTo;
	}

	/** For game tests: rows dry this many more real days. */
	public void dryForTest(double days) {
		drying.replaceAll((k, v) -> v + days);
	}

	// ---- moving ----

	/** Moves the whole gantry one block along its rails, if the rails go on and the way is clear. */
	private boolean move(ServerLevel level, Frame f, Direction d) {
		if (!railAt(f, d)) {
			return false;
		}
		List<BlockPos> beam = beamCells(f);
		List<BlockPos> from = new ArrayList<>();
		from.add(f.e0().below());
		from.add(f.e1().below());
		from.addAll(beam);
		from.add(worldPosition);
		for (BlockPos p : from) {
			BlockState t = level.getBlockState(p.relative(d));
			if (!t.isAir() && !t.canBeReplaced()) {
				fault = "BLOCKED at " + p.relative(d).toShortString();
				return false;
			}
		}
		step += d == forward ? 1 : -1;
		movedTo = worldPosition.relative(d);
		var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		saveCustomOnly(out);
		var data = out.buildResult();
		List<BlockState> states = new ArrayList<>();
		for (BlockPos p : from) {
			states.add(level.getBlockState(p));
		}
		for (int i = 0; i < from.size(); i++) {
			level.setBlock(from.get(i).relative(d), states.get(i), 2 | 16);
		}
		for (BlockPos p : from) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
		}
		if (level.getBlockEntity(movedTo) instanceof GantryCabEntity next) {
			next.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), data));
			next.movedTo = null;
			next.setChanged();
		}
		return true;
	}

	// ---- the tools ----

	private double binKg() {
		double kg = extras.size();
		for (double v : bin.values()) {
			kg += v;
		}
		return kg;
	}

	private void add(String product, double kg) {
		bin.merge(product, kg, Double::sum);
	}

	private void work(ServerLevel level, Frame f) {
		if (tool == null) {
			return;
		}
		double dried = drying.getOrDefault(step, -1.0);
		boolean rowHasSwath = false;
		for (BlockPos p : f.row()) {
			BlockState s = level.getBlockState(p);
			switch (tool) {
				case SEED_DRILL -> drill(level, p, s);
				case HARVESTER -> harvest(level, p, s);
				case MOWER -> {
					if (s.getBlock() instanceof PerennialCropBlock pc && pc.isMaxAge(s) && s.getValue(PerennialCropBlock.SWATH) == 0) {
						pc.cut(level, p);
						drying.put(step, 0.0);
						worked++;
					}
				}
				case RAKE -> {
					if (dried >= GantryTool.RAKE_AFTER_DAYS) {
						if (s.getBlock() instanceof PerennialCropBlock && s.getValue(PerennialCropBlock.SWATH) == 1) {
							level.setBlock(p, s.setValue(PerennialCropBlock.SWATH, 2), 3);
							worked++;
						} else if (s.getBlock() instanceof SwathBlock && !s.getValue(SwathBlock.RAKED)) {
							level.setBlock(p, s.setValue(SwathBlock.RAKED, true), 3);
							worked++;
						}
					}
				}
				case BALER -> {
					if (dried >= GantryTool.BALE_AFTER_DAYS) {
						if (s.getBlock() instanceof PerennialCropBlock pc && s.getValue(PerennialCropBlock.SWATH) == 2) {
							level.setBlock(p, s.setValue(PerennialCropBlock.SWATH, 0), 3);
							add(pc.crop() == FieldCrop.ALFALFA ? "alfalfa_hay" : "grass_hay", pc.crop().hayKg());
							worked++;
						} else if (s.getBlock() instanceof SwathBlock && s.getValue(SwathBlock.RAKED)) {
							level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
							add("straw", s.getValue(SwathBlock.KIND) == SwathBlock.Kind.OAT_STRAW ? FieldCrop.OATS.strawKg() : FieldCrop.BARLEY.strawKg());
							worked++;
						}
					}
				}
				case SPREADER -> spread(level, p, s);
			}
			BlockState now = level.getBlockState(p);
			if (now.getBlock() instanceof PerennialCropBlock && now.getValue(PerennialCropBlock.SWATH) > 0 || now.getBlock() instanceof SwathBlock) {
				rowHasSwath = true;
			}
		}
		if (!rowHasSwath) {
			drying.remove(step);
		}
	}

	private void drill(ServerLevel level, BlockPos p, BlockState s) {
		if (!s.isAir() && !s.canBeReplaced()) {
			return;
		}
		BlockState under = level.getBlockState(p.below());
		if (!under.is(Blocks.DIRT) && !under.is(Blocks.GRASS_BLOCK) && !under.is(Blocks.FARMLAND) && !under.is(Blocks.COARSE_DIRT)) {
			return;
		}
		for (int i = 0; i < inputs.size(); i++) {
			ItemStack st = inputs.get(i);
			if (st.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock crop) {
				level.setBlock(p.below(), Blocks.FARMLAND.defaultBlockState(), 3);
				level.setBlock(p, crop.defaultBlockState(), 3);
				st.shrink(1);
				worked++;
				return;
			}
		}
		fault = "NO SEED in the cab";
	}

	private void harvest(ServerLevel level, BlockPos p, BlockState s) {
		Block b = s.getBlock();
		if (b instanceof CornCropBlock) {
			if (CornCropBlock.grainReady(s)) {
				add("corn_grain", FieldCrop.CORN_GRAIN_KG);
			} else if (CornCropBlock.silageReady(s)) {
				add("chopped_corn", FieldCrop.CORN.choppedKg());
			} else {
				return;
			}
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
			worked++;
		} else if (b instanceof PerennialCropBlock pc) {
			// wilted alfalfa (a swath not yet raked) is chopped for haylage (crops.md)
			if (pc.crop() == FieldCrop.ALFALFA && s.getValue(PerennialCropBlock.SWATH) == 1) {
				add("chopped_alfalfa", pc.crop().yieldKg / 0.375);
				level.setBlock(p, s.setValue(PerennialCropBlock.SWATH, 0), 3);
				worked++;
			}
		} else if (b instanceof FieldCropBlock fc && fc.isMaxAge(s)) {
			add(fc.crop() == FieldCrop.OATS ? "oat_grain" : "barley_grain", fc.crop().yieldKg);
			level.setBlock(p, GantryBlocks.SWATH.defaultBlockState().setValue(SwathBlock.KIND,
					fc.crop() == FieldCrop.OATS ? SwathBlock.Kind.OAT_STRAW : SwathBlock.Kind.BARLEY_STRAW), 3);
			drying.putIfAbsent(step, 0.0);
			worked++;
		} else if (b instanceof CropBlock vanilla && vanilla.isMaxAge(s)) {
			extras.addAll(Block.getDrops(s, level, p, null));
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
			worked++;
		}
	}

	private void spread(ServerLevel level, BlockPos p, BlockState s) {
		if (!(s.getBlock() instanceof BonemealableBlock bm) || !bm.isValidBonemealTarget(level, p, s, BonemealSource.INTERACTION)) {
			return;
		}
		for (ItemStack st : inputs) {
			if (Fertilizers.is(st)) {
				bm.performBonemeal(level, level.getRandom(), p, s, BonemealSource.INTERACTION);
				st.shrink(1);
				worked++;
				return;
			}
		}
		fault = "NO FERTILIZER in the cab";
	}

	// ---- unloading at the headland stop ----

	/** Turns the bin into items: bales of hay and straw, 1 kg grain, 25 kg chopped forage (crops.md). Remainders stay. */
	private List<ItemStack> takeItems() {
		List<ItemStack> out = new ArrayList<>(extras);
		extras.clear();
		for (Map.Entry<String, Double> e : bin.entrySet()) {
			String k = e.getKey();
			double kg = e.getValue();
			double each;
			Item item;
			switch (k) {
				case "alfalfa_hay" -> { each = GantryTool.BALE_KG; item = FieldCrops.ALFALFA_BALE; }
				case "grass_hay" -> { each = GantryTool.BALE_KG; item = FieldCrops.GRASS_BALE; }
				case "straw" -> { each = GantryTool.BALE_KG; item = FieldCrops.STRAW_BALE; }
				case "corn_grain" -> { each = 1; item = FieldCrops.CORN_GRAIN; }
				case "oat_grain" -> { each = 1; item = FieldCrops.OAT_GRAIN; }
				case "barley_grain" -> { each = 1; item = FieldCrops.BARLEY_GRAIN; }
				case "chopped_corn" -> { each = 25; item = FieldCrops.CHOPPED_CORN; }
				default -> { each = 25; item = FieldCrops.CHOPPED_ALFALFA; }
			}
			int n = (int) Math.floor(kg / each + 1e-9);
			e.setValue(kg - n * each);
			while (n > 0) {
				int c = Math.min(n, new ItemStack(item).getMaxStackSize());
				out.add(new ItemStack(item, c));
				n -= c;
			}
		}
		return out;
	}

	/** Unloads into whatever touches the headland stop. True when everything whole is out. */
	private boolean unload(ServerLevel level, Frame f) {
		BlockPos stop = f.rail0().relative(forward);
		if (level.getBlockState(stop).getBlock() != GantryBlocks.HEADLAND_STOP) {
			stop = f.rail1().relative(forward);
		}
		if (level.getBlockState(stop).getBlock() != GantryBlocks.HEADLAND_STOP) {
			fault = "NO HEADLAND STOP at the end of the rails";
			return false;
		}
		List<ItemStack> items = takeItems();
		List<ItemStack> left = new ArrayList<>();
		for (ItemStack st : items) {
			ItemStack rest = st;
			for (Direction d : Direction.values()) {
				if (rest.isEmpty()) {
					break;
				}
				rest = deliver(level, stop.relative(d), d.getOpposite(), rest);
			}
			if (!rest.isEmpty()) {
				left.add(rest);
			}
		}
		extras.addAll(left);
		if (!left.isEmpty()) {
			fault = "STOP FULL: nothing at the headland stop can take the load";
			return false;
		}
		fault = "";
		return true;
	}

	private static ItemStack deliver(ServerLevel level, BlockPos at, Direction face, ItemStack stack) {
		if (level.getBlockState(at).getBlock() instanceof ConveyorPieceBlock) {
			ConveyorDriveEntity d = ConveyorDriveEntity.driveOf(level, at);
			if (d != null && d.kind().carries(stack) && d.hasRoom()) {
				d.putOn(stack.copy(), at);
				return ItemStack.EMPTY;
			}
			return stack;
		}
		BlockEntity be = level.getBlockEntity(at);
		if (be instanceof ItemSink sink) {
			return sink.accept(stack);
		}
		if (be instanceof Container c && !(be instanceof GantryCabEntity)) {
			return HopperBlockEntity.addItem(null, c, stack, face);
		}
		return stack;
	}

	public String readout() {
		StringBuilder b = new StringBuilder("Field gantry: ");
		Frame f = frame();
		if (f == null) {
			return b.append(fault).toString();
		}
		b.append(String.format(Locale.ROOT, "span %d, tool %s, %s, row %d", f.span(), tool == null ? "none" : tool.label, phase.name().toLowerCase(Locale.ROOT), step));
		if (tool != null) {
			double realKg = tool == GantryTool.HARVESTER ? FieldCrop.CORN.choppedKg() / 10 : tool == GantryTool.BALER ? FieldCrop.ALFALFA.hayKg() / 10 : 0;
			b.append(String.format(Locale.ROOT, ", %.0f kW at %.0f km/h", tool.watts(f.span(), realKg) / 1000, tool.kmPerHour));
		}
		b.append(String.format(Locale.ROOT, ", bin %.0f of %.0f kg", binKg(), GantryTool.BIN_KG));
		if (!drying.isEmpty()) {
			double most = drying.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
			b.append(String.format(Locale.ROOT, ", %d row%s drying (longest %.1f days)", drying.size(), drying.size() == 1 ? "" : "s", most));
		}
		if (!fault.isEmpty()) {
			b.append(", ").append(fault);
		}
		return b.toString();
	}

	// ---- inputs: seed and fertilizer, by hand or hopper ----

	public static boolean acceptsInput(ItemStack s) {
		return s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock || Fertilizers.is(s);
	}

	public ItemStack addInput(ItemStack stack) {
		ItemStack rest = stack.copy();
		for (int i = 0; i < inputs.size() && !rest.isEmpty(); i++) {
			ItemStack in = inputs.get(i);
			if (in.isEmpty()) {
				inputs.set(i, rest.copy());
				rest = ItemStack.EMPTY;
			} else if (ItemStack.isSameItemSameComponents(in, rest) && in.getCount() < in.getMaxStackSize()) {
				int n = Math.min(rest.getCount(), in.getMaxStackSize() - in.getCount());
				in.grow(n);
				rest.shrink(n);
			}
		}
		setChanged();
		return rest;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return acceptsInput(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public int getContainerSize() {
		return inputs.size();
	}

	@Override
	public boolean isEmpty() {
		return inputs.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return inputs.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		return ContainerHelper.removeItem(inputs, slot, count);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(inputs, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		inputs.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		inputs.clear();
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		ContainerHelper.saveAllItems(out, inputs);
		out.putString("Tool", tool == null ? "" : tool.name());
		out.putString("Phase", phase.name());
		out.putString("Forward", forward.getName());
		out.putInt("Step", step);
		out.putInt("Target", target);
		out.putInt("Resume", resume);
		out.putLong("Junction", junction == null ? Long.MIN_VALUE : junction.asLong());
		out.putBoolean("ForcedPower", forcedPower);
		var bins = out.childrenList("Bin");
		bin.forEach((k, v) -> {
			var o = bins.addChild();
			o.putString("K", k);
			o.putDouble("Kg", v);
		});
		var dr = out.childrenList("Drying");
		drying.forEach((k, v) -> {
			var o = dr.addChild();
			o.putInt("Row", k);
			o.putDouble("Days", v);
		});
		var ex = out.list("Extras", ItemStack.CODEC);
		extras.forEach(ex::add);
		out.putInt("Worked", worked);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		inputs.clear();
		ContainerHelper.loadAllItems(in, inputs);
		String t = in.getStringOr("Tool", "");
		tool = t.isEmpty() ? null : GantryTool.valueOf(t);
		phase = Phase.valueOf(in.getStringOr("Phase", Phase.PARKED.name()));
		Direction fw = Direction.byName(in.getStringOr("Forward", "north"));
		forward = fw == null ? Direction.NORTH : fw;
		step = in.getIntOr("Step", 0);
		target = in.getIntOr("Target", 0);
		resume = in.getIntOr("Resume", -1);
		long j = in.getLongOr("Junction", Long.MIN_VALUE);
		junction = j == Long.MIN_VALUE ? null : BlockPos.of(j);
		forcedPower = in.getBooleanOr("ForcedPower", false);
		bin.clear();
		for (ValueInput o : in.childrenListOrEmpty("Bin")) {
			bin.put(o.getStringOr("K", ""), o.getDoubleOr("Kg", 0));
		}
		drying.clear();
		for (ValueInput o : in.childrenListOrEmpty("Drying")) {
			drying.put(o.getIntOr("Row", 0), o.getDoubleOr("Days", 0));
		}
		extras.clear();
		in.listOrEmpty("Extras", ItemStack.CODEC).forEach(extras::add);
		worked = in.getIntOr("Worked", 0);
	}

	public int worked() {
		return worked;
	}
}
