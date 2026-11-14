package dev.farmings.neoforge.gametest;

import dev.farmings.piggery.HeatLampEntity;
import dev.farmings.piggery.PigBarnBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

/** Farmings loads on a real Wildcatter supply. */
final class PowerGameTests {
	private PowerGameTests() {
	}

	private static void wildcatter(GameTestHelper helper, int x, int z, String id) {
		helper.setBlock(new BlockPos(x, 2, z), BuiltInRegistries.BLOCK.getValue(dev.wildcatter.Wildcatter.id(id)));
	}

	/**
	 * A heat lamp on Wildcatter's campfire boiler and steam turbine (Wildcatter's own lamp scene): a resistive load, so its watts are its
	 * volts times its amps (power factor 1), it never reports a motor's inrush or stall, and it draws its 250 W rating at the voltage it gets.
	 */
	static void heatLampIsResistive(GameTestHelper helper) {
		for (int x = -1; x <= 6; x++) {
			for (int z = -1; z <= 2; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				for (int y = 1; y <= 3; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
		helper.setBlock(new BlockPos(0, 1, 0), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		wildcatter(helper, 0, 0, "boiler");
		wildcatter(helper, 1, 0, "steam_turbine");
		((dev.wildcatter.block.BoilerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(0, 2, 0)))).addWater(4000);
		for (int x = 2; x <= 4; x++) {
			wildcatter(helper, x, 0, "wool_wire_hot");
		}
		for (int x = 1; x <= 5; x++) {
			wildcatter(helper, x, 1, "wool_wire_neutral");
		}
		helper.setBlock(new BlockPos(5, 2, 0), PigBarnBlocks.HEAT_LAMP);
		HeatLampEntity lamp = (HeatLampEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(5, 2, 0)));
		boolean[] motorWords = {false};
		helper.succeedWhen(() -> {     // polled each tick until it passes, so every reading on the way is seen
			String reading = lamp.inspect().getString();
			if (reading.contains("inrush") || reading.contains("STALLED") || reading.contains("motor")) {
				motorWords[0] = true;
			}
			helper.assertTrue(!motorWords[0], Component.literal("never a motor's inrush or stall: " + reading));
			helper.assertTrue(lamp.powered(), Component.literal("the lamp heats on the turbine's supply: " + reading));
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("heating, (\\d+) V, ([\\d.]+) A, (\\d+) W").matcher(reading);
			helper.assertTrue(m.find(), Component.literal("a heater's reading: " + reading));
			double v = Double.parseDouble(m.group(1));
			double a = Double.parseDouble(m.group(2));
			double w = Double.parseDouble(m.group(3));
			helper.assertTrue(Math.abs(w - v * a) <= Math.max(3, 0.05 * w), Component.literal("power factor 1, W = V x A: " + reading));
			double expected = HeatLampEntity.WATTS * Math.pow(v / dev.wildcatter.energy.Voltage.LOW.nominal(), 2);
			helper.assertTrue(Math.abs(w - expected) <= 0.1 * expected, Component.literal("P = V^2 / R from its 250 W rating (" + Math.round(expected) + " W): " + reading));
		});
	}
}
