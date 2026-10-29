package com.thextrunner.praeteritum_futurum.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface ICustomScreenProvider {
    <T extends AbstractContainerMenu> AbstractContainerScreen<?> createScreen(T menu, Inventory playerInventory, Component title);
}