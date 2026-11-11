package dev.farmings.digester;

import dev.wildcatter.block.FluidNode;
import dev.wildcatter.fluid.Product;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The gas holder on the digester (digester.md): offers its biogas (m3, 60% CH4) on every face to Wildcatter's steel pipe, through
 * Wildcatter's {@link FluidNode} contract (Wildcatter docs/biogas.md). A gas set on that pipe burns it, derated to 850 kW.
 */
public class GasHolderEntity extends BlockEntity implements FluidNode {
	public GasHolderEntity(BlockPos pos, BlockState state) {
		super(DigesterBlocks.HOLDER_ENTITY, pos, state);
	}

	private @Nullable DigesterEntity digesterBelow() {
		return level != null && level.getBlockEntity(worldPosition.below()) instanceof DigesterEntity t ? t : null;
	}

	public double m3() {
		DigesterEntity t = digesterBelow();
		return t == null ? 0 : t.digester().gasM3();
	}

	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return side == Direction.DOWN || m3() <= 0 ? null : Product.BIOGAS;
	}

	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		return product == Product.BIOGAS && side != Direction.DOWN && pull != Pull.TANK ? m3() : 0;
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		DigesterEntity t = digesterBelow();
		if (t == null || available(cell, side, product, pull) <= 0) {
			return 0;
		}
		double got = t.digester().takeGas(max);
		t.setChanged();
		return got;
	}
}
