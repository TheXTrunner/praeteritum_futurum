package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.item.ModItems;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.api.module.IModuleMenuProvider;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigMenu;
import com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Filter Module — controls which items pass through adjacent IO and Inventory modules.
 * <p>
 * Extends LogisticsModuleItem to provide whitelist/blacklist filtering. When placed
 * adjacent to an IO module or Inventory module, it checks each item against its
 * 27-slot filter list. Items that don't match the filter criteria are blocked.
 * <p>
 * Two modes:
 * <ul>
 *   <li>Whitelist: only items in the filter list are allowed through</li>
 *   <li>Blacklist: all items except those in the filter list are allowed through</li>
 * </ul>
 * <p>
 * Implements IModuleMenuProvider to provide a custom 27-slot config screen where
 * players can drag and drop items to set their filter list.
 */
public class FilterModuleItem extends LogisticsModuleItem implements IModuleMenuProvider {

    /** Number of filter slots in the module's config screen (3×9 grid). */
    public static final int FILTER_SLOTS = 27;

    /**
     * Enum defining the filter modes — whitelist allows only listed items,
     * blacklist blocks only listed items.
     */
    public enum FilterMode {
        WHITELIST("Whitelist"),
        BLACKLIST("Blacklist");

        private final String label;

        FilterMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public FilterMode next() {
            return this == WHITELIST ? BLACKLIST : WHITELIST;
        }

        public static FilterMode fromId(int id) {
            return id >= 0 && id < values().length ? values()[id] : WHITELIST;
        }
    }

    /**
     * Constructs the Filter Module with the given properties.
     *
     * @param properties The item properties
     */
    public FilterModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/filter_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16,
                16, 16
        );
    }

    @Override
    public void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos) {
        SimpleMenuProvider provider = new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new GenericModuleConfigMenu(containerId, playerInventory, moduleStack, slotIndex, blockPos),
                moduleStack.getHoverName()
        );

        player.openMenu(provider, buf -> {
            ItemStack.STREAM_CODEC.encode(buf, moduleStack);
            buf.writeInt(slotIndex);
            buf.writeBlockPos(blockPos);
        });
    }

    @Override
    public void addModuleSlots(Inventory playerInventory, ItemStack moduleStack, BlockPos blockPos, Consumer<Slot> slotAdder) {
        ItemStackHandler ghostHandler = getFilterInventory(moduleStack, playerInventory.player.registryAccess());
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                slotAdder.accept(new SlotItemHandler(ghostHandler, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return true;
                    }

                    @Override
                    public boolean mayPickup(@NotNull Player player) {
                        return false;
                    }
                });
            }
        }
    }

    @Override
    public void saveMenuData(GenericModuleConfigMenu menu, HolderLookup.Provider registries) {
        ItemStackHandler inventory = new ItemStackHandler(FILTER_SLOTS);
        for (int slot = 0; slot < FILTER_SLOTS && slot < menu.slots.size(); slot++) {
            ItemStack filter = menu.slots.get(slot).getItem();
            inventory.setStackInSlot(slot, filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1));
        }
        saveFilterInventory(menu.getModuleStack(), inventory, registries);
    }

    @Override
    public void handleMenuClick(GenericModuleConfigMenu menu, int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 27) {
            ItemStack carried = menu.getCarried();
            ItemStackHandler ghostHandler = getFilterInventory(menu.getModuleStack(), player.registryAccess());

            if (carried.isEmpty()) {
                ghostHandler.setStackInSlot(slotId, ItemStack.EMPTY);
            } else {
                ItemStack ghostCopy = carried.copyWithCount(1);
                ghostHandler.setStackInSlot(slotId, ghostCopy);
            }
            saveFilterInventory(menu.getModuleStack(), ghostHandler, player.registryAccess());

            if (slotId < menu.slots.size()) {
                menu.slots.get(slotId).set(ghostHandler.getStackInSlot(slotId));
            }

            menu.broadcastChanges();
            return;
        }
        menu.superClicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack handleQuickMove(GenericModuleConfigMenu menu, Player player, int index) {
        List<Slot> slots = menu.slots;
        if (index >= 27 && index < slots.size()) {
            Slot slot = slots.get(index);
            if (slot.hasItem()) {
                ItemStack stackToFilter = slot.getItem();
                ItemStackHandler ghostHandler = getFilterInventory(menu.getModuleStack(), player.registryAccess());
                for (int i = 0; i < 27; i++) {
                    if (ghostHandler.getStackInSlot(i).isEmpty()) {
                        ghostHandler.setStackInSlot(i, stackToFilter.copyWithCount(1));
                        if (i < menu.slots.size()) {
                            menu.slots.get(i).set(ghostHandler.getStackInSlot(i));
                        }
                        saveFilterInventory(menu.getModuleStack(), ghostHandler, player.registryAccess());
                        break;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStackHandler getFilterInventory(ItemStack stack, HolderLookup.Provider registries) {
        ItemStackHandler handler = new ItemStackHandler(FILTER_SLOTS);
        CompoundTag filterTag = getCustomTag(stack, "FilterItems");
        if (!filterTag.isEmpty()) {
            handler.deserializeNBT(registries, filterTag);
        }
        return handler;
    }

    public static void saveFilterInventory(ItemStack stack, ItemStackHandler handler, HolderLookup.Provider registries) {
        CompoundTag serialized = handler.serializeNBT(registries);
        modifyCustomData(stack, "FilterItems", tag -> {
            for (String key : serialized.getAllKeys()) {
                tag.put(key, Objects.requireNonNull(serialized.get(key)));
            }
        });

        boolean hasItems = false;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        updateStackSize(stack, hasItems || getFilterMode(stack) != FilterMode.WHITELIST);
    }

    public static FilterMode getFilterMode(ItemStack stack) {
        CompoundTag tag = getCustomTag(stack, "FilterConfig");
        return tag.contains("Mode") ? FilterMode.fromId(tag.getInt("Mode")) : FilterMode.WHITELIST;
    }

    public static void toggleFilterMode(ItemStack stack) {
        FilterMode next = getFilterMode(stack).next();
        modifyCustomData(stack, "FilterConfig", tag -> tag.putInt("Mode", next.ordinal()));
        updateStackSize(stack, true);
    }

    public static boolean isItemBlockedByFilters(MachineFrameworkBlockEntity blockEntity, int ioModuleSlot, int targetSlotIndex, ItemStack stack) {
        Level level = blockEntity.getLevel();
        if (level == null) return false;
        HolderLookup.Provider registries = level.registryAccess();

        int[] ioNeighbors = blockEntity.getAdjacentSlots(ioModuleSlot);
        for (int adj : ioNeighbors) {
            ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(adj);
            if (moduleStack.is(ModItems.FILTER_MODULE.get())) {
                if (!test(moduleStack, stack, registries)) {
                    return true; // Blocked by IO filter
                }
            }
        }

        int[] invNeighbors = blockEntity.getAdjacentSlots(targetSlotIndex);
        for (int adj : invNeighbors) {
            ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(adj);
            if (moduleStack.is(ModItems.FILTER_MODULE.get())) {
                if (!test(moduleStack, stack, registries)) {
                    return true; // Blocked by Inventory filter
                }
            }
        }

        return false;
    }

    public static boolean test(ItemStack filterModuleStack, ItemStack targetStack, HolderLookup.Provider registries) {
        if (targetStack.isEmpty()) return false;
        ItemStackHandler handler = getFilterInventory(filterModuleStack, registries);
        FilterMode mode = getFilterMode(filterModuleStack);

        boolean foundMatch = false;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack filterItem = handler.getStackInSlot(i);
            if (!filterItem.isEmpty() && ItemStack.isSameItemSameComponents(filterItem, targetStack)) {
                foundMatch = true;
                break;
            }
        }

        if (mode == FilterMode.WHITELIST) {
            return foundMatch; // Whitelist: must match to pass
        } else {
            return !foundMatch; // Blacklist: must NOT match to pass
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        if (context.registries() != null) {
            ItemStackHandler handler = getFilterInventory(stack, context.registries());
            FilterMode mode = getFilterMode(stack);
            int activeFilters = 0;
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    activeFilters++;
                }
            }

            tooltipComponents.add(Component.literal("Mode: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(mode.getLabel()).withStyle(ChatFormatting.GOLD)));
            if (activeFilters > 0) {
                tooltipComponents.add(Component.literal("Status: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("Configured (" + activeFilters + " / " + FILTER_SLOTS + " slots)").withStyle(ChatFormatting.GREEN)));
            } else {
                tooltipComponents.add(Component.literal("Status: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("Empty").withStyle(ChatFormatting.RED)));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
