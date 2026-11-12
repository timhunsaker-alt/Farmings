package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.entity.SheepDogEntity;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.husbandry.SheepBreed;
import dev.farmings.item.WhistleItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/** The sheep dog (docs/animals/sheep.md, "Sheep dog"; docs/taming.md), 2026-11-11. */
final class DogGameTests {
	private DogGameTests() {
	}

	static SheepDogEntity tameDog(GameTestHelper helper, BlockPos at, ServerPlayer owner) {
		SheepDogEntity dog = helper.spawn(FarmingsEntities.SHEEP_DOG, at, EntitySpawnReason.COMMAND);
		dog.setWild(false);
		dog.setOwner(owner.getUUID());
		return dog;
	}

	/** Come bye: the dog gathers the tame sheep around it and brings them to the whistler. */
	static void dogGathersFlock(GameTestHelper helper) {
		TamingGameTests.field(helper, 28);
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		BlockPos stand = helper.absolutePos(new BlockPos(3, 1, 3));
		p.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
		SheepDogEntity dog = tameDog(helper, new BlockPos(14, 1, 14), p);
		List<SheepEntity> flock = List.of(SheepGameTests.adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 121L, new BlockPos(18, 1, 18)),
				SheepGameTests.adult(helper, SheepBreed.MERINO, Sex.FEMALE, 122L, new BlockPos(19, 1, 17)),
				SheepGameTests.adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 123L, new BlockPos(17, 1, 19)));
		helper.assertTrue(WhistleItem.blow(helper.getLevel(), p, SheepDogEntity.Command.COME_BYE, null) == 1, Component.literal("the dog hears the whistle"));
		helper.assertTrue(dog.working() && dog.flock().size() == 3, Component.literal("it sets out for 3 sheep: " + dog.readout()));
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		TamingGameTests.waitFor(helper, 0, 1100, () -> !dog.working(), () -> "the flock is brought in: " + dog.readout() + "; target " + dog.target().subtract(o).toShortString()
				+ ", dog at " + dog.blockPosition().subtract(o).toShortString() + ", player at " + p.blockPosition().subtract(o).toShortString() + "; sheep at "
				+ flock.stream().map(s -> s.blockPosition().subtract(o).toShortString() + " to " + (s.sentTo() == null ? "-" : s.sentTo().subtract(o).toShortString())).toList(), () -> {
					for (SheepEntity s : flock) {
						helper.assertTrue(s.blockPosition().distSqr(stand) <= 5 * 5, Component.literal("each sheep is near the whistler: " + s.blockPosition().toShortString()));
					}
					helper.assertTrue(dog.command() == SheepDogEntity.Command.THATLL_DO, Component.literal("and the dog comes off the stock"));
					p.discard();
					helper.succeed();
				});
	}

	/** Walk up: the dog drives sheep off the pasture through the gate to the shed's pen. */
	static void dogDrivesFlockToShed(GameTestHelper helper) {
		BlockPos c = SheepGameTests.buildShed(helper);
		ServerLevel level = helper.getLevel();
		SheepGameTests.fill(helper);
		var shed = SheepGameTests.shed(helper, c);
		shed.refresh(level);
		shed.forceDayForTest(true);     // the shed itself would send them out: the dog's drive has to win
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		BlockPos stand = helper.absolutePos(new BlockPos(9, 1, 3));
		p.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
		SheepDogEntity dog = tameDog(helper, new BlockPos(12, 1, 3), p);
		List<SheepEntity> flock = List.of(SheepGameTests.adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 124L, new BlockPos(9, 1, 1)),
				SheepGameTests.adult(helper, SheepBreed.MERINO, Sex.FEMALE, 125L, new BlockPos(10, 1, 4)));
		for (SheepEntity s : flock) {
			s.assignShed(c, null);
		}
		BlockPos target = WhistleItem.resolve(level, c);
		helper.assertTrue(level.getBlockState(target).getBlock() == dev.farmings.shed.ShedBlocks.SHEEP_PEN, Component.literal("the shed controller means its pen: " + target));
		WhistleItem.blow(level, p, SheepDogEntity.Command.WALK_UP, target);
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		TamingGameTests.waitFor(helper, 0, 1100, () -> !dog.working(), () -> "the sheep are driven to the pen: " + dog.readout() + "; dog at " + dog.blockPosition().subtract(o).toShortString()
				+ "; sheep at " + flock.stream().map(s -> s.blockPosition().subtract(o).toShortString() + " to " + (s.sentTo() == null ? "-" : s.sentTo().subtract(o).toShortString())).toList()
				+ ", pen " + target.subtract(o).toShortString(), () -> {
					p.discard();
					helper.succeed();
				});
	}

	/** Lie down holds the dog and the sheep; a wild dog ignores the whistle; another player's dog does too. */
	static void dogCommandsAndOwners(GameTestHelper helper) {
		TamingGameTests.field(helper, 12);
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
		BlockPos stand = helper.absolutePos(new BlockPos(2, 1, 2));
		p.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
		stranger.snapTo(stand.getX() + 1.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
		helper.assertTrue(!p.getUUID().equals(stranger.getUUID()), Component.literal("two players"));
		SheepDogEntity dog = tameDog(helper, new BlockPos(6, 1, 6), p);
		SheepDogEntity wild = helper.spawn(FarmingsEntities.SHEEP_DOG, new BlockPos(3, 1, 3), EntitySpawnReason.COMMAND);
		wild.setWild(true);
		helper.assertTrue(WhistleItem.blow(helper.getLevel(), stranger, SheepDogEntity.Command.COME_BYE, null) == 0, Component.literal("no dog answers a stranger"));
		helper.assertTrue(WhistleItem.blow(helper.getLevel(), p, SheepDogEntity.Command.LIE_DOWN, null) == 1, Component.literal("only the owner's tame dog hears"));
		helper.assertTrue(dog.sitting() && !dog.working(), Component.literal("lie down: " + dog.readout()));
		helper.assertTrue(wild.readout().contains("WILD"), Component.literal(wild.readout()));
		p.discard();
		stranger.discard();
		helper.succeed();
	}

	/** A wild dog settles in a corral on raw meat, not hay, and is tamed in 7 real days. */
	static void dogTamedInCorral(GameTestHelper helper) {
		BlockPos c = TamingGameTests.buildCorral(helper);
		ServerLevel level = helper.getLevel();
		TamingGameTests.fillCorral(helper);
		var post = TamingGameTests.post(helper, c);
		java.util.UUID tim = java.util.UUID.randomUUID();
		post.claim(tim);
		post.refresh(level);
		SheepDogEntity dog = helper.spawn(FarmingsEntities.SHEEP_DOG, new BlockPos(4, 1, 6), EntitySpawnReason.COMMAND);
		dog.setWild(true);
		dog.setNoAi(true);
		int half = (int) dev.farmings.husbandry.TimeScale.LIVESTOCK.realDaysToTicks(3.5);
		post.tend(level, dog, dog, half);
		helper.assertTrue(dog.settledDays() == 0, Component.literal("hay is no food for a dog: " + dog.settledDays()));
		var bait = (dev.farmings.corral.BaitTroughEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(5, 1, 4)));
		for (int i = 0; i < 8; i++) {
			bait.addMeat();
		}
		post.tend(level, dog, dog, half);
		post.tend(level, dog, dog, half);
		helper.assertTrue(!dog.isWild() && tim.equals(dog.owner()), Component.literal("tamed on meat, and the corral owner's: " + dog.readout()));
		// one game day of the corral is one real day's intake (the barn rule): 7 real days at 15:1 is 0.47 game days, 0.23 kg
		double ate = 4.0 - bait.meatKg();
		double want = 2 * SheepDogEntity.MEAT_KG_PER_DAY * half / (double) dev.farmings.husbandry.TimeScale.TICKS_PER_GAME_DAY;
		helper.assertTrue(Math.abs(ate - want) < 1e-6, Component.literal("it ate its meat: " + ate + " kg, expected " + want));
		helper.succeed();
	}
}
