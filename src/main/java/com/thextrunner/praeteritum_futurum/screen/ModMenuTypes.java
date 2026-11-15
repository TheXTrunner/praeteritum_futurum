package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/**
 * Registers all GUI Menu Types and their corresponding screen renderers.
 * <p>
 * In NeoForge, a Menu (server-side) defines the inventory logic and slot layout,
 * while a Screen (client-side) defines how the GUI is drawn. They're paired together
 * here: each MenuType is registered with its Screen class.
 * <p>
 * Flow: Player right-clicks a block → server opens a Menu → client receives the Menu
 * and creates the matching Screen to display it.
 */
public class ModMenuTypes {
    // DeferredRegister for menu types, tied to our mod ID
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, PraeteritumFuturumMod.MODID);

    /**
     * Machine Config Menu — the main machine interface where players manage modules,
     * view inventory, and configure IO faces. Opened by right-clicking a Machine Framework.
     */
    public static final Supplier<MenuType<MachineConfigMenu>> MACHINE_CONFIG_MENU =
            MENUS.register("machine_config_menu", () ->
                    IMenuTypeExtension.create(MachineConfigMenu::new)
            );

    /**
     * Machine Info Menu — a simpler info screen showing machine status details.
     */
    public static final Supplier<MenuType<MachineInfoMenu>> MACHINE_INFO_MENU =
            MENUS.register("machine_info_menu", () ->
                    IMenuTypeExtension.create(MachineInfoMenu::new)
            );

    /**
     * Generic Module Config Menu — used for configuring individual modules
     * (e.g., setting filter items, toggling modes). Shared across module types.
     * Third-party modules can use this menu type for their custom module screens.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<GenericModuleConfigMenu>> GENERIC_MODULE_CONFIG_MENU =
            MENUS.register("generic_module_config_menu", () ->
                    IMenuTypeExtension.create(GenericModuleConfigMenu::new));

    /**
     * Redstone Module Config Menu — dedicated menu type for the Redstone Module
     * circuit editor. Uses RedstoneConfigMenu for double-chest layout (54 slots)
     * and RedstoneConfigScreen to render rotation and wall attachment overlays.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<RedstoneConfigMenu>> REDSTONE_MODULE_CONFIG_MENU =
            MENUS.register("redstone_module_config_menu", () ->
                    IMenuTypeExtension.create(RedstoneConfigMenu::new));

    /**
     * Registers the menu types with the mod event bus.
     * Called from PraeteritumFuturumMod's constructor.
     */
    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    /**
     * Registers the client-side screen renderers for each menu type.
     * Called from ClientModSetup on the client only. This links each MenuType
     * to its visual Screen class so the GUI renders correctly.
     */
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MACHINE_CONFIG_MENU.get(), MachineConfigScreen::new);
        event.register(MACHINE_INFO_MENU.get(), MachineInfoScreen::new);
        event.register(GENERIC_MODULE_CONFIG_MENU.get(), GenericModuleConfigScreen::new);
        event.register(REDSTONE_MODULE_CONFIG_MENU.get(), RedstoneConfigScreen::new);
    }
}