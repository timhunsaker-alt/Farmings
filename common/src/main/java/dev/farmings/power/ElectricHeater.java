package dev.farmings.power;

import dev.wildcatter.Tuning;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.solver.Complex;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A resistive load on Wildcatter's grid: a heat lamp, an incubator's element, a pasteurizer's heater. Unlike a motor it runs at a
 * power factor of 1, draws no inrush and cannot stall: a relay or thermostat closes it across the supply and it takes P = V^2 / R,
 * as Wildcatter's own lamps and heaters do (its {@code LoadBlockEntity}). Single-phase it sits between hot and neutral; three-phase
 * it is three equal elements in a floating star.
 */
public final class ElectricHeater {
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};

	private final boolean threePhase;
	private final Voltage voltage;
	private final double ratedWatts;
	private final int[] hot = new int[3];
	private int neutral;
	private int star;
	private final int[] branch = {-1, -1, -1};
	private boolean on;
	private double volts;
	private double amps;
	private double watts;

	public ElectricHeater(boolean threePhase, Voltage voltage, double ratedWatts) {
		this.threePhase = threePhase;
		this.voltage = voltage;
		this.ratedWatts = ratedWatts;
	}

	public void build(Circuit circuit, BlockPos pos) {
		build(circuit, pos, List.of(Direction.values()));
	}

	/** Terminals on {@code faces}; the elements only while its relay is closed. */
	public void build(Circuit circuit, BlockPos pos, List<Direction> faces) {
		int phases = threePhase ? 3 : 1;
		for (int k = 0; k < phases; k++) {
			hot[k] = circuit.node();
		}
		neutral = circuit.node();
		java.util.Arrays.fill(branch, -1);
		if (on) {
			double lineToNeutral = voltage.nominal();
			double ohms = lineToNeutral * lineToNeutral / (ratedWatts / phases);   // P = V^2 / R, each element its share
			if (threePhase) {
				star = circuit.node();
				for (int k = 0; k < 3; k++) {
					branch[k] = circuit.impedance(hot[k], star, ohms, 0);
				}
			} else {
				branch[0] = circuit.impedance(hot[0], neutral, ohms, 0);
			}
		}
		for (Direction face : faces) {
			if (threePhase) {
				for (int k = 0; k < 3; k++) {
					circuit.port(pos, face, PHASES[k], voltage, hot[k], Tuning.CONTACT_OHMS / 2, false);
				}
			} else {
				circuit.hotPort(pos, face, voltage, hot[0], Tuning.CONTACT_OHMS / 2, false);
				circuit.port(pos, face, Conductor.N, voltage, neutral, Tuning.CONTACT_OHMS / 2, false);
			}
		}
	}

	/** Reads the solve; {@code wanted}: whether its relay or thermostat calls for heat (it takes effect at the next build). */
	public void apply(Circuit circuit, int ticks, boolean wanted) {
		if (threePhase) {
			volts = (circuit.acrossPhasor(hot[0], hot[1]).abs() + circuit.acrossPhasor(hot[1], hot[2]).abs()
					+ circuit.acrossPhasor(hot[2], hot[0]).abs()) / 3.0;
		} else {
			volts = Math.abs(circuit.across(hot[0], neutral));
		}
		watts = 0;
		amps = 0;
		for (int k = 0; k < (threePhase ? 3 : 1); k++) {
			if (branch[k] >= 0) {
				Complex i = circuit.currentPhasor(branch[k]);
				Complex v = circuit.acrossPhasor(hot[k], threePhase ? star : neutral);
				amps = Math.max(amps, i.abs());
				watts += v.times(i.conjugate()).re();
			}
		}
		on = wanted;   // the relay: the next solve builds it with or without its elements, as a motor's start does
	}

	private double nominal() {
		return threePhase ? voltage.lineToLine() : voltage.nominal();
	}

	/** True when the supply is there to heat on. */
	public boolean supplied() {
		return volts > nominal() * Tuning.BRIGHT_ABOVE;
	}

	/** Its heat as a share of its rating, 0 to 1: V^2 / R falls with the square of the voltage. */
	public double output() {
		if (!on || branch[0] < 0 || !supplied()) {
			return 0;
		}
		return Math.min(1.0, Math.pow(volts / nominal(), 2));
	}

	public double watts() {
		return watts;
	}

	public double amps() {
		return amps;
	}

	public double volts() {
		return volts;
	}

	/** One line for the machine's readout. */
	public String describe() {
		String supply = threePhase ? voltage.label() + " three-phase" : voltage.label();
		if (volts < 1.0) {
			return "NO POWER: it needs " + supply;
		}
		if (!supplied()) {
			return "too little voltage to heat (" + Math.round(volts) + " V)";
		}
		if (!on || branch[0] < 0) {
			return "heater off (no call for heat), " + Math.round(volts) + " V";
		}
		return String.format("heating, %.0f V, %.1f A, %.0f W", volts, amps, watts);
	}
}
