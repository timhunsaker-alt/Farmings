package dev.farmings.item;

import dev.farmings.entity.SheepDogEntity;
import dev.farmings.husbandry.shed.PastureLayout;
import dev.farmings.husbandry.shed.ShedLayout;
import dev.farmings.shed.ShedBlocks;
import dev.farmings.shed.ShedControllerEntity;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The shepherd's whistle (docs/animals/sheep.md, "Sheep dog"). Sneak-use on a block sets the target: a sheep shed controller means its
 * pen, a pasture gate its pasture, any other block that spot. Sneak-use in the air picks the next command; use in the air blows it to
 * your tame dogs within 64 blocks.
 */
public class WhistleItem extends Item {
	private static final String KEY_COMMAND = "Command";
	private static final String KEY_TARGET = "Target";

	public WhistleItem(Properties properties) {
		super(properties);
	}

	public static SheepDogEntity.Command command(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		try {
			return d == null ? SheepDogEntity.Command.COME_BYE : SheepDogEntity.Command.valueOf(d.copyTag().getStringOr(KEY_COMMAND, "COME_BYE"));
		} catch (IllegalArgumentException e) {
			return SheepDogEntity.Command.COME_BYE;
		}
	}

	public static @Nullable BlockPos target(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		long t = d == null ? Long.MIN_VALUE : d.copyTag().getLongOr(KEY_TARGET, Long.MIN_VALUE);
		return t == Long.MIN_VALUE ? null : BlockPos.of(t);
	}

	public static void setCommand(ItemStack stack, SheepDogEntity.Command c) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(KEY_COMMAND, c.name()));
	}

	public static void setTarget(ItemStack stack, BlockPos p) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(KEY_TARGET, p.asLong()));
	}

	public static String name(SheepDogEntity.Command c) {
		return switch (c) {
			case COME_BYE -> "Come bye";
			case WALK_UP -> "Walk up";
			case LIE_DOWN -> "Lie down";
			case THATLL_DO -> "That'll do";
		};
	}

	/** Where a target block sends the flock: a shed's middle pen cell, a gate's nearest pasture spot, or the spot above the block. */
	public static BlockPos resolve(Level level, BlockPos clicked) {
		if (level.getBlockEntity(clicked) instanceof ShedControllerEntity shed && shed.check() != null && !shed.check().pen().isEmpty()) {
			ShedLayout.Pos p = shed.check().pen().get(shed.check().pen().size() / 2);
			return new BlockPos(p.x(), p.y(), p.z());
		}
		if (level.getBlockState(clicked).getBlock() == ShedBlocks.PASTURE_GATE) {
			for (BlockPos b : BlockPos.betweenClosed(clicked.offset(-16, -2, -16), clicked.offset(16, 2, 16))) {
				if (level.getBlockEntity(b) instanceof ShedControllerEntity shed && shed.pasture() != null && shed.pasture().found()) {
					PastureLayout.Spot best = null;
					double bd = Double.MAX_VALUE;
					for (PastureLayout.Spot s : shed.pasture().spots()) {
						double d = clicked.distSqr(new BlockPos(s.x(), s.y(), s.z()));
						if (d > 8 && d < bd) {     // a little way in, past the gate
							bd = d;
							best = s;
						}
					}
					if (best != null) {
						return new BlockPos(best.x(), best.y(), best.z());
					}
				}
			}
		}
		return clicked.above();
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null || !player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!ctx.getLevel().isClientSide()) {
			BlockPos t = resolve(ctx.getLevel(), ctx.getClickedPos());
			setTarget(ctx.getItemInHand(), t);
			player.sendOverlayMessage(Component.literal("Whistle target set: " + t.toShortString()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		SheepDogEntity.Command c = command(stack);
		if (player.isSecondaryUseActive()) {
			SheepDogEntity.Command next = SheepDogEntity.Command.values()[(c.ordinal() + 1) % SheepDogEntity.Command.values().length];
			setCommand(stack, next);
			player.sendOverlayMessage(Component.literal("Whistle: " + name(next)));
			return InteractionResult.SUCCESS;
		}
		int heard = blow(level, player, c, target(stack));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0F, 1.8F);
		player.sendOverlayMessage(Component.literal(name(c) + (heard == 0 ? ": no dog of yours heard it" : ": " + heard + " dog" + (heard == 1 ? "" : "s"))));
		return InteractionResult.SUCCESS;
	}

	/** Gives the command to the player's tame dogs within hearing; returns how many heard. */
	public static int blow(Level level, Player player, SheepDogEntity.Command c, @Nullable BlockPos target) {
		int heard = 0;
		for (SheepDogEntity dog : level.getEntitiesOfClass(SheepDogEntity.class, player.getBoundingBox().inflate(SheepDogEntity.HEAR_RANGE))) {
			if (dog.hear(c, target, player)) {
				heard++;
			}
		}
		return heard;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		BlockPos t = target(stack);
		out.accept(Component.literal("Command: " + name(command(stack))));
		out.accept(Component.literal("Target: " + (t == null ? "none (sneak-use on a block)" : t.toShortString())));
	}
}
