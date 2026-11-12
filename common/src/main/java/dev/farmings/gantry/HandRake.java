package dev.farmings.gantry;

import dev.farmings.crops.FieldCrops;
import dev.farmings.crops.PerennialCropBlock;
import dev.farmings.farm.crops.FieldCrop;
import dev.farmings.farm.gantry.GantryTool;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Raking and gathering by hand with a hoe (field-gantry.md, "Hand raking"): a swath dried 1 real day is raked into a windrow, and a
 * windrow dried 1.5 real days is gathered as loose hay or straw (10 kg items; a block's lighter cut drops an item by its share, so the mass
 * holds on average). The drying clock is the gantry row's; a swath no loaded cab is timing counts as dry.
 */
public final class HandRake {
	public static final double LOOSE_KG = 10;

	private HandRake() {
	}

	public static InteractionResult use(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
		if (!stack.is(ItemTags.HOES)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		boolean perennial = state.getBlock() instanceof PerennialCropBlock;
		int stage = perennial ? state.getValue(PerennialCropBlock.SWATH) : state.getValue(SwathBlock.RAKED) ? 2 : 1;
		if (perennial && stage == 0) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		double dried = GantryCabEntity.dryingNear(level, pos);
		double need = stage == 1 ? GantryTool.RAKE_AFTER_DAYS : GantryTool.BALE_AFTER_DAYS;
		if (dried >= 0 && dried < need) {
			player.sendOverlayMessage(Component.literal(String.format(Locale.ROOT, "Still drying: %.1f of %.1f days", dried, need)));
			return InteractionResult.FAIL;
		}
		if (stage == 1) {
			level.setBlock(pos, perennial ? state.setValue(PerennialCropBlock.SWATH, 2) : state.setValue(SwathBlock.RAKED, true), Block.UPDATE_ALL);
		} else {
			double kg;
			Item item;
			if (perennial) {
				FieldCrop crop = ((PerennialCropBlock) state.getBlock()).crop();
				kg = crop.hayKg();
				item = crop == FieldCrop.ALFALFA ? FieldCrops.ALFALFA_HAY : FieldCrops.GRASS_HAY;
				level.setBlock(pos, state.setValue(PerennialCropBlock.SWATH, 0), Block.UPDATE_ALL);
			} else {
				kg = state.getValue(SwathBlock.KIND) == SwathBlock.Kind.OAT_STRAW ? FieldCrop.OATS.strawKg() : FieldCrop.BARLEY.strawKg();
				item = FieldCrops.STRAW;
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			int n = (int) Math.floor(kg / LOOSE_KG);
			if (level.getRandom().nextDouble() < kg / LOOSE_KG - n) {
				n++;
			}
			if (n > 0) {
				Block.popResource(level, pos, new ItemStack(item, n));
			}
		}
		stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
		return InteractionResult.SUCCESS;
	}
}
