package dev.farmings.client.manual;

import dev.farmings.client.manual.ManualPages.Block;
import dev.farmings.client.manual.ManualPages.Chapter;
import dev.farmings.client.manual.ManualPages.Layer;
import dev.farmings.client.manual.ManualPages.Page;
import dev.farmings.client.manual.ManualPages.Run;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Field Manual screen (docs/field-manual.md, "The screen"): contents and search on the left page, the page being read on the right,
 * split into sheets. Drawn as filled shapes, no texture.
 */
public class ManualScreen extends Screen {
	static final int BOOK_W = 300;
	static final int BOOK_H = 196;
	static final int PAGE_W = 128;
	static final int MARGIN = 10;
	static final int TEXT_TOP = 30;
	static final int TEXT_BOTTOM = 26;

	static final int PAPER = 0xFFF3E9D2;
	static final int EDGE = 0xFF6B4F2A;
	static final int SPINE = 0xFF8A6A3C;
	static final int INK = 0xFF3A2E1E;
	static final int FAINT = 0xFF8C7B5E;
	static final int LINK = 0xFF2457A6;
	static final int HEAD = 0xFF2F5D2A;
	static final int MARK = 0xFFE2D3AE;

	/** Where the reader was, kept for the game session only. */
	private static String lastPage;
	private static int lastSheet;
	private static final Set<String> OPEN = new HashSet<>();

	private ManualPages manual;
	private EditBox search;
	private String pageId;
	private int sheet;
	private int contentsSheet;
	private final Deque<String> back = new ArrayDeque<>();
	private List<List<Line>> sheets = List.of();
	private final List<Hit> hits = new ArrayList<>();
	private int left;
	private int top;
	private int groups;

	/** Something drawn on a line of the right page. */
	record El(int x, Component text, String link, ItemStack icon, int size) {
	}

	/** A laid-out line; lines with the same non-zero {@code group} (a grid layer) stay on one sheet when they fit. */
	record Line(int height, List<El> els, int group) {
		Line(int height, List<El> els) {
			this(height, els, 0);
		}
	}

	record Hit(int x, int y, int w, int h, Runnable action) {
		boolean in(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	public ManualScreen() {
		super(Component.translatable("item.farmings.field_manual"));
	}

	/** The page being read, for tests and the client scene. */
	public String pageId() {
		return pageId;
	}

	public int sheetCount() {
		return sheets.size();
	}

	@Override
	protected void init() {
		if (manual == null) {
			manual = ManualPages.load(minecraft.getResourceManager());
			String start = lastPage != null && manual.pages.containsKey(lastPage) ? lastPage : firstPage();
			open(start, lastPage != null && lastPage.equals(start) ? lastSheet : 0, false);
		}
		left = (width - BOOK_W) / 2;
		top = (height - BOOK_H) / 2;
		String query = search == null ? "" : search.getValue();
		search = new EditBox(font, left + MARGIN, top + MARGIN, PAGE_W, 14, Component.translatable("gui.farmings.manual.search"));
		search.setHint(Component.translatable("gui.farmings.manual.search"));
		search.setMaxLength(40);
		search.setValue(query);
		search.setResponder(q -> contentsSheet = 0);
		addRenderableWidget(search);
		layout();
	}

	private String firstPage() {
		for (Chapter c : manual.chapters) {
			if (!c.pages().isEmpty()) {
				return c.pages().get(0);
			}
		}
		return null;
	}

	/** Opens a page; {@code remember} pushes the current one on the Back list. */
	public void open(String id, int toSheet, boolean remember) {
		if (id == null || !manual.pages.containsKey(id)) {
			return;
		}
		if (remember && pageId != null && !pageId.equals(id)) {
			back.push(pageId);
			while (back.size() > 32) {
				back.removeLast();
			}
		}
		pageId = id;
		sheet = toSheet;
		OPEN.add(manual.pages.get(id).chapter());
		if (font != null) {
			layout();
		}
	}

	/** Opens a page by id from outside (the client scene); false if there is no such page. */
	public boolean openPage(String id) {
		if (manual == null || !manual.pages.containsKey(id)) {
			return false;
		}
		open(id, 0, true);
		return true;
	}

	public void search(String query) {
		search.setValue(query);
	}

	public void turn(int by) {
		sheet = Math.max(0, Math.min(sheets.size() - 1, sheet + by));
		lastSheet = sheet;
	}

	// ---- layout of the right page ----

	private void layout() {
		Page page = pageId == null ? null : manual.pages.get(pageId);
		List<Line> lines = new ArrayList<>();
		if (page != null) {
			for (Block b : page.blocks()) {
				switch (b.type()) {
					case "heading" -> {
						lines.add(new Line(3, List.of()));
						flow(lines, b.lines().get(0), 0, Style.EMPTY.withBold(true).withColor(HEAD), null);
					}
					case "para" -> flow(lines, b.lines().get(0), 0, Style.EMPTY, null);
					case "list" -> {
						for (List<Run> item : b.lines()) {
							flow(lines, item, 8, Style.EMPTY, Component.literal("•"));
						}
					}
					case "icons" -> iconRow(lines, b.icons());
					case "grid" -> grid(lines, b);
					default -> {
					}
				}
				lines.add(new Line(4, List.of()));
			}
		}
		int room = BOOK_H - TEXT_TOP - TEXT_BOTTOM;
		List<List<Line>> out = new ArrayList<>();
		List<Line> cur = new ArrayList<>();
		int used = 0;
		for (int i = 0; i < lines.size(); i++) {
			Line l = lines.get(i);
			int need = l.height();
			if (l.group() != 0 && (i == 0 || lines.get(i - 1).group() != l.group())) {
				int whole = 0;
				for (int j = i; j < lines.size() && lines.get(j).group() == l.group(); j++) {
					whole += lines.get(j).height();
				}
				if (whole <= room) {
					need = whole;
				}
			}
			if (used + need > room && !cur.isEmpty()) {
				out.add(cur);
				cur = new ArrayList<>();
				used = 0;
				if (l.els().isEmpty()) {
					continue;
				}
			}
			cur.add(l);
			used += l.height();
		}
		if (!cur.isEmpty() || out.isEmpty()) {
			out.add(cur);
		}
		sheets = out;
		sheet = Math.max(0, Math.min(sheet, sheets.size() - 1));
		lastPage = pageId;
		lastSheet = sheet;
	}

	/** Word-wraps runs into lines; an inline icon makes its line 18 tall. */
	private void flow(List<Line> lines, List<Run> runs, int indent, Style base, Component bullet) {
		List<El> els = new ArrayList<>();
		boolean tall = false;
		int x = indent;
		if (bullet != null) {
			els.add(new El(0, bullet, null, null, 0));
		}
		for (Run r : runs) {
			if (r.icon() != null) {
				if (x + 16 > PAGE_W && x > indent) {
					lines.add(new Line(tall ? 18 : 10, els));
					els = new ArrayList<>();
					tall = false;
					x = indent;
				}
				els.add(new El(x, null, null, stack(r.icon()), 16));
				tall = true;
				x += 17;
				continue;
			}
			Style style = base.withBold(r.bold() || Boolean.TRUE.equals(base.isBold()) ? Boolean.TRUE : null).withItalic(r.italic() ? Boolean.TRUE : null);
			if (r.link() != null) {
				style = style.withColor(LINK).withUnderlined(true);
			}
			for (String word : r.text().split("(?<= )")) {
				Component c = Component.literal(word).withStyle(style);
				int w = font.width(c);
				if (x + font.width(word.stripTrailing()) > PAGE_W && x > indent) {
					lines.add(new Line(tall ? 18 : 10, els));
					els = new ArrayList<>();
					tall = false;
					x = indent;
					if (word.isBlank()) {
						continue;
					}
				}
				els.add(new El(x, c, r.link(), null, 0));
				x += w;
			}
		}
		if (!els.isEmpty()) {
			lines.add(new Line(tall ? 18 : 10, els));
		}
	}

	private void iconRow(List<Line> lines, List<String> icons) {
		List<El> els = new ArrayList<>();
		int x = 0;
		for (String id : icons) {
			if (x + 16 > PAGE_W) {
				lines.add(new Line(18, els));
				els = new ArrayList<>();
				x = 0;
			}
			els.add(new El(x, null, null, stack(id), 16));
			x += 18;
		}
		if (!els.isEmpty()) {
			lines.add(new Line(18, els));
		}
	}

	/** A multiblock's layers as block icons, shrunk to fit; under 8 px a cell, the letters and key instead. */
	private void grid(List<Line> lines, Block b) {
		int cols = 1;
		for (Layer l : b.layers()) {
			for (String row : l.rows()) {
				cols = Math.max(cols, row.length());
			}
		}
		int cell = Math.min(16, PAGE_W / cols);
		for (Layer l : b.layers()) {
			int group = ++groups;
			lines.add(new Line(10, List.of(new El(0, Component.literal(l.name()).withStyle(Style.EMPTY.withItalic(true).withColor(FAINT)), null, null, 0)), group));
			for (String row : l.rows()) {
				List<El> els = new ArrayList<>();
				if (cell >= 8) {
					for (int i = 0; i < row.length(); i++) {
						String id = b.key().get(row.charAt(i));
						if (id != null) {
							els.add(new El(i * cell, null, null, stack(id), cell));
						}
					}
					lines.add(new Line(cell, els, group));
				} else {
					lines.add(new Line(10, List.of(new El(0, Component.literal(row), null, null, 0)), group));
				}
			}
		}
		if (cell < 8) {
			b.key().forEach((ch, id) -> lines.add(new Line(10, List.of(new El(0, Component.literal(ch + " ").append(stack(id).getHoverName()), null, null, 0)))));
		}
	}

	static ItemStack stack(String id) {
		Identifier key = Identifier.tryParse(id);
		Item item = key == null ? Items.BARRIER : BuiltInRegistries.ITEM.getValue(key);
		return new ItemStack(item == Items.AIR ? Items.BARRIER : item);
	}

	// ---- drawing ----

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
		hits.clear();
		g.fill(left - 4, top - 4, left + BOOK_W + 4, top + BOOK_H + 4, EDGE);
		g.fill(left, top, left + BOOK_W, top + BOOK_H, PAPER);
		g.fill(left + BOOK_W / 2 - 2, top, left + BOOK_W / 2 + 2, top + BOOK_H, SPINE);
		contents(g, mx, my);
		rightPage(g, mx, my);
		super.extractRenderState(g, mx, my, partial);
	}

	private void contents(GuiGraphicsExtractor g, int mx, int my) {
		int x = left + MARGIN;
		int y0 = top + TEXT_TOP;
		int rows = (BOOK_H - TEXT_TOP - TEXT_BOTTOM) / 10;
		List<Object[]> entries = new ArrayList<>();
		String query = search.getValue().trim();
		if (!query.isEmpty()) {
			List<Page> found = manual.search(query);
			if (found.isEmpty()) {
				entries.add(new Object[] {Component.translatable("gui.farmings.manual.no_results").withStyle(Style.EMPTY.withItalic(true)), null, 0});
			}
			for (Page p : found) {
				entries.add(new Object[] {Component.literal(p.title()), p.id(), 0});
			}
		} else {
			for (Chapter c : manual.chapters) {
				boolean open = OPEN.contains(c.id());
				entries.add(new Object[] {Component.literal((open ? "▾ " : "▸ ") + c.title()).withStyle(Style.EMPTY.withBold(true)), "#" + c.id(), 0});
				if (open) {
					for (String id : c.pages()) {
						entries.add(new Object[] {Component.literal(manual.pages.get(id).title()), id, 10});
					}
				}
			}
		}
		int sheetsOfContents = Math.max(1, (entries.size() + rows - 1) / rows);
		contentsSheet = Math.min(contentsSheet, sheetsOfContents - 1);
		for (int i = 0; i < rows && contentsSheet * rows + i < entries.size(); i++) {
			Object[] e = entries.get(contentsSheet * rows + i);
			int y = y0 + i * 10;
			String target = (String) e[1];
			int indent = (int) e[2];
			if (target != null && target.equals(pageId)) {
				g.fill(x - 2, y - 1, x + PAGE_W + 2, y + 9, MARK);
			}
			Component label = (Component) e[0];
			boolean hover = target != null && mx >= x && mx < x + PAGE_W && my >= y - 1 && my < y + 9;
			g.text(font, label, x + indent, y, hover ? LINK : INK, false);
			if (target != null) {
				hits.add(new Hit(x, y - 1, PAGE_W, 10, () -> {
					if (target.startsWith("#")) {
						String chapter = target.substring(1);
						if (!OPEN.remove(chapter)) {
							OPEN.add(chapter);
						}
					} else {
						open(target, 0, true);
					}
				}));
			}
		}
		if (sheetsOfContents > 1) {
			int by = top + BOOK_H - 18;
			arrow(g, mx, my, x, by, "‹", contentsSheet > 0, () -> contentsSheet--);
			g.centeredText(font, (contentsSheet + 1) + " / " + sheetsOfContents, x + PAGE_W / 2, by, FAINT);
			arrow(g, mx, my, x + PAGE_W - 8, by, "›", contentsSheet < sheetsOfContents - 1, () -> contentsSheet++);
		}
	}

	private void rightPage(GuiGraphicsExtractor g, int mx, int my) {
		int x0 = left + BOOK_W / 2 + MARGIN;
		Page page = pageId == null ? null : manual.pages.get(pageId);
		if (page == null) {
			g.text(font, Component.translatable("gui.farmings.manual.empty"), x0, top + MARGIN + 4, FAINT, false);
			return;
		}
		if (!page.icon().isEmpty()) {
			g.item(stack(page.icon()), x0 + PAGE_W - 16, top + MARGIN);
		}
		g.text(font, Component.literal(page.title()).withStyle(Style.EMPTY.withBold(true)), x0, top + MARGIN + 4, HEAD, false);
		int y = top + TEXT_TOP;
		for (Line line : sheets.get(sheet)) {
			for (El e : line.els()) {
				int ex = x0 + e.x();
				if (e.icon() != null) {
					float s = e.size() / 16f;
					g.pose().pushMatrix();
					g.pose().translate(ex, y);
					g.pose().scale(s, s);
					g.item(e.icon(), 0, 0);
					g.pose().popMatrix();
					if (mx >= ex && mx < ex + e.size() && my >= y && my < y + e.size()) {
						g.setTooltipForNextFrame(font, e.icon(), mx, my);
					}
				} else {
					int ty = y + (line.height() - 8) / 2;
					g.text(font, e.text(), ex, ty, INK, false);
					if (e.link() != null) {
						String link = e.link();
						hits.add(new Hit(ex, ty - 1, font.width(e.text()), 10, () -> open(link, 0, true)));
					}
				}
			}
			y += line.height();
		}
		int by = top + BOOK_H - 18;
		if (sheets.size() > 1) {
			arrow(g, mx, my, x0 + PAGE_W - 58, by, "‹", sheet > 0, () -> turn(-1));
			g.centeredText(font, (sheet + 1) + " / " + sheets.size(), x0 + PAGE_W - 29, by, FAINT);
			arrow(g, mx, my, x0 + PAGE_W - 6, by, "›", sheet < sheets.size() - 1, () -> turn(1));
		}
		if (!back.isEmpty()) {
			Component label = Component.literal("‹ ").append(Component.translatable("gui.farmings.manual.back"));
			int bx = x0;
			int bY = by;
			boolean hover = mx >= bx && mx < bx + font.width(label) && my >= bY - 1 && my < bY + 9;
			g.text(font, label, bx, bY, hover ? LINK : FAINT, false);
			hits.add(new Hit(bx, bY - 1, font.width(label), 10, () -> open(back.pop(), 0, false)));
		}
	}

	private void arrow(GuiGraphicsExtractor g, int mx, int my, int x, int y, String glyph, boolean active, Runnable action) {
		if (!active) {
			return;
		}
		boolean hover = mx >= x - 2 && mx < x + 10 && my >= y - 2 && my < y + 10;
		g.text(font, glyph, x, y, hover ? LINK : INK, false);
		hits.add(new Hit(x - 2, y - 2, 12, 12, action));
	}

	// ---- input ----

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		for (Hit h : List.copyOf(hits)) {
			if (h.in(event.x(), event.y())) {
				search.setFocused(false);
				h.action().run();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!search.isFocused()) {
			if (minecraft.options.keyInventory.matches(event)) {
				onClose();
				return true;
			}
			if (event.key() == 262) {
				turn(1);
				return true;
			}
			if (event.key() == 263) {
				turn(-1);
				return true;
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		if (mx > left + BOOK_W / 2) {
			turn(dy < 0 ? 1 : -1);
		} else {
			contentsSheet = Math.max(0, contentsSheet + (dy < 0 ? 1 : -1));
		}
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
