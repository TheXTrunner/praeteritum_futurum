package com.thextrunner.praeteritum_futurum.api.module;

import com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;

public interface IModuleMenuProvider {
    void addModuleSlots(Inventory playerInventory, ItemStack moduleStack, BlockPos blockPos, Consumer<Slot> slotAdder);
    void handleMenuClick(GenericModuleConfigMenu menu, int slotId, int button, ClickType clickType, Player player);
    ItemStack handleQuickMove(GenericModuleConfigMenu menu, Player player, int index);
}