package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.HorseEntity;
import dev.farmings.genetics.HorseGenes;
import dev.farmings.genetics.HorseLocus;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.Equine;
import dev.farmings.husbandry.Gallop;
import dev.farmings.husbandry.HorseBreed;
import dev.farmings.husbandry.HorseStage;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/** Headless scenes for horses (docs/animals/horses.md). Registered from {@link FarmingsGameTests}. */
final class HorseGameTests {
	private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

	private HorseGameTests() {
	}

	static HorseEntity horse(GameTestHelper helper, HorseBreed breed, Sex sex, long seed, double ageDays, BlockPos at) {
		HorseEntity h = helper.spawn(FarmingsEntities.HORSE, at);
		h.initFounder(breed, sex, seed, ageDays, helper.getLevel().getGameTime());
		h.setWild(false);
		return h;
	}

	static HorseEntity adult(GameTestHelper helper, HorseBreed breed, Sex sex, long seed, BlockPos at) {
		return horse(helper, breed, sex, seed, 5 * 365, at);
	}

	/** Founders carry a horse genome with SPEED and STAMINA; breed, genes, fitness and pregnancy survive a save. */
	static void horseSpawnsAndSurvivesSave(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		HorseEntity plain = helper.spawn(FarmingsEntities.HORSE, new BlockPos(1, 1, 1), EntitySpawnReason.COMMAND);
		helper.assertTrue(plain.genome() != null && plain.genome().species() == Species.HORSE && plain.ageYears() >= 3,
				Component.literal("a spawned horse is an adult with a horse genome"));
		HorseEntity mare = adult(helper, HorseBreed.ARABIAN, Sex.FEMALE, 1L, new BlockPos(3, 1, 3));
		HorseEntity stallion = adult(helper, HorseBreed.THOROUGHBRED, Sex.MALE, 2L, new BlockPos(6, 1, 3));
		helper.assertTrue(mare.weightKg() == 408 && stallion.weightKg() == 454, Component.literal("mature weights 408 and 454 kg"));
		mare.setFitnessForTest(0.6);
		mare.startHeatForDisplay();
		mare.sureConceptionForTest();
		helper.assertTrue(mare.conceive(stallion.genome(), stallion.genes(), level.getGameTime()), Component.literal("she conceives"));
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		mare.saveWithoutId(out);
		HorseEntity b = FarmingsEntities.HORSE.create(level, EntitySpawnReason.LOAD);
		b.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		helper.assertTrue(b.breed() == HorseBreed.ARABIAN && b.genome().equals(mare.genome()) && b.genes().equals(mare.genes()) && b.pregnant()
				&& Math.abs(b.fitness() - 0.6) < 1e-9 && b.isTamed() && !b.isWild(), Component.literal("breed, genome, genes, fitness, pregnancy survive"));
		for (Trait t : Species.HORSE.traits()) {
			helper.assertTrue(b.score(t) == mare.score(t), Component.literal("score " + t.id + " survives"));
		}
		helper.succeed();
	}

	/** A foal, a filly at weaning; shears make a colt a gelding; a 3-year male is a stallion; under 2 years nobody rides. */
	static void horseLifecycle(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		HorseEntity foal = horse(helper, HorseBreed.QUARTER_HORSE, Sex.FEMALE, 11L, 30, new BlockPos(2, 1, 2));
		helper.assertTrue(foal.stage() == HorseStage.FOAL && foal.isBaby(), Component.literal("a 30-day horse is a foal"));
		foal.advanceForTest(150);
		foal.settle(helper.getLevel());
		helper.assertTrue(foal.stage() == HorseStage.FILLY && !foal.isBaby(), Component.literal("weaned she is a filly: " + foal.stage()));
		var player = helper.makeMockServerPlayerInLevel();
		foal.mobInteract(player, HAND);
		helper.assertTrue(!foal.isVehicle(), Component.literal("a yearling can't be ridden"));
		HorseEntity colt = horse(helper, HorseBreed.QUARTER_HORSE, Sex.MALE, 12L, 400, new BlockPos(5, 1, 2));
		helper.assertTrue(colt.stage() == HorseStage.COLT, Component.literal("a colt"));
		player.setItemInHand(HAND, new ItemStack(Items.SHEARS));
		colt.mobInteract(player, HAND);
		colt.advanceForTest(3 * 365);
		helper.assertTrue(colt.stage() == HorseStage.GELDING, Component.literal("he grows as a gelding: " + colt.stage()));
		helper.assertTrue(adult(helper, HorseBreed.ARABIAN, Sex.MALE, 13L, new BlockPos(5, 1, 5)).stage() == HorseStage.STALLION,
				Component.literal("a grown male is a stallion"));
		player.discard();
		helper.succeed();
	}

	/** In season a stallion serves a mare in heat and she foals one foal of a tenth her weight; out of season she doesn't cycle. */
	static void mareFoals(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		HorseEntity mare = adult(helper, HorseBreed.THOROUGHBRED, Sex.FEMALE, 21L, new BlockPos(4, 1, 4));
		HorseEntity stallion = adult(helper, HorseBreed.ARABIAN, Sex.MALE, 22L, new BlockPos(5, 1, 4));
		mare.setNoAi(true);
		stallion.setNoAi(true);
		mare.forceDayOfYearForTest(0);     // 1 January
		mare.settle(level);
		helper.assertTrue(!mare.inHeat() && !mare.pregnant(), Component.literal("no heat in January"));
		mare.forceDayOfYearForTest(120);     // 1 May
		mare.sureConceptionForTest();
		mare.settle(level);
		mare.startHeatForDisplay();
		mare.settle(level);
		helper.assertTrue(mare.pregnant() && Math.abs(mare.daysToFoaling() - 340) < 0.1, Component.literal("served, due in 340 days: " + mare.readout()));
		mare.makeDueNow();
		mare.settle(level);
		List<HorseEntity> foals = level.getEntitiesOfClass(HorseEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12)).stream()
				.filter(h -> h != mare && h != stallion).toList();
		helper.assertTrue(foals.size() == 1, Component.literal("one foal, got " + foals.size()));
		HorseEntity f = foals.get(0);
		helper.assertTrue(f.stage() == HorseStage.FOAL && Math.abs(f.weightKg() - 45.4) < 1e-6, Component.literal("a foal of 45.4 kg: " + f.weightKg()));
		helper.assertTrue(f.composition().equals(java.util.Map.of("TB", 5000, "ARA", 5000)) && f.damId().equals(mare.herdId()),
				Component.literal("a half-bred foal out of her"));
		helper.assertTrue(mare.nursing() && mare.stage() == HorseStage.MARE, Component.literal("a mare with foal at foot"));
		helper.succeed();
	}

	/** Plains spawn Farmings horses; a wild group is one breed with one stallion; a wild horse throws you, a tame one takes a saddle. */
	static void wildHorseReplacesVanilla(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		var plains = TamingGameTests.creatures(level, net.minecraft.world.level.biome.Biomes.PLAINS);
		var types = plains.stream().map(w -> w.value().type()).toList();
		helper.assertTrue(!types.contains(net.minecraft.world.entity.EntityTypes.HORSE) && types.contains(FarmingsEntities.HORSE),
				Component.literal("plains have Farmings horses: " + types));
		HorseEntity a = FarmingsEntities.HORSE.create(level, EntitySpawnReason.NATURAL);
		BlockPos p = helper.absolutePos(new BlockPos(2, 1, 2));
		a.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
		var data = a.finalizeSpawn(level, level.getCurrentDifficultyAt(a.blockPosition()), EntitySpawnReason.NATURAL, null);
		HorseEntity b = FarmingsEntities.HORSE.create(level, EntitySpawnReason.NATURAL);
		b.snapTo(a.getX() + 2, a.getY(), a.getZ(), 0, 0);
		b.finalizeSpawn(level, level.getCurrentDifficultyAt(b.blockPosition()), EntitySpawnReason.NATURAL, data);
		level.addFreshEntity(a);
		level.addFreshEntity(b);
		helper.assertTrue(a.isWild() && b.isWild() && a.breed() == b.breed() && a.sex() == Sex.MALE && b.sex() == Sex.FEMALE
				&& a.breed() != HorseBreed.ARABIAN, Component.literal("a wild plains group: one breed, one stallion"));
		var player = helper.makeMockServerPlayerInLevel();
		a.mobInteract(player, HAND);
		helper.assertTrue(!a.isVehicle() && !a.isTamed(), Component.literal("the wild horse won't be ridden"));
		ItemStack saddle = new ItemStack(Items.SADDLE);
		player.setItemInHand(HAND, saddle);
		a.mobInteract(player, HAND);
		helper.assertTrue(a.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), Component.literal("nor saddled"));
		a.tame(player.getUUID());
		helper.assertTrue(a.isTamed(), Component.literal("tamed in the corral, it is a herd horse"));
		player.setItemInHand(HAND, new ItemStack(Items.SADDLE));
		a.mobInteract(player, HAND);
		helper.assertTrue(!a.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), Component.literal("it takes a saddle"));
		player.setItemInHand(HAND, ItemStack.EMPTY);
		a.mobInteract(player, HAND);
		helper.assertTrue(player.getVehicle() == a, Component.literal("and is ridden"));
		player.stopRiding();
		player.discard();
		helper.succeed();
	}

	/**
	 * Galloping counts: a ridden horse moved at 50 m/s (well over half its pace) for 1 km builds 0.02 fitness and syncs its ridden speed
	 * from the distance galloped; 30 s at a walk resets that distance.
	 */
	static void horseGallopBuildsFitness(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		HorseEntity h = adult(helper, HorseBreed.THOROUGHBRED, Sex.FEMALE, 31L, new BlockPos(3, 1, 3));
		h.setNoAi(true);
		h.setNoGravity(true);
		var player = helper.makeMockServerPlayerInLevel();
		player.startRiding(h);
		double x0 = h.getX();
		int[] tick = {0};
		double[] paceSeen = {0};
		helper.succeedWhen(() -> {
			int t = tick[0]++;
			if (t < 420) {
				h.setPos(x0 + (t % 2 == 0 ? 2.5 : 0), h.getY(), h.getZ());
				if (t == 410) {
					paceSeen[0] = h.syncedPace();
				}
			}
			helper.assertTrue(t >= 420 + 620, Component.literal("still running"));
			Gallop.Runner r = h.runner();
			helper.assertTrue(Math.abs(h.fitness() - 0.02) < 0.002, Component.literal("1 km builds 0.02 fitness: " + h.fitness()));
			double expect = new Gallop.Runner(h.composition(), h.genes().count(HorseLocus.MSTN), h.score(Trait.SPEED), h.score(Trait.STAMINA),
					h.ageYears(), 0.02).speedAt(1000);
			helper.assertTrue(paceSeen[0] > 0 && Math.abs(paceSeen[0] - expect) / expect < 0.02 && paceSeen[0] < r.pace(402) * 1.2,
					Component.literal("ridden pace after 1 km " + paceSeen[0] + " vs " + expect));
			helper.assertTrue(h.gallopedM() == 0, Component.literal("30 s at rest resets the distance: " + h.gallopedM()));
			player.stopRiding();
			player.discard();
		});
	}

	/** A stallion's straw keeps (as a bull's) and gets a mare in heat in foal. */
	static void stallionStrawInseminatesMare(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		HorseEntity stallion = adult(helper, HorseBreed.QUARTER_HORSE, Sex.MALE, 41L, new BlockPos(2, 1, 2));
		HorseEntity mare = adult(helper, HorseBreed.QUARTER_HORSE, Sex.FEMALE, 42L, new BlockPos(5, 1, 2));
		stallion.setNoAi(true);
		mare.setNoAi(true);
		Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack empty = new ItemStack(FarmingsItems.EMPTY_STRAW, 1);
		helper.assertTrue(empty.getItem().interactLivingEntity(empty, player, stallion, HAND).consumesAction(), Component.literal("the stallion fills a straw"));
		ItemStack straw = player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(FarmingsItems.SEMEN_STRAW)).findFirst().orElseThrow();
		helper.assertTrue(!dev.farmings.item.StrawItem.stale(straw, helper.getLevel().getGameTime() + 10_000_000L), Component.literal("it keeps"));
		mare.forceDayOfYearForTest(150);
		mare.startHeatForDisplay();
		mare.sureConceptionForTest();
		helper.assertTrue(straw.getItem().interactLivingEntity(straw, player, mare, HAND).consumesAction() && mare.pregnant(), Component.literal("the mare takes it"));
		helper.succeed();
	}

	/** The MSTN gene through the entity: a C/C horse wins the quarter mile and a T/T horse the mile and a half, all else equal. */
	static void sprinterAndStayer(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		HorseEntity cc = adult(helper, HorseBreed.THOROUGHBRED, Sex.MALE, 51L, new BlockPos(2, 1, 2));
		HorseEntity tt = adult(helper, HorseBreed.THOROUGHBRED, Sex.MALE, 51L, new BlockPos(5, 1, 2));
		HorseGenes g = cc.genes();
		cc.setGenesForTest(g.with(HorseLocus.MSTN, 1, 1));
		tt.setGenesForTest(g.with(HorseLocus.MSTN, 0, 0));
		for (Trait t : Species.HORSE.traits()) {
			tt.setScoreForTest(t, cc.score(t));
		}
		helper.assertTrue(cc.runner().time(402) < tt.runner().time(402) && tt.runner().time(2414) < cc.runner().time(2414),
				Component.literal(String.format("402 m: %.2f vs %.2f s; 2,414 m: %.2f vs %.2f s", cc.runner().time(402), tt.runner().time(402),
						cc.runner().time(2414), tt.runner().time(2414))));
		helper.assertTrue(cc.readout().contains("MSTN C/C") && tt.readout().contains("MSTN T/T"), Component.literal(cc.readout()));
		helper.assertTrue(Equine.dryMatterKg(cc.weightKg()) == cc.dailyDryMatterKg(), Component.literal("2.25% of body weight in dry matter"));
		helper.succeed();
	}

	// ---- the stable (docs/multiblocks/stable.md) ----

	/** Stall floor 3 x 8 (two stalls) with a hay rack and trough, a controller, and a gate to a fenced grass paddock to the east. */
	static BlockPos buildStable(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = -1; x <= 15; x++) {
			for (int z = -2; z <= 11; z++) {
				helper.setBlock(new BlockPos(x, 0, z), x >= 6 && x <= 13 && z >= 0 && z <= 9 ? net.minecraft.world.level.block.Blocks.GRASS_BLOCK
						: net.minecraft.world.level.block.Blocks.STONE);
				for (int y = 1; y <= 4; y++) {
					helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
				}
			}
		}
		for (int x = 5; x <= 14; x++) {
			for (int z = -1; z <= 10; z++) {
				if (x == 5 || x == 14 || z == -1 || z == 10) {
					helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.OAK_FENCE);
				}
			}
		}
		helper.setBlock(new BlockPos(1, 1, 2), dev.farmings.stable.StableBlocks.STABLE_CONTROLLER);
		for (int x = 2; x <= 4; x++) {
			for (int z = 1; z <= 8; z++) {
				helper.setBlock(new BlockPos(x, 1, z), dev.farmings.stable.StableBlocks.HORSE_STALL);
			}
		}
		helper.setBlock(new BlockPos(5, 1, 3), dev.farmings.shed.ShedBlocks.PASTURE_GATE);
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.HAY_FEEDER, helper.absolutePos(new BlockPos(2, 1, 0)), net.minecraft.core.Direction.NORTH);
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.WATER_TROUGH, helper.absolutePos(new BlockPos(3, 1, 9)), net.minecraft.core.Direction.NORTH);
		return helper.absolutePos(new BlockPos(1, 1, 2));
	}

	static dev.farmings.stable.StableControllerEntity stable(GameTestHelper helper, BlockPos c) {
		return (dev.farmings.stable.StableControllerEntity) helper.getLevel().getBlockEntity(c);
	}

	/** The stable forms: 24 blocks of stall floor are two stalls, with a rack, a trough and a paddock; without the trough it names it. */
	static void stableForms(GameTestHelper helper) {
		BlockPos c = buildStable(helper);
		ServerLevel level = helper.getLevel();
		var st = stable(helper, c);
		st.refresh(level);
		helper.assertTrue(st.formed() && st.stalls() == 2 && st.paddock() != null && st.paddock().found(), Component.literal("it forms: " + st.readout(level)));
		level.destroyBlock(helper.absolutePos(new BlockPos(3, 1, 9)), false);
		st.refresh(level);
		helper.assertTrue(!st.formed() && st.status().contains("trough"), Component.literal(st.status()));
		helper.succeed();
	}

	/**
	 * Two stalls take two horses, each its own; a third waits. Horses on the paddock graze it bare to dirt and are fed; a stalled horse
	 * eats hay from the rack; indoors their manure soils the straw, and with no straw the bedding is dirty.
	 */
	static void stableHousesFeedsBeds(GameTestHelper helper) {
		BlockPos c = buildStable(helper);
		ServerLevel level = helper.getLevel();
		var st = stable(helper, c);
		st.refresh(level);
		st.forceDayForTest(false);
		HorseEntity a = adult(helper, HorseBreed.THOROUGHBRED, Sex.FEMALE, 61L, new BlockPos(3, 1, 2));
		HorseEntity b = adult(helper, HorseBreed.ARABIAN, Sex.FEMALE, 62L, new BlockPos(3, 1, 6));
		HorseEntity extra = adult(helper, HorseBreed.QUARTER_HORSE, Sex.MALE, 63L, new BlockPos(0, 1, 6));
		for (HorseEntity h : List.of(a, b, extra)) {
			h.setNoAi(true);
		}
		helper.runAfterDelay(45, () -> {
			List<HorseEntity> members = st.members(level);
			helper.assertTrue(members.size() == 2 && !c.equals(extra.place().shedController()), Component.literal("two stalls, two horses: " + members.size()));
			helper.assertTrue(a.place().home() != null && b.place().home() != null && !a.place().home().equals(b.place().home()),
					Component.literal("each its own stall"));
			// a on the paddock grazes; b in her stall eats from the rack
			BlockPos pad = helper.absolutePos(new BlockPos(9, 1, 4));
			a.snapTo(pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5, 0, 0);
			st.refresh(level);
			var rack = (dev.farmings.barn.FeederEntity) level.getBlockEntity(dev.wildcatter.multiblock.BigParts.owner(level, helper.absolutePos(new BlockPos(2, 1, 0))));
			rack.addBlock();
			a.recordTending(0, 1);
			b.recordTending(0, 1);
			ItemStack straw = new ItemStack(dev.farmings.crops.FieldCrops.STRAW_BALE);
			helper.assertTrue(st.addStraw(straw), Component.literal("a straw bale beds the stable"));
			for (int i = 0; i < 100; i++) {
				st.tend(level, List.of(a, b), 0.01);
			}
			helper.assertTrue(st.grazedBlocks() >= 1 && a.lastFed() > 0.85, Component.literal("she grazes the paddock: " + st.grazedBlocks() + ", fed " + a.lastFed()));
			helper.assertTrue(b.lastFed() > 0.85 && rack.dryMatterKg() < 1000, Component.literal("she eats from the rack: fed " + b.lastFed()));
			helper.assertTrue(st.bedding().soiledKg() > 0 && !st.beddingDirty(), Component.literal("manure soils clean straw: " + st.readout(level)));
			helper.assertTrue(st.readout(level).contains("fitness"), Component.literal("the readout lists fitness"));
			st.bedding().load(0, 0);
			st.tend(level, List.of(a, b), 0.01);
			helper.assertTrue(st.beddingDirty(), Component.literal("no straw: dirty"));
			helper.succeed();
		});
	}

	/** An underfed horse gains no fitness from work; a fed one does. */
	static void underfedHorseNoFitness(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		HorseEntity h = adult(helper, HorseBreed.QUARTER_HORSE, Sex.FEMALE, 71L, new BlockPos(3, 1, 3));
		h.setNoAi(true);
		h.recordTending(0.5, 1);
		h.work(1000);
		helper.assertTrue(h.fitness() == 0, Component.literal("underfed: no gain, " + h.fitness()));
		h.recordTending(1, 1);
		h.work(1000);
		helper.assertTrue(Math.abs(h.fitness() - 0.02) < 1e-9, Component.literal("fed: 0.02, " + h.fitness()));
		helper.succeed();
	}

	// ---- the race track (docs/multiblocks/race-track.md) ----

	static final double OVAL_A = 30;
	static final double OVAL_B = 14;
	static final double OVAL_W = 4;

	/** Ramanujan's perimeter of the oval's centre line. */
	static double ovalLap() {
		double h = Math.pow(OVAL_A - OVAL_B, 2) / Math.pow(OVAL_A + OVAL_B, 2);
		return Math.PI * (OVAL_A + OVAL_B) * (1 + 3 * h / (10 + Math.sqrt(4 - 3 * h)));
	}

	/** An oval track around (0, 0) at y 0 on stone, 4 wide; the start gate beside its east end; a finish post at its west end if asked. */
	static BlockPos buildTrack(GameTestHelper helper, boolean post) {
		int ex = (int) (OVAL_A + OVAL_W) + 2;
		int ez = (int) (OVAL_B + OVAL_W) + 2;
		int maxX = Integer.MIN_VALUE;
		for (int x = -ex; x <= ex; x++) {
			for (int z = -ez; z <= ez; z++) {
				double cx = x + 0.5;
				double cz = z + 0.5;
				double r = Math.hypot(cx / OVAL_A, cz / OVAL_B);
				double along = Math.hypot(cx, cz);
				boolean track = Math.abs(along - along / Math.max(1e-9, r)) <= OVAL_W / 2;
				helper.setBlock(new BlockPos(x, 0, z), track ? dev.farmings.racing.RaceBlocks.RACE_TRACK : net.minecraft.world.level.block.Blocks.STONE);
				for (int y = 1; y <= 3; y++) {
					helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
				}
				if (track && z == 0) {
					maxX = Math.max(maxX, x);
				}
			}
		}
		helper.setBlock(new BlockPos(maxX + 1, 0, 0), dev.farmings.racing.RaceBlocks.START_GATE);
		if (post) {
			helper.setBlock(new BlockPos(-maxX - 2, 1, 0), dev.farmings.racing.RaceBlocks.FINISH_POST);
		}
		return helper.absolutePos(new BlockPos(maxX + 1, 0, 0));
	}

	static dev.farmings.racing.StartGateEntity gate(GameTestHelper helper, BlockPos g) {
		return (dev.farmings.racing.StartGateEntity) helper.getLevel().getBlockEntity(g);
	}

	static HorseEntity inStall(GameTestHelper helper, dev.farmings.racing.StartGateEntity gate, int stall, HorseBreed breed, long seed, int mstnC) {
		BlockPos c = gate.stalls().get(stall);
		HorseEntity h = horse(helper, breed, Sex.MALE, seed, 5 * 365, BlockPos.ZERO);
		h.snapTo(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 0, 0);
		h.setNoAi(true);
		h.setGenesForTest(h.genes().with(HorseLocus.MSTN, mstnC >= 1 ? 1 : 0, mstnC >= 2 ? 1 : 0));
		h.setScoreForTest(Trait.SPEED, 700);
		h.setScoreForTest(Trait.STAMINA, 500);
		h.setFitnessForTest(1);
		return h;
	}

	/** The oval forms with its lap the length of its centre line and four stalls; without a finish post it says so. */
	static void trackForms(GameTestHelper helper) {
		BlockPos g = buildTrack(helper, false);
		ServerLevel level = helper.getLevel();
		var gate = gate(helper, g);
		gate.refresh(level);
		helper.assertTrue(gate.problem().contains("finish post"), Component.literal(gate.problem()));
		int maxX = g.getX() - helper.absolutePos(BlockPos.ZERO).getX() - 1;
		helper.setBlock(new BlockPos(-maxX - 2, 1, 0), dev.farmings.racing.RaceBlocks.FINISH_POST);
		helper.runAfterDelay(1, () -> {
			gate.refresh(level);
			helper.assertTrue(gate.formed() && gate.stalls().size() == 4, Component.literal("formed with 4 stalls: " + gate.problem() + " " + gate.stalls().size()));
			helper.assertTrue(Math.abs(gate.shape().lap() - ovalLap()) / ovalLap() < 0.04,
					Component.literal(String.format("lap %.1f m vs centre line %.1f m", gate.shape().lap(), ovalLap())));
			helper.assertTrue(gate.nextDistance().contains("1,207 m"), Component.literal("sneak-use steps the distance"));
			helper.succeed();
		});
	}

	/**
	 * Two AI-run horses over 402 m: the C/C Quarter Horse beats the T/T Arabian; each time is within 5% of what the speed model gives for
	 * the distance; records and the win are kept.
	 */
	static void raceTwoHorses(GameTestHelper helper) {
		BlockPos g = buildTrack(helper, true);
		ServerLevel level = helper.getLevel();
		var gate = gate(helper, g);
		gate.refresh(level);
		HorseEntity qh = inStall(helper, gate, 0, HorseBreed.QUARTER_HORSE, 81L, 2);
		HorseEntity ara = inStall(helper, gate, 2, HorseBreed.ARABIAN, 82L, 0);
		double expectQh = qh.runner().time(402);
		double expectAra = ara.runner().time(402);
		String said = gate.start(level);
		helper.assertTrue(said.contains("2 runners"), Component.literal(said));
		helper.succeedWhen(() -> {
			helper.assertTrue(gate.state() == dev.farmings.racing.StartGateEntity.State.IDLE && !gate.lastResult().isEmpty(), Component.literal("racing"));
			helper.assertTrue(gate.lastResult().get(1).startsWith("1. " + qh.raceName()), Component.literal(String.join(" | ", gate.lastResult())));
			double tq = qh.bestTicks(402) / 20.0;
			double ta = ara.bestTicks(402) / 20.0;
			helper.assertTrue(Math.abs(tq - expectQh) / expectQh < 0.05 && Math.abs(ta - expectAra) / expectAra < 0.05,
					Component.literal(String.format("times %.2f vs %.2f s, %.2f vs %.2f s", tq, expectQh, ta, expectAra)));
			helper.assertTrue(qh.wins() == 1 && qh.races() == 1 && ara.wins() == 0 && ara.races() == 1, Component.literal("records"));
			helper.assertTrue(qh.isNoAi() && ara.isNoAi(), Component.literal("the jockeys hand back the horses as they were (no AI in this test)"));
		});
	}

	/** A ridden horse is timed as its rider runs it: carried round at 15 m/s, 402 m takes 26.8 s. */
	static void riddenRunTimed(GameTestHelper helper) {
		BlockPos g = buildTrack(helper, true);
		ServerLevel level = helper.getLevel();
		var gate = gate(helper, g);
		gate.refresh(level);
		HorseEntity h = inStall(helper, gate, 1, HorseBreed.THOROUGHBRED, 91L, 1);
		h.setNoGravity(true);
		var player = helper.makeMockServerPlayerInLevel();
		player.startRiding(h);
		var shape = gate.shape();
		double s0 = shape.project(h.getX(), h.getZ());
		double off = shape.offset(h.getX(), h.getZ());
		double y = h.getY();
		String said = gate.start(level);
		helper.assertTrue(said.contains("1 runner"), Component.literal(said));
		int[] t = {0};
		helper.succeedWhen(() -> {
			if (gate.state() == dev.farmings.racing.StartGateEntity.State.RUNNING) {
				t[0]++;
				double[] p = shape.pointAt(s0 + 0.75 * t[0], off);
				h.setPos(p[0], y, p[1]);
			}
			helper.assertTrue(gate.state() == dev.farmings.racing.StartGateEntity.State.IDLE && h.bestTicks(402) > 0, Component.literal("running"));
			helper.assertTrue(Math.abs(h.bestTicks(402) - 536) <= 16, Component.literal("timed " + h.bestTicks(402) + " ticks, about 536"));
			helper.assertTrue(gate.lastResult().get(1).contains("(ridden)") && h.wins() == 0, Component.literal(String.join(" | ", gate.lastResult())));
			player.stopRiding();
			player.discard();
		});
	}

	/** The stud book records horses with their race records, and gives a foal's grandparents from its dam's page. */
	static void studBookPedigree(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		HorseEntity mare = adult(helper, HorseBreed.ARABIAN, Sex.FEMALE, 101L, new BlockPos(2, 1, 2));
		HorseEntity stallion = adult(helper, HorseBreed.THOROUGHBRED, Sex.MALE, 102L, new BlockPos(5, 1, 2));
		mare.setNoAi(true);
		stallion.setNoAi(true);
		stallion.recordRace(402, 430, true);
		mare.forceDayOfYearForTest(150);
		mare.startHeatForDisplay();
		mare.sureConceptionForTest();
		helper.assertTrue(mare.conceive(stallion.genome(), stallion.genes(), level.getGameTime()), Component.literal("in foal"));
		mare.makeDueNow();
		mare.settle(level);
		HorseEntity foal = level.getEntitiesOfClass(HorseEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12)).stream()
				.filter(x -> x != mare && x != stallion).findFirst().orElseThrow();
		ItemStack book = new ItemStack(FarmingsItems.STUD_BOOK);
		dev.farmings.item.StudBookItem.record(book, stallion);
		dev.farmings.item.StudBookItem.record(book, mare);
		dev.farmings.item.StudBookItem.record(book, foal);
		var entries = dev.farmings.item.StudBookItem.entries(book);
		helper.assertTrue(entries.size() == 3 && entries.get(0).page().contains("best 402 m 21.50 s") && entries.get(0).page().contains("1 race, 1 win"),
				Component.literal(entries.get(0).page()));
		String ped = dev.farmings.item.StudBookItem.pedigree(entries, entries.get(2));
		helper.assertTrue(ped.contains("Dam " + mare.herdId() + " (by founder out of founder)") && ped.contains("Sire " + stallion.herdId()),
				Component.literal(ped));
		helper.assertTrue(book.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT).pages().size() == 3, Component.literal("three pages"));
		helper.succeed();
	}
}
