package com.thextrunner.praeteritum_futurum.recipe;

import com.thextrunner.praeteritum_futurum.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ModuleCleaningRecipe implements CraftingRecipe {//if it's record class it will break in the future... maybe
    private final CraftingBookCategory category;

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return ItemStack.EMPTY;
    }

    public ModuleCleaningRecipe(CraftingBookCategory category) {
        this.category = category;
    }

    @Override
    public @NotNull CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        ItemStack foundModule = ItemStack.EMPTY;
        int itemCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (!stack.is(ModTags.Items.CONFIGURABLE_MODULES)) {
                    return false;
                }
                if (!foundModule.isEmpty()) {
                    return false;
                }
                foundModule = stack;
                itemCount++;
            }
        }
        return itemCount == 1;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModTags.Items.CONFIGURABLE_MODULES)) {
                return new ItemStack(stack.getItem());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.MODULE_CLEANING.get();
    }
}