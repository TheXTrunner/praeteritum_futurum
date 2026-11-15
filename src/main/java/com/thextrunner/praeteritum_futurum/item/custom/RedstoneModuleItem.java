package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.api.module.RedstoneCircuit;
import com.thextrunner.praeteritum_futurum.api.module.RedstoneComponent;
import com.thextrunner.praeteritum_futurum.api.module.IConfigurableModule;
import com.thextrunner.praeteritum_futurum.api.module.IModuleMenuProvider;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.screen.RedstoneConfigMenu;
import com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigMenu;
import com.thextrunner.praeteritum_futurum.screen.ModMenuTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Redstone Module — acts like a modified double chest (54-slot inventory) for redstone components.
 * <p>
 * Players place vanilla redstone items (dust, repeater, comparator, torch) into the 54-slot grid.
 * The module stores these items in NBT (like a real chest) and evaluates the circuit logic every tick.
 * Right-click rotates components; right-click on empty slots toggles wall attachments for torches.
 */
public class RedstoneModuleItem extends LogisticsModuleItem implements IModuleMenuProvider {

    public static final int GRID_SIZE = RedstoneCircuit.GRID_SIZE; // 54 slots

    public RedstoneModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/redstone_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16,
                16, 16
        );
        super.renderModuleOverlay(graphics, slot, stack, blockEntity);
    }

    @Override
    public void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos) {
        SimpleMenuProvider provider = new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new RedstoneConfigMenu(
                        ModMenuTypes.REDSTONE_MODULE_CONFIG_MENU.get(),
                        containerId, playerInventory, moduleStack, slotIndex, blockPos),
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
        // Load the item inventory from NBT (like a real chest)
        ItemStackHandler inventory = getRedstoneInventory(moduleStack, playerInventory.player.registryAccess());

        // Add slots in a 9×6 grid layout (matching double chest: 9 columns × 6 rows)
        for (int row = 0; row < RedstoneCircuit.GRID_HEIGHT; row++) {
            for (int col = 0; col < RedstoneCircuit.GRID_WIDTH; col++) {
                int index = row * RedstoneCircuit.GRID_WIDTH + col;
                slotAdder.accept(new SlotItemHandler(inventory, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        // Only allow redstone component items
                        return isRedstoneComponentItem(stack);
                    }

                    @Override
                    public boolean mayPickup(@NotNull Player player) {
                        return true; // Allow picking up components
                    }
                });
            }
        }
    }

    @Override
    public void handleMenuClick(GenericModuleConfigMenu menu, int slotId, int button, ClickType clickType, Player player) {
        // Right-click on circuit slots handles rotation and wall attachments
        if (slotId >= 0 && slotId < GRID_SIZE && button == 1) {
            RedstoneCircuit circuit = RedstoneCircuit.fromStack(menu.getModuleStack(), player.registryAccess());
            ItemStackHandler inventory = getRedstoneInventory(menu.getModuleStack(), player.registryAccess());

            if (inventory.getStackInSlot(slotId).isEmpty()) {
                // Right click on empty slot: toggle wall attachment
                circuit.toggleWallAttachment(slotId);
            } else {
                // Right click on occupied slot: rotate component
                circuit.rotateComponent(slotId);
            }
            circuit.toStack(menu.getModuleStack());
            menu.broadcastChanges();
            return;
        }

        // All other clicks: use vanilla slot handling (place, remove, swap items)
        menu.superClicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack handleQuickMove(GenericModuleConfigMenu menu, Player player, int index) {
        // Allow shift-clicking redstone components from inventory into the grid
        List<Slot> slots = menu.slots;
        if (index >= GRID_SIZE && index < slots.size()) {
            Slot slot = slots.get(index);
            if (slot.hasItem() && isRedstoneComponentItem(slot.getItem())) {
                ItemStackHandler inventory = getRedstoneInventory(menu.getModuleStack(), player.registryAccess());
                for (int i = 0; i < GRID_SIZE; i++) {
                    if (inventory.getStackInSlot(i).isEmpty()) {
                        inventory.setStackInSlot(i, slot.getItem().copyWithCount(1));
                        saveRedstoneInventory(menu.getModuleStack(), inventory, player.registryAccess());
                        break;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    // --- Item Inventory Storage (NBT-backed, like a real chest) ---

    /**
     * Loads the 54-slot item inventory from the module's NBT data.
     */
    public static ItemStackHandler getRedstoneInventory(ItemStack stack, HolderLookup.Provider registries) {
        ItemStackHandler handler = new ItemStackHandler(GRID_SIZE);
        CompoundTag filterTag = getCustomTag(stack, "RedstoneInventory");
        if (!filterTag.isEmpty()) {
            handler.deserializeNBT(registries, filterTag);
        }
        return handler;
    }

    /**
     * Saves the 54-slot item inventory to the module's NBT data.
     */
    public static void saveRedstoneInventory(ItemStack stack, ItemStackHandler handler, HolderLookup.Provider registries) {
        CompoundTag serialized = handler.serializeNBT(registries);
        modifyCustomData(stack, "RedstoneInventory", tag -> {
            for (String key : serialized.getAllKeys()) {
                tag.put(key, Objects.requireNonNull(serialized.get(key)));
            }
        });

        // Update stack size to reflect whether the module has components
        boolean hasItems = false;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }
        updateStackSize(stack, hasItems);
    }

    // --- Circuit Data Sync ---

    /**
     * Syncs the RedstoneCircuit metadata (rotations, wall attachments) from the item inventory.
     * The circuit reads which slots have items from the inventory, but keeps its own metadata.
     * Called during tickModule to ensure circuit state matches the stored items.
     */
    private RedstoneCircuit syncCircuitFromInventory(ItemStack stack, HolderLookup.Provider registries) {
        ItemStackHandler inventory = getRedstoneInventory(stack, registries);
        RedstoneCircuit circuit = RedstoneCircuit.fromStack(stack, registries);

        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack slotItem = inventory.getStackInSlot(i);
            if (slotItem.isEmpty()) {
                if (!circuit.isEmpty(i)) {
                    circuit.setComponent(i, null);
                }
            } else {
                RedstoneComponent component = getComponentFromItem(slotItem);
                if (component != null) {
                    circuit.setComponent(i, component);
                }
            }
        }
        circuit.toStack(stack);
        return circuit;
    }

    @Override
    public void tickModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        // Respect the redstone evaluation tick rate config
        int tickRate = Config.getRedstoneEvaluationTickRate();
        if (level.getGameTime() % tickRate != 0) {
            return;
        }

        // Sync circuit metadata with the actual stored items
        RedstoneCircuit circuit = syncCircuitFromInventory(stack, level.registryAccess());

        // Get input signals from adjacent machine slots
        int[] inputSignals = getInputSignals(level, pos, blockEntity, slotIndex);

        // Propagate signals through the circuit
        int[] outputSignals = circuit.propagateSignals(inputSignals);

        // Save updated circuit state
        circuit.toStack(stack);

        // Send output signals to adjacent slots
        setOutputSignals(blockEntity, slotIndex, outputSignals);
    }

    /**
     * Gets input signals from adjacent machine slots.
     */
    private int[] getInputSignals(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        int[] signals = new int[6];

        // Check adjacent blocks for redstone power
        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = pos.relative(dir);
            int power = level.getSignal(adjacentPos, dir.getOpposite());
            signals[dir.getStepY() + 2] = Math.min(15, power); // Map direction to signal index
        }

        return signals;
    }

    /**
     * Sets output signals to adjacent machine slots.
     */
    private void setOutputSignals(MachineFrameworkBlockEntity blockEntity, int slotIndex, int[] outputSignals) {
        // Store output signals in module data channels
        for (int i = 0; i < Math.min(outputSignals.length, 4); i++) {
            blockEntity.setModuleData(slotIndex, i, outputSignals[i]);
        }
    }

    /**
     * Checks if an item stack is a valid redstone component (vanilla items).
     */
    private boolean isRedstoneComponentItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return getComponentFromItem(stack) != null;
    }

    /**
     * Maps vanilla redstone items to RedstoneComponent enum values.
     * - Redstone Dust → Wire
     * - Repeater → Repeater
     * - Comparator → Comparator
     * - Redstone Torch → Torch
     */
    private RedstoneComponent getComponentFromItem(ItemStack stack) {
        return switch (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()) {
            case "redstone_wire" -> RedstoneComponent.WIRE;
            case "repeater" -> RedstoneComponent.REPEATER;
            case "comparator" -> RedstoneComponent.COMPARATOR;
            case "redstone_torch" -> RedstoneComponent.TORCH;
            default -> null;
        };
    }

    /**
     * Creates a vanilla item stack for a redstone component.
     */
    private ItemStack getComponentItemStack(RedstoneComponent component) {
        Item item = switch (component) {
            case WIRE -> Items.REDSTONE;
            case REPEATER -> Items.REPEATER;
            case COMPARATOR -> Items.COMPARATOR;
            case TORCH -> Items.REDSTONE_TORCH;
        };
        return new ItemStack(item);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        if (context.registries() != null) {
            ItemStackHandler inventory = getRedstoneInventory(stack, context.registries());
            int componentCount = 0;
            for (int i = 0; i < GRID_SIZE; i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) componentCount++;
            }

            tooltipComponents.add(Component.literal("Circuit: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(componentCount + " / " + GRID_SIZE + " slots").withStyle(
                            componentCount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED)));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
