package com.thextrunner.praeteritum_futurum.api.module;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Marker interface for modules that provide a configuration screen.
 * <p>
 * When a player right-clicks a module in the machine menu, the server checks if the
 * module implements this interface. If it does, {@link #openConfiguration} is called
 * to open the appropriate config screen.
 * <p>
 * Implementing classes: FilterModuleItem, IoModuleItem, ProcessingModuleItem (and subclasses).
 */
public interface IConfigurableModule {

    /**
     * Opens the configuration screen for this module.
     * <p>
     * Called on the server side when the player requests the module config screen.
     * The implementation typically opens a GenericModuleConfigMenu and sends the
     * corresponding screen to the client.
     *
     * @param player      The player opening the config screen
     * @param moduleStack The item stack representing the installed module
     * @param slotIndex   The module slot index in the machine
     * @param blockPos    The world position of the machine block
     */
    void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos);
}
