package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.ModBlocks;
import com.thextrunner.praeteritum_futurum.ModTags;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, PraeteritumFuturumMod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        this.tag(ModTags.Blocks.SALVAGE_BLOCKS).add(
                ModBlocks.SALVAGE_FRAMING.get(),
                ModBlocks.ADVANCED_SALVAGE.get()
        );

        // Only register the Machine Framework for pickaxe mining so it handles tool checks correctly
        this.tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModBlocks.MACHINE_FRAMEWORK.get()
        );

        this.tag(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL).add(
                ModBlocks.MACHINE_FRAMEWORK.get()
        );
    }
}