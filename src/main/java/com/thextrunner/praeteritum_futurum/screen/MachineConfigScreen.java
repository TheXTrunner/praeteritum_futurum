package com.thextrunner.praeteritum_futurum.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.network.ServerboundToggleMachinePublicPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * GUI screen for configuring the Machine Framework.
 * <p>
 * Renders the 27-slot module grid with module overlays, inventory slots,
 * and the player's inventory. Handles mouse clicks for module configuration,
 * IO face toggling, and opening module-specific config screens.
 */
public class MachineConfigScreen extends AbstractContainerScreen<MachineConfigMenu> {
    /** GUI texture — uses the vanilla shulker box texture as the base. */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");

    private Button lockToggleButton;

    public MachineConfigScreen(MachineConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // Add the public/private lock toggle button in the top-right area of the GUI
        int btnX = this.leftPos + this.imageWidth - 16;
        int btnY = this.topPos + 4;

        MachineFrameworkBlockEntity blockEntity = getMenuBlockEntity();
        boolean isPublic = blockEntity != null && blockEntity.isPublic();

        lockToggleButton = Button.builder(
                Component.literal(isPublic ? "\uD83D\uDD13" : "\uD83D\uDD12"),  // Open padlock (public) / Closed padlock (private)
                btn -> onLockToggle()
        ).bounds(btnX, btnY, 12, 12)
         .build();
        this.addRenderableWidget(lockToggleButton);
    }

    private MachineFrameworkBlockEntity getMenuBlockEntity() {
        if (menu.getBlockPos() == null) return null;
        Object be = minecraft.level.getBlockEntity(menu.getBlockPos());
        if (be instanceof MachineFrameworkBlockEntity machine) {
            return machine;
        }
        return null;
    }

    private void onLockToggle() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
        }

        PacketDistributor.sendToServer(new ServerboundToggleMachinePublicPacket(menu.getBlockPos()));

        // Optimistically toggle the client-side value and update the button immediately
        MachineFrameworkBlockEntity blockEntity = getMenuBlockEntity();
        if (blockEntity != null) {
            blockEntity.togglePublic();
            boolean isPublic = blockEntity.isPublic();
            lockToggleButton.setMessage(Component.literal(isPublic ? "\uD83D\uDD13" : "\uD83D\uDD12"));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Render tooltip for the lock toggle button
        if (lockToggleButton != null && lockToggleButton.isMouseOver(mouseX, mouseY)) {
            MachineFrameworkBlockEntity blockEntity = getMenuBlockEntity();
            boolean isPublic = blockEntity != null && blockEntity.isPublic();
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Machine access").withStyle(ChatFormatting.AQUA),
                    Component.translatable(isPublic
                            ? "tooltip.praeteritum_futurum.machine_public"
                            : "tooltip.praeteritum_futurum.machine_private")
                            .withStyle(isPublic ? ChatFormatting.GREEN : ChatFormatting.RED),
                    Component.literal("Click to change who may access this machine").withStyle(ChatFormatting.GOLD)
            ), mouseX, mouseY);
        }
    }
}