package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Generates block state and block model JSON files for the mod's blocks.
 * <p>
 * Registers simple blocks with automatic item models. The Machine Framework
 * uses hand-crafted blockstate and models in src/main/resources/ and is not
 * generated here.
 */
public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PraeteritumFuturumMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Registers standard simple blocks with automatic item models
        simpleBlockWithItem(ModBlocks.SALVAGE_FRAMING.get(), cubeAll(ModBlocks.SALVAGE_FRAMING.get()));
        simpleBlockWithItem(ModBlocks.ADVANCED_SALVAGE.get(), cubeAll(ModBlocks.ADVANCED_SALVAGE.get()));
        simpleBlockWithItem(ModBlocks.SCRAP_BLOCK.get(), cubeAll(ModBlocks.SCRAP_BLOCK.get()));

        // Machine Framework uses hand-crafted blockstate, block model, and item model
        // in src/main/resources/ - not generated here
    }
}