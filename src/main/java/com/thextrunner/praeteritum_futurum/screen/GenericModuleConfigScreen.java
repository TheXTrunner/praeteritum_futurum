package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.item.custom.FilterModuleItem;
import com.thextrunner.praeteritum_futurum.network.ServerboundReturnToMachineMenuPacket;
import com.thextrunner.praeteritum_futurum.network.ServerboundToggleFilterModePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * Generic base screen for module configuration menus.
 * Third-party modules can extend this class to add custom UI elements
 * while inheriting the standard back button, background rendering,
 * and filter mode toggle functionality.
 */
public class GenericModuleConfigScreen extends AbstractContainerScreen<GenericModuleConfigMenu> {
    private static final ResourceLocation SHULKER_GUI_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");
    private Button backButton;
    private Button filterModeButton;

    public GenericModuleConfigScreen(GenericModuleConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        backButton = Button.builder(
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
                .build();
        this.addRenderableWidget(backButton);

        // Client-side check keeps common interfaces clean and safe for third-party modules
        if (menu.getModuleStack().getItem() instanceof FilterModuleItem) {
            FilterModuleItem.FilterMode currentMode = FilterModuleItem.getFilterMode(menu.getModuleStack());

            filterModeButton = Button.builder(
                            Component.literal("Mode: " + currentMode.getLabel()), button -> {
                                if (this.minecraft != null && this.minecraft.player != null) {
                                    this.minecraft.player.playNotifySound(
                                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                                            net.minecraft.sounds.SoundSource.PLAYERS,
                                            0.5F,
                                            1.0F
                                    );
                                }

                                PacketDistributor.sendToServer(
                                        new ServerboundToggleFilterModePacket(
                                                menu.getSlotIndexInMachine(), menu.getBlockPos()
                                        )
                                );

                                FilterModuleItem.toggleFilterMode(menu.getModuleStack());
                                FilterModuleItem.FilterMode nextMode = FilterModuleItem.getFilterMode(menu.getModuleStack());
                                button.setMessage(Component.literal("Mode: " + nextMode.getLabel()));
                            })
                    .bounds(this.leftPos + 60, this.topPos - 22, 110, 20)
                    .build();
            this.addRenderableWidget(filterModeButton);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (backButton != null && backButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Return to machine").withStyle(ChatFormatting.AQUA),
                    Component.literal("Close this module's configuration and reopen the machine.")
                            .withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        } else if (filterModeButton != null && filterModeButton.isMouseOver(mouseX, mouseY)) {
            FilterModuleItem.FilterMode mode = FilterModuleItem.getFilterMode(menu.getModuleStack());
            ChatFormatting color = mode == FilterModuleItem.FilterMode.WHITELIST
                    ? ChatFormatting.GREEN : ChatFormatting.RED;
            var explanation = mode == FilterModuleItem.FilterMode.WHITELIST
                    ? Component.literal("Only listed items pass the filter.")
                    : Component.literal("Listed items are blocked; other items pass.");
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Filter mode").withStyle(ChatFormatting.AQUA),
                    Component.literal(mode.getLabel()).withStyle(color),
                    explanation.withStyle(ChatFormatting.GRAY),
                    Component.literal("Click to switch modes").withStyle(ChatFormatting.GOLD)
            ), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(SHULKER_GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }
}