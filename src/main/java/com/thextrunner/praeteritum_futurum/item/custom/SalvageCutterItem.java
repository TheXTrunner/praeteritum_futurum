package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.ModTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Salvage Cutter — a custom tool specialized for mining salvage blocks.
 * <p>
 * Provides increased mining speed (1.8x) on blocks tagged with salvage_blocks
 * and is recognized as the correct tool for dropping them. Takes 1 durability
 * damage per block mined.
 */
public class SalvageCutterItem extends Item {
    /**
     * Constructs the Salvage Cutter with the given properties.
     *
     * @param properties The item properties (typically with durability)
     */
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
            return 1.8f;
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