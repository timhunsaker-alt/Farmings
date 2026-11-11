package dev.farmings.silo;

import dev.farmings.crops.FieldCrops;
import dev.farmings.farm.silo.Silage;
import dev.farmings.farm.silo.SiloLayout;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The grain bin's hopper and the bin's controller (silo.md): it holds one grain at a time, up to the bin's volume at the grain's bulk
 * density, takes grain from the roof's fill hole and lets it out by gravity, with no power, to a belt, auger, hopper or chest beside it.
 */
public class GrainBinEntity extends BlockEntity {
	private @Nullable Item grain;
	private int items;
	private int out;

	public GrainBinEntity(BlockPos pos, BlockState state) {
		super(SiloBlocks.HOPPER_ENTITY, pos, state);
	}

	public SiloLayout.@Nullable Tower tower() {
		return level == null ? null : Towers.fromBase(level, worldPosition, SiloBlocks.BIN_WALL, SiloBlocks.BIN_ROOF, Towers.BIN_MAX);
	}

	/** Bulk density of a grain item, kg/m3; 0 if it is not grain the bin holds. Seed and vanilla wheat ride as oats' density (proposed). */
	public static double density(ItemStack s) {
		if (s.is(FieldCrops.CORN_GRAIN)) {
			return Silage.CORN_DENSITY;
		}
		if (s.is(FieldCrops.OAT_GRAIN) || s.is(Items.WHEAT)) {
			return Silage.OAT_DENSITY;
		}
		if (s.is(FieldCrops.BARLEY_GRAIN)) {
			return Silage.BARLEY_DENSITY;
		}
		return 0;
	}

	public int items() {
		return items;
	}

	public @Nullable Item grain() {
		return grain;
	}

	/** Grain into the bin (1 kg an item); one grain at a time. Returns the rest. */
	public ItemStack fill(ItemStack stack) {
		double rho = density(stack);
		SiloLayout.Tower t = tower();
		if (rho <= 0 || t == null || !t.formed() || grain != null && items > 0 && !stack.is(grain)) {
			return stack;
		}
		int cap = (int) Math.floor(t.volume() * rho);
		int n = Math.min(stack.getCount(), cap - items);
		if (n <= 0) {
			return stack;
		}
		grain = stack.getItem();
		items += n;
		setChanged();
		return stack.copyWithCount(stack.getCount() - n);
	}

	public void serverTick(ServerLevel level) {
		if (grain == null || items <= 0 || level.getGameTime() % 4 != 0) {
			return;
		}
		int n = Math.min(items, 16);
		ItemStack rest = Towers.handOn(level, worldPosition, new ItemStack(grain, n), p -> level.getBlockState(p).getBlock() == SiloBlocks.BIN_WALL);
		int moved = n - rest.getCount();
		if (moved > 0) {
			items -= moved;
			out += moved;
			if (items == 0) {
				grain = null;
			}
			setChanged();
		}
	}

	public String readout() {
		SiloLayout.Tower t = tower();
		if (t == null || !t.formed()) {
			return "Grain bin: " + (t == null ? "set the hopper beside the bottom ring of bin wall" : t.problem());
		}
		String what = grain == null ? "empty" : String.format(Locale.ROOT, "%d kg of %s", items, new ItemStack(grain).getHoverName().getString());
		return String.format(Locale.ROOT, "Grain bin: %d high, %d m3, %s; %d kg let out", t.height(), t.volume(), what, out);
	}

	@Override
	protected void saveAdditional(ValueOutput o) {
		super.saveAdditional(o);
		o.putString("Grain", grain == null ? "" : BuiltInRegistries.ITEM.getKey(grain).toString());
		o.putInt("Items", items);
		o.putInt("Out", out);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		String g = in.getStringOr("Grain", "");
		grain = g.isEmpty() ? null : BuiltInRegistries.ITEM.getValue(Identifier.parse(g));
		items = in.getIntOr("Items", 0);
		out = in.getIntOr("Out", 0);
	}
}
