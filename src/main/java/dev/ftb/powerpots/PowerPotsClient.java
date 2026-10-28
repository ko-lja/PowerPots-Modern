package dev.ftb.powerpots;

import dev.ftb.powerpots.pot.PowerPotRender;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PowerPots.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PowerPotsClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PowerPots.POWER_TILE_MK1.get(), PowerPotRender::new);
        event.registerBlockEntityRenderer(PowerPots.POWER_TILE_MK2.get(), PowerPotRender::new);
        event.registerBlockEntityRenderer(PowerPots.POWER_TILE_MK3.get(), PowerPotRender::new);
        event.registerBlockEntityRenderer(PowerPots.POWER_TILE_MK4.get(), PowerPotRender::new);
    }
}
