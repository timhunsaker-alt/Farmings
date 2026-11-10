package dev.farmings.woolmill;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A wool-mill machine block. Right-click with what it takes to put it in, with a water bucket to fill a scouring bowl; with an empty hand
 * it gives its output, or reads it when there is none.
 */
public class WoolMachineBlock extends BaseEntityBlock {
	private final WoolMachine kind;

	public WoolMachineBlock(Properties properties, WoolMachine kind) {
		super(properties);
		this.kind = kind;
	}

	public WoolMachine kind() {
		return kind;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WoolMachineEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != WoolMillBlocks.MACHINE_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> ((WoolMachineEntity) e).serverTick(l);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof WoolMachineEntity m)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (stack.is(Items.WATER_BUCKET) && kind == WoolMachine.SCOURING_BOWL) {
			if (!level.isClientSide() && m.pourBucket()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
			}
			return InteractionResult.SUCCESS;
		}
		if (m.accepts(stack)) {
			if (!level.isClientSide()) {
				m.insertByHand(stack);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WoolMachineEntity m) {
			ItemStack out = m.takeOutput();
			if (out.isEmpty()) {
				player.sendOverlayMessage(Component.literal(m.readout()));
			} else if (!player.getInventory().add(out)) {
				net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, out);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
