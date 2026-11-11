package dev.farmings.neoforge;

import dev.farmings.Farmings;
import dev.farmings.FarmingsBlocks;
import dev.farmings.FarmingsEntities;
import dev.farmings.entity.CattleEntity;
import dev.wildcatter.Wildcatter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge entry point. Wildcatter is a hard dependency and a compile dependency. */
@Mod(Farmings.MOD_ID)
public final class FarmingsNeoForge {
	private static boolean registered;

	public FarmingsNeoForge(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> {
			if (!registered) {
				registered = true;
				Farmings.register(Wildcatter.MOD_ID);
				Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Farmings.id("farmings"), CreativeModeTab.builder()
						.title(FarmingsBlocks.tabTitle())
						.icon(FarmingsBlocks::tabIcon)
						.displayItems((parameters, output) -> FarmingsBlocks.creativeTab(output::accept))
						.build());
			}
		});
		modBus.addListener(RegisterEvent.class, event -> event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
				helper -> helper.register(Farmings.id("wild_spawns"), WildSpawnsModifier.CODEC)));
		modBus.addListener(EntityAttributeCreationEvent.class,
				event -> {
					event.put(FarmingsEntities.CATTLE, CattleEntity.createAttributes().build());
					event.put(FarmingsEntities.SHEEP, dev.farmings.entity.SheepEntity.createAttributes().build());
					for (var type : FarmingsEntities.FOWL) {
						event.put(type, dev.farmings.entity.FowlEntity.createAttributes(FarmingsEntities.speciesOf(type)).build());
					}
				});
		// new crop seeds from grass a player breaks (docs/crops.md, "Seeds")
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.level.block.BreakBlockEvent.class, event -> {
			if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level && event.getPlayer() != null && !event.isCanceled()) {
				dev.farmings.crops.GrassSeeds.onBreak(level, event.getPlayer(), event.getPos(), event.getState());
			}
		});
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			dev.farmings.neoforge.client.FarmingsNeoForgeClient.init(modBus);
		}
	}
}
