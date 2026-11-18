package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.api.module.IItemHandlerModule;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class BaseModuleItem extends Item implements IItemHandlerModule {
    public BaseModuleItem(Properties properties) {
        super(properties);
    }

    /**
     * Stores the slot index during overlay rendering so border-aware button rendering works.
     * Set by the overlay renderer (e.g., MachineInfoScreen) before calling renderCustomOverlay.
     */
    private int overlaySlotIndex = -1;

    /**
     * Returns the slot index for overlay rendering. Set by the overlay renderer.
     */
    protected int getOverlaySlotIndex() {
        return overlaySlotIndex;
    }

    /**
     * Sets the slot index for overlay rendering. Called by the overlay renderer.
     */
    public void setOverlaySlotIndex(int index) {
        this.overlaySlotIndex = index;
    }

    // Default overlay bounds cover the central slot. Subclasses expand this for their buttons.
    public boolean isInsideOverlay(double mouseX, double mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
    }

    public boolean opensInlineOverlay() {
        return false; // Default modules open separate container menus via IConfigurableModule
    }

    @SuppressWarnings("unused")
    public void handleCustomClick(double mouseX, double mouseY, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex, Minecraft minecraft) {
        // Default implementation does nothing
    }

    /**
     * Returns whether the player is holding the shift key during a GUI interaction.
     * Used by {@link #handleCustomClick} implementations to detect reverse mode cycling.
     * <p>
     * This wraps {@code Screen.hasShiftDown()} so all modules use the same reliable
     * GUI-context shift detection instead of {@code player.isShiftKeyDown()}, which
     * doesn't reflect keyboard state during mouse events in GUI screens.
     * </p>
     */
    @SuppressWarnings("unused")
    public boolean isShiftHeld() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }

    // Returns true if the click is on the module's close (X) button.
    // Checks the standard position above the center slot — all modules use this same position.
    public boolean isCloseButton(double mouseX, double mouseY, int slotX, int slotY) {
        return mouseX >= slotX - 18 && mouseX <= slotX - 2 && mouseY >= slotY - 18 && mouseY <= slotY - 2;
    }

    /**
     * Controls whether the module's center slot (the module slot itself) should allow
     * normal inventory interaction (pickup/place items) while the config overlay is open.
     * Override to return {@code true} if the module has an actual inventory slot that
     * the player should be able to interact with while configuring (e.g., Jukebox Module's disc slot).
     * <p>
     * The default implementation returns {@code false}, which blocks clicks on the module slot
     * while the overlay is open (preventing accidental item manipulation during configuration).
     * </p>
     */
    public boolean allowsCenterSlotInteraction() {
        return false;
    }

    /**
     * Controls whether the module's slot item should be hidden in the machine screen.
     * When {@code true}, the machine screen skips rendering the item in the slot,
     * allowing the module's overlay to fully cover the slot without the item showing through.
     * <p>
     * The default implementation returns {@code false}, meaning the item is rendered normally.
     * </p>
     * <p>
     * Example: JukeboxModuleItem returns {@code true} when a song is playing so the overlay
     * and progress bar are visible without the disc item showing through.
     * </p>
     */
    @SuppressWarnings("unused")
    public boolean hidesSlotItemInMachineScreen(ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        return false;
    }

    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {}

    public boolean shouldHideSlot(int configuringSlotIndex, int targetSlotIndex) {
        int row = configuringSlotIndex / MachineFrameworkBlockEntity.GRID_WIDTH;
        int col = configuringSlotIndex % MachineFrameworkBlockEntity.GRID_WIDTH;

        if (targetSlotIndex == configuringSlotIndex) return true;
        if (row > 0 && targetSlotIndex == configuringSlotIndex - MachineFrameworkBlockEntity.GRID_WIDTH) return true;
        if (row < MachineFrameworkBlockEntity.GRID_ROWS - 1 && targetSlotIndex == configuringSlotIndex + MachineFrameworkBlockEntity.GRID_WIDTH) return true;
        if (col > 0 && targetSlotIndex == configuringSlotIndex - 1) return true;
        if (col < MachineFrameworkBlockEntity.GRID_WIDTH - 1 && targetSlotIndex == configuringSlotIndex + 1) return true;
        return row > 0 && col > 0 && targetSlotIndex == configuringSlotIndex - MachineFrameworkBlockEntity.GRID_WIDTH - 1;
    }

    /**
     * Controls whether this module's inventory slot should be interactable by the player
     * in the machine UI (pickup/place items). This is opt-in: modules must explicitly
     * override this to return {@code true} to allow player interaction with their slot.
     * <p>
     * The default implementation returns {@code false}, meaning the slot is not interactable
     * — items can only be transferred via IO modules or other automation.
     * </p>
     * <p>
     * Example: The Jukebox Module returns {@code true} so the player can manually
     * place/remove music discs from its slot while the config overlay is open.
     * </p>
     */
    @SuppressWarnings("unused")
    public boolean hasInteractableInventorySlot() {
        return false;
    }

    /**
     * Checks whether an item is valid for this module's interactable inventory slot.
     * Used by the machine UI to decide whether clicking the module with this item
     * should place the item or open the config overlay.
     * <p>
     * The default implementation returns {@code false}. Override to return {@code true}
     * for items that the module accepts (e.g., music discs for the Jukebox Module).
     * </p>
     *
     * @param stack the item the player is holding
     * @param blockEntity the machine framework block entity
     * @param slotIndex the module's slot index in the grid
     * @return {@code true} if the item can be placed in this module's inventory slot
     */
    @SuppressWarnings("unused")
    public boolean isItemValidForSlot(ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        return false;
    }

    /**
     * Controls whether this module uses the center slot in its config overlay.
     * When {@code false}, the center slot is rendered as a dark hatch lid to indicate
     * it has no function for this module. When {@code true}, the module controls
     * how the center slot is rendered (ghost item, real inventory, custom rendering).
     * <p>
     * The default implementation returns {@code false}, meaning the center slot is
     * not used by this module.
     * </p>
     * <p>
     * Example: StonecutterModuleItem returns {@code true} for its ghost target item.
     * JukeboxModuleItem returns {@code true} for its real disc slot.
     * InventoryModuleItem returns {@code false} — the center slot does nothing.
     * </p>
     */
    @SuppressWarnings("unused")
    public boolean usesCenterSlot() {
        return false;
    }

    @SuppressWarnings("unused")
    @Override
    public @Nullable IItemHandlerModifiable getItemHandlerForSide(
            Level level,
            BlockPos pos,
            MachineFrameworkBlockEntity blockEntity,
            int slotIndex,
            ItemStack stack,
            Direction side) {
        return null;
    }

    public CompoundTag getBlueprintData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        return customData.copyTag();
    }

    public void renderCustomOverlay(GuiGraphics graphics, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, Font font) {}

    public void tickModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
    }

    /**
     * Draws the standard close (X) button with a raised border, positioned above the center slot.
     * Subclasses call this from {@link #renderCustomOverlay} instead of duplicating the fill calls.
     */
    @SuppressWarnings("unused")
    protected void drawCloseButton(GuiGraphics graphics, Font font, int slotX, int slotY) {
        graphics.fill(slotX - 18, slotY - 18, slotX - 3, slotY - 3, 0xFF8B0000);
        graphics.fill(slotX - 18, slotY - 18, slotX - 3, slotY - 17, 0xFFAA4444);
        graphics.fill(slotX - 18, slotY - 18, slotX - 17, slotY - 3, 0xFFAA4444);
        graphics.fill(slotX - 18, slotY - 3, slotX - 2, slotY - 2, 0xFF660000);
        graphics.fill(slotX - 3, slotY - 18, slotX - 2, slotY - 2, 0xFF660000);
        graphics.drawCenteredString(font, "X", slotX - 10, slotY - 14, 0xFFFFFF);
    }


    /**
     * Draws a dark hatch lid over the center slot to indicate it has no function for this module.
     * Subclasses call this from {@link #renderCustomOverlay} when {@link #usesCenterSlot()} returns {@code false}.
     */
    @SuppressWarnings("unused")
    protected void drawDarkHatchLid(GuiGraphics graphics, int slotX, int slotY) {
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF555555);
        graphics.fill(slotX, slotY, slotX + 16, slotY + 1, 0xFF777777);
        graphics.fill(slotX, slotY, slotX + 1, slotY + 16, 0xFF777777);
        graphics.fill(slotX, slotY + 15, slotX + 16, slotY + 16, 0xFF333333);
        graphics.fill(slotX + 15, slotY, slotX + 16, slotY + 16, 0xFF333333);
    }

    /**
     * Controls whether this module can be picked up (removed) from its slot via the wrench/config menu.
     * Override to return {@code false} while the module is in a state that should prevent removal,
     * e.g., while a song is playing or while an operation is in progress.
     * <p>
     * The default implementation returns {@code true}, allowing the module to be removed freely.
     * The {@link com.thextrunner.praeteritum_futurum.inventory.ModuleConfigSlotItemHandler} also checks
     * if the inventory slot is empty — both conditions must pass for the module to be removable.
     * </p>
     *
     * @param level the world
     * @param pos the machine framework block position
     * @param blockEntity the machine framework block entity
     * @param slotIndex the module's slot index in the grid
     * @param stack the module item stack
     * @return {@code true} if the module can be picked up; {@code false} to lock it in place
     */
    public boolean mayPickupModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        return true;
    }

    /**
     * Server-side handler for direction-button clicks on grid-adjacent configurable modules.
     * <p>
     * When a player clicks a U/D/L/R direction button on a module overlay, the client sends
     * a {@code ServerboundToggleModuleDirectionPacket} to the server. The server calls this
     * method on the module item to cycle the mode for that direction.
     * <p>
     * Subclasses that use direction-based configuration (ProcessingModuleItem, EjectionModuleItem,
     * etc.) override this method to perform their own mode cycling and NBT updates.
     * The default implementation does nothing.
     * <p>
     * The {@code shiftHeld} parameter indicates whether the player held shift, allowing
     * the mode to cycle in reverse order. This makes shift-click reverse cycling universal
     * for all modules — subclasses simply check {@code shiftHeld} and cycle accordingly.
     * <p>
     * This is the same pattern used by {@link #getFaceModes(ItemStack)} for IO-face configuration:
     * the packet handler checks {@code instanceof BaseModuleItem} and delegates to the subclass.
     *
     * @param stack       The module item stack
     * @param direction   The grid direction (UP/DOWN/LEFT/RIGHT) that was clicked
     * @param blockEntity The parent machine framework block entity
     * @param slotIndex   The grid slot index of this module
     * @param shiftHeld   Whether the player held shift (cycles mode in reverse)
     */
    public void toggleDirectionMode(ItemStack stack, GridDirection direction, MachineFrameworkBlockEntity blockEntity, int slotIndex, boolean shiftHeld) {
        // Default no-op — subclasses override to cycle their direction modes
    }

    /**
     * Server-side handler for auto-resetting a direction config when the adjacent module
     * is no longer valid (border or disallowed module type).
     * <p>
     * When the client detects a stale direction config during validation, it resets the
     * client-side NBT and sends a {@code ServerboundResetModuleDirectionPacket}. The server
     * calls this method to perform the same reset on its copy of the NBT.
     * <p>
     * Subclasses override this method to reset their specific NBT structure.
     * The default implementation does nothing.
     *
     * @param stack       The module item stack
     * @param direction   The grid direction (UP/DOWN/LEFT/RIGHT) to reset
     * @param blockEntity The parent machine framework block entity
     * @param slotIndex   The grid slot index of this module
     */
    public void resetDirectionMode(ItemStack stack, GridDirection direction, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        // Default no-op — subclasses override to reset their direction modes
    }

    public Map<RelativeSide, IoModuleItem.FaceMode> getFaceModes(ItemStack stack) {
        return Map.of();
    }

    public void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos) {}

    /**
     * Returns the max stack limit for a specific inventory slot used by this module.
     * Override in subclasses to restrict slot capacity (e.g., ingredient slots limited to 1).
     *
     * @param blockEntity The parent machine framework
     * @param moduleSlotIndex The grid slot index where this module is placed
     * @param inventorySlotIndex The inventory slot index to check
     * @return The max stack limit for the slot, or 0 to use the default limit
     */
    public int getInventorySlotLimit(MachineFrameworkBlockEntity blockEntity, int moduleSlotIndex, int inventorySlotIndex) {
        return 0; // 0 means use default (no restriction)
    }

    // ========================================================================
    // Adjacent Module Detection Infrastructure
    // ========================================================================
    // These methods provide a shared way for configurable modules to detect
    // what type of module sits adjacent in each grid direction, and to declare
    // which adjacent types are relevant for their configuration.
    // ========================================================================

    /**
     * Types of modules that can sit adjacent to a configurable module.
     * Used by {@link #getAdjacentModuleType} and {@link #getAllowedAdjacentModuleTypes}.
     */
    public enum AdjacentModuleType {
        /** No module or an empty slot */
        NONE,
        /** Inventory module — provides item storage */
        INVENTORY,
        /** IO module — determines world-facing directions */
        IO,
        /** Filter module — filters items flowing through IO */
        FILTER,
        /** Processing module — transforms items (smelting, brewing, etc.) */
        PROCESSING,
        /** Logistics module — manages item flow (ejection, etc.) */
        LOGISTICS
    }

    /**
     * Returns the grid slot index adjacent to the given slot in the specified direction.
     * Returns -1 if no adjacent slot exists in that direction (border of the grid).
     * Delegates to {@link GridDirection#getAdjacentSlotIndex(int)}.
     */
    public static int getAdjacentSlotIndex(int slotIndex, GridDirection dir) {
        return dir.getAdjacentSlotIndex(slotIndex);
    }

    /**
     * Checks if the given slot index has an adjacent slot in the specified direction.
     * Returns false for border positions where no adjacent slot exists.
     * Instance method — subclasses may override for custom behavior.
     */
    public boolean hasAdjacentSlot(int slotIndex, GridDirection dir) {
        return dir.hasAdjacentSlot(slotIndex);
    }

    /**
     * Static version of {@link #hasAdjacentSlot(int, GridDirection)} for use in static contexts.
     */
    public static boolean hasAdjacentSlotStatic(int slotIndex, GridDirection dir) {
        return dir.hasAdjacentSlot(slotIndex);
    }

    /**
     * Determines the type of module sitting adjacent to the given slot in the specified direction.
     * Returns {@link AdjacentModuleType#NONE} if no relevant module is found.
     */
    public static AdjacentModuleType getAdjacentModuleType(MachineFrameworkBlockEntity blockEntity, int slotIndex, GridDirection dir) {
        int adjSlot = getAdjacentSlotIndex(slotIndex, dir);
        if (adjSlot < 0) {
            return AdjacentModuleType.NONE;
        }

        ItemStackHandler moduleHandler = blockEntity.getModuleHandler();
        ItemStack adjStack = moduleHandler.getStackInSlot(adjSlot);
        if (adjStack.isEmpty()) return AdjacentModuleType.NONE;

        return getModuleType(adjStack);
    }

    /**
     * Determines the type of a module from its item stack.
     */
    public static AdjacentModuleType getModuleType(ItemStack stack) {
        if (stack.isEmpty()) return AdjacentModuleType.NONE;

        if (stack.is(ModItems.INVENTORY_MODULE.get())) return AdjacentModuleType.INVENTORY;
        if (stack.is(ModItems.IO_MODULE.get())) return AdjacentModuleType.IO;
        if (stack.is(ModItems.FILTER_MODULE.get())) return AdjacentModuleType.FILTER;
        if (stack.is(ModItems.EJECTION_MODULE.get())) return AdjacentModuleType.LOGISTICS;

        // Check if it's a processing module by checking the item instance
        Item item = stack.getItem();
        if (item instanceof ProcessingModuleItem) return AdjacentModuleType.PROCESSING;

        // Check if it's a logistics module (but not one already matched above)
        if (item instanceof LogisticsModuleItem) return AdjacentModuleType.LOGISTICS;

        return AdjacentModuleType.NONE;
    }

    /**
     * Returns the set of adjacent module types this module cares about for configuration.
     * Subclasses override this to declare which adjacent module types are relevant.
     * <p>
     * For example, a processing module returns only {@link AdjacentModuleType#INVENTORY},
     * while an ejection module returns both {@link AdjacentModuleType#INVENTORY} and
     * {@link AdjacentModuleType#IO}.
     *
     * @return List of adjacent module types that this module can configure directions for
     */
    public List<AdjacentModuleType> getAllowedAdjacentModuleTypes() {
        return List.of();
    }

    /**
     * Checks if the given adjacent module type is allowed for configuration by this module.
     * Delegates to {@link #getAllowedAdjacentModuleTypes()}.
     *
     * @param type The adjacent module type to check
     * @return true if this module can configure directions toward this type of adjacent module
     */
    public boolean isAdjacentModuleAllowed(AdjacentModuleType type) {
        return getAllowedAdjacentModuleTypes().contains(type);
    }

    /**
     * Checks if the adjacent slot in the given direction has a module type that this
     * module can configure for.
     *
     * @param blockEntity The parent machine framework
     * @param slotIndex The grid slot index of this module
     * @param dir The grid direction to check
     * @return true if there's an adjacent slot with an allowed module type
     */
    public boolean hasAllowedAdjacentModule(MachineFrameworkBlockEntity blockEntity, int slotIndex, GridDirection dir) {
        if (!hasAdjacentSlotStatic(slotIndex, dir)) return false;
        AdjacentModuleType type = getAdjacentModuleType(blockEntity, slotIndex, dir);
        return isAdjacentModuleAllowed(type);
    }

    /**
     * Shared helper to determine which direction button was clicked based on mouse position.
     * Only returns a direction if the adjacent slot exists (border awareness).
     *
     * @param slotX X coordinate of the module slot
     * @param slotY Y coordinate of the module slot
     * @param mx Mouse X coordinate
     * @param my Mouse Y coordinate
     * @param slotIndex The grid slot index of this module
     * @return The clicked grid direction, or null if no direction button was clicked
     */
    @Nullable
    public GridDirection getClickedDirection(int slotX, int slotY, double mx, double my, int slotIndex) {
        int centerX = slotX + 8;
        int centerY = slotY + 8;
        int offset = 18;

        if (hasAdjacentSlotStatic(slotIndex, GridDirection.UP) && isInsideButton(mx, my, centerX, slotY - (offset - 8))) return GridDirection.UP;
        if (hasAdjacentSlotStatic(slotIndex, GridDirection.LEFT) && isInsideButton(mx, my, slotX - (offset - 8), centerY)) return GridDirection.LEFT;
        if (hasAdjacentSlotStatic(slotIndex, GridDirection.RIGHT) && isInsideButton(mx, my, slotX + 16 + (offset - 8), centerY)) return GridDirection.RIGHT;
        if (hasAdjacentSlotStatic(slotIndex, GridDirection.DOWN) && isInsideButton(mx, my, centerX, slotY + 16 + (offset - 8))) return GridDirection.DOWN;
        return null;
    }

    /**
     * Checks if the mouse is inside a circular button at the given position.
     */
    public static boolean isInsideButton(double mx, double my, int x, int y) {
        return mx >= x - 8 && mx < x + 8 && my >= y - 8 && my < y + 8;
    }

    // ========================================================================
    // Direction Config Validation — auto-disable invalid directions
    // ========================================================================
    // When a module has a direction configured but the adjacent slot is a border
    // or holds a disallowed module type, the config should be reset to disabled.
    // This is called from handleCustomClick and renderCustomOverlay to keep
    // the config in sync with the actual machine layout.
    // ========================================================================

    /**
     * Validates all configured directions and resets any that are no longer valid.
     * A direction is invalid if it's a border (no adjacent slot) or the adjacent
     * module type is not allowed by this module.
     * <p>
     * Subclasses override {@link #resetDirection(ItemStack, GridDirection, MachineFrameworkBlockEntity, int)}
     * to perform the actual NBT reset for their config format.
     * After resetting the client-side NBT, the caller should send a
     * {@code ServerboundResetModuleDirectionPacket} for each reset direction so the
     * server syncs its copy of the NBT.
     *
     * @param stack       The module item stack
     * @param blockEntity The parent machine framework
     * @param slotIndex   The grid slot index of this module
     * @return true if any direction was reset, false if all configs were already valid
     */
    public boolean validateDirectionConfig(ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        boolean changed = false;
        for (GridDirection dir : GridDirection.VALUES) {
            if (!dir.hasAdjacentSlot(slotIndex)) {
                // Border — reset if configured
                if (isDirectionConfigured(stack, dir)) {
                    resetDirection(stack, dir, blockEntity, slotIndex);
                    changed = true;
                }
                continue;
            }

            AdjacentModuleType adjType = getAdjacentModuleType(blockEntity, slotIndex, dir);
            if (!isAdjacentModuleAllowed(adjType)) {
                // Wrong module type — reset if configured
                if (isDirectionConfigured(stack, dir)) {
                    resetDirection(stack, dir, blockEntity, slotIndex);
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Returns the list of directions that have an active (non-disabled) configuration.
     * Used after validation to know which directions need server-side sync.
     *
     * @param stack The module item stack
     * @return List of grid directions that are currently configured
     */
    public java.util.List<GridDirection> getConfiguredDirections(ItemStack stack) {
        java.util.List<GridDirection> configured = new java.util.ArrayList<>();
        for (GridDirection dir : GridDirection.VALUES) {
            if (isDirectionConfigured(stack, dir)) {
                configured.add(dir);
            }
        }
        return configured;
    }

    /**
     * Checks if a direction has a non-default (non-disabled) configuration.
     * Override in subclasses to check their specific NBT structure.
     * The default implementation returns false (no direction is configured).
     *
     * @param stack   The module item stack
     * @param dir     The grid direction to check
     * @return true if this direction has an active (non-disabled) config
     */
    public boolean isDirectionConfigured(ItemStack stack, GridDirection dir) {
        return false;
    }

    /**
     * Resets the configuration for a specific direction to its default (disabled) state.
     * Override in subclasses to perform the actual NBT update for their config format.
     * The default implementation does nothing.
     *
     * @param stack       The module item stack to modify
     * @param dir         The grid direction to reset
     * @param blockEntity The parent machine framework
     * @param slotIndex   The grid slot index of this module
     */
    public void resetDirection(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        // Default no-op — subclasses override to reset their NBT
    }

    /**
     * Validates direction config and sends reset packets for any directions that were cleared.
     * Subclasses that use GridDirection-based config should call this at the start of
     * {@link #handleCustomClick(double, double, int, int, ItemStack, MachineFrameworkBlockEntity, int, net.minecraft.client.Minecraft)}.
     * <p>
     * This combines three steps into one call:
     * 1. Collects currently configured directions.
     * 2. Validates and resets any that are no longer valid (border or disallowed module type).
     * 3. Sends {@code ServerboundResetModuleDirectionPacket} for each reset direction.
     * <p>
     * Subclasses only need to override {@link #isDirectionConfigured(ItemStack, GridDirection)},
     * {@link #resetDirection(ItemStack, GridDirection, MachineFrameworkBlockEntity, int)}, and
     * {@link #resetDirectionMode(ItemStack, GridDirection, MachineFrameworkBlockEntity, int)}.
     */
    protected void validateAndSyncDirectionConfig(ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
        // Collect configured directions before validation so we can detect resets
        java.util.Set<GridDirection> configuredBefore = java.util.Set.copyOf(getConfiguredDirections(stack));

        // Validate config — auto-disable directions that are no longer valid
        validateDirectionConfig(stack, blockEntity, slotIndex);

        // Send reset packets for directions that were just reset
        for (GridDirection dir : configuredBefore) {
            if (!isDirectionConfigured(stack, dir)) {
                // This direction was reset — sync to server
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new com.thextrunner.praeteritum_futurum.network.ServerboundResetModuleDirectionPacket(
                                slotIndex, dir, blockEntity.getBlockPos()
                        )
                );
            }
        }
    }

    // ========================================================================
    // NBT / CustomData helpers — available to all module types
    // ========================================================================

    /**
     * Reads a sub-compound from the module's CustomData NBT.
     * Returns an empty CompoundTag if the key doesn't exist.
     */
    @SuppressWarnings("unused")
    protected static CompoundTag getCustomTag(ItemStack stack, String key) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(key)) {
                return tag.getCompound(key);
            }
        }
        return new CompoundTag();
    }

    /**
     * Modifies a sub-compound in the module's CustomData NBT.
     * Uses direct component replacement so client-side UI updates immediately.
     */
    @SuppressWarnings("unused")
    protected static void modifyCustomData(ItemStack stack, String key, java.util.function.Consumer<CompoundTag> action) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        CompoundTag subTag = tag.contains(key) ? tag.getCompound(key) : new CompoundTag();
        action.accept(subTag);
        tag.put(key, subTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Sets or removes the max stack size component based on whether the module has configuration.
     * When configured, the module becomes stack-size 1 (like a tool).
     */
    @SuppressWarnings("unused")
    protected static void updateStackSize(ItemStack stack, boolean hasConfiguration) {
        if (hasConfiguration) {
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        } else {
            stack.remove(DataComponents.MAX_STACK_SIZE);
        }
    }
}