package dev.farmings.item;

import dev.farmings.entity.HorseEntity;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.TimeScale;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
 * The stud book (docs/animals/horses.md, "The stud book"), the herd book's pattern for horses. Right-click a horse to record it: a page
 * each, keyed by its herd ID, a later record replacing the earlier one. A page gives the horse, its genes and speed, its sire and dam,
 * and its grandparents when the parents have pages in the same book, and its race record. Use it in the air to read it. Holds up to
 * {@link #MAX_ENTRIES} horses.
 */
public class StudBookItem extends Item {
	public static final int MAX_ENTRIES = 64;
	private static final String KEY = "StudBook";

	public StudBookItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof HorseEntity horse)) {
			return InteractionResult.PASS;
		}
		if (player.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemStack held = player.getItemInHand(hand);
		boolean added = record(held, horse);
		player.sendOverlayMessage(Component.translatable(added ? "message.farmings.stud_book.recorded" : "message.farmings.stud_book.full", horse.herdId()));
		return added ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer sp) {
			sp.openItemGui(player.getItemInHand(hand), hand);
		}
		return InteractionResult.SUCCESS;
	}

	/** One stored entry: herd id, sire id, dam id, and the page. */
	public record Entry(String id, String sire, String dam, String page) {
	}

	/** Records the horse; false if the book is full and the horse is not already in it. */
	public static boolean record(ItemStack book, HorseEntity horse) {
		List<Entry> entries = entries(book);
		Entry mine = new Entry(horse.herdId(), horse.sireId(), horse.damId(), page(horse));
		boolean replaced = false;
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).id().equals(mine.id())) {
				entries.set(i, mine);
				replaced = true;
			}
		}
		if (!replaced) {
			if (entries.size() >= MAX_ENTRIES) {
				return false;
			}
			entries.add(mine);
		}
		ListTag list = new ListTag();
		for (Entry e : entries) {
			CompoundTag t = new CompoundTag();
			t.putString("Id", e.id());
			t.putString("Sire", e.sire());
			t.putString("Dam", e.dam());
			t.putString("Page", e.page());
			list.add(t);
		}
		CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> tag.put(KEY, list));
		List<Filterable<Component>> pages = new ArrayList<>();
		for (Entry e : entries) {
			pages.add(Filterable.passThrough(Component.literal(e.page() + pedigree(entries, e))));
		}
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Stud Book"), "Farmings", 0, pages, true));
		return true;
	}

	/** Every entry, in the order recorded. */
	public static List<Entry> entries(ItemStack book) {
		List<Entry> out = new ArrayList<>();
		CustomData data = book.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return out;
		}
		for (Tag t : data.copyTag().getListOrEmpty(KEY)) {
			if (t instanceof CompoundTag c) {
				out.add(new Entry(c.getStringOr("Id", ""), c.getStringOr("Sire", ""), c.getStringOr("Dam", ""), c.getStringOr("Page", "")));
			}
		}
		return out;
	}

	/** The pedigree lines for an entry: sire and dam, each with their own sire and dam when the book has them. */
	public static String pedigree(List<Entry> entries, Entry e) {
		StringBuilder b = new StringBuilder("\nSire ").append(parent(entries, e.sire())).append("\nDam ").append(parent(entries, e.dam()));
		return b.toString();
	}

	private static String parent(List<Entry> entries, String id) {
		if (id.isEmpty()) {
			return "founder";
		}
		for (Entry p : entries) {
			if (p.id().equals(id)) {
				return id + " (by " + (p.sire().isEmpty() ? "founder" : p.sire()) + " out of " + (p.dam().isEmpty() ? "founder" : p.dam()) + ")";
			}
		}
		return id;
	}

	/** One page: the horse, its genes and speed, and its race record. Short lines, so it fits a book page. */
	public static String page(HorseEntity h) {
		StringBuilder b = new StringBuilder();
		b.append("ID ").append(h.herdId()).append(h.hasCustomName() ? " \"" + h.getCustomName().getString() + "\"" : "").append('\n');
		b.append(h.breedLabel()).append(", ").append(h.stage().label()).append('\n');
		if (h.genome() != null) {
			b.append(h.look().describe()).append('\n');
			b.append("MSTN ").append(h.genes().mstn()).append('\n');
		}
		b.append(String.format(Locale.ROOT, "%.1f years, born day %d", h.ageYears(), Math.floorDiv(h.level().getGameTime(), TimeScale.TICKS_PER_GAME_DAY)
				- (long) Math.floor(h.ageRealDays() / TimeScale.REAL_DAYS_PER_GAME_DAY))).append('\n');
		b.append(String.format(Locale.ROOT, "speed %d, stamina %d", h.score(Trait.SPEED), h.score(Trait.STAMINA))).append('\n');
		b.append(String.format(Locale.ROOT, "fitness %.0f%%", h.fitness() * 100)).append('\n');
		b.append(String.format(Locale.ROOT, "%d race%s, %d win%s", h.races(), h.races() == 1 ? "" : "s", h.wins(), h.wins() == 1 ? "" : "s"));
		for (Map.Entry<Integer, Integer> e : h.bests().entrySet()) {
			b.append(String.format(Locale.ROOT, "\nbest %,d m %.2f s", e.getKey(), e.getValue() / 20.0));
		}
		return b.toString();
	}
}
