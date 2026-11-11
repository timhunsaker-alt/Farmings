package dev.farmings.farm.conveyor;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a belt or auger run's load is along the run (docs/conveyors.md). Each parcel rides from where it was put on toward the head end
 * at the run's speed; only what has reached the head can be delivered, so a run's transit time is its length over its speed. Follows
 * Tailings' BeltTravel, but a parcel here is any thing (an item stack on the Minecraft side). Plain Java.
 *
 * @param <T> what a parcel carries
 */
public final class Travel<T> {
	/** One parcel and the metres it still has to ride to the head end (0 or less: there). */
	public record Parcel<T>(T load, double toGo) {
	}

	/** Head end first. */
	private final List<Parcel<T>> parcels = new ArrayList<>();

	/** Puts a load on {@code toGo} metres short of the head end. */
	public void add(T load, double toGo) {
		double at = Math.max(0, toGo);
		for (int i = 0; i < parcels.size(); i++) {
			if (parcels.get(i).toGo() > at) {
				parcels.add(i, new Parcel<>(load, at));
				return;
			}
		}
		parcels.add(new Parcel<>(load, at));
	}

	/** The run moves {@code metres} on. A parcel stops at the head end (it waits there to be taken). */
	public void advance(double metres) {
		for (int i = 0; i < parcels.size(); i++) {
			Parcel<T> p = parcels.get(i);
			parcels.set(i, new Parcel<>(p.load(), Math.max(0, p.toGo() - metres)));
		}
	}

	/** The first parcel that has reached the head end, or null. */
	public T peekReady() {
		return !parcels.isEmpty() && parcels.get(0).toGo() <= 1e-9 ? parcels.get(0).load() : null;
	}

	/** Takes the first parcel at the head end; null if none is there. */
	public T takeReady() {
		T t = peekReady();
		if (t != null) {
			parcels.remove(0);
		}
		return t;
	}

	/** Replaces the parcel at the head end (what could not be delivered of it). */
	public void putBackReady(T load) {
		parcels.add(0, new Parcel<>(load, 0));
	}

	public List<Parcel<T>> parcels() {
		return List.copyOf(parcels);
	}

	public int size() {
		return parcels.size();
	}

	public boolean isEmpty() {
		return parcels.isEmpty();
	}

	public void clear() {
		parcels.clear();
	}

	/** Seconds a parcel put on the tail of a {@code metres} run takes to reach the head at {@code metresPerSecond}. */
	public static double transitSeconds(int metres, double metresPerSecond) {
		return metres / metresPerSecond;
	}

	/** Drive power for a belt run: 7.5 kW for each 30 m, rounded up (Tailings' figure, proposed there). */
	public static double beltWatts(int metres) {
		return 7500.0 * Math.max(1, (metres + 29) / 30);
	}

	/** Auger drives: one 3.7 kW (5 hp) drive for each 21 m of run (the retailer's 8 in x 21 ft auger, TO VERIFY). */
	public static double augerWatts(int metres) {
		return 3700.0 * Math.max(1, (metres + 20) / 21);
	}
}
