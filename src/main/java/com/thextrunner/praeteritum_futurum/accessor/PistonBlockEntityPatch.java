package com.thextrunner.praeteritum_futurum.accessor;

import net.minecraft.nbt.CompoundTag;

/**
 * Accessor interface applied to {@link net.minecraft.world.level.block.piston.PistonMovingBlockEntity}
 * via mixin, allowing us to store the original block entity during piston movement.
 * Placed outside the mixin package hierarchy to avoid IllegalClassLoadError.
 */
public interface PistonBlockEntityPatch {
    void mte_setHeldBlockEntityData(CompoundTag data);
    CompoundTag mte_getHeldBlockEntityData();
}
