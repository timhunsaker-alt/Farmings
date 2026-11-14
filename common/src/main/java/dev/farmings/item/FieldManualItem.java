package dev.farmings.item;

import dev.farmings.Farmings;
import dev.farmings.FarmingsItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Field Manual (docs/field-manual.md): use it to open the guide screen. Every player is given one on first join. The screen is
 * client-only; {@link Farmings#openManual} is set by the loader's client entry point, so this class never touches client code.
 */
public class FieldManualItem extends Item {
	/** Kept on the player once given, so a second join, death or relog gives nothing. */
	public static final String GIVEN_TAG = "farmings.manual_given";

	public FieldManualItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			Runnable open = Farmings.openManual;
			if (open != null) {
				open.run();
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Gives the manual the first time a player joins; true if it was given now. Dropped at the player's feet if the inventory is full. */
	public static boolean giveOnce(ServerPlayer player) {
		if (player.entityTags().contains(GIVEN_TAG)) {
			return false;
		}
		player.addTag(GIVEN_TAG);
		ItemStack manual = new ItemStack(FarmingsItems.FIELD_MANUAL);
		if (!player.getInventory().add(manual)) {
			player.spawnAtLocation(player.level(), manual);
		}
		return true;
	}
}
