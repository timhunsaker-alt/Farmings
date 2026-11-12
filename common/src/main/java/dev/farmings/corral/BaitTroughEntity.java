package dev.farmings.corral;

import dev.farmings.feed.Hay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The corral's bait trough (docs/taming.md): up to 4 hay blocks, kept as kilograms of dry matter as the barn's feeder does, and up to 256
 * grain items for birds, kept as kilograms of feed (docs/animals/poultry.md: 100 g an item, proposed). Its feed is the lure and the
 * corral's feed; each animal eats only its own. A hopper can put hay blocks and grain in; nothing comes out.
 */
public class BaitTroughEntity extends BlockEntity implements WorldlyContainer {
	public static final int MAX_BLOCKS = 4;
	public static final double CAPACITY_KG = MAX_BLOCKS * Hay.blockDryMatterKg();
	private static final int[] SLOTS = {0};

	/** Grain the trough holds: 256 items (proposed, the coop's grain feeder). */
	public static final double GRAIN_CAPACITY_KG = 256 * dev.farmings.husbandry.poultry.Fowl.GRAIN_ITEM_KG;

	private double dryMatterKg;
	private double grainKg;
	/** Raw meat for a wild dog: one item is its day's 0.5 kg (sheep.md, "Sheep dog"; proposed), and the trough holds 16. */
	public static final double MEAT_ITEM_KG = 0.5;
	public static final double MEAT_CAPACITY_KG = 16 * MEAT_ITEM_KG;
	private double meatKg;

	public BaitTroughEntity(BlockPos pos, BlockState state) {
		super(CorralBlocks.BAIT_TROUGH_ENTITY, pos, state);
	}

	public double dryMatterKg() {
		return dryMatterKg;
	}

	public boolean addBlock() {
		if (dryMatterKg + Hay.blockDryMatterKg() > CAPACITY_KG + 1e-9) {
			return false;
		}
		dryMatterKg += Hay.blockDryMatterKg();
		setChanged();
		return true;
	}

	public double grainKg() {
		return grainKg;
	}

	public boolean addGrain() {
		if (grainKg + dev.farmings.husbandry.poultry.Fowl.GRAIN_ITEM_KG > GRAIN_CAPACITY_KG + 1e-9) {
			return false;
		}
		grainKg += dev.farmings.husbandry.poultry.Fowl.GRAIN_ITEM_KG;
		setChanged();
		return true;
	}

	public double takeGrain(double kg) {
		double taken = Math.max(0, Math.min(kg, grainKg));
		if (taken > 0) {
			grainKg -= taken;
			setChanged();
		}
		return taken;
	}

	public double meatKg() {
		return meatKg;
	}

	public boolean addMeat() {
		if (meatKg + MEAT_ITEM_KG > MEAT_CAPACITY_KG + 1e-9) {
			return false;
		}
		meatKg += MEAT_ITEM_KG;
		setChanged();
		return true;
	}

	/** Feed of one kind: hay (kg DM), grain or meat (kg as fed). */
	public double feedKg(dev.farmings.entity.Wild.Bait bait) {
		return switch (bait) {
			case HAY -> dryMatterKg;
			case GRAIN -> grainKg;
			case MEAT -> meatKg;
		};
	}

	public double take(dev.farmings.entity.Wild.Bait bait, double kg) {
		if (bait != dev.farmings.entity.Wild.Bait.MEAT) {
			return take(bait == dev.farmings.entity.Wild.Bait.GRAIN, kg);
		}
		double taken = Math.max(0, Math.min(kg, meatKg));
		if (taken > 0) {
			meatKg -= taken;
			setChanged();
		}
		return taken;
	}

	/** Feed for this animal: grain for birds, hay for the rest. */
	public double feedKg(boolean grain) {
		return grain ? grainKg : dryMatterKg;
	}

	public double take(boolean grain, double kg) {
		return grain ? takeGrain(kg) : take(kg);
	}

	public double take(double kg) {
		double taken = Math.max(0, Math.min(kg, dryMatterKg));
		if (taken > 0) {
			dryMatterKg -= taken;
			setChanged();
		}
		return taken;
	}

	// ---- a hopper puts hay blocks in ----

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		if (dev.farmings.entity.Wild.meat(stack)) {
			return meatKg + MEAT_ITEM_KG <= MEAT_CAPACITY_KG + 1e-9;
		}
		if (dev.farmings.entity.Wild.grain(stack)) {
			return grainKg + dev.farmings.husbandry.poultry.Fowl.GRAIN_ITEM_KG <= GRAIN_CAPACITY_KG + 1e-9;
		}
		return stack.is(Items.HAY_BLOCK) && dryMatterKg + Hay.blockDryMatterKg() <= CAPACITY_KG + 1e-9;
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
		for (int i = 0; i < stack.getCount() && stack.is(Items.HAY_BLOCK); i++) {
			addBlock();
		}
		for (int i = 0; i < stack.getCount() && dev.farmings.entity.Wild.grain(stack); i++) {
			addGrain();
		}
		for (int i = 0; i < stack.getCount() && dev.farmings.entity.Wild.meat(stack); i++) {
			addMeat();
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("dry_matter_kg", dryMatterKg);
		output.putDouble("grain_kg", grainKg);
		output.putDouble("meat_kg", meatKg);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		dryMatterKg = Math.max(0, Math.min(CAPACITY_KG, input.getDoubleOr("dry_matter_kg", 0)));
		grainKg = Math.max(0, Math.min(GRAIN_CAPACITY_KG, input.getDoubleOr("grain_kg", 0)));
		meatKg = Math.max(0, Math.min(MEAT_CAPACITY_KG, input.getDoubleOr("meat_kg", 0)));
	}
}
