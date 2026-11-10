package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.entity.CattleEntity;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.SheepBreed;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Headless scenes for wild animals and the taming corral (docs/taming.md). Registered from {@link FarmingsGameTests}. */
final class TamingGameTests {
	private TamingGameTests() {
	}

	/** A natural spawn is wild; a spawn group shares one breed with one male; /summon gives a herd animal. */
	static void wildSpawnIsWild(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		CattleEntity a = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.NATURAL);
		a.snapTo(helper.absolutePos(new BlockPos(2, 1, 2)).getX() + 0.5, helper.absolutePos(new BlockPos(2, 1, 2)).getY(), helper.absolutePos(new BlockPos(2, 1, 2)).getZ() + 0.5, 0, 0);
		var data = a.finalizeSpawn(level, level.getCurrentDifficultyAt(a.blockPosition()), EntitySpawnReason.NATURAL, null);
		CattleEntity b = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.NATURAL);
		b.snapTo(a.getX() + 1, a.getY(), a.getZ(), 0, 0);
		b.finalizeSpawn(level, level.getCurrentDifficultyAt(b.blockPosition()), EntitySpawnReason.NATURAL, data);
		level.addFreshEntity(a);
		level.addFreshEntity(b);
		helper.assertTrue(a.isWild() && b.isWild(), Component.literal("natural spawns are wild"));
		helper.assertTrue(a.breed() == b.breed(), Component.literal("a spawn group shares one breed"));
		helper.assertTrue(a.sex() == Sex.MALE && b.sex() == Sex.FEMALE, Component.literal("the first of a group is the bull, the rest cows"));
		helper.assertTrue(!a.canConceive() || !a.isWild(), Component.literal("a wild animal does not breed"));
		helper.assertTrue(a.readout().startsWith("WILD"), Component.literal(a.readout()));

		SheepEntity s = helper.spawn(FarmingsEntities.SHEEP, new BlockPos(4, 1, 4), EntitySpawnReason.NATURAL);
		helper.assertTrue(s.isWild(), Component.literal("a naturally spawned sheep is wild"));
		CattleEntity summoned = helper.spawn(FarmingsEntities.CATTLE, new BlockPos(5, 1, 1), EntitySpawnReason.COMMAND);
		SheepEntity egg = helper.spawn(FarmingsEntities.SHEEP, new BlockPos(6, 1, 1), EntitySpawnReason.SPAWN_ITEM_USE);
		helper.assertTrue(!summoned.isWild() && !egg.isWild(), Component.literal("summoned and egg-spawned animals are herd animals"));
		helper.succeed();
	}

	/** Wildness and settled days come back from a save. */
	static void wildSurvivesSave(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		SheepEntity s = helper.spawn(FarmingsEntities.SHEEP, new BlockPos(2, 1, 2), EntitySpawnReason.NATURAL);
		s.setSettledDays(3.5);
		var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
		s.saveWithoutId(out);
		SheepEntity b = FarmingsEntities.SHEEP.create(level, EntitySpawnReason.LOAD);
		b.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		helper.assertTrue(b.isWild() && Math.abs(b.settledDays() - 3.5) < 1e-9, Component.literal("wild and settled days survive a save"));
		helper.succeed();
	}

	/** A sheep shed takes in a herd sheep beside it but leaves a wild one alone. */
	static void shedSkipsWild(GameTestHelper helper) {
		BlockPos c = SheepGameTests.buildShed(helper);
		ServerLevel level = helper.getLevel();
		SheepGameTests.fill(helper);
		SheepEntity tame = SheepGameTests.adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 1L, new BlockPos(3, 1, 2));
		SheepEntity wild = SheepGameTests.adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 2L, new BlockPos(4, 1, 2));
		wild.setWild(true);
		SheepGameTests.shed(helper, c).refresh(level);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(c.equals(tame.shedController()), Component.literal("the herd sheep is taken in"));
			helper.assertTrue(wild.shedController() == null, Component.literal("the wild sheep is not"));
			helper.succeed();
		});
	}

	/** A cleared stone field, size x size, with air above it. */
	static void field(GameTestHelper helper, int size) {
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				helper.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
				for (int y = 1; y <= 3; y++) {
					helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
				}
			}
		}
	}

	/** A wild sheep follows a player holding wheat, and keeps away from one with empty hands. */
	static void wildFollowsFeed(GameTestHelper helper) {
		field(helper, 12);
		ServerLevel level = helper.getLevel();
		SheepEntity s = SheepGameTests.adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 3L, new BlockPos(2, 1, 6));
		s.setWild(true);
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(new BlockPos(8, 1, 6));
		p.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 90, 0);
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT));
		double start = s.distanceTo(p);
		waitFor(helper, 0, 500, () -> s.distanceTo(p) < start - 2, "she follows the wheat: " + start + " now " + s.distanceTo(p) + " removed " + s.isRemoved() + " noAi " + s.isNoAi() + " shed " + s.shedController() + " pos " + s.blockPosition() + " p " + p.blockPosition() + " goals " + s.goalSelector.getAvailableGoals().stream().filter(g -> g.isRunning()).map(g -> g.getGoal().getClass().getSimpleName()).toList(), () -> {
			double near = s.distanceTo(p);
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			waitFor(helper, 0, 500, () -> s.distanceTo(p) > near + 1, "she keeps away from empty hands: " + near, () -> {
				p.discard();
				helper.succeed();
			});
		});
	}

	private static java.util.List<net.minecraft.util.random.Weighted<net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData>> creatures(ServerLevel level,
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key) {
		var biome = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(key);
		var settings = biome.value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,
				net.minecraft.world.level.biome.MobSpawnSettings.EMPTY);
		return settings.getMobsInCategory(net.minecraft.world.entity.MobCategory.CREATURE).unwrap();
	}

	/** Plains spawns Farmings cattle (weight 8, groups of 4) and sheep (12) and no vanilla cows or sheep; the cherry grove gets Wagyu. */
	static void biomeSpawnsReplaced(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var plains = creatures(level, net.minecraft.world.level.biome.Biomes.PLAINS);
		var types = plains.stream().map(w -> w.value().type()).toList();
		helper.assertTrue(!types.contains(net.minecraft.world.entity.EntityTypes.COW) && !types.contains(net.minecraft.world.entity.EntityTypes.SHEEP),
				Component.literal("no vanilla cows or sheep in plains: " + types));
		var cattle = plains.stream().filter(w -> w.value().type() == FarmingsEntities.CATTLE).findFirst();
		var sheep = plains.stream().filter(w -> w.value().type() == FarmingsEntities.SHEEP).findFirst();
		helper.assertTrue(cattle.isPresent() && cattle.get().weight() == 8 && cattle.get().value().count().maxInclusive() == 4,
				Component.literal("plains cattle weight 8, groups of 4: " + cattle));
		helper.assertTrue(sheep.isPresent() && sheep.get().weight() == 12, Component.literal("plains sheep weight 12: " + sheep));
		var cherry = creatures(level, net.minecraft.world.level.biome.Biomes.CHERRY_GROVE);
		helper.assertTrue(cherry.stream().anyMatch(w -> w.value().type() == FarmingsEntities.CATTLE && w.weight() == 2),
				Component.literal("cherry grove gets cattle at weight 2"));
		helper.succeed();
	}

	/** A natural spawn needs grass under it and light; a summon does not. */
	static void naturalSpawnNeedsGrass(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(1, 0, 1), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
		helper.setBlock(new BlockPos(4, 0, 1), net.minecraft.world.level.block.Blocks.STONE);
		for (BlockPos p : new BlockPos[] {new BlockPos(1, 1, 1), new BlockPos(1, 2, 1), new BlockPos(4, 1, 1), new BlockPos(4, 2, 1)}) {
			helper.setBlock(p, net.minecraft.world.level.block.Blocks.AIR);
		}
		CattleEntity onGrass = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.NATURAL);
		BlockPos g = helper.absolutePos(new BlockPos(1, 1, 1));
		onGrass.snapTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 0, 0);
		CattleEntity onStone = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.NATURAL);
		BlockPos st = helper.absolutePos(new BlockPos(4, 1, 1));
		onStone.snapTo(st.getX() + 0.5, st.getY(), st.getZ() + 0.5, 0, 0);
		helper.assertTrue(!onStone.checkSpawnRules(level, EntitySpawnReason.NATURAL), Component.literal("no natural spawn on stone"));
		helper.assertTrue(onStone.checkSpawnRules(level, EntitySpawnReason.COMMAND), Component.literal("a summon can go anywhere"));
		boolean bright = level.getRawBrightness(g, 0) > 8;
		helper.assertTrue(!bright || onGrass.checkSpawnRules(level, EntitySpawnReason.NATURAL), Component.literal("a natural spawn on lit grass is fine"));
		helper.succeed();
	}

	/** Checks every 10 ticks until the condition holds, then runs next; fails after limit ticks. */
	static void waitFor(GameTestHelper helper, int waited, int limit, java.util.function.BooleanSupplier cond, String what, Runnable next) {
		if (cond.getAsBoolean()) {
			next.run();
		} else if (waited >= limit) {
			helper.fail(Component.literal(what + " (not after " + limit + " ticks)"));
		} else {
			helper.runAfterDelay(10, () -> waitFor(helper, waited + 10, limit, cond, what, next));
		}
	}
}
