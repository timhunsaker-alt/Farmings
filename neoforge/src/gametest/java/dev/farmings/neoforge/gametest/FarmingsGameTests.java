package dev.farmings.neoforge.gametest;

import dev.farmings.Farmings;
import dev.farmings.FarmingsBlocks;
import dev.farmings.FarmingsEntities;
import dev.farmings.barn.BarnControllerEntity;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import dev.farmings.barn.BarnParts;
import dev.farmings.husbandry.barn.BarnCheck;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import dev.farmings.FarmingsItems;
import dev.farmings.barn.DriveEntity;
import dev.farmings.barn.PitEntity;
import dev.farmings.husbandry.barn.ScraperPass;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import dev.farmings.entity.CattleEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.husbandry.Lifecycle;
import dev.farmings.husbandry.Stage;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Headless scenes for the animals. {@code ./gradlew :neoforge:runGameTestServer} */
@Mod("farmings_gametest")
public class FarmingsGameTests {
	private static final String[] SCENES = {"registers", "cattle_spawns", "cattle_state_survives_save", "calf_grows_up", "bull_does_not_milk", "barn_forms", "barn_missing_part", "barn_part_breaks_whole", "barn_usual_orientation", "barn_scraper_pass", "barn_scraper_no_power", "barn_scraper_blocked", "barn_scraper_pit_full", "manure_fork", "pit_gives_manure", "cows_take_stalls", "cow_eats_and_drinks", "cow_makes_manure", "cow_hungry_without_hay", "feeder_takes_hay_blocks", "trough_takes_bucket", "cow_walks_to_stall", "cow_walks_in_from_the_pit_end", "cow_stall_survives_save"};

	public FarmingsGameTests(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> event.register(Registries.TEST_FUNCTION, helper -> {
			helper.register(id("registers"), loaded(FarmingsGameTests::registers));
			helper.register(id("cattle_spawns"), loaded(FarmingsGameTests::cattleSpawns));
			helper.register(id("cattle_state_survives_save"), loaded(FarmingsGameTests::cattleStateSurvivesSave));
			helper.register(id("calf_grows_up"), loaded(FarmingsGameTests::calfGrowsUp));
			helper.register(id("bull_does_not_milk"), loaded(FarmingsGameTests::bullDoesNotMilk));
			helper.register(id("barn_forms"), loaded(FarmingsGameTests::barnForms));
			helper.register(id("barn_missing_part"), loaded(FarmingsGameTests::barnMissingPart));
			helper.register(id("barn_part_breaks_whole"), loaded(FarmingsGameTests::barnPartBreaksWhole));
			helper.register(id("barn_usual_orientation"), loaded(FarmingsGameTests::barnUsualOrientation));
			helper.register(id("barn_scraper_pass"), loaded(FarmingsGameTests::barnScraperPass));
			helper.register(id("barn_scraper_no_power"), loaded(FarmingsGameTests::barnScraperNoPower));
			helper.register(id("barn_scraper_blocked"), loaded(FarmingsGameTests::barnScraperBlocked));
			helper.register(id("barn_scraper_pit_full"), loaded(FarmingsGameTests::barnScraperPitFull));
			helper.register(id("manure_fork"), loaded(FarmingsGameTests::manureFork));
			helper.register(id("pit_gives_manure"), loaded(FarmingsGameTests::pitGivesManure));
			helper.register(id("cows_take_stalls"), loaded(FarmingsGameTests::cowsTakeStalls));
			helper.register(id("cow_eats_and_drinks"), loaded(FarmingsGameTests::cowEatsAndDrinks));
			helper.register(id("cow_makes_manure"), loaded(FarmingsGameTests::cowMakesManure));
			helper.register(id("cow_hungry_without_hay"), loaded(FarmingsGameTests::cowHungryWithoutHay));
			helper.register(id("feeder_takes_hay_blocks"), loaded(FarmingsGameTests::feederTakesHayBlocks));
			helper.register(id("trough_takes_bucket"), loaded(FarmingsGameTests::troughTakesBucket));
			helper.register(id("cow_walks_to_stall"), loaded(FarmingsGameTests::cowWalksToStall));
			helper.register(id("cow_walks_in_from_the_pit_end"), loaded(FarmingsGameTests::cowWalksInFromThePitEnd));
			helper.register(id("cow_stall_survives_save"), loaded(FarmingsGameTests::cowStallSurvivesSave));
		}));
		modBus.addListener(RegisterGameTestsEvent.class, event -> {
			Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"));
			for (String scene : SCENES) {
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						new TestData<>(plain, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), scene.startsWith("barn_scraper") || scene.startsWith("cow_walks") ? 1200 : 200, 1, true,
								net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 40)));
			}
		});
	}

	/** Runs a scene with the chunks around it loaded and ticking. */
	private static Consumer<GameTestHelper> loaded(Consumer<GameTestHelper> scene) {
		return helper -> {
			ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
			for (int dx = -1; dx <= 2; dx++) {
				for (int dz = -1; dz <= 2; dz++) {
					helper.getLevel().setChunkForced(origin.x() + dx, origin.z() + dz, true);
				}
			}
			scene.accept(helper);
		};
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("farmings_gametest", path);
	}

	/** A flat floor under the scene so the animals do not fall. */
	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	private static CattleEntity cow(GameTestHelper helper, CattleBreed breed, Sex sex, long seed, double ageDays, double weightKg) {
		floor(helper);
		CattleEntity c = helper.spawn(FarmingsEntities.CATTLE, new BlockPos(3, 1, 3));
		c.initFounder(breed, sex, seed, ageDays, weightKg, helper.getLevel().getGameTime());
		return c;
	}

	private static void registers(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Farmings.id("cattle")), Component.literal("the cattle entity should be registered"));
		helper.succeed();
	}

	private static void cattleSpawns(GameTestHelper helper) {
		floor(helper);
		// finalizeSpawn gives a plain spawn a founder: an adult that can be bred today.
		CattleEntity c = helper.spawn(FarmingsEntities.CATTLE, new BlockPos(3, 1, 3), EntitySpawnReason.COMMAND);
		helper.assertTrue(c.genome() != null, Component.literal("a spawned animal should have a genome"));
		helper.assertTrue(c.breed() == CattleBreed.HOLSTEIN, Component.literal("the default breed is Holstein"));
		helper.assertTrue(!c.isBaby(), Component.literal("a spawned animal is an adult"));
		helper.assertTrue(Lifecycle.canFirstBreed(c.breed(), c.ageRealDays(), c.weightKg()), Component.literal("a spawned animal can be bred today"));
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(c.isAlive(), Component.literal("the animal should still be alive after 40 ticks"));
			helper.succeed();
		});
	}

	/** Saves a cow the way a chunk unload does and loads her into a new entity: every fact must come back. */
	private static void cattleStateSurvivesSave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CattleEntity a = cow(helper, CattleBreed.JERSEY, Sex.FEMALE, 12345L, 500, 300);
		a.markBred(level.getGameTime());

		ProblemReporter reporter = ProblemReporter.DISCARDING;
		TagValueOutput out = TagValueOutput.createWithContext(reporter, level.registryAccess());
		a.saveWithoutId(out);
		CompoundTag tag = out.buildResult();

		CattleEntity b = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.LOAD);
		helper.assertTrue(b != null, Component.literal("a new cattle entity should be created"));
		b.load(TagValueInput.create(reporter, level.registryAccess(), tag));

		helper.assertTrue(b.breed() == CattleBreed.JERSEY, Component.literal("breed should survive"));
		helper.assertTrue(b.sex() == Sex.FEMALE, Component.literal("sex should survive"));
		helper.assertTrue(b.genome() != null && b.genome().equals(a.genome()), Component.literal("genome should survive exactly"));
		helper.assertTrue(b.genome().hash().equals(a.genome().hash()), Component.literal("genome hash should survive"));
		for (Trait t : Trait.values()) {
			helper.assertTrue(b.score(t) == a.score(t), Component.literal("trait score " + t.id + " should survive"));
		}
		helper.assertTrue(b.weightKg() == a.weightKg(), Component.literal("weight should survive"));
		helper.assertTrue(Math.abs(b.ageRealDays() - a.ageRealDays()) < 0.01, Component.literal("age should survive"));
		helper.assertTrue(b.isInCalf(), Component.literal("pregnancy should survive"));
		helper.assertTrue(b.stage() == a.stage(), Component.literal("stage should survive"));
		helper.succeed();
	}

	/** A calf below 40% of mature weight is a calf (and shows as a baby); at that weight she becomes a heifer. */
	private static void calfGrowsUp(GameTestHelper helper) {
		CattleEntity c = cow(helper, CattleBreed.HOLSTEIN, Sex.FEMALE, 7L, 120, 100);
		helper.assertTrue(c.stage() == Stage.CALF, Component.literal("100 kg Holstein should be a calf, got " + c.stage()));
		helper.assertTrue(c.isBaby(), Component.literal("a calf should show as a baby"));
		c.setWeightKg(CattleBreed.HOLSTEIN.pubertyWeightKg());
		helper.assertTrue(c.stage() == Stage.HEIFER, Component.literal("at puberty weight she should be a heifer, got " + c.stage()));
		helper.assertTrue(!c.isBaby(), Component.literal("a heifer should not show as a baby"));
		helper.succeed();
	}

	private static void bullDoesNotMilk(GameTestHelper helper) {
		CattleEntity bull = cow(helper, CattleBreed.BROWN_SWISS, Sex.MALE, 99L, 800, 600);
		helper.assertTrue(bull.stage() == Stage.BULL, Component.literal("a mature male is a bull, got " + bull.stage()));
		helper.assertTrue(!Lifecycle.givesMilk(bull.stage()), Component.literal("a bull gives no milk"));
		helper.succeed();
	}

	// ---- the barn ----

	private static void barnPart(ServerLevel level, BarnParts kind, BlockPos main, Direction facing) {
		BlockState state = FarmingsBlocks.part(kind).defaultBlockState().setValue(BigPartBlock.FACING, facing);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
	}

	/**
	 * A whole barn, alley east-west, pit at the east end, controller at {@code c}: n stalls a side take 2n+1 cells along the alley.
	 * Everything is placed facing north, as the footprints are drawn.
	 */
	private static void buildBarn(ServerLevel level, BlockPos c, int stalls) {
		int fl = c.getY() - 1;
		int xd = c.getX() - (2 * stalls + 1);
		Direction n = Direction.NORTH;
		barnPart(level, BarnParts.COLLECTION_PIT, new BlockPos(c.getX(), fl - 1, c.getZ()), n);
		level.setBlock(c, FarmingsBlocks.BARN_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
		barnPart(level, BarnParts.SCRAPER_DRIVE, new BlockPos(xd, c.getY(), c.getZ()), n);
		for (int x = xd; x < c.getX(); x++) {
			barnPart(level, BarnParts.SCRAPER_RAIL, new BlockPos(x, fl, c.getZ()), n);
			barnPart(level, BarnParts.ALLEY_FLOOR, new BlockPos(x, fl, c.getZ() - 1), n);
			barnPart(level, BarnParts.ALLEY_FLOOR, new BlockPos(x, fl, c.getZ() + 1), n);
		}
		barnPart(level, BarnParts.MANURE_SCRAPER, new BlockPos(xd + 1, c.getY(), c.getZ()), n);
		for (int dir = -1; dir <= 1; dir += 2) {
			int firstZ = dir < 0 ? c.getZ() - 4 : c.getZ() + 2;
			for (int j = 0; j <= stalls; j++) {
				barnPart(level, BarnParts.STALL_DIVIDER, new BlockPos(xd + 2 * j, fl, firstZ), n);
			}
			for (int i = 0; i < stalls; i++) {
				int x = xd + 1 + 2 * i;
				barnPart(level, BarnParts.STALL_BED, new BlockPos(x, fl, firstZ), n);
				barnPart(level, BarnParts.FEED_BUNK, new BlockPos(x, fl, c.getZ() + dir * 5), n);
				barnPart(level, BarnParts.HAY_FEEDER, new BlockPos(x, fl + 1, c.getZ() + dir * 5), n);
			}
			barnPart(level, BarnParts.WATER_TROUGH, new BlockPos(c.getX() + 1, fl, c.getZ() + dir * 2), n);
		}
	}

	private static BarnControllerEntity controller(GameTestHelper helper, BlockPos relative) {
		helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(relative)) instanceof BarnControllerEntity, Component.literal("the controller block entity is missing"));
		BarnControllerEntity e = (BarnControllerEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
		e.refresh(helper.getLevel());
		return e;
	}

	private static void barnForms(GameTestHelper helper) {
		BlockPos c = new BlockPos(14, 4, 8);
		buildBarn(helper.getLevel(), helper.absolutePos(c), 3);
		BarnControllerEntity e = controller(helper, c);
		helper.assertTrue(e.formed(), Component.literal("a whole barn should form, got: " + e.readout()));
		helper.assertTrue(e.check().stallsPerSide() == 3, Component.literal("3 stalls a side, got " + e.check().stallsPerSide()));
		helper.assertTrue(e.check().stalls() == 6, Component.literal("6 cows"));
		helper.assertTrue(e.readout().equals("Barn: FORMED, 3 stalls a side (6 cows), scraper idle, 0 housed"), Component.literal("readout: " + e.readout()));
		helper.succeed();
	}

	private static void barnMissingPart(GameTestHelper helper) {
		BlockPos c = new BlockPos(14, 4, 8);
		ServerLevel level = helper.getLevel();
		buildBarn(level, helper.absolutePos(c), 3);
		// the divider between stall 2 and stall 3 on the north side: x = xd + 4, its cells run from z = cz - 4 for 3 cells, 2 high
		int xd = c.getX() - 7;
		BlockPos divider = helper.absolutePos(new BlockPos(xd + 4, c.getY() - 1, c.getZ() - 4));
		helper.assertTrue(level.getBlockState(divider).getBlock() == FarmingsBlocks.part(BarnParts.STALL_DIVIDER), Component.literal("the divider should be where the test thinks"));
		level.destroyBlock(divider, false);
		helper.runAfterDelay(5, () -> {
			BarnControllerEntity e = controller(helper, c);
			helper.assertTrue(!e.formed(), Component.literal("a barn missing a divider should not form"));
			helper.assertTrue(e.check().problems().size() == 1, Component.literal("only the divider should be wrong, got " + e.check().problems()));
			helper.assertTrue(e.check().status().equals("Stall 3 north has no divider on its near side"), Component.literal("status: " + e.check().status()));
			// the rest still stands: every other part is where it was
			helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(xd + 5, c.getY() - 1, c.getZ() - 4))).getBlock() == FarmingsBlocks.part(BarnParts.STALL_BED),
					Component.literal("the bed next to the missing divider should still stand"));
			helper.succeed();
		});
	}

	/** Breaking any one cell of a big part takes the whole part (Wildcatter's rule): every cell of a stall divider goes. */
	private static void barnPartBreaksWhole(GameTestHelper helper) {
		BlockPos c = new BlockPos(14, 4, 8);
		ServerLevel level = helper.getLevel();
		buildBarn(level, helper.absolutePos(c), 2);
		int xd = c.getX() - 5;
		BlockPos main = helper.absolutePos(new BlockPos(xd + 2, c.getY() - 1, c.getZ() + 2));
		// break the topmost cell of the far end of the divider
		level.destroyBlock(main.offset(0, 1, 2), false);
		helper.runAfterDelay(5, () -> {
			for (int dy = 0; dy < 2; dy++) {
				for (int dz = 0; dz < 3; dz++) {
					helper.assertTrue(level.getBlockState(main.offset(0, dy, dz)).isAir(), Component.literal("every cell of the divider should be gone, " + main.offset(0, dy, dz) + " stayed"));
				}
			}
			helper.succeed();
		});
	}

	/** A barn built the way it is drawn reads as turned 0, not mirrored. (All eight orientations are covered by the plain-Java unit tests.) */
	private static void barnUsualOrientation(GameTestHelper helper) {
		BlockPos c = new BlockPos(14, 4, 8);
		ServerLevel level = helper.getLevel();
		buildBarn(level, helper.absolutePos(c), 2);
		BarnControllerEntity e = controller(helper, c);
		helper.assertTrue(e.formed() && e.check().orientation().turns() == 0 && !e.check().orientation().mirror(),
				Component.literal("a barn drawn the usual way reads as turns 0, not mirrored; got " + e.check().orientation()));
		helper.succeed();
	}

	// ---- the scraper, the pit and manure ----

	private static final BlockPos C = new BlockPos(14, 4, 8);      // the controller, relative to the test

	private static void dropManure(ServerLevel level, BlockPos at, int count) {
		ItemEntity e = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.3, at.getZ() + 0.5, new ItemStack(FarmingsItems.MANURE, count));
		e.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(e);
	}

	private static int manureOnFloor(ServerLevel level, BlockPos around) {
		int n = 0;
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(around).inflate(14, 4, 8), i -> i.getItem().is(FarmingsItems.MANURE))) {
			n += e.getItem().getCount();
		}
		return n;
	}

	private static DriveEntity driveOf(GameTestHelper helper, int stalls) {
		BlockPos drive = helper.absolutePos(C).offset(-(2 * stalls + 1), 0, 0);
		return (DriveEntity) BigParts.entity(helper.getLevel(), drive);
	}

	/** A powered scraper pushes every manure item along the alley into the pit and comes back to rest beside the drive. */
	private static void barnScraperPass(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 3);
		driveOf(helper, 3).forceRunning(true);
		BarnControllerEntity ctl = controller(helper, C);
		PitEntity pit = (PitEntity) BigParts.entity(level, c.below());
		int fl = c.getY() - 1;
		int xd = c.getX() - 7;
		dropManure(level, new BlockPos(xd + 3, fl, c.getZ()), 7);
		dropManure(level, new BlockPos(xd + 5, fl, c.getZ() - 1), 5);
		dropManure(level, new BlockPos(xd + 6, fl, c.getZ() + 1), 3);
		helper.assertTrue(ctl.startPass(level), Component.literal("a pass should start, got " + ctl.readout()));
		helper.succeedWhen(() -> {
			helper.assertTrue(pit.manure() == 15, Component.literal("all 15 manure should reach the pit, it holds " + pit.manure() + "; " + ctl.readout()));
			helper.assertTrue(manureOnFloor(level, c) == 0, Component.literal("no manure should be left on the floor"));
			helper.assertTrue(!ctl.pass().running(), Component.literal("the pass should be over"));
			helper.assertTrue(level.getBlockState(c.offset(-6, 0, 0)).getBlock() == FarmingsBlocks.part(BarnParts.MANURE_SCRAPER),
					Component.literal("the blade should be back beside the drive"));
			helper.assertTrue(ctl.readout().contains("scraper idle"), Component.literal("readout: " + ctl.readout()));
		});
	}

	/** With no power the blade does not move at all, and the controller says NO POWER. */
	private static void barnScraperNoPower(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 3);
		BarnControllerEntity ctl = controller(helper, C);
		dropManure(level, c.offset(-4, -1, 0), 4);
		helper.assertTrue(ctl.startPass(level), Component.literal("a pass starts even unpowered (it then waits)"));
		helper.runAfterDelay(120, () -> {
			helper.assertTrue(level.getBlockState(c.offset(-6, 0, 0)).getBlock() == FarmingsBlocks.part(BarnParts.MANURE_SCRAPER), Component.literal("the blade must not have moved"));
			helper.assertTrue(ctl.readout().contains("NO POWER"), Component.literal("readout: " + ctl.readout()));
			helper.assertTrue(((PitEntity) BigParts.entity(level, c.below())).manure() == 0, Component.literal("nothing reaches the pit"));
			helper.succeed();
		});
	}

	/** A block on the rail stops the blade in front of it; take it away and the pass finishes. */
	private static void barnScraperBlocked(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 3);
		driveOf(helper, 3).forceRunning(true);
		BarnControllerEntity ctl = controller(helper, C);
		PitEntity pit = (PitEntity) BigParts.entity(level, c.below());
		dropManure(level, c.offset(-2, -1, 0), 9);
		BlockPos obstacle = c.offset(-4, 0, 0);          // two cells ahead of where the blade will stop
		level.setBlock(obstacle, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		ctl.startPass(level);
		helper.runAfterDelay(200, () -> {
			helper.assertTrue(ctl.readout().contains("BLOCKED"), Component.literal("readout: " + ctl.readout()));
			helper.assertTrue(level.getBlockState(c.offset(-5, 0, 0)).getBlock() == FarmingsBlocks.part(BarnParts.MANURE_SCRAPER), Component.literal("the blade stops in front of the block"));
			level.setBlock(obstacle, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			helper.succeedWhen(() -> {
				helper.assertTrue(pit.manure() == 9, Component.literal("the manure should reach the pit once cleared, it holds " + pit.manure()));
				helper.assertTrue(!ctl.pass().running(), Component.literal("the pass should finish"));
			});
		});
	}

	/** A full pit stops a pass from starting and the controller says PIT FULL; nothing is lost. */
	private static void barnScraperPitFull(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 3);
		driveOf(helper, 3).forceRunning(true);
		BarnControllerEntity ctl = controller(helper, C);
		PitEntity pit = (PitEntity) BigParts.entity(level, c.below());
		helper.assertTrue(pit.add(1000) == PitEntity.CAPACITY, Component.literal("the pit holds 384"));
		helper.assertTrue(pit.add(1) == 0, Component.literal("a full pit takes nothing more"));
		dropManure(level, c.offset(-3, -1, 0), 5);
		helper.assertTrue(!ctl.startPass(level), Component.literal("no pass should start into a full pit"));
		helper.assertTrue(ctl.pass().fault() == ScraperPass.Fault.PIT_FULL, Component.literal("fault " + ctl.pass().fault()));
		helper.assertTrue(manureOnFloor(level, c) == 5, Component.literal("the manure stays on the floor"));
		helper.assertTrue(ctl.readout().contains("PIT FULL"), Component.literal("readout: " + ctl.readout()));
		helper.succeed();
	}

	/** The manure fork gathers manure lying around a click into the inventory and wears by one. */
	private static void manureFork(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);    // creative players do not wear their tools
		BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
		level.setBlock(at.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		dropManure(level, at, 3);
		dropManure(level, at.offset(1, 0, 1), 4);
		dropManure(level, at.offset(9, 0, 9), 6);       // too far: stays
		ItemStack fork = new ItemStack(FarmingsItems.MANURE_FORK);
		player.setItemInHand(InteractionHand.MAIN_HAND, fork);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at.below()), Direction.UP, at.below(), false);
		FarmingsItems.MANURE_FORK.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		int held = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).is(FarmingsItems.MANURE)) {
				held += player.getInventory().getItem(i).getCount();
			}
		}
		helper.assertTrue(held == 7, Component.literal("the fork should gather 7 manure, got " + held));
		int far = 0;
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(at.offset(9, 0, 9)).inflate(1.5), i -> i.getItem().is(FarmingsItems.MANURE))) {
			far += e.getItem().getCount();
		}
		helper.assertTrue(far == 6, Component.literal("the far manure stays, found " + far));
		helper.assertTrue(fork.getDamageValue() == 1, Component.literal("one use, one wear, got " + fork.getDamageValue()));
		helper.succeed();
	}

	/** An empty hand on the pit takes up to a stack of manure; a hand with an item only reads the pit. */
	private static void pitGivesManure(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		PitEntity pit = (PitEntity) BigParts.entity(level, c.below());
		pit.add(100);
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(c.below()), Direction.UP, c.below(), false);
		BlockState state = level.getBlockState(c.below());
		state.useWithoutItem(level, player, hit);
		helper.assertTrue(pit.manure() == 36, Component.literal("a stack of 64 comes out, leaving 36, got " + pit.manure()));
		int held = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).is(FarmingsItems.MANURE)) {
				held += player.getInventory().getItem(i).getCount();
			}
		}
		helper.assertTrue(held == 64, Component.literal("the player should hold 64, got " + held));
		helper.succeed();
	}

	// ---- cows in the barn ----

	/** A heifer (stage with a dry-cow style intake) standing in the alley of the test barn, no AI unless asked. */
	private static CattleEntity barnCow(ServerLevel level, BlockPos near, double weightKg, boolean ai) {
		CattleEntity c = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
		c.setPos(near.getX() + 0.5, near.getY() + 0.2, near.getZ() + 0.5);
		c.setBreedForDisplay(CattleBreed.HOLSTEIN, Sex.FEMALE, weightKg, false);
		c.setNoAi(!ai);
		level.addFreshEntity(c);
		return c;
	}

	private static FeederEntity feederAt(ServerLevel level, BlockPos bunk) {
		return (FeederEntity) BigParts.entity(level, bunk.above());
	}

	private static TroughEntity anyTrough(ServerLevel level, BlockPos c) {
		return (TroughEntity) BigParts.entity(level, c.offset(1, -1, -2));
	}

	private static void fillBarn(ServerLevel level, BlockPos c) {
		for (int dir = -1; dir <= 1; dir += 2) {
			((TroughEntity) BigParts.entity(level, c.offset(1, -1, dir * 2))).pourBucket();
		}
	}

	/** Five cows and four stalls: each stall goes to one cow, the fifth has none. */
	private static void cowsTakeStalls(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		BarnControllerEntity ctl = controller(helper, C);
		java.util.List<CattleEntity> cows = new java.util.ArrayList<>();
		for (int i = 0; i < 5; i++) {
			cows.add(barnCow(level, c.offset(-4 + (i % 4), -1, i < 3 ? 0 : 1), 650, false));
		}
		ctl.tend(level, 20);
		java.util.Set<BlockPos> stands = new java.util.HashSet<>();
		int housed = 0;
		for (CattleEntity cow : cows) {
			if (cow.stallStand() != null) {
				housed++;
				stands.add(cow.stallStand());
				helper.assertTrue(level.getBlockState(cow.stallStand()).getBlock() == FarmingsBlocks.part(BarnParts.STALL_BED), Component.literal("a stall stand should be a bed cell"));
				helper.assertTrue(ctl.pit(level) != null && cow.stallController().equals(c), Component.literal("she belongs to this barn"));
			}
		}
		helper.assertTrue(housed == 4, Component.literal("4 stalls house 4 cows, got " + housed));
		helper.assertTrue(stands.size() == 4, Component.literal("each stall holds one cow, got " + stands.size() + " distinct"));
		helper.assertTrue(ctl.stallsTaken(level) == 4, Component.literal("the controller counts 4"));
		helper.succeed();
	}

	/** A day of tending takes exactly a day's intake out of the feeder and the trough (the dry-cow rate: 2.2% of 700 kg, 6 L per kg). */
	private static void cowEatsAndDrinks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		fillBarn(level, c);
		BarnControllerEntity ctl = controller(helper, C);
		CattleEntity cow = barnCow(level, c.offset(-4, -1, -1), 700, false);
		ctl.tend(level, 20);
		helper.assertTrue(cow.stallStand() != null, Component.literal("she should have a stall"));
		FeederEntity feeder = feederAt(level, cow.stallBunk());
		feeder.addBlock();
		feeder.addBlock();
		double hayBefore = feeder.dryMatterKg();
		TroughEntity trough = (TroughEntity) BigParts.entity(level, c.offset(1, -1, cow.stallBunk().getZ() < c.getZ() ? -2 : 2));
		double waterBefore = trough.litres();
		cow.setWeightKg(700);
		ctl.tend(level, 24_000);
		double dmi = 0.022 * 700;
		helper.assertTrue(Math.abs((hayBefore - feeder.dryMatterKg()) - dmi) < 0.01, Component.literal("a day takes " + dmi + " kg, took " + (hayBefore - feeder.dryMatterKg())));
		helper.assertTrue(Math.abs((waterBefore - trough.litres()) - 6 * dmi) < 0.01, Component.literal("a day drinks " + 6 * dmi + " L, drank " + (waterBefore - trough.litres())));
		helper.assertTrue(cow.lastFed() == 1.0 && cow.lastWatered() == 1.0, Component.literal("fully fed and watered"));
		helper.assertTrue(Math.abs(cow.hayEatenKg() - dmi) < 0.01, Component.literal("her own count agrees"));
		helper.succeed();
	}

	/** Manure follows what she ate (2.23 kg per kg of dry matter): the first day is short of an item, the second makes one in the alley. */
	private static void cowMakesManure(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		fillBarn(level, c);
		BarnControllerEntity ctl = controller(helper, C);
		CattleEntity cow = barnCow(level, c.offset(-4, -1, -1), 700, false);
		ctl.tend(level, 20);
		FeederEntity feeder = feederAt(level, cow.stallBunk());
		for (int i = 0; i < 4; i++) {
			feeder.addBlock();
		}
		ctl.tend(level, 24_000);
		helper.assertTrue(manureOnFloor(level, c) == 0, Component.literal("34 kg is not yet an item"));
		ctl.tend(level, 24_000);
		helper.assertTrue(manureOnFloor(level, c) == 1, Component.literal("69 kg makes one 50 kg item, found " + manureOnFloor(level, c)));
		helper.succeed();
	}

	/** No hay and no water: she eats and drinks nothing, says so, makes no manure, and lives. */
	private static void cowHungryWithoutHay(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		BarnControllerEntity ctl = controller(helper, C);
		CattleEntity cow = barnCow(level, c.offset(-4, -1, -1), 700, false);
		ctl.tend(level, 20);
		ctl.tend(level, 24_000);
		helper.assertTrue(cow.lastFed() == 0.0 && cow.lastWatered() == 0.0, Component.literal("nothing to eat or drink: " + cow.lastFed() + ", " + cow.lastWatered()));
		helper.assertTrue(manureOnFloor(level, c) == 0, Component.literal("no manure without feed"));
		helper.assertTrue(cow.isAlive(), Component.literal("she never dies from a missed day"));
		helper.succeed();
	}

	/** A hay block goes into a feeder up to 4, and the fifth is refused and kept. */
	private static void feederTakesHayBlocks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getAbilities().instabuild = false;     // a creative player's items are not used up
		BlockPos bunk = c.offset(-4, -1, -5);
		BlockPos feederCell = bunk.above();
		FeederEntity feeder = (FeederEntity) BigParts.entity(level, feederCell);
		ItemStack hay = new ItemStack(net.minecraft.world.item.Items.HAY_BLOCK, 5);
		player.setItemInHand(InteractionHand.MAIN_HAND, hay);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(feederCell), Direction.UP, feederCell, false);
		for (int i = 0; i < 5; i++) {
			level.getBlockState(feederCell).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
		}
		helper.assertTrue(Math.abs(feeder.dryMatterKg() - 340) < 1e-6, Component.literal("4 blocks of 85 kg: " + feeder.dryMatterKg()));
		helper.assertTrue(player.getMainHandItem().getCount() == 1, Component.literal("the fifth block is kept, hand holds " + player.getMainHandItem().getCount()));
		helper.succeed();
	}

	/** A water bucket fills a trough with 1,000 L and comes back empty; a full trough refuses the next. */
	private static void troughTakesBucket(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getAbilities().instabuild = false;
		BlockPos troughCell = c.offset(1, -1, -2);
		TroughEntity trough = (TroughEntity) BigParts.entity(level, troughCell);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(troughCell), Direction.UP, troughCell, false);
		level.getBlockState(troughCell).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(trough.litres() == 1000, Component.literal("a bucket is 1,000 L, trough has " + trough.litres()));
		helper.assertTrue(player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET), Component.literal("the bucket comes back empty"));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
		level.getBlockState(troughCell).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(player.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET), Component.literal("a full trough keeps the bucket full"));
		helper.succeed();
	}

	/** A cow with her own AI, put in the alley, walks to her stall and stands there. */
	private static void cowWalksToStall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		// real barns stand on ground, and the pathfinder needs solid ground below the floor to find a way
		for (int x = c.getX() - 8; x <= c.getX() + 3; x++) {
			for (int z = c.getZ() - 6; z <= c.getZ() + 6; z++) {
				boolean pit = x >= c.getX() && x <= c.getX() + 1 && z >= c.getZ() - 1 && z <= c.getZ() + 1;
				if (!pit) {
					level.setBlock(new BlockPos(x, c.getY() - 2, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}
		buildBarn(level, c, 2);
		controller(helper, C);
		CattleEntity cow = barnCow(level, c.offset(-2, -1, 0), 650, true);
		helper.succeedWhen(() -> {
			BarnControllerEntity ctl = (BarnControllerEntity) level.getBlockEntity(c);
			helper.assertTrue(cow.stallStand() != null, Component.literal("she should be given a stall; ctl=" + (ctl == null ? "none" : ctl.readout()) + " cow=" + cow.position() + " alive=" + cow.isAlive()
					+ " removed=" + cow.isRemoved() + " ctlPos=" + c + " chunkTicking=" + level.isPositionEntityTicking(cow.blockPosition())));
			BlockPos stand = cow.stallStand();
			double d = cow.distanceToSqr(stand.getX() + 0.5, stand.getY() + 0.25, stand.getZ() + 0.5);
			helper.assertTrue(d < 0.8 * 0.8, Component.literal("she should stand in her stall; she is " + Math.sqrt(d) + " away at " + cow.position()));
		});
	}

	/** The only way into the alley is over the pit end: a cow outside the barn, beside the pit, must be able to walk in to her stall. */
	private static void cowWalksInFromThePitEnd(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		for (int x = c.getX() - 8; x <= c.getX() + 6; x++) {
			for (int z = c.getZ() - 7; z <= c.getZ() + 7; z++) {
				boolean pit = x >= c.getX() && x <= c.getX() + 1 && z >= c.getZ() - 1 && z <= c.getZ() + 1;
				if (!pit) {
					level.setBlock(new BlockPos(x, c.getY() - 2, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}
		buildBarn(level, c, 2);
		controller(helper, C);
		CattleEntity cow = barnCow(level, c.offset(3, -1, -2), 650, true);     // east of the pit and the troughs, outside the barn
		helper.succeedWhen(() -> {
			BlockPos stand = cow.stallStand();
			helper.assertTrue(stand != null, Component.literal("she should be given a stall (the barn box reaches past the pit)"));
			double d = cow.distanceToSqr(stand.getX() + 0.5, stand.getY() + 0.25, stand.getZ() + 0.5);
			helper.assertTrue(d < 0.8 * 0.8, Component.literal("she should walk in from the pit end; she is " + Math.sqrt(d) + " from her stall at " + cow.position()));
		});
	}

	private static void cowStallSurvivesSave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		BarnControllerEntity ctl = controller(helper, C);
		CattleEntity a = barnCow(level, c.offset(-4, -1, -1), 650, false);
		ctl.tend(level, 20);
		helper.assertTrue(a.stallStand() != null, Component.literal("housed"));
		net.minecraft.util.ProblemReporter reporter = net.minecraft.util.ProblemReporter.DISCARDING;
		net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, level.registryAccess());
		a.saveWithoutId(out);
		CattleEntity b = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.LOAD);
		b.load(net.minecraft.world.level.storage.TagValueInput.create(reporter, level.registryAccess(), out.buildResult()));
		helper.assertTrue(a.stallStand().equals(b.stallStand()) && a.stallBunk().equals(b.stallBunk()) && a.stallController().equals(b.stallController()),
				Component.literal("her stall should survive a save"));
		helper.succeed();
	}
}
