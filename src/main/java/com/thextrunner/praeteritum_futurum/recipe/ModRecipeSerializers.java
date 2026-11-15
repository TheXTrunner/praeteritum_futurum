package com.thextrunner.praeteritum_futurum.recipe;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registers custom recipe serializers for the mod.
 * <p>
 * Each custom recipe type needs a serializer so Minecraft can read the recipe JSON files
 * and reconstruct the recipe objects at runtime. The serializer name ("data_cleaning")
 * becomes the "type" field in recipe JSON files: {"type": "praeteritum_futurum:data_cleaning"}.
 */
public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PraeteritumFuturumMod.MODID);

    /**
     * Data Cleaning recipe — used to clean or refine items in the mod's processing system.
     * Uses SimpleCraftingRecipeSerializer, which means the recipe JSON has a simple
     * ingredient/result structure without complex conditions.
     */
    public static final Supplier<RecipeSerializer<DataCleaningRecipe>> DATA_CLEANING =
            SERIALIZERS.register("data_cleaning", () -> new SimpleCraftingRecipeSerializer<>(DataCleaningRecipe::new));

    /**
     * Registers this DeferredRegister with the mod's event bus.
     * Called from PraeteritumFuturumMod's constructor.
     */
    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}