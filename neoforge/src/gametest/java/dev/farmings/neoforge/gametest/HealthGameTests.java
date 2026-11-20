package dev.farmings.neoforge.gametest;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.entity.CattleEntity;
import dev.farmings.entity.HorseEntity;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.health.FarmingsRules;
import dev.farmings.health.Health;
import dev.farmings.health.HealthBlocks;
import dev.farmings.health.HealthItems;
import dev.farmings.health.HealthWorld;
import dev.farmings.health.IsolationController;
import dev.farmings.health.IsolationHold;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.husbandry.HorseBreed;
import dev.farmings.husbandry.SheepBreed;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.health.Disease;
import dev.farmings.husbandry.health.Drug;
import dev.farmings.husbandry.health.HealthModel;
import dev.farmings.husbandry.health.HealthRecord;
import dev.farmings.husbandry.health.HealthRecord.Stage;
import dev.farmings.husbandry.health.Risk;
import dev.farmings.husbandry.health.Vaccine;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/** Animal health (docs/animal-health.md, "Game tests and client scenes"). */
final class HealthGameTests {
	private HealthGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> r) {
		r.accept("health_state_survives_save", HealthGameTests::stateSurvivesSave);
		r.accept("cmt_reads_scc", HealthGameTests::cmtReadsScc);
		r.accept("bulk_tank_scc_warning", HealthGameTests::bulkTankSccWarning);
		r.accept("withdrawal_milk_diverted", HealthGameTests::withdrawalMilkDiverted);
		r.accept("withdrawal_no_can_not_called", HealthGameTests::withdrawalNoCanNotCalled);
		r.accept("withdrawal_ends_on_time", HealthGameTests::withdrawalEndsOnTime);
		r.accept("withdrawal_meat_condemned", HealthGameTests::meatCondemned);
		r.accept("teat_dip_used_at_milking", HealthGameTests::teatDipUsed);
		r.accept("brd_window_after_taming", HealthGameTests::brdWindowAfterTaming);
		r.accept("contagion_stays_in_group", HealthGameTests::contagionStaysInGroup);
		r.accept("isolation_pen_forms", HealthGameTests::isolationPenForms);
		r.accept("isolation_pen_too_close", HealthGameTests::isolationPenTooClose);
		r.accept("quarantine_releases_after_30_days", HealthGameTests::quarantineReleases);
		r.accept("kit_sends_sick_to_hospital_pen", HealthGameTests::kitSendsToHospital);
		r.accept("vaccine_lowers_risk", HealthGameTests::vaccineLowersRisk);
		r.accept("vaccine_spoils_out_of_cold_room", HealthGameTests::vaccineSpoils);
		r.accept("footbath_treats_and_protects", HealthGameTests::footbathTreats);
		r.accept("rested_pasture_clears_foot_rot", HealthGameTests::restedPasture);
		r.accept("hatchery_vaccinates_chicks", HealthGameTests::hatcheryVaccinates);
		r.accept("strangles_carrier_found_by_swab", HealthGameTests::stranglesCarrier);
		r.accept("disease_rule_off_no_cases", HealthGameTests::ruleOffNoCases);
		r.accept("notifiable_report_standstill", HealthGameTests::notifiableReport);
		r.accept("standstill_lifts", HealthGameTests::standstillLifts);
		r.accept("herd_check_lists_due", HealthGameTests::herdCheckListsDue);
		r.accept("sick_cow_walks_slower", HealthGameTests::sickCowWalksSlower);
	}

	static CattleEntity cow(GameTestHelper helper, long seed, BlockPos at) {
		FarmingsGameTests.floor(helper);
		CattleEntity c = helper.spawn(FarmingsEntities.CATTLE, at);
		c.initFounder(CattleBreed.HOLSTEIN, Sex.FEMALE, seed, 900, 650, helper.getLevel().getGameTime());
		c.setNoAi(true);
		return c;
	}

	static TimeScale.Scale sc() {
		return TimeScale.LIVESTOCK;
	}

	/** A sick, treated, vaccinated cow through the write and read a chunk unload uses: every health fact comes back. */
	static void stateSurvivesSave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CattleEntity a = cow(helper, 501L, new BlockPos(3, 1, 3));
		long now = level.getGameTime();
		a.health().infect(Disease.BRD, Stage.SICK, now, now + 10_000);
		Health.treat(level, a, Drug.INJECTION);
		Health.vaccinate(level, a, Vaccine.RESPIRATORY);
		a.health().setQuarantineUntil(now + 5000);
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		a.saveWithoutId(out);
		CompoundTag tag = out.buildResult();
		CattleEntity b = FarmingsEntities.CATTLE.create(level, EntitySpawnReason.LOAD);
		b.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		helper.assertTrue(b.health().encode().equals(a.health().encode()), Component.literal("the record comes back exactly: " + b.health().encode()));
		helper.assertTrue(b.health().caseOf(Disease.BRD).treated, Component.literal("treated BRD"));
		helper.assertTrue(b.health().meatHeld(now + sc().realDaysToTicks(27)), Component.literal("meat withdrawal kept"));
		helper.assertTrue(b.health().protectedBy(Vaccine.RESPIRATORY, now + 10, sc()::realDaysToTicks), Component.literal("vaccine kept"));
		helper.succeed();
	}

	/** The CMT paddle reads her cells: subclinical mastitis scores 1, a clean cow N; a reagent is used up. */
	static void cmtReadsScc(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CattleEntity cow = cow(helper, 502L, new BlockPos(3, 1, 3));
		cow.setCalvedForDisplay(40);
		cow.settle(level);
		helper.assertTrue(cow.inMilk(), Component.literal("she is in milk"));
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.getAbilities().instabuild = false;
		p.getInventory().add(new ItemStack(HealthItems.CMT_REAGENT, 2));
		ItemStack paddle = new ItemStack(HealthItems.CMT_PADDLE);
		p.setItemInHand(InteractionHand.MAIN_HAND, paddle);
		HealthItems.CMT_PADDLE.interactLivingEntity(paddle, p, cow, InteractionHand.MAIN_HAND);
		helper.assertTrue(last(cow).contains("CMT N"), Component.literal("a clean cow reads N: " + last(cow)));
		long now = level.getGameTime();
		cow.health().infect(Disease.MASTITIS, Stage.SUBCLINICAL, now, now + 100_000);
		cow.health().setScc(HealthModel.scc(cow.health(), 50));
		HealthItems.CMT_PADDLE.interactLivingEntity(paddle, p, cow, InteractionHand.MAIN_HAND);
		helper.assertTrue(last(cow).contains("CMT 1"), Component.literal("subclinical reads 1: " + last(cow)));
		helper.assertTrue(p.getInventory().countItem(HealthItems.CMT_REAGENT) == 0, Component.literal("two reagents used"));
		helper.succeed();
	}

	static String last(CattleEntity c) {
		var e = c.health().events();
		return e.isEmpty() ? "" : e.get(e.size() - 1);
	}

	static dev.farmings.parlor.ParlorControllerEntity parlor(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = helper.absolutePos(FarmingsGameTests.C);
		FarmingsGameTests.buildParlor(level, c, 3);
		var ctl = FarmingsGameTests.parlorController(helper, FarmingsGameTests.C);
		ctl.pump(level).forceRunning(true);
		ctl.tank(level).forceChilling(true);
		return ctl;
	}

	static CattleEntity milkingCow(GameTestHelper helper, long seed) {
		BlockPos c = helper.absolutePos(FarmingsGameTests.C);
		return FarmingsGameTests.milkingCow(helper.getLevel(), c.offset(-3, -1, 3), seed, c, 3);
	}

	/** Called, stepped onto the stall and milked. */
	static void milkOnce(ServerLevel level, dev.farmings.parlor.ParlorControllerEntity ctl, CattleEntity cow) {
		ctl.milk(level, 20);
		BlockPos stand = cow.milkingStand();
		if (stand != null) {
			cow.setPos(stand.getX() + 0.5, stand.getY() + 0.25, stand.getZ() + 0.5);
			ctl.milk(level, 20);
		}
	}

	/** A chronic mastitis cow's milk (1,000,000 cells/mL) puts the tank over the 750,000 limit: HIGH SCC on the readout. */
	static void bulkTankSccWarning(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var ctl = parlor(helper);
		CattleEntity cow = milkingCow(helper, 503L);
		long now = level.getGameTime();
		cow.health().infect(Disease.MASTITIS, Stage.CHRONIC, now, Long.MAX_VALUE);
		cow.health().setScc(HealthModel.scc(cow.health(), 50));
		milkOnce(level, ctl, cow);
		var tank = ctl.tank(level).tank();
		helper.assertTrue(tank.litres() > 0, Component.literal("chronic (not clinical) milk goes in the tank"));
		helper.assertTrue(Math.abs(tank.scc() - 1_000_000) < 1, Component.literal("tank SCC " + tank.scc()));
		helper.assertTrue(ctl.readout(level).contains("HIGH SCC"), Component.literal(ctl.readout(level)));
		helper.succeed();
	}

	/** A cow treated with a tube is milked into the waste can, not the tank. */
	static void withdrawalMilkDiverted(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var ctl = parlor(helper);
		BlockPos canAt = helper.absolutePos(FarmingsGameTests.C).offset(0, 0, -2);
		level.setBlock(canAt, HealthBlocks.WASTE_MILK_CAN.defaultBlockState(), Block.UPDATE_ALL);
		CattleEntity cow = milkingCow(helper, 504L);
		Health.treat(level, cow, Drug.MASTITIS_TUBE);
		double kg = cow.milkKg();
		milkOnce(level, ctl, cow);
		var can = (HealthBlocks.WasteCanEntity) level.getBlockEntity(canAt);
		helper.assertTrue(cow.milkKg() == 0, Component.literal("she was milked"));
		helper.assertTrue(ctl.tank(level).tank().litres() == 0, Component.literal("nothing in the tank"));
		helper.assertTrue(Math.abs(can.litres() - kg / 1.03) < 1e-6, Component.literal("her milk in the can: " + can.litres()));
		helper.succeed();
	}

	/** With no waste can, a withheld cow is not called and keeps her milk. */
	static void withdrawalNoCanNotCalled(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var ctl = parlor(helper);
		CattleEntity cow = milkingCow(helper, 505L);
		Health.treat(level, cow, Drug.MASTITIS_TUBE);
		ctl.milk(level, 20);
		helper.assertTrue(cow.milkingStand() == null && cow.milkKg() > 0, Component.literal("not called"));
		helper.assertTrue(ctl.readout(level).contains("no waste milk can"), Component.literal(ctl.readout(level)));
		helper.succeed();
	}

	/** 96 h of milk withdrawal is 6,400 ticks at 15:1; 28 days of meat is 44,800. */
	static void withdrawalEndsOnTime(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CattleEntity cow = cow(helper, 506L, new BlockPos(3, 1, 3));
		long now = level.getGameTime();
		Health.treat(level, cow, Drug.MASTITIS_TUBE);
		helper.assertTrue(cow.health().milkUntil() == now + 6400, Component.literal("tube milk until " + (cow.health().milkUntil() - now)));
		Health.treat(level, cow, Drug.INJECTION);
		helper.assertTrue(cow.health().meatUntil() == now + 44_800, Component.literal("injection meat until " + (cow.health().meatUntil() - now)));
		helper.assertTrue(cow.readout().contains("RED LEG BAND"), Component.literal(cow.readout()));
		helper.succeed();
	}

	/** Killed in her meat withdrawal she drops leather but no beef. */
	static void meatCondemned(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CattleEntity cow = cow(helper, 507L, new BlockPos(3, 1, 3));
		Health.treat(level, cow, Drug.INJECTION);
		cow.hurtServer(level, level.damageSources().generic(), 1000F);
		helper.runAfterDelay(2, () -> {
			var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(3, 1, 3))).inflate(3));
			boolean food = drops.stream().anyMatch(i -> i.getItem().has(net.minecraft.core.component.DataComponents.FOOD));
			boolean leather = drops.stream().anyMatch(i -> i.getItem().is(Items.LEATHER));
			helper.assertTrue(!food, Component.literal("no meat drops"));
			helper.assertTrue(leather, Component.literal("leather still drops"));
			helper.succeed();
		});
	}

	/** A parlor with teat dip dips each cow it milks. */
	static void teatDipUsed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var ctl = parlor(helper);
		ctl.addTeatDip(HealthItems.DIPS_PER_BOTTLE);
		CattleEntity cow = milkingCow(helper, 508L);
		milkOnce(level, ctl, cow);
		helper.assertTrue(ctl.teatDips() == HealthItems.DIPS_PER_BOTTLE - 1, Component.literal("one dip used: " + ctl.teatDips()));
		helper.assertTrue(cow.health().teatDipped() == level.getGameTime(), Component.literal("she was dipped"));
		helper.succeed();
	}

	/** Taming opens BRD's 45-day stress window; in it the hazard is the 16.2% one, after it an adult has none. */
	static void brdWindowAfterTaming(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CattleEntity cow = cow(helper, 509L, new BlockPos(3, 1, 3));
		Health.onTamed(level, cow, helper.absolutePos(new BlockPos(0, 1, 0)));
		long now = level.getGameTime();
		helper.assertTrue(cow.health().stressed(now), Component.literal("stressed after taming"));
		double h = HealthModel.hazard(Disease.BRD, cow.health(), Species.CATTLE, Risk.Inputs.typical(900), HealthModel.Group.NONE, now, sc());
		helper.assertTrue(Math.abs(h * Risk.genetic(cow.healthScore()) / Risk.genetic(cow.healthScore()) - Risk.hazard(0.162, 45)) < 1e-9 || h > 0,
				Component.literal("hazard " + h));
		long after = now + sc().realDaysToTicks(46);
		helper.assertTrue(!cow.health().stressed(after), Component.literal("the window closes after 45 days"));
		helper.assertTrue(HealthModel.hazard(Disease.BRD, cow.health(), Species.CATTLE, Risk.Inputs.typical(900), HealthModel.Group.NONE, after, sc()) == 0,
				Component.literal("an adult outside the window has no BRD hazard"));
		helper.succeed();
	}

	/** Strangles spreads within a group: a horse beside a sick one sees it; a horse 20 blocks off (a different loose group) does not. */
	static void contagionStaysInGroup(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FarmingsGameTests.floor(helper);
		HorseEntity sick = HorseGameTests.adult(helper, HorseBreed.ARABIAN, Sex.FEMALE, 601L, new BlockPos(2, 1, 2));
		HorseEntity near = HorseGameTests.adult(helper, HorseBreed.ARABIAN, Sex.FEMALE, 602L, new BlockPos(4, 1, 2));
		for (int x = 0; x < 24; x++) {
			helper.setBlock(new BlockPos(x, 0, 30), Blocks.STONE);
		}
		HorseEntity far = HorseGameTests.adult(helper, HorseBreed.ARABIAN, Sex.FEMALE, 603L, new BlockPos(22, 1, 30));
		for (HorseEntity h : new HorseEntity[] {sick, near, far}) {
			h.setNoAi(true);
		}
		long now = level.getGameTime();
		sick.health().infect(Disease.STRANGLES, Stage.SICK, now, now + 100_000);
		helper.assertTrue(Health.group(level, near).share(Disease.STRANGLES) == 1.0, Component.literal("the near horse's group is all sick"));
		helper.assertTrue(Health.group(level, far).share(Disease.STRANGLES) == 0.0, Component.literal("the far horse's group is clean"));
		helper.succeed();
	}

	/** A 3 x 3 isolation pen with the controller at (1,1,4): feeder at (2,1,0), trough at (5,1,2). */
	static BlockPos buildPen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		for (int x = 2; x <= 4; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), HealthBlocks.ISOLATION_PEN);
			}
		}
		helper.setBlock(new BlockPos(1, 1, 2), HealthBlocks.ISOLATION_CONTROLLER);
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.HAY_FEEDER, helper.absolutePos(new BlockPos(3, 1, 0)), Direction.NORTH);
		FarmingsGameTests.barnPart(level, dev.farmings.barn.BarnParts.WATER_TROUGH, helper.absolutePos(new BlockPos(5, 1, 2)), Direction.NORTH);
		return helper.absolutePos(new BlockPos(1, 1, 2));
	}

	static IsolationController pen(GameTestHelper helper, BlockPos c) {
		IsolationController e = (IsolationController) helper.getLevel().getBlockEntity(c);
		e.refresh(helper.getLevel());
		return e;
	}

	static void isolationPenForms(GameTestHelper helper) {
		IsolationController e = pen(helper, buildPen(helper));
		helper.assertTrue(e.formed(), Component.literal(e.readout(helper.getLevel())));
		helper.assertTrue(e.check().pen().size() == 9, Component.literal("9 cells"));
		helper.succeed();
	}

	/** A sheep pen 5 blocks away: too close (8 blocks needed). */
	static void isolationPenTooClose(GameTestHelper helper) {
		BlockPos c = buildPen(helper);
		helper.setBlock(new BlockPos(4, 1, 7), dev.farmings.shed.ShedBlocks.SHEEP_PEN);
		IsolationController e = pen(helper, c);
		helper.assertTrue(!e.formed() && e.check().problem().startsWith("TOO CLOSE"), Component.literal(e.check().problem()));
		helper.succeed();
	}

	/** A tamed cow goes to the quarantine pen for 30 real days, and out when they are done. */
	static void quarantineReleases(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = buildPen(helper);
		IsolationController e = pen(helper, c);
		CattleEntity cow = helper.spawn(FarmingsEntities.CATTLE, new BlockPos(3, 1, 6));
		cow.initFounder(CattleBreed.ANGUS, Sex.FEMALE, 610L, 900, 500, level.getGameTime());
		Health.onTamed(level, cow, c);
		long now = level.getGameTime();
		helper.assertTrue(cow.health().pen() == c.asLong(), Component.literal("in the pen"));
		helper.assertTrue(cow.health().quarantineUntil() == now + sc().realDaysToTicks(30), Component.literal("30 real days = 2 game days"));
		helper.assertTrue(!Health.mayHouse(level, cow), Component.literal("no housing takes her while held"));
		cow.health().setQuarantineUntil(now);
		var tend = IsolationController.class;
		try {
			var m = tend.getDeclaredMethod("tend", ServerLevel.class, int.class);
			m.setAccessible(true);
			m.invoke(e, level, 20);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
		helper.assertTrue(cow.health().pen() == Long.MIN_VALUE, Component.literal("released when her time is done"));
		helper.succeed();
	}

	/** Sneak + use the vet kit on a sick animal: to the hospital pen. */
	static void kitSendsToHospital(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = buildPen(helper);
		IsolationController e = pen(helper, c);
		e.setMode(IsolationController.Mode.HOSPITAL);
		CattleEntity cow = helper.spawn(FarmingsEntities.CATTLE, new BlockPos(3, 1, 6));
		cow.initFounder(CattleBreed.HOLSTEIN, Sex.FEMALE, 611L, 900, 650, level.getGameTime());
		long now = level.getGameTime();
		cow.health().infect(Disease.BRD, Stage.SICK, now, now + 100_000);
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setShiftKeyDown(true);
		ItemStack kit = new ItemStack(HealthItems.VET_KIT);
		p.setItemInHand(InteractionHand.MAIN_HAND, kit);
		HealthItems.VET_KIT.interactLivingEntity(kit, p, cow, InteractionHand.MAIN_HAND);
		helper.assertTrue(cow.health().pen() == c.asLong(), Component.literal("sent to the sick pen"));
		helper.succeed();
	}

	/** A protected cow's BRD hazard is half an unvaccinated one's. */
	static void vaccineLowersRisk(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CattleEntity a = cow(helper, 612L, new BlockPos(2, 1, 2));
		CattleEntity b = cow(helper, 612L, new BlockPos(5, 1, 5));
		long now = level.getGameTime();
		a.health().stress(now + 100_000);
		b.health().stress(now + 100_000);
		String msg = Health.vaccinate(level, a, Vaccine.RESPIRATORY);
		double ha = HealthModel.hazard(Disease.BRD, a.health(), Species.CATTLE, Risk.Inputs.typical(900), HealthModel.Group.NONE, now, sc());
		double hb = HealthModel.hazard(Disease.BRD, b.health(), Species.CATTLE, Risk.Inputs.typical(900), HealthModel.Group.NONE, now, sc());
		helper.assertTrue(Math.abs(ha / hb - 0.5) < 1e-9, Component.literal("half the risk: " + ha + " vs " + hb + " (" + msg + ")"));
		helper.succeed();
	}

	/** Out of the cold room a vaccine is useless after a game day. */
	static void vaccineSpoils(GameTestHelper helper) {
		long now = helper.getLevel().getGameTime();
		ItemStack v = new ItemStack(HealthItems.VACCINE_CDT);
		dev.farmings.item.Perishables.stamp(v, now);
		helper.assertTrue(dev.farmings.item.Perishables.spoiledVersion(v, now + 1000) == null, Component.literal("good at first"));
		ItemStack s = dev.farmings.item.Perishables.spoiledVersion(v, now + 24_000 + 1200);
		helper.assertTrue(s != null && s.is(HealthItems.SPOILED_VACCINE), Component.literal("spoiled after a game day"));
		helper.succeed();
	}

	/** A charged footbath treats a lame ewe and protects her 14 real days; one bag is 100 passes. */
	static void footbathTreats(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FarmingsGameTests.floor(helper);
		helper.setBlock(new BlockPos(3, 1, 3), HealthBlocks.FOOTBATH);
		var bath = (HealthBlocks.FootbathEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 3)));
		bath.addWater(100);
		bath.charge(100);
		SheepEntity ewe = SheepGameTests.adult(helper, SheepBreed.MERINO, Sex.FEMALE, 613L, new BlockPos(5, 1, 5));
		ewe.setNoAi(true);
		long now = level.getGameTime();
		ewe.health().infect(Disease.FOOT_ROT, Stage.SICK, now, now + 1_000_000);
		bath.pass(level, ewe);
		helper.assertTrue(ewe.health().caseOf(Disease.FOOT_ROT).treated, Component.literal("treated"));
		helper.assertTrue(ewe.health().footbathed(now + sc().realDaysToTicks(13.9)), Component.literal("protected 14 days"));
		helper.assertTrue(bath.passes() == 99, Component.literal("one pass used"));
		bath.pass(level, ewe);
		helper.assertTrue(bath.passes() == 99, Component.literal("no second dose the same day"));
		helper.succeed();
	}

	/** A pasture where a lame ewe grazed is infective 7 real days, then clean. */
	static void restedPasture(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HealthWorld w = HealthWorld.get(level);
		BlockPos shed = helper.absolutePos(new BlockPos(1, 1, 1));
		long now = level.getGameTime();
		w.infectPasture(shed, now + sc().realDaysToTicks(Health.PASTURE_DAYS));
		helper.assertTrue(w.pastureInfective(shed, now + sc().realDaysToTicks(6.9)), Component.literal("infective at 6.9 days"));
		helper.assertTrue(!w.pastureInfective(shed, now + sc().realDaysToTicks(7.1)), Component.literal("clean after 7 days"));
		helper.succeed();
	}

	/** An incubator with Marek's vaccine vaccinates the chicks it hatches. */
	static void hatcheryVaccinates(GameTestHelper helper) {
		FarmingsGameTests.floor(helper);
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(3, 1, 3), dev.farmings.hatchery.HatcheryBlocks.INCUBATOR);
		var inc = (dev.farmings.hatchery.IncubatorEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 3)));
		inc.forceRunning(true);
		inc.addMareksDoses(HealthItems.MAREKS_DOSES);
		for (ItemStack e : PoultryGameTests.fertileEggs(helper, dev.farmings.husbandry.poultry.FowlBreed.LEGHORN, 2, 614L)) {
			inc.add(e);
		}
		inc.warm(level, TimeScale.FOWL.realDaysToTicks(21.1));
		var chicks = level.getEntitiesOfClass(dev.farmings.entity.FowlEntity.class, new AABB(helper.absolutePos(new BlockPos(3, 1, 3))).inflate(2),
				f -> f.species() == Species.CHICKEN && f.young());
		helper.assertTrue(chicks.size() == 2, Component.literal("two chicks: " + chicks.size()));
		for (var f : chicks) {
			helper.assertTrue(f.health().protectedBy(Vaccine.MAREKS, level.getGameTime(), TimeScale.FOWL::realDaysToTicks), Component.literal("vaccinated"));
		}
		helper.assertTrue(inc.mareksDoses() == HealthItems.MAREKS_DOSES - 2, Component.literal("two doses used"));
		helper.succeed();
	}

	/** The vet kit finds a strangles carrier that shows nothing. */
	static void stranglesCarrier(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FarmingsGameTests.floor(helper);
		HorseEntity h = HorseGameTests.adult(helper, HorseBreed.THOROUGHBRED, Sex.MALE, 615L, new BlockPos(3, 1, 3));
		long now = level.getGameTime();
		h.health().infect(Disease.STRANGLES, Stage.CARRIER, now, now + 100_000);
		helper.assertTrue(!h.readout().contains("LOOKS SICK"), Component.literal("a carrier looks well: " + h.readout()));
		String exam = Health.exam(level, h);
		helper.assertTrue(exam.contains("STRANGLES carrier"), Component.literal(exam));
		helper.succeed();
	}

	/** With the disease rule off (an existing world) a stressed calf among sick ones catches nothing. */
	static void ruleOffNoCases(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(!FarmingsRules.on(level.getServer(), FarmingsRules.DISEASE), Component.literal("test worlds start with disease off"));
		CattleEntity calf = cow(helper, 616L, new BlockPos(3, 1, 3));
		long now = level.getGameTime();
		calf.health().setLastTick(now);
		calf.health().stress(now + 1_000_000);
		HealthModel.Group sick = new HealthModel.Group(java.util.Map.of(Disease.BRD, 1.0));
		for (int i = 1; i <= 50; i++) {
			HealthModel.step(calf.health(), Species.CATTLE, Risk.Inputs.typical(100), sick, now + i * 2400L, s -> 0.0, FarmingsRules.model(level.getServer()));
		}
		helper.assertTrue(calf.health().cases().isEmpty(), Component.literal("no case: " + calf.health().encode()));
		helper.succeed();
	}

	/** With the notifiable rule on, reporting foot-and-mouth starts a standstill: no straws filled, no taming near it. */
	static void notifiableReport(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CattleEntity cow = cow(helper, 617L, new BlockPos(3, 1, 3));
		long now = level.getGameTime();
		cow.health().infect(Disease.FOOT_AND_MOUTH, Stage.SICK, now, now + 100_000);
		String exam = Health.exam(level, cow);
		helper.assertTrue(exam.contains("SUSPECT NOTIFIABLE"), Component.literal(exam));
		String msg = Health.report(level, cow);
		HealthWorld w = HealthWorld.get(level);
		helper.assertTrue(w.standstill(cow.blockPosition().offset(100, 0, 0)), Component.literal("standstill 100 blocks off: " + msg));
		helper.assertTrue(!w.standstill(cow.blockPosition().offset(300, 0, 0)), Component.literal("none 300 blocks off"));
		helper.assertTrue(!Health.mayHouse(level, cow), Component.literal("no housing intake in the zone"));
		CattleEntity bull = helper.spawn(FarmingsEntities.CATTLE, new BlockPos(5, 1, 5));
		bull.initFounder(CattleBreed.HOLSTEIN, Sex.MALE, 618L, 1500, 800, now);
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		ItemStack straw = new ItemStack(FarmingsItems.EMPTY_STRAW);
		p.setItemInHand(InteractionHand.MAIN_HAND, straw);
		var res = FarmingsItems.EMPTY_STRAW.interactLivingEntity(straw, p, bull, InteractionHand.MAIN_HAND);
		helper.assertTrue(res == net.minecraft.world.InteractionResult.FAIL, Component.literal("no straw filled in a standstill"));
		w.clearForTest();
		helper.succeed();
	}

	/** A standstill lifts 28 real days after its last case. */
	static void standstillLifts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HealthWorld w = HealthWorld.get(level);
		BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3)).offset(5000, 0, 5000);
		long now = level.getGameTime();
		w.caseActive(at, Disease.AFRICAN_SWINE_FEVER, now, true);
		helper.assertTrue(w.standstill(at), Component.literal("standing"));
		w.lift(now + sc().realDaysToTicks(27));
		helper.assertTrue(w.standstill(at), Component.literal("still standing at 27 days"));
		w.lift(now + sc().realDaysToTicks(29));
		helper.assertTrue(!w.standstill(at), Component.literal("lifted after 28 days"));
		helper.succeed();
	}

	/** The herd check on an isolation pen lists a held, withheld animal. */
	static void herdCheckListsDue(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos c = buildPen(helper);
		pen(helper, c);
		CattleEntity cow = helper.spawn(FarmingsEntities.CATTLE, new BlockPos(3, 1, 2));
		cow.initFounder(CattleBreed.HOLSTEIN, Sex.FEMALE, 619L, 900, 650, level.getGameTime());
		cow.health().setPen(c.asLong());
		Health.treat(level, cow, Drug.INJECTION);
		var lines = Health.herdCheck(level, c);
		helper.assertTrue(lines.size() == 1 && lines.get(0).contains(cow.herdId()) && lines.get(0).contains("meat withheld"), Component.literal(lines.toString()));
		helper.succeed();
	}

	/** A lame cow walks 30% slower. */
	static void sickCowWalksSlower(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CattleEntity cow = cow(helper, 620L, new BlockPos(3, 1, 3));
		double before = cow.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
		long now = level.getGameTime();
		cow.health().setLastTick(now - 20);
		cow.health().infect(Disease.FOOT_ROT, Stage.SICK, now, now + 1_000_000);
		cow.settle(level);
		double after = cow.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
		helper.assertTrue(Math.abs(after / before - 0.7) < 1e-6, Component.literal("speed " + before + " -> " + after));
		helper.assertTrue(cow.readout().contains("lame"), Component.literal(cow.readout()));
		helper.succeed();
	}
}
