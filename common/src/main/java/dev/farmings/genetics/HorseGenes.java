package dev.farmings.genetics;

import java.util.EnumMap;
import java.util.Map;

/** A horse's MSTN and coat genes: one allele pair per {@link HorseLocus}, kept apart from the core {@link Genome} as the pig's are. */
public record HorseGenes(Map<HorseLocus, int[]> alleles) {
	public HorseGenes {
		EnumMap<HorseLocus, int[]> copy = new EnumMap<>(HorseLocus.class);
		for (HorseLocus l : HorseLocus.values()) {
			int[] p = alleles.get(l);
			copy.put(l, p == null ? new int[] {0, 0} : new int[] {l.clamp(p[0]), l.clamp(p[1])});
		}
		alleles = java.util.Collections.unmodifiableMap(copy);
	}

	public int[] pair(HorseLocus l) {
		return alleles.get(l).clone();
	}

	/** Copies of allele 1 at a locus: 0, 1 or 2. */
	public int count(HorseLocus l) {
		return alleles.get(l)[0] + alleles.get(l)[1];
	}

	public boolean has(HorseLocus l) {
		return count(l) > 0;
	}

	/** The MSTN genotype as written: "C/C", "C/T" or "T/T". */
	public String mstn() {
		int c = count(HorseLocus.MSTN);
		return c == 2 ? "C/C" : c == 1 ? "C/T" : "T/T";
	}

	public static HorseGenes inherit(HorseGenes dam, HorseGenes sire, long breedingSeed) {
		Rng rng = new Rng(breedingSeed ^ VisualGenome.VISUAL_SALT);
		EnumMap<HorseLocus, int[]> out = new EnumMap<>(HorseLocus.class);
		for (HorseLocus l : HorseLocus.values()) {
			int a = dam.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			int b = sire.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			out.put(l, new int[] {a, b});
		}
		return new HorseGenes(out);
	}

	/** A founder of a breed: each allele drawn by the breed's share. */
	public static HorseGenes founder(String breedCode, long seed) {
		Rng rng = new Rng(seed ^ VisualGenome.VISUAL_SALT);
		EnumMap<HorseLocus, int[]> out = new EnumMap<>(HorseLocus.class);
		for (HorseLocus l : HorseLocus.values()) {
			double p = HorseLocus.founderShare(breedCode, l) / 1000.0;
			out.put(l, new int[] {rng.next() < p ? 1 : 0, rng.next() < p ? 1 : 0});
		}
		return new HorseGenes(out);
	}

	/** The same genes with one locus set: game tests and the creative tools. */
	public HorseGenes with(HorseLocus l, int a, int b) {
		EnumMap<HorseLocus, int[]> out = new EnumMap<>(alleles);
		out.put(l, new int[] {a, b});
		return new HorseGenes(out);
	}

	public int[] save() {
		int[] out = new int[HorseLocus.values().length * 2];
		for (HorseLocus l : HorseLocus.values()) {
			out[l.ordinal() * 2] = alleles.get(l)[0];
			out[l.ordinal() * 2 + 1] = alleles.get(l)[1];
		}
		return out;
	}

	public static HorseGenes load(int[] saved) {
		if (saved == null || saved.length != HorseLocus.values().length * 2) {
			return null;
		}
		EnumMap<HorseLocus, int[]> out = new EnumMap<>(HorseLocus.class);
		for (HorseLocus l : HorseLocus.values()) {
			out.put(l, new int[] {saved[l.ordinal() * 2], saved[l.ordinal() * 2 + 1]});
		}
		return new HorseGenes(out);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof HorseGenes g && java.util.Arrays.equals(save(), g.save());
	}

	@Override
	public int hashCode() {
		return java.util.Arrays.hashCode(save());
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("HorseGenes{");
		for (HorseLocus l : HorseLocus.values()) {
			b.append(l.id).append('=').append(l.label(alleles.get(l)[0])).append('/').append(l.label(alleles.get(l)[1])).append(' ');
		}
		return b.append('}').toString();
	}
}
