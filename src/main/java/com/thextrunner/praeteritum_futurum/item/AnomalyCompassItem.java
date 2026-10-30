package com.thextrunner.praeteritum_futurum.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class AnomalyCompassItem extends Item {
    public AnomalyCompassItem(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos playerPos = player.blockPosition();

            var structureRegistry = serverLevel.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            var structureHolder = structureRegistry.get(
                    ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("praeteritum_futurum", "ruined_outpost"))
            );

            BlockPos outpostPos = null;
            if (structureHolder.isPresent()) {
                HolderSet<net.minecraft.world.level.levelgen.structure.Structure> holderSet = HolderSet.direct(structureHolder.get());
                var structureResult = serverLevel.getChunkSource().getGenerator().findNearestMapStructure(
                        serverLevel,
                        holderSet,
                        playerPos,
                        100,
                        false
                );
                if (structureResult != null) {
                    outpostPos = structureResult.getFirst();
                }
            }

            if (outpostPos != null) {
                BlockPos finalOutpostPos = outpostPos;
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt("TargetX", finalOutpostPos.getX());
                    tag.putInt("TargetZ", finalOutpostPos.getZ());
                    tag.putBoolean("HasTarget", true);
                });

                player.displayClientMessage(Component.literal("Anomaly Signal Calibrated! Target locked."), true);
            } else {
                player.displayClientMessage(Component.literal("No anomalous signatures detected nearby..."), true);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}