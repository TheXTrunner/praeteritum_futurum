package com.thextrunner.praeteritum_futurum.block;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;

/**
 * Directions for navigating the machine's internal 3×9 grid of slots.
 * <p>
 * This enum is used exclusively for grid-adjacent slot navigation — finding which
 * slot sits above, below, to the left, or to the right of a given slot in the
 * machine's inventory grid. It is NOT related to world-facing directions.
 * <p>
 * World-facing directions (NORTH, SOUTH, WEST, EAST, UP, DOWN) are handled by
 * {@link net.minecraft.core.Direction} and {@link RelativeSide}, which are used
 * by the IO module to determine which block faces interact with the world.
 * <p>
 * The four grid directions map to the machine grid as follows:
 * <ul>
 *   <li>{@link #UP} — the slot in the row above (slotIndex - GRID_WIDTH)</li>
 *   <li>{@link #DOWN} — the slot in the row below (slotIndex + GRID_WIDTH)</li>
 *   <li>{@link #LEFT} — the slot to the left (slotIndex - 1)</li>
 *   <li>{@link #RIGHT} — the slot to the right (slotIndex + 1)</li>
 * </ul>
 * <p>
 * Using this enum instead of {@link net.minecraft.core.Direction} for grid
 * navigation prevents confusion between grid-adjacent slots and world-facing
 * block directions. For example, {@link net.minecraft.core.Direction#UP} and
 * {@link net.minecraft.core.Direction#DOWN} are valid world-facing directions
 * (ceiling/floor) but have different meanings in the grid context.
 */
public enum GridDirection {
    /** Slot in the row above the current slot */
    UP,
    /** Slot in the row below the current slot */
    DOWN,
    /** Slot to the left of the current slot */
    LEFT,
    /** Slot to the right of the current slot */
    RIGHT;

    /**
     * All grid directions in a fixed order for iteration.
     */
    public static final GridDirection[] VALUES = values();

    /**
     * Returns the label character used in overlay UI buttons.
     */
    public String labelChar() {
        return switch (this) {
            case UP -> "U";
            case DOWN -> "D";
            case LEFT -> "L";
            case RIGHT -> "R";
        };
    }

    /**
     * Returns the NBT key name for this direction.
     * Uses lowercase names matching the enum constant names.
     */
    public String nbtKey() {
        return name().toLowerCase();
    }

    /**
     * Computes the grid slot index adjacent to the given slot in this direction.
     * Returns -1 if no adjacent slot exists in that direction (border of the grid).
     */
    public int getAdjacentSlotIndex(int slotIndex) {
        int row = slotIndex / MachineFrameworkBlockEntity.GRID_WIDTH;
        int col = slotIndex % MachineFrameworkBlockEntity.GRID_WIDTH;

        return switch (this) {
            case UP -> row > 0 ? slotIndex - MachineFrameworkBlockEntity.GRID_WIDTH : -1;
            case DOWN -> row < MachineFrameworkBlockEntity.GRID_ROWS - 1 ? slotIndex + MachineFrameworkBlockEntity.GRID_WIDTH : -1;
            case LEFT -> col > 0 ? slotIndex - 1 : -1;
            case RIGHT -> col < MachineFrameworkBlockEntity.GRID_WIDTH - 1 ? slotIndex + 1 : -1;
        };
    }

    /**
     * Checks if the given slot index has an adjacent slot in this direction.
     * Returns false for border positions where no adjacent slot exists.
     */
    public boolean hasAdjacentSlot(int slotIndex) {
        return getAdjacentSlotIndex(slotIndex) >= 0;
    }

    /**
     * Parses a GridDirection from an NBT key string.
     * Returns null if the key doesn't match any grid direction.
     */
    public static GridDirection fromNbtKey(String key) {
        for (GridDirection dir : VALUES) {
            if (dir.nbtKey().equals(key)) {
                return dir;
            }
        }
        return null;
    }
}
