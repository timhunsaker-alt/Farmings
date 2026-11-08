package dev.farmings.genetics;

/**
 * splitmix32, the public-domain generator FarmChain uses (packages/genetics/src/domain/rng.ts). The same seed gives the same
 * sequence here as there; a test checks it against values taken from the FarmChain code.
 */
public final class Rng {
	private int state;

	public Rng(long seed) {
		this.state = (int) seed;
	}

	/** The next float in [0, 1). */
	public double next() {
		state += 0x9e3779b9;
		int t = state ^ (state >>> 16);
		t *= 0x21f0aaad;
		t ^= t >>> 15;
		t *= 0x735a2d97;
		t ^= t >>> 15;
		return (t & 0xFFFFFFFFL) / 4294967296.0;
	}

	public long nextUint32() {
		return (long) Math.floor(next() * 4294967296.0) & 0xFFFFFFFFL;
	}
}
