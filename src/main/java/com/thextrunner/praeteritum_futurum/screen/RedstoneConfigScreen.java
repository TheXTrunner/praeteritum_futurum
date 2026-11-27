package com.thextrunner.praeteritum_futurum.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuit;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuitState;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneComponent;
import com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem;
import com.thextrunner.praeteritum_futurum.network.ServerboundRedstoneConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom screen for the Redstone Module circuit editor.
 * <p>
 * This is the Render Layer in the three-layer architecture. It reads from the server-synced
 * {@link RedstoneCircuitState} snapshot to display overlays, tooltips, and visual indicators.
 * It does not run circuit simulation or access inventory slots directly for logic purposes.
 * <p>
 * <b>Three-Layer Architecture:</b>
 * <ul>
 *   <li><b>Inventory Layer</b> — {@link com.thextrunner.praeteritum_futurum.screen.RedstoneConfigMenu}
 *   stores ItemStacks in 54 slots</li>
 *   <li><b>Logic Layer</b> — {@link com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuit}
 *   runs propagation on the server and creates a state snapshot</li>
 *   <li><b>Render Layer (this class)</b> — reads from {@link #serverCircuitState} for all display data</li>
 * </ul>
 * <p>
 * <b>Data flow:</b> The server sends a {@link com.thextrunner.praeteritum_futurum.network.ClientboundRedstoneCircuitStatePacket}
 * after circuit evaluations and component changes. Untimed circuits may be evaluated only at the
 * configured interval, while scheduled transitions and configuration actions can trigger updates sooner.
 * The packet handler calls
 * {@link #setServerCircuitState(RedstoneCircuitState)} to update the stored state.
 * This screen reads from that state for rendering overlays and tooltips.
 * If no server state has been received yet (first frame), the grid shows nothing.
 * <p>
 * Extends GenericModuleConfigScreen to inherit the standard back button,
 * filter mode toggle, and background rendering. Uses a double-chest layout for the 54-slot (9×6) grid.
 * <p>
 * Visual indicators include:
 * <ul>
 *   <li>Component overlays (including cropped vanilla material textures for buttons)</li>
 *   <li>Directional artwork for repeaters, comparators, observers, and attachable components</li>
 *   <li>A dark bar overlay on side-locked repeaters</li>
 *   <li>Custom tooltips showing signal strength, input/output, and lock state</li>
 * </ul>
 * <p>
 * Third-party modules can similarly extend GenericModuleConfigScreen
 * to add their own custom UI overlays.
 */
public class RedstoneConfigScreen extends GenericModuleConfigScreen {

    // Vanilla double chest texture (176x166) — used for 54-slot inventories
    private static final ResourceLocation DOUBLE_CHEST_GUI_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");


    // Rotation text color
    private static final int ROTATION_TEXT_COLOR = 0xFFFFA500; // Orange

    // Vanilla redstone sprites are used directly so the GUI matches the game's pixel art.
    private static final ResourceLocation VANILLA_WIRE_LINE_0 = ResourceLocation.withDefaultNamespace("textures/block/redstone_dust_line0.png");
    private static final ResourceLocation VANILLA_WIRE_LINE_1 = ResourceLocation.withDefaultNamespace("textures/block/redstone_dust_line1.png");
    private static final ResourceLocation VANILLA_WIRE_DOT = ResourceLocation.withDefaultNamespace("textures/block/redstone_dust_dot.png");
    private static final ResourceLocation VANILLA_WIRE_OVERLAY = ResourceLocation.withDefaultNamespace("textures/block/redstone_dust_overlay.png");
    private static final ResourceLocation VANILLA_REDSTONE_BLOCK = ResourceLocation.withDefaultNamespace("textures/block/redstone_block.png");
    private static final ResourceLocation VANILLA_OBSERVER_TOP = ResourceLocation.withDefaultNamespace("textures/block/observer_top.png");
    // Custom overlay textures for redstone components (16×16 PNGs)
    private static final ResourceLocation OVERLAY_REPEATER_ON = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/repeater_on.png");
    private static final ResourceLocation OVERLAY_REPEATER_OFF = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/repeater_off.png");
    private static final ResourceLocation OVERLAY_COMPARATOR_COMPARE_ON = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/comparator_compare_on.png");
    private static final ResourceLocation OVERLAY_COMPARATOR_COMPARE_OFF = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/comparator_compare_off.png");
    private static final ResourceLocation OVERLAY_COMPARATOR_SUBTRACT_ON = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/comparator_subtract_on.png");
    private static final ResourceLocation OVERLAY_COMPARATOR_SUBTRACT_OFF = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/comparator_subtract_off.png");
    private static final ResourceLocation OVERLAY_TORCH_ATTACHED_ON = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/torch_attached_on.png");
    private static final ResourceLocation OVERLAY_TORCH_ATTACHED_OFF = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/torch_attached_off.png");
    private static final ResourceLocation OVERLAY_TORCH_STANDING_ON = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/torch_standing_on.png");
    private static final ResourceLocation OVERLAY_TORCH_STANDING_OFF = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/torch_standing_off.png");
    private static final ResourceLocation OVERLAY_LEVER_ON = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/lever_on.png");
    private static final ResourceLocation OVERLAY_LEVER_OFF = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/lever_off.png");
    private static final ResourceLocation OVERLAY_LEVER_ON_STANDING = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/lever_on_standing.png");
    private static final ResourceLocation OVERLAY_LEVER_OFF_STANDING = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/lever_off_standing.png");
    private static final ResourceLocation OVERLAY_REPEATER_LOCK_BAR = ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/redstone/repeater_lock_bar.png");

    /** Circuit state synced from the server. Null until the first packet arrives. */
    private RedstoneCircuitState serverCircuitState;

    /** When true, clicks configure components instead of interacting with items. */
    private boolean configMode = false;

    /** Button for toggling Config Mode, kept as a reference to update its message. */
    private Button configModeButton;

    private int editingSignalSourceSlot = -1;
    private int editingSignalSourceValue;
    private boolean draggingSignalSourceSlider;

    private static final int SIGNAL_SOURCE_EDITOR_WIDTH = 72;
    private static final int SIGNAL_SOURCE_EDITOR_HEIGHT = 18;

    public RedstoneConfigScreen(GenericModuleConfigMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // Shulker box layout: 176x166 for the grid area, inventory below
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // Add Config Mode toggle button next to the Back button
        this.configModeButton = Button.builder(
                Component.literal("Config Mode: OFF"), button -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.playNotifySound(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                                net.minecraft.sounds.SoundSource.PLAYERS,
                                0.5F,
                                1.0F
                        );
                    }
                    this.configMode = !this.configMode;
                    if (!this.configMode) {
                        editingSignalSourceSlot = -1;
                        draggingSignalSourceSlider = false;
                    }
                    button.setMessage(Component.literal("Config Mode: " + (this.configMode ? "ON" : "OFF")));
                })
                .bounds(this.leftPos + 60, this.topPos - 22, 110, 20)
                .build();

        this.addRenderableWidget(this.configModeButton);
    }

    /**
     * Intercepts mouse clicks when Config Mode is enabled.
     * In Config Mode, all clicks on grid slots send configuration packets instead of
     * performing vanilla inventory actions (which would pick up the item).
     * - Left-click (button 0): Toggle lever/comparator mode, cycle repeater delay, press button
     * - Right-click (button 1): Rotate components, torches, cycle lever attachment
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editingSignalSourceSlot >= 0) {
            handleSignalSourceEditorClick(mouseX, mouseY, button);
            return true;
        }

        // In Config Mode, intercept all clicks on grid slots
        if (this.configMode) {
            int gridStartX = leftPos + 8;
            int gridStartY = topPos + 18;

            for (int slotIndex = 0; slotIndex < RedstoneCircuit.GRID_SIZE; slotIndex++) {
                int col = slotIndex % RedstoneCircuit.GRID_WIDTH;
                int row = slotIndex / RedstoneCircuit.GRID_WIDTH;
                int x = gridStartX + col * 18;
                int y = gridStartY + row * 18;

                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    // Check if this slot has a configurable component
                    RedstoneCircuitState state = getCircuitState();
                    if (state != null) {
                        RedstoneComponent component = state.getComponent(slotIndex);
                        if (component != null) {
                            if (component == RedstoneComponent.SIGNAL_SOURCE && button == 0) {
                                editingSignalSourceSlot = slotIndex;
                                editingSignalSourceValue = state.getSignalSourceStrength(slotIndex);
                                return true;
                            }

                            int action = -1;
                            // Left-click (button 0): toggle, cycle, or press a component
                            if (button == 0) {
                                if (component == RedstoneComponent.LEVER) {
                                    action = ServerboundRedstoneConfigPacket.ACTION_TOGGLE_LEVER;
                                } else if (component == RedstoneComponent.COMPARATOR) {
                                    action = ServerboundRedstoneConfigPacket.ACTION_TOGGLE_COMPARATOR_MODE;
                                } else if (component == RedstoneComponent.REPEATER) {
                                    action = ServerboundRedstoneConfigPacket.ACTION_CYCLE_REPEATER_DELAY;
                                } else if (component == RedstoneComponent.BUTTON) {
                                    action = ServerboundRedstoneConfigPacket.ACTION_PRESS_BUTTON;
                                }
                            }
                            // Right-click (button 1): rotate components, torches, cycle lever attachment
                            else if (button == 1) {
                                switch (component) {
                                    case REPEATER, COMPARATOR, OBSERVER -> action = ServerboundRedstoneConfigPacket.ACTION_ROTATE_COMPONENT;
                                    case TORCH -> action = ServerboundRedstoneConfigPacket.ACTION_ROTATE_TORCH;
                                    case LEVER -> action = ServerboundRedstoneConfigPacket.ACTION_CYCLE_LEVER_ATTACHMENT;
                                    case BUTTON -> action = ServerboundRedstoneConfigPacket.ACTION_CYCLE_BUTTON_ATTACHMENT;
                                    default -> { /* redstone block and wall have no config action */ }
                                }
                            }
                            if (action >= 0) {
                                // Play click sound for feedback
                                if (this.minecraft != null && this.minecraft.player != null) {
                                    this.minecraft.player.playNotifySound(
                                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                                            net.minecraft.sounds.SoundSource.PLAYERS,
                                            0.5F,
                                            1.0F
                                    );
                                }

                                // Send configuration packet to server
                                PacketDistributor.sendToServer(
                                        new ServerboundRedstoneConfigPacket(menu.getSlotIndexInMachine(), slotIndex, action, menu.getBlockPos())
                                );

                                // Consume the click — don't let it reach the inventory
                                return true;
                            }
                        }
                    }
                    // Consume the click even if no action — prevent item interaction in Config Mode
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (editingSignalSourceSlot >= 0 && draggingSignalSourceSlider && button == 0) {
            setSignalSourceValueFromSlider(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (editingSignalSourceSlot >= 0 && draggingSignalSourceSlider) {
            draggingSignalSourceSlider = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingSignalSourceSlot >= 0 && keyCode == 256) {
            editingSignalSourceSlot = -1;
            draggingSignalSourceSlider = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleSignalSourceEditorClick(double mouseX, double mouseY, int button) {
        int panelX = signalSourceEditorX();
        int panelY = signalSourceEditorY();
        if (button == 0 && isInside(mouseX, mouseY, panelX + 2, panelY + 2, 12, 14)) {
            setSignalSourceStrength(editingSignalSourceValue - 1);
        } else if (button == 0 && isInside(mouseX, mouseY, panelX + 58, panelY + 2, 12, 14)) {
            setSignalSourceStrength(editingSignalSourceValue + 1);
        } else if (button == 0 && isInside(mouseX, mouseY, signalSliderStartX(), panelY + 1,
                signalSliderWidth(), SIGNAL_SOURCE_EDITOR_HEIGHT - 2)) {
            draggingSignalSourceSlider = true;
            setSignalSourceValueFromSlider(mouseX);
        } else if (!isInside(mouseX, mouseY, panelX, panelY,
                SIGNAL_SOURCE_EDITOR_WIDTH, SIGNAL_SOURCE_EDITOR_HEIGHT)) {
            editingSignalSourceSlot = -1;
            draggingSignalSourceSlider = false;
        }
    }

    private void setSignalSourceValueFromSlider(double mouseX) {
        double progress = (mouseX - signalSliderStartX()) / (signalSliderWidth() - 1.0);
        setSignalSourceStrength((int) Math.round(Math.clamp(progress, 0.0, 1.0) * 15.0));
    }

    private void setSignalSourceStrength(int strength) {
        int updatedStrength = Math.clamp(strength, 0, 15);
        if (updatedStrength == editingSignalSourceValue) return;
        editingSignalSourceValue = updatedStrength;
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playNotifySound(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    0.35F,
                    1.0F
            );
        }
        PacketDistributor.sendToServer(new ServerboundRedstoneConfigPacket(
                menu.getSlotIndexInMachine(), editingSignalSourceSlot,
                ServerboundRedstoneConfigPacket.ACTION_SET_SIGNAL_SOURCE_STRENGTH,
                editingSignalSourceValue, menu.getBlockPos()));
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int signalSourceEditorX() {
        int col = editingSignalSourceSlot % RedstoneCircuit.GRID_WIDTH;
        int slotX = leftPos + 8 + col * 18;
        return col < RedstoneCircuit.GRID_WIDTH / 2
                ? slotX + 18
                : slotX - SIGNAL_SOURCE_EDITOR_WIDTH;
    }

    private int signalSourceEditorY() {
        int row = editingSignalSourceSlot / RedstoneCircuit.GRID_WIDTH;
        return topPos + 18 + row * 18;
    }

    private int signalSliderStartX() {
        return signalSourceEditorX() + 16;
    }

    private int signalSliderWidth() {
        return 40;
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
            // Render custom tooltips instead of vanilla item tooltips for grid slots
            renderRedstoneTooltips(guiGraphics, mouseX, mouseY);
        }

        if (configModeButton != null && configModeButton.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("Redstone Config Mode").withStyle(ChatFormatting.AQUA),
                    Component.literal(configMode ? "ON — configure components" : "OFF — manage items")
                            .withStyle(configMode ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    Component.literal(configMode
                            ? "Grid clicks change component settings; items cannot be moved."
                            : "Grid clicks use normal inventory behavior." ).withStyle(ChatFormatting.GRAY),
                    Component.literal("Click to switch modes").withStyle(ChatFormatting.GOLD)
            ), mouseX, mouseY);
        }

        if (editingSignalSourceSlot >= 0) {
            renderSignalSourceEditor(guiGraphics, mouseX, mouseY);
        }
    }

    /**
     * Overrides tooltip rendering to suppress vanilla item tooltips for grid slots.
     * The custom redstone tooltips are rendered separately in renderRedstoneTooltips().
     */
    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Check if mouse is over a grid slot — if so, skip the vanilla tooltip
        int gridStartX = leftPos + 8;
        int gridStartY = topPos + 18;
        for (int slotIndex = 0; slotIndex < RedstoneCircuit.GRID_SIZE; slotIndex++) {
            int col = slotIndex % RedstoneCircuit.GRID_WIDTH;
            int row = slotIndex / RedstoneCircuit.GRID_WIDTH;
            int x = gridStartX + col * 18;
            int y = gridStartY + row * 18;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                // Skip vanilla tooltip for grid slots — custom tooltip handles this
                return;
            }
        }
        // For non-grid slots (player inventory, etc.), use vanilla tooltips
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * Overrides slot rendering to hide vanilla item sprites for redstone components.
     * This allows the custom overlay textures to replace the item appearance.
     */
    @Override
    protected void renderSlot(@NotNull GuiGraphics graphics, @NotNull Slot slot) {
        // For grid slots (0-53), hide the vanilla item sprite so only the custom overlay shows
        if (slot.index >= 0 && slot.index < RedstoneCircuit.GRID_SIZE && slot.hasItem()) {
            ItemStack saved = slot.getItem();
            slot.set(ItemStack.EMPTY);
            super.renderSlot(graphics, slot);
            slot.set(saved);
        } else {
            super.renderSlot(graphics, slot);
        }
    }

    /**
     * Renders visual indicators for component overlays and wall attachments on the 9×6 grid.
     * Reads from the server-synced circuit state (logic layer) — no inventory access or propagation.
     */
    private void renderRedstoneOverlays(GuiGraphics guiGraphics) {
        RedstoneCircuitState state = getCircuitState();
        if (state == null) return;

        int gridStartX = leftPos + 8;
        int gridStartY = topPos + 18;

        for (int slotIndex = 0; slotIndex < RedstoneCircuit.GRID_SIZE; slotIndex++) {
            int col = slotIndex % RedstoneCircuit.GRID_WIDTH;
            int row = slotIndex / RedstoneCircuit.GRID_WIDTH;

            int x = gridStartX + col * 18;
            int y = gridStartY + row * 18;

            RedstoneComponent component = state.getComponent(slotIndex);
            if (component != null) {
                // Each directional component has its own rotation field:
                // - torches use torchRotation, levers use leverRotation, repeaters/comparators use rotation
                int rotation;
                if (component == RedstoneComponent.TORCH) {
                    rotation = state.getTorchRotation(slotIndex);
                } else if (component == RedstoneComponent.LEVER) {
                    rotation = state.getLeverRotation(slotIndex);
                } else if (component == RedstoneComponent.BUTTON) {
                    rotation = state.getButtonRotation(slotIndex);
                } else {
                    rotation = state.getRotation(slotIndex);
                }
                renderComponentOverlay(guiGraphics, x, y, component, rotation, slotIndex, state);
            }
        }
    }

    /**
     * Called by the network packet handler when the server sends updated circuit state.
     */
    public void setServerCircuitState(RedstoneCircuitState state) {
        this.serverCircuitState = state;
    }

    /**
     * Returns whether a server update belongs to the module shown by this screen.
     */
    public boolean matchesModule(BlockPos blockPos, int moduleSlotIndex) {
        return menu.getBlockPos().equals(blockPos) && menu.getSlotIndexInMachine() == moduleSlotIndex;
    }

    /**
     * Returns the circuit state synced from the server.
     * Returns null if no server state has been received yet (first frame before the first packet).
     * The render layer reads from this state only — no local computation or fallback.
     */
    private RedstoneCircuitState getCircuitState() {
        return serverCircuitState;
    }

    /**
     * Renders a custom overlay texture for a redstone component.
     * For directional components (repeater, comparator), rotates the texture programmatically.
     * For levers, selects the ON/OFF texture based on state.
     * For side-powered repeaters, draws a colored lock indicator.
     */
    private void renderComponentOverlay(GuiGraphics guiGraphics, int x, int y, RedstoneComponent component,
                                        int rotation, int slotIndex, RedstoneCircuitState state) {
        // WALL is drawn programmatically to match the Machine Framework slot lid style
        if (component == RedstoneComponent.WALL) {
            renderWallOverlay(guiGraphics, x, y, slotIndex, state);
            return;
        }
        if (component == RedstoneComponent.WIRE) {
            renderWireOverlay(guiGraphics, x, y, slotIndex, state);
            return;
        }
        if (component == RedstoneComponent.BUTTON) {
            renderButtonOverlay(guiGraphics, x, y, slotIndex, state);
            return;
        }
        if (component == RedstoneComponent.OBSERVER) {
            renderObserverOverlay(guiGraphics, x, y, rotation, slotIndex, state);
            return;
        }
        if (component == RedstoneComponent.SIGNAL_SOURCE) {
            renderSignalSourceOverlay(guiGraphics, x, y, state.getSignalSourceStrength(slotIndex));
            return;
        }

        ResourceLocation texture;
        switch (component) {
            case REPEATER:
                int repeaterOutput = state.getOutputSignal(slotIndex);
                texture = repeaterOutput > 0 ? OVERLAY_REPEATER_ON : OVERLAY_REPEATER_OFF;
                break;
            case COMPARATOR:
                int comparatorOutput = state.getOutputSignal(slotIndex);
                boolean isSubtract = state.getComparatorSubtract(slotIndex);
                if (isSubtract) {
                    texture = comparatorOutput > 0 ? OVERLAY_COMPARATOR_SUBTRACT_ON : OVERLAY_COMPARATOR_SUBTRACT_OFF;
                } else {
                    texture = comparatorOutput > 0 ? OVERLAY_COMPARATOR_COMPARE_ON : OVERLAY_COMPARATOR_COMPARE_OFF;
                }
                break;
            case TORCH:
                int torchRot = rotation;
                int signalStrength = state.getSignalStrength(slotIndex);
                boolean torchOn = signalStrength > 0;
                if (torchRot == 4) {
                    // Standing torch — use standing textures, no rotation
                    texture = torchOn ? OVERLAY_TORCH_STANDING_ON : OVERLAY_TORCH_STANDING_OFF;
                } else {
                    // Attached torch — use attached textures, rotated by direction
                    texture = torchOn ? OVERLAY_TORCH_ATTACHED_ON : OVERLAY_TORCH_ATTACHED_OFF;
                }
                break;
            case LEVER:
                int leverRot = state.getLeverRotation(slotIndex);
                boolean leverOn = state.getLeverState(slotIndex);
                if (leverRot == 4) {
                    // Standing lever — use standing textures, no rotation
                    texture = leverOn ? OVERLAY_LEVER_ON_STANDING : OVERLAY_LEVER_OFF_STANDING;
                } else {
                    // Attached lever — use attached textures, rotated by direction
                    texture = leverOn ? OVERLAY_LEVER_ON : OVERLAY_LEVER_OFF;
                }
                break;
            case REDSTONE_BLOCK: texture = VANILLA_REDSTONE_BLOCK; break;
            default: return;
        }

        int texSize = 16;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Determine if this component needs directional rotation
        boolean isDirectional = false;
        if (component == RedstoneComponent.REPEATER || component == RedstoneComponent.COMPARATOR) {
            isDirectional = true;
        }
        else if (component == RedstoneComponent.TORCH && rotation < 4) {
            isDirectional = true;
        }
        else if (component == RedstoneComponent.LEVER) {
            int leverRot = state.getLeverRotation(slotIndex);
            isDirectional = leverRot < 4;
        }

        if (isDirectional) {
            // Directional components: rotate the texture around its center
            // The texture is 16x16, drawn at (x, y), so the center is at (x+8, y+8)
            // Standing torches (rotation 4) are drawn without rotation
            // Torches and levers: base texture faces South (attached to block below), so add 180° offset
            // Repeaters and comparators: base texture faces North, no offset
            float texCenterX = x + (texSize / 2.0f);
            float texCenterY = y + (texSize / 2.0f);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(texCenterX, texCenterY, 0);
            float baseAngle = rotation * 90.0f;
            float offset = (component == RedstoneComponent.TORCH || component == RedstoneComponent.LEVER) ? 180.0f : 0.0f;
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(baseAngle + offset));
            guiGraphics.pose().translate(-texCenterX, -texCenterY, 0);
            guiGraphics.blit(texture, x, y, 0, 0, texSize, texSize, texSize, texSize);
            guiGraphics.pose().popPose();
        } else {
            // Non-directional components: draw centered in the slot
            guiGraphics.blit(texture, x, y, 0, 0, texSize, texSize, texSize, texSize);
        }

        RenderSystem.disableBlend();

        if (component == RedstoneComponent.REPEATER) {
            renderRepeaterDelayOverlay(guiGraphics, x, y, rotation, slotIndex, state);
        }

        // Draw lock bar for repeaters when locked (bedrock-style dark bar across the top)
        if (component == RedstoneComponent.REPEATER && state.isRepeaterLocked(slotIndex)) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, OVERLAY_REPEATER_LOCK_BAR);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            // Draw the lock bar across the top of the repeater, rotated to match the repeater's facing
            // Rotate around the texture center (16x16 texture drawn at x, y)
            float texCenterX = x + 8.0f;
            float texCenterY = y + 8.0f;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(texCenterX, texCenterY, 0);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation * 90.0f));
            guiGraphics.pose().translate(-texCenterX, -texCenterY, 0);
            guiGraphics.blit(OVERLAY_REPEATER_LOCK_BAR, x, y, 0, 0, 16, 16, 16, 16);
            guiGraphics.pose().popPose();
            RenderSystem.disableBlend();
        }
    }

    /** Draws a narrow crop from the bottom of the placed button's vanilla material texture. */
    private void renderButtonOverlay(GuiGraphics guiGraphics, int x, int y, int slotIndex, RedstoneCircuitState state) {
        ItemStack buttonStack = menu.getSlot(slotIndex).getItem();
        if (!(buttonStack.getItem() instanceof BlockItem buttonItem)) return;

        TextureAtlasSprite materialSprite = Minecraft.getInstance().getBlockRenderer()
                .getBlockModelShaper().getBlockModel(buttonItem.getBlock().defaultBlockState()).getParticleIcon();
        var materialTexture = materialSprite.contents();
        ResourceLocation textureLocation = materialTexture.name()
                .withPath(path -> "textures/" + path + ".png");
        int rotation = state.getButtonRotation(slotIndex);
        boolean pressed = state.getSignalStrength(slotIndex) > 0;
        float angle = rotation < 4 ? (rotation - 2) * 90.0F : 0.0F;
        int textureWidth = materialTexture.width();
        int textureHeight = materialTexture.height();
        int cropWidth = Math.min(8, textureWidth);
        int cropHeight = Math.min(3, textureHeight);
        int cropX = (textureWidth - cropWidth) / 2;
        int cropY = textureHeight - cropHeight;
        int drawX = x + (16 - cropWidth) / 2;
        int baseY = rotation < 4 ? y + 16 - cropHeight : y + (16 - cropHeight) / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 8.0F, y + 8.0F, 0.0F);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        guiGraphics.pose().translate(-x - 8.0F, -y - 8.0F, 0.0F);
        guiGraphics.blit(textureLocation, drawX, baseY + (pressed ? 1 : 0), 0,
                cropX, cropY, cropWidth, cropHeight, textureWidth, textureHeight);
        guiGraphics.pose().popPose();
    }

    private void renderObserverOverlay(GuiGraphics guiGraphics, int x, int y, int rotation,
                                       int slotIndex, RedstoneCircuitState state) {
        Direction observedDirection = RedstoneCircuit.getFacingDirection(rotation);
        Direction outputDirection = observedDirection.getOpposite();
        int outputX = x + (outputDirection == Direction.WEST ? 1 : outputDirection == Direction.EAST ? 13 : 7);
        int outputY = y + (outputDirection == Direction.NORTH ? 1 : outputDirection == Direction.SOUTH ? 13 : 7);
        boolean powered = state.getOutputSignal(slotIndex) > 0;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 8.0F, y + 8.0F, 0.0F);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation * 90.0F));
        guiGraphics.pose().translate(-x - 8.0F, -y - 8.0F, 0.0F);
        guiGraphics.blit(VANILLA_OBSERVER_TOP, x, y, 0, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();

        guiGraphics.fill(outputX, outputY, outputX + 2, outputY + 2,
                powered ? 0xFFFF2222 : 0xFF552222);
    }

    private void renderRepeaterDelayOverlay(GuiGraphics guiGraphics, int x, int y, int rotation,
                                            int slotIndex, RedstoneCircuitState state) {
        ResourceLocation texture = state.getOutputSignal(slotIndex) > 0
                ? OVERLAY_TORCH_STANDING_ON
                : OVERLAY_TORCH_STANDING_OFF;
        int verticalOffset = Math.clamp(state.getRepeaterDelay(slotIndex), 1, 4) * 2 - 3;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        float texCenterX = x + 8.0f;
        float texCenterY = y + 8.0f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(texCenterX, texCenterY, 0);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation * 90.0f));
        guiGraphics.pose().translate(-texCenterX, -texCenterY, 0);
        guiGraphics.blit(texture, x, y + verticalOffset, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();

        RenderSystem.disableBlend();
    }

    private void renderWireOverlay(GuiGraphics guiGraphics, int x, int y, int slotIndex, RedstoneCircuitState state) {
        int tint = getWireTint(state.getSignalStrength(slotIndex));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        boolean north = state.isWireConnected(slotIndex, Direction.NORTH);
        boolean east = state.isWireConnected(slotIndex, Direction.EAST);
        boolean south = state.isWireConnected(slotIndex, Direction.SOUTH);
        boolean west = state.isWireConnected(slotIndex, Direction.WEST);

        // Vanilla uses two half-wire sprites for the four arms and adds the dot sprite for
        // isolated, corner, and branched shapes, but not for a straight run.
        if (north) drawWireArm(guiGraphics, x, y, Direction.NORTH, tint);
        if (east) drawWireArm(guiGraphics, x, y, Direction.EAST, tint);
        if (south) drawWireArm(guiGraphics, x, y, Direction.SOUTH, tint);
        if (west) drawWireArm(guiGraphics, x, y, Direction.WEST, tint);

        boolean straightNorthSouth = north && south && !east && !west;
        boolean straightEastWest = east && west && !north && !south;
        if (!straightNorthSouth && !straightEastWest) {
            drawWireTexture(guiGraphics, VANILLA_WIRE_DOT, x, y, tint);
            drawWireTexture(guiGraphics, VANILLA_WIRE_OVERLAY, x, y, 0xFFFFFF);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void drawWireArm(GuiGraphics guiGraphics, int x, int y, Direction direction, int tint) {
        ResourceLocation lineTexture = direction.getAxis() == Direction.Axis.Z ? VANILLA_WIRE_LINE_0 : VANILLA_WIRE_LINE_1;
        drawWireArmTexture(guiGraphics, lineTexture, x, y, direction, tint);
        drawWireArmTexture(guiGraphics, VANILLA_WIRE_OVERLAY, x, y, direction, 0xFFFFFF);
    }

    private void drawWireArmTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y,
                                    Direction direction, int tint) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(((tint >> 16) & 0xFF) / 255.0f,
                ((tint >> 8) & 0xFF) / 255.0f, (tint & 0xFF) / 255.0f, 1.0f);

        if (direction == Direction.NORTH) {
            guiGraphics.blit(texture, x, y, 0, 0, 16, 8, 16, 16);
        } else if (direction == Direction.SOUTH) {
            guiGraphics.blit(texture, x, y + 8, 0, 8, 16, 8, 16, 16);
        } else {
            // Rotating the vanilla north/south half-wire maps its top/bottom half to west/east.
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + 8.0f, y + 8.0f, 0.0f);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(270.0f));
            guiGraphics.pose().translate(-(x + 8.0f), -(y + 8.0f), 0.0f);
            if (direction == Direction.EAST) {
                guiGraphics.blit(texture, x, y + 8, 0, 8, 16, 8, 16, 16);
            } else {
                guiGraphics.blit(texture, x, y, 0, 0, 16, 8, 16, 16);
            }
            guiGraphics.pose().popPose();
        }
    }

    private void drawWireTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int tint) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(((tint >> 16) & 0xFF) / 255.0f,
                ((tint >> 8) & 0xFF) / 255.0f, (tint & 0xFF) / 255.0f, 1.0f);
        guiGraphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
    }

    private int getWireTint(int signalStrength) {
        float power = Math.clamp(signalStrength, 0, 15) / 15.0f;
        float red = power * 0.6f + (power > 0.0f ? 0.4f : 0.3f);
        float green = Math.clamp(power * power * 0.7f - 0.5f, 0.0f, 1.0f);
        float blue = Math.clamp(power * power * 0.6f - 0.7f, 0.0f, 1.0f);
        return (Math.round(red * 255.0f) << 16) |
                (Math.round(green * 255.0f) << 8) | Math.round(blue * 255.0f);
    }


    /**
     * Renders a WALL overlay that matches the Machine Framework slot lid style.
     * Uses the same "raised lid" (inverted inset frame) visual as empty module slots.
     */
    private void renderWallOverlay(GuiGraphics guiGraphics, int x, int y, int slotIndex, RedstoneCircuitState state) {
        // 1. Flat base filled with the exact UI panel background color
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFFC6C6C6);

        // 2. Subtle outer border for the inverted inset frame (raised lid illusion)
        // Top and Left edges (Light highlights)
        guiGraphics.fill(x, y, x + 16, y + 1, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + 1, y + 16, 0xFFFFFFFF);

        // Bottom and Right edges (Dark shadows)
        guiGraphics.fill(x, y + 15, x + 16, y + 16, 0xFF555555);
        guiGraphics.fill(x + 15, y, x + 16, y + 16, 0xFF555555);
    }

    private void renderSignalSourceOverlay(GuiGraphics guiGraphics, int x, int y, int strength) {
        int color = strength == 0 ? 0xFF383838 : getWireTint(strength) | 0xFF000000;
        guiGraphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF202020);
        guiGraphics.fill(x + 2, y + 2, x + 14, y + 14, color);
        guiGraphics.drawCenteredString(font, Integer.toString(strength), x + 8, y + 4, 0xFFFFFFFF);
    }

    private void renderSignalSourceEditor(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        RedstoneCircuitState state = getCircuitState();
        if (state == null || state.getComponent(editingSignalSourceSlot) != RedstoneComponent.SIGNAL_SOURCE) {
            editingSignalSourceSlot = -1;
            draggingSignalSourceSlider = false;
            return;
        }

        int x = signalSourceEditorX();
        int y = signalSourceEditorY();
        guiGraphics.fill(x, y, x + SIGNAL_SOURCE_EDITOR_WIDTH, y + SIGNAL_SOURCE_EDITOR_HEIGHT, 0xE0202020);
        guiGraphics.fill(x, y, x + SIGNAL_SOURCE_EDITOR_WIDTH, y + 1, 0xFF777777);
        guiGraphics.fill(x, y + SIGNAL_SOURCE_EDITOR_HEIGHT - 1,
                x + SIGNAL_SOURCE_EDITOR_WIDTH, y + SIGNAL_SOURCE_EDITOR_HEIGHT, 0xFF111111);
        drawSignalSourceEditorButton(guiGraphics, x + 2, y + 2, "−",
                isInside(mouseX, mouseY, x + 2, y + 2, 12, 14));
        drawSignalSourceEditorButton(guiGraphics, x + 58, y + 2, "+",
                isInside(mouseX, mouseY, x + 58, y + 2, 12, 14));

        int sliderX = signalSliderStartX();
        int sliderY = y + 8;
        int sliderWidth = signalSliderWidth();
        guiGraphics.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 2, 0xFF444444);
        int progress = sliderX + Math.round((sliderWidth - 1) * editingSignalSourceValue / 15.0f);
        guiGraphics.fill(sliderX, sliderY, progress + 1, sliderY + 2, 0xFF55AAFF);
        for (int value = 0; value <= 15; value++) {
            int tickX = sliderX + Math.round((sliderWidth - 1) * value / 15.0f);
            guiGraphics.fill(tickX, sliderY - 3, tickX + 1, sliderY + 5, 0xFFCCCCCC);
        }
        guiGraphics.fill(progress - 2, sliderY - 4, progress + 3, sliderY + 6, 0xFFFFFFFF);
    }

    private void drawSignalSourceEditorButton(GuiGraphics guiGraphics, int x, int y, String label, boolean hovered) {
        int baseColor = hovered ? 0xFF777777 : 0xFF3A3A3A;
        guiGraphics.fill(x, y, x + 12, y + 14, baseColor);
        guiGraphics.fill(x, y, x + 12, y + 1, 0xFFAAAAAA);
        guiGraphics.fill(x, y + 13, x + 12, y + 14, 0xFF111111);
        guiGraphics.drawCenteredString(font, label, x + 6, y + 3, 0xFFFFFFFF);
    }

    /**
     * Renders custom tooltips for redstone components when hovering over grid slots.
     * Shows component name, rotation direction, signal strength, and lever state.
     * Reads from the server-synced circuit state (logic layer) — no inventory access or propagation.
     */
    private void renderRedstoneTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        RedstoneCircuitState state = getCircuitState();
        if (state == null || editingSignalSourceSlot >= 0) return;

        int gridStartX = leftPos + 8;
        int gridStartY = topPos + 18;

        for (int slotIndex = 0; slotIndex < RedstoneCircuit.GRID_SIZE; slotIndex++) {
            int col = slotIndex % RedstoneCircuit.GRID_WIDTH;
            int row = slotIndex / RedstoneCircuit.GRID_WIDTH;

            int x = gridStartX + col * 18;
            int y = gridStartY + row * 18;

            // Check if mouse is over this slot
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                RedstoneComponent component = state.getComponent(slotIndex);
                if (component != null) {
                    List<Component> tooltipLines = new ArrayList<>();

                    // Line 1: Component name with color based on component type
                    ChatFormatting nameColor = getComponentChatFormatting(component);
                    tooltipLines.add(Component.literal(component.getDisplayName()).withStyle(nameColor));

                    // Line 2: Rotation direction (for directional components)
                    if (component == RedstoneComponent.REPEATER || component == RedstoneComponent.COMPARATOR) {
                        int rotation = state.getRotation(slotIndex);
                        String direction = getDirectionName(rotation);
                        tooltipLines.add(Component.literal("Facing: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(direction).withStyle(ChatFormatting.WHITE)));
                    } else if (component == RedstoneComponent.TORCH) {
                        int torchRot = state.getTorchRotation(slotIndex);
                        if (torchRot == 4) {
                            tooltipLines.add(Component.literal("State: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal("Standing (no wall)").withStyle(ChatFormatting.WHITE)));
                        } else {
                            String direction = getDirectionName(torchRot);
                            // Check if the torch is attached to a border wall
                            int tx = slotIndex % RedstoneCircuit.GRID_WIDTH;
                            int ty = slotIndex / RedstoneCircuit.GRID_WIDTH;
                            boolean isBorder = (torchRot == 0 && ty == 0) || (torchRot == 2 && ty == RedstoneCircuit.GRID_HEIGHT - 1) ||
                                              (torchRot == 1 && tx == RedstoneCircuit.GRID_WIDTH - 1) || (torchRot == 3 && tx == 0);
                            if (isBorder) {
                                tooltipLines.add(Component.literal("Attached: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(direction + " (border)").withStyle(ChatFormatting.WHITE)));
                            } else {
                                tooltipLines.add(Component.literal("Attached: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(direction).withStyle(ChatFormatting.WHITE)));
                            }
                        }
                        tooltipLines.add(Component.literal("(Config Mode: Right-click to rotate)").withStyle(ChatFormatting.DARK_GRAY));
                    } else if (component == RedstoneComponent.OBSERVER) {
                        int rotation = state.getRotation(slotIndex);
                        tooltipLines.add(Component.literal("Watches: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(getDirectionName(rotation)).withStyle(ChatFormatting.WHITE)));
                        tooltipLines.add(Component.literal("Output: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(getDirectionName((rotation + 2) % 4))
                                        .withStyle(ChatFormatting.WHITE)));
                        tooltipLines.add(Component.literal("(Config Mode: Right-click to rotate)")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    } else if (component == RedstoneComponent.LEVER) {
                        int leverRot = state.getLeverRotation(slotIndex);
                        if (leverRot == 4) {
                            tooltipLines.add(Component.literal("Attachment: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal("Standing (no wall)").withStyle(ChatFormatting.WHITE)));
                        } else {
                            String direction = getDirectionName(leverRot);
                            // Check if the lever is attached to a border wall
                            int tx = slotIndex % RedstoneCircuit.GRID_WIDTH;
                            int ty = slotIndex / RedstoneCircuit.GRID_WIDTH;
                            boolean isBorder = (leverRot == 0 && ty == 0) || (leverRot == 2 && ty == RedstoneCircuit.GRID_HEIGHT - 1) ||
                                              (leverRot == 1 && tx == RedstoneCircuit.GRID_WIDTH - 1) || (leverRot == 3 && tx == 0);
                            if (isBorder) {
                                tooltipLines.add(Component.literal("Attachment: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(direction + " (border)").withStyle(ChatFormatting.WHITE)));
                            } else {
                                tooltipLines.add(Component.literal("Attachment: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(direction).withStyle(ChatFormatting.WHITE)));
                            }
                        }
                        tooltipLines.add(Component.literal("(Config Mode: Left-click toggle, Right-click cycle attachment)").withStyle(ChatFormatting.DARK_GRAY));
                    } else if (component == RedstoneComponent.BUTTON) {
                        int duration = state.getButtonDurationGameTicks(slotIndex);
                        tooltipLines.add(Component.literal("Pulse: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(duration + " game ticks (" + duration / 2 + " redstone ticks)")
                                        .withStyle(ChatFormatting.WHITE)));
                        int buttonRot = state.getButtonRotation(slotIndex);
                        if (buttonRot == 4) {
                            tooltipLines.add(Component.literal("Attachment: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal("Floor-perched").withStyle(ChatFormatting.WHITE)));
                        } else {
                            String direction = getDirectionName(buttonRot);
                            int tx = slotIndex % RedstoneCircuit.GRID_WIDTH;
                            int ty = slotIndex / RedstoneCircuit.GRID_WIDTH;
                            boolean isBorder = (buttonRot == 0 && ty == 0) ||
                                    (buttonRot == 2 && ty == RedstoneCircuit.GRID_HEIGHT - 1) ||
                                    (buttonRot == 1 && tx == RedstoneCircuit.GRID_WIDTH - 1) ||
                                    (buttonRot == 3 && tx == 0);
                            tooltipLines.add(Component.literal("Attachment: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(direction + (isBorder ? " (border)" : ""))
                                            .withStyle(ChatFormatting.WHITE)));
                        }
                        tooltipLines.add(Component.literal("(Config Mode: Left-click press, Right-click cycle attachment)")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    } else if (component == RedstoneComponent.SIGNAL_SOURCE) {
                        int configuredStrength = state.getSignalSourceStrength(slotIndex);
                        tooltipLines.add(Component.literal("Configured output: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(configuredStrength + " / 15")
                                        .withStyle(configuredStrength > 0 ? ChatFormatting.AQUA : ChatFormatting.WHITE)));
                        tooltipLines.add(Component.literal("Config Mode: Left-click to set strength")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }

                    // Line 3: Signal strength or power
                    int signalStrength = state.getSignalStrength(slotIndex);
                    if (component == RedstoneComponent.LEVER) {
                        boolean on = state.getLeverState(slotIndex);
                        tooltipLines.add(Component.literal("State: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(on ? "ON (15)" : "OFF (0)")
                                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)));
                    } else if (component == RedstoneComponent.BUTTON) {
                        boolean on = signalStrength > 0;
                        tooltipLines.add(Component.literal("State: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(on ? "PRESSED (15)" : "Released (0)")
                                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)));
                    } else if (component == RedstoneComponent.TORCH) {
                        // Torch is a NOT gate — show ON/OFF state
                        int torchRot = state.getTorchRotation(slotIndex);
                        if (torchRot == 4) {
                            tooltipLines.add(Component.literal("State: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal("ON (15) - Standing").withStyle(ChatFormatting.GREEN)));
                            tooltipLines.add(Component.literal("(Acts like redstone block)").withStyle(ChatFormatting.DARK_GRAY));
                        } else {
                            boolean torchOn = signalStrength > 0;
                            tooltipLines.add(Component.literal("State: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(torchOn ? "ON (15)" : "OFF (0)")
                                            .withStyle(torchOn ? ChatFormatting.GREEN : ChatFormatting.RED)));
                            tooltipLines.add(Component.literal("(Inverts the signal at its support block)").withStyle(ChatFormatting.DARK_GRAY));
                        }
                    } else if (component == RedstoneComponent.WALL) {
                        tooltipLines.add(Component.literal("Powered: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(signalStrength + " / 15")
                                        .withStyle(signalStrength > 0 ? ChatFormatting.GREEN : ChatFormatting.WHITE)));
                    } else if (component == RedstoneComponent.OBSERVER) {
                        int output = state.getOutputSignal(slotIndex);
                        tooltipLines.add(Component.literal("State: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(output > 0 ? "PULSING (15)" : "Idle (0)")
                                        .withStyle(output > 0 ? ChatFormatting.RED : ChatFormatting.WHITE)));
                        tooltipLines.add(Component.literal("Pulse: 2 game ticks after a detected change")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    } else if (component == RedstoneComponent.REPEATER || component == RedstoneComponent.COMPARATOR) {
                        // Repeaters/comparators are pass-through — show input and output
                        int rotation = state.getRotation(slotIndex);
                        int inputStrength = state.getInputStrength(slotIndex);
                        String direction = getDirectionName(rotation);
                        if (component == RedstoneComponent.REPEATER) {
                            int delay = state.getRepeaterDelay(slotIndex);
                            tooltipLines.add(Component.literal("Delay: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(delay + " redstone tick" + (delay == 1 ? "" : "s") +
                                            " (" + delay * 2 + " game ticks)").withStyle(ChatFormatting.WHITE)));
                            tooltipLines.add(Component.literal("(Config Mode: Left-click delay, Right-click rotate)")
                                    .withStyle(ChatFormatting.DARK_GRAY));
                            if (state.isRepeaterLocked(slotIndex)) {
                                // Locked repeater: show frozen output state
                                tooltipLines.add(Component.literal("State: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(state.getRepeaterLockOutput(slotIndex) ? "LOCKED ON" : "LOCKED OFF")
                                                .withStyle(state.getRepeaterLockOutput(slotIndex) ? ChatFormatting.GREEN : ChatFormatting.RED)));
                                tooltipLines.add(Component.literal("Output: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal((state.getRepeaterLockOutput(slotIndex) ? 15 : 0) + " / 15 → " + direction)
                                                .withStyle(state.getRepeaterLockOutput(slotIndex) ? ChatFormatting.GREEN : ChatFormatting.RED)));
                                tooltipLines.add(Component.literal("(Frozen by side power)").withStyle(ChatFormatting.DARK_GRAY));
                            } else {
                                tooltipLines.add(Component.literal("Input: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(inputStrength + " / 15").withStyle(ChatFormatting.WHITE)));
                                int output = state.getOutputSignal(slotIndex);
                                tooltipLines.add(Component.literal("Output: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(output + " / 15 → " + direction)
                                                .withStyle(output > 0 ? ChatFormatting.GREEN : ChatFormatting.RED)));
                            }
                        } else {
                            // Comparator: show mode, input, side inputs, and output
                            boolean subtractMode = state.getComparatorSubtract(slotIndex);
                            tooltipLines.add(Component.literal("Mode: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(subtractMode ? "Subtract" : "Compare")
                                            .withStyle(subtractMode ? ChatFormatting.DARK_RED : ChatFormatting.BLUE)));
                            tooltipLines.add(Component.literal("Input: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(inputStrength + " / 15").withStyle(ChatFormatting.WHITE)));
                            int compOutput = state.getOutputSignal(slotIndex);
                            tooltipLines.add(Component.literal("Output: ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(compOutput + " / 15 → " + direction)
                                            .withStyle(ChatFormatting.BLUE)));
                            tooltipLines.add(Component.literal("(Config Mode: Left-click toggle mode, Right-click rotate)").withStyle(ChatFormatting.DARK_GRAY));
                        }
                    } else if (component != RedstoneComponent.SIGNAL_SOURCE) {
                        tooltipLines.add(Component.literal("Signal: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(signalStrength + " / 15").withStyle(ChatFormatting.WHITE)));
                    }
                    // WALL tooltip is handled above in the WALL case


                    // Line 5: Grid position
                    tooltipLines.add(Component.literal("Position: (" + col + ", " + row + ")").withStyle(ChatFormatting.DARK_GRAY));

                    guiGraphics.renderComponentTooltip(minecraft.font, tooltipLines, mouseX, mouseY);
                    return; // Only show one tooltip at a time
                }
            }
        }
    }

    /**
     * Maps a component type to a ChatFormatting color for tooltip display.
     */
    private ChatFormatting getComponentChatFormatting(RedstoneComponent component) {
        return switch (component) {
            case WIRE -> ChatFormatting.RED;
            case REPEATER -> ChatFormatting.GREEN;
            case COMPARATOR -> ChatFormatting.BLUE;
            case TORCH -> ChatFormatting.YELLOW;
            case LEVER -> ChatFormatting.GOLD;
            case BUTTON -> ChatFormatting.GOLD;
            case OBSERVER -> ChatFormatting.DARK_RED;
            case SIGNAL_SOURCE -> ChatFormatting.AQUA;
            case REDSTONE_BLOCK -> ChatFormatting.DARK_RED;
            case WALL -> ChatFormatting.GRAY;
        };
    }

    /**
     * Converts a rotation index (0-3) to a direction name.
     */
    private String getDirectionName(int rotation) {
        return switch (rotation % 4) {
            case 0 -> "North ↑";
            case 1 -> "East →";
            case 2 -> "South ↓";
            case 3 -> "West ←";
            default -> "North ↑";
        };
    }

}
