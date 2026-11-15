package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PraeteritumFuturumMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Registers standard simple blocks with automatic item models
        simpleBlockWithItem(ModBlocks.SALVAGE_FRAMING.get(), cubeAll(ModBlocks.SALVAGE_FRAMING.get()));
        simpleBlockWithItem(ModBlocks.ADVANCED_SALVAGE.get(), cubeAll(ModBlocks.ADVANCED_SALVAGE.get()));

        // Generates the multi-texture cube model with individual face textures and an explicit particle texture
        ModelFile machineFrameworkModel = models().cube(
                "machine_framework",
                modLoc("block/machine_framework_down"), // down
                modLoc("block/machine_framework_up"),   // up
                modLoc("block/machine_framework_front"), // Nort
                modLoc("block/machine_framework_back"),  // South
                modLoc("block/machine_framework_left"),  // West
                modLoc("block/machine_framework_right")  // East
        ).texture("particle", modLoc("block/machine_framework"));

        // Applies horizontal rotation blockstate variants and item model mapping for the machine framework
        horizontalBlock(ModBlocks.MACHINE_FRAMEWORK.get(), machineFrameworkModel);
        simpleBlockItem(ModBlocks.MACHINE_FRAMEWORK.get(), machineFrameworkModel);
    }
}