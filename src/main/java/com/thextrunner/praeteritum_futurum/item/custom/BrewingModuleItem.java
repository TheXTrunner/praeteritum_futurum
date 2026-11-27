package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.api.module.ProcessingMode;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.util.BrewingRecipeItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BrewingModuleItem extends ProcessingModuleItem {

    public enum SlotMode implements ProcessingMode {
        DISABLED("D", 0xFF555555, "No items are transferred through this side.", Role.DISABLED),
        POTION("P", 0xFF9933CC, "Use potion items from the adjacent inventory.", Role.INPUT),
        INGREDIENT("G", 0xFF33CC66, "Use ingredients from the adjacent inventory.", Role.OTHER);

        private final String labelChar;
        private final int color;
        private final String description;
        private final Role role;

        SlotMode(String labelChar, int color, String description, Role role) {
            this.labelChar = labelChar;
            this.color = color;
            this.description = description;
            this.role = role;
        }

        @Override public String getLabelChar() { return labelChar; }
        @Override public int getColor() { return color; }
        @Override public String getDescription() { return description; }
        @Override public Role getRole() { return role; }
    }

    private static final String NBT_KEY = "BrewingModes";
    private static final int BREW_TIME = 200; // ticks per brew stage, matching vanilla

    public BrewingModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/brewing_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16,
                16, 16
        );

        if (blockEntity != null) {
            int slotIndex = slot.index;
            int fuelTime = blockEntity.getModuleData(slotIndex, 0);
            int fuelTimeTotal = blockEntity.getModuleData(slotIndex, 1);
            int brewTime = blockEntity.getModuleData(slotIndex, 2);
            int brewTimeTotal = blockEntity.getModuleData(slotIndex, 3);

            // Fuel bar (left side, orange)
            if (fuelTime > 0 && fuelTimeTotal > 0) {
                int fuelHeight = Math.min(14, (fuelTime * 14) / fuelTimeTotal);
                graphics.fill(slot.x + 1, slot.y + 15 - fuelHeight, slot.x + 3, slot.y + 15, 0xFFFF6600);
            }

            // Brew progress bar (right side, cyan)
            if (brewTime > 0 && brewTimeTotal > 0) {
                int brewWidth = Math.min(12, (brewTime * 12) / brewTimeTotal);
                graphics.fill(slot.x + 3, slot.y + 13, slot.x + 3 + brewWidth, slot.y + 15, 0xFF00FFCC);
            }
        }
    }

    @Override
    public String getModuleConfigKey() {
        return NBT_KEY;
    }

    @Override
    public boolean hasConfigSlot() {
        return false; // No config slot needed - fuel and ingredient both use adjacent slots
    }

    @Override
    public int getMaxInputs() {
        return 3; // 3 potion slots
    }

    @Override
    public int getMaxOutputs() {
        return 0; // In-place brewing: potions transform in their slots
    }

    /**
     * Returns the maximum number of ingredient slots (always 1 for brewing).
     */
    public int getMaxIngredients() {
        return 1;
    }

    @Override
    public int getInventorySlotLimit(MachineFrameworkBlockEntity blockEntity, int moduleSlotIndex, int inventorySlotIndex) {
        Map<GridDirection, ProcessingMode> modes = getSlotModes(blockEntity.getModuleHandler().getStackInSlot(moduleSlotIndex), NBT_KEY);

        for (GridDirection dir : GridDirection.VALUES) {
            int adjSlot = dir.getAdjacentSlotIndex(moduleSlotIndex);
            if (adjSlot < 0) continue;

            ItemStack adjModule = blockEntity.getModuleHandler().getStackInSlot(adjSlot);
            if (!adjModule.is(com.thextrunner.praeteritum_futurum.item.ModItems.INVENTORY_MODULE.get())) {
                continue;
            }

            ProcessingMode mode = modes.get(dir);
            if (mode == SlotMode.INGREDIENT && adjSlot == inventorySlotIndex) {
                return 1; // Limit ingredient slot to 1 item for proper queuing
            }
        }
        return 0; // Use default limit for non-ingredient slots
    }

    @Override
    public SlotMode[] getSupportedModes() {
        // POTION and INGREDIENT modes for brewing; no OUTPUT since brewing is in-place
        return new SlotMode[]{SlotMode.DISABLED, SlotMode.POTION, SlotMode.INGREDIENT};
    }

    @Override
    protected int getMaxForMode(ProcessingMode mode) {
        if (mode == SlotMode.POTION) return getMaxInputs();
        if (mode == SlotMode.INGREDIENT) return getMaxIngredients();
        return super.getMaxForMode(mode);
    }

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, NBT_KEY);

        ItemStackHandler moduleHandler = blockEntity.getModuleHandler();
        ItemStackHandler itemHandler = blockEntity.getInventoryHandler();

        // Resolve potion slots and ingredient slot by their dedicated modes
        List<Integer> potionSlots = new java.util.ArrayList<>();
        int ingredientSlot = -1;

        for (GridDirection dir : GridDirection.VALUES) {
            int adjSlot = dir.getAdjacentSlotIndex(slotIndex);
            if (adjSlot < 0) continue;

            ItemStack adjModule = moduleHandler.getStackInSlot(adjSlot);
            if (!adjModule.is(com.thextrunner.praeteritum_futurum.item.ModItems.INVENTORY_MODULE.get())) {
                continue;
            }

            ProcessingMode mode = modes.get(dir);
            if (mode == SlotMode.POTION && potionSlots.size() < getMaxInputs()) {
                potionSlots.add(adjSlot);
            } else if (mode == SlotMode.INGREDIENT && ingredientSlot == -1) {
                ingredientSlot = adjSlot;
            }
        }

        // Auto-detect fuel: if ingredient slot has Blaze Powder and fuel is depleted, use it as fuel
        // This runs regardless of potion slot config so the module can fuel up even with minimal setup
        if (ingredientSlot >= 0) {
            ItemStack ingredientItem = itemHandler.getStackInSlot(ingredientSlot);
            if (!ingredientItem.isEmpty() && ingredientItem.is(Items.BLAZE_POWDER)) {
                int fuelTime = blockEntity.getModuleData(slotIndex, 0);
                if (fuelTime <= 0) {
                    itemHandler.extractItem(ingredientSlot, 1, false);
                    int fuelTimeNew = 20;
                    blockEntity.setModuleData(slotIndex, 0, fuelTimeNew);
                    blockEntity.setModuleData(slotIndex, 1, fuelTimeNew);
                    return;
                }
            }
        }

        // Need at least 3 potion slots and 1 ingredient slot for brewing
        if (potionSlots.size() < 3 || ingredientSlot == -1) {
            return;
        }

        // Get the item in the ingredient slot
        ItemStack ingredientItem = itemHandler.getStackInSlot(ingredientSlot);
        if (ingredientItem.isEmpty()) {
            return;
        }

        // Check fuel state
        int fuelTime = blockEntity.getModuleData(slotIndex, 0);
        int fuelTimeTotal = blockEntity.getModuleData(slotIndex, 1);
        int brewTime = blockEntity.getModuleData(slotIndex, 2);
        int brewTimeTotal = blockEntity.getModuleData(slotIndex, 3);

        if (brewTimeTotal <= 0) {
            brewTimeTotal = BREW_TIME;
            blockEntity.setModuleData(slotIndex, 3, brewTimeTotal);
        }

        boolean isBurning = fuelTime > 0;

        // Item is a brewing ingredient - need fuel to brew
        if (!isBurning) {
            return;
        }

        // If item is Blaze Powder, but we're already burning, treat it as a brewing ingredient (Strength potion)
        // This allows Blaze Powder to work as both fuel (when depleted) and ingredient (when burning)

        // Item is a brewing ingredient - need fuel to brew
        if (!isBurning) {
            return;
        }

        ItemStack ingredient = ingredientItem;

        // Check if at least one potion has a valid recipe
        boolean anyValidRecipe = false;
        for (int inSlot : potionSlots) {
            ItemStack potion = itemHandler.getStackInSlot(inSlot);
            if (!potion.isEmpty() && hasBrewingRecipe(level, potion, ingredient)) {
                anyValidRecipe = true;
                break;
            }
        }

        // Brew potions in-place
        if (isBurning && anyValidRecipe) {
            // Increment brew timer once per tick for the entire batch
            brewTime++;
            blockEntity.setModuleData(slotIndex, 2, brewTime);

            if (brewTime >= brewTimeTotal) {
                // Batch complete: apply results to all potions with valid recipes
                for (int inSlot : potionSlots) {
                    ItemStack potion = itemHandler.getStackInSlot(inSlot);
                    if (potion.isEmpty()) continue;
                    if (!hasBrewingRecipe(level, potion, ingredient)) continue;

                    ItemStack result = getBrewingResult(level, potion, ingredient);
                    if (result.isEmpty()) continue;

                    // Replace the input potion with the brewed result in the same slot (in-place)
                    itemHandler.setStackInSlot(inSlot, result.copy());
                }

                // Consume one ingredient from the ingredient slot
                itemHandler.extractItem(ingredientSlot, 1, false);

                // Decrement fuel (one batch per brew cycle)
                fuelTime--;
                blockEntity.setModuleData(slotIndex, 0, fuelTime);

                // Reset brew timer and clear progress data
                blockEntity.setModuleData(slotIndex, 2, 0);
                clearProgressData(stack);
            }
        } else if (brewTime > 0) {
            brewTime = Math.max(0, brewTime - 2);
            blockEntity.setModuleData(slotIndex, 2, brewTime);
        }
    }

    /**
     * Get the potion holder from a potion item stack.
     */
    private static Optional<Holder<Potion>> getPotionHolder(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null ? contents.potion() : Optional.empty();
    }

    /**
     * Check if a brewing recipe exists for the given input potion and ingredient.
     * Uses hardcoded vanilla recipes because NeoForge's PotionBrewing API is broken in 1.21.1.
     * Also handles splash (gunpowder) and lingering (dragon breath) conversions.
     */
    private static boolean hasBrewingRecipe(Level level, ItemStack inputPotion, ItemStack ingredient) {
        if (inputPotion.isEmpty() || ingredient.isEmpty()) {
            return false;
        }

        // Splash conversion: any potion + gunpowder
        if (BrewingRecipeItem.isSplashConversion(ingredient)) {
            return inputPotion.is(Items.POTION) || inputPotion.is(Items.SPLASH_POTION) || inputPotion.is(Items.LINGERING_POTION);
        }

        // Lingering conversion: splash potion + dragon breath
        if (BrewingRecipeItem.isLingeringConversion(ingredient)) {
            return inputPotion.is(Items.SPLASH_POTION);
        }

        // Regular potion recipe
        return getPotionHolder(inputPotion).map(potion ->
                BrewingRecipeItem.hasRecipe(potion, ingredient)).orElse(false);
    }

    /**
     * Get the brewing result for the given input potion and ingredient.
     * Uses hardcoded vanilla recipes because NeoForge's PotionBrewing API is broken in 1.21.1.
     */
    private static ItemStack getBrewingResult(Level level, ItemStack inputPotion, ItemStack ingredient) {
        // Splash conversion: potion -> splash_potion
        if (BrewingRecipeItem.isSplashConversion(ingredient)) {
            Optional<Holder<Potion>> potionOpt = getPotionHolder(inputPotion);
            if (potionOpt.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = new ItemStack(Items.SPLASH_POTION, 1);
            result.set(DataComponents.POTION_CONTENTS, new PotionContents(potionOpt, Optional.empty(), Collections.emptyList()));
            return result;
        }

        // Lingering conversion: splash_potion -> lingering_potion
        if (BrewingRecipeItem.isLingeringConversion(ingredient)) {
            Optional<Holder<Potion>> potionOpt = getPotionHolder(inputPotion);
            if (potionOpt.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = new ItemStack(Items.LINGERING_POTION, 1);
            result.set(DataComponents.POTION_CONTENTS, new PotionContents(potionOpt, Optional.empty(), Collections.emptyList()));
            return result;
        }

        // Regular potion recipe: keep the same item type
        Holder<Potion> outputPotion = getPotionHolder(inputPotion)
                .map(potion -> BrewingRecipeItem.getOutput(potion, ingredient))
                .orElse(null);

        if (outputPotion == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = inputPotion.copy();
        result.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(outputPotion), Optional.empty(), Collections.emptyList()));
        return result;
    }
}
