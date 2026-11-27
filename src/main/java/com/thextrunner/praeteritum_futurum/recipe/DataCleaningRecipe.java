package com.thextrunner.praeteritum_futurum.recipe;

import com.thextrunner.praeteritum_futurum.ModTags;
import com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Custom recipe that clears configuration data from modules and blueprints.
 * <p>
 * When a single configurable module or blueprint is placed in any crafting grid,
 * this recipe produces a clean copy with all CustomData removed. This allows
 * players to reset module configurations or clear blueprint data without
 * needing to craft new items.
 * <p>
 * Matches any single item tagged with configurable_modules or blueprints.
 * Uses the standard CraftingRecipe serializer.
 */
public class DataCleaningRecipe extends CustomRecipe {
    /**
     * Constructs the data cleaning recipe.
     *
     * @param category The crafting book category (unused)
     */
    public DataCleaningRecipe(CraftingBookCategory category) {
        super(category);
    }

    /**
     * Returns the contents of a cleaned Redstone Module to its crafter.
     * <p>
     * Crafting remainders provide only one stack per input slot, which is not enough for the
     * module's 54-slot component inventory. This event runs for the completed craft and lets
     * every stored component be returned, dropping only items that do not fit in the inventory.
     */
    public static void returnRedstoneComponents(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        Player player = event.getEntity();
        if (!(result.getItem() instanceof RedstoneModuleItem) || player.level().isClientSide()) {
            return;
        }

        Container craftingInput = event.getInventory();
        for (int inputSlot = 0; inputSlot < craftingInput.getContainerSize(); inputSlot++) {
            ItemStack input = craftingInput.getItem(inputSlot);
            if (!(input.getItem() instanceof RedstoneModuleItem)) {
                continue;
            }

            ItemStackHandler components = RedstoneModuleItem.getRedstoneInventory(input, player.registryAccess());
            for (int componentSlot = 0; componentSlot < components.getSlots(); componentSlot++) {
                ItemStack component = components.getStackInSlot(componentSlot).copy();
                if (component.isEmpty()) {
                    continue;
                }

                if (!player.getInventory().add(component) && !component.isEmpty()) {
                    player.drop(component, false);
                }
            }
            return;
        }
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
        return ModRecipeSerializers.DATA_CLEANING.get();
    }
}