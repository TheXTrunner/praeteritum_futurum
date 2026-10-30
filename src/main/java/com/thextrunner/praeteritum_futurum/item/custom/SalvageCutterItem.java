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

    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull net.minecraft.world.level.Level level, @NotNull BlockState state, @NotNull net.minecraft.core.BlockPos pos, @NotNull net.minecraft.world.entity.LivingEntity miningEntity) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            stack.hurtAndBreak(1, miningEntity, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        return true;
    }
}