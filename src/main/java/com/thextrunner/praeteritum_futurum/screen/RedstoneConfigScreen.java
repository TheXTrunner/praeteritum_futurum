package com.thextrunner.praeteritum_futurum.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.thextrunner.praeteritum_futurum.api.module.RedstoneCircuit;
import com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Custom screen for the Redstone Module circuit editor.
 * Extends GenericModuleConfigScreen to inherit the standard back button,
 * filter mode toggle, and background rendering.
 * Uses a double-chest layout for the 54-slot (9×6) circuit grid.
 * Adds visual indicators for:
 * - Component rotation (direction text: N, E, S, W)
 * - Wall attachments (colored border overlay on the slot edge)
 *
 * Third-party modules can similarly extend GenericModuleConfigScreen
 * to add their own custom UI overlays.
 */
public class RedstoneConfigScreen extends GenericModuleConfigScreen {

    // Vanilla double chest texture (176x166) — used for 54-slot inventories
    private static final ResourceLocation DOUBLE_CHEST_GUI_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    // Colors for wall attachment indicators (semi-transparent)
    private static final int WALL_ATTACHMENT_UP_COLOR = 0x8000FF00;     // Green for UP
    private static final int WALL_ATTACHMENT_DOWN_COLOR = 0x80FF0000;   // Red for DOWN
    private static final int WALL_ATTACHMENT_LEFT_COLOR = 0x800000FF;   // Blue for LEFT
    private static final int WALL_ATTACHMENT_RIGHT_COLOR = 0x80FFFF00;  // Yellow for RIGHT

    // Rotation text color
    private static final int ROTATION_TEXT_COLOR = 0xFFFFA500; // Orange

    public RedstoneConfigScreen(GenericModuleConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // Shulker box layout: 176x166 for the grid area, inventory below
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Render the full double chest texture (generic_54.png is 176x222, covers grid + inventory)
        guiGraphics.blit(DOUBLE_CHEST_GUI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render redstone-specific overlays ON TOP of everything
        if (menu.getModuleStack().getItem() instanceof RedstoneModuleItem) {
            renderRedstoneOverlays(guiGraphics);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * Renders visual indicators for rotation and wall attachments on the 6×9 grid.
     */
    private void renderRedstoneOverlays(GuiGraphics guiGraphics) {
        ItemStack moduleStack = menu.getModuleStack();
        if (moduleStack.isEmpty()) return;

        RedstoneCircuit circuit = RedstoneCircuit.fromStack(moduleStack, minecraft.player.registryAccess());

        int gridStartX = leftPos + 8;
        int gridStartY = topPos + 18;

        for (int slotIndex = 0; slotIndex < RedstoneCircuit.GRID_SIZE; slotIndex++) {
            int col = slotIndex % RedstoneCircuit.GRID_WIDTH;
            int row = slotIndex / RedstoneCircuit.GRID_WIDTH;

            int x = gridStartX + col * 18;
            int y = gridStartY + row * 18;

            // Render wall attachment indicator
            int wallAtt = circuit.getWallAttachment(slotIndex);
            if (wallAtt >= 0 && wallAtt < 4) {
                renderWallAttachment(guiGraphics, x, y, wallAtt);
            }

            // Render rotation indicator for non-empty slots
            if (!circuit.isEmpty(slotIndex)) {
                int rotation = circuit.getRotation(slotIndex);
                renderRotationIndicator(guiGraphics, x, y, rotation);
            }
        }
    }

    /**
     * Renders a colored border on the edge of the slot to indicate wall attachment direction.
     */
    private void renderWallAttachment(GuiGraphics guiGraphics, int x, int y, int wallAttachmentIndex) {
        int color;
        switch (wallAttachmentIndex) {
            case 0 -> color = WALL_ATTACHMENT_UP_COLOR;
            case 1 -> color = WALL_ATTACHMENT_DOWN_COLOR;
            case 2 -> color = WALL_ATTACHMENT_LEFT_COLOR;
            case 3 -> color = WALL_ATTACHMENT_RIGHT_COLOR;
            default -> color = WALL_ATTACHMENT_UP_COLOR;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int thickness = 3;
        switch (wallAttachmentIndex) {
            case 0 -> guiGraphics.fill(x, y, x + 18, y + thickness, color);
            case 1 -> guiGraphics.fill(x, y + 18 - thickness, x + 18, y + 18, color);
            case 2 -> guiGraphics.fill(x, y, x + thickness, y + 18, color);
            case 3 -> guiGraphics.fill(x + 18 - thickness, y, x + 18, y + 18, color);
        }

        RenderSystem.disableBlend();
    }

    /**
     * Renders a small text indicator showing the component's rotation direction.
     * Uses single letters: N, E, S, W in the bottom-right corner of the slot.
     */
    private void renderRotationIndicator(GuiGraphics guiGraphics, int x, int y, int rotation) {
        String directionText;
        switch (rotation % 4) {
            case 0 -> directionText = "N";
            case 1 -> directionText = "E";
            case 2 -> directionText = "S";
            case 3 -> directionText = "W";
            default -> directionText = "?";
        }

        // Draw a small background box for the text
        int textX = x + 12;
        int textY = y + 10;
        guiGraphics.fill(textX, textY, textX + 6, textY + 6, 0xAA000000);

        // Draw the direction text
        guiGraphics.drawString(this.font, directionText, textX + 1, textY + 1, ROTATION_TEXT_COLOR, false);
    }
}
