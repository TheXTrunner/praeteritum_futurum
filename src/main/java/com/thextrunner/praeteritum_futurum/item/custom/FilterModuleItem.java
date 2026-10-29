package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.api.module.IModuleMenuProvider;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigMenu;
import com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class FilterModuleItem extends LogisticsModuleItem implements IModuleMenuProvider {

    public static final int FILTER_SLOTS = 27;

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
    public AbstractContainerScreen<?> createScreen(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        return new GenericModuleConfigScreen((GenericModuleConfigMenu) menu, playerInventory, title);
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

            // updates slot directly as interacting
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
        updateStackSize(stack, hasItems);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        if (context.registries() != null) {
            ItemStackHandler handler = getFilterInventory(stack, context.registries());
            int activeFilters = 0;
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    activeFilters++;
                }
            }

            if (activeFilters > 0) {
                tooltipComponents.add(Component.literal("§7Status: §aFilter Configured (" + activeFilters + " / " + FILTER_SLOTS + " slots)"));
            } else if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                tooltipComponents.add(Component.literal("§7Status: §eData Initialized (Empty)"));
            } else {
                tooltipComponents.add(Component.literal("§7Status: §cUnconfigured (Clean)"));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}