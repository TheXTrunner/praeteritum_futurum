package com.thextrunner.praeteritum_futurum.block;

import net.minecraft.core.Direction;

/**
 * Directions relative to a Machine Framework's facing direction.
 * <p>
 * The machine has a front face determined by how it was placed. RelativeSide maps
 * that facing direction to six relative positions: FRONT, BACK, LEFT, RIGHT, UP, DOWN.
 * This allows the IO module to reference faces relative to the machine's orientation
 * rather than absolute world directions.
 * <p>
 * For example, if a machine faces NORTH:
 * <ul>
 *   <li>FRONT → NORTH</li>
 *   <li>BACK → SOUTH</li>
 *   <li>LEFT → EAST (clockwise from facing)</li>
 *   <li>RIGHT → WEST (counter-clockwise from facing)</li>
 *   <li>UP → UP (always absolute)</li>
 *   <li>DOWN → DOWN (always absolute)</li>
 * </ul>
 * <p>
 * Used by the IO module to determine which world-facing block face corresponds to
 * a given relative side, and vice versa.
 */
public enum RelativeSide {
    /** The face the machine is pointing toward */
    FRONT,
    /** The face opposite the machine's front */
    BACK,
    /** The face to the left of the machine (clockwise from facing) */
    LEFT,
    /** The face to the right of the machine (counter-clockwise from facing) */
    RIGHT,
    /** The ceiling face — always absolute UP */
    UP,
    /** The floor face — always absolute DOWN */
    DOWN;

    /**
     * Converts this relative side to an absolute world direction based on the machine's facing.
     * <p>
     * UP and DOWN are always absolute. FRONT, BACK, LEFT, and RIGHT are computed
     * relative to the machine's facing direction.
     *
     * @param machineFacing The direction the machine is facing (its front)
     * @return The corresponding world Direction
     */
    public Direction toWorldDirection(Direction machineFacing) {
        return switch (this) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case FRONT -> machineFacing;
            case BACK -> machineFacing.getOpposite();
            case LEFT -> machineFacing.getClockWise();
            case RIGHT -> machineFacing.getCounterClockWise();
        };
    }

    /**
     * Converts an absolute world direction to a relative side based on the machine's facing.
     * <p>
     * Used when an IO module detects a connection on a specific world face and needs
     * to know which relative side that corresponds to.
     *
     * @param worldDir      The world Direction to convert
     * @param machineFacing The direction the machine is facing (its front)
     * @return The corresponding RelativeSide (defaults to FRONT if no match)
     */
    public static RelativeSide fromWorldDirection(Direction worldDir, Direction machineFacing) {
        if (worldDir == Direction.UP) return UP;
        if (worldDir == Direction.DOWN) return DOWN;
        if (worldDir == machineFacing) return FRONT;
        if (worldDir == machineFacing.getOpposite()) return BACK;
        if (worldDir == machineFacing.getClockWise()) return LEFT;
        if (worldDir == machineFacing.getCounterClockWise()) return RIGHT;
        return FRONT;
    }
}