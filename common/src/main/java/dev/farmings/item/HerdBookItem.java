package dev.farmings.item;

import dev.farmings.entity.CattleEntity;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.Carcass;
import dev.farmings.husbandry.TimeScale;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

/**
 * The herd book (Phase 2 "Records"). Right-click an animal to record it: one page each, keyed by its herd ID, a later record replacing
 * the earlier one. Use it in the air to read it, as a vanilla written book. Holds up to {@link #MAX_ENTRIES} animals *(proposed)*.
 */
public class HerdBookItem extends Item {
	public static final int MAX_ENTRIES = 64;
	private static final String KEY = "HerdBook";

	public HerdBookItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof dev.farmings.health.HealthHost h)) {
			return InteractionResult.PASS;
		}
		if (player.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		// interactLivingEntity hands us a copy in creative; write to the stack in the hand.
		ItemStack held = player.getItemInHand(hand);
		boolean added = target instanceof CattleEntity cow ? record(held, cow) : put(held, h.herdId(), healthOnlyPage(h));
		player.sendOverlayMessage(Component.translatable(added ? "message.farmings.herd_book.recorded" : "message.farmings.herd_book.full", h.herdId()));
		return added ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer sp) {
			sp.openItemGui(player.getItemInHand(hand), hand);
		}
		return InteractionResult.SUCCESS;
	}

	/** Records the animal into the book; false if the book is full and she is not already in it. */
	public static boolean record(ItemStack book, CattleEntity cow) {
		return put(book, cow.herdId(), page(cow) + PAGE_BREAK + healthPage(cow));
	}

	/** A page break inside one entry: the entry takes two book pages (the animal, then its health). */
	public static final String PAGE_BREAK = "\f";

	/** The health section (docs/animal-health.md, "What the player sees"): cases, treatments, vaccines, cells, quarantine. */
	public static String healthPage(dev.farmings.health.HealthHost h) {
		StringBuilder b = new StringBuilder("Health ").append(h.herdId()).append('\n');
		var r = h.health();
		long now = h.animal().level().getGameTime();
		if (h.inMilk()) {
			b.append("SCC ").append(String.format(Locale.ROOT, "%,d", r.scc())).append(" CMT ").append(dev.farmings.husbandry.health.Scc.cmt(r.scc())).append('\n');
		}
		for (var e : r.vaccines().entrySet()) {
			b.append(e.getKey().label).append(" x").append(e.getValue().doses()).append(r.doseDue(e.getKey(), now, dev.farmings.health.Health.scale(h)::realDaysToTicks) ? " DUE" : "").append('\n');
		}
		if (now < r.milkUntil() || now < r.meatUntil()) {
			b.append(String.format(Locale.ROOT, "withheld: milk %.1f, meat %.1f gd", Math.max(0, r.milkUntil() - now) / 24000.0, Math.max(0, r.meatUntil() - now) / 24000.0)).append('\n');
		}
		for (String e : dev.farmings.health.Health.history(h)) {
			b.append(e).append('\n');
		}
		return b.toString();
	}

	/** Species other than cattle: who it is and its health. */
	public static String healthOnlyPage(dev.farmings.health.HealthHost h) {
		return "ID " + h.herdId() + "\n" + h.species().name().toLowerCase(Locale.ROOT) + "\n" + PAGE_BREAK + healthPage(h);
	}

	/** A herd check from the vet kit, as its own entry, replaced by the next check of the same housing. */
	public static boolean writeHerdCheck(ItemStack book, net.minecraft.core.BlockPos at, long tick, List<String> lines) {
		StringBuilder b = new StringBuilder("Herd check, day ").append(Math.floorDiv(tick, TimeScale.TICKS_PER_GAME_DAY)).append('\n')
				.append(at.toShortString()).append('\n');
		for (String l : lines) {
			b.append("- ").append(l).append('\n');
		}
		return put(book, "check@" + at.toShortString(), b.toString());
	}

	/** Puts one entry in the book, replacing the entry with the same key. */
	public static boolean put(ItemStack book, String id, String page) {
		List<String[]> entries = entries(book);
		boolean replaced = false;
		for (String[] e : entries) {
			if (e[0].equals(id)) {
				e[1] = page;
				replaced = true;
			}
		}
		if (!replaced) {
			if (entries.size() >= MAX_ENTRIES) {
				return false;
			}
			entries.add(new String[] {id, page});
		}
		ListTag list = new ListTag();
		for (String[] e : entries) {
			CompoundTag t = new CompoundTag();
			t.putString("Id", e[0]);
			t.putString("Page", e[1]);
			list.add(t);
		}
		CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> tag.put(KEY, list));
		List<Filterable<Component>> pages = new ArrayList<>();
		for (String[] e : entries) {
			for (String part : e[1].split(PAGE_BREAK)) {
				pages.add(Filterable.passThrough(Component.literal(part)));
			}
		}
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Herd Book"), "Farmings", 0, pages, true));
		return true;
	}

	/** Every entry as {id, page text}, in the order recorded. */
	public static List<String[]> entries(ItemStack book) {
		List<String[]> out = new ArrayList<>();
		CustomData data = book.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return out;
		}
		for (Tag t : data.copyTag().getListOrEmpty(KEY)) {
			if (t instanceof CompoundTag c) {
				out.add(new String[] {c.getStringOr("Id", ""), c.getStringOr("Page", "")});
			}
		}
		return out;
	}

	/** One page: who she is, her parents, her traits and her beef traits. Short lines, so it fits a book page. */
	public static String page(CattleEntity cow) {
		StringBuilder b = new StringBuilder();
		b.append("ID ").append(cow.herdId()).append('\n');
		b.append(cow.breedLabel()).append(", ").append(cow.sex().name().toLowerCase(Locale.ROOT)).append(cow.castrated() ? " (steer)" : "").append('\n');
		if (cow.genome() != null) {
			b.append("Coat ").append(cow.look().describe()).append('\n');
		}
		b.append("Born day ").append(Math.floorDiv(cow.birthTick(), TimeScale.TICKS_PER_GAME_DAY)).append('\n');
		b.append("Dam ").append(cow.damId().isEmpty() ? "founder" : cow.damId()).append('\n');
		b.append("Sire ").append(cow.sireId().isEmpty() ? "founder" : cow.sireId()).append('\n');
		b.append("Calves ").append(cow.lactations()).append(cow.freemartin() ? ", freemartin" : "").append('\n');
		for (Trait t : dev.farmings.genetics.Species.CATTLE.traits()) {
			b.append(t.id.replace('_', ' ')).append(' ').append(cow.score(t)).append('\n');
		}
		Carcass.Yield y = cow.carcass();
		b.append(String.format(Locale.ROOT, "dressing %.0f%%, marbling %d %s", 100 * y.carcassKg() / Math.max(1, cow.weightKg()), y.marbling(),
				y.grade().name().charAt(0) + y.grade().name().substring(1).toLowerCase(Locale.ROOT)));
		return b.toString();
	}
}
