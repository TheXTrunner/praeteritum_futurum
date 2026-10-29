package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.ModItems;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Map;
import java.util.Optional;

public class SmeltingModuleItem extends ProcessingModuleItem {
    public SmeltingModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/smelting_module_overlay.png"),
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

    private static final String NBT_KEY = "SmeltingModes";

    @Override
    public String getModuleConfigKey() {
        return NBT_KEY;
    }

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        Map<Direction, SlotMode> modes = getSlotModes(stack, NBT_KEY);
        ItemStackHandler moduleHandler = blockEntity.getModuleHandler();
        ItemStackHandler itemHandler = blockEntity.getInventoryHandler();

        int row = slotIndex / 9;
        int col = slotIndex % 9;

        int[] neighborIndices = {
                row > 0 ? slotIndex - 9 : -1,
                row < 2 ? slotIndex + 9 : -1,
                col > 0 ? slotIndex - 1 : -1,
                col < 8 ? slotIndex + 1 : -1
        };
        Direction[] dirs = {Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST};

        int inputSlot = -1;
        int fuelSlot = -1;
        int outputSlot = -1;

        for (int i = 0; i < 4; i++) {
            int adjSlot = neighborIndices[i];
            if (adjSlot < 0) continue;

            ItemStack adjModule = moduleHandler.getStackInSlot(adjSlot);
            if (!adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                continue;
            }

            SlotMode mode = modes.get(dirs[i]);
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
            cookTimeTotal = 200;
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
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, recipeInput, level);

        boolean canSmelt = !inputStack.isEmpty() && recipe.isPresent();

        if (!isBurning && canSmelt && !fuelStack.isEmpty()) {
            int fuelBurnTime = fuelStack.getBurnTime(RecipeType.SMELTING);
            if (fuelBurnTime > 0) {
                fuelStack.shrink(1);
                burnTime = fuelBurnTime;
                blockEntity.setModuleData(slotIndex, 0, burnTime);
                blockEntity.setModuleData(slotIndex, 1, fuelBurnTime);
                isBurning = true;
            }
        }

        if (isBurning && canSmelt) {
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
    public SlotMode getNextMode(ItemStack stack, Direction clickedDir) {
        Map<Direction, SlotMode> modes = getSlotModes(stack, NBT_KEY);
        SlotMode current = modes.get(clickedDir);
        SlotMode[] values = SlotMode.values();

        SlotMode next;
        for (int i = 1; i <= values.length; i++) {
            int nextOrdinal = (current.ordinal() + i) % values.length;
            next = values[nextOrdinal];

            if (next == SlotMode.DISABLED) {
                return SlotMode.DISABLED;
            }

            boolean isTaken = false;
            for (Map.Entry<Direction, SlotMode> entry : modes.entrySet()) {
                if (entry.getKey() != clickedDir && entry.getValue() == next) {
                    isTaken = true;
                    break;
                }
            }

            if (!isTaken) {
                return next;
            }
        }
        return SlotMode.DISABLED;
    }
}