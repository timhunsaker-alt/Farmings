package dev.farmings.digester;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The digester's parts with a block entity. Right-click reads the digester; manure in hand goes into a feed pit. */
public class DigesterBlock extends BaseEntityBlock {
	public enum Kind { TANK, FEED_PIT, HOLDER, PRESS }

	public final Kind kind;

	public DigesterBlock(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return switch (kind) {
			case TANK -> new DigesterEntity(pos, state);
			case FEED_PIT -> new FeedPitEntity(pos, state);
			case HOLDER -> new GasHolderEntity(pos, state);
			case PRESS -> new PressEntity(pos, state);
		};
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		if (type == DigesterBlocks.TANK_ENTITY) {
			return (l, p, s, e) -> ((DigesterEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
		}
		if (type == DigesterBlocks.FEED_PIT_ENTITY) {
			return (l, p, s, e) -> ((FeedPitEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
		}
		if (type == DigesterBlocks.PRESS_ENTITY) {
			return (l, p, s, e) -> ((PressEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
		}
		return null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (kind != Kind.FEED_PIT || FeedPitEntity.kg(stack) <= 0 || !(level.getBlockEntity(pos) instanceof FeedPitEntity pit)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			stack.setCount(pit.accept(stack.copy()).getCount());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			BlockEntity be = level.getBlockEntity(pos);
			DigesterEntity d = be instanceof DigesterEntity t ? t : level.getBlockEntity(pos.below()) instanceof DigesterEntity t2 ? t2 : null;
			if (be instanceof PressEntity p) {
				player.sendSystemMessage(p.inspect());
			} else if (d != null) {
				player.sendSystemMessage(Component.literal(d.readout()));
			} else {
				player.sendSystemMessage(Component.literal("Digester part: set it against a digester tank"));
			}
		}
		return InteractionResult.SUCCESS;
	}
}
