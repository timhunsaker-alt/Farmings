package dev.farmings.conveyor;

import dev.farmings.farm.conveyor.Travel;
import net.minecraft.world.item.ItemStack;

/** The two kinds of run (docs/conveyors.md): a belt for anything, and an auger for grain, meal and seed. */
public enum ConveyorKind {
	/** 2 m/s (Tailings' belt); level runs only in this pass. */
	BELT(2.0),
	/** 1 m/s along the tube (proposed: no auger flight speed found); any direction, up too. */
	AUGER(1.0);

	public final double metresPerSecond;

	ConveyorKind(double metresPerSecond) {
		this.metresPerSecond = metresPerSecond;
	}

	public double watts(int metres) {
		return this == BELT ? Travel.beltWatts(metres) : Travel.augerWatts(metres);
	}

	public boolean carries(ItemStack stack) {
		if (this == BELT) {
			return true;
		}
		return dev.farmings.entity.Wild.grain(stack) || stack.is(dev.farmings.crops.FieldCrops.CORN_GRAIN) || stack.is(dev.farmings.crops.FieldCrops.OAT_GRAIN)
				|| stack.is(dev.farmings.crops.FieldCrops.BARLEY_GRAIN) || ConveyorBlocks.isMeal(stack) || dev.farmings.crops.FieldCrops.SEEDS.containsValue(stack.getItem());
	}
}
