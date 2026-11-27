package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.api.module.ProcessingMode;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
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

/**
 * Composter Module — adds composter processing capability to the Machine Framework.
 * <p>
 * Extends ProcessingModuleItem to provide composter-style processing: takes compostable
 * items and converts them to bone meal after reaching 7 compost levels. Each compostable
 * item has a chance to increase the compost level based on its Compostables data map entry.
 * Supports up to 3 input slots and 1 output slot.
 * <p>
 * Unlike other processing modules, it requires no fuel and uses a level-based progress
 * system (0-7) instead of a tick-based progress bar.
 */
public class ComposterModuleItem extends ProcessingModuleItem {
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
    private static final String NBT_KEY = "ComposterModes";

    /** Maximum compost level before producing bone meal (vanilla composter behavior). */
    private static final int MAX_COMPOST_LEVEL = 7;

    /**
     * Constructs the Composter Module with the given properties.
     *
     * @param properties The item properties
     */
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
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, NBT_KEY);
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
    public SlotMode[] getSupportedModes() {
        return new SlotMode[]{SlotMode.DISABLED, SlotMode.INPUT, SlotMode.OUTPUT};
    }
}