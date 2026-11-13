package dev.farmings.genetics;

import java.util.EnumMap;
import java.util.Map;

/** A pig's coat genes: one allele pair per {@link PigLocus}, kept apart from the core {@link Genome} as the goat's are. */
public record PigGenes(Map<PigLocus, int[]> alleles) {
	public PigGenes {
		EnumMap<PigLocus, int[]> copy = new EnumMap<>(PigLocus.class);
		for (PigLocus l : PigLocus.values()) {
			int[] p = alleles.get(l);
			copy.put(l, p == null ? new int[] {0, 0} : new int[] {l.clamp(p[0]), l.clamp(p[1])});
		}
		alleles = java.util.Collections.unmodifiableMap(copy);
	}

	public int[] pair(PigLocus l) {
		return alleles.get(l).clone();
	}

	public int expressed(PigLocus l) {
		return l.express(alleles.get(l)[0], alleles.get(l)[1]);
	}


	public static PigGenes inherit(PigGenes dam, PigGenes sire, long breedingSeed) {
		Rng rng = new Rng(breedingSeed ^ VisualGenome.VISUAL_SALT);
		EnumMap<PigLocus, int[]> out = new EnumMap<>(PigLocus.class);
		for (PigLocus l : PigLocus.values()) {
			int a = dam.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			int b = sire.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			out.put(l, new int[] {a, b});
		}
		return new PigGenes(out);
	}

	/** A founder of a breed. */
	public static PigGenes founder(String breedCode, long seed) {
		Rng rng = new Rng(seed ^ VisualGenome.VISUAL_SALT);
		EnumMap<PigLocus, int[]> out = new EnumMap<>(PigLocus.class);
		for (PigLocus l : PigLocus.values()) {
			out.put(l, new int[] {draw(breedCode, l, rng), draw(breedCode, l, rng)});
		}
		return new PigGenes(out);
	}

	private static int draw(String breed, PigLocus l, Rng rng) {
		int[] w = PigLocus.founder(breed, l);
		int total = 0;
		for (int i = 1; i < w.length; i += 2) {
			total += w[i];
		}
		double r = rng.next() * total;
		for (int i = 0; i < w.length; i += 2) {
			r -= w[i + 1];
			if (r < 0) {
				return w[i];
			}
		}
		return w[w.length - 2];
	}

	public int[] save() {
		int[] out = new int[PigLocus.values().length * 2];
		for (PigLocus l : PigLocus.values()) {
			out[l.ordinal() * 2] = alleles.get(l)[0];
			out[l.ordinal() * 2 + 1] = alleles.get(l)[1];
		}
		return out;
	}

	public static PigGenes load(int[] saved) {
		if (saved == null || saved.length != PigLocus.values().length * 2) {
			return null;
		}
		EnumMap<PigLocus, int[]> out = new EnumMap<>(PigLocus.class);
		for (PigLocus l : PigLocus.values()) {
			out.put(l, new int[] {saved[l.ordinal() * 2], saved[l.ordinal() * 2 + 1]});
		}
		return new PigGenes(out);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof PigGenes g && java.util.Arrays.equals(save(), g.save());
	}

	@Override
	public int hashCode() {
		return java.util.Arrays.hashCode(save());
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("PigGenes{");
		for (PigLocus l : PigLocus.values()) {
			b.append(l.id).append('=').append(l.label(alleles.get(l)[0])).append('/').append(l.label(alleles.get(l)[1])).append(' ');
		}
		return b.append('}').toString();
	}
}
