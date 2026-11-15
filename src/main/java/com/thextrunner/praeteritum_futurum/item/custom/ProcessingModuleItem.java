package com.thextrunner.praeteritum_futurum.item.custom;

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

    /**
     * Stores the slot index during overlay rendering so border-aware button rendering works.
     * Set by the overlay renderer before calling renderCustomOverlay.
     */
    private int overlaySlotIndex = -1;

    /**
     * Returns the slot index for overlay rendering. Set by the overlay renderer.
     */
    protected int getOverlaySlotIndex() {
        return overlaySlotIndex;
    }

    /**
     * Sets the slot index for overlay rendering. Called by the overlay renderer.
     */
    public void setOverlaySlotIndex(int index) {
        this.overlaySlotIndex = index;
    }

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

    @Override
    public boolean isCloseButton(double mouseX, double mouseY, int slotX, int slotY) {
        return mouseX >= slotX - 18 && mouseX <= slotX - 2 && mouseY >= slotY - 18 && mouseY <= slotY - 2;
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

    public enum SlotMode {
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

        public SlotMode next() {
            return values()[(ordinal() + 1) % values().length];
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
        Map<Direction, SlotMode> modes = getSlotModes(stack, getModuleConfigKey());
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
     * Counts how many slots are currently configured as INPUT or OUTPUT
     * in the given module stack's slot mode map.
     *
     * @param stack  The module item stack
     * @param mode   The slot mode to count (INPUT or OUTPUT)
     * @return The number of directions currently set to the given mode
     */
    protected int countSlotMode(ItemStack stack, SlotMode mode) {
        Map<Direction, SlotMode> modes = getSlotModes(stack, getModuleConfigKey());
        int count = 0;
        for (SlotMode m : modes.values()) {
            if (m == mode) count++;
        }
        return count;
    }

    @Override
    public void handleCustomClick(double mouseX, double mouseY, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex, net.minecraft.client.Minecraft minecraft) {
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

        Direction clickedDir = getProcessingClickedDirection(slotX, slotY, mouseX, mouseY, slotIndex);
        if (clickedDir != null) {
            if (minecraft.player != null) {
                minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
            }
            SlotMode nextMode = getNextMode(stack, clickedDir);
            setSlotMode(stack, clickedDir, nextMode, getModuleConfigKey());

            PacketDistributor.sendToServer(new ServerboundToggleModuleDirectionPacket(slotIndex, clickedDir, blockEntity.getBlockPos()));
        }
    }

    @Override
    public boolean opensInlineOverlay() {
        return true; // Processing modules use inline overlays
    }

    private Direction getProcessingClickedDirection(int slotX, int slotY, double mx, double my, int slotIndex) {
        int centerX = slotX + 8;
        int centerY = slotY + 8;
        int offset = 18;

        // Only check buttons for directions that have adjacent slots
        if (hasAdjacentSlot(slotIndex, Direction.UP) && isInsideButton(mx, my, centerX, slotY - (offset - 8))) return Direction.UP;
        if (hasAdjacentSlot(slotIndex, Direction.WEST) && isInsideButton(mx, my, slotX - (offset - 8), centerY)) return Direction.WEST;
        if (hasAdjacentSlot(slotIndex, Direction.EAST) && isInsideButton(mx, my, slotX + 16 + (offset - 8), centerY)) return Direction.EAST;
        if (hasAdjacentSlot(slotIndex, Direction.DOWN) && isInsideButton(mx, my, centerX, slotY + 16 + (offset - 8))) return Direction.DOWN;
        return null;
    }

    /**
     * Checks if the given slot index has an adjacent slot in the specified direction.
     * Returns false for border positions where no adjacent slot exists.
     */
    public boolean hasAdjacentSlot(int slotIndex, Direction dir) {
        int row = slotIndex / MachineFrameworkBlockEntity.GRID_WIDTH;
        int col = slotIndex % MachineFrameworkBlockEntity.GRID_WIDTH;

        return switch (dir) {
            case UP -> row > 0;
            case DOWN -> row < MachineFrameworkBlockEntity.GRID_ROWS - 1;
            case WEST -> col > 0;
            case EAST -> col < MachineFrameworkBlockEntity.GRID_WIDTH - 1;
            default -> false; // UP, DOWN, NORTH, SOUTH, etc. have no grid neighbor
        };
    }

    private boolean isInsideButton(double mx, double my, int x, int y) {
        return mx >= x - 8 && mx < x + 8 && my >= y - 8 && my < y + 8;
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

    public static Map<Direction, SlotMode> getSlotModes(ItemStack stack, String nbtKey) {
        Map<Direction, SlotMode> modes = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            modes.put(dir, SlotMode.DISABLED);
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(nbtKey)) {
                CompoundTag modesTag = tag.getCompound(nbtKey);
                for (Direction dir : Direction.values()) {
                    if (modesTag.contains(dir.getName())) {
                        modes.put(dir, SlotMode.valueOf(modesTag.getString(dir.getName())));
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

    public SlotMode getNextMode(ItemStack stack, Direction clickedDir) {
        Map<Direction, SlotMode> modes = getSlotModes(stack, getModuleConfigKey());
        SlotMode current = modes.get(clickedDir);
        return current.next();
    }

    public static void setSlotMode(ItemStack stack, Direction dir, SlotMode mode, String nbtKey) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        CompoundTag modesTag = tag.contains(nbtKey) ? tag.getCompound(nbtKey) : new CompoundTag();

        modesTag.putString(dir.getName(), mode.name());
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
        Map<Direction, SlotMode> modes = getSlotModes(stack, getModuleConfigKey());

        int centerX = slotX + 8;
        int centerY = slotY + 8;
        int offset = 18;

        // Render only buttons for directions that have adjacent slots
        int slotIndex = getOverlaySlotIndex();
        if (slotIndex >= 0) {
            if (hasAdjacentSlot(slotIndex, Direction.UP))
                drawProcessingButton(graphics, font, centerX, slotY - (offset - 8), modes.get(Direction.UP), "U");
            if (hasAdjacentSlot(slotIndex, Direction.WEST))
                drawProcessingButton(graphics, font, slotX - (offset - 8), centerY, modes.get(Direction.WEST), "L");
            if (hasAdjacentSlot(slotIndex, Direction.EAST))
                drawProcessingButton(graphics, font, slotX + 16 + (offset - 8), centerY, modes.get(Direction.EAST), "R");
            if (hasAdjacentSlot(slotIndex, Direction.DOWN))
                drawProcessingButton(graphics, font, centerX, slotY + 16 + (offset - 8), modes.get(Direction.DOWN), "D");
        } else {
            // Fallback: render all buttons if slot index is unknown (e.g., third-party overlay)
            drawProcessingButton(graphics, font, centerX, slotY - (offset - 8), modes.get(Direction.UP), "U");
            drawProcessingButton(graphics, font, slotX - (offset - 8), centerY, modes.get(Direction.WEST), "L");
            drawProcessingButton(graphics, font, slotX + 16 + (offset - 8), centerY, modes.get(Direction.EAST), "R");
            drawProcessingButton(graphics, font, centerX, slotY + 16 + (offset - 8), modes.get(Direction.DOWN), "D");
        }

        if (hasConfigSlot()) {
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF888888);
            assert blockEntity.getLevel() != null;
            ItemStack targetItem = getConfigSlotItem(stack, blockEntity.getLevel().registryAccess());
            if (!targetItem.isEmpty()) {
                graphics.renderItem(targetItem, slotX, slotY);
                graphics.renderItemDecorations(font, targetItem, slotX, slotY);
            }
        }

        // Close button
        graphics.fill(slotX - 18, slotY - 18, slotX - 2, slotY - 2, 0xFF8B0000);
        graphics.drawCenteredString(font, "X", slotX - 10, slotY - 14, 0xFFFFFF);
    }

    private void drawProcessingButton(GuiGraphics graphics, net.minecraft.client.gui.Font font, int x, int y, SlotMode mode, String sideLabel) {
        graphics.fill(x - 8, y - 8, x + 8, y + 8, 0xFF888888);
        graphics.fill(x - 7, y - 7, x + 7, y + 7, mode.getColor());

        // Displays e.g., "U:T" or "L:F"
        graphics.drawCenteredString(font, sideLabel + ":" + mode.getLabelChar(), x, y - 4, 0xFFFFFF);
    }

    // --- STANDARDIZED GRID NEIGHBOR RESOLVER ---
    public record ResolvedIoSlots(List<Integer> inputs, List<Integer> outputs) {}

    public static ResolvedIoSlots resolveAdjacentIoSlots(MachineFrameworkBlockEntity blockEntity, int slotIndex, Map<Direction, SlotMode> modes) {
        ItemStackHandler moduleHandler = blockEntity.getModuleHandler();
        List<Integer> inputs = new ArrayList<>();
        List<Integer> outputs = new ArrayList<>();

        int row = slotIndex / 9;
        int col = slotIndex % 9;

        int[] neighborIndices = {
                row > 0 ? slotIndex - 9 : -1,     // UP
                row < 2 ? slotIndex + 9 : -1,     // DOWN
                col > 0 ? slotIndex - 1 : -1,     // WEST
                col < 8 ? slotIndex + 1 : -1      // EAST
        };
        Direction[] dirs = {Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST};

        for (int i = 0; i < 4; i++) {
            int adjSlot = neighborIndices[i];
            if (adjSlot < 0) continue;

            ItemStack adjModule = moduleHandler.getStackInSlot(adjSlot);
            if (!adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                continue;
            }

            SlotMode mode = modes.get(dirs[i]);
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
        if (blockEntity.getModuleData(slotIndex, 2) == 0 && blockEntity.getModuleData(slotIndex, 0) == 0) {
            if (stack.has(DataComponents.CUSTOM_DATA)) {
                CustomData customDataComp = stack.get(DataComponents.CUSTOM_DATA);
                if (customDataComp != null) {
                    CompoundTag tag = customDataComp.copyTag();
                    if (tag.contains("ModuleChannels")) {
                        int[] channels = tag.getIntArray("ModuleChannels");
                        for (int i = 0; i < Math.min(channels.length, 4); i++) {
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

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();

        if (burnTime > 0 || cookTime > 0) {
            int[] channels = new int[4];
            for (int i = 0; i < 4; i++) {
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