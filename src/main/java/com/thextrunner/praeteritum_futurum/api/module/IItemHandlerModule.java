package com.thextrunner.praeteritum_futurum.api.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;

/**
 * Interface for modules that expose an item handler to IO modules for item transfer.
 * <p>
 * When a module implements this interface, adjacent IO modules can pull items into
 * and push items out of the module's exposed item handler. This enables modules
 * like the Jukebox Module to receive and return items through the standard IO module
 * system, without requiring an Inventory module intermediary.
 * <p>
 * The framework calls {@link #getItemHandlerForSide} during IO transfer to get the
 * target item handler. If the method returns {@code null}, the module is skipped
 * for that transfer direction.
 * <p>
 * Implementing classes: JukeboxModuleItem.
 */
public interface IItemHandlerModule {

    /**
     * Returns the item handler exposed by this module for item transfer on the given side.
     * <p>
     * Called by the machine framework when an adjacent IO module attempts to pull or
     * push items. The returned handler is used as the source or target of the transfer,
     * depending on the IO module's face mode (INPUT/OUTPUT).
     * <p>
     * Return {@code null} if the module does not expose an item handler on this side
     * or does not support item transfer at all.
     *
     * @param level       The world level
     * @param pos         The world position of the machine framework block
     * @param blockEntity The machine framework block entity
     * @param slotIndex   The grid slot index of this module
     * @param stack       The module item stack
     * @param side        The direction from which the IO module is accessing this module
     * @return The item handler for transfer, or {@code null} if not supported
     */
    @Nullable
    IItemHandlerModifiable getItemHandlerForSide(
            Level level,
            BlockPos pos,
            MachineFrameworkBlockEntity blockEntity,
            int slotIndex,
            ItemStack stack,
            Direction side
    );
}
