package com.thextrunner.praeteritum_futurum.redstone;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Data model for the 6×9 (54-slot) redstone circuit grid inside a Redstone Module.
 * <p>
 * This is the core of the Logic Layer in the three-layer architecture. It holds all
 * redstone component properties (type, rotation, lever state, lock state) and runs
 * circuit evaluation (signal propagation). The Inventory Layer provides the raw
 * ItemStacks, and the Render Layer reads from a snapshot of this circuit's state.
 * <p>
 * <b>Three-Layer Architecture:</b>
 * <ul>
 *   <li><b>Inventory → Logic</b> — {@link com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem#syncCircuitFromInventory}
 *   syncs menu slot items to this circuit whenever a circuit evaluation is triggered</li>
 *   <li><b>Logic evaluation</b> — {@link #propagateSignals(int[])} runs 16 passes of signal
 *   propagation, evaluating all component behaviors including repeater side-locking</li>
 *   <li><b>Logic → Render</b> — {@link #computeState()} creates a {@link RedstoneCircuitState}
 *   snapshot that is sent to the client for rendering</li>
 * </ul>
 * <p>
 * Each slot holds a {@link RedstoneSlotData} object that contains all configuration
 * for that slot's component — mirroring how vanilla Minecraft stores block state
 * properties: each block type defines its own properties, unused properties are ignored.
 * <p>
 * The circuit is stored in the module's CustomData component as NBT.
 * Combinational propagation runs at the configured evaluation rate, or every game tick while
 * a machine contains a repeater, comparator, attached torch, or observer. Their state transitions are
 * scheduled separately from the propagation passes and coordinated by
 * {@link com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem}.
 * <p>
 * <b>Signal propagation:</b> The algorithm resets all signals to 0, then runs 16 passes
 * (one source-initialization pass plus 15 signal-strength steps). Each pass evaluates every slot:
 * power sources emit signal, wires propagate and decay, and repeaters/comparators/observers use latched outputs.
 * Comparator targets are calculated from current inputs and applied on scheduled ticks. Repeater
 * side-lock transitions are processed before the passes begin (server-side only).
 */
public class RedstoneCircuit {
    
    /** Grid width in slots (9 columns, matching a double chest). */
    public static final int GRID_WIDTH = 9;

    /** Grid height in slots (6 rows, matching a double chest). */
    public static final int GRID_HEIGHT = 6;

    /** Total number of slots in the grid (9 × 6 = 54). */
    public static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT; // 54 slots (double chest layout: 9 columns × 6 rows)

    /** Vanilla comparator delay in game ticks. */
    public static final int COMPARATOR_DELAY_GAME_TICKS = 2;

    /** Vanilla observer delay before beginning its output pulse, in game ticks. */
    public static final int OBSERVER_DELAY_GAME_TICKS = 2;

    /** Vanilla observer output pulse duration, in game ticks. */
    public static final int OBSERVER_PULSE_GAME_TICKS = 2;

    /** Vanilla redstone torch transition delay in game ticks. */
    public static final int TORCH_DELAY_GAME_TICKS = 2;

    /** Vanilla stone and polished blackstone button pulse duration in game ticks. */
    public static final int STONE_BUTTON_DURATION_GAME_TICKS = 20;

    /** Vanilla wooden button pulse duration in game ticks. */
    public static final int WOODEN_BUTTON_DURATION_GAME_TICKS = 30;

    /** Number of recent turn-offs allowed before a torch burns out. */
    public static final int TORCH_BURNOUT_MAX_OFFS = 8;

    /** Rolling window for redstone torch burnout, in game ticks. */
    public static final int TORCH_BURNOUT_WINDOW_GAME_TICKS = 60;

    /** Vanilla's scheduled retry delay after a redstone torch burns out, in game ticks. */
    public static final int TORCH_BURNOUT_RETRY_DELAY_GAME_TICKS = 160;

    /** Per-slot data — one object per grid slot, null means empty */
    private final RedstoneSlotData[] slots;

    /** Current input signals from the 4 borders: [top, bottom, left, right]. Set before propagation. */
    private int[] inputSignals;

    /** Per-position signals arriving through neighboring modules' shared border walls. */
    private int[][] boundaryInputSignals = createBoundarySignalMatrix();

    /** Strong power received by each slot on the previous propagation pass. */
    private final int[] previousStrongPower = new int[GRID_SIZE];

    /** Per-position signal strengths this circuit outputs into neighboring modules' border walls. */
    private int[][] boundaryWallOutputs = createBoundarySignalMatrix();

    /**
     * Creates an empty redstone circuit with all 54 slots initialized to null.
     */
    public RedstoneCircuit() {
        this.slots = new RedstoneSlotData[GRID_SIZE];
    }

    /**
     * Gets the component type at the given slot index.
     * @param slotIndex The slot index (0-53)
     * @return The component type, or null if the slot is empty or out of bounds
     */
    public RedstoneComponent getComponent(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return null;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return null;
        return data.type;
    }

    /** Returns true when this circuit contains at least one repeater. */
    public boolean hasRepeater() {
        for (RedstoneSlotData data : slots) {
            if (data != null && data.type == RedstoneComponent.REPEATER) return true;
        }
        return false;
    }

    /** Returns true when this circuit contains at least one comparator. */
    public boolean hasComparator() {
        for (RedstoneSlotData data : slots) {
            if (data != null && data.type == RedstoneComponent.COMPARATOR) return true;
        }
        return false;
    }

    /** Returns true when this circuit contains an observer. */
    public boolean hasObserver() {
        for (RedstoneSlotData data : slots) {
            if (data != null && data.type == RedstoneComponent.OBSERVER) return true;
        }
        return false;
    }

    /** Returns true when this circuit contains a wall-mounted torch with timed state. */
    public boolean hasAttachedTorch() {
        for (RedstoneSlotData data : slots) {
            if (data != null && data.type == RedstoneComponent.TORCH && data.torchRotation < 4) return true;
        }
        return false;
    }

    /**
     * Sets the component type at the given slot index.
     * Creates a new RedstoneSlotData with default values if the component is not null.
     * Setting null clears the slot.
     *
     * @param slotIndex The slot index (0-53)
     * @param component The component to place, or null to clear the slot
     */
    public void setComponent(int slotIndex, RedstoneComponent component) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        if (component == null) {
            slots[slotIndex] = null;
        } else {
            slots[slotIndex] = new RedstoneSlotData(component);
        }
    }

    /**
     * Gets the slot data for a given slot index.
     * Returns null if the slot is empty or out of bounds.
     *
     * @param slotIndex The slot index (0-53)
     * @return The slot data, or null if empty
     */
    public RedstoneSlotData getSlotData(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return null;
        return slots[slotIndex];
    }

    /**
     * Gets the horizontal connection arms for a wire, using one bit per {@link Direction#get3DDataValue()}.
     * A wire with no eligible neighbors defaults to a cross; with one eligible neighbor it extends
     * into a straight line; with multiple neighbors it connects only to those neighbors.
     */
    public int getWireConnectionMask(int slotIndex) {
        RedstoneSlotData wire = getSlotData(slotIndex);
        if (wire == null || wire.type != RedstoneComponent.WIRE) return 0;

        int mask = 0;
        int connectionCount = 0;
        Direction onlyConnection = null;
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            int neighborSlot = getNeighborSlot(getX(slotIndex), getY(slotIndex), direction);
            if (neighborSlot >= 0 && isWireShapeNeighbor(neighborSlot, direction)) {
                mask |= 1 << direction.get3DDataValue();
                connectionCount++;
                onlyConnection = direction;
            }
        }

        if (connectionCount == 0) {
            return horizontalWireConnectionMask();
        }
        if (connectionCount == 1) {
            mask |= 1 << onlyConnection.getOpposite().get3DDataValue();
        }
        return mask;
    }

    /** Returns whether the wire at a slot has an arm in the given horizontal direction. */
    public boolean isWireConnected(int slotIndex, Direction direction) {
        return direction != null && (getWireConnectionMask(slotIndex) & (1 << direction.get3DDataValue())) != 0;
    }

    private boolean isWireShapeNeighbor(int neighborSlot, Direction direction) {
        RedstoneSlotData neighbor = slots[neighborSlot];
        if (neighbor == null || neighbor.type == null) return false;

        return switch (neighbor.type) {
            case WIRE, REDSTONE_BLOCK, TORCH, LEVER, BUTTON, COMPARATOR, SIGNAL_SOURCE -> true;
            case OBSERVER -> getFacingDirection(neighbor.rotation) == direction;
            case REPEATER -> {
                Direction facing = getFacingDirection(neighbor.rotation);
                yield facing == direction || facing == direction.getOpposite();
            }
            case WALL -> false;
        };
    }

    private static int horizontalWireConnectionMask() {
        return (1 << Direction.NORTH.get3DDataValue()) |
                (1 << Direction.EAST.get3DDataValue()) |
                (1 << Direction.SOUTH.get3DDataValue()) |
                (1 << Direction.WEST.get3DDataValue());
    }

    /**
     * Gets the rotation of the component at the given slot index (0-3 for N/E/S/W).
     *
     * @param slotIndex The slot index (0-53)
     * @return The rotation value (0=NORTH, 1=EAST, 2=SOUTH, 3=WEST), or 0 if empty
     */
    public int getRotation(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return 0;
        return data.rotation;
    }

    /**
     * Sets the rotation of the component at the given slot index.
     * Rotation is stored modulo 4, so values outside 0-3 are wrapped.
     *
     * @param slotIndex The slot index (0-53)
     * @param rotation The rotation value (0=NORTH, 1=EAST, 2=SOUTH, 3=WEST)
     */
    public void setRotation(int slotIndex, int rotation) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return;
        data.rotation = rotation % 4;
    }

    /**
     * Rotates the component at the given slot index 90° clockwise.
     * Used when the player clicks the rotate button in the circuit editor.
     *
     * @param slotIndex The slot index (0-53)
     */
    public void rotateComponent(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null || (data.type != RedstoneComponent.REPEATER &&
                data.type != RedstoneComponent.COMPARATOR && data.type != RedstoneComponent.OBSERVER)) return;
        data.rotation = (data.rotation + 1) % 4;
    }

    /** Cycles a repeater through its four vanilla delay settings (1-4 redstone ticks). */
    public void cycleRepeaterDelay(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null || data.type != RedstoneComponent.REPEATER) return;
        data.repeaterDelay = data.repeaterDelay % 4 + 1;
    }

    /**
     * Gets the lever on/off state at the given slot index.
     *
     * @param slotIndex The slot index (0-53)
     * @return True if the lever is on, false otherwise
     */
    public boolean getLeverState(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return false;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return false;
        return data.leverOn;
    }

    /**
     * Sets the lever on/off state at the given slot index.
     *
     * @param slotIndex The slot index (0-53)
     * @param state True to turn the lever on, false to turn it off
     */
    public void setLeverState(int slotIndex, boolean state) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return;
        data.leverOn = state;
    }

    /** Sets the configured signal strength for a SIGNAL_SOURCE component. */
    public void setSignalSourceStrength(int slotIndex, int strength) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null || data.type != RedstoneComponent.SIGNAL_SOURCE) return;
        data.signalSourceStrength = Math.clamp(strength, 0, 15);
    }

    /** Activates a button immediately if it is currently released. */
    public boolean pressButton(int slotIndex, long gameTime) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return false;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null || data.type != RedstoneComponent.BUTTON || data.buttonPowered) return false;

        data.buttonPowered = true;
        int duration = data.buttonDurationGameTicks == WOODEN_BUTTON_DURATION_GAME_TICKS
                ? WOODEN_BUTTON_DURATION_GAME_TICKS : STONE_BUTTON_DURATION_GAME_TICKS;
        data.buttonScheduledGameTime = gameTime + duration;
        return true;
    }

    /**
     * Toggles the lever on/off state at the given slot index.
     * Used when the player right-clicks a lever in the circuit editor.
     *
     * @param slotIndex The slot index (0-53)
     */
    public void toggleLever(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return;
        data.leverOn = !data.leverOn;
    }

    /**
     * Cycles the lever attachment direction at the given slot index.
     * The lever cycles through directions where an adjacent wall exists or where the grid border
     * acts as an implicit wall. If no walls or borders are adjacent, the lever stays standing (rotation 4).
     * Used when the player right-clicks a lever in Config Mode.
     *
     * @param slotIndex The slot index (0-53)
     */
    public void cycleLeverAttachment(int slotIndex) {
        cycleAttachment(slotIndex, RedstoneComponent.LEVER);
    }

    /** Cycles the button attachment through adjacent walls/borders and its floor-perched state. */
    public void cycleButtonAttachment(int slotIndex) {
        cycleAttachment(slotIndex, RedstoneComponent.BUTTON);
    }

    private void cycleAttachment(int slotIndex, RedstoneComponent component) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null || data.type != component) return;

        int tx = getX(slotIndex);
        int ty = getY(slotIndex);

        // Find which directions have adjacent walls or border walls
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        java.util.ArrayList<Integer> validRotations = new java.util.ArrayList<>();
        for (int i = 0; i < dirs.length; i++) {
            int neighborSlot = getNeighborSlot(tx, ty, dirs[i]);
            if (neighborSlot >= 0) {
                RedstoneSlotData neighbor = slots[neighborSlot];
                if (neighbor != null && neighbor.type == RedstoneComponent.WALL) {
                    validRotations.add(i); // i corresponds to the rotation for this direction
                }
            } else {
                // Out of bounds — the grid border acts as an implicit wall
                validRotations.add(i);
            }
        }

        int currentRotation = component == RedstoneComponent.BUTTON ? data.buttonRotation : data.leverRotation;
        if (validRotations.isEmpty()) {
            // No adjacent walls — keep the component floor-perched.
            setAttachmentRotation(data, component, 4);
        } else {
            // Build a cycle list: valid wall rotations followed by the floor-perched state (4).
            java.util.ArrayList<Integer> cycle = new java.util.ArrayList<>(validRotations);
            cycle.add(4);

            // Find current position in cycle
            int currentIndex = -1;
            for (int i = 0; i < cycle.size(); i++) {
                if (cycle.get(i) == currentRotation) {
                    currentIndex = i;
                    break;
                }
            }

            // Move to next in cycle (wrap around)
            int nextIndex = (currentIndex + 1) % cycle.size();
            setAttachmentRotation(data, component, cycle.get(nextIndex));
        }
    }

    private static void setAttachmentRotation(RedstoneSlotData data, RedstoneComponent component, int rotation) {
        if (component == RedstoneComponent.BUTTON) {
            data.buttonRotation = rotation;
        } else {
            data.leverRotation = rotation;
        }
    }

    /**
     * Cycles the torch attachment/support direction at the given slot index.
     * The torch cycles through directions where an adjacent wall exists or where the grid border
     * acts as an implicit wall. If no walls or borders are adjacent, the torch stays standing (rotation 4).
     * Used when the player right-clicks a torch in Config Mode.
     *
     * @param slotIndex The slot index (0-53)
     */
    public void rotateTorch(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return;

        int tx = getX(slotIndex);
        int ty = getY(slotIndex);

        // Find which directions have adjacent walls or border walls
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        java.util.ArrayList<Integer> validRotations = new java.util.ArrayList<>();
        for (int i = 0; i < dirs.length; i++) {
            int neighborSlot = getNeighborSlot(tx, ty, dirs[i]);
            if (neighborSlot >= 0) {
                RedstoneSlotData neighbor = slots[neighborSlot];
                if (neighbor != null && neighbor.type == RedstoneComponent.WALL) {
                    validRotations.add(i); // i corresponds to the rotation for this direction
                }
            } else {
                // Out of bounds — the grid border acts as an implicit wall
                validRotations.add(i);
            }
        }

        if (validRotations.isEmpty()) {
            // No adjacent walls — keep torch standing
            data.torchRotation = 4;
        } else {
            // Build a cycle list: valid wall rotations followed by standing (4)
            java.util.ArrayList<Integer> cycle = new java.util.ArrayList<>(validRotations);
            cycle.add(4); // Always include standing as an option

            // Find current position in cycle
            int currentIndex = -1;
            for (int i = 0; i < cycle.size(); i++) {
                if (cycle.get(i) == data.torchRotation) {
                    currentIndex = i;
                    break;
                }
            }

            // Move to next in cycle (wrap around)
            int nextIndex = (currentIndex + 1) % cycle.size();
            data.torchRotation = cycle.get(nextIndex);
        }
    }

    /**
     * Toggles the comparator mode at the given slot index between compare and subtract.
     * Used when the player left-clicks a comparator in Config Mode.
     *
     * @param slotIndex The slot index (0-53)
     */
    public void toggleComparatorMode(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return;
        if (data.type != RedstoneComponent.COMPARATOR) return;
        data.comparatorSubtract = !data.comparatorSubtract;
    }

    /**
     * Checks if the input slot of a directional component (repeater/comparator) is powered.
     * Instead of relying on signal strength arrays (which are reset every tick when the circuit
     * is recreated from NBT), this method evaluates power by checking component types directly:
     * - Power sources (redstone block, torch, lever ON) always provide power.
     * - Another repeater/comparator provides power if its own input is powered (recursive check).
     * - A wire provides power if it's adjacent to any power source.
     *
     * @param slotIndex the slot of the directional component
     * @param rotation the rotation of the directional component
     * @return true if the input slot provides a signal
     */
    private boolean isSlotPowered(int slotIndex, int rotation) {
        Direction inputDir = getInputDirection(rotation);
        int x = getX(slotIndex);
        int y = getY(slotIndex);
        int inputSlot = getNeighborSlot(x, y, inputDir);
        if (inputSlot < 0) return getBoundaryInputSignal(slotIndex, inputDir) > 0;

        return isSlotPowered(inputSlot, /* visited */ new java.util.HashSet<>());
    }

    /**
     * Recursively checks if a slot is powered by examining component types.
     * Uses a visited set to prevent infinite loops in circular circuits.
     */
    private boolean isSlotPowered(int slotIndex, java.util.Set<Integer> visited) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return false;
        if (visited.contains(slotIndex)) return false; // Prevent infinite loops
        visited.add(slotIndex);

        RedstoneSlotData data = slots[slotIndex];
        if (data == null || data.type == null) return false;

        switch (data.type) {
            case REDSTONE_BLOCK -> {
                return true; // Always powered
            }
            case TORCH -> {
                if (data.torchRotation == 4) {
                    return true; // Standing torch always ON
                }
                return data.torchPowered;
            }
            case LEVER -> {
                return data.leverOn; // Powered when toggled on
            }
            case BUTTON -> {
                return data.buttonPowered;
            }
            case SIGNAL_SOURCE -> {
                return data.signalSourceStrength > 0;
            }
            case OBSERVER -> {
                return data.observerPowered;
            }
            case REPEATER -> {
                // Repeater outputs if its input is powered
                return isSlotPowered(slotIndex, data.rotation, visited);
            }
            case COMPARATOR -> {
                // Comparator outputs if its input is powered
                return isSlotPowered(slotIndex, data.rotation, visited);
            }
            case WIRE -> {
                // Wire is powered if any adjacent neighbor is a power source
                int[] adjacent = getAdjacentSlots(slotIndex);
                for (int adj : adjacent) {
                    if (isAdjacentSlotPowerSource(adj)) return true;
                }
                return false;
            }
            case WALL -> {
                // Wall can be powered but is not itself a power source
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Checks if a repeater/comparator's input is powered, following the chain of components.
     */
    private boolean isSlotPowered(int slotIndex, int rotation, java.util.Set<Integer> visited) {
        Direction inputDir = getInputDirection(rotation);
        int x = getX(slotIndex);
        int y = getY(slotIndex);
        int inputSlot = getNeighborSlot(x, y, inputDir);
        if (inputSlot < 0) return getBoundaryInputSignal(slotIndex, inputDir) > 0;
        return isSlotPowered(inputSlot, visited);
    }

    /**
     * Checks if an adjacent slot is a direct power source (redstone block, torch, lever ON,
     * or a repeater/comparator that outputs toward this slot).
     */
    private boolean isAdjacentSlotPowerSource(int slotIndex) {
        RedstoneSlotData data = slots[slotIndex];
        if (data == null || data.type == null) return false;

        switch (data.type) {
            case REDSTONE_BLOCK -> {
                return true;
            }
            case TORCH -> {
                if (data.torchRotation == 4) {
                    return true; // Standing torch always ON, is a power source
                }
                return data.torchPowered;
            }
            case LEVER -> {
                return data.leverOn;
            }
            case BUTTON -> {
                return data.buttonPowered;
            }
            case SIGNAL_SOURCE -> {
                return data.signalSourceStrength > 0;
            }
            case OBSERVER -> {
                return data.observerPowered;
            }
            case REPEATER, COMPARATOR -> {
                // Check if this repeater/comparator outputs toward any neighbor (meaning it's powered)
                // For simplicity, check if its input is powered
                return isSlotPowered(slotIndex, data.rotation, new java.util.HashSet<>());
            }
            case WALL -> {
                // Wall is not a power source
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    public boolean isRepeaterSidePowered(int slotIndex, int[] signalStrengths) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return false;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return false;
        if (data.type != RedstoneComponent.REPEATER) return false;

        int x = getX(slotIndex);
        int y = getY(slotIndex);
        Direction facing = getFacingDirection(data.rotation);

        // Side directions: left and right of facing direction
        Direction side1 = facing.getClockWise();
        Direction side2 = facing.getCounterClockWise();

        for (Direction side : new Direction[]{side1, side2}) {
            int neighborSlot = getNeighborSlot(x, y, side);
            if (neighborSlot >= 0) {
                RedstoneSlotData neighbor = slots[neighborSlot];
                if (neighbor != null && neighbor.type != null) {
                    // Only repeater and comparator OUTPUTS can side-lock a repeater
                    if (neighbor.type == RedstoneComponent.REPEATER || neighbor.type == RedstoneComponent.COMPARATOR) {
                        // Check if the neighbor is facing this repeater (outputting toward it)
                        // The neighbor's output direction must point toward this repeater
                        Direction neighborFacing = getFacingDirection(neighbor.rotation);
                        // neighborFacing is where the neighbor outputs. We need neighborFacing.getOpposite() == side
                        // which means: the neighbor faces TOWARD this repeater (its output direction points back)
                        if (neighborFacing.getOpposite() == side) {
                            // Neighbor is outputting toward this repeater — check its actual output state.
                            if (neighbor.type == RedstoneComponent.REPEATER) {
                                boolean repeaterOutput = neighbor.repeaterLocked
                                        ? neighbor.repeaterLockOutput
                                        : neighbor.repeaterPowered;
                                if (repeaterOutput) return true;
                            } else {
                                // Comparator output can differ from its rear input due to side inputs/mode.
                                if (neighbor.outputSignal > 0) return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }


    /**
     * Gets the facing (output) direction for a component based on its rotation.
     * Rotation 0=N, 1=E, 2=S, 3=W — matches vanilla Minecraft where "facing" is the
     * output direction (where the arrow points). The input comes from the opposite side.
     *
     * @param rotation The rotation value (0-3)
     * @return The facing Direction
     */
    public static Direction getFacingDirection(int rotation) {
        return switch (rotation % 4) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    /**
     * Gets the input direction for a component based on its rotation.
     * The input comes from the opposite side of the facing direction.
     * For example, if facing NORTH (output goes north), the input comes from SOUTH.
     *
     * @param rotation The rotation value (0-3)
     * @return The input Direction (opposite of facing)
     */
    public static Direction getInputDirection(int rotation) {
        return getFacingDirection(rotation).getOpposite();
    }

    /**
     * Gets the output direction for a component based on its rotation.
     * Same as the facing direction — where the arrow points.
     *
     * @param rotation The rotation value (0-3)
     * @return The output Direction (same as facing)
     */
    public static Direction getOutputDirection(int rotation) {
        return getFacingDirection(rotation);
    }

    /**
     * Gets the signal strength at the given slot index.
     * Signal strength is a computed value updated by {@link #propagateSignals(int[])}. It is not persisted in NBT.
     *
     * @param slotIndex The slot index (0-53)
     * @return The signal strength (0-15), or 0 if empty
     */
    public int getSignalStrength(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return 0;
        return data.signalStrength;
    }

    /**
     * Gets the signal strength this circuit sends into a shared wall at one aligned edge position.
     * Edge positions are columns for north/south and rows for west/east.
     */
    public int getBoundaryWallOutput(Direction direction, int edgePosition) {
        int side = getBoundarySideIndex(direction);
        if (side < 0 || edgePosition < 0 || edgePosition >= boundaryWallOutputs[side].length) return 0;
        return boundaryWallOutputs[side][edgePosition];
    }

    /** Returns the [top, bottom, left, right] index used by per-position boundary signal arrays. */
    public static int getBoundarySideIndex(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case SOUTH -> 1;
            case WEST -> 2;
            case EAST -> 3;
            default -> -1;
        };
    }

    /** Returns the number of aligned positions on the given edge. */
    public static int getBoundaryEdgeLength(Direction direction) {
        return switch (direction) {
            case NORTH, SOUTH -> GRID_WIDTH;
            case WEST, EAST -> GRID_HEIGHT;
            default -> 0;
        };
    }

    /**
     * Gets the rear input strength used during the latest propagation pass for a directional component.
     */
    public int getInputStrength(int slotIndex, int rotation) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        RedstoneSlotData data = slots[slotIndex];
        return data == null ? 0 : data.inputStrength;
    }

    /**
     * Sets the signal strength at the given slot index.
     * Normally signal strength is computed by propagation, but this can be used
     * to manually set it (e.g., for external input injection).
     *
     * @param slotIndex The slot index (0-53)
     * @param strength The signal strength (clamped to 0-15)
     */
    public void setSignalStrength(int slotIndex, int strength) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return;
        data.signalStrength = Math.clamp(strength, 0, 15);
    }

    /**
     * Checks if a slot is empty (no component placed).
     *
     * @param slotIndex The slot index (0-53)
     * @return True if the slot is empty or out of bounds
     */
    public boolean isEmpty(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return true;
        return slots[slotIndex] == null;
    }

    /**
     * Converts a 2D grid coordinate to a slot index.
     * Formula: y * GRID_WIDTH + x
     *
     * @param x The X coordinate (0-8)
     * @param y The Y coordinate (0-5)
     * @return The slot index (0-53)
     */
    public static int slotIndex(int x, int y) {
        return y * GRID_WIDTH + x;
    }

    /**
     * Gets the X coordinate (column) from a slot index.
     *
     * @param slotIndex The slot index (0-53)
     * @return The X coordinate (0-8)
     */
    public static int getX(int slotIndex) {
        return slotIndex % GRID_WIDTH;
    }

    /**
     * Gets the Y coordinate (row) from a slot index.
     *
     * @param slotIndex The slot index (0-53)
     * @return The Y coordinate (0-5)
     */
    public static int getY(int slotIndex) {
        return slotIndex / GRID_WIDTH;
    }

    /**
     * Gets all adjacent slot indices (up, down, left, right) for a given slot.
     * Returns only valid slots within the grid bounds — edge slots have fewer neighbors.
     *
     * @param slotIndex The slot index (0-53)
     * @return Array of adjacent slot indices (1-4 elements)
     */
    public int[] getAdjacentSlots(int slotIndex) {
        int x = getX(slotIndex);
        int y = getY(slotIndex);
        int[] adjacent = new int[4];
        int count = 0;

        if (y > 0) adjacent[count++] = slotIndex(x, y - 1); // up
        if (y < GRID_HEIGHT - 1) adjacent[count++] = slotIndex(x, y + 1); // down
        if (x > 0) adjacent[count++] = slotIndex(x - 1, y); // left
        if (x < GRID_WIDTH - 1) adjacent[count++] = slotIndex(x + 1, y); // right

        // Return only valid adjacent slots
        int[] result = new int[count];
        System.arraycopy(adjacent, 0, result, 0, count);
        return result;
    }

    /**
     * Evaluates signal propagation across the circuit once.
     * Resets all signal strengths to 0 before propagation, so only components with
     * actual power sources produce signals — matching vanilla redstone behavior where
     * isolated dust has no power.
     * Uses multi-pass propagation (16 passes) to seed sources and allow 15 dust-strength steps.
     * Injects external input signals into edge slots before propagation.
     * Processes repeater lock state transitions (server-side only).
     *
     * @param inputSignals array of 4 input signal strengths [top, bottom, left, right]
     * @return array of 4 output signal strengths [top, bottom, left, right]
     */
    public int[] propagateSignals(int[] inputSignals) {
        return propagateSignals(inputSignals, null, /* processLockTransitions= */ true);
    }

    /**
     * Runs signal propagation with per-position inputs from adjacent redstone modules.
     * Boundary positions use the same coordinate on both circuits (column for top/bottom,
     * row for left/right), so each virtual wall connects only aligned slots.
     *
     * @param inputSignals array of 4 external input strengths [top, bottom, left, right]
     * @param boundaryInputSignals per-position shared-wall inputs in [top, bottom, left, right] order
     * @return array of 4 aggregate output strengths [top, bottom, left, right]
     */
    public int[] propagateSignals(int[] inputSignals, int[][] boundaryInputSignals) {
        return propagateSignals(inputSignals, boundaryInputSignals, /* processLockTransitions= */ true);
    }

    /**
     * Runs signal propagation with control over repeater lock transition processing.
     *
     * @param inputSignals array of 4 input signal strengths [top, bottom, left, right]
     * @param processLockTransitions if true, processes repeater side-lock state transitions
     *        (should only be true on the server; the client should use the server-synced lock state)
     * @return array of 4 output signal strengths [top, bottom, left, right]
     */
    public int[] propagateSignals(int[] inputSignals, boolean processLockTransitions) {
        return propagateSignals(inputSignals, null, processLockTransitions);
    }

    /**
     * Runs signal propagation with per-position inputs and control over repeater lock transitions.
     */
    public int[] propagateSignals(int[] inputSignals, int[][] boundaryInputSignals, boolean processLockTransitions) {
        // Store input signals for border wall access during propagation
        this.inputSignals = inputSignals == null ? null : inputSignals.clone();
        this.boundaryInputSignals = copyBoundarySignalMatrix(boundaryInputSignals);

        // Save previous signal strengths before reset so lock detection reads the previous
        // propagation state. The frozen output itself comes from repeaterPowered below.
        int[] previousStrengths = new int[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            RedstoneSlotData data = slots[i];
            if (data != null) {
                previousStrengths[i] = data.signalStrength;
            }
        }

        // Reset all signal strengths to 0 before propagation.
        // Signal strength is a computed value — it should not persist between ticks.
        // This ensures isolated components show 0 signal, matching vanilla behavior.
        for (RedstoneSlotData data : slots) {
            if (data != null) {
                data.signalStrength = 0;
                data.inputStrength = 0;
            }
        }
        java.util.Arrays.fill(previousStrongPower, 0);

        // Process repeater lock state transitions using previous propagation's signal strengths.
        // This must happen before propagation so the lock state is stable during all passes.
        // A newly locked repeater freezes its latched output state.
        // Only run on the server — the client uses the server-synced lock state from NBT.
        if (processLockTransitions) {
            processRepeaterLockTransitions(previousStrengths);
        }

        // The first pass establishes source strengths; the remaining passes allow 15 strength steps.
        for (int pass = 0; pass < 16; pass++) {
            propagateOnePass();
        }

        calculateBoundaryWallOutputs();

        // Calculate output signals from edge slots
        return calculateOutputs();
    }

    /**
     * Returns the due time of this circuit's earliest pending repeater tick, or -1 when none is pending.
     */
    public long getNextRepeaterTickTime() {
        long nextTick = Long.MAX_VALUE;
        for (RedstoneSlotData data : slots) {
            if (data != null && data.type == RedstoneComponent.REPEATER && data.repeaterScheduledGameTime >= 0) {
                nextTick = Math.min(nextTick, data.repeaterScheduledGameTime);
            }
        }
        return nextTick == Long.MAX_VALUE ? -1L : nextTick;
    }

    /** Returns the due time of this circuit's earliest pending comparator tick, or -1 when none is pending. */
    public long getNextComparatorTickTime() {
        long nextTick = Long.MAX_VALUE;
        for (RedstoneSlotData data : slots) {
            if (data != null && data.type == RedstoneComponent.COMPARATOR && data.comparatorScheduledGameTime >= 0) {
                nextTick = Math.min(nextTick, data.comparatorScheduledGameTime);
            }
        }
        return nextTick == Long.MAX_VALUE ? -1L : nextTick;
    }

    /** Returns the due time of the earliest torch transition or burnout retry, or -1. */
    public long getNextTorchTickTime() {
        long nextTick = Long.MAX_VALUE;
        for (RedstoneSlotData data : slots) {
            if (data != null && data.type == RedstoneComponent.TORCH && data.torchRotation < 4 &&
                data.torchScheduledGameTime >= 0) {
                nextTick = Math.min(nextTick, data.torchScheduledGameTime);
            }
        }
        return nextTick == Long.MAX_VALUE ? -1L : nextTick;
    }

    /** Returns the due time of the earliest button release, or -1 when none is pending. */
    public long getNextButtonTickTime() {
        long nextTick = Long.MAX_VALUE;
        for (RedstoneSlotData data : slots) {
            if (data != null && data.type == RedstoneComponent.BUTTON && data.buttonScheduledGameTime >= 0) {
                nextTick = Math.min(nextTick, data.buttonScheduledGameTime);
            }
        }
        return nextTick == Long.MAX_VALUE ? -1L : nextTick;
    }

    /** Returns the due time of the earliest observer pulse transition, or -1 when none is pending. */
    public long getNextObserverTickTime() {
        long nextTick = Long.MAX_VALUE;
        for (RedstoneSlotData data : slots) {
            if (data != null && data.type == RedstoneComponent.OBSERVER &&
                data.observerScheduledGameTime >= 0) {
                nextTick = Math.min(nextTick, data.observerScheduledGameTime);
            }
        }
        return nextTick == Long.MAX_VALUE ? -1L : nextTick;
    }

    /**
     * Processes all repeater ticks scheduled for the same game time from the same input snapshot.
     * The inputStrength values must have been refreshed by a completed propagation first.
     * Re-settling between simultaneous events can let one repeater cancel another's due transition
     * solely because of slot ordering, preventing repeater-loop clocks from alternating.
     *
     * @return whether any repeater's latched output changed
     */
    public boolean processRepeaterTicksAt(long scheduledGameTime) {
        boolean anyOutputChanged = false;
        for (RedstoneSlotData data : slots) {
            if (data == null || data.type != RedstoneComponent.REPEATER ||
                data.repeaterScheduledGameTime != scheduledGameTime) continue;

            data.repeaterScheduledGameTime = -1L;
            if (data.repeaterLocked) continue;

            // A scheduled turn-on still occurs if its input vanished before the tick. The
            // subsequent scheduling pass then queues the delayed turn-off, extending short pulses.
            boolean shouldBePowered = data.repeaterPowered ? data.inputStrength > 0 : true;
            if (shouldBePowered != data.repeaterPowered) {
                data.repeaterPowered = shouldBePowered;
                anyOutputChanged = true;
            }
        }
        return anyOutputChanged;
    }

    /**
     * Schedules a repeater transition for each unlocked repeater whose stable input differs from its output.
     * Existing pending ticks are preserved, matching vanilla's one-scheduled-tick-at-a-time behavior.
     */
    public void scheduleRepeaterTransitions(long gameTime) {
        for (RedstoneSlotData data : slots) {
            if (data == null || data.type != RedstoneComponent.REPEATER) continue;
            if (data.repeaterLocked) continue;
            if (data.repeaterScheduledGameTime < 0 && (data.inputStrength > 0) != data.repeaterPowered) {
                int delay = Math.max(1, Math.min(4, data.repeaterDelay));
                data.repeaterScheduledGameTime = gameTime + delay * 2L;
            }
        }
    }

    /**
     * Applies comparator updates due at the given game time. Each due comparator samples the
     * latest output calculated from its settled inputs, so a pulse that ends before this tick is missed.
     *
     * @return whether any comparator output changed
     */
    public boolean processComparatorTicksAt(long scheduledGameTime) {
        boolean anyOutputChanged = false;
        for (RedstoneSlotData data : slots) {
            if (data == null || data.type != RedstoneComponent.COMPARATOR ||
                data.comparatorScheduledGameTime != scheduledGameTime) continue;

            data.comparatorScheduledGameTime = -1L;
            if (data.outputSignal != data.comparatorTargetSignal) {
                data.outputSignal = data.comparatorTargetSignal;
                anyOutputChanged = true;
            }
        }
        return anyOutputChanged;
    }

    /** Schedules an update when a comparator's desired output differs and it has no pending tick. */
    public void scheduleComparatorTransitions(long gameTime) {
        for (RedstoneSlotData data : slots) {
            if (data == null || data.type != RedstoneComponent.COMPARATOR) continue;
            if (data.comparatorScheduledGameTime < 0 && data.comparatorTargetSignal != data.outputSignal) {
                data.comparatorScheduledGameTime = gameTime + COMPARATOR_DELAY_GAME_TICKS;
            }
        }
    }

    /** Releases buttons whose scheduled press duration has elapsed. */
    public boolean processButtonTicksAt(long scheduledGameTime) {
        boolean anyOutputChanged = false;
        for (RedstoneSlotData data : slots) {
            if (data == null || data.type != RedstoneComponent.BUTTON ||
                data.buttonScheduledGameTime != scheduledGameTime) continue;

            data.buttonScheduledGameTime = -1L;
            if (data.buttonPowered) {
                data.buttonPowered = false;
                anyOutputChanged = true;
            }
        }
        return anyOutputChanged;
    }

    /** Processes observer pulse starts and ends scheduled for the same world game time. */
    public boolean processObserverTicksAt(long scheduledGameTime) {
        boolean anyOutputChanged = false;
        for (RedstoneSlotData data : slots) {
            if (data == null || data.type != RedstoneComponent.OBSERVER ||
                data.observerScheduledGameTime != scheduledGameTime) continue;

            if (data.observerPowered) {
                data.observerPowered = false;
                data.observerScheduledGameTime = -1L;
            } else {
                data.observerPowered = true;
                data.observerScheduledGameTime = scheduledGameTime + OBSERVER_PULSE_GAME_TICKS;
            }
            anyOutputChanged = true;
        }
        return anyOutputChanged;
    }

    /**
     * Compares observers' watched slots after circuit and shared-module signals have settled.
     * Repeater lock-only changes are excluded because Java observers do not receive a shape update for them.
     */
    public void updateObserverInputs(long gameTime) {
        for (int slotIndex = 0; slotIndex < GRID_SIZE; slotIndex++) {
            RedstoneSlotData observer = slots[slotIndex];
            if (observer == null || observer.type != RedstoneComponent.OBSERVER) continue;

            long observedState = getObservedBlockStateSignature(slotIndex, observer.rotation);
            if (observer.observerLastObservedRotation != observer.rotation ||
                observer.observerObservedStateSignature == Long.MIN_VALUE) {
                observer.observerLastObservedRotation = observer.rotation;
                observer.observerObservedStateSignature = observedState;
                continue;
            }

            if (observer.observerObservedStateSignature != observedState) {
                observer.observerObservedStateSignature = observedState;
                if (!observer.observerPowered && observer.observerScheduledGameTime < 0) {
                    observer.observerScheduledGameTime = gameTime + OBSERVER_DELAY_GAME_TICKS;
                }
            }
        }
    }

    private long getObservedBlockStateSignature(int observerSlot, int rotation) {
        Direction observedDirection = getFacingDirection(rotation);
        int observedSlot = getNeighborSlot(getX(observerSlot), getY(observerSlot), observedDirection);
        if (observedSlot < 0 || slots[observedSlot] == null || slots[observedSlot].type == null) return 0L;

        RedstoneSlotData observed = slots[observedSlot];
        long state = switch (observed.type) {
            case WIRE -> observed.signalStrength | ((long) getWireConnectionMask(observedSlot) << 4);
            case REPEATER -> observed.rotation | ((long) observed.repeaterDelay << 2) |
                    (observed.repeaterPowered ? 1L << 5 : 0L);
            case COMPARATOR -> observed.rotation | (observed.comparatorSubtract ? 1L << 2 : 0L) |
                    (observed.outputSignal > 0 ? 1L << 3 : 0L);
            case TORCH -> observed.torchRotation | (observed.torchPowered ? 1L << 3 : 0L);
            case LEVER -> observed.leverRotation | (observed.leverOn ? 1L << 3 : 0L);
            case BUTTON -> observed.buttonRotation | (observed.buttonPowered ? 1L << 3 : 0L) |
                    ((long) observed.buttonDurationGameTicks << 4);
            case OBSERVER -> observed.rotation | (observed.observerPowered ? 1L << 2 : 0L);
            case SIGNAL_SOURCE -> observed.signalSourceStrength;
            case REDSTONE_BLOCK, WALL -> 0L;
        };
        return ((long) observed.type.getId() + 1L) << 32 | state;
    }

    /** Processes torches scheduled for the same world time using the latest settled support inputs. */
    public boolean processTorchTicksAt(long scheduledGameTime) {
        boolean anyOutputChanged = false;
        for (RedstoneSlotData data : slots) {
            if (data == null || data.type != RedstoneComponent.TORCH || data.torchRotation == 4 ||
                data.torchScheduledGameTime != scheduledGameTime) continue;

            data.torchScheduledGameTime = -1L;
            data.torchRecentOffTimes = pruneRecentTorchOffs(data.torchRecentOffTimes, scheduledGameTime);
            if (data.torchBurnedOut) {
                data.torchBurnoutUntil = -1L;
                if (data.torchRecentOffTimes.length < TORCH_BURNOUT_MAX_OFFS) {
                    data.torchBurnedOut = false;
                }
                if (!data.torchBurnedOut && data.inputStrength == 0 && !data.torchPowered) {
                    data.torchPowered = true;
                    anyOutputChanged = true;
                }
                continue;
            }

            if (data.inputStrength > 0 && data.torchPowered) {
                data.torchPowered = false;
                data.torchRecentOffTimes = addRecentTorchOff(data.torchRecentOffTimes, scheduledGameTime);
                anyOutputChanged = true;
                if (data.torchRecentOffTimes.length > TORCH_BURNOUT_MAX_OFFS) {
                    data.torchBurnedOut = true;
                    data.torchBurnoutUntil = scheduledGameTime + TORCH_BURNOUT_RETRY_DELAY_GAME_TICKS;
                    data.torchScheduledGameTime = data.torchBurnoutUntil;
                }
            } else if (data.inputStrength == 0 && !data.torchPowered) {
                data.torchPowered = true;
                anyOutputChanged = true;
            }
        }
        return anyOutputChanged;
    }

    /** Schedules torch state changes after the vanilla two-game-tick delay. */
    public void scheduleTorchTransitions(long gameTime) {
        for (RedstoneSlotData data : slots) {
            if (data == null || data.type != RedstoneComponent.TORCH || data.torchRotation == 4) continue;
            data.torchRecentOffTimes = pruneRecentTorchOffs(data.torchRecentOffTimes, gameTime);
            boolean supportInputChanged = data.torchLastInputStrength != data.inputStrength;
            data.torchLastInputStrength = data.inputStrength;

            if (data.torchBurnedOut) {
                if (supportInputChanged && data.torchRecentOffTimes.length < TORCH_BURNOUT_MAX_OFFS) {
                    data.torchBurnedOut = false;
                    data.torchBurnoutUntil = -1L;
                    data.torchScheduledGameTime = -1L;
                } else {
                    continue;
                }
            }

            if (data.torchScheduledGameTime >= 0) continue;
            boolean shouldBePowered = data.inputStrength == 0;
            if (shouldBePowered != data.torchPowered) {
                data.torchScheduledGameTime = gameTime + TORCH_DELAY_GAME_TICKS;
            }
        }
    }

    private static long[] addRecentTorchOff(long[] recentOffTimes, long gameTime) {
        long[] recent = pruneRecentTorchOffs(recentOffTimes, gameTime);
        long[] updated = java.util.Arrays.copyOf(recent, recent.length + 1);
        updated[recent.length] = gameTime;
        return updated;
    }

    private static long[] pruneRecentTorchOffs(long[] recentOffTimes, long gameTime) {
        if (recentOffTimes.length == 0) return recentOffTimes;
        int firstRecent = 0;
        while (firstRecent < recentOffTimes.length &&
               gameTime - recentOffTimes[firstRecent] > TORCH_BURNOUT_WINDOW_GAME_TICKS) {
            firstRecent++;
        }
        return firstRecent == 0 ? recentOffTimes :
                java.util.Arrays.copyOfRange(recentOffTimes, firstRecent, recentOffTimes.length);
    }

    /**
     * Processes repeater lock state transitions using signal strengths from the previous tick.
     * When side power is first applied, the repeater freezes its current output state.
     * When side power is removed, the repeater unlocks and resumes normal operation.
     */
    private void processRepeaterLockTransitions(int[] previousStrengths) {
        for (int i = 0; i < GRID_SIZE; i++) {
            RedstoneSlotData data = slots[i];
            if (data == null) continue;
            if (data.type != RedstoneComponent.REPEATER) continue;

            boolean sidePowered = isRepeaterSidePowered(i, previousStrengths);

            if (sidePowered && !data.repeaterLocked) {
                // Transitioning from unlocked to locked: freeze the current output, not the live input.
                data.repeaterLocked = true;
                data.repeaterLockOutput = data.repeaterPowered;
            } else if (!sidePowered && data.repeaterLocked) {
                // Side power removed: unlock and resume normal operation.
                data.repeaterLocked = false;
                data.repeaterLockOutput = false;
            }
        }
    }

    /**
     * Runs a single pass of signal propagation.
     *
     * Models signal propagation in the grid:
     * - Power sources provide their configured strength to eligible adjacent components.
     * - Wire-to-wire transmission loses one signal strength; a direct source or strong wall
     *   provides its full level to adjacent wire.
     * - A wire's signal strength is what it outputs to adjacent wires.
     * - A wire directly in front of a repeater/comparator output, or behind an observer,
     *   receives the full directional output strength.
     *
     * Repeaters and comparators have no inherent signal strength on their own slots; their
     * directional outputs are handled separately. Observer output is event-triggered and also
     * stored separately from signalStrength, which remains 0 on those component slots.
     * - Repeater: its latched output is 15 when powered; scheduled transitions handle its delay.
     * - Comparator: in compare mode, outputs rear signal if both sides <= rear (else 0);
     *   in subtract mode, outputs max(rear - max(sides), 0). Side inputs read dust, directional components,
     *   strongly powered walls, and adjacent redstone blocks; torches and levers are ignored.
     */
    private void propagateOnePass() {
        // Snapshot current signal strengths so all components read from the same state.
        int[] currentStrengths = new int[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            RedstoneSlotData data = slots[i];
            if (data != null) {
                currentStrengths[i] = data.signalStrength;
            }
        }

        // Compute what signal each slot receives from its neighbors.
        // Weak-power sources broadcast to adjacent components (wire decays their signal by 1).
        // Repeater/comparator/observer outputs and mounted switch support power are handled separately.
        int[] receivedSignals = new int[GRID_SIZE];
        int[] receivedWireSignals = new int[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            receivedSignals[i] = getBoundaryInputSignalForSlot(i);
        }

        for (int i = 0; i < GRID_SIZE; i++) {
            RedstoneSlotData data = slots[i];
            if (data == null) continue;
            if (data.type == null) continue;

            // Directional components are handled separately; these components use weak-power broadcast.
            if (data.type == RedstoneComponent.REPEATER || data.type == RedstoneComponent.COMPARATOR ||
                data.type == RedstoneComponent.OBSERVER) continue;

            // WALL does not broadcast power — it's a solid block
            if (data.type == RedstoneComponent.WALL) continue;

            int outputSignal = currentStrengths[i];
            if (outputSignal == 0) continue;

            // Power sources weakly power adjacent components; attached torches exclude their support.
            // Redstone blocks, torches, levers, buttons, and signal sources skip adjacent wall blocks.
            Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            for (Direction dir : dirs) {
                if (data.type == RedstoneComponent.TORCH && data.torchRotation < 4 &&
                    dir == getFacingDirection(data.torchRotation)) continue;

                int x = getX(i);
                int y = getY(i);
                int neighborSlot = getNeighborSlot(x, y, dir);
                if (neighborSlot >= 0) {
                    if (data.type == RedstoneComponent.REDSTONE_BLOCK ||
                        data.type == RedstoneComponent.TORCH ||
                        data.type == RedstoneComponent.LEVER ||
                        data.type == RedstoneComponent.BUTTON ||
                        data.type == RedstoneComponent.SIGNAL_SOURCE) {
                        RedstoneSlotData neighborData = slots[neighborSlot];
                        if (neighborData != null && neighborData.type == RedstoneComponent.WALL) continue;
                    }

                    RedstoneSlotData neighborData = slots[neighborSlot];
                    if (data.type == RedstoneComponent.WIRE && neighborData != null &&
                        neighborData.type == RedstoneComponent.WIRE) {
                        receivedWireSignals[neighborSlot] = Math.max(receivedWireSignals[neighborSlot], outputSignal);
                    } else if (data.type == RedstoneComponent.WIRE && neighborData != null &&
                        neighborData.type == RedstoneComponent.WALL) {
                        if (isWireConnected(i, dir)) {
                            receivedSignals[neighborSlot] = Math.max(receivedSignals[neighborSlot], outputSignal);
                        }
                    } else {
                        receivedSignals[neighborSlot] = Math.max(receivedSignals[neighborSlot], outputSignal);
                    }
                }
            }
        }

        // Handle directional components: they output directly to their facing neighbor.
        // They do NOT set their own signalStrength — they are pass-through devices.
        // Their output is a strong power signal — the facing wire receives the full strength.
        int[] strongPower = new int[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            RedstoneSlotData data = slots[i];
            if (data == null) continue;
            if (data.type == null) continue;

            if (data.type == RedstoneComponent.REPEATER || data.type == RedstoneComponent.COMPARATOR ||
                data.type == RedstoneComponent.OBSERVER ||
                (data.type == RedstoneComponent.LEVER && data.leverRotation < 4) ||
                (data.type == RedstoneComponent.BUTTON && data.buttonRotation < 4)) {
                int outputSignal;

                if (data.type == RedstoneComponent.LEVER) {
                    // Wall-mounted lever: output is 15 if ON, 0 if OFF. Strongly powers the attachment block.
                    outputSignal = data.leverOn ? 15 : 0;
                } else if (data.type == RedstoneComponent.BUTTON) {
                    // Wall-mounted button: strongly powers its attachment while pressed.
                    outputSignal = data.buttonPowered ? 15 : 0;
                } else if (data.type == RedstoneComponent.REPEATER) {
                    int inputStrength = getInputFromDirectionAt(i, data.rotation, currentStrengths);
                    data.inputStrength = inputStrength;
                    if (data.repeaterLocked) {
                        // Locked repeater: output the frozen value regardless of input.
                        // The lock state is managed by processRepeaterLockTransitions() before propagation.
                        outputSignal = data.repeaterLockOutput ? 15 : 0;
                    } else {
                        // Normal repeater output is latched and only changes on a scheduled repeater tick.
                        outputSignal = data.repeaterPowered ? 15 : 0;
                    }
                } else if (data.type == RedstoneComponent.OBSERVER) {
                    outputSignal = data.observerPowered ? 15 : 0;
                } else {
                    // Comparator: compare or subtract mode
                    int rearInput = getInputFromDirectionAt(i, data.rotation, currentStrengths);
                    data.inputStrength = rearInput;
                    // Comparator sides read dust strength and strong power from directional components, walls, or redstone blocks.
                    int x = getX(i);
                    int y = getY(i);
                    Direction facing = getFacingDirection(data.rotation);
                    Direction side1 = facing.getClockWise();
                    Direction side2 = facing.getCounterClockWise();
                    int side1Signal = 0;
                    int side2Signal = 0;
                    int side1Slot = getNeighborSlot(x, y, side1);
                    int side2Slot = getNeighborSlot(x, y, side2);
                    side1Signal = getComparatorSideInput(side1Slot, side1);
                    side2Signal = getComparatorSideInput(side2Slot, side2);
                    int maxSide = Math.max(side1Signal, side2Signal);
                    if (data.comparatorSubtract) {
                        // Subtract mode: output = max(rear - max(left, right), 0)
                        data.comparatorTargetSignal = Math.max(rearInput - maxSide, 0);
                    } else {
                        // Compare mode: output = rear if both sides <= rear, else 0
                        data.comparatorTargetSignal = (maxSide <= rearInput) ? rearInput : 0;
                    }
                    // Comparator output is latched and changes only on its scheduled tick.
                    outputSignal = data.outputSignal;
                }

                // Directional outputs are latched by their component-specific scheduled transitions.
                if (data.type == RedstoneComponent.REPEATER || data.type == RedstoneComponent.OBSERVER) {
                    data.outputSignal = outputSignal;
                }

                // Output to neighbor(s) (strong power)
                if (outputSignal > 0) {
                    int x = getX(i);
                    int y = getY(i);
                    if (data.type == RedstoneComponent.LEVER || data.type == RedstoneComponent.BUTTON) {
                        // Wall-mounted switches strongly power only their attachment block.
                        int attachmentRotation = data.type == RedstoneComponent.BUTTON
                                ? data.buttonRotation : data.leverRotation;
                        Direction attachmentDir = getFacingDirection(attachmentRotation);
                        int attachmentSlot = getNeighborSlot(x, y, attachmentDir);
                        if (attachmentSlot >= 0) {
                            strongPower[attachmentSlot] = Math.max(strongPower[attachmentSlot], outputSignal);
                        }
                    } else {
                        // Repeaters/comparators output to facing direction; observers output opposite their observed side.
                        Direction facingDir = data.type == RedstoneComponent.OBSERVER
                                ? getFacingDirection(data.rotation).getOpposite()
                                : getFacingDirection(data.rotation);
                        int facingSlot = getNeighborSlot(x, y, facingDir);
                        if (facingSlot >= 0) {
                            strongPower[facingSlot] = Math.max(strongPower[facingSlot], outputSignal);
                        }
                    }
                }
            }
        }

        // A solid block strongly powered by a repeater, comparator, wall-mounted lever, or button
        // can pass that power to adjacent dust without dust-to-dust attenuation.
        for (int i = 0; i < GRID_SIZE; i++) {
            RedstoneSlotData data = slots[i];
            if (data == null || data.type != RedstoneComponent.WALL || strongPower[i] == 0) continue;

            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                int neighborSlot = getNeighborSlot(getX(i), getY(i), dir);
                if (neighborSlot < 0) continue;
                RedstoneSlotData neighborData = slots[neighborSlot];
                if (neighborData != null && neighborData.type == RedstoneComponent.WIRE) {
                    receivedSignals[neighborSlot] = Math.max(receivedSignals[neighborSlot], strongPower[i]);
                }
            }
        }

        // Compute each component's new signal strength based on what it received.
        // Power sources set their own signal. Wires use strong power or decay weak power.
        // Repeaters/comparators stay at 0.
        int[] newStrengths = new int[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            RedstoneSlotData data = slots[i];
            if (data == null) continue;
            if (data.type == null) continue;

            switch (data.type) {
                case WIRE -> {
                    // Power sources retain their strength; only dust-to-dust propagation decays.
                    int fromDust = Math.max(0, receivedWireSignals[i] - 1);
                    newStrengths[i] = Math.max(strongPower[i], Math.max(receivedSignals[i], fromDust));
                }
                case REPEATER -> {
                    // Repeater: no inherent signal strength (pass-through device)
                    newStrengths[i] = 0;
                }
                case COMPARATOR -> {
                    // Comparator: no inherent signal strength (pass-through device)
                    newStrengths[i] = 0;
                }
                case OBSERVER -> {
                    // Observer: output is directional and stored separately from signalStrength.
                    newStrengths[i] = 0;
                }
                case TORCH -> {
                    if (data.torchRotation == 4) {
                        newStrengths[i] = 15; // Standing torch always ON
                    } else {
                        int tx = getX(i);
                        int ty = getY(i);
                        Direction backDirection = getFacingDirection(data.torchRotation);
                        int backSlot = getNeighborSlot(tx, ty, backDirection);
                        data.inputStrength = backSlot >= 0
                                ? currentStrengths[backSlot]
                                : getBoundaryInputSignal(i, backDirection);
                        newStrengths[i] = data.torchPowered ? 15 : 0;
                    }
                }
                case WALL -> {
                    // Wall: solid block that can be powered but doesn't broadcast.
                    // If strongly powered (by repeater/comparator), use full strength.
                    // Otherwise, signal strength = max weak power received (no decay).
                    int strong = strongPower[i];
                    if (strong > 0) {
                        newStrengths[i] = strong;
                    } else {
                        newStrengths[i] = receivedSignals[i];
                    }
                }
                case LEVER -> {
                    // Lever: toggleable power source
                    newStrengths[i] = data.leverOn ? 15 : 0;
                }
                case BUTTON -> {
                    // Button: momentary power source, released by its scheduled tick.
                    newStrengths[i] = data.buttonPowered ? 15 : 0;
                }
                case SIGNAL_SOURCE -> newStrengths[i] = Math.clamp(data.signalSourceStrength, 0, 15);
                case REDSTONE_BLOCK -> {
                    // Redstone block: constant power source
                    newStrengths[i] = 15;
                }
            }
        }

        // Update signal strengths in slot data objects
        for (int i = 0; i < GRID_SIZE; i++) {
            RedstoneSlotData data = slots[i];
            if (data != null) {
                data.signalStrength = newStrengths[i];
            }
        }
        System.arraycopy(strongPower, 0, previousStrongPower, 0, GRID_SIZE);
    }

    private int getComparatorSideInput(int sideSlot, Direction directionFromComparator) {
        if (sideSlot < 0 || sideSlot >= GRID_SIZE) return 0;
        RedstoneSlotData sideData = slots[sideSlot];
        if (sideData == null) return 0;

        if (sideData.type == RedstoneComponent.REDSTONE_BLOCK) return 15;
        if (sideData.type == RedstoneComponent.WALL) return previousStrongPower[sideSlot];
        if (sideData.type == RedstoneComponent.WIRE) return sideData.signalStrength;
        if ((sideData.type == RedstoneComponent.REPEATER || sideData.type == RedstoneComponent.COMPARATOR) &&
            getFacingDirection(sideData.rotation) == directionFromComparator.getOpposite()) {
            return sideData.outputSignal;
        }
        return 0;
    }

    /**
     * Gets the neighbor slot index in the given direction from (x, y).
     * Returns -1 if out of bounds.
     */
    private int getNeighborSlot(int x, int y, Direction dir) {
        return switch (dir) {
            case NORTH -> { if (y > 0) yield slotIndex(x, y - 1); else yield -1; }
            case SOUTH -> { if (y < GRID_HEIGHT - 1) yield slotIndex(x, y + 1); else yield -1; }
            case WEST -> { if (x > 0) yield slotIndex(x - 1, y); else yield -1; }
            case EAST -> { if (x < GRID_WIDTH - 1) yield slotIndex(x + 1, y); else yield -1; }
            default -> -1;
        };
    }

    /**
     * Gets the input signal from a specific direction using a snapshot array.
     * This ensures repeaters and comparators read from the same signal state as other components in a pass.
     */
    private int getInputFromDirectionAt(int slotIndex, int rotation, int[] strengths) {
        Direction inputDir = getInputDirection(rotation);
        int x = getX(slotIndex);
        int y = getY(slotIndex);
        int inputSlot = -1;

        switch (inputDir) {
            case NORTH -> {
                if (y > 0) inputSlot = slotIndex(x, y - 1);
            }
            case SOUTH -> {
                if (y < GRID_HEIGHT - 1) inputSlot = slotIndex(x, y + 1);
            }
            case WEST -> {
                if (x > 0) inputSlot = slotIndex(x - 1, y);
            }
            case EAST -> {
                if (x < GRID_WIDTH - 1) inputSlot = slotIndex(x + 1, y);
            }
        }

        if (inputSlot < 0) return getBoundaryInputSignal(slotIndex, inputDir);

        int inputStrength = strengths[inputSlot];
        RedstoneSlotData inputData = slots[inputSlot];
        if (inputData != null &&
            (inputData.type == RedstoneComponent.REPEATER || inputData.type == RedstoneComponent.COMPARATOR) &&
            getFacingDirection(inputData.rotation) == inputDir.getOpposite()) {
            inputStrength = Math.max(inputStrength, inputData.outputSignal);
        }
        return inputStrength;
    }


    private int getBoundaryInputSignal(int slotIndex, Direction direction) {
        int side = getBoundarySideIndex(direction);
        if (side < 0) return 0;

        int edgePosition = switch (direction) {
            case NORTH, SOUTH -> getX(slotIndex);
            case WEST, EAST -> getY(slotIndex);
            default -> -1;
        };

        int worldSignal = inputSignals != null && inputSignals.length > side
                ? Math.clamp(inputSignals[side], 0, 15)
                : 0;
        int sharedWallSignal = edgePosition >= 0 && edgePosition < boundaryInputSignals[side].length
                ? boundaryInputSignals[side][edgePosition]
                : 0;
        return Math.max(worldSignal, sharedWallSignal);
    }

    private int getBoundaryInputSignalForSlot(int slotIndex) {
        int x = getX(slotIndex);
        int y = getY(slotIndex);
        int signal = 0;
        if (y == 0) signal = Math.max(signal, getBoundaryInputSignal(slotIndex, Direction.NORTH));
        if (y == GRID_HEIGHT - 1) signal = Math.max(signal, getBoundaryInputSignal(slotIndex, Direction.SOUTH));
        if (x == 0) signal = Math.max(signal, getBoundaryInputSignal(slotIndex, Direction.WEST));
        if (x == GRID_WIDTH - 1) signal = Math.max(signal, getBoundaryInputSignal(slotIndex, Direction.EAST));
        return signal;
    }

    private void calculateBoundaryWallOutputs() {
        boundaryWallOutputs = createBoundarySignalMatrix();
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            int side = getBoundarySideIndex(direction);
            for (int edgePosition = 0; edgePosition < getBoundaryEdgeLength(direction); edgePosition++) {
                int slotIndex = getBoundarySlot(direction, edgePosition);
                RedstoneSlotData data = slots[slotIndex];
                if (data == null || data.type == null) continue;

                int output = switch (data.type) {
                    // Wire weakly powers the shared wall only when its shape points out of the grid.
                    case WIRE -> isWireConnected(slotIndex, direction) ? data.signalStrength : 0;
                    // Directional components only power the wall when aimed into it.
                    case REPEATER, COMPARATOR -> getFacingDirection(data.rotation) == direction ? data.outputSignal : 0;
                    case OBSERVER -> getFacingDirection(data.rotation).getOpposite() == direction
                            ? data.outputSignal : 0;
                    // A wall-mounted lever strongly powers its attachment wall.
                    case LEVER -> data.leverOn && data.leverRotation < 4 &&
                            getFacingDirection(data.leverRotation) == direction ? 15 : 0;
                    // A wall-mounted button strongly powers its attachment wall while pressed.
                    case BUTTON -> data.buttonPowered && data.buttonRotation < 4 &&
                            getFacingDirection(data.buttonRotation) == direction ? 15 : 0;
                    // Torches, standing sources, and walls do not power wall blocks here.
                    default -> 0;
                };
                boundaryWallOutputs[side][edgePosition] = Math.clamp(output, 0, 15);
            }
        }
    }

    private static int getBoundarySlot(Direction direction, int edgePosition) {
        return switch (direction) {
            case NORTH -> slotIndex(edgePosition, 0);
            case SOUTH -> slotIndex(edgePosition, GRID_HEIGHT - 1);
            case WEST -> slotIndex(0, edgePosition);
            case EAST -> slotIndex(GRID_WIDTH - 1, edgePosition);
            default -> -1;
        };
    }

    private static int[][] createBoundarySignalMatrix() {
        return new int[][]{
                new int[GRID_WIDTH],
                new int[GRID_WIDTH],
                new int[GRID_HEIGHT],
                new int[GRID_HEIGHT]
        };
    }

    private static int[][] copyBoundarySignalMatrix(int[][] source) {
        int[][] copy = createBoundarySignalMatrix();
        if (source == null) return copy;

        for (int side = 0; side < copy.length && side < source.length; side++) {
            if (source[side] == null) continue;
            for (int position = 0; position < copy[side].length && position < source[side].length; position++) {
                copy[side][position] = Math.clamp(source[side][position], 0, 15);
            }
        }
        return copy;
    }


    /**
     * Calculates output signals from edge slots of the circuit.
     * Returns 4-element array: [top, bottom, left, right]
     */
    private int[] calculateOutputs() {
        int[] outputs = new int[4];
        // Top edge (y=0)
        for (int x = 0; x < GRID_WIDTH; x++) {
            int slot = slotIndex(x, 0);
            outputs[0] = Math.max(outputs[0], getEdgeOutputStrength(slot, Direction.NORTH));
        }
        // Bottom edge (y=GRID_HEIGHT-1)
        for (int x = 0; x < GRID_WIDTH; x++) {
            int slot = slotIndex(x, GRID_HEIGHT - 1);
            outputs[1] = Math.max(outputs[1], getEdgeOutputStrength(slot, Direction.SOUTH));
        }
        // Left edge (x=0)
        for (int y = 0; y < GRID_HEIGHT; y++) {
            int slot = slotIndex(0, y);
            outputs[2] = Math.max(outputs[2], getEdgeOutputStrength(slot, Direction.WEST));
        }
        // Right edge (x=GRID_WIDTH-1)
        for (int y = 0; y < GRID_HEIGHT; y++) {
            int slot = slotIndex(GRID_WIDTH - 1, y);
            outputs[3] = Math.max(outputs[3], getEdgeOutputStrength(slot, Direction.EAST));
        }
        return outputs;
    }

    private int getEdgeOutputStrength(int slotIndex, Direction edgeDirection) {
        RedstoneSlotData data = slots[slotIndex];
        if (data == null) return 0;

        int output = data.signalStrength;
        if ((data.type == RedstoneComponent.REPEATER || data.type == RedstoneComponent.COMPARATOR) &&
            getFacingDirection(data.rotation) == edgeDirection) {
            output = Math.max(output, data.outputSignal);
        } else if (data.type == RedstoneComponent.OBSERVER &&
                getFacingDirection(data.rotation).getOpposite() == edgeDirection) {
            output = Math.max(output, data.outputSignal);
        }
        return output;
    }

    /**
     * Serializes the circuit to NBT for persistence.
     * Uses a per-slot CompoundTag format for extensibility.
     * Computed slot signal strengths are not persisted. Latched comparator outputs and pending
     * comparator due times are persisted; current comparator targets are recalculated next tick.
     *
     * @return The NBT tag containing the circuit data
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag slotsList = new ListTag();

        for (int i = 0; i < GRID_SIZE; i++) {
            RedstoneSlotData data = slots[i];
            if (data == null) continue;

            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("Index", i);
            slotTag.putInt("Type", data.type.getId());
            slotTag.putInt("Rotation", data.rotation);
            slotTag.putInt("TorchRotation", data.torchRotation);
            slotTag.putBoolean("TorchPowered", data.torchPowered);
            slotTag.putLong("TorchScheduledGameTime", data.torchScheduledGameTime);
            slotTag.putBoolean("TorchBurnedOut", data.torchBurnedOut);
            slotTag.putLong("TorchBurnoutUntil", data.torchBurnoutUntil);
            slotTag.putLongArray("TorchRecentOffTimes", data.torchRecentOffTimes);
            slotTag.putInt("TorchLastInputStrength", data.torchLastInputStrength);
            slotTag.putInt("Delay", data.repeaterDelay);
            slotTag.putBoolean("RepeaterPowered", data.repeaterPowered);
            slotTag.putLong("RepeaterScheduledGameTime", data.repeaterScheduledGameTime);
            slotTag.putBoolean("Locked", data.repeaterLocked);
            slotTag.putBoolean("LockOutput", data.repeaterLockOutput);
            slotTag.putBoolean("Subtract", data.comparatorSubtract);
            if (data.type == RedstoneComponent.COMPARATOR) {
                slotTag.putInt("ComparatorOutputSignal", data.outputSignal);
                slotTag.putLong("ComparatorScheduledGameTime", data.comparatorScheduledGameTime);
            }
            slotTag.putBoolean("LeverOn", data.leverOn);
            slotTag.putInt("LeverRotation", data.leverRotation);
            slotTag.putBoolean("ButtonPowered", data.buttonPowered);
            slotTag.putLong("ButtonScheduledGameTime", data.buttonScheduledGameTime);
            slotTag.putInt("ButtonDurationGameTicks", data.buttonDurationGameTicks);
            slotTag.putInt("ButtonRotation", data.buttonRotation);
            slotTag.putBoolean("ObserverPowered", data.observerPowered);
            slotTag.putLong("ObserverScheduledGameTime", data.observerScheduledGameTime);
            slotTag.putInt("ObserverLastObservedRotation", data.observerLastObservedRotation);
            slotTag.putLong("ObserverObservedStateSignature", data.observerObservedStateSignature);
            slotTag.putInt("SignalSourceStrength", data.signalSourceStrength);
            // Signal strength is a computed value — do not persist it in NBT.
            // It will be recalculated by propagateSignals() on the next tick.
            slotsList.add(slotTag);
        }

        tag.put("Slots", slotsList);
        CompoundTag boundaryOutputs = new CompoundTag();
        boundaryOutputs.putIntArray("Top", boundaryWallOutputs[0]);
        boundaryOutputs.putIntArray("Bottom", boundaryWallOutputs[1]);
        boundaryOutputs.putIntArray("Left", boundaryWallOutputs[2]);
        boundaryOutputs.putIntArray("Right", boundaryWallOutputs[3]);
        tag.put("BoundaryWallOutputs", boundaryOutputs);
        return tag;
    }

    /**
     * Deserializes the circuit from NBT data.
     * Uses the per-slot CompoundTag format. Computed signal strengths start at 0 and are
     * recomputed by the next propagation cycle; latched comparator outputs and pending times persist.
     *
     * @param tag The NBT tag containing the circuit data
     * @return A new RedstoneCircuit with the deserialized data
     */
    public static RedstoneCircuit deserializeNBT(CompoundTag tag) {
        RedstoneCircuit circuit = new RedstoneCircuit();

        if (tag.contains("Slots")) {
            ListTag slotsList = tag.getList("Slots", 10);
            for (int i = 0; i < slotsList.size(); i++) {
                CompoundTag slotTag = (CompoundTag) slotsList.get(i);
                int index = slotTag.getInt("Index");
                if (index < 0 || index >= GRID_SIZE) continue;

                int typeId = slotTag.getInt("Type");
                RedstoneComponent comp = RedstoneComponent.fromId(typeId);
                if (comp == null) continue;

                RedstoneSlotData data = new RedstoneSlotData(comp);
                data.rotation = Math.clamp(getIntOrDefault(slotTag, "Rotation", data.rotation), 0, 3);
                data.torchRotation = Math.clamp(getIntOrDefault(slotTag, "TorchRotation", data.torchRotation), 0, 4);
                data.torchPowered = slotTag.contains("TorchPowered", Tag.TAG_BYTE)
                        ? slotTag.getBoolean("TorchPowered") : true;
                data.torchScheduledGameTime = slotTag.contains("TorchScheduledGameTime", Tag.TAG_LONG)
                        ? slotTag.getLong("TorchScheduledGameTime") : -1L;
                data.torchBurnedOut = slotTag.getBoolean("TorchBurnedOut");
                data.torchBurnoutUntil = slotTag.contains("TorchBurnoutUntil", Tag.TAG_LONG)
                        ? slotTag.getLong("TorchBurnoutUntil") : -1L;
                data.torchRecentOffTimes = slotTag.getLongArray("TorchRecentOffTimes");
                data.torchLastInputStrength = Math.clamp(getIntOrDefault(slotTag, "TorchLastInputStrength", 0), 0, 15);
                data.repeaterDelay = Math.clamp(getIntOrDefault(slotTag, "Delay", data.repeaterDelay), 1, 4);
                data.repeaterPowered = slotTag.getBoolean("RepeaterPowered");
                data.repeaterScheduledGameTime = slotTag.contains("RepeaterScheduledGameTime")
                        ? slotTag.getLong("RepeaterScheduledGameTime") : -1L;
                data.repeaterLocked = slotTag.getBoolean("Locked");
                data.repeaterLockOutput = slotTag.getBoolean("LockOutput");
                data.comparatorSubtract = slotTag.getBoolean("Subtract");
                if (comp == RedstoneComponent.COMPARATOR) {
                    data.outputSignal = Math.clamp(getIntOrDefault(slotTag, "ComparatorOutputSignal", 0), 0, 15);
                    data.comparatorScheduledGameTime = slotTag.contains("ComparatorScheduledGameTime")
                            ? slotTag.getLong("ComparatorScheduledGameTime") : -1L;
                }
                data.leverOn = slotTag.getBoolean("LeverOn");
                data.leverRotation = Math.clamp(getIntOrDefault(slotTag, "LeverRotation", data.leverRotation), 0, 4);
                data.buttonPowered = slotTag.getBoolean("ButtonPowered");
                data.buttonScheduledGameTime = slotTag.contains("ButtonScheduledGameTime", Tag.TAG_LONG)
                        ? slotTag.getLong("ButtonScheduledGameTime") : -1L;
                int buttonDuration = getIntOrDefault(slotTag, "ButtonDurationGameTicks", STONE_BUTTON_DURATION_GAME_TICKS);
                data.buttonDurationGameTicks = buttonDuration == WOODEN_BUTTON_DURATION_GAME_TICKS
                        ? WOODEN_BUTTON_DURATION_GAME_TICKS : STONE_BUTTON_DURATION_GAME_TICKS;
                data.buttonRotation = Math.clamp(getIntOrDefault(slotTag, "ButtonRotation", data.buttonRotation), 0, 4);
                data.observerPowered = slotTag.getBoolean("ObserverPowered");
                data.observerScheduledGameTime = slotTag.contains("ObserverScheduledGameTime", Tag.TAG_LONG)
                        ? slotTag.getLong("ObserverScheduledGameTime") : -1L;
                data.observerLastObservedRotation = Math.clamp(
                        getIntOrDefault(slotTag, "ObserverLastObservedRotation", -1), -1, 3);
                data.observerObservedStateSignature = slotTag.contains("ObserverObservedStateSignature", Tag.TAG_LONG)
                        ? slotTag.getLong("ObserverObservedStateSignature") : Long.MIN_VALUE;
                data.signalSourceStrength = Math.clamp(
                        getIntOrDefault(slotTag, "SignalSourceStrength", data.signalSourceStrength), 0, 15);
                // Signal strength is not persisted — it starts at 0 and is computed by propagateSignals()
                circuit.slots[index] = data;
            }
        }

        if (tag.contains("BoundaryWallOutputs", 10)) {
            CompoundTag boundaryOutputs = tag.getCompound("BoundaryWallOutputs");
            circuit.boundaryWallOutputs = copyBoundarySignalMatrix(new int[][]{
                    boundaryOutputs.getIntArray("Top"),
                    boundaryOutputs.getIntArray("Bottom"),
                    boundaryOutputs.getIntArray("Left"),
                    boundaryOutputs.getIntArray("Right")
            });
        }

        return circuit;
    }

    private static int getIntOrDefault(CompoundTag tag, String key, int defaultValue) {
        return tag.contains(key, Tag.TAG_INT) ? tag.getInt(key) : defaultValue;
    }

    /**
     * Reads the circuit data from a module's item stack CustomData component.
     * Returns a new empty circuit if no data is found.
     *
     * @param stack The module item stack
     * @param registries Registry lookup provider (reserved for future use)
     * @return The deserialized circuit, or a new empty circuit if none is stored
     */
    public static RedstoneCircuit fromStack(ItemStack stack, HolderLookup.Provider registries) {
        CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Circuit")) {
                return deserializeNBT(tag.getCompound("Circuit"));
            }
        }
        return new RedstoneCircuit();
    }

    /**
     * Saves the circuit data to a module's item stack CustomData component.
     * Creates the CustomData component if it doesn't exist.
     *
     * @param stack The module item stack to save to
     */
    public void toStack(ItemStack stack) {
        CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.put("Circuit", serializeNBT());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Creates a snapshot of the current circuit state for client rendering.
     * This captures all component data and computed signal strengths after propagation.
     *
     * @return A read-only snapshot of the circuit state
     */
    public RedstoneCircuitState computeState() {
        return new RedstoneCircuitState(this);
    }
}
