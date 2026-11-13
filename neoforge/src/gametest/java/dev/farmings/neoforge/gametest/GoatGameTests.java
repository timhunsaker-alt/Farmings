package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.GoatEntity;
import dev.farmings.entity.SheepDogEntity;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.GoatGenes;
import dev.farmings.genetics.GoatLocus;
import dev.farmings.genetics.GoatLook;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.GoatBreed;
import dev.farmings.husbandry.GoatStage;
import dev.farmings.husbandry.Herd;
import dev.farmings.husbandry.SheepBreed;
import dev.farmings.husbandry.parlor.MilkKind;
import dev.farmings.item.StrawItem;
import dev.farmings.item.WhistleItem;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/** Headless scenes for goats (docs/animals/goats.md). Registered from {@link FarmingsGameTests}. */
final class GoatGameTests {
	private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

	private GoatGameTests() {
	}

	static GoatEntity goat(GameTestHelper helper, GoatBreed breed, Sex sex, long seed, double ageDays, BlockPos at) {
		GoatEntity g = helper.spawn(FarmingsEntities.GOAT, at);
		g.initFounder(breed, sex, seed, ageDays, Herd.targetKg(Map.of(breed.code, 10_000), sex == Sex.MALE, ageDays), helper.getLevel().getGameTime());
		return g;
	}

	static GoatEntity adult(GameTestHelper helper, GoatBreed breed, Sex sex, long seed, BlockPos at) {
		return goat(helper, breed, sex, seed, 800, at);
	}

	/** Horned (not polled), so no test goat is intersex or a sterile buck by chance. */
	private static void horned(GoatEntity g) {
		Map<GoatLocus, int[]> m = new EnumMap<>(g.genes().alleles());
		m.put(GoatLocus.POLLED, new int[] {0, 0});
		g.setGenesForTest(new GoatGenes(m));
	}

	private static <T extends net.minecraft.world.entity.Entity> List<T> around(GameTestHelper helper, Class<T> type) {
		return helper.getLevel().getEntitiesOfClass(type, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12));
	}

	/** Founders look like their breed, carry a goat genome, and everything comes back from a save. */
	static void goatSpawnsAndSurvivesSave(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		GoatEntity plain = helper.spawn(FarmingsEntities.GOAT, new BlockPos(1, 1, 1), EntitySpawnReason.COMMAND);
		helper.assertTrue(plain.genome() != null && plain.genome().species() == Species.GOAT && !plain.isBaby(), Component.literal("a spawned goat is an adult goat"));
		GoatEntity boer = adult(helper, GoatBreed.BOER, Sex.MALE, 9L, new BlockPos(3, 1, 1));
		helper.assertTrue(boer.look().pattern() == 2 && !boer.look().polled(), Component.literal("a Boer: white body, red head, horned: " + boer.look().describe()));
		GoatEntity saanen = adult(helper, GoatBreed.SAANEN, Sex.FEMALE, 4242L, new BlockPos(5, 1, 4));
		horned(saanen);
		helper.assertTrue(saanen.look().colour() == GoatLook.Colour.WHITE, Component.literal("a Saanen is white"));
		saanen.setDayOfYearForTest(273);
		saanen.startHeatForDisplay();
		horned(boer);
		helper.assertTrue(saanen.conceive(boer.genome(), boer.genes(), level.getGameTime()), Component.literal("she conceives in heat"));
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		saanen.saveWithoutId(out);
		CompoundTag tag = out.buildResult();
		GoatEntity b = FarmingsEntities.GOAT.create(level, EntitySpawnReason.LOAD);
		b.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		helper.assertTrue(b.breed() == GoatBreed.SAANEN && b.sex() == Sex.FEMALE && b.genome().equals(saanen.genome()), Component.literal("breed, sex, genome survive"));
		helper.assertTrue(b.genes().equals(saanen.genes()) && b.pregnant() && Math.abs(b.weightKg() - saanen.weightKg()) < 1e-9, Component.literal("genes, pregnancy, weight survive"));
		for (Trait t : Species.GOAT.traits()) {
			helper.assertTrue(b.score(t) == saanen.score(t), Component.literal("score " + t.id + " survives"));
		}
		helper.succeed();
	}

	/** Kid, doeling at weaning; a buckling castrated with shears grows as a wether; a grown male is a buck. */
	static void goatLifecycle(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		GoatEntity kid = goat(helper, GoatBreed.NUBIAN, Sex.FEMALE, 1L, 30, new BlockPos(2, 1, 2));
		helper.assertTrue(kid.stage() == GoatStage.KID && kid.isBaby(), Component.literal("a 30-day kid is a kid, got " + kid.stage()));
		kid.advanceForTest(100);
		kid.settle(helper.getLevel());
		helper.assertTrue(kid.stage() == GoatStage.DOELING && !kid.isBaby(), Component.literal("weaned at 90 days she is a doeling, got " + kid.stage()));
		GoatEntity buckling = goat(helper, GoatBreed.BOER, Sex.MALE, 2L, 30, new BlockPos(5, 1, 2));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(HAND, new ItemStack(Items.SHEARS));
		buckling.mobInteract(player, HAND);
		buckling.advanceForTest(300);
		buckling.settle(helper.getLevel());
		helper.assertTrue(buckling.stage() == GoatStage.WETHER, Component.literal("he grows as a wether, got " + buckling.stage()));
		helper.assertTrue(adult(helper, GoatBreed.SAANEN, Sex.MALE, 3L, new BlockPos(5, 1, 5)).stage() == GoatStage.BUCK, Component.literal("a grown male is a buck"));
		player.discard();
		helper.succeed();
	}

	/** In season a buck serves a doe in heat; she kids twins beside her (Saanen x Boer); out of season she is anoestrous. */
	static void doeKids(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		GoatEntity doe = adult(helper, GoatBreed.SAANEN, Sex.FEMALE, 11L, new BlockPos(3, 1, 3));
		GoatEntity buck = adult(helper, GoatBreed.BOER, Sex.MALE, 12L, new BlockPos(4, 1, 3));
		horned(doe);
		horned(buck);
		doe.setNoAi(true);
		buck.setNoAi(true);
		doe.setDayOfYearForTest(150);     // 31 May: a Saanen is out of season
		doe.settle(level);
		helper.assertTrue(!doe.inSeason(level.getGameTime()) && doe.readout().contains("ANOESTROUS"), Component.literal("out of season: " + doe.readout()));
		doe.setDayOfYearForTest(273);
		doe.settle(level);
		doe.startHeatForDisplay();
		doe.settle(level);
		helper.assertTrue(doe.pregnant(), Component.literal("the buck serves her in heat: " + doe.readout()));
		doe.forceKidsForTest(2);
		doe.makeDueNow();
		doe.settle(level);
		List<GoatEntity> kids = around(helper, GoatEntity.class).stream().filter(g -> g != doe && g != buck).toList();
		helper.assertTrue(kids.size() == 2, Component.literal("twins expected, got " + kids.size()));
		double single = Herd.birthKg(Map.of("SAA", 5000, "BOE", 5000), 1);
		for (GoatEntity k : kids) {
			helper.assertTrue(k.stage() == GoatStage.KID && k.composition().equals(Map.of("SAA", 5000, "BOE", 5000)), Component.literal("an F1 kid: " + k.composition()));
			helper.assertTrue(Math.abs(k.weightKg() - single * 0.92) < 1e-6, Component.literal("a twin at 92% of a single's weight: " + k.weightKg()));
			helper.assertTrue(k.damId().equals(doe.herdId()) && k.sireId().equals(buck.herdId()), Component.literal("parents recorded"));
			helper.assertTrue(k.look().pattern() == 1, Component.literal("one EDNRA white-body copy: a belt and the red head"));
		}
		helper.assertTrue(!doe.pregnant() && doe.kiddings() == 1 && doe.kidsNursing() == 2 && doe.milkKg() > 0, Component.literal("she kidded, nurses two and has milk"));
		helper.succeed();
	}

	/** A polled/polled doe is intersex: she never conceives. */
	static void intersexDoeDoesNotBreed(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		GoatEntity doe = adult(helper, GoatBreed.SAANEN, Sex.FEMALE, 21L, new BlockPos(3, 1, 3));
		Map<GoatLocus, int[]> m = new EnumMap<>(doe.genes().alleles());
		m.put(GoatLocus.POLLED, new int[] {1, 1});
		doe.setGenesForTest(new GoatGenes(m));
		GoatEntity buck = adult(helper, GoatBreed.SAANEN, Sex.MALE, 22L, new BlockPos(4, 1, 3));
		horned(buck);
		doe.setDayOfYearForTest(273);
		doe.startHeatForDisplay();
		helper.assertTrue(doe.intersex() && !doe.canConceive() && !doe.conceive(buck.genome(), buck.genes(), level.getGameTime()),
				Component.literal("a P/P doe can't conceive: " + doe.readout()));
		helper.assertTrue(doe.readout().contains("INTERSEX"), Component.literal(doe.readout()));
		helper.succeed();
	}

	/** Killed, a young goat drops kid chops and an old one chevon, by Herd.meatItems. */
	static void goatMeatDrops(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		GoatEntity young = goat(helper, GoatBreed.BOER, Sex.MALE, 31L, 200, new BlockPos(2, 1, 3));
		GoatEntity old = adult(helper, GoatBreed.BOER, Sex.MALE, 32L, new BlockPos(5, 1, 3));
		int youngItems = Herd.meatItems(young.composition(), young.weightKg(), young.ageRealDays());
		int oldItems = Herd.meatItems(old.composition(), old.weightKg(), old.ageRealDays());
		young.kill(helper.getLevel());
		old.kill(helper.getLevel());
		helper.runAfterDelay(5, () -> {
			int kid = around(helper, ItemEntity.class).stream().map(ItemEntity::getItem).filter(s -> s.is(FarmingsItems.KID_CHOP)).mapToInt(ItemStack::getCount).sum();
			int chevon = around(helper, ItemEntity.class).stream().map(ItemEntity::getItem).filter(s -> s.is(FarmingsItems.CHEVON_CHOP)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(kid == youngItems && kid > 0, Component.literal("kid chops " + kid + " expected " + youngItems));
			helper.assertTrue(chevon == oldItems && chevon == 21, Component.literal("chevon chops " + chevon + " expected " + oldItems));
			helper.succeed();
		});
	}

	/** Farmings goats take vanilla goats' mountain spawns; a natural spawn group is wild with one buck. */
	static void wildGoatReplacesVanilla(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		var slopes = TamingGameTests.creatures(level, net.minecraft.world.level.biome.Biomes.SNOWY_SLOPES);
		var types = slopes.stream().map(w -> w.value().type()).toList();
		helper.assertTrue(!types.contains(net.minecraft.world.entity.EntityTypes.GOAT) && types.contains(FarmingsEntities.GOAT),
				Component.literal("snowy slopes have Farmings goats, not vanilla ones: " + types));
		GoatEntity a = FarmingsEntities.GOAT.create(level, EntitySpawnReason.NATURAL);
		BlockPos p = helper.absolutePos(new BlockPos(2, 1, 2));
		a.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
		var data = a.finalizeSpawn(level, level.getCurrentDifficultyAt(a.blockPosition()), EntitySpawnReason.NATURAL, null);
		GoatEntity b = FarmingsEntities.GOAT.create(level, EntitySpawnReason.NATURAL);
		b.snapTo(a.getX() + 1, a.getY(), a.getZ(), 0, 0);
		b.finalizeSpawn(level, level.getCurrentDifficultyAt(b.blockPosition()), EntitySpawnReason.NATURAL, data);
		helper.assertTrue(a.isWild() && b.isWild() && a.breed() == b.breed(), Component.literal("a wild group of one breed"));
		helper.assertTrue(a.sex() == Sex.MALE && b.sex() == Sex.FEMALE, Component.literal("one buck a group"));
		helper.assertTrue(a.readout().startsWith("WILD"), Component.literal(a.readout()));
		helper.succeed();
	}

	/** A wild goat rams a hostile mob that comes close (the test's mock player is always creative, which goats leave be). */
	static void wildGoatRams(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		GoatEntity wild = adult(helper, GoatBreed.BOER, Sex.MALE, 41L, new BlockPos(2, 1, 2));
		wild.setWild(true);
		wild.readyToRamForTest();
		var zombie = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(5, 1, 2));
		zombie.setNoAi(true);
		TamingGameTests.waitFor(helper, 0, 200, () -> wild.rams() > 0, () -> "the wild goat should ram the zombie: at " + wild.distanceTo(zombie) + " blocks", helper::succeed);
	}

	/** One pen run holds one species: with sheep housed in the only run a goat is not taken in, and once the sheep are gone it is. */
	static void goatsKeepTheirOwnPen(GameTestHelper helper) {
		BlockPos c = SheepGameTests.buildShed(helper);
		ServerLevel level = helper.getLevel();
		SheepGameTests.fill(helper);
		var shed = SheepGameTests.shed(helper, c);
		shed.forceDayForTest(false);
		SheepEntity ewe = SheepGameTests.adult(helper, SheepBreed.SUFFOLK, Sex.FEMALE, 51L, new BlockPos(3, 1, 2));
		ewe.assignShed(c, null);
		TamingGameTests.waitFor(helper, 0, 200, () -> ewe.home() != null, () -> "the ewe gets a pen cell: " + shed.readout(), () -> {
			GoatEntity goat = adult(helper, GoatBreed.SAANEN, Sex.FEMALE, 52L, new BlockPos(2, 1, 1));
			goat.assignShed(c, null);       // scenes run side by side: tie her to this scene's shed
			helper.assertTrue(shed.runSpecies(level, ewe.home()) == Species.SHEEP, Component.literal("the pen run is the sheep's"));
			helper.runAfterDelay(40, () -> {
				helper.assertTrue(goat.home() == null, Component.literal("the goat gets no cell in the sheep's run: " + goat.home()));
				ewe.discard();
				TamingGameTests.waitFor(helper, 0, 200, () -> goat.home() != null, () -> "with the sheep gone the goat is housed: " + shed.readout(), () -> {
					helper.assertTrue(shed.runSpecies(level, goat.home()) == Species.GOAT, Component.literal("the run is now the goats'"));
					helper.succeed();
				});
			});
		});
	}

	/** On pasture a goat beside leaves browses half its feed from them, so it grazes fewer grass blocks than a goat in the open. */
	static void goatBrowsesBesideLeaves(GameTestHelper helper) {
		BlockPos c = SheepGameTests.buildShed(helper);
		ServerLevel level = helper.getLevel();
		var shed = SheepGameTests.shed(helper, c);
		shed.forceDayForTest(true);
		shed.refresh(level);
		GoatEntity g = adult(helper, GoatBreed.SAANEN, Sex.FEMALE, 61L, new BlockPos(9, 1, 3));
		g.setNoAi(true);
		g.assignShed(c, helper.absolutePos(new BlockPos(3, 1, 2)));
		for (int i = 0; i < 300; i++) {
			shed.tend(level, List.of(g), 0.01);
		}
		double open = shed.grazedBlocks();
		helper.setBlock(new BlockPos(10, 2, 3), net.minecraft.world.level.block.Blocks.OAK_LEAVES);
		for (int i = 0; i < 300; i++) {
			shed.tend(level, List.of(g), 0.01);
		}
		double browsing = shed.grazedBlocks() - open;
		helper.assertTrue(open >= 3 && browsing <= open / 2 + 1, Component.literal("grazed " + open + " blocks in the open, " + browsing + " beside leaves"));
		helper.assertTrue(g.lastFed() > 0.9, Component.literal("and she is fed: " + g.lastFed()));
		helper.succeed();
	}

	private static GoatEntity dairyDoe(ServerLevel level, BlockPos at, BlockPos shed, long seed) {
		GoatEntity e = FarmingsEntities.GOAT.create(level, EntitySpawnReason.COMMAND);
		e.initFounder(GoatBreed.SAANEN, Sex.FEMALE, seed, 800, Herd.targetKg(Map.of("SAA", 10_000), false, 800), level.getGameTime());
		horned(e);
		e.setPos(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5);
		level.addFreshEntity(e);
		e.assignShed(shed, at);
		e.setKiddedForTest(40, 1);
		return e;
	}

	/** A Saanen doe is called to a small stall and milked into the tank as goat milk at the Saanen fat. */
	static void dairyDoeMilkedGoatTank(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(FarmingsGameTests.C);
		BlockPos shed = FarmingsGameTests.buildSheepParlor(level, c, 2);
		var ctl = FarmingsGameTests.parlorController(helper, FarmingsGameTests.C);
		ctl.pump(level).forceRunning(true);
		var tank = ctl.tank(level);
		tank.forceChilling(true);
		GoatEntity doe = dairyDoe(level, c.offset(-2, -1, 3), shed, 601L);
		double kg = doe.milkKg();
		helper.assertTrue(kg > 2, Component.literal("a Saanen at her 40-day peak has milk, got " + kg));
		ctl.milk(level, 20);
		helper.assertTrue(doe.sentTo() != null && doe.reservedBy() != null, Component.literal("she is called to a stall: " + ctl.readout(level)));
		BlockPos stand = doe.sentTo();
		doe.setPos(stand.getX() + 0.5, stand.getY() + 0.2, stand.getZ() + 1.3);
		ctl.milk(level, 20);
		helper.assertTrue(doe.milkKg() == 0 && tank.tank().holds(MilkKind.GOAT), Component.literal("she is milked into the tank as goat milk"));
		helper.assertTrue(Math.abs(tank.tank().fatPct() - 3.2) < 1e-6 && Math.abs(tank.tank().litres() - kg / 1.03) < 1e-6, Component.literal("Saanen fat, got " + tank.tank().fatPct()));
		helper.succeed();
	}

	/** A tank holding sheep milk calls no doe: the fault names the sheep milk and she keeps hers. */
	static void goatMilkNotMixedWithSheep(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(FarmingsGameTests.C);
		BlockPos shed = FarmingsGameTests.buildSheepParlor(level, c, 2);
		var ctl = FarmingsGameTests.parlorController(helper, FarmingsGameTests.C);
		ctl.pump(level).forceRunning(true);
		var tank = ctl.tank(level);
		tank.forceChilling(true);
		tank.tank().add(10, 4, 5.68, 4.92, MilkKind.SHEEP);
		GoatEntity doe = dairyDoe(level, c.offset(-2, -1, 3), shed, 602L);
		ctl.milk(level, 20);
		helper.assertTrue(ctl.fault().equals("TANK HOLDS SHEEP MILK") && doe.reservedBy() == null && doe.milkKg() > 0,
				Component.literal("fault " + ctl.fault() + ", doe called " + (doe.reservedBy() != null)));
		helper.succeed();
	}

	/** Come bye gathers goats as it does sheep. */
	static void dogHerdsGoats(GameTestHelper helper) {
		TamingGameTests.field(helper, 28);
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		BlockPos stand = helper.absolutePos(new BlockPos(3, 1, 3));
		p.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
		SheepDogEntity dog = DogGameTests.tameDog(helper, new BlockPos(14, 1, 14), p);
		List<GoatEntity> herd = List.of(adult(helper, GoatBreed.BOER, Sex.FEMALE, 71L, new BlockPos(18, 1, 18)),
				adult(helper, GoatBreed.SAANEN, Sex.FEMALE, 72L, new BlockPos(19, 1, 17)));
		helper.assertTrue(WhistleItem.blow(helper.getLevel(), p, SheepDogEntity.Command.COME_BYE, null) == 1, Component.literal("the dog hears the whistle"));
		helper.assertTrue(dog.working() && dog.flock().size() == 2, Component.literal("it sets out for 2 goats: " + dog.readout()));
		TamingGameTests.waitFor(helper, 0, 1100, () -> !dog.working(), () -> "the goats are brought in: " + dog.readout(), () -> {
			for (GoatEntity g : herd) {
				helper.assertTrue(g.blockPosition().distSqr(stand) <= 5 * 5, Component.literal("each goat is near the whistler: " + g.blockPosition().toShortString()));
			}
			p.discard();
			helper.succeed();
		});
	}

	/** A buck's straw takes on a doe in heat; on a ewe it is refused as the wrong species. */
	static void buckStrawInseminatesDoe(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		GoatEntity buck = adult(helper, GoatBreed.NUBIAN, Sex.MALE, 81L, new BlockPos(2, 1, 2));
		GoatEntity doe = adult(helper, GoatBreed.SAANEN, Sex.FEMALE, 82L, new BlockPos(4, 1, 2));
		SheepEntity ewe = SheepGameTests.adult(helper, SheepBreed.MERINO, Sex.FEMALE, 83L, new BlockPos(6, 1, 2));
		horned(buck);
		horned(doe);
		for (var e : List.of(buck, doe, ewe)) {
			e.setNoAi(true);
		}
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack empty = new ItemStack(FarmingsItems.EMPTY_STRAW, 2);
		helper.assertTrue(empty.getItem().interactLivingEntity(empty, player, buck, HAND).consumesAction(), Component.literal("the buck fills a straw"));
		empty.getItem().interactLivingEntity(empty, player, buck, HAND);
		List<ItemStack> straws = player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(FarmingsItems.SEMEN_STRAW)).toList();
		ItemStack straw = straws.get(0);
		helper.assertTrue(StrawItem.genomeOf(straw).species() == Species.GOAT, Component.literal("the straw holds a goat genome"));
		ewe.startHeatForDisplay();
		helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, ewe, HAND) == InteractionResult.FAIL && !ewe.pregnant(),
				Component.literal("a buck straw is refused on a ewe"));
		doe.setDayOfYearForTest(273);
		doe.startHeatForDisplay();
		helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, doe, HAND).consumesAction() && doe.pregnant(), Component.literal("the doe in heat takes it"));
		helper.succeed();
	}
}
