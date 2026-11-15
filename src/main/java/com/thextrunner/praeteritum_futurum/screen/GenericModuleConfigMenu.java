package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
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

public class GenericModuleConfigMenu extends BaseMachineContainerMenu {
    private final ItemStack moduleStack;
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

    /**
     * Sets the carried (cursor) stack. Used by custom click handlers to update
     * the cursor when picking up or swapping items. Uses reflection to access
     * the private field in AbstractContainerMenu.
     */
    public void setCarried(ItemStack stack) {
        try {
            java.lang.reflect.Field field = AbstractContainerMenu.class.getDeclaredField("carried");
            field.setAccessible(true);
            field.set(this, stack);
        } catch (Exception e) {
            PraeteritumFuturumMod.LOGGER.error("Failed to set carried stack", e);
        }
    }

    @Override
    public void removed(@NotNull Player player) {
        // Save any pending inventory data from the module slots back to the module stack
        if (moduleStack.getItem() instanceof IModuleMenuProvider provider) {
            saveModuleInventory(provider, player);
        }
        // Save the module stack back to the machine's module handler when the menu closes
        if (player.level().getBlockEntity(blockPos) instanceof MachineFrameworkBlockEntity be) {
            be.getModuleHandler().setStackInSlot(slotIndexInMachine, moduleStack);
        }
        super.removed(player);
    }

    /**
     * Saves the inventory data from module slots back to the module stack.
     * This ensures that items placed in the module's slots are persisted to NBT.
     */
    private void saveModuleInventory(IModuleMenuProvider provider, Player player) {
        // Import the specific implementations that need save on close
        if (provider instanceof com.thextrunner.praeteritum_futurum.item.custom.FilterModuleItem) {
            com.thextrunner.praeteritum_futurum.item.custom.FilterModuleItem.saveFilterInventory(
                    moduleStack, com.thextrunner.praeteritum_futurum.item.custom.FilterModuleItem.getFilterInventory(moduleStack, player.registryAccess()), player.registryAccess());
        }
    }
}