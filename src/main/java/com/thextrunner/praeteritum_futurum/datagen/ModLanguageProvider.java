package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, PraeteritumFuturumMod.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        // Items
        addItem(ModItems.ANOMALY_COMPASS, "Anomaly Compass");
        addItem(ModItems.SALVAGE_SCRAP, "Salvage Scrap");
        addItem(ModItems.YOOL, "Yool");
        addItem(ModItems.SALVAGE_CUTTER, "Salvage Cutter");
        addItem(ModItems.REFINED_SCRAP, "Refined Scrap");
        addItem(ModItems.STRANGE_ARTIFACT, "Strange Artifact");

        addItem(ModItems.INVENTORY_MODULE, "Inventory Module");
        addItem(ModItems.IO_MODULE, "IO Module");
        addItem(ModItems.FILTER_MODULE, "Filter Module");
        addItem(ModItems.SMELTING_MODULE, "Smelting Module");
        addItem(ModItems.BLAST_MODULE, "Blast Module");
        addItem(ModItems.SMOKER_MODULE, "Smoker Module");
        addItem(ModItems.STONECUTTER_MODULE, "Stonecutter Module");
        addItem(ModItems.COMPOSTER_MODULE,"Composter Module");
        addItem(ModItems.GRINDSTONE_MODULE, "Grindstone Module");
        addItem(ModItems.CARTOGRAPHY_MODULE, "Cartography Module");
        addItem(ModItems.ANVIL_MODULE, "Anvil Module");
        addItem(ModItems.BREWING_MODULE, "Brewing Module");
        addItem(ModItems.EJECTION_MODULE, "Ejection Module");
        addItem(ModItems.JUKEBOX_MODULE, "Jukebox Module");

        addItem(ModItems.BLUEPRINT, "Blueprint");

        // Blocks
        addBlock(ModBlocks.SALVAGE_FRAMING, "Salvage Framing");
        addBlock(ModBlocks.ADVANCED_SALVAGE, "Advanced Salvage");
        addBlock(ModBlocks.MACHINE_FRAMEWORK, "Machine Framework");

        // Creative Tabs and UI
        add("itemGroup.praeteritum_futurum.praeteritum_tab", "Praeteritum Futurum");

        // Tags (Fixes JEI missing lang keys)
        add("tag.item.praeteritum_futurum.machine_modules", "Machine Modules");
        add("tag.item.praeteritum_futurum.configurable_modules", "Configurable Modules");
        add("tag.item.praeteritum_futurum.processing_modules", "Processing Modules");
        add("tag.item.praeteritum_futurum.logistics_modules", "Logistics Modules");
        add("tag.block.praeteritum_futurum.salvage_blocks", "Salvage Blocks");

        // Tooltips
        add("tooltip.praeteritum_futurum.configured", "Configured");
        add("tooltip.praeteritum_futurum.contents_saved", "Contents saved");

        // Config screen translations (required for NeoForge ConfigurationScreen to display settings)
        // Screen title
        add("praeteritum_futurum.configuration.title", "Praeteritum Futurum");
        
        // Config file section labels (COMMON config)
        add("praeteritum_futurum.configuration.section.praeteritum_futurum-common.toml", "Common Settings");
        add("praeteritum_futurum.configuration.section.praeteritum_futurum-common.toml.title", "Common Settings");
        
        // Section keys with dots instead of underscores (NeoForge generates both variants)
        add("praeteritum_futurum.configuration.section.praeteritum.futurum.common.toml", "Common Settings");
        add("praeteritum_futurum.configuration.section.praeteritum.futurum.common.toml.title", "Common Settings");
        
        // Categories
        add("text.autoconfig.praeteritum_futurum.category.general", "General");
        add("text.autoconfig.praeteritum_futurum.category.general.tooltip",
            "General mod settings including tool behavior");
        add("text.autoconfig.praeteritum_futurum.category.general.button",
            "General");
        add("text.autoconfig.praeteritum_futurum.category.machine", "Machine");
        add("text.autoconfig.praeteritum_futurum.category.machine.tooltip",
            "Machine module settings including tick rates and behavior");
        add("text.autoconfig.praeteritum_futurum.category.machine.button",
            "Machine");
        add("text.autoconfig.praeteritum_futurum.category.worldgen", "World Generation");
        add("text.autoconfig.praeteritum_futurum.category.worldgen.tooltip",
            "World generation settings including structure spawn rates");
        add("text.autoconfig.praeteritum_futurum.category.worldgen.button",
            "World Generation");
        add("text.autoconfig.praeteritum_futurum.category.loot", "Loot");
        add("text.autoconfig.praeteritum_futurum.category.loot.tooltip",
            "Entity loot drop settings");
        add("text.autoconfig.praeteritum_futurum.category.loot.button",
            "Loot");

        // General category options
        add("text.autoconfig.praeteritum_futurum.option.general.enableYoolRotation", "Enable Yool Rotation");
        add("text.autoconfig.praeteritum_futurum.option.general.enableYoolRotation.@", 
            "Enable the Yool wrench block rotation feature. When disabled, right-clicking with a Yool on directional blocks will do nothing.");
        add("text.autoconfig.praeteritum_futurum.option.general.enableYoolRotation.tooltip",
            "When enabled, the Yool wrench rotates directional blocks on right-click. Disable to prevent accidental rotation.");

        // Machine category options
        add("text.autoconfig.praeteritum_futurum.option.machine.ioTransferTickRate", "IO Transfer Tick Rate");
        add("text.autoconfig.praeteritum_futurum.option.machine.ioTransferTickRate.@",
            "How many ticks between IO module item transfer attempts. Higher values = slower transfers but less server load. Default: 8 ticks between attempts (hopper cooldown).");
        add("text.autoconfig.praeteritum_futurum.option.machine.ioTransferTickRate.tooltip",
            "How many ticks between IO module item transfer attempts. Higher values = slower transfers but less server load. Default: 8 ticks between attempts (hopper cooldown).");
        add("text.autoconfig.praeteritum_futurum.option.machine.anvilMaxRepairCost", "Anvil Max Repair Cost");
        add("text.autoconfig.praeteritum_futurum.option.machine.anvilMaxRepairCost.@",
            "Maximum repair cost for Anvil module operations. 0 = no limit (default), 40 = vanilla behavior.");
        add("text.autoconfig.praeteritum_futurum.option.machine.anvilMaxRepairCost.tooltip",
            "Maximum repair cost for Anvil module operations. 0 = no limit (default), 40 = vanilla behavior.");
        add("text.autoconfig.praeteritum_futurum.option.machine.enableDebugLogging", "Enable Debug Logging");
        add("text.autoconfig.praeteritum_futurum.option.machine.enableDebugLogging.@",
            "Enable verbose debug logging for all machine modules. Generates a large amount of log output — only enable for debugging.");
        add("text.autoconfig.praeteritum_futurum.option.machine.enableDebugLogging.tooltip",
            "Enable verbose debug logging for all machine modules. Generates a large amount of log output — only enable for debugging.");

        // Worldgen category options
        add("text.autoconfig.praeteritum_futurum.option.worldgen.ruinedOutpostSpawnRate", "Ruined Outpost Spawn Rate");
        add("text.autoconfig.praeteritum_futurum.option.worldgen.ruinedOutpostSpawnRate.@",
            "Multiplier for Ruined Outpost structure spawn rate. 1.0 = default, 2.0 = twice as many, 0.5 = half as many, 0.0 = disabled.");
        add("text.autoconfig.praeteritum_futurum.option.worldgen.ruinedOutpostSpawnRate.tooltip",
            "Adjusts how often Ruined Outposts appear. Set to 0 to disable. Changes apply only at world creation.");

        // Loot category options
        add("text.autoconfig.praeteritum_futurum.option.loot.enableHuskSandDrop", "Enable Husk Sand Drop");
        add("text.autoconfig.praeteritum_futurum.option.loot.enableHuskSandDrop.@",
            "Enable sand drops from Husks. Husks live in deserts, so sand is their biome signature drop.");
        add("text.autoconfig.praeteritum_futurum.option.loot.enableHuskSandDrop.tooltip",
            "Husks will occasionally drop sand on death, reflecting their desert biome origin.");
        add("text.autoconfig.praeteritum_futurum.option.loot.huskSandDropChance", "Husk Sand Drop Chance");
        add("text.autoconfig.praeteritum_futurum.option.loot.huskSandDropChance.@",
            "Chance for Husks to drop sand (0.0 to 1.0). 0.025 = 2.5% chance (matches iron/carrot/potato rarity).");
        add("text.autoconfig.praeteritum_futurum.option.loot.huskSandDropChance.tooltip",
            "Probability per Husk death that sand will drop. Default 2.5% matches rarity of other Husk drops.");
        add("text.autoconfig.praeteritum_futurum.option.loot.huskSandDropMin", "Husk Sand Drop Min");
        add("text.autoconfig.praeteritum_futurum.option.loot.huskSandDropMin.@",
            "Minimum sand count dropped by Husks when the drop triggers.");
        add("text.autoconfig.praeteritum_futurum.option.loot.huskSandDropMin.tooltip",
            "Fewest sand items dropped when a Husk's sand drop triggers. Paired with the max value for a random range.");
        add("text.autoconfig.praeteritum_futurum.option.loot.huskSandDropMax", "Husk Sand Drop Max");
        add("text.autoconfig.praeteritum_futurum.option.loot.huskSandDropMax.@",
            "Maximum sand count dropped by Husks when the drop triggers.");
        add("text.autoconfig.praeteritum_futurum.option.loot.huskSandDropMax.tooltip",
            "Most sand items dropped when a Husk's sand drop triggers. Paired with the min value for a random range.");

        // Generic text input screen (used by any module implementing ITextInputModule)
        add("screen.praeteritum_futurum.text_input.save", "Save");
        add("screen.praeteritum_futurum.text_input.clear", "Clear");
        add("screen.praeteritum_futurum.text_input.saved", "Name saved");
        add("screen.praeteritum_futurum.text_input.cleared", "Name cleared");

        // Anvil name input screen
        add("screen.praeteritum_futurum.anvil_name_input.title", "Set Item Name");
        add("screen.praeteritum_futurum.anvil_name_input.hint", "Enter name...");

        // Machine lock toggle tooltips
        add("tooltip.praeteritum_futurum.machine_public", "Public: Anyone can apply blueprints");
        add("tooltip.praeteritum_futurum.machine_private", "Private: Only the owner can apply blueprints");
    }
}
