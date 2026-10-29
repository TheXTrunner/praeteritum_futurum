package com.thextrunner.praeteritum_futurum.inventory;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ReadOnlySlotItemHandler extends SlotItemHandler {
    public ReadOnlySlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return false;
    }
}