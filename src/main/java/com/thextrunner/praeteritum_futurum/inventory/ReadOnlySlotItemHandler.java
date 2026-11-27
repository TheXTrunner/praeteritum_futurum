package com.thextrunner.praeteritum_futurum.inventory;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Read-only GUI slot — items can be seen but not picked up by the player.
 * <p>
 * Used in the machine info screen to display the machine's current state
 * without allowing the player to modify it.
 */
public class ReadOnlySlotItemHandler extends SlotItemHandler {
    /**
     * Creates a read-only slot.
     *
     * @param itemHandler The item handler backing this slot
     * @param index The slot index in the handler
     * @param xPosition The X position in the GUI
     * @param yPosition The Y position in the GUI
     */
    public ReadOnlySlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return false;
    }
}