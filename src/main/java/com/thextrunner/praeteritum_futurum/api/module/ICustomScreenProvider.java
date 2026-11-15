package com.thextrunner.praeteritum_futurum.api.module;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Provides custom graphical user interface screens for modules that require specialized client rendering.
 */
public interface ICustomScreenProvider {
    <T extends AbstractContainerMenu> AbstractContainerScreen<?> createScreen(T menu, Inventory playerInventory, Component title);
}