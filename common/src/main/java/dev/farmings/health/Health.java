package dev.farmings.health;

import dev.farmings.genetics.Species;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.health.Disease;
import dev.farmings.husbandry.health.Drug;
import dev.farmings.husbandry.health.HealthModel;
import dev.farmings.husbandry.health.HealthRecord;
import dev.farmings.husbandry.health.HealthRecord.Stage;
import dev.farmings.husbandry.health.Risk;
import dev.farmings.husbandry.health.Scc;
import dev.farmings.husbandry.health.Vaccine;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Animal health in the world (docs/animal-health.md): runs each animal's {@link HealthModel} once a second from its own bookkeeping, finds
 * its group, applies what disease does (a slower walk, a cough, lost health, a death when that rule is on), and gives the readout, the
 * vet kit's exam, treatments, vaccines and the meat condemnation.
 */
public final class Health {
	/** The sick walk: lame 30% slower, sick 20% slower (proposed). */
	public static final Identifier SLOW = dev.farmings.Farmings.id("sick_walk");
	/** The group around an animal with no housing: those within 8 blocks (proposed). */
	public static final double LOOSE_GROUP = 8;
	/** How far a housing's members are looked for (the housings take in from 48). */
	public static final double HOUSING_REACH = 64;
	/** Health never falls below this share of the animal's maximum from disease (the dirty-stall floor, 2 of 10). */
	public static final double FLOOR_SHARE = 0.2;
	/** A tamed horse arrives carrying strangles 1 time in 20; a tamed cloven-hoofed animal brings a notifiable disease 1 in 2,000 (proposed). */
	public static final double STRANGLES_ARRIVAL = 1.0 / 20;
	public static final double NOTIFIABLE_ARRIVAL = 1.0 / 2000;
	/** A foot rot pasture stays infective 7 real days after the last sick sheep (Agriculture Victoria: "a maximum of 7 days" off the foot). */
	public static final double PASTURE_DAYS = 7;
	/** A pass through a charged footbath protects 14 real days (MSD: a bath every 14 days in the wet season). */
	public static final double FOOTBATH_DAYS = 14;

	private static final WeakHashMap<LivingEntity, double[]> FORAGE = new WeakHashMap<>();

	private Health() {
	}

	public static TimeScale.Scale scale(HealthHost h) {
		return TimeScale.of(h.species());
	}

	/** The once-a-second step. Each species' {@code settle} calls it. */
	public static void tick(ServerLevel level, HealthHost h) {
		Animal a = h.animal();
		if (!a.isAlive() || h.isWild()) {
			return;
		}
		HealthRecord r = h.health();
		long now = level.getGameTime();
		TimeScale.Scale sc = scale(h);
		// a move into new housing is a stress, as taming is (BRD: the window after arrival)
		BlockPos home = h.housing();
		long homeKey = home == null ? Long.MIN_VALUE : home.asLong();
		if (homeKey != r.home()) {
			if (homeKey != Long.MIN_VALUE && r.home() != Long.MIN_VALUE && h.species() == Species.CATTLE) {
				r.stress(now + sc.realDaysToTicks(HealthModel.BRD_WINDOW_DAYS));
			}
			r.setHome(homeKey);
		}
		HealthModel.Rules rules = FarmingsRules.model(level.getServer());
		HealthWorld world = HealthWorld.get(level);
		HealthModel.Outcome out = HealthModel.step(r, h.species(), inputs(level, h, world, now, rules), group(level, h), now, roll(level, h, now), rules);
		for (String e : out.events()) {
			if (e.startsWith("fell ill")) {
				dev.farmings.Farmings.LOGGER.debug("{} {}", h.herdId(), e);
			}
		}
		if (out.died()) {
			a.hurtServer(level, level.damageSources().generic(), Float.MAX_VALUE);
			return;
		}
		effects(level, h, r, world, now, sc);
		IsolationHold.hold(level, h, now);
		// a footbath under its feet (its 3 px are thinner than the step a block's stepOn reads, so it is also checked here)
		BlockPos feet = a.blockPosition();
		for (BlockPos p : new BlockPos[] {feet, feet.below()}) {
			if (level.getBlockEntity(p) instanceof HealthBlocks.FootbathEntity bath) {
				bath.pass(level, h);
				break;
			}
		}
	}

	private static Risk.Inputs inputs(ServerLevel level, HealthHost h, HealthWorld world, long now, HealthModel.Rules rules) {
		Animal a = h.animal();
		boolean smallRuminant = h.species() == Species.SHEEP || h.species() == Species.GOAT;
		boolean wet = false;
		if (smallRuminant && level.isRaining()) {
			float t = level.getBiome(a.blockPosition()).value().getBaseTemperature();
			// biome temperature 0.4-0.84 stands for 10-21 C, the "50-70 F" soil MSD gives as ideal for foot rot (proposed mapping)
			wet = t >= 0.4F && t <= 0.84F;
		}
		boolean grainJump = false;
		double forage = h.forageShare();
		if (smallRuminant && forage >= 0) {
			double[] f = FORAGE.computeIfAbsent(a, k -> new double[] {forage, now});
			if (f[0] - forage > 0.2 && now - (long) f[1] <= scale(h).realDaysToTicks(3)) {
				grainJump = true;
				f[0] = forage;
				f[1] = now;
			} else if (now - (long) f[1] > scale(h).realDaysToTicks(3) || forage > f[0]) {
				f[0] = forage;
				f[1] = now;
			}
		}
		int wildFowl = 0;
		boolean outbreak = false;
		if (rules.notifiable()) {
			if (h.species().fowl()) {
				wildFowl = level.getEntitiesOfClass(dev.farmings.entity.FowlEntity.class, a.getBoundingBox().inflate(16),
						f -> f.isWild() && (f.species() == Species.DUCK || f.species() == Species.GOOSE)).size();
			}
			for (Disease d : Disease.values()) {
				if (d.notifiable && d.affects(h.species()) && world.unreportedNear(a.blockPosition(), d)) {
					outbreak = true;
				}
			}
		}
		return new Risk.Inputs(h.healthScore(), h.hardinessScore(), h.dirtShare(), h.fedShare(), h.rationScore(), h.cold(), h.ageRealDays(),
				h.inMilk(), h.milkDays(), wet, smallRuminant && world.pastureInfective(h.housing(), now), grainJump, wildFowl, outbreak);
	}

	/** The share of the animal's group spreading each disease, itself left out. */
	public static HealthModel.Group group(ServerLevel level, HealthHost h) {
		List<HealthHost> mates = mates(level, h);
		if (mates.isEmpty()) {
			return HealthModel.Group.NONE;
		}
		Map<Disease, Double> spreading = new EnumMap<>(Disease.class);
		for (HealthHost m : mates) {
			for (HealthRecord.Case c : m.health().cases()) {
				if (c.stage.contagious() && c.disease.beta > 0) {
					spreading.merge(c.disease, 1.0 / mates.size(), Double::sum);
				}
			}
		}
		return new HealthModel.Group(spreading);
	}

	/** Its group: the same housing (or isolation pen), or with neither, the loose animals within 8 blocks. Itself left out. */
	public static List<HealthHost> mates(ServerLevel level, HealthHost h) {
		Animal a = h.animal();
		long pen = h.health().pen();
		BlockPos home = pen != Long.MIN_VALUE ? null : h.housing();
		double reach = pen != Long.MIN_VALUE || home != null ? HOUSING_REACH : LOOSE_GROUP;
		AABB box = a.getBoundingBox().inflate(reach, 8, reach);
		java.util.ArrayList<HealthHost> out = new java.util.ArrayList<>();
		for (Animal e : level.getEntitiesOfClass(Animal.class, box, e -> e != a && e instanceof HealthHost && e.isAlive())) {
			HealthHost m = (HealthHost) e;
			if (m.isWild()) {
				continue;
			}
			long mpen = m.health().pen();
			if (pen != Long.MIN_VALUE) {
				if (mpen == pen) {
					out.add(m);
				}
			} else if (mpen != Long.MIN_VALUE) {
				continue;
			} else if (home != null ? home.equals(m.housing()) : m.housing() == null) {
				out.add(m);
			}
		}
		return out;
	}

	/** A generator seeded by the world, the animal and the tick: the same world gives the same cases (as breeding seeds do). */
	static HealthModel.Roll roll(ServerLevel level, HealthHost h, long now) {
		long seed = level.getSeed() * 0x9E3779B97F4A7C15L ^ h.animal().getUUID().getMostSignificantBits() ^ h.animal().getUUID().getLeastSignificantBits() * 31 ^ now;
		SplittableRandom r = new SplittableRandom(seed);
		return salt -> r.nextDouble();
	}

	private static void effects(ServerLevel level, HealthHost h, HealthRecord r, HealthWorld world, long now, TimeScale.Scale sc) {
		Animal a = h.animal();
		// the walk
		AttributeInstance speed = a.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			double slow = r.lame() ? -0.3 : r.sick() ? -0.2 : 0;
			AttributeModifier have = speed.getModifier(SLOW);
			if (slow == 0 && have != null) {
				speed.removeModifier(SLOW);
			} else if (slow != 0 && (have == null || have.amount() != slow)) {
				speed.removeModifier(SLOW);
				speed.addTransientModifier(new AttributeModifier(SLOW, slow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		}
		// the cough, every 15 s or so
		if (r.coughing() && Math.floorMod(now + a.getId() * 7L, 300L) < 20) {
			net.minecraft.sounds.SoundEvent cough = HealthSounds.cough(h.species());
			if (cough != null) {
				level.playSound(null, a.blockPosition(), cough, net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
			}
		}
		// lost health, never below the floor
		double loss = r.healthLossPerGameDay();
		float floor = (float) (a.getMaxHealth() * FLOOR_SHARE);
		if (loss > 0 && a.getHealth() > floor) {
			a.setHealth(loss >= 100 ? floor : (float) Math.max(floor, a.getHealth() - loss * 20.0 / TimeScale.TICKS_PER_GAME_DAY));
		}
		// a sick sheep or goat leaves its pasture infective (foot rot)
		if ((h.species() == Species.SHEEP || h.species() == Species.GOAT) && h.housing() != null) {
			HealthRecord.Case c = r.caseOf(Disease.FOOT_ROT);
			if (c != null && (c.stage == Stage.SICK || c.stage == Stage.CHRONIC)) {
				world.infectPasture(h.housing(), now + sc.realDaysToTicks(PASTURE_DAYS));
			}
		}
		// a notifiable case keeps its zone alive
		Disease n = r.notifiable();
		if (n != null) {
			world.caseActive(a.blockPosition(), n, now, r.reported());
		}
		if (Math.floorMod(now, 200L) < 20) {
			world.lift(now);
		}
	}

	// ---- arrivals ----

	/** A wild animal tamed in a corral: the stress window, and what it may bring with it (docs/animal-health.md). */
	public static void onTamed(ServerLevel level, HealthHost h, BlockPos corral) {
		long now = level.getGameTime();
		HealthRecord r = h.health();
		TimeScale.Scale sc = scale(h);
		r.setLastTick(now);
		r.setHome(h.housing() == null ? Long.MIN_VALUE : h.housing().asLong());
		HealthModel.Rules rules = FarmingsRules.model(level.getServer());
		SplittableRandom rnd = new SplittableRandom(level.getSeed() ^ h.animal().getUUID().getLeastSignificantBits() ^ now * 0x2545F4914F6CDD1DL);
		if (h.species() == Species.CATTLE) {
			r.stress(now + sc.realDaysToTicks(HealthModel.BRD_WINDOW_DAYS));
		}
		if (rules.disease() && h.species() == Species.HORSE && rnd.nextDouble() < STRANGLES_ARRIVAL) {
			r.infect(Disease.STRANGLES, Stage.INCUBATING, now, now + sc.realDaysToTicks(Disease.STRANGLES.incubationDays));
		}
		if (rules.notifiable() && rnd.nextDouble() < NOTIFIABLE_ARRIVAL) {
			Disease d = h.species() == Species.PIG && rnd.nextBoolean() ? Disease.AFRICAN_SWINE_FEVER
					: Disease.FOOT_AND_MOUTH.affects(h.species()) ? Disease.FOOT_AND_MOUTH : null;
			if (d != null) {
				r.infect(d, Stage.INCUBATING, now, now + sc.realDaysToTicks(d.incubationDays));
			}
		}
		r.event(now, "tamed");
		IsolationHold.sendToQuarantine(level, h, corral);
	}

	/** Housing may take this animal in: not held in an isolation pen and not inside a standstill. */
	public static boolean mayHouse(ServerLevel level, Animal a) {
		if (!(a instanceof HealthHost h)) {
			return true;
		}
		if (h.health().pen() != Long.MIN_VALUE) {
			return false;
		}
		return !HealthWorld.get(level).standstill(a.blockPosition());
	}

	// ---- treatment ----

	/** Gives {@code drug}: treats what it treats and sets the withdrawals on the animal's clock. Returns a message. */
	public static String treat(ServerLevel level, HealthHost h, Drug drug) {
		long now = level.getGameTime();
		TimeScale.Scale sc = scale(h);
		List<Disease> treated = h.health().treat(drug, now, sc.realDaysToTicks(drug.milkWithdrawalDays), sc.realDaysToTicks(drug.meatWithdrawalDays),
				sc::realDaysToTicks);
		StringBuilder b = new StringBuilder(treated.isEmpty() ? "Given " + drug.label + "; nothing it treats" : "Treated " + String.join(", ",
				treated.stream().map(d -> d.label).toList()) + " with " + drug.label);
		if (drug.milkWithdrawalDays > 0) {
			b.append(String.format(Locale.ROOT, "; milk withheld %.0f real days (%.2f game days)", drug.milkWithdrawalDays, sc.realDaysToGameDays(drug.milkWithdrawalDays)));
		}
		if (drug.meatWithdrawalDays > 0) {
			b.append(String.format(Locale.ROOT, "; meat withheld %.0f real days (%.2f game days)", drug.meatWithdrawalDays, sc.realDaysToGameDays(drug.meatWithdrawalDays)));
		}
		return b.toString();
	}

	public static String vaccinate(ServerLevel level, HealthHost h, Vaccine v) {
		long now = level.getGameTime();
		TimeScale.Scale sc = scale(h);
		int doses = h.health().vaccinate(v, now, h.ageRealDays(), sc::realDaysToTicks);
		if (doses == 0) {
			return v.label + " vaccine: too young, the dose does nothing (" + (int) v.minAgeDays + " days at least)";
		}
		dev.wildcatter.quest.QuestEvents.milestone(level, h.animal().blockPosition(), dev.farmings.quest.Milestones.VACCINATED);
		String more = doses < v.primaryDoses ? String.format(Locale.ROOT, "; dose %d due in %.0f real days", doses + 1, v.primaryGapDays) : "; protected, booster in a year";
		return v.label + " vaccine, dose " + doses + more;
	}

	/** A pass through a charged footbath: treats foot rot and protects 14 real days. */
	public static void footbath(ServerLevel level, HealthHost h) {
		long now = level.getGameTime();
		TimeScale.Scale sc = scale(h);
		HealthRecord r = h.health();
		if (r.footbathed(now + sc.realDaysToTicks(FOOTBATH_DAYS - 1))) {
			return;     // bathed within the last real day: no second dose
		}
		if (r.has(Disease.FOOT_ROT)) {
			r.treat(Drug.FOOTBATH, now, 0, 0, sc::realDaysToTicks);
		} else {
			r.event(now, "footbath");
		}
		r.footbath(now + sc.realDaysToTicks(FOOTBATH_DAYS));
	}

	/** Meat from an animal killed in its meat withdrawal is condemned (no food drops). */
	public static boolean condemned(HealthHost h, ItemStack stack) {
		return h.health().meatHeld(h.animal().level().getGameTime()) && stack.has(net.minecraft.core.component.DataComponents.FOOD);
	}

	// ---- what the player sees ----

	/** The readout's health line: only what can be seen without a kit. */
	public static String readout(HealthHost h) {
		long now = h.animal().level().getGameTime();
		HealthRecord r = h.health();
		TimeScale.Scale sc = scale(h);
		StringBuilder b = new StringBuilder();
		java.util.LinkedHashSet<String> signs = new java.util.LinkedHashSet<>();
		for (HealthRecord.Case c : r.cases()) {
			if (c.stage == Stage.SICK || c.stage == Stage.CHRONIC && c.disease != Disease.MASTITIS) {
				signs.add(c.disease.signs());
			}
		}
		if (!signs.isEmpty()) {
			b.append(", LOOKS SICK: ").append(String.join("; ", signs));
		}
		if (now < r.milkUntil() || now < r.meatUntil()) {
			b.append(", RED LEG BAND (withdrawal");
			if (now < r.milkUntil()) {
				b.append(String.format(Locale.ROOT, ", milk %.2f game days", (r.milkUntil() - now) / (double) TimeScale.TICKS_PER_GAME_DAY));
			}
			if (now < r.meatUntil()) {
				b.append(String.format(Locale.ROOT, ", meat %.2f game days", (r.meatUntil() - now) / (double) TimeScale.TICKS_PER_GAME_DAY));
			}
			b.append(')');
		}
		if (r.inQuarantine(now)) {
			b.append(String.format(Locale.ROOT, ", in quarantine %.2f game days more", (r.quarantineUntil() - now) / (double) TimeScale.TICKS_PER_GAME_DAY));
		}
		return b.toString();
	}

	/** The vet kit's exam: temperature, signs, diagnosis, vaccines, withdrawals (docs/animal-health.md, "What the player sees"). */
	public static String exam(ServerLevel level, HealthHost h) {
		long now = level.getGameTime();
		HealthRecord r = h.health();
		TimeScale.Scale sc = scale(h);
		r.examined(now);
		double[] range = HealthModel.normalRange(h.species());
		double t = HealthModel.temperature(h.species(), r, roll(level, h, now).next(0));
		StringBuilder b = new StringBuilder();
		b.append("Exam ").append(h.herdId()).append(String.format(Locale.ROOT, ": %.1f C (normal %.1f-%.1f)%s", t, range[0], range[1], t > range[1] ? " FEVER" : ""));
		boolean any = false;
		for (HealthRecord.Case c : r.cases()) {
			if (c.stage == Stage.IMMUNE) {
				continue;
			}
			any = true;
			b.append("; ").append(c.disease.label.toUpperCase(Locale.ROOT)).append(' ').append(stageLabel(c));
			if (c.stage != Stage.CHRONIC) {
				b.append(String.format(Locale.ROOT, " (%.0f real days)", sc.ticksToRealDays(now - c.since)));
			}
			if (c.treated) {
				b.append(", treated");
			}
			if (c.disease.notifiable) {
				b.append(" - SUSPECT NOTIFIABLE DISEASE: report it (sneak + use the kit)");
			}
		}
		if (!any) {
			b.append("; no disease found");
		}
		if (h.inMilk()) {
			String cmt = Scc.cmt(r.scc());
			b.append(String.format(Locale.ROOT, "; SCC about %,d cells/mL (CMT %s)", r.scc(), cmt));
		}
		for (Map.Entry<Vaccine, HealthRecord.Doses> e : r.vaccines().entrySet()) {
			boolean prot = r.protectedBy(e.getKey(), now, sc::realDaysToTicks);
			b.append("; ").append(e.getKey().label).append(" vaccine ").append(e.getValue().doses()).append(" dose(s)").append(prot ? ", protected" : ", NOT protected");
			if (r.doseDue(e.getKey(), now, sc::realDaysToTicks)) {
				b.append(", DOSE DUE");
			}
		}
		if (now < r.milkUntil()) {
			b.append(String.format(Locale.ROOT, "; milk withheld %.2f game days", (r.milkUntil() - now) / (double) TimeScale.TICKS_PER_GAME_DAY));
		}
		if (now < r.meatUntil()) {
			b.append(String.format(Locale.ROOT, "; meat withheld %.2f game days", (r.meatUntil() - now) / (double) TimeScale.TICKS_PER_GAME_DAY));
		}
		if (r.inQuarantine(now)) {
			b.append(String.format(Locale.ROOT, "; quarantine ends in %.2f game days", (r.quarantineUntil() - now) / (double) TimeScale.TICKS_PER_GAME_DAY));
		}
		dev.wildcatter.quest.QuestEvents.milestone(level, h.animal().blockPosition(), dev.farmings.quest.Milestones.EXAMINED);
		return b.toString();
	}

	private static String stageLabel(HealthRecord.Case c) {
		return switch (c.stage) {
			case INCUBATING -> c.disease == Disease.MASTITIS ? "subclinical" : "incubating";
			case SUBCLINICAL -> "subclinical";
			case SICK -> c.disease == Disease.MASTITIS ? "clinical" : "sick";
			case CHRONIC -> "chronic";
			case CARRIER -> "carrier (shows nothing, still spreads it)";
			case IMMUNE -> "recovered";
		};
	}

	/** Reports a notifiable disease found on this animal: a standstill around it. */
	public static String report(ServerLevel level, HealthHost h) {
		Disease d = h.health().notifiable();
		if (d == null) {
			return "Nothing to report";
		}
		long now = level.getGameTime();
		HealthWorld world = HealthWorld.get(level);
		HealthWorld.Zone z = world.caseActive(h.animal().blockPosition(), d, now, true);
		for (Animal e : level.getEntitiesOfClass(Animal.class, new AABB(z.center).inflate(HealthWorld.ZONE_BLOCKS, 64, HealthWorld.ZONE_BLOCKS),
				e -> e instanceof HealthHost)) {
			HealthHost m = (HealthHost) e;
			if (m.health().notifiable() == d) {
				m.health().setReported(true);
			}
		}
		h.health().event(now, "reported " + d.label);
		return "Reported " + d.label + ": STANDSTILL on every Farmings housing within " + HealthWorld.ZONE_BLOCKS
				+ " blocks until 28 real days after the last case recovers. No animal in or out, no taming, no straws.";
	}

	/** The herd check (the vet kit on a housing controller): every member that is sick, withheld, due a dose or a recheck, or in quarantine. */
	public static List<String> herdCheck(ServerLevel level, BlockPos controller) {
		long now = level.getGameTime();
		java.util.ArrayList<String> lines = new java.util.ArrayList<>();
		for (Animal e : level.getEntitiesOfClass(Animal.class, new AABB(controller).inflate(HOUSING_REACH, 16, HOUSING_REACH), e -> e instanceof HealthHost)) {
			HealthHost h = (HealthHost) e;
			boolean member = controller.equals(h.housing()) || h.health().pen() == controller.asLong();
			if (!member) {
				continue;
			}
			HealthRecord r = h.health();
			TimeScale.Scale sc = scale(h);
			java.util.ArrayList<String> notes = new java.util.ArrayList<>();
			if (r.sick()) {
				notes.add("looks sick");
			}
			for (HealthRecord.Case c : r.cases()) {
				if (c.treated && c.stage == Stage.SICK) {
					notes.add("recheck " + c.disease.label);
				}
			}
			if (now < r.milkUntil()) {
				notes.add(String.format(Locale.ROOT, "milk withheld %.2f game days", (r.milkUntil() - now) / (double) TimeScale.TICKS_PER_GAME_DAY));
			}
			if (now < r.meatUntil()) {
				notes.add(String.format(Locale.ROOT, "meat withheld %.2f game days", (r.meatUntil() - now) / (double) TimeScale.TICKS_PER_GAME_DAY));
			}
			for (Vaccine v : r.vaccines().keySet()) {
				if (r.doseDue(v, now, sc::realDaysToTicks)) {
					notes.add(v.label + " dose due");
				}
			}
			if (r.inQuarantine(now)) {
				notes.add(String.format(Locale.ROOT, "quarantine %.2f game days more", (r.quarantineUntil() - now) / (double) TimeScale.TICKS_PER_GAME_DAY));
			}
			if (h.inMilk() && r.scc() >= Scc.SUBCLINICAL) {
				notes.add("CMT her (high cells)");
			}
			if (!notes.isEmpty()) {
				lines.add(h.herdId() + " (" + h.species().name().toLowerCase(Locale.ROOT) + "): " + String.join(", ", notes));
			}
		}
		if (lines.isEmpty()) {
			lines.add("Nothing due: no animal here is sick, withheld, due a dose or in quarantine.");
		}
		return lines;
	}

	/** For the herd book's health section: the last events with real-day dates. */
	public static List<String> history(HealthHost h) {
		java.util.ArrayList<String> out = new java.util.ArrayList<>();
		for (String e : h.health().events()) {
			int bar = e.indexOf('|');
			long tick = Long.parseLong(e.substring(0, bar));
			out.add(String.format(Locale.ROOT, "day %.0f: %s", tick / (double) TimeScale.TICKS_PER_GAME_DAY, e.substring(bar + 1)));
		}
		return out;
	}

	public static @Nullable HealthHost host(net.minecraft.world.entity.Entity e) {
		return e instanceof HealthHost h ? h : null;
	}
}
