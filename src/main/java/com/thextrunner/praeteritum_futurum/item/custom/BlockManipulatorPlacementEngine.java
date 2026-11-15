package com.thextrunner.praeteritum_futurum.item.custom;

import com.mojang.logging.LogUtils;
import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all block placement logic for the Block Manipulator Module.
 * <p>
 * This engine contains the smart placement algorithm that tries different orientations
 * based on block type and IO facing, as well as special-case handlers for block types
 * that need custom placement logic (doors, beds, scaffolding, candles, kelp, etc.).
 * <p>
 * Designed to be extensible: adding support for a new block type requires adding a new
 * {@code tryPlaceX} method and a dispatch call in {@link #placeBlockFromSlot}.
 */
public class BlockManipulatorPlacementEngine {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Places a block from a specific inventory slot into the world in the given world direction.
     * Acts like a player placing blocks: tries wall mounting first (based on IO facing),
     * then floor placement, then ceiling placement. If all fail, drops the item as an entity.
     * Restores block entity data (NBT) if the stored item contains it.
     */
    public static void placeBlockFromSlot(
            Level level,
            BlockPos machinePos,
            MachineFrameworkBlockEntity blockEntity,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        BlockPos targetPos = machinePos.relative(worldDirection);

        ItemStack stack = itemHandler.getStackInSlot(invSlot);
        if (stack.isEmpty()) {
            return;
        }

        // Check if the item is a block item
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        Block block = blockItem.getBlock();
        if (block == Blocks.AIR) {
            return;
        }

        // Extract block state and block entity data from the item's CustomData component, if present
        CompoundTag blockEntityNbt = null;
        CompoundTag blockStateNbt = null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag itemNbt = customData.copyTag();
            if (itemNbt.contains("BlockEntityTag")) {
                blockEntityNbt = itemNbt.getCompound("BlockEntityTag");
            }
            if (itemNbt.contains("BlockState")) {
                blockStateNbt = itemNbt.getCompound("BlockState");
            }
        }

        // Also read the CONTAINER data component (Minecraft 1.21+ vanilla inventory storage)
        // and merge it into blockEntityNbt so that handleUpdateTag can restore the inventory.
        // This is needed because vanilla stores container contents in CONTAINER, not BlockEntityTag.
        ItemContainerContents containerContents = stack.get(DataComponents.CONTAINER);
        if (containerContents != null) {
            ListTag itemsList = new ListTag();
            int slotCount = containerContents.getSlots();
            for (int i = 0; i < slotCount; i++) {
                ItemStack slotStack = containerContents.getStackInSlot(i);
                if (!slotStack.isEmpty()) {
                    // In Minecraft 1.21+, use save(RegistryAccess) which returns a complete CompoundTag
                    // with id, count, and components in the format expected by ItemStack.parseOptional().
                    // Then add the Slot field for container block entity compatibility.
                    CompoundTag slotNbt = (CompoundTag) slotStack.save(level.registryAccess());
                    slotNbt.putByte("Slot", (byte) i);
                    itemsList.add(slotNbt);
                }
            }
            if (!itemsList.isEmpty()) {
                if (blockEntityNbt == null) {
                    blockEntityNbt = new CompoundTag();
                }
                blockEntityNbt.put("Items", itemsList);
                // Try to detect the slot count from the block type
                if (block instanceof net.minecraft.world.level.block.ChestBlock) {
                    blockEntityNbt.putInt("Size", 27);
                } else if (block instanceof net.minecraft.world.level.block.BarrelBlock) {
                    blockEntityNbt.putInt("Size", 27);
                } else if (block instanceof net.minecraft.world.level.block.FurnaceBlock) {
                    blockEntityNbt.putInt("Size", 3);
                } else if (block == Blocks.CHISELED_BOOKSHELF) {
                    blockEntityNbt.putInt("Size", 6);
                } else if (block instanceof net.minecraft.world.level.block.HopperBlock) {
                    blockEntityNbt.putInt("Size", 5);
                } else if (block instanceof net.minecraft.world.level.block.DropperBlock) {
                    blockEntityNbt.putInt("Size", 27);
                } else if (block instanceof net.minecraft.world.level.block.DispenserBlock) {
                    blockEntityNbt.putInt("Size", 9);
                }
            }
        }

        // Smart placement: try different orientations based on block type and IO facing
        // The worldDirection is the direction FROM the machine TO the target position
        // The opposite direction points FROM the target back TO the machine (the "wall" side)
        Direction attachDirection = worldDirection.getOpposite(); // Direction from target back to machine

        // Extract machine facing for horizontal-only blocks during vertical placement
        Direction machineFacing = blockEntity.getBlockState().hasProperty(MachineFrameworkBlock.FACING)
                ? blockEntity.getBlockState().getValue(MachineFrameworkBlock.FACING)
                : Direction.NORTH;

        // Scaffolding: special case — allow even if target is not air (stacking on existing scaffolding)
        if (block == Blocks.SCAFFOLDING) {
            if (tryPlaceScaffolding(level, targetPos, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection, machineFacing)) {
                return;
            }
        }

        // Double-height blocks (doors, tall plants, end rods, etc.): special case — check for solid support below and 2 air blocks
        boolean isDoubleHeightBlock = block instanceof net.minecraft.world.level.block.DoorBlock;
        if (!isDoubleHeightBlock) {
            BlockState defaultState = block.defaultBlockState();
            if (defaultState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                isDoubleHeightBlock = true;
            }
        }
        if (isDoubleHeightBlock) {
            if (tryPlaceDoubleBlock(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection, machineFacing)) {
                return;
            }
        }

        // Beds: special case — check for floor support and head attachment
        // Beds MUST go through tryPlaceBed — if it fails, reject entirely (no fallthrough to normal placement)
        if (block instanceof BedBlock) {
            boolean bedPlaced = tryPlaceBed(level, machinePos, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection, machineFacing);
            if (bedPlaced) {
                return;
            }
            // Bed placement failed — reject entirely, don't fall through to normal placement
            return;
        }

        // Candles: special case — allow stacking on existing candles at the target position
        if (block instanceof CandleBlock) {
            if (tryPlaceCandle(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection)) {
                return;
            }
        }

        // Sea Pickles: special case — allow stacking on existing sea pickles at the target position
        if (block instanceof net.minecraft.world.level.block.SeaPickleBlock) {
            if (tryPlaceSeaPickle(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection)) {
                return;
            }
        }

        // Snow Layers: special case — allow stacking on existing snow at the target position
        if (block instanceof net.minecraft.world.level.block.SnowLayerBlock) {
            if (tryPlaceSnowLayer(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection)) {
                return;
            }
        }

        // Pink Petals: special case — allow stacking on existing pink petals at the target position
        if (block instanceof net.minecraft.world.level.block.PinkPetalsBlock) {
            if (tryPlacePinkPetals(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection)) {
                return;
            }
        }

        // Pointed Dripstone: special case — allow stacking on existing dripstone or hanging from ceiling
        if (block instanceof net.minecraft.world.level.block.PointedDripstoneBlock) {
            if (tryPlaceDripstone(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection)) {
                return;
            }
        }

        // Turtle Eggs: special case — allow stacking on existing turtle eggs at the target position
        if (block instanceof net.minecraft.world.level.block.TurtleEggBlock) {
            if (tryPlaceTurtleEgg(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection)) {
                return;
            }
        }

        // Amethyst buds/clusters: special case — try all 6 directions, machine face last
        if (block instanceof net.minecraft.world.level.block.AmethystClusterBlock) {
            if (tryPlaceAmethyst(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection, attachDirection)) {
                return;
            }
        }

        // Glow Lichen: special case — try all 6 directions, machine face never tried
        if (block instanceof net.minecraft.world.level.block.GlowLichenBlock) {
            if (tryPlaceGlowLichen(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection, attachDirection)) {
                return;
            }
            // Glow lichen placement failed — reject entirely, don't fall through to normal placement
            return;
        }

        // Vines: special case — try all 6 directions, machine face never tried
        if (block instanceof net.minecraft.world.level.block.VineBlock) {
            if (tryPlaceVines(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection, attachDirection)) {
                return;
            }
            // Vines placement failed — reject entirely, don't fall through to normal placement
            return;
        }

        // Bell: special case — specific priority order for attachment
        if (block instanceof net.minecraft.world.level.block.BellBlock) {
            if (tryPlaceBell(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection, attachDirection, machineFacing)) {
                return;
            }
            // Bell placement failed — reject entirely, don't fall through to normal placement
            return;
        }

        // Sculk Vein: special case — try all 6 directions, machine face never tried
        if (block instanceof net.minecraft.world.level.block.SculkVeinBlock) {
            if (tryPlaceSculkVein(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection, attachDirection)) {
                return;
            }
            // Sculk vein placement failed — reject entirely, don't fall through to normal placement
            return;
        }

        // Kelp: special case — grows upward in water, can stack on existing kelp
        if (block instanceof net.minecraft.world.level.block.KelpBlock) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR] [KELP] Detected kelp at target={}, worldDirection={}", targetPos, worldDirection);
            }
            if (tryPlaceKelp(level, targetPos, block, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection)) {
                return;
            }
            // Kelp placement failed — reject entirely, don't fall through to normal placement
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.warn("[BLOCK MANIPULATOR] [KELP] Placement failed for target={}, worldDirection={}", targetPos, worldDirection);
            }
            return;
        }

        // Check if the target position is valid for placing this block (air or waterloggable water)
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return;
        }

        // Try placement with smart orientation logic
        boolean placed = trySmartPlacement(level, targetPos, block, blockStateNbt, blockEntityNbt,
                itemHandler, invSlot, worldDirection, attachDirection, machineFacing);

        if (!placed && Config.isAllowFloatingBlocks()) {
            // If smart placement failed and floating blocks are allowed, try to place with stored state
            BlockState placeState = block.defaultBlockState();
            if (blockStateNbt != null) {
                try {
                    placeState = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), blockStateNbt);
                } catch (Exception e) {
                    // Fall back to default
                }
            }
            placed = placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection);
        }

        if (!placed && Config.isDebugLoggingEnabled()) {
            LOGGER.warn("[BLOCK MANIPULATOR]   All placement attempts failed for {}", block.getDescriptionId());
        }
    }

    // ========================================================================
    // Smart Placement Core
    // ========================================================================

    /**
     * Tries to place a block using player-like logic.
     * Uses the IO facing direction to determine how to orient the block:
     * - For wall-mounted blocks: try to attach to the side facing the machine first, then other walls
     * - For floor/ceiling blocks: try floor (block below) first, then ceiling
     * - For directional blocks (ladders, lanterns, trapdoors, etc.): orient based on IO facing
     * - For regular full blocks: place with default state
     * - For torch/sign blocks: try wall placement first (converting to wall variant), then floor/ceiling
     * <p>
     * The worldDirection is the direction FROM the machine TO the target position.
     * The attachDirection (opposite) points FROM the target back TO the machine — this is the "wall" side.
     *
     * @return true if the block was placed successfully
     */
    private static boolean trySmartPlacement(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection,
            Direction attachDirection,
            Direction machineFacing
    ) {
        // Determine block type category
        boolean isWallOnly = block instanceof net.minecraft.world.level.block.WallTorchBlock ||
                block instanceof net.minecraft.world.level.block.LadderBlock ||
                block instanceof net.minecraft.world.level.block.WallSignBlock ||
                block instanceof net.minecraft.world.level.block.WallHangingSignBlock ||
                block instanceof net.minecraft.world.level.block.WallBannerBlock ||
                block instanceof net.minecraft.world.level.block.WallSkullBlock ||
                block == Blocks.TRIPWIRE_HOOK;

        boolean isFaceAttached = block instanceof net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;

        boolean isFloorCeilingMounted = block instanceof net.minecraft.world.level.block.TorchBlock ||
                block instanceof net.minecraft.world.level.block.CeilingHangingSignBlock ||
                block instanceof net.minecraft.world.level.block.SignBlock ||
                block instanceof net.minecraft.world.level.block.BannerBlock ||
                block instanceof net.minecraft.world.level.block.SkullBlock ||
                block instanceof net.minecraft.world.level.block.LanternBlock ||
                block instanceof net.minecraft.world.level.block.TrapDoorBlock;

        // Check if block has a wall variant (TorchBlock → WallTorchBlock, HangingSign → WallHangingSign, etc.)
        Block wallVariant = getWallVariant(block);

// Beds are now handled by tryPlaceBed before reaching trySmartPlacement — no rejection needed here

        // For blocks with wall variants (torches, signs, banners, skulls):
        // Try all valid horizontal walls first (opposite wall, then side walls — never the machine).
        // For hanging signs, always try wall placement regardless of worldDirection (even vertical),
        // because hanging signs can be wall-mounted and players expect wall-first behavior.
        boolean isHangingSign = block instanceof net.minecraft.world.level.block.CeilingHangingSignBlock;
        // The direction from the target back to the machine — the machine's own wall must never be used as a placement wall
        Direction machineWallDir = attachDirection;
        // For hanging signs, only attempt wall placement if there's an actual adjacent wall block (not the machine)
        // that can support the sign. If no such wall exists, skip wall placement and fall through to ceiling variant.
        boolean hasWallForHangingSign = false;
        if (isHangingSign) {
            // Check ALL 4 horizontal directions for adjacent wall blocks, not just getWallPlacementDirections
            // (which returns empty for vertical worldDirection)
            for (Direction wallDir : Direction.Plane.HORIZONTAL) {
                if (wallDir == machineWallDir) continue;
                BlockPos wallPos = targetPos.relative(wallDir);
                if (level.getBlockState(wallPos).isSolidRender(level, wallPos)) {
                    hasWallForHangingSign = true;
                    break;
                }
            }
        }
        // For hanging signs, only enter wall placement if an actual adjacent horizontal wall exists
        // For other wall-variant blocks, enter wall placement when worldDirection is horizontal
        if (wallVariant != null && ((isHangingSign && hasWallForHangingSign) || (!isHangingSign && worldDirection.getAxis().isHorizontal()))) {
            for (Direction wallDir : getWallPlacementDirections(worldDirection)) {
                if (isHangingSign) {
                    // For hanging signs, try perpendicular facings (clockwise and counter-clockwise of the wall)
                    // This matches vanilla behavior where wall-mounted hanging signs face perpendicular to the wall
                    Direction[] perpendicularFacings = {wallDir.getClockWise(), wallDir.getCounterClockWise()};
                    for (Direction perpFacing : perpendicularFacings) {
                        // Skip if the perpendicular facing would attach to the machine's own wall
                        if (perpFacing == machineWallDir) {
                            continue;
                        }
                        BlockState wallState = createPlacementState(wallVariant, blockStateNbt, perpFacing, machineFacing, level, targetPos);
                        if (canBlockSurvive(wallVariant, wallState, level, targetPos)) {
                            if (placeBlockAt(level, targetPos, wallState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                                return true;
                            }
                        }
                    }
                } else {
                    BlockState wallState = createPlacementState(wallVariant, blockStateNbt, wallDir, machineFacing, level, targetPos);
                    if (canBlockSurvive(wallVariant, wallState, level, targetPos)) {
                        if (placeBlockAt(level, targetPos, wallState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                            return true;
                        }
                    }
                }
            }
        }

        // Build placement attempts in priority order
        List<Direction> attemptDirections = new ArrayList<>();

        if (isWallOnly) {
            // Can only attach to walls: try opposite wall first, then side walls (never the machine)
            attemptDirections.addAll(getWallPlacementDirections(worldDirection));
        } else if (isFaceAttached) {
            // Face-attached (buttons, levers, bells): try opposite wall first, then side walls (never machine), then floor, then ceiling
            attemptDirections.addAll(getWallPlacementDirections(worldDirection));
            attemptDirections.add(Direction.DOWN);
            attemptDirections.add(Direction.UP);
        } else if (isFloorCeilingMounted) {
            // Floor/ceiling blocks: try floor (DOWN) first, then ceiling (UP)
            // Exception: hanging signs only attach to ceilings, never floors (player-placed behavior)
            if (isHangingSign) {
                attemptDirections.add(Direction.UP);
            } else {
                attemptDirections.add(Direction.DOWN);
                attemptDirections.add(Direction.UP);
            }
        } else {
            // Regular blocks (chains, logs, pillars, dispensers, observers, beds, etc.): align with worldDirection
            attemptDirections.add(worldDirection);
        }

        // Try each placement attempt
        for (Direction attemptDir : attemptDirections) {
            BlockState placeState = createPlacementState(block, blockStateNbt, attemptDir, machineFacing, level, targetPos);

            // If the block is a double-height block (doors, tall plants), verify space above is air or valid
            if (placeState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                BlockPos abovePos = targetPos.above();
                if (abovePos.getY() >= level.getMaxBuildHeight() || !isPositionValidForPlacement(level, abovePos, block)) {
                    continue;
                }
            }

            // If the block is a bed, verify head position is clear and valid
            if (placeState.hasProperty(BlockStateProperties.BED_PART)) {
                if (!worldDirection.getAxis().isHorizontal()) {
                    continue;
                }
                BlockPos headPos = targetPos.relative(worldDirection);
                if (headPos.getY() >= level.getMaxBuildHeight() || headPos.getY() < level.getMinBuildHeight()
                        || !isPositionValidForPlacement(level, headPos, block)) {
                    continue;
                }
            }

            // Check if the block can survive at this position
            boolean canSurvive = canBlockSurvive(block, placeState, level, targetPos);
            if (!canSurvive) {
                continue;
            }

            // Try to place the block
            if (placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                return true;
            }
        }

        return false;
    }

    // ========================================================================
    // Special-Case Placement Methods
    // ========================================================================

    /**
     * Handles double-height block placement with vanilla-like support checking.
     * Works for any 2-block-tall block: doors (DoorBlock) and generic double blocks (DOUBLE_BLOCK_HALF property).
     * Checks for a solid supporting block below the bottom position and 2 air blocks (bottom + top).
     * Places both halves with proper state properties.
     * Works for all placement directions (UP, DOWN, horizontal).
     */
    private static boolean tryPlaceDoubleBlock(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection,
            Direction machineFacing
    ) {
        boolean isDoor = block instanceof net.minecraft.world.level.block.DoorBlock;

        // Determine the position of the bottom half
        // If placing DOWN (from ceiling), the bottom (LOWER half) is one block below target,
        // and the top (UPPER half) is at the target position
        // If placing UP (from floor) or horizontally, the bottom is the target position
        BlockPos bottomPos;
        if (worldDirection == Direction.DOWN) {
            bottomPos = targetPos.below();
        } else {
            bottomPos = targetPos;
        }

        // Check build height bounds for both halves
        BlockPos topPos = bottomPos.above();
        if (bottomPos.getY() < level.getMinBuildHeight() || topPos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }

        // Check for solid supporting block below the bottom position
        BlockPos supportPos = bottomPos.below();
        BlockState supportState = level.getBlockState(supportPos);
        if (!supportState.isSolidRender(level, supportPos) || supportState.getBlock() == Blocks.AIR) {
            return false;
        }
        // Check that both positions are air (or waterloggable water)
        if (!isPositionValidForPlacement(level, bottomPos, block)) {
            return false;
        }
        if (!isPositionValidForPlacement(level, topPos, block)) {
            return false;
        }
        // Get the default block state
        BlockState bottomState = block.defaultBlockState();

        // Apply stored NBT state if present
        if (blockStateNbt != null) {
            try {
                bottomState = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), blockStateNbt);
            } catch (Exception e) {
                // Fall back to default
            }
        }

        // Determine FACING direction for blocks that need it (doors, etc.)
        Direction facing;
        if (worldDirection.getAxis().isHorizontal()) {
            facing = worldDirection;
        } else {
            facing = machineFacing != null && machineFacing.getAxis().isHorizontal() ? machineFacing : Direction.NORTH;
        }

        // Set properties based on block type
        if (isDoor) {
            // Door-specific properties - use DoorBlock.FACING (horizontal only)
            bottomState = bottomState
                    .setValue(DoorBlock.FACING, facing)
                    .setValue(DoorBlock.HINGE, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT)
                    .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                    .setValue(BlockStateProperties.OPEN, false)
                    .setValue(BlockStateProperties.POWERED, false);
        } else {
            // Generic double block (tall plants, end rods, etc.)
            if (bottomState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                bottomState = bottomState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
            }
            // Set FACING if the block has it
            if (bottomState.hasProperty(BlockStateProperties.FACING)) {
                bottomState = bottomState.setValue(BlockStateProperties.FACING, facing);
            }
        }

        // Apply WATERLOGGED if applicable
        if (bottomState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(bottomPos);
            bottomState = bottomState.setValue(BlockStateProperties.WATERLOGGED, fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        // Place the bottom half
        if (!level.setBlock(bottomPos, bottomState, 3)) {
            return false;
        }

        // Create the top half state
        BlockState topState;
        if (isDoor) {
            topState = bottomState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        } else {
            topState = bottomState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        }

        // Apply WATERLOGGED for top half
        if (topState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidAbove = level.getFluidState(topPos);
            topState = topState.setValue(BlockStateProperties.WATERLOGGED, fluidAbove.is(Fluids.WATER) && fluidAbove.isSource());
        }

        // Place the top half
        level.setBlock(topPos, topState, 3);
        // Restore block entity data if present (bottom half has the block entity)
        if (blockEntityNbt != null && bottomState.hasBlockEntity()) {
            BlockEntity newBlockEntity = level.getBlockEntity(bottomPos);
            if (newBlockEntity != null) {
                newBlockEntity.handleUpdateTag(blockEntityNbt, level.registryAccess());
                newBlockEntity.setChanged();
            }
        }

        // Consume one block from inventory
        itemHandler.extractItem(invSlot, 1, false);

        // Play the block place sound
        net.minecraft.world.level.block.SoundType soundType = bottomState.getSoundType();
        net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
        if (placeSound != null) {
            level.playSound(null, bottomPos, placeSound,
                    net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }

        return true;
    }

    /**
     * Handles bed placement with vanilla-like support checking.
     * Beds are 2-block-wide blocks with a head part and a foot part.
     *
     * Horizontal placement: Both parts are placed side by side on the floor.
     * Requires a solid block below both parts (floor support).
     *
     * DOWN placement (from ceiling): Places the bed below the machine.
     * The head part attaches to the block behind the machine (if it exists),
     * otherwise the bed faces the machine's facing direction.
     *
     * UP placement: Places the bed above, with the head part at the target
     * and the foot part extending in the machine's facing direction.
     */
    private static boolean tryPlaceBed(
            Level level,
            BlockPos machinePos,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection,
            Direction machineFacing
    ) {
        // Determine head and foot positions based on placement direction
        // In vanilla beds: BedBlock.FACING is the direction your feet point when sleeping
        // The head part is at the position you right-click, the foot part extends in the FACING direction
        BlockPos headPos;
        BlockPos footPos;
        Direction bedFacing;
        if (worldDirection == Direction.DOWN) {
            // DOWN placement (from ceiling): like right-clicking the space below while facing the machine
            // Foot at targetPos (closest to machine), head extends in bedFacing direction (away from machine)
            bedFacing = machineFacing != null && machineFacing.getAxis().isHorizontal() ? machineFacing.getOpposite() : Direction.NORTH;
            footPos = targetPos;
            headPos = targetPos.relative(bedFacing);
        } else if (worldDirection == Direction.UP) {
            // UP placement (from floor): like right-clicking the space above while facing the machine
            // Foot at targetPos (closest to machine), head extends in bedFacing direction (away from machine)
            bedFacing = machineFacing != null && machineFacing.getAxis().isHorizontal() ? machineFacing.getOpposite() : Direction.NORTH;
            footPos = targetPos;
            headPos = targetPos.relative(bedFacing);
        } else {
            // Horizontal placement: machine faces into the feet of the bed
            // Foot at targetPos (closest to machine), head extends in worldDirection (away from machine)
            bedFacing = worldDirection;
            footPos = targetPos;
            headPos = targetPos.relative(bedFacing);
        }

        // Check build height bounds
        if (headPos.getY() < level.getMinBuildHeight() || headPos.getY() >= level.getMaxBuildHeight() ||
            footPos.getY() < level.getMinBuildHeight() || footPos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }

        // Beds can float in vanilla, so no floor support check is needed
        // Check that both positions are air (or waterloggable water)
        if (!isPositionValidForPlacement(level, headPos, block)) {
            return false;
        }
        if (!isPositionValidForPlacement(level, footPos, block)) {
            return false;
        }
        // Get the default block state
        BlockState headState = block.defaultBlockState();

        // Apply stored NBT state if present
        if (blockStateNbt != null) {
            try {
                headState = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), blockStateNbt);
            } catch (Exception e) {
                // Fall back to default
            }
        }

        // Set bed properties: FACING (horizontal), PART (head), OCCUPIED (false)
        headState = headState
                .setValue(BedBlock.FACING, bedFacing)
                .setValue(BedBlock.PART, BedPart.HEAD)
                .setValue(BlockStateProperties.OCCUPIED, false);

        // Apply WATERLOGGED if applicable
        if (headState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(headPos);
            headState = headState.setValue(BlockStateProperties.WATERLOGGED, fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        // Place the head part
        if (!level.setBlock(headPos, headState, 3)) {
            return false;
        }

        // Create the foot part state
        BlockState footState = headState.setValue(BedBlock.PART, BedPart.FOOT);

        // Apply WATERLOGGED for foot part
        if (footState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(footPos);
            footState = footState.setValue(BlockStateProperties.WATERLOGGED, fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        // Place the foot part
        level.setBlock(footPos, footState, 3);
        // Consume one block from inventory
        itemHandler.extractItem(invSlot, 1, false);

        // Play the block place sound
        net.minecraft.world.level.block.SoundType soundType = headState.getSoundType();
        net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
        if (placeSound != null) {
            level.playSound(null, headPos, placeSound,
                    net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }

        return true;
    }

    /**
     * Handles scaffolding placement with direction-aware logic.
     * Vertical output (UP/DOWN): Stack upward, finding the top of existing scaffolding.
     * Horizontal output: Place in the direction of output, extending horizontally like a player.
     * If no existing scaffolding, places normally at the target position.
     */
    private static boolean tryPlaceScaffolding(
            Level level,
            BlockPos targetPos,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection,
            Direction machineFacing
    ) {
        BlockPos placePos;

        // Vertical output (UP/DOWN): Stack upward behavior
        if (worldDirection.getAxis().isVertical()) {
            // Check if scaffolding already exists at the target position — stacking case
            if (level.getBlockState(targetPos).is(Blocks.SCAFFOLDING)) {
                BlockPos stackTop = findScaffoldingStackTop(level, targetPos);
                if (stackTop == null || stackTop.getY() >= level.getMaxBuildHeight() - 1) {
                    return false;
                }
                placePos = stackTop.above();
            } else {
                // No scaffolding at target — normal placement
                placePos = targetPos;
            }
        } else {
            // Horizontal output: Extend in the direction of output, like player placement
            // Check if scaffolding already exists at the target position
            if (level.getBlockState(targetPos).is(Blocks.SCAFFOLDING)) {
                // Find the end of the horizontal scaffolding chain in the output direction
                BlockPos chainEnd = findScaffoldingChainEnd(level, targetPos, worldDirection);
                placePos = chainEnd.relative(worldDirection);
            } else {
                // No scaffolding at target — normal placement
                placePos = targetPos;
            }
        }

        // Check place position is clear
        if (!isPositionValidForPlacement(level, placePos, Blocks.SCAFFOLDING)) {
            return false;
        }
        // Calculate distance, excluding the machine block from counting as support
        BlockPos machinePos = targetPos.relative(worldDirection.getOpposite());
        int distance = calculateScaffoldingDistance(level, placePos, machinePos);
        boolean bottom = !level.getBlockState(placePos.below()).is(Blocks.SCAFFOLDING);
        BlockState placeState = Blocks.SCAFFOLDING.defaultBlockState()
                .setValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE, distance)
                .setValue(net.minecraft.world.level.block.ScaffoldingBlock.BOTTOM, bottom);
        // Apply waterlogging
        if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(placePos);
            placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                    fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        if (level.setBlock(placePos, placeState, 3)) {
            itemHandler.extractItem(invSlot, 1, false);
            net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, placePos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }
            return true;
        }
        return false;
    }

    /**
     * Finds the end of a horizontal scaffolding chain in the given direction.
     * Starting from the given position, follows scaffolding blocks in the direction
     * until a non-scaffolding block is found, then returns the last scaffolding position.
     */
    private static BlockPos findScaffoldingChainEnd(Level level, BlockPos startPos, Direction direction) {
        BlockPos current = startPos;
        while (level.getBlockState(current).is(Blocks.SCAFFOLDING)) {
            BlockPos next = current.relative(direction);
            if (!level.getBlockState(next).is(Blocks.SCAFFOLDING)) {
                break;
            }
            current = next;
        }
        return current; // Return the last scaffolding block in the chain
    }

    /**
     * Finds the top of a scaffolding stack by going up from the given position.
     * Returns the highest scaffolding position in the stack.
     */
    private static BlockPos findScaffoldingStackTop(Level level, BlockPos pos) {
        BlockPos top = pos;
        while (level.getBlockState(top).is(Blocks.SCAFFOLDING)) {
            top = top.above();
        }
        return top.below(); // Return the last scaffolding block
    }

    /**
     * Calculates the scaffolding DISTANCE property for a position.
     * Distance is the minimum horizontal distance from a solid supporting block.
     * Scaffolding can survive up to distance 6 from support.
     * The machine position is excluded so the machine itself doesn't count as support.
     */
    private static int calculateScaffoldingDistance(Level level, BlockPos pos, BlockPos machinePos) {
        // Check if there's scaffolding below — inherit its distance
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.SCAFFOLDING)) {
            return below.getValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE);
        }
        // Check if there's a solid block below — distance 0 (but not the machine)
        if (pos.below().equals(machinePos)) {
            // Machine is below — don't count it as support
        } else if (below.isSolidRender(level, pos.below()) && below.getBlock() != Blocks.AIR) {
            return 0;
        }
        // Check horizontal directions for nearby scaffolding or solid blocks
        int minDistance = 7;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adjPos = pos.relative(dir);
            // Skip the machine's position
            if (adjPos.equals(machinePos)) {
                continue;
            }
            BlockState adjState = level.getBlockState(adjPos);
            if (adjState.is(Blocks.SCAFFOLDING)) {
                int adjDistance = adjState.getValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE);
                if (adjDistance < minDistance) {
                    minDistance = adjDistance;
                }
            } else if (adjState.isSolidRender(level, adjPos) && adjState.getBlock() != Blocks.AIR) {
                return 0;
            }
        }
        return Math.min(minDistance + 1, 7);
    }

    /**
     * Handles candle stacking — if a candle already exists at the target position,
     * increases the CANDLES count (up to 4). Otherwise, places normally.
     */
    private static boolean tryPlaceCandle(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        BlockState existing = level.getBlockState(targetPos);

        // Check if there's already a candle at the target position
        if (existing.getBlock() instanceof CandleBlock) {
            int currentCandles = existing.getValue(CandleBlock.CANDLES);
            if (currentCandles >= 4) {
                return false;
            }
            // Stack on existing candle
            int newCandles = currentCandles + 1;
            BlockState newState = existing.setValue(CandleBlock.CANDLES, newCandles);
            level.setBlock(targetPos, newState, 3);
            itemHandler.extractItem(invSlot, 1, false);
            net.minecraft.world.level.block.SoundType soundType = newState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, targetPos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }
            return true;
        }

        // No existing candle — check if target is air and place normally
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        // Place a single candle normally
        BlockState placeState = block.defaultBlockState()
                .setValue(CandleBlock.CANDLES, 1)
                .setValue(CandleBlock.LIT, false);
        // Apply waterlogging
        if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(targetPos);
            placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                    fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        level.setBlock(targetPos, placeState, 3);
        itemHandler.extractItem(invSlot, 1, false);
        net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
        net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
        if (placeSound != null) {
            level.playSound(null, targetPos, placeSound,
                    net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
        return true;
    }

    /**
     * Handles sea pickle stacking — if sea pickles already exist at the target position,
     * increases the PICKLES count (up to 4). Otherwise, places normally.
     */
    private static boolean tryPlaceSeaPickle(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        BlockState existing = level.getBlockState(targetPos);

        // Check if there are already sea pickles at the target position
        if (existing.getBlock() instanceof net.minecraft.world.level.block.SeaPickleBlock) {
            int currentPickles = existing.getValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES);
            if (currentPickles >= 4) {
                return false;
            }
            // Stack on existing sea pickles
            int newPickles = currentPickles + 1;
            BlockState newState = existing.setValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES, newPickles);
            level.setBlock(targetPos, newState, 3);
            itemHandler.extractItem(invSlot, 1, false);
            net.minecraft.world.level.block.SoundType soundType = newState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, targetPos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }
            return true;
        }

        // No existing sea pickles — check if target is air and place normally
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        // Sea pickles require a solid block below (vanilla behavior — cannot float)
        BlockState belowState = level.getBlockState(targetPos.below());
        if (!belowState.isSolidRender(level, targetPos.below())) {
            return false;
        }

        // Place a single sea pickle normally
        BlockState placeState = block.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES, 1);
        // Apply waterlogging
        if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(targetPos);
            placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                    fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        level.setBlock(targetPos, placeState, 3);
        itemHandler.extractItem(invSlot, 1, false);
        net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
        net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
        if (placeSound != null) {
            level.playSound(null, targetPos, placeSound,
                    net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
        return true;
    }

    /**
     * Handles snow layer stacking — if snow already exists at the target position,
     * increases the LAYERS count (up to 8). Otherwise, places normally.
     */
    private static boolean tryPlaceSnowLayer(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        BlockState existing = level.getBlockState(targetPos);

        // Check if there's already snow at the target position
        if (existing.getBlock() instanceof net.minecraft.world.level.block.SnowLayerBlock) {
            int currentLayers = existing.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS);
            if (currentLayers >= 8) {
                return false;
            }
            // Stack on existing snow
            int newLayers = currentLayers + 1;
            BlockState newState = existing.setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, newLayers);
            level.setBlock(targetPos, newState, 3);
            itemHandler.extractItem(invSlot, 1, false);
            net.minecraft.world.level.block.SoundType soundType = newState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, targetPos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }
            return true;
        }

        // No existing snow — check if target is air and place normally
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        // Snow layers require a solid block below (vanilla behavior — cannot float)
        BlockState belowState = level.getBlockState(targetPos.below());
        if (!belowState.isSolidRender(level, targetPos.below())) {
            return false;
        }

        // Place a single snow layer normally
        BlockState placeState = block.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 1);

        level.setBlock(targetPos, placeState, 3);
        itemHandler.extractItem(invSlot, 1, false);
        net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
        net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
        if (placeSound != null) {
            level.playSound(null, targetPos, placeSound,
                    net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
        return true;
    }

    /**
     * Handles pink petal stacking — if pink petals already exist at the target position,
     * increases the flower_amount count (up to 4). Otherwise, places normally.
     * Pink petals require a valid ground block (grass, dirt, moss, etc.) and cannot float.
     */
    @SuppressWarnings("unchecked")
    private static boolean tryPlacePinkPetals(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        // Look up pink petal properties by name (Parchment mappings may rename the class fields)
        net.minecraft.world.level.block.state.properties.IntegerProperty flowerAmountProp = null;
        Property facingProp = null;
        BlockState defaultState = block.defaultBlockState();
        for (Property<?> prop : defaultState.getProperties()) {
            if (prop.getName().equals("flower_amount") && prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty) {
                flowerAmountProp = (net.minecraft.world.level.block.state.properties.IntegerProperty) prop;
            }
            if (prop.getName().equals("facing") && prop instanceof net.minecraft.world.level.block.state.properties.DirectionProperty) {
                facingProp = (Property) prop;
            }
        }

        if (flowerAmountProp == null) {
            return false;
        }

        BlockState existing = level.getBlockState(targetPos);

        // Check if there are already pink petals at the target position
        if (existing.getBlock() instanceof net.minecraft.world.level.block.PinkPetalsBlock) {
            int currentPetals = existing.getValue(flowerAmountProp);
            if (currentPetals >= 4) {
                return false;
            }
            // Stack on existing pink petals
            int newPetals = currentPetals + 1;
            BlockState newState = existing.setValue(flowerAmountProp, newPetals);
            level.setBlock(targetPos, newState, 3);
            itemHandler.extractItem(invSlot, 1, false);
            net.minecraft.world.level.block.SoundType soundType = newState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, targetPos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }
            return true;
        }

        // No existing pink petals — check if target is air and place normally
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        // Pink petals require a valid ground block below (vanilla behavior — cannot float)
        BlockState belowState = level.getBlockState(targetPos.below());
        if (!belowState.isSolidRender(level, targetPos.below())) {
            return false;
        }

        // Place a single pink petal normally
        BlockState placeState = defaultState.setValue(flowerAmountProp, 1);
        // Set facing direction (opposite of placement direction, like vanilla)
        if (facingProp != null) {
            placeState = placeState.setValue(facingProp, worldDirection.getOpposite());
        }

        level.setBlock(targetPos, placeState, 3);
        itemHandler.extractItem(invSlot, 1, false);
        net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
        net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
        if (placeSound != null) {
            level.playSound(null, targetPos, placeSound,
                    net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
        return true;
    }

    /**
     * Handles pointed dripstone placement with stacking and ceiling/floor attachment logic.
     * If existing dripstone is found at the target, extends the chain in its growth direction.
     * If no existing dripstone but a ceiling block exists above, places a new dripstone hanging from it (DOWN).
     * If no existing dripstone but a floor block exists below, places a new dripstone growing from it (UP).
     * Dripstone can stack up to 4 blocks tall (tip → frustum → middle → base).
     */
    @SuppressWarnings("unchecked")
    private static boolean tryPlaceDripstone(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        // Look up dripstone-specific properties by name (Parchment mappings may rename the class fields)
        Property verticalDirProp = null;
        Property thicknessProp = null;
        BlockState defaultState = block.defaultBlockState();
        for (Property<?> prop : defaultState.getProperties()) {
            if (prop.getName().equals("vertical_direction") && prop instanceof net.minecraft.world.level.block.state.properties.DirectionProperty) {
                verticalDirProp = (Property) prop;
            }
            if (prop.getName().equals("thickness")) {
                thicknessProp = (Property) prop;
            }
        }

        if (verticalDirProp == null || thicknessProp == null) {
            return false;
        }

        // Check if there's already dripstone at the target position — stacking case
        BlockState existing = level.getBlockState(targetPos);
        if (existing.getBlock() instanceof net.minecraft.world.level.block.PointedDripstoneBlock) {
            // Detect the growth direction of the existing dripstone chain
            Direction chainDirection = (Direction) existing.getValue(verticalDirProp);
            // Find the end of the existing dripstone chain in its growth direction
            BlockPos chainEnd = findDripstoneChainEnd(level, targetPos, chainDirection);
            // Check if the chain is already at max length (4 blocks) — thickness "base" means max
            Object thicknessValue = existing.getValue(thicknessProp);
            String thicknessName = thicknessValue.toString().toLowerCase();
            if ("base".equals(thicknessName)) {
                return false;
            }

            // Place one block beyond the chain end in the growth direction
            BlockPos placePos = chainEnd.relative(chainDirection);
            if (!isPositionValidForPlacement(level, placePos, block)) {
                return false;
            }

            // Determine next thickness value based on current position in chain
            // tip → frustum → middle → base
            String newThicknessName;
            switch (thicknessName) {
                case "tip" -> newThicknessName = "frustum";
                case "frustum" -> newThicknessName = "middle";
                case "middle" -> newThicknessName = "base";
                default -> {
                    return false;
                }
            }

            // Find the matching enum constant from possible values
            Object newThicknessValue = null;
            for (Object val : thicknessProp.getPossibleValues()) {
                if (val.toString().toLowerCase().equals(newThicknessName)) {
                    newThicknessValue = val;
                    break;
                }
            }
            if (newThicknessValue == null) {
                return false;
            }

            BlockState placeState = defaultState
                    .setValue(verticalDirProp, chainDirection)
                    .setValue(thicknessProp, (Comparable) newThicknessValue);

            // Apply waterlogging
            if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                FluidState fluidState = level.getFluidState(placePos);
                placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                        fluidState.is(Fluids.WATER) && fluidState.isSource());
            }

            level.setBlock(placePos, placeState, 3);
            itemHandler.extractItem(invSlot, 1, false);
            net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, placePos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }
            return true;
        }

        // No existing dripstone at target — check for ceiling block above to hang from (DOWN)
        BlockState aboveState = level.getBlockState(targetPos.above());
        boolean hasCeiling = aboveState.isSolidRender(level, targetPos.above()) && aboveState.getBlock() != Blocks.AIR;

        if (hasCeiling) {
            if (!isPositionValidForPlacement(level, targetPos, block)) {
                return false;
            }

            // Find the "tip" thickness value
            Object tipValue = null;
            for (Object val : thicknessProp.getPossibleValues()) {
                if (val.toString().toLowerCase().equals("tip")) {
                    tipValue = val;
                    break;
                }
            }
            if (tipValue == null) {
                return false;
            }

            // Place a new tip dripstone hanging from the ceiling
            BlockState placeState = defaultState
                    .setValue(verticalDirProp, Direction.DOWN)
                    .setValue(thicknessProp, (Comparable) tipValue);

            // Apply waterlogging
            if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                FluidState fluidState = level.getFluidState(targetPos);
                placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                        fluidState.is(Fluids.WATER) && fluidState.isSource());
            }

            level.setBlock(targetPos, placeState, 3);
            itemHandler.extractItem(invSlot, 1, false);
            net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, targetPos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }
            return true;
        }

        // Check for floor block below to grow from (UP)
        BlockState belowState = level.getBlockState(targetPos.below());
        boolean hasFloor = belowState.isSolidRender(level, targetPos.below()) && belowState.getBlock() != Blocks.AIR;

        if (hasFloor) {
            if (!isPositionValidForPlacement(level, targetPos, block)) {
                return false;
            }

            // Find the "tip" thickness value
            Object tipValue = null;
            for (Object val : thicknessProp.getPossibleValues()) {
                if (val.toString().toLowerCase().equals("tip")) {
                    tipValue = val;
                    break;
                }
            }
            if (tipValue == null) {
                return false;
            }

            // Place a new tip dripstone growing from the floor
            BlockState placeState = defaultState
                    .setValue(verticalDirProp, Direction.UP)
                    .setValue(thicknessProp, (Comparable) tipValue);

            // Apply waterlogging
            if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                FluidState fluidState = level.getFluidState(targetPos);
                placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                        fluidState.is(Fluids.WATER) && fluidState.isSource());
            }

            level.setBlock(targetPos, placeState, 3);
            itemHandler.extractItem(invSlot, 1, false);
            net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, targetPos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }
            return true;
        }

        // No ceiling, no floor, and no existing dripstone — cannot place
        return false;
    }

    /**
     * Finds the end of a pointed dripstone chain by following the given direction from the start position.
     * Returns the last dripstone position in the chain in the given direction.
     */
    private static BlockPos findDripstoneChainEnd(Level level, BlockPos startPos, Direction direction) {
        BlockPos current = startPos;
        while (level.getBlockState(current).getBlock() instanceof net.minecraft.world.level.block.PointedDripstoneBlock) {
            BlockPos next = current.relative(direction);
            if (level.getBlockState(next).getBlock() instanceof net.minecraft.world.level.block.PointedDripstoneBlock) {
                current = next;
            } else {
                break;
            }
        }
        return current;
    }

    /**
     * Handles turtle egg stacking — if turtle eggs already exist at the target position,
     * increases the EGGS count (up to 4). Otherwise, places normally.
     * Turtle eggs can float (no support block required) and can even be placed on other turtle eggs.
     */
    private static boolean tryPlaceTurtleEgg(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        BlockState existing = level.getBlockState(targetPos);

        // Check if there are already turtle eggs at the target position
        if (existing.getBlock() instanceof net.minecraft.world.level.block.TurtleEggBlock) {
            int currentEggs = existing.getValue(net.minecraft.world.level.block.TurtleEggBlock.EGGS);
            if (currentEggs >= 4) {
                return false;
            }
            // Stack on existing turtle eggs
            int newEggs = currentEggs + 1;
            BlockState newState = existing.setValue(net.minecraft.world.level.block.TurtleEggBlock.EGGS, newEggs);
            level.setBlock(targetPos, newState, 3);
            itemHandler.extractItem(invSlot, 1, false);
            net.minecraft.world.level.block.SoundType soundType = newState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, targetPos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }
            return true;
        }

        // No existing turtle eggs — check if target is air and place normally
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        // Place a single turtle egg normally (turtle eggs can float — no support check needed)
        BlockState placeState = block.defaultBlockState()
                .setValue(net.minecraft.world.level.block.TurtleEggBlock.EGGS, 1)
                .setValue(net.minecraft.world.level.block.TurtleEggBlock.HATCH, 0);

        level.setBlock(targetPos, placeState, 3);
        itemHandler.extractItem(invSlot, 1, false);
        net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
        net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
        if (placeSound != null) {
            level.playSound(null, targetPos, placeSound,
                    net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
        return true;
    }

    /**
     * Handles amethyst bud/cluster placement by trying all 6 directions.
     * Priority order: opposite of placement direction first (so if placing UP, amethyst faces DOWN to perch on ceiling),
     * then perpendicular sides, then remaining directions, with the machine face (attachDirection) as the absolute last resort.
     * Amethyst clusters have a FACING property that accepts all 6 directions.
     */
    private static boolean tryPlaceAmethyst(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection,
            Direction attachDirection
    ) {
        // Check if target position is valid
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        // Build direction priority based on placement direction (worldDirection).
        // The amethyst FACING points to where it perches, so if placing UP, we want it to face DOWN (perch on ceiling).
        // Priority: opposite of worldDirection first, then perpendicular sides, then remaining, machine face last.
        // Note: getClockWise/getCounterClockWise only work on horizontal directions, so handle vertical separately.
        List<Direction> attemptDirections = new ArrayList<>();

        // 1. Opposite of placement direction (highest priority - the direction the amethyst will perch toward)
        Direction opposite = worldDirection.getOpposite();
        attemptDirections.add(opposite);

        // 2. Perpendicular sides — for horizontal placement, use clockwise/counter-clockwise;
        //    for vertical placement (UP/DOWN), try all 4 cardinal directions instead
        if (opposite.getAxis().isHorizontal()) {
            Direction perp1 = opposite.getClockWise();
            Direction perp2 = opposite.getCounterClockWise();
            attemptDirections.add(perp1);
            attemptDirections.add(perp2);
        } else {
            // Vertical placement: try all cardinal wall directions before falling back to machine face
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (!attemptDirections.contains(dir)) {
                    attemptDirections.add(dir);
                }
            }
        }

        // 3. Remaining directions (excluding machine face)
        for (Direction dir : Direction.values()) {
            if (!attemptDirections.contains(dir) && dir != attachDirection) {
                attemptDirections.add(dir);
            }
        }

        // 4. Machine face is the absolute last resort
        attemptDirections.add(attachDirection);

        BlockState defaultState = block.defaultBlockState();

        // Try each direction
        for (Direction dir : attemptDirections) {
            BlockState placeState = defaultState.setValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING, dir);

            // Apply waterlogging
            if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                FluidState fluidState = level.getFluidState(targetPos);
                placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                        fluidState.is(Fluids.WATER) && fluidState.isSource());
            }

            if (canBlockSurvive(block, placeState, level, targetPos)) {
                if (placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Handles glow lichen placement by trying all 6 directions.
     * Priority order: opposite of placement direction first, then perpendicular sides,
     * then remaining directions. The machine face (attachDirection) is NEVER tried — placement fails if no other direction works.
     * Glow lichen has boolean properties for each face (DOWN, UP, NORTH, SOUTH, WEST, EAST).
     */
    private static boolean tryPlaceGlowLichen(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection,
            Direction attachDirection
    ) {
        // Check if target position is valid
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        // Build direction priority: opposite of placement direction first, then perpendicular sides,
        // then remaining directions, with the machine face as the absolute last resort.
        List<Direction> attemptDirections = new ArrayList<>();

        // 1. Opposite of placement direction (highest priority)
        // BUT exclude the machine's own direction — worldDirection.getOpposite() IS attachDirection,
        // so we don't add it here; instead, try the opposite wall (the block the machine is facing)
        Direction opposite = worldDirection.getOpposite();
        if (opposite != attachDirection) {
            attemptDirections.add(opposite);
        }

        // 2. Perpendicular sides — for horizontal placement, use clockwise/counter-clockwise;
        //    for vertical placement (UP/DOWN), try all 4 cardinal directions instead
        if (opposite.getAxis().isHorizontal()) {
            Direction perp1 = opposite.getClockWise();
            Direction perp2 = opposite.getCounterClockWise();
            if (perp1 != attachDirection && !attemptDirections.contains(perp1)) {
                attemptDirections.add(perp1);
            }
            if (perp2 != attachDirection && !attemptDirections.contains(perp2)) {
                attemptDirections.add(perp2);
            }
        } else {
            // Vertical placement: try all cardinal wall directions before falling back to machine face
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (dir != attachDirection && !attemptDirections.contains(dir)) {
                    attemptDirections.add(dir);
                }
            }
        }

        // 3. Remaining directions (excluding machine face - never attach to the machine)
        for (Direction dir : Direction.values()) {
            if (!attemptDirections.contains(dir) && dir != attachDirection) {
                attemptDirections.add(dir);
            }
        }

        // Machine face (attachDirection) is NEVER tried — fail if no other direction works

        BlockState defaultState = block.defaultBlockState();

        // Try each direction — glow lichen uses boolean properties for each face
        for (Direction dir : attemptDirections) {
            // Glow lichen has boolean properties: DOWN, UP, NORTH, SOUTH, WEST, EAST
            // We need to set only the one for the direction we want
            BlockState placeState = defaultState;
            for (Direction d : Direction.values()) {
                Property<Boolean> prop = null;
                for (Property<?> p : placeState.getProperties()) {
                    if (p.getName().equalsIgnoreCase(d.getName()) && p instanceof net.minecraft.world.level.block.state.properties.BooleanProperty) {
                        prop = (Property<Boolean>) (Property<?>) p;
                        break;
                    }
                }
                if (prop != null) {
                    placeState = placeState.setValue(prop, d == dir);
                }
            }

            // Apply waterlogging
            if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                FluidState fluidState = level.getFluidState(targetPos);
                placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                        fluidState.is(Fluids.WATER) && fluidState.isSource());
            }

            if (canBlockSurvive(block, placeState, level, targetPos)) {
                if (placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Handles vine placement by trying all 6 directions.
     * Priority order: opposite of placement direction first, then perpendicular sides,
     * then remaining directions. The machine face (attachDirection) is NEVER tried — placement fails if no other direction works.
     * Vines have boolean properties for each face (DOWN, UP, NORTH, SOUTH, WEST, EAST).
     */
    private static boolean tryPlaceVines(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection,
            Direction attachDirection
    ) {
        // Check if target position is valid
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        // Build direction priority: opposite of placement direction first, then perpendicular sides,
        // then remaining directions, with the machine face as the absolute last resort.
        List<Direction> attemptDirections = new ArrayList<>();

        // 1. Opposite of placement direction (highest priority)
        // BUT exclude the machine's own direction — worldDirection.getOpposite() IS attachDirection,
        // so we don't add it here; instead, try the opposite wall (the block the machine is facing)
        Direction opposite = worldDirection.getOpposite();
        if (opposite != attachDirection) {
            attemptDirections.add(opposite);
        }

        // 2. Perpendicular sides — for horizontal placement, use clockwise/counter-clockwise;
        //    for vertical placement (UP/DOWN), try all 4 cardinal directions instead
        if (opposite.getAxis().isHorizontal()) {
            Direction perp1 = opposite.getClockWise();
            Direction perp2 = opposite.getCounterClockWise();
            if (perp1 != attachDirection && !attemptDirections.contains(perp1)) {
                attemptDirections.add(perp1);
            }
            if (perp2 != attachDirection && !attemptDirections.contains(perp2)) {
                attemptDirections.add(perp2);
            }
        } else {
            // Vertical placement: try all cardinal wall directions before falling back to machine face
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (dir != attachDirection && !attemptDirections.contains(dir)) {
                    attemptDirections.add(dir);
                }
            }
        }

        // 3. Remaining directions (excluding machine face - never attach to the machine)
        for (Direction dir : Direction.values()) {
            if (!attemptDirections.contains(dir) && dir != attachDirection) {
                attemptDirections.add(dir);
            }
        }

        // Machine face (attachDirection) is NEVER tried — fail if no other direction works

        BlockState defaultState = block.defaultBlockState();

        // Try each direction — vines use boolean properties for each face
        for (Direction dir : attemptDirections) {
            // Vines have boolean properties: DOWN, UP, NORTH, SOUTH, WEST, EAST
            // We need to set only the one for the direction we want
            BlockState placeState = defaultState;
            for (Direction d : Direction.values()) {
                Property<Boolean> prop = null;
                for (Property<?> p : placeState.getProperties()) {
                    if (p.getName().equalsIgnoreCase(d.getName()) && p instanceof net.minecraft.world.level.block.state.properties.BooleanProperty) {
                        prop = (Property<Boolean>) (Property<?>) p;
                        break;
                    }
                }
                if (prop != null) {
                    placeState = placeState.setValue(prop, d == dir);
                }
            }

            // Apply waterlogging
            if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                FluidState fluidState = level.getFluidState(targetPos);
                placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                        fluidState.is(Fluids.WATER) && fluidState.isSource());
            }

            if (canBlockSurvive(block, placeState, level, targetPos)) {
                if (placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Handles bell placement with specific priority order:
     * 1. Floor-mounted (FLOOR attachment) — when a floor block exists below target. FACING matches machine facing.
     * 2. Ceiling-mounted (CEILING attachment) — when a ceiling block exists and no floor block exists. FACING doesn't matter visually.
     * 3. Wall-mounted on opposite wall (SINGLE_WALL attachment) — when an opposite wall exists and no floor or ceiling. FACING matches machine facing.
     * 4. Fail if only the machine is eligible for perching.
     */
    private static boolean tryPlaceBell(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection,
            Direction attachDirection,
            Direction machineFacing
    ) {
        // Check if target position is valid
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        BlockState defaultState = block.defaultBlockState();

        // Ensure machineFacing is horizontal for bell FACING property
        Direction bellFacing = machineFacing;
        if (!bellFacing.getAxis().isHorizontal()) {
            bellFacing = Direction.NORTH;
        }

        // Check which adjacent blocks exist (excluding the machine's own direction)
        boolean hasFloor = level.getBlockState(targetPos.relative(Direction.DOWN)).isSolidRender(level, targetPos.relative(Direction.DOWN));
        boolean hasCeiling = level.getBlockState(targetPos.relative(Direction.UP)).isSolidRender(level, targetPos.relative(Direction.UP));

        // Opposite wall of the IO direction (the wall the machine is facing toward)
        Direction oppositeWall = worldDirection.getOpposite();
        boolean hasOppositeWall = oppositeWall.getAxis().isHorizontal() &&
                level.getBlockState(targetPos.relative(oppositeWall)).isSolidRender(level, targetPos.relative(oppositeWall));

        // Check for side walls (excluding machine face)
        boolean hasSideWall = false;
        if (oppositeWall.getAxis().isHorizontal()) {
            Direction side1 = oppositeWall.getClockWise();
            Direction side2 = oppositeWall.getCounterClockWise();
            if (side1 != attachDirection && level.getBlockState(targetPos.relative(side1)).isSolidRender(level, targetPos.relative(side1))) {
                hasSideWall = true;
            }
            if (side2 != attachDirection && level.getBlockState(targetPos.relative(side2)).isSolidRender(level, targetPos.relative(side2))) {
                hasSideWall = true;
            }
        } else {
            // For vertical placement, check all horizontal directions except machine face
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (dir != attachDirection && level.getBlockState(targetPos.relative(dir)).isSolidRender(level, targetPos.relative(dir))) {
                    hasSideWall = true;
                    break;
                }
            }
        }

        // Priority 1: Floor-mounted (FLOOR attachment) — when floor block exists
        // This applies for UP output (bell hangs from floor above) and horizontal output (bell sits on floor below)
        if (hasFloor) {
            BlockState placeState = defaultState
                    .setValue(BlockStateProperties.BELL_ATTACHMENT,
                            net.minecraft.world.level.block.state.properties.BellAttachType.FLOOR)
                    .setValue(net.minecraft.world.level.block.BellBlock.FACING, bellFacing);

            if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                FluidState fluidState = level.getFluidState(targetPos);
                placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                        fluidState.is(Fluids.WATER) && fluidState.isSource());
            }

            if (canBlockSurvive(block, placeState, level, targetPos)) {
                if (placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                    return true;
                }
            }
        }

        // Priority 2: Ceiling-mounted (CEILING attachment) — when a ceiling block exists and no floor block exists
        if (hasCeiling && !hasFloor) {
            BlockState placeState = defaultState
                    .setValue(BlockStateProperties.BELL_ATTACHMENT,
                            net.minecraft.world.level.block.state.properties.BellAttachType.CEILING)
                    .setValue(net.minecraft.world.level.block.BellBlock.FACING, bellFacing);

            if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                FluidState fluidState = level.getFluidState(targetPos);
                placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                        fluidState.is(Fluids.WATER) && fluidState.isSource());
            }

            if (canBlockSurvive(block, placeState, level, targetPos)) {
                if (placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                    return true;
                }
            }
        }

        // Priority 3: Wall-mounted on opposite wall (SINGLE_WALL) — when opposite wall exists and no floor or ceiling
        if (hasOppositeWall && !hasFloor && !hasCeiling) {
            BlockState placeState = defaultState
                    .setValue(BlockStateProperties.BELL_ATTACHMENT,
                            net.minecraft.world.level.block.state.properties.BellAttachType.SINGLE_WALL)
                    .setValue(net.minecraft.world.level.block.BellBlock.FACING, bellFacing);

            if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                FluidState fluidState = level.getFluidState(targetPos);
                placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                        fluidState.is(Fluids.WATER) && fluidState.isSource());
            }

            if (canBlockSurvive(block, placeState, level, targetPos)) {
                if (placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Handles sculk vein placement by trying all 6 directions.
     * Priority order: opposite of placement direction first, then perpendicular sides,
     * then remaining directions. The machine face (attachDirection) is NEVER tried — placement fails if no other direction works.
     * Sculk vein has boolean properties for each face (DOWN, UP, NORTH, SOUTH, WEST, EAST).
     */
    private static boolean tryPlaceSculkVein(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection,
            Direction attachDirection
    ) {
        // Check if target position is valid
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        // Build direction priority: opposite of placement direction first, then perpendicular sides,
        // then remaining directions, with the machine face as the absolute last resort.
        List<Direction> attemptDirections = new ArrayList<>();

        // 1. Opposite of placement direction (highest priority)
        // BUT exclude the machine's own direction — worldDirection.getOpposite() IS attachDirection,
        // so we don't add it here; instead, try the opposite wall (the block the machine is facing)
        Direction opposite = worldDirection.getOpposite();
        if (opposite != attachDirection) {
            attemptDirections.add(opposite);
        }

        // 2. Perpendicular sides — for horizontal placement, use clockwise/counter-clockwise;
        //    for vertical placement (UP/DOWN), try all 4 cardinal directions instead
        if (opposite.getAxis().isHorizontal()) {
            Direction perp1 = opposite.getClockWise();
            Direction perp2 = opposite.getCounterClockWise();
            if (perp1 != attachDirection && !attemptDirections.contains(perp1)) {
                attemptDirections.add(perp1);
            }
            if (perp2 != attachDirection && !attemptDirections.contains(perp2)) {
                attemptDirections.add(perp2);
            }
        } else {
            // Vertical placement: try all cardinal wall directions before falling back to machine face
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (dir != attachDirection && !attemptDirections.contains(dir)) {
                    attemptDirections.add(dir);
                }
            }
        }

        // 3. Remaining directions (excluding machine face - never attach to the machine)
        for (Direction dir : Direction.values()) {
            if (!attemptDirections.contains(dir) && dir != attachDirection) {
                attemptDirections.add(dir);
            }
        }

        // Machine face (attachDirection) is NEVER tried — fail if no other direction works

        BlockState defaultState = block.defaultBlockState();

        // Try each direction — sculk vein uses boolean properties for each face
        for (Direction dir : attemptDirections) {
            // Sculk vein has boolean properties: DOWN, UP, NORTH, SOUTH, WEST, EAST
            // We need to set only the one for the direction we want
            BlockState placeState = defaultState;
            for (Direction d : Direction.values()) {
                Property<Boolean> prop = null;
                for (Property<?> p : placeState.getProperties()) {
                    if (p.getName().equalsIgnoreCase(d.getName()) && p instanceof net.minecraft.world.level.block.state.properties.BooleanProperty) {
                        prop = (Property<Boolean>) (Property<?>) p;
                        break;
                    }
                }
                if (prop != null) {
                    placeState = placeState.setValue(prop, d == dir);
                }
            }

            // Apply waterlogging
            if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                FluidState fluidState = level.getFluidState(targetPos);
                placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                        fluidState.is(Fluids.WATER) && fluidState.isSource());
            }

            if (canBlockSurvive(block, placeState, level, targetPos)) {
                if (placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Simplified kelp placement: places only when there's a base block on water.
     * If the target already has kelp, climbs to the top of the column and keeps
     * going up through water (any water, not just source) until hitting non-water,
     * then places at the last water position.
     */
    private static boolean isKelpColumnBlock(BlockState state) {
        return state.getBlock() instanceof net.minecraft.world.level.block.KelpBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.KelpPlantBlock;
    }

    @SuppressWarnings("unchecked")
    private static boolean tryPlaceKelp(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        // Look up kelp AGE property
        net.minecraft.world.level.block.state.properties.IntegerProperty ageProp = null;
        BlockState defaultState = block.defaultBlockState();
        for (Property<?> prop : defaultState.getProperties()) {
            if (prop.getName().equals("age") && prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty) {
                ageProp = (net.minecraft.world.level.block.state.properties.IntegerProperty) prop;
            }
        }

        // Start searching from the target position
        BlockPos searchPos = targetPos;

        // If there's already kelp at target, climb to the top of the column first
        if (isKelpColumnBlock(level.getBlockState(searchPos))) {
            BlockPos check = searchPos.above();
            while (isKelpColumnBlock(level.getBlockState(check))) {
                searchPos = check;
                check = check.above();
            }
            // Now searchPos is the top of the kelp column; continue climbing through water
        } else {
            // No kelp at target — this is a new base placement
            // Require: water at target + solid block below
            FluidState targetFluid = level.getFluidState(targetPos);
            if (!targetFluid.is(Fluids.WATER)) {
                return false;
            }
            BlockState targetState = level.getBlockState(targetPos);
            if (!targetState.isAir() && !targetState.getFluidState().is(Fluids.WATER)) {
                return false;
            }
            BlockState belowState = level.getBlockState(targetPos.below());
            if (!belowState.isSolidRender(level, targetPos.below())) {
                return false;
            }
            // Place at target — no need to climb further
            return placeKelpAt(level, searchPos, defaultState, ageProp, itemHandler, invSlot);
        }

        // Climb from the top of the kelp column through water blocks
        BlockPos check = searchPos.above();
        while (check.getY() < level.getMaxBuildHeight()) {
            FluidState fluid = level.getFluidState(check);
            if (!fluid.is(Fluids.WATER)) {
                break; // End of water column
            }
            BlockState state = level.getBlockState(check);
            if (isKelpColumnBlock(state)) {
                // Keep climbing through existing kelp
                searchPos = check;
                check = check.above();
                continue;
            }
            if (!state.isAir() && !state.getFluidState().is(Fluids.WATER)) {
                break; // Hit a non-water, non-kelp block (ceiling, etc.)
            }
            // Found an empty water block — place here
            searchPos = check;
            break;
        }

        // Make sure the final position is still in water
        if (!level.getFluidState(searchPos).is(Fluids.WATER)) {
            return false;
        }
        // If we didn't find an empty water slot and searchPos is still the top of the
        // existing kelp column, don't place — we would just replace the existing kelp.
        if (isKelpColumnBlock(level.getBlockState(searchPos))) {
            return false;
        }

        return placeKelpAt(level, searchPos, defaultState, ageProp, itemHandler, invSlot);
    }

    /**
     * Places a single kelp block at the given position and consumes one from inventory.
     */
    private static boolean placeKelpAt(
            Level level,
            BlockPos pos,
            BlockState defaultState,
            net.minecraft.world.level.block.state.properties.IntegerProperty ageProp,
            ItemStackHandler itemHandler,
            int invSlot
    ) {
        if (pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockState placeState = defaultState;
        if (ageProp != null) {
            placeState = placeState.setValue(ageProp, (int) (Math.random() * 25));
        }
        level.setBlock(pos, placeState, 3);
        itemHandler.extractItem(invSlot, 1, false);
        net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
        net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
        if (placeSound != null) {
            level.playSound(null, pos, placeSound,
                    net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
        return true;
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Gets horizontal wall directions to attempt, in priority order:
     * 1. Wall opposite the machine (attachDirection)
     * 2. Side walls (clockwise, counter-clockwise)
     * The machine's own wall (worldDirection) is strictly excluded so blocks are never attached to the machine.
     */
    private static List<Direction> getWallPlacementDirections(Direction worldDirection) {
        List<Direction> dirs = new ArrayList<>(3);
        if (worldDirection.getAxis().isHorizontal()) {
            Direction opposite = worldDirection.getOpposite(); // Wall opposite the machine
            dirs.add(opposite);
            dirs.add(opposite.getClockWise());        // Side wall 1
            dirs.add(opposite.getCounterClockWise()); // Side wall 2
            // Note: worldDirection (the machine's own wall) is intentionally excluded!
        }
        return dirs;
    }

    /**
     * Creates a block state for placement with the given facing direction.
     * Applies HORIZONTAL_FACING, FACING, ATTACH_FACE, AXIS, HANGING, and WATERLOGGED appropriately.
     */
    private static BlockState createPlacementState(Block block, CompoundTag blockStateNbt, Direction facing, Direction machineFacing, Level level, BlockPos targetPos) {
        BlockState placeState = block.defaultBlockState();
        boolean restoredFromNbt = false;

        // If we have stored block state data, try to use it as a base
        if (blockStateNbt != null) {
            try {
                placeState = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), blockStateNbt);
                restoredFromNbt = true;
            } catch (Exception e) {
                // Fall back to default state
            }
        }

        // For Machine Framework blocks, preserve the saved facing from NBT — don't overwrite it
        boolean isMachineFramework = block instanceof com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;

        // Apply HORIZONTAL_FACING if the block supports it
        if (placeState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && !(restoredFromNbt && isMachineFramework)) {
            if (facing.getAxis().isHorizontal()) {
                placeState = placeState.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            } else if (machineFacing != null && machineFacing.getAxis().isHorizontal()) {
                placeState = placeState.setValue(BlockStateProperties.HORIZONTAL_FACING, machineFacing);
            }
        }

        // Apply FACING if the block supports it and the direction is valid for this block
        if (placeState.hasProperty(BlockStateProperties.FACING) && !(restoredFromNbt && isMachineFramework)) {
            if (BlockStateProperties.FACING.getPossibleValues().contains(facing)) {
                placeState = placeState.setValue(BlockStateProperties.FACING, facing);
            }
        }

        // Apply ATTACH_FACE for face-attached blocks (buttons, levers, grindstone)
        if (placeState.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            if (facing == Direction.DOWN) {
                placeState = placeState.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
            } else if (facing == Direction.UP) {
                placeState = placeState.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING);
            } else if (facing.getAxis().isHorizontal()) {
                placeState = placeState.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL);
            }
        }


        // Apply AXIS if the block supports it
        if (placeState.hasProperty(BlockStateProperties.AXIS)) {
            placeState = placeState.setValue(BlockStateProperties.AXIS, facing.getAxis());
        }

        // Apply HANGING for lanterns (true = ceiling, false = floor)
        if (placeState.hasProperty(BlockStateProperties.HANGING)) {
            placeState = placeState.setValue(BlockStateProperties.HANGING, facing == Direction.UP);
        }

        // Apply ROTATION for hanging signs, standing signs, banners, and skulls (0-15, where 0=south, 4=west, 8=north, 12=east)
        // The "rotation" property is defined on each block class, not in BlockStateProperties, so we check by name.
        Property<Integer> rotationProp = null;
        for (Property<?> prop : placeState.getProperties()) {
            if (prop.getName().equals("rotation")) {
                rotationProp = (Property<Integer>) (Property<?>) prop;
                break;
            }
        }
        if (rotationProp != null) {
            Direction rotationDir;
            if (facing.getAxis().isHorizontal()) {
                // Wall-mounted or floor/ceiling with horizontal facing: face the placement direction
                rotationDir = facing;
            } else if (facing == Direction.UP) {
                // Ceiling-mounted: face away from the machine (use machine facing as the outward direction)
                rotationDir = machineFacing != null ? machineFacing : Direction.SOUTH;
            } else {
                // Floor-mounted (facing DOWN): face away from the machine
                rotationDir = machineFacing != null ? machineFacing : Direction.SOUTH;
            }
            int rotation = switch (rotationDir) {
                case SOUTH -> 0;
                case WEST -> 4;
                case NORTH -> 8;
                case EAST -> 12;
                default -> 0;
            };
            placeState = placeState.setValue(rotationProp, rotation);
        }

        // Apply stair-specific logic: HALF (top/bottom) for upside-down placement
        if (block instanceof net.minecraft.world.level.block.StairBlock) {
            if (facing == Direction.DOWN) {
                // Placing from ceiling — stairs should be upside down (HALF=TOP)
                placeState = placeState.setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
                // FACING should be horizontal — use machine facing for rotation
                if (placeState.hasProperty(BlockStateProperties.FACING)) {
                    if (machineFacing != null && machineFacing.getAxis().isHorizontal()) {
                        placeState = placeState.setValue(BlockStateProperties.FACING, machineFacing);
                    }
                }
            } else if (facing == Direction.UP) {
                // Placing from floor — stairs should be right side up (HALF=BOTTOM)
                placeState = placeState.setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM);
                // FACING should be horizontal — use machine facing for rotation
                if (placeState.hasProperty(BlockStateProperties.FACING)) {
                    if (machineFacing != null && machineFacing.getAxis().isHorizontal()) {
                        placeState = placeState.setValue(BlockStateProperties.FACING, machineFacing);
                    }
                }
            } else if (facing.getAxis().isHorizontal()) {
                // Horizontal placement — use the facing direction
                if (placeState.hasProperty(BlockStateProperties.FACING)) {
                    placeState = placeState.setValue(BlockStateProperties.FACING, facing);
                }
            }
        }

        // Apply trapdoor-specific logic: when placed from ceiling, trapdoor should be closed (hanging from ceiling)
        // TrapDoorBlock.FACING is horizontal-only (NORTH/SOUTH/EAST/WEST), unlike BlockStateProperties.FACING (6-direction)
        // For vertical placement (UP/DOWN), the FACING property determines rotation, not the mount direction
        // HALF property determines whether the trapdoor is in the top or bottom half of the block
        if (block instanceof net.minecraft.world.level.block.TrapDoorBlock) {
            Direction trapdoorFacing;
            if (facing == Direction.DOWN) {
                // Placing from ceiling — trapdoor hangs from ceiling, closed, in the top half
                trapdoorFacing = machineFacing != null && machineFacing.getAxis().isHorizontal() ? machineFacing : Direction.NORTH;
                placeState = placeState
                        .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, trapdoorFacing)
                        .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP)
                        .setValue(BlockStateProperties.OPEN, false);
            } else if (facing == Direction.UP) {
                // Placing from floor — trapdoor is on the floor, closed, in the bottom half
                trapdoorFacing = machineFacing != null && machineFacing.getAxis().isHorizontal() ? machineFacing : Direction.NORTH;
                placeState = placeState
                        .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, trapdoorFacing)
                        .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM)
                        .setValue(BlockStateProperties.OPEN, false);
            } else {
                // Horizontal placement — trapdoor attaches to wall
                trapdoorFacing = facing;
                placeState = placeState
                        .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, trapdoorFacing)
                        .setValue(BlockStateProperties.OPEN, false);
            }
        }

        // Apply slab-specific logic: TYPE (top/bottom) for upper-half placement
        if (block instanceof net.minecraft.world.level.block.SlabBlock) {
            if (facing == Direction.DOWN) {
                // Placing from ceiling — slab should be in the upper half
                placeState = placeState.setValue(BlockStateProperties.SLAB_TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP);
            } else if (facing == Direction.UP) {
                // Placing from floor — slab should be in the lower half
                placeState = placeState.setValue(BlockStateProperties.SLAB_TYPE, net.minecraft.world.level.block.state.properties.SlabType.BOTTOM);
            }
        }

        // Apply WATERLOGGED if applicable
        if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(targetPos);
            placeState = placeState.setValue(BlockStateProperties.WATERLOGGED, fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        return placeState;
    }

    /**
     * Checks if a block can survive at a position.
     * Uses BlockState#canSurvive directly.
     */
    private static boolean canBlockSurvive(Block block, BlockState state, Level level, BlockPos pos) {
        try {
            return state.canSurvive(level, pos);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Checks if a position is valid for placing the given block.
     * Water sources can be replaced by waterloggable blocks and full collision blocks.
     */
    private static boolean isPositionValidForPlacement(Level level, BlockPos pos, Block block) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.is(Fluids.WATER) && fluidState.isSource()) {
            BlockState defaultState = block.defaultBlockState();
            boolean isWaterloggable = defaultState.hasProperty(BlockStateProperties.WATERLOGGED);
            boolean isFullBlock = Block.isShapeFullBlock(defaultState.getCollisionShape(level, pos));
            if (Config.isDebugLoggingEnabled() && (isWaterloggable || isFullBlock)) {
                LOGGER.info("[BLOCK MANIPULATOR]   Position {} contains water for block {}", pos, block.getDescriptionId());
            }
            return isWaterloggable || isFullBlock;
        }
        return false;
    }

    /**
     * Returns the wall variant of a block, if one exists.
     * Maps floor/ceiling blocks to their wall-mounted equivalents:
     * - TorchBlock → WallTorchBlock (torch, soul torch, redstone torch)
     * - HangingSignBlock → WallHangingSignBlock
     * - StandingSignBlock → WallSignBlock
     * - BannerBlock → WallBannerBlock
     * - SkullBlock → WallSkullBlock
     * Returns null if the block has no wall variant.
     */
    private static Block getWallVariant(Block block) {
        // Torch → Wall Torch
        if (block == Blocks.TORCH) return Blocks.WALL_TORCH;
        if (block == Blocks.SOUL_TORCH) return Blocks.SOUL_WALL_TORCH;
        if (block == Blocks.REDSTONE_TORCH) return Blocks.REDSTONE_WALL_TORCH;

        // Hanging Sign → Wall Hanging Sign
        if (block == Blocks.OAK_HANGING_SIGN) return Blocks.OAK_WALL_HANGING_SIGN;
        if (block == Blocks.SPRUCE_HANGING_SIGN) return Blocks.SPRUCE_WALL_HANGING_SIGN;
        if (block == Blocks.BIRCH_HANGING_SIGN) return Blocks.BIRCH_WALL_HANGING_SIGN;
        if (block == Blocks.JUNGLE_HANGING_SIGN) return Blocks.JUNGLE_WALL_HANGING_SIGN;
        if (block == Blocks.ACACIA_HANGING_SIGN) return Blocks.ACACIA_WALL_HANGING_SIGN;
        if (block == Blocks.DARK_OAK_HANGING_SIGN) return Blocks.DARK_OAK_WALL_HANGING_SIGN;
        if (block == Blocks.CRIMSON_HANGING_SIGN) return Blocks.CRIMSON_WALL_HANGING_SIGN;
        if (block == Blocks.WARPED_HANGING_SIGN) return Blocks.WARPED_WALL_HANGING_SIGN;
        if (block == Blocks.MANGROVE_HANGING_SIGN) return Blocks.MANGROVE_WALL_HANGING_SIGN;
        if (block == Blocks.CHERRY_HANGING_SIGN) return Blocks.CHERRY_WALL_HANGING_SIGN;
        if (block == Blocks.BAMBOO_HANGING_SIGN) return Blocks.BAMBOO_WALL_HANGING_SIGN;

        // Standing Sign → Wall Sign
        if (block == Blocks.OAK_SIGN) return Blocks.OAK_WALL_SIGN;
        if (block == Blocks.SPRUCE_SIGN) return Blocks.SPRUCE_WALL_SIGN;
        if (block == Blocks.BIRCH_SIGN) return Blocks.BIRCH_WALL_SIGN;
        if (block == Blocks.JUNGLE_SIGN) return Blocks.JUNGLE_WALL_SIGN;
        if (block == Blocks.ACACIA_SIGN) return Blocks.ACACIA_WALL_SIGN;
        if (block == Blocks.DARK_OAK_SIGN) return Blocks.DARK_OAK_WALL_SIGN;
        if (block == Blocks.CRIMSON_SIGN) return Blocks.CRIMSON_WALL_SIGN;
        if (block == Blocks.WARPED_SIGN) return Blocks.WARPED_WALL_SIGN;
        if (block == Blocks.MANGROVE_SIGN) return Blocks.MANGROVE_WALL_SIGN;
        if (block == Blocks.CHERRY_SIGN) return Blocks.CHERRY_WALL_SIGN;
        if (block == Blocks.BAMBOO_SIGN) return Blocks.BAMBOO_WALL_SIGN;

        // Banners
        if (block == Blocks.WHITE_BANNER) return Blocks.WHITE_WALL_BANNER;
        if (block == Blocks.ORANGE_BANNER) return Blocks.ORANGE_WALL_BANNER;
        if (block == Blocks.MAGENTA_BANNER) return Blocks.MAGENTA_WALL_BANNER;
        if (block == Blocks.LIGHT_BLUE_BANNER) return Blocks.LIGHT_BLUE_WALL_BANNER;
        if (block == Blocks.YELLOW_BANNER) return Blocks.YELLOW_WALL_BANNER;
        if (block == Blocks.LIME_BANNER) return Blocks.LIME_WALL_BANNER;
        if (block == Blocks.PINK_BANNER) return Blocks.PINK_WALL_BANNER;
        if (block == Blocks.GRAY_BANNER) return Blocks.GRAY_WALL_BANNER;
        if (block == Blocks.LIGHT_GRAY_BANNER) return Blocks.LIGHT_GRAY_WALL_BANNER;
        if (block == Blocks.CYAN_BANNER) return Blocks.CYAN_WALL_BANNER;
        if (block == Blocks.PURPLE_BANNER) return Blocks.PURPLE_WALL_BANNER;
        if (block == Blocks.BLUE_BANNER) return Blocks.BLUE_WALL_BANNER;
        if (block == Blocks.BROWN_BANNER) return Blocks.BROWN_WALL_BANNER;
        if (block == Blocks.GREEN_BANNER) return Blocks.GREEN_WALL_BANNER;
        if (block == Blocks.RED_BANNER) return Blocks.RED_WALL_BANNER;
        if (block == Blocks.BLACK_BANNER) return Blocks.BLACK_WALL_BANNER;

        // Skulls / Heads
        if (block == Blocks.SKELETON_SKULL) return Blocks.SKELETON_WALL_SKULL;
        if (block == Blocks.WITHER_SKELETON_SKULL) return Blocks.WITHER_SKELETON_WALL_SKULL;
        if (block == Blocks.ZOMBIE_HEAD) return Blocks.ZOMBIE_WALL_HEAD;
        if (block == Blocks.PLAYER_HEAD) return Blocks.PLAYER_WALL_HEAD;
        if (block == Blocks.CREEPER_HEAD) return Blocks.CREEPER_WALL_HEAD;
        if (block == Blocks.DRAGON_HEAD) return Blocks.DRAGON_WALL_HEAD;
        if (block == Blocks.PIGLIN_HEAD) return Blocks.PIGLIN_WALL_HEAD;

        return null;
    }

    /**
     * Places a block at a position and restores block entity data if present.
     * Consumes one block from the inventory slot.
     *
     * @return true if the block was placed successfully
     */
    private static boolean placeBlockAt(
            Level level,
            BlockPos targetPos,
            BlockState placeState,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Block block,
            Direction worldDirection
    ) {
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            return false;
        }

        boolean isDoubleBlock = placeState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
        BlockPos abovePos = targetPos.above();

        if (isDoubleBlock) {
            if (abovePos.getY() >= level.getMaxBuildHeight() || !isPositionValidForPlacement(level, abovePos, block)) {
                return false;
            }
        }

        boolean isBed = placeState.hasProperty(BlockStateProperties.BED_PART);
        BlockPos headPos = isBed ? targetPos.relative(worldDirection) : null;
        if (isBed) {
            if (!worldDirection.getAxis().isHorizontal()
                    || headPos.getY() < level.getMinBuildHeight()
                    || headPos.getY() >= level.getMaxBuildHeight()
                    || !isPositionValidForPlacement(level, headPos, block)) {
                return false;
            }
        }

        BlockState lowerState;
        if (isDoubleBlock) {
            lowerState = placeState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        } else if (isBed) {
            lowerState = placeState
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, worldDirection)
                    .setValue(BlockStateProperties.BED_PART, BedPart.FOOT)
                    .setValue(BlockStateProperties.OCCUPIED, false);
        } else {
            lowerState = placeState;
        }

        if (lowerState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(targetPos);
            lowerState = lowerState.setValue(BlockStateProperties.WATERLOGGED, fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        boolean setResult = level.setBlock(targetPos, lowerState, 3);
        if (setResult) {
            if (isDoubleBlock) {
                BlockState upperState = placeState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                if (upperState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    FluidState fluidAbove = level.getFluidState(abovePos);
                    upperState = upperState.setValue(BlockStateProperties.WATERLOGGED, fluidAbove.is(Fluids.WATER) && fluidAbove.isSource());
                }
                level.setBlock(abovePos, upperState, 3);
            } else if (isBed && headPos != null) {
                BlockState headState = placeState
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, worldDirection)
                        .setValue(BlockStateProperties.BED_PART, BedPart.HEAD)
                        .setValue(BlockStateProperties.OCCUPIED, false);
                if (headState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    FluidState fluidHead = level.getFluidState(headPos);
                    headState = headState.setValue(BlockStateProperties.WATERLOGGED, fluidHead.is(Fluids.WATER) && fluidHead.isSource());
                }
                level.setBlock(headPos, headState, 3);
            }

            // Restore block entity data if present
            if (blockEntityNbt != null && placeState.hasBlockEntity()) {
                BlockEntity newBlockEntity = level.getBlockEntity(targetPos);
                if (newBlockEntity != null) {
                    newBlockEntity.handleUpdateTag(blockEntityNbt, level.registryAccess());
                    newBlockEntity.setChanged();
                }
            }

            // Consume one block from inventory
            itemHandler.extractItem(invSlot, 1, false);

            // Play the block-specific place sound
            net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, targetPos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }

            return true;
        }
        return false;
    }
}
