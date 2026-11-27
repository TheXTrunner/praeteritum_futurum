package com.thextrunner.praeteritum_futurum.item.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class BlueprintTooltipHelper {
    private BlueprintTooltipHelper() {}

    static void appendMissingMaterials(BlueprintItem.MaterialRequirements requirements, List<Component> tooltip) {
        if (!Screen.hasShiftDown()) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            tooltip.add(Component.literal("§7Inventory unavailable"));
        } else if (player.isCreative()) {
            tooltip.add(Component.literal("§aCreative mode: material costs are waived."));
        } else {
            Map<Item, Integer> missing = BlueprintItem.getMissingMaterials(requirements,
                    BlueprintItem.countItemsInInventory(player), BlueprintItem.countCleanModulesInInventory(player));
            if (missing.isEmpty()) {
                tooltip.add(Component.literal("§aYou have all required materials."));
            } else {
                tooltip.add(Component.literal("§cMissing materials:"));
                for (Map.Entry<Item, Integer> entry : missing.entrySet().stream()
                        .sorted(java.util.Comparator.comparing(entry -> entry.getKey().getDescriptionId()))
                        .toList()) {
                    tooltip.add(Component.literal("§8 • §c" + entry.getValue() + "x §f"
                            + new ItemStack(entry.getKey()).getHoverName().getString()));
                }
            }
        }
    }
}