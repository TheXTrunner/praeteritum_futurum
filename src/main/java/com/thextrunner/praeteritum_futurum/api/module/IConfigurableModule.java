package com.thextrunner.praeteritum_futurum.api.module;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface IConfigurableModule {
    void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos);

    // Los módulos pueden proveer su pantalla, pero por defecto retorna null para overlays flotantes
    @Nullable
    default AbstractContainerScreen<?> createScreen(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        return null;
    }
}