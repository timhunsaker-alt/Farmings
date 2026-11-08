package dev.farmings.neoforge.gametest;

import dev.farmings.Farmings;
import dev.farmings.FarmingsEntities;
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
	private static final String[] SCENES = {"registers", "cattle_spawns", "cattle_state_survives_save", "calf_grows_up", "bull_does_not_milk"};

	public FarmingsGameTests(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> event.register(Registries.TEST_FUNCTION, helper -> {
			helper.register(id("registers"), loaded(FarmingsGameTests::registers));
			helper.register(id("cattle_spawns"), loaded(FarmingsGameTests::cattleSpawns));
			helper.register(id("cattle_state_survives_save"), loaded(FarmingsGameTests::cattleStateSurvivesSave));
			helper.register(id("calf_grows_up"), loaded(FarmingsGameTests::calfGrowsUp));
			helper.register(id("bull_does_not_milk"), loaded(FarmingsGameTests::bullDoesNotMilk));
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
}
