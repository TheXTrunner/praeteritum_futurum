package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.api.module.CyclingMode;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ejection Module — ejects items from adjacent inventory modules toward the world.
 * <p>
 * Direction buttons adapt to what module is adjacent:
 * - Adjacent Inventory module → cycles DISABLED → DISPENSE → DROP → CATCH
 * - Adjacent IO module → cycles DISABLED → ENABLED
 * - No relevant adjacent module → only DISABLED
 * <p>
 * DISPENSE uses vanilla dispenser behavior (arrows shoot, snowballs throw, shears shear, etc.).
 * DROP simply ejects items as entity drops. CATCH captures item entities spawned by DISPENSE
 * or DROP (e.g., from shearing sheep, bucket collecting, armor ejection) and routes them
 * to a designated output inventory slot instead of letting them fall to the ground.
 */
public class EjectionModuleItem extends LogisticsModuleItem {

    private static final String NBT_KEY = "EjectionModes";
    private static final String REDSTONE_PULSE_STATE_KEY = "EjectionRedstonePulse";
    private static final String LAST_SIGNAL_POWERED_KEY = "LastSignalPowered";
    private static final String PENDING_PULSES_KEY = "PendingPulses";
    private static final int MAX_PENDING_PULSES = 64;

    /**
     * Mode for directions adjacent to an Inventory module.
     */
    public enum InventoryMode implements CyclingMode<InventoryMode> {
        DISABLED("D", 0xFF555555),  // Disabled (Dark Gray)
        DISPENSE("S", 0xFF33CC33),  // Dispense with smart behavior (Green)
        DROP("R", 0xFFCC3333),      // Drop as entity (Red)
        CATCH("C", 0xFFCC9933);     // Catch returned items (Orange)

        private final String labelChar;
        private final int color;

        InventoryMode(String labelChar, int color) {
            this.labelChar = labelChar;
            this.color = color;
        }

        public String getLabelChar() {
            return labelChar;
        }

        public int getColor() {
            return color;
        }

        public static InventoryMode fromId(int id) {
            for (InventoryMode mode : values()) {
                if (mode.ordinal() == id) return mode;
            }
            return DISABLED;
        }

        @Override public InventoryMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override public InventoryMode previous() {
            return values()[(ordinal() - 1 + values().length) % values().length];
        }
    }

    /**
     * Mode for directions adjacent to an IO module.
     */
    public enum IoMode implements CyclingMode<IoMode> {
        DISABLED("D", 0xFF555555),  // Disabled (Dark Gray)
        ENABLED("E", 0xFF3366CC);   // Enabled (Blue)

        private final String labelChar;
        private final int color;

        IoMode(String labelChar, int color) {
            this.labelChar = labelChar;
            this.color = color;
        }

        public String getLabelChar() {
            return labelChar;
        }

        public int getColor() {
            return color;
        }

        public static IoMode fromId(int id) {
            for (IoMode mode : values()) {
                if (mode.ordinal() == id) return mode;
            }
            return DISABLED;
        }

        @Override public IoMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override public IoMode previous() {
            return values()[(ordinal() - 1 + values().length) % values().length];
        }
    }

    public EjectionModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean supportsRedstoneControl() {
        return true;
    }

    @Override
    public RedstoneControlMode[] getSupportedRedstoneControlModes() {
        return new RedstoneControlMode[]{
                RedstoneControlMode.IGNORE,
                RedstoneControlMode.HIGH,
                RedstoneControlMode.LOW,
                RedstoneControlMode.PULSE
        };
    }

    @Override
    public void setRedstoneControlMode(ItemStack stack, RedstoneControlMode mode) {
        super.setRedstoneControlMode(stack, mode);
        if (mode != RedstoneControlMode.PULSE && getRedstoneControlMode(stack) == mode) {
            modifyCustomData(stack, REDSTONE_PULSE_STATE_KEY, tag -> tag.putInt(PENDING_PULSES_KEY, 0));
        }
    }

    @Override
    public boolean isRedstoneControlSatisfied(ItemStack stack, MachineFrameworkBlockEntity blockEntity,
                                              int slotIndex) {
        int signalStrength = getRedstoneControlSignal(blockEntity, slotIndex);
        boolean signalPowered = signalStrength > 0;
        RedstoneControlMode mode = getRedstoneControlMode(stack);
        CompoundTag pulseState = getCustomTag(stack, REDSTONE_PULSE_STATE_KEY);
        boolean signalStateInitialized = pulseState.contains(LAST_SIGNAL_POWERED_KEY);
        boolean lastSignalPowered = pulseState.getBoolean(LAST_SIGNAL_POWERED_KEY);
        int pendingPulses = pulseState.getInt(PENDING_PULSES_KEY);

        if (!signalStateInitialized || signalPowered != lastSignalPowered) {
            boolean risingEdge = signalStateInitialized && signalPowered && !lastSignalPowered;
            int updatedPendingPulses = risingEdge && mode == RedstoneControlMode.PULSE
                    ? Math.min(MAX_PENDING_PULSES, pendingPulses + 1)
                    : pendingPulses;
            modifyCustomData(stack, REDSTONE_PULSE_STATE_KEY, tag -> {
                tag.putBoolean(LAST_SIGNAL_POWERED_KEY, signalPowered);
                tag.putInt(PENDING_PULSES_KEY, updatedPendingPulses);
            });
            pendingPulses = updatedPendingPulses;
        }

        if (mode == RedstoneControlMode.PULSE) {
            return pendingPulses > 0;
        }
        return mode.allowsSignal(signalStrength);
    }

    private boolean consumePendingPulse(ItemStack stack) {
        CompoundTag pulseState = getCustomTag(stack, REDSTONE_PULSE_STATE_KEY);
        int pendingPulses = pulseState.getInt(PENDING_PULSES_KEY);
        if (pendingPulses <= 0) return false;

        modifyCustomData(stack, REDSTONE_PULSE_STATE_KEY,
                tag -> tag.putInt(PENDING_PULSES_KEY, pendingPulses - 1));
        return true;
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/ejection_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16,
                16, 16
        );
    }

    @Override
    public boolean isInsideOverlay(double mouseX, double mouseY, int slotX, int slotY) {
        boolean center = isWithinBounds(mouseX, mouseY, slotX, slotY, 16, 16);
        boolean up = isWithinBounds(mouseX, mouseY, slotX, slotY - 18, 16, 16);
        boolean down = isWithinBounds(mouseX, mouseY, slotX, slotY + 18, 16, 20);
        boolean west = isWithinBounds(mouseX, mouseY, slotX - 18, slotY, 16, 16);
        boolean east = isWithinBounds(mouseX, mouseY, slotX + 18, slotY, 16, 16);
        boolean close = isWithinBounds(mouseX, mouseY, slotX - 18, slotY - 18, 16, 16);
        return center || up || down || west || east || close;
    }


    @Override
    public boolean opensInlineOverlay() {
        return true;
    }

    @Override
    public java.util.List<BaseModuleItem.AdjacentModuleType> getAllowedAdjacentModuleTypes() {
        return java.util.List.of(
                BaseModuleItem.AdjacentModuleType.INVENTORY,
                BaseModuleItem.AdjacentModuleType.IO
        );
    }

    @Override
    public void openConfiguration(net.minecraft.server.level.ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos) {
        // Client-side overlay handled via screen state
    }

    @Override
    public boolean isDirectionConfigured(ItemStack stack, GridDirection dir) {
        return getInventoryMode(stack, dir) != InventoryMode.DISABLED || getIoMode(stack, dir) != IoMode.DISABLED;
    }

    @Override
    public void resetDirection(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        clearDirectionModes(stack, dir);
    }

    @Override
    public void resetDirectionMode(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        clearDirectionModes(stack, dir);
    }

    private void clearDirectionModes(ItemStack stack, GridDirection dir) {
        setInventoryMode(stack, dir, InventoryMode.DISABLED);
        setIoMode(stack, dir, IoMode.DISABLED);
    }

    @Override
    public void handleCustomClick(double mouseX, double mouseY, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex, net.minecraft.client.Minecraft minecraft) {
        // Validate config — auto-disable directions that are no longer valid
        validateAndSyncDirectionConfig(stack, blockEntity, slotIndex);

        GridDirection clickedDir = getClickedDirection(slotX, slotY, mouseX, mouseY, slotIndex);
        if (clickedDir != null) {
            if (minecraft.player != null) {
                minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
            }

            BaseModuleItem.AdjacentModuleType adjType = BaseModuleItem.getAdjacentModuleType(blockEntity, slotIndex, clickedDir);

            if (adjType == BaseModuleItem.AdjacentModuleType.INVENTORY) {
                InventoryMode current = getInventoryMode(stack, clickedDir);
                InventoryMode next = isShiftHeld() ? current.previous() : current.next();
                setInventoryMode(stack, clickedDir, next);
            } else if (adjType == BaseModuleItem.AdjacentModuleType.IO) {
                IoMode current = getIoMode(stack, clickedDir);
                IoMode next = isShiftHeld() ? current.previous() : current.next();
                setIoMode(stack, clickedDir, next);
            } else {
                // No relevant adjacent module — ignore click
                return;
            }

            // Sync to server
            com.thextrunner.praeteritum_futurum.network.ServerboundToggleModuleDirectionPacket packet =
                    new com.thextrunner.praeteritum_futurum.network.ServerboundToggleModuleDirectionPacket(slotIndex, clickedDir, blockEntity.getBlockPos(), isShiftHeld());
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(packet);
        }
    }

    @Override
    public void renderCustomOverlay(GuiGraphics graphics, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, Font font) {
        int slotIndex = getOverlaySlotIndex();
        int centerX = slotX + 8;
        int offset = 18;

        GridDirection[] dirs = {GridDirection.UP, GridDirection.DOWN, GridDirection.LEFT, GridDirection.RIGHT};
        int[] buttonX = {centerX, centerX, slotX - (offset - 8), slotX + 16 + (offset - 8)};
        int[] buttonY = {slotY - (offset - 8), slotY + 16 + (offset - 8), slotY + 8, slotY + 8};

        for (int i = 0; i < dirs.length; i++) {
            GridDirection dir = dirs[i];
            if (slotIndex >= 0 && !BaseModuleItem.hasAdjacentSlotStatic(slotIndex, dir)) continue;

            BaseModuleItem.AdjacentModuleType adjType = slotIndex >= 0
                    ? BaseModuleItem.getAdjacentModuleType(blockEntity, slotIndex, dir)
                    : BaseModuleItem.AdjacentModuleType.NONE;

            // Determine the display mode for this button
            int modeColor;
            String modeLabel;

            if (adjType == BaseModuleItem.AdjacentModuleType.INVENTORY) {
                InventoryMode mode = getInventoryMode(stack, dir);
                modeColor = mode.getColor();
                modeLabel = mode.getLabelChar();
            } else if (adjType == BaseModuleItem.AdjacentModuleType.IO) {
                IoMode mode = getIoMode(stack, dir);
                modeColor = mode.getColor();
                modeLabel = mode.getLabelChar();
            } else {
                modeColor = InventoryMode.DISABLED.getColor();
                modeLabel = InventoryMode.DISABLED.getLabelChar();
            }

            drawEjectionButton(graphics, font, buttonX[i], buttonY[i], modeColor, modeLabel);
        }

        drawDarkHatchLid(graphics, slotX, slotY);

        drawCloseButton(graphics, font, slotX, slotY);
    }

    @Override
    public List<Component> getCustomOverlayTooltip(double mouseX, double mouseY, int slotX, int slotY,
                                                   ItemStack stack, MachineFrameworkBlockEntity blockEntity,
                                                   int slotIndex) {
        List<Component> baseTooltip = super.getCustomOverlayTooltip(mouseX, mouseY, slotX, slotY,
                stack, blockEntity, slotIndex);
        if (!baseTooltip.isEmpty()) return baseTooltip;

        GridDirection direction = getClickedDirection(slotX, slotY, mouseX, mouseY, slotIndex);
        if (direction == null) return List.of();

        AdjacentModuleType adjacentType = getAdjacentModuleType(blockEntity, slotIndex, direction);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(direction.name() + " connection").withStyle(ChatFormatting.AQUA));

        if (adjacentType == AdjacentModuleType.INVENTORY) {
            InventoryMode mode = getInventoryMode(stack, direction);
            lines.add(tooltipValue("Inventory mode: ", mode.name(), mode.getColor()));
            lines.add(Component.literal(getInventoryModeDescription(mode)).withStyle(ChatFormatting.GRAY));
            lines.add(tooltipHint("Click to cycle; Shift-click to cycle backward"));
        } else if (adjacentType == AdjacentModuleType.IO) {
            IoMode mode = getIoMode(stack, direction);
            lines.add(tooltipValue("I/O link: ", mode.name(), mode.getColor()));
            lines.add(Component.literal(mode == IoMode.ENABLED
                    ? "Marks this adjacent I/O link as enabled."
                    : "This adjacent I/O link is inactive.").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("No separate direction-specific ejection action is currently applied to this setting.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(tooltipHint("Click to cycle; Shift-click to cycle backward"));
        } else {
            lines.add(Component.literal("No compatible adjacent module").withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    private String getInventoryModeDescription(InventoryMode mode) {
        return switch (mode) {
            case DISABLED -> "No items are taken from this inventory for ejection.";
            case DISPENSE -> "Use dispenser behavior to eject items through an enabled I/O output face.";
            case DROP -> "Drop items as entities through an enabled I/O output face.";
            case CATCH -> "Catch returned item entities for routing back into an inventory.";
        };
    }

    private void drawEjectionButton(GuiGraphics graphics, Font font, int x, int y, int color, String text) {
        graphics.fill(x - 8, y - 8, x + 8, y + 8, 0xFF888888);
        graphics.fill(x - 7, y - 7, x + 7, y + 7, color);
        graphics.drawCenteredString(font, text, x, y - 4, 0xFFFFFF);
    }

    // --- NBT Storage ---

    private Map<GridDirection, InventoryMode> getInventoryModes(ItemStack stack) {
        Map<GridDirection, InventoryMode> modes = new EnumMap<>(GridDirection.class);
        for (GridDirection dir : GridDirection.VALUES) {
            modes.put(dir, InventoryMode.DISABLED);
        }
        CompoundTag tag = getCustomTag(stack, NBT_KEY);
        for (String key : tag.getAllKeys()) {
            GridDirection dir = GridDirection.fromNbtKey(key);
            if (dir != null && tag.contains(key)) {
                modes.put(dir, InventoryMode.fromId(tag.getInt(key)));
            }
        }
        return modes;
    }

    // Public for server-side packet handler access
    public InventoryMode getInventoryMode(ItemStack stack, GridDirection dir) {
        return getInventoryModes(stack).get(dir);
    }

    // Public for server-side packet handler access
    public void setInventoryMode(ItemStack stack, GridDirection dir, InventoryMode mode) {
        modifyCustomData(stack, NBT_KEY, tag -> tag.putInt(dir.nbtKey(), mode.ordinal()));
        updateStackSize(stack, hasAnyConfig(stack));
    }

    private Map<GridDirection, IoMode> getIoModes(ItemStack stack) {
        Map<GridDirection, IoMode> modes = new EnumMap<>(GridDirection.class);
        for (GridDirection dir : GridDirection.VALUES) {
            modes.put(dir, IoMode.DISABLED);
        }
        CompoundTag tag = getCustomTag(stack, NBT_KEY + "Io");
        for (String key : tag.getAllKeys()) {
            GridDirection dir = GridDirection.fromNbtKey(key);
            if (dir != null && tag.contains(key)) {
                modes.put(dir, IoMode.fromId(tag.getInt(key)));
            }
        }
        return modes;
    }

    // Public for server-side packet handler access
    public IoMode getIoMode(ItemStack stack, GridDirection dir) {
        return getIoModes(stack).get(dir);
    }

    // Public for server-side packet handler access
    public void setIoMode(ItemStack stack, GridDirection dir, IoMode mode) {
        modifyCustomData(stack, NBT_KEY + "Io", tag -> tag.putInt(dir.nbtKey(), mode.ordinal()));
        updateStackSize(stack, hasAnyConfig(stack));
    }

    private boolean hasAnyConfig(ItemStack stack) {
        Map<GridDirection, InventoryMode> invModes = getInventoryModes(stack);
        for (InventoryMode m : invModes.values()) {
            if (m != InventoryMode.DISABLED) return true;
        }
        Map<GridDirection, IoMode> ioModes = getIoModes(stack);
        for (IoMode m : ioModes.values()) {
            if (m != IoMode.DISABLED) return true;
        }
        return false;
    }

    @Override
    public void toggleDirectionMode(ItemStack stack, GridDirection direction, MachineFrameworkBlockEntity blockEntity, int slotIndex, boolean shiftHeld) {
        AdjacentModuleType adjType = getAdjacentModuleType(blockEntity, slotIndex, direction);

        if (adjType == AdjacentModuleType.INVENTORY) {
            InventoryMode current = getInventoryMode(stack, direction);
            InventoryMode next = shiftHeld ? current.previous() : current.next();
            setInventoryMode(stack, direction, next);
        } else if (adjType == AdjacentModuleType.IO) {
            IoMode current = getIoMode(stack, direction);
            IoMode next = shiftHeld ? current.previous() : current.next();
            setIoMode(stack, direction, next);
        }
    }

    @Override
    public void tickModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        if (level.isClientSide()) return;

        // Respect the configured tick rate for ejection (shares IO transfer tick rate)
        int tickRate = Config.getIoTransferTickRate();
        if (level.getGameTime() % tickRate != 0) {
            return;
        }

        ItemStackHandler moduleHandler = blockEntity.getModuleHandler();
        ItemStackHandler itemHandler = blockEntity.getInventoryHandler();

        // Get the machine's facing for world direction conversion
        BlockState machineState = blockEntity.getBlockState();
        Direction machineFacing = machineState.hasProperty(MachineFrameworkBlock.FACING)
                ? machineState.getValue(MachineFrameworkBlock.FACING)
                : Direction.NORTH;

        // Find an adjacent IO module to determine world directions
        int ioModuleSlot = findAdjacentIoModule(blockEntity, slotIndex);
        if (ioModuleSlot < 0) {
            return;
        }

        // Read the IO module's face config
        ItemStack ioStack = moduleHandler.getStackInSlot(ioModuleSlot);
        IoModuleItem ioModule = (IoModuleItem) ioStack.getItem();
        Map<RelativeSide, IoModuleItem.FaceMode> faceModes = ioModule.getFaceModes(ioStack);

        // Find all adjacent Inventory Modules
        List<Integer> inventorySlots = findAdjacentInventoryModules(blockEntity, slotIndex);
        if (inventorySlots.isEmpty()) {
            return;
        }

        if (getRedstoneControlMode(stack) == RedstoneControlMode.PULSE && !consumePendingPulse(stack)) {
            return;
        }

        // Get the ejection mode configuration
        Map<GridDirection, InventoryMode> invModes = getInventoryModes(stack);

        // Find the CATCH slot (if configured) to receive returned items
        int catchSlot = findCatchSlot(blockEntity, slotIndex, inventorySlots, invModes);

        // Process each face of the IO module — only OUTPUT and OPEN trigger ejection
        for (Map.Entry<RelativeSide, IoModuleItem.FaceMode> entry : faceModes.entrySet()) {
            IoModuleItem.FaceMode faceMode = entry.getValue();
            if (faceMode == IoModuleItem.FaceMode.DISABLED || faceMode == IoModuleItem.FaceMode.INPUT) {
                continue;
            }

            Direction ioWorldDir = entry.getKey().toWorldDirection(machineFacing);

            // Use the machine's own position as the origin — like a real dispenser block,
            // projectiles spawn from the center of the machine block and fire outward
            // based on the facing direction. The BlockSource position is the dispenser's
            // own block position, not the block in front of it.
            // Note: pos is the machine's world position, not the IO face position.

            // Find all inventory slots with DISPENSE/DROP mode (up to max per mode)
            if (faceMode == IoModuleItem.FaceMode.OUTPUT || faceMode == IoModuleItem.FaceMode.OPEN) {
                List<Integer> dispenseSlots = findInventorySlotsByMode(blockEntity, slotIndex, inventorySlots, invModes, InventoryMode.DISPENSE);
                for (int dispenseSlot : dispenseSlots) {
                    dispenseItem(level, pos, itemHandler, dispenseSlot, ioWorldDir, catchSlot);
                }

                List<Integer> dropSlots = findInventorySlotsByMode(blockEntity, slotIndex, inventorySlots, invModes, InventoryMode.DROP);
                for (int dropSlot : dropSlots) {
                    dropItem(level, pos, itemHandler, dropSlot, ioWorldDir, catchSlot);
                }
            }
        }
    }

    /**
     * Finds an adjacent IO Module slot.
     */
    private int findAdjacentIoModule(MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        ItemStackHandler moduleHandler = blockEntity.getModuleHandler();
        int[] adjacentSlots = blockEntity.getAdjacentSlots(slotIndex);

        for (int adj : adjacentSlots) {
            if (adj < 0 || adj >= MachineFrameworkBlockEntity.GRID_SIZE) continue;
            ItemStack adjModule = moduleHandler.getStackInSlot(adj);
            if (adjModule.is(ModItems.IO_MODULE.get())) {
                return adj;
            }
        }
        return -1;
    }

    /**
     * Finds all adjacent Inventory Module slots.
     */
    private List<Integer> findAdjacentInventoryModules(MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        List<Integer> result = new ArrayList<>();
        ItemStackHandler moduleHandler = blockEntity.getModuleHandler();
        int[] adjacentSlots = blockEntity.getAdjacentSlots(slotIndex);

        for (int adj : adjacentSlots) {
            if (adj < 0 || adj >= MachineFrameworkBlockEntity.GRID_SIZE) continue;
            ItemStack adjModule = moduleHandler.getStackInSlot(adj);
            if (adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                result.add(adj);
            }
        }
        return result;
    }

    /**
     * Returns the maximum number of DISPENSE slots this module can pull from per tick.
     */
    public int getMaxDispenseSlots() {
        return 3;
    }

    /**
     * Returns the maximum number of DROP slots this module can pull from per tick.
     */
    public int getMaxDropSlots() {
        return 3;
    }

    /**
     * Returns the maximum number of CATCH slots this module can have configured.
     * Only one catch slot is needed to receive all returned items.
     */
    public int getMaxCatchSlots() {
        return 1;
    }

    /**
     * Finds all Inventory Module slots configured with the given ejection mode,
     * up to the maximum allowed for that mode.
     */
    private List<Integer> findInventorySlotsByMode(
            MachineFrameworkBlockEntity blockEntity,
            int slotIndex,
            List<Integer> inventorySlots,
            Map<GridDirection, InventoryMode> modes,
            InventoryMode mode
    ) {
        List<Integer> result = new ArrayList<>();
        int maxSlots;
        if (mode == InventoryMode.DISPENSE) {
            maxSlots = getMaxDispenseSlots();
        } else if (mode == InventoryMode.DROP) {
            maxSlots = getMaxDropSlots();
        } else if (mode == InventoryMode.CATCH) {
            maxSlots = getMaxCatchSlots();
        } else {
            maxSlots = Integer.MAX_VALUE;
        }

        for (int invSlot : inventorySlots) {
            if (result.size() >= maxSlots) break;
            for (GridDirection dir : GridDirection.VALUES) {
                int adjSlot = getAdjacentSlotIndex(slotIndex, dir);
                if (adjSlot == invSlot && modes.get(dir) == mode) {
                    result.add(invSlot);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Finds the Inventory Module slot configured as CATCH mode.
     * Returns -1 if no CATCH slot is configured.
     */
    private int findCatchSlot(
            MachineFrameworkBlockEntity blockEntity,
            int slotIndex,
            List<Integer> inventorySlots,
            Map<GridDirection, InventoryMode> modes
    ) {
        List<Integer> catchSlots = findInventorySlotsByMode(blockEntity, slotIndex, inventorySlots, modes, InventoryMode.CATCH);
        return catchSlots.isEmpty() ? -1 : catchSlots.get(0);
    }

    /**
     * Dispenses one item from the inventory slot using vanilla dispenser behavior.
     * Uses DispenseItemBehavior to handle all item types correctly
     * (arrows, snowballs, potions, eggs, spawn eggs, bone meal, boats, buckets, etc.)
     * without manual handling.
     * <p>
     * If a catchSlot is configured, item entities spawned by the dispense behavior
     * (e.g., from shearing sheep, bucket collecting, armor ejection) are captured
     * and routed to the catch slot instead of falling to the ground.
     *
     * @param catchSlot The inventory slot index to receive returned items, or -1 if not configured
     */
    @SuppressWarnings("unchecked")
    private void dispenseItem(Level level, BlockPos machinePos, ItemStackHandler itemHandler, int invSlot, Direction direction, int catchSlot) {
        ItemStack stack = itemHandler.getStackInSlot(invSlot);
        if (stack.isEmpty() || !(level instanceof ServerLevel serverLevel)) return;

        try {
            // Find the BEHAVIORS map field by iterating over all declared fields
            // (the field name may vary between mapping versions)
            Map<net.minecraft.world.item.Item, DispenseItemBehavior> behaviors = null;
            for (java.lang.reflect.Field field : DispenserBlock.class.getDeclaredFields()) {
                if (field.getType().isAssignableFrom(Map.class)) {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value instanceof Map map) {
                        behaviors = (Map<net.minecraft.world.item.Item, DispenseItemBehavior>) map;
                        break;
                    }
                }
            }
            DispenseItemBehavior behavior = behaviors != null
                    ? behaviors.get(stack.getItem())
                    : null;

            // If the item has no registered dispenser behavior, fall back to DROP mode
            // (simple entity drop) so the item still ejects instead of silently doing nothing.
            if (behavior == null || behavior == DispenseItemBehavior.NOOP) {
                dropItem(level, machinePos, itemHandler, invSlot, direction, catchSlot);
                return;
            }

            // Create a BlockSource using a Dispenser block state so vanilla dispense behaviors
            // can read the FACING property they expect (ProjectileDispenseBehavior, etc.).
            // The machine's own block state has MachineFrameworkBlock.FACING, which is a
            // different DirectionProperty instance and causes IllegalArgumentException.
            // The dispenser faces the ejection direction so projectiles fire outward.
            BlockState dispenserState = net.minecraft.world.level.block.Blocks.DISPENSER
                    .defaultBlockState()
                    .setValue(net.minecraft.world.level.block.DispenserBlock.FACING, direction);
            BlockSource blockSource = new BlockSource(
                    serverLevel, machinePos, dispenserState, null);

            // If a catch slot is configured, record which item entities already exist
            // so we only capture newly spawned ones after the dispense call
            java.util.Set<ItemEntity> preExistingEntities = null;
            if (catchSlot >= 0) {
                preExistingEntities = serverLevel.getEntitiesOfClass(
                        ItemEntity.class,
                        new net.minecraft.world.phys.AABB(
                                machinePos.getX() - 1.5, machinePos.getY() - 1.5, machinePos.getZ() - 1.5,
                                machinePos.getX() + 2.5, machinePos.getY() + 2.5, machinePos.getZ() + 2.5
                        )
                ).stream().collect(java.util.stream.Collectors.toSet());
            }

            // Let vanilla handle the dispense logic
            ItemStack remaining = behavior.dispense(blockSource, stack);

            // Update the inventory slot with the remaining stack
            itemHandler.setStackInSlot(invSlot, remaining);

            // If a catch slot is configured, capture any newly spawned item entities
            // and discard them from the world — CATCH mode never lets entities persist.
            if (catchSlot >= 0 && preExistingEntities != null) {
                java.util.List<ItemEntity> nearbyItems = serverLevel.getEntitiesOfClass(
                        ItemEntity.class,
                        new net.minecraft.world.phys.AABB(
                                machinePos.getX() - 1.5, machinePos.getY() - 1.5, machinePos.getZ() - 1.5,
                                machinePos.getX() + 2.5, machinePos.getY() + 2.5, machinePos.getZ() + 2.5
                        )
                );

                for (ItemEntity itemEntity : nearbyItems) {
                    // Only capture entities that didn't exist before the dispense call
                    if (preExistingEntities.contains(itemEntity)) continue;

                    ItemStack caught = itemEntity.getItem();
                    if (!caught.isEmpty()) {
                        insertIntoCatchSlot(itemHandler, catchSlot, caught);
                    }
                    // Always discard the entity from the world — CATCH mode suppresses all item drops
                    itemEntity.discard();
                }
            }
        } catch (ReflectiveOperationException e) {
            PraeteritumFuturumMod.LOGGER.error("Failed to dispense item using vanilla behavior", e);
        }
    }

    /**
     * Drops one item from the inventory slot.
     * If a catchSlot is configured, the item is routed directly to the catch slot
     * instead of spawning as an entity drop.
     *
     * @param catchSlot The inventory slot index to receive returned items, or -1 to drop as entity
     */
    private void dropItem(Level level, BlockPos machinePos, ItemStackHandler itemHandler, int invSlot, Direction direction, int catchSlot) {
        ItemStack stack = itemHandler.getStackInSlot(invSlot);
        if (stack.isEmpty()) return;

        // If a catch slot is configured, try to route the item directly
        if (catchSlot >= 0) {
            ItemStack existing = itemHandler.getStackInSlot(catchSlot);
            boolean canInsert = existing.isEmpty()
                    || (net.minecraft.world.item.ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize());

            if (canInsert) {
                // Split one item from the source stack and insert into catch slot
                ItemStack toTransfer = stack.split(1);
                if (!toTransfer.isEmpty()) {
                    if (existing.isEmpty()) {
                        itemHandler.setStackInSlot(catchSlot, toTransfer);
                    } else {
                        existing.grow(toTransfer.getCount());
                        itemHandler.setStackInSlot(catchSlot, existing);
                    }
                }
                level.playSound(null, machinePos, SoundEvents.DISPENSER_DISPENSE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.0F);
                return;
            }
            // Catch slot is full or has a different item — fall through to entity drop
        }

        // No catch slot configured or catch slot can't accept — spawn as entity drop
        ItemStack toDrop = stack.split(1);
        spawnItemEntity(level, machinePos, toDrop, direction);
        level.playSound(null, machinePos, SoundEvents.DISPENSER_DISPENSE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    /**
     * Inserts a caught item entity's stack into the catch slot.
     * The entity is always discarded from the world after this call — CATCH mode suppresses all item drops.
     * If the catch slot can't accept the item (full or different type), the item is lost.
     */
    private void insertIntoCatchSlot(ItemStackHandler itemHandler, int catchSlot, ItemStack caught) {
        ItemStack existing = itemHandler.getStackInSlot(catchSlot);
        if (existing.isEmpty()) {
            itemHandler.setStackInSlot(catchSlot, caught.split(caught.getCount()));
        } else if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(existing, caught)) {
            int space = existing.getMaxStackSize() - existing.getCount();
            int transfer = Math.min(caught.getCount(), space);
            if (transfer > 0) {
                existing.grow(transfer);
                itemHandler.setStackInSlot(catchSlot, existing);
            }
            // If there's remaining items that couldn't fit, they are lost (entity will be discarded)
        }
        // Different item type — item is lost (entity will be discarded)
    }

    /**
     * Spawns an item entity at the machine's face with velocity in the ejection direction.
     */
    private void spawnItemEntity(Level level, BlockPos machinePos, ItemStack stack, Direction direction) {
        if (stack.isEmpty()) return;
        BlockPos targetPos = machinePos.relative(direction);
        double x = targetPos.getX() + 0.5;
        double y = targetPos.getY() + 0.5;
        double z = targetPos.getZ() + 0.5;

        double speed = 0.4;
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
        itemEntity.setDeltaMovement(
                direction.getStepX() * speed,
                0.2, // slight upward velocity
                direction.getStepZ() * speed
        );
        itemEntity.setPickUpDelay(10);
        level.addFreshEntity(itemEntity);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            boolean configured = hasAnyConfig(stack);
            if (configured) {
                tooltipComponents.add(Component.translatable("tooltip.praeteritum_futurum.configured").withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
    }
}
