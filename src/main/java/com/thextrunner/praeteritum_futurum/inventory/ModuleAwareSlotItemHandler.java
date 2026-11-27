package com.thextrunner.praeteritum_futurum.inventory;

import com.thextrunner.praeteritum_futurum.api.module.IItemHandlerModule;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * GUI slot for inventory items in the machine config screen.
 * <p>
 * Extends SlotItemHandler to check whether the corresponding grid position
 * has an Inventory module or an IItemHandlerModule installed. Only those slots
 * allow placing and picking up items, preventing interaction with empty grid positions.
 */
public class ModuleAwareSlotItemHandler extends SlotItemHandler {
    /** The machine framework block entity this slot belongs to. */
    private final MachineFrameworkBlockEntity blockEntity;

    /** The slot index in the machine's grid. */
    private final int slotIndex;

    /**
     * Creates a module-aware inventory slot.
     *
     * @param blockEntity The machine framework block entity
     * @param index The slot index in the inventory handler
     * @param xPosition The X position in the GUI
     * @param yPosition The Y position in the GUI
     */
    public ModuleAwareSlotItemHandler(MachineFrameworkBlockEntity blockEntity, int index, int xPosition, int yPosition) {
        super(blockEntity.getInventoryHandler(), index, xPosition, yPosition);
        this.blockEntity = blockEntity;
        this.slotIndex = index;
    }

    /**
     * Checks if an item can be placed in this slot.
     * Only allows placement if an Inventory module or IItemHandlerModule is installed
     * at the corresponding grid position.
     */
    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        boolean allowed = blockEntity.hasInventoryModule(slotIndex);
        // Also allow placing into slots with modules that expose their own item handler (e.g., Jukebox Module)
        if (!allowed && blockEntity.hasItemHandlerModule(slotIndex)) {
            allowed = true;
        }
        boolean superValid = super.mayPlace(stack);
        return allowed && superValid;
    }

    /**
     * Checks if an item can be picked up from this slot.
     * Only allows pickup from slots with an Inventory module or IItemHandlerModule.
     */
    @Override
    public boolean mayPickup(@NotNull Player player) {
        // Allow pickup from Inventory modules and any module that exposes its own item handler
        return blockEntity.hasInventoryModule(slotIndex) || blockEntity.hasItemHandlerModule(slotIndex);
    }
}