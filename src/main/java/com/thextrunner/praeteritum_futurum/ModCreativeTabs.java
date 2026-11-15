package com.thextrunner.praeteritum_futurum;

import com.thextrunner.praeteritum_futurum.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Defines the mod's Creative Mode tab — the inventory page players see
 * when they open creative mode and select the Praeteritum Futurum tab.
 * <p>
 * The DeferredHolder may appear unused in IDE analysis, but it's essential:
 * registering it with CREATIVE_MODE_TABS tells NeoForge to create the tab,
 * and the tab is referenced by its registry name ("praeteritum_futurum_tab")
 * in the language file for display.
 */
public class ModCreativeTabs {
    // DeferredRegister for creative tabs, tied to our mod ID
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PraeteritumFuturumMod.MODID);

    /**
     * The mod's creative tab. Items are added manually in the order they should
     * appear — the displayItems callback respects insertion order, so items listed
     * first appear at the top-left of the tab.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PRAETERITUM_FUTURUM_TAB =
            CREATIVE_MODE_TABS.register("praeteritum_futurum_tab", () -> CreativeModeTab.builder()
                    // Tab title is translated via lang file: "itemGroup.praeteritum_futurum.praeteritum_tab"
                    .title(Component.translatable("itemGroup.praeteritum_futurum.praeteritum_tab"))
                    // Tab icon — the Machine Framework item model
                    .icon(() -> new ItemStack(ModItems.MACHINE_FRAMEWORK_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        // Items are added in display order (top-to-bottom, left-to-right).
                        // Each output.accept() call adds one item to the tab.

                        // Tools and utilities
                        output.accept(ModItems.ANOMALY_COMPASS.get());
                        output.accept(ModItems.SALVAGE_CUTTER.get());
                        output.accept(ModItems.YOOL.get());

                        // Blocks (as pickable items)
                        output.accept(ModItems.SALVAGE_FRAMING_ITEM.get());
                        output.accept(ModItems.ADVANCED_SALVAGE_ITEM.get());
                        output.accept(ModItems.MACHINE_FRAMEWORK_ITEM.get());

                        // Materials
                        output.accept(ModItems.SALVAGE_SCRAP.get());
                        output.accept(ModItems.REFINED_SCRAP.get());
                        output.accept(ModItems.STRANGE_ARTIFACT.get());

                        // Modules
                        output.accept(ModItems.INVENTORY_MODULE.get());
                        output.accept(ModItems.IO_MODULE.get());
                        output.accept(ModItems.FILTER_MODULE.get());
                        output.accept(ModItems.SMELTING_MODULE.get());
                        output.accept(ModItems.STONECUTTER_MODULE.get());
                        output.accept(ModItems.COMPOSTER_MODULE.get());
                        output.accept(ModItems.GRINDSTONE_MODULE.get());
                        output.accept(ModItems.CARTOGRAPHY_MODULE.get());
                        output.accept(ModItems.ANVIL_MODULE.get());
                        output.accept(ModItems.BREWING_MODULE.get());
                        output.accept(ModItems.REDSTONE_MODULE.get());

                        // Utility items
                        output.accept(ModItems.BLUEPRINT.get());
                    })
                    .build());
}