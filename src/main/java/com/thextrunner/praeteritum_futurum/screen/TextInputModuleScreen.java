package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.api.module.ITextInputModule;
import com.thextrunner.praeteritum_futurum.network.ServerboundReturnToMachineMenuPacket;
import com.thextrunner.praeteritum_futurum.network.ServerboundSetModuleTextPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * Generic text input screen for modules implementing {@link ITextInputModule}.
 * <p>
 * This screen provides a text input field with Save and Clear buttons, plus a
 * Back button to return to the machine menu. The module defines the label, hint,
 * and max length via the ITextInputModule interface.
 * <p>
 * Third-party modders can use this screen for any module implementing
 * {@link ITextInputModule} — no custom screen code is needed.
 */
public class TextInputModuleScreen extends AbstractContainerScreen<TextInputModuleMenu> {
    private static final ResourceLocation SHULKER_GUI_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");

    private EditBox textField;
    private Button saveButton;
    private final String existingText;

    public TextInputModuleScreen(TextInputModuleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.existingText = menu.getTextInputModule().getExistingText(menu.getModuleStack());
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        ITextInputModule textModule = menu.getTextInputModule();

        // Back button
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

        // Text input field
        int fieldWidth = 160;
        int fieldX = this.leftPos + (this.imageWidth - fieldWidth) / 2;
        int fieldY = this.topPos + 40;

        textField = new EditBox(this.font, fieldX, fieldY, fieldWidth, 20, Component.empty());
        textField.setResponder(this::onTextChanged);
        if (textModule.getTextInputHint() != null) {
            textField.setHint(textModule.getTextInputHint());
        }
        textField.setFilter(s -> s.length() <= textModule.getMaxTextLength());
        textField.setValue(existingText);
        textField.setBordered(true);
        this.addRenderableWidget(textField);

        // Save button
        int buttonWidth = 90;
        int buttonsY = this.topPos + 70;

        saveButton = Button.builder(Component.translatable("screen.praeteritum_futurum.text_input.save"),
                btn -> saveText())
                .bounds(this.leftPos + (this.imageWidth - buttonWidth * 2 - 10) / 2, buttonsY, buttonWidth, 20)
                .build();
        this.addRenderableWidget(saveButton);

        // Clear button
        Button clearButton = Button.builder(Component.translatable("screen.praeteritum_futurum.text_input.clear"),
                btn -> clearText())
                .bounds(this.leftPos + (this.imageWidth - buttonWidth * 2 - 10) / 2 + buttonWidth + 10, buttonsY, buttonWidth, 20)
                .build();
        this.addRenderableWidget(clearButton);

        // Focus the text field
        textField.setFocused(true);
        updateSaveButtonState();
    }

    private void onTextChanged(String newText) {
        updateSaveButtonState();
    }

    private void updateSaveButtonState() {
        boolean changed = !textField.getValue().equals(existingText);
        boolean clearing = !existingText.isEmpty() && textField.getValue().isEmpty();
        saveButton.active = changed || clearing;
    }

    private void saveText() {
        String text = textField.getValue().trim();
        PacketDistributor.sendToServer(
                new ServerboundSetModuleTextPacket(text, menu.getSlotIndexInMachine(), menu.getBlockPos())
        );
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private void clearText() {
        if (!existingText.isEmpty()) {
            PacketDistributor.sendToServer(
                    new ServerboundSetModuleTextPacket("", menu.getSlotIndexInMachine(), menu.getBlockPos())
            );
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Draw title (label from the module)
        ITextInputModule textModule = menu.getTextInputModule();
        guiGraphics.drawCenteredString(this.font, textModule.getTextInputLabel(),
                this.leftPos + this.imageWidth / 2, this.topPos + 20, 0xFFFFFFFF);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(SHULKER_GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Allow Enter key to save
        if (chr == '\n' || chr == '\r') {
            if (saveButton.active) {
                saveText();
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
