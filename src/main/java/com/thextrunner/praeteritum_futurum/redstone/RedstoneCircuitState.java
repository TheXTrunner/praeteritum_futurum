package com.thextrunner.praeteritum_futurum.redstone;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * A lightweight snapshot of redstone circuit state intended for client rendering.
 * <p>
 * This is the bridge between the Logic Layer and the Render Layer in the three-layer
 * architecture. The server creates this snapshot after running propagation, serializes
 * it to NBT, and sends it to the client via {@link com.thextrunner.praeteritum_futurum.network.ClientboundRedstoneCircuitStatePacket}.
 * The client screen stores this state and reads from it for rendering overlays and tooltips,
 * without local propagation or inventory access.
 * <p>
 * <b>Three-Layer Architecture:</b>
 * <ul>
 *   <li><b>Inventory Layer</b> — stores ItemStacks in menu slots (RedstoneConfigMenu)</li>
 *   <li><b>Logic Layer</b> — holds component properties and runs circuit simulation (RedstoneCircuit)</li>
 *   <li><b>Render Layer</b> — displays data from this snapshot (RedstoneConfigScreen)</li>
 * </ul>
 * <p>
 * This class holds all the data the render layer needs: component types, rotations,
 * lever states, lock states, and computed signal strengths — without requiring the
 * client to run propagation or access inventory slots directly.
 * <p>
 * <b>Ownership:</b> Snapshot data is stored in private arrays and exposed through per-slot read
 * methods. NBT serialization creates detached data. The server refreshes the snapshot when circuit
 * state is synchronized; circuits without timed components may only be evaluated at the configured
 * interval, so a snapshot is not guaranteed to be replaced every game tick.
 */
public class RedstoneCircuitState {

    public static final int GRID_SIZE = RedstoneCircuit.GRID_SIZE; // 54 slots
    public static final int GRID_WIDTH = RedstoneCircuit.GRID_WIDTH; // 9 columns
    public static final int GRID_HEIGHT = RedstoneCircuit.GRID_HEIGHT; // 6 rows

    /** RedstoneComponent ID per grid slot; -1 means the slot is empty. */
    private final int[] componentIds = new int[GRID_SIZE];

    /** Rotation per slot (0-3 for N/E/S/W). Used by REPEATER, COMPARATOR, and OBSERVER. */
    private final int[] rotations = new int[GRID_SIZE];

    /** Lever on/off state per slot. Only meaningful for LEVER. */
    private final boolean[] leverStates = new boolean[GRID_SIZE];

    /** Repeater locked state per slot. True when side-locked by another repeater/comparator output. */
    private final boolean[] repeaterLocked = new boolean[GRID_SIZE];

    /** Repeater frozen output value per slot (true=ON/15, false=OFF/0). Only meaningful when repeaterLocked is true. */
    private final boolean[] repeaterLockOutput = new boolean[GRID_SIZE];

    /** Repeater delay setting per slot (1-4 redstone ticks). */
    private final int[] repeaterDelays = new int[GRID_SIZE];

    /** Torch support direction per slot (0-4 for N/E/S/W/Standing). Only meaningful for TORCH. */
    private final int[] torchRotations = new int[GRID_SIZE];

    /** Lever attachment direction per slot (0-4 for N/E/S/W/Standing). Only meaningful for LEVER. */
    private final int[] leverRotations = new int[GRID_SIZE];

    /** Computed signal strength per slot (0-15). Calculated by server-side propagation. */
    private final int[] signalStrengths = new int[GRID_SIZE];

    /** Horizontal connection arms for each wire, encoded with one bit per direction's 3D data value. */
    private final int[] wireConnectionMasks = new int[GRID_SIZE];

    /** Output signal strength per slot (0-15). Used by REPEATER, COMPARATOR, and OBSERVER. */
    private final int[] outputSignals = new int[GRID_SIZE];

    /** Rear input strength used during the latest propagation pass for REPEATER and COMPARATOR. */
    private final int[] inputStrengths = new int[GRID_SIZE];

    /** Comparator subtract mode per slot. True = subtract mode, False = compare mode. Only meaningful for COMPARATOR. */
    private final boolean[] comparatorSubtract = new boolean[GRID_SIZE];

    /** Button pulse duration in game ticks per slot. Only meaningful for BUTTON. */
    private final int[] buttonDurations = new int[GRID_SIZE];

    /** Button attachment direction per slot (0-4 for N/E/S/W/floor-perched). */
    private final int[] buttonRotations = new int[GRID_SIZE];

    /** Configured strength per SIGNAL_SOURCE slot (0-15). */
    private final int[] signalSourceStrengths = new int[GRID_SIZE];

    /**
     * Creates an empty circuit state with all slots cleared.
     */
    public RedstoneCircuitState() {
        for (int i = 0; i < GRID_SIZE; i++) {
            componentIds[i] = -1;
            repeaterDelays[i] = 1;
            buttonDurations[i] = RedstoneCircuit.STONE_BUTTON_DURATION_GAME_TICKS;
            buttonRotations[i] = 4;
        }
    }

    /**
     * Creates a snapshot from a RedstoneCircuit after propagation.
     *
     * @param circuit The circuit to snapshot
     */
    public RedstoneCircuitState(RedstoneCircuit circuit) {
        for (int i = 0; i < GRID_SIZE; i++) {
            RedstoneSlotData data = circuit.getSlotData(i);
            if (data == null || data.type == null) {
                componentIds[i] = -1;
            } else {
                componentIds[i] = data.type.getId();
                rotations[i] = data.rotation;
                torchRotations[i] = data.torchRotation;
                leverStates[i] = data.leverOn;
                leverRotations[i] = data.leverRotation;
                repeaterLocked[i] = data.repeaterLocked;
                repeaterLockOutput[i] = data.repeaterLockOutput;
                repeaterDelays[i] = data.repeaterDelay;
                comparatorSubtract[i] = data.comparatorSubtract;
                buttonDurations[i] = data.buttonDurationGameTicks;
                buttonRotations[i] = data.buttonRotation;
                signalSourceStrengths[i] = data.signalSourceStrength;
                signalStrengths[i] = data.signalStrength;
                wireConnectionMasks[i] = data.type == RedstoneComponent.WIRE ? circuit.getWireConnectionMask(i) : 0;
                outputSignals[i] = data.outputSignal;
                inputStrengths[i] = data.inputStrength;
            }
        }
    }

    /**
     * Returns the component type at the given slot index.
     * @return The component type, or null if empty
     */
    public RedstoneComponent getComponent(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return null;
        int id = componentIds[slotIndex];
        if (id < 0) return null;
        return RedstoneComponent.fromId(id);
    }

    /**
     * Returns the rotation at the given slot index.
     */
    public int getRotation(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        return rotations[slotIndex];
    }

    /**
     * Returns the lever state at the given slot index.
     */
    public boolean getLeverState(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return false;
        return leverStates[slotIndex];
    }

    /**
     * Returns whether the repeater at the given slot index is locked.
     */
    public boolean isRepeaterLocked(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return false;
        return repeaterLocked[slotIndex];
    }

    /**
     * Returns the frozen output value of a locked repeater.
     */
    public boolean getRepeaterLockOutput(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return false;
        return repeaterLockOutput[slotIndex];
    }

    /** Returns the repeater delay setting in redstone ticks (1-4). */
    public int getRepeaterDelay(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 1;
        return repeaterDelays[slotIndex];
    }

    /**
     * Returns the torch rotation at the given slot index.
     */
    public int getTorchRotation(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        return torchRotations[slotIndex];
    }

    /**
     * Returns the lever attachment rotation at the given slot index.
     */
    public int getLeverRotation(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 4;
        return leverRotations[slotIndex];
    }

    /**
     * Returns the signal strength at the given slot index.
     */
    public int getSignalStrength(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        return signalStrengths[slotIndex];
    }

    /** Returns whether the rendered wire has a connection arm in the given direction. */
    public boolean isWireConnected(int slotIndex, Direction direction) {
        return slotIndex >= 0 && slotIndex < GRID_SIZE && direction != null &&
                (wireConnectionMasks[slotIndex] & (1 << direction.get3DDataValue())) != 0;
    }

    /**
     * Returns the output signal strength at the given slot index.
     * Only meaningful for REPEATER and COMPARATOR (0=off, >0=on).
     */
    public int getOutputSignal(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        return outputSignals[slotIndex];
    }

    /**
     * Returns whether the comparator at the given slot index is in subtract mode.
     */
    public boolean getComparatorSubtract(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return false;
        return comparatorSubtract[slotIndex];
    }

    /** Returns the button pulse duration in game ticks. */
    public int getButtonDurationGameTicks(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return RedstoneCircuit.STONE_BUTTON_DURATION_GAME_TICKS;
        return buttonDurations[slotIndex];
    }

    /** Returns the button attachment rotation at the given slot index. */
    public int getButtonRotation(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 4;
        return buttonRotations[slotIndex];
    }

    /** Returns the configured output strength for a SIGNAL_SOURCE slot. */
    public int getSignalSourceStrength(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        return signalSourceStrengths[slotIndex];
    }

    /**
     * Returns the rear input strength used during the latest propagation pass.
     */
    public int getInputStrength(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        return inputStrengths[slotIndex];
    }

    /** Kept for callers using the former direction-derived accessor. */
    public int getInputStrength(int slotIndex, int rotation) {
        return getInputStrength(slotIndex);
    }

    /**
     * Returns true if the slot is empty.
     */
    public boolean isEmpty(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return true;
        return componentIds[slotIndex] < 0;
    }

    /**
     * Converts a 2D grid coordinate to a slot index.
     */
    public static int slotIndex(int x, int y) {
        return y * RedstoneCircuit.GRID_WIDTH + x;
    }

    /**
     * Serializes the circuit state to NBT for network transmission.
     * Uses compact arrays instead of per-slot CompoundTags for efficiency.
     *
     * @return The NBT tag containing the circuit state
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("ComponentIds", componentIds.clone());
        tag.putIntArray("Rotations", rotations.clone());
        tag.putIntArray("TorchRotations", torchRotations.clone());
        tag.putIntArray("LeverRotations", leverRotations.clone());
        tag.putIntArray("SignalStrengths", signalStrengths.clone());
        tag.putIntArray("WireConnectionMasks", wireConnectionMasks.clone());
        tag.putIntArray("OutputSignals", outputSignals.clone());
        tag.putIntArray("InputStrengths", inputStrengths.clone());
        tag.putIntArray("RepeaterDelays", repeaterDelays.clone());
        tag.putIntArray("ButtonDurations", buttonDurations.clone());
        tag.putIntArray("ButtonRotations", buttonRotations.clone());
        tag.putIntArray("SignalSourceStrengths", signalSourceStrengths.clone());

        // Pack booleans into byte arrays for compactness
        byte[] leverBytes = new byte[GRID_SIZE];
        byte[] lockedBytes = new byte[GRID_SIZE];
        byte[] lockOutputBytes = new byte[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            leverBytes[i] = (byte) (leverStates[i] ? 1 : 0);
            lockedBytes[i] = (byte) (repeaterLocked[i] ? 1 : 0);
            lockOutputBytes[i] = (byte) (repeaterLockOutput[i] ? 1 : 0);
        }
        tag.putByteArray("LeverStates", leverBytes);
        tag.putByteArray("RepeaterLocked", lockedBytes);
        tag.putByteArray("RepeaterLockOutput", lockOutputBytes);

        byte[] subtractBytes = new byte[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            subtractBytes[i] = (byte) (comparatorSubtract[i] ? 1 : 0);
        }
        tag.putByteArray("ComparatorSubtract", subtractBytes);

        return tag;
    }

    /**
     * Deserializes the circuit state from NBT data.
     *
     * @param tag The NBT tag containing the circuit state
     * @return A new RedstoneCircuitState with the deserialized data
     */
    public static RedstoneCircuitState deserializeNBT(CompoundTag tag) {
        RedstoneCircuitState state = new RedstoneCircuitState();

        if (tag.contains("ComponentIds")) {
            System.arraycopy(tag.getIntArray("ComponentIds"), 0, state.componentIds, 0, GRID_SIZE);
        }
        if (tag.contains("Rotations")) {
            System.arraycopy(tag.getIntArray("Rotations"), 0, state.rotations, 0, GRID_SIZE);
        }
        if (tag.contains("TorchRotations")) {
            System.arraycopy(tag.getIntArray("TorchRotations"), 0, state.torchRotations, 0, GRID_SIZE);
        }
        if (tag.contains("LeverRotations")) {
            System.arraycopy(tag.getIntArray("LeverRotations"), 0, state.leverRotations, 0, GRID_SIZE);
        }
        if (tag.contains("SignalStrengths")) {
            System.arraycopy(tag.getIntArray("SignalStrengths"), 0, state.signalStrengths, 0, GRID_SIZE);
        }
        if (tag.contains("WireConnectionMasks")) {
            System.arraycopy(tag.getIntArray("WireConnectionMasks"), 0, state.wireConnectionMasks, 0, GRID_SIZE);
        }
        if (tag.contains("OutputSignals")) {
            System.arraycopy(tag.getIntArray("OutputSignals"), 0, state.outputSignals, 0, GRID_SIZE);
        }
        if (tag.contains("InputStrengths")) {
            System.arraycopy(tag.getIntArray("InputStrengths"), 0, state.inputStrengths, 0, GRID_SIZE);
        }
        if (tag.contains("RepeaterDelays")) {
            System.arraycopy(tag.getIntArray("RepeaterDelays"), 0, state.repeaterDelays, 0, GRID_SIZE);
        }
        if (tag.contains("ButtonDurations")) {
            System.arraycopy(tag.getIntArray("ButtonDurations"), 0, state.buttonDurations, 0, GRID_SIZE);
        }
        if (tag.contains("ButtonRotations")) {
            System.arraycopy(tag.getIntArray("ButtonRotations"), 0, state.buttonRotations, 0, GRID_SIZE);
        }
        if (tag.contains("SignalSourceStrengths")) {
            int[] strengths = tag.getIntArray("SignalSourceStrengths");
            for (int i = 0; i < Math.min(strengths.length, GRID_SIZE); i++) {
                state.signalSourceStrengths[i] = Math.clamp(strengths[i], 0, 15);
            }
        }
        if (tag.contains("LeverStates")) {
            byte[] bytes = tag.getByteArray("LeverStates");
            for (int i = 0; i < Math.min(bytes.length, GRID_SIZE); i++) {
                state.leverStates[i] = bytes[i] == 1;
            }
        }
        if (tag.contains("RepeaterLocked")) {
            byte[] bytes = tag.getByteArray("RepeaterLocked");
            for (int i = 0; i < Math.min(bytes.length, GRID_SIZE); i++) {
                state.repeaterLocked[i] = bytes[i] == 1;
            }
        }
        if (tag.contains("RepeaterLockOutput")) {
            byte[] bytes = tag.getByteArray("RepeaterLockOutput");
            for (int i = 0; i < Math.min(bytes.length, GRID_SIZE); i++) {
                state.repeaterLockOutput[i] = bytes[i] == 1;
            }
        }
        if (tag.contains("ComparatorSubtract")) {
            byte[] bytes = tag.getByteArray("ComparatorSubtract");
            for (int i = 0; i < Math.min(bytes.length, GRID_SIZE); i++) {
                state.comparatorSubtract[i] = bytes[i] == 1;
            }
        }

        return state;
    }
}
