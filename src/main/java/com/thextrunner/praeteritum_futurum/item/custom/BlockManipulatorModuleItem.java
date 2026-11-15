package com.thextrunner.praeteritum_futurum.item.custom;

import com.mojang.logging.LogUtils;
import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A block manipulator module that places and breaks blocks in the world.
 * <p>
 * Uses the processing module cross config pattern (U/D/L/R buttons + X close).
 * Requires an adjacent IO module to function — the IO module's face config determines
 * which world directions to interact with. Adjacent Inventory Modules are configured
 * per direction:
 * <ul>
 *   <li>If adjacent is an <b>Inventory Module</b>: mode cycles DISABLED → PLACE → BREAK
 *     <ul>
 *       <li>PLACE: this inventory provides blocks for placing (triggered by IO OUTPUT/OPEN faces)</li>
 *       <li>BREAK: this inventory receives broken blocks (triggered by IO INPUT/OPEN faces)</li>
 *     </ul>
 *   </li>
 *   <li>If adjacent is an <b>IO Module</b>: mode cycles DISABLED → ENABLED
 *     <ul>
 *       <li>ENABLED: reads the IO module's face config for world directions</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * The IO module face modes control the action:
 * <ul>
 *   <li>INPUT: breaks blocks, stores in the Inventory Module with BREAK mode</li>
 *   <li>OUTPUT: places blocks from the Inventory Module with PLACE mode</li>
 *   <li>OPEN: tries to place first, then break</li>
 * </ul>
 * <p>
 * Block breaking is instant (no progress tracking) for testing purposes.
 */
public class BlockManipulatorModuleItem extends ProcessingModuleItem {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Mode for each direction in the Block Manipulator Module.
     */
    public enum ManipulatorMode {
        DISABLED("D", 0xFF555555),  // Disabled (Dark Gray)
        PLACE("P", 0xFF33CC33),     // Place blocks from inventory (Green)
        BREAK("B", 0xFFCC3333);     // Break blocks in the world (Red)

        private final String labelChar;
        private final int color;

        ManipulatorMode(String labelChar, int color) {
            this.labelChar = labelChar;
            this.color = color;
        }

        public String getLabelChar() {
            return labelChar;
        }

        public int getColor() {
            return color;
        }

        /**
         * Cycles through DISABLED → PLACE → BREAK → DISABLED
         */
        public ManipulatorMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public BlockManipulatorModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/block_manipulator_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16,
                16, 16
        );
        super.renderModuleOverlay(graphics, slot, stack, blockEntity);
    }

    @Override
    public String getModuleConfigKey() {
        return "BlockManipulatorConfig";
    }

    /**
     * Gets the manipulator modes for each direction from the module's NBT data.
     */
    public static Map<Direction, ManipulatorMode> getManipulatorModes(ItemStack stack, String nbtKey) {
        Map<Direction, ManipulatorMode> modes = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            modes.put(dir, ManipulatorMode.DISABLED);
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(nbtKey)) {
                CompoundTag modesTag = tag.getCompound(nbtKey);
                for (Direction dir : Direction.values()) {
                    if (modesTag.contains(dir.getName())) {
                        try {
                            modes.put(dir, ManipulatorMode.valueOf(modesTag.getString(dir.getName())));
                        } catch (IllegalArgumentException e) {
                            // Unknown enum value — default to DISABLED
                            modes.put(dir, ManipulatorMode.DISABLED);
                        }
                    }
                }
            }
        }
        return modes;
    }

    /**
     * Sets the manipulator mode for a specific direction.
     */
    public static void setManipulatorMode(ItemStack stack, Direction dir, ManipulatorMode mode, String nbtKey) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        CompoundTag modesTag = tag.contains(nbtKey) ? tag.getCompound(nbtKey) : new CompoundTag();

        modesTag.putString(dir.getName(), mode.name());
        tag.put(nbtKey, modesTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public SlotMode getNextMode(ItemStack stack, Direction clickedDir) {
        // Override to use ManipulatorMode cycling instead of SlotMode
        Map<Direction, ManipulatorMode> modes = getManipulatorModes(stack, getModuleConfigKey());
        ManipulatorMode current = modes.get(clickedDir);
        ManipulatorMode next = current.next();
        setManipulatorMode(stack, clickedDir, next, getModuleConfigKey());
        // Return a dummy SlotMode to satisfy the super call — we handle our own mode storage
        return SlotMode.DISABLED;
    }

    @Override
    public void handleCustomClick(double mouseX, double mouseY, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex, net.minecraft.client.Minecraft minecraft) {
        // Handle the X close button
        if (isCloseButton(mouseX, mouseY, slotX, slotY)) {
            return; // Let the parent class handle closing the overlay
        }

        // Handle direction button clicks — only for directions with adjacent slots
        Direction clickedDir = getClickedDirection(slotX, slotY, mouseX, mouseY, slotIndex);
        if (clickedDir != null) {
            if (minecraft.player != null) {
                minecraft.player.playNotifySound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
            }

            // Check what module is in the adjacent slot to determine valid modes
            int adjSlot = getAdjacentSlotByDirection(slotIndex, clickedDir);
            ManipulatorMode next;

            if (adjSlot >= 0 && adjSlot < MachineFrameworkBlockEntity.GRID_SIZE) {
                ItemStackHandler moduleHandler = blockEntity.getModuleHandler();
                ItemStack adjModule = moduleHandler.getStackInSlot(adjSlot);

                if (adjModule.is(ModItems.IO_MODULE.get())) {
                    // IO Module adjacent: cycle DISABLED → ENABLED → DISABLED
                    Map<Direction, ManipulatorMode> modes = getManipulatorModes(stack, getModuleConfigKey());
                    ManipulatorMode current = modes.get(clickedDir);
                    next = current == ManipulatorMode.DISABLED ? ManipulatorMode.PLACE : ManipulatorMode.DISABLED;
                    // Use PLACE as the "ENABLED" marker since we only have 3 enum values
                } else if (adjModule.is(ModItems.INVENTORY_MODULE.get())) {
                    // Inventory Module adjacent: cycle DISABLED → PLACE → BREAK → DISABLED
                    Map<Direction, ManipulatorMode> modes = getManipulatorModes(stack, getModuleConfigKey());
                    ManipulatorMode current = modes.get(clickedDir);
                    next = current.next();
                } else {
                    // Neither IO nor Inventory: allow cycling to "DISABLED"
                    Map<Direction, ManipulatorMode> modes = getManipulatorModes(stack, getModuleConfigKey());
                    ManipulatorMode current = modes.get(clickedDir);
                    if (current == ManipulatorMode.DISABLED) {
                        // Already disabled, don't cycle
                        return;
                    }
                    next = ManipulatorMode.DISABLED;
                }
            } else {
                // No adjacent slot
                return;
            }

            setManipulatorMode(stack, clickedDir, next, getModuleConfigKey());

            // Send the packet to sync the change
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.thextrunner.praeteritum_futurum.network.ServerboundToggleModuleDirectionPacket(slotIndex, clickedDir, blockEntity.getBlockPos())
            );
        }
    }

    /**
     * Gets the clicked direction, only checking buttons for directions with adjacent slots.
     */
    private Direction getClickedDirection(int slotX, int slotY, double mx, double my, int slotIndex) {
        int centerX = slotX + 8;
        int centerY = slotY + 8;
        int offset = 18;

        // Only check buttons for directions that have adjacent slots
        if (hasAdjacentSlot(slotIndex, Direction.UP) && isInsideButton(mx, my, centerX, slotY - (offset - 8))) return Direction.UP;
        if (hasAdjacentSlot(slotIndex, Direction.WEST) && isInsideButton(mx, my, slotX - (offset - 8), centerY)) return Direction.WEST;
        if (hasAdjacentSlot(slotIndex, Direction.EAST) && isInsideButton(mx, my, slotX + 16 + (offset - 8), centerY)) return Direction.EAST;
        if (hasAdjacentSlot(slotIndex, Direction.DOWN) && isInsideButton(mx, my, centerX, slotY + 16 + (offset - 8))) return Direction.DOWN;
        return null;
    }

    private boolean isInsideButton(double mx, double my, int x, int y) {
        return mx >= x - 8 && mx < x + 8 && my >= y - 8 && my < y + 8;
    }

    @Override
    public void renderCustomOverlay(
            net.minecraft.client.gui.GuiGraphics graphics,
            int slotX, int slotY,
            ItemStack stack,
            MachineFrameworkBlockEntity blockEntity,
            net.minecraft.client.gui.Font font
    ) {
        Map<Direction, ManipulatorMode> modes = getManipulatorModes(stack, getModuleConfigKey());

        int centerX = slotX + 8;
        int centerY = slotY + 8;
        int offset = 18;

        // Render only buttons for directions that have adjacent slots
        int slotIndex = getOverlaySlotIndex();
        if (slotIndex >= 0) {
            // Check adjacent modules for each direction to determine label
            ItemStackHandler moduleHandler = blockEntity.getModuleHandler();

            if (hasAdjacentSlot(slotIndex, Direction.UP)) {
                int adjSlot = getAdjacentSlotByDirection(slotIndex, Direction.UP);
                String label = getButtonLabel(adjSlot, moduleHandler, modes.get(Direction.UP));
                drawManipulatorButton(graphics, font, centerX, slotY - (offset - 8), modes.get(Direction.UP), "U", label);
            }
            if (hasAdjacentSlot(slotIndex, Direction.WEST)) {
                int adjSlot = getAdjacentSlotByDirection(slotIndex, Direction.WEST);
                String label = getButtonLabel(adjSlot, moduleHandler, modes.get(Direction.WEST));
                drawManipulatorButton(graphics, font, slotX - (offset - 8), centerY, modes.get(Direction.WEST), "L", label);
            }
            if (hasAdjacentSlot(slotIndex, Direction.EAST)) {
                int adjSlot = getAdjacentSlotByDirection(slotIndex, Direction.EAST);
                String label = getButtonLabel(adjSlot, moduleHandler, modes.get(Direction.EAST));
                drawManipulatorButton(graphics, font, slotX + 16 + (offset - 8), centerY, modes.get(Direction.EAST), "R", label);
            }
            if (hasAdjacentSlot(slotIndex, Direction.DOWN)) {
                int adjSlot = getAdjacentSlotByDirection(slotIndex, Direction.DOWN);
                String label = getButtonLabel(adjSlot, moduleHandler, modes.get(Direction.DOWN));
                drawManipulatorButton(graphics, font, centerX, slotY + 16 + (offset - 8), modes.get(Direction.DOWN), "D", label);
            }
        } else {
            // Fallback: render all buttons if slot index is unknown
            drawManipulatorButton(graphics, font, centerX, slotY - (offset - 8), modes.get(Direction.UP), "U");
            drawManipulatorButton(graphics, font, slotX - (offset - 8), centerY, modes.get(Direction.WEST), "L");
            drawManipulatorButton(graphics, font, slotX + 16 + (offset - 8), centerY, modes.get(Direction.EAST), "R");
            drawManipulatorButton(graphics, font, centerX, slotY + 16 + (offset - 8), modes.get(Direction.DOWN), "D");
        }

        // Close button
        graphics.fill(slotX - 18, slotY - 18, slotX - 2, slotY - 2, 0xFF8B0000);
        graphics.drawCenteredString(font, "X", slotX - 10, slotY - 14, 0xFFFFFF);
    }

    /**
     * Gets the label for a direction button based on the adjacent module type.
     * IO Module shows "E" (Enabled), Inventory Module shows "P"/"B"/"D", others show "D".
     */
    private String getButtonLabel(int adjSlot, ItemStackHandler moduleHandler, ManipulatorMode mode) {
        if (adjSlot < 0 || adjSlot >= MachineFrameworkBlockEntity.GRID_SIZE) {
            return mode.getLabelChar();
        }
        ItemStack adjModule = moduleHandler.getStackInSlot(adjSlot);
        if (adjModule.is(ModItems.IO_MODULE.get())) {
            // IO Module: show "E" for enabled (PLACE mode used as ENABLED marker), "D" for disabled
            return mode == ManipulatorMode.PLACE ? "E" : "D";
        }
        // Inventory Module or other: use standard label
        return mode.getLabelChar();
    }

    private void drawManipulatorButton(GuiGraphics graphics, net.minecraft.client.gui.Font font, int x, int y, ManipulatorMode mode, String sideLabel) {
        drawManipulatorButton(graphics, font, x, y, mode, sideLabel, mode.getLabelChar());
    }

    private void drawManipulatorButton(GuiGraphics graphics, net.minecraft.client.gui.Font font, int x, int y, ManipulatorMode mode, String sideLabel, String label) {
        graphics.fill(x - 8, y - 8, x + 8, y + 8, 0xFF888888);
        graphics.fill(x - 7, y - 7, x + 7, y + 7, mode.getColor());

        // Displays e.g., "U:P", "L:E", "D:B"
        graphics.drawCenteredString(font, sideLabel + ":" + label, x, y - 4, 0xFFFFFF);
    }

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        // Respect the block module tick rate config
        int tickRate = Config.getBlockModuleTickRate();
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

        // Get the manipulator mode configuration
        Map<Direction, ManipulatorMode> modes = getManipulatorModes(stack, getModuleConfigKey());

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR] Tick at {}, slot {}, facing {}", pos, slotIndex, machineFacing);
            for (Direction dir : Direction.values()) {
                ManipulatorMode mode = modes.get(dir);
                if (mode != ManipulatorMode.DISABLED) {
                    int adjSlot = getAdjacentSlotByDirection(slotIndex, dir);
                    ItemStack adjModule = adjSlot >= 0 && adjSlot < MachineFrameworkBlockEntity.GRID_SIZE ? moduleHandler.getStackInSlot(adjSlot) : ItemStack.EMPTY;
                    LOGGER.info("[BLOCK MANIPULATOR]   Direction {}: mode={}, adjSlot={}, adjModule={}", dir, mode, adjSlot, adjModule.isEmpty() ? "empty" : adjModule.getItem().getDescriptionId());
                }
            }
        }

        // First, check if there's an adjacent IO module to determine world directions
        // If no IO module is present, the module does nothing — it needs the IO module to know where to interact
        int ioModuleSlot = findAdjacentIoModule(blockEntity, slotIndex);
        if (ioModuleSlot < 0) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR] No adjacent IO module found, doing nothing");
            }
            return;
        }

        // Read the IO module's face config
        ItemStack ioStack = moduleHandler.getStackInSlot(ioModuleSlot);
        IoModuleItem ioModule = (IoModuleItem) ioStack.getItem();
        Map<RelativeSide, IoModuleItem.FaceMode> faceModes = ioModule.getFaceModes(ioStack);

        // Find all adjacent Inventory Modules for block storage
        List<Integer> inventorySlots = findAdjacentInventoryModules(blockEntity, slotIndex);
        if (inventorySlots.isEmpty()) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.warn("[BLOCK MANIPULATOR] IO module at slot {} but no adjacent Inventory Modules found!", ioModuleSlot);
            }
            return;
        }

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR] IO Module at slot {}, inventory slots: {}", ioModuleSlot, inventorySlots);
        }

        // Process each face of the IO module
        for (Map.Entry<RelativeSide, IoModuleItem.FaceMode> entry : faceModes.entrySet()) {
            IoModuleItem.FaceMode faceMode = entry.getValue();
            if (faceMode == IoModuleItem.FaceMode.DISABLED) continue;

            Direction ioWorldDir = entry.getKey().toWorldDirection(machineFacing);

            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   IO face {} -> worldDir={}, faceMode={}", entry.getKey(), ioWorldDir, faceMode);
            }

            // Find an Inventory Module with the appropriate mode for this action
            int targetInvSlot = -1;

            if (faceMode == IoModuleItem.FaceMode.INPUT) {
                // INPUT: break blocks — need an Inventory Module with BREAK mode
                targetInvSlot = findInventorySlotByMode(blockEntity, slotIndex, inventorySlots, modes, ManipulatorMode.BREAK);
                if (targetInvSlot < 0) {
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.info("[BLOCK MANIPULATOR]   No Inventory Module with BREAK mode found for INPUT face");
                    }
                    continue;
                }
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Breaking block at worldDir {} into inventory slot {}", ioWorldDir, targetInvSlot);
                }
                breakBlockInSlot(level, pos, blockEntity, itemHandler, targetInvSlot, ioWorldDir);

            } else if (faceMode == IoModuleItem.FaceMode.OUTPUT) {
                // OUTPUT: place blocks — need an Inventory Module with PLACE mode
                targetInvSlot = findInventorySlotByMode(blockEntity, slotIndex, inventorySlots, modes, ManipulatorMode.PLACE);
                if (targetInvSlot < 0) {
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.info("[BLOCK MANIPULATOR]   No Inventory Module with PLACE mode found for OUTPUT face");
                    }
                    continue;
                }
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Placing block at worldDir {} from inventory slot {}", ioWorldDir, targetInvSlot);
                }
                placeBlockFromSlot(level, pos, blockEntity, itemHandler, targetInvSlot, ioWorldDir);

            } else if (faceMode == IoModuleItem.FaceMode.OPEN) {
                // OPEN: try to place first, then break
                int placeSlot = findInventorySlotByMode(blockEntity, slotIndex, inventorySlots, modes, ManipulatorMode.PLACE);
                if (placeSlot >= 0) {
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.info("[BLOCK MANIPULATOR]   OPEN mode: placing at worldDir {} from slot {}", ioWorldDir, placeSlot);
                    }
                    placeBlockFromSlot(level, pos, blockEntity, itemHandler, placeSlot, ioWorldDir);
                }
                int breakSlot = findInventorySlotByMode(blockEntity, slotIndex, inventorySlots, modes, ManipulatorMode.BREAK);
                if (breakSlot >= 0) {
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.info("[BLOCK MANIPULATOR]   OPEN mode: breaking at worldDir {} into slot {}", ioWorldDir, breakSlot);
                    }
                    breakBlockInSlot(level, pos, blockEntity, itemHandler, breakSlot, ioWorldDir);
                }
            }
        }
    }

    /**
     * Converts a grid direction to a world direction based on machine facing.
     * UP/DOWN are absolute; horizontal directions rotate with the machine facing.
     */
    private Direction gridDirectionToWorldDirection(Direction gridDir, Direction machineFacing) {
        if (gridDir.getAxis().isVertical()) {
            return gridDir; // UP and DOWN are absolute
        }
        // Rotate horizontal direction based on "machine facing"
        // Machine facing NORTH: WEST->NORTH, EAST->EAST, etc.
        return switch (machineFacing) {
            case NORTH -> gridDir;
            case EAST -> switch (gridDir) {
                case WEST -> Direction.NORTH;
                case EAST -> Direction.SOUTH;
                case NORTH -> Direction.EAST;
                case SOUTH -> Direction.WEST;
                default -> gridDir;
            };
            case SOUTH -> switch (gridDir) {
                case WEST -> Direction.EAST;
                case EAST -> Direction.WEST;
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                default -> gridDir;
            };
            case WEST -> switch (gridDir) {
                case WEST -> Direction.SOUTH;
                case EAST -> Direction.NORTH;
                case NORTH -> Direction.WEST;
                case SOUTH -> Direction.EAST;
                default -> gridDir;
            };
            default -> gridDir;
        };
    }

    /**
     * Gets the adjacent slot index for a given direction.
     */
    private int getAdjacentSlotByDirection(int slotIndex, Direction dir) {
        int row = slotIndex / MachineFrameworkBlockEntity.GRID_WIDTH;
        int col = slotIndex % MachineFrameworkBlockEntity.GRID_WIDTH;

        return switch (dir) {
            case UP -> row > 0 ? slotIndex - MachineFrameworkBlockEntity.GRID_WIDTH : -1;
            case DOWN -> row < MachineFrameworkBlockEntity.GRID_ROWS - 1 ? slotIndex + MachineFrameworkBlockEntity.GRID_WIDTH : -1;
            case WEST -> col > 0 ? slotIndex - 1 : -1;
            case EAST -> col < MachineFrameworkBlockEntity.GRID_WIDTH - 1 ? slotIndex + 1 : -1;
            default -> -1;
        };
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
     * Finds an Inventory Module slot that is configured with the given mode.
     * Checks the direction from the block manipulator to the inventory module.
     */
    private int findInventorySlotByMode(
            MachineFrameworkBlockEntity blockEntity,
            int slotIndex,
            List<Integer> inventorySlots,
            Map<Direction, ManipulatorMode> modes,
            ManipulatorMode mode
    ) {
        for (int invSlot : inventorySlots) {
            // Find the direction from the block manipulator to this inventory module
            for (Direction dir : Direction.values()) {
                int adjSlot = getAdjacentSlotByDirection(slotIndex, dir);
                if (adjSlot == invSlot && modes.get(dir) == mode) {
                    return invSlot;
                }
            }
        }
        return -1;
    }

    /**
     * Places a block from a specific inventory slot into the world in the given world direction.
     * Acts like a player placing blocks: tries wall mounting first (based on IO facing),
     * then floor placement, then ceiling placement. If all fail, drops the item as an entity.
     * Restores block entity data (NBT) if the stored item contains it.
     */
    private void placeBlockFromSlot(
            Level level,
            BlockPos machinePos,
            MachineFrameworkBlockEntity blockEntity,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        BlockPos targetPos = machinePos.relative(worldDirection);

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR] placeBlockFromSlot: targetPos={}, invSlot={}, worldDir={}", targetPos, invSlot, worldDirection);
        }

        ItemStack stack = itemHandler.getStackInSlot(invSlot);
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   Inventory slot {}: {}", invSlot, stack.isEmpty() ? "empty" : stack.getItem().getDescriptionId());
        }
        if (stack.isEmpty()) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Skipping: inventory slot is empty");
            }
            return;
        }

        // Check if the item is a block item
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Skipping: item is not a block item");
            }
            return;
        }
        Block block = blockItem.getBlock();
        if (block == Blocks.AIR) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Skipping: block is AIR");
            }
            return;
        }

        // Extract block state and block entity data from the item's CustomData component, if present
        CompoundTag blockEntityNbt = null;
        CompoundTag blockStateNbt = null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag itemNbt = customData.copyTag();
            if (itemNbt.contains("BlockEntityTag")) {
                blockEntityNbt = itemNbt.getCompound("BlockEntityTag");
            }
            if (itemNbt.contains("BlockState")) {
                blockStateNbt = itemNbt.getCompound("BlockState");
            }
        }

        // Smart placement: try different orientations based on block type and IO facing
        // The worldDirection is the direction FROM the machine TO the target position
        // The opposite direction points FROM the target back TO the machine (the "wall" side)
        Direction attachDirection = worldDirection.getOpposite(); // Direction from target back to machine

        // Extract machine facing for horizontal-only blocks during vertical placement
        Direction machineFacing = blockEntity.getBlockState().hasProperty(MachineFrameworkBlock.FACING)
                ? blockEntity.getBlockState().getValue(MachineFrameworkBlock.FACING)
                : Direction.NORTH;

        // Scaffolding: special case — allow even if target is not air (stacking on existing scaffolding)
        if (block == Blocks.SCAFFOLDING) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Scaffolding detected — checking for stacking");
            }
            if (tryPlaceScaffolding(level, targetPos, blockStateNbt, blockEntityNbt, itemHandler, invSlot, worldDirection, machineFacing)) {
                return;
            }
            // If stacking failed, fall through to normal placement below
            // But still need to check if target is valid for normal placement
        }

        // Check if the target position is valid for placing this block (air or waterloggable water)
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Skipping: target position is not air or valid water for placement");
            }
            return;
        }

        // Try placement with smart orientation logic
        boolean placed = trySmartPlacement(level, targetPos, block, blockStateNbt, blockEntityNbt,
                itemHandler, invSlot, worldDirection, attachDirection, machineFacing);

        if (!placed && Config.isAllowFloatingBlocks()) {
            // If smart placement failed and floating blocks are allowed, try to place with stored state
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Smart placement failed, trying floating placement (config enabled)");
            }
            BlockState placeState = block.defaultBlockState();
            if (blockStateNbt != null) {
                try {
                    placeState = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), blockStateNbt);
                } catch (Exception e) {
                    // Fall back to default
                }
            }
            placed = placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection);
        }

        if (!placed) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   All placement attempts failed for {}", block.getDescriptionId());
            }
        }
    }

    /**
     * Tries to place a block using player-like logic.
     * Uses the IO facing direction to determine how to orient the block:
     * - For wall-mounted blocks: try to attach to the side facing the machine first, then other walls
     * - For floor/ceiling blocks: try floor (block below) first, then ceiling
     * - For directional blocks (ladders, lanterns, trapdoors, etc.): orient based on IO facing
     * - For regular full blocks: place with default state
     * - For torch/sign blocks: try wall placement first (converting to wall variant), then floor/ceiling
     *
     * The worldDirection is the direction FROM the machine TO the target position.
     * The attachDirection (opposite) points FROM the target back TO the machine — this is the "wall" side.
     *
     * @return true if the block was placed successfully
     */
    private boolean trySmartPlacement(
            Level level,
            BlockPos targetPos,
            Block block,
            CompoundTag blockStateNbt,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection,
            Direction attachDirection,
            Direction machineFacing
    ) {
        // Determine block type category
        boolean isWallOnly = block instanceof net.minecraft.world.level.block.WallTorchBlock ||
                block instanceof net.minecraft.world.level.block.LadderBlock ||
                block instanceof net.minecraft.world.level.block.WallSignBlock ||
                block instanceof net.minecraft.world.level.block.WallHangingSignBlock ||
                block instanceof net.minecraft.world.level.block.WallBannerBlock ||
                block instanceof net.minecraft.world.level.block.WallSkullBlock ||
                block == Blocks.TRIPWIRE_HOOK;

        boolean isFaceAttached = block instanceof net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock ||
                block instanceof net.minecraft.world.level.block.BellBlock;

        boolean isFloorCeilingMounted = block instanceof net.minecraft.world.level.block.TorchBlock ||
                block instanceof net.minecraft.world.level.block.CeilingHangingSignBlock ||
                block instanceof net.minecraft.world.level.block.SignBlock ||
                block instanceof net.minecraft.world.level.block.BannerBlock ||
                block instanceof net.minecraft.world.level.block.SkullBlock ||
                block instanceof net.minecraft.world.level.block.LanternBlock;

        // Check if block has a wall variant (TorchBlock → WallTorchBlock, HangingSign → WallHangingSign, etc.)
        Block wallVariant = getWallVariant(block);

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR] trySmartPlacement: block={}, isWallOnly={}, isFaceAttached={}, isFloorCeiling={}, wallVariant={}, worldDir={}, attachDir={}",
                    block.getDescriptionId(), isWallOnly, isFaceAttached, isFloorCeilingMounted, wallVariant != null ? wallVariant.getDescriptionId() : "none", worldDirection, attachDirection);
        }

        // Beds cannot be placed vertically
        if (block instanceof net.minecraft.world.level.block.BedBlock && worldDirection.getAxis().isVertical()) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Beds cannot be placed vertically (direction={})", worldDirection);
            }
            return false;
        }

        // For blocks with wall variants (torches, signs, banners, skulls):
        // Try all valid horizontal walls first (opposite wall, then side walls — never the machine).
        // For hanging signs, always try wall placement regardless of worldDirection (even vertical),
        // because hanging signs can be wall-mounted and players expect wall-first behavior.
        boolean isHangingSign = block instanceof net.minecraft.world.level.block.CeilingHangingSignBlock;
        // The direction from the target back to the machine — the machine's own wall must never be used as a placement wall
        Direction machineWallDir = attachDirection;
        // For hanging signs, only attempt wall placement if there's an actual adjacent wall block (not the machine)
        // that can support the sign. If no such wall exists, skip wall placement and fall through to ceiling variant.
        boolean hasWallForHangingSign = false;
        if (isHangingSign) {
            // Check ALL 4 horizontal directions for adjacent wall blocks, not just getWallPlacementDirections
            // (which returns empty for vertical worldDirection)
            for (Direction wallDir : Direction.Plane.HORIZONTAL) {
                if (wallDir == machineWallDir) continue; // skip the machine's own wall
                BlockPos wallPos = targetPos.relative(wallDir);
                if (level.getBlockState(wallPos).isSolidRender(level, wallPos)) {
                    hasWallForHangingSign = true;
                    break;
                }
            }
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Hanging sign wall check: hasWall={}, machineWall={}", hasWallForHangingSign, machineWallDir);
            }
        }
        // For hanging signs, only enter wall placement if an actual adjacent horizontal wall exists
        // For other wall-variant blocks, enter wall placement when worldDirection is horizontal
        if (wallVariant != null && ((isHangingSign && hasWallForHangingSign) || (!isHangingSign && worldDirection.getAxis().isHorizontal()))) {
            for (Direction wallDir : getWallPlacementDirections(worldDirection)) {
                if (isHangingSign) {
                    // For hanging signs, try perpendicular facings (clockwise and counter-clockwise of the wall)
                    // This matches vanilla behavior where wall-mounted hanging signs face perpendicular to the wall
                    Direction[] perpendicularFacings = {wallDir.getClockWise(), wallDir.getCounterClockWise()};
                    for (Direction perpFacing : perpendicularFacings) {
                        // Skip if the perpendicular facing would attach to the machine's own wall
                        if (perpFacing == machineWallDir) {
                            if (Config.isDebugLoggingEnabled()) {
                                LOGGER.info("[BLOCK MANIPULATOR]   Skipping perpendicular facing {} (machine wall)", perpFacing);
                            }
                            continue;
                        }
                        if (Config.isDebugLoggingEnabled()) {
                            LOGGER.info("[BLOCK MANIPULATOR]   Trying wall placement with variant {} on wall {} facing perpendicular {}",
                                    wallVariant.getDescriptionId(), wallDir, perpFacing);
                        }
                        BlockState wallState = createPlacementState(wallVariant, blockStateNbt, perpFacing, machineFacing, level, targetPos);
                        if (canBlockSurvive(wallVariant, wallState, level, targetPos)) {
                            if (placeBlockAt(level, targetPos, wallState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                                if (Config.isDebugLoggingEnabled()) {
                                    LOGGER.info("[BLOCK MANIPULATOR]   Placed {} (wall variant) at {} on wall {} facing perpendicular {}",
                                            block.getDescriptionId(), targetPos, wallDir, perpFacing);
                                }
                                return true;
                            }
                        }
                    }
                } else {
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.info("[BLOCK MANIPULATOR]   Trying wall placement with variant {} facing {}",
                                wallVariant.getDescriptionId(), wallDir);
                    }
                    BlockState wallState = createPlacementState(wallVariant, blockStateNbt, wallDir, machineFacing, level, targetPos);
                    if (canBlockSurvive(wallVariant, wallState, level, targetPos)) {
                        if (placeBlockAt(level, targetPos, wallState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                            if (Config.isDebugLoggingEnabled()) {
                                LOGGER.info("[BLOCK MANIPULATOR]   Placed {} (wall variant) at {} facing {}",
                                        block.getDescriptionId(), targetPos, wallDir);
                            }
                            return true;
                        }
                    }
                }
            }
        }

        // Build placement attempts in priority order
        List<Direction> attemptDirections = new ArrayList<>();

        if (isWallOnly) {
            // Can only attach to walls: try opposite wall first, then side walls (never the machine)
            attemptDirections.addAll(getWallPlacementDirections(worldDirection));
        } else if (isFaceAttached) {
            // Face-attached (buttons, levers, bells): try opposite wall first, then side walls (never machine), then floor, then ceiling
            attemptDirections.addAll(getWallPlacementDirections(worldDirection));
            attemptDirections.add(Direction.DOWN);
            attemptDirections.add(Direction.UP);
        } else if (isFloorCeilingMounted) {
            // Floor/ceiling blocks: try floor (DOWN) first, then ceiling (UP)
            // Exception: hanging signs only attach to ceilings, never floors (player-placed behavior)
            if (isHangingSign) {
                attemptDirections.add(Direction.UP);
            } else {
                attemptDirections.add(Direction.DOWN);
                attemptDirections.add(Direction.UP);
            }
        } else {
            // Regular blocks (chains, logs, pillars, dispensers, observers, beds, etc.): align with worldDirection
            attemptDirections.add(worldDirection);
        }

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   Attempt directions: {}", attemptDirections);
        }

        // Try each placement attempt
        for (Direction attemptDir : attemptDirections) {
            BlockState placeState = createPlacementState(block, blockStateNbt, attemptDir, machineFacing, level, targetPos);

            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Attempt facing={}: placeState={}", attemptDir, placeState);
            }

            // If the block is a double-height block (doors, tall plants), verify space above is air or valid
            if (placeState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                BlockPos abovePos = targetPos.above();
                if (abovePos.getY() >= level.getMaxBuildHeight() || !isPositionValidForPlacement(level, abovePos, block)) {
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.info("[BLOCK MANIPULATOR]   Double block {} cannot be placed: space above is not air or valid",
                                block.getDescriptionId());
                    }
                    continue;
                }
            }

            // If the block is a bed, verify head position is clear and valid
            if (placeState.hasProperty(BlockStateProperties.BED_PART)) {
                if (!worldDirection.getAxis().isHorizontal()) {
                    continue;
                }
                BlockPos headPos = targetPos.relative(worldDirection);
                if (headPos.getY() >= level.getMaxBuildHeight() || headPos.getY() < level.getMinBuildHeight()
                        || !isPositionValidForPlacement(level, headPos, block)) {
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.info("[BLOCK MANIPULATOR]   Bed {} cannot be placed: head position {} is not valid",
                                block.getDescriptionId(), headPos);
                    }
                    continue;
                }
            }

            // Check if the block can survive at this position
            boolean canSurvive = canBlockSurvive(block, placeState, level, targetPos);
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   canBlockSurvive={}", canSurvive);
            }
            if (!canSurvive) {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Block {} cannot survive with facing={}",
                            block.getDescriptionId(), attemptDir);
                }
                continue;
            }

            // Try to place the block
            if (placeBlockAt(level, targetPos, placeState, blockEntityNbt, itemHandler, invSlot, block, worldDirection)) {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Placed {} at {} with facing={}",
                            block.getDescriptionId(), targetPos, attemptDir);
                }
                return true;
            } else {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   placeBlockAt returned false for facing={}", attemptDir);
                }
            }
        }

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   All attempts failed for {}", block.getDescriptionId());
        }
        return false;
    }

    /**
     * Handles scaffolding placement with direction-aware logic.
     * Vertical output (UP/DOWN): Stack upward, finding the top of existing scaffolding.
     * Horizontal output: Place in the direction of output, extending horizontally like a player.
     * If no existing scaffolding, places normally at the target position.
     */
  	private boolean tryPlaceScaffolding(
			Level level,
			BlockPos targetPos,
			CompoundTag blockStateNbt,
			CompoundTag blockEntityNbt,
			ItemStackHandler itemHandler,
			int invSlot,
			Direction worldDirection,
			Direction machineFacing
	) {
		BlockPos placePos;

		if (Config.isDebugLoggingEnabled()) {
			LOGGER.info("[BLOCK MANIPULATOR]   === Scaffolding placement started ===");
			LOGGER.info("[BLOCK MANIPULATOR]   Target pos: {}, worldDirection: {}, machineFacing: {}", targetPos, worldDirection, machineFacing);
			LOGGER.info("[BLOCK MANIPULATOR]   Target block: {}", level.getBlockState(targetPos).getBlock());
		}

		// Vertical output (UP/DOWN): Stack upward behavior
		if (worldDirection.getAxis().isVertical()) {
			if (Config.isDebugLoggingEnabled()) {
				LOGGER.info("[BLOCK MANIPULATOR]   Vertical output direction — stacking upward");
			}
			// Check if scaffolding already exists at the target position — stacking case
			if (level.getBlockState(targetPos).is(Blocks.SCAFFOLDING)) {
				if (Config.isDebugLoggingEnabled()) {
					LOGGER.info("[BLOCK MANIPULATOR]   Scaffolding at target {} — stacking on top", targetPos);
				}
				BlockPos stackTop = findScaffoldingStackTop(level, targetPos);
				if (Config.isDebugLoggingEnabled()) {
					LOGGER.info("[BLOCK MANIPULATOR]   Stack top found: {}", stackTop);
				}
				if (stackTop == null || stackTop.getY() >= level.getMaxBuildHeight() - 1) {
					if (Config.isDebugLoggingEnabled()) {
						LOGGER.info("[BLOCK MANIPULATOR]   Cannot stack: stackTop=null={} or at build height {}", stackTop == null, stackTop != null ? stackTop.getY() : "N/A");
					}
					return false;
				}
				placePos = stackTop.above();
				if (Config.isDebugLoggingEnabled()) {
					LOGGER.info("[BLOCK MANIPULATOR]   Place position (stacked): {}", placePos);
				}
			} else {
				// No scaffolding at target — normal placement
				if (Config.isDebugLoggingEnabled()) {
					LOGGER.info("[BLOCK MANIPULATOR]   No scaffolding at target — normal placement at {}", targetPos);
				}
				placePos = targetPos;
			}
		} else {
			// Horizontal output: Extend in the direction of output, like player placement
			if (Config.isDebugLoggingEnabled()) {
				LOGGER.info("[BLOCK MANIPULATOR]   Horizontal output direction — extending horizontally");
			}
			// Check if scaffolding already exists at the target position
			if (level.getBlockState(targetPos).is(Blocks.SCAFFOLDING)) {
				// Find the end of the horizontal scaffolding chain in the output direction
				BlockPos chainEnd = findScaffoldingChainEnd(level, targetPos, worldDirection);
				if (Config.isDebugLoggingEnabled()) {
					LOGGER.info("[BLOCK MANIPULATOR]   Horizontal chain end: {}", chainEnd);
				}
				placePos = chainEnd.relative(worldDirection);
				if (Config.isDebugLoggingEnabled()) {
					LOGGER.info("[BLOCK MANIPULATOR]   Place position (horizontal extend): {}", placePos);
				}
			} else {
				// No scaffolding at target — normal placement
				if (Config.isDebugLoggingEnabled()) {
					LOGGER.info("[BLOCK MANIPULATOR]   No scaffolding at target — normal placement at {}", targetPos);
				}
				placePos = targetPos;
			}
		}

		// Check place position is clear
		if (!isPositionValidForPlacement(level, placePos, Blocks.SCAFFOLDING)) {
			if (Config.isDebugLoggingEnabled()) {
				LOGGER.info("[BLOCK MANIPULATOR]   Place position not valid: {} (block: {})", placePos, level.getBlockState(placePos).getBlock());
			}
			return false;
		}
		if (Config.isDebugLoggingEnabled()) {
			LOGGER.info("[BLOCK MANIPULATOR]   Place position is clear: {}", placePos);
		}

		// Calculate distance, excluding the machine block from counting as support
		BlockPos machinePos = targetPos.relative(worldDirection.getOpposite());
		int distance = calculateScaffoldingDistance(level, placePos, machinePos);
		if (Config.isDebugLoggingEnabled()) {
			LOGGER.info("[BLOCK MANIPULATOR]   Calculated distance: {}", distance);
		}

        boolean bottom = !level.getBlockState(placePos.below()).is(Blocks.SCAFFOLDING);
        BlockState placeState = Blocks.SCAFFOLDING.defaultBlockState()
                .setValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE, distance)
                .setValue(net.minecraft.world.level.block.ScaffoldingBlock.BOTTOM, bottom);
        // Apply waterlogging
        if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(placePos);
            placeState = placeState.setValue(BlockStateProperties.WATERLOGGED,
                    fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   Placing scaffolding at {} distance={}, bottom={}", placePos, distance, bottom);
        }
        if (level.setBlock(placePos, placeState, 3)) {
            itemHandler.extractItem(invSlot, 1, false);
            net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, placePos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }
            return true;
        }
        return false;
    }

    /**
     * Finds the end of a horizontal scaffolding chain in the given direction.
     * Starting from the given position, follows scaffolding blocks in the direction
     * until a non-scaffolding block is found, then returns the last scaffolding position.
     */
    private BlockPos findScaffoldingChainEnd(Level level, BlockPos startPos, Direction direction) {
        BlockPos current = startPos;
        while (level.getBlockState(current).is(Blocks.SCAFFOLDING)) {
            BlockPos next = current.relative(direction);
            if (!level.getBlockState(next).is(Blocks.SCAFFOLDING)) {
                break;
            }
            current = next;
        }
        return current; // Return the last scaffolding block in the chain
    }

    /**
     * Finds the top of a scaffolding stack by going up from the given position.
     * Returns the highest scaffolding position in the stack.
     */
    private BlockPos findScaffoldingStackTop(Level level, BlockPos pos) {
        BlockPos top = pos;
        while (level.getBlockState(top).is(Blocks.SCAFFOLDING)) {
            top = top.above();
        }
        return top.below(); // Return the last scaffolding block
    }

    /**
     * Calculates the scaffolding DISTANCE property for a position.
     * Distance is the minimum horizontal distance from a solid supporting block.
     * Scaffolding can survive up to distance 6 from support.
     * The machine position is excluded so the machine itself doesn't count as support.
     */
    private int calculateScaffoldingDistance(Level level, BlockPos pos, BlockPos machinePos) {
        // Check if there's scaffolding below — inherit its distance
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.SCAFFOLDING)) {
            return below.getValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE);
        }
        // Check if there's a solid block below — distance 0 (but not the machine)
        if (pos.below().equals(machinePos)) {
            // Machine is below — don't count it as support
        } else if (below.isSolidRender(level, pos.below()) && below.getBlock() != Blocks.AIR) {
            return 0;
        }
        // Check horizontal directions for nearby scaffolding or solid blocks
        int minDistance = 7;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adjPos = pos.relative(dir);
            // Skip the machine's position
            if (adjPos.equals(machinePos)) {
                continue;
            }
            BlockState adjState = level.getBlockState(adjPos);
            if (adjState.is(Blocks.SCAFFOLDING)) {
                int adjDistance = adjState.getValue(net.minecraft.world.level.block.ScaffoldingBlock.DISTANCE);
                if (adjDistance < minDistance) {
                    minDistance = adjDistance;
                }
            } else if (adjState.isSolidRender(level, adjPos) && adjState.getBlock() != Blocks.AIR) {
                return 0;
            }
        }
        return Math.min(minDistance + 1, 7);
    }

    /**
     * Gets horizontal wall directions to attempt, in priority order:
     * 1. Wall opposite the machine (attachDirection)
     * 2. Side walls (clockwise, counter-clockwise)
     * The machine's own wall (worldDirection) is strictly excluded so blocks are never attached to the machine.
     */
    private List<Direction> getWallPlacementDirections(Direction worldDirection) {
        List<Direction> dirs = new ArrayList<>(3);
        if (worldDirection.getAxis().isHorizontal()) {
            Direction opposite = worldDirection.getOpposite(); // Wall opposite the machine
            dirs.add(opposite);
            dirs.add(opposite.getClockWise());        // Side wall 1
            dirs.add(opposite.getCounterClockWise()); // Side wall 2
            // Note: worldDirection (the machine's own wall) is intentionally excluded!
        }
        return dirs;
    }

    /**
     * Creates a block state for placement with the given facing direction.
     * Applies HORIZONTAL_FACING, FACING, ATTACH_FACE, AXIS, HANGING, and WATERLOGGED appropriately.
     */
    private BlockState createPlacementState(Block block, CompoundTag blockStateNbt, Direction facing, Direction machineFacing, Level level, BlockPos targetPos) {
        BlockState placeState = block.defaultBlockState();

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   createPlacementState: defaultState={}, blockStateNbt={}", placeState, blockStateNbt != null);
        }

        // If we have stored block state data, try to use it as a base
        if (blockStateNbt != null) {
            try {
                placeState = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), blockStateNbt);
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Restored from NBT: {}", placeState);
                }
            } catch (Exception e) {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Failed to read NBT block state: {}", e.getMessage());
                }
                // Fall back to default state
            }
        }

        // Apply HORIZONTAL_FACING if the block supports it
        if (placeState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            if (facing.getAxis().isHorizontal()) {
                placeState = placeState.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            } else if (machineFacing != null && machineFacing.getAxis().isHorizontal()) {
                placeState = placeState.setValue(BlockStateProperties.HORIZONTAL_FACING, machineFacing);
            }
        }

        // Apply FACING if the block supports it and the direction is valid for this block
        if (placeState.hasProperty(BlockStateProperties.FACING)) {
            if (BlockStateProperties.FACING.getPossibleValues().contains(facing)) {
                placeState = placeState.setValue(BlockStateProperties.FACING, facing);
            }
        }

        // Apply ATTACH_FACE for face-attached blocks (buttons, levers, grindstone)
        if (placeState.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            if (facing == Direction.DOWN) {
                placeState = placeState.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
            } else if (facing == Direction.UP) {
                placeState = placeState.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING);
            } else if (facing.getAxis().isHorizontal()) {
                placeState = placeState.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL);
            }
        }

        // Apply BELL_ATTACHMENT for bells
        if (placeState.hasProperty(BlockStateProperties.BELL_ATTACHMENT)) {
            if (facing == Direction.DOWN) {
                placeState = placeState.setValue(BlockStateProperties.BELL_ATTACHMENT, net.minecraft.world.level.block.state.properties.BellAttachType.FLOOR);
            } else if (facing == Direction.UP) {
                placeState = placeState.setValue(BlockStateProperties.BELL_ATTACHMENT, net.minecraft.world.level.block.state.properties.BellAttachType.CEILING);
            } else if (facing.getAxis().isHorizontal()) {
                placeState = placeState.setValue(BlockStateProperties.BELL_ATTACHMENT, net.minecraft.world.level.block.state.properties.BellAttachType.SINGLE_WALL);
            }
        }

        // Apply AXIS if the block supports it
        if (placeState.hasProperty(BlockStateProperties.AXIS)) {
            placeState = placeState.setValue(BlockStateProperties.AXIS, facing.getAxis());
        }

        // Apply HANGING for lanterns (true = ceiling, false = floor)
        if (placeState.hasProperty(BlockStateProperties.HANGING)) {
            placeState = placeState.setValue(BlockStateProperties.HANGING, facing == Direction.UP);
        }

        // Apply ROTATION for hanging signs, standing signs, banners, and skulls (0-15, where 0=south, 4=west, 8=north, 12=east)
        // The "rotation" property is defined on each block class, not in BlockStateProperties, so we check by name.
        Property<Integer> rotationProp = null;
        for (Property<?> prop : placeState.getProperties()) {
            if (prop.getName().equals("rotation")) {
                rotationProp = (Property<Integer>) (Property<?>) prop;
                break;
            }
        }
        if (rotationProp != null) {
            Direction rotationDir;
            if (facing.getAxis().isHorizontal()) {
                // Wall-mounted or floor/ceiling with horizontal facing: face the placement direction
                rotationDir = facing;
            } else if (facing == Direction.UP) {
                // Ceiling-mounted: face away from the machine (use machine facing as the outward direction)
                rotationDir = machineFacing != null ? machineFacing : Direction.SOUTH;
            } else {
                // Floor-mounted (facing DOWN): face away from the machine
                rotationDir = machineFacing != null ? machineFacing : Direction.SOUTH;
            }
            int rotation = switch (rotationDir) {
                case SOUTH -> 0;
                case WEST -> 4;
                case NORTH -> 8;
                case EAST -> 12;
                default -> 0;
            };
            placeState = placeState.setValue(rotationProp, rotation);
        }

        // Apply WATERLOGGED if applicable
        if (placeState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(targetPos);
            placeState = placeState.setValue(BlockStateProperties.WATERLOGGED, fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   Final placeState: {}", placeState);
        }
        return placeState;
    }

    /**
     * Checks if a block can survive at a position.
     * Uses BlockState#canSurvive directly.
     */
    private boolean canBlockSurvive(Block block, BlockState state, Level level, BlockPos pos) {
        try {
            return state.canSurvive(level, pos);
        } catch (Exception e) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   canSurvive exception for {}: {}", block.getDescriptionId(), e.getMessage());
            }
            return true;
        }
    }

    /**
     * Checks if a position is valid for placing the given block.
     * Valid if the position is air, or if the block is waterloggable and the position contains a water source.
     */
    private boolean isPositionValidForPlacement(Level level, BlockPos pos, Block block) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (block.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(pos);
            boolean isWaterSource = fluidState.is(Fluids.WATER) && fluidState.isSource();
            if (Config.isDebugLoggingEnabled() && isWaterSource) {
                LOGGER.info("[BLOCK MANIPULATOR]   Position {} contains water source for waterloggable block {}", pos, block.getDescriptionId());
            }
            return isWaterSource;
        }
        return false;
    }

    /**
     * Returns the wall variant of a block, if one exists.
     * Maps floor/ceiling blocks to their wall-mounted equivalents:
     * - TorchBlock → WallTorchBlock (torch, soul torch, redstone torch)
     * - HangingSignBlock → WallHangingSignBlock
     * - StandingSignBlock → WallSignBlock
     * - BannerBlock → WallBannerBlock
     * - SkullBlock → WallSkullBlock
     * Returns null if the block has no wall variant.
     */
    private net.minecraft.world.level.block.Block getWallVariant(Block block) {
        // Torch → Wall Torch
        if (block == Blocks.TORCH) return Blocks.WALL_TORCH;
        if (block == Blocks.SOUL_TORCH) return Blocks.SOUL_WALL_TORCH;
        if (block == Blocks.REDSTONE_TORCH) return Blocks.REDSTONE_WALL_TORCH;

        // Hanging Sign → Wall Hanging Sign
        if (block == Blocks.OAK_HANGING_SIGN) return Blocks.OAK_WALL_HANGING_SIGN;
        if (block == Blocks.SPRUCE_HANGING_SIGN) return Blocks.SPRUCE_WALL_HANGING_SIGN;
        if (block == Blocks.BIRCH_HANGING_SIGN) return Blocks.BIRCH_WALL_HANGING_SIGN;
        if (block == Blocks.JUNGLE_HANGING_SIGN) return Blocks.JUNGLE_WALL_HANGING_SIGN;
        if (block == Blocks.ACACIA_HANGING_SIGN) return Blocks.ACACIA_WALL_HANGING_SIGN;
        if (block == Blocks.DARK_OAK_HANGING_SIGN) return Blocks.DARK_OAK_WALL_HANGING_SIGN;
        if (block == Blocks.CRIMSON_HANGING_SIGN) return Blocks.CRIMSON_WALL_HANGING_SIGN;
        if (block == Blocks.WARPED_HANGING_SIGN) return Blocks.WARPED_WALL_HANGING_SIGN;
        if (block == Blocks.MANGROVE_HANGING_SIGN) return Blocks.MANGROVE_WALL_HANGING_SIGN;
        if (block == Blocks.CHERRY_HANGING_SIGN) return Blocks.CHERRY_WALL_HANGING_SIGN;
        if (block == Blocks.BAMBOO_HANGING_SIGN) return Blocks.BAMBOO_WALL_HANGING_SIGN;

        // Standing Sign → Wall Sign
        if (block == Blocks.OAK_SIGN) return Blocks.OAK_WALL_SIGN;
        if (block == Blocks.SPRUCE_SIGN) return Blocks.SPRUCE_WALL_SIGN;
        if (block == Blocks.BIRCH_SIGN) return Blocks.BIRCH_WALL_SIGN;
        if (block == Blocks.JUNGLE_SIGN) return Blocks.JUNGLE_WALL_SIGN;
        if (block == Blocks.ACACIA_SIGN) return Blocks.ACACIA_WALL_SIGN;
        if (block == Blocks.DARK_OAK_SIGN) return Blocks.DARK_OAK_WALL_SIGN;
        if (block == Blocks.CRIMSON_SIGN) return Blocks.CRIMSON_WALL_SIGN;
        if (block == Blocks.WARPED_SIGN) return Blocks.WARPED_WALL_SIGN;
        if (block == Blocks.MANGROVE_SIGN) return Blocks.MANGROVE_WALL_SIGN;
        if (block == Blocks.CHERRY_SIGN) return Blocks.CHERRY_WALL_SIGN;
        if (block == Blocks.BAMBOO_SIGN) return Blocks.BAMBOO_WALL_SIGN;

        // Banners
        if (block == Blocks.WHITE_BANNER) return Blocks.WHITE_WALL_BANNER;
        if (block == Blocks.ORANGE_BANNER) return Blocks.ORANGE_WALL_BANNER;
        if (block == Blocks.MAGENTA_BANNER) return Blocks.MAGENTA_WALL_BANNER;
        if (block == Blocks.LIGHT_BLUE_BANNER) return Blocks.LIGHT_BLUE_WALL_BANNER;
        if (block == Blocks.YELLOW_BANNER) return Blocks.YELLOW_WALL_BANNER;
        if (block == Blocks.LIME_BANNER) return Blocks.LIME_WALL_BANNER;
        if (block == Blocks.PINK_BANNER) return Blocks.PINK_WALL_BANNER;
        if (block == Blocks.GRAY_BANNER) return Blocks.GRAY_WALL_BANNER;
        if (block == Blocks.LIGHT_GRAY_BANNER) return Blocks.LIGHT_GRAY_WALL_BANNER;
        if (block == Blocks.CYAN_BANNER) return Blocks.CYAN_WALL_BANNER;
        if (block == Blocks.PURPLE_BANNER) return Blocks.PURPLE_WALL_BANNER;
        if (block == Blocks.BLUE_BANNER) return Blocks.BLUE_WALL_BANNER;
        if (block == Blocks.BROWN_BANNER) return Blocks.BROWN_WALL_BANNER;
        if (block == Blocks.GREEN_BANNER) return Blocks.GREEN_WALL_BANNER;
        if (block == Blocks.RED_BANNER) return Blocks.RED_WALL_BANNER;
        if (block == Blocks.BLACK_BANNER) return Blocks.BLACK_WALL_BANNER;

        // Skulls / Heads
        if (block == Blocks.SKELETON_SKULL) return Blocks.SKELETON_WALL_SKULL;
        if (block == Blocks.WITHER_SKELETON_SKULL) return Blocks.WITHER_SKELETON_WALL_SKULL;
        if (block == Blocks.ZOMBIE_HEAD) return Blocks.ZOMBIE_WALL_HEAD;
        if (block == Blocks.PLAYER_HEAD) return Blocks.PLAYER_WALL_HEAD;
        if (block == Blocks.CREEPER_HEAD) return Blocks.CREEPER_WALL_HEAD;
        if (block == Blocks.DRAGON_HEAD) return Blocks.DRAGON_WALL_HEAD;
        if (block == Blocks.PIGLIN_HEAD) return Blocks.PIGLIN_WALL_HEAD;

        return null;
    }

    /**
     * Places a block at a position and restores block entity data if present.
     * Consumes one block from the inventory slot.
     *
     * @return true if the block was placed successfully
     */
    private boolean placeBlockAt(
            Level level,
            BlockPos targetPos,
            BlockState placeState,
            CompoundTag blockEntityNbt,
            ItemStackHandler itemHandler,
            int invSlot,
            Block block,
            Direction worldDirection
    ) {
        if (!isPositionValidForPlacement(level, targetPos, block)) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   placeBlockAt: target position not valid for placement");
            }
            return false;
        }

        boolean isDoubleBlock = placeState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
        BlockPos abovePos = targetPos.above();

        if (isDoubleBlock) {
            if (abovePos.getY() >= level.getMaxBuildHeight() || !isPositionValidForPlacement(level, abovePos, block)) {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   placeBlockAt: space above double block is not air or exceeds height");
                }
                return false;
            }
        }

        boolean isBed = placeState.hasProperty(BlockStateProperties.BED_PART);
        BlockPos headPos = isBed ? targetPos.relative(worldDirection) : null;
        if (isBed) {
            if (!worldDirection.getAxis().isHorizontal()
                    || headPos.getY() < level.getMinBuildHeight()
                    || headPos.getY() >= level.getMaxBuildHeight()
                    || !isPositionValidForPlacement(level, headPos, block)) {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   placeBlockAt: bed head position not valid");
                }
                return false;
            }
        }

        BlockState lowerState;
        if (isDoubleBlock) {
            lowerState = placeState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        } else if (isBed) {
            lowerState = placeState
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, worldDirection)
                    .setValue(BlockStateProperties.BED_PART, BedPart.FOOT)
                    .setValue(BlockStateProperties.OCCUPIED, false);
        } else {
            lowerState = placeState;
        }

        if (lowerState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = level.getFluidState(targetPos);
            lowerState = lowerState.setValue(BlockStateProperties.WATERLOGGED, fluidState.is(Fluids.WATER) && fluidState.isSource());
        }

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   placeBlockAt: calling setBlock with state={}", lowerState);
        }
        boolean setResult = level.setBlock(targetPos, lowerState, 3);
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   setBlock result={}", setResult);
        }
        if (setResult) {
            if (isDoubleBlock) {
                BlockState upperState = placeState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                if (upperState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    FluidState fluidAbove = level.getFluidState(abovePos);
                    upperState = upperState.setValue(BlockStateProperties.WATERLOGGED, fluidAbove.is(Fluids.WATER) && fluidAbove.isSource());
                }
                level.setBlock(abovePos, upperState, 3);
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Placed upper half of double block at {}", abovePos);
                }
            } else if (isBed && headPos != null) {
                BlockState headState = placeState
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, worldDirection)
                        .setValue(BlockStateProperties.BED_PART, BedPart.HEAD)
                        .setValue(BlockStateProperties.OCCUPIED, false);
                if (headState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    FluidState fluidHead = level.getFluidState(headPos);
                    headState = headState.setValue(BlockStateProperties.WATERLOGGED, fluidHead.is(Fluids.WATER) && fluidHead.isSource());
                }
                level.setBlock(headPos, headState, 3);
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Placed bed: FOOT at {}, HEAD at {} facing {}", targetPos, headPos, worldDirection);
                }
            }

            // Restore block entity data if present
            if (blockEntityNbt != null && placeState.hasBlockEntity()) {
                BlockEntity newBlockEntity = level.getBlockEntity(targetPos);
                if (newBlockEntity != null) {
                    newBlockEntity.handleUpdateTag(blockEntityNbt, level.registryAccess());
                    newBlockEntity.setChanged();
                }
            }

            // Consume one block from inventory
            itemHandler.extractItem(invSlot, 1, false);

            // Play the block-specific place sound
            net.minecraft.world.level.block.SoundType soundType = placeState.getSoundType();
            net.minecraft.sounds.SoundEvent placeSound = soundType.getPlaceSound();
            if (placeSound != null) {
                level.playSound(null, targetPos, placeSound,
                        net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
            }

            return true;
        }
        return false;
    }

    /**
     * Breaks a block in the world in the given world direction and stores it in a specific inventory slot.
     * Instant break for testing purposes (no progress tracking).
     * Preserves block entity data (NBT) for blocks like chests, furnaces, and Machine Framework blocks.
     */
    private void breakBlockInSlot(
            Level level,
            BlockPos machinePos,
            MachineFrameworkBlockEntity blockEntity,
            ItemStackHandler itemHandler,
            int invSlot,
            Direction worldDirection
    ) {
        BlockPos targetPos = machinePos.relative(worldDirection);

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR] breakBlockInSlot: targetPos={}, invSlot={}, worldDir={}", targetPos, invSlot, worldDirection);
        }

        // Check if there's a block to break
        BlockState targetState = level.getBlockState(targetPos);
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   Target block state: {}", targetState.getBlock().getDescriptionId());
        }
        if (targetState.isAir() || targetState.is(Blocks.BEDROCK) || targetState.is(Blocks.BARRIER)) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Skipping: block is air, bedrock, or barrier");
            }
            return;
        }

        // Skip redstone components — they belong in the Redstone Module, not the Block Manipulator
        Block block = targetState.getBlock();
        if (block == Blocks.REDSTONE_WIRE || block == Blocks.REPEATER || block == Blocks.COMPARATOR ||
                block == Blocks.REDSTONE_TORCH || block == Blocks.LEVER || block instanceof ButtonBlock ||
                block instanceof RailBlock || block == Blocks.TRIPWIRE_HOOK || block == Blocks.TRIPWIRE) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Skipping: redstone component {} belongs to Redstone Module", block.getDescriptionId());
            }
            return;
        }

        // Get the block and create the drop item
        // Use getCloneItemStack to get the correct item with block state properties (e.g., facing, axis)
        ItemStack dropStack = block.getCloneItemStack(level, targetPos, targetState);

        // Only preserve NBT data for blocks that have block entities.
        // Simple blocks (stone, dirt, etc.) should remain plain items that stack normally.
        // Attaching NBT to every block would make them non-stackable with their vanilla counterparts.
        BlockEntity blockEntityAtPos = level.getBlockEntity(targetPos);
        if (blockEntityAtPos != null) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Block {} has block entity, capturing data", block.getDescriptionId());
            }

            // Preserve block state (facing, axis, etc.) so the placed block matches the original
            CompoundTag blockStateNbt = NbtUtils.writeBlockState(targetState);

            // Preserve block entity data (NBT) if present
            // IMPORTANT: We must capture the block entity's inventory BEFORE destroying the block,
            // otherwise destroyBlock() will drop all the items as entities. We also must clear the
            // inventory after capturing to prevent duping. This prevents duping of modules and items.
            CompoundTag blockEntityNbt = blockEntityAtPos.saveCustomOnly(level.registryAccess());
            if (!blockEntityNbt.isEmpty()) {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Captured block entity data for {}: keys={}", block.getDescriptionId(), blockEntityNbt.getAllKeys());
                }
            }

            // For block entities with inventories (chests, furnaces, etc.), capture the inventory
            // items directly using the ItemHandler capability, since saveCustomOnly() may not
            // capture vanilla inventory data. Merge the inventory into the BlockEntityTag so that
            // vanilla block placement (when the player manually places the item) can restore it.
            // This ensures consistency: both machine placement and manual placement preserve contents.
            net.neoforged.neoforge.items.IItemHandler itemHandlerCap =
                    level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, targetPos, null);
            if (itemHandlerCap != null) {
                // Check if there are any items to capture
                boolean hasItems = false;
                for (int i = 0; i < itemHandlerCap.getSlots(); i++) {
                    if (!itemHandlerCap.getStackInSlot(i).isEmpty()) {
                        hasItems = true;
                        break;
                    }
                }
                if (hasItems) {
                    // Manually serialize the inventory to NBT in the format vanilla chests use
                    net.minecraft.nbt.ListTag itemsList = new net.minecraft.nbt.ListTag();
                    for (int i = 0; i < itemHandlerCap.getSlots(); i++) {
                        ItemStack slotStack = itemHandlerCap.getStackInSlot(i);
                        if (!slotStack.isEmpty()) {
                            CompoundTag slotNbt = new CompoundTag();
                            slotNbt.putInt("Slot", i);
                            slotStack.save(level.registryAccess(), slotNbt);
                            itemsList.add(slotNbt);
                        }
                    }
                    // Merge the inventory into the BlockEntityTag so vanilla placement restores it
                    // If saveCustomOnly returned empty, create a new tag; otherwise merge into existing
                    if (blockEntityNbt == null || blockEntityNbt.isEmpty()) {
                        blockEntityNbt = new CompoundTag();
                    }
                    blockEntityNbt.putInt("Size", itemHandlerCap.getSlots());
                    blockEntityNbt.put("Items", itemsList);
                    if (Config.isDebugLoggingEnabled()) {
                        LOGGER.info("[BLOCK MANIPULATOR]   Captured inventory data for {} with {} slots, {} items", block.getDescriptionId(), itemHandlerCap.getSlots(), itemsList.size());
                    }
                }
            }

            // Store block state and block entity data (which now includes inventory) in the item's CustomData component
            CompoundTag itemNbt = new CompoundTag();
            itemNbt.put("BlockState", blockStateNbt);
            if (blockEntityNbt != null && !blockEntityNbt.isEmpty()) {
                itemNbt.put("BlockEntityTag", blockEntityNbt);
            }
            dropStack.set(DataComponents.CUSTOM_DATA, CustomData.of(itemNbt));
        } else {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Block {} has no block entity, storing as plain item (stackable)", block.getDescriptionId());
            }
        }

        // Clear the block entity's inventory to prevent drops when the block is destroyed
        // This is safe because we've already captured all the data in NBT
        if (blockEntityAtPos instanceof MachineFrameworkBlockEntity machineBe) {
            ItemStackHandler moduleHandler = machineBe.getModuleHandler();
            for (int i = 0; i < moduleHandler.getSlots(); i++) {
                moduleHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
            ItemStackHandler invHandler = machineBe.getInventoryHandler();
            for (int i = 0; i < invHandler.getSlots(); i++) {
                invHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
            machineBe.setChanged();
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Cleared Machine Framework inventory at {} to prevent drops", targetPos);
            }
        } else if (blockEntityAtPos != null) {
            // For other block entities (chests, furnaces, etc.), use the ItemHandler capability
            // to clear their inventory
            net.neoforged.neoforge.items.IItemHandler itemHandlerCap =
                    level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, targetPos, null);
            if (itemHandlerCap != null) {
                for (int i = 0; i < itemHandlerCap.getSlots(); i++) {
                    ItemStack stack = itemHandlerCap.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        itemHandlerCap.extractItem(i, stack.getCount(), false);
                    }
                }
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Cleared block entity inventory at {} to prevent drops", targetPos);
                }
            }
        }

        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   Drop stack: {}, invSlot {} current: {}", dropStack.getItem().getDescriptionId(), invSlot, itemHandler.getStackInSlot(invSlot).isEmpty() ? "empty" : itemHandler.getStackInSlot(invSlot).getItem().getDescriptionId());
        }

        // Try to insert into the specific inventory slot
        ItemStack remainder = itemHandler.insertItem(invSlot, dropStack, false);
        if (remainder.isEmpty()) {
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Stored {} in inventory slot {}", block.getDescriptionId(), invSlot);
            }
        } else {
            // Inventory slot full — drop the item in the world
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                    level, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, remainder
            );
            level.addFreshEntity(itemEntity);
            if (Config.isDebugLoggingEnabled()) {
                LOGGER.info("[BLOCK MANIPULATOR]   Inventory full, dropped {} in world", block.getDescriptionId());
            }
        }

        // Destroy the block instantly (drop=false means no block item drop)
        // The block entity was already cleared above to prevent item drops from its inventory
        if (Config.isDebugLoggingEnabled()) {
            LOGGER.info("[BLOCK MANIPULATOR]   Destroying block at {}", targetPos);
        }

        // If this is a double-height block (doors, tall plants), also remove the other half
        if (targetState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = targetState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? targetPos.above() : targetPos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(targetState.getBlock())) {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Destroying other half of double block at {}", otherPos);
                }
                level.destroyBlock(otherPos, false);
            }
        }

        // If this is a bed, also remove the other half
        if (targetState.hasProperty(BlockStateProperties.BED_PART) && targetState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            net.minecraft.world.level.block.state.properties.BedPart part = targetState.getValue(BlockStateProperties.BED_PART);
            Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos otherPos = part == net.minecraft.world.level.block.state.properties.BedPart.FOOT
                    ? targetPos.relative(facing)
                    : targetPos.relative(facing.getOpposite());
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(targetState.getBlock())) {
                if (Config.isDebugLoggingEnabled()) {
                    LOGGER.info("[BLOCK MANIPULATOR]   Destroying other half of bed (target was {}, other is at {})", part, otherPos);
                }
                level.destroyBlock(otherPos, false);
            }
        }

        level.destroyBlock(targetPos, false);
    }

    /**
     * Appends a minimal tooltip showing whether the module is configured.
     * Does NOT call super.appendHoverText() to avoid ProcessingModuleItem parsing ManipulatorMode values as SlotMode.
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull net.minecraft.world.item.Item.TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        Map<Direction, ManipulatorMode> modes = getManipulatorModes(stack, getModuleConfigKey());
        boolean configured = false;
        for (ManipulatorMode mode : modes.values()) {
            if (mode != ManipulatorMode.DISABLED) {
                configured = true;
                break;
            }
        }
        if (configured) {
            tooltipComponents.add(Component.translatable("tooltip.praeteritum_futurum.configured").withStyle(ChatFormatting.GRAY));
        }
    }
}
