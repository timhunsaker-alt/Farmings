package dev.farmings.curing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

/** The four curing blocks: use an item to put it in, an empty hand to take what is ready (or read the block when nothing is). */
public class CuringMachineBlock extends BaseEntityBlock {
	private final int kind;

	public CuringMachineBlock(Properties properties, int kind) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return switch (kind) {
			case 0 -> new CuringRackEntity(pos, state);
			case 1 -> new SmokehouseEntity(pos, state);
			case 2 -> new LardKettleEntity(pos, state);
			default -> new MeatGrinderEntity(pos, state);
		};
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return (l, p, s, e) -> {
			if (e instanceof CuringMachine m) {
				m.serverTick((ServerLevel) l);
			}
		};
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof CuringMachine m) {
			if (m.insert(stack, (ServerLevel) level)) {
				return InteractionResult.SUCCESS;
			}
			player.sendOverlayMessage(Component.literal(m.readout()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CuringMachine m) {
			ItemStack out = m.take((ServerLevel) level);
			if (!out.isEmpty()) {
				if (!player.getInventory().add(out)) {
					player.spawnAtLocation((ServerLevel) level, out);
				}
			} else {
				player.sendSystemMessage(Component.literal(m.readout()));
			}
		}
		return InteractionResult.SUCCESS;
	}
}
