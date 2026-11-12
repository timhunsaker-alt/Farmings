package dev.farmings.digester;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The digester tank at full size (docs/multiblocks/digester.md, "the full-size tank"): a 3 x 3 x 3 big part placed from one item, its main
 * cell at the bottom centre. The gas holder sits on its roof, the feed pit and digestate tank against its sides. A tank placed as one
 * block before this grows to full size where there is room (Wildcatter's big parts).
 */
public class DigesterTankBlock extends BigPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(3, 3, 3, 1, 1);

	public DigesterTankBlock(Properties properties) {
		super(properties, FOOTPRINT, "farmings_digester_tank");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new DigesterEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != DigesterBlocks.TANK_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> ((DigesterEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && BigParts.entity(level, pos) instanceof DigesterEntity d) {
			player.sendSystemMessage(Component.literal(d.readout()));
		}
		return InteractionResult.SUCCESS;
	}
}
