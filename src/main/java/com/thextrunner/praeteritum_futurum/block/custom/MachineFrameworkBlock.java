package com.thextrunner.praeteritum_futurum.block.custom;

import com.mojang.serialization.MapCodec;
import com.thextrunner.praeteritum_futurum.ModBlockEntities;
import com.thextrunner.praeteritum_futurum.ModItems;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MachineFrameworkBlock extends BaseEntityBlock {
    public static final MapCodec<MachineFrameworkBlock> CODEC = simpleCodec(MachineFrameworkBlock::new);

    public MachineFrameworkBlock(Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MachineFrameworkBlockEntity(pos, state);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineFrameworkBlockEntity frameworkEntity) {
                // If holding the Yool tool, open the Module Config Menu
                if (stack.is(ModItems.YOOL.get())) {
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    serverPlayer.openMenu(frameworkEntity, buf -> buf.writeBlockPos(pos));
                    return ItemInteractionResult.SUCCESS;
                }

                // Otherwise, open the Machine Info Menu (Machine Mode UI)
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public net.minecraft.network.chat.@NotNull Component getDisplayName() {
                        return frameworkEntity.getDisplayName();
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
                        return frameworkEntity.createInfoMenu(containerId, playerInventory, player);
                    }
                }, buf -> buf.writeBlockPos(pos));
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineFrameworkBlockEntity frameworkEntity) {
                // Empty hand right-click opens the Machine Info Menu (Machine Mode UI)
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public @NotNull Component getDisplayName() {
                        return frameworkEntity.getDisplayName();
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
                        return frameworkEntity.createInfoMenu(containerId, playerInventory, player);
                    }
                }, buf -> buf.writeBlockPos(pos));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineFrameworkBlockEntity frameworkEntity) {
                net.neoforged.neoforge.items.IItemHandler inventoryHandler = frameworkEntity.getInventoryHandler();
                for (int i = 0; i < inventoryHandler.getSlots(); i++) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventoryHandler.getStackInSlot(i));
                }

                net.neoforged.neoforge.items.IItemHandler moduleHandler = frameworkEntity.getModuleHandler();
                for (int i = 0; i < moduleHandler.getSlots(); i++) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), moduleHandler.getStackInSlot(i));
                }

                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            @NotNull net.minecraft.world.level.Level level,
            @NotNull BlockState state,
            @NotNull net.minecraft.world.level.block.entity.BlockEntityType<T> blockEntityType
    ) {
        return level.isClientSide() ? null : BaseEntityBlock.createTickerHelper(
                blockEntityType,
                ModBlockEntities.MACHINE_FRAMEWORK_BE.get(),
                MachineFrameworkBlockEntity::serverTick
        );
    }
}