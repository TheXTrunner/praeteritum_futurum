package com.thextrunner.praeteritum_futurum.item;

import com.thextrunner.praeteritum_futurum.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

            // Use the structure tag from ModTags to find all structures in the tag.
            // Since ConfigDrivenRandomSpreadPlacement extends RandomSpreadStructurePlacement,
            // vanilla's findNearestMapStructure can now search for our structures.
            var structureLookup = serverLevel.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            HolderSet<Structure> holderSet = structureLookup.getOrThrow(ModTags.Structures.RUINED_OUTPOST);

            List<BlockPos> foundStructures = new ArrayList<>();

            // Search in 4 directions from the player to find nearby structures.
            // With high spawn rates, structures can be every few chunks, so we need
            // to search in multiple directions rather than jumping far outward.
            int[] searchDirsX = {0, 1, 0, -1};
            int[] searchDirsZ = {1, 0, -1, 0};

            for (int dir = 0; dir < 4; dir++) {
                // Search along each direction, stepping outward in increments
                for (int step = 0; step < 5; step++) {
                    int searchDist = step * 120; // 120 blocks per step, up to 480 blocks total
                    BlockPos searchPos;
                    if (searchDist == 0) {
                        searchPos = playerPos;
                    } else {
                        searchPos = playerPos.offset(searchDirsX[dir] * searchDist, 0, searchDirsZ[dir] * searchDist);
                    }

                    var structureResult = serverLevel.getChunkSource().getGenerator().findNearestMapStructure(
                            serverLevel,
                            holderSet,
                            searchPos,
                            80,
                            false
                    );
                    if (structureResult != null) {
                        BlockPos foundPos = structureResult.getFirst();

                        // Skip if too far from player (max ~500 blocks)
                        if (foundPos.distSqr(playerPos) > 250000) continue;

                        // Skip if too close to an already found structure (within 50 blocks)
                        boolean duplicate = false;
                        for (BlockPos existing : foundStructures) {
                            if (existing.distSqr(foundPos) < 2500) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            foundStructures.add(foundPos);
                        }
                    }
                }
            }

            // Sort by distance so the compass locks onto the nearest structure first
            foundStructures.sort(Comparator.comparingDouble(pos -> pos.distSqr(playerPos)));

            // Keep only the closest 3 structures to avoid clutter
            while (foundStructures.size() > 3) {
                foundStructures.remove(foundStructures.size() - 1);
            }

            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                boolean isCycling = player.isShiftKeyDown();
                int targetCount = tag.getInt("TargetCount");
                int currentIndex = tag.getInt("TargetIndex");

                if (isCycling && targetCount > 0) {
                    currentIndex = (currentIndex + 1) % targetCount;
                    int newX = tag.getInt("TargetX_" + currentIndex);
                    int newZ = tag.getInt("TargetZ_" + currentIndex);
                    tag.putInt("TargetIndex", currentIndex);
                    tag.putInt("TargetX", newX);
                    tag.putInt("TargetZ", newZ);
                    tag.putBoolean("HasTarget", true);
                } else if (!foundStructures.isEmpty()) {
                    tag.putInt("TargetCount", foundStructures.size());
                    tag.putInt("TargetIndex", 0);
                    for (int i = 0; i < foundStructures.size(); i++) {
                        BlockPos p = foundStructures.get(i);
                        tag.putInt("TargetX_" + i, p.getX());
                        tag.putInt("TargetZ_" + i, p.getZ());
                    }
                    tag.putInt("TargetX", foundStructures.get(0).getX());
                    tag.putInt("TargetZ", foundStructures.get(0).getZ());
                    tag.putBoolean("HasTarget", true);
                } else {
                    tag.putBoolean("HasTarget", false);
                    tag.putInt("TargetCount", 0);
                }
            });

            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                if (tag.getBoolean("HasTarget")) {
                    int count = tag.getInt("TargetCount");
                    int idx = tag.getInt("TargetIndex") + 1;
                    if (player.isShiftKeyDown() && count > 1) {
                        player.displayClientMessage(Component.literal("Anomaly Signal Cycled: Target " + idx + " / " + count), true);
                    } else {
                        player.displayClientMessage(Component.literal("Anomaly Signal Calibrated! Target locked (" + count + " nearby)."), true);
                    }
                } else {
                    player.displayClientMessage(Component.literal("No anomalous signatures detected nearby..."), true);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static float calculateAngle(ItemStack stack, @org.jetbrains.annotations.Nullable Level level, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity entity) {
        if (level == null && entity != null) {
            level = entity.level();
        }
        if (level == null) {
            return 0.0F;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0.0F;
        }

        net.minecraft.nbt.CompoundTag tag = customData.copyTag();
        if (!tag.contains("HasTarget") || !tag.getBoolean("HasTarget")) {
            return 0.0F;
        }

        int targetX = tag.getInt("TargetX");
        int targetZ = tag.getInt("TargetZ");

        BlockPos entityPos = entity != null ? entity.blockPosition() : new BlockPos(0, 0, 0);
        double d3 = Math.atan2((double) targetZ + 0.5 - (entityPos.getZ() + 0.5), (double) targetX + 0.5 - (entityPos.getX() + 0.5));

        double angle = 0.0;
        if (entity != null) {
            float f = entity.getYRot();
            float f1 = net.minecraft.util.Mth.positiveModulo(f / 360.0F, 1.0F);
            angle = 0.5 - (f1 - 0.25 - (d3 / (Math.PI * 2)));
        } else {
            double d2 = Math.atan2(targetZ - entityPos.getZ(), targetX - entityPos.getX());
            angle = 0.5 - (d2 / (Math.PI * 2) - 0.25);
        }

        return net.minecraft.util.Mth.positiveModulo((float) angle + 0.5F, 1.0F);
    }
}