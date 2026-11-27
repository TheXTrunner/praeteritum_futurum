package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.api.module.IModuleMenuProvider;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Container menu for configuring a single module's settings.
 * <p>
 * Used by modules that implement IModuleMenuProvider (Filter, Redstone, etc.).
 * Provides the module's custom slots plus the player's inventory. Handles
 * click events and item transfer between the module's slots and the player's inventory.
 */
public class GenericModuleConfigMenu extends BaseMachineContainerMenu {
    /** The module item stack being configured. */
    private final ItemStack moduleStack;

    /** The slot index of the module in the machine's grid. */
    private final int slotIndexInMachine;

    public GenericModuleConfigMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, ItemStack.STREAM_CODEC.decode(extraData), extraData.readInt(), extraData.readBlockPos());
    }

    public GenericModuleConfigMenu(int containerId, Inventory playerInventory, ItemStack moduleStack, int slotIndexInMachine, BlockPos blockPos) {
        this(ModMenuTypes.GENERIC_MODULE_CONFIG_MENU.get(), containerId, playerInventory, moduleStack, slotIndexInMachine, blockPos);
    }

    /**
     * Constructor allowing a custom menu type.
     * Third-party modules can use this to associate their own MenuType with the generic config menu,
     * enabling custom screen renderers via the screen registration system.
     *
     * @param menuType the menu type for this instance (determines which screen is rendered on the client)
     * @param containerId the container ID
     * @param playerInventory the player's inventory
     * @param moduleStack the module item stack being configured
     * @param slotIndexInMachine the slot index of this module in the machine grid
     * @param blockPos the position of the machine block
     */
    public GenericModuleConfigMenu(MenuType<?> menuType, int containerId, Inventory playerInventory, ItemStack moduleStack, int slotIndexInMachine, BlockPos blockPos) {
        super(menuType, containerId, blockPos);
        this.moduleStack = moduleStack;
        this.slotIndexInMachine = slotIndexInMachine;

        if (moduleStack.getItem() instanceof IModuleMenuProvider menuProvider) {
            menuProvider.addModuleSlots(playerInventory, moduleStack, blockPos, this::addSlot);
        }

        // Add player inventory with default positions (subclasses can override getInventoryYPositions)
        int[] positions = getInventoryYPositions();
        addPlayerInventory(playerInventory, positions[0], positions[1]);
    }

    /**
     * Returns the Y positions for the player inventory slots.
     * Override this in subclasses to change the layout (e.g., double-chest layout).
     * @return int array of size 2: [mainInventoryY, hotbarY]
     */
    protected int[] getInventoryYPositions() {
        // Default: single-chest layout (for Filter module with 27 slots)
        return new int[]{84, 142};
    }

    /**
     * Adds player inventory slots at the specified Y positions.
     * @param playerInventory the player's inventory
     * @param mainInventoryY Y position of the main inventory (top row of 3-row section)
     * @param hotbarY Y position of the hotbar
     */
    protected void addPlayerInventory(Inventory playerInventory, int mainInventoryY, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, mainInventoryY + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
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

    /**
     * Returns the number of module-specific slots (before player inventory slots).
     * Override this in subclasses to specify the module slot count.
     */
    protected int getModuleSlotCount() {
        // Default: single-chest layout (27 slots, for Filter module)
        return 27;
    }

    /**
     * Implements vanilla-like shift-click behavior using {@link AbstractContainerMenu#moveItemStackTo}.
     * This respects {@link Slot#mayPlace()} and {@link Slot#getMaxStackSize()} overrides,
     * so it works like a normal double chest.
     */
    public ItemStack superQuickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack;
            int moduleSlotCount = getModuleSlotCount();
            // If clicked on player inventory slot, move to module slots
            if (index >= moduleSlotCount) {
                if (!this.moveItemStackTo(slotStack, 0, moduleSlotCount, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // If clicked on module slot, move to player inventory
            else {
                if (!this.moveItemStackTo(slotStack, moduleSlotCount, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
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

    @Override
    public void removed(@NotNull Player player) {
        // Save any pending inventory data from the module slots back to the module stack
        if (moduleStack.getItem() instanceof IModuleMenuProvider provider) {
            provider.saveMenuData(this, player.registryAccess());
        }
        // Save the module stack back to the machine's module handler when the menu closes
        if (player.level().getBlockEntity(blockPos) instanceof MachineFrameworkBlockEntity be) {
            be.getModuleHandler().setStackInSlot(slotIndexInMachine, moduleStack);
        }
        super.removed(player);
    }

}