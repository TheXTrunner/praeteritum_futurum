package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.ModTags;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates item tag JSON files for all mod items.
 * <p>
 * Defines the module tag hierarchy:
 * <ul>
 *   <li>machine_modules: all modules (parent tag)</li>
 *   <li>logistics_modules: Inventory, IO, Filter, Ejection, Redstone modules</li>
 *   <li>configurable_modules: modules with a config screen</li>
 *   <li>processing_modules: Smelting, Blast, Smoker, Stonecutter, Composter, Grindstone, Cartography, Anvil, Brewing, Jukebox modules</li>
 *   <li>blueprints: the Blueprint utility item</li>
 * </ul>
 */
public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTagLookup, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTagLookup, PraeteritumFuturumMod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        this.tag(ModTags.Items.MACHINE_MODULES).add(
                ModItems.INVENTORY_MODULE.get(),
                ModItems.IO_MODULE.get(),
                ModItems.FILTER_MODULE.get(),
                ModItems.SMELTING_MODULE.get(),
                ModItems.BLAST_MODULE.get(),
                ModItems.SMOKER_MODULE.get(),
                ModItems.STONECUTTER_MODULE.get(),
                ModItems.COMPOSTER_MODULE.get(),
                ModItems.GRINDSTONE_MODULE.get(),
                ModItems.CARTOGRAPHY_MODULE.get(),
                ModItems.ANVIL_MODULE.get(),
                ModItems.BREWING_MODULE.get(),
                ModItems.EJECTION_MODULE.get(),
                ModItems.JUKEBOX_MODULE.get(),
                ModItems.REDSTONE_MODULE.get()
        );

        this.tag(ModTags.Items.LOGISTICS_MODULES).add(
                ModItems.INVENTORY_MODULE.get(),
                ModItems.IO_MODULE.get(),
                ModItems.FILTER_MODULE.get(),
                ModItems.EJECTION_MODULE.get(),
                ModItems.REDSTONE_MODULE.get()
        );

        this.tag(ModTags.Items.CONFIGURABLE_MODULES).add(
                ModItems.IO_MODULE.get(),
                ModItems.FILTER_MODULE.get(),
                ModItems.SMELTING_MODULE.get(),
                ModItems.BLAST_MODULE.get(),
                ModItems.SMOKER_MODULE.get(),
                ModItems.STONECUTTER_MODULE.get(),
                ModItems.COMPOSTER_MODULE.get(),
                ModItems.GRINDSTONE_MODULE.get(),
                ModItems.CARTOGRAPHY_MODULE.get(),
                ModItems.ANVIL_MODULE.get(),
                ModItems.BREWING_MODULE.get(),
                ModItems.EJECTION_MODULE.get(),
                ModItems.JUKEBOX_MODULE.get(),
                ModItems.REDSTONE_MODULE.get()
        );

        this.tag(ModTags.Items.PROCESSING_MODULES).add(
                ModItems.SMELTING_MODULE.get(),
                ModItems.BLAST_MODULE.get(),
                ModItems.SMOKER_MODULE.get(),
                ModItems.STONECUTTER_MODULE.get(),
                ModItems.COMPOSTER_MODULE.get(),
                ModItems.GRINDSTONE_MODULE.get(),
                ModItems.CARTOGRAPHY_MODULE.get(),
                ModItems.ANVIL_MODULE.get(),
                ModItems.BREWING_MODULE.get(),
                ModItems.JUKEBOX_MODULE.get()
        );

        this.tag(ModTags.Items.BLUEPRINTS).add(
                ModItems.BLUEPRINT.get()
        );
    }
}