package dev.farmings.creamery;

import dev.farmings.FarmingsItems;
import dev.farmings.process.creamery.Creamery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * What a hopper sees on a creamery part (docs/multiblocks/creamery.md, "Hoppers"). Slot 0 is the product, taken from below: filled bottles
 * from the bottler, butter from the churn, aged wheels from the aging shelf. Slot 1 is the bottler's stock of empty glass bottles, put in
 * from the top or sides; each filled bottle uses one, like a hand-filled one. The product is read from the creamery each time, so nothing
 * is held twice. Shared by the unpowered part entity (bottler, shelf) and the machine entity (churn).
 */
public final class CreameryOutlet {
	public static final int OUT = 0;
	public static final int GLASS = 1;
	private static final int[] DOWN = {OUT};
	private static final int[] IN = {GLASS};
	private static final int[] NONE = {};

	private final CreameryParts kind;
	private ItemStack glass = ItemStack.EMPTY;
	private @Nullable BlockPos controller;

	public CreameryOutlet(CreameryParts kind) {
		this.kind = kind;
	}

	public boolean active() {
		return kind == CreameryParts.BOTTLER || kind == CreameryParts.BUTTER_CHURN || kind == CreameryParts.AGING_SHELF;
	}

	public int glassBottles() {
		return glass.getCount();
	}

	public void setGlassBottles(int n) {
		glass = n <= 0 ? ItemStack.EMPTY : new ItemStack(Items.GLASS_BOTTLE, Math.min(64, n));
	}

	private @Nullable Creamery creamery(Level level, BlockPos self) {
		if (level == null) {
			return null;
		}
		if (controller != null && level.getBlockEntity(controller) instanceof CreameryControllerEntity c && c.formed()) {
			return c.creamery();
		}
		CreameryControllerEntity c = CreameryWorld.controllerOf(level, self);
		controller = c == null ? null : c.getBlockPos();
		return c == null ? null : c.creamery();
	}

	public int[] slots(Direction side) {
		if (!active()) {
			return NONE;
		}
		if (side == Direction.DOWN) {
			return DOWN;
		}
		return kind == CreameryParts.BOTTLER ? IN : NONE;
	}

	public boolean canPlace(int slot, ItemStack stack, @Nullable Direction side) {
		return kind == CreameryParts.BOTTLER && slot == GLASS && side != Direction.DOWN && stack.is(Items.GLASS_BOTTLE);
	}

	public boolean canTake(int slot, Direction side) {
		return slot == OUT && side == Direction.DOWN;
	}

	/** What is ready to take now (a copy; taking goes through {@link #take}). */
	public ItemStack peek(Level level, BlockPos self, int slot) {
		if (slot == GLASS) {
			return glass;
		}
		Creamery c = creamery(level, self);
		if (c == null) {
			return ItemStack.EMPTY;
		}
		return switch (kind) {
			case BOTTLER -> {
				Creamery.Bottle b = c.nextBottle();
				yield b == null || glass.isEmpty() ? ItemStack.EMPTY : new ItemStack(FarmingsItems.bottle(b, c.kind()));
			}
			case BUTTER_CHURN -> {
				int n = (int) Math.min(64, Math.floor(c.butterKg() / Creamery.BUTTER_ITEM_KG + 1e-9));
				yield n <= 0 ? ItemStack.EMPTY : new ItemStack(FarmingsItems.BUTTER, n);
			}
			case AGING_SHELF -> {
				if (c.agedWheels() > 0) {
					yield new ItemStack(FarmingsItems.cheeseWheel(c.kind()), Math.min(16, c.agedWheels()));
				}
				int n = (int) Math.min(64, Math.floor(c.chevreKg() / Creamery.CHEVRE_ITEM_KG + 1e-9));     // drained chèvre comes off the shelf too
				yield n <= 0 ? ItemStack.EMPTY : new ItemStack(FarmingsItems.CHEVRE, n);
			}
			default -> ItemStack.EMPTY;
		};
	}

	/** Takes the product: it leaves the cold creamery now, so its spoil clock starts (docs/food.md). */
	public ItemStack take(Level level, BlockPos self, int slot, int count) {
		if (slot == GLASS) {
			return glass.split(count);
		}
		return dev.farmings.item.Perishables.stamp(takeRaw(level, self, count), level.getGameTime());
	}

	private ItemStack takeRaw(Level level, BlockPos self, int count) {
		Creamery c = creamery(level, self);
		if (c == null || count <= 0) {
			return ItemStack.EMPTY;
		}
		return switch (kind) {
			case BOTTLER -> {
				if (glass.isEmpty()) {
					yield ItemStack.EMPTY;
				}
				Creamery.Bottle b = c.takeBottle();
				if (b == null) {
					yield ItemStack.EMPTY;
				}
				glass.shrink(1);
				yield new ItemStack(FarmingsItems.bottle(b, c.kind()));
			}
			case BUTTER_CHURN -> {
				int n = c.takeButter(count);
				yield n <= 0 ? ItemStack.EMPTY : new ItemStack(FarmingsItems.BUTTER, n);
			}
			case AGING_SHELF -> {
				int n = c.takeWheels(count);
				if (n > 0) {
					yield new ItemStack(FarmingsItems.cheeseWheel(c.kind()), n);
				}
				int ch = c.takeChevre(count);
				yield ch <= 0 ? ItemStack.EMPTY : new ItemStack(FarmingsItems.CHEVRE, ch);
			}
			default -> ItemStack.EMPTY;
		};
	}

	/**
	 * A hopper sets a slot: glass bottles into the bottler's stock, or, on the product slot, what it took and could not hold, which goes
	 * back into the creamery (a hopper takes first and puts back when it is full).
	 */
	public void put(Level level, BlockPos self, int slot, ItemStack stack) {
		if (slot == GLASS) {
			glass = stack.is(Items.GLASS_BOTTLE) ? stack : ItemStack.EMPTY;
			return;
		}
		Creamery c = creamery(level, self);
		if (c == null || stack.isEmpty()) {
			return;
		}
		if (stack.is(FarmingsItems.BUTTER)) {
			c.returnButter(stack.getCount());
		} else if (stack.is(FarmingsItems.CHEDDAR_WHEEL) || stack.is(FarmingsItems.SHEEP_CHEESE_WHEEL)) {
			c.returnWheels(stack.getCount());
		} else if (stack.is(FarmingsItems.CHEVRE)) {
			c.returnChevre(stack.getCount());
		} else {
			for (Creamery.Bottle b : Creamery.Bottle.values()) {
				if (stack.is(FarmingsItems.bottle(b, c.kind()))) {
					for (int i = 0; i < stack.getCount(); i++) {
						c.returnBottle(b);
					}
					glass.grow(stack.getCount());
					if (glass.isEmpty()) {
						glass = new ItemStack(Items.GLASS_BOTTLE, stack.getCount());
					}
				}
			}
		}
	}
}
