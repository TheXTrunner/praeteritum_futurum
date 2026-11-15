package com.thextrunner.praeteritum_futurum.event;

import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = PraeteritumFuturumMod.MODID)
public class YoolInteractHandler {

    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());

        if (stack.is(ModItems.YOOL.get())) {
            Level level = event.getLevel();
            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);

            // Skips machine frameworks to preserve menu interaction behavior
            if (state.getBlock() instanceof MachineFrameworkBlock) {
                return;
            }

            // Cycles hopper states to support downward and horizontal facings
            if (state.is(Blocks.HOPPER)) {
                if (!level.isClientSide()) {
                    BlockState newState = state.cycle(HopperBlock.FACING);
                    level.setBlock(pos, newState, 3);
                    level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 1.0F);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }

            // Rotates general directional blocks counterclockwise
            // This feature can be disabled via config: Config.General.ENABLE_YOOL_ROTATION
            if (state.hasProperty(BlockStateProperties.FACING) || state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                // Check if Yool rotation is enabled in the config (use safe accessor to avoid NPE before spec is built)
                if (!Config.isYoolRotationEnabled()) {
                    return;
                }
                if (!level.isClientSide()) {
                    BlockState newState = state.rotate(Rotation.COUNTERCLOCKWISE_90);
                    level.setBlock(pos, newState, 3);
                    level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 1.0F);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }
}