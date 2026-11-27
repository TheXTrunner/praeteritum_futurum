package com.thextrunner.praeteritum_futurum;

import com.mojang.logging.LogUtils;
import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.block.entity.ModBlockEntities;
import com.thextrunner.praeteritum_futurum.gametest.BlueprintItemGameTests;
import com.thextrunner.praeteritum_futurum.gametest.EjectionModuleGameTests;
import com.thextrunner.praeteritum_futurum.gametest.MachineFrameworkBlockEntityGameTests;
import com.thextrunner.praeteritum_futurum.gametest.ModuleMenuProviderGameTests;
import com.thextrunner.praeteritum_futurum.gametest.ProcessingModuleGameTests;
import com.thextrunner.praeteritum_futurum.gametest.RedstoneCircuitGameTests;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.loot.ModLootModifiers;
import com.thextrunner.praeteritum_futurum.network.*;
import com.thextrunner.praeteritum_futurum.recipe.DataCleaningRecipe;
import com.thextrunner.praeteritum_futurum.recipe.ModRecipeSerializers;
import com.thextrunner.praeteritum_futurum.screen.ModMenuTypes;
import com.thextrunner.praeteritum_futurum.worldgen.ModStructurePlacements;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

/**
 * Main mod entry point for Praeteritum Futurum.
 * <p>
 * The @Mod annotation tells NeoForge this is the main class for the mod with the given ID.
 * When the game loads, NeoForge instantiates this class and passes the mod's event bus,
 * which we use to register all our content (blocks, items, block entities, etc.).
 * <p>
 * Registration flow:
 * 1. DeferredRegister objects (in ModBlocks, ModItems, etc.) hold lazy definitions
 * 2. We call .register(modEventBus) to attach them to NeoForge's lifecycle
 * 3. NeoForge creates the actual objects at the right time during startup
 */
@Mod(PraeteritumFuturumMod.MODID)
public class PraeteritumFuturumMod {
    public static final String MODID = "praeteritum_futurum";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Mod constructor — called by NeoForge during mod loading.
     * <p>
     * NeoForge injects both parameters:
     * - modEventBus: used to register content and listen for mod lifecycle events
     * - modContainer: provides mod metadata and config handling (kept for lifecycle management)
     */
    public PraeteritumFuturumMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the mod's config system — this tells NeoForge to create the TOML
        // config file and load any user-edited values. The config spec is defined
        // in Config.java with defaults and validation ranges.
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register all deferred content with the mod event bus.
        // Each of these attaches the DeferredRegister to NeoForge's lifecycle so blocks,
        // items, etc. are created and registered at the correct startup phase.
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);

        // Register network payload handlers — these define how packets are encoded,
        // decoded, and processed when sent between client and server.
        modEventBus.addListener(this::registerPayloads);

        // Register capabilities — this tells NeoForge how block entities expose
        // their functionality (e.g., item handler for pipe compatibility).
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerGameTests);
        NeoForge.EVENT_BUS.addListener(DataCleaningRecipe::returnRedstoneComponents);

        // Register the custom structure placement type so it can be referenced
        // in structure_set JSON files. This allows the config-driven spawn rate
        // to control how frequently structures appear in the world.
        ModStructurePlacements.register(modEventBus);

        // Register loot modifier serializers — these define how custom loot modifiers
        // are serialized/deserialized from JSON loot modifier files.
        ModLootModifiers.GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
    }

    /**
     * Registers all network packets for client-server communication.
     * <p>
     * Each packet is registered with:
     * - A unique TYPE identifier (ResourceLocation + packet class)
     * - A STREAM_CODEC that knows how to serialize/deserialize the packet data
     * - A handler method that processes the packet on the receiving side
     * <p>
     * All packets here are playToServer, meaning they're sent from the client
     * when the player interacts with the machine UI, and handled on the server.
     */
    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        // The registrar is created with a protocol version string. If the client and
        // server versions don't match, the connection is rejected.
        // IMPORTANT: Increment this version whenever packet structure changes are made
        // (adding/removing fields, changing field types, or adding/removing packets).
        // Current version "2" — includes the redstone-control configuration payload.
        final PayloadRegistrar registrar = event.registrar("2");

        // Toggle which face of the machine is set as input/output
        registrar.playToServer(
                ServerboundToggleIoFacePacket.TYPE,
                ServerboundToggleIoFacePacket.STREAM_CODEC,
                ServerboundToggleIoFacePacket::handle
        );

        // Return from a module's config screen to the main machine menu
        registrar.playToServer(
                ServerboundReturnToMachineMenuPacket.TYPE,
                ServerboundReturnToMachineMenuPacket.STREAM_CODEC,
                ServerboundReturnToMachineMenuPacket::handle
        );

        // Change the direction a module is facing (up/down/left/right)
        registrar.playToServer(
                ServerboundToggleModuleDirectionPacket.TYPE,
                ServerboundToggleModuleDirectionPacket.STREAM_CODEC,
                ServerboundToggleModuleDirectionPacket::handle
        );

        // Open the configuration screen for a specific module
        registrar.playToServer(
                ServerboundOpenModuleConfigPacket.TYPE,
                ServerboundOpenModuleConfigPacket.STREAM_CODEC,
                ServerboundOpenModuleConfigPacket::handle
        );

        // Set the filter/config item for a module (e.g., which item to filter)
        registrar.playToServer(
                ServerboundSetModuleConfigItemPacket.TYPE,
                ServerboundSetModuleConfigItemPacket.STREAM_CODEC,
                ServerboundSetModuleConfigItemPacket::handle
        );

        // Toggle between whitelist and blacklist mode on a filter module
        registrar.playToServer(
                ServerboundToggleFilterModePacket.TYPE,
                ServerboundToggleFilterModePacket.STREAM_CODEC,
                ServerboundToggleFilterModePacket::handle
        );

        registrar.playToServer(
                ServerboundCycleRedstoneControlPacket.TYPE,
                ServerboundCycleRedstoneControlPacket.STREAM_CODEC,
                ServerboundCycleRedstoneControlPacket::handle
        );

        // Set text on a module implementing ITextInputModule (generic, used by Anvil and others)
        registrar.playToServer(
                ServerboundSetModuleTextPacket.TYPE,
                ServerboundSetModuleTextPacket.STREAM_CODEC,
                ServerboundSetModuleTextPacket::handle
        );

        // Toggle the public/private lock on a Machine Framework
        registrar.playToServer(
                ServerboundToggleMachinePublicPacket.TYPE,
                ServerboundToggleMachinePublicPacket.STREAM_CODEC,
                ServerboundToggleMachinePublicPacket::handle
        );

        // Reset a module's direction config when the adjacent module is no longer valid
        registrar.playToServer(
                ServerboundResetModuleDirectionPacket.TYPE,
                ServerboundResetModuleDirectionPacket.STREAM_CODEC,
                ServerboundResetModuleDirectionPacket::handle
        );

        // Send computed redstone circuit state from server to client for rendering
        registrar.playToClient(
                ClientboundRedstoneCircuitStatePacket.TYPE,
                ClientboundRedstoneCircuitStatePacket.STREAM_CODEC,
                ClientboundRedstoneCircuitStatePacket::handle
        );

        // Configure redstone component (toggle lever, rotate, cycle attachment) in Edit Mode
        registrar.playToServer(
                ServerboundRedstoneConfigPacket.TYPE,
                ServerboundRedstoneConfigPacket.STREAM_CODEC,
                ServerboundRedstoneConfigPacket::handle
        );
    }

    private void registerGameTests(RegisterGameTestsEvent event) {
        event.register(BlueprintItemGameTests.class);
        event.register(RedstoneCircuitGameTests.class);
        event.register(ProcessingModuleGameTests.class);
        event.register(EjectionModuleGameTests.class);
        event.register(MachineFrameworkBlockEntityGameTests.class);
        event.register(ModuleMenuProviderGameTests.class);
    }

    /**
     * Registers block entity capabilities for integration with other mods.
     * <p>
     * This exposes the Machine Framework's item handler to the NeoForge capability system,
     * allowing other mods (like pipes or hoppers) to interact with the machine's inventory
     * through the standard ItemHandler API.
     */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,           // The capability type: item handler on block faces
                ModBlockEntities.MACHINE_FRAMEWORK_BE.get(), // The block entity class to apply it to
                MachineFrameworkBlockEntity::getItemHandlerForSide // Method that returns the item handler for a given face
        );
    }

}
