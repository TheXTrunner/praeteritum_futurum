package com.thextrunner.praeteritum_futurum;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PraeteritumFuturumMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PRAETERITUM_FUTURUM_TAB =
            //for some reason this shows no usages, as in... the variable isn't being used anywhere else?
            //and now it is being used!!!
            CREATIVE_MODE_TABS.register("praeteritum_futurum_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.praeteritum_futurum.praeteritum_tab"))
                    .icon(() -> new ItemStack(ModItems.MACHINE_FRAMEWORK_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        // so apparently the items and blocks have to be manually added here on the creative tab
                        // tests have shown me the order the items show up is respected!
                        output.accept(ModItems.ANOMALY_COMPASS.get());
                        output.accept(ModItems.SALVAGE_CUTTER.get());
                        output.accept(ModItems.YOOL.get());
                        output.accept(ModItems.SALVAGE_FRAMING_ITEM.get());
                        output.accept(ModItems.ADVANCED_SALVAGE_ITEM.get());
                        output.accept(ModItems.MACHINE_FRAMEWORK_ITEM.get());
                        output.accept(ModItems.SALVAGE_SCRAP.get());
                        output.accept(ModItems.REFINED_SCRAP.get());
                        output.accept(ModItems.STRANGE_ARTIFACT.get());
                        output.accept(ModItems.INVENTORY_MODULE.get());
                        output.accept(ModItems.IO_MODULE.get());
                        output.accept(ModItems.FILTER_MODULE.get());
                        output.accept(ModItems.SMELTING_MODULE.get());
                        output.accept(ModItems.STONECUTTER_MODULE.get());
                        output.accept(ModItems.COMPOSTER_MODULE.get());
                    })
                    .build());
}