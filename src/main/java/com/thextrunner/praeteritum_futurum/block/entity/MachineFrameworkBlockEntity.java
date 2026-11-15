package com.thextrunner.praeteritum_futurum.block.entity;

import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.ModTags;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.FilterModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import com.thextrunner.praeteritum_futurum.screen.MachineConfigMenu;
import com.thextrunner.praeteritum_futurum.screen.MachineInfoMenu;
import com.thextrunner.praeteritum_futurum.inventory.SingleSlotItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.thextrunner.praeteritum_futurum.item.custom.InventoryModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.neoforged.neoforge.capabilities.Capabilities;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MachineFrameworkBlockEntity extends BlockEntity implements MenuProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Grid dimensions: 3×3 = 9 total module slots per row, 3 rows. */
    public static final int GRID_SIZE = 27;
    /** Number of generic status channels per slot (progress, max progress, secondary, capacity). */
    public static final int NUM_CHANNELS = 4;
    /** Width of the grid in slots (3×3 layout). */
    public static final int GRID_WIDTH = 9;
    /** Number of rows in the grid (3 rows of 9). */
    public static final int GRID_ROWS = 3;

    // 27 slots x 4 generic status channels per slot.
    // Safe future-proofing hook: these channels store custom states, progress values,
    // or fluid levels for current and upcoming processing modules.
    // Channel 0: Progress / Burn Time
    // Channel 1: Max Progress / Burn Time Total / Wear
    // Channel 2: Secondary Progress / Fluid / Energy Level
    // Channel 3: Secondary Max / Capacity / Extra State
    private final int[][] moduleData = new int[GRID_SIZE][NUM_CHANNELS];

    /** UUID of the player who placed this machine framework. Empty string = unknown (old machines). */
    private String ownerUUID = "";

    /** When false, only the owner (or ops) can apply blueprints to this machine. Default: true (public). */
    private boolean isPublic = true;

    /**
     * Retrieves the numerical value stored in a specific status channel for a given module slot.
     */
    public int getModuleData(int slot, int channel) {
        if (slot < 0 || slot >= GRID_SIZE || channel < 0 || channel >= NUM_CHANNELS) return 0;
        return moduleData[slot][channel];
    }

    /**
     * Updates a specific status channel for a given module slot and triggers a block state sync to the client.
     */
    public void setModuleData(int slot, int channel, int value) {
        if (slot >= 0 && slot < GRID_SIZE && channel >= 0 && channel < NUM_CHANNELS) {
            if (moduleData[slot][channel] != value) {
                moduleData[slot][channel] = value;
                setChanged();
                if (level != null && !level.isClientSide()) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        }
    }

    // Handler managing the 27 module slots within the machine framework.
    private final ItemStackHandler moduleHandler = new ItemStackHandler(GRID_SIZE) {
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

            // Automatically initializes IO module default face states to DISABLED across all relative sides upon insertion.
            if (stack.is(ModItems.IO_MODULE.get()) && !stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                ItemStack singleModule = stack.copyWithCount(1);
                for (RelativeSide side : RelativeSide.values()) {
                    IoModuleItem.setFaceMode(singleModule, side, IoModuleItem.FaceMode.DISABLED);
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
                    ProcessingModuleItem.saveProgressData(MachineFrameworkBlockEntity.this, slot, extracted);
                }

                for (int ch = 0; ch < NUM_CHANNELS; ch++) {
                    setModuleData(slot, ch, 0);
                }
            }

            return extracted;
        }
    };

    // Handler managing the internal item inventory slots corresponding to each grid position.
    private final ItemStackHandler inventoryHandler = new ItemStackHandler(GRID_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            // Ask each module if it wants to restrict this inventory slot
            for (int i = 0; i < moduleHandler.getSlots(); i++) {
                ItemStack moduleStack = moduleHandler.getStackInSlot(i);
                if (moduleStack.getItem() instanceof BaseModuleItem module) {
                    int limit = module.getInventorySlotLimit(MachineFrameworkBlockEntity.this, i, slot);
                    if (limit > 0) {
                        return limit;
                    }
                }
            }
            return super.getSlotLimit(slot);
        }
    };

    public MachineFrameworkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_FRAMEWORK_BE.get(), pos, state);
    }

    public ItemStackHandler getModuleHandler() {
        return moduleHandler;
    }

    /** Sets the owner UUID when the block is first placed. */
    public void setOwnerUUID(String uuid) {
        this.ownerUUID = uuid;
    }

    /** Returns the owner UUID (may be empty for old machines). */
    public String getOwnerUUID() {
        return ownerUUID;
    }

    /** Returns whether the machine is publicly accessible for blueprint operations. */
    public boolean isPublic() {
        return isPublic;
    }

    /** Toggles the public/private state and persists the change. */
    public void togglePublic() {
        this.isPublic = !this.isPublic;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Checks whether the given player is allowed to modify this machine with a blueprint.
     * Returns true if the machine is public, the player is the owner, or the player is an operator.
     */
    public boolean canModify(Player player) {
        if (isPublic) return true;
        if (player.isCreative()) return true;
        if (player.getUUID().toString().equals(ownerUUID)) return true;
        return false;
    }

    public ItemStackHandler getInventoryHandler() {
        return inventoryHandler;
    }

    public boolean hasInventoryModule(int slotIndex) {
        ItemStack stack = moduleHandler.getStackInSlot(slotIndex);
        return stack.getItem() instanceof InventoryModuleItem;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
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

        int[] flatData = new int[GRID_SIZE * NUM_CHANNELS];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(moduleData[i], 0, flatData, i * NUM_CHANNELS, NUM_CHANNELS);
        }
        tag.putIntArray("ModuleData", flatData);

        tag.putString("OwnerUUID", ownerUUID);
        tag.putBoolean("IsPublic", isPublic);
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
            for (int i = 0; i < Math.min(flatData.length, GRID_SIZE * NUM_CHANNELS); i++) {
                moduleData[i / NUM_CHANNELS][i % NUM_CHANNELS] = flatData[i];
            }
        }
        if (tag.contains("OwnerUUID")) {
            ownerUUID = tag.getString("OwnerUUID");
        }
        if (tag.contains("IsPublic")) {
            isPublic = tag.getBoolean("IsPublic");
        }
    }

    /**
     * Calculates the adjacent grid slot indices (up, down, left, right) for a given slot index.
     */
    public int[] getAdjacentSlots(int slotIndex) {
        int row = slotIndex / GRID_WIDTH;
        int col = slotIndex % GRID_WIDTH;

        List<Integer> neighbors = new ArrayList<>();

        if (row > 0) {
            neighbors.add(slotIndex - GRID_WIDTH);
        }
        if (row < GRID_ROWS - 1) {
            neighbors.add(slotIndex + GRID_WIDTH);
        }
        if (col > 0) {
            neighbors.add(slotIndex - 1);
        }
        if (col < GRID_WIDTH - 1) {
            neighbors.add(slotIndex + 1);
        }

        return neighbors.stream().mapToInt(i -> i).toArray();
    }

    public IoModuleItem.FaceMode getEffectiveFaceMode(RelativeSide side) {
        IoModuleItem.FaceMode resolvedMode = IoModuleItem.FaceMode.DISABLED;

        for (int i = 0; i < this.getModuleHandler().getSlots(); i++) {
            ItemStack stack = this.getModuleHandler().getStackInSlot(i);
            if (stack.getItem() instanceof BaseModuleItem baseModule) {
                Map<RelativeSide, IoModuleItem.FaceMode> modes = baseModule.getFaceModes(stack);
                IoModuleItem.FaceMode mode = modes.get(side);

                if (mode == null || mode == IoModuleItem.FaceMode.DISABLED) {
                    continue;
                }

                resolvedMode = combineFaceModes(resolvedMode, mode);
            }
        }
        return resolvedMode;
    }

    private IoModuleItem.FaceMode combineFaceModes(IoModuleItem.FaceMode current, IoModuleItem.FaceMode incoming) {
        if (current == IoModuleItem.FaceMode.OPEN || incoming == IoModuleItem.FaceMode.OPEN) {
            return IoModuleItem.FaceMode.OPEN;
        }
        if ((current == IoModuleItem.FaceMode.INPUT && incoming == IoModuleItem.FaceMode.OUTPUT) ||
                (current == IoModuleItem.FaceMode.OUTPUT && incoming == IoModuleItem.FaceMode.INPUT)) {
            return IoModuleItem.FaceMode.OPEN;
        }
        return incoming.ordinal() > current.ordinal() ? incoming : current;
    }

    /**
     * Server-side tick logic running every tick to process modules and handle item push/pull automation based on relative IO configurations.
     * <p>
     * The tick is split into 3 phases:
     * 1. Pull — IO modules pull items from external inventories into the machine
     * 2. Process — Processing modules (smelter, stonecutter, etc.) work on internal items
     * 3. Push — IO modules push items from the machine to external inventories
     * <p>
     * IO transfer (phases 1 & 3) respects the configured tick rate from Config.Machine.IO_TRANSFER_TICK_RATE,
     * so transfers only happen every N ticks instead of every tick. Processing modules always run every tick
     * because they use vanilla mechanics for their own timing.
     */
    public static void serverTick(
            net.minecraft.world.level.Level level,
            BlockPos blockPos,
            BlockState state,
            MachineFrameworkBlockEntity blockEntity
    ) {
        if (level.isClientSide()) return;

        // Get the current tick count to determine if IO transfers should run this tick.
        // The config value controls how many ticks between transfer attempts (default: 1 = every tick).
        // Use the safe accessor to avoid NPE if called before the config spec is built.
        int tickRate = Config.getIoTransferTickRate();
        boolean shouldTransfer = (level.getGameTime() % tickRate == 0);

        // Log chunk loading status periodically (every 100 ticks) to monitor simulation distance
        if (Config.isDebugLoggingEnabled() && level.getGameTime() % 100 == 0) {
            boolean chunkLoaded = level.hasChunkAt(blockPos);
            LOGGER.info("[MACHINE TICK] pos={}, chunkLoaded={}, gameTime={}", blockPos, chunkLoaded, level.getGameTime());
        }

        // Phase 1: Pull external items into internal inventory modules
        // Only runs on ticks matching the configured IO transfer rate
        if (shouldTransfer) {
            for (int i = 0; i < blockEntity.moduleHandler.getSlots(); i++) {
                ItemStack moduleStack = blockEntity.moduleHandler.getStackInSlot(i);
                if (moduleStack.getItem() instanceof IoModuleItem ioModule) {
                    ioModule.tickInputs(level, blockPos, blockEntity, i, moduleStack);
                }
            }
        }

        // Phase 2: Processing modules process recipes / work on internal inventories
        // Always runs every tick — processing speed is controlled by vanilla mechanics
        for (int i = 0; i < blockEntity.moduleHandler.getSlots(); i++) {
            ItemStack moduleStack = blockEntity.moduleHandler.getStackInSlot(i);
            if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
                baseModule.tickModule(level, blockPos, blockEntity, i, moduleStack);
            }
        }

        // Phase 3: Push processed or finished items out to external inventories
        // Only runs on ticks matching the configured IO transfer rate
        if (shouldTransfer) {
            for (int i = 0; i < blockEntity.moduleHandler.getSlots(); i++) {
                ItemStack moduleStack = blockEntity.moduleHandler.getStackInSlot(i);
                if (moduleStack.getItem() instanceof IoModuleItem ioModule) {
                    ioModule.tickOutputs(level, blockPos, blockEntity, i, moduleStack);
                }
            }
        }
    }
    public Map<RelativeSide, IoModuleItem.FaceMode> getFaceModes(ItemStack stack) {
        return Collections.emptyMap();
    }

    /**
     * Pulls items from an external inventory into adjacent inventory modules.
     */
    public void pullItemsFromSide(int moduleSlot, Direction side) {
        if (level == null) return;
        if (moduleSlot < 0 || moduleSlot >= GRID_SIZE) return;

        int[] adjacentIndices = getAdjacentSlots(moduleSlot);
        for (int adj : adjacentIndices) {
            // Guard against out-of-bounds adjacent slot indices (e.g. from corrupted NBT data)
            if (adj < 0 || adj >= GRID_SIZE) continue;

            ItemStack adjModule = moduleHandler.getStackInSlot(adj);

            if (adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                BlockPos sourcePos = worldPosition.relative(side);

                // Do not interact with inventories outside the active block-ticking area.
                boolean sourceChunkTicking = level instanceof ServerLevel serverLevel
                        && serverLevel.shouldTickBlocksAt(sourcePos);
                if (!sourceChunkTicking) {
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.warn("[IO PULL] pos={}, from={}, chunkTicking=false — skipping capability lookup",
                                worldPosition, sourcePos);
                    }
                    continue;
                }

                IItemHandler sourceHandler = level.getCapability(
                        Capabilities.ItemHandler.BLOCK,
                        sourcePos,
                        side.getOpposite()
                );

                if (sourceHandler != null) {
                    IItemHandler targetHandler = new SingleSlotItemHandler(inventoryHandler, adj);
                    int before = countItems(inventoryHandler);
                    transferItems(sourceHandler, targetHandler, moduleSlot, adj);
                    int after = countItems(inventoryHandler);
                    if (Config.isDebugLoggingEnabled() && after > before) {
                        LOGGER.info("[IO PULL] pos={}, from={}, chunkTicking={}, transferred {} items",
                                worldPosition, sourcePos, sourceChunkTicking, after - before);
                    }
                }
            }
        }
    }

    /**
     * Pushes items from internal inventory modules out to an external inventory.
     */
    public void pushItemsToSide(int moduleSlot, Direction side) {
        if (level == null) return;
        if (moduleSlot < 0 || moduleSlot >= GRID_SIZE) return;

        int[] adjacentIndices = getAdjacentSlots(moduleSlot);
        BlockPos targetPos = worldPosition.relative(side);
        boolean targetChunkLoaded = level.hasChunkAt(targetPos);
        boolean targetChunkTicking = level instanceof ServerLevel serverLevel
                && serverLevel.shouldTickBlocksAt(targetPos);

        // Temporary diagnostic for IO chunk-boundary testing. Keep this at one line per
        // machine/IO module every 100 game ticks so a fast transfer config cannot flood logs.
        if (Config.isDebugLoggingEnabled() && level.getGameTime() % 100 == 0) {
            String adjacentModules = Arrays.stream(adjacentIndices)
                    .mapToObj(adj -> adj + ":" + BuiltInRegistries.ITEM.getKey(moduleHandler.getStackInSlot(adj).getItem()))
                    .collect(Collectors.joining(","));
            LOGGER.info("[IO PUSH DIAGNOSTIC] pos={}, ioSlot={}, side={}, adjacentModules=[{}], target={}, targetChunkLoaded={}, targetChunkTicking={}",
                    worldPosition, moduleSlot, side, adjacentModules, targetPos, targetChunkLoaded, targetChunkTicking);
        }

        for (int adj : adjacentIndices) {
            // Guard against out-of-bounds adjacent slot indices (e.g. from corrupted NBT data)
            if (adj < 0 || adj >= GRID_SIZE) continue;

            ItemStack adjModule = moduleHandler.getStackInSlot(adj);

            if (adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                IItemHandler sourceHandler = new SingleSlotItemHandler(inventoryHandler, adj);

                // Do not interact with inventories outside the active block-ticking area.
                if (!targetChunkTicking) {
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.warn("[IO PUSH] pos={}, to={}, chunkTicking=false — skipping capability lookup",
                                worldPosition, targetPos);
                    }
                    continue;
                }

                IItemHandler targetHandler = level.getCapability(
                        Capabilities.ItemHandler.BLOCK,
                        targetPos,
                        side.getOpposite()
                );

                if (targetHandler != null) {
                    int before = countItems(inventoryHandler);
                    transferItems(sourceHandler, targetHandler, moduleSlot, adj);
                    int after = countItems(inventoryHandler);
                    if (Config.isDebugLoggingEnabled() && before > after) {
                        LOGGER.info("[IO PUSH] pos={}, to={}, chunkTicking={}, transferred {} items",
                                worldPosition, targetPos, targetChunkTicking, before - after);
                    }
                }
            }
        }
    }

    /**
     * Exposes item handlers for external automation and pipe connections on specific world directions.
     */
    public IItemHandler getItemHandlerForSide(Direction side) {
        if (side == null || level == null) return null;

        List<IItemHandlerModifiable> targetHandlers = new ArrayList<>();

        for (int i = 0; i < moduleHandler.getSlots(); i++) {
            ItemStack moduleStack = moduleHandler.getStackInSlot(i);
            if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
                IItemHandlerModifiable handler = baseModule.getItemHandlerForSide(level, worldPosition, this, i, moduleStack, side);
                if (handler != null) {
                    targetHandlers.add(handler);
                }
            }
        }

        if (targetHandlers.isEmpty()) {
            return null;
        }

        return new CombinedInvWrapper(targetHandlers.toArray(new IItemHandlerModifiable[0]));
    }

    /**
     * Helper method to transfer items between handlers while respecting filter restrictions.
     */
    private void transferItems(IItemHandler source, IItemHandler target, int ioModuleSlot, int targetSlotIndex) {
        // Count items transferred for logging
        int sourceBefore = countItems(source);
        int targetBefore = countItems(target);
        for (int i = 0; i < source.getSlots(); i++) {
            ItemStack extracted = source.extractItem(i, 1, true);

            if (!extracted.isEmpty()) {
                if (FilterModuleItem.isItemBlockedByFilters(this, ioModuleSlot, targetSlotIndex, extracted)) {
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

    @NotNull
    private static int countItems(IItemHandler handler) {
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            count += handler.getStackInSlot(i).getCount();
        }
        return count;
    }
}
