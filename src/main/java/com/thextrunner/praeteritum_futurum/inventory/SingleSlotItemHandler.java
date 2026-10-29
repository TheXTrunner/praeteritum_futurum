package com.thextrunner.praeteritum_futurum.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public class SingleSlotItemHandler implements IItemHandlerModifiable {
    private final IItemHandlerModifiable parent;
    private final int slot;

    public SingleSlotItemHandler(IItemHandlerModifiable parent, int slot) {
        this.parent = parent;
        this.slot = slot;
    }

    public SingleSlotItemHandler(net.neoforged.neoforge.items.IItemHandler parent, int slot) {
        // Fallback if parent is regular IItemHandler, though inventoryHandler is Modifiable
        if (parent instanceof IItemHandlerModifiable modifiable) {
            this.parent = modifiable;
        } else {
            throw new IllegalArgumentException("Parent must be IItemHandlerModifiable");
        }
        this.slot = slot;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (slot == 0) {
            parent.setStackInSlot(this.slot, stack);
        }
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return slot == 0 ? parent.getStackInSlot(this.slot) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return slot == 0 ? parent.insertItem(this.slot, stack, simulate) : stack;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot == 0 ? parent.extractItem(this.slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == 0 ? parent.getSlotLimit(this.slot) : 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot == 0 && parent.isItemValid(this.slot, stack);
    }
}