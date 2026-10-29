package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.ModBlocks;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PraeteritumFuturumMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // so I have to add the same blocks and items to like... 4 files
        // Modblock, BlockItem, ModItem for the model, and this provider
        simpleBlockWithItem(ModBlocks.SALVAGE_FRAMING.get(), cubeAll(ModBlocks.SALVAGE_FRAMING.get()));
        simpleBlockWithItem(ModBlocks.ADVANCED_SALVAGE.get(), cubeAll(ModBlocks.ADVANCED_SALVAGE.get()));
        simpleBlockWithItem(ModBlocks.MACHINE_FRAMEWORK.get(), cubeAll(ModBlocks.MACHINE_FRAMEWORK.get()));
        //at least I understand why, this file generates the .json files
    }
}