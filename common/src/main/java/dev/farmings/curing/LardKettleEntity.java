package dev.farmings.curing;

import dev.farmings.FarmingsItems;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.process.curing.Curing;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The lard kettle (curing-room.md): pork fat in, over a lit campfire or fire below; a batch renders in 3 real hours at the machine scale
 * into lard at 0.75 kg a kg *(proposed)*. An empty hand takes the lard.
 */
public class LardKettleEntity extends BlockEntity implements CuringMachine {
	public static final double MAX_FAT_KG = 64;
	private double fatKg;
	private double lardKg;
	private double renderDays;

	public LardKettleEntity(BlockPos pos, BlockState state) {
		super(CuringBlocks.LARD_KETTLE_ENTITY, pos, state);
	}

	public double lardKg() {
		return lardKg;
	}

	public boolean heated(ServerLevel level) {
		BlockState below = level.getBlockState(worldPosition.below());
		return CampfireBlock.isLitCampfire(below) || below.is(net.minecraft.world.level.block.Blocks.FIRE);
	}

	@Override
	public boolean insert(ItemStack stack, ServerLevel level) {
		if (!stack.is(FarmingsItems.PORK_FAT) || fatKg >= MAX_FAT_KG) {
			return false;
		}
		int n = (int) Math.min(stack.getCount(), MAX_FAT_KG - fatKg);
		fatKg += n;
		stack.shrink(n);
		setChanged();
		return true;
	}

	@Override
	public ItemStack take(ServerLevel level) {
		int n = (int) Math.floor(lardKg + 1e-9);
		if (n <= 0) {
			return ItemStack.EMPTY;
		}
		lardKg -= n;
		setChanged();
		return dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.LARD, n), level.getGameTime());
	}

	public void run(ServerLevel level, long ticks) {
		if (fatKg <= 0 || !heated(level)) {
			return;
		}
		renderDays += TimeScale.LIVESTOCK.ticksToRealDays(ticks);
		if (renderDays >= Curing.RENDER_DAYS) {
			lardKg += fatKg * Curing.LARD_YIELD;
			fatKg = 0;
			renderDays = 0;
		}
		setChanged();
	}

	@Override
	public void serverTick(ServerLevel level) {
		if (level.getGameTime() % 20 == 0) {
			run(level, 20);
		}
	}

	@Override
	public String readout() {
		return String.format(Locale.ROOT, "Lard kettle: %.0f kg fat%s, %.1f kg lard", fatKg,
				level instanceof ServerLevel s && !heated(s) && fatKg > 0 ? " (NO FIRE BELOW)" : fatKg > 0 ? " rendering" : "", lardKg);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putDouble("Fat", fatKg);
		out.putDouble("Lard", lardKg);
		out.putDouble("Render", renderDays);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		fatKg = in.getDoubleOr("Fat", 0);
		lardKg = in.getDoubleOr("Lard", 0);
		renderDays = in.getDoubleOr("Render", 0);
	}
}
