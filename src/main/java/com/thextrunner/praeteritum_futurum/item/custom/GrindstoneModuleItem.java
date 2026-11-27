package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.api.module.ProcessingMode;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GrindstoneModuleItem extends ProcessingModuleItem {

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

    private static final String NBT_KEY = "GrindstoneModes";

    public GrindstoneModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/grindstone_module_overlay.png"),
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
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, NBT_KEY);
        ResolvedIoSlots resolved = ProcessingModuleItem.resolveAdjacentIoSlots(blockEntity, slotIndex, modes);

        List<Integer> inputSlots = resolved.inputs().stream().limit(3).toList();
        List<Integer> outputSlots = resolved.outputs().stream().limit(1).toList();

        if (inputSlots.isEmpty() || outputSlots.isEmpty()) return;

        ItemStackHandler itemHandler = blockEntity.getInventoryHandler();

        // Collect all non-empty input items (up to 3)
        List<Integer> filledSlotIndices = new ArrayList<>();
        List<ItemStack> filledItems = new ArrayList<>();

        for (int slot : inputSlots) {
            ItemStack candidate = itemHandler.getStackInSlot(slot);
            if (!candidate.isEmpty()) {
                filledSlotIndices.add(slot);
                filledItems.add(candidate);
            }
        }

        if (filledItems.isEmpty()) return;

        // Check output slot is available before processing — prevents XP farming exploit
        int outSlot = outputSlots.get(0);
        if (!itemHandler.getStackInSlot(outSlot).isEmpty()) return;

        // Determine processing mode based on collected items
        ItemStack resultStack;
        int xpReward;

        // Check if all items are the same type for repair mode
        boolean allSameType = true;
        for (int i = 1; i < filledItems.size(); i++) {
            if (!filledItems.get(0).is(filledItems.get(i).getItem())) {
                allSameType = false;
                break;
            }
        }

        if (allSameType && filledItems.size() > 1) {
            // Repair mode: combine all same-type items
            resultStack = repairItems(filledItems);
            xpReward = 0;
            for (ItemStack item : filledItems) {
                xpReward += calculateDisenchantXp(item);
            }
        } else {
            // Disenchant mode: process only the first item
            resultStack = disenchantItem(filledItems.get(0));
            xpReward = calculateDisenchantXp(filledItems.get(0));
            // Only consume the first item in disenchant mode
            filledSlotIndices = filledSlotIndices.subList(0, 1);
            filledItems = filledItems.subList(0, 1);
        }

        // Push result to output slot
        itemHandler.setStackInSlot(outSlot, resultStack);

        // Consume all processed input items
        for (int slotIdx : filledSlotIndices) {
            itemHandler.extractItem(slotIdx, 1, false);
        }

        // Award XP orbs only after successfully processing and outputting
        if (level instanceof ServerLevel serverLevel && xpReward > 0) {
            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), xpReward);
        }
    }

    /**
     * Repairs multiple items of the same type (2-3 items), combining their remaining durability with a 5% bonus per pair.
     * Removes all non-curse enchantments from the result, matching vanilla grindstone behavior.
     */
    private ItemStack repairItems(List<ItemStack> items) {
        ItemStack first = items.get(0);
        int maxDamage = first.getMaxDamage();

        // Sum up remaining durability from all items
        int totalRemaining = 0;
        for (ItemStack item : items) {
            totalRemaining += maxDamage - item.getDamageValue();
        }

        // Add 5% bonus per repair pair (n-1 pairs for n items)
        int pairCount = items.size() - 1;
        int bonus = (maxDamage * 5 / 100) * pairCount;

        int combinedRemaining = totalRemaining + bonus;
        int newDamage = Math.max(0, maxDamage - combinedRemaining);

        ItemStack result = first.copy();
        result.setDamageValue(newDamage);

        // Remove all non-curse enchantments, matching vanilla grindstone behavior
        removeNonCurseEnchantments(result);

        return result;
    }

    /**
     * Disenchants a single item, removing all non-curse enchantments.
     * Returns a copy of the item with enchantments cleared.
     */
    private ItemStack disenchantItem(ItemStack item) {
        ItemStack result = item.copy();
        removeNonCurseEnchantments(result);
        return result;
    }

    /**
     * Removes all non-curse enchantments from the given item stack.
     * Curse enchantments (Curse of Binding, Curse of Vanishing) are preserved,
     * matching vanilla grindstone behavior.
     */
    private void removeNonCurseEnchantments(ItemStack stack) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) return;

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
            if (entry.getKey().is(EnchantmentTags.CURSE)) {
                mutable.set(entry.getKey(), entry.getValue());
            }
        }
        EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
    }

    /**
     * Calculates the XP reward for disenchanting an item, matching vanilla grindstone behavior.
     * The XP is based on the sum of the minimum modified levels of each non-curse enchantment,
     * with a random bonus between half the sum (rounded up) and the full sum.
     */
    private int calculateDisenchantXp(ItemStack stack) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) return 0;

        int totalLevel = 0;
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
            // Curses don't contribute to XP
            if (entry.getKey().is(EnchantmentTags.CURSE)) continue;
            totalLevel += entry.getValue();
        }

        if (totalLevel == 0) return 0;

        // Vanilla formula: random between ceil(total/2) and total
        int min = (totalLevel + 1) / 2;
        int max = totalLevel;
        return min + (int) (Math.random() * (max - min + 1));
    }

    @Override
    public SlotMode[] getSupportedModes() {
        return new SlotMode[]{SlotMode.DISABLED, SlotMode.INPUT, SlotMode.OUTPUT};
    }
}