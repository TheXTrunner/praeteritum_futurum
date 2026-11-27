package com.thextrunner.praeteritum_futurum.client;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Utility class for rendering module overlays in GUI screens.
 * <p>
 * Handles the visual representation of module slots in machine config screens:
 * empty slots show a hatch lid placeholder, while installed modules delegate
 * their overlay rendering to their respective item classes.
 */
public class ModuleScreenRenderer {

    /**
     * Renders the overlay for a module slot in the machine config screen.
     * <p>
     * If the slot is empty, renders a hatch lid placeholder. If a module is
     * installed, delegates to the module's {@link BaseModuleItem#renderModuleOverlay}
     * method for custom rendering (progress bars, status indicators, etc.).
     *
     * @param graphics The GUI graphics context
     * @param slot The inventory slot being rendered
     * @param moduleStack The module item stack in the slot (may be empty)
     * @param blockEntity The machine framework block entity
     */
    public static void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack moduleStack, MachineFrameworkBlockEntity blockEntity) {
        if (moduleStack.isEmpty()) {
            renderHatchLid(graphics, slot.x, slot.y);
        } else if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
            baseModule.renderModuleOverlay(graphics, slot, moduleStack, blockEntity);
        }
    }

    /**
     * Renders a hatch lid placeholder for empty module slots.
     * <p>
     * Creates a subtle inset frame effect with light highlights on the top/left
     * edges and dark shadows on the bottom/right edges, giving the appearance
     * of a raised lid covering an empty slot.
     *
     * @param graphics The GUI graphics context
     * @param x The X coordinate of the slot
     * @param y The Y coordinate of the slot
     */
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