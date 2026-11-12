package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsItems;
import dev.farmings.entity.FowlEntity;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.SheepBreed;
import dev.farmings.husbandry.poultry.FowlBreed;
import dev.farmings.item.StrawItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** One straw for every species (docs/animals/breeding.md, "one straw for every species", 2026-11-11). */
final class StrawGameTests {
	private StrawGameTests() {
	}

	private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

	/** Fills one empty straw from a male and returns the semen straw from the player's inventory. */
	private static ItemStack fill(GameTestHelper helper, Player player, LivingEntity male) {
		ItemStack empty = new ItemStack(FarmingsItems.EMPTY_STRAW, 1);
		helper.assertTrue(empty.getItem().interactLivingEntity(empty, player, male, HAND).consumesAction(), Component.literal("a " + male.getType().toShortString() + " fills a straw"));
		for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
			if (s.is(FarmingsItems.SEMEN_STRAW)) {
				player.getInventory().removeItem(s);
				return s;
			}
		}
		throw new AssertionError("no semen straw");
	}

	/** A ram's straw inseminates a ewe in heat; out of heat it is refused and kept. */
	static void ramStrawInseminatesEwe(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		SheepEntity ram = SheepGameTests.adult(helper, SheepBreed.SUFFOLK, Sex.MALE, 111L, new BlockPos(2, 1, 2));
		SheepEntity ewe = SheepGameTests.adult(helper, SheepBreed.MERINO, Sex.FEMALE, 112L, new BlockPos(4, 1, 2));
		ram.setNoAi(true);
		ewe.setNoAi(true);
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack straw = fill(helper, player, ram);
		helper.assertTrue(StrawItem.genomeOf(straw).species() == dev.farmings.genetics.Species.SHEEP, Component.literal("the straw holds a sheep genome"));
		helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, ewe, HAND) == InteractionResult.FAIL && straw.getCount() == 1,
				Component.literal("a ewe not in heat is refused"));
		ewe.startHeatForDisplay();
		helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, ewe, HAND).consumesAction(), Component.literal("a ewe in heat takes it"));
		helper.assertTrue(ewe.pregnant() && straw.isEmpty(), Component.literal("she is in lamb and the straw is used up"));
		helper.succeed();
	}

	/** A Broad Breasted White tom can't mate, but his straw makes a BBW hen's eggs fertile. */
	static void bbwHenFertileByStraw(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		FowlEntity tom = PoultryGameTests.adult(helper, FowlBreed.BROAD_BREASTED_WHITE, Sex.MALE, 113L, new BlockPos(2, 1, 2));
		FowlEntity hen = PoultryGameTests.adult(helper, FowlBreed.BROAD_BREASTED_WHITE, Sex.FEMALE, 114L, new BlockPos(4, 1, 2));
		tom.setNoAi(true);
		hen.setNoAi(true);
		hen.settle(helper.getLevel());
		helper.assertTrue(!hen.fertile(), Component.literal("a BBW tom can't serve her"));
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack straw = fill(helper, player, tom);
		helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, hen, HAND).consumesAction(), Component.literal("the hen takes the straw"));
		helper.assertTrue(hen.fertile() && hen.fertileFrom().equals(tom.herdId()), Component.literal("her eggs are fertile from the BBW tom: " + hen.fertileFrom()));
		helper.succeed();
	}

	/** Turkey semen goes stale after 30 real minutes: then a hen is refused it, and in an inventory it turns back into an empty straw. */
	static void turkeyStrawGoesStale(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		FowlEntity tom = PoultryGameTests.adult(helper, FowlBreed.BRONZE, Sex.MALE, 115L, new BlockPos(2, 1, 2));
		FowlEntity hen = PoultryGameTests.adult(helper, FowlBreed.BRONZE, Sex.FEMALE, 116L, new BlockPos(4, 1, 2));
		tom.setNoAi(true);
		hen.setNoAi(true);
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack straw = fill(helper, player, tom);
		long now = helper.getLevel().getGameTime();
		helper.assertTrue(!StrawItem.stale(straw, now + StrawItem.TURKEY_FRESH_TICKS), Component.literal("fresh for its 30 minutes"));
		helper.assertTrue(StrawItem.stale(straw, now + StrawItem.TURKEY_FRESH_TICKS + 1), Component.literal("stale after"));
		helper.assertTrue(StrawItem.TURKEY_FRESH_TICKS > 100 && StrawItem.TURKEY_FRESH_TICKS < 250,
				Component.literal("30 real minutes is about 8 seconds at 3:1: " + StrawItem.TURKEY_FRESH_TICKS + " ticks"));
		helper.runAfterDelay((int) StrawItem.TURKEY_FRESH_TICKS + 5, () -> {
			helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, hen, HAND) == InteractionResult.FAIL && !hen.fertile(),
					Component.literal("stale semen doesn't take"));
			helper.succeed();
		});
	}

	/** A bull's straw on a ewe, or a ram's on a turkey, does nothing and is kept. */
	static void strawWrongSpeciesRefused(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		SheepEntity ram = SheepGameTests.adult(helper, SheepBreed.SUFFOLK, Sex.MALE, 117L, new BlockPos(2, 1, 2));
		SheepEntity ewe = SheepGameTests.adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 118L, new BlockPos(4, 1, 2));
		FowlEntity hen = PoultryGameTests.adult(helper, FowlBreed.BRONZE, Sex.FEMALE, 119L, new BlockPos(6, 1, 2));
		ram.setNoAi(true);
		ewe.setNoAi(true);
		hen.setNoAi(true);
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack straw = fill(helper, player, ram);
		helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, hen, HAND) == InteractionResult.FAIL && straw.getCount() == 1 && !hen.fertile(),
				Component.literal("ram semen on a turkey hen is refused and kept"));
		ItemStack empty = new ItemStack(FarmingsItems.EMPTY_STRAW, 1);
		helper.assertTrue(empty.getItem().interactLivingEntity(empty, player, ewe, HAND) == InteractionResult.FAIL && empty.getCount() == 1,
				Component.literal("a ewe can't fill a straw"));
		ram.setWild(true);
		helper.assertTrue(empty.getItem().interactLivingEntity(empty, player, ram, HAND) == InteractionResult.FAIL, Component.literal("nor a wild ram"));
		helper.succeed();
	}
}
