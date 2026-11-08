package dev.farmings.item;

import dev.farmings.FarmingsItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Right-click a stall or the alley: the fork gathers every manure item lying within two blocks into your inventory. One use, one wear. */
public class ManureForkItem extends Item {
	public static final int REACH = 2;

	public ManureForkItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockPos at = context.getClickedPos();
		AABB area = new AABB(at).inflate(REACH, 1, REACH);
		List<ItemEntity> manure = level.getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(FarmingsItems.MANURE));
		int gathered = 0;
		for (ItemEntity e : manure) {
			ItemStack stack = e.getItem().copy();
			gathered += stack.getCount();
			e.discard();
			if (!player.getInventory().add(stack)) {
				level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack));   // a full inventory: it lands at the player's feet
			}
		}
		if (gathered > 0) {
			context.getItemInHand().hurtAndBreak(1, player, context.getHand());
		}
		return InteractionResult.SUCCESS;
	}
}
