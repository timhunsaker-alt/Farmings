package dev.farmings.genetics;

import java.util.Map;
import java.util.TreeMap;

/**
 * What an animal looks like, resolved from its coat genes and breed composition by FarmChain's cross-rendering order (BREED-SPEC §7.4):
 * body family from the breed shares (a second family blended when it gives at least 25%), then pigment, dilution, pattern, white, face,
 * horns and the zebu features. The pattern seed only picks among the exact patch shapes the phenotype allows; it never changes the
 * phenotype (§7.4 step 5). Packed into one long so it can be sent to the client with the entity.
 */
public record VisualPhenotype(Coat coat, int dilution, int patternMask, int white, int face, boolean horned, int hump, int dewlap, int ears,
		BodyFamily body, BodyFamily secondary, int secondaryPct, int roan, boolean brindle) {

	/** The look without roan or brindle (as before 2026-11-11). */
	public VisualPhenotype(Coat coat, int dilution, int patternMask, int white, int face, boolean horned, int hump, int dewlap, int ears,
			BodyFamily body, BodyFamily secondary, int secondaryPct) {
		this(coat, dilution, patternMask, white, face, horned, hump, dewlap, ears, body, secondary, secondaryPct, 0, false);
	}

	public enum Coat { BLACK, RED, WILD }

	public enum BodyFamily {
		DAIRY, BEEF, ZEBU;

		public static BodyFamily of(String breedCode) {
			return switch (breedCode) {
				case "ANG", "WAG" -> BEEF;
				case "GYR" -> ZEBU;
				default -> DAIRY;
			};
		}
	}

	/** A second body family is blended when it gives at least this share (BREED-SPEC §7.4). */
	public static final int BLEND_THRESHOLD_PCT = 25;

	public static VisualPhenotype of(VisualGenome g, Map<String, Integer> compositionBp) {
		int ext = g.expressed(VisualLocus.EXTENSION);
		Coat coat = ext <= 1 ? Coat.BLACK : ext == 2 ? Coat.WILD : Coat.RED;
		// body families by summed breed share
		Map<BodyFamily, Integer> shares = new TreeMap<>();
		int total = 0;
		for (Map.Entry<String, Integer> e : compositionBp.entrySet()) {
			shares.merge(BodyFamily.of(e.getKey()), e.getValue(), Integer::sum);
			total += e.getValue();
		}
		java.util.List<Map.Entry<BodyFamily, Integer>> ranked = new java.util.ArrayList<>(shares.entrySet());
		ranked.sort((x, y) -> y.getValue() != x.getValue().intValue() ? Integer.compare(y.getValue(), x.getValue()) : x.getKey().compareTo(y.getKey()));
		BodyFamily first = ranked.isEmpty() ? BodyFamily.DAIRY : ranked.get(0).getKey();
		BodyFamily second = ranked.size() < 2 ? null : ranked.get(1).getKey();
		int secondBp = ranked.size() < 2 ? 0 : ranked.get(1).getValue();
		int pct = total <= 0 || second == null ? 0 : (int) Math.round(100.0 * secondBp / total);
		if (pct < BLEND_THRESHOLD_PCT) {
			second = null;
			pct = 0;
		}
		// shown dilution: the breed-shade modifier plus PMEL's strength, at most strong (docs/GENETICS.md, loose ends)
		int dilution = Math.min(3, g.expressed(VisualLocus.DILUTION) + g.sum(VisualLocus.PMEL));
		// brindle shows only on wild-type E+; ED and e/e hide it (AHCA)
		boolean brindle = coat == Coat.WILD && g.expressed(VisualLocus.AGOUTI) == 1;
		return new VisualPhenotype(coat, dilution, g.expressed(VisualLocus.PATTERN), g.expressed(VisualLocus.WHITE),
				g.expressed(VisualLocus.FACE), g.expressed(VisualLocus.HORNS) == 1, g.expressed(VisualLocus.HUMP), g.expressed(VisualLocus.DEWLAP),
				g.expressed(VisualLocus.EARS), first, second, pct, g.sum(VisualLocus.ROAN), brindle);
	}

	public boolean shows(int pattern) {
		return (patternMask & (1 << pattern)) != 0;
	}

	public boolean solid() {
		return patternMask == 1;
	}

	/**
	 * The base coat colour as 0xRRGGBB *(proposed tints, drawn to match the breeds)*: black, red, or wild type (reddish brown), lightened
	 * towards a cream by a fifth per dilution step (Jersey fawn and Brown Swiss grey-brown come from this).
	 */
	public int coatRgb() {
		int[] base = switch (coat) {
			case BLACK -> new int[] {30, 26, 26};
			case RED -> new int[] {150, 64, 34};
			case WILD -> new int[] {124, 78, 48};
		};
		int[] cream = {205, 188, 165};
		double t = dilution * 0.22;
		int r = (int) Math.round(base[0] + (cream[0] - base[0]) * t);
		int gr = (int) Math.round(base[1] + (cream[1] - base[1]) * t);
		int b = (int) Math.round(base[2] + (cream[2] - base[2]) * t);
		return (r << 16) | (gr << 8) | b;
	}

	public String describe() {
		StringBuilder s = new StringBuilder();
		if (roan == 2) {
			s.append("white (roan R/R)");
			s.append(horned ? ", horned" : ", polled");
			return s.toString();
		}
		if (brindle) {
			s.append("brindle ");
		}
		s.append(switch (coat) {
			case BLACK -> dilution >= 2 ? "grey-brown" : "black";
			case RED -> dilution >= 2 ? "light red" : "red";
			case WILD -> dilution >= 1 ? "fawn" : "brown";
		});
		if (!solid()) {
			String[] names = {"", "color-sided", "speckled", "patched"};
			for (int p = 1; p <= 3; p++) {
				if (shows(p)) {
					s.append(" ").append(names[p]);
				}
			}
			s.append(" with white");
		}
		if (roan == 1) {
			s.append(" roan");
		}
		s.append(horned ? ", horned" : ", polled");
		if (hump > 0) {
			s.append(", hump ").append(hump);
		}
		return s.toString();
	}

	// ---- packing: 41 bits ----

	public long pack() {
		long v = 0;
		v |= coat.ordinal();
		v |= (long) dilution << 2;
		v |= (long) (patternMask & 0xF) << 4;
		v |= (long) (white & 0x3FF) << 8;
		v |= (long) face << 18;
		v |= (horned ? 1L : 0L) << 20;
		v |= (long) hump << 21;
		v |= (long) dewlap << 23;
		v |= (long) ears << 25;
		v |= (long) body.ordinal() << 27;
		v |= (long) (secondary == null ? 3 : secondary.ordinal()) << 29;
		v |= (long) (secondaryPct & 0x7F) << 31;
		v |= (long) (roan & 3) << 38;
		v |= (brindle ? 1L : 0L) << 40;
		return v;
	}

	public static VisualPhenotype unpack(long v) {
		int sec = (int) (v >> 29 & 3);
		return new VisualPhenotype(Coat.values()[(int) (v & 3) % 3], (int) (v >> 2 & 3), (int) (v >> 4 & 0xF), (int) (v >> 8 & 0x3FF),
				(int) (v >> 18 & 3), (v >> 20 & 1) == 1, (int) (v >> 21 & 3), (int) (v >> 23 & 3), (int) (v >> 25 & 3),
				BodyFamily.values()[(int) (v >> 27 & 3) % 3], sec == 3 ? null : BodyFamily.values()[sec], (int) (v >> 31 & 0x7F),
				(int) (v >> 38 & 3), (v >> 40 & 1) == 1);
	}

	/** The seed that picks exact patch shapes: from the genome hash, so an animal's markings never change. */
	public static int patternSeed(Genome genome) {
		return (int) Long.parseLong(genome.hash().substring(0, 8), 16);
	}
}
