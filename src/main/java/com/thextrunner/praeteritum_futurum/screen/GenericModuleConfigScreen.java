package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.network.ServerboundReturnToMachineMenuPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class GenericModuleConfigScreen extends AbstractContainerScreen<GenericModuleConfigMenu> {
    private static final ResourceLocation SHULKER_GUI_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");

    public GenericModuleConfigScreen(GenericModuleConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(Button.builder(
                        Component.literal("<- Back"), button -> {
                            if (this.minecraft != null && this.minecraft.player != null) {
                                this.minecraft.player.playNotifySound(
                                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                                        net.minecraft.sounds.SoundSource.PLAYERS,
                                        0.5F,
                                        1.0F
                                );
                            }

                            PacketDistributor.sendToServer(
                                    new ServerboundReturnToMachineMenuPacket(menu.getBlockPos())
                            );
                        })
                .bounds(this.leftPos + 6, this.topPos - 22, 50, 20)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(SHULKER_GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }
}