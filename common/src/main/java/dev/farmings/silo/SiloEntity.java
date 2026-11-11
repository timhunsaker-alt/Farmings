package dev.farmings.silo;

import dev.farmings.crops.FieldCrops;
import dev.farmings.farm.silo.Silage;
import dev.farmings.farm.silo.SiloLayout;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The tower silo's unloader and the silo's controller (docs/multiblocks/silo.md). It holds the silo's dry matter, takes chopped forage
 * from the blower (10.5% of the dry matter is lost), ferments for 21 real days after the last fill, then unloads 25 kg silage items to a
 * belt, hopper or chest beside it at the unloader's rate. 18.6 kW (25 hp) on Wildcatter 480 V.
 */
public class SiloEntity extends BlockEntity implements CircuitPart, Inspectable {
	public enum Kind { CORN, HAYLAGE }

	public static final double WATTS = 18_650;
	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, WATTS);
	private @Nullable Kind kind;
	private double dmKg;
	private long lastFill = Long.MIN_VALUE;
	private int cooldown;
	private boolean forced;
	private int unloaded;

	public SiloEntity(BlockPos pos, BlockState state) {
		super(SiloBlocks.UNLOADER_ENTITY, pos, state);
	}

	public SiloLayout.@Nullable Tower tower() {
		return level == null ? null : Towers.fromBase(level, worldPosition, SiloBlocks.SILO_STAVE, SiloBlocks.SILO_ROOF, Towers.SILO_MAX);
	}

	public double dmKg() {
		return dmKg;
	}

	public @Nullable Kind kind() {
		return kind;
	}

	public int unloaded() {
		return unloaded;
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	public boolean fermenting() {
		return level != null && lastFill != Long.MIN_VALUE && level.getGameTime() - lastFill < Silage.fermentTicks();
	}

	/** For game tests: the last fill was this many more real days ago. */
	public void ageForTest(double days) {
		lastFill -= dev.farmings.husbandry.TimeScale.realDaysToTicks(days);
	}

	/** Blown in: chopped corn or chopped alfalfa, one kind at a time, up to the silo's room. Returns the rest. */
	public ItemStack fill(ItemStack stack) {
		Kind k = stack.is(FieldCrops.CHOPPED_CORN) ? Kind.CORN : stack.is(FieldCrops.CHOPPED_ALFALFA) ? Kind.HAYLAGE : null;
		SiloLayout.Tower t = tower();
		if (k == null || t == null || !t.formed() || kind != null && kind != k && dmKg > 0.01) {
			return stack;
		}
		double each = Silage.ITEM_KG * (k == Kind.CORN ? Silage.CHOPPED_CORN_DM : Silage.HAYLAGE_DM);
		double room = Silage.capacityKgDm(t.volume()) - dmKg;
		int n = (int) Math.min(stack.getCount(), Math.floor(room / each + 1e-9));
		if (n <= 0) {
			return stack;
		}
		kind = k;
		dmKg += Silage.kept(n * each);
		lastFill = level.getGameTime();
		setChanged();
		return stack.copyWithCount(stack.getCount() - n);
	}

	private Item silageItem() {
		return kind == Kind.HAYLAGE ? FieldCrops.HAYLAGE : FieldCrops.CORN_SILAGE;
	}

	private double silageDm() {
		return Silage.ITEM_KG * (kind == Kind.HAYLAGE ? Silage.HAYLAGE_DM : Silage.CORN_SILAGE_DM);
	}

	public void serverTick(ServerLevel level) {
		if (--cooldown > 0 || kind == null || !powered() || fermenting() || dmKg < silageDm() - 1e-9) {
			return;
		}
		cooldown = Silage.unloadTicks();
		SiloLayout.Tower t = tower();
		if (t == null || !t.formed()) {
			return;
		}
		ItemStack out = new ItemStack(silageItem());
		ItemStack rest = Towers.handOn(level, worldPosition, out, p -> level.getBlockState(p).getBlock() == SiloBlocks.SILO_STAVE
				|| level.getBlockState(p).getBlock() == SiloBlocks.FORAGE_BLOWER);
		if (rest.isEmpty()) {
			dmKg -= silageDm();
			unloaded++;
			if (dmKg < 0.01) {
				kind = null;
				dmKg = 0;
			}
			setChanged();
		}
	}

	public String readout() {
		SiloLayout.Tower t = tower();
		if (t == null || !t.formed()) {
			return "Tower silo: " + (t == null ? "set the unloader beside the bottom ring of silo staves" : t.problem());
		}
		double cap = Silage.capacityKgDm(t.volume());
		StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "Tower silo: %d high, %d m3, holds %.0f of %.0f kg DM", t.height(), t.volume(), dmKg, cap));
		if (kind != null) {
			b.append(kind == Kind.CORN ? " (corn silage)" : " (haylage)");
		}
		if (fermenting()) {
			double left = dev.farmings.husbandry.TimeScale.ticksToRealDays(Silage.fermentTicks() - (level.getGameTime() - lastFill));
			b.append(String.format(Locale.ROOT, ", FERMENTING (%.1f days to go)", left));
		}
		b.append(powered() ? ", unloader on" : ", unloader NO POWER").append(", ").append(unloaded).append(" unloaded");
		return b.toString();
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, kind != null && !fermenting());
	}

	@Override
	public Component inspect() {
		return Component.literal(readout() + "; " + motor.describe());
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putString("Kind", kind == null ? "" : kind.name());
		out.putDouble("DmKg", dmKg);
		out.putLong("LastFill", lastFill);
		out.putInt("Unloaded", unloaded);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		String k = in.getStringOr("Kind", "");
		kind = k.isEmpty() ? null : Kind.valueOf(k);
		dmKg = in.getDoubleOr("DmKg", 0);
		lastFill = in.getLongOr("LastFill", Long.MIN_VALUE);
		unloaded = in.getIntOr("Unloaded", 0);
	}
}
