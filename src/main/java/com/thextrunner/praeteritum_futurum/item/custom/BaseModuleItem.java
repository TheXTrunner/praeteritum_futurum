package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BaseModuleItem extends Item {
    public BaseModuleItem(Properties properties) {
        super(properties);
    }

    // Default renderer does nothing; subclasses override this
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
    }

    // Default tick does nothing; processing modules override this
    public void tickModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
    }

    // In BaseModuleItem.java
    public boolean isConfigurable() {
        return false;
    }

    public void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos) {
        // Default does nothing
    }
}