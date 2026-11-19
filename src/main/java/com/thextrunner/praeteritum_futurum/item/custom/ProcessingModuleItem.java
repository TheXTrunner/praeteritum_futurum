package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.api.module.CyclingMode;
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
        boolean center = mouseX >= slotX && mouseX <= slotX + 16 && mouseY >= slotY && mouseY <= slotY + 16;
        boolean up = mouseX >= slotX && mouseX <= slotX + 16 && mouseY >= slotY - 18 && mouseY <= slotY - 2;
        // Extended downward to cover the inventory overlap segment
        boolean down = mouseX >= slotX && mouseX <= slotX + 16 && mouseY >= slotY + 18 && mouseY <= slotY + 38;
        boolean west = mouseX >= slotX - 18 && mouseX <= slotX - 2 && mouseY >= slotY && mouseY <= slotY + 16;
        boolean east = mouseX >= slotX + 18 && mouseX <= slotX + 34 && mouseY >= slotY && mouseY <= slotY + 16;
        boolean close = mouseX >= slotX - 18 && mouseX <= slotX - 2 && mouseY >= slotY - 18 && mouseY <= slotY - 2;

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

    public enum SlotMode implements CyclingMode<SlotMode> {
        DISABLED("D", 0xFF555555),  // False / Disabled (Dark Gray)
        INPUT("I", 0xFF3366CC),     // True / Input (Blue)
        FUEL("F", 0xFFCC9933),      // True / Fuel (Amber/Orange)
        OUTPUT("O", 0xFFCC3333),    // True / Output (Red)
        POTION("P", 0xFF9933CC),    // True / Potion (Purple) — Brewing module
        INGREDIENT("G", 0xFF33CC66); // True / Ingredient (Green) — Brewing module

        private final String labelChar;
        private final int color;

        SlotMode(String labelChar, int color) {
            this.labelChar = labelChar;
            this.color = color;
        }

        public String getLabelChar() {
            return labelChar;
        }

        public int getColor() {
            return color;
        }

        @Override public SlotMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override public SlotMode previous() {
            return values()[(ordinal() - 1 + values().length) % values().length];
        }
    }

    /**
     * Constructs a ProcessingModuleItem with the given properties.
     * <p>
     * Third-party modders should extend this class for modules that transform items
     * over time (e.g., smelting, brewing, cartography). Override {@link #processModule()}
     * to define the processing logic.
     *
     * @param properties The item properties (stack size, rarity, etc.)
     */
    public ProcessingModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull net.minecraft.world.item.Item.TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        Map<GridDirection, SlotMode> modes = getSlotModes(stack, getModuleConfigKey());
        boolean configured = false;
        for (SlotMode mode : modes.values()) {
            if (mode != SlotMode.DISABLED) {
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
     * Returns the list of slot modes this module supports in its direction-button cycle.
     * <p>
     * Each subclass overrides this method to declare which modes appear when the player
     * clicks a direction button. The base {@link #getCycledMode(ItemStack, GridDirection, boolean)}
     * method iterates only over these supported modes, so a Smelting Module will never
     * cycle into POTION or INGREDIENT modes that belong to the Brewing Module.
     * <p>
     * The default implementation returns all {@link SlotMode} values. Override to restrict
     * the cycle to the modes your module actually uses.
     *
     * @return An array of supported slot modes in cycle order (DISABLED should be first)
     */
    public SlotMode[] getSupportedModes() {
        return SlotMode.values();
    }

    /**
     * Returns the maximum count for a given slot mode.
     * <p>
     * Used by {@link #getCycledMode(ItemStack, GridDirection, boolean)} to enforce slot limits.
     * Override in subclasses that have special modes (e.g., FUEL with a fixed limit
     * separate from INPUT/OUTPUT).
     *
     * @param mode The slot mode to get the max count for
     * @return The maximum number of slots that can be configured to this mode
     */
    protected int getMaxForMode(SlotMode mode) {
        if (mode == SlotMode.INPUT) return getMaxInputs();
        if (mode == SlotMode.OUTPUT) return getMaxOutputs();
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
    protected int countSlotMode(ItemStack stack, SlotMode mode) {
        Map<GridDirection, SlotMode> modes = getSlotModes(stack, getModuleConfigKey());
        int count = 0;
        for (SlotMode m : modes.values()) {
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
        Map<GridDirection, SlotMode> modes = getSlotModes(stack, getModuleConfigKey());
        return modes.get(dir) != SlotMode.DISABLED;
    }

    @Override
    public void resetDirection(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        setSlotMode(stack, dir, SlotMode.DISABLED, getModuleConfigKey());
    }

    @Override
    public void resetDirectionMode(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        // Server-side reset — set the direction mode to DISABLED
        setSlotMode(stack, dir, SlotMode.DISABLED, getModuleConfigKey());
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
            SlotMode nextMode = getCycledMode(stack, clickedDir, isShiftHeld());
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

    public static Map<GridDirection, SlotMode> getSlotModes(ItemStack stack, String nbtKey) {
        Map<GridDirection, SlotMode> modes = new EnumMap<>(GridDirection.class);
        for (GridDirection dir : GridDirection.VALUES) {
            modes.put(dir, SlotMode.DISABLED);
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
                        modes.put(dir, SlotMode.valueOf(modesTag.getString(key)));
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
     * enforcing the per-mode slot limits via {@link #getMaxForMode(SlotMode)}.
     * Returns {@link SlotMode#DISABLED} when the cycle wraps back to the beginning.
     * <p>
     * Subclasses that need custom cycling logic (e.g., special FUEL handling) can override
     * this method. Most modules only need to override {@link #getSupportedModes()} and
     * {@link #getMaxForMode(SlotMode)}.
     *
     * @param stack       The module item stack
     * @param clickedDir  The grid direction whose button was clicked
     * @param reverse     If true, cycle in reverse order (shift-click behavior)
     * @return The next or previous slot mode in the cycle
     */
    public SlotMode getCycledMode(ItemStack stack, GridDirection clickedDir, boolean reverse) {
        Map<GridDirection, SlotMode> modes = getSlotModes(stack, getModuleConfigKey());
        SlotMode current = modes.get(clickedDir);
        SlotMode[] supported = getSupportedModes();

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
            SlotMode next = supported[nextIndex];

            if (next == SlotMode.DISABLED) {
                return SlotMode.DISABLED;
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
        return SlotMode.DISABLED;
    }

    @Override
    public void toggleDirectionMode(ItemStack stack, GridDirection direction, MachineFrameworkBlockEntity blockEntity, int slotIndex, boolean shiftHeld) {
        // Only cycle mode if an adjacent Inventory module is present
        AdjacentModuleType adjType = getAdjacentModuleType(blockEntity, slotIndex, direction);
        if (!isAdjacentModuleAllowed(adjType)) {
            return;
        }
        SlotMode nextMode = getCycledMode(stack, direction, shiftHeld);
        setSlotMode(stack, direction, nextMode, getModuleConfigKey());
    }

    public static void setSlotMode(ItemStack stack, GridDirection dir, SlotMode mode, String nbtKey) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        CompoundTag modesTag = tag.contains(nbtKey) ? tag.getCompound(nbtKey) : new CompoundTag();

        modesTag.putString(dir.nbtKey(), mode.name());
        tag.put(nbtKey, modesTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public boolean shouldHideSlot(int configuringSlotIndex, int targetSlotIndex) {
        // Uses standard baseline defined in BaseModuleItem
        return super.shouldHideSlot(configuringSlotIndex, targetSlotIndex);
    }

    @Override
    public void renderCustomOverlay(GuiGraphics graphics, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, net.minecraft.client.gui.Font font) {
        Map<GridDirection, SlotMode> modes = getSlotModes(stack, getModuleConfigKey());
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

    /**
     * Draws the 4 direction buttons (U/D/L/R) around a module slot.
     * If the overlay slot index is known, only draws buttons for directions with adjacent slots.
     * Subclasses like AnvilModuleItem can call this instead of duplicating the pattern.
     */
    @SuppressWarnings("unused")
    protected void drawDirectionButtons(GuiGraphics graphics, net.minecraft.client.gui.Font font, int slotX, int slotY,
                                        MachineFrameworkBlockEntity blockEntity, Map<GridDirection, SlotMode> modes) {
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

    protected void drawProcessingButton(GuiGraphics graphics, net.minecraft.client.gui.Font font, int x, int y, SlotMode mode) {
        graphics.fill(x - 8, y - 8, x + 8, y + 8, 0xFF888888);
        graphics.fill(x - 7, y - 7, x + 7, y + 7, mode.getColor());

        // Displays e.g., "T" or "F"
        graphics.drawCenteredString(font, mode.getLabelChar(), x, y - 4, 0xFFFFFF);
    }

    /**
     * Draws a processing button that checks if the adjacent module type is allowed.
     * If no allowed module is adjacent, renders the button in disabled state.
     */
    protected void drawProcessingButtonWithCheck(GuiGraphics graphics, net.minecraft.client.gui.Font font, int slotIndex, MachineFrameworkBlockEntity blockEntity, int x, int y, GridDirection dir, SlotMode mode) {
        BaseModuleItem.AdjacentModuleType adjType = BaseModuleItem.getAdjacentModuleType(blockEntity, slotIndex, dir);
        boolean allowed = isAdjacentModuleAllowed(adjType);

        graphics.fill(x - 8, y - 8, x + 8, y + 8, 0xFF888888);
        if (allowed) {
            graphics.fill(x - 7, y - 7, x + 7, y + 7, mode.getColor());
            graphics.drawCenteredString(font, mode.getLabelChar(), x, y - 4, 0xFFFFFF);
        } else {
            // No relevant adjacent module — show disabled
            graphics.fill(x - 7, y - 7, x + 7, y + 7, SlotMode.DISABLED.getColor());
            graphics.drawCenteredString(font, SlotMode.DISABLED.getLabelChar(), x, y - 4, 0xFFAAAAAA);
        }
    }

    // --- STANDARDIZED GRID NEIGHBOR RESOLVER ---
    public record ResolvedIoSlots(List<Integer> inputs, List<Integer> outputs) {}

    public static ResolvedIoSlots resolveAdjacentIoSlots(MachineFrameworkBlockEntity blockEntity, int slotIndex, Map<GridDirection, SlotMode> modes) {
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

            SlotMode mode = modes.get(dir);
            if (mode == SlotMode.INPUT) {
                inputs.add(adjSlot);
            } else if (mode == SlotMode.OUTPUT) {
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