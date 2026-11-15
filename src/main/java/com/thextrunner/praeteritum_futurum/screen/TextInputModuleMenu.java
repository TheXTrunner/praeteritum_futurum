package com.thextrunner.praeteritum_futurum.screen;

import com.thextrunner.praeteritum_futurum.api.module.ITextInputModule;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Simple container menu for modules that use text input configuration.
 * <p>
 * This menu holds only the module stack and block position — it has no inventory
 * slots. The corresponding {@link TextInputModuleScreen} provides the text input UI.
 * <p>
 * Third-party modders can use this menu type for any module implementing
 * {@link ITextInputModule}.
 */
public class TextInputModuleMenu extends BaseMachineContainerMenu {
    private final ItemStack moduleStack;
    private final int slotIndexInMachine;

    public TextInputModuleMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, ItemStack.STREAM_CODEC.decode(extraData), extraData.readInt(), extraData.readBlockPos());
    }

    public TextInputModuleMenu(int containerId, Inventory playerInventory, ItemStack moduleStack, int slotIndexInMachine, BlockPos blockPos) {
        super(ModMenuTypes.TEXT_INPUT_MODULE_MENU.get(), containerId, blockPos);
        this.moduleStack = moduleStack;
        this.slotIndexInMachine = slotIndexInMachine;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    public ItemStack getModuleStack() {
        return moduleStack;
    }

    public int getSlotIndexInMachine() {
        return slotIndexInMachine;
    }

    public ITextInputModule getTextInputModule() {
        if (moduleStack.getItem() instanceof ITextInputModule textModule) {
            return textModule;
        }
        throw new IllegalStateException("Module does not implement ITextInputModule");
    }
}
