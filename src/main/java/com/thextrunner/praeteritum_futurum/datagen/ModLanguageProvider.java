package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.ModItems;
import com.thextrunner.praeteritum_futurum.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, PraeteritumFuturumMod.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        // Items
        addItem(ModItems.SALVAGE_SCRAP, "Salvage Scrap");
        addItem(ModItems.REFINED_SCRAP, "Refined Scrap");
        addItem(ModItems.STRANGE_ARTIFACT, "Strange Artifact");
        addItem(ModItems.YOOL, "Yool");
        addItem(ModItems.SALVAGE_CUTTER, "Salvage Cutter");
        addItem(ModItems.INVENTORY_MODULE, "Inventory Module");
        addItem(ModItems.IO_MODULE, "IO Module");
        addItem(ModItems.FILTER_MODULE, "Filter Module");
        addItem(ModItems.SMELTING_MODULE, "Smelting Module");
        addItem(ModItems.STONECUTTER_MODULE, "Stonecutter Module");
        addItem(ModItems.COMPOSTER_MODULE,"Composter Module");

        // Blocks
        addBlock(ModBlocks.SALVAGE_FRAMING, "Salvage Framing");
        addBlock(ModBlocks.ADVANCED_SALVAGE, "Advanced Salvage");
        addBlock(ModBlocks.MACHINE_FRAMEWORK, "Machine Framework");

        // Creative Tabs and UI
        add("itemGroup.praeteritum_futurum.praeteritum_tab", "Praeteritum Futurum");
        add("screen.praeteritum_futurum.io_config", "IO Configuration");
    }
}