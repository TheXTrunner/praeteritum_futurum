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

public class YoolItem extends Item {
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