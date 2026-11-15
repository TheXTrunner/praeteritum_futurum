package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.api.module.IConfigurableModule;
import com.thextrunner.praeteritum_futurum.api.module.ITextInputModule;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnvilModuleItem extends ProcessingModuleItem implements IConfigurableModule, ITextInputModule {

    private static final String NBT_KEY = "AnvilModes";

    public AnvilModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/anvil_module_overlay.png"),
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
    public boolean hasConfigSlot() {
        return true;
    }

    @Override
    public boolean useDefaultConfigSlotClick() {
        return false; // Text input panel is handled by MachineInfoScreen for ITextInputModule
    }

    @Override
    public void handleCustomClick(double mouseX, double mouseY, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex, Minecraft minecraft) {
        // Center config slot click is handled by MachineInfoScreen for ITextInputModule —
        // it opens the inline text input panel directly. This method is only called
        // for clicks outside the center slot (direction buttons U/D/L/R).
        super.handleCustomClick(mouseX, mouseY, slotX, slotY, stack, blockEntity, slotIndex, minecraft);
    }

    // --- ITextInputModule implementation ---

    @Override
    public Component getTextInputLabel() {
        return Component.translatable("screen.praeteritum_futurum.anvil_name_input.title");
    }

    @Override
    public Component getTextInputHint() {
        return Component.translatable("screen.praeteritum_futurum.anvil_name_input.hint");
    }

    @Override
    public String getExistingText(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("CustomName")) {
                return tag.getString("CustomName");
            }
        }
        return "";
    }

    @Override
    public void setText(ItemStack stack, String text) {
        if (text.isEmpty()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                tag.remove("CustomName");
                if (tag.isEmpty()) {
                    stack.remove(DataComponents.CUSTOM_DATA);
                } else {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            }
        } else {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
            tag.putString("CustomName", text);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    // --- IConfigurableModule implementation (no-op — inline panel handles text input) ---
    // The MachineInfoScreen handles text input inline for ITextInputModule implementations.

    // --- Config slot display for inline overlay ---

    @Override
    public ItemStack getConfigSlotItem(ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        String text = getExistingText(stack);
        if (!text.isEmpty()) {
            ItemStack nameTag = new ItemStack(Items.NAME_TAG);
            nameTag.set(DataComponents.CUSTOM_NAME, Component.literal(text));
            return nameTag;
        }
        return ItemStack.EMPTY;
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
        ResolvedIoSlots resolved = ProcessingModuleItem.resolveAdjacentIoSlots(blockEntity, slotIndex, modes);

        List<Integer> inputSlots = resolved.inputs();
        List<Integer> outputSlots = resolved.outputs();

        if (inputSlots.size() < 1 || outputSlots.isEmpty()) return;

        ItemStackHandler itemHandler = blockEntity.getInventoryHandler();

        // Find the main item and optional repair material/second item
        ItemStack mainItem = ItemStack.EMPTY;
        ItemStack secondItem = ItemStack.EMPTY;
        int mainSlot = -1;
        int secondSlot = -1;

        for (int inSlot : inputSlots) {
            ItemStack stackInSlot = itemHandler.getStackInSlot(inSlot);
            if (stackInSlot.isEmpty()) continue;

            if (mainItem.isEmpty()) {
                mainItem = stackInSlot;
                mainSlot = inSlot;
            } else if (secondItem.isEmpty()) {
                secondItem = stackInSlot;
                secondSlot = inSlot;
                break;
            }
        }

        if (mainItem.isEmpty()) return;

        // Check if the main item is damaged or can be combined
        @Nullable ItemStack result = null;

        if (!secondItem.isEmpty()) {
            // Try to repair or combine
            result = repairOrCombine(level, mainItem, secondItem);
        }

        // Rename-only mode: if there's only one input and a name is configured, allow renaming
        if (result == null && secondItem.isEmpty()) {
            String customNameText = getExistingText(stack);
            if (!customNameText.isEmpty()) {
                result = mainItem.copy();
                result.set(DataComponents.CUSTOM_NAME, Component.literal(customNameText));
                // Increase repair cost for rename operation
                int currentCost = mainItem.getOrDefault(DataComponents.REPAIR_COST, 0);
                result.set(DataComponents.REPAIR_COST, currentCost + 1);
            }
        }

        if (result == null) return;

        // Check repair cost against config (0 = no limit)
        int repairCost = result.getOrDefault(DataComponents.REPAIR_COST, 0);
        int maxCost = Config.getAnvilMaxRepairCost();
        if (maxCost > 0 && repairCost > maxCost) return; // Too expensive

        // Apply custom name from config slot if a name is set
        String customNameText = getExistingText(stack);
        if (!customNameText.isEmpty()) {
            result.set(DataComponents.CUSTOM_NAME, Component.literal(customNameText));
        }

        // Try to output the result
        for (int outSlot : outputSlots) {
            if (outSlot == mainSlot || outSlot == secondSlot) continue;

            ItemStack currentOut = itemHandler.getStackInSlot(outSlot);

            if (currentOut.isEmpty()) {
                itemHandler.setStackInSlot(outSlot, result.copy());
                itemHandler.extractItem(mainSlot, 1, false);
                if (secondSlot != -1) {
                    itemHandler.extractItem(secondSlot, 1, false);
                }
                return;
            } else if (ItemStack.isSameItemSameComponents(currentOut, result) && currentOut.getCount() < currentOut.getMaxStackSize()) {
                currentOut.grow(1);
                itemHandler.extractItem(mainSlot, 1, false);
                if (secondSlot != -1) {
                    itemHandler.extractItem(secondSlot, 1, false);
                }
                return;
            }
        }
    }

    @Nullable
    private ItemStack repairOrCombine(Level level, ItemStack mainItem, ItemStack secondItem) {
        // Case 1: Enchanted book — apply book enchantments to the main item
        if (secondItem.is(Items.ENCHANTED_BOOK)) {
            return applyBookEnchantments(level, mainItem, secondItem);
        }

        // Case 2: Same item type — combine enchantments or repair
        if (mainItem.is(secondItem.getItem())) {
            return combineItems(level, mainItem, secondItem);
        }

        // Case 3: Repair material — repair the main item
        if (isRepairMaterial(mainItem, secondItem)) {
            return repairWithMaterial(mainItem, secondItem);
        }

        return null;
    }

    /**
     * Applies enchanted book enchantments to an item, matching vanilla anvil behavior.
     * Only enchantments supported by the item are applied.
     */
    @Nullable
    private ItemStack applyBookEnchantments(Level level, ItemStack item, ItemStack book) {
        List<Map.Entry<Holder<Enchantment>, Integer>> bookEnchantments;

        // Try to get stored enchantments (for enchanted books)
        if (book.has(DataComponents.STORED_ENCHANTMENTS)) {
            ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
            if (stored != null && !stored.isEmpty()) {
                bookEnchantments = new ArrayList<>(stored.entrySet());
            } else {
                return null;
            }
        } else {
            return null;
        }

        // Check if the book has any enchantments applicable to the item
        boolean hasApplicable = false;
        for (Map.Entry<Holder<Enchantment>, Integer> entry : bookEnchantments) {
            if (isEnchantmentApplicable(entry.getKey(), item, level)) {
                hasApplicable = true;
                break;
            }
        }
        if (!hasApplicable) return null;

        ItemStack result = item.copy();
        ItemEnchantments itemEnchantments = result.get(DataComponents.ENCHANTMENTS);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                itemEnchantments != null ? itemEnchantments : ItemEnchantments.EMPTY
        );

        // Apply each book enchantment
        for (Map.Entry<Holder<Enchantment>, Integer> entry : bookEnchantments) {
            Holder<Enchantment> enchantHolder = entry.getKey();
            int bookLevel = entry.getValue();

            // Skip enchantments not applicable to this item
            if (!isEnchantmentApplicable(enchantHolder, result, level)) continue;

            // Check if this enchantment already exists on the item
            int currentLevel = 0;
            ItemEnchantments currentEnchants = mutable.toImmutable();
            for (Map.Entry<Holder<Enchantment>, Integer> existing : currentEnchants.entrySet()) {
                if (existing.getKey().value() == enchantHolder.value()) {
                    currentLevel = existing.getValue();
                    break;
                }
            }

            if (currentLevel > 0) {
                // Same enchantment already on item
                if (bookLevel > currentLevel) {
                    // Book has higher level — upgrade
                    mutable.set(enchantHolder, bookLevel);
                } else if (bookLevel == currentLevel) {
                    // Same level — try to upgrade (capped by enchantment max level)
                    int maxLevel = enchantHolder.value().definition().maxLevel();
                    if (currentLevel < maxLevel) {
                        mutable.set(enchantHolder, currentLevel + 1);
                    }
                    // Otherwise keep at max level
                }
                // If book level is lower, keep the item's higher level
            } else {
                // New enchantment — add it
                mutable.set(enchantHolder, bookLevel);
            }
        }

        EnchantmentHelper.setEnchantments(result, mutable.toImmutable());

        // Increase repair cost
        int currentCost = item.getOrDefault(DataComponents.REPAIR_COST, 0);
        result.set(DataComponents.REPAIR_COST, currentCost + 1);

        return result;
    }

    /**
     * Checks if an enchantment can be applied to an item.
     * Returns true for curse enchantments and enchantments that support the item.
     */
    private boolean isEnchantmentApplicable(Holder<Enchantment> enchantHolder, ItemStack item, Level level) {
        Enchantment enchantment = enchantHolder.value();

        // Curses can always be applied (they come from the source)
        if (enchantHolder.is(EnchantmentTags.CURSE)) {
            return true;
        }

        // Check if the enchantment supports this item via its definition's supported items
        return enchantment.definition().supportedItems().stream()
                .anyMatch(h -> h.value() == item.getItem());
    }

    /**
     * Combines two items of the same type, merging enchantments and repairing durability.
     * Matches vanilla anvil behavior: same enchantments upgrade, different enchantments combine,
     * and incompatible enchantments on the target are removed.
     */
    @Nullable
    private ItemStack combineItems(Level level, ItemStack first, ItemStack second) {
        ItemStack result = first.copy();

        ItemEnchantments firstEnchantments = first.get(DataComponents.ENCHANTMENTS);
        ItemEnchantments secondEnchantments = second.get(DataComponents.ENCHANTMENTS);

        if (firstEnchantments != null || secondEnchantments != null) {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                    firstEnchantments != null ? firstEnchantments : ItemEnchantments.EMPTY
            );

            if (secondEnchantments != null) {
                for (Map.Entry<Holder<Enchantment>, Integer> entry : secondEnchantments.entrySet()) {
                    Holder<Enchantment> enchantHolder = entry.getKey();
                    int secondLevel = entry.getValue();

                    // Skip enchantments not applicable to this item
                    if (!isEnchantmentApplicable(enchantHolder, result, level)) continue;

                    // Check if this enchantment already exists on the first item
                    int firstLevel = 0;
                    ItemEnchantments currentEnchants = mutable.toImmutable();
                    for (Map.Entry<Holder<Enchantment>, Integer> existing : currentEnchants.entrySet()) {
                        if (existing.getKey().value() == enchantHolder.value()) {
                            firstLevel = existing.getValue();
                            break;
                        }
                    }

                    if (firstLevel > 0) {
                        // Same enchantment on both items
                        if (secondLevel == firstLevel) {
                            // Same level — upgrade (capped by max level)
                            int maxLevel = enchantHolder.value().definition().maxLevel();
                            if (firstLevel < maxLevel) {
                                mutable.set(enchantHolder, firstLevel + 1);
                            }
                        } else if (secondLevel > firstLevel) {
                            // Second has higher level — use it
                            mutable.set(enchantHolder, secondLevel);
                        }
                        // If first level is higher, keep it
                    } else {
                        // New enchantment from second item — add it
                        mutable.set(enchantHolder, secondLevel);
                    }
                }
            }

            EnchantmentHelper.setEnchantments(result, mutable.toImmutable());
        }

        // Repair durability from the second item
        int maxDamage = result.getMaxDamage();
        if (maxDamage > 0) {
            int currentDamage = result.getDamageValue();
            int secondDamage = second.getDamageValue();

            // Repair amount: second item's remaining durability + 12% of max
            int repairAmount = (maxDamage - secondDamage) + (int) (maxDamage * 0.12F);
            int newDamage = Math.max(0, currentDamage - repairAmount);
            result.setDamageValue(newDamage);
        }

        // Increase repair cost
        int cost1 = first.getOrDefault(DataComponents.REPAIR_COST, 0);
        int cost2 = second.getOrDefault(DataComponents.REPAIR_COST, 0);
        int newCost = Math.max(cost1, cost2) + 1;
        result.set(DataComponents.REPAIR_COST, newCost);

        return result;
    }

    @Nullable
    private ItemStack repairWithMaterial(ItemStack item, ItemStack material) {
        int maxDamage = item.getMaxDamage();
        int currentDamage = item.getDamageValue();
        if (currentDamage <= 0) return null;

        // Calculate repair amount based on material
        int repairAmount;
        if (material.is(Items.IRON_INGOT)) {
            repairAmount = maxDamage / 9;
        } else if (material.is(Items.GOLD_INGOT)) {
            repairAmount = maxDamage / 9;
        } else if (material.is(Items.DIAMOND)) {
            repairAmount = maxDamage / 5;
        } else if (material.is(Items.NETHERITE_INGOT)) {
            repairAmount = maxDamage / 3;
        } else if (material.is(item.getItem())) {
            // Same material as the item itself
            repairAmount = maxDamage / 2;
        } else {
            return null;
        }

        ItemStack result = item.copy();
        int newDamage = Math.max(0, currentDamage - repairAmount);
        result.setDamageValue(newDamage);

        // Increase repair cost
        int currentCost = item.getOrDefault(DataComponents.REPAIR_COST, 0);
        result.set(DataComponents.REPAIR_COST, currentCost + 1);

        return result;
    }

    private boolean isRepairMaterial(ItemStack item, ItemStack material) {
        // Standard repair materials
        if (material.is(Items.IRON_INGOT) || material.is(Items.GOLD_INGOT) ||
                material.is(Items.DIAMOND) || material.is(Items.NETHERITE_INGOT)) {
            return item.getMaxDamage() > 0;
        }

        // Same item type (e.g., pickaxe + pickaxe)
        if (material.is(item.getItem())) {
            return true;
        }

        return false;
    }
}
