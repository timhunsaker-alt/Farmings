package dev.farmings.digester;

import dev.farmings.FarmingsItems;
import dev.farmings.barn.PitEntity;
import dev.farmings.conveyor.ItemSink;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The digester's feed pit (digester.md): takes manure (50 kg) and poultry litter (10 kg) by belt, hopper or hand, or straight from a barn
 * collection pit it touches, and feeds the digester tank beside it.
 */
public class FeedPitEntity extends BlockEntity implements ItemSink, WorldlyContainer {
	private static final int[] SLOTS = {0};

	public FeedPitEntity(BlockPos pos, BlockState state) {
		super(DigesterBlocks.FEED_PIT_ENTITY, pos, state);
	}

	private @Nullable DigesterEntity tank() {
		if (level == null) {
			return null;
		}
		for (Direction d : Direction.values()) {
			if (dev.wildcatter.multiblock.BigParts.entity(level, worldPosition.relative(d)) instanceof DigesterEntity t) {
				return t;
			}
		}
		return null;
	}

	public static double kg(ItemStack s) {
		return s.is(FarmingsItems.MANURE) ? 50 : s.is(FarmingsItems.POULTRY_LITTER) ? 10 : 0;
	}

	@Override
	public ItemStack accept(ItemStack stack) {
		DigesterEntity t = tank();
		double each = kg(stack);
		if (t == null || each <= 0) {
			return stack;
		}
		t.digester().feed(each * stack.getCount());
		t.setChanged();
		return ItemStack.EMPTY;
	}

	/**
	 * Pig slurry from a slurry pump (pig-barn.md, "Digester"): 0.56 m³ of biogas a kg VS at VS 8.8% of fresh weight (NC State; MWPS-18) is
	 * 49 m³ a tonne against dairy manure's 23, so a kg of slurry feeds the digester as 2.14 kg of dairy manure *(derived)*. Returns kg taken.
	 */
	public double feedSlurry(double kg) {
		DigesterEntity t = tank();
		if (t == null || kg <= 0) {
			return 0;
		}
		t.digester().feed(kg * SLURRY_DAIRY_EQUIVALENT);
		t.setChanged();
		return kg;
	}

	public static final double SLURRY_DAIRY_EQUIVALENT = 0.56 * 0.088 * 1000 / (dev.farmings.farm.digester.Digester.M3_PER_KG * 1000);

	/** Pulls manure from a barn collection pit it touches, a few items a second. */
	public void serverTick(ServerLevel level) {
		if (level.getGameTime() % 20 != 0 || tank() == null) {
			return;
		}
		for (Direction d : Direction.values()) {
			if (BigParts.entity(level, worldPosition.relative(d)) instanceof PitEntity pit) {
				int n = pit.take(4);
				if (n > 0) {
					accept(new ItemStack(FarmingsItems.MANURE, n));
				}
			}
		}
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return kg(stack) > 0 && tank() != null;
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
}
