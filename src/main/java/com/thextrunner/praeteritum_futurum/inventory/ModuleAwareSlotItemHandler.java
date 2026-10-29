package com.thextrunner.praeteritum_futurum.inventory;

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
        boolean hasModule = blockEntity.hasInventoryModule(slotIndex);
        boolean superValid = super.mayPlace(stack);
        return hasModule && superValid;
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return blockEntity.hasInventoryModule(slotIndex);
    }
}