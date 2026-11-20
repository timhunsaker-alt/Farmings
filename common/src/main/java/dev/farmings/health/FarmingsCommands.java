package dev.farmings.health;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /farmings rule [name] [true|false]}: lists Farmings' world rules, or sets one (an operator's command, as Tailings'
 * {@code /tailings rule}). So a world saved before animal health can turn disease on.
 */
public final class FarmingsCommands {
	private FarmingsCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("farmings").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("rule")
						.executes(c -> list(c.getSource()))
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(c -> show(c.getSource(), StringArgumentType.getString(c, "name")))
								.then(Commands.argument("value", BoolArgumentType.bool())
										.executes(c -> set(c.getSource(), StringArgumentType.getString(c, "name"), BoolArgumentType.getBool(c, "value")))))));
	}

	private static int list(CommandSourceStack src) {
		FarmingsRules.all(src.getServer()).forEach((k, v) -> src.sendSuccess(() -> Component.literal(k + " = " + v + ": " + FarmingsRules.meaning(k)), false));
		return 1;
	}

	private static int show(CommandSourceStack src, String key) {
		if (!FarmingsRules.NEW_WORLD.containsKey(key)) {
			src.sendFailure(Component.literal("No Farmings rule " + key + "; the rules are " + FarmingsRules.NEW_WORLD.keySet()));
			return 0;
		}
		src.sendSuccess(() -> Component.literal(key + " = " + FarmingsRules.on(src.getServer(), key)), false);
		return 1;
	}

	private static int set(CommandSourceStack src, String key, boolean value) {
		if (!FarmingsRules.NEW_WORLD.containsKey(key)) {
			src.sendFailure(Component.literal("No Farmings rule " + key + "; the rules are " + FarmingsRules.NEW_WORLD.keySet()));
			return 0;
		}
		FarmingsRules.set(src.getServer(), key, value);
		src.sendSuccess(() -> Component.literal(key + " set to " + value), true);
		return 1;
	}
}
