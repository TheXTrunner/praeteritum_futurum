package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.List;
import java.util.Map;

public class ComposterModuleItem extends ProcessingModuleItem {

    private static final String NBT_KEY = "ComposterModes";
    private static final int MAX_COMPOST_LEVEL = 7;

    public ComposterModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/composter_module_overlay.png"),
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
    public int getMaxInputs() {
        return 3;
    }

    @Override
    public int getMaxOutputs() {
        return 1;
    }

    @Override
    public float getProgressPercent(ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        int currentLevel = blockEntity.getModuleData(slotIndex, 0);
        return (float) currentLevel / (float) MAX_COMPOST_LEVEL;
    }

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        Map<Direction, SlotMode> modes = getSlotModes(stack, NBT_KEY);
        ResolvedIoSlots resolved = ProcessingModuleItem.resolveAdjacentIoSlots(blockEntity, slotIndex, modes);

        List<Integer> inputSlots = resolved.inputs();
        List<Integer> outputSlots = resolved.outputs();

        if (inputSlots.isEmpty() || outputSlots.isEmpty()) return;

        ItemStackHandler itemHandler = blockEntity.getInventoryHandler();
        int currentCompostLevel = blockEntity.getModuleData(slotIndex, 0);
        boolean statusChanged = false;

        for (int inSlot : inputSlots) {
            ItemStack inputStack = itemHandler.getStackInSlot(inSlot);
            if (inputStack.isEmpty()) continue;

            var compostData = inputStack.getItemHolder().getData(NeoForgeDataMaps.COMPOSTABLES);
            if (compostData == null || compostData.chance() <= 0.0F) continue;

            float chance = compostData.chance();

            itemHandler.extractItem(inSlot, 1, false);
            statusChanged = true;

            if (level.random.nextFloat() < chance) {
                currentCompostLevel++;

                if (currentCompostLevel >= MAX_COMPOST_LEVEL) {
                    ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);
                    boolean pushed = false;

                    for (int outSlot : outputSlots) {
                        if (outSlot == inSlot) continue;
                        ItemStack currentOut = itemHandler.getStackInSlot(outSlot);

                        if (currentOut.isEmpty()) {
                            itemHandler.setStackInSlot(outSlot, boneMeal.copy());
                            pushed = true;
                            break;
                        } else if (ItemStack.isSameItemSameComponents(currentOut, boneMeal) && currentOut.getCount() < currentOut.getMaxStackSize()) {
                            currentOut.grow(1);
                            pushed = true;
                            break;
                        }
                    }

                    if (pushed) {
                        currentCompostLevel = 0;
                        clearProgressData(stack);
                    } else {
                        currentCompostLevel = MAX_COMPOST_LEVEL;
                    }
                }
            }
        }

        if (statusChanged || currentCompostLevel != blockEntity.getModuleData(slotIndex, 0)) {
            blockEntity.setModuleData(slotIndex, 0, currentCompostLevel);
        }
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

            // If current mode is the same as next, don't count the clicked direction toward the limit
            int currentCount = countSlotMode(stack, next);
            if (current == next) {
                currentCount--; // exclude the clicked direction itself
            }

            int max = (next == SlotMode.INPUT) ? getMaxInputs() : getMaxOutputs();
            if (currentCount >= max) {
                continue; // skip this mode, limit reached
            }

            return next;
        }
        return SlotMode.DISABLED;
    }
}