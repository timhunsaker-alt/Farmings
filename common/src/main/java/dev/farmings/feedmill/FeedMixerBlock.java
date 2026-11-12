package dev.farmings.feedmill;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The TMR mixer at full size (docs/multiblocks/feed-mill.md, "the full-size mixer"): a 2 x 2 x 2 big part placed from one item. It takes
 * ingredients on any face (by hand, belt, auger or hopper) and hands rations on from any face. A mixer placed as one block before this
 * grows to full size where there is room.
 */
public class FeedMixerBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(2, 2, 2, 0, 0);

	public FeedMixerBlock(Properties properties) {
		super(properties, FOOTPRINT, "farmings_tmr_mixer");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new FeedMachineEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != FeedMillBlocks.MACHINE_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> ((FeedMachineEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(BigParts.entity(level, pos) instanceof FeedMachineEntity m) || !m.takes(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			ItemStack rest = m.accept(stack.copy());
			stack.setCount(rest.getCount());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && BigParts.entity(level, pos) instanceof FeedMachineEntity m) {
			player.sendSystemMessage(Component.literal(m.readout()));
		}
		return InteractionResult.SUCCESS;
	}
}
