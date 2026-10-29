package com.thextrunner.praeteritum_futurum;

import com.mojang.logging.LogUtils;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.network.*;
import com.thextrunner.praeteritum_futurum.screen.ModMenuTypes;
import com.thextrunner.praeteritum_futurum.recipe.ModRecipeSerializers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(PraeteritumFuturumMod.MODID)
public class PraeteritumFuturumMod {
    public static final String MODID = "praeteritum_futurum";
    public static final Logger LOGGER = LogUtils.getLogger();

    // NeoForge injects ModContainer during class instantiation for lifecycle and metadata management <- DEFINITELY NOT UNUSED
    public PraeteritumFuturumMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);

        // payload handler, whatever it is
        modEventBus.addListener(this::registerPayloads);

        // Register block entity capabilities
        modEventBus.addListener(this::registerCapabilities);
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                ServerboundToggleIoFacePacket.TYPE,
                ServerboundToggleIoFacePacket.STREAM_CODEC,
                ServerboundToggleIoFacePacket::handle
        );
        registrar.playToServer(
                ServerboundReturnToMachineMenuPacket.TYPE,
                ServerboundReturnToMachineMenuPacket.STREAM_CODEC,
                ServerboundReturnToMachineMenuPacket::handle
        );
        // normalized module direction packet
        registrar.playToServer(
                ServerboundToggleModuleDirectionPacket.TYPE,
                ServerboundToggleModuleDirectionPacket.STREAM_CODEC,
                ServerboundToggleModuleDirectionPacket::handle
        );
        registrar.playToServer(
                ServerboundOpenModuleConfigPacket.TYPE,
                ServerboundOpenModuleConfigPacket.STREAM_CODEC,
                ServerboundOpenModuleConfigPacket::handle
        );
        registrar.playToServer(
                ServerboundSetModuleConfigItemPacket.TYPE,
                ServerboundSetModuleConfigItemPacket.STREAM_CODEC,
                ServerboundSetModuleConfigItemPacket::handle
        );
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MACHINE_FRAMEWORK_BE.get(),
                MachineFrameworkBlockEntity::getItemHandlerForSide
        );
    }
}