package com.thextrunner.praeteritum_futurum.api.module;

import com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;

/**
 * Interface for modules that provide custom container slots in their config screen.
 * <p>
 * When a module implements this interface, the GenericModuleConfigMenu will call
 * {@link #addModuleSlots} to add the module's custom slots (e.g., a filter item slot,
 * input/output slots). This allows each module type to define its own slot layout
 * without modifying the screen code.
 * <p>
 * Implementing classes: FilterModuleItem and RedstoneModuleItem.
 */
public interface IModuleMenuProvider {

    /**
     * Adds custom container slots for a module.
     * <p>
     * Called when the module config menu is created. Each call to slotAdder.accept()
     * adds one slot to the menu at the next available position.
     *
     * @param playerInventory The inventory of the player opening the menu
     * @param moduleStack     The stack representing the installed module
     * @param blockPos        The world position of the machine block, reserved for modules that require positional context or world data
     * @param slotAdder       A consumer function used to register new slots to the menu
     */
    void addModuleSlots(Inventory playerInventory, ItemStack moduleStack, BlockPos blockPos, Consumer<Slot> slotAdder);

    /**
     * Persists module-specific state from the menu when it closes.
     * <p>
     * Providers that save changes immediately can keep the default no-op implementation.
     *
     * @param menu the closing module configuration menu
     * @param registries registry access used to serialize item stacks
     */
    default void saveMenuData(GenericModuleConfigMenu menu, HolderLookup.Provider registries) {
    }

    /**
     * Handles click events on the module's custom slots.
     * <p>
     * Override this to implement custom click behavior, such as clearing a filter item
     * when right-clicked or cycling through options.
     *
     * @param menu     The open module configuration menu
     * @param slotId   The slot index that was clicked
     * @param button   The mouse button (0 = left, 1 = right)
     * @param clickType The type of click (PICKUP, QUICK_CRAFT, etc.)
     * @param player   The player who clicked
     */
    void handleMenuClick(GenericModuleConfigMenu menu, int slotId, int button, ClickType clickType, Player player);

    /**
     * Handles shift-clicking items within custom module screens.
     * Override this in custom modules to define container item transfers.
     *
     * @param menu   The open module configuration menu
     * @param player The player interacting with the menu
     * @param index  The raw slot index clicked
     * @return The remaining item stack after transfer, or ItemStack.EMPTY if transfer is unhandled
     */
    @SuppressWarnings("SameReturnValue")
    ItemStack handleQuickMove(GenericModuleConfigMenu menu, Player player, int index);
}