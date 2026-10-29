package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.api.module.IModuleMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GenericModuleConfigMenu extends BaseMachineContainerMenu {
    private final ItemStack moduleStack;
    private final int slotIndexInMachine;

    public GenericModuleConfigMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, ItemStack.STREAM_CODEC.decode(extraData), extraData.readInt(), extraData.readBlockPos());
    }

    public GenericModuleConfigMenu(int containerId, Inventory playerInventory, ItemStack moduleStack, int slotIndexInMachine, BlockPos blockPos) {
        super(ModMenuTypes.GENERIC_MODULE_CONFIG_MENU.get(), containerId, blockPos);
        this.moduleStack = moduleStack;
        this.slotIndexInMachine = slotIndexInMachine;

        if (moduleStack.getItem() instanceof IModuleMenuProvider menuProvider) {
            menuProvider.addModuleSlots(playerInventory, moduleStack, blockPos, this::addSlot);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (moduleStack.getItem() instanceof IModuleMenuProvider provider) {
            provider.handleMenuClick(this, slotId, button, clickType, player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    public void superClicked(int slotId, int button, ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (moduleStack.getItem() instanceof IModuleMenuProvider provider) {
            return provider.handleQuickMove(this, player, index);
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getModuleStack() {
        return moduleStack;
    }

    public int getSlotIndexInMachine() {//more future proofing!!!!
        return slotIndexInMachine;
    }
}