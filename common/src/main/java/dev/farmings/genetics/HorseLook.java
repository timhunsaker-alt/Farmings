package dev.farmings.genetics;

/**
 * What a horse shows (docs/animals/horses.md, "Coat colour genes"): the base colour from Extension and Agouti (e/e chestnut; E with A bay;
 * E with a/a black), cream dilution by copies, dun, roan, sabino white by copies, and grey by age; plus face and leg markings from a seed
 * (drawn only).
 */
public record HorseLook(Base base, int cream, boolean dun, boolean roan, int sabino, int grey, int face, boolean socks, int seed) {
	public enum Base { CHESTNUT, BAY, BLACK }

	/** A grey is born its base colour and is grey by 6 years *(proposed pace)*: stage 1 from 1 year, 2 from 3, 3 (white) from 6. */
	public static int greyStage(boolean grey, double ageYears) {
		if (!grey || ageYears < 1) {
			return 0;
		}
		return ageYears < 3 ? 1 : ageYears < 6 ? 2 : 3;
	}

	public static HorseLook of(HorseGenes g, double ageYears) {
		int seed = java.util.Arrays.hashCode(g.save());
		Base base = !g.has(HorseLocus.E) ? Base.CHESTNUT : g.has(HorseLocus.A) ? Base.BAY : Base.BLACK;
		int face = Math.floorMod(seed, 3);
		boolean socks = Math.floorMod(seed >> 3, 4) == 0;
		return new HorseLook(base, g.count(HorseLocus.CR), g.has(HorseLocus.D), g.has(HorseLocus.RN), g.count(HorseLocus.SB1),
				greyStage(g.has(HorseLocus.G), ageYears), face, socks, seed);
	}

	/** The colour's name, as horsemen say it. */
	public String colourName() {
		String c = switch (base) {
			case CHESTNUT -> cream == 0 ? "chestnut" : cream == 1 ? "palomino" : "cremello";
			case BAY -> cream == 0 ? "bay" : cream == 1 ? "buckskin" : "perlino";
			case BLACK -> cream == 0 ? "black" : cream == 1 ? "smoky black" : "smoky cream";
		};
		if (dun) {
			c = switch (c) {
				case "chestnut" -> "red dun";
				case "bay" -> "dun";
				case "black" -> "grullo";
				default -> c + " dun";
			};
		}
		return c;
	}

	public String describe() {
		StringBuilder b = new StringBuilder();
		b.append(grey == 3 ? "grey (born " + colourName() + ")" : grey > 0 ? "greying " + colourName() : colourName());
		if (roan) {
			b.append(" roan");
		}
		if (sabino > 0) {
			b.append(sabino == 2 ? ", sabino white" : ", sabino");
		}
		return b.toString();
	}

	/** The base texture's name: base, cream copies and dun, e.g. "bay_1_dun". */
	public String texture() {
		return base.name().toLowerCase(java.util.Locale.ROOT) + "_" + cream + (dun ? "_dun" : "");
	}

	public long pack() {
		long v = base.ordinal() | cream << 2 | (dun ? 1 : 0) << 4 | (roan ? 1 : 0) << 5 | sabino << 6 | grey << 8 | face << 10 | (socks ? 1 : 0) << 12;
		return v | (seed & 0xFFFFFFFFL) << 13;
	}

	public static HorseLook unpack(long v) {
		return new HorseLook(Base.values()[(int) (v & 3) % 3], (int) (v >> 2 & 3), (v >> 4 & 1) == 1, (v >> 5 & 1) == 1, (int) (v >> 6 & 3),
				(int) (v >> 8 & 3), (int) (v >> 10 & 3), (v >> 12 & 1) == 1, (int) (v >> 13 & 0xFFFFFFFFL));
	}
}
