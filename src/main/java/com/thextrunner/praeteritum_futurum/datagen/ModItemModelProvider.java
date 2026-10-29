package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.ModItems;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PraeteritumFuturumMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.SALVAGE_SCRAP.get());
        basicItem(ModItems.REFINED_SCRAP.get());
        basicItem(ModItems.STRANGE_ARTIFACT.get());

        withExistingParent(ModItems.YOOL.getId().getPath(),
                mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/yool"));

        withExistingParent(ModItems.SALVAGE_CUTTER.getId().getPath(),
                mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/salvage_cutter"));

        withExistingParent(ModItems.INVENTORY_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/inventory_module"));

        withExistingParent(ModItems.IO_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/io_module"));

        withExistingParent(ModItems.FILTER_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/filter_module"));

        withExistingParent(ModItems.SMELTING_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/smelting_module"));

        withExistingParent(ModItems.STONECUTTER_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/stonecutter_module"));

        withExistingParent(ModItems.COMPOSTER_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/composter_module"));
    }
}