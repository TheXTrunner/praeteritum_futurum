package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.ModItems;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.inventory.ModuleAwareSlotItemHandler;
import com.thextrunner.praeteritum_futurum.inventory.ModuleConfigSlotItemHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MachineConfigMenu extends BaseMachineContainerMenu {
    private final boolean isConfigMode;

    public MachineConfigMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                (MachineFrameworkBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos())
        );
    }

    public MachineConfigMenu(int containerId, Inventory playerInventory, MachineFrameworkBlockEntity blockEntity) {
        super(ModMenuTypes.MACHINE_CONFIG_MENU.get(), containerId, blockEntity != null ? blockEntity.getBlockPos() : null);

        Player player = playerInventory.player;
        this.isConfigMode = player.getMainHandItem().is(ModItems.YOOL.get()) || player.getOffhandItem().is(ModItems.YOOL.get());

        // 1. Container Slots (27 slots) - Switches between module handler and inventory handler
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9;
                assert blockEntity != null;
                if (isConfigMode) {
                    this.addSlot(new ModuleConfigSlotItemHandler(
                            blockEntity,
                            index,
                            8 + col * 18,
                            18 + row * 18
                    ));
                } else {
                    this.addSlot(new ModuleAwareSlotItemHandler(
                            blockEntity,
                            index,
                            8 + col * 18,
                            18 + row * 18
                    ));
                }
            }
        }

        // 2. Player Inventory Main Slots (27 slots)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                ));
            }
        }

        // 3. Player Hotbar Slots (9 slots)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,
                    142
            ));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 27) {
                if (isConfigMode) {
                    if (slot instanceof ModuleConfigSlotItemHandler configSlot && !configSlot.mayPickup(player)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (slot instanceof ModuleAwareSlotItemHandler awareSlot && !awareSlot.mayPickup(player)) {
                        return ItemStack.EMPTY;
                    }
                }
                if (!this.moveItemStackTo(itemstack1, 27, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (isConfigMode) {
                    if (itemstack1.is(com.thextrunner.praeteritum_futurum.ModTags.Items.MACHINE_MODULES)) {
                        ItemStack singleModule = itemstack1.copyWithCount(1);
                        if (this.moveItemStackTo(singleModule, 0, 27, false)) {
                            itemstack1.shrink(1);
                        } else {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.set(itemstack1);
            }
        }
        return itemstack;
    }
}