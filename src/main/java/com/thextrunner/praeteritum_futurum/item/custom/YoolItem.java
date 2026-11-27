package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Yool — a wrench-like tool for interacting with the Machine Framework.
 * <p>
 * Behaviors:
 * <ul>
 *   <li>Right-Click a Machine Framework: opens the config screen (handled by the block)</li>
 *   <li>Shift + Right-Click a Machine Framework: breaks and drops the block safely</li>
 *   <li>Right-Click directional blocks: rotates them (handled by YoolInteractHandler)</li>
 * </ul>
 * <p>
 * Stacks to 1 so each Yool is unique. The rotation feature can be disabled via config.
 */
public class YoolItem extends Item {
    /**
     * Constructs the Yool item with the given properties.
     *
     * @param properties The item properties (typically stacksTo(1))
     */
    public YoolItem(Properties properties) {
        super(properties);
    }

    /**
     * Handles tool interactions when right-clicking blocks in the world.
     */
    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // Breaks and drops the machine block when sneaking and right-clicking
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof MachineFrameworkBlockEntity) {
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    Block.popResource(level, pos, new ItemStack(ModBlocks.MACHINE_FRAMEWORK.get().asItem()));
                    level.removeBlock(pos, false);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Passes normal right clicks through so the block interaction opens the configuration menu
        return super.useOn(context);
    }
}