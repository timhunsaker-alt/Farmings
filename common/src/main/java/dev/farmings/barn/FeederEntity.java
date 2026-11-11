package dev.farmings.barn;

import dev.farmings.FarmingsBlocks;
import dev.farmings.feed.Hay;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A hay feeder's contents, as kilograms of dry matter and the mixture's analysis (docs/multiblocks/barn.md; feed-mill.md): up to 4 hay
 * blocks of 85 kg each, or the same weight of any feed. A round bale (372 kg DM) goes in only when the feeder is empty, as into a ring
 * feeder (proposed).
 */
public class FeederEntity extends BlockEntity implements BigPartEntity {
	public static final int MAX_BLOCKS = 4;
	public static final double CAPACITY_KG = MAX_BLOCKS * Hay.BLOCK_FRESH_KG * Hay.DRY_MATTER_FRACTION;

	private final dev.farmings.feed.FeedMix mix = new dev.farmings.feed.FeedMix();
	private boolean grown;

	public FeederEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.FEEDER_ENTITY, pos, state);
	}

	public double dryMatterKg() {
		return mix.kg();
	}

	public dev.farmings.feed.Ration.Analysis analysis() {
		return mix.analysis();
	}

	/** Puts one feed item in if it is feed and there is room; returns whether it went in. */
	public boolean addFeed(net.minecraft.world.item.ItemStack stack) {
		dev.farmings.feedmill.FeedItems.Feed f = dev.farmings.feedmill.FeedItems.feed(stack);
		if (f == null) {
			return false;
		}
		boolean bale = f.kgAsFed() > 400;
		if (bale ? mix.kg() > 1e-6 : mix.kg() + f.kgDm() > CAPACITY_KG + 1e-9) {
			return false;
		}
		mix.add(f.kgDm(), f.analysis());
		setChanged();
		return true;
	}

	/** Hay blocks' worth, rounded down, for the readout. */
	public double blocks() {
		return mix.kg() / Hay.blockDryMatterKg();
	}

	/** Puts one hay block in if there is room for it; returns whether it went in. */
	public boolean addBlock() {
		if (mix.kg() + Hay.blockDryMatterKg() > CAPACITY_KG + 1e-9) {
			return false;
		}
		mix.add(Hay.blockDryMatterKg(), dev.farmings.feed.Feedstuff.GRASS_HAY.analysis());
		setChanged();
		return true;
	}

	/** Takes up to {@code kg} of dry matter; returns how much was there. */
	public double take(double kg) {
		double taken = mix.take(kg);
		if (taken > 0) {
			setChanged();
		}
		return taken;
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putDouble("dry_matter_kg", mix.kg());
		dev.farmings.feed.Ration.Analysis a = mix.analysis();
		output.putDouble("cp", a.cp());
		output.putDouble("ndf", a.ndf());
		output.putDouble("tdn", a.tdn());
		output.putDouble("forage", a.forage());
		output.putDouble("ca", a.ca());
		output.putDouble("premix", a.premix());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		double kg = Math.max(0, input.getDoubleOr("dry_matter_kg", 0));
		dev.farmings.feed.Ration.Analysis g = dev.farmings.feed.Feedstuff.GRASS_HAY.analysis();
		mix.set(kg, new dev.farmings.feed.Ration.Analysis(input.getDoubleOr("cp", g.cp()), input.getDoubleOr("ndf", g.ndf()), input.getDoubleOr("tdn", g.tdn()),
				input.getDoubleOr("forage", g.forage()), input.getDoubleOr("ca", g.ca()), input.getDoubleOr("premix", g.premix())));
	}
}
