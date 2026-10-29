package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.ModTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class SalvageCutterItem extends Item {
    public SalvageCutterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isCorrectToolForDrops(@NotNull ItemStack stack, BlockState state) {
        return state.is(ModTags.Blocks.SALVAGE_BLOCKS);
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, BlockState state) {
        if (state.is(ModTags.Blocks.SALVAGE_BLOCKS)) {
            return 1.5f;
        }
        return 1.0f;
    }
}