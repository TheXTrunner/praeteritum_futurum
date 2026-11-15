package com.thextrunner.praeteritum_futurum.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Redstone Module circuit editor.
 * Extends GenericModuleConfigMenu with a double-chest layout (54 slots in a 9×6 grid)
 * and properly positioned player inventory below.
 */
public class RedstoneConfigMenu extends GenericModuleConfigMenu {

    public RedstoneConfigMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(ModMenuTypes.REDSTONE_MODULE_CONFIG_MENU.get(), containerId, playerInventory,
            ItemStack.STREAM_CODEC.decode(extraData), extraData.readInt(), extraData.readBlockPos());
    }

    public RedstoneConfigMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, ItemStack moduleStack, int slotIndexInMachine, BlockPos blockPos) {
        super(menuType, containerId, playerInventory, moduleStack, slotIndexInMachine, blockPos);
    }

    @Override
    protected int[] getInventoryYPositions() {
        // Double-chest layout: 54 slots (9×6 grid) start at y=18 and end at y=122
        // Player inventory at y=140 (main) and y=198 (hotbar) — matches vanilla double chest
        return new int[]{140, 198};
    }
}
