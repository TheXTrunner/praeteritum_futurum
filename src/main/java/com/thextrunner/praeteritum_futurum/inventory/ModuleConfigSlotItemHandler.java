package com.thextrunner.praeteritum_futurum.inventory;

import com.thextrunner.praeteritum_futurum.ModTags;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * GUI slot for module configuration in the machine config screen.
 * <p>
 * Extends SlotItemHandler to add validation: only machine modules can be placed,
 * and modules cannot be picked up if their corresponding inventory slot contains items.
 * Also delegates to the module's mayPickupModule method for additional checks.
 */
public class ModuleConfigSlotItemHandler extends SlotItemHandler {
    /** The machine framework block entity this slot belongs to. */
    private final MachineFrameworkBlockEntity blockEntity;

    /** The slot index in the machine's grid. */
    private final int slotIndex;

    /**
     * Creates a module config slot.
     *
     * @param blockEntity The machine framework block entity
     * @param index The slot index in the module handler
     * @param xPosition The X position in the GUI
     * @param yPosition The Y position in the GUI
     */
    public ModuleConfigSlotItemHandler(MachineFrameworkBlockEntity blockEntity, int index, int xPosition, int yPosition) {
        super(blockEntity.getModuleHandler(), index, xPosition, yPosition);
        this.blockEntity = blockEntity;
        this.slotIndex = index;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(ModTags.Items.MACHINE_MODULES) && super.mayPlace(stack);
    }

    /**
     * Checks if the module can be picked up from this slot.
     * Prevents pickup if the corresponding inventory slot contains items
     * or if the module itself disallows pickup.
     */
    @Override
    public boolean mayPickup(@NotNull Player player) {
        // Check if the inventory slot is empty
        if (!blockEntity.getInventoryHandler().getStackInSlot(slotIndex).isEmpty()) {
            return false;
        }
        // Check if the module itself allows being picked up
        ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slotIndex);
        if (!moduleStack.isEmpty() && moduleStack.getItem() instanceof BaseModuleItem baseModule) {
            return baseModule.mayPickupModule(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity, slotIndex, moduleStack);
        }
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}