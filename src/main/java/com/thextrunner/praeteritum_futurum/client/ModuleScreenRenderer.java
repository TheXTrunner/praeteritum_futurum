package com.thextrunner.praeteritum_futurum.client;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ModuleScreenRenderer {

    public static void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack moduleStack, MachineFrameworkBlockEntity blockEntity, Font font) {
        if (moduleStack.isEmpty()) {
            renderHatchLid(graphics, slot.x, slot.y);
        } else if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
            baseModule.renderModuleOverlay(graphics, slot, moduleStack, blockEntity);
        }
    }

    private static void renderHatchLid(GuiGraphics graphics, int x, int y) {
        // 1. Flat base filled with the exact UI panel background color
        graphics.fill(x, y, x + 16, y + 16, 0xFFC6C6C6);

        // 2. Subtle outer border for the inverted inset frame (raised lid illusion)
        // Top and Left edges (Light highlights)
        graphics.fill(x, y, x + 16, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + 16, 0xFFFFFFFF);

        // Bottom and Right edges (Dark shadows)
        graphics.fill(x, y + 15, x + 16, y + 16, 0xFF555555);
        graphics.fill(x + 15, y, x + 16, y + 16, 0xFF555555);
    }
}