package dev.farmings.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Udder balm made from lanolin (docs/wool.md): rubbed on a hurt cow or sheep it heals 4 health *(proposed)* and is used up. Lanolin is
 * the base of real udder and teat salves; how much it heals is a game value.
 */
public class BalmItem extends Item {
	public static final float HEALS = 4.0F;

	public BalmItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		boolean farm = target instanceof dev.farmings.entity.CattleEntity || target instanceof dev.farmings.entity.SheepEntity;
		if (!farm || target.getHealth() >= target.getMaxHealth()) {
			return InteractionResult.PASS;
		}
		if (!player.level().isClientSide()) {
			target.heal(HEALS);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
