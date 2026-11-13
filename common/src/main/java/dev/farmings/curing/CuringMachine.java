package dev.farmings.curing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** What the curing blocks share: a once-a-second step, using an item on them, using them empty-handed, a readout. */
public interface CuringMachine {
	void serverTick(ServerLevel level);

	/** The item in hand goes in; true if it was taken (the caller shrinks the stack). */
	boolean insert(ItemStack stack, ServerLevel level);

	/** An empty hand takes what is ready; returns it (may be empty). */
	ItemStack take(ServerLevel level);

	String readout();
}
