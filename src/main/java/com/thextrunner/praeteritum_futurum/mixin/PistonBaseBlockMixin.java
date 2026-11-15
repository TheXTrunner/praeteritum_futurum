package com.thextrunner.praeteritum_futurum.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.accessor.PistonBlockEntityPatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mixin that allows {@link MachineFrameworkBlock} to be moved by pistons,
 * preserving the block entity data during movement.
 * <p>
 * Only MachineFrameworkBlock is affected — other block entities behave normally.
 * For non-MachineFramework operations, we let vanilla handle everything.
 */
@Mixin(value = PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin extends DirectionalBlock {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    @Final
    private boolean isSticky;

    protected PistonBaseBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * Overrides isPushable to allow block entities to be pushed by pistons.
     * By injecting at TAIL and setting the return value, we override the vanilla
     * check that refuses to push blocks with block entities.
     */
    @Inject(
            method = {"isPushable(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;ZLnet/minecraft/core/Direction;)Z"},
            at = {@At(value = "TAIL")},
            cancellable = true
    )
    private static void mte_allowPushMachineFramework(
            BlockState state, Level world, BlockPos pos, Direction direction,
            boolean canBreak, Direction pistonDir,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Only allow pushing MachineFrameworkBlock — vanilla refuses blocks with block entities
        if (state.getBlock() instanceof MachineFrameworkBlock) {
            LOGGER.info("[PISTON DEBUG] isPushable: allowing MachineFrameworkBlock at {}", pos);
            cir.setReturnValue(true);
        }
    }

    /**
     * Intercepts moveBlocks to check if MachineFrameworkBlock is involved.
     * If not, lets vanilla handle it normally. If yes, takes over to preserve block entity data.
     */
    @Inject(
            method = {"moveBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Z"},
            at = {@At(value = "HEAD")},
            cancellable = true
    )
    private void mte_checkAndOverwriteMove(
            Level world, BlockPos pos, Direction dir, boolean extending,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LOGGER.info("[PISTON DEBUG] moveBlocks called: pos={}, dir={}, extending={}, isSticky={}", pos, dir, extending, isSticky);

        PistonStructureResolver pistonHandler = new PistonStructureResolver(world, pos, dir, extending);
        boolean resolved = pistonHandler.resolve();
        List<BlockPos> list = resolved ? new ArrayList<>(pistonHandler.getToPush()) : new ArrayList<>();
        // Sticky retraction considers the block two spaces ahead separately
        // from the resolver's ordinary push list. Include a machine there so
        // its block entity data follows the pulled block.
        BlockPos stickyPullPos = pos.relative(dir, 2);
        boolean hasStickyPulledMachine = !extending && isSticky
                && world.getBlockState(stickyPullPos).getBlock() instanceof MachineFrameworkBlock;
        if (hasStickyPulledMachine
                && !list.contains(stickyPullPos)) {
            list.add(stickyPullPos);
            LOGGER.info("[PISTON DEBUG] Added sticky-pulled MachineFrameworkBlock at {}", stickyPullPos);
        }
        if (!extending && isSticky) {
            LOGGER.info("[PISTON DEBUG] Sticky retract at {}: candidate={}, candidateState={}, resolverPositions={}, finalPositions={}",
                    pos, stickyPullPos, world.getBlockState(stickyPullPos), pistonHandler.getToPush(), list);
        }
        if (!resolved && !hasStickyPulledMachine) {
            LOGGER.info("[PISTON DEBUG] Resolver rejected movement at {}; no sticky-pulled machine candidate", pos);
            return;
        }
        boolean hasMachineFramework = list.stream()
                .map(world::getBlockState)
                .anyMatch(state -> state.getBlock() instanceof MachineFrameworkBlock);
        if (!hasMachineFramework) {
            return;
        }

        LOGGER.info("[PISTON DEBUG] MachineFrameworkBlock detected, taking over moveBlocks");
        LOGGER.info("[PISTON DEBUG] resolve() found {} blocks to push", list.size());

        // Vanilla removes the existing head before resolving a retraction.
        BlockPos blockPos = pos.relative(dir);
        if (!extending && world.getBlockState(blockPos).is(Blocks.PISTON_HEAD)) {
            LOGGER.info("[PISTON DEBUG] Removing piston head at {}", blockPos);
            world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 20);
        }

        int k;
        BlockPos blockPos5;
        BlockPos blockPos3;
        int j;

        // Store block states before moving
        HashMap<BlockPos, BlockState> map = Maps.newHashMap();
        ArrayList<BlockState> list2 = Lists.newArrayList();

        for (BlockPos blockPos2 : list) {
            BlockState blockState = world.getBlockState(blockPos2);
            list2.add(blockState);
            map.put(blockPos2, blockState);
        }

        List<BlockPos> list3 = resolved ? pistonHandler.getToDestroy() : List.of();
        BlockState[] blockStates = new BlockState[list.size() + list3.size()];
        int i = 0;

        // Destroy blocks that need to be destroyed (exceeds push limit)
        for (int j2 = list3.size() - 1; j2 >= 0; --j2) {
            BlockPos blockPos32 = list3.get(j2);
            BlockState blockState22 = world.getBlockState(blockPos32);
            BlockEntity blockEntity = blockState22.hasBlockEntity() ? world.getBlockEntity(blockPos32) : null;
            mte_dropResources(world, blockState22, blockPos32, blockEntity);
            world.setBlock(blockPos32, Blocks.AIR.defaultBlockState(), 18);
            world.gameEvent(GameEvent.BLOCK_DESTROY, blockPos32, GameEvent.Context.of(blockState22));
            if (!blockState22.is(BlockTags.FIRE)) {
                world.addDestroyBlockEffect(blockPos32, blockState22);
            }
            blockStates[i++] = blockState22;
        }

        // Capture only MachineFrameworkBlockEntity before removing it
        HashMap<Integer, CompoundTag> heldBlockEntities = Maps.newHashMap();
        for (j = 0; j < list.size(); ++j) {
            blockPos3 = list.get(j);
            BlockEntity blockEntity = list2.get(j).hasBlockEntity() ? world.getBlockEntity(blockPos3) : null;
            if (blockEntity instanceof MachineFrameworkBlockEntity) {
                CompoundTag heldData = blockEntity.saveCustomOnly(world.registryAccess());
                heldBlockEntities.put(j, heldData.copy());
                blockEntity.setChanged();
                world.removeBlockEntity(blockPos3);
                LOGGER.info("[PISTON DEBUG] Captured MachineFrameworkBlockEntity at index {} pos {}", j, blockPos3);
            }
        }

        // Vanilla's resolver direction is the piston facing when extending and
        // the opposite direction when retracting.
        for (j = list.size() - 1; j >= 0; --j) {
            blockPos3 = list.get(j);
            BlockState blockState = world.getBlockState(blockPos3);
            blockPos3 = blockPos3.relative(extending ? dir : dir.getOpposite());
            map.remove(blockPos3);
            BlockState blockState3 = Blocks.MOVING_PISTON.defaultBlockState()
                    .setValue(FACING, dir);
            world.setBlock(blockPos3, blockState3, 68);

            PistonMovingBlockEntity pushedBlocks = new PistonMovingBlockEntity(
                    blockPos3, blockState3, list2.get(j), dir, extending, false);

            if (!heldBlockEntities.isEmpty() && heldBlockEntities.containsKey(j)
                    && pushedBlocks instanceof PistonBlockEntityPatch) {
                ((PistonBlockEntityPatch) pushedBlocks).mte_setHeldBlockEntityData(heldBlockEntities.get(j));
                LOGGER.info("[PISTON DEBUG] Attached held block entity to PistonMovingBlockEntity at {}", blockPos3);
            }

            world.setBlockEntity(pushedBlocks);
            world.blockUpdated(blockPos3, blockState3.getBlock());
            blockState3.updateNeighbourShapes(world, blockPos3, 2);
            blockStates[i++] = blockState;
        }

        // Vanilla creates a moving head only while extending. During retraction
        // the head is removed before resolution and the piston base stays at pos.
        if (extending) {
            LOGGER.info("[PISTON DEBUG] Handling extension - piston head moving out");
            PistonType pistonType = this.isSticky ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState pistonHeadState = Blocks.PISTON_HEAD.defaultBlockState()
                    .setValue(PistonHeadBlock.FACING, dir)
                    .setValue(PistonHeadBlock.TYPE, pistonType);
            BlockState movingPistonState = Blocks.MOVING_PISTON.defaultBlockState()
                    .setValue(MovingPistonBlock.FACING, dir)
                    .setValue(MovingPistonBlock.TYPE, pistonType);
            BlockPos armPos = pos.relative(dir);
            map.remove(armPos);
            world.setBlock(armPos, movingPistonState, 68);
            world.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(armPos, movingPistonState, pistonHeadState, dir, true, true));
            world.blockUpdated(armPos, movingPistonState.getBlock());
            movingPistonState.updateNeighbourShapes(world, armPos, 2);
        }

        // Clear old positions
        BlockState blockState5 = Blocks.AIR.defaultBlockState();
        for (BlockPos blockPos2 : map.keySet()) {
            world.setBlock(blockPos2, blockState5, 82);
        }

        // Update shapes
        for (Map.Entry<BlockPos, BlockState> entry : map.entrySet()) {
            blockPos5 = entry.getKey();
            BlockState blockState6 = entry.getValue();
            blockState6.updateIndirectNeighbourShapes(world, blockPos5, 2);
            blockState5.updateNeighbourShapes(world, blockPos5, 2);
            blockState5.updateIndirectNeighbourShapes(world, blockPos5, 2);
        }

        // Update neighbors for destroyed blocks
        i = 0;
        for (k = list3.size() - 1; k >= 0; --k) {
            BlockState blockState = blockStates[i++];
            blockPos5 = list3.get(k);
            blockState.updateIndirectNeighbourShapes(world, blockPos5, 2);
            world.updateNeighborsAt(blockPos5, blockState.getBlock());
        }

        // Update neighbors for moved blocks
        for (k = list.size() - 1; k >= 0; --k) {
            world.updateNeighborsAt(list.get(k), blockStates[i++].getBlock());
        }

        if (extending) {
            world.updateNeighborsAt(blockPos, Blocks.PISTON_HEAD);
        }

        LOGGER.info("[PISTON DEBUG] moveBlocks completed successfully");
        cir.setReturnValue(true);
    }

    /**
     * Helper to drop block resources when a block is destroyed during piston movement.
     * Uses the vanilla Block.popResource to drop the block item.
     */
    @Unique
    private static void mte_dropResources(Level world, BlockState state, BlockPos pos, @org.jetbrains.annotations.Nullable BlockEntity blockEntity) {
        Block.popResource(world, pos, state.getBlock().getCloneItemStack(world, pos, state));
    }
}
