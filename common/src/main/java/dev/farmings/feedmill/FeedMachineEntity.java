package dev.farmings.feedmill;

import dev.farmings.conveyor.ItemSink;
import dev.farmings.crops.FieldCrops;
import dev.farmings.feed.FeedMix;
import dev.farmings.feed.Ration;
import dev.farmings.silo.Towers;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The hammer mill and the TMR mixer (docs/multiblocks/feed-mill.md), both Wildcatter 480 V loads that take items from a belt, auger,
 * hopper or hand and put their product out to a belt, a Farmings machine or a container beside them.
 * <ul>
 *   <li><b>Mill</b> (9.1 kW, 0.53 kg/s: Mississippi State's 4.8 kWh/t at 1.9 t/h): grain to meal 1 kg for 1 kg; loose alfalfa hay (10 kg)
 *       or an alfalfa bale (454 kg) to alfalfa meal; calcite to 16 kg of ground limestone (proposed); 4 cod or salmon to 1 kg of fish meal
 *       (proposed).</li>
 *   <li><b>Mixer</b> (56 kW, TO VERIFY): holds up to 2 t as fed (7.1 m3 x 80% at 350 kg/m3, proposed); one minute of real time (4 s of play
 *       at 15:1) after the last ingredient it mixes the batch into 25 kg ration items with the batch's analysis.</li>
 * </ul>
 */
public class FeedMachineEntity extends BlockEntity implements ItemSink, WorldlyContainer, CircuitPart, Inspectable {
	public static final double MILL_WATTS = 9100;
	public static final double MIXER_WATTS = 56_000;
	public static final int MILL_TICKS_PER_KG = (int) Math.round(20 / 0.53);
	public static final double MIXER_KG = 2000;
	public static final int MIX_AFTER_TICKS = 80;
	private static final int[] SLOTS = {0};

	private Motor motor;
	private boolean forced;
	/** Mill: meal waiting to come out, kg by item id; fish waiting for a whole kg. */
	private final Map<String, Integer> pending = new LinkedHashMap<>();
	private int fish;
	private int cooldown;
	/** Mixer: the batch, by dry matter, and its mass as fed. */
	private final FeedMix batch = new FeedMix();
	private double asFed;
	private long lastAdd = Long.MIN_VALUE;
	private int made;

	public FeedMachineEntity(BlockPos pos, BlockState state) {
		super(FeedMillBlocks.MACHINE_ENTITY, pos, state);
		motor = new Motor(true, Voltage.INDUSTRIAL, mixer() ? MIXER_WATTS : MILL_WATTS);
	}

	public boolean mixer() {
		return getBlockState().getBlock() == FeedMillBlocks.TMR_MIXER;
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	public int made() {
		return made;
	}

	public double asFed() {
		return asFed;
	}

	public Ration.Analysis batchAnalysis() {
		return batch.analysis();
	}

	// ---- in ----

	@Override
	public ItemStack accept(ItemStack stack) {
		if (stack.isEmpty() || level == null) {
			return stack;
		}
		return mixer() ? mixIn(stack) : millIn(stack);
	}

	private ItemStack millIn(ItemStack s) {
		Item out;
		int kgEach;
		if (s.is(FieldCrops.CORN_GRAIN)) {
			out = FeedItems.CORN_MEAL;
			kgEach = 1;
		} else if (s.is(FieldCrops.OAT_GRAIN)) {
			out = FeedItems.OAT_MEAL;
			kgEach = 1;
		} else if (s.is(FieldCrops.BARLEY_GRAIN)) {
			out = FeedItems.BARLEY_MEAL;
			kgEach = 1;
		} else if (s.is(FieldCrops.ALFALFA_HAY)) {
			out = FeedItems.ALFALFA_MEAL;
			kgEach = 10;
		} else if (s.is(FieldCrops.ALFALFA_BALE)) {
			out = FeedItems.ALFALFA_MEAL;
			kgEach = 454;
		} else if (s.is(Items.CALCITE)) {
			out = FeedItems.GROUND_LIMESTONE;
			kgEach = 16;
		} else if (s.is(Items.COD) || s.is(Items.SALMON)) {
			fish += s.getCount();
			int meal = fish / 4;
			fish %= 4;
			if (meal > 0) {
				pending.merge(id(FeedItems.FISH_MEAL), meal, Integer::sum);
			}
			setChanged();
			return ItemStack.EMPTY;
		} else {
			return s;
		}
		pending.merge(id(out), s.getCount() * kgEach, Integer::sum);
		setChanged();
		return ItemStack.EMPTY;
	}

	private ItemStack mixIn(ItemStack s) {
		FeedItems.Feed f = FeedItems.feed(s);
		if (f == null) {
			return s;
		}
		int n = (int) Math.min(s.getCount(), Math.floor((MIXER_KG - asFed) / f.kgAsFed() + 1e-9));
		if (n <= 0) {
			return s;
		}
		batch.add(n * f.kgDm(), f.analysis());
		asFed += n * f.kgAsFed();
		lastAdd = level.getGameTime();
		setChanged();
		return s.copyWithCount(s.getCount() - n);
	}

	private static String id(Item i) {
		return BuiltInRegistries.ITEM.getKey(i).toString();
	}

	// ---- work ----

	public void serverTick(ServerLevel level) {
		if (!powered()) {
			return;
		}
		if (mixer()) {
			if (asFed >= RationItem.KG - 1e-9 && level.getGameTime() - lastAdd >= MIX_AFTER_TICKS) {
				int n = (int) Math.min(16, Math.floor(asFed / RationItem.KG + 1e-9));
				double dm = batch.kg() / asFed;
				ItemStack out = RationItem.of(batch.analysis(), dm, n);
				ItemStack rest = Towers.handOn(level, worldPosition, out, p -> false);
				int moved = n - rest.getCount();
				if (moved > 0) {
					double share = moved * RationItem.KG / asFed;
					batch.take(batch.kg() * share);
					asFed -= moved * RationItem.KG;
					made += moved;
					setChanged();
				}
			}
			return;
		}
		if (pending.isEmpty() || --cooldown > 0) {
			return;
		}
		cooldown = MILL_TICKS_PER_KG;
		Map.Entry<String, Integer> e = pending.entrySet().iterator().next();
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(e.getKey()));
		ItemStack rest = Towers.handOn(level, worldPosition, new ItemStack(item), p -> false);
		if (rest.isEmpty()) {
			made++;
			if (e.getValue() <= 1) {
				pending.remove(e.getKey());
			} else {
				e.setValue(e.getValue() - 1);
			}
			setChanged();
		}
	}

	public String readout() {
		if (mixer()) {
			Ration.Analysis a = batch.analysis();
			return String.format(Locale.ROOT, "TMR mixer (56 kW): %s, batch %.0f of %.0f kg as fed%s, %d rations made", powered() ? "on" : "NO POWER", asFed, MIXER_KG,
					asFed > 0 ? String.format(Locale.ROOT, " (CP %.1f%%, NDF %.1f%%, TDN %.1f%%, Ca %.1f%%, forage %.0f%%)", a.cp(), a.ndf(), a.tdn(), a.ca(), a.forage() * 100) : "",
					made);
		}
		int waiting = pending.values().stream().mapToInt(Integer::intValue).sum();
		return String.format(Locale.ROOT, "Hammer mill (9.1 kW): %s, %d kg waiting, %d kg ground", powered() ? "on" : "NO POWER", waiting, made);
	}

	// ---- hoppers ----

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	/** Whether this machine would take the item now, changing nothing. */
	public boolean takes(ItemStack s) {
		if (mixer()) {
			FeedItems.Feed f = FeedItems.feed(s);
			return f != null && asFed + f.kgAsFed() <= MIXER_KG + 1e-9;
		}
		return s.is(FieldCrops.CORN_GRAIN) || s.is(FieldCrops.OAT_GRAIN) || s.is(FieldCrops.BARLEY_GRAIN) || s.is(FieldCrops.ALFALFA_HAY)
				|| s.is(FieldCrops.ALFALFA_BALE) || s.is(Items.CALCITE) || s.is(Items.COD) || s.is(Items.SALMON);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return takes(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		accept(stack);
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
	}

	// ---- Wildcatter ----

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL;
	}

	@Override
	public void build(Circuit circuit) {
		motor = new Motor(true, Voltage.INDUSTRIAL, mixer() ? MIXER_WATTS : MILL_WATTS);
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, mixer() ? asFed > 0 : !pending.isEmpty());
	}

	@Override
	public Component inspect() {
		return Component.literal(readout() + "; " + motor.describe());
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		var p = out.childrenList("Pending");
		pending.forEach((k, v) -> {
			var o = p.addChild();
			o.putString("Id", k);
			o.putInt("Kg", v);
		});
		out.putInt("Fish", fish);
		Ration.Analysis a = batch.analysis();
		out.putDouble("BatchKg", batch.kg());
		out.putDouble("AsFed", asFed);
		out.putDouble("Cp", a.cp());
		out.putDouble("Ndf", a.ndf());
		out.putDouble("Tdn", a.tdn());
		out.putDouble("Forage", a.forage());
		out.putDouble("Ca", a.ca());
		out.putDouble("Premix", a.premix());
		out.putInt("Made", made);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		pending.clear();
		for (ValueInput o : in.childrenListOrEmpty("Pending")) {
			pending.put(o.getStringOr("Id", ""), o.getIntOr("Kg", 0));
		}
		fish = in.getIntOr("Fish", 0);
		batch.set(in.getDoubleOr("BatchKg", 0), new Ration.Analysis(in.getDoubleOr("Cp", 0), in.getDoubleOr("Ndf", 0), in.getDoubleOr("Tdn", 0),
				in.getDoubleOr("Forage", 0), in.getDoubleOr("Ca", 0), in.getDoubleOr("Premix", 0)));
		asFed = in.getDoubleOr("AsFed", 0);
		made = in.getIntOr("Made", 0);
	}
}
