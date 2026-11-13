package dev.farmings.curing;

import dev.farmings.FarmingsItems;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.process.curing.Curing;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The curing rack (curing-room.md): up to 4 fresh hams or bellies with their cure. The cure runs only while the rack's cold room keeps
 * 4 °C (the room tells it, {@link #chill}); a ham then equalizes, is smoked by a lit smokehouse beside the rack, and ages; a belly is smoked
 * into bacon. An empty hand takes what is done.
 */
public class CuringRackEntity extends BlockEntity implements CuringMachine {
	public static final int SIZE = 4;
	private final List<Curing.Piece> pieces = new ArrayList<>();
	private double cureKg;
	private long lastCold = Long.MIN_VALUE / 2;
	private double lastTempC = 20;
	private boolean forcedCold;

	public CuringRackEntity(BlockPos pos, BlockState state) {
		super(CuringBlocks.CURING_RACK_ENTITY, pos, state);
	}

	/** The cold room's mean temperature over its last step. */
	public void chill(double meanC, long now) {
		lastCold = now;
		lastTempC = meanC;
	}

	public void forceColdForTest(boolean on) {
		forcedCold = on;
	}

	public boolean cold(long now) {
		return forcedCold || now - lastCold <= 1300 && lastTempC <= Curing.CURE_MAX_C;
	}

	public List<Curing.Piece> pieces() {
		return pieces;
	}

	public double cureKg() {
		return cureKg;
	}

	@Override
	public boolean insert(ItemStack stack, ServerLevel level) {
		if (stack.is(FarmingsItems.CURE)) {
			cureKg += stack.getCount() * Curing.CURE_PER_KG;
			stack.setCount(0);
			setChanged();
			return true;
		}
		Curing.Kind kind = stack.is(FarmingsItems.FRESH_HAM) ? Curing.Kind.HAM : stack.is(FarmingsItems.PORK_BELLY) ? Curing.Kind.BACON : null;
		if (kind == null || pieces.size() >= SIZE) {
			return false;
		}
		double kg = kind == Curing.Kind.HAM ? FarmingsItems.kgOf(stack) : 2;
		if (kg <= 0 || cureKg + 1e-9 < kg * Curing.CURE_PER_KG) {
			return false;
		}
		cureKg -= kg * Curing.CURE_PER_KG;
		pieces.add(new Curing.Piece(kind, kg));
		stack.shrink(1);
		setChanged();
		return true;
	}

	@Override
	public ItemStack take(ServerLevel level) {
		for (int i = 0; i < pieces.size(); i++) {
			Curing.Piece p = pieces.get(i);
			if (p.phase == Curing.Phase.DONE || p.phase == Curing.Phase.SPOILED) {
				pieces.remove(i);
				setChanged();
				long now = level.getGameTime();
				if (p.phase == Curing.Phase.SPOILED) {
					return new ItemStack(FarmingsItems.SPOILED_PORK, p.kind == Curing.Kind.HAM ? 1 : 1);
				}
				return p.kind == Curing.Kind.HAM ? FarmingsItems.countryHam(p.outKg(), now)
						: dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.BACON, p.baconItems()), now);
			}
		}
		return ItemStack.EMPTY;
	}

	private SmokehouseEntity smokehouse(ServerLevel level) {
		for (Direction d : Direction.values()) {
			if (level.getBlockEntity(worldPosition.relative(d)) instanceof SmokehouseEntity s) {
				return s;
			}
		}
		return null;
	}

	/** Runs the pieces {@code ticks} on at the machine scale (15:1). */
	public void run(ServerLevel level, long ticks) {
		long now = level.getGameTime();
		double days = TimeScale.LIVESTOCK.ticksToRealDays(ticks);
		boolean anySmoking = false;
		for (Curing.Piece p : pieces) {
			anySmoking |= p.phase == Curing.Phase.SMOKE;
		}
		SmokehouseEntity s = anySmoking ? smokehouse(level) : null;
		boolean smoked = s != null && s.smoke(ticks);
		boolean cold = cold(now);
		for (Curing.Piece p : pieces) {
			p.step(days, cold, smoked);
		}
		setChanged();
	}

	@Override
	public void serverTick(ServerLevel level) {
		if (level.getGameTime() % 20 == 0 && !pieces.isEmpty()) {
			run(level, 20);
		}
	}

	@Override
	public String readout() {
		StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "Curing rack: cure for %.0f kg, %s", cureKg,
				level != null && cold(level.getGameTime()) ? "cold" : "NOT COLD (the cure needs a cold room at 4 C)"));
		for (Curing.Piece p : pieces) {
			double need = Curing.days(p.kind, p.phase, p.kg);
			b.append(String.format(Locale.ROOT, "; %s %.1f kg %s%s", p.kind == Curing.Kind.HAM ? "ham" : "belly", p.kg, p.phase.name().toLowerCase(Locale.ROOT),
					need > 0 ? String.format(Locale.ROOT, " %.0f of %.0f days", p.progress, need) : ""));
		}
		return b.toString();
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putDouble("CureKg", cureKg);
		var list = out.childrenList("Pieces");
		for (Curing.Piece p : pieces) {
			var o = list.addChild();
			o.putString("Kind", p.kind.name());
			o.putDouble("Kg", p.kg);
			o.putString("Phase", p.phase.name());
			o.putDouble("Progress", p.progress);
			o.putDouble("Warm", p.warmDays);
		}
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		cureKg = in.getDoubleOr("CureKg", 0);
		pieces.clear();
		for (ValueInput o : in.childrenListOrEmpty("Pieces")) {
			Curing.Piece p = new Curing.Piece(Curing.Kind.valueOf(o.getStringOr("Kind", "HAM")), o.getDoubleOr("Kg", 1));
			p.phase = Curing.Phase.valueOf(o.getStringOr("Phase", "CURE"));
			p.progress = o.getDoubleOr("Progress", 0);
			p.warmDays = o.getDoubleOr("Warm", 0);
			pieces.add(p);
		}
	}
}
