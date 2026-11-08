package dev.farmings.neoforge.gametest;

import dev.farmings.Farmings;
import dev.farmings.FarmingsBlocks;
import dev.farmings.FarmingsEntities;
import dev.farmings.barn.BarnControllerEntity;
import dev.farmings.barn.BarnParts;
import dev.farmings.husbandry.barn.BarnCheck;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.Direction;
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
	private static final String[] SCENES = {"registers", "cattle_spawns", "cattle_state_survives_save", "calf_grows_up", "bull_does_not_milk", "barn_forms", "barn_missing_part", "barn_part_breaks_whole", "barn_usual_orientation"};

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
		}));
		modBus.addListener(RegisterGameTestsEvent.class, event -> {
			Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"));
			for (String scene : SCENES) {
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						new TestData<>(plain, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), 200, 1, true,
								net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 40)));
			}
		});
	}

	/** Runs a scene with the chunks around it loaded and ticking. */
	private static Consumer<GameTestHelper> loaded(Consumer<GameTestHelper> scene) {
		return helper -> {
			ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
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
		barnPart(level, BarnParts.MANURE_SCRAPER, new BlockPos(xd + 2, c.getY(), c.getZ()), n);
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
		helper.assertTrue(e.readout().equals("Barn: FORMED, 3 stalls a side (6 cows)"), Component.literal("readout: " + e.readout()));
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
}
