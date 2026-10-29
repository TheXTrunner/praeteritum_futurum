package com.thextrunner.praeteritum_futurum.client;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = PraeteritumFuturumMod.MODID, value = Dist.CLIENT)
public class ClientModSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Fully decoupled: modules handle their own rendering and ticks natively. yay!
        });
    }
}