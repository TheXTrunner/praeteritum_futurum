package com.thextrunner.praeteritum_futurum.block;

import net.minecraft.core.Direction;

public enum RelativeSide {
    FRONT, BACK, LEFT, RIGHT, UP, DOWN;

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