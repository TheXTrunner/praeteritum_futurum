package com.thextrunner.praeteritum_futurum.block.entity;

import com.thextrunner.praeteritum_futurum.ModBlockEntities;
import com.thextrunner.praeteritum_futurum.ModItems;
import com.thextrunner.praeteritum_futurum.ModTags;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import com.thextrunner.praeteritum_futurum.screen.MachineConfigMenu;
import com.thextrunner.praeteritum_futurum.screen.MachineInfoMenu;
import com.thextrunner.praeteritum_futurum.inventory.SingleSlotItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MachineFrameworkBlockEntity extends BlockEntity implements MenuProvider {

    // 27 slots x 4 generic status channels per slot.
    // Safe future-proofing hook: these channels store custom states, progress values,
    // or fluid levels for current and upcoming processing modules.
    // Channel 0: Progress / Burn Time
    // Channel 1: Max Progress / Burn Time Total / Wear
    // Channel 2: Secondary Progress / Fluid / Energy Level
    // Channel 3: Secondary Max / Capacity / Extra State
    private final int[][] moduleData = new int[27][4];

    public int getModuleData(int slot, int channel) {
        if (slot < 0 || slot >= 27 || channel < 0 || channel >= 4) return 0;
        return moduleData[slot][channel];
    }

    public void setModuleData(int slot, int channel, int value) {
        if (slot >= 0 && slot < 27 && channel >= 0 && channel < 4) {
            if (moduleData[slot][channel] != value) {
                moduleData[slot][channel] = value;
                setChanged();
                if (level != null && !level.isClientSide()) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        }
    }

    private final ItemStackHandler moduleHandler = new ItemStackHandler(27) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(ModTags.Items.MACHINE_MODULES);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) return stack;

            if (stack.is(ModItems.IO_MODULE.get()) && !stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                ItemStack singleModule = stack.copyWithCount(1);
                for (Direction dir : Direction.values()) {
                    IoModuleItem.setFaceMode(singleModule, dir, IoModuleItem.FaceMode.DISABLED);
                }
                return super.insertItem(slot, singleModule, simulate);
            }

            if (stack.is(ModItems.FILTER_MODULE.get()) && !stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                ItemStack singleModule = stack.copyWithCount(1);
                singleModule.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
                singleModule.set(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE, 1);
                return super.insertItem(slot, singleModule, simulate);
            }

            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack extracted = super.extractItem(slot, amount, simulate);

            if (!simulate && !extracted.isEmpty()) {
                if (extracted.is(ModTags.Items.PROCESSING_MODULES)) {
                    com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem.saveProgressData(MachineFrameworkBlockEntity.this, slot, extracted);
                }

                for (int ch = 0; ch < 4; ch++) {
                    setModuleData(slot, ch, 0);
                }
            }

            return extracted;
        }
    };

    private final ItemStackHandler inventoryHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public MachineFrameworkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_FRAMEWORK_BE.get(), pos, state);
    }

    public ItemStackHandler getModuleHandler() {
        return moduleHandler;
    }

    public ItemStackHandler getInventoryHandler() {
        return inventoryHandler;
    }

    public boolean hasInventoryModule(int slotIndex) {
        ItemStack stack = moduleHandler.getStackInSlot(slotIndex);
        return stack.getItem() instanceof com.thextrunner.praeteritum_futurum.item.custom.InventoryModuleItem;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.praeteritum_futurum.machine_framework");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player /* Intentional: required by MenuProvider interface contract for container creation */) {
        return new MachineConfigMenu(containerId, playerInventory, this);
    }

    public AbstractContainerMenu createInfoMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineInfoMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Modules", moduleHandler.serializeNBT(registries));
        tag.put("Inventory", inventoryHandler.serializeNBT(registries));

        int[] flatData = new int[27 * 4];
        for (int i = 0; i < 27; i++) {
            System.arraycopy(moduleData[i], 0, flatData, i * 4, 4);
        }
        tag.putIntArray("ModuleData", flatData);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Modules")) {
            moduleHandler.deserializeNBT(registries, tag.getCompound("Modules"));
        }
        if (tag.contains("Inventory")) {
            inventoryHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("ModuleData")) {
            int[] flatData = tag.getIntArray("ModuleData");
            for (int i = 0; i < Math.min(flatData.length, 27 * 4); i++) {
                moduleData[i / 4][i % 4] = flatData[i];
            }
        }
    }

    public int[] getAdjacentSlots(int slotIndex) {
        int row = slotIndex / 9;
        int col = slotIndex % 9;

        List<Integer> neighbors = new ArrayList<>();

        if (row > 0) {
            neighbors.add(slotIndex - 9);
        }
        if (row < 2) {
            neighbors.add(slotIndex + 9);
        }
        if (col > 0) {
            neighbors.add(slotIndex - 1);
        }
        if (col < 8) {
            neighbors.add(slotIndex + 1);
        }

        return neighbors.stream().mapToInt(i -> i).toArray();
    }

    public IItemHandler getItemHandlerForSide(Direction side) {
        if (side == null) return null;

        List<IItemHandlerModifiable> targetHandlers = new ArrayList<>();

        for (int i = 0; i < moduleHandler.getSlots(); i++) {
            ItemStack moduleStack = moduleHandler.getStackInSlot(i);

            if (moduleStack.is(ModItems.IO_MODULE.get())) {
                Set<Direction> enabledFaces = IoModuleItem.getEnabledFaces(moduleStack);

                if (enabledFaces.contains(side)) {
                    int[] adjacentIndices = getAdjacentSlots(i);
                    for (int adj : adjacentIndices) {
                        ItemStack adjModule = moduleHandler.getStackInSlot(adj);
                        if (adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                            targetHandlers.add(new SingleSlotItemHandler(inventoryHandler, adj));
                        }
                    }
                }
            }
        }

        if (targetHandlers.isEmpty()) {
            return null;
        }

        return new CombinedInvWrapper(targetHandlers.toArray(new IItemHandlerModifiable[0]));
    }

    public static void serverTick(
            net.minecraft.world.level.Level level,
            BlockPos blockPos /* Intentional: required by BlockEntityTicker signature */,
            BlockState state /* Intentional: required by BlockEntityTicker signature */,
            MachineFrameworkBlockEntity blockEntity
    ) {
        if (level.isClientSide()) return;

        for (int i = 0; i < blockEntity.moduleHandler.getSlots(); i++) {
            ItemStack moduleStack = blockEntity.moduleHandler.getStackInSlot(i);
            if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
                baseModule.tickModule(level, blockEntity.getBlockPos(), blockEntity, i, moduleStack);
            }
        }

        if (level.getGameTime() % 10 != 0) return;

        for (int i = 0; i < blockEntity.moduleHandler.getSlots(); i++) {
            ItemStack moduleStack = blockEntity.moduleHandler.getStackInSlot(i);

            if (moduleStack.is(ModItems.IO_MODULE.get())) {
                Map<Direction, IoModuleItem.FaceMode> modes = IoModuleItem.getFaceModes(moduleStack);

                for (Map.Entry<Direction, IoModuleItem.FaceMode> entry : modes.entrySet()) {
                    if (entry.getValue() == IoModuleItem.FaceMode.OUTPUT) {
                        blockEntity.pushItemsToSide(i, entry.getKey());
                    } else if (entry.getValue() == IoModuleItem.FaceMode.INPUT) {
                        blockEntity.pullItemsFromSide(i, entry.getKey());
                    }
                }
            }
        }
    }

    private void pullItemsFromSide(int moduleSlot, Direction side) {
        if (level == null) return;

        int[] adjacentIndices = getAdjacentSlots(moduleSlot);
        for (int adj : adjacentIndices) {
            ItemStack adjModule = moduleHandler.getStackInSlot(adj);

            if (adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                BlockPos sourcePos = worldPosition.relative(side);
                IItemHandler sourceHandler = level.getCapability(
                        net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                        sourcePos,
                        side.getOpposite()
                );

                if (sourceHandler != null) {
                    IItemHandler targetHandler = new SingleSlotItemHandler(inventoryHandler, adj);
                    transferItems(sourceHandler, targetHandler, moduleSlot, adj);
                }
            }
        }
    }

    private void pushItemsToSide(int moduleSlot, Direction side) {
        if (level == null) return;

        int[] adjacentIndices = getAdjacentSlots(moduleSlot);
        for (int adj : adjacentIndices) {
            ItemStack adjModule = moduleHandler.getStackInSlot(adj);

            if (adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                IItemHandler sourceHandler = new SingleSlotItemHandler(inventoryHandler, adj);
                BlockPos targetPos = worldPosition.relative(side);

                IItemHandler targetHandler = level.getCapability(
                        net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                        targetPos,
                        side.getOpposite()
                );

                if (targetHandler != null) {
                    transferItems(sourceHandler, targetHandler, moduleSlot, adj);
                }
            }
        }
    }

    private boolean isItemAllowedByFilters(int ioModuleSlot, int targetSlotIndex, ItemStack stack) {
        int[] ioNeighbors = getAdjacentSlots(ioModuleSlot);
        boolean ioHasFilter = false;
        boolean ioMatchesFilter = false;

        for (int adj : ioNeighbors) {
            ItemStack moduleStack = moduleHandler.getStackInSlot(adj);
            if (moduleStack.is(ModItems.FILTER_MODULE.get())) {
                ioHasFilter = true;
                assert level != null;
                ItemStackHandler filterInv = com.thextrunner.praeteritum_futurum.item.custom.FilterModuleItem.getFilterInventory(moduleStack, level.registryAccess());

                for (int i = 0; i < filterInv.getSlots(); i++) {
                    ItemStack filterItem = filterInv.getStackInSlot(i);
                    if (!filterItem.isEmpty() && ItemStack.isSameItem(filterItem, stack)) {
                        ioMatchesFilter = true;
                        break;
                    }
                }
                if (ioMatchesFilter) break;
            }
        }

        int[] invNeighbors = getAdjacentSlots(targetSlotIndex);
        boolean invHasFilter = false;
        boolean invMatchesFilter = false;

        for (int adj : invNeighbors) {
            ItemStack moduleStack = moduleHandler.getStackInSlot(adj);
            if (moduleStack.is(ModItems.FILTER_MODULE.get())) {
                invHasFilter = true;
                assert level != null;
                ItemStackHandler filterInv = com.thextrunner.praeteritum_futurum.item.custom.FilterModuleItem.getFilterInventory(moduleStack, level.registryAccess());

                for (int i = 0; i < filterInv.getSlots(); i++) {
                    ItemStack filterItem = filterInv.getStackInSlot(i);
                    if (!filterItem.isEmpty() && ItemStack.isSameItem(filterItem, stack)) {
                        invMatchesFilter = true;
                        break;
                    }
                }
                if (invMatchesFilter) break;
            }
        }

        boolean ioAllowed = !ioHasFilter || ioMatchesFilter;
        boolean invAllowed = !invHasFilter || invMatchesFilter;

        return ioAllowed && invAllowed;
    }

    private void transferItems(IItemHandler source, IItemHandler target, int ioModuleSlot, int targetSlotIndex) {
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack extracted = source.extractItem(i, 1, true);

            if (!extracted.isEmpty()) {
                if (!isItemAllowedByFilters(ioModuleSlot, targetSlotIndex, extracted)) {
                    continue;
                }

                ItemStack remainder = extracted.copy();

                for (int j = 0; j < target.getSlots(); j++) {
                    remainder = target.insertItem(j, remainder, false);
                    if (remainder.isEmpty()) break;
                }

                int moved = extracted.getCount() - remainder.getCount();
                if (moved > 0) {
                    source.extractItem(i, moved, false);
                    setChanged();
                    break;
                }
            }
        }
    }
}