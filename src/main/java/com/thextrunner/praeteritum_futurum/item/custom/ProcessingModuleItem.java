package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.ModItems;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
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

    public float getProgressPercent(ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        return 0.0F;
    }

    public enum SlotMode {
        DISABLED, INPUT, FUEL, OUTPUT;

        public SlotMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public ProcessingModuleItem(Properties properties) {
        super(properties);
    }

    public abstract String getModuleConfigKey();

    public boolean hasConfigSlot() {
        return false;
    }

    public ItemStack getConfigSlotItem(ItemStack moduleStack, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    public void setConfigSlotItem(ItemStack moduleStack, ItemStack target, HolderLookup.Provider registries) {}

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
                col > 0 ? slotIndex - 1 : -1,     // LEFT
                col < 8 ? slotIndex + 1 : -1      // RIGHT
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