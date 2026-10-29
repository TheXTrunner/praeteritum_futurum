package com.thextrunner.praeteritum_futurum.inventory;

import com.thextrunner.praeteritum_futurum.ModTags;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ModuleConfigSlotItemHandler extends SlotItemHandler {
    private final MachineFrameworkBlockEntity blockEntity;
    private final int slotIndex;

    public ModuleConfigSlotItemHandler(MachineFrameworkBlockEntity blockEntity, int index, int xPosition, int yPosition) {
        super(blockEntity.getModuleHandler(), index, xPosition, yPosition);
        this.blockEntity = blockEntity;
        this.slotIndex = index;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(ModTags.Items.MACHINE_MODULES) && super.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return blockEntity.getInventoryHandler().getStackInSlot(slotIndex).isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}