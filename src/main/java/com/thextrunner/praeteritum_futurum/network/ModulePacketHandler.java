package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Shared handler utility for serverbound packets that modify module stack NBT.
 * <p>
 * Most module config packets follow the same pattern:
 * <ol>
 *   <li>Validate the player is within reach distance</li>
 *   <li>Find the Machine Framework block entity at the target position</li>
 *   <li>Get the module stack from the specified slot</li>
 *   <li>Optionally check the module item type</li>
 *   <li>Modify the stack's CustomData (NBT)</li>
 *   <li>Write the modified stack back to the moduleHandler</li>
 *   <li>Mark the block entity as changed for persistence</li>
 * </ol>
 * <p>
 * This utility encapsulates steps 1-3 and 6-7, leaving steps 4-5 (type check and
 * modification) to the caller. Each packet's {@code handle()} method becomes a single
 * call to this utility instead of 15+ lines of boilerplate.
 */
public final class ModulePacketHandler {

    private ModulePacketHandler() {
        // Utility class, no instantiation
    }

    /**
     * Handles a serverbound packet that modifies a module stack's NBT.
     * <p>
     * Validates the player's distance, finds the block entity, retrieves the module stack,
     * runs the provided modifier, writes the stack back, and marks the block entity dirty.
     *
     * @param player the server player sending the packet
     * @param blockPos the position of the Machine Framework block
     * @param slotIndex the module slot index
     * @param stackModifier the action to perform on the module stack (e.g., toggle a flag, set a value)
     * @param actionName short description for the warning log if the player is too far
     */
    public static void handleAtBlock(ServerPlayer player, BlockPos blockPos, int slotIndex,
                                     Consumer<ItemStack> stackModifier, String actionName) {
        if (player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > 64.0) {
            PraeteritumFuturumMod.LOGGER.warn("Player {} tried to {} from too far away at {}",
                    player.getName().getString(), actionName, blockPos);
            return;
        }
        if (player.level().getBlockEntity(blockPos) instanceof MachineFrameworkBlockEntity blockEntity) {
            if (slotIndex < 0 || slotIndex >= blockEntity.getModuleHandler().getSlots()) return;
            ItemStack stack = blockEntity.getModuleHandler().getStackInSlot(slotIndex);
            stackModifier.accept(stack);
            blockEntity.getModuleHandler().setStackInSlot(slotIndex, stack);
            blockEntity.setChanged();
        }
    }

    /**
     * Handles a serverbound packet that modifies a module stack's NBT, with a type check.
     * <p>
     * Same as {@link #handleAtBlock} but additionally checks that the module stack's item
     * matches the provided predicate before running the modifier.
     *
     * @param player the server player sending the packet
     * @param blockPos the position of the Machine Framework block
     * @param slotIndex the module slot index
     * @param typeCheck predicate to validate the module item type (e.g., {@code BaseModuleItem.class::isInstance})
     * @param stackModifier the action to perform on the module stack
     * @param actionName short description for the warning log if the player is too far
     */
    public static void handleAtBlock(ServerPlayer player, BlockPos blockPos, int slotIndex,
                                     Predicate<Item> typeCheck,
                                     Consumer<ItemStack> stackModifier, String actionName) {
        if (player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > 64.0) {
            PraeteritumFuturumMod.LOGGER.warn("Player {} tried to {} from too far away at {}",
                    player.getName().getString(), actionName, blockPos);
            return;
        }
        if (player.level().getBlockEntity(blockPos) instanceof MachineFrameworkBlockEntity blockEntity) {
            if (slotIndex < 0 || slotIndex >= blockEntity.getModuleHandler().getSlots()) return;
            ItemStack stack = blockEntity.getModuleHandler().getStackInSlot(slotIndex);
            if (!stack.isEmpty() && typeCheck.test(stack.getItem())) {
                stackModifier.accept(stack);
                blockEntity.getModuleHandler().setStackInSlot(slotIndex, stack);
                blockEntity.setChanged();
            }
        }
    }
}
