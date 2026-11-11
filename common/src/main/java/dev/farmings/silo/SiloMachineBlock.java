package dev.farmings.silo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The silo's and bin's parts with a block entity: blower, unloader, bin hopper, bin roof. Right-click reads the tower. */
public class SiloMachineBlock extends BaseEntityBlock {
	public enum Kind { BLOWER, UNLOADER, HOPPER, ROOF }

	public final Kind kind;

	public SiloMachineBlock(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return switch (kind) {
			case BLOWER -> new BlowerEntity(pos, state);
			case UNLOADER -> new SiloEntity(pos, state);
			case HOPPER -> new GrainBinEntity(pos, state);
			case ROOF -> new BinRoofEntity(pos, state);
		};
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		if (type == SiloBlocks.UNLOADER_ENTITY) {
			return (l, p, s, e) -> ((SiloEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
		}
		if (type == SiloBlocks.HOPPER_ENTITY) {
			return (l, p, s, e) -> ((GrainBinEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
		}
		return null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			BlockEntity be = level.getBlockEntity(pos);
			String text = be instanceof SiloEntity s ? s.readout() : be instanceof GrainBinEntity b ? b.readout()
					: be instanceof BlowerEntity bl ? bl.inspect().getString() : "Grain bin roof: feed grain into its fill hole";
			player.sendSystemMessage(Component.literal(text));
		}
		return InteractionResult.SUCCESS;
	}
}
