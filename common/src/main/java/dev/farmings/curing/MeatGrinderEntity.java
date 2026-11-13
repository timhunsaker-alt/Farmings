package dev.farmings.curing;

import dev.farmings.FarmingsItems;
import dev.farmings.process.curing.Curing;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The hand-cranked meat grinder (curing-room.md): pork shoulder, fat and salt go in; with 3 shoulders, 2 fat and a salt an empty hand
 * cranks out 32 sausage links (25% fat, USDA 1978).
 */
public class MeatGrinderEntity extends BlockEntity implements CuringMachine {
	private int shoulders;
	private int fat;
	private int salt;

	public MeatGrinderEntity(BlockPos pos, BlockState state) {
		super(CuringBlocks.MEAT_GRINDER_ENTITY, pos, state);
	}

	@Override
	public boolean insert(ItemStack stack, ServerLevel level) {
		if (stack.is(FarmingsItems.PORK_SHOULDER) && shoulders < 64) {
			shoulders++;
		} else if (stack.is(FarmingsItems.PORK_FAT) && fat < 64) {
			fat++;
		} else if (stack.is(FarmingsItems.SALT) && salt < 64) {
			salt++;
		} else {
			return false;
		}
		stack.shrink(1);
		setChanged();
		return true;
	}

	@Override
	public ItemStack take(ServerLevel level) {
		if (shoulders < Curing.SAUSAGE_SHOULDERS || fat < Curing.SAUSAGE_FAT || salt < 1) {
			return ItemStack.EMPTY;
		}
		shoulders -= Curing.SAUSAGE_SHOULDERS;
		fat -= Curing.SAUSAGE_FAT;
		salt--;
		setChanged();
		return dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.SAUSAGE, Curing.SAUSAGE_LINKS), level.getGameTime());
	}

	@Override
	public void serverTick(ServerLevel level) {
	}

	@Override
	public String readout() {
		return String.format(Locale.ROOT, "Meat grinder: %d shoulder, %d fat, %d salt (a batch takes %d, %d and 1)", shoulders, fat, salt,
				Curing.SAUSAGE_SHOULDERS, Curing.SAUSAGE_FAT);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putInt("Shoulders", shoulders);
		out.putInt("Fat", fat);
		out.putInt("Salt", salt);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		shoulders = in.getIntOr("Shoulders", 0);
		fat = in.getIntOr("Fat", 0);
		salt = in.getIntOr("Salt", 0);
	}
}
