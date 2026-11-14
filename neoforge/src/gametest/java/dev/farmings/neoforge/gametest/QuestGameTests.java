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
		helper.assertTrue(farm.quests().size() == 13 && book.quest(Farmings.id("biogas_genset")) != null,
				Component.literal("13 farm quests, biogas_genset among them: " + farm.quests().size()));
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
}
