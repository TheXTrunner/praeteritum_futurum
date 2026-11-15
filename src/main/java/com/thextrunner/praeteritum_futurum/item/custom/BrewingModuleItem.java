package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.util.BrewingRecipeItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Map;

public class BrewingModuleItem extends ProcessingModuleItem {

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
        return 4; // 3 potion slots + 1 ingredient/fuel slot
    }

    @Override
    public int getMaxOutputs() {
        return 3;
    }

    @Override
    public SlotMode getNextMode(ItemStack stack, Direction clickedDir) {
        Map<Direction, SlotMode> modes = getSlotModes(stack, NBT_KEY);
        SlotMode current = modes.get(clickedDir);

        SlotMode[] allowedModes = {SlotMode.DISABLED, SlotMode.INPUT, SlotMode.OUTPUT};

        int currentIndex = 0;
        for (int i = 0; i < allowedModes.length; i++) {
            if (allowedModes[i] == current) {
                currentIndex = i;
                break;
            }
        }

        for (int i = 1; i <= allowedModes.length; i++) {
            int nextIndex = (currentIndex + i) % allowedModes.length;
            SlotMode next = allowedModes[nextIndex];

            if (next == SlotMode.DISABLED) {
                return SlotMode.DISABLED;
            }

            int currentCount = countSlotMode(stack, next);
            if (current == next) {
                currentCount--;
            }

            int max = (next == SlotMode.INPUT) ? getMaxInputs() : getMaxOutputs();

            if (currentCount >= max) {
                continue;
            }

            return next;
        }
        return SlotMode.DISABLED;
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

        // Resolve input slots (3 potions + 1 material slot) and output slots
        List<Integer> inputSlots = new java.util.ArrayList<>();
        List<Integer> outputSlots = new java.util.ArrayList<>();

        for (int i = 0; i < 4; i++) {
            int adjSlot = neighborIndices[i];
            if (adjSlot < 0) continue;

            ItemStack adjModule = moduleHandler.getStackInSlot(adjSlot);
            if (!adjModule.is(com.thextrunner.praeteritum_futurum.item.ModItems.INVENTORY_MODULE.get())) {
                continue;
            }

            SlotMode mode = modes.get(dirs[i]);
            if (mode == SlotMode.INPUT && inputSlots.size() < getMaxInputs()) {
                inputSlots.add(adjSlot);
            } else if (mode == SlotMode.OUTPUT && outputSlots.size() < getMaxOutputs()) {
                outputSlots.add(adjSlot);
            }
        }

        // Need at least 4 inputs (3 potions + 1 material) and 3 outputs
        if (inputSlots.size() < 4 || outputSlots.isEmpty()) {
            return;
        }

        // The last input slot is the material slot (ingredient or fuel); the first 3 are potion slots
        int materialSlot = inputSlots.get(3);
        List<Integer> potionSlots = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            potionSlots.add(inputSlots.get(i));
        }

        // Get the item in the material slot
        ItemStack materialItem = itemHandler.getStackInSlot(materialSlot);
        if (materialItem.isEmpty()) {
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

        // Auto-detect: if material slot has Blaze Powder, use it as fuel
        if (materialItem.is(Items.BLAZE_POWDER)) {
            if (!isBurning) {
                // Consume one blaze powder and start fuel
                itemHandler.extractItem(materialSlot, 1, false);
                fuelTime = 20; // 20 batches per blaze powder
                fuelTimeTotal = 20;
                blockEntity.setModuleData(slotIndex, 0, fuelTime);
                blockEntity.setModuleData(slotIndex, 1, fuelTimeTotal);
                isBurning = true;
            }
            // If already burning, just continue (don't need to do anything with the material slot)
            return;
        }

        // Material is an ingredient, not fuel - need fuel to brew
        if (!isBurning) {
            return;
        }

        ItemStack ingredient = materialItem;

        // Decrement fuel each tick while brewing
        fuelTime--;
        blockEntity.setModuleData(slotIndex, 0, fuelTime);

        // Check if at least one potion has a valid recipe
        boolean anyValidRecipe = false;
        for (int inSlot : potionSlots) {
            ItemStack potion = itemHandler.getStackInSlot(inSlot);
            if (!potion.isEmpty() && BrewingRecipeItem.hasRecipe(potion, ingredient)) {
                anyValidRecipe = true;
                break;
            }
        }

        // Brew potions
        if (isBurning && anyValidRecipe) {
            boolean brewedAny = false;

            for (int i = 0; i < potionSlots.size(); i++) {
                int inSlot = potionSlots.get(i);
                int outSlot = outputSlots.size() > i ? outputSlots.get(i) : -1;
                if (outSlot == -1 || outSlot == inSlot) continue;

                ItemStack potion = itemHandler.getStackInSlot(inSlot);
                if (potion.isEmpty()) continue;

                if (!BrewingRecipeItem.hasRecipe(potion, ingredient)) {
                    continue; // Skip potions that don't have a valid recipe
                }

                ItemStack result = BrewingRecipeItem.getBrewingResult(potion, ingredient);
                if (result.isEmpty()) continue;

                ItemStack currentOut = itemHandler.getStackInSlot(outSlot);
                boolean hasSpace = currentOut.isEmpty() ||
                        (ItemStack.isSameItemSameComponents(currentOut, result) &&
                         currentOut.getCount() + result.getCount() <= currentOut.getMaxStackSize());

                if (hasSpace) {
                    brewTime++;
                    blockEntity.setModuleData(slotIndex, 2, brewTime);

                    if (brewTime >= brewTimeTotal) {
                        blockEntity.setModuleData(slotIndex, 2, 0);

                        // Consume the input potion
                        itemHandler.extractItem(inSlot, 1, false);

                        // Output the result
                        if (currentOut.isEmpty()) {
                            itemHandler.setStackInSlot(outSlot, result.copy());
                        } else {
                            currentOut.grow(result.getCount());
                        }

                        brewedAny = true;
                    }
                }
            }

            // Consume one ingredient and decrement fuel after a full brew cycle
            if (brewedAny) {
                // Consume ingredient from the material slot
                itemHandler.extractItem(materialSlot, 1, false);

                // Decrement fuel (one batch per brew cycle)
                fuelTime--;
                blockEntity.setModuleData(slotIndex, 0, fuelTime);

                clearProgressData(stack);
            }
        } else if (brewTime > 0) {
            brewTime = Math.max(0, brewTime - 2);
            blockEntity.setModuleData(slotIndex, 2, brewTime);
        }
    }
}
