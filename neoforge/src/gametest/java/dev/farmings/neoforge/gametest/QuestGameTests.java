package dev.farmings.neoforge.gametest;

import dev.farmings.Farmings;
import dev.wildcatter.quest.Milestones;
import dev.wildcatter.quest.QuestBook;
import dev.wildcatter.quest.QuestData;
import dev.wildcatter.quest.QuestRules;
import dev.wildcatter.quest.QuestTracker;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Farmings' chapter in Wildcatter's quest book (data/farmings/wildcatter_quests/farm.json; Wildcatter docs/quests.md). */
final class QuestGameTests {
	private QuestGameTests() {
	}

	/** Every chapter loaded, Farmings' with it, passes Wildcatter's check: ids resolve, prerequisites exist, no cycle, rewards allowed. */
	static void questsValid(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		QuestBook book = QuestTracker.book(server);
		QuestBook.Chapter farm = book.chapter(Farmings.id("farm"));
		helper.assertTrue(farm != null, Component.literal("the farm chapter should load: " + book.chapters().stream().map(c -> c.id().toString()).toList()));
		helper.assertTrue(farm.quests().size() == 18 && book.quest(Farmings.id("biogas_genset")) != null && book.quest(Farmings.id("herd_health")) != null,
				Component.literal("18 farm quests (five on animal health), biogas_genset among them: " + farm.quests().size()));
		List<String> problems = QuestRules.problems(server);
		helper.assertTrue(problems.isEmpty(), Component.literal("every chapter should pass the check: " + problems));
		Farmings.LOGGER.info("[quests_valid] ok: {} chapters, {} quests", book.chapters().size(), book.quests().size());
		helper.succeed();
	}

	/**
	 * The digester's gas holder piped to a running Wildcatter gas set, a player beside it: the set fires {@code wildcatter:genset_on_biogas}
	 * and the farm's biogas_genset quest is done.
	 */
	static void biogasMilestone(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		DigesterGameTests.BiogasSet s = DigesterGameTests.biogasSet(helper);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos at = helper.absolutePos(new BlockPos(6, 2, 2));
		player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.getInventory().clearContent();
		helper.runAfterDelay(30, () -> s.panel().setWantRun(true));
		helper.succeedWhen(() -> {
			QuestData.Progress p = QuestData.get(server).of(player.getStringUUID());
			helper.assertTrue(s.panel().onBiogas() && p.milestones.contains(Milestones.GENSET_ON_BIOGAS.toString()),
					Component.literal("the set on the farm's biogas should fire genset_on_biogas: on biogas " + s.panel().onBiogas() + ", " + p.milestones));
			QuestTracker.check(player);
			helper.assertTrue(p.done.contains(Farmings.id("biogas_genset").toString()), Component.literal("biogas_genset should be done: " + p.done));
			server.getPlayerList().remove(player);
			Farmings.LOGGER.info("[quest_biogas_milestone] ok: burned {} m3", s.panel().biogasBurned());
		});
	}

	/**
	 * The taming corral with a player beside it: a wild sheep fed and watered there for 7 real days settles, the post fires
	 * {@code farmings:tamed}, and with 4 hay blocks in hand the farm's cattle quest is done.
	 */
	static void tamedMilestone(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		net.minecraft.server.level.ServerLevel level = helper.getLevel();
		BlockPos c = TamingGameTests.buildCorral(helper);
		TamingGameTests.fillCorral(helper);
		var post = TamingGameTests.post(helper, c);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos at = helper.absolutePos(new BlockPos(5, 1, 11));
		player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.getInventory().clearContent();
		post.claim(player.getUUID());
		post.refresh(level);
		dev.farmings.entity.SheepEntity s = SheepGameTests.adult(helper, dev.farmings.husbandry.SheepBreed.MERINO, dev.farmings.genetics.Sex.FEMALE, 18L, new BlockPos(4, 1, 6));
		s.setWild(true);
		s.setNoAi(true);
		QuestData.Progress p = QuestData.get(server).of(player.getStringUUID());
		post.tend(level, s, s, (int) dev.farmings.husbandry.TimeScale.realDaysToTicks(3.5));
		helper.assertTrue(s.isWild() && !p.milestones.contains(dev.farmings.quest.Milestones.TAMED.toString()),
				Component.literal("half settled: no milestone yet: " + p.milestones));
		post.tend(level, s, s, (int) dev.farmings.husbandry.TimeScale.realDaysToTicks(3.5));
		helper.assertTrue(!s.isWild(), Component.literal("after 7 days fed and watered she is tamed"));
		helper.assertTrue(p.milestones.contains(dev.farmings.quest.Milestones.TAMED.toString()),
				Component.literal("the corral should fire farmings:tamed: " + p.milestones));
		player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.HAY_BLOCK, 4));
		QuestTracker.check(player);
		helper.assertTrue(p.done.contains(Farmings.id("cattle").toString()), Component.literal("cattle should be done: " + p.done));
		server.getPlayerList().remove(player);
		s.discard();
		Farmings.LOGGER.info("[quest_tamed_milestone] ok: {}", post.readout(level));
		helper.succeed();
	}

	/**
	 * Built From Scratch check: every item task in every loaded chapter can be made in a fresh survival world with only
	 * its prerequisites done ({@link QuestReach}). Logs each problem, then fails if there is any.
	 */
	static void itemsReachable(GameTestHelper helper) {
		List<String> problems = QuestReach.problems(helper.getLevel());
		problems.forEach(p -> Farmings.LOGGER.warn("[quest_items_reachable] {}", p));
		helper.assertTrue(problems.isEmpty(), Component.literal(problems.size() + " quest items cannot be made in order: " + problems));
		Farmings.LOGGER.info("[quest_items_reachable] ok: {} quests", QuestTracker.book(helper.getLevel().getServer()).quests().size());
		helper.succeed();
	}
}
