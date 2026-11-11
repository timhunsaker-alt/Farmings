package dev.farmings.coop;

import dev.farmings.entity.Wild;
import dev.farmings.husbandry.poultry.Fowl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** A grain feeder (docs/multiblocks/coop.md): up to 256 grain items, kept as kilograms of feed (100 g an item). Hand or hopper fills it. */
public class GrainFeederEntity extends BlockEntity implements WorldlyContainer {
	public static final int MAX_ITEMS = 256;
	public static final double CAPACITY_KG = MAX_ITEMS * Fowl.GRAIN_ITEM_KG;
	private static final int[] SLOTS = {0};
	private final dev.farmings.feed.FeedMix mix = new dev.farmings.feed.FeedMix();

	public GrainFeederEntity(BlockPos pos, BlockState state) {
		super(CoopBlocks.GRAIN_FEEDER_ENTITY, pos, state);
	}

	public double kg() {
		return mix.kg();
	}

	public dev.farmings.feed.Ration.Analysis analysis() {
		return mix.analysis();
	}

	/** One seed item of mixed grain (100 g). */
	public boolean addItem() {
		return addItem(new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS));
	}

	/** One item of bird feed (grain, seed, meal or a ration), by its mass as fed; false if it is not bird feed or there is no room. */
	public boolean addItem(ItemStack stack) {
		dev.farmings.feedmill.FeedItems.Feed f = dev.farmings.feedmill.FeedItems.feed(stack);
		if (f == null || !dev.farmings.feedmill.FeedItems.birdFeed(stack) || mix.kg() + f.kgAsFed() > CAPACITY_KG + 1e-9) {
			return false;
		}
		mix.add(f.kgAsFed(), f.analysis());
		setChanged();
		return true;
	}

	public double take(double want) {
		double t = mix.take(want);
		if (t > 0) {
			setChanged();
		}
		return t;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		dev.farmings.feedmill.FeedItems.Feed f = dev.farmings.feedmill.FeedItems.feed(stack);
		return f != null && dev.farmings.feedmill.FeedItems.birdFeed(stack) && mix.kg() + f.kgAsFed() <= CAPACITY_KG + 1e-9;
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
		for (int i = 0; i < stack.getCount(); i++) {
			addItem(stack);
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
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putDouble("grain_kg", mix.kg());
		dev.farmings.feed.Ration.Analysis a = mix.analysis();
		out.putDouble("cp", a.cp());
		out.putDouble("ndf", a.ndf());
		out.putDouble("tdn", a.tdn());
		out.putDouble("forage", a.forage());
		out.putDouble("ca", a.ca());
		out.putDouble("premix", a.premix());
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		dev.farmings.feed.Ration.Analysis g = dev.farmings.feed.Feedstuff.MIXED_GRAIN.analysis();
		mix.set(Math.max(0, Math.min(CAPACITY_KG, in.getDoubleOr("grain_kg", 0))), new dev.farmings.feed.Ration.Analysis(in.getDoubleOr("cp", g.cp()),
				in.getDoubleOr("ndf", g.ndf()), in.getDoubleOr("tdn", g.tdn()), in.getDoubleOr("forage", g.forage()), in.getDoubleOr("ca", g.ca()),
				in.getDoubleOr("premix", g.premix())));
	}
}
