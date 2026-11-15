package com.thextrunner.praeteritum_futurum.recipe;

import com.thextrunner.praeteritum_futurum.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class DataWipeRecipe extends CustomRecipe {
    public DataWipeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        int validItemCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModTags.Items.CONFIGURABLE_MODULES) || stack.is(ModTags.Items.BLUEPRINTS)) {
                    validItemCount++;
                } else {
                    return false;
                }
            }
        }
        return validItemCount == 1;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModTags.Items.CONFIGURABLE_MODULES) || stack.is(ModTags.Items.BLUEPRINTS)) {
                ItemStack cleanStack = new ItemStack(stack.getItem());
                cleanStack.remove(DataComponents.CUSTOM_DATA);
                return cleanStack;
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
        return ModRecipeSerializers.DATA_WIPE_SERIALIZER.get();
    }
}