package dev.farmings.piggery;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A heat lamp hung over a farrowing crate's creep (pig-barn.md): 250 W on Wildcatter power; right-click reads it. */
public class HeatLampBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(5, 4, 5, 11, 12, 11);

	public HeatLampBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HeatLampEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != PigBarnBlocks.HEAT_LAMP_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> ((HeatLampEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HeatLampEntity e) {
			player.sendOverlayMessage(Component.literal(e.readout()));
		}
		return InteractionResult.SUCCESS;
	}
}
