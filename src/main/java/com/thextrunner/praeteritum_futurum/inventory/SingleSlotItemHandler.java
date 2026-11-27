package com.thextrunner.praeteritum_futurum.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps a single slot from a larger item handler as its own 1-slot handler.
 * <p>
 * Used by IO modules to transfer items to/from a specific inventory slot
 * without exposing the entire machine inventory. Maps slot index 0 to the
 * actual slot in the parent handler.
 */
public class SingleSlotItemHandler implements IItemHandlerModifiable {
    /** The parent item handler containing the actual slots. */
    private final IItemHandlerModifiable parent;

    /** The slot index in the parent handler that this wrapper exposes. */
    private final int slot;

    /**
     * Creates a single-slot wrapper around a modifiable item handler.
     *
     * @param parent The parent item handler
     * @param slot The slot index to expose
     */
    public SingleSlotItemHandler(IItemHandlerModifiable parent, int slot) {
        this.parent = parent;
        this.slot = slot;
    }

    /**
     * Creates a single-slot wrapper, validating that the parent is modifiable.
     * Throws IllegalArgumentException if the parent is not IItemHandlerModifiable.
     *
     * @param parent The parent item handler
     * @param slot The slot index to expose
     */
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