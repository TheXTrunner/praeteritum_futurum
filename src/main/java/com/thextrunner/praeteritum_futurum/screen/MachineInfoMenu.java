package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.inventory.ModuleAwareSlotItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MachineInfoMenu extends BaseMachineContainerMenu {
    private final MachineFrameworkBlockEntity blockEntity;

    public MachineInfoMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntityFromBuffer(playerInventory, extraData));
    }

    private static MachineFrameworkBlockEntity getBlockEntityFromBuffer(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof MachineFrameworkBlockEntity be) {
            return be;
        }
        return null;
    }

    public MachineInfoMenu(int containerId, Inventory playerInventory, MachineFrameworkBlockEntity blockEntity) {
        super(ModMenuTypes.MACHINE_INFO_MENU.get(), containerId, blockEntity != null ? blockEntity.getBlockPos() : BlockPos.ZERO);
        this.blockEntity = blockEntity;

        this.addDataSlots(new net.minecraft.world.inventory.ContainerData() {
            @Override
            public int get(int index) {
                if (blockEntity == null) return 0;
                int slot = index / 4;
                int channel = index % 4;
                return blockEntity.getModuleData(slot, channel);
            }

            @Override
            public void set(int index, int value) {
                if (blockEntity == null) return;
                int slot = index / 4;
                int channel = index % 4;
                blockEntity.setModuleData(slot, channel, value);
            }

            @Override
            public int getCount() {
                return 108;
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9;
                this.addSlot(new ModuleAwareSlotItemHandler(
                        blockEntity,
                        index,
                        8 + col * 18,
                        18 + row * 18
                ));
            }
        }

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

            int totalMachineSlots = 27;

            if (index < totalMachineSlots) {
                if (!this.moveItemStackTo(itemstack1, totalMachineSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemstack1, 0, totalMachineSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public MachineFrameworkBlockEntity getBlockEntity() {
        return blockEntity;
    }
}