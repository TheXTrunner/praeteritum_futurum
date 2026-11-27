package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuit;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuitState;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneComponent;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneSlotData;
import com.thextrunner.praeteritum_futurum.api.module.IConfigurableModule;
import com.thextrunner.praeteritum_futurum.api.module.IModuleMenuProvider;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.network.ClientboundRedstoneCircuitStatePacket;
import com.thextrunner.praeteritum_futurum.screen.RedstoneConfigMenu;
import com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigMenu;
import com.thextrunner.praeteritum_futurum.screen.ModMenuTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.ticks.TickPriority;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Redstone Module — acts like a modified double chest (54-slot inventory) for redstone components.
 * <p>
 * This is the entry point for the Logic Layer in the three-layer architecture. It bridges
 * the Inventory Layer (menu slots with ItemStacks) and the Logic Layer (circuit simulation).
 * When circuit evaluation is triggered by its configured cadence, a scheduled component tick,
 * or a component inventory/configuration change, it syncs inventory items to the circuit,
 * settles connected redstone modules as one circuit, and sends computed states to clients.
 * <p>
 * <b>Three-Layer Architecture:</b>
 * <ul>
 *   <li><b>Inventory → Logic sync</b> — {@link #syncCircuitFromInventory(ItemStack, HolderLookup.Provider)}
 *   reads items from menu slots and populates the {@link com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuit}</li>
 *   <li><b>Logic simulation</b> — {@link #tickMachineCircuits} settles all adjacent modules
 *   together, then creates state snapshots and sends them to clients</li>
 *   <li><b>Logic → Render sync</b> — sends {@link ClientboundRedstoneCircuitStatePacket}
 *   to all players tracking the chunk, so the client screen has current data</li>
 * </ul>
 * <p>
 * Players place redstone components (dust, repeater, comparator, torch, lever, button, observer,
 * redstone block, scrap-block walls, and the custom signal source) into the 54-slot grid. The
 * module stores these items in NBT (like a real chest) and evaluates
 * the circuit at a configurable interval when it has no timed components. Circuits containing
 * repeaters, comparators, attached torches, or observers are evaluated every game tick, and
 * scheduled transitions or component changes can trigger additional evaluations. In Config Mode,
 * left-click toggles levers/comparator modes, cycles repeater delay, presses buttons, or opens the
 * Signal Source editor; right-click rotates directional components or cycles torch, lever, and
 * button attachment.
 * <p>
 * <b>Server-side tick flow:</b>
 * <ol>
 *   <li>Sync all redstone module circuits from their inventories</li>
 *   <li>Read world inputs and snapshot previous shared-wall outputs</li>
 *   <li>Settle modules together, running 16 internal propagation passes per round</li>
 *   <li>Schedule future repeater/comparator/torch transitions; due transitions run from the machine block's scheduled tick</li>
 *   <li>Save circuit state and aggregate output channels</li>
 *   <li>Create state snapshots and send them to tracking clients</li>
 * </ol>
 * <p>
 * <b>Immediate click handling:</b>
 * After a component inventory change or server-handled configuration action, the server
 * immediately runs propagation and sends updated states to clients via
 * {@link #propagateAndSendState(Level, BlockPos)}. This provides visual feedback without waiting
 * for the next periodic or scheduled evaluation.
 */
public class RedstoneModuleItem extends LogisticsModuleItem implements IModuleMenuProvider {
    public static final int GRID_SIZE = RedstoneCircuit.GRID_SIZE; // 54 slots

    private static final int MAX_BOUNDARY_SETTLE_ROUNDS = MachineFrameworkBlockEntity.GRID_SIZE + 1;

    public RedstoneModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/redstone_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16,
                16, 16
        );
        super.renderModuleOverlay(graphics, slot, stack, blockEntity);
    }

    @Override
    public void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos) {
        SimpleMenuProvider provider = new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new RedstoneConfigMenu(
                        ModMenuTypes.REDSTONE_MODULE_CONFIG_MENU.get(),
                        containerId, playerInventory, moduleStack, slotIndex, blockPos),
                moduleStack.getHoverName()
        );

        player.openMenu(provider, buf -> {
            ItemStack.STREAM_CODEC.encode(buf, moduleStack);
            buf.writeInt(slotIndex);
            buf.writeBlockPos(blockPos);
        });
    }

    @Override
    public void addModuleSlots(Inventory playerInventory, ItemStack moduleStack, BlockPos blockPos, Consumer<Slot> slotAdder) {
        // Load the item inventory from NBT (like a real chest)
        ItemStackHandler inventory = getRedstoneInventory(moduleStack, playerInventory.player.registryAccess());

        // Add slots in a 9×6 grid layout (matching double chest: 9 columns × 6 rows)
        for (int row = 0; row < RedstoneCircuit.GRID_HEIGHT; row++) {
            for (int col = 0; col < RedstoneCircuit.GRID_WIDTH; col++) {
                int index = row * RedstoneCircuit.GRID_WIDTH + col;
                slotAdder.accept(new SlotItemHandler(inventory, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        // Only allow redstone component items
                        return isRedstoneComponentItem(stack);
                    }

                    @Override
                    public int getMaxStackSize(@NotNull ItemStack stack) {
                        // Max 1 item per slot, like modules in the config screen
                        return 1;
                    }

                    @Override
                    public boolean mayPickup(@NotNull Player player) {
                        return true; // Allow picking up components
                    }
                });
            }
        }
    }

    @Override
    public void saveMenuData(GenericModuleConfigMenu menu, HolderLookup.Provider registries) {
        ItemStackHandler inventory = getRedstoneInventory(menu.getModuleStack(), registries);
        for (int slot = 0; slot < GRID_SIZE && slot < menu.slots.size(); slot++) {
            inventory.setStackInSlot(slot, menu.slots.get(slot).getItem());
        }
        saveRedstoneInventory(menu.getModuleStack(), inventory, registries);
    }

    @Override
    public void handleMenuClick(GenericModuleConfigMenu menu, int slotId, int button, ClickType clickType, Player player) {
        // All clicks use vanilla slot handling (place, remove, swap, quick-craft drag, etc.).
        // Component configuration (toggle lever/mode, rotate, cycle delay/attachment) is handled
        // exclusively in Config Mode via ServerboundRedstoneConfigPacket sent from the client screen.
        menu.superClicked(slotId, button, clickType, player);

        // On the server, sync the RedstoneInventory NBT from the current menu slots
        // so the server tick (which reads from NBT) sees the latest grid layout.
        // This ensures the server and client always agree on what's in the grid.
        if (!player.level().isClientSide()) {
            ItemStackHandler inventory = getRedstoneInventory(menu.getModuleStack(), player.registryAccess());
            for (int i = 0; i < GRID_SIZE; i++) {
                if (i < menu.slots.size()) {
                    inventory.setStackInSlot(i, menu.slots.get(i).getItem());
                }
            }
            saveRedstoneInventory(menu.getModuleStack(), inventory, player.registryAccess());

            // Persist the updated stack to the machine block entity
            if (player.level().getBlockEntity(menu.getBlockPos()) instanceof MachineFrameworkBlockEntity blockEntity) {
                blockEntity.getModuleHandler().setStackInSlot(menu.getSlotIndexInMachine(), menu.getModuleStack());
            }
            // Immediately propagate and send updated state to the client
            propagateAndSendState(player.level(), menu.getBlockPos());
        }
    }

    /**
     * Recomputes the machine's redstone circuits after a player changes a component.
     * This provides immediate visual feedback without waiting for the next tick.
     */
    private void propagateAndSendState(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineFrameworkBlockEntity blockEntity) {
            recomputeMachineCircuits(level, pos, blockEntity);
        }
    }

    /**
     * Evaluates all redstone modules in a machine together so adjacent modules behave as
     * one extended circuit instead of depending on module slot tick order.
     */
    public static void tickMachineCircuits(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity) {
        if (level.isClientSide()) return;
        int tickRate = Config.getRedstoneEvaluationTickRate();
        boolean hasTimedComponent = false;
        for (int slot = 0; slot < blockEntity.getModuleHandler().getSlots(); slot++) {
            ItemStack stack = blockEntity.getModuleHandler().getStackInSlot(slot);
            if (stack.getItem() instanceof RedstoneModuleItem) {
                RedstoneCircuit circuit = RedstoneCircuit.fromStack(stack, level.registryAccess());
                if (circuit.hasRepeater() || circuit.hasComparator() || circuit.hasAttachedTorch() ||
                    circuit.hasObserver()) {
                    hasTimedComponent = true;
                }
            }
            if (hasTimedComponent) {
                break;
            }
        }
        if (!hasTimedComponent && level.getGameTime() % tickRate != 0) return;
        recomputeMachineCircuits(level, pos, blockEntity);
    }

    /**
     * Handles a machine scheduled block tick in Minecraft's block-tick phase. Timed circuit
     * transitions are processed here so they share the same world scheduler as vanilla redstone.
     */
    public static void tickScheduledMachineCircuits(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineFrameworkBlockEntity blockEntity) {
            recomputeMachineCircuits(level, pos, blockEntity, true);
        }
    }

    /** Recomputes and syncs all redstone circuits in the machine immediately. */
    public static void recomputeMachineCircuits(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity) {
        recomputeMachineCircuits(level, pos, blockEntity, false);
    }

    private static void recomputeMachineCircuits(Level level, BlockPos pos,
                                                  MachineFrameworkBlockEntity blockEntity,
                                                  boolean processDueTicks) {
        if (level.isClientSide()) return;
        long gameTime = level.getGameTime();

        int moduleCount = blockEntity.getModuleHandler().getSlots();
        RedstoneCircuit[] circuits = new RedstoneCircuit[moduleCount];
        ItemStack[] moduleStacks = new ItemStack[moduleCount];
        int[] machineWorldInputs = gatherWorldInputSignals(blockEntity);
        int[][] worldInputs = new int[moduleCount][];

        for (int slot = 0; slot < moduleCount; slot++) {
            ItemStack stack = blockEntity.getModuleHandler().getStackInSlot(slot);
            if (stack.getItem() instanceof RedstoneModuleItem redstoneModule) {
                circuits[slot] = redstoneModule.syncCircuitFromInventory(stack, level.registryAccess());
                moduleStacks[slot] = stack;
                worldInputs[slot] = machineWorldInputs;
            }
        }

        int[][] aggregateOutputs = settleMachineCircuits(circuits, worldInputs);
        for (RedstoneCircuit circuit : circuits) {
            if (circuit != null) circuit.updateObserverInputs(gameTime);
        }

        if (processDueTicks) {
            // Components due together read the same settled inputs before shared modules re-settle.
            for (int event = 0; event < moduleCount * RedstoneCircuit.GRID_SIZE; event++) {
                long nextTickTime = Long.MAX_VALUE;
                for (int slot = 0; slot < moduleCount; slot++) {
                    RedstoneCircuit circuit = circuits[slot];
                    if (circuit == null) continue;
                    long repeaterTickTime = circuit.getNextRepeaterTickTime();
                    long comparatorTickTime = circuit.getNextComparatorTickTime();
                    long torchTickTime = circuit.getNextTorchTickTime();
                    long buttonTickTime = circuit.getNextButtonTickTime();
                    long observerTickTime = circuit.getNextObserverTickTime();
                    if (repeaterTickTime >= 0 && repeaterTickTime <= gameTime && repeaterTickTime < nextTickTime) {
                        nextTickTime = repeaterTickTime;
                    }
                    if (comparatorTickTime >= 0 && comparatorTickTime <= gameTime && comparatorTickTime < nextTickTime) {
                        nextTickTime = comparatorTickTime;
                    }
                    if (torchTickTime >= 0 && torchTickTime <= gameTime && torchTickTime < nextTickTime) {
                        nextTickTime = torchTickTime;
                    }
                    if (buttonTickTime >= 0 && buttonTickTime <= gameTime && buttonTickTime < nextTickTime) {
                        nextTickTime = buttonTickTime;
                    }
                    if (observerTickTime >= 0 && observerTickTime <= gameTime && observerTickTime < nextTickTime) {
                        nextTickTime = observerTickTime;
                    }
                }
                if (nextTickTime == Long.MAX_VALUE) break;

                boolean outputChanged = false;
                for (RedstoneCircuit circuit : circuits) {
                    if (circuit != null) {
                        outputChanged |= circuit.processRepeaterTicksAt(nextTickTime);
                        outputChanged |= circuit.processComparatorTicksAt(nextTickTime);
                        outputChanged |= circuit.processTorchTicksAt(nextTickTime);
                        outputChanged |= circuit.processButtonTicksAt(nextTickTime);
                        outputChanged |= circuit.processObserverTicksAt(nextTickTime);
                    }
                }
                if (outputChanged) {
                    aggregateOutputs = settleMachineCircuits(circuits, worldInputs);
                    for (RedstoneCircuit circuit : circuits) {
                        if (circuit != null) {
                            circuit.updateObserverInputs(gameTime);
                            circuit.scheduleComparatorTransitions(gameTime);
                            circuit.scheduleTorchTransitions(gameTime);
                        }
                    }
                }
            }
        }

        for (RedstoneCircuit circuit : circuits) {
            if (circuit != null) {
                circuit.scheduleRepeaterTransitions(gameTime);
                circuit.scheduleComparatorTransitions(gameTime);
                circuit.scheduleTorchTransitions(gameTime);
            }
        }

        for (int slot = 0; slot < moduleCount; slot++) {
            RedstoneCircuit circuit = circuits[slot];
            if (circuit == null) continue;

            ItemStack stack = moduleStacks[slot];
            circuit.toStack(stack);
            blockEntity.getModuleHandler().setStackInSlot(slot, stack);

            int[] outputs = aggregateOutputs[slot];
            for (int channel = 0; channel < Math.min(outputs.length, 4); channel++) {
                blockEntity.setModuleData(slot, channel, outputs[channel]);
            }

            if (level instanceof ServerLevel serverLevel) {
                RedstoneCircuitState state = circuit.computeState();
                ClientboundRedstoneCircuitStatePacket packet = new ClientboundRedstoneCircuitStatePacket(
                        pos, slot, state.serializeNBT());
                PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(pos), packet);
            }
        }

        publishIoModuleSignals(blockEntity, aggregateOutputs);

        boolean hasPendingTick = false;
        for (RedstoneCircuit circuit : circuits) {
            if (circuit != null && (circuit.getNextRepeaterTickTime() >= 0 ||
                    circuit.getNextComparatorTickTime() >= 0 || circuit.getNextTorchTickTime() >= 0 ||
                    circuit.getNextButtonTickTime() >= 0 || circuit.getNextObserverTickTime() >= 0)) {
                hasPendingTick = true;
                break;
            }
        }
        if (hasPendingTick && level instanceof ServerLevel serverLevel &&
                level.getBlockState(pos).getBlock() instanceof MachineFrameworkBlock machineBlock) {
            // Keep one scheduled callback per world tick while transitions are pending. A machine
            // contains many independent grid slots, so a one-tick poll in the scheduled-tick phase
            // avoids one later machine event masking a newly-added earlier event at the same world pos.
            serverLevel.scheduleTick(pos, machineBlock, 1,
                    getNextScheduledTickPriority(circuits, gameTime + 1));
        }
    }

    private static TickPriority getNextScheduledTickPriority(RedstoneCircuit[] circuits, long nextGameTime) {
        boolean hasDueTransition = false;
        boolean hasTurnOffTransition = false;
        for (RedstoneCircuit circuit : circuits) {
            if (circuit == null) continue;
            for (int slot = 0; slot < RedstoneCircuit.GRID_SIZE; slot++) {
                RedstoneSlotData data = circuit.getSlotData(slot);
                if (data == null) continue;

                boolean repeaterDue = data.repeaterScheduledGameTime >= 0 &&
                        data.repeaterScheduledGameTime <= nextGameTime;
                boolean comparatorDue = data.comparatorScheduledGameTime >= 0 &&
                        data.comparatorScheduledGameTime <= nextGameTime;
                boolean torchDue = data.type == RedstoneComponent.TORCH && data.torchRotation < 4 &&
                        data.torchScheduledGameTime >= 0 && data.torchScheduledGameTime <= nextGameTime;
                if (!repeaterDue && !comparatorDue && !torchDue) continue;
                hasDueTransition = true;

                if (repeaterDue && !data.repeaterLocked && data.repeaterPowered && data.inputStrength == 0) {
                    hasTurnOffTransition = true;
                }
                if (comparatorDue && data.outputSignal > 0 && data.comparatorTargetSignal == 0) {
                    hasTurnOffTransition = true;
                }
                if (torchDue && data.torchPowered && data.inputStrength > 0) {
                    hasTurnOffTransition = true;
                }
            }
        }

        if (!hasDueTransition) return TickPriority.NORMAL;
        return hasTurnOffTransition ? TickPriority.VERY_HIGH : TickPriority.HIGH;
    }

    private static int[][] settleMachineCircuits(RedstoneCircuit[] circuits, int[][] worldInputs) {
        int[][][] boundaryOutputs = readBoundaryWallOutputs(circuits);
        int[][] aggregateOutputs = new int[circuits.length][];
        for (int round = 0; round < MAX_BOUNDARY_SETTLE_ROUNDS; round++) {
            for (int slot = 0; slot < circuits.length; slot++) {
                RedstoneCircuit circuit = circuits[slot];
                if (circuit == null) continue;
                int[][] boundaryInputs = gatherSharedWallInputs(slot, boundaryOutputs);
                aggregateOutputs[slot] = circuit.propagateSignals(worldInputs[slot], boundaryInputs, round == 0);
            }

            int[][][] nextBoundaryOutputs = readBoundaryWallOutputs(circuits);
            boolean settled = Objects.deepEquals(boundaryOutputs, nextBoundaryOutputs);
            boundaryOutputs = nextBoundaryOutputs;
            if (settled) break;
        }
        return aggregateOutputs;
    }

    private static int[] gatherWorldInputSignals(MachineFrameworkBlockEntity blockEntity) {
        int strongestInput = blockEntity.getRedstoneInputSignal();
        // The basic I/O module is a shared bus: the strongest enabled face input is
        // available to every circuit edge. Edge-specific routing can be added later.
        return new int[]{strongestInput, strongestInput, strongestInput, strongestInput};
    }

    private static void publishIoModuleSignals(MachineFrameworkBlockEntity blockEntity,
                                               int[][] aggregateOutputs) {
        int strongestOutput = 0;
        for (int[] moduleOutputs : aggregateOutputs) {
            if (moduleOutputs == null) continue;
            for (int output : moduleOutputs) {
                strongestOutput = Math.max(strongestOutput, output);
            }
        }

        for (int slot = 0; slot < blockEntity.getModuleHandler().getSlots(); slot++) {
            ItemStack stack = blockEntity.getModuleHandler().getStackInSlot(slot);
            if (!(stack.getItem() instanceof IoModuleItem ioModule)) continue;

            Map<RelativeSide, IoModuleItem.FaceMode> faceModes = ioModule.getFaceModes(stack);
            for (RelativeSide side : RelativeSide.values()) {
                IoModuleItem.FaceMode mode = faceModes.getOrDefault(side, IoModuleItem.FaceMode.DISABLED);
                int signal = mode == IoModuleItem.FaceMode.OUTPUT || mode == IoModuleItem.FaceMode.OPEN
                        ? strongestOutput
                        : 0;
                blockEntity.setModuleData(slot, IoModuleItem.getRedstoneSignalChannel(side), signal);
            }
        }
    }

    private static int[][] gatherSharedWallInputs(int moduleSlot, int[][][] boundaryOutputs) {
        int[][] inputs = new int[][]{
                new int[RedstoneCircuit.GRID_WIDTH],
                new int[RedstoneCircuit.GRID_WIDTH],
                new int[RedstoneCircuit.GRID_HEIGHT],
                new int[RedstoneCircuit.GRID_HEIGHT]
        };

        addAdjacentWallSignals(moduleSlot, GridDirection.UP, Direction.SOUTH, Direction.NORTH, boundaryOutputs, inputs);
        addAdjacentWallSignals(moduleSlot, GridDirection.DOWN, Direction.NORTH, Direction.SOUTH, boundaryOutputs, inputs);
        addAdjacentWallSignals(moduleSlot, GridDirection.LEFT, Direction.EAST, Direction.WEST, boundaryOutputs, inputs);
        addAdjacentWallSignals(moduleSlot, GridDirection.RIGHT, Direction.WEST, Direction.EAST, boundaryOutputs, inputs);
        return inputs;
    }

    private static void addAdjacentWallSignals(int moduleSlot, GridDirection neighborDirection,
                                               Direction neighborOutputDirection, Direction thisInputDirection,
                                               int[][][] boundaryOutputs, int[][] inputs) {
        int neighborSlot = BaseModuleItem.getAdjacentSlotIndex(moduleSlot, neighborDirection);
        if (neighborSlot < 0 || boundaryOutputs[neighborSlot] == null) return;

        int outputSide = RedstoneCircuit.getBoundarySideIndex(neighborOutputDirection);
        int inputSide = RedstoneCircuit.getBoundarySideIndex(thisInputDirection);
        for (int position = 0; position < RedstoneCircuit.getBoundaryEdgeLength(thisInputDirection); position++) {
            inputs[inputSide][position] = Math.max(inputs[inputSide][position],
                    boundaryOutputs[neighborSlot][outputSide][position]);
        }
    }

    private static int[][][] readBoundaryWallOutputs(RedstoneCircuit[] circuits) {
        int[][][] outputs = new int[circuits.length][][];
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int slot = 0; slot < circuits.length; slot++) {
            RedstoneCircuit circuit = circuits[slot];
            if (circuit == null) continue;

            outputs[slot] = new int[4][];
            for (Direction direction : directions) {
                int side = RedstoneCircuit.getBoundarySideIndex(direction);
                int edgeLength = RedstoneCircuit.getBoundaryEdgeLength(direction);
                outputs[slot][side] = new int[edgeLength];
                for (int position = 0; position < edgeLength; position++) {
                    outputs[slot][side][position] = circuit.getBoundaryWallOutput(direction, position);
                }
            }
        }
        return outputs;
    }

    @Override
    public ItemStack handleQuickMove(GenericModuleConfigMenu menu, Player player, int index) {
        // Delegate to vanilla shift-click handling — works like a normal double chest.
        // The SlotItemHandler slots already have mayPlace() (redstone-only filter)
        // and getMaxStackSize() (1 per slot) overrides, so vanilla respects those rules.
        return menu.superQuickMoveStack(player, index);
    }

    // --- Item Inventory Storage (NBT-backed, like a real chest) ---

    /**
     * Loads the 54-slot item inventory from the module's NBT data.
     */
    public static ItemStackHandler getRedstoneInventory(ItemStack stack, HolderLookup.Provider registries) {
        ItemStackHandler handler = new ItemStackHandler(GRID_SIZE);
        CompoundTag filterTag = getCustomTag(stack, "RedstoneInventory");
        if (!filterTag.isEmpty()) {
            handler.deserializeNBT(registries, filterTag);
        }
        return handler;
    }

    @Override
    public CompoundTag getBlueprintData(ItemStack stack) {
        CompoundTag blueprintData = super.getBlueprintData(stack);
        if (blueprintData == null || !blueprintData.contains("Circuit", Tag.TAG_COMPOUND)) return blueprintData;

        CompoundTag circuitData = blueprintData.getCompound("Circuit").copy();
        ListTag slots = circuitData.getList("Slots", Tag.TAG_COMPOUND);
        String[] transientKeys = {
                "TorchPowered", "TorchScheduledGameTime", "TorchBurnedOut", "TorchBurnoutUntil",
                "TorchRecentOffTimes", "TorchLastInputStrength", "RepeaterPowered",
                "RepeaterScheduledGameTime", "Locked", "LockOutput", "ComparatorOutputSignal",
                "ComparatorScheduledGameTime", "ButtonPowered", "ButtonScheduledGameTime",
                "ObserverPowered", "ObserverScheduledGameTime", "ObserverLastObservedRotation",
                "ObserverObservedStateSignature"
        };

        for (int i = 0; i < slots.size(); i++) {
            CompoundTag slot = slots.getCompound(i);
            for (String key : transientKeys) {
                slot.remove(key);
            }
        }

        circuitData.remove("BoundaryWallOutputs");
        blueprintData.put("Circuit", circuitData);
        return blueprintData;
    }

    /**
     * Saves the 54-slot item inventory to the module's NBT data.
     */
    public static void saveRedstoneInventory(ItemStack stack, ItemStackHandler handler, HolderLookup.Provider registries) {
        CompoundTag serialized = handler.serializeNBT(registries);
        modifyCustomData(stack, "RedstoneInventory", tag -> {
            for (String key : serialized.getAllKeys()) {
                tag.put(key, Objects.requireNonNull(serialized.get(key)));
            }
        });

        // Update stack size to reflect whether the module has components
        boolean hasItems = false;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        updateStackSize(stack, hasItems);
    }

    // --- Circuit Data Sync ---

    /**
     * Syncs the RedstoneCircuit metadata from the item inventory.
     * <p>
     * This is the Inventory → Logic bridge in the three-layer architecture. It reads
     * ItemStacks from the menu slots and populates the circuit's slot data. It preserves
     * existing component state (rotation, lever, lock, etc.) when the component type matches,
     * so that user interactions (right-click to rotate, toggle lever) are not lost.
     * <p>
     * Only creates new slot data when a component is newly placed, and clears when removed.
     *
     * @param stack the module item stack containing the circuit NBT
     * @param registries the registry lookup provider
     * @return the updated circuit with synced component data
     */
    private RedstoneCircuit syncCircuitFromInventory(ItemStack stack, HolderLookup.Provider registries) {
        ItemStackHandler inventory = getRedstoneInventory(stack, registries);
        RedstoneCircuit circuit = RedstoneCircuit.fromStack(stack, registries);

        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack slotItem = inventory.getStackInSlot(i);
            if (slotItem.isEmpty()) {
                // Slot is empty — clear circuit data if present
                if (!circuit.isEmpty(i)) {
                    circuit.setComponent(i, null);
                }
            } else {
                RedstoneComponent component = getComponentFromItem(slotItem);
                if (component != null) {
                    // Check if the slot already has this component — preserve existing state
                    RedstoneComponent existing = circuit.getComponent(i);
                    if (existing == null || existing != component) {
                        // New or changed component — create fresh slot data
                        circuit.setComponent(i, component);
                    }
                    // If same component, keep existing rotation/lever/wall attachment state
                    if (component == RedstoneComponent.BUTTON) {
                        circuit.getSlotData(i).buttonDurationGameTicks = getButtonPulseDurationGameTicks(slotItem);
                    }
                }
            }
        }
        circuit.toStack(stack);
        return circuit;
    }


    /**
     * Checks if an item stack is a valid redstone component (vanilla items).
     */
    private boolean isRedstoneComponentItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return getComponentFromItem(stack) != null;
    }

    /**
     * Maps vanilla redstone items to RedstoneComponent enum values.
     * - Redstone Dust → Wire
     * - Repeater → Repeater
     * - Comparator → Comparator
     * - Redstone Torch → Torch
     * - Lever → Lever
     * - Vanilla buttons → Button
     * - Redstone Block → Redstone Block
     * - Signal Source → configurable analog source
     */
    public static RedstoneComponent getComponentFromItem(ItemStack stack) {
        return switch (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()) {
            case "redstone", "redstone_wire" -> RedstoneComponent.WIRE;
            case "repeater" -> RedstoneComponent.REPEATER;
            case "comparator" -> RedstoneComponent.COMPARATOR;
            case "redstone_torch" -> RedstoneComponent.TORCH;
            case "lever" -> RedstoneComponent.LEVER;
            case "stone_button", "polished_blackstone_button", "oak_button", "spruce_button", "birch_button",
                 "jungle_button", "acacia_button", "dark_oak_button", "mangrove_button", "cherry_button",
                 "bamboo_button", "crimson_button", "warped_button" -> RedstoneComponent.BUTTON;
            case "observer" -> RedstoneComponent.OBSERVER;
            case "signal_source" -> RedstoneComponent.SIGNAL_SOURCE;
            case "redstone_block" -> RedstoneComponent.REDSTONE_BLOCK;
            case "scrap_block" -> RedstoneComponent.WALL;
            default -> null;
        };
    }

    /** Returns the vanilla button pulse duration for a button item stack. */
    public static int getButtonPulseDurationGameTicks(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (path) {
            case "stone_button", "polished_blackstone_button" -> RedstoneCircuit.STONE_BUTTON_DURATION_GAME_TICKS;
            default -> RedstoneCircuit.WOODEN_BUTTON_DURATION_GAME_TICKS;
        };
    }

    /**
     * Creates a vanilla item stack for a redstone component.
     */
    private ItemStack getComponentItemStack(RedstoneComponent component) {
        Item item = switch (component) {
            case WIRE -> Items.REDSTONE;
            case REPEATER -> Items.REPEATER;
            case COMPARATOR -> Items.COMPARATOR;
            case TORCH -> Items.REDSTONE_TORCH;
            case LEVER -> Items.LEVER;
            case REDSTONE_BLOCK -> Items.REDSTONE_BLOCK;
            case WALL -> ModItems.SCRAP_BLOCK_ITEM.get();
            case BUTTON -> Items.STONE_BUTTON;
            case OBSERVER -> Items.OBSERVER;
            case SIGNAL_SOURCE -> ModItems.SIGNAL_SOURCE.get();
        };
        return new ItemStack(item);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        if (context.registries() != null) {
            ItemStackHandler inventory = getRedstoneInventory(stack, context.registries());
            int componentCount = 0;
            for (int i = 0; i < GRID_SIZE; i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) componentCount++;
            }

            tooltipComponents.add(Component.literal("Circuit: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(componentCount + " / " + GRID_SIZE + " slots").withStyle(
                            componentCount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED)));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
