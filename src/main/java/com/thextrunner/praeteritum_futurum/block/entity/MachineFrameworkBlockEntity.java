package com.thextrunner.praeteritum_futurum.block.entity;

import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.ModTags;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.FilterModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Block Entity for the Machine Framework — the central data container for all machine state.
 * <p>
 * Stores and manages:
 * <ul>
 *   <li>27 module slots (3×9 grid) with their item stacks</li>
 *   <li>27 inventory slots (one per grid position)</li>
 *   <li>Per-slot status channels for module progress data</li>
 *   <li>Owner UUID and public/private access control</li>
 *   <li>Module-owned objects and cleanup actions</li>
 * </ul>
 * <p>
 * All server-side logic (IO transfers, processing, ticking) runs through this block entity.
 * The server tick method ({@link #serverTick}) is called every tick and orchestrates the
 * three-phase processing cycle: pull → process → push.
 * <p>
 * Implements {@link MenuProvider} so players can open the machine's GUI screens.
 */
public class MachineFrameworkBlockEntity extends BlockEntity implements MenuProvider {

    /** Logger for machine diagnostics and optional debug output. */
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Grid dimensions: 3×9 = 27 total module slots. */
    public static final int GRID_SIZE = 27;

    /** Number of generic status channels per slot. Processing modules use channels 0-3 for progress; I/O modules use channels 0-5 for per-face redstone signals. */
    public static final int NUM_CHANNELS = 6;

    /** Width of the grid in slots (9 columns). */
    public static final int GRID_WIDTH = 9;

    /** Number of rows in the grid (3 rows of 9). */
    public static final int GRID_ROWS = 3;

    /**
     * Per-slot status channel data: 27 slots × 6 channels each.
     * <p>
     * Safe future-proofing hook: these channels store custom states, progress values,
     * or fluid levels for current and upcoming processing modules.
     * <ul>
     *   <li>Channel 0: Progress / Burn Time</li>
     *   <li>Channel 1: Max Progress / Burn Time Total / Wear</li>
     *   <li>Channel 2: Secondary Progress / Fluid / Energy Level</li>
     *   <li>Channel 3: Secondary Max / Capacity / Extra State</li>
     *   <li>Channel 4-5: Additional module-specific state</li>
     *   <li>I/O module redstone signals: FRONT, BACK, LEFT, RIGHT, UP, DOWN map to channels 0-5</li>
     * </ul>
     */
    private final int[][] moduleData = new int[GRID_SIZE][NUM_CHANNELS];

    /** UUID of the player who placed this machine framework. Empty string = unknown (old machines). */
    private String ownerUUID = "";

    /** When false, only the owner (or ops) can apply blueprints to this machine. Default: true (public). */
    private boolean isPublic = true;

    /**
     * Maps module slot indices to cleanup runnables registered by modules.
     * When the machine is destroyed, all registered cleanups are executed.
     * This allows modules to manage their own lifecycle (e.g., stopping sounds)
     * without the block entity knowing about module-specific state.
     */
    private final Map<Integer, Runnable> moduleCleanups = new HashMap<>();

    /**
     * Maps module slot indices to module-owned objects. Modules use this to store
     * references that need to persist across ticks (e.g., song players, timers).
     * The block entity does not interpret these objects — they belong entirely to the module.
     */
    private final Map<Integer, Object> moduleObjects = new HashMap<>();

    /**
     * Stores an arbitrary object owned by a module at the given slot index.
     * Used to persist references across ticks without the block entity knowing their type.
     */
    public void setModuleObject(int slotIndex, Object object) {
        if (object == null) {
            moduleObjects.remove(slotIndex);
        } else {
            moduleObjects.put(slotIndex, object);
        }
    }

    /**
     * Retrieves a module-owned object by slot index and checks that it has the expected type.
     *
     * @param slotIndex the module-owned storage key
     * @param objectType the expected object type
     * @return the stored object, or {@code null} if no object is stored
     * @throws ClassCastException if the stored object is not an instance of {@code objectType}
     */
    public <T> T getModuleObject(int slotIndex, Class<T> objectType) {
        return objectType.cast(moduleObjects.get(slotIndex));
    }

    /**
     * Registers a cleanup action for a module slot. Called when the machine is destroyed.
     * Modules should use this to stop any ongoing operations (sounds, timers, etc).
     */
    public void registerModuleCleanup(int slotIndex, Runnable cleanup) {
        moduleCleanups.put(slotIndex, cleanup);
    }

    /**
     * Runs all registered module cleanups and clears them.
     * Called when the machine block is destroyed.
     */
    public void runAllModuleCleanups() {
        for (Map.Entry<Integer, Runnable> entry : moduleCleanups.entrySet()) {
            try {
                entry.getValue().run();
            } catch (Exception e) {
                LOGGER.error("Module cleanup failed for slot {} at machine {}", entry.getKey(), worldPosition, e);
            }
        }
        moduleCleanups.clear();
        moduleObjects.clear();
    }

    /**
     * Retrieves the numerical value stored in a specific status channel for a given module slot.
     *
     * @param slot The module slot index (0-26)
     * @param channel The status channel index (0-5)
     * @return The stored value, or 0 if out of bounds
     */
    public int getModuleData(int slot, int channel) {
        if (slot < 0 || slot >= GRID_SIZE || channel < 0 || channel >= NUM_CHANNELS) return 0;
        return moduleData[slot][channel];
    }

    /**
     * Gets the redstone signal emitted from this machine toward a world direction by configured I/O modules.
     * I/O output is weak power only; the machine does not directly power the neighboring block.
     */
    public int getRedstoneOutputSignal(Direction worldDirection) {
        if (worldDirection == null) return 0;
        BlockState state = getBlockState();
        Direction machineFacing = state.hasProperty(MachineFrameworkBlock.FACING)
                ? state.getValue(MachineFrameworkBlock.FACING)
                : Direction.NORTH;
        RelativeSide side = RelativeSide.fromWorldDirection(worldDirection, machineFacing);
        int channel = IoModuleItem.getRedstoneSignalChannel(side);
        int strongestSignal = 0;

        for (int slot = 0; slot < moduleHandler.getSlots(); slot++) {
            ItemStack stack = moduleHandler.getStackInSlot(slot);
            if (!(stack.getItem() instanceof IoModuleItem ioModule)) continue;

            IoModuleItem.FaceMode mode = ioModule.getFaceModes(stack)
                    .getOrDefault(side, IoModuleItem.FaceMode.DISABLED);
            if (mode == IoModuleItem.FaceMode.OUTPUT || mode == IoModuleItem.FaceMode.OPEN) {
                strongestSignal = Math.max(strongestSignal, getModuleData(slot, channel));
            }
        }

        return Math.clamp(strongestSignal, 0, 15);
    }

    /**
     * Reads the strongest world redstone signal on faces configured as INPUT or OPEN.
     * This is the shared input bus used by redstone circuits and opted-in module controls.
     */
    public int getRedstoneInputSignal() {
        var level = getLevel();
        if (level == null) return 0;

        BlockState state = getBlockState();
        Direction machineFacing = state.hasProperty(MachineFrameworkBlock.FACING)
                ? state.getValue(MachineFrameworkBlock.FACING)
                : Direction.NORTH;
        int strongestInput = 0;

        for (RelativeSide side : RelativeSide.values()) {
            IoModuleItem.FaceMode mode = getEffectiveFaceMode(side);
            if (mode != IoModuleItem.FaceMode.INPUT && mode != IoModuleItem.FaceMode.OPEN) continue;

            Direction worldDirection = side.toWorldDirection(machineFacing);
            int signal = level.getSignal(worldPosition.relative(worldDirection), worldDirection);
            strongestInput = Math.max(strongestInput, Math.clamp(signal, 0, 15));
        }

        return strongestInput;
    }

    /**
     * Updates a specific status channel for a given module slot, syncs the block entity, and notifies neighbors.
     * Neighbor notifications allow changes to configured redstone outputs to be observed by world components.
     *
     * @param slot The module slot index (0-26)
     * @param channel The status channel index (0-5)
     * @param value The new value to store
     */
    public void setModuleData(int slot, int channel, int value) {
        if (slot >= 0 && slot < GRID_SIZE && channel >= 0 && channel < NUM_CHANNELS) {
            if (moduleData[slot][channel] != value) {
                moduleData[slot][channel] = value;
                setChanged();
                if (level != null && !level.isClientSide()) {
                    BlockState state = getBlockState();
                    level.sendBlockUpdated(worldPosition, state, state, 3);
                    level.updateNeighborsAt(worldPosition, state.getBlock());
                }
            }
        }
    }

    /**
     * Handler managing the 27 module slots within the machine framework.
     * Validates that only machine modules can be inserted, initializes IO module defaults,
     * and saves processing data when modules are extracted.
     */
    private final ItemStackHandler moduleHandler = new ItemStackHandler(GRID_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

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

    /**
     * Handler managing the internal item inventory slots corresponding to each grid position.
     * Each slot's stack limit can be overridden by installed modules via BaseModuleItem.getInventorySlotLimit().
     */
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

    /**
     * Constructs a new Machine Framework block entity.
     *
     * @param pos The block position
     * @param state The block state
     */
    public MachineFrameworkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_FRAMEWORK_BE.get(), pos, state);
    }

    /** Returns the module slot item handler (27 slots). */
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
     *
     * @param player The player attempting to modify the machine
     * @return True if the player has permission
     */
    public boolean canModify(Player player) {
        if (isPublic) return true;
        if (player.isCreative()) return true;
        if (player.getUUID().toString().equals(ownerUUID)) return true;
        return false;
    }

    /** Returns the internal inventory item handler (27 slots). */
    public ItemStackHandler getInventoryHandler() {
        return inventoryHandler;
    }

    /**
     * Checks if the module at the given slot is an Inventory module.
     *
     * @param slotIndex The module slot index (0-26)
     * @return True if an Inventory module is installed at that slot
     */
    public boolean hasInventoryModule(int slotIndex) {
        ItemStack stack = moduleHandler.getStackInSlot(slotIndex);
        return stack.getItem() instanceof InventoryModuleItem;
    }

    /**
     * Checks whether the module at the given slot exposes its own item handler via {@link com.thextrunner.praeteritum_futurum.api.module.IItemHandlerModule}
     * AND has declared its inventory slot as interactable via {@link com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem#hasInteractableInventorySlot()}.
     * This allows modules like the Jukebox Module to have their inventory slots be interactable by the player.
     *
     * @param slotIndex The module slot index (0-26)
     * @return True if the module has an interactable inventory slot
     */
    public boolean hasItemHandlerModule(int slotIndex) {
        ItemStack stack = moduleHandler.getStackInSlot(slotIndex);
        if (stack.getItem() instanceof com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem baseModule) {
            return baseModule.hasInteractableInventorySlot();
        }
        return false;
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

    /**
     * Creates the main machine config menu (opened with Yool).
     * This is the MenuProvider implementation used when the player opens the config screen.
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new MachineConfigMenu(containerId, playerInventory, this);
    }

    /**
     * Creates the machine info menu (opened by right-clicking without an item).
     * Shows a read-only view of the machine's current state.
     */
    public AbstractContainerMenu createInfoMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineInfoMenu(containerId, playerInventory, this);
    }

    /**
     * Saves all machine state to NBT for persistence.
     * Stores modules, inventory, module data channels, owner UUID, and public flag.
     */
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

    /**
     * Loads all machine state from NBT data.
     * Restores modules, inventory, module data channels, owner UUID, and public flag.
     */
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
     * Used by IO modules to find neighboring modules for item transfer.
     *
     * @param slotIndex The slot index (0-26)
     * @return Array of adjacent slot indices (1-4 elements, depending on grid position)
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

    /**
     * Resolves the effective IO face mode for a given relative side by combining
     * the face modes from all installed modules. If conflicting modes are detected
     * (e.g., INPUT and OUTPUT), the result is OPEN (both directions).
     *
     * @param side The relative side to check
     * @return The combined face mode (DISABLED, INPUT, OUTPUT, or OPEN)
     */
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

    /**
     * Combines two face modes according to IO priority rules.
     * OPEN takes precedence over everything. Conflicting INPUT/OUTPUT resolves to OPEN.
     *
     * @param current The current resolved mode
     * @param incoming The new mode to combine
     * @return The combined mode
     */
    private IoModuleItem.FaceMode combineFaceModes(IoModuleItem.FaceMode current, IoModuleItem.FaceMode incoming) {
        if (current == incoming) {
            return current;
        }
        if (current == IoModuleItem.FaceMode.DISABLED) {
            return incoming;
        }
        if (incoming == IoModuleItem.FaceMode.DISABLED) {
            return current;
        }
        return IoModuleItem.FaceMode.OPEN;
    }

    /**
     * Server-side tick logic running every game tick to process modules and handle item transfers
     * according to their configurations.
     * <p>
     * The tick is split into 3 phases:
     * 1. Pull — IO modules pull items from external inventories into the machine
     * 2. Process — eligible modules work on internal items; opted-in redstone control may skip a module's tick
     * 3. Push — IO modules push items from the machine to external inventories
     * <p>
     * I/O transfers (phases 1 & 3) respect Config.Machine.IO_TRANSFER_TICK_RATE. Module ticks are
     * visited every game tick, but redstone-controlled modules are skipped when their policy denies operation.
     * Redstone circuits are evaluated together on their own configured cadence and scheduled component ticks.
     */
    public static void serverTick(
            net.minecraft.world.level.Level level,
            BlockPos blockPos,
            BlockState state,
            MachineFrameworkBlockEntity blockEntity
    ) {
        if (level.isClientSide()) return;

        // Get the current tick count to determine if I/O transfers should run this tick.
        // The default config value is 8, meaning one transfer attempt every eight ticks.
        int tickRate = Config.getIoTransferTickRate();
        boolean shouldTransfer = (level.getGameTime() % tickRate == 0);

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

        // Phase 2: Give eligible modules their normal tick; redstone control may pause opted-in modules.
        for (int i = 0; i < blockEntity.moduleHandler.getSlots(); i++) {
            ItemStack moduleStack = blockEntity.moduleHandler.getStackInSlot(i);
            if (moduleStack.getItem() instanceof BaseModuleItem baseModule) {
                if (baseModule instanceof RedstoneModuleItem) continue;
                if (!baseModule.isRedstoneControlSatisfied(moduleStack, blockEntity, i)) continue;
                baseModule.tickModule(level, blockPos, blockEntity, i, moduleStack);
                // Write the modified stack back to the moduleHandler so changes (e.g., circuit NBT)
                // are persisted when the world saves.
                blockEntity.moduleHandler.setStackInSlot(i, moduleStack);
            }
        }

        // Redstone modules must evaluate together so shared-wall signals settle without
        // depending on the order in which the machine's module slots are visited.
        RedstoneModuleItem.tickMachineCircuits(level, blockPos, blockEntity);

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

    /**
     * Pulls items from an external inventory into adjacent modules.
     * Supports both Inventory modules (pulls into their inventory slot) and
     * any module that exposes an item handler via {@link BaseModuleItem#getItemHandlerForSide}
     * (e.g., Jukebox Module pulls into its own slot).
     */
    public void pullItemsFromSide(int moduleSlot, Direction side) {
        if (level == null) return;
        if (moduleSlot < 0 || moduleSlot >= GRID_SIZE) return;

        BlockPos sourcePos = worldPosition.relative(side);

        // Do not interact with inventories outside the active block-ticking area.
        boolean sourceChunkTicking = level instanceof ServerLevel serverLevel
                && serverLevel.shouldTickBlocksAt(sourcePos);
        if (!sourceChunkTicking) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.warn("[IO PULL] pos={}, from={}, chunkTicking=false — skipping capability lookup",
                        worldPosition, sourcePos);
            }
            return;
        }

        IItemHandler sourceHandler = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                sourcePos,
                side.getOpposite()
        );
        if (sourceHandler == null) return;

        int[] adjacentIndices = getAdjacentSlots(moduleSlot);
        for (int adj : adjacentIndices) {
            // Guard against out-of-bounds adjacent slot indices (e.g. from corrupted NBT data)
            if (adj < 0 || adj >= GRID_SIZE) continue;

            ItemStack adjModule = moduleHandler.getStackInSlot(adj);

            // Inventory modules: pull into their corresponding inventory slot
            if (adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                IItemHandler targetHandler = new SingleSlotItemHandler(inventoryHandler, adj);
                int before = countItems(inventoryHandler);
                transferItems(sourceHandler, targetHandler, moduleSlot, adj);
                int after = countItems(inventoryHandler);
                if (Config.isDebugLoggingEnabled() && after > before) {
                    LOGGER.info("[IO PULL] pos={}, from={}, chunkTicking={}, transferred {} items",
                            worldPosition, sourcePos, sourceChunkTicking, after - before);
                }
                continue;
            }

            // Other modules: pull into their exposed item handler (e.g., Jukebox Module)
            if (adjModule.getItem() instanceof com.thextrunner.praeteritum_futurum.api.module.IItemHandlerModule itemHandlerModule) {
                IItemHandlerModifiable targetHandler = itemHandlerModule.getItemHandlerForSide(
                        level, worldPosition, this, adj, adjModule, side
                );
                if (targetHandler != null) {
                    int before = countItems(targetHandler);
                    transferItems(sourceHandler, targetHandler, moduleSlot, adj);
                    int after = countItems(targetHandler);
                    if (Config.isDebugLoggingEnabled() && after > before) {
                        LOGGER.info("[IO PULL] pos={}, from={}, toModule={}, transferred {} items",
                                worldPosition, sourcePos, BuiltInRegistries.ITEM.getKey(adjModule.getItem()), after - before);
                    }
                }
            }
        }
    }

    /**
     * Pushes items from adjacent modules out to an external inventory.
     * Supports both Inventory modules (pushes from their inventory slot) and
     * any module that exposes an item handler via {@link BaseModuleItem#getItemHandlerForSide}
     * (e.g., Jukebox Module pushes from its own slot).
     */
    public void pushItemsToSide(int moduleSlot, Direction side) {
        if (level == null) return;
        if (moduleSlot < 0 || moduleSlot >= GRID_SIZE) return;

        BlockPos targetPos = worldPosition.relative(side);
        boolean targetChunkTicking = level instanceof ServerLevel serverLevel
                && serverLevel.shouldTickBlocksAt(targetPos);

        if (!targetChunkTicking) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.warn("[IO PUSH] pos={}, to={}, chunkTicking=false — skipping capability lookup",
                        worldPosition, targetPos);
            }
            return;
        }

        IItemHandler targetHandler = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                targetPos,
                side.getOpposite()
        );
        if (targetHandler == null) return;

        int[] adjacentIndices = getAdjacentSlots(moduleSlot);
        for (int adj : adjacentIndices) {
            // Guard against out-of-bounds adjacent slot indices (e.g. from corrupted NBT data)
            if (adj < 0 || adj >= GRID_SIZE) continue;

            ItemStack adjModule = moduleHandler.getStackInSlot(adj);

            // Inventory modules: push from their corresponding inventory slot
            if (adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                IItemHandler sourceHandler = new SingleSlotItemHandler(inventoryHandler, adj);
                int before = countItems(inventoryHandler);
                transferItems(sourceHandler, targetHandler, moduleSlot, adj);
                int after = countItems(inventoryHandler);
                if (Config.isDebugLoggingEnabled() && before > after) {
                    LOGGER.info("[IO PUSH] pos={}, to={}, chunkTicking={}, transferred {} items",
                            worldPosition, targetPos, targetChunkTicking, before - after);
                }
                continue;
            }

            // Other modules: push from their exposed item handler (e.g., Jukebox Module)
            if (adjModule.getItem() instanceof com.thextrunner.praeteritum_futurum.api.module.IItemHandlerModule itemHandlerModule) {
                IItemHandlerModifiable sourceHandler = itemHandlerModule.getItemHandlerForSide(
                        level, worldPosition, this, adj, adjModule, side
                );
                if (sourceHandler != null) {
                    int before = countItems(sourceHandler);
                    transferItems(sourceHandler, targetHandler, moduleSlot, adj);
                    int after = countItems(sourceHandler);
                    if (Config.isDebugLoggingEnabled() && before > after) {
                        LOGGER.info("[IO PUSH] pos={}, to={}, fromModule={}, transferred {} items",
                                worldPosition, targetPos, BuiltInRegistries.ITEM.getKey(adjModule.getItem()), before - after);
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
