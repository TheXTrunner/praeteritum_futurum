package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMachineContainerMenu extends AbstractContainerMenu {
    protected final BlockPos blockPos;

    protected BaseMachineContainerMenu(@Nullable MenuType<?> menuType, int containerId, BlockPos blockPos) {
        super(menuType, containerId);
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (blockPos != null && player.level().getBlockEntity(blockPos) instanceof MachineFrameworkBlockEntity) {
            return player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 64.0D;
        }
        return false;
    }
}