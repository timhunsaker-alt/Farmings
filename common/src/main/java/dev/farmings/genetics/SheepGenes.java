package dev.farmings.genetics;

import java.util.EnumMap;
import java.util.Map;

/**
 * A sheep's coat and horn genes: one allele pair per {@link SheepLocus}, one allele from each parent. Kept apart from the core
 * {@link Genome} as the cattle coat genes are ({@link VisualGenome}), and drawn from the breeding seed with the same domain salt.
 */
public record SheepGenes(Map<SheepLocus, int[]> alleles) {
	public SheepGenes {
		EnumMap<SheepLocus, int[]> copy = new EnumMap<>(SheepLocus.class);
		for (SheepLocus l : SheepLocus.values()) {
			int[] p = alleles.get(l);
			copy.put(l, p == null ? new int[] {0, 0} : new int[] {l.clamp(p[0]), l.clamp(p[1])});
		}
		alleles = java.util.Collections.unmodifiableMap(copy);
	}

	public int[] pair(SheepLocus l) {
		return alleles.get(l).clone();
	}

	public int expressed(SheepLocus l) {
		return l.express(alleles.get(l)[0], alleles.get(l)[1]);
	}

	public static SheepGenes inherit(SheepGenes dam, SheepGenes sire, long breedingSeed) {
		Rng rng = new Rng(breedingSeed ^ VisualGenome.VISUAL_SALT);
		EnumMap<SheepLocus, int[]> out = new EnumMap<>(SheepLocus.class);
		for (SheepLocus l : SheepLocus.values()) {
			int a = dam.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			int b = sire.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			out.put(l, new int[] {a, b});
		}
		return new SheepGenes(out);
	}

	public static SheepGenes founder(String breedCode, long seed) {
		Rng rng = new Rng(seed ^ VisualGenome.VISUAL_SALT);
		EnumMap<SheepLocus, int[]> out = new EnumMap<>(SheepLocus.class);
		for (SheepLocus l : SheepLocus.values()) {
			out.put(l, new int[] {draw(breedCode, l, rng), draw(breedCode, l, rng)});
		}
		return new SheepGenes(out);
	}

	private static int draw(String breed, SheepLocus l, Rng rng) {
		int[] w = SheepLocus.founder(breed, l);
		if (l.range) {
			return w[0] + (int) Math.floor(rng.next() * (w[1] - w[0] + 1));
		}
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
		int[] out = new int[SheepLocus.values().length * 2];
		for (SheepLocus l : SheepLocus.values()) {
			out[l.ordinal() * 2] = alleles.get(l)[0];
			out[l.ordinal() * 2 + 1] = alleles.get(l)[1];
		}
		return out;
	}

	public static SheepGenes load(int[] saved) {
		if (saved == null || saved.length != SheepLocus.values().length * 2) {
			return null;
		}
		EnumMap<SheepLocus, int[]> out = new EnumMap<>(SheepLocus.class);
		for (SheepLocus l : SheepLocus.values()) {
			out.put(l, new int[] {saved[l.ordinal() * 2], saved[l.ordinal() * 2 + 1]});
		}
		return new SheepGenes(out);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof SheepGenes g && java.util.Arrays.equals(save(), g.save());
	}

	@Override
	public int hashCode() {
		return java.util.Arrays.hashCode(save());
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("SheepGenes{");
		for (SheepLocus l : SheepLocus.values()) {
			b.append(l.id).append('=').append(l.label(alleles.get(l)[0])).append('/').append(l.label(alleles.get(l)[1])).append(' ');
		}
		return b.append('}').toString();
	}
}
