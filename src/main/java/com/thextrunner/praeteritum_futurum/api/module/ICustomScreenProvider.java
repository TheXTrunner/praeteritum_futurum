package com.thextrunner.praeteritum_futurum.api.module;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Provides custom graphical user interface screens for modules that require specialized client rendering.
 * <p>
 * When a module implements this interface, the machine config screen will use the
 * returned screen instance instead of the default GenericModuleConfigScreen. This
 * allows modules like the Redstone Module to provide entirely custom UI layouts.
 * <p>
 * Implementing classes: RedstoneModuleItem.
 */
public interface ICustomScreenProvider {
    /**
     * Creates and returns the custom screen for this module's configuration.
     * <p>
     * Called on the client side when the player opens the module's config screen.
     * The implementation should create and return a new Screen instance that handles
     * the module's specific UI needs.
     *
     * @param menu            The container menu for this screen
     * @param playerInventory The player's inventory
     * @param title           The screen title component
     * @return The custom screen instance
     */
    <T extends AbstractContainerMenu> AbstractContainerScreen<?> createScreen(T menu, Inventory playerInventory, Component title);
}