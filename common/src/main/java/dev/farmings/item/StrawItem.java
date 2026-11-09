package dev.farmings.item;

import dev.farmings.FarmingsItems;
import dev.farmings.entity.CattleEntity;
import dev.farmings.genetics.Genome;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.Stage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Artificial insemination (docs/animals/breeding.md, approved 2026-11-08). An empty straw, used on a grown bull, becomes a semen straw
 * holding his genome. A semen straw, used on a cow in heat, inseminates her and is used up. Straws never expire *(proposed)*.
 */
public class StrawItem extends Item {
	private static final String KEY_GENOME = "Genome";
	/** The bull's coat genes (VisualGenome.save); a straw filled before coat genes existed has none, and the cow uses his genome's. */
	private static final String KEY_COAT = "Coat";
	private static final String KEY_SIRE = "Sire";
	private final boolean filled;

	public StrawItem(Properties properties, boolean filled) {
		super(properties);
		this.filled = filled;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof CattleEntity cattle)) {
			return InteractionResult.PASS;
		}
		if (player.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		return filled ? inseminate(stack, player, cattle) : collect(stack, player, cattle);
	}

	private InteractionResult collect(ItemStack stack, Player player, CattleEntity bull) {
		Genome g = bull.genome();
		if (bull.sex() != Sex.MALE || bull.stage() != Stage.BULL || g == null) {
			player.sendOverlayMessage(Component.translatable("message.farmings.straw.not_a_bull"));
			return InteractionResult.FAIL;
		}
		ItemStack full = new ItemStack(FarmingsItems.SEMEN_STRAW);
		CustomData.update(DataComponents.CUSTOM_DATA, full, tag -> {
			tag.putString(KEY_GENOME, g.encode());
			tag.putIntArray(KEY_COAT, bull.visualGenome().save());
			tag.putString(KEY_SIRE, bull.getUUID().toString());
		});
		stack.consume(1, player);
		if (!player.getInventory().add(full)) {
			player.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), full));
		}
		return InteractionResult.SUCCESS;
	}

	private InteractionResult inseminate(ItemStack stack, Player player, CattleEntity cow) {
		Genome sire = genomeOf(stack);
		if (sire == null || cow.sex() != Sex.FEMALE || !cow.canConceive()) {
			player.sendOverlayMessage(Component.translatable("message.farmings.straw.not_in_heat"));
			return InteractionResult.FAIL;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		dev.farmings.genetics.VisualGenome coat = data == null ? null : data.copyTag().getIntArray(KEY_COAT).map(dev.farmings.genetics.VisualGenome::load).orElse(null);
		if (!(coat == null ? cow.conceive(sire, cow.level().getGameTime()) : cow.conceive(sire, coat, cow.level().getGameTime()))) {
			return InteractionResult.FAIL;
		}
		if (cow.owner() == null) {
			cow.setOwner(player.getUUID());
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	/** The sire's genome stored in a semen straw, or null for an empty or damaged one. */
	public static Genome genomeOf(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		String text = data.copyTag().getStringOr(KEY_GENOME, "");
		if (text.isEmpty()) {
			return null;
		}
		try {
			return Genome.decode(text);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
