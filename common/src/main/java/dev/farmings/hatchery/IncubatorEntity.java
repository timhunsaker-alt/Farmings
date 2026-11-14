package dev.farmings.hatchery;

import dev.farmings.entity.FowlEggs;
import dev.farmings.entity.FowlEntity;
import dev.farmings.genetics.Genome;
import dev.farmings.genetics.Species;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.poultry.Clutch;
import dev.farmings.husbandry.poultry.FowlBreed;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The incubator (docs/multiblocks/hatchery.md): 24 fertile eggs of any species, kept at the source settings while powered. Each egg's
 * clock runs on the birds' 3:1 scale and hatches after its species' days; without power the clocks pause. Hatchlings belong to the
 * player who last filled it.
 */
public class IncubatorEntity extends BlockEntity implements CircuitPart, Inspectable, WorldlyContainer {
	public static final int SIZE = 24;
	/** Power drawn, 0.025 kW: "110 Volt 25 Watt Square Heat Element" (GQF Hova-Bator 1602N manual). */
	public static final double WATTS = 25;
	private static final int[] SLOTS = java.util.stream.IntStream.range(0, SIZE).toArray();
	private final NonNullList<ItemStack> eggs = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private final Motor heater = new Motor(false, Voltage.LOW, WATTS);
	private boolean forced;
	private int hatched;
	private @Nullable UUID owner;

	public IncubatorEntity(BlockPos pos, BlockState state) {
		super(HatcheryBlocks.INCUBATOR_ENTITY, pos, state);
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || heater.output() > 0;
	}

	public int hatched() {
		return hatched;
	}

	public NonNullList<ItemStack> eggs() {
		return eggs;
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
		setChanged();
	}

	/** Only fertile Farmings eggs go in. */
	public static boolean accepts(ItemStack egg) {
		return FowlEggs.species(egg) != null && FowlEggs.fertile(egg);
	}

	public boolean add(ItemStack egg) {
		if (!accepts(egg)) {
			return false;
		}
		for (int i = 0; i < SIZE; i++) {
			if (eggs.get(i).isEmpty()) {
				eggs.set(i, egg.copyWithCount(1));
				setChanged();
				return true;
			}
		}
		return false;
	}

	public int count() {
		int n = 0;
		for (ItemStack e : eggs) {
			if (!e.isEmpty()) {
				n++;
			}
		}
		return n;
	}

	static FowlBreed breedOf(ItemStack egg) {
		Species s = FowlEggs.species(egg);
		Genome dam = Genome.decode(FowlEggs.tag(egg).getStringOr(FowlEggs.DAM, ""));
		return FowlEntity.topBreed(dam.breedCompositionBp(), s == null ? Species.CHICKEN : s);
	}

	public void serverTick(ServerLevel level) {
		if (level.getGameTime() % 20 == 0) {
			warm(level, 20);
		}
	}

	/** {@code ticks} of warmth for every egg that can still hatch; hatches those whose days are up, on top of the incubator. */
	public void warm(ServerLevel level, long ticks) {
		if (!powered()) {
			return;
		}
		long now = level.getGameTime();
		double days = TimeScale.FOWL.ticksToRealDays(ticks);
		for (int i = 0; i < SIZE; i++) {
			ItemStack egg = eggs.get(i);
			if (egg.isEmpty() || !FowlEggs.hatchable(egg, now)) {
				continue;
			}
			double warm = FowlEggs.warmDays(egg) + days;
			FowlEggs.setWarmDays(egg, warm);
			if (warm >= breedOf(egg).incubationDays) {
				BlockPos at = level.getBlockState(worldPosition.above()).getCollisionShape(level, worldPosition.above()).isEmpty() ? worldPosition.above()
						: worldPosition.relative(Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom()));
				if (FowlEggs.hatch(level, egg, at, owner) != null) {
					hatched++;
				}
				eggs.set(i, ItemStack.EMPTY);
			}
		}
		setChanged();
	}

	public String readout() {
		StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "Incubator (%.2f kW): %s, %d of %d eggs", WATTS / 1000, powered() ? "running" : "NO POWER",
				count(), SIZE));
		double next = Double.MAX_VALUE;
		int lockdown = 0;
		int dead = 0;
		long now = level == null ? 0 : level.getGameTime();
		for (ItemStack e : eggs) {
			if (e.isEmpty()) {
				continue;
			}
			if (!FowlEggs.hatchable(e, now)) {
				dead++;
				continue;
			}
			FowlBreed br = breedOf(e);
			double left = br.incubationDays - FowlEggs.warmDays(e);
			next = Math.min(next, left);
			if (Clutch.stage(br, FowlEggs.warmDays(e)) == Clutch.Stage.LOCKDOWN) {
				lockdown++;
			}
		}
		if (next < Double.MAX_VALUE) {
			b.append(String.format(Locale.ROOT, ", next hatch in %.1f days (%.1f game days)", next, TimeScale.FOWL.realDaysToGameDays(next)));
		}
		if (lockdown > 0) {
			b.append(", ").append(lockdown).append(" in lockdown (no turning, high humidity)");
		}
		if (dead > 0) {
			b.append(", ").append(dead).append(" too old to start");
		}
		b.append(", 99.5-100.5 F, ").append(hatched).append(" hatched");
		return b.toString();
	}

	// ---- Wildcatter ----

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.LOW;
	}

	@Override
	public void build(Circuit circuit) {
		heater.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		heater.apply(circuit, ticks, count() > 0);
	}

	@Override
	public Component inspect() {
		return Component.literal(readout() + "; " + heater.describe());
	}

	// ---- hoppers: fertile eggs in from above and the sides; nothing out ----

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return eggs.get(slot).isEmpty() && accepts(stack) && stack.getCount() == 1;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		return count() == 0;
	}

	@Override
	public ItemStack getItem(int slot) {
		return eggs.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack r = ContainerHelper.removeItem(eggs, slot, count);
		setChanged();
		return r;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(eggs, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		eggs.set(slot, stack.copyWithCount(Math.min(1, stack.getCount())));
		setChanged();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		eggs.clear();
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		ContainerHelper.saveAllItems(out, eggs);
		out.putInt("Hatched", hatched);
		if (owner != null) {
			out.putString("Owner", owner.toString());
		}
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		eggs.clear();
		ContainerHelper.loadAllItems(in, eggs);
		hatched = in.getIntOr("Hatched", 0);
		String o = in.getStringOr("Owner", "");
		owner = o.isEmpty() ? null : UUID.fromString(o);
	}
}
