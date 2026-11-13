package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.PigEntity;
import dev.farmings.genetics.PigLook;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.PigBreed;
import dev.farmings.husbandry.PigStage;
import dev.farmings.husbandry.Swine;
import dev.farmings.item.StrawItem;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/** Headless scenes for pigs (docs/animals/pigs.md). Registered from {@link FarmingsGameTests}. */
final class PigGameTests {
	private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

	private PigGameTests() {
	}

	static PigEntity pig(GameTestHelper helper, PigBreed breed, Sex sex, long seed, double ageDays, BlockPos at) {
		PigEntity p = helper.spawn(FarmingsEntities.PIG, at);
		p.initFounder(breed, sex, seed, ageDays, Swine.targetKg(Map.of(breed.code, 10_000), sex == Sex.MALE, ageDays), helper.getLevel().getGameTime());
		return p;
	}

	static PigEntity adult(GameTestHelper helper, PigBreed breed, Sex sex, long seed, BlockPos at) {
		return pig(helper, breed, sex, seed, 400, at);
	}

	private static <T extends net.minecraft.world.entity.Entity> List<T> around(GameTestHelper helper, Class<T> type) {
		return helper.getLevel().getEntitiesOfClass(type, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12));
	}

	/** Founders look like their breed and carry a pig genome; everything survives a save. */
	static void pigSpawnsAndSurvivesSave(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		PigEntity plain = helper.spawn(FarmingsEntities.PIG, new BlockPos(1, 1, 1), EntitySpawnReason.COMMAND);
		helper.assertTrue(plain.genome() != null && plain.genome().species() == Species.PIG, Component.literal("a spawned pig has a pig genome"));
		PigEntity lw = adult(helper, PigBreed.LARGE_WHITE, Sex.FEMALE, 1L, new BlockPos(3, 1, 1));
		PigEntity dur = adult(helper, PigBreed.DUROC, Sex.MALE, 2L, new BlockPos(5, 1, 1));
		PigEntity brk = adult(helper, PigBreed.BERKSHIRE, Sex.FEMALE, 3L, new BlockPos(7, 1, 1));
		helper.assertTrue(lw.look().colour() == PigLook.Colour.WHITE && dur.look().colour() == PigLook.Colour.RED
				&& brk.look().colour() == PigLook.Colour.BERKSHIRE, Component.literal("white, red, Berkshire"));
		lw.startHeatForDisplay();
		lw.sureConceptionForTest();
		helper.assertTrue(lw.conceive(dur.genome(), dur.genes(), level.getGameTime()), Component.literal("she conceives"));
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		lw.saveWithoutId(out);
		PigEntity b = FarmingsEntities.PIG.create(level, EntitySpawnReason.LOAD);
		b.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		helper.assertTrue(b.breed() == PigBreed.LARGE_WHITE && b.genome().equals(lw.genome()) && b.genes().equals(lw.genes()) && b.pregnant(),
				Component.literal("breed, genome, genes and pregnancy survive"));
		for (Trait t : Species.PIG.traits()) {
			helper.assertTrue(b.score(t) == lw.score(t), Component.literal("score " + t.id + " survives"));
		}
		helper.succeed();
	}

	/** Piglet, gilt at weaning (21 days); a castrated boar piglet grows as a barrow; a grown male is a boar. */
	static void pigLifecycle(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		PigEntity piglet = pig(helper, PigBreed.DUROC, Sex.FEMALE, 11L, 10, new BlockPos(2, 1, 2));
		helper.assertTrue(piglet.stage() == PigStage.PIGLET && piglet.isBaby(), Component.literal("a 10-day pig is a piglet"));
		piglet.advanceForTest(20);
		piglet.settle(helper.getLevel());
		helper.assertTrue(piglet.stage() == PigStage.GILT, Component.literal("weaned she is a gilt: " + piglet.stage()));
		PigEntity male = pig(helper, PigBreed.DUROC, Sex.MALE, 12L, 10, new BlockPos(5, 1, 2));
		var player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(HAND, new ItemStack(Items.SHEARS));
		male.mobInteract(player, HAND);
		male.advanceForTest(300);
		male.setWeightKg(150);
		helper.assertTrue(male.stage() == PigStage.BARROW, Component.literal("he grows as a barrow: " + male.stage()));
		helper.assertTrue(adult(helper, PigBreed.BERKSHIRE, Sex.MALE, 13L, new BlockPos(5, 1, 5)).stage() == PigStage.BOAR, Component.literal("a grown male is a boar"));
		player.discard();
		helper.succeed();
	}

	/** A boar serves a sow in heat; she farrows a litter beside her, the piglets at 1.5 kg, with the stillborn ones not spawned. */
	static void sowFarrowsLitter(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		PigEntity sow = adult(helper, PigBreed.LARGE_WHITE, Sex.FEMALE, 21L, new BlockPos(4, 1, 4));
		PigEntity boar = adult(helper, PigBreed.DUROC, Sex.MALE, 22L, new BlockPos(5, 1, 4));
		sow.setNoAi(true);
		boar.setNoAi(true);
		sow.sureConceptionForTest();
		sow.settle(level);
		sow.startHeatForDisplay();
		sow.settle(level);
		helper.assertTrue(sow.pregnant(), Component.literal("the boar serves her: " + sow.readout()));
		sow.forcePigletsForTest(10);
		sow.makeDueNow();
		sow.settle(level);
		List<PigEntity> piglets = around(helper, PigEntity.class).stream().filter(p -> p != sow && p != boar).toList();
		helper.assertTrue(piglets.size() == 10, Component.literal("ten piglets, got " + piglets.size()));
		for (PigEntity p : piglets) {
			helper.assertTrue(p.stage() == PigStage.PIGLET && Math.abs(p.weightKg() - 1.5) < 1e-9, Component.literal("a 1.5 kg piglet"));
			helper.assertTrue(p.composition().equals(Map.of("LWH", 5000, "DUR", 5000)) && p.look().colour() == PigLook.Colour.WHITE,
					Component.literal("an F1 cross, white over red (dominant white)"));
		}
		helper.assertTrue(sow.pigletsNursing() == 10 && sow.feedKgPerDay() == 6.5, Component.literal("she nurses 10 on 6.5 kg a day"));
		helper.succeed();
	}

	/** A market pig gives chops, bellies, shoulders, fat and two hams of its weight. */
	static void pigCutsDrop(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		PigEntity p = pig(helper, PigBreed.DUROC, Sex.FEMALE, 31L, 186, new BlockPos(3, 1, 3));
		p.setWeightKg(120);
		p.kill(helper.getLevel());
		helper.runAfterDelay(5, () -> {
			var items = around(helper, ItemEntity.class).stream().map(ItemEntity::getItem).toList();
			int chops = items.stream().filter(s -> s.is(FarmingsItems.PORK_CHOP)).mapToInt(ItemStack::getCount).sum();
			int bellies = items.stream().filter(s -> s.is(FarmingsItems.PORK_BELLY)).mapToInt(ItemStack::getCount).sum();
			int fat = items.stream().filter(s -> s.is(FarmingsItems.PORK_FAT)).mapToInt(ItemStack::getCount).sum();
			List<ItemStack> hams = items.stream().filter(s -> s.is(FarmingsItems.FRESH_HAM)).toList();
			helper.assertTrue(chops == 24 && bellies == 5 && fat == 12, Component.literal("chops " + chops + ", bellies " + bellies + ", fat " + fat));
			helper.assertTrue(hams.size() == 2 && Math.abs(FarmingsItems.kgOf(hams.get(0)) - 84 * 0.158 / 2) < 1e-6, Component.literal("two hams of 6.6 kg"));
			helper.succeed();
		});
	}

	/** Farmings pigs take vanilla pigs' spawns; a wild group has one boar. */
	static void wildPigReplacesVanilla(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		var plains = TamingGameTests.creatures(level, net.minecraft.world.level.biome.Biomes.PLAINS);
		var types = plains.stream().map(w -> w.value().type()).toList();
		helper.assertTrue(!types.contains(net.minecraft.world.entity.EntityTypes.PIG) && types.contains(FarmingsEntities.PIG),
				Component.literal("plains have Farmings pigs: " + types));
		PigEntity a = FarmingsEntities.PIG.create(level, EntitySpawnReason.NATURAL);
		BlockPos p = helper.absolutePos(new BlockPos(2, 1, 2));
		a.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
		var data = a.finalizeSpawn(level, level.getCurrentDifficultyAt(a.blockPosition()), EntitySpawnReason.NATURAL, null);
		PigEntity b = FarmingsEntities.PIG.create(level, EntitySpawnReason.NATURAL);
		b.snapTo(a.getX() + 1, a.getY(), a.getZ(), 0, 0);
		b.finalizeSpawn(level, level.getCurrentDifficultyAt(b.blockPosition()), EntitySpawnReason.NATURAL, data);
		helper.assertTrue(a.isWild() && b.isWild() && a.breed() == b.breed() && a.sex() == Sex.MALE && b.sex() == Sex.FEMALE,
				Component.literal("a wild group: one breed, one boar"));
		helper.succeed();
	}

	/** A boar's straw takes on a sow in heat within 6 days; past 6 days it is stale. */
	static void boarStrawGoesStale(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		PigEntity boar = adult(helper, PigBreed.BERKSHIRE, Sex.MALE, 41L, new BlockPos(2, 1, 2));
		PigEntity sow = adult(helper, PigBreed.LARGE_WHITE, Sex.FEMALE, 42L, new BlockPos(4, 1, 2));
		boar.setNoAi(true);
		sow.setNoAi(true);
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack empty = new ItemStack(FarmingsItems.EMPTY_STRAW, 1);
		helper.assertTrue(empty.getItem().interactLivingEntity(empty, player, boar, HAND).consumesAction(), Component.literal("the boar fills a straw"));
		ItemStack straw = player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(FarmingsItems.SEMEN_STRAW)).findFirst().orElseThrow();
		long now = level.getGameTime();
		helper.assertTrue(!StrawItem.stale(straw, now + StrawItem.BOAR_FRESH_TICKS - 20) && StrawItem.stale(straw, now + StrawItem.BOAR_FRESH_TICKS + 20),
				Component.literal("fresh for 6 days, then stale"));
		sow.startHeatForDisplay();
		sow.sureConceptionForTest();
		helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, sow, HAND).consumesAction() && sow.pregnant(), Component.literal("the sow takes it"));
		helper.succeed();
	}
}
