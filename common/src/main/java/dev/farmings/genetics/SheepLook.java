package dev.farmings.genetics;

import java.util.Locale;

/**
 * What a sheep shows, worked out from its {@link SheepGenes} and sex (docs/animals/sheep.md, "Coat genes").
 *
 * <ul>
 *   <li>Colour: dominant black E^D gives black; otherwise white/tan A^Wt gives white; a non-agouti a/a sheep is black, or brown
 *       (moorit) when it is also b/b.</li>
 *   <li>Horns (RXFP2, sex-influenced): H/H is horned in both sexes; P/H rams grow scurs, P/H ewes are polled; P/P is polled.</li>
 *   <li>Head: white, mottled or black (only seen on a white sheep). Spotting shows white patches on a coloured sheep only.</li>
 * </ul>
 *
 * @param horns 0 polled, 1 scurs, 2 horned
 * @param coat  0 wool, 1 part hair (sheds part of its coat), 2 hair (sheds)
 */
public record SheepLook(Colour colour, int head, boolean spotted, int white, int horns, int coat, int crimp, int seed) {
	public enum Colour { WHITE, BLACK, BROWN }

	/** Natural wool colours for fleece and yarn. Black wool greys with age in many sheep (Sponenberg). */
	public enum Wool { WHITE, BLACK, BROWN, GREY }

	/** Age at which a black fleece is taken off as grey *(proposed)*. */
	public static final double BLACK_GREYS_AT_DAYS = 730;

	public static SheepLook of(SheepGenes g, Sex sex) {
		Colour c;
		if (g.expressed(SheepLocus.EXTENSION) == 1) {
			c = Colour.BLACK;
		} else if (g.expressed(SheepLocus.AGOUTI) == 0) {
			c = Colour.WHITE;
		} else {
			c = g.expressed(SheepLocus.BROWN) == 1 ? Colour.BROWN : Colour.BLACK;
		}
		int[] h = g.pair(SheepLocus.HORNS);
		int horns = h[0] == 1 && h[1] == 1 ? 2 : h[0] + h[1] == 1 && sex == Sex.MALE ? 1 : 0;
		boolean spotted = c != Colour.WHITE && g.expressed(SheepLocus.SPOTTING) == 1;
		return new SheepLook(c, g.expressed(SheepLocus.HEAD), spotted, g.expressed(SheepLocus.WHITE), horns, g.expressed(SheepLocus.COAT),
				g.expressed(SheepLocus.CRIMP), java.util.Arrays.hashCode(g.save()));
	}

	/** The colour of the fleece shorn now: white, black, brown, or grey for a spotted coloured sheep or an old black one. */
	public Wool wool(double ageRealDays) {
		return switch (colour) {
			case WHITE -> Wool.WHITE;
			case BROWN -> spotted ? Wool.GREY : Wool.BROWN;
			case BLACK -> spotted || ageRealDays >= BLACK_GREYS_AT_DAYS ? Wool.GREY : Wool.BLACK;
		};
	}

	/** Sheds instead of growing a shearable fleece (full hair coat). */
	public boolean sheds() {
		return coat == 2;
	}

	/** Share of a full fleece that is kept for shearing: all of it for wool, half for a part-hair cross *(proposed)*, none for hair. */
	public double fleeceShare() {
		return coat == 0 ? 1.0 : coat == 1 ? 0.5 : 0.0;
	}

	public String describe() {
		StringBuilder b = new StringBuilder(colour.name().toLowerCase(Locale.ROOT));
		if (colour == Colour.WHITE && head > 0) {
			b.append(head == 2 ? ", black head" : ", mottled head");
		}
		if (spotted) {
			b.append(", spotted");
		}
		b.append(horns == 2 ? ", horned" : horns == 1 ? ", scurs" : ", polled");
		if (coat > 0) {
			b.append(coat == 2 ? ", hair coat" : ", part hair");
		}
		return b.toString();
	}

	/** Packs the look into a long for syncing to clients: colour 2 bits, head 2, spotted 1, white 10, horns 2, coat 2, crimp 2, seed 32. */
	public long pack() {
		long v = colour.ordinal();
		v |= (long) head << 2;
		v |= (spotted ? 1L : 0L) << 4;
		v |= (long) (white & 0x3FF) << 5;
		v |= (long) horns << 15;
		v |= (long) coat << 17;
		v |= (long) crimp << 19;
		v |= (seed & 0xFFFFFFFFL) << 21;
		return v;
	}

	public static SheepLook unpack(long v) {
		return new SheepLook(Colour.values()[(int) (v & 3)], (int) (v >> 2 & 3), (v >> 4 & 1) == 1, (int) (v >> 5 & 0x3FF), (int) (v >> 15 & 3),
				(int) (v >> 17 & 3), (int) (v >> 19 & 3), (int) (v >> 21 & 0xFFFFFFFFL));
	}
}
