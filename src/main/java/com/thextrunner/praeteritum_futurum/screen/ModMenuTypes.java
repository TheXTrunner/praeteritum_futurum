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

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, PraeteritumFuturumMod.MODID);

    public static final Supplier<MenuType<MachineConfigMenu>> MACHINE_CONFIG_MENU =
            MENUS.register("machine_config_menu", () ->
                    IMenuTypeExtension.create(MachineConfigMenu::new)
            );

    public static final Supplier<MenuType<MachineInfoMenu>> MACHINE_INFO_MENU =
            MENUS.register("machine_info_menu", () ->
                    IMenuTypeExtension.create(MachineInfoMenu::new)
            );

    public static final DeferredHolder<MenuType<?>, MenuType<GenericModuleConfigMenu>> GENERIC_MODULE_CONFIG_MENU =
            MENUS.register("generic_module_config_menu", () ->
                    IMenuTypeExtension.create(GenericModuleConfigMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MACHINE_CONFIG_MENU.get(), MachineConfigScreen::new);
        event.register(MACHINE_INFO_MENU.get(), MachineInfoScreen::new);
        event.register(GENERIC_MODULE_CONFIG_MENU.get(), GenericModuleConfigScreen::new);
    }
}