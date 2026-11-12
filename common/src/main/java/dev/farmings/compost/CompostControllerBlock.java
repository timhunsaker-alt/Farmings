package dev.farmings.compost;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The windrow controller, or (with {@code turner}) the turner. Right-click with an input loads it; empty-handed reads it and takes compost; sneak starts it. */
public class CompostControllerBlock extends BaseEntityBlock {
	private final boolean turner;

	public CompostControllerBlock(boolean turner, Properties properties) {
		super(properties);
		this.turner = turner;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return turner ? new TurnerEntity(pos, state) : new CompostControllerEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != CompostBlocks.CONTROLLER_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> ((CompostControllerEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof CompostControllerEntity c) || CompostControllerEntity.input(stack) == null) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			boolean bucket = stack.is(Items.WATER_BUCKET);
			ItemStack rest = c.accept(stack.copyWithCount(bucket ? 1 : stack.getCount()));
			if (bucket) {
				if (rest.isEmpty() && !player.hasInfiniteMaterials()) {
					player.setItemInHand(hand, new ItemStack(Items.BUCKET));
				}
			} else {
				int took = stack.getCount() - rest.getCount();
				boolean bottles = stack.is(dev.farmings.FarmingsItems.SPOILED_BOTTLE);
				stack.setCount(rest.getCount());
				if (bottles && took > 0 && !player.hasInfiniteMaterials()) {
					// the milk goes on the pile; the glass comes back
					ItemStack glass = new ItemStack(Items.GLASS_BOTTLE, took);
					if (!player.getInventory().add(glass)) {
						level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), glass));
					}
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			if (level.getBlockEntity(pos) instanceof CompostControllerEntity c) {
				if (player.isShiftKeyDown()) {
					c.start();
				}
				int n = c.takeCompostItems();
				if (n > 0) {
					ItemStack out = new ItemStack(CompostBlocks.COMPOST, n);
					if (!player.addItem(out)) {
						net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), out);
					}
				}
				player.sendSystemMessage(Component.literal(c.readout()));
			} else if (level.getBlockEntity(pos) instanceof TurnerEntity t) {
				player.sendSystemMessage(t.inspect());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
