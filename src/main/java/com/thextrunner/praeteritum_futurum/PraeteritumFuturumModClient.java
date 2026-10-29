package com.thextrunner.praeteritum_futurum;

import com.thextrunner.praeteritum_futurum.screen.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = PraeteritumFuturumMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = PraeteritumFuturumMod.MODID)
public class PraeteritumFuturumModClient {
    public PraeteritumFuturumModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        ModMenuTypes.registerScreens(event);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {// Provides access to client thread work queues and deferred setup registrations... it logs things in the thing logger
        PraeteritumFuturumMod.LOGGER.info("Past meets the future...");
        PraeteritumFuturumMod.LOGGER.info("YOUR NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}