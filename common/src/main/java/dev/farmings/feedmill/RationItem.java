package dev.farmings.feedmill;

import dev.farmings.feed.Ration;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A 25 kg mixed ration from the TMR mixer, carrying its analysis (feed-mill.md). Rations of the same analysis stack. */
public class RationItem extends Item {
	public static final double KG = 25;

	public RationItem(Properties props) {
		super(props);
	}

	public static ItemStack of(Ration.Analysis a, double dm, int count) {
		ItemStack s = new ItemStack(FeedItems.RATION, count);
		Ration.Analysis r = new Ration.Analysis(round(a.cp()), round(a.ndf()), round(a.tdn()), round(a.forage()), round(a.ca()), Math.round(a.premix() * 1000) / 1000.0);
		FeedItems.putAnalysis(s, r, Math.round(dm * 1000) / 1000.0);
		return s;
	}

	private static double round(double v) {
		return Math.round(v * 10) / 10.0;
	}

	static FeedItems.Feed feed(ItemStack s) {
		FeedItems.Feed f = FeedItems.readRation(s);
		return f == null ? new FeedItems.Feed(KG, 0.5, Ration.Analysis.BALANCED) : f;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		FeedItems.Feed f = FeedItems.readRation(stack);
		if (f == null) {
			return;
		}
		Ration.Analysis a = f.analysis();
		out.accept(Component.literal(String.format(Locale.ROOT, "%.0f%% DM; CP %.1f%%, NDF %.1f%%, TDN %.1f%%, Ca %.1f%%, forage %.0f%%", f.dm() * 100,
				a.cp(), a.ndf(), a.tdn(), a.ca(), a.forage() * 100)));
		out.accept(Component.literal(String.format(Locale.ROOT, "Milk x%.2f, growth x%.2f, lambs x%.2f, eggs x%.2f", Ration.milkFactor(a),
				Ration.growthFactor(a), Ration.lambFactor(a), Ration.eggFactor(a))));
	}
}
