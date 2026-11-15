package com.thextrunner.praeteritum_futurum.mixin;

import com.mojang.logging.LogUtils;
import com.thextrunner.praeteritum_futurum.accessor.PistonBlockEntityPatch;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;

/**
 * Mixin that patches {@link PistonMovingBlockEntity} to hold a reference to the
 * original block entity during piston movement, and restores it when the animation finishes.
 */
@Mixin(value = PistonMovingBlockEntity.class)
public abstract class PistonBlockEntityMixin extends BlockEntity implements PistonBlockEntityPatch {

    @Unique
    private static final Logger MTE_LOGGER = LogUtils.getLogger();

    @Unique
    private CompoundTag mte_heldBlockEntityData;

    protected PistonBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void mte_setHeldBlockEntityData(CompoundTag data) {
        this.mte_heldBlockEntityData = data == null ? null : data.copy();
    }

    @Override
    public CompoundTag mte_getHeldBlockEntityData() {
        return this.mte_heldBlockEntityData == null ? null : this.mte_heldBlockEntityData.copy();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void mte_saveHeldData(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (this.mte_heldBlockEntityData != null) {
            tag.put("MteHeldBlockEntityData", this.mte_heldBlockEntityData.copy());
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void mte_loadHeldData(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.mte_heldBlockEntityData = tag.contains("MteHeldBlockEntityData", CompoundTag.TAG_COMPOUND)
                ? tag.getCompound("MteHeldBlockEntityData").copy()
                : null;
    }

    @Inject(method = "finalTick()V", at = @At("TAIL"))
    private void mte_restoreHeldData(CallbackInfo ci) {
        mte_restoreHeldData((PistonMovingBlockEntity) (Object) this);
    }

    @Inject(
            method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;)V",
            at = @At("TAIL")
    )
    private static void mte_restoreHeldDataAfterTick(Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity moving, CallbackInfo ci) {
        mte_restoreHeldData(moving);
    }

    @Unique
    private static void mte_restoreHeldData(PistonMovingBlockEntity moving) {
        if (!(moving instanceof PistonBlockEntityPatch patch)) return;
        CompoundTag data = patch.mte_getHeldBlockEntityData();
        Level level = moving.getLevel();
        if (level == null || level.isClientSide) return;
        if (data == null) {
            MTE_LOGGER.warn("[PISTON DEBUG] No held machine data on moving piston at {} (movedState={})", moving.getBlockPos(), moving.getBlockState());
            return;
        }
        BlockEntity target = level.getBlockEntity(moving.getBlockPos());
        if (target instanceof MachineFrameworkBlockEntity machine) {
            target.loadCustomOnly(data, level.registryAccess());
            target.setChanged();
            MTE_LOGGER.info("[PISTON DEBUG] Restored machine at {}: dataKeys={}, modules={}, inventory={}, owner={}, public={}",
                    moving.getBlockPos(), data.getAllKeys(), occupied(machine.getModuleHandler()), occupied(machine.getInventoryHandler()),
                    machine.getOwnerUUID(), machine.isPublic());
        } else {
            MTE_LOGGER.warn("[PISTON DEBUG] Could not restore machine at {}: target={}, targetState={}, dataKeys={}",
                    moving.getBlockPos(), target == null ? "null" : target.getClass().getName(),
                    level.getBlockState(moving.getBlockPos()), data.getAllKeys());
        }
    }

    @Unique
    private static int occupied(net.neoforged.neoforge.items.ItemStackHandler handler) {
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) count++;
        }
        return count;
    }
}
