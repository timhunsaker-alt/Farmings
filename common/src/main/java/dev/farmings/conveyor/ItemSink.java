package dev.farmings.conveyor;

import net.minecraft.world.item.ItemStack;

/** A Farmings machine a belt or auger run can deliver into (silo blower, grain bin, mill, mixer, baler). */
public interface ItemSink {
	/** Takes what it can of {@code stack}; returns what it could not take (empty if it took it all). Must not change {@code stack}. */
	ItemStack accept(ItemStack stack);
}
