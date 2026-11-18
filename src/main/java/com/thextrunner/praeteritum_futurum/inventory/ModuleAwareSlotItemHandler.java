package com.thextrunner.praeteritum_futurum.inventory;

import com.thextrunner.praeteritum_futurum.api.module.IItemHandlerModule;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ModuleAwareSlotItemHandler extends SlotItemHandler {
    private final MachineFrameworkBlockEntity blockEntity;
    private final int slotIndex;

    public ModuleAwareSlotItemHandler(MachineFrameworkBlockEntity blockEntity, int index, int xPosition, int yPosition) {
        super(blockEntity.getInventoryHandler(), index, xPosition, yPosition);
        this.blockEntity = blockEntity;
        this.slotIndex = index;
    }

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

    @Override
    public boolean mayPickup(@NotNull Player player) {
        // Allow pickup from Inventory modules and any module that exposes its own item handler
        return blockEntity.hasInventoryModule(slotIndex) || blockEntity.hasItemHandlerModule(slotIndex);
    }
}