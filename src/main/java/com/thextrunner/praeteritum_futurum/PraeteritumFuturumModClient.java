package com.thextrunner.praeteritum_futurum;

import com.thextrunner.praeteritum_futurum.block.entity.ModBlockEntities;
import com.thextrunner.praeteritum_futurum.client.ClientModSetup;
import com.thextrunner.praeteritum_futurum.client.MachineFrameworkRenderer;
import com.thextrunner.praeteritum_futurum.screen.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-side entry point for Praeteritum Futurum.
 * <p>
 * The @Mod(dist = Dist.CLIENT) annotation tells NeoForge this class runs only on the client.
 * It handles client-only setup: block entity renderers, screen registration, and the config
 * screen factory. All other client logic (like item property overrides) lives in
 * ClientModSetup, which is wired up in the main mod class via modEventBus.addListener().
 * <p>
 * Note: The config spec itself is registered in the main mod class (PraeteritumFuturumMod).
 * Registering it here as well would cause a "config file conflict" crash since both
 * ModContainer instances would try to manage the same praeteritum_futurum-common.toml file.
 */
@Mod(value = PraeteritumFuturumMod.MODID, dist = Dist.CLIENT)
public class PraeteritumFuturumModClient {

    /**
     * Client mod constructor — called by NeoForge during client-side mod loading.
     * <p>
     * Registers the config screen button, block entity renderers, and GUI screen factories.
     */
    public PraeteritumFuturumModClient(ModContainer container, IEventBus modEventBus) {
        // Register the config screen factory so NeoForge shows a "Config..." button
        // in the mod list screen for this mod.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // Register custom block entity renderer for the Machine Framework
        modEventBus.addListener(this::registerBlockEntityRenderers);

        // Register GUI screen factories (links MenuTypes to their visual Screen classes)
        modEventBus.addListener(ModMenuTypes::registerScreens);

        // Item property overrides use client-only types, so keep their listener
        // registration inside the client-only mod entry point.
        modEventBus.addListener(ClientModSetup::onClientSetup);
    }

    /**
     * Registers custom renderers for block entities.
     * <p>
     * Called by NeoForge during the RegisterRenderers event. The Machine Framework
     * uses a custom renderer to draw the 3D model with emissive layers and module overlays.
     */
    private void registerBlockEntityRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.MACHINE_FRAMEWORK_BE.get(),
                MachineFrameworkRenderer::new
        );
    }
}
