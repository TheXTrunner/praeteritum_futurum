package com.thextrunner.praeteritum_futurum.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Redstone Module circuit editor.
 * <p>
 * This is the Inventory Layer in the three-layer architecture. It knows only what items
 * are in the slots — like a normal inventory with a max stack size of 1 and filter rules
 * that only allow redstone component items. It does not run any circuit simulation or
 * rendering logic.
 * <p>
 * <b>Three-Layer Architecture:</b>
 * <ul>
 *   <li><b>Inventory Layer (this class)</b> — stores ItemStacks in 54 slots with filter rules</li>
 *   <li><b>Logic Layer</b> — {@link com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuit} reads
 *   from these slots, runs propagation, and computes signal strengths</li>
 *   <li><b>Render Layer</b> — {@link com.thextrunner.praeteritum_futurum.screen.RedstoneConfigScreen}
 *   reads from the Logic Layer's snapshot for display</li>
 * </ul>
 * <p>
 * Extends GenericModuleConfigMenu with a double-chest layout (54 slots in a 9×6 grid)
 * and properly positioned player inventory below. The slot count matches a vanilla double chest.
 */
public class RedstoneConfigMenu extends GenericModuleConfigMenu {

    /**
     * Constructs the menu from network data (client-side).
     *
     * @param containerId the container ID from the open window packet
     * @param playerInventory the player's inventory
     * @param extraData the buffer containing module stack, slot index, and block position
     */
    public RedstoneConfigMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(ModMenuTypes.REDSTONE_MODULE_CONFIG_MENU.get(), containerId, playerInventory,
            ItemStack.STREAM_CODEC.decode(extraData), extraData.readInt(), extraData.readBlockPos());
    }

    /**
     * Constructs the menu with explicit parameters (server-side).
     *
     * @param menuType the menu type registry entry
     * @param containerId the container ID
     * @param playerInventory the player's inventory
     * @param moduleStack the redstone module item stack
     * @param slotIndexInMachine the slot index in the machine framework
     * @param blockPos the position of the machine block entity
     */
    public RedstoneConfigMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, ItemStack moduleStack, int slotIndexInMachine, BlockPos blockPos) {
        super(menuType, containerId, playerInventory, moduleStack, slotIndexInMachine, blockPos);
    }

    @Override
    protected int[] getInventoryYPositions() {
        // Double-chest layout: 54 slots (9×6 grid) start at y=18 and end at y=122
        // Player inventory at y=140 (main) and y=198 (hotbar) — matches vanilla double chest
        return new int[]{140, 198};
    }

    @Override
    protected int getModuleSlotCount() {
        // Redstone module has 54 slots (9×6 grid), matching a vanilla double chest
        return 54;
    }
}
