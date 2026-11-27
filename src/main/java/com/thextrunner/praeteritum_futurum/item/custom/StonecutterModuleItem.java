package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.api.module.ProcessingMode;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Map;

/**
 * Stonecutter Module — adds stonecutter processing capability to the Machine Framework.
 * <p>
 * Extends ProcessingModuleItem to provide stonecutter-style processing: takes input items
 * and converts them to a specific target item (set via the config slot). Unlike smelting
 * modules, it requires no fuel and processes items instantly. Supports up to 3 input slots
 * and 1 output slot.
 * <p>
 * The config slot holds the target item — only recipes that produce this exact item will
 * be processed. Multiple input items are processed at once (up to 64 per tick).
 */
public class StonecutterModuleItem extends ProcessingModuleItem {
    public enum SlotMode implements ProcessingMode {
        DISABLED("D", 0xFF555555, "No items are transferred through this side.", Role.DISABLED),
        INPUT("I", 0xFF3366CC, "Pull items from the adjacent inventory for processing.", Role.INPUT),
        OUTPUT("O", 0xFFCC3333, "Send processed items to the adjacent inventory.", Role.OUTPUT);

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

    /** NBT key for storing the module's slot mode configuration. */
    private static final String NBT_KEY = "StonecutterModes";

    /**
     * Constructs the Stonecutter Module with the given properties.
     *
     * @param properties The item properties
     */
    public StonecutterModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/stonecutter_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16,
                16, 16
        );
        super.renderModuleOverlay(graphics, slot, stack, blockEntity);
    }


    @Override
    public String getModuleConfigKey() {
        return NBT_KEY;
    }

    @Override
    public boolean hasConfigSlot() {
        return true;
    }

    @Override
    public boolean usesCenterSlot() {
        return true; // ghost target item slot
    }

    @Override
    public int getMaxInputs() {
        return 3;
    }

    @Override
    public int getMaxOutputs() {
        return 1;
    }

    @Override
    public ItemStack getConfigSlotItem(ItemStack stack, HolderLookup.Provider registries) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("TargetItem")) {
                return ItemStack.parseOptional(registries, tag.getCompound("TargetItem"));
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setConfigSlotItem(ItemStack stack, ItemStack target, HolderLookup.Provider registries) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();

        if (target.isEmpty()) {
            tag.remove("TargetItem");
        } else {
            CompoundTag itemTag = (CompoundTag) target.save(registries);
            tag.put("TargetItem", itemTag);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public SlotMode[] getSupportedModes() {
        return new SlotMode[]{SlotMode.DISABLED, SlotMode.INPUT, SlotMode.OUTPUT};
    }

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        ItemStack targetItem = getConfigSlotItem(stack, level.registryAccess());
        if (targetItem.isEmpty()) return;

        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack);
        ResolvedIoSlots resolved = ProcessingModuleItem.resolveAdjacentIoSlots(blockEntity, slotIndex, modes);

        List<Integer> inputSlots = resolved.inputs();
        List<Integer> outputSlots = resolved.outputs();

        if (inputSlots.isEmpty() || outputSlots.isEmpty()) return;

        ItemStackHandler itemHandler = blockEntity.getInventoryHandler();

        for (int inSlot : inputSlots) {
            ItemStack inputStack = itemHandler.getStackInSlot(inSlot);
            if (inputStack.isEmpty()) continue;

            SingleRecipeInput recipeInput = new SingleRecipeInput(inputStack);
            List<RecipeHolder<StonecutterRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING);

            for (RecipeHolder<StonecutterRecipe> holder : recipes) {
                StonecutterRecipe recipe = holder.value();
                if (recipe.matches(recipeInput, level)) {
                    ItemStack baseResult = recipe.assemble(recipeInput, level.registryAccess());

                    if (ItemStack.isSameItemSameComponents(baseResult, targetItem)) {
                        for (int outSlot : outputSlots) {
                            if (outSlot == inSlot) continue;
                            ItemStack currentOut = itemHandler.getStackInSlot(outSlot);

                            int maxStackSize = baseResult.getMaxStackSize();
                            int currentCount = currentOut.isEmpty() ? 0 : currentOut.getCount();

                            if (!currentOut.isEmpty() && !ItemStack.isSameItemSameComponents(currentOut, baseResult)) {
                                continue;
                            }

                            int spaceAvailable = maxStackSize - currentCount;
                            if (spaceAvailable <= 0) continue;

                            int resultPerInput = baseResult.getCount();
                            int maxInputsForSpace = spaceAvailable / resultPerInput;
                            if (maxInputsForSpace <= 0) continue;

                            int itemsToProcess = Math.min(inputStack.getCount(), maxInputsForSpace);
                            itemsToProcess = Math.min(itemsToProcess, 64);
                            if (itemsToProcess <= 0) continue;

                            itemHandler.extractItem(inSlot, itemsToProcess, false);

                            ItemStack finalResult = baseResult.copy();
                            finalResult.setCount(itemsToProcess * resultPerInput);

                            if (currentOut.isEmpty()) {
                                itemHandler.setStackInSlot(outSlot, finalResult);
                            } else {
                                currentOut.grow(finalResult.getCount());
                            }
                            return;
                        }
                    }
                }
            }
        }
    }
}