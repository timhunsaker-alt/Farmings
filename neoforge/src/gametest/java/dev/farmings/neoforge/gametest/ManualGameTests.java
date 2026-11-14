package dev.farmings.neoforge.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.farmings.Farmings;
import dev.farmings.FarmingsItems;
import dev.farmings.item.FieldManualItem;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The Field Manual (docs/field-manual.md): given once, crafted from a book and wheat, and every page's items registered. */
final class ManualGameTests {
	private ManualGameTests() {
	}

	static void givenOnce(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.removeTag(FieldManualItem.GIVEN_TAG);
		player.getInventory().clearContent();
		boolean first = FieldManualItem.giveOnce(player);
		boolean second = FieldManualItem.giveOnce(player);
		int count = player.getInventory().countItem(FarmingsItems.FIELD_MANUAL);
		helper.assertTrue(first && !second && count == 1 && player.entityTags().contains(FieldManualItem.GIVEN_TAG),
				Component.literal("given first " + first + ", second " + second + ", manuals " + count));
		helper.succeed();
	}

	@SuppressWarnings("unchecked")
	static void recipe(GameTestHelper helper) {
		var holder = helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Farmings.id("field_manual")));
		helper.assertTrue(holder.isPresent(), Component.literal("no field_manual recipe"));
		CraftingRecipe recipe = ((RecipeHolder<CraftingRecipe>) holder.get()).value();
		CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.WHEAT)));
		ItemStack out = recipe.matches(input, helper.getLevel()) ? recipe.assemble(input) : ItemStack.EMPTY;
		CraftingInput herdBook = CraftingInput.of(2, 1, List.of(new ItemStack(Items.WRITABLE_BOOK), new ItemStack(Items.WHEAT)));
		helper.assertTrue(out.is(FarmingsItems.FIELD_MANUAL) && !recipe.matches(herdBook, helper.getLevel()),
				Component.literal("book + wheat gave " + out + "; writable book + wheat matches " + recipe.matches(herdBook, helper.getLevel())));
		helper.succeed();
	}

	static void pagesResolve(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		int pages = 0;
		JsonObject index = read("_index");
		helper.assertTrue(index != null, Component.literal("no manual index"));
		for (JsonElement c : index.getAsJsonArray("chapters")) {
			for (JsonElement id : c.getAsJsonObject().getAsJsonArray("pages")) {
				JsonObject page = read(id.getAsString());
				if (page == null) {
					problems.add(id.getAsString() + ": missing");
					continue;
				}
				pages++;
				List<String> items = new ArrayList<>();
				if (page.has("icon") && !page.get("icon").getAsString().isEmpty()) {
					items.add(page.get("icon").getAsString());
				}
				for (JsonElement b : page.getAsJsonArray("blocks")) {
					JsonObject block = b.getAsJsonObject();
					switch (block.get("type").getAsString()) {
						case "icons" -> block.getAsJsonArray("items").forEach(i -> items.add(i.getAsString()));
						case "grid" -> block.getAsJsonObject("key").entrySet().forEach(k -> items.add(k.getValue().getAsString()));
						case "para", "heading" -> runs(block.getAsJsonArray("runs"), items);
						case "list" -> block.getAsJsonArray("items").forEach(i -> runs(i.getAsJsonArray(), items));
						default -> {
						}
					}
				}
				for (String item : items) {
					Identifier key = Identifier.tryParse(item);
					if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
						problems.add(id.getAsString() + ": " + item);
					}
				}
			}
		}
		helper.assertTrue(problems.isEmpty() && pages > 0, Component.literal(pages + " pages; unregistered: " + problems));
		helper.succeed();
	}

	private static void runs(com.google.gson.JsonArray runs, List<String> items) {
		for (JsonElement r : runs) {
			if (r.getAsJsonObject().has("icon")) {
				items.add(r.getAsJsonObject().get("icon").getAsString());
			}
		}
	}

	private static JsonObject read(String name) {
		try (InputStream in = Farmings.class.getResourceAsStream("/assets/farmings/manual/en_us/" + name + ".json")) {
			return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			return null;
		}
	}
}
