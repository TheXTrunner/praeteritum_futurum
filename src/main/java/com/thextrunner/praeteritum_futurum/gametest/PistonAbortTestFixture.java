package com.thextrunner.praeteritum_futurum.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Development-only seam for exercising piston preflight against a destination
 * treated as unloaded while the GameTest structure itself remains loaded.
 */
public final class PistonAbortTestFixture {
    private static volatile ForcedDestination forcedDestination;
    private static volatile ForcedDestruction forcedDestruction;

    private PistonAbortTestFixture() {
    }

    public static synchronized void forceUnloadedDestination(Level level, BlockPos destination, long lifetimeTicks) {
        if (FMLEnvironment.production) {
            throw new IllegalStateException("Piston abort fixture is only available in development environments");
        }
        forcedDestination = new ForcedDestination(level, destination.immutable(),
                level.getGameTime() + lifetimeTicks, new AtomicInteger());
    }

    public static boolean isForcedUnloaded(Level level, BlockPos pos) {
        ForcedDestination active = forcedDestination;
        if (FMLEnvironment.production || active == null) {
            return false;
        }
        if (active.level() != level || level.getGameTime() > active.expiresAt()) {
            clear(active);
            return false;
        }
        if (!active.destination().equals(pos)) {
            return false;
        }
        active.checks().incrementAndGet();
        return true;
    }

    public static int destinationCheckCount(Level level, BlockPos destination) {
        ForcedDestination active = forcedDestination;
        if (active == null || active.level() != level || !active.destination().equals(destination)) {
            return 0;
        }
        return active.checks().get();
    }

    public static synchronized void forceUnloadedDestructionPosition(
            Level level, BlockPos position, long lifetimeTicks) {
        if (FMLEnvironment.production) {
            throw new IllegalStateException("Piston abort fixture is only available in development environments");
        }
        forcedDestruction = new ForcedDestruction(level, position.immutable(),
                level.getGameTime() + lifetimeTicks, new AtomicInteger());
    }

    public static boolean isForcedUnloadedDestructionPosition(Level level, BlockPos pos) {
        ForcedDestruction active = forcedDestruction;
        if (FMLEnvironment.production || active == null) {
            return false;
        }
        if (active.level() != level || level.getGameTime() > active.expiresAt()) {
            clear(active);
            return false;
        }
        if (!active.position().equals(pos)) {
            return false;
        }
        active.checks().incrementAndGet();
        return true;
    }

    public static int destructionCheckCount(Level level, BlockPos position) {
        ForcedDestruction active = forcedDestruction;
        if (active == null || active.level() != level || !active.position().equals(position)) {
            return 0;
        }
        return active.checks().get();
    }

    public static synchronized void clearDestructionPosition(Level level, BlockPos position) {
        ForcedDestruction active = forcedDestruction;
        if (active != null && active.level() == level && active.position().equals(position)) {
            forcedDestruction = null;
        }
    }

    public static synchronized void clear(Level level, BlockPos destination) {
        ForcedDestination active = forcedDestination;
        if (active != null && active.level() == level && active.destination().equals(destination)) {
            forcedDestination = null;
        }
    }

    private static synchronized void clear(ForcedDestination expected) {
        if (forcedDestination == expected) {
            forcedDestination = null;
        }
    }

    private static synchronized void clear(ForcedDestruction expected) {
        if (forcedDestruction == expected) {
            forcedDestruction = null;
        }
    }

    private record ForcedDestination(Level level, BlockPos destination, long expiresAt, AtomicInteger checks) {
    }

    private record ForcedDestruction(Level level, BlockPos position, long expiresAt, AtomicInteger checks) {
    }
}
