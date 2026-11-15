package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all block breaking logic for the Block Manipulator Module.
 * <p>
 * Breaks blocks instantly (no progress tracking) and stores them in the inventory,
 * preserving block entity data (NBT) for blocks like chests, furnaces, and Machine Framework blocks.
 * <p>
 * Special handling for:
 * <ul>
 *   <li>Stacking blocks (candles, sea pickles, snow, turtle eggs, pink petals) — extracts all stacked items</li>
 *   <li>Kelp columns — breaks the entire column and stores the correct count</li>
 *   <li>Machine Framework blocks — preserves all modules, inventory, and config data</li>
 *   <li>Shulker boxes — preserves contents via CONTAINER component</li>
 *   <li>Beehives/Bee nests — uses Silk Touch to preserve bees</li>
 *   <li>Double-height blocks (doors, tall plants) — removes both halves</li>
 *   <li>Beds — removes both head and foot parts</li>
 * </ul>
 */
public class BlockManipulatorBreakingEngine {

    /**
     * Breaks a block in the world in the given world direction and stores it in a specific inventory slot.
     * Instant break for testing purposes (no progress tracking).
     * Preserves block entity data (NBT) for blocks like chests, furnaces, and Machine Framework blocks.
     */
    public static void breakBlockInSlot(
            Level level,
            BlockPos machinePos,
            MachineFrameworkBlockEntity blockEntity,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        BlockPos targetPos = machinePos.relative(worldDirection);

        // Check if there's a block to break
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.isAir() || targetState.is(Blocks.BEDROCK) || targetState.is(Blocks.BARRIER)) {
            return;
        }

        Block block = targetState.getBlock();

        // Skip indestructible portal frames — End Portal Frame, End Portal, and End Gateway cannot be broken in vanilla
        if (block == Blocks.END_PORTAL_FRAME || block == Blocks.END_PORTAL || block == Blocks.END_GATEWAY) {
            return;
        }

        // Skip redstone components — they belong in the Redstone Module, not the Block Manipulator
        if (block == Blocks.REDSTONE_WIRE || block == Blocks.REPEATER || block == Blocks.COMPARATOR ||
                block == Blocks.REDSTONE_TORCH || block == Blocks.LEVER || block instanceof ButtonBlock ||
                block instanceof RailBlock || block == Blocks.TRIPWIRE_HOOK || block == Blocks.TRIPWIRE) {
            return;
        }

        // Get the block and create the drop item
        // Use getCloneItemStack to get the correct item with block state properties (e.g., facing, axis)
        ItemStack dropStack = block.getCloneItemStack(level, targetPos, targetState);

        // For stacking blocks, set the drop count to match the stacking property value
        // so all stacked items are extracted (e.g., 4 candles drops 4 candle items)
        if (block instanceof CandleBlock) {
            int candles = targetState.getValue(CandleBlock.CANDLES);
            dropStack.setCount(candles);
        } else if (block instanceof net.minecraft.world.level.block.SeaPickleBlock) {
            int pickles = targetState.getValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES);
            dropStack.setCount(pickles);
        } else if (block instanceof net.minecraft.world.level.block.SnowLayerBlock) {
            int layers = targetState.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS);
            dropStack.setCount(layers);
        } else if (block instanceof net.minecraft.world.level.block.TurtleEggBlock) {
            int eggs = targetState.getValue(net.minecraft.world.level.block.TurtleEggBlock.EGGS);
            dropStack.setCount(eggs);
        } else if (block instanceof net.minecraft.world.level.block.PinkPetalsBlock) {
            // Look up flower_amount property by name (Parchment mappings may rename the class field)
            for (Property<?> prop : targetState.getProperties()) {
                if (prop.getName().equals("flower_amount") && prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty) {
                    dropStack.setCount(targetState.getValue((net.minecraft.world.level.block.state.properties.IntegerProperty) prop));
                    break;
                }
            }
        } else if (block instanceof net.minecraft.world.level.block.KelpBlock) {
            // Kelp: in vanilla, breaking the bottom kelp breaks the entire column above.
            // Count the full column height so we store the correct number of kelp items.
            int kelpCount = 1;
            BlockPos checkAbove = targetPos.above();
            while (checkAbove.getY() < level.getMaxBuildHeight()
                    && level.getBlockState(checkAbove).getBlock() instanceof net.minecraft.world.level.block.KelpBlock) {
                kelpCount++;
                checkAbove = checkAbove.above();
            }
            dropStack.setCount(kelpCount);
        }

        // Handle block entity data preservation.
        // - Machine Framework blocks: preserve all data (modules, inventory, config) via CUSTOM_DATA.BlockEntityTag
        // - Shulker boxes: preserve contents via CONTAINER component (vanilla behavior)
        // - Beehives/Bee nests: use getDrops() with Silk Touch to get the block with bees preserved (vanilla behavior)
        // - Banners: vanilla destroyBlock() automatically preserves patterns via data components — no custom code needed
        // All other vanilla blocks (chests, furnaces, signs, etc.) behave like normal vanilla block breaking —
        // they drop their contents as item entities and save only the block itself without data.
        BlockEntity blockEntityAtPos = level.getBlockEntity(targetPos);
        if (blockEntityAtPos instanceof MachineFrameworkBlockEntity machineBe) {
            // Preserve block state (facing, axis, etc.) so the placed block matches the original
            CompoundTag blockStateNbt = NbtUtils.writeBlockState(targetState);

            // Preserve all machine framework data (modules, inventory, config, etc.)
            CompoundTag blockEntityNbt = machineBe.saveCustomOnly(level.registryAccess());
            if (!blockEntityNbt.isEmpty()) {
                CompoundTag itemNbt = new CompoundTag();
                itemNbt.put("BlockState", blockStateNbt);
                itemNbt.put("BlockEntityTag", blockEntityNbt);
                dropStack.set(DataComponents.CUSTOM_DATA, CustomData.of(itemNbt));
            }

            // Clear the machine framework's inventory to prevent drops when the block is destroyed
            ItemStackHandler moduleHandler = machineBe.getModuleHandler();
            for (int i = 0; i < moduleHandler.getSlots(); i++) {
                moduleHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
            ItemStackHandler invHandler = machineBe.getInventoryHandler();
            for (int i = 0; i < invHandler.getSlots(); i++) {
                invHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
            machineBe.setChanged();
        } else if (block == Blocks.BEEHIVE || block == Blocks.BEE_NEST) {
            // Beehives and bee nests need Silk Touch to preserve bees inside.
            // Use getDrops() with a Silk Touch tool to simulate Silk Touch breaking.
            // Create a Silk Touch axe to simulate Silk Touch breaking
            ItemStack silkTouchTool = new ItemStack(Items.STONE_AXE);
            silkTouchTool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.SILK_TOUCH).orElseThrow(), 1);

            // Get the drops as if broken with Silk Touch
            // getDrops signature: (BlockState, ServerLevel, BlockPos, BlockEntity, Entity, ItemStack)
            List<ItemStack> drops = block.getDrops(targetState, (net.minecraft.server.level.ServerLevel) level, targetPos, blockEntityAtPos, null, silkTouchTool);
            if (!drops.isEmpty()) {
                // Replace the dropStack with the Silk Touch drop (which has bees preserved)
                dropStack = drops.get(0);
            }
        } else if (blockEntityAtPos != null) {
            // Check if this is a shulker box (preserves contents via CONTAINER component)
            if (block instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
                IItemHandler itemHandlerCap =
                        level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, null);
                if (itemHandlerCap != null) {
                    List<ItemStack> inventoryStacks = new ArrayList<>(itemHandlerCap.getSlots());
                    for (int i = 0; i < itemHandlerCap.getSlots(); i++) {
                        inventoryStacks.add(itemHandlerCap.getStackInSlot(i).copy());
                    }
                    int nonEmptyCount = 0;
                    for (ItemStack s : inventoryStacks) {
                        if (!s.isEmpty()) nonEmptyCount++;
                    }
                    if (nonEmptyCount > 0) {
                        dropStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventoryStacks));
                        // Clear the shulker box inventory to prevent drops when destroyed
                        for (int i = 0; i < itemHandlerCap.getSlots(); i++) {
                            itemHandlerCap.extractItem(i, itemHandlerCap.getStackInSlot(i).getCount(), false);
                        }
                    }
                }
            } else {
                // For other vanilla block entities (chests, furnaces, signs, banners, etc.),
                // let them drop their contents naturally via destroyBlock() — no data preservation needed.
                // Banners are handled automatically by vanilla destroyBlock() via data components.
            }
        } else {
        }

        // Try to insert into the specific inventory slot
        ItemStack remainder = itemHandler.insertItem(invSlot, dropStack, false);
        if (remainder.isEmpty()) {
        } else {
            // Inventory slot full — drop the item in the world
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                    level, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, remainder
            );
            level.addFreshEntity(itemEntity);
        }

        // Destroy the block instantly (drop=false means no block item drop)
        // For vanilla block entities, their contents are dropped as item entities by destroyBlock.
        // For machine frameworks, the inventory was already cleared above to prevent drops.
        // If this is a double-height block (doors, tall plants), also remove the other half
        if (targetState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = targetState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? targetPos.above() : targetPos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(targetState.getBlock())) {
                level.destroyBlock(otherPos, false);
            }
        }

        // If this is a bed, also remove the other half
        if (targetState.hasProperty(BlockStateProperties.BED_PART) && targetState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            net.minecraft.world.level.block.state.properties.BedPart part = targetState.getValue(BlockStateProperties.BED_PART);
            Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos otherPos = part == net.minecraft.world.level.block.state.properties.BedPart.FOOT
                    ? targetPos.relative(facing)
                    : targetPos.relative(facing.getOpposite());
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(targetState.getBlock())) {
                level.destroyBlock(otherPos, false);
            }
        }

        // If this is kelp, also remove the entire column above (vanilla behavior:
        // breaking the bottom kelp breaks the whole column)
        if (block instanceof net.minecraft.world.level.block.KelpBlock) {
            BlockPos checkAbove = targetPos.above();
            while (checkAbove.getY() < level.getMaxBuildHeight()
                    && level.getBlockState(checkAbove).getBlock() instanceof net.minecraft.world.level.block.KelpBlock) {
                level.destroyBlock(checkAbove, false);
                checkAbove = checkAbove.above();
            }
        }

        level.destroyBlock(targetPos, false);
    }
}
