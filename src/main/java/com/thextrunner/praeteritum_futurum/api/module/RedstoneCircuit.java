package com.thextrunner.praeteritum_futurum.api.module;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Data model for the 6×9 (54-slot) redstone circuit grid inside a Redstone Module.
 * <p>
 * Each slot can contain a RedstoneComponent with a signal strength (0-15).
 * The circuit is stored in the module's CustomData component as NBT.
 * <p>
 * Signal propagation runs every tick and evaluates the circuit state.
 */
public class RedstoneCircuit {
    public static final int GRID_WIDTH = 9;
    public static final int GRID_HEIGHT = 6;
    public static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT; // 54 slots (double chest layout: 9 columns × 6 rows)

    private final int[] componentIds;  // component type ID for each slot
    private final int[] signalStrengths; // current signal strength (0-15) for each slot
    private final int[] rotations; // rotation 0-3 (N/E/S/W) for each slot
    private final int[] wallAttachments; // wall attachment direction index (0-3 for UP/DOWN/LEFT/RIGHT, -1 = none)

    public RedstoneCircuit() {
        this.componentIds = new int[GRID_SIZE];
        this.signalStrengths = new int[GRID_SIZE];
        this.rotations = new int[GRID_SIZE];
        this.wallAttachments = new int[GRID_SIZE];
        java.util.Arrays.fill(this.wallAttachments, -1);
    }

    public RedstoneCircuit(int[] componentIds, int[] signalStrengths, int[] rotations, int[] wallAttachments) {
        this.componentIds = componentIds.clone();
        this.signalStrengths = signalStrengths.clone();
        this.rotations = rotations.clone();
        this.wallAttachments = wallAttachments.clone();
    }

    /**
     * Gets the component type at the given slot index.
     */
    public RedstoneComponent getComponent(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return null;
        int id = componentIds[slotIndex];
        if (id == 0) return null; // empty slot
        return RedstoneComponent.fromId(id);
    }

    /**
     * Sets the component type at the given slot index.
     */
    public void setComponent(int slotIndex, RedstoneComponent component) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        if (component == null) {
            componentIds[slotIndex] = 0;
            signalStrengths[slotIndex] = 0;
            rotations[slotIndex] = 0;
        } else {
            componentIds[slotIndex] = component.getId();
        }
    }

    /**
     * Gets the rotation of the component at the given slot index (0-3 for N/E/S/W).
     */
    public int getRotation(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        return rotations[slotIndex];
    }

    /**
     * Sets the rotation of the component at the given slot index.
     */
    public void setRotation(int slotIndex, int rotation) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        rotations[slotIndex] = rotation % 4;
    }

    /**
     * Rotates the component at the given slot index 90° clockwise.
     */
    public void rotateComponent(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        if (isEmpty(slotIndex)) return;
        rotations[slotIndex] = (rotations[slotIndex] + 1) % 4;
    }

    /**
     * Gets the wall attachment direction at the given slot index.
     * Returns -1 if no wall attachment is present.
     */
    public int getWallAttachment(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return -1;
        return wallAttachments[slotIndex];
    }

    /**
     * Toggles the wall attachment at the given slot index.
     * Cycles through UP(0) → DOWN(1) → LEFT(2) → RIGHT(3) → none(-1).
     */
    public void toggleWallAttachment(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        int current = wallAttachments[slotIndex];
        if (current < 0 || current >= 4) {
            wallAttachments[slotIndex] = 0;
        } else {
            wallAttachments[slotIndex] = (current + 1) % 5;
            if (wallAttachments[slotIndex] == 4) {
                wallAttachments[slotIndex] = -1;
            }
        }
    }

    /**
     * Converts a wall attachment index to a Direction.
     * 0=UP, 1=DOWN, 2=LEFT, 3=RIGHT
     */
    public static Direction wallAttachmentToDirection(int index) {
        return switch (index) {
            case 0 -> Direction.UP;
            case 1 -> Direction.DOWN;
            case 2 -> Direction.WEST;
            case 3 -> Direction.EAST;
            default -> null;
        };
    }

    /**
     * Gets the input direction for a component based on its rotation.
     * Rotation 0=N, 1=E, 2=S, 3=W
     */
    public static Direction getInputDirection(int rotation) {
        return switch (rotation % 4) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    /**
     * Gets the output direction for a component based on its rotation.
     * Opposite of input direction.
     */
    public static Direction getOutputDirection(int rotation) {
        return getInputDirection(rotation).getOpposite();
    }

    /**
     * Gets the signal strength at the given slot index.
     */
    public int getSignalStrength(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return 0;
        return signalStrengths[slotIndex];
    }

    /**
     * Sets the signal strength at the given slot index.
     */
    public void setSignalStrength(int slotIndex, int strength) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return;
        signalStrengths[slotIndex] = Math.clamp(strength, 0, 15);
    }

    /**
     * Checks if a slot is empty.
     */
    public boolean isEmpty(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= GRID_SIZE) return true;
        return componentIds[slotIndex] == 0;
    }

    /**
     * Converts a 2D grid coordinate to a slot index.
     */
    public static int slotIndex(int x, int y) {
        return y * GRID_WIDTH + x;
    }

    /**
     * Gets the X coordinate from a slot index.
     */
    public static int getX(int slotIndex) {
        return slotIndex % GRID_WIDTH;
    }

    /**
     * Gets the Y coordinate from a slot index.
     */
    public static int getY(int slotIndex) {
        return slotIndex / GRID_WIDTH;
    }

    /**
     * Gets all adjacent slot indices (up, down, left, right) for a given slot.
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
     * Runs one tick of signal propagation across the circuit.
     * This evaluates all components and updates their signal strengths.
     *
     * @param inputSignals array of 6 input signal strengths (from adjacent machine slots)
     * @return array of 6 output signal strengths (to adjacent machine slots)
     */
    public int[] propagateSignals(int[] inputSignals) {
        int[] newStrengths = new int[GRID_SIZE];

        for (int i = 0; i < GRID_SIZE; i++) {
            if (isEmpty(i)) continue;

            RedstoneComponent component = getComponent(i);
            if (component == null) continue;

            int rotation = getRotation(i);
            switch (component) {
                case WIRE -> {
                    // Wire passes signal from all neighbors, decays by 1
                    int inputStrength = getMaximumInput(i);
                    newStrengths[i] = Math.max(0, inputStrength - 1);
                }
                case REPEATER -> {
                    // Repeater reads from input direction, outputs to opposite direction
                    // Works as a one-tick repeater (vanilla default)
                    int inputStrength = getInputFromDirection(i, rotation);
                    newStrengths[i] = inputStrength > 0 ? Math.min(15, inputStrength) : 0;
                }
                case COMPARATOR -> {
                    // Comparator reads from input direction, outputs to opposite direction
                    int inputStrength = getInputFromDirection(i, rotation);
                    newStrengths[i] = inputStrength;
                }
                case TORCH -> {
                    // Torch inverts signal (NOT gate)
                    // If wall attachment is present, check input from that direction
                    int wallAtt = getWallAttachment(i);
                    int inputStrength;
                    if (wallAtt >= 0 && wallAtt < 4) {
                        // Torch attached to wall — check input from the wall side
                        inputStrength = getInputFromWallDirection(i, wallAtt);
                    } else {
                        inputStrength = getMaximumInput(i);
                    }
                    newStrengths[i] = inputStrength > 0 ? 0 : 15;
                }
            }
        }

        // Update signal strengths
        System.arraycopy(newStrengths, 0, signalStrengths, 0, GRID_SIZE);

        // Calculate output signals from edge slots
        return calculateOutputs();
    }

    /**
     * Gets the maximum input signal strength from adjacent slots.
     */
    private int getMaximumInput(int slotIndex) {
        int[] adjacent = getAdjacentSlots(slotIndex);
        int maxStrength = 0;
        for (int adj : adjacent) {
            maxStrength = Math.max(maxStrength, signalStrengths[adj]);
        }
        return maxStrength;
    }

    /**
     * Gets the input signal from a specific direction based on component rotation.
     * Used by repeaters and comparators to read only from their input side.
     */
    private int getInputFromDirection(int slotIndex, int rotation) {
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

        if (inputSlot >= 0) {
            return signalStrengths[inputSlot];
        }
        return 0;
    }

    /**
     * Gets the input signal from a wall attachment direction.
     * Used by torches attached to walls to check input from that side.
     */
    private int getInputFromWallDirection(int slotIndex, int wallAttachmentIndex) {
        Direction wallDir = wallAttachmentToDirection(wallAttachmentIndex);
        if (wallDir == null) return getMaximumInput(slotIndex);

        int x = getX(slotIndex);
        int y = getY(slotIndex);
        int inputSlot = -1;

        // Wall attachment direction is the side the torch is attached to
        // Input comes from the opposite side
        Direction inputDir = wallDir.getOpposite();
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

        if (inputSlot >= 0) {
            return signalStrengths[inputSlot];
        }
        return 0;
    }

    /**
     * Calculates output signals from edge slots of the circuit.
     */
    private int[] calculateOutputs() {
        int[] outputs = new int[6];
        // Top edge (y=0)
        for (int x = 0; x < GRID_WIDTH; x++) {
            int slot = slotIndex(x, 0);
            if (!isEmpty(slot)) {
                outputs[0] = Math.max(outputs[0], signalStrengths[slot]);
            }
        }
        // Bottom edge (y=GRID_HEIGHT-1)
        for (int x = 0; x < GRID_WIDTH; x++) {
            int slot = slotIndex(x, GRID_HEIGHT - 1);
            if (!isEmpty(slot)) {
                outputs[1] = Math.max(outputs[1], signalStrengths[slot]);
            }
        }
        // Left edge (x=0)
        for (int y = 0; y < GRID_HEIGHT; y++) {
            int slot = slotIndex(0, y);
            if (!isEmpty(slot)) {
                outputs[2] = Math.max(outputs[2], signalStrengths[slot]);
            }
        }
        // Right edge (x=GRID_WIDTH-1)
        for (int y = 0; y < GRID_HEIGHT; y++) {
            int slot = slotIndex(GRID_WIDTH - 1, y);
            if (!isEmpty(slot)) {
                outputs[3] = Math.max(outputs[3], signalStrengths[slot]);
            }
        }
        return outputs;
    }

    /**
     * Serializes the circuit to NBT.
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag componentsList = new ListTag();
        ListTag signalsList = new ListTag();
        ListTag rotationsList = new ListTag();
        ListTag wallAttachmentsList = new ListTag();

        for (int i = 0; i < GRID_SIZE; i++) {
            if (!isEmpty(i)) {
                componentsList.add(IntTag.valueOf(i * 100 + componentIds[i]));
            }
            signalsList.add(IntTag.valueOf(signalStrengths[i]));
            rotationsList.add(IntTag.valueOf(rotations[i]));
            wallAttachmentsList.add(IntTag.valueOf(wallAttachments[i]));
        }

        tag.put("Components", componentsList);
        tag.put("Signals", signalsList);
        tag.put("Rotations", rotationsList);
        tag.put("WallAttachments", wallAttachmentsList);
        return tag;
    }

    /**
     * Deserializes the circuit from NBT.
     */
    public static RedstoneCircuit deserializeNBT(CompoundTag tag) {
        int[] componentIds = new int[GRID_SIZE];
        int[] signalStrengths = new int[GRID_SIZE];
        int[] rotations = new int[GRID_SIZE];
        int[] wallAttachments = new int[GRID_SIZE];
        java.util.Arrays.fill(wallAttachments, -1);

        if (tag.contains("Components")) {
            ListTag componentsList = tag.getList("Components", 99);
            for (int i = 0; i < componentsList.size(); i++) {
                int value = componentsList.getInt(i);
                int slotIndex = value / 100;
                int componentId = value % 100;
                if (slotIndex >= 0 && slotIndex < GRID_SIZE) {
                    componentIds[slotIndex] = componentId;
                }
            }
        }

        if (tag.contains("Signals")) {
            ListTag signalsList = tag.getList("Signals", 99);
            for (int i = 0; i < Math.min(signalsList.size(), GRID_SIZE); i++) {
                signalStrengths[i] = signalsList.getInt(i);
            }
        }

        if (tag.contains("Rotations")) {
            ListTag rotationsList = tag.getList("Rotations", 99);
            for (int i = 0; i < Math.min(rotationsList.size(), GRID_SIZE); i++) {
                rotations[i] = rotationsList.getInt(i);
            }
        }

        if (tag.contains("WallAttachments")) {
            ListTag wallAttachmentsList = tag.getList("WallAttachments", 99);
            for (int i = 0; i < Math.min(wallAttachmentsList.size(), GRID_SIZE); i++) {
                wallAttachments[i] = wallAttachmentsList.getInt(i);
            }
        }

        return new RedstoneCircuit(componentIds, signalStrengths, rotations, wallAttachments);
    }

    /**
     * Gets the circuit data from a module's item stack.
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
     * Saves the circuit data to a module's item stack.
     */
    public void toStack(ItemStack stack) {
        CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.put("Circuit", serializeNBT());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
