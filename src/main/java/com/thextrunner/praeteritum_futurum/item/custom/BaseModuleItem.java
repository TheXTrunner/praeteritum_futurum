package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;
import java.util.Map;

public class BaseModuleItem extends Item {
    public BaseModuleItem(Properties properties) {
        super(properties);
    }

    // Default overlay bounds cover the central slot. Subclasses expand this for their buttons.
    public boolean isInsideOverlay(double mouseX, double mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
    }

    public boolean opensInlineOverlay() {
        return false; // Default modules open separate container menus via IConfigurableModule
    }

    @SuppressWarnings("unused")
    public void handleCustomClick(double mouseX, double mouseY, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex, Minecraft minecraft) {
        // Default implementation does nothing
    }

    // Returns true if the click is on the module's close (X) button
    public boolean isCloseButton(double mouseX, double mouseY, int slotX, int slotY) {
        return false;
    }

    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {}

    public boolean shouldHideSlot(int configuringSlotIndex, int targetSlotIndex) {
        int row = configuringSlotIndex / MachineFrameworkBlockEntity.GRID_WIDTH;
        int col = configuringSlotIndex % MachineFrameworkBlockEntity.GRID_WIDTH;

        if (targetSlotIndex == configuringSlotIndex) return true;
        if (row > 0 && targetSlotIndex == configuringSlotIndex - MachineFrameworkBlockEntity.GRID_WIDTH) return true;
        if (row < MachineFrameworkBlockEntity.GRID_ROWS - 1 && targetSlotIndex == configuringSlotIndex + MachineFrameworkBlockEntity.GRID_WIDTH) return true;
        if (col > 0 && targetSlotIndex == configuringSlotIndex - 1) return true;
        if (col < MachineFrameworkBlockEntity.GRID_WIDTH - 1 && targetSlotIndex == configuringSlotIndex + 1) return true;
        return row > 0 && col > 0 && targetSlotIndex == configuringSlotIndex - MachineFrameworkBlockEntity.GRID_WIDTH - 1;
    }

    @SuppressWarnings("unused")
    public @Nullable IItemHandlerModifiable getItemHandlerForSide(
            Level level,
            BlockPos pos,
            MachineFrameworkBlockEntity blockEntity,
            int slotIndex,
            ItemStack stack,
            Direction side) {
        return null;
    }

    public CompoundTag getBlueprintData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        return customData.copyTag();
    }

    public void renderCustomOverlay(GuiGraphics graphics, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, Font font) {}

    public void tickModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
    }

    public Map<RelativeSide, IoModuleItem.FaceMode> getFaceModes(ItemStack stack) {
        return Map.of();
    }

    public void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos) {}

    /**
     * Returns the max stack limit for a specific inventory slot used by this module.
     * Override in subclasses to restrict slot capacity (e.g., ingredient slots limited to 1).
     *
     * @param blockEntity The parent machine framework
     * @param moduleSlotIndex The grid slot index where this module is placed
     * @param inventorySlotIndex The inventory slot index to check
     * @return The max stack limit for the slot, or 0 to use the default limit
     */
    public int getInventorySlotLimit(MachineFrameworkBlockEntity blockEntity, int moduleSlotIndex, int inventorySlotIndex) {
        return 0; // 0 means use default (no restriction)
    }
}