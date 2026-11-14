package dev.farmings.client.manual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.farmings.Farmings;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * The Field Manual's pages as {@code tools/make_manual.py} writes them: {@code assets/farmings/manual/en_us/_index.json} and one file
 * a page. Read through the resource manager each time the screen opens, so a resource pack can replace pages.
 */
public final class ManualPages {
	/** A styled run: text (bold, italic, a link to a page) or an item icon. */
	public record Run(String text, boolean bold, boolean italic, String link, String icon) {
	}

	/** A block of a page: para, heading, list (items), icons, or grid (layers, key). */
	public record Block(String type, List<List<Run>> lines, List<String> icons, List<Layer> layers, Map<Character, String> key) {
	}

	public record Layer(String name, List<String> rows) {
	}

	public record Page(String id, String title, String chapter, String icon, List<Block> blocks) {
		/** Every word of the page, for search. */
		public String text() {
			StringBuilder b = new StringBuilder(title).append(' ');
			for (Block block : blocks) {
				for (List<Run> line : block.lines()) {
					for (Run r : line) {
						if (r.text() != null) {
							b.append(r.text()).append(' ');
						}
					}
				}
			}
			return b.toString().toLowerCase(Locale.ROOT);
		}
	}

	public record Chapter(String id, String title, List<String> pages) {
	}

	public final List<Chapter> chapters = new ArrayList<>();
	public final Map<String, Page> pages = new LinkedHashMap<>();

	public static ManualPages load(ResourceManager resources) {
		ManualPages out = new ManualPages();
		try {
			JsonObject index = read(resources, "_index");
			for (JsonElement c : index.getAsJsonArray("chapters")) {
				JsonObject co = c.getAsJsonObject();
				List<String> ids = new ArrayList<>();
				for (JsonElement p : co.getAsJsonArray("pages")) {
					String id = p.getAsString();
					try {
						out.pages.put(id, page(read(resources, id)));
						ids.add(id);
					} catch (Exception e) {
						Farmings.LOGGER.warn("Field Manual: page {} unreadable: {}", id, e.toString());
					}
				}
				out.chapters.add(new Chapter(co.get("id").getAsString(), co.get("title").getAsString(), ids));
			}
		} catch (Exception e) {
			Farmings.LOGGER.warn("Field Manual: no index: {}", e.toString());
		}
		return out;
	}

	/** Pages whose title or text holds every word of the query, in contents order. */
	public List<Page> search(String query) {
		String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
		List<Page> out = new ArrayList<>();
		for (Page p : pages.values()) {
			String text = p.text();
			boolean all = true;
			for (String w : words) {
				if (!w.isEmpty() && !text.contains(w)) {
					all = false;
					break;
				}
			}
			if (all) {
				out.add(p);
			}
		}
		return out;
	}

	private static JsonObject read(ResourceManager resources, String name) throws Exception {
		try (Reader r = resources.openAsReader(Farmings.id("manual/en_us/" + name + ".json"))) {
			return JsonParser.parseReader(r).getAsJsonObject();
		}
	}

	static Page page(JsonObject o) {
		List<Block> blocks = new ArrayList<>();
		for (JsonElement e : o.getAsJsonArray("blocks")) {
			JsonObject b = e.getAsJsonObject();
			String type = b.get("type").getAsString();
			List<List<Run>> lines = new ArrayList<>();
			List<String> icons = new ArrayList<>();
			List<Layer> layers = new ArrayList<>();
			Map<Character, String> key = new LinkedHashMap<>();
			switch (type) {
				case "para", "heading" -> lines.add(runs(b.getAsJsonArray("runs")));
				case "list" -> b.getAsJsonArray("items").forEach(i -> lines.add(runs(i.getAsJsonArray())));
				case "icons" -> b.getAsJsonArray("items").forEach(i -> icons.add(i.getAsString()));
				case "grid" -> {
					for (JsonElement l : b.getAsJsonArray("layers")) {
						List<String> rows = new ArrayList<>();
						l.getAsJsonObject().getAsJsonArray("rows").forEach(r -> rows.add(r.getAsString()));
						layers.add(new Layer(l.getAsJsonObject().get("name").getAsString(), rows));
					}
					b.getAsJsonObject("key").entrySet().forEach(k -> key.put(k.getKey().charAt(0), k.getValue().getAsString()));
				}
				default -> {
					continue;
				}
			}
			blocks.add(new Block(type, lines, icons, layers, key));
		}
		return new Page(o.get("id").getAsString(), o.get("title").getAsString(), o.get("chapter").getAsString(),
				o.has("icon") ? o.get("icon").getAsString() : "", blocks);
	}

	private static List<Run> runs(JsonArray a) {
		List<Run> out = new ArrayList<>();
		for (JsonElement e : a) {
			JsonObject r = e.getAsJsonObject();
			if (r.has("icon")) {
				out.add(new Run(null, false, false, null, r.get("icon").getAsString()));
			} else {
				out.add(new Run(r.get("t").getAsString(), r.has("b"), r.has("i"), r.has("link") ? r.get("link").getAsString() : null, null));
			}
		}
		return out;
	}
}
