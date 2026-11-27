package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Generates item model JSON files for all mod items.
 * <p>
 * Registers flat (generated) item models for modules, materials, and utility items.
 * The Anomaly Compass uses a special multi-layer override system with 32 frames
 * for its rotating animation. Handheld items (Yool, Salvage Cutter) use the
 * handheld transform parent. The Machine Framework item model is hand-crafted
 * in src/main/resources/ and not generated here.
 */
public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PraeteritumFuturumMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ItemModelBuilder compassBuilder = withExistingParent(ModItems.ANOMALY_COMPASS.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/anomaly_compass_00"));

        for (int i = 1; i < 32; i++) {
            String frameName = String.format("anomaly_compass_%02d", i);

            withExistingParent(frameName, mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/" + frameName));

            compassBuilder.override()
                    .predicate(ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "angle"), (float) i / 32.0f)
                    .model(getExistingFile(modLoc("item/" + frameName)))
                    .end();
        }

        basicItem(ModItems.SALVAGE_SCRAP.get());
        basicItem(ModItems.REFINED_SCRAP.get());
        basicItem(ModItems.STRANGE_ARTIFACT.get());
        basicItem(ModItems.BLUEPRINT.get());
        basicItem(ModItems.SCRAP_BLOCK_ITEM.get());

        // Machine Framework item model is hand-crafted in src/main/resources/

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

        withExistingParent(ModItems.BLAST_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/blast_module"));

        withExistingParent(ModItems.SMOKER_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/smoker_module"));

        withExistingParent(ModItems.STONECUTTER_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/stonecutter_module"));

        withExistingParent(ModItems.COMPOSTER_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/composter_module"));

        withExistingParent(ModItems.GRINDSTONE_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/grindstone_module"));

        withExistingParent(ModItems.CARTOGRAPHY_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/cartography_module"));

        withExistingParent(ModItems.ANVIL_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/anvil_module"));

        withExistingParent(ModItems.BREWING_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/brewing_module"));

        withExistingParent(ModItems.EJECTION_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/ejection_module"));

        withExistingParent(ModItems.JUKEBOX_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/jukebox_module"));

        withExistingParent(ModItems.REDSTONE_MODULE.getId().getPath(),
                mcLoc("item/generated"))
                .texture("layer0", modLoc("item/redstone_module"));

        basicItem(ModItems.SIGNAL_SOURCE.get());
    }
}