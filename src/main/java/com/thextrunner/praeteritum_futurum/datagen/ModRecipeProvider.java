package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.ModItems;
import com.thextrunner.praeteritum_futurum.ModTags;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        // Yool (Wrench): Y-shape made of salvage scrap
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.YOOL.get())
                .define('S', ModItems.SALVAGE_SCRAP.get())
                .pattern("S S")
                .pattern(" S ")
                .pattern(" S ")
                .unlockedBy("has_salvage_scrap", has(ModItems.SALVAGE_SCRAP.get()))
                .save(recipeOutput);

        // Machine Framework: Strange artifact center, salvage scrap corners, refined scrap sides
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.MACHINE_FRAMEWORK_ITEM.get())
                .define('A', ModItems.STRANGE_ARTIFACT.get())
                .define('S', ModItems.SALVAGE_SCRAP.get())
                .define('R', ModItems.REFINED_SCRAP.get())
                .pattern("SRS")
                .pattern("RAR")
                .pattern("SRS")
                .unlockedBy("has_strange_artifact", has(ModItems.STRANGE_ARTIFACT.get()))
                .save(recipeOutput);

        // Inventory Module: Chest surrounded by salvage scrap
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INVENTORY_MODULE.get())
                .define('S', ModItems.SALVAGE_SCRAP.get())
                .define('C', Items.CHEST)
                .pattern("SSS")
                .pattern("SCS")
                .pattern("SSS")
                .unlockedBy("has_salvage_scrap", has(ModItems.SALVAGE_SCRAP.get()))
                .save(recipeOutput);

        // IO Module: Hopper with salvage scrap
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.IO_MODULE.get())
                .define('S', ModItems.SALVAGE_SCRAP.get())
                .define('H', Items.HOPPER)
                .pattern(" S ")
                .pattern("SHS")
                .pattern(" S ")
                .unlockedBy("has_salvage_scrap", has(ModItems.SALVAGE_SCRAP.get()))
                .save(recipeOutput);

        // Filter Module: Iron bars and refined scrap
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FILTER_MODULE.get())
                .define('R', ModItems.REFINED_SCRAP.get())
                .define('I', Items.IRON_BARS)
                .pattern(" R ")
                .pattern("RIR")
                .pattern(" R ")
                .unlockedBy("has_refined_scrap", has(ModItems.REFINED_SCRAP.get()))
                .save(recipeOutput);

        // Smelting Module: Furnace and refined scrap
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SMELTING_MODULE.get())
                .define('R', ModItems.REFINED_SCRAP.get())
                .define('F', Items.FURNACE)
                .pattern(" R ")
                .pattern("RFR")
                .pattern(" R ")
                .unlockedBy("has_refined_scrap", has(ModItems.REFINED_SCRAP.get()))
                .save(recipeOutput);

        // Stonecutter Module: Stonecutter and refined scrap
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STONECUTTER_MODULE.get())
                .define('R', ModItems.REFINED_SCRAP.get())
                .define('S', Items.STONECUTTER)
                .pattern(" R ")
                .pattern("RSR")
                .pattern(" R ")
                .unlockedBy("has_refined_scrap", has(ModItems.REFINED_SCRAP.get()))
                .save(recipeOutput);

        // Composter Module: Composter and refined scrap
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPOSTER_MODULE.get())
                .define('R', ModItems.REFINED_SCRAP.get())
                .define('C', Items.COMPOSTER)
                .pattern(" R ")
                .pattern("RCR")
                .pattern(" R ")
                .unlockedBy("has_refined_scrap", has(ModItems.REFINED_SCRAP.get()))
                .save(recipeOutput);

        // Salvage Cutter: Diamond sword and salvage scrap
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SALVAGE_CUTTER.get())
                .define('S', ModItems.SALVAGE_SCRAP.get())
                .define('P', Items.DIAMOND_SWORD)
                .pattern("S S")
                .pattern(" P ")
                .pattern("S S")
                .unlockedBy("has_salvage_scrap", has(ModItems.SALVAGE_SCRAP.get()))
                .save(recipeOutput);
    }
}