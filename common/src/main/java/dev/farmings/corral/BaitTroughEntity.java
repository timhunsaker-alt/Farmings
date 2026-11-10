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
 * The corral's bait trough (docs/taming.md): up to 4 hay blocks, kept as kilograms of dry matter as the barn's feeder does. Its hay is the
 * lure and the corral's feed. A hopper can put hay blocks in; nothing comes out.
 */
public class BaitTroughEntity extends BlockEntity implements WorldlyContainer {
	public static final int MAX_BLOCKS = 4;
	public static final double CAPACITY_KG = MAX_BLOCKS * Hay.blockDryMatterKg();
	private static final int[] SLOTS = {0};

	private double dryMatterKg;

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
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		dryMatterKg = Math.max(0, Math.min(CAPACITY_KG, input.getDoubleOr("dry_matter_kg", 0)));
	}
}
