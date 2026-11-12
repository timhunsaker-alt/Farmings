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
	private static final String[] SCENES = {"registers", "cattle_spawns", "cattle_state_survives_save", "calf_grows_up", "bull_does_not_milk", "barn_forms", "barn_missing_part", "barn_part_breaks_whole", "barn_usual_orientation", "barn_scraper_pass", "barn_scraper_no_power", "barn_scraper_blocked", "barn_scraper_pit_full", "manure_fork", "pit_gives_manure", "cows_take_stalls", "cow_eats_and_drinks", "cow_makes_manure", "cow_hungry_without_hay", "feeder_takes_hay_blocks", "trough_takes_bucket", "cow_walks_to_stall", "cow_walks_in_from_the_pit_end", "cow_stall_survives_save", "cow_gives_milk", "heifer_comes_into_heat", "bull_serves_cow_in_heat", "calf_is_born_beside_dam", "first_two_calves_are_heifers", "sire_survives_save", "calf_grows_to_heifer", "underfed_heifer_grows_slower", "grown_cow_stops_growing", "weight_survives_save", "straw_inseminates", "parlor_forms", "parlor_missing_part", "parlor_milks_cow", "parlor_no_power_no_milk", "tank_cools_and_warms", "bucket_takes_milk", "parlor_state_survives_save", "parlor_cow_walks_and_is_milked", "parlor_milks_a_herd", "first_lactation_lower", "twins_born", "stillbirth_no_calf", "freemartin_does_not_cycle", "dirty_stall_less_milk", "cow_readout_and_castration", "angus_wagyu_cross", "beef_drops_by_weight", "wagyu_more_steaks", "herd_book_records", "trough_filled_by_pipe", "milk_armor_spoils_and_sickens", "creamery_forms", "creamery_missing_part", "pasteurizer_bottles", "separator_splits_fat", "churn_makes_butter", "cheese_yield_and_aging", "creamery_no_power", "milk_line_from_parlor", "creamery_state_survives_save", "recipes_cover_all_items", "lane_links_barn_and_parlor", "no_lane_no_milking", "cows_wait_in_pen", "cow_walks_lane_and_return", "coat_inherited", "hopper_takes_butter_and_wheels", "hopper_bottles_with_glass", "whey_from_cheese", "milk_spoils_at_room", "spoiled_food_sickens", "cold_room_forms", "cold_room_gap_named", "fridge_slows_spoilage", "cold_room_no_power_warms", "sheep_spawns_and_survives_save", "sheep_lifecycle", "ewe_lambs_twins", "fleece_grows_and_shears", "dorper_sheds", "ewe_milk_by_breed", "sheep_meat_by_age", "shed_forms", "shed_gap_named", "flock_goes_to_pasture_and_back", "ewe_moves_to_lambing_pen", "grazing_feeds", "shearing_shed_forms", "sheep_shorn_by_stand", "no_power_no_shearing", "press_makes_bale", "fleece_to_yarn", "scour_needs_water", "hopper_takes_cloth", "wool_clothing_stops_freezing", "yarn_to_wool_blocks", "ewe_milked_in_sheep_stall", "tank_refuses_mixed_milk", "sheep_cheese_from_creamery", "wild_spawn_is_wild", "wild_survives_save", "barn_skips_wild", "shed_skips_wild", "wild_follows_feed", "wild_biome_spawns_replaced", "wild_natural_spawn_needs_grass", "corral_forms", "corral_gate_is_one_way", "corral_bait_draws_wild", "corral_wild_settles_into_herd", "corral_unfed_wild_does_not_settle", "fowl_founders_per_breed", "fowl_save_survives", "hen_lays_floor_egg", "egg_carries_genes", "bbw_tom_cannot_mate", "fowl_spawn_is_wild", "fowl_biome_spawns_replaced", "fowl_follows_seeds", "fowl_settles_in_corral", "coop_forms", "coop_takes_tame_birds", "hen_lays_in_nest", "broody_hen_hatches", "birds_out_by_day_in_at_night", "duck_needs_pond", "litter_builds_up", "chick_cold_without_heat", "incubator_hatches", "incubator_no_power", "brooder_keeps_chicks", "grading_packs_cartons", "table_eggs_spoil_fertile_dont", "poultry_meat_and_down", "crop_grows_on_clock", "alfalfa_regrows_and_dies", "corn_ripens_for_grain", "ripe_oats_by_hand", "belt_carries_to_chest", "belt_blocked_head_holds", "auger_lifts_grain", "gantry_plants_strip", "gantry_harvests_to_stop", "gantry_mows_rakes_bales", "gantry_runs_on_its_clock", "silo_ferments_and_unloads", "grain_bin_fills_and_empties", "mill_grinds", "mixer_makes_ration", "feeder_ration_feeds_cow", "belt_feeds_barn_feeder", "compost_matures", "compost_waits_for_turns", "digester_fills_holder", "separator_splits", "genset_burns_farm_biogas"};

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
			helper.register(id("cow_gives_milk"), loaded(FarmingsGameTests::cowGivesMilk));
			helper.register(id("heifer_comes_into_heat"), loaded(FarmingsGameTests::heiferComesIntoHeat));
			helper.register(id("bull_serves_cow_in_heat"), loaded(FarmingsGameTests::bullServesCowInHeat));
			helper.register(id("calf_is_born_beside_dam"), loaded(FarmingsGameTests::calfIsBornBesideDam));
			helper.register(id("first_two_calves_are_heifers"), loaded(FarmingsGameTests::firstTwoCalvesAreHeifers));
			helper.register(id("sire_survives_save"), loaded(FarmingsGameTests::sireSurvivesSave));
			helper.register(id("calf_grows_to_heifer"), loaded(FarmingsGameTests::calfGrowsToHeifer));
			helper.register(id("underfed_heifer_grows_slower"), loaded(FarmingsGameTests::underfedHeiferGrowsSlower));
			helper.register(id("grown_cow_stops_growing"), loaded(FarmingsGameTests::grownCowStopsGrowing));
			helper.register(id("weight_survives_save"), loaded(FarmingsGameTests::weightSurvivesSave));
			helper.register(id("straw_inseminates"), loaded(FarmingsGameTests::strawInseminates));
			helper.register(id("parlor_forms"), loaded(FarmingsGameTests::parlorForms));
			helper.register(id("parlor_missing_part"), loaded(FarmingsGameTests::parlorMissingPart));
			helper.register(id("parlor_milks_cow"), loaded(FarmingsGameTests::parlorMilksCow));
			helper.register(id("parlor_no_power_no_milk"), loaded(FarmingsGameTests::parlorNoPowerNoMilk));
			helper.register(id("tank_cools_and_warms"), loaded(FarmingsGameTests::tankCoolsAndWarms));
			helper.register(id("bucket_takes_milk"), loaded(FarmingsGameTests::bucketTakesMilk));
			helper.register(id("parlor_state_survives_save"), loaded(FarmingsGameTests::parlorStateSurvivesSave));
			helper.register(id("parlor_cow_walks_and_is_milked"), loaded(FarmingsGameTests::parlorCowWalks));
			helper.register(id("parlor_milks_a_herd"), loaded(FarmingsGameTests::parlorMilksAHerd));
			helper.register(id("first_lactation_lower"), loaded(FarmingsGameTests::firstLactationLower));
			helper.register(id("twins_born"), loaded(FarmingsGameTests::twinsBorn));
			helper.register(id("stillbirth_no_calf"), loaded(FarmingsGameTests::stillbirthNoCalf));
			helper.register(id("freemartin_does_not_cycle"), loaded(FarmingsGameTests::freemartinDoesNotCycle));
			helper.register(id("dirty_stall_less_milk"), loaded(FarmingsGameTests::dirtyStallLessMilk));
			helper.register(id("cow_readout_and_castration"), loaded(FarmingsGameTests::cowReadoutAndCastration));
			helper.register(id("angus_wagyu_cross"), loaded(FarmingsGameTests::angusWagyuCross));
			helper.register(id("beef_drops_by_weight"), loaded(FarmingsGameTests::beefDropsByWeight));
			helper.register(id("wagyu_more_steaks"), loaded(FarmingsGameTests::wagyuMoreSteaks));
			helper.register(id("herd_book_records"), loaded(FarmingsGameTests::herdBookRecords));
			helper.register(id("trough_filled_by_pipe"), loaded(FarmingsGameTests::troughFilledByPipe));
			helper.register(id("milk_armor_spoils_and_sickens"), loaded(FarmingsGameTests::milkArmorSpoils));
			helper.register(id("creamery_forms"), loaded(FarmingsGameTests::creameryForms));
			helper.register(id("creamery_missing_part"), loaded(FarmingsGameTests::creameryMissingPart));
			helper.register(id("pasteurizer_bottles"), loaded(FarmingsGameTests::pasteurizerBottles));
			helper.register(id("separator_splits_fat"), loaded(FarmingsGameTests::separatorSplitsFat));
			helper.register(id("churn_makes_butter"), loaded(FarmingsGameTests::churnMakesButter));
			helper.register(id("cheese_yield_and_aging"), loaded(FarmingsGameTests::cheeseYieldAndAging));
			helper.register(id("creamery_no_power"), loaded(FarmingsGameTests::creameryNoPower));
			helper.register(id("milk_line_from_parlor"), loaded(FarmingsGameTests::milkLineFromParlor));
			helper.register(id("creamery_state_survives_save"), loaded(FarmingsGameTests::creameryStateSurvivesSave));
			helper.register(id("recipes_cover_all_items"), loaded(FarmingsGameTests::recipesCoverAllItems));
			helper.register(id("lane_links_barn_and_parlor"), loaded(FarmingsGameTests::laneLinksBarnAndParlor));
			helper.register(id("no_lane_no_milking"), loaded(FarmingsGameTests::noLaneNoMilking));
			helper.register(id("cows_wait_in_pen"), loaded(FarmingsGameTests::cowsWaitInPen));
			helper.register(id("cow_walks_lane_and_return"), loaded(FarmingsGameTests::cowWalksLaneAndReturn));
			helper.register(id("coat_inherited"), loaded(FarmingsGameTests::coatInherited));
			helper.register(id("hopper_takes_butter_and_wheels"), loaded(FarmingsGameTests::hopperTakesButterAndWheels));
			helper.register(id("hopper_bottles_with_glass"), loaded(FarmingsGameTests::hopperBottlesWithGlass));
			helper.register(id("whey_from_cheese"), loaded(FarmingsGameTests::wheyFromCheese));
			helper.register(id("milk_spoils_at_room"), loaded(FarmingsGameTests::milkSpoilsAtRoom));
			helper.register(id("spoiled_food_sickens"), loaded(FarmingsGameTests::spoiledFoodSickens));
			helper.register(id("cold_room_forms"), loaded(FarmingsGameTests::coldRoomForms));
			helper.register(id("cold_room_gap_named"), loaded(FarmingsGameTests::coldRoomGapNamed));
			helper.register(id("fridge_slows_spoilage"), loaded(FarmingsGameTests::fridgeSlowsSpoilage));
			helper.register(id("cold_room_no_power_warms"), loaded(FarmingsGameTests::coldRoomNoPowerWarms));
			helper.register(id("sheep_spawns_and_survives_save"), loaded(SheepGameTests::sheepSpawnsAndSurvivesSave));
			helper.register(id("sheep_lifecycle"), loaded(SheepGameTests::sheepLifecycle));
			helper.register(id("ewe_lambs_twins"), loaded(SheepGameTests::eweLambsTwins));
			helper.register(id("fleece_grows_and_shears"), loaded(SheepGameTests::fleeceGrowsAndShears));
			helper.register(id("dorper_sheds"), loaded(SheepGameTests::dorperSheds));
			helper.register(id("ewe_milk_by_breed"), loaded(SheepGameTests::eweMilkByBreed));
			helper.register(id("sheep_meat_by_age"), loaded(SheepGameTests::sheepMeatByAge));
			helper.register(id("shed_forms"), loaded(SheepGameTests::shedForms));
			helper.register(id("shed_gap_named"), loaded(SheepGameTests::shedGapNamed));
			helper.register(id("flock_goes_to_pasture_and_back"), loaded(SheepGameTests::flockGoesToPastureAndBack));
			helper.register(id("ewe_moves_to_lambing_pen"), loaded(SheepGameTests::eweMovesToLambingPen));
			helper.register(id("grazing_feeds"), loaded(SheepGameTests::grazingFeeds));
			helper.register(id("shearing_shed_forms"), loaded(SheepGameTests::shearingShedForms));
			helper.register(id("sheep_shorn_by_stand"), loaded(SheepGameTests::sheepShornByStand));
			helper.register(id("no_power_no_shearing"), loaded(SheepGameTests::noPowerNoShearing));
			helper.register(id("press_makes_bale"), loaded(SheepGameTests::pressMakesBale));
			helper.register(id("fleece_to_yarn"), loaded(SheepGameTests::fleeceToYarn));
			helper.register(id("scour_needs_water"), loaded(SheepGameTests::scourNeedsWater));
			helper.register(id("hopper_takes_cloth"), loaded(SheepGameTests::hopperTakesCloth));
			helper.register(id("wool_clothing_stops_freezing"), loaded(SheepGameTests::woolClothingStopsFreezing));
			helper.register(id("yarn_to_wool_blocks"), loaded(SheepGameTests::yarnToWoolBlocks));
			helper.register(id("ewe_milked_in_sheep_stall"), loaded(FarmingsGameTests::eweMilkedInSheepStall));
			helper.register(id("tank_refuses_mixed_milk"), loaded(FarmingsGameTests::tankRefusesMixedMilk));
			helper.register(id("sheep_cheese_from_creamery"), loaded(FarmingsGameTests::sheepCheeseFromCreamery));
			helper.register(id("wild_spawn_is_wild"), loaded(TamingGameTests::wildSpawnIsWild));
			helper.register(id("fowl_founders_per_breed"), loaded(PoultryGameTests::foundersPerBreed));
			helper.register(id("fowl_save_survives"), loaded(PoultryGameTests::fowlSaveSurvives));
			helper.register(id("hen_lays_floor_egg"), loaded(PoultryGameTests::henLaysFloorEgg));
			helper.register(id("egg_carries_genes"), loaded(PoultryGameTests::eggCarriesGenes));
			helper.register(id("bbw_tom_cannot_mate"), loaded(PoultryGameTests::bbwTomCannotMate));
			helper.register(id("fowl_spawn_is_wild"), loaded(PoultryGameTests::fowlSpawnIsWild));
			helper.register(id("fowl_biome_spawns_replaced"), loaded(PoultryGameTests::fowlBiomeSpawnsReplaced));
			helper.register(id("fowl_follows_seeds"), loaded(PoultryGameTests::fowlFollowsSeeds));
			helper.register(id("fowl_settles_in_corral"), loaded(PoultryGameTests::fowlSettlesInCorral));
			helper.register(id("coop_forms"), loaded(PoultryGameTests::coopForms));
			helper.register(id("coop_takes_tame_birds"), loaded(PoultryGameTests::coopTakesTameBirds));
			helper.register(id("hen_lays_in_nest"), loaded(PoultryGameTests::henLaysInNest));
			helper.register(id("broody_hen_hatches"), loaded(PoultryGameTests::broodyHenHatches));
			helper.register(id("birds_out_by_day_in_at_night"), loaded(PoultryGameTests::birdsOutByDayInAtNight));
			helper.register(id("duck_needs_pond"), loaded(PoultryGameTests::duckNeedsPond));
			helper.register(id("litter_builds_up"), loaded(PoultryGameTests::litterBuildsUp));
			helper.register(id("chick_cold_without_heat"), loaded(PoultryGameTests::chickColdWithoutHeat));
			helper.register(id("incubator_hatches"), loaded(PoultryGameTests::incubatorHatches));
			helper.register(id("incubator_no_power"), loaded(PoultryGameTests::incubatorNoPower));
			helper.register(id("brooder_keeps_chicks"), loaded(PoultryGameTests::brooderKeepsChicks));
			helper.register(id("grading_packs_cartons"), loaded(PoultryGameTests::gradingPacksCartons));
			helper.register(id("table_eggs_spoil_fertile_dont"), loaded(PoultryGameTests::tableEggsSpoil));
			helper.register(id("poultry_meat_and_down"), loaded(PoultryGameTests::poultryMeatAndDown));
			helper.register(id("crop_grows_on_clock"), loaded(CropGameTests::cropGrowsOnClock));
			helper.register(id("alfalfa_regrows_and_dies"), loaded(CropGameTests::alfalfaRegrowsAndDies));
			helper.register(id("corn_ripens_for_grain"), loaded(CropGameTests::cornRipensForGrain));
			helper.register(id("ripe_oats_by_hand"), loaded(CropGameTests::ripeOatsByHand));
			helper.register(id("belt_carries_to_chest"), loaded(ConveyorGameTests::beltCarriesToChest));
			helper.register(id("belt_blocked_head_holds"), loaded(ConveyorGameTests::blockedHeadHolds));
			helper.register(id("auger_lifts_grain"), loaded(ConveyorGameTests::augerLiftsGrain));
			helper.register(id("gantry_plants_strip"), loaded(GantryGameTests::gantryPlantsStrip));
			helper.register(id("gantry_harvests_to_stop"), loaded(GantryGameTests::gantryHarvestsToStop));
			helper.register(id("gantry_mows_rakes_bales"), loaded(GantryGameTests::gantryMowsRakesBales));
			helper.register(id("gantry_runs_on_its_clock"), loaded(GantryGameTests::gantryRunsOnItsClock));
			helper.register(id("silo_ferments_and_unloads"), loaded(SiloGameTests::siloFermentsAndUnloads));
			helper.register(id("grain_bin_fills_and_empties"), loaded(SiloGameTests::grainBinFillsAndEmpties));
			helper.register(id("mill_grinds"), loaded(FeedMillGameTests::millGrinds));
			helper.register(id("mixer_makes_ration"), loaded(FeedMillGameTests::mixerMakesRation));
			helper.register(id("feeder_ration_feeds_cow"), loaded(FeedMillGameTests::feederRationFeedsCow));
			helper.register(id("belt_feeds_barn_feeder"), loaded(FeedMillGameTests::beltFeedsBarnFeeder));
			helper.register(id("compost_matures"), loaded(CompostGameTests::compostMatures));
			helper.register(id("compost_waits_for_turns"), loaded(CompostGameTests::compostWaitsForTurns));
			helper.register(id("digester_fills_holder"), loaded(DigesterGameTests::digesterFillsHolder));
			helper.register(id("separator_splits"), loaded(DigesterGameTests::separatorSplits));
			helper.register(id("genset_burns_farm_biogas"), loaded(DigesterGameTests::gensetBurnsFarmBiogas));
			helper.register(id("wild_survives_save"), loaded(TamingGameTests::wildSurvivesSave));
			helper.register(id("barn_skips_wild"), loaded(FarmingsGameTests::barnSkipsWild));
			helper.register(id("shed_skips_wild"), loaded(TamingGameTests::shedSkipsWild));
			helper.register(id("wild_follows_feed"), loaded(TamingGameTests::wildFollowsFeed));
			helper.register(id("wild_biome_spawns_replaced"), loaded(TamingGameTests::biomeSpawnsReplaced));
			helper.register(id("wild_natural_spawn_needs_grass"), loaded(TamingGameTests::naturalSpawnNeedsGrass));
			helper.register(id("corral_forms"), loaded(TamingGameTests::corralForms));
			helper.register(id("corral_gate_is_one_way"), loaded(TamingGameTests::gateIsOneWay));
			helper.register(id("corral_bait_draws_wild"), loaded(TamingGameTests::baitDrawsWildThroughGate));
			helper.register(id("corral_wild_settles_into_herd"), loaded(TamingGameTests::wildSettlesIntoHerd));
			helper.register(id("corral_unfed_wild_does_not_settle"), loaded(TamingGameTests::unfedWildDoesNotSettle));
		}));
		modBus.addListener(RegisterGameTestsEvent.class, event -> {
			Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"));
			String only = System.getenv("FARMINGS_TESTS");     // a comma-separated filter, for running a few scenes while debugging
			for (String scene : SCENES) {
				if (only != null && !only.isBlank() && java.util.Arrays.stream(only.split(",")).noneMatch(f -> scene.contains(f.trim()))) {
					continue;
				}
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						new TestData<>(plain, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), scene.startsWith("barn_scraper") || scene.startsWith("flock_") || scene.startsWith("ewe_moves") || scene.startsWith("sheep_shorn") || scene.startsWith("fleece_to_yarn") || scene.startsWith("cow_walks") || scene.startsWith("parlor_cow_walks") || scene.startsWith("hopper") || scene.startsWith("wild_follows") || scene.startsWith("fowl_follows") || scene.startsWith("gantry_runs") || scene.startsWith("silo_") || scene.startsWith("genset_burns") || scene.startsWith("belt_feeds") || scene.startsWith("corral_bait") ? 1200 : 200, 1, true,
								net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, padding(scene))));
			}
		});
	}

	/**
	 * Space around a scene. A sheep shed takes in any tame, unhoused sheep within 48 blocks, so shed scenes stand 64 apart, or one scene's shed
	 * claims the next scene's sheep (a cause of the flaky walking scenes, 2026-11-11).
	 */
	private static int padding(String scene) {
		return scene.startsWith("flock_") || scene.startsWith("ewe_") || scene.startsWith("shed") || scene.startsWith("sheep") || scene.startsWith("lamb")
				|| scene.startsWith("graz") || scene.startsWith("fleece") || scene.startsWith("shear") || scene.startsWith("ram") ? 64 : 40;
	}

	/** How many running scenes hold each forced chunk: a chunk is let go when the last scene using it ends. */
	private static final java.util.Map<Long, Integer> FORCED = new java.util.HashMap<>();

	/**
	 * Runs a scene with the chunks around it loaded and ticking, and lets them go when the scene passes or fails. (Left forced, 80 scenes'
	 * barns, parlors and cows kept ticking after the run; with the machine short of memory the last vanilla test could not load its own
	 * chunks and the server ran out of heap.)
	 */
	private static Consumer<GameTestHelper> loaded(Consumer<GameTestHelper> scene) {
		return helper -> {
			ServerLevel level = helper.getLevel();
			ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
			java.util.List<ChunkPos> mine = new java.util.ArrayList<>();
			for (int dx = -1; dx <= 2; dx++) {
				for (int dz = -1; dz <= 2; dz++) {
					ChunkPos c = new ChunkPos(origin.x() + dx, origin.z() + dz);
					mine.add(c);
					FORCED.merge(c.pack(), 1, Integer::sum);
					level.setChunkForced(c.x(), c.z(), true);
				}
			}
			Runnable release = () -> {
				for (ChunkPos c : mine) {
					if (FORCED.get(c.pack()) != null && FORCED.get(c.pack()) <= 1) {
						// Detach the scene's circuit parts before its chunks go: Wildcatter unregisters a part only when its chunk
						// actually unloads, and until then its solver reads the part's cells, which loads the chunk again, tick after tick
						// (with 87 scenes that churn ran the server out of heap).
						net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(c.x(), c.z());
						for (BlockPos p : new java.util.ArrayList<>(chunk.getBlockEntities().keySet())) {
							if (chunk.getBlockEntity(p) instanceof dev.wildcatter.circuit.CircuitPart) {
								level.removeBlockEntity(p);
							}
						}
					}
					if (FORCED.merge(c.pack(), -1, Integer::sum) <= 0) {
						FORCED.remove(c.pack());
						level.setChunkForced(c.x(), c.z(), false);
					}
				}
			};
			try {
				java.lang.reflect.Field f = GameTestHelper.class.getDeclaredField("testInfo");
				f.setAccessible(true);
				((net.minecraft.gametest.framework.GameTestInfo) f.get(helper)).addListener(new net.minecraft.gametest.framework.GameTestListener() {
					@Override
					public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo info) {
					}

					@Override
					public void testPassed(net.minecraft.gametest.framework.GameTestInfo info, net.minecraft.gametest.framework.GameTestRunner runner) {
						release.run();
					}

					@Override
					public void testFailed(net.minecraft.gametest.framework.GameTestInfo info, net.minecraft.gametest.framework.GameTestRunner runner) {
						release.run();
					}

					@Override
					public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo a, net.minecraft.gametest.framework.GameTestInfo b,
							net.minecraft.gametest.framework.GameTestRunner runner) {
					}
				});
			} catch (ReflectiveOperationException e) {
				Farmings.LOGGER.warn("Game test chunks stay forced: {}", e.toString());
			}
			scene.accept(helper);
		};
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("farmings_gametest", path);
	}

	/** A flat floor under the scene so the animals do not fall. */
	static void floor(GameTestHelper helper) {
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
		for (Trait t : dev.farmings.genetics.Species.CATTLE.traits()) {
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

	static void barnPart(ServerLevel level, BarnParts kind, BlockPos main, Direction facing) {
		BlockState state = FarmingsBlocks.part(kind).defaultBlockState().setValue(BigPartBlock.FACING, facing);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
	}

	/**
	 * A whole barn, alley east-west, pit at the east end, controller at {@code c}: the alley is twice the 2n+1-cell stall row (BarnLayout.alleyLength).
	 * Everything is placed facing north, as the footprints are drawn.
	 */
	private static void buildBarn(ServerLevel level, BlockPos c, int stalls) {
		int fl = c.getY() - 1;
		int xd = c.getX() - dev.farmings.husbandry.barn.BarnLayout.alleyLength(stalls);
		int row = xd + stalls;
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
				barnPart(level, BarnParts.STALL_DIVIDER, new BlockPos(row + 2 * j, fl, firstZ), n);
			}
			for (int i = 0; i < stalls; i++) {
				int x = row + 1 + 2 * i;
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
		// the divider between stall 2 and stall 3 on the north side: x = row + 4, its cells run from z = cz - 4 for 3 cells, 2 high
		// (3 stalls: a 14-cell alley, the row starting 3 cells from the drive)
		int xd = c.getX() - 11;
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
		int xd = c.getX() - 8;                           // 2 stalls: a 10-cell alley, the row starting 2 cells from the drive
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
		BlockPos drive = helper.absolutePos(C).offset(-dev.farmings.husbandry.barn.BarnLayout.alleyLength(stalls), 0, 0);
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
		int xd = c.getX() - dev.farmings.husbandry.barn.BarnLayout.alleyLength(3);   // 14 cells: open, stall row, open
		dropManure(level, new BlockPos(xd + 2, fl, c.getZ()), 7);         // the drive-end open stretch
		dropManure(level, new BlockPos(xd + 7, fl, c.getZ() - 1), 5);     // in front of the stalls
		dropManure(level, new BlockPos(xd + 12, fl, c.getZ() + 1), 3);    // the pit-end open stretch
		helper.assertTrue(ctl.startPass(level), Component.literal("a pass should start, got " + ctl.readout()));
		helper.succeedWhen(() -> {
			helper.assertTrue(pit.manure() == 15, Component.literal("all 15 manure should reach the pit, it holds " + pit.manure() + "; " + ctl.readout()));
			helper.assertTrue(manureOnFloor(level, c) == 0, Component.literal("no manure should be left on the floor"));
			helper.assertTrue(!ctl.pass().running(), Component.literal("the pass should be over"));
			helper.assertTrue(level.getBlockState(new BlockPos(xd + 1, c.getY(), c.getZ())).getBlock() == FarmingsBlocks.part(BarnParts.MANURE_SCRAPER),
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
			helper.assertTrue(level.getBlockState(c.offset(1 - dev.farmings.husbandry.barn.BarnLayout.alleyLength(3), 0, 0)).getBlock() == FarmingsBlocks.part(BarnParts.MANURE_SCRAPER), Component.literal("the blade must not have moved"));
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

	/** A barn houses a herd cow but not a wild one beside her (docs/taming.md). */
	private static void barnSkipsWild(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		BarnControllerEntity ctl = controller(helper, C);
		CattleEntity tame = barnCow(level, c.offset(-4, -1, 0), 650, false);
		CattleEntity wild = barnCow(level, c.offset(-3, -1, 0), 650, false);
		wild.setWild(true);
		ctl.tend(level, 20);
		helper.assertTrue(tame.stallStand() != null, Component.literal("the herd cow takes a stall"));
		helper.assertTrue(wild.stallStand() == null, Component.literal("the wild cow does not"));
		helper.succeed();
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
		BlockPos bunk = c.offset(-7, -1, -5);          // stall 1 north of a 2-stall barn
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

	private static void cowGivesMilk(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CattleEntity cow = cow(helper, CattleBreed.HOLSTEIN, Sex.FEMALE, 5L, 900, 650);
		helper.assertTrue(cow.milkKg() == 0, Component.literal("a heifer has no milk"));
		cow.setCalvedForDisplay(69);
		helper.assertTrue(cow.stage() == Stage.LACTATING_COW, Component.literal("she is lactating, got " + cow.stage()));
		helper.assertTrue(cow.daysInMilk() == 70, Component.literal("day 70, got " + cow.daysInMilk()));
		double kg = cow.milkKg();
		double expected = dev.farmings.husbandry.Lactation.producedKg(CattleBreed.HOLSTEIN, 70, cow.score(Trait.MILK_YIELD), 1, 1);
		helper.assertTrue(kg > 15 && kg < 80 && Math.abs(kg - expected) < 0.01, Component.literal("peak-week milk should be " + expected + ", got " + kg));
		// The udder survives a save.
		net.minecraft.util.ProblemReporter reporter = net.minecraft.util.ProblemReporter.DISCARDING;
		net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, level.registryAccess());
		cow.saveWithoutId(out);
		CattleEntity copy = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.LOAD);
		copy.load(net.minecraft.world.level.storage.TagValueInput.create(reporter, level.registryAccess(), out.buildResult()));
		helper.assertTrue(Math.abs(copy.milkKg() - kg) < 1e-9, Component.literal("her milk should survive a save"));
		// A milking machine takes it all.
		helper.assertTrue(Math.abs(cow.takeMilk() - kg) < 1e-9 && cow.milkKg() == 0, Component.literal("milking empties the udder"));
		// A dry cow, a bull and a beef cow give none.
		CattleEntity dry = cow(helper, CattleBreed.JERSEY, Sex.FEMALE, 6L, 1500, 450);
		dry.setCalvedForDisplay(400);
		helper.assertTrue(dry.stage() == Stage.DRY_COW && dry.milkKg() == 0, Component.literal("a dry cow gives no milk"));
		CattleEntity beef = cow(helper, CattleBreed.HOLSTEIN, Sex.FEMALE, 7L, 900, 650);
		beef.setBreedForDisplay(CattleBreed.ANGUS, Sex.FEMALE, 550, false);
		beef.setCalvedForDisplay(60);
		helper.assertTrue(beef.milkKg() == 0, Component.literal("a beef cow is not milked"));
		helper.succeed();
	}

	// ---- breeding and calving ----

	private static CattleEntity founderAt(ServerLevel level, BlockPos near, Sex sex, long seed, double ageDays, double weightKg) {
		CattleEntity c = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
		c.setPos(near.getX() + 0.5, near.getY() + 0.2, near.getZ() + 0.5);
		c.initFounder(CattleBreed.HOLSTEIN, sex, seed, ageDays, weightKg, level.getGameTime());
		c.setNoAi(true);
		level.addFreshEntity(c);
		return c;
	}

	private static void heiferComesIntoHeat(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity young = founderAt(level, p, Sex.FEMALE, 11L, 300, 300);
		young.settle(level);
		helper.assertTrue(!young.inHeat() && !young.canConceive(), Component.literal("a young heifer does not cycle"));
		CattleEntity heifer = founderAt(level, p, Sex.FEMALE, 12L, 500, 400);
		helper.assertTrue(heifer.stage() == Stage.HEIFER, Component.literal("a heifer at 15 months and 400 kg, got " + heifer.stage()));
		heifer.settle(level);
		helper.assertTrue(!heifer.canConceive() || heifer.inHeat(), Component.literal("consistent"));
		heifer.startHeatForDisplay();
		helper.assertTrue(heifer.inHeat() && heifer.canConceive(), Component.literal("she is in heat when a window opens"));
		heifer.settle(level);
		helper.assertTrue(heifer.inHeat(), Component.literal("settling keeps the open window"));
		helper.succeed();
	}

	private static void bullServesCowInHeat(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		BarnControllerEntity ctl = controller(helper, C);
		CattleEntity bull = founderAt(level, c.offset(-4, -1, -1), Sex.MALE, 21L, 1500, 800);
		CattleEntity cow = founderAt(level, c.offset(-3, -1, -1), Sex.FEMALE, 22L, 500, 400);
		CattleEntity other = founderAt(level, c.offset(-2, -1, -1), Sex.FEMALE, 23L, 500, 400);
		helper.assertTrue(bull.stage() == Stage.BULL, Component.literal("a bull, got " + bull.stage()));
		cow.startHeatForDisplay();
		ctl.tend(level, 20);
		helper.assertTrue(bull.stallStand() != null && cow.stallStand() != null, Component.literal("both are housed (the bull has his own stall)"));
		helper.assertTrue(cow.isInCalf() && cow.hasSire(), Component.literal("the bull served the cow in heat"));
		helper.assertTrue(!other.isInCalf(), Component.literal("a cow not in heat is not served"));
		helper.assertTrue(!cow.inHeat(), Component.literal("heat ends once she is in calf"));
		helper.succeed();
	}

	private static CattleEntity calveOnce(ServerLevel level, CattleEntity dam, CattleEntity sire) {
		dam.forceCalvingForTest(1, false, false);   // one live calf, so the rolls cannot make a test flaky
		return calveAs(level, dam, sire).stream().findFirst().orElse(null);
	}

	/** Breeds and calves her now; returns every new calf (none, one or two). */
	private static java.util.List<CattleEntity> calveAs(ServerLevel level, CattleEntity dam, CattleEntity sire) {
		java.util.List<CattleEntity> out = new java.util.ArrayList<>();
		dam.startHeatForDisplay();
		if (!dam.conceive(sire.genome(), sire.visualGenome(), level.getGameTime())) {
			return out;
		}
		java.util.Set<java.util.UUID> before = new java.util.HashSet<>();
		for (CattleEntity e : level.getEntitiesOfClass(CattleEntity.class, dam.getBoundingBox().inflate(6))) {
			before.add(e.getUUID());
		}
		dam.makeDueNow();
		dam.settle(level);
		for (CattleEntity e : level.getEntitiesOfClass(CattleEntity.class, dam.getBoundingBox().inflate(6))) {
			if (!before.contains(e.getUUID())) {
				out.add(e);
			}
		}
		return out;
	}

	private static CattleEntity breedAt(ServerLevel level, BlockPos near, CattleBreed breed, Sex sex, long seed, double ageDays, double weightKg) {
		CattleEntity c = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.COMMAND);
		c.setPos(near.getX() + 0.5, near.getY() + 0.2, near.getZ() + 0.5);
		c.initFounder(breed, sex, seed, ageDays, weightKg, level.getGameTime());
		c.setNoAi(true);
		level.addFreshEntity(c);
		return c;
	}

	// ---- dairy finish and beef (2026-11-09) ----

	private static void firstLactationLower(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = founderAt(level, p, Sex.MALE, 61L, 1500, 800);
		CattleEntity dam = founderAt(level, p, Sex.FEMALE, 62L, 500, 400);
		calveOnce(level, dam, bull);
		double expected = dev.farmings.husbandry.Lactation.producedKg(CattleBreed.HOLSTEIN, 1, dam.score(Trait.MILK_YIELD), 1, 1, 1, 0);
		double later = dev.farmings.husbandry.Lactation.producedKg(CattleBreed.HOLSTEIN, 1, dam.score(Trait.MILK_YIELD), 1, 1, 2, 0);
		helper.assertTrue(dam.lactations() == 1, Component.literal("one lactation, got " + dam.lactations()));
		helper.assertTrue(Math.abs(dam.milkKg() - expected) < 1e-6 && Math.abs(expected - later * 0.85) < 1e-6,
				Component.literal("first lactation is 85%: " + dam.milkKg() + " vs " + later));
		helper.succeed();
	}

	private static void twinsBorn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = founderAt(level, p, Sex.MALE, 63L, 1500, 800);
		CattleEntity dam = founderAt(level, p, Sex.FEMALE, 64L, 500, 400);
		dam.forceCalvingForTest(2, false, false);
		java.util.List<CattleEntity> calves = calveAs(level, dam, bull);
		helper.assertTrue(calves.size() == 2, Component.literal("twins, got " + calves.size()));
		helper.assertTrue(dam.lastCalving().size() == 2 && dam.stage() == Stage.LACTATING_COW, Component.literal("both recorded, she milks"));
		helper.succeed();
	}

	private static void stillbirthNoCalf(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = founderAt(level, p, Sex.MALE, 65L, 1500, 800);
		CattleEntity dam = founderAt(level, p, Sex.FEMALE, 66L, 500, 400);
		dam.forceCalvingForTest(1, true, false);
		java.util.List<CattleEntity> calves = calveAs(level, dam, bull);
		helper.assertTrue(calves.isEmpty(), Component.literal("a stillborn calf is not spawned, got " + calves.size()));
		helper.assertTrue(dam.lastCalving().get(0).startsWith("stillborn"), Component.literal("recorded as stillborn"));
		helper.assertTrue(dam.isAlive() && dam.stage() == Stage.LACTATING_COW && dam.milkKg() > 0, Component.literal("she lives and milks"));
		helper.succeed();
	}

	private static void freemartinDoesNotCycle(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = founderAt(level, p, Sex.MALE, 67L, 1500, 800);
		CattleEntity dam = founderAt(level, p, Sex.FEMALE, 68L, 500, 400);
		dam.setOwner(java.util.UUID.randomUUID());   // the first two calves are heifers
		dam.forceCalvingForTest(2, false, true);
		java.util.List<CattleEntity> calves = calveAs(level, dam, bull);
		helper.assertTrue(calves.size() == 2 && calves.stream().allMatch(CattleEntity::freemartin), Component.literal("heifer twins with a bull are freemartins"));
		CattleEntity fm = calves.get(0);
		fm.setWeightKg(420);
		fm.advanceForTest(500);
		fm.settle(level);
		fm.startHeatForDisplay();
		helper.assertTrue(!fm.canConceive(), Component.literal("a freemartin cannot conceive"));
		// it survives a save
		ProblemReporter reporter = ProblemReporter.DISCARDING;
		TagValueOutput out = TagValueOutput.createWithContext(reporter, level.registryAccess());
		fm.saveWithoutId(out);
		CattleEntity copy = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.LOAD);
		copy.load(TagValueInput.create(reporter, level.registryAccess(), out.buildResult()));
		helper.assertTrue(copy.freemartin(), Component.literal("freemartin survives a save"));
		helper.succeed();
	}

	private static void dirtyStallLessMilk(GameTestHelper helper) {
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
		BlockPos alley = ctl.alleyInFrontOf(cow);
		helper.assertTrue(alley != null, Component.literal("her alley cell is known"));
		ctl.tend(level, 20);
		helper.assertTrue(cow.dirty() == 0, Component.literal("a clean alley is clean, got " + cow.dirty()));
		dropManure(level, alley, 9);
		float before = cow.getHealth();
		ctl.tend(level, 24_000);
		helper.assertTrue(cow.dirty() == 1, Component.literal("nine items is fully dirty, got " + cow.dirty()));
		helper.assertTrue(Math.abs(cow.getHealth() - (before - 1)) < 1e-3, Component.literal("one health a game day, " + before + " -> " + cow.getHealth()));
		for (int i = 0; i < 20; i++) {
			ctl.tend(level, 24_000);
		}
		helper.assertTrue(cow.isAlive() && cow.getHealth() >= 2 - 1e-3, Component.literal("never below 2, got " + cow.getHealth()));
		helper.assertTrue(Math.abs(dev.farmings.husbandry.Hygiene.milkFactor(cow.dirty()) - 0.9) < 1e-9, Component.literal("10% less milk"));
		helper.succeed();
	}

	private static void cowReadoutAndCastration(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity cow = founderAt(level, p, Sex.FEMALE, 69L, 500, 400);
		cow.startHeatForDisplay();
		String r = cow.readout();
		helper.assertTrue(r.startsWith("holstein female") && r.contains("IN HEAT") && r.contains("400 kg") && r.contains("health"), Component.literal("readout: " + r));
		CattleEntity calf = founderAt(level, p, Sex.MALE, 70L, 30, 60);
		helper.assertTrue(calf.stage() == Stage.BULL_CALF, Component.literal("a bull calf, got " + calf.stage()));
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.SHEARS));
		calf.mobInteract(player, InteractionHand.MAIN_HAND);
		calf.setWeightKg(400);
		helper.assertTrue(calf.stage() == Stage.STEER, Component.literal("shears make a steer, got " + calf.stage()));
		helper.succeed();
	}

	private static void angusWagyuCross(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = breedAt(level, p, CattleBreed.WAGYU, Sex.MALE, 71L, 1500, 480);
		CattleEntity dam = breedAt(level, p, CattleBreed.ANGUS, Sex.FEMALE, 72L, 500, 320);
		CattleEntity calf = calveOnce(level, dam, bull);
		helper.assertTrue(calf != null && calf.genome().breedCompositionBp().get("WAG") == 5000, Component.literal("an Angus x Wagyu calf"));
		helper.assertTrue(dam.milkKg() == 0, Component.literal("a beef cow is not milked"));
		helper.assertTrue(calf.readout().contains("50%"), Component.literal("the readout shows the cross: " + calf.readout()));
		helper.succeed();
	}

	private static java.util.Map<net.minecraft.world.item.Item, Integer> dropsOf(ServerLevel level, CattleEntity animal) {
		net.minecraft.world.phys.AABB box = animal.getBoundingBox().inflate(4);
		animal.kill(level);
		java.util.Map<net.minecraft.world.item.Item, Integer> got = new java.util.HashMap<>();
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
			got.merge(e.getItem().getItem(), e.getItem().getCount(), Integer::sum);
		}
		return got;
	}

	private static void beefDropsByWeight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity steer = breedAt(level, p, CattleBreed.ANGUS, Sex.MALE, 73L, 700, 550);
		java.util.Map<net.minecraft.world.item.Item, Integer> got = dropsOf(level, steer);
		helper.assertTrue(got.getOrDefault(net.minecraft.world.item.Items.BEEF, 0) == 13, Component.literal("13 beef (10 kg each), got " + got));
		helper.assertTrue(got.getOrDefault(FarmingsItems.CHOICE_STEAK, 0) == 3, Component.literal("3 Choice steaks, got " + got));
		int leather = got.getOrDefault(net.minecraft.world.item.Items.LEATHER, 0);
		helper.assertTrue(leather >= 1 && leather <= 3, Component.literal("1-3 leather, got " + leather));
		helper.succeed();
	}

	private static void wagyuMoreSteaks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity w = breedAt(level, p, CattleBreed.WAGYU, Sex.MALE, 74L, 700, 487);
		java.util.Map<net.minecraft.world.item.Item, Integer> got = dropsOf(level, w);
		helper.assertTrue(got.getOrDefault(FarmingsItems.PRIME_STEAK, 0) == 4, Component.literal("4 Prime steaks (Angus gives 3), got " + got));
		helper.succeed();
	}

	private static void calfIsBornBesideDam(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = founderAt(level, p, Sex.MALE, 31L, 1500, 800);
		CattleEntity dam = founderAt(level, p, Sex.FEMALE, 32L, 500, 400);
		dam.setOwner(java.util.UUID.randomUUID());
		CattleEntity calf = calveOnce(level, dam, bull);
		helper.assertTrue(calf != null, Component.literal("a calf is born"));
		helper.assertTrue(calf.stage() == Stage.CALF && calf.isBaby(), Component.literal("a newborn is a calf, got " + calf.stage()));
		helper.assertTrue(calf.genome() != null && calf.genome().generation() == 1, Component.literal("the calf has a bred genome, one generation on"));
		helper.assertTrue(calf.sex() == Sex.FEMALE, Component.literal("the owner's first calf is a heifer"));
		helper.assertTrue(calf.distanceTo(dam) < 3, Component.literal("it appears beside her"));
		helper.assertTrue(dam.stage() == Stage.LACTATING_COW && !dam.isInCalf() && !dam.hasSire(), Component.literal("she now gives milk, got " + dam.stage()));
		helper.assertTrue(dam.milkKg() > 0, Component.literal("her udder fills"));
		helper.succeed();
	}

	private static void firstTwoCalvesAreHeifers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = founderAt(level, p, Sex.MALE, 41L, 1500, 800);
		java.util.UUID player = java.util.UUID.randomUUID();
		Sex[] got = new Sex[3];
		for (int i = 0; i < 3; i++) {
			CattleEntity dam = founderAt(level, p, Sex.FEMALE, 50L + i, 500, 400);
			dam.setOwner(player);
			CattleEntity calf = calveOnce(level, dam, bull);
			helper.assertTrue(calf != null, Component.literal("calf " + (i + 1) + " is born"));
			got[i] = calf.sex();
		}
		helper.assertTrue(got[0] == Sex.FEMALE && got[1] == Sex.FEMALE && got[2] == Sex.MALE,
				Component.literal("heifer, heifer, bull; got " + java.util.Arrays.toString(got)));
		helper.succeed();
	}

	private static void sireSurvivesSave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = founderAt(level, p, Sex.MALE, 61L, 1500, 800);
		CattleEntity dam = founderAt(level, p, Sex.FEMALE, 62L, 500, 400);
		java.util.UUID player = java.util.UUID.randomUUID();
		dam.setOwner(player);
		dam.startHeatForDisplay();
		helper.assertTrue(dam.conceive(bull.genome(), level.getGameTime()), Component.literal("conceives"));
		net.minecraft.util.ProblemReporter reporter = net.minecraft.util.ProblemReporter.DISCARDING;
		net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, level.registryAccess());
		dam.saveWithoutId(out);
		CattleEntity copy = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.LOAD);
		copy.load(net.minecraft.world.level.storage.TagValueInput.create(reporter, level.registryAccess(), out.buildResult()));
		helper.assertTrue(copy.isInCalf() && copy.hasSire() && player.equals(copy.owner()), Component.literal("her pregnancy, sire and owner survive a save"));
		helper.succeed();
	}

	// ---- growth ----

	private static CattleEntity newborn(ServerLevel level, BlockPos p, long seed) {
		CattleEntity c = founderAt(level, p, Sex.FEMALE, seed, 0, dev.farmings.husbandry.Growth.targetKg(CattleBreed.HOLSTEIN, 0));
		c.settle(level);
		return c;
	}

	private static void calfGrowsToHeifer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity calf = newborn(level, p, 71L);
		helper.assertTrue(calf.stage() == Stage.CALF, Component.literal("born a calf, got " + calf.stage()));
		calf.advanceForTest(456);
		calf.settle(level);
		helper.assertTrue(Math.abs(calf.weightKg() - 425) < 1, Component.literal("a Holstein is 425 kg at 15 months, got " + calf.weightKg()));
		helper.assertTrue(calf.stage() == Stage.HEIFER, Component.literal("she is a heifer by then, got " + calf.stage()));
		helper.succeed();
	}

	private static void underfedHeiferGrowsSlower(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		double start = dev.farmings.husbandry.Growth.targetKg(CattleBreed.HOLSTEIN, 500);
		CattleEntity fed = founderAt(level, p, Sex.FEMALE, 72L, 500, start);
		CattleEntity hungry = founderAt(level, p, Sex.FEMALE, 73L, 500, start);
		helper.assertTrue(hungry.stage() != Stage.CALF, Component.literal("old enough to need feed, got " + hungry.stage()));
		hungry.assignStall(p, p.offset(1, 0, 0), p.offset(2, 0, 0));
		hungry.settle(level);
		fed.settle(level);
		hungry.recordTending(0, 0, 0.5, 1);
		hungry.advanceForTest(400);
		hungry.settle(level);
		fed.advanceForTest(400);
		fed.settle(level);
		helper.assertTrue(hungry.weightKg() < fed.weightKg() - 5,
				Component.literal("the half-fed heifer is lighter: " + hungry.weightKg() + " vs " + fed.weightKg()));
		helper.assertTrue(hungry.weightKg() > start + 10, Component.literal("but she still grows"));
		helper.succeed();
	}

	private static void grownCowStopsGrowing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity cow = founderAt(level, p, Sex.FEMALE, 74L, 2000, 710);
		cow.settle(level);
		cow.advanceForTest(300);
		cow.settle(level);
		helper.assertTrue(cow.weightKg() == 710, Component.literal("a grown cow keeps 710 kg, got " + cow.weightKg()));
		helper.succeed();
	}

	private static void weightSurvivesSave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity calf = newborn(level, p, 75L);
		calf.advanceForTest(100);
		calf.settle(level);
		double w = calf.weightKg();
		net.minecraft.util.ProblemReporter reporter = net.minecraft.util.ProblemReporter.DISCARDING;
		net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, level.registryAccess());
		calf.saveWithoutId(out);
		CattleEntity copy = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.LOAD);
		copy.load(net.minecraft.world.level.storage.TagValueInput.create(reporter, level.registryAccess(), out.buildResult()));
		helper.assertTrue(Math.abs(copy.weightKg() - w) < 1e-9 && w > 50, Component.literal("weight survives a save: " + w + " vs " + copy.weightKg()));
		copy.advanceForTest(100);
		copy.settle(level);
		helper.assertTrue(copy.weightKg() > w, Component.literal("and she keeps growing after the load"));
		helper.succeed();
	}

	// ---- straws ----

	private static void strawInseminates(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = founderAt(level, p, Sex.MALE, 81L, 1500, 800);
		CattleEntity cow = founderAt(level, p, Sex.FEMALE, 82L, 500, 400);
		net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		net.minecraft.world.item.ItemStack empty = new net.minecraft.world.item.ItemStack(dev.farmings.FarmingsItems.EMPTY_STRAW, 2);
		net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;
		helper.assertTrue(empty.getItem().interactLivingEntity(empty, player, cow, hand) == net.minecraft.world.InteractionResult.FAIL,
				Component.literal("a cow cannot fill a straw"));
		helper.assertTrue(empty.getCount() == 2, Component.literal("and the empty straw is kept"));
		helper.assertTrue(empty.getItem().interactLivingEntity(empty, player, bull, hand).consumesAction(), Component.literal("a bull fills one"));
		helper.assertTrue(empty.getCount() == 1, Component.literal("it used one empty straw"));
		net.minecraft.world.item.ItemStack full = null;
		for (net.minecraft.world.item.ItemStack s : player.getInventory().getNonEquipmentItems()) {
			if (s.is(dev.farmings.FarmingsItems.SEMEN_STRAW)) {
				full = s;
			}
		}
		helper.assertTrue(full != null && dev.farmings.item.StrawItem.genomeOf(full) != null, Component.literal("the full straw holds a genome"));
		helper.assertTrue(full.getItem().interactLivingEntity(full, player, cow, hand) == net.minecraft.world.InteractionResult.FAIL && full.getCount() == 1,
				Component.literal("a cow not in heat is not inseminated and keeps the straw out of use"));
		cow.startHeatForDisplay();
		helper.assertTrue(full.getItem().interactLivingEntity(full, player, cow, hand).consumesAction(), Component.literal("a cow in heat is inseminated"));
		helper.assertTrue(cow.isInCalf() && cow.hasSire(), Component.literal("she is in calf by the straw's sire"));
		helper.assertTrue(full.isEmpty(), Component.literal("the straw is used up"));
		helper.succeed();
	}

	// ---- the milking parlor ----

	private static void parlorPart(ServerLevel level, dev.farmings.parlor.ParlorParts kind, BlockPos main) {
		BlockState state = FarmingsBlocks.parlorPart(kind).defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
	}

	/**
	 * A whole parlor, row east-west, tank at the east end, stalls to the south: controller at {@code c}, n stalls. Ground is laid under it.
	 * Behind the stalls a 2-row holding pen, and from its west end a 4-cell cow lane west to a stub of barn alley (two floor cells) with a
	 * barn controller beside it: enough for the parlor to find "its barn" ({@link #barnStub}). The full barn-to-parlor walk is in
	 * cows_walk_lane_and_return.
	 */
	private static void buildParlor(ServerLevel level, BlockPos c, int stalls) {
		buildParlorOnly(level, c, stalls);
		int fl = c.getY() - 1;
		int xp = c.getX() - stalls - 1;
		for (int x = xp + 1; x < c.getX(); x++) {
			parlorPart(level, dev.farmings.parlor.ParlorParts.HOLDING_PEN, new BlockPos(x, fl, c.getZ() + 3));
			parlorPart(level, dev.farmings.parlor.ParlorParts.HOLDING_PEN, new BlockPos(x, fl, c.getZ() + 4));
		}
		for (int x = xp - 3; x <= xp; x++) {
			level.setBlock(new BlockPos(x, fl - 1, c.getZ() + 3), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			parlorPart(level, dev.farmings.parlor.ParlorParts.COW_LANE, new BlockPos(x, fl, c.getZ() + 3));
		}
		for (int x = xp - 5; x <= xp - 4; x++) {
			level.setBlock(new BlockPos(x, fl - 1, c.getZ() + 3), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			barnPart(level, BarnParts.ALLEY_FLOOR, new BlockPos(x, fl, c.getZ() + 3), Direction.NORTH);
		}
		level.setBlock(barnStub(c, stalls), FarmingsBlocks.BARN_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
	}

	/** The barn controller of {@link #buildParlor}'s stub barn. */
	private static BlockPos barnStub(BlockPos c, int stalls) {
		return new BlockPos(c.getX() - stalls - 7, c.getY(), c.getZ() + 3);
	}

	/** The parlor's own parts, with no pen, lane or barn. */
	private static void buildParlorOnly(ServerLevel level, BlockPos c, int stalls) {
		int fl = c.getY() - 1;
		for (int x = c.getX() - stalls - 4; x <= c.getX() + 4; x++) {
			for (int z = c.getZ() - 3; z <= c.getZ() + 6; z++) {
				level.setBlock(new BlockPos(x, fl - 1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		level.setBlock(c, FarmingsBlocks.PARLOR_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
		parlorPart(level, dev.farmings.parlor.ParlorParts.BULK_TANK, new BlockPos(c.getX() + 1, fl, c.getZ()));
		int xp = c.getX() - stalls - 1;
		parlorPart(level, dev.farmings.parlor.ParlorParts.VACUUM_PUMP, new BlockPos(xp, fl, c.getZ()));
		for (int x = xp + 1; x <= c.getX(); x++) {
			parlorPart(level, dev.farmings.parlor.ParlorParts.MILK_LINE, new BlockPos(x, fl, c.getZ()));
		}
		for (int x = xp + 1; x < c.getX(); x++) {
			parlorPart(level, dev.farmings.parlor.ParlorParts.MILKING_UNIT, new BlockPos(x, c.getY(), c.getZ()));
			parlorPart(level, dev.farmings.parlor.ParlorParts.PARLOR_STALL, new BlockPos(x, fl, c.getZ() + 1));
		}
	}

	private static dev.farmings.parlor.ParlorControllerEntity parlorController(GameTestHelper helper, BlockPos relative) {
		helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(relative)) instanceof dev.farmings.parlor.ParlorControllerEntity,
				Component.literal("the parlor controller block entity is missing"));
		dev.farmings.parlor.ParlorControllerEntity e = (dev.farmings.parlor.ParlorControllerEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
		e.refresh(helper.getLevel());
		return e;
	}

	/** A cow 30 days in milk, housed in the stub barn of the parlor at {@code parlor} (so that parlor milks her), her stall where she stands. */
	private static CattleEntity milkingCow(ServerLevel level, BlockPos near, long seed, BlockPos parlor, int stalls) {
		CattleEntity cow = founderAt(level, near, Sex.FEMALE, seed, 900, 650);
		cow.assignStall(barnStub(parlor, stalls), near, null);
		cow.setCalvedForDisplay(30);
		cow.settle(level);
		return cow;
	}

	private static void parlorForms(GameTestHelper helper) {
		buildParlor(helper.getLevel(), helper.absolutePos(C), 3);
		dev.farmings.parlor.ParlorControllerEntity e = parlorController(helper, C);
		helper.assertTrue(e.formed(), Component.literal("a whole parlor should form, got: " + e.readout(helper.getLevel())));
		helper.assertTrue(e.check().stalls() == 3, Component.literal("3 stalls, got " + e.check().stalls()));
		helper.assertTrue(e.readout(helper.getLevel()).startsWith("Parlor: FORMED, 3 stalls, tank 0 L at 4.0 C"), Component.literal("readout: " + e.readout(helper.getLevel())));
		helper.succeed();
	}

	private static void parlorMissingPart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildParlor(level, c, 3);
		BlockPos unit = c.offset(-2, 0, 0);
		helper.assertTrue(level.getBlockState(unit).getBlock() == FarmingsBlocks.parlorPart(dev.farmings.parlor.ParlorParts.MILKING_UNIT), Component.literal("the unit should be where the test thinks"));
		level.destroyBlock(unit, false);
		helper.runAfterDelay(5, () -> {
			dev.farmings.parlor.ParlorControllerEntity e = parlorController(helper, C);
			helper.assertTrue(!e.formed(), Component.literal("a parlor missing a unit should not form"));
			helper.assertTrue(e.check().status().equals("Stall 2 has no milking unit"), Component.literal("status: " + e.check().status()));
			helper.succeed();
		});
	}

	private static void parlorMilksCow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildParlor(level, c, 3);
		dev.farmings.parlor.ParlorControllerEntity ctl = parlorController(helper, C);
		ctl.pump(level).forceRunning(true);
		dev.farmings.parlor.TankEntity tank = ctl.tank(level);
		tank.forceChilling(true);
		CattleEntity cow = milkingCow(level, c.offset(-3, -1, 3), 91L, c, 3);
		double kg = cow.milkKg();
		helper.assertTrue(kg > 5, Component.literal("a cow 30 days in milk has milk, got " + kg));
		ctl.milk(level, 20);
		helper.assertTrue(cow.milkingStand() != null, Component.literal("she is called to a stall"));
		BlockPos stand = cow.milkingStand();
		cow.setPos(stand.getX() + 0.5, stand.getY() + 0.25, stand.getZ() + 0.5);
		ctl.milk(level, 20);
		helper.assertTrue(cow.milkKg() == 0 && cow.milkingStand() == null, Component.literal("she is milked and sent home"));
		helper.assertTrue(Math.abs(tank.tank().litres() - kg / 1.03) < 1e-6, Component.literal("the tank got her milk in litres: " + tank.tank().litres() + " vs " + kg / 1.03));
		helper.assertTrue(tank.tank().tempC() == 35, Component.literal("the milk comes in at 35 C, got " + tank.tank().tempC()));
		ctl.milk(level, 7_200);
		helper.assertTrue(tank.tank().tempC() < 35 && tank.tank().tempC() > 4, Component.literal("then it cools, got " + tank.tank().tempC()));
		helper.succeed();
	}

	private static void parlorNoPowerNoMilk(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildParlor(level, c, 3);
		dev.farmings.parlor.ParlorControllerEntity ctl = parlorController(helper, C);
		CattleEntity cow = milkingCow(level, c.offset(-3, -1, 3), 92L, c, 3);
		ctl.milk(level, 20);
		helper.assertTrue(cow.milkingStand() == null && cow.milkKg() > 0, Component.literal("with no power she is not called and keeps her milk"));
		helper.assertTrue(ctl.fault().equals("NO POWER"), Component.literal("fault: " + ctl.fault()));
		helper.assertTrue(ctl.tank(level).tank().litres() == 0, Component.literal("the tank stays empty"));
		helper.succeed();
	}

	private static void tankCoolsAndWarms(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildParlor(level, c, 3);
		dev.farmings.parlor.ParlorControllerEntity ctl = parlorController(helper, C);
		ctl.pump(level).forceRunning(true);
		dev.farmings.parlor.TankEntity tank = ctl.tank(level);
		tank.tank().add(100, 35);
		helper.assertTrue(tank.tank().warm(), Component.literal("fresh milk makes the tank warm"));
		ctl.milk(level, 20);
		helper.assertTrue(ctl.fault().isEmpty(), Component.literal("a session is under way, so milking goes on: " + ctl.fault()));
		ctl.milk(level, 6_000);
		helper.assertTrue(ctl.fault().equals("TANK WARM"), Component.literal("fault: " + ctl.fault()));
		tank.forceChilling(true);
		ctl.milk(level, 14_400);
		helper.assertTrue(Math.abs(tank.tank().tempC() - 4) < 1e-6 && !tank.tank().warm(), Component.literal("the chiller brings it to 4 C, got " + tank.tank().tempC()));
		helper.succeed();
	}

	private static void bucketTakesMilk(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildParlor(level, c, 3);
		dev.farmings.parlor.TankEntity tank = parlorController(helper, C).tank(level);
		tank.tank().add(25, 4);
		net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.BUCKET));
		BlockPos tankCell = c.offset(1, -1, 0);
		FarmingsBlocks.parlorPart(dev.farmings.parlor.ParlorParts.BULK_TANK).defaultBlockState();
		level.getBlockState(tankCell).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(tankCell), Direction.UP, tankCell, false));
		helper.assertTrue(player.getMainHandItem().is(net.minecraft.world.item.Items.MILK_BUCKET), Component.literal("a bucket becomes a milk bucket, got " + player.getMainHandItem()));
		helper.assertTrue(Math.abs(tank.tank().litres() - 15) < 1e-9, Component.literal("it took 10 L, left " + tank.tank().litres()));
		helper.succeed();
	}

	private static void parlorStateSurvivesSave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildParlor(level, c, 3);
		dev.farmings.parlor.TankEntity tank = parlorController(helper, C).tank(level);
		tank.tank().add(120, 20);
		net.minecraft.nbt.CompoundTag tag = tank.saveWithFullMetadata(level.registryAccess());
		net.minecraft.world.level.block.entity.BlockEntity copy = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(tank.getBlockPos(), tank.getBlockState(), tag, level.registryAccess());
		helper.assertTrue(copy instanceof dev.farmings.parlor.TankEntity t && Math.abs(t.tank().litres() - 120) < 1e-9 && Math.abs(t.tank().tempC() - 20) < 1e-9,
				Component.literal("the tank's milk and temperature survive a save"));
		helper.succeed();
	}

	private static void parlorCowWalks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildParlor(level, c, 3);
		dev.farmings.parlor.ParlorControllerEntity ctl = parlorController(helper, C);
		ctl.pump(level).forceRunning(true);
		ctl.tank(level).forceChilling(true);
		CattleEntity cow = milkingCow(level, c.offset(-1, -1, 5), 93L, c, 3);
		cow.setNoAi(false);
		helper.succeedWhen(() -> {
			ctl.milk(level, 20);
			helper.assertTrue(ctl.tank(level).tank().litres() > 5, Component.literal("she walks in and is milked; tank " + ctl.tank(level).tank().litres() + " L, cow at " + cow.position()
					+ ", stand " + cow.milkingStand() + ", fault " + ctl.fault()));
		});
	}

	/** Found by the client screenshots: the first cow's warm milk must not stop the rest of the herd being milked in the same session. */
	private static void parlorMilksAHerd(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildParlor(level, c, 4);
		dev.farmings.parlor.ParlorControllerEntity ctl = parlorController(helper, C);
		ctl.pump(level).forceRunning(true);
		ctl.tank(level).forceChilling(true);
		CattleEntity[] cows = new CattleEntity[4];
		for (int i = 0; i < cows.length; i++) {
			cows[i] = milkingCow(level, c.offset(-4 + i, -1, 3), 94L + i, c, 4);     // waiting in the pen
		}
		for (int round = 0; round < 3; round++) {
			ctl.milk(level, 20);
			for (CattleEntity cow : cows) {
				BlockPos stand = cow.milkingStand();
				if (stand != null) {
					cow.setPos(stand.getX() + 0.5, stand.getY() + 0.25, stand.getZ() + 0.5);
				}
			}
			ctl.milk(level, 20);
		}
		for (CattleEntity cow : cows) {
			helper.assertTrue(cow.milkKg() == 0, Component.literal("every cow is milked in one session, fault " + ctl.fault() + ", tank " + ctl.tank(level).tank().litres() + " L"));
		}
		helper.succeed();
	}

	// ---- the cow lane and holding pen (2026-11-09) ----

	/**
	 * A real 2-stall barn (controller at C) and, north of it, a 2-stall parlor (controller at P) with a 2 x 2 holding pen behind its stalls.
	 * A 7-cell cow lane runs from the pen south to the barn's north alley edge, in the pit-end open stretch. Returns P (absolute).
	 */
	private static BlockPos buildDairy(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		// solid ground under it all, as in cow_walks_to_stall: the pathfinder needs it below the floor
		for (int x = c.getX() - 12; x <= c.getX() + 4; x++) {
			for (int z = c.getZ() - 17; z <= c.getZ() + 6; z++) {
				boolean pit = x >= c.getX() && x <= c.getX() + 1 && z >= c.getZ() - 1 && z <= c.getZ() + 1;
				if (!pit) {
					level.setBlock(new BlockPos(x, c.getY() - 2, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}
		buildBarn(level, c, 2);
		BlockPos p = c.offset(0, 0, -13);
		buildParlorOnly(level, p, 2);
		int fl = c.getY() - 1;
		for (int x = p.getX() - 2; x <= p.getX() - 1; x++) {
			parlorPart(level, dev.farmings.parlor.ParlorParts.HOLDING_PEN, new BlockPos(x, fl, p.getZ() + 3));
			parlorPart(level, dev.farmings.parlor.ParlorParts.HOLDING_PEN, new BlockPos(x, fl, p.getZ() + 4));
		}
		for (int z = p.getZ() + 5; z <= c.getZ() - 2; z++) {
			parlorPart(level, dev.farmings.parlor.ParlorParts.COW_LANE, new BlockPos(p.getX() - 2, fl, z));
		}
		return p;
	}

	private static void laneLinksBarnAndParlor(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos p = buildDairy(helper);
		dev.farmings.parlor.ParlorControllerEntity ctl = (dev.farmings.parlor.ParlorControllerEntity) level.getBlockEntity(p);
		ctl.refresh(level);
		helper.assertTrue(ctl.formed(), Component.literal("the parlor forms: " + ctl.readout(level)));
		helper.assertTrue(controller(helper, C).formed(), Component.literal("the barn forms"));
		helper.assertTrue(ctl.linked(), Component.literal("the lane links them: " + ctl.link().problem()));
		helper.assertTrue(ctl.barn().equals(helper.absolutePos(C)), Component.literal("its barn is the barn at C, got " + ctl.barn()));
		helper.assertTrue(ctl.link().route().size() == 9, Component.literal("alley exit, 7 lane cells, pen entry: " + ctl.link().route()));
		helper.assertTrue(ctl.link().penCapacity() == 2, Component.literal("4 m2 of pen holds 2 cows at 15 sq ft"));
		helper.assertTrue(ctl.readout(level).contains("linked to the barn"), Component.literal("readout: " + ctl.readout(level)));
		helper.succeed();
	}

	private static void noLaneNoMilking(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildParlor(level, c, 3);
		// take the lane's first cell away: the pen is cut off from the barn
		level.destroyBlock(new BlockPos(c.getX() - 4, c.getY() - 1, c.getZ() + 3), false);
		dev.farmings.parlor.ParlorControllerEntity ctl = parlorController(helper, C);
		ctl.pump(level).forceRunning(true);
		ctl.tank(level).forceChilling(true);
		CattleEntity cow = milkingCow(level, c.offset(-3, -1, 3), 95L, c, 3);
		ctl.milk(level, 20);
		helper.assertTrue(ctl.fault().equals("NO LANE TO BARN: No cow lane leaves the holding pen"), Component.literal("fault: " + ctl.fault()));
		helper.assertTrue(cow.milkingStand() == null && cow.penSpot() == null && cow.milkKg() > 0, Component.literal("she is not called and keeps her milk"));
		helper.succeed();
	}

	/** Four cows, two stalls: two are milked while two wait in the pen, then those two. */
	private static void cowsWaitInPen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildParlor(level, c, 2);
		for (int x = c.getX() - 2; x < c.getX(); x++) {      // a third pen row: 6 cells hold 4 cows
			level.setBlock(new BlockPos(x, c.getY() - 2, c.getZ() + 5), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			parlorPart(level, dev.farmings.parlor.ParlorParts.HOLDING_PEN, new BlockPos(x, c.getY() - 1, c.getZ() + 5));
		}
		dev.farmings.parlor.ParlorControllerEntity ctl = parlorController(helper, C);
		ctl.pump(level).forceRunning(true);
		ctl.tank(level).forceChilling(true);
		helper.assertTrue(ctl.link().penCapacity() == 4, Component.literal("pen holds " + ctl.link().penCapacity()));
		CattleEntity[] cows = new CattleEntity[4];
		for (int i = 0; i < 4; i++) {
			cows[i] = milkingCow(level, c.offset(-2 + i % 2, -1, 3 + i / 2), 96L + i, c, 2);
		}
		ctl.milk(level, 20);
		int atStall = 0;
		int inPen = 0;
		for (CattleEntity cow : cows) {
			atStall += cow.milkingStand() != null ? 1 : 0;
			inPen += cow.milkingStand() == null && cow.penSpot() != null ? 1 : 0;
		}
		helper.assertTrue(atStall == 2 && inPen == 2, Component.literal("2 at the stalls, 2 waiting in the pen; got " + atStall + " and " + inPen));
		for (int round = 0; round < 3; round++) {
			for (CattleEntity cow : cows) {
				BlockPos stand = cow.milkingStand();
				if (stand != null) {
					cow.setPos(stand.getX() + 0.5, stand.getY() + 0.25, stand.getZ() + 0.5);
				}
			}
			ctl.milk(level, 20);
		}
		for (CattleEntity cow : cows) {
			helper.assertTrue(cow.milkKg() == 0, Component.literal("each cow is milked in her turn"));
			helper.assertTrue(cow.walkingHome(), Component.literal("and sent home along the lane"));
		}
		helper.succeed();
	}

	/** The whole walk with the cow's own AI: stall, alley, lane, pen, parlor stall, milked, then back along the lane to her stall. */
	private static void cowWalksLaneAndReturn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos p = buildDairy(helper);
		BlockPos c = helper.absolutePos(C);
		BarnControllerEntity barn = controller(helper, C);
		dev.farmings.parlor.ParlorControllerEntity ctl = (dev.farmings.parlor.ParlorControllerEntity) level.getBlockEntity(p);
		ctl.refresh(level);
		ctl.pump(level).forceRunning(true);
		ctl.tank(level).forceChilling(true);
		CattleEntity cow = founderAt(level, c.offset(-8, -1, 0), Sex.FEMALE, 99L, 900, 650);
		cow.setCalvedForDisplay(30);
		cow.settle(level);
		barn.tend(level, 20);
		helper.assertTrue(cow.stallController() != null, Component.literal("she takes a stall in the barn"));
		BlockPos home = cow.stallStand();
		cow.setNoAi(false);
		boolean[] milked = {false};
		helper.succeedWhen(() -> {
			if (cow.milkKg() == 0 && ctl.tank(level).tank().litres() > 5) {
				milked[0] = true;
			}
			helper.assertTrue(milked[0], Component.literal("she walks the lane and is milked; at " + cow.blockPosition() + ", stand " + cow.milkingStand()
					+ ", pen " + cow.penSpot() + ", on route " + cow.onRoute() + " to " + cow.routeTarget() + " (barn " + c + ", parlor " + p + "), nav done " + cow.getNavigation().isDone() + ", fault " + ctl.fault()));
			helper.assertTrue(!cow.onRoute() && cow.distanceToSqr(home.getX() + 0.5, home.getY() + 0.25, home.getZ() + 0.5) < 1.0,
					Component.literal("then she walks back to her stall; at " + cow.blockPosition() + ", home " + home));
		});
	}

	// ---- coat genes (2026-11-09) ----

	private static dev.farmings.genetics.VisualGenome coat(CattleEntity c, dev.farmings.genetics.VisualLocus l, int a, int b) {
		java.util.EnumMap<dev.farmings.genetics.VisualLocus, int[]> m = new java.util.EnumMap<>(c.visualGenome().alleles());
		m.put(l, new int[] {a, b});
		return new dev.farmings.genetics.VisualGenome(m);
	}

	/**
	 * A black polled bull (ED/ED, polled/polled) on a red horned cow (e/e, horned/horned): every calf is black and polled, carries red and
	 * horns, and the calf's coat genes survive a save. Her red calf by a red bull is red.
	 */
	private static void coatInherited(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = founderAt(level, p, Sex.MALE, 201L, 1500, 800);
		CattleEntity dam = founderAt(level, p, Sex.FEMALE, 202L, 500, 400);
		bull.setVisualGenome(coat(bull, dev.farmings.genetics.VisualLocus.EXTENSION, 0, 0));
		bull.setVisualGenome(coat(bull, dev.farmings.genetics.VisualLocus.HORNS, 0, 0));
		dam.setVisualGenome(coat(dam, dev.farmings.genetics.VisualLocus.EXTENSION, 3, 3));
		dam.setVisualGenome(coat(dam, dev.farmings.genetics.VisualLocus.HORNS, 1, 1));
		CattleEntity calf = calveOnce(level, dam, bull);
		helper.assertTrue(calf != null, Component.literal("a calf is born"));
		dev.farmings.genetics.VisualPhenotype look = calf.look();
		helper.assertTrue(look.coat() == dev.farmings.genetics.VisualPhenotype.Coat.BLACK && !look.horned(), Component.literal("black and polled: " + look.describe()));
		helper.assertTrue(java.util.Arrays.equals(calf.visualGenome().pair(dev.farmings.genetics.VisualLocus.EXTENSION), new int[] {3, 0}),
				Component.literal("she carries her dam's red and her sire's black: " + calf.visualGenome()));
		helper.assertTrue(calf.syncedLook() != null && calf.syncedLook().equals(look), Component.literal("the client is sent her looks"));
		helper.assertTrue(calf.readout().contains("black") && calf.readout().contains("polled"), Component.literal("readout: " + calf.readout()));
		// saved and loaded, the same coat genes
		net.minecraft.util.ProblemReporter reporter = net.minecraft.util.ProblemReporter.DISCARDING;
		net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, level.registryAccess());
		calf.saveWithoutId(out);
		CattleEntity copy = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.LOAD);
		copy.load(net.minecraft.world.level.storage.TagValueInput.create(reporter, level.registryAccess(), out.buildResult()));
		helper.assertTrue(copy.visualGenome().equals(calf.visualGenome()), Component.literal("coat genes survive a save"));
		// red by red is red
		CattleEntity redBull = founderAt(level, p, Sex.MALE, 203L, 1500, 800);
		redBull.setVisualGenome(coat(redBull, dev.farmings.genetics.VisualLocus.EXTENSION, 3, 3));
		CattleEntity dam2 = founderAt(level, p, Sex.FEMALE, 204L, 500, 400);
		dam2.setVisualGenome(coat(dam2, dev.farmings.genetics.VisualLocus.EXTENSION, 3, 3));
		CattleEntity red = calveOnce(level, dam2, redBull);
		helper.assertTrue(red.look().coat() == dev.farmings.genetics.VisualPhenotype.Coat.RED, Component.literal("red x red is red: " + red.look().describe()));
		helper.succeed();
	}

	// ---- herd book, piped trough, milk armor (2026-11-09) ----

	private static void herdBookRecords(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos p = helper.absolutePos(new BlockPos(3, 1, 3));
		CattleEntity bull = founderAt(level, p, Sex.MALE, 81L, 1500, 800);
		CattleEntity dam = founderAt(level, p, Sex.FEMALE, 82L, 500, 400);
		CattleEntity calf = calveOnce(level, dam, bull);
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack book = new ItemStack(FarmingsItems.HERD_BOOK);
		player.setItemInHand(InteractionHand.MAIN_HAND, book);
		FarmingsItems.HERD_BOOK.interactLivingEntity(book, player, calf, InteractionHand.MAIN_HAND);
		FarmingsItems.HERD_BOOK.interactLivingEntity(book, player, dam, InteractionHand.MAIN_HAND);
		FarmingsItems.HERD_BOOK.interactLivingEntity(book, player, calf, InteractionHand.MAIN_HAND);   // again: replaced, not added
		ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
		java.util.List<String[]> entries = dev.farmings.item.HerdBookItem.entries(held);
		helper.assertTrue(entries.size() == 2, Component.literal("two animals, got " + entries.size()));
		String page = entries.get(0)[1];
		helper.assertTrue(page.contains("Dam " + dam.herdId()) && page.contains("Sire " + bull.herdId()) && page.contains("milk yield"),
				Component.literal("the calf's page names her parents and traits: " + page));
		net.minecraft.world.item.component.WrittenBookContent content = held.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
		helper.assertTrue(content != null && content.pages().size() == 2, Component.literal("it reads as a two-page book"));
		helper.assertTrue(entries.get(0)[1].length() <= net.minecraft.world.item.component.WrittenBookContent.PAGE_LENGTH, Component.literal("a page fits"));
		helper.succeed();
	}

	private static void troughFilledByPipe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		buildBarn(level, c, 2);
		TroughEntity trough = anyTrough(level, c);
		helper.assertTrue(trough != null && trough.litres() == 0, Component.literal("an empty trough"));
		// A pipe cell touching any cell of the trough finds it, the way a Wildcatter pump looks for its machines.
		BlockPos pipe = trough.getBlockPos().above();
		java.util.List<net.minecraft.world.level.block.entity.BlockEntity> found = dev.wildcatter.block.Plumbing.machines(level, java.util.Set.of(),
				java.util.Set.of(pipe), false, net.minecraft.world.level.block.entity.BlockEntity.class);
		dev.wildcatter.block.WaterUser user = found.stream().filter(e -> e instanceof dev.wildcatter.block.WaterUser).map(e -> (dev.wildcatter.block.WaterUser) e).findFirst().orElse(null);
		helper.assertTrue(user == trough, Component.literal("the pump sees the trough as a water user, found " + found));
		helper.assertTrue(user.waterLevel() == 0 && user.addWater(600) == 600 && user.addWater(600) == 400, Component.literal("it takes what fits"));
		helper.assertTrue(trough.litres() == TroughEntity.CAPACITY_LITRES && user.waterLevel() == 1, Component.literal("full at 1,000 L"));
		helper.succeed();
	}

	private static void milkArmorSpoils(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack helmet = new ItemStack(FarmingsItems.MILK_HELMET);
		ItemStack netherite = new ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET);
		helper.assertTrue(helmet.getMaxDamage() == netherite.getMaxDamage(), Component.literal("netherite durability"));
		helper.assertTrue(java.util.Objects.equals(helmet.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).modifiers().size(),
				netherite.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).modifiers().size()), Component.literal("netherite stats"));
		player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet);
		ItemStack worn = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
		dev.farmings.item.MilkArmorItem.setMade(worn, level.getGameTime());
		worn.inventoryTick(level, player, net.minecraft.world.entity.EquipmentSlot.HEAD);
		helper.assertTrue(!dev.farmings.item.MilkArmorItem.spoiled(worn) && !player.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA), Component.literal("fresh: no sickness"));
		dev.farmings.item.MilkArmorItem.setMade(worn, level.getGameTime() - dev.farmings.item.MilkArmorItem.SPOIL_TICKS);
		worn.setDamageValue(17);
		worn.inventoryTick(level, player, net.minecraft.world.entity.EquipmentSlot.HEAD);
		helper.assertTrue(dev.farmings.item.MilkArmorItem.spoiled(worn), Component.literal("a game day later it is spoiled"));
		boolean viaTick = player.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA);
		dev.farmings.item.MilkArmorItem.sicken(player);
		helper.assertTrue(viaTick, Component.literal("worn spoiled: Hunger and Nausea (direct call gives " + player.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA)
				+ ", same stack " + (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD) == worn) + ", creative " + player.isCreative() + ")"));
		helper.assertTrue(worn.getDamageValue() == 17, Component.literal("durability kept"));
		helper.assertTrue(worn.getHoverName().getString().startsWith("Spoiled") || worn.getHoverName().getString().contains("spoiled"), Component.literal("spoiled name: " + worn.getHoverName().getString()));
		helper.succeed();
	}

	// ---- the creamery (2026-11-09) ----

	private static final BlockPos K = new BlockPos(2, 2, 4);   // the creamery controller, relative to the test

	private static void creameryPart(ServerLevel level, dev.farmings.creamery.CreameryParts kind, BlockPos main) {
		BlockState state = FarmingsBlocks.creameryPart(kind).defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
	}

	/** The row runs east from the controller at {@code c}: tank, pasteurizer, separator, churn, vat, press, shelf, bottler. */
	private static void buildCreamery(ServerLevel level, BlockPos c) {
		for (int x = c.getX() - 1; x <= c.getX() + 12; x++) {
			for (int z = c.getZ() - 1; z <= c.getZ() + 2; z++) {
				level.setBlock(new BlockPos(x, c.getY() - 1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		level.setBlock(c, FarmingsBlocks.CREAMERY_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
		int x = c.getX() + 1;
		for (dev.farmings.creamery.CreameryParts kind : dev.farmings.creamery.CreameryParts.values()) {
			creameryPart(level, kind, new BlockPos(x, c.getY(), c.getZ()));
			x += kind.part.length();
		}
	}

	private static dev.farmings.creamery.CreameryControllerEntity creamery(GameTestHelper helper, BlockPos relative, boolean power) {
		ServerLevel level = helper.getLevel();
		dev.farmings.creamery.CreameryControllerEntity ctl = (dev.farmings.creamery.CreameryControllerEntity) level.getBlockEntity(helper.absolutePos(relative));
		ctl.refresh(level);
		if (power && ctl.formed()) {
			for (dev.farmings.process.creamery.CreameryPart p : new dev.farmings.process.creamery.CreameryPart[] {dev.farmings.process.creamery.CreameryPart.BATCH_PASTEURIZER,
					dev.farmings.process.creamery.CreameryPart.CREAM_SEPARATOR, dev.farmings.process.creamery.CreameryPart.BUTTER_CHURN, dev.farmings.process.creamery.CreameryPart.CHEESE_VAT}) {
				ctl.machine(level, p).forceRunning(true);
			}
		}
		return ctl;
	}

	private static void runFor(ServerLevel level, dev.farmings.creamery.CreameryControllerEntity ctl, long ticks) {
		for (long t = 0; t < ticks; t += 20) {
			ctl.run(level, 20);
		}
	}

	private static BlockPos partPos(BlockPos c, dev.farmings.creamery.CreameryParts kind) {
		int x = c.getX() + 1;
		for (dev.farmings.creamery.CreameryParts k : dev.farmings.creamery.CreameryParts.values()) {
			if (k == kind) {
				return new BlockPos(x, c.getY(), c.getZ());
			}
			x += k.part.length();
		}
		return null;
	}

	private static InteractionResult useOn(ServerLevel level, net.minecraft.server.level.ServerPlayer player, BlockPos pos, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		BlockState state = level.getBlockState(pos);
		InteractionResult r = stack.isEmpty() ? InteractionResult.PASS : state.useItemOn(stack, level, player, InteractionHand.MAIN_HAND, hit);
		if (r == InteractionResult.TRY_WITH_EMPTY_HAND || stack.isEmpty()) {
			r = state.useWithoutItem(level, player, hit);
		}
		return r;
	}

	private static void creameryForms(GameTestHelper helper) {
		buildCreamery(helper.getLevel(), helper.absolutePos(K));
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, false);
		helper.assertTrue(ctl.formed(), Component.literal("a full row forms: " + ctl.readout()));
		helper.assertTrue(ctl.owns(partPos(helper.absolutePos(K), dev.farmings.creamery.CreameryParts.BOTTLER)), Component.literal("it owns its bottler"));
		helper.succeed();
	}

	private static void creameryMissingPart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(K);
		buildCreamery(level, c);
		level.setBlock(partPos(c, dev.farmings.creamery.CreameryParts.CREAM_SEPARATOR), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, false);
		helper.assertTrue(!ctl.formed() && ctl.readout().contains("cream separator"), Component.literal("it names the missing separator: " + ctl.readout()));
		helper.succeed();
	}

	private static void pasteurizerBottles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(K);
		buildCreamery(level, c);
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, true);
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		useOn(level, player, partPos(c, dev.farmings.creamery.CreameryParts.RECEIVING_TANK), new ItemStack(net.minecraft.world.item.Items.MILK_BUCKET));
		helper.assertTrue(ctl.creamery().rawL() == 10,   // the mock player is in creative, so the bucket stays full in hand
				Component.literal("a milk bucket pours 10 L, got " + ctl.creamery().rawL()));
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PHASE_TICKS * 3 + 60);
		helper.assertTrue(ctl.creamery().bottled(dev.farmings.process.creamery.Creamery.Bottle.MILK) == 10, Component.literal("10 L pasteurized: " + ctl.readout()));
		useOn(level, player, partPos(c, dev.farmings.creamery.CreameryParts.BOTTLER), new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
		helper.assertTrue(player.getInventory().countItem(FarmingsItems.BOTTLED_MILK) == 1 && ctl.creamery().bottled(dev.farmings.process.creamery.Creamery.Bottle.MILK) == 9,
				Component.literal("a glass bottle fills with milk"));
		helper.succeed();
	}

	private static void separatorSplitsFat(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		buildCreamery(level, helper.absolutePos(K));
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, true);
		ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.CREAM);
		ctl.creamery().addMilk(100, 4.76, 3.63);
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PHASE_TICKS * 3 + 60);
		double cream = ctl.creamery().bottled(dev.farmings.process.creamery.Creamery.Bottle.CREAM);
		double skim = ctl.creamery().bottled(dev.farmings.process.creamery.Creamery.Bottle.SKIM);
		helper.assertTrue(Math.abs(cream + skim - 100) < 1e-6 && Math.abs(cream * 36 + skim * 0.1 - 476) < 1e-6, Component.literal("fat balance holds: " + cream + " cream, " + skim + " skim"));
		helper.succeed();
	}

	private static void churnMakesButter(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(K);
		buildCreamery(level, c);
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, true);
		ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.BUTTER);
		ctl.creamery().addMilk(200, 3.72, 3.06);
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PHASE_TICKS * 3 + 60);
		double kg = ctl.creamery().butterKg();
		helper.assertTrue(kg > 8.5 && kg < 9.5, Component.literal("about 9.1 kg of butter from 200 L Holstein milk, got " + kg));
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		useOn(level, player, partPos(c, dev.farmings.creamery.CreameryParts.BUTTER_CHURN), ItemStack.EMPTY);
		int got = player.getInventory().countItem(FarmingsItems.BUTTER);
		helper.assertTrue(got == (int) Math.floor(kg / 0.5), Component.literal("half-kilo blocks from the churn: " + got));
		helper.assertTrue(ctl.creamery().bottled(dev.farmings.process.creamery.Creamery.Bottle.BUTTERMILK) > 0, Component.literal("and buttermilk"));
		helper.succeed();
	}

	// ---- whey and hoppers (2026-11-09) ----

	private static int countIn(net.minecraft.world.Container box, net.minecraft.world.item.Item item) {
		int n = 0;
		for (int i = 0; i < box.getContainerSize(); i++) {
			if (box.getItem(i).is(item)) {
				n += box.getItem(i).getCount();
			}
		}
		return n;
	}

	private static net.minecraft.world.level.block.entity.HopperBlockEntity hopperAt(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
		return (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(pos);
	}

	/** A hopper under the churn takes the butter, and one under the aging shelf takes the aged wheels, with nobody there. */
	private static void hopperTakesButterAndWheels(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(K);
		buildCreamery(level, c);
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, true);
		ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.BUTTER);
		ctl.creamery().addMilk(200, 3.72, 3.06);
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PHASE_TICKS * 3 + 60);
		int blocks = (int) Math.floor(ctl.creamery().butterKg() / 0.5);
		ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.CHEESE);
		ctl.creamery().addMilk(200, 3.72, 3.06);
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PHASE_TICKS * 3 + 60);
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PRESS_TICKS + dev.farmings.process.creamery.Creamery.AGE_TICKS + 60);
		int wheels = ctl.creamery().agedWheels();
		helper.assertTrue(blocks >= 17 && wheels == 2, Component.literal("butter " + blocks + ", wheels " + wheels));
		var churnHopper = hopperAt(level, partPos(c, dev.farmings.creamery.CreameryParts.BUTTER_CHURN).below());
		var shelfHopper = hopperAt(level, partPos(c, dev.farmings.creamery.CreameryParts.AGING_SHELF).below());
		helper.succeedWhen(() -> {
			helper.assertTrue(countIn(churnHopper, FarmingsItems.BUTTER) == blocks, Component.literal("the churn's hopper holds " + countIn(churnHopper, FarmingsItems.BUTTER) + " of " + blocks));
			helper.assertTrue(countIn(shelfHopper, FarmingsItems.CHEDDAR_WHEEL) == 2, Component.literal("the shelf's hopper holds " + countIn(shelfHopper, FarmingsItems.CHEDDAR_WHEEL) + " wheels"));
			helper.assertTrue(ctl.creamery().butterKg() < 0.5 && ctl.creamery().agedWheels() == 0, Component.literal("and the creamery is empty of them"));
		});
	}

	/** Glass bottles fed from a hopper on top of the bottler come out filled into a hopper below, one bottle a litre. */
	private static void hopperBottlesWithGlass(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(K);
		buildCreamery(level, c);
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, true);
		ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.BOTTLE);
		ctl.creamery().addMilk(10, 3.72, 3.06);
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PHASE_TICKS * 3 + 60);
		helper.assertTrue(ctl.creamery().bottled(dev.farmings.process.creamery.Creamery.Bottle.MILK) >= 10 - 1e-9, Component.literal("10 L ready to bottle"));
		BlockPos bottler = partPos(c, dev.farmings.creamery.CreameryParts.BOTTLER);
		var below = hopperAt(level, bottler.below());
		var above = hopperAt(level, bottler.above());
		above.setItem(0, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 6));
		helper.succeedWhen(() -> {
			helper.assertTrue(countIn(below, FarmingsItems.BOTTLED_MILK) == 6, Component.literal("6 glass bottles make 6 bottles of milk, got " + countIn(below, FarmingsItems.BOTTLED_MILK)
					+ "; glass left above " + countIn(above, net.minecraft.world.item.Items.GLASS_BOTTLE)));
			helper.assertTrue(Math.abs(ctl.creamery().bottled(dev.farmings.process.creamery.Creamery.Bottle.MILK) - 4) < 1e-6, Component.literal("4 L left in the store"));
		});
	}

	/** Cheddar leaves whey (milk less cheese by mass): about 180 L from 200 L, bottled like the other products. */
	private static void wheyFromCheese(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(K);
		buildCreamery(level, c);
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, true);
		ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.CHEESE);
		ctl.creamery().addMilk(200, 3.72, 3.06);
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PHASE_TICKS * 3 + 60);
		double whey = ctl.creamery().bottled(dev.farmings.process.creamery.Creamery.Bottle.WHEY);
		helper.assertTrue(whey > 175 && whey < 185, Component.literal("about 180 L of whey, got " + whey));
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		useOn(level, player, partPos(c, dev.farmings.creamery.CreameryParts.BOTTLER), new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
		helper.assertTrue(player.getInventory().countItem(FarmingsItems.WHEY_BOTTLE) == 1, Component.literal("a glass bottle fills with whey"));
		helper.assertTrue(ctl.readout().contains("whey"), Component.literal("readout: " + ctl.readout()));
		helper.succeed();
	}

	// ---- spoilage (2026-11-09) ----

	/** Milk bottled at the creamery starts its clock as it leaves (1 game day at room temperature), and turns when the day is up. */
	private static void milkSpoilsAtRoom(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(K);
		buildCreamery(level, c);
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, true);
		ctl.creamery().addMilk(10, 3.72, 3.06);
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PHASE_TICKS * 3 + 60);
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getAbilities().instabuild = false;
		player.getInventory().clearContent();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 3));
		BlockPos bottler = partPos(c, dev.farmings.creamery.CreameryParts.BOTTLER);
		for (int i = 0; i < 3; i++) {
			useOn(level, player, bottler, player.getMainHandItem());
		}
		int slot = -1;
		for (int i = 0; i < player.getInventory().getContainerSize() && slot < 0; i++) {
			if (player.getInventory().getItem(i).is(FarmingsItems.BOTTLED_MILK)) {
				slot = i;
			}
		}
		helper.assertTrue(slot >= 0, Component.literal("bottled milk in hand"));
		ItemStack milk = player.getInventory().getItem(slot);
		long now = level.getGameTime();
		long at = dev.farmings.item.Perishables.spoilsAt(milk);
		helper.assertTrue(at - now >= 24_000 && at - now <= 24_000 + 1_200, Component.literal("it keeps one game day out of the cold, spoils in " + (at - now)));
		// three bottles filled in the same tick carry the same clock, so they stack (the last fills the emptied hand, the others stack together)
		helper.assertTrue(player.getInventory().countItem(FarmingsItems.BOTTLED_MILK) == 3, Component.literal("three bottles"));
		boolean stacked = false;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack o = player.getInventory().getItem(i);
			if (o.is(FarmingsItems.BOTTLED_MILK) && o != milk) {
				stacked |= ItemStack.isSameItemSameComponents(o, milk);
			}
		}
		helper.assertTrue(stacked, Component.literal("bottles filled together carry the same clock and stack"));
		milk.getItem().inventoryTick(milk, level, player, null);
		helper.assertTrue(player.getInventory().getItem(slot).is(FarmingsItems.BOTTLED_MILK), Component.literal("still good today"));
		dev.farmings.item.Perishables.setSpoilsAt(milk, now - 1);    // a day later
		milk.getItem().inventoryTick(milk, level, player, null);
		ItemStack after = player.getInventory().getItem(slot);
		helper.assertTrue(after.is(FarmingsItems.SPOILED_BOTTLE) && after.getCount() == milk.getCount(), Component.literal("then it turns, the same count: " + after));
		helper.succeed();
	}

	/** Spoiled food is edible but sickens like spoiled milk armor: Hunger and Nausea. */
	private static void spoiledFoodSickens(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		for (net.minecraft.world.item.Item item : new net.minecraft.world.item.Item[] {FarmingsItems.SPOILED_BOTTLE, FarmingsItems.RANCID_BUTTER,
				FarmingsItems.MOLDY_CHEESE, FarmingsItems.SPOILED_STEAK}) {
			player.removeAllEffects();
			ItemStack food = new ItemStack(item);
			food.finishUsingItem(level, player);
			helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER) && player.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA),
					Component.literal(item + " should sicken"));
		}
		helper.succeed();
	}

	// ---- the walk-in fridge (2026-11-09) ----

	private static final BlockPos ROOM = new BlockPos(6, 2, 6);    // the room's inside starts here (relative)

	/**
	 * A 3 x 2 x 3 walk-in fridge (inside from {@code a}): a full panel box, the evaporator in the west wall facing in, a door in the south
	 * wall, the condensing unit outside the west wall, and a chest inside. Returns the evaporator's position.
	 */
	private static BlockPos buildColdRoom(ServerLevel level, BlockPos a) {
		for (int x = -1; x <= 3; x++) {
			for (int y = -1; y <= 2; y++) {
				for (int z = -1; z <= 3; z++) {
					boolean in = x >= 0 && x < 3 && y >= 0 && y < 2 && z >= 0 && z < 3;
					level.setBlock(a.offset(x, y, z), in ? Blocks.AIR.defaultBlockState() : dev.farmings.coldroom.ColdRoomBlocks.PANEL.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}
		BlockPos evap = a.offset(-1, 0, 1);
		level.setBlock(evap, dev.farmings.coldroom.ColdRoomBlocks.EVAPORATOR.defaultBlockState().setValue(dev.farmings.coldroom.EvaporatorBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
		BlockState door = dev.farmings.coldroom.ColdRoomBlocks.DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.NORTH);
		level.setBlock(a.offset(1, 0, 3), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
		level.setBlock(a.offset(1, 1, 3), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
		level.setBlock(a.offset(-2, 0, 1), dev.farmings.coldroom.ColdRoomBlocks.CONDENSING_UNIT.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(a.offset(1, 0, 1), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		return evap;
	}

	private static dev.farmings.coldroom.ColdRoomEntity room(ServerLevel level, BlockPos evap) {
		dev.farmings.coldroom.ColdRoomEntity r = (dev.farmings.coldroom.ColdRoomEntity) level.getBlockEntity(evap);
		r.refresh(level);
		return r;
	}

	private static void coldRoomForms(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos evap = buildColdRoom(level, helper.absolutePos(ROOM));
		var r = room(level, evap);
		helper.assertTrue(r.formed(), Component.literal("a sealed panel box forms: " + r.readout(level)));
		helper.assertTrue(r.check().volumeM3() == 18, Component.literal("3 x 2 x 3 inside"));
		r.condenser(level).forceRunning(true);
		r.cool(level, level.getGameTime(), dev.farmings.process.food.Spoilage.STEP);
		double want = dev.farmings.process.coldroom.ColdRoomLoad.electricWatts(18);
		helper.assertTrue(Math.abs(r.condenser(level).ratedWatts() - want) < 1, Component.literal("the compressor is sized for the room: " + r.condenser(level).ratedWatts() + " W vs " + want));
		helper.assertTrue(r.readout(level).contains("COLD") && r.readout(level).contains("kW cooling"), Component.literal("readout: " + r.readout(level)));
		helper.succeed();
	}

	private static void coldRoomGapNamed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos a = helper.absolutePos(ROOM);
		BlockPos evap = buildColdRoom(level, a);
		level.setBlock(a.offset(1, 2, 1), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);     // a hole in the roof
		var r = room(level, evap);
		helper.assertTrue(!r.formed() && r.check().problem().startsWith("The walls are not sealed"), Component.literal("got: " + r.readout(level)));
		level.setBlock(a.offset(1, 2, 1), dev.farmings.coldroom.ColdRoomBlocks.PANEL.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(a.offset(-2, 0, 1), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertTrue(room(level, evap).check().problem().equals("No condensing unit touching the walls outside"), Component.literal("no unit named"));
		helper.succeed();
	}

	private static ItemStack milkMadeAt(long now) {
		return dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.BOTTLED_MILK, 4), now);
	}

	/** Milk in a chest inside a powered fridge: each minute cold pushes its spoil tick later by 6/7 of a minute; milk outside does not move. */
	private static void fridgeSlowsSpoilage(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos a = helper.absolutePos(ROOM);
		BlockPos evap = buildColdRoom(level, a);
		var r = room(level, evap);
		r.condenser(level).forceRunning(true);
		long now = level.getGameTime();
		var inside = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(a.offset(1, 0, 1));
		inside.setItem(0, milkMadeAt(now));
		level.setBlock(a.offset(6, 0, 1), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		var outside = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(a.offset(6, 0, 1));
		outside.setItem(0, milkMadeAt(now));
		long before = dev.farmings.item.Perishables.spoilsAt(inside.getItem(0));
		for (int i = 0; i < 10; i++) {
			r.cool(level, now, dev.farmings.process.food.Spoilage.STEP);
		}
		long credit = dev.farmings.process.food.Spoilage.coldCredit(dev.farmings.process.food.Spoilage.Food.MILK, dev.farmings.process.food.Spoilage.STEP);
		helper.assertTrue(dev.farmings.item.Perishables.spoilsAt(inside.getItem(0)) == before + 10 * credit,
				Component.literal("ten cold minutes add " + 10 * credit + " ticks, got " + (dev.farmings.item.Perishables.spoilsAt(inside.getItem(0)) - before)));
		helper.assertTrue(dev.farmings.item.Perishables.spoilsAt(outside.getItem(0)) == before, Component.literal("the milk outside is not helped"));
		helper.assertTrue(credit > 1000 && credit < 1050, Component.literal("6/7 of a minute: " + credit));
		helper.succeed();
	}

	private static void coldRoomNoPowerWarms(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos a = helper.absolutePos(ROOM);
		BlockPos evap = buildColdRoom(level, a);
		var r = room(level, evap);
		long now = level.getGameTime();
		var inside = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(a.offset(1, 0, 1));
		inside.setItem(0, milkMadeAt(now));
		long before = dev.farmings.item.Perishables.spoilsAt(inside.getItem(0));
		r.cool(level, now, dev.farmings.process.food.Spoilage.STEP);
		helper.assertTrue(r.fault().equals("NO POWER"), Component.literal("fault: " + r.fault()));
		helper.assertTrue(dev.farmings.item.Perishables.spoilsAt(inside.getItem(0)) == before, Component.literal("no power, no cold: the clock runs as at room temperature"));
		helper.assertTrue(r.readout(level).contains("NO POWER"), Component.literal("readout: " + r.readout(level)));
		helper.succeed();
	}

	private static void cheeseYieldAndAging(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(K);
		buildCreamery(level, c);
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, true);
		ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.CHEESE);
		ctl.creamery().addMilk(200, 3.72, 3.06);
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PHASE_TICKS * 3 + 60);
		double expected = dev.farmings.process.creamery.Dairy.cheeseKg(200, 3.72, 3.06);
		helper.assertTrue(Math.abs(ctl.creamery().curdKg() + (ctl.creamery().pressingWheel() ? 20 : 0) - expected) < 1e-6, Component.literal("Van Slyke curd " + expected + ": " + ctl.readout()));
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		useOn(level, player, partPos(c, dev.farmings.creamery.CreameryParts.AGING_SHELF), ItemStack.EMPTY);
		helper.assertTrue(player.getInventory().countItem(FarmingsItems.CHEDDAR_WHEEL) == 0, Component.literal("no wheel before it has aged"));
		for (int i = 0; i < 6; i++) {
			ctl.run(level, 24_000);
		}
		useOn(level, player, partPos(c, dev.farmings.creamery.CreameryParts.AGING_SHELF), ItemStack.EMPTY);
		helper.assertTrue(player.getInventory().countItem(FarmingsItems.CHEDDAR_WHEEL) == 2, Component.literal("two aged 10 kg wheels after 4 game days, got "
				+ player.getInventory().countItem(FarmingsItems.CHEDDAR_WHEEL)));
		helper.succeed();
	}

	/**
	 * A sheep-only parlor (docs/multiblocks/parlor.md, "Sheep stalls"): the two-cell cow stalls replaced by one-cell sheep stalls, a sheep
	 * pen behind them at z+2..z+3 and a shed controller beside the pen. Returns the shed controller.
	 */
	private static BlockPos buildSheepParlor(ServerLevel level, BlockPos c, int stalls) {
		buildParlorOnly(level, c, stalls);
		int fl = c.getY() - 1;
		int xp = c.getX() - stalls - 1;
		for (int x = xp + 1; x < c.getX(); x++) {
			level.removeBlock(new BlockPos(x, fl, c.getZ() + 1), false);
			level.removeBlock(new BlockPos(x, fl, c.getZ() + 2), false);
		}
		for (int x = xp + 1; x < c.getX(); x++) {
			parlorPart(level, dev.farmings.parlor.ParlorParts.SHEEP_STALL, new BlockPos(x, fl, c.getZ() + 1));
			for (int z = 2; z <= 3; z++) {
				level.setBlock(new BlockPos(x, fl, c.getZ() + z), dev.farmings.shed.ShedBlocks.SHEEP_PEN.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		BlockPos shed = new BlockPos(xp, fl, c.getZ() + 3);
		level.setBlock(shed, dev.farmings.shed.ShedBlocks.SHED_CONTROLLER.defaultBlockState(), Block.UPDATE_ALL);
		return shed;
	}

	private static dev.farmings.entity.SheepEntity dairyEwe(ServerLevel level, BlockPos at, BlockPos shed, long seed) {
		dev.farmings.entity.SheepEntity e = FarmingsEntities.SHEEP.create(level, EntitySpawnReason.COMMAND);
		java.util.Map<String, Integer> comp = java.util.Map.of("EFR", 10_000);
		e.initFounder(dev.farmings.husbandry.SheepBreed.EAST_FRIESIAN, Sex.FEMALE, seed, 800, dev.farmings.husbandry.Flock.targetKg(comp, 800), level.getGameTime());
		e.setPos(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5);
		level.addFreshEntity(e);
		e.assignShed(shed, at);
		e.setLambedForTest(30, 1);
		return e;
	}

	/** A dairy ewe is called from the shed's pen to a sheep stall and milked into the tank as sheep milk. */
	private static void eweMilkedInSheepStall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		BlockPos shed = buildSheepParlor(level, c, 2);
		dev.farmings.parlor.ParlorControllerEntity ctl = parlorController(helper, C);
		ctl.pump(level).forceRunning(true);
		dev.farmings.parlor.TankEntity tank = ctl.tank(level);
		tank.forceChilling(true);
		helper.assertTrue(ctl.check().sheepStall(0) && ctl.check().sheepStall(1), Component.literal("two sheep stalls: " + ctl.check().status()));
		dev.farmings.entity.SheepEntity ewe = dairyEwe(level, c.offset(-2, -1, 3), shed, 501L);
		double kg = ewe.milkKg();
		helper.assertTrue(kg > 0.5, Component.literal("an East Friesian 30 days in milk has milk, got " + kg));
		ctl.milk(level, 20);
		helper.assertTrue(ewe.sentTo() != null && ewe.reservedBy() != null, Component.literal("she is called to a sheep stall: " + ctl.readout(level)));
		BlockPos stand = ewe.sentTo();
		ewe.setPos(stand.getX() + 0.5, stand.getY() + 0.2, stand.getZ() + 1.3);
		ctl.milk(level, 20);
		helper.assertTrue(ewe.milkKg() == 0 && ewe.reservedBy() == null, Component.literal("she is milked and let go"));
		helper.assertTrue(Math.abs(tank.tank().litres() - kg / 1.03) < 1e-6 && tank.tank().sheep(), Component.literal("the tank holds her milk as sheep milk: " + tank.tank().litres()));
		helper.assertTrue(Math.abs(tank.tank().fatPct() - 5.68) < 1e-6, Component.literal("East Friesian fat, got " + tank.tank().fatPct()));
		helper.succeed();
	}

	/** A tank holding cow milk takes no sheep milk: the fault says so and the ewe keeps hers. */
	private static void tankRefusesMixedMilk(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(C);
		BlockPos shed = buildSheepParlor(level, c, 2);
		dev.farmings.parlor.ParlorControllerEntity ctl = parlorController(helper, C);
		ctl.pump(level).forceRunning(true);
		dev.farmings.parlor.TankEntity tank = ctl.tank(level);
		tank.forceChilling(true);
		tank.tank().add(10, 4, 3.72, 3.06, false);
		dev.farmings.entity.SheepEntity ewe = dairyEwe(level, c.offset(-2, -1, 3), shed, 502L);
		ctl.milk(level, 20);
		helper.assertTrue(ctl.fault().equals("TANK HOLDS COW MILK") && ewe.reservedBy() == null && ewe.milkKg() > 0,
				Component.literal("fault " + ctl.fault() + ", ewe called " + (ewe.reservedBy() != null)));
		helper.assertTrue(tank.tank().add(5, 35, 5.68, 4.92, true) == 0, Component.literal("the tank refuses sheep milk while it holds cow milk"));
		tank.tank().take(10);
		helper.assertTrue(tank.tank().add(5, 35, 5.68, 4.92, true) == 5, Component.literal("emptied, it takes sheep milk"));
		helper.succeed();
	}

	/** Sheep milk in the creamery makes sheep cheese wheels; with sheep milk in it the creamery refuses cow milk. */
	private static void sheepCheeseFromCreamery(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(K);
		buildCreamery(level, c);
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, true);
		ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.CHEESE);
		helper.assertTrue(ctl.creamery().addMilk(200, 5.68, 4.92, true) == 200, Component.literal("sheep milk goes in"));
		helper.assertTrue(ctl.creamery().addMilk(10, 3.72, 3.06, false) == 0, Component.literal("cow milk is refused while it holds sheep milk"));
		runFor(level, ctl, dev.farmings.process.creamery.Creamery.PHASE_TICKS * 3 + 60);
		double expected = dev.farmings.process.creamery.Dairy.cheeseKg(200, 5.68, 4.92);
		helper.assertTrue(expected > dev.farmings.process.creamery.Dairy.cheeseKg(200, 3.72, 3.06), Component.literal("richer milk gives more cheese"));
		for (int i = 0; i < 6; i++) {
			ctl.run(level, 24_000);
		}
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		useOn(level, player, partPos(c, dev.farmings.creamery.CreameryParts.AGING_SHELF), ItemStack.EMPTY);
		helper.assertTrue(player.getInventory().countItem(FarmingsItems.SHEEP_CHEESE_WHEEL) >= 2 && player.getInventory().countItem(FarmingsItems.CHEDDAR_WHEEL) == 0,
				Component.literal("sheep cheese wheels, got " + player.getInventory().countItem(FarmingsItems.SHEEP_CHEESE_WHEEL) + "; " + ctl.readout()));
		helper.succeed();
	}

	private static void creameryNoPower(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		buildCreamery(level, helper.absolutePos(K));
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, false);
		ctl.creamery().addMilk(50, 3.72, 3.06);
		runFor(level, ctl, 400);
		helper.assertTrue(ctl.creamery().bottled(dev.farmings.process.creamery.Creamery.Bottle.MILK) == 0 && ctl.readout().contains("NO POWER"),
				Component.literal("no heater power, no milk: " + ctl.readout()));
		helper.succeed();
	}

	private static void milkLineFromParlor(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos p = helper.absolutePos(new BlockPos(8, 3, 9));    // the parlor controller
		buildParlor(level, p, 2);
		BlockPos q = new BlockPos(p.getX(), p.getY() - 1, p.getZ() - 5);   // the creamery controller, on the parlor's floor level
		buildCreamery(level, q);
		for (int z = p.getZ() - 3; z <= p.getZ() - 1; z++) {
			parlorPart(level, dev.farmings.parlor.ParlorParts.MILK_LINE, new BlockPos(p.getX() + 1, p.getY() - 1, z));
		}
		dev.farmings.parlor.ParlorControllerEntity parlor = (dev.farmings.parlor.ParlorControllerEntity) level.getBlockEntity(p);
		parlor.refresh(level);
		parlor.tank(level).tank().add(120, 4, 4.76, 3.63);
		dev.farmings.creamery.CreameryControllerEntity ctl = (dev.farmings.creamery.CreameryControllerEntity) level.getBlockEntity(q);
		ctl.refresh(level);
		helper.assertTrue(ctl.formed(), Component.literal("the creamery forms: " + ctl.readout()));
		double moved = ctl.pullFromParlor(level, 50);
		helper.assertTrue(moved == 50 && Math.abs(ctl.creamery().rawFat() - 4.76) < 1e-9 && Math.abs(parlor.tank(level).tank().litres() - 70) < 1e-9,
				Component.literal("50 L of Jersey milk piped across, moved " + moved + ", fat " + ctl.creamery().rawFat()));
		helper.succeed();
	}

	private static void creameryStateSurvivesSave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		buildCreamery(level, helper.absolutePos(K));
		dev.farmings.creamery.CreameryControllerEntity ctl = creamery(helper, K, true);
		ctl.creamery().setMode(dev.farmings.process.creamery.Creamery.Mode.CHEESE);
		ctl.creamery().addMilk(300, 3.9, 3.1);
		runFor(level, ctl, 400);
		net.minecraft.nbt.CompoundTag tag = ctl.saveWithFullMetadata(level.registryAccess());
		net.minecraft.world.level.block.entity.BlockEntity copy = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(ctl.getBlockPos(), ctl.getBlockState(), tag, level.registryAccess());
		helper.assertTrue(copy instanceof dev.farmings.creamery.CreameryControllerEntity k && java.util.Arrays.equals(k.creamery().save(), ctl.creamery().save()),
				Component.literal("the creamery's milk, batch and products survive a save"));
		helper.succeed();
	}

	/** Every block and every hand-made item has a recipe; animal and machine products (manure, filled straws, raw steaks, dairy) do not. */
	private static void recipesCoverAllItems(GameTestHelper helper) {
		java.util.Set<String> madeElsewhere = java.util.Set.of("manure", "semen_straw", "select_steak", "choice_steak", "prime_steak", "bottled_milk",
				"cream_bottle", "skim_milk_bottle", "buttermilk_bottle", "whey_bottle", "butter", "cheddar_wheel", "spoiled_bottle", "rancid_butter", "moldy_cheese", "spoiled_steak",
				"fleece", "loose_wool", "chicken_egg", "turkey_egg", "quail_egg", "duck_egg", "poultry_litter", "turkey", "duck", "quail", "spoiled_poultry", "rotten_egg", "down", "egg_carton", "alfalfa_seeds", "timothy_seeds", "orchardgrass_seeds", "corn_seeds", "oats_seeds", "barley_seeds", "alfalfa_hay", "grass_hay", "alfalfa_bale", "grass_bale", "straw_bale", "corn_grain", "oat_grain", "barley_grain", "chopped_corn", "chopped_alfalfa", "corn_silage", "haylage", "compost", "separated_solids", "liquid_digestate", "corn_meal", "oat_meal", "barley_meal", "alfalfa_meal", "fish_meal", "ground_limestone", "ration", "lamb_chop", "mutton_chop", "spoiled_mutton", "skirtings", "wool_bale",
				"clean_wool", "sliver", "lanolin", "wool_cloth", "felt", "yarn_white", "sheep_milk_bottle", "sheep_cheese_wheel");
		net.minecraft.world.item.crafting.RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
		java.util.List<String> missing = new java.util.ArrayList<>();
		int checked = 0;
		for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
			if (!id.getNamespace().equals(Farmings.MOD_ID) || madeElsewhere.contains(id.getPath())) {
				continue;
			}
			checked++;
			boolean has = false;
			for (String suffix : new String[] {"", "_from_smelting"}) {
				has |= recipes.byKey(ResourceKey.create(Registries.RECIPE, Farmings.id(id.getPath() + suffix))).isPresent();
			}
			if (!has) {
				missing.add(id.getPath());
			}
		}
		helper.assertTrue(missing.isEmpty() && checked >= 37, Component.literal("items without a recipe: " + missing + " (checked " + checked + ")"));
		helper.succeed();
	}
}
