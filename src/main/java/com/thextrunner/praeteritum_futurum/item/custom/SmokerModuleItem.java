package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.api.module.ProcessingMode;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Map;
import java.util.Optional;

/**
 * Smoker Module — adds smoker cooking capability to the Machine Framework.
 * <p>
 * Extends ProcessingModuleItem to provide smoker-style cooking: takes food items
 * and fuel, cooks over 100 ticks (faster than smelting), and outputs the result.
 * Supports three slot modes: INPUT for food, FUEL for fuel items, and OUTPUT for results.
 * <p>
 * Renders a custom overlay showing burn time (orange bar) and cook progress (cyan bar).
 */
public class SmokerModuleItem extends ProcessingModuleItem {
    public enum SlotMode implements ProcessingMode {
        DISABLED("D", 0xFF555555, "No items are transferred through this side.", Role.DISABLED),
        INPUT("I", 0xFF3366CC, "Pull items from the adjacent inventory for processing.", Role.INPUT),
        FUEL("F", 0xFFCC9933, "Use items from the adjacent inventory as fuel.", Role.OTHER),
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
    private static final String NBT_KEY = "SmokerModes";

    /**
     * Constructs the Smoker Module with the given properties.
     *
     * @param properties The item properties
     */
    public SmokerModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/smoker_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16,
                16, 16
        );

        if (blockEntity != null) {
            int slotIndex = slot.index;
            int burnTime = blockEntity.getModuleData(slotIndex, 0);
            int burnTimeTotal = blockEntity.getModuleData(slotIndex, 1);
            int cookTime = blockEntity.getModuleData(slotIndex, 2);
            int cookTimeTotal = blockEntity.getModuleData(slotIndex, 3);

            if (burnTime > 0 && burnTimeTotal > 0) {
                int burnHeight = Math.min(14, (burnTime * 14) / burnTimeTotal);
                graphics.fill(slot.x + 1, slot.y + 15 - burnHeight, slot.x + 3, slot.y + 15, 0xFFFF6600);
            }

            if (cookTime > 0 && cookTimeTotal > 0) {
                int cookWidth = Math.min(12, (cookTime * 12) / cookTimeTotal);
                graphics.fill(slot.x + 3, slot.y + 13, slot.x + 3 + cookWidth, slot.y + 15, 0xFF00FFCC);
            }
        }
    }

    @Override
    public String getModuleConfigKey() {
        return NBT_KEY;
    }

    @Override
    public int getMaxInputs() {
        return 1;
    }

    @Override
    public int getMaxOutputs() {
        return 1;
    }

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, NBT_KEY);
        ItemStackHandler moduleHandler = blockEntity.getModuleHandler();
        ItemStackHandler itemHandler = blockEntity.getInventoryHandler();

        int inputSlot = -1;
        int fuelSlot = -1;
        int outputSlot = -1;

        for (GridDirection dir : GridDirection.VALUES) {
            int adjSlot = dir.getAdjacentSlotIndex(slotIndex);
            if (adjSlot < 0) continue;

            ItemStack adjModule = moduleHandler.getStackInSlot(adjSlot);
            if (!adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                continue;
            }

            ProcessingMode mode = modes.get(dir);
            if (mode == SlotMode.INPUT && inputSlot == -1) {
                inputSlot = adjSlot;
            } else if (mode == SlotMode.FUEL && fuelSlot == -1) {
                fuelSlot = adjSlot;
            } else if (mode == SlotMode.OUTPUT && outputSlot == -1) {
                outputSlot = adjSlot;
            }
        }

        if (inputSlot == -1 || fuelSlot == -1 || outputSlot == -1) {
            return;
        }

        int burnTime = blockEntity.getModuleData(slotIndex, 0);
        int cookTime = blockEntity.getModuleData(slotIndex, 2);
        int cookTimeTotal = blockEntity.getModuleData(slotIndex, 3);
        if (cookTimeTotal <= 0) {
            cookTimeTotal = 100;
            blockEntity.setModuleData(slotIndex, 3, cookTimeTotal);
        }

        ItemStack inputStack = itemHandler.getStackInSlot(inputSlot);
        ItemStack fuelStack = itemHandler.getStackInSlot(fuelSlot);

        boolean isBurning = burnTime > 0;

        if (isBurning) {
            burnTime--;
            blockEntity.setModuleData(slotIndex, 0, burnTime);
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(inputStack);
        Optional<RecipeHolder<SmokingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMOKING, recipeInput, level);

        boolean canSmoke = !inputStack.isEmpty() && recipe.isPresent();

        if (!isBurning && canSmoke && !fuelStack.isEmpty()) {
            int fuelBurnTime = fuelStack.getBurnTime(RecipeType.SMOKING);
            if (fuelBurnTime > 0) {
                fuelStack.shrink(1);
                burnTime = fuelBurnTime;
                blockEntity.setModuleData(slotIndex, 0, burnTime);
                blockEntity.setModuleData(slotIndex, 1, fuelBurnTime);
                isBurning = true;
            }
        }

        if (isBurning && canSmoke) {
            ItemStack result = recipe.get().value().assemble(recipeInput, level.registryAccess());
            ItemStack currentOutput = itemHandler.getStackInSlot(outputSlot);

            boolean hasSpace = currentOutput.isEmpty() ||
                    (ItemStack.isSameItemSameComponents(currentOutput, result) && currentOutput.getCount() + result.getCount() <= currentOutput.getMaxStackSize());

            if (hasSpace) {
                cookTime++;
                blockEntity.setModuleData(slotIndex, 2, cookTime);
                if (cookTime >= cookTimeTotal) {
                    blockEntity.setModuleData(slotIndex, 2, 0);
                    itemHandler.extractItem(inputSlot, 1, false);
                    if (currentOutput.isEmpty()) {
                        itemHandler.setStackInSlot(outputSlot, result.copy());
                    } else {
                        currentOutput.grow(result.getCount());
                    }

                    clearProgressData(stack);
                }
            } else if (cookTime > 0) {
                cookTime = Math.max(0, cookTime - 2);
                blockEntity.setModuleData(slotIndex, 2, cookTime);
            }
        } else if (cookTime > 0) {
            cookTime = Math.max(0, cookTime - 2);
            blockEntity.setModuleData(slotIndex, 2, cookTime);
        }
    }

    @Override
    public SlotMode[] getSupportedModes() {
        return new SlotMode[]{SlotMode.DISABLED, SlotMode.INPUT, SlotMode.FUEL, SlotMode.OUTPUT};
    }

    @Override
    protected int getMaxForMode(ProcessingMode mode) {
        if (mode == SlotMode.FUEL) return 1;
        return super.getMaxForMode(mode);
    }

}
