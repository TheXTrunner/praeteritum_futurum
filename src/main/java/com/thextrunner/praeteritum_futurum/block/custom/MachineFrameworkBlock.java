package com.thextrunner.praeteritum_futurum.block.custom;

import com.mojang.serialization.MapCodec;
import com.thextrunner.praeteritum_futurum.block.entity.ModBlockEntities;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom block class for the Machine Framework — the core block of the mod.
 * <p>
 * Extends BaseEntityBlock to associate the MachineFrameworkBlockEntity with each placed block.
 * Handles all player interactions: right-clicking opens the machine info screen, using a Yool
 * opens the config screen, and using a Blueprint applies a saved configuration.
 * <p>
 * Key behaviors:
 * <ul>
 *   <li>Placement: faces away from the player (like a furnace), records the placing player as owner</li>
 *   <li>Rotation: supports horizontal facing (N/E/S/W) via block state property</li>
 *   <li>Interaction: right-click opens info screen; Yool right-click opens config screen</li>
 *   <li>Destruction: drops all inventory and module items as entity drops</li>
 *   <li>Ticking: server-side only, delegates to MachineFrameworkBlockEntity.serverTick</li>
 * </ul>
 */
public class MachineFrameworkBlock extends BaseEntityBlock {
    /** Codec for NBT serialization of this block type. */
    public static final MapCodec<MachineFrameworkBlock> CODEC = simpleCodec(MachineFrameworkBlock::new);

    /** Horizontal facing direction — determines which way the machine's front faces. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * Constructs the Machine Framework block with the given properties.
     * Sets the default facing to NORTH.
     */
    public MachineFrameworkBlock(Properties properties) {
        super(properties);
        // Default facing is NORTH when unplaced
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    @NotNull
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * Determines the block state when the player places the block.
     * Sets the facing direction away from the player, similar to a furnace or chest.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Sets facing away from the player (like a furnace or chest)
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * Called when the block is placed by a player.
     * Records the placing player's UUID as the machine's owner for access control.
     */
    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && !level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MachineFrameworkBlockEntity machine) {
                machine.setOwnerUUID(player.getUUID().toString());
                machine.setChanged();
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        // Minecraft marks these methods as deprecated because they use internal fallback implementations,
        // but overriding them is still completely required for block rotation and mirroring to function
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Creates a new MachineFrameworkBlockEntity for this block.
     * Called by Minecraft when the block is first placed.
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MachineFrameworkBlockEntity(pos, state);
    }

    @Override
    public boolean isSignalSource(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getSignal(@NotNull BlockState state, @NotNull BlockGetter level,
                         @NotNull BlockPos pos, @NotNull Direction direction) {
        if (level.getBlockEntity(pos) instanceof MachineFrameworkBlockEntity machine) {
            return machine.getRedstoneOutputSignal(direction.getOpposite());
        }
        return 0;
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                @NotNull Block neighborBlock, @NotNull BlockPos neighborPos,
                                boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineFrameworkBlockEntity machine) {
            RedstoneModuleItem.recomputeMachineCircuits(level, pos, machine);
        }
    }

    /**
     * Handles item usage on the block (right-clicking while holding an item).
     * <p>
     * Supported items:
     * <ul>
     *   <li>Blueprint: applies a saved configuration (handled by the item itself)</li>
     *   <li>Yool: opens the machine config screen (for admin/owner access)</li>
     *   <li>Other: falls through to open the info screen</li>
     * </ul>
     */
    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (stack.is(ModItems.BLUEPRINT.get())) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(ModItems.YOOL.get())) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof MachineFrameworkBlockEntity frameworkEntity) {
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    serverPlayer.openMenu(frameworkEntity, buf -> buf.writeBlockPos(pos));
                }
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineFrameworkBlockEntity frameworkEntity) {
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
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * Handles right-clicking the block without holding any item.
     * Opens the machine info screen showing the current state and modules.
     */
    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineFrameworkBlockEntity frameworkEntity) {
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

    /**
     * Called when the block is broken or replaced.
     * Drops all items in the machine's inventory and module slots as entity drops.
     * Skips dropping when moved by a piston (vanilla handles that case).
     */
    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // Don't drop items when moved by piston — vanilla handles the drop.
            if (!movedByPiston) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof MachineFrameworkBlockEntity frameworkEntity) {
                    // Run module cleanup actions (e.g., stop playing sounds) before dropping items
                    frameworkEntity.runAllModuleCleanups();

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
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Returns the server-side ticker for this block entity.
     * Client-side ticking is not needed — all logic runs on the server.
     */
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

    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos,
                     @NotNull RandomSource random) {
        RedstoneModuleItem.tickScheduledMachineCircuits(level, pos);
    }
}