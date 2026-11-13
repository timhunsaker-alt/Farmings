package dev.farmings.genetics;

import java.util.EnumMap;
import java.util.Map;

/**
 * A goat's coat, horn and wattle genes: one allele pair per {@link GoatLocus}. Kept apart from the core {@link Genome}, as the sheep's
 * {@link SheepGenes}, and drawn from the breeding seed with the same domain salt.
 */
public record GoatGenes(Map<GoatLocus, int[]> alleles) {
	public GoatGenes {
		EnumMap<GoatLocus, int[]> copy = new EnumMap<>(GoatLocus.class);
		for (GoatLocus l : GoatLocus.values()) {
			int[] p = alleles.get(l);
			copy.put(l, p == null ? new int[] {0, 0} : new int[] {l.clamp(p[0]), l.clamp(p[1])});
		}
		alleles = java.util.Collections.unmodifiableMap(copy);
	}

	public int[] pair(GoatLocus l) {
		return alleles.get(l).clone();
	}

	public int expressed(GoatLocus l) {
		return l.express(alleles.get(l)[0], alleles.get(l)[1]);
	}

	/** Two polled alleles: an intersex doe or (half the time) a sterile buck (goats.md). */
	public boolean polledHomozygous() {
		int[] p = alleles.get(GoatLocus.POLLED);
		return p[0] == 1 && p[1] == 1;
	}

	public static GoatGenes inherit(GoatGenes dam, GoatGenes sire, long breedingSeed) {
		Rng rng = new Rng(breedingSeed ^ VisualGenome.VISUAL_SALT);
		EnumMap<GoatLocus, int[]> out = new EnumMap<>(GoatLocus.class);
		for (GoatLocus l : GoatLocus.values()) {
			int a = dam.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			int b = sire.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			out.put(l, new int[] {a, b});
		}
		return new GoatGenes(out);
	}

	/** A founder of a breed. Founders never start polled/polled (goats.md: no founder is intersex). */
	public static GoatGenes founder(String breedCode, long seed) {
		Rng rng = new Rng(seed ^ VisualGenome.VISUAL_SALT);
		EnumMap<GoatLocus, int[]> out = new EnumMap<>(GoatLocus.class);
		for (GoatLocus l : GoatLocus.values()) {
			out.put(l, new int[] {draw(breedCode, l, rng), draw(breedCode, l, rng)});
		}
		int[] p = out.get(GoatLocus.POLLED);
		if (p[0] == 1 && p[1] == 1) {
			p[1] = 0;
		}
		return new GoatGenes(out);
	}

	private static int draw(String breed, GoatLocus l, Rng rng) {
		int[] w = GoatLocus.founder(breed, l);
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
		int[] out = new int[GoatLocus.values().length * 2];
		for (GoatLocus l : GoatLocus.values()) {
			out[l.ordinal() * 2] = alleles.get(l)[0];
			out[l.ordinal() * 2 + 1] = alleles.get(l)[1];
		}
		return out;
	}

	public static GoatGenes load(int[] saved) {
		if (saved == null || saved.length != GoatLocus.values().length * 2) {
			return null;
		}
		EnumMap<GoatLocus, int[]> out = new EnumMap<>(GoatLocus.class);
		for (GoatLocus l : GoatLocus.values()) {
			out.put(l, new int[] {saved[l.ordinal() * 2], saved[l.ordinal() * 2 + 1]});
		}
		return new GoatGenes(out);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof GoatGenes g && java.util.Arrays.equals(save(), g.save());
	}

	@Override
	public int hashCode() {
		return java.util.Arrays.hashCode(save());
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("GoatGenes{");
		for (GoatLocus l : GoatLocus.values()) {
			b.append(l.id).append('=').append(l.label(alleles.get(l)[0])).append('/').append(l.label(alleles.get(l)[1])).append(' ');
		}
		return b.append('}').toString();
	}
}
