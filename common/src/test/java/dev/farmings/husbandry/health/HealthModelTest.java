package dev.farmings.husbandry.health;

import static org.junit.jupiter.api.Assertions.*;

import dev.farmings.genetics.Species;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.health.HealthRecord.Stage;
import java.util.Map;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class HealthModelTest {
	static final TimeScale.Scale SC = TimeScale.LIVESTOCK;
	static final HealthModel.Rules ON = new HealthModel.Rules(true, false, false);

	static HealthModel.Roll seeded(long seed) {
		SplittableRandom r = new SplittableRandom(seed);
		return salt -> r.nextDouble();
	}

	@Test
	void hazardFromAnnualShare() {
		assertEquals(0.000781, Risk.hazard(0.248, 365), 1e-6);
		assertEquals(0.00393, Risk.hazard(0.162, 45), 1e-5);
		// a whole year at the mastitis hazard gives back 24.8%
		assertEquals(0.248, Risk.chance(Risk.hazard(0.248, 365), 365), 1e-9);
	}

	@Test
	void healthCurveHalvesAndDoubles() {
		assertEquals(1.0, Risk.genetic(50), 1e-9);
		assertEquals(0.5, Risk.genetic(100), 1e-9);
		assertEquals(2.0, Risk.genetic(0), 1e-9);
	}

	@Test
	void cmtBands() {
		assertEquals("N", Scc.cmt(100_000));
		assertEquals("T", Scc.cmt(250_000));
		assertEquals("1", Scc.cmt(600_000));
		assertEquals("2", Scc.cmt(3_000_000));
		assertEquals("3", Scc.cmt(6_000_000));
		assertEquals(750_000, Scc.tankLimit(false));
		assertEquals(1_500_000, Scc.tankLimit(true));
	}

	@Test
	void recordSurvivesEncoding() {
		HealthRecord r = new HealthRecord();
		r.infect(Disease.BRD, Stage.SICK, 100, 5000);
		r.vaccinate(Vaccine.RESPIRATORY, 50, 400, SC::realDaysToTicks);
		r.treat(Drug.INJECTION, 200, SC.realDaysToTicks(10), SC.realDaysToTicks(28), SC::realDaysToTicks);
		r.setQuarantineUntil(9999);
		r.setScc(321_000);
		HealthRecord back = HealthRecord.decode(r.encode());
		assertEquals(r.encode(), back.encode());
		assertTrue(back.has(Disease.BRD));
		assertTrue(back.caseOf(Disease.BRD).treated);
		assertTrue(back.meatHeld(200 + SC.realDaysToTicks(27)));
		assertFalse(back.meatHeld(200 + SC.realDaysToTicks(28)));
		assertEquals(321_000, back.scc());
	}

	@Test
	void withdrawalOnTheClock() {
		HealthRecord r = new HealthRecord();
		r.treat(Drug.MASTITIS_TUBE, 0, SC.realDaysToTicks(4), SC.realDaysToTicks(4), SC::realDaysToTicks);
		assertEquals(6400, SC.realDaysToTicks(4));
		assertTrue(r.milkHeld(6399));
		assertFalse(r.milkHeld(6400));
	}

	@Test
	void sameSeedSameCases() {
		for (long seed = 1; seed < 5; seed++) {
			HealthRecord a = run(seed);
			HealthRecord b = run(seed);
			assertEquals(a.encode(), b.encode());
		}
	}

	static HealthRecord run(long seed) {
		HealthRecord r = new HealthRecord();
		r.setLastTick(0);
		r.stress(SC.realDaysToTicks(45));
		HealthModel.Roll roll = seeded(seed);
		for (long t = 20; t < SC.realDaysToTicks(60); t += 20) {
			HealthModel.step(r, Species.CATTLE, Risk.Inputs.typical(200), HealthModel.Group.NONE, t, roll, ON);
		}
		return r;
	}

	/** Over many calves the BRD window gives about 16.2% cases. */
	@Test
	void brdWindowRate() {
		int n = 2000;
		int cases = 0;
		for (int i = 0; i < n; i++) {
			HealthRecord r = new HealthRecord();
			r.setLastTick(0);
			r.stress(SC.realDaysToTicks(45));
			HealthModel.Roll roll = seeded(i);
			// adults: no BRD outside the window
			for (long t = 240; t <= SC.realDaysToTicks(45); t += 240) {
				HealthModel.step(r, Species.CATTLE, Risk.Inputs.typical(800), HealthModel.Group.NONE, t, roll, ON);
			}
			if (r.caseOf(Disease.BRD) != null) {
				cases++;
			}
		}
		assertEquals(0.162, cases / (double) n, 0.03);
	}

	@Test
	void vaccineHalvesBrd() {
		HealthRecord r = new HealthRecord();
		r.stress(1_000_000);
		r.vaccinate(Vaccine.RESPIRATORY, 0, 400, SC::realDaysToTicks);
		HealthRecord plain = new HealthRecord();
		plain.stress(1_000_000);
		double v = HealthModel.hazard(Disease.BRD, r, Species.CATTLE, Risk.Inputs.typical(800), HealthModel.Group.NONE, 10, SC);
		double p = HealthModel.hazard(Disease.BRD, plain, Species.CATTLE, Risk.Inputs.typical(800), HealthModel.Group.NONE, 10, SC);
		assertEquals(0.5, v / p, 1e-9);
	}

	@Test
	void teatDipAndDirtMoveMastitis() {
		HealthRecord r = new HealthRecord();
		Risk.Inputs milk = Risk.Inputs.typical(1000).withMilk(100);
		double clean = HealthModel.hazard(Disease.MASTITIS, r, Species.CATTLE, milk, HealthModel.Group.NONE, 1000, SC);
		double dirty = HealthModel.hazard(Disease.MASTITIS, r, Species.CATTLE, milk.withDirt(1), HealthModel.Group.NONE, 1000, SC);
		assertEquals(3.0, dirty / clean, 1e-9);
		r.dipTeats(1000);
		double dipped = HealthModel.hazard(Disease.MASTITIS, r, Species.CATTLE, milk, HealthModel.Group.NONE, 1000, SC);
		assertEquals(0.3, dipped / clean, 1e-9);
		double fresh = HealthModel.hazard(Disease.MASTITIS, new HealthRecord(), Species.CATTLE, Risk.Inputs.typical(1000).withMilk(10),
				HealthModel.Group.NONE, 1000, SC);
		assertEquals(2.0, fresh / clean, 1e-9);
		assertEquals(0, HealthModel.hazard(Disease.MASTITIS, new HealthRecord(), Species.CATTLE, Risk.Inputs.typical(1000), HealthModel.Group.NONE, 1000, SC));
	}

	@Test
	void contagionNeedsSickPenMates() {
		HealthRecord r = new HealthRecord();
		Risk.Inputs adult = Risk.Inputs.typical(800);
		assertEquals(0, HealthModel.hazard(Disease.STRANGLES, r, Species.HORSE, adult, HealthModel.Group.NONE, 0, SC));
		double h = HealthModel.hazard(Disease.STRANGLES, r, Species.HORSE, adult, new HealthModel.Group(Map.of(Disease.STRANGLES, 0.5)), 0, SC);
		assertEquals(0.075, h, 1e-9);
	}

	@Test
	void treatedCaseRecovers() {
		HealthRecord r = new HealthRecord();
		r.setLastTick(0);
		r.infect(Disease.BRD, Stage.SICK, 0, SC.realDaysToTicks(7));
		r.treat(Drug.INJECTION, 0, SC.realDaysToTicks(10), SC.realDaysToTicks(28), SC::realDaysToTicks);
		HealthModel.step(r, Species.CATTLE, Risk.Inputs.typical(800), HealthModel.Group.NONE, SC.realDaysToTicks(3) + 1, seeded(1), ON);
		assertEquals(Stage.IMMUNE, r.caseOf(Disease.BRD).stage);
		assertFalse(r.sick());
	}

	@Test
	void diseaseOffMeansNoNewCases() {
		HealthRecord r = new HealthRecord();
		r.setLastTick(0);
		r.stress(1_000_000);
		HealthModel.Rules off = new HealthModel.Rules(false, false, false);
		for (long t = 1000; t < 200_000; t += 1000) {
			HealthModel.step(r, Species.CATTLE, Risk.Inputs.typical(100), new HealthModel.Group(Map.of(Disease.BRD, 1.0)), t, seeded(t), off);
		}
		assertTrue(r.cases().isEmpty());
	}

	@Test
	void cdtTooYoungDoesNothing() {
		HealthRecord r = new HealthRecord();
		assertEquals(0, r.vaccinate(Vaccine.CDT, 0, 20, SC::realDaysToTicks));
		assertEquals(1, r.vaccinate(Vaccine.CDT, 0, 50, SC::realDaysToTicks));
		assertFalse(r.protectedBy(Vaccine.CDT, 10, SC::realDaysToTicks));
		assertEquals(2, r.vaccinate(Vaccine.CDT, SC.realDaysToTicks(28), 78, SC::realDaysToTicks));
		assertTrue(r.protectedBy(Vaccine.CDT, SC.realDaysToTicks(30), SC::realDaysToTicks));
		assertFalse(r.protectedBy(Vaccine.CDT, SC.realDaysToTicks(28 + 366), SC::realDaysToTicks));
		assertTrue(r.doseDue(Vaccine.CDT, SC.realDaysToTicks(28 + 366), SC::realDaysToTicks));
	}

	@Test
	void temperaturesSitInTheMsdRange() {
		double[] cow = HealthModel.normalRange(Species.CATTLE);
		double t = HealthModel.temperature(Species.CATTLE, new HealthRecord(), 0.5);
		assertTrue(t >= cow[0] && t <= cow[1]);
		HealthRecord sick = new HealthRecord();
		sick.infect(Disease.BRD, Stage.SICK, 0, 100);
		assertTrue(HealthModel.temperature(Species.CATTLE, sick, 0.5) > cow[1]);
	}
}
