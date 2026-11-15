package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CartographyModuleItem extends ProcessingModuleItem {

    private static final String NBT_KEY = "CartographyModes";

    public CartographyModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/cartography_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16,
                16, 16
        );
        super.renderModuleOverlay(graphics, slot, stack, blockEntity);
    }

    @Override
    public String getModuleConfigKey() {
        return NBT_KEY;
    }

    @Override
    public int getMaxInputs() {
        return 2;
    }

    @Override
    public int getMaxOutputs() {
        return 1;
    }

    @Override
    public SlotMode getNextMode(ItemStack stack, Direction clickedDir) {
        Map<Direction, SlotMode> modes = getSlotModes(stack, NBT_KEY);
        SlotMode current = modes.get(clickedDir);

        SlotMode[] allowedModes = {SlotMode.DISABLED, SlotMode.INPUT, SlotMode.OUTPUT};
        int currentIndex = 0;
        for (int i = 0; i < allowedModes.length; i++) {
            if (allowedModes[i] == current) {
                currentIndex = i;
                break;
            }
        }

        for (int i = 1; i <= allowedModes.length; i++) {
            int nextIndex = (currentIndex + i) % allowedModes.length;
            SlotMode next = allowedModes[nextIndex];

            if (next == SlotMode.DISABLED) {
                return SlotMode.DISABLED;
            }

            int currentCount = countSlotMode(stack, next);
            if (current == next) {
                currentCount--;
            }

            int max = (next == SlotMode.INPUT) ? getMaxInputs() : getMaxOutputs();
            if (currentCount >= max) {
                continue;
            }

            return next;
        }
        return SlotMode.DISABLED;
    }

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        Map<Direction, SlotMode> modes = getSlotModes(stack, NBT_KEY);
        ResolvedIoSlots resolved = ProcessingModuleItem.resolveAdjacentIoSlots(blockEntity, slotIndex, modes);

        List<Integer> inputSlots = resolved.inputs();
        List<Integer> outputSlots = resolved.outputs();

        if (inputSlots.size() < 2 || outputSlots.isEmpty()) return;

        ItemStackHandler itemHandler = blockEntity.getInventoryHandler();

        // Get the two input items
        ItemStack item1 = ItemStack.EMPTY;
        ItemStack item2 = ItemStack.EMPTY;
        int slot1 = -1;
        int slot2 = -1;

        for (int inSlot : inputSlots) {
            ItemStack stackInSlot = itemHandler.getStackInSlot(inSlot);
            if (stackInSlot.isEmpty()) continue;

            if (item1.isEmpty()) {
                item1 = stackInSlot;
                slot1 = inSlot;
            } else if (item2.isEmpty()) {
                item2 = stackInSlot;
                slot2 = inSlot;
                break;
            }
        }

        if (item1.isEmpty() || item2.isEmpty()) return;

        // Cartography operations are hardcoded in vanilla - no recipe system
        // Determine which item is the map
        ItemStack mapItem = ItemStack.EMPTY;
        ItemStack otherItem = ItemStack.EMPTY;
        int mapSlot = -1;
        int otherSlot = -1;

        if (item1.is(Items.FILLED_MAP)) {
            mapItem = item1;
            otherItem = item2;
            mapSlot = slot1;
            otherSlot = slot2;
        } else if (item2.is(Items.FILLED_MAP)) {
            mapItem = item2;
            otherItem = item1;
            mapSlot = slot2;
            otherSlot = slot1;
        } else {
            return; // No map found
        }

        // Cartography operations are hardcoded in vanilla - no recipe system
        // Case 1: Map + Paper = Zoom out map
        if (otherItem.is(Items.PAPER)) {
            ItemStack result = zoomOutMap(level, mapItem);
            if (result == null || result.isEmpty()) return;
            processSingleOutput(itemHandler, outputSlots, mapSlot, otherSlot, result);
            return;
        }
        // Case 2: Map + Empty Map = Clone map (original map is preserved)
        else if (otherItem.is(Items.MAP)) {
            ItemStack cloned = cloneMap(level, mapItem);
            if (cloned == null || cloned.isEmpty()) return;
            // For cloning, consume only the empty map; the original map stays
            // Output the cloned map
            for (int outSlot : outputSlots) {
                if (outSlot == mapSlot || outSlot == otherSlot) continue;

                ItemStack currentOut = itemHandler.getStackInSlot(outSlot);

                if (currentOut.isEmpty()) {
                    itemHandler.setStackInSlot(outSlot, cloned);
                    itemHandler.extractItem(otherSlot, 1, false);
                    return;
                } else if (ItemStack.isSameItemSameComponents(currentOut, cloned) && currentOut.getCount() < currentOut.getMaxStackSize()) {
                    currentOut.grow(cloned.getCount());
                    itemHandler.extractItem(otherSlot, 1, false);
                    return;
                }
            }
            return;
        }
        // Case 3: Map + Glass Pane = Lock map
        else if (otherItem.is(Items.GLASS_PANE)) {
            ItemStack result = lockMap(level, mapItem);
            if (result == null || result.isEmpty()) return;
            processSingleOutput(itemHandler, outputSlots, mapSlot, otherSlot, result);
            return;
        }
    }

    /**
     * Processes a single-output cartography operation (zoom out, lock).
     * Consumes both inputs and outputs the result.
     */
    private void processSingleOutput(ItemStackHandler itemHandler, List<Integer> outputSlots, int mapSlot, int otherSlot, ItemStack result) {
        for (int outSlot : outputSlots) {
            if (outSlot == mapSlot || outSlot == otherSlot) continue;

            ItemStack currentOut = itemHandler.getStackInSlot(outSlot);

            if (currentOut.isEmpty()) {
                itemHandler.setStackInSlot(outSlot, result);
                itemHandler.extractItem(mapSlot, 1, false);
                itemHandler.extractItem(otherSlot, 1, false);
                return;
            } else if (ItemStack.isSameItemSameComponents(currentOut, result) && currentOut.getCount() < currentOut.getMaxStackSize()) {
                currentOut.grow(result.getCount());
                itemHandler.extractItem(mapSlot, 1, false);
                itemHandler.extractItem(otherSlot, 1, false);
                return;
            }
        }
    }

    /**
     * Zooms out a map by one level (if not already at max zoom).
     * Creates a new map with double the scale.
     */
    @Nullable
    private ItemStack zoomOutMap(Level level, ItemStack map) {
        net.minecraft.world.level.saveddata.maps.MapId mapId = map.get(DataComponents.MAP_ID);
        if (mapId == null) return null;

        MapItemSavedData sourceData = level.getMapData(mapId);
        if (sourceData == null) return null;

        // Check if already at max zoom level (scale 4 = 1:16)
        if (sourceData.scale >= 4) return null;

        // Use the scaled() method to create a zoomed-out copy
        MapItemSavedData scaledData = sourceData.scaled();
        if (scaledData == null) return null;

        net.minecraft.world.level.saveddata.maps.MapId newMapId = level.getFreeMapId();
        level.setMapData(newMapId, scaledData);
        scaledData.setDirty();

        ItemStack result = new ItemStack(Items.FILLED_MAP);
        result.set(DataComponents.MAP_ID, newMapId);
        return result;
    }

    /**
     * Clones a map to a new map ID.
     * Creates an independent copy of the map data.
     */
    @Nullable
    private ItemStack cloneMap(Level level, ItemStack sourceMap) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return null;

        net.minecraft.world.level.saveddata.maps.MapId sourceMapId = sourceMap.get(DataComponents.MAP_ID);
        if (sourceMapId == null) return null;

        MapItemSavedData sourceData = level.getMapData(sourceMapId);
        if (sourceData == null) return null;

        // Create a new map with the same data
        net.minecraft.world.level.saveddata.maps.MapId newMapId = level.getFreeMapId();

        // Save the source data to a tag and load it as a new map
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        sourceData.save(tag, level.registryAccess());

        MapItemSavedData cloneData = MapItemSavedData.load(tag, level.registryAccess());
        cloneData.setDirty();

        level.setMapData(newMapId, cloneData);

        ItemStack result = new ItemStack(Items.FILLED_MAP);
        result.set(DataComponents.MAP_ID, newMapId);
        return result;
    }

    /**
     * Locks a map, creating a new locked copy that won't update.
     */
    @Nullable
    private ItemStack lockMap(Level level, ItemStack map) {
        net.minecraft.world.level.saveddata.maps.MapId mapId = map.get(DataComponents.MAP_ID);
        if (mapId == null) return null;

        MapItemSavedData sourceData = level.getMapData(mapId);
        if (sourceData == null) return null;

        // Use the locked() method to create a locked copy
        MapItemSavedData lockedData = sourceData.locked();
        if (lockedData == null) return null;

        net.minecraft.world.level.saveddata.maps.MapId newMapId = level.getFreeMapId();
        level.setMapData(newMapId, lockedData);
        lockedData.setDirty();

        ItemStack result = new ItemStack(Items.FILLED_MAP);
        result.set(DataComponents.MAP_ID, newMapId);
        return result;
    }
}
