package com.thextrunner.praeteritum_futurum.recipe;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PraeteritumFuturumMod.MODID);

    public static final Supplier<RecipeSerializer<ModuleCleaningRecipe>> MODULE_CLEANING =
            SERIALIZERS.register("module_cleaning", () -> new SimpleCraftingRecipeSerializer<>(ModuleCleaningRecipe::new));

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}