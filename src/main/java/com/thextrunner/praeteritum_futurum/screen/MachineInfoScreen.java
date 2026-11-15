package com.thextrunner.praeteritum_futurum.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.thextrunner.praeteritum_futurum.api.module.ITextInputModule;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import com.thextrunner.praeteritum_futurum.api.module.IConfigurableModule;
import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem;
import com.thextrunner.praeteritum_futurum.network.ServerboundOpenModuleConfigPacket;
import com.thextrunner.praeteritum_futurum.network.ServerboundSetModuleConfigItemPacket;
import com.thextrunner.praeteritum_futurum.network.ServerboundSetModuleTextPacket;
import com.thextrunner.praeteritum_futurum.network.ServerboundToggleMachinePublicPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MachineInfoScreen extends AbstractContainerScreen<MachineInfoMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");

    private int configuringSlotIndex = -1;
    private boolean lastClickWasOverlay = false;

    // Lock toggle button
    private Button lockToggleButton;

    // Inline text input panel state (for ITextInputModule implementations)
    private EditBox textInputField = null;
    private Button textInputSaveButton = null;
    private Button textInputClearButton = null;
    private int textInputSlotIndex = -1;
    private String textInputExistingText = "";

    public MachineInfoScreen(MachineInfoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    private boolean isSlotInActiveOverlay(int slotIndex) {
        if (configuringSlotIndex < 0 || configuringSlotIndex >= 27) return false;
        Slot slot = this.menu.slots.get(configuringSlotIndex);
        MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return false;

        ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slot.index);
        if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
            return baseModule.shouldHideSlot(configuringSlotIndex, slotIndex);
        }
        return false;
    }

    @Override
    protected void renderSlot(@NotNull GuiGraphics graphics, @NotNull Slot slot) {
        if (isSlotInActiveOverlay(slot.index)) {
            return;
        }

        super.renderSlot(graphics, slot);

        if (slot.index < 27) {
            MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slot.index);
                com.thextrunner.praeteritum_futurum.client.ModuleScreenRenderer.renderModuleOverlay(graphics, slot, moduleStack, blockEntity, this.font);
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        // Add the public/private lock toggle button in the top-right area of the GUI
        int btnX = this.leftPos + this.imageWidth - 16;
        int btnY = this.topPos + 4;

        MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
        boolean isPublic = blockEntity != null && blockEntity.isPublic();

        lockToggleButton = Button.builder(
                Component.literal(isPublic ? "\uD83D\uDD13" : "\uD83D\uDD12"),  // Open padlock (public) / Closed padlock (private)
                btn -> onLockToggle()
        ).bounds(btnX, btnY, 12, 12)
         .build();
        this.addRenderableWidget(lockToggleButton);

        // If text input panel was active, re-create the widgets after screen resize/reinit
        if (textInputSlotIndex >= 0) {
            setupTextInputPanel();
        }
    }

    private void onLockToggle() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
        }

        PacketDistributor.sendToServer(new ServerboundToggleMachinePublicPacket(menu.getBlockPos()));

        // Optimistically toggle the client-side value and update the button immediately
        MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            blockEntity.togglePublic();
            boolean isPublic = blockEntity.isPublic();
            lockToggleButton.setMessage(Component.literal(isPublic ? "\uD83D\uDD13" : "\uD83D\uDD12"));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Render tooltip for the lock toggle button
        if (lockToggleButton != null && lockToggleButton.isMouseOver(mouseX, mouseY)) {
            MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                Component tooltip;
                if (blockEntity.isPublic()) {
                    tooltip = Component.translatable("tooltip.praeteritum_futurum.machine_public");
                } else {
                    tooltip = Component.translatable("tooltip.praeteritum_futurum.machine_private");
                }
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(tooltip), mouseX, mouseY);
            }
        }

        if (configuringSlotIndex >= 0 && configuringSlotIndex < 27) {
            Slot slot = this.menu.slots.get(configuringSlotIndex);
            MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();

            if (blockEntity != null) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slot.index);
                int renderX = slot.x + this.leftPos;
                int renderY = slot.y + this.topPos;

                if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
                    // Set overlay slot index for ProcessingModuleItem so border-aware rendering works
                    if (baseModule instanceof com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem processingModule) {
                        processingModule.setOverlaySlotIndex(configuringSlotIndex);
                    }
                    baseModule.renderCustomOverlay(guiGraphics, renderX, renderY, moduleStack, blockEntity, this.font);
                } else {
                    configuringSlotIndex = -1;
                }
            }
        }

        // Render the inline text input panel overlay ON TOP of everything else
        if (textInputField != null && textInputSlotIndex >= 0 && textInputSlotIndex < 27) {
            renderTextInputPanel(guiGraphics);
        }
    }

    /**
     * Sets up the inline text input panel widgets (EditBox, Save, Clear buttons).
     * Positioned above the machine GUI menu so it doesn't intersect the module grid.
     */
    private void setupTextInputPanel() {
        // Remove any existing text input widgets first (but keep other widgets)
        removeTextInputWidgets();

        MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) { closeTextInputPanel(); return; }

        ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(textInputSlotIndex);
        if (!(moduleStack.getItem() instanceof ITextInputModule textModule)) { closeTextInputPanel(); return; }

        // Panel positioned above the machine GUI
        // The machine GUI is centered: leftPos = (width - 176) / 2, topPos = (height - 166) / 2
        int panelWidth = 200;
        int panelHeight = 26;
        int panelX = this.leftPos + (this.imageWidth - panelWidth) / 2; // centered on the GUI
        int panelY = this.topPos - panelHeight - 6; // 6px above the GUI

        // Save and Clear buttons — compact, positioned to the right
        int btnWidth = 36;
        int btnHeight = 16;
        int btnY = panelY + (panelHeight - btnHeight) / 2; // vertically centered in panel

        textInputSaveButton = Button.builder(
                        Component.translatable("screen.praeteritum_futurum.text_input.save"),
                        btn -> saveTextInput())
                .bounds(panelX + panelWidth - btnWidth - 4, btnY, btnWidth, btnHeight)
                .build();
        this.addRenderableWidget(textInputSaveButton);

        textInputClearButton = Button.builder(
                        Component.translatable("screen.praeteritum_futurum.text_input.clear"),
                        btn -> clearTextInput())
                .bounds(panelX + panelWidth - btnWidth * 2 - 8, btnY, btnWidth, btnHeight)
                .build();
        this.addRenderableWidget(textInputClearButton);

        // Text input field — fills the remaining space to the left of the buttons
        int fieldX = panelX + 4;
        int fieldY = panelY + (panelHeight - 16) / 2; // vertically centered
        int fieldWidth = panelWidth - btnWidth * 2 - 16; // space between buttons and field

        textInputField = new EditBox(this.font, fieldX, fieldY, fieldWidth, 16, Component.empty());
        textInputField.setHint(textModule.getTextInputHint());
        textInputField.setFilter(s -> s.length() <= textModule.getMaxTextLength());
        textInputField.setBordered(true);
        textInputField.setValue(textInputExistingText);
        textInputField.setResponder(this::onTextInputChanged);
        this.addRenderableWidget(textInputField);

        // Set focus AFTER adding to renderables so the EditBox can receive keyboard input
        textInputField.setFocused(true);
        updateTextInputSaveButtonState();
    }

    /**
     * Removes only the text input panel widgets from the renderables list.
     */
    private void removeTextInputWidgets() {
        if (textInputField != null) {
            this.renderables.remove(textInputField);
            textInputField = null;
        }
        if (textInputSaveButton != null) {
            this.renderables.remove(textInputSaveButton);
            textInputSaveButton = null;
        }
        if (textInputClearButton != null) {
            this.renderables.remove(textInputClearButton);
            textInputClearButton = null;
        }
    }

    /**
     * Renders the text input panel overlay (dark background + label).
     * Positioned above the machine GUI menu.
     */
    private void renderTextInputPanel(GuiGraphics guiGraphics) {
        MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) { closeTextInputPanel(); return; }

        ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(textInputSlotIndex);
        if (!(moduleStack.getItem() instanceof ITextInputModule textModule)) { closeTextInputPanel(); return; }

        int panelWidth = 200;
        int panelHeight = 26;
        int panelX = this.leftPos + (this.imageWidth - panelWidth) / 2;
        int panelY = this.topPos - panelHeight - 6;

        // Dark semi-transparent background
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xDD181818);
        // Border
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF505050);
        guiGraphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF505050);
        guiGraphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF505050);
        guiGraphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF505050);

        // Label above the panel
        guiGraphics.drawCenteredString(this.font, textModule.getTextInputLabel(),
                panelX + panelWidth / 2, panelY - 9, 0xFFAAAAAA);
    }

    private void onTextInputChanged(String newText) {
        updateTextInputSaveButtonState();
    }

    private void updateTextInputSaveButtonState() {
        if (textInputSaveButton == null || textInputField == null) return;
        // Save button is active when there's any text to save
        String text = textInputField.getValue().trim();
        textInputSaveButton.active = !text.isEmpty();
    }

    private void saveTextInput() {
        if (textInputField == null || textInputSlotIndex < 0) return;
        String text = textInputField.getValue().trim();
        MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) { closeTextInputPanel(); return; }

        // Update client-side module stack immediately
        ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(textInputSlotIndex);
        if (moduleStack.getItem() instanceof ITextInputModule textModule) {
            textModule.setText(moduleStack, text);
        }

        PacketDistributor.sendToServer(
                new ServerboundSetModuleTextPacket(text, textInputSlotIndex, blockEntity.getBlockPos())
        );
        if (this.minecraft != null && this.minecraft.player != null) {
            Component confirmation = text.isEmpty()
                    ? Component.translatable("screen.praeteritum_futurum.text_input.cleared")
                    : Component.translatable("screen.praeteritum_futurum.text_input.saved");
            this.minecraft.player.displayClientMessage(confirmation, true);
            this.minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        // Close the panel after saving so it can be reopened by clicking the config slot again
        closeTextInputPanel();
    }

    private void clearTextInput() {
        if (textInputSlotIndex < 0) return;
        MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) { closeTextInputPanel(); return; }

        // Update client-side module stack immediately
        ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(textInputSlotIndex);
        if (moduleStack.getItem() instanceof ITextInputModule textModule) {
            textModule.setText(moduleStack, "");
        }

        PacketDistributor.sendToServer(
                new ServerboundSetModuleTextPacket("", textInputSlotIndex, blockEntity.getBlockPos())
        );
        if (this.minecraft != null && this.minecraft.player != null) {
            Component confirmation = Component.translatable("screen.praeteritum_futurum.text_input.cleared");
            this.minecraft.player.displayClientMessage(confirmation, true);
            this.minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        closeTextInputPanel();
    }

    /**
     * Closes the inline text input panel and returns to the normal config overlay.
     */
    private void closeTextInputPanel() {
        // Remove widgets from the renderables list
        removeTextInputWidgets();
        textInputSlotIndex = -1;
        textInputExistingText = "";
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        if (configuringSlotIndex >= 0 || lastClickWasOverlay || textInputField != null) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // When text input panel is active, let the EditBox handle all keyboard input first
        if (textInputField != null) {
            // Escape — close the text input panel
            if (keyCode == 256) {
                closeTextInputPanel();
                return true;
            }
            // Let the EditBox handle the key press — it consumes what it needs (letters, numbers, backspace, etc.)
            // and returns false for keys it doesn't care about
            if (textInputField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            // If the EditBox didn't consume it, still don't let the parent handle it
            // to prevent keys like 'E' (inventory toggle) from closing the screen
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // When text input panel is active, let the EditBox handle all character input
        if (textInputField != null) {
            // Handle Enter key to save text input
            if (chr == '\n' || chr == '\r') {
                if (textInputSaveButton != null && textInputSaveButton.active) {
                    saveTextInput();
                    return true;
                }
            }
            // Let the EditBox handle the character
            textInputField.charTyped(chr, modifiers);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (configuringSlotIndex < 0 && this.isDragging()) {
            this.setDragging(false);
        }

        // If text input panel is active, handle clicks
        if (textInputField != null) {
            // Check if click is inside the panel bounds (positioned above the GUI)
            int panelWidth = 200;
            int panelHeight = 26;
            int panelX = this.leftPos + (this.imageWidth - panelWidth) / 2;
            int panelY = this.topPos - panelHeight - 6;

            if (mouseX >= panelX && mouseX < panelX + panelWidth && mouseY >= panelY && mouseY < panelY + panelHeight) {
                // Click inside panel — let widgets handle it
                lastClickWasOverlay = true;
                return super.mouseClicked(mouseX, mouseY, button);
            } else {
                // Click outside panel — close it
                closeTextInputPanel();
                lastClickWasOverlay = true;
                return true;
            }
        }

        if (configuringSlotIndex >= 0 && configuringSlotIndex < 27) {
            Slot slot = this.menu.slots.get(configuringSlotIndex);
            MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();

            if (blockEntity != null) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slot.index);
                int slotX = slot.x + this.leftPos;
                int slotY = slot.y + this.topPos;

                if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
                    if (baseModule.isInsideOverlay(mouseX, mouseY, slotX, slotY)) {
                        lastClickWasOverlay = true;
                        this.setDragging(false);

                        // ITextInputModule: open inline text input panel on center slot click
                        if (moduleStack.getItem() instanceof ITextInputModule textModule &&
                                mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                            if (this.minecraft != null && this.minecraft.player != null) {
                                this.minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(),
                                        net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
                            }
                            // Open inline text input panel
                            textInputSlotIndex = slot.index;
                            textInputExistingText = textModule.getExistingText(moduleStack);
                            setupTextInputPanel();
                            return true;
                        }

                        if (baseModule instanceof ProcessingModuleItem procModule && procModule.hasConfigSlot() &&
                                procModule.useDefaultConfigSlotClick() &&
                                mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                            if (this.minecraft != null && this.minecraft.player != null) {
                                ItemStack carried = this.minecraft.player.containerMenu.getCarried();
                                ItemStack ghostCopy = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);

                                assert blockEntity.getLevel() != null;
                                procModule.setConfigSlotItem(moduleStack, ghostCopy, blockEntity.getLevel().registryAccess());
                                PacketDistributor.sendToServer(new ServerboundSetModuleConfigItemPacket(slot.index, ghostCopy, blockEntity.getBlockPos()));
                            }
                            return true;
                        }

                        // Consume the click on the close button and play sound, deferring close to mouseReleased
                        if (baseModule.isCloseButton(mouseX, mouseY, slotX, slotY)) {
                            if (this.minecraft != null && this.minecraft.player != null) {
                                this.minecraft.player.playNotifySound(
                                        SoundEvents.UI_BUTTON_CLICK.value(),
                                        net.minecraft.sounds.SoundSource.PLAYERS,
                                        0.5F,
                                        1.0F
                                );
                            }
                            return true;
                        }

                        baseModule.handleCustomClick(mouseX, mouseY, slotX, slotY, moduleStack, blockEntity, slot.index, this.minecraft);
                        return true;
                    }
                }
            }
        }

        lastClickWasOverlay = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (configuringSlotIndex >= 0 && configuringSlotIndex < 27 && lastClickWasOverlay) {
            Slot slot = this.menu.slots.get(configuringSlotIndex);
            MachineFrameworkBlockEntity blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slot.index);
                int slotX = slot.x + this.leftPos;
                int slotY = slot.y + this.topPos;

                if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
                    if (baseModule.isCloseButton(mouseX, mouseY, slotX, slotY)) {
                        configuringSlotIndex = -1;
                    }
                }
            }
            lastClickWasOverlay = false;
            return true; // Consumes the mouse release so vanilla never processes a slot click or drop!
        }

        lastClickWasOverlay = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @SuppressWarnings("DataFlowIssue")//that of nullable warning for "slot" is annoying and nothing to worry about
    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, @NotNull ClickType type) {

        if (type == ClickType.QUICK_CRAFT) {
            super.slotClicked(slot, slotId, mouseButton, type);
            return;
        }

        // Block all slot interactions when text input panel is active
        if (textInputField != null) {
            return;
        }

        // Ignore underlying inventory slot clicks if the click originated inside the overlay
        if (configuringSlotIndex >= 0 && lastClickWasOverlay) {
            return;
        }

        // Allow player inventory interactions while the module config overlay is open
        if (configuringSlotIndex >= 0 && slot != null && slot.index >= 27) {
            super.slotClicked(slot, slotId, mouseButton, type);
            return;
        }

        // Prevent container interaction if the slot is hidden under the active config overlay
        if (configuringSlotIndex >= 0 && slot != null && isSlotInActiveOverlay(slot.index)) {
            return;
        }

        // Prevent container interaction and duplicate sounds when clicking buttons on the active config slot
        if (configuringSlotIndex >= 0 && slot != null && slot.index == configuringSlotIndex) {
            return;
        }

        if (slot != null && slot.index < 27) {
            MachineFrameworkBlockEntity blockEntity = menu.getBlockEntity();
            if (blockEntity != null) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(slot.index);

                if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
                    if (baseModule.opensInlineOverlay()) {
                        if (configuringSlotIndex != slot.index) {
                            if (this.minecraft != null && this.minecraft.player != null) {
                                this.minecraft.player.playNotifySound(
                                        SoundEvents.UI_BUTTON_CLICK.value(),
                                        net.minecraft.sounds.SoundSource.PLAYERS,
                                        0.5F,
                                        1.0F
                                );
                            }
                            configuringSlotIndex = slot.index;
                        }
                        return;
                    }
                }

                if (moduleStack.getItem() instanceof IConfigurableModule) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.playNotifySound(
                                SoundEvents.UI_BUTTON_CLICK.value(),
                                net.minecraft.sounds.SoundSource.PLAYERS,
                                0.5F,
                                1.0F
                        );
                    }
                    PacketDistributor.sendToServer(new ServerboundOpenModuleConfigPacket(slot.index, blockEntity.getBlockPos()));
                    return;
                }
            }
        }

        if (configuringSlotIndex >= 0) {
            return;
        }

        configuringSlotIndex = -1;

        super.slotClicked(slot, slotId, mouseButton, type);
    }
}