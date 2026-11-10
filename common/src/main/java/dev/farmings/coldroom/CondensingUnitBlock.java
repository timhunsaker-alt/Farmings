package dev.farmings.coldroom;

import dev.wildcatter.energy.Inspectable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The walk-in fridge's condensing unit (compressor and condenser), outside against a wall, on Wildcatter three-phase power. */
public class CondensingUnitBlock extends BaseEntityBlock {
	public CondensingUnitBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CondenserEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof Inspectable unit) {
			player.sendOverlayMessage(unit.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
