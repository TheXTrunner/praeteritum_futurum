package com.thextrunner.praeteritum_futurum.client;

import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Client-side setup logic triggered during FMLClientSetupEvent.
 * <p>
 * This class is wired up in the main mod class via:
 * {@code modEventBus.addListener(ClientModSetup::onClientSetup)}
 * <p>
 * Handles item model overrides, custom renderers, and other client-only
 * initialization that needs access to the full Minecraft client instance.
 */
public class ClientModSetup {

    /**
     * Called by NeoForge during client setup phase.
     * <p>
     * Registers item property overrides — these allow item models to change dynamically
     * based on game state. The Anomaly Compass uses the "angle" property to rotate
     * its model toward the target structure.
     * <p>
     * The property key {@code praeteritum_futurum:angle} matches the predicate used
     * in the anomaly_compass.json item model overrides.
     */
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.ANOMALY_COMPASS.get(),
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "angle"),
                (stack, level, entity, seed) -> com.thextrunner.praeteritum_futurum.item.AnomalyCompassItem.calculateAngle(stack, level, entity)
        ));

        // Add "Contents saved" tooltip to any Machine Framework block item with saved data
        // (inventory, modules, config — any combination indicates the block was extracted with data preserved)
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent evt) -> {
            CustomData customData = evt.getItemStack().get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag itemNbt = customData.copyTag();
                CompoundTag blockEntityTag = itemNbt.getCompound("BlockEntityTag");
                // Show "Contents saved" if any Machine Framework data is present
                if (blockEntityTag.contains("Modules") || blockEntityTag.contains("Inventory")) {
                    evt.getToolTip().add(Component.translatable("tooltip.praeteritum_futurum.contents_saved").withStyle(ChatFormatting.GREEN));
                }
            }
        });
    }
}