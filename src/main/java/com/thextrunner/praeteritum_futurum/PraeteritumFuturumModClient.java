package com.thextrunner.praeteritum_futurum;

import com.thextrunner.praeteritum_futurum.screen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = PraeteritumFuturumMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = PraeteritumFuturumMod.MODID)
public class PraeteritumFuturumModClient {
    public PraeteritumFuturumModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        ModMenuTypes.registerScreens(event);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        PraeteritumFuturumMod.LOGGER.info("Past meets the future...");
        PraeteritumFuturumMod.LOGGER.info("YOUR NAME >> {}", Minecraft.getInstance().getUser().getName());

        event.enqueueWork(() -> ItemProperties.register(
                ModItems.ANOMALY_COMPASS.get(),
                ResourceLocation.withDefaultNamespace("angle"),
                (ItemStack stack, ClientLevel level, LivingEntity entity, int seed) -> {
                    if (entity == null || level == null) {
                        return 0.0F;
                    }

                    CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        CompoundTag tag = customData.copyTag();
                        if (tag.getBoolean("HasTarget")) {
                            int targetX = tag.getInt("TargetX");
                            int targetZ = tag.getInt("TargetZ");

                            double angleToTarget = Math.atan2(
                                    targetZ - entity.getZ(),
                                    targetX - entity.getX()
                            );
                            double entityRotation = Math.toRadians(entity.getYRot());
// Add Math.PI / 2 to correct the ninety-degree rotational offset
                            double difference = angleToTarget - entityRotation + (Math.PI / 2);

                            return (float) Math.floorMod((long) (difference / (2 * Math.PI) * 360), 360) / 360.0F;
                        }
                    }

                    // Return 0.5 or 0.0 instead of Math.random() so it stays still when uncalibrated
                    return 0.0F;
                }
        ));
    }
}