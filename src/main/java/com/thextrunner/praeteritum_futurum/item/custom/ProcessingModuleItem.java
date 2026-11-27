package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.api.module.ProcessingMode;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.network.ServerboundSetModuleConfigItemPacket;
import com.thextrunner.praeteritum_futurum.network.ServerboundToggleModuleDirectionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all processing modules — modules that transform items over time.
 * <p>
 * Provides the framework for modules like Smelting, Blast, Smoker, Stonecutter,
 * Composter, Grindstone, Cartography, Anvil, Brewing, and Jukebox. Each subclass
 * implements {@link #processModule} to define its specific processing logic.
 * <p>
 * Key features:
 * <ul>
 *   <li>Direction button system: U/D/L/R buttons cycle through slot modes (INPUT, OUTPUT, FUEL, etc.)</li>
 *   <li>Progress bar rendering: shows processing progress in the GUI</li>
 *   <li>Config slot support: optional center slot for configuration items (used by Brewing, Cartography)</li>
 *   <li>Progress persistence: saves burn/cook progress to NBT when modules are removed</li>
 *   <li>Slot mode limits: enforces max input/output counts per direction</li>
 * </ul>
 * <p>
 * Each subclass defines its own mode enum implementing {@link ProcessingMode}, and declares
 * the supported modes via {@link #getSupportedModes()}.
 * <p>
 * Third-party modders should extend this class for any module that transforms items
 * over time. Override {@link #processModule} for the core logic,
 * {@link #getSupportedModes} for the direction button cycle, and
 * {@link #getMaxInputs}/{@link #getMaxOutputs} for slot limits.
 */
public abstract class ProcessingModuleItem extends BaseModuleItem {

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        float progress = getProgressPercent(stack, blockEntity, slot.index);
        if (progress > 0.0F) {
            int barHeight = (int) (progress * 14.0F);
            graphics.fill(slot.x + 14, slot.y + 1, slot.x + 16, slot.y + 15, 0xFF333333);
            graphics.fill(slot.x + 14, slot.y + 15 - barHeight, slot.x + 16, slot.y + 15, 0xFF33CC33);
        }
    }

    @Override
    public boolean isInsideOverlay(double mouseX, double mouseY, int slotX, int slotY) {
        boolean center = isWithinBounds(mouseX, mouseY, slotX, slotY, 16, 16);
        boolean up = isWithinBounds(mouseX, mouseY, slotX, slotY - 18, 16, 16);
        // Extended downward to cover the inventory overlap segment
        boolean down = isWithinBounds(mouseX, mouseY, slotX, slotY + 18, 16, 20);
        boolean west = isWithinBounds(mouseX, mouseY, slotX - 18, slotY, 16, 16);
        boolean east = isWithinBounds(mouseX, mouseY, slotX + 18, slotY, 16, 16);
        boolean close = isWithinBounds(mouseX, mouseY, slotX - 18, slotY - 18, 16, 16);

        return center || up || down || west || east || close;
    }


    /**
     * Calculates the progress percentage for rendering custom progress bars.
     *
     * @param stack       The module stack instance, reserved for reading stored progress or energy data
     * @param blockEntity The parent machine framework block entity
     * @param slotIndex   The inventory index of the module slot
     * @return A float value between 0.0 and 1.0 representing progress completion
     */
    public float getProgressPercent(ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        return 0.0F;
    }

    /**
     * Constructs a ProcessingModuleItem with the given properties.
     * <p>
     * Third-party modders should extend this class for modules that transform items
     * over time (e.g., smelting, brewing, cartography). Override {@link #processModule(Level, BlockPos, MachineFrameworkBlockEntity, int, ItemStack)}
     * to define the processing logic.
     *
     * @param properties The item properties (stack size, rarity, etc.)
     */
    public ProcessingModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean supportsRedstoneControl() {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull net.minecraft.world.item.Item.TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, getModuleConfigKey());
        boolean configured = false;
        for (ProcessingMode mode : modes.values()) {
            if (mode.getRole() != ProcessingMode.Role.DISABLED) {
                configured = true;
                break;
            }
        }
        if (configured) {
            tooltipComponents.add(Component.translatable("tooltip.praeteritum_futurum.configured").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public abstract String getModuleConfigKey();

    public boolean hasConfigSlot() {
        return false;
    }

    /**
     * Returns the maximum number of input slots this module can have configured.
     * Override in subclasses to limit input slot count.
     *
     * @return The maximum number of input slots (default: no limit)
     */
    public int getMaxInputs() {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns the maximum number of output slots this module can have configured.
     * Override in subclasses to limit output slot count.
     *
     * @return The maximum number of output slots (default: no limit)
     */
    public int getMaxOutputs() {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns the list of modes this module supports in its direction-button cycle.
     * <p>
     * Subclasses that use processing direction buttons declare their local modes here.
     * The DISABLED mode must be first, followed by the modes in the desired cycle order.
     * Modules with a separate configuration model can leave the list empty.
     *
     * @return An array of supported modes in cycle order (DISABLED should be first)
     */
    public ProcessingMode[] getSupportedModes() {
        return new ProcessingMode[0];
    }

    private ProcessingMode getDisabledMode() {
        ProcessingMode[] modes = getSupportedModes();
        if (modes.length == 0 || modes[0].getRole() != ProcessingMode.Role.DISABLED) {
            throw new IllegalStateException(getClass().getSimpleName() + " must list its DISABLED mode first");
        }
        return modes[0];
    }

    /**
     * Returns the maximum count for a given slot mode.
     * <p>
     * Used by {@link #getCycledMode(ItemStack, GridDirection, boolean)} to enforce slot limits.
     * The shared framework limits input and output roles; subclasses can override this method
     * to enforce limits for their own module-specific modes.
     *
     * @param mode The slot mode to get the max count for
     * @return The maximum number of slots that can be configured to this mode
     */
    protected int getMaxForMode(ProcessingMode mode) {
        if (mode.getRole() == ProcessingMode.Role.INPUT) return getMaxInputs();
        if (mode.getRole() == ProcessingMode.Role.OUTPUT) return getMaxOutputs();
        return Integer.MAX_VALUE;
    }

    /**
     * Counts how many slots are currently configured as INPUT or OUTPUT
     * in the given module stack's slot mode map.
     *
     * @param stack  The module item stack
     * @param mode   The slot mode to count (INPUT or OUTPUT)
     * @return The number of directions currently set to the given mode
     */
    protected int countSlotMode(ItemStack stack, ProcessingMode mode) {
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, getModuleConfigKey());
        int count = 0;
        for (ProcessingMode m : modes.values()) {
            if (m == mode) count++;
        }
        return count;
    }

    @Override
    public java.util.List<BaseModuleItem.AdjacentModuleType> getAllowedAdjacentModuleTypes() {
        return java.util.List.of(BaseModuleItem.AdjacentModuleType.INVENTORY);
    }

    @Override
    public boolean isDirectionConfigured(ItemStack stack, GridDirection dir) {
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, getModuleConfigKey());
        return modes.get(dir).getRole() != ProcessingMode.Role.DISABLED;
    }

    @Override
    public void resetDirection(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        clearDirectionMode(stack, dir);
    }

    @Override
    public void resetDirectionMode(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        clearDirectionMode(stack, dir);
    }

    private void clearDirectionMode(ItemStack stack, GridDirection dir) {
        setSlotMode(stack, dir, getDisabledMode(), getModuleConfigKey());
    }

    @Override
    public void handleCustomClick(double mouseX, double mouseY, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex, net.minecraft.client.Minecraft minecraft) {
        // Validate config — auto-disable directions that are no longer valid
        validateAndSyncDirectionConfig(stack, blockEntity, slotIndex);

        // 1. Center config ghost slot click
        if (hasConfigSlot() && mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
            if (minecraft.player != null) {
                ItemStack carried = minecraft.player.containerMenu.getCarried();
                ItemStack ghostCopy = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);

                // Check if the module accepts this item for its config slot
                if (!ghostCopy.isEmpty() && !acceptsConfigSlotItem(ghostCopy)) {
                    return;
                }

                assert blockEntity.getLevel() != null;
                setConfigSlotItem(stack, ghostCopy, blockEntity.getLevel().registryAccess());
                PacketDistributor.sendToServer(new ServerboundSetModuleConfigItemPacket(slotIndex, ghostCopy, blockEntity.getBlockPos()));
            }
            return;
        }

        GridDirection clickedDir = getClickedDirection(slotX, slotY, mouseX, mouseY, slotIndex);
        if (clickedDir != null) {
            // Check if the adjacent module type is allowed for configuration
            BaseModuleItem.AdjacentModuleType adjType = BaseModuleItem.getAdjacentModuleType(blockEntity, slotIndex, clickedDir);
            if (!isAdjacentModuleAllowed(adjType)) {
                return; // No relevant adjacent module — ignore click
            }

            if (minecraft.player != null) {
                minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
            }
            // Client-side preview — server will cycle the actual mode
            ProcessingMode nextMode = getCycledMode(stack, clickedDir, isShiftHeld());
            setSlotMode(stack, clickedDir, nextMode, getModuleConfigKey());

            PacketDistributor.sendToServer(new ServerboundToggleModuleDirectionPacket(slotIndex, clickedDir, blockEntity.getBlockPos(), isShiftHeld()));
        }
    }

    @Override
    public boolean opensInlineOverlay() {
        return true; // Processing modules use inline overlays
    }

    public ItemStack getConfigSlotItem(ItemStack moduleStack, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /**
     * Retrieves the item stack stored in the module configuration slot.

     * @ param moduleStack The module stack containing configuration data
     * @ param registries  The holder lookup registries for registry access
     * @ return The configured item stack
     */
    public void setConfigSlotItem(ItemStack moduleStack, ItemStack target, HolderLookup.Provider registries) {}

    /**
     * Checks whether the given item is accepted by this module's config slot.
     * Override in subclasses to restrict which items can be placed in the config slot.
     *
     * @param item The item to check
     * @return true if the item is accepted, false otherwise
     */
    public boolean acceptsConfigSlotItem(ItemStack item) {
        return true;
    }

    /**
     * Controls whether the MachineInfoScreen should handle the center config slot click
     * with the default item-pickup behavior. Override to return false if the module
     * handles the config slot click itself (e.g., opening a custom text input screen).
     *
     * @return true to use the default item-pickup behavior, false to handle clicks in handleCustomClick
     */
    public boolean useDefaultConfigSlotClick() {
        return true;
    }

    public Map<GridDirection, ProcessingMode> getSlotModes(ItemStack stack) {
        return getSlotModes(stack, getModuleConfigKey());
    }

    public Map<GridDirection, ProcessingMode> getSlotModes(ItemStack stack, String nbtKey) {
        ProcessingMode[] supportedModes = getSupportedModes();
        if (supportedModes.length == 0) {
            return Map.of();
        }
        ProcessingMode disabledMode = getDisabledMode();
        Map<GridDirection, ProcessingMode> modes = new EnumMap<>(GridDirection.class);
        for (GridDirection dir : GridDirection.VALUES) {
            modes.put(dir, disabledMode);
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(nbtKey)) {
                CompoundTag modesTag = tag.getCompound(nbtKey);
                // Iterate over NBT keys and parse GridDirection from them
                for (String key : modesTag.getAllKeys()) {
                    GridDirection dir = GridDirection.fromNbtKey(key);
                    if (dir != null) {
                        String modeName = modesTag.getString(key);
                        for (ProcessingMode mode : supportedModes) {
                            if (mode.getName().equals(modeName)) {
                                modes.put(dir, mode);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return modes;
    }

    @Override
    public CompoundTag getBlueprintData(ItemStack stack) {
        CompoundTag tag = super.getBlueprintData(stack);
        if (tag != null) {
            // Strip the generic runtime progress channel container so blueprints
            // never clone active processing states, leaving channel interpretation
            // entirely to the respective module implementations.
            tag.remove("ModuleChannels");
        }
        return tag;
    }

    /**
     * Returns the next or previous slot mode when a direction button is clicked.
     * <p>
     * Iterates through {@link #getSupportedModes()} in the direction determined by {@code reverse},
     * enforcing the per-mode slot limits via {@link #getMaxForMode(ProcessingMode)}.
     * Returns the module's disabled mode when the cycle wraps back to the beginning.
     * <p>
     * Subclasses with module-specific slot constraints can override
     * {@link #getMaxForMode(ProcessingMode)}.
     *
     * @param stack       The module item stack
     * @param clickedDir  The grid direction whose button was clicked
     * @param reverse     If true, cycle in reverse order (shift-click behavior)
     * @return The next or previous slot mode in the cycle
     */
    public ProcessingMode getCycledMode(ItemStack stack, GridDirection clickedDir, boolean reverse) {
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, getModuleConfigKey());
        ProcessingMode current = modes.get(clickedDir);
        ProcessingMode[] supported = getSupportedModes();

        // Find the current mode's index in the supported array
        int currentIndex = 0;
        for (int i = 0; i < supported.length; i++) {
            if (supported[i] == current) {
                currentIndex = i;
                break;
            }
        }

        // Iterate through supported modes in the appropriate direction
        for (int i = 1; i <= supported.length; i++) {
            int step = reverse ? -i : i;
            int nextIndex = (currentIndex + step + supported.length) % supported.length;
            ProcessingMode next = supported[nextIndex];

            if (next.getRole() == ProcessingMode.Role.DISABLED) {
                return next;
            }

            // Check if we've reached the max for this mode
            int currentCount = countSlotMode(stack, next);
            if (current == next) {
                currentCount--; // exclude the clicked direction itself
            }

            int max = getMaxForMode(next);
            if (currentCount >= max) {
                continue; // skip this mode, limit reached
            }

            return next;
        }
        return getDisabledMode();
    }

    @Override
    public void toggleDirectionMode(ItemStack stack, GridDirection direction, MachineFrameworkBlockEntity blockEntity, int slotIndex, boolean shiftHeld) {
        // Only cycle mode if an adjacent Inventory module is present
        AdjacentModuleType adjType = getAdjacentModuleType(blockEntity, slotIndex, direction);
        if (!isAdjacentModuleAllowed(adjType)) {
            return;
        }
        ProcessingMode nextMode = getCycledMode(stack, direction, shiftHeld);
        setSlotMode(stack, direction, nextMode, getModuleConfigKey());
    }

    public static void setSlotMode(ItemStack stack, GridDirection dir, ProcessingMode mode, String nbtKey) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        CompoundTag modesTag = tag.contains(nbtKey) ? tag.getCompound(nbtKey) : new CompoundTag();

        modesTag.putString(dir.nbtKey(), mode.getName());
        tag.put(nbtKey, modesTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void renderCustomOverlay(GuiGraphics graphics, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, net.minecraft.client.gui.Font font) {
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, getModuleConfigKey());
        drawDirectionButtons(graphics, font, slotX, slotY, blockEntity, modes);

        if (hasConfigSlot()) {
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF888888);
            assert blockEntity.getLevel() != null;
            ItemStack targetItem = getConfigSlotItem(stack, blockEntity.getLevel().registryAccess());
            if (!targetItem.isEmpty()) {
                graphics.renderItem(targetItem, slotX, slotY);
                graphics.renderItemDecorations(font, targetItem, slotX, slotY);
            }
        } else if (!usesCenterSlot()) {
            drawDarkHatchLid(graphics, slotX, slotY);
        }

        drawCloseButton(graphics, font, slotX, slotY);
    }

    @Override
    public List<Component> getCustomOverlayTooltip(double mouseX, double mouseY, int slotX, int slotY,
                                                   ItemStack stack, MachineFrameworkBlockEntity blockEntity,
                                                   int slotIndex) {
        GridDirection direction = getClickedDirection(slotX, slotY, mouseX, mouseY, slotIndex);
        if (direction != null) {
            AdjacentModuleType adjacentType = getAdjacentModuleType(blockEntity, slotIndex, direction);
            boolean allowed = isAdjacentModuleAllowed(adjacentType);
            ProcessingMode mode = allowed
                    ? getSlotModes(stack, getModuleConfigKey()).get(direction)
                    : getDisabledMode();

            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(direction.name() + " slot").withStyle(ChatFormatting.AQUA));
            lines.add(tooltipValue("Mode: ", mode.getName(), mode.getColor()));
            lines.add(Component.literal(mode.getDescription()).withStyle(ChatFormatting.GRAY));
            if (allowed) {
                lines.add(tooltipHint("Click to cycle; Shift-click to cycle backward"));
            } else {
                lines.add(Component.literal("Requires an adjacent compatible module").withStyle(ChatFormatting.RED));
            }
            return lines;
        }

        if (hasConfigSlot() && mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
            return List.of(
                    Component.literal("Target item").withStyle(ChatFormatting.AQUA),
                    Component.literal("Click with an item on your cursor to select it.").withStyle(ChatFormatting.GRAY),
                    tooltipHint("Click with an empty cursor to clear it")
            );
        }

        return super.getCustomOverlayTooltip(mouseX, mouseY, slotX, slotY, stack, blockEntity, slotIndex);
    }


    /**
     * Draws the 4 direction buttons (U/D/L/R) around a module slot.
     * If the overlay slot index is known, only draws buttons for directions with adjacent slots.
     * Subclasses like AnvilModuleItem can call this instead of duplicating the pattern.
     */
    @SuppressWarnings("unused")
    protected void drawDirectionButtons(GuiGraphics graphics, net.minecraft.client.gui.Font font, int slotX, int slotY,
                                        MachineFrameworkBlockEntity blockEntity, Map<GridDirection, ProcessingMode> modes) {
        int centerX = slotX + 8;
        int centerY = slotY + 8;
        int offset = 18;

        int slotIndex = getOverlaySlotIndex();
        if (slotIndex >= 0) {
            if (hasAdjacentSlot(slotIndex, GridDirection.UP))
                drawProcessingButtonWithCheck(graphics, font, slotIndex, blockEntity, centerX, slotY - (offset - 8), GridDirection.UP, modes.get(GridDirection.UP));
            if (hasAdjacentSlot(slotIndex, GridDirection.LEFT))
                drawProcessingButtonWithCheck(graphics, font, slotIndex, blockEntity, slotX - (offset - 8), centerY, GridDirection.LEFT, modes.get(GridDirection.LEFT));
            if (hasAdjacentSlot(slotIndex, GridDirection.RIGHT))
                drawProcessingButtonWithCheck(graphics, font, slotIndex, blockEntity, slotX + 16 + (offset - 8), centerY, GridDirection.RIGHT, modes.get(GridDirection.RIGHT));
            if (hasAdjacentSlot(slotIndex, GridDirection.DOWN))
                drawProcessingButtonWithCheck(graphics, font, slotIndex, blockEntity, centerX, slotY + 16 + (offset - 8), GridDirection.DOWN, modes.get(GridDirection.DOWN));
        } else {
            // Fallback: render all buttons if slot index is unknown (e.g., third-party overlay)
            drawProcessingButton(graphics, font, centerX, slotY - (offset - 8), modes.get(GridDirection.UP));
            drawProcessingButton(graphics, font, slotX - (offset - 8), centerY, modes.get(GridDirection.LEFT));
            drawProcessingButton(graphics, font, slotX + 16 + (offset - 8), centerY, modes.get(GridDirection.RIGHT));
            drawProcessingButton(graphics, font, centerX, slotY + 16 + (offset - 8), modes.get(GridDirection.DOWN));
        }
    }

    protected void drawProcessingButton(GuiGraphics graphics, net.minecraft.client.gui.Font font, int x, int y, ProcessingMode mode) {
        graphics.fill(x - 8, y - 8, x + 8, y + 8, 0xFF888888);
        graphics.fill(x - 7, y - 7, x + 7, y + 7, mode.getColor());

        // Displays e.g., "T" or "F"
        graphics.drawCenteredString(font, mode.getLabelChar(), x, y - 4, 0xFFFFFF);
    }

    /**
     * Draws a processing button that checks if the adjacent module type is allowed.
     * If no allowed module is adjacent, renders the button in disabled state.
     */
    protected void drawProcessingButtonWithCheck(GuiGraphics graphics, net.minecraft.client.gui.Font font, int slotIndex, MachineFrameworkBlockEntity blockEntity, int x, int y, GridDirection dir, ProcessingMode mode) {
        BaseModuleItem.AdjacentModuleType adjType = BaseModuleItem.getAdjacentModuleType(blockEntity, slotIndex, dir);
        boolean allowed = isAdjacentModuleAllowed(adjType);

        graphics.fill(x - 8, y - 8, x + 8, y + 8, 0xFF888888);
        if (allowed) {
            graphics.fill(x - 7, y - 7, x + 7, y + 7, mode.getColor());
            graphics.drawCenteredString(font, mode.getLabelChar(), x, y - 4, 0xFFFFFF);
        } else {
            // No relevant adjacent module — show disabled
            ProcessingMode disabledMode = getDisabledMode();
            graphics.fill(x - 7, y - 7, x + 7, y + 7, disabledMode.getColor());
            graphics.drawCenteredString(font, disabledMode.getLabelChar(), x, y - 4, 0xFFAAAAAA);
        }
    }

    // --- STANDARDIZED GRID NEIGHBOR RESOLVER ---
    public record ResolvedIoSlots(List<Integer> inputs, List<Integer> outputs) {}

    public static ResolvedIoSlots resolveAdjacentIoSlots(MachineFrameworkBlockEntity blockEntity, int slotIndex, Map<GridDirection, ProcessingMode> modes) {
        ItemStackHandler moduleHandler = blockEntity.getModuleHandler();
        List<Integer> inputs = new ArrayList<>();
        List<Integer> outputs = new ArrayList<>();

        for (GridDirection dir : GridDirection.VALUES) {
            int adjSlot = dir.getAdjacentSlotIndex(slotIndex);
            if (adjSlot < 0) continue;

            ItemStack adjModule = moduleHandler.getStackInSlot(adjSlot);
            if (!adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                continue;
            }

            ProcessingMode mode = modes.get(dir);
            if (mode.getRole() == ProcessingMode.Role.INPUT) {
                inputs.add(adjSlot);
            } else if (mode.getRole() == ProcessingMode.Role.OUTPUT) {
                outputs.add(adjSlot);
            }
        }

        return new ResolvedIoSlots(inputs, outputs);
    }

    @Override
    public void tickModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        // Restore from saved NBT when channels 0 and 2 are zero, but skip if a stalled disc
        // is held (channel 4 > 0) — restoring would wipe the stalled flag before the stall
        // handler has a chance to run. The module grid slot (moduleHandler) always contains
        // the module item itself, so we cannot use slot emptiness as a guard.
        int stalled = blockEntity.getModuleData(slotIndex, 4);
        if (blockEntity.getModuleData(slotIndex, 2) == 0 && blockEntity.getModuleData(slotIndex, 0) == 0 && stalled == 0) {
            if (stack.has(DataComponents.CUSTOM_DATA)) {
                CustomData customDataComp = stack.get(DataComponents.CUSTOM_DATA);
                if (customDataComp != null) {
                    CompoundTag tag = customDataComp.copyTag();
                    if (tag.contains("ModuleChannels")) {
                        int[] channels = tag.getIntArray("ModuleChannels");
                        for (int i = 0; i < Math.min(channels.length, 6); i++) {
                            blockEntity.setModuleData(slotIndex, i, channels[i]);
                        }
                    }
                }
            }
        }
        processModule(level, pos, blockEntity, slotIndex, stack);
    }

    /**
     * Executes core ticking logic for processing modules.
     *
     * @param level       The world level instance
     * @param pos         The block position of the machine framework
     * @param blockEntity The parent machine framework block entity
     * @param slotIndex   The inventory index of the module slot
     * @param stack       The module stack instance
     */
    protected abstract void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack);

    public static void saveProgressData(MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        int burnTime = blockEntity.getModuleData(slotIndex, 0);
        int cookTime = blockEntity.getModuleData(slotIndex, 2);
        int stalled = blockEntity.getModuleData(slotIndex, 4);

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();

        if (burnTime > 0 || cookTime > 0 || stalled > 0) {
            int[] channels = new int[6];
            for (int i = 0; i < 6; i++) {
                channels[i] = blockEntity.getModuleData(slotIndex, i);
            }
            tag.putIntArray("ModuleChannels", channels);
        } else {
            tag.remove("ModuleChannels");
        }

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static void clearProgressData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            tag.remove("ModuleChannels");
            if (tag.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }
}