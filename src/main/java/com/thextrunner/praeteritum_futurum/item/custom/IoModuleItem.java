package com.thextrunner.praeteritum_futurum.item.custom;

import com.mojang.logging.LogUtils;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.api.module.CyclingMode;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.network.ServerboundToggleIoFacePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class IoModuleItem extends LogisticsModuleItem {

    private static final Logger LOGGER = LogUtils.getLogger();

    public IoModuleItem(Properties properties) {
        super(properties);
    }

    /**
     * Renders the background overlay texture on top of the module slot in the GUI.
     */
    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity blockEntity) {
        graphics.blit(
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "textures/gui/container/io_module_overlay.png"),
                slot.x, slot.y,
                0.0f, 0.0f,
                16, 16,
                16, 16
        );
    }

    /**
     * Defines the bounding box area that triggers the inline configuration overlay when interacting with the slot.
     */
    @Override
    public boolean isInsideOverlay(double mouseX, double mouseY, int slotX, int slotY) {
        return isWithinBounds(mouseX, mouseY, slotX, slotY, 16, 16)
                || isWithinBounds(mouseX, mouseY, slotX, slotY - 18, 16, 16)
                || isWithinBounds(mouseX, mouseY, slotX, slotY + 18, 16, 16)
                || isWithinBounds(mouseX, mouseY, slotX - 18, slotY, 16, 16)
                || isWithinBounds(mouseX, mouseY, slotX + 18, slotY, 16, 16)
                || isWithinBounds(mouseX, mouseY, slotX + 36, slotY, 16, 16)
                || isCloseButton(mouseX, mouseY, slotX, slotY);
    }

    /**
     * Determines which relative side button the player clicked in the overlay UI.
     */
    private RelativeSide getIoClickedRelativeSide(int slotX, int slotY, double mx, double my) {
        if (isWithinBounds(mx, my, slotX, slotY, 16, 16)) return RelativeSide.FRONT;
        if (isWithinBounds(mx, my, slotX, slotY - 18, 16, 16)) return RelativeSide.UP;
        if (isWithinBounds(mx, my, slotX, slotY + 18, 16, 16)) return RelativeSide.DOWN;
        if (isWithinBounds(mx, my, slotX - 18, slotY, 16, 16)) return RelativeSide.LEFT;
        if (isWithinBounds(mx, my, slotX + 18, slotY, 16, 16)) return RelativeSide.RIGHT;
        if (isWithinBounds(mx, my, slotX + 36, slotY, 16, 16)) return RelativeSide.BACK;
        return null;
    }

    /**
     * Specifies that IO modules use an inline UI overlay rather than opening a separate menu screen.
     */
    @Override
    public boolean opensInlineOverlay() {
        return true;
    }

    /**
     * Hides underlying container slots when the configuration overlay is open to prevent accidental item clicks.
     */
    @Override
    public boolean shouldHideSlot(int configuringSlotIndex, int targetSlotIndex) {
        int row = configuringSlotIndex / MachineFrameworkBlockEntity.GRID_WIDTH;
        int col = configuringSlotIndex % MachineFrameworkBlockEntity.GRID_WIDTH;

        if (targetSlotIndex == configuringSlotIndex) return true;
        if (row > 0 && targetSlotIndex == configuringSlotIndex - MachineFrameworkBlockEntity.GRID_WIDTH) return true;
        if (row < MachineFrameworkBlockEntity.GRID_ROWS - 1 && targetSlotIndex == configuringSlotIndex + MachineFrameworkBlockEntity.GRID_WIDTH) return true;
        if (col > 0 && targetSlotIndex == configuringSlotIndex - 1) return true;
        if (col < MachineFrameworkBlockEntity.GRID_WIDTH - 1 && targetSlotIndex == configuringSlotIndex + 1) return true;
        if (col < MachineFrameworkBlockEntity.GRID_WIDTH - 2 && targetSlotIndex == configuringSlotIndex + 2) return true;
        return col > 1 && targetSlotIndex == configuringSlotIndex - 2;
    }

    /**
     * Renders the interactive configuration overlay buttons around the module slot.
     */
    @Override
    public void renderCustomOverlay(GuiGraphics graphics, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, Font font) {
        Map<RelativeSide, FaceMode> modes = getFaceModes(stack);

        // Draw textless textured face buttons for each relative side
        drawConfigButton(graphics, slotX, slotY, modes.get(RelativeSide.FRONT), RelativeSide.FRONT);
        drawConfigButton(graphics, slotX, slotY - 18, modes.get(RelativeSide.UP), RelativeSide.UP);
        drawConfigButton(graphics, slotX, slotY + 18, modes.get(RelativeSide.DOWN), RelativeSide.DOWN);
        drawConfigButton(graphics, slotX - 18, slotY, modes.get(RelativeSide.LEFT), RelativeSide.LEFT);
        drawConfigButton(graphics, slotX + 18, slotY, modes.get(RelativeSide.RIGHT), RelativeSide.RIGHT);
        drawConfigButton(graphics, slotX + 36, slotY, modes.get(RelativeSide.BACK), RelativeSide.BACK);

        // Draw the close button in the slot two positions to the left of center.
        drawCloseButton(graphics, font, slotX - 18, slotY + 18);
    }

    @Override
    public List<Component> getCustomOverlayTooltip(double mouseX, double mouseY, int slotX, int slotY,
                                                   ItemStack stack, MachineFrameworkBlockEntity blockEntity,
                                                   int slotIndex) {
        List<Component> baseTooltip = super.getCustomOverlayTooltip(mouseX, mouseY, slotX, slotY,
                stack, blockEntity, slotIndex);
        if (!baseTooltip.isEmpty()) return baseTooltip;

        RelativeSide side = getIoClickedRelativeSide(slotX, slotY, mouseX, mouseY);
        if (side == null) return List.of();

        FaceMode mode = getFaceModes(stack).get(side);
        return List.of(
                Component.literal("Machine face: " + side.name()).withStyle(ChatFormatting.AQUA),
                tooltipValue("Mode: ", mode.name(), mode.getColor()),
                Component.literal(getFaceModeDescription(mode)).withStyle(ChatFormatting.GRAY),
                tooltipHint("Click to cycle; Shift-click to cycle backward")
        );
    }

    private String getFaceModeDescription(FaceMode mode) {
        return switch (mode) {
            case DISABLED -> "No transfer";
            case INPUT -> "Pulls items/signals";
            case OUTPUT -> "Pushes items/signals";
            case OPEN -> "Face accepts insertion/extraction";
        };
    }

    /**
     * Checks if the user clicked the close button on the overlay menu.
     * The IO Module places the close button in the slot two positions to the left of center.
     */
    @Override
    public boolean isCloseButton(double mouseX, double mouseY, int slotX, int slotY) {
        // The close button is drawn in the slot two positions to the left of the module.
        return isWithinBounds(mouseX, mouseY, slotX - 36, slotY, 16, 16);
    }

    /**
     * Renders an individual relative face configuration button using the machine face textures
     * and a small color marker for its configured mode.
     */
    private void drawConfigButton(GuiGraphics graphics, int x, int y, FaceMode mode, RelativeSide side) {
        Minecraft mc = Minecraft.getInstance();
        String sideName = side.name().toLowerCase();

        TextureAtlasSprite frameSprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/frame"));
        TextureAtlasSprite faceSprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/" + sideName));

        graphics.blit(x, y, 0, 16, 16, frameSprite, 1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(x + 3, y + 3, 0, 10, 10, faceSprite, 1.0f, 1.0f, 1.0f, 1.0f);

        if (mode != null && mode != FaceMode.DISABLED) {
            int markerSize = mode == FaceMode.INPUT ? 8 : 6;
            int markerOffset = (16 - markerSize) / 2;
            graphics.fill(x + markerOffset, y + markerOffset,
                    x + markerOffset + markerSize, y + markerOffset + markerSize, mode.getColor());
        }
    }

    @Override
    public void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos) {
        // Client-side overlay handled via screen state
    }

    /**
     * Handles mouse clicks on the configuration overlay, cycles the face mode, updates NBT, and syncs via packet.
     */
    @Override
    public void handleCustomClick(double mouseX, double mouseY, int slotX, int slotY, ItemStack stack, MachineFrameworkBlockEntity blockEntity, int slotIndex, net.minecraft.client.Minecraft minecraft) {
        RelativeSide clickedSide = getIoClickedRelativeSide(slotX, slotY, mouseX, mouseY);
        if (clickedSide != null) {
            if (minecraft.player != null) {
                minecraft.player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
            }
            Map<RelativeSide, FaceMode> modes = getFaceModes(stack);
            FaceMode nextMode = isShiftHeld() ? modes.get(clickedSide).previous() : modes.get(clickedSide).next();
            setFaceMode(stack, clickedSide, nextMode);

            PacketDistributor.sendToServer(new ServerboundToggleIoFacePacket(slotIndex, clickedSide, blockEntity.getBlockPos(), isShiftHeld()));
        }
    }

    /**
     * Provides an item handler for external pipes and automation based on configured relative side modes.
     */
    @Override
    public @Nullable IItemHandlerModifiable getItemHandlerForSide(
            Level level,
            BlockPos pos,
            MachineFrameworkBlockEntity blockEntity,
            int slotIndex,
            ItemStack stack,
            Direction side) {
        if (blockEntity == null) return null;

        Direction machineFacing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(MachineFrameworkBlock.FACING)) {
            machineFacing = blockEntity.getBlockState().getValue(MachineFrameworkBlock.FACING);
        }
        RelativeSide relSide = RelativeSide.fromWorldDirection(side, machineFacing);

        Map<RelativeSide, FaceMode> modes = getFaceModes(stack);
        FaceMode mode = modes.get(relSide);

        if (mode == FaceMode.DISABLED) {
            return null;
        }

        int[] adjacentIndices = blockEntity.getAdjacentSlots(slotIndex);
        List<IItemHandlerModifiable> handlers = new ArrayList<>();

        for (int adj : adjacentIndices) {
            ItemStack adjModule = blockEntity.getModuleHandler().getStackInSlot(adj);
            if (adjModule.is(com.thextrunner.praeteritum_futurum.item.ModItems.INVENTORY_MODULE.get())) {
                handlers.add(new FilteredSingleSlotItemHandler(blockEntity.getInventoryHandler(), adj, blockEntity, slotIndex));
            }
        }

        if (handlers.isEmpty()) {
            return null;
        }

        return new CombinedInvWrapper(handlers.toArray(new IItemHandlerModifiable[0]));
    }

    /**
     * Custom item handler wrapper that checks filter rules before allowing item insertion.
     */
    public static class FilteredSingleSlotItemHandler extends com.thextrunner.praeteritum_futurum.inventory.SingleSlotItemHandler {
        private final MachineFrameworkBlockEntity blockEntity;
        private final int ioModuleSlot;
        private final int targetSlot;

        public FilteredSingleSlotItemHandler(IItemHandlerModifiable itemHandler, int slot, MachineFrameworkBlockEntity blockEntity, int ioModuleSlot) {
            super(itemHandler, slot);
            this.blockEntity = blockEntity;
            this.ioModuleSlot = ioModuleSlot;
            this.targetSlot = slot;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (FilterModuleItem.isItemBlockedByFilters(blockEntity, ioModuleSlot, this.targetSlot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }
    }

    /**
     * Enum representing the operational state of an individual machine face.
     */
    public enum FaceMode implements CyclingMode<FaceMode> {
        DISABLED(0, 0xFF555555),
        INPUT(1, 0xFF3366CC),
        OUTPUT(2, 0xFFCC3333),
        OPEN(3, 0xFF9933CC);

        private final int id;
        private final int color;

        FaceMode(int id, int color) {
            this.id = id;
            this.color = color;
        }

        public int getColor() {
            return color;
        }

        /** Returns the fixed display color for this mode. */
        public int getDynamicColor(long time) {
            return color;
        }

        public static FaceMode fromId(int id) {
            for (FaceMode mode : values()) {
                if (mode.id == id) return mode;
            }
            return DISABLED;
        }

        @Override public FaceMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        @Override public FaceMode previous() {
            return values()[(this.ordinal() - 1 + values().length) % values().length];
        }
    }

    /**
     * Writes face configuration state for a specific relative side back into the module item's NBT data.
     */
    public static void setFaceMode(ItemStack stack, RelativeSide side, FaceMode mode) {
        modifyCustomData(stack, "FaceModes", facesTag -> facesTag.putInt(side.name().toLowerCase(), mode.id));

        boolean hasConfig = false;
        CompoundTag customData = getCustomTag(stack, "FaceModes");
        for (RelativeSide s : RelativeSide.values()) {
            if (customData.contains(s.name().toLowerCase()) && customData.getInt(s.name().toLowerCase()) != 0) {
                hasConfig = true;
                break;
            }
        }
        updateStackSize(stack, hasConfig);
    }

    /**
     * Gets the machine data channel used to expose redstone strength for a relative face.
     * The channel mapping is kept explicit so enum reordering cannot change the data contract.
     */
    public static int getRedstoneSignalChannel(RelativeSide side) {
        return switch (side) {
            case FRONT -> 0;
            case BACK -> 1;
            case LEFT -> 2;
            case RIGHT -> 3;
            case UP -> 4;
            case DOWN -> 5;
        };
    }

    @Override
    public Map<RelativeSide, FaceMode> getFaceModes(ItemStack stack) {
        Map<RelativeSide, FaceMode> modes = new EnumMap<>(RelativeSide.class);
        CompoundTag facesTag = getCustomTag(stack, "FaceModes");
        for (RelativeSide side : RelativeSide.values()) {
            if (facesTag.contains(side.name().toLowerCase())) {
                modes.put(side, FaceMode.fromId(facesTag.getInt(side.name().toLowerCase())));
            } else {
                modes.put(side, FaceMode.DISABLED);
            }
        }
        return modes;
    }

    @Override
    public void tickModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        // Left empty intentionally: automation is handled via explicit framework pass execution
    }

    public void tickInputs(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        if (level.isClientSide()) return;

        BlockState state = level.getBlockState(pos);
        Direction machineFacing = state.hasProperty(MachineFrameworkBlock.FACING)
                ? state.getValue(MachineFrameworkBlock.FACING)
                : Direction.NORTH;

        Map<RelativeSide, FaceMode> modes = getFaceModes(stack);
        for (Map.Entry<RelativeSide, FaceMode> entry : modes.entrySet()) {
            if (entry.getValue() == FaceMode.INPUT) {
                Direction worldDir = entry.getKey().toWorldDirection(machineFacing);
                blockEntity.pullItemsFromSide(slotIndex, worldDir);
            }
        }
    }

    public void tickOutputs(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity, int slotIndex, ItemStack stack) {
        if (level.isClientSide()) return;

        BlockState state = level.getBlockState(pos);
        Direction machineFacing = state.hasProperty(MachineFrameworkBlock.FACING)
                ? state.getValue(MachineFrameworkBlock.FACING)
                : Direction.NORTH;

        Map<RelativeSide, FaceMode> modes = getFaceModes(stack);
        for (Map.Entry<RelativeSide, FaceMode> entry : modes.entrySet()) {
            if (entry.getValue() == FaceMode.OUTPUT) {
                Direction worldDir = entry.getKey().toWorldDirection(machineFacing);
                blockEntity.pushItemsToSide(slotIndex, worldDir);
            }
        }
    }

    /**
     * Appends a clean, color-coded summary of face configurations to the item's tooltip.
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            Map<RelativeSide, FaceMode> modes = getFaceModes(stack);
            tooltipComponents.add(Component.literal("§7Face Configurations:"));
            for (Map.Entry<RelativeSide, FaceMode> entry : modes.entrySet()) {
                String sideName = entry.getKey().name();
                String capitalized = sideName.charAt(0) + sideName.substring(1).toLowerCase();
                FaceMode mode = entry.getValue();

                String colorCode = switch (mode) {
                    case DISABLED -> "§8";
                    case INPUT -> "§9";
                    case OUTPUT -> "§c";
                    case OPEN -> "§5";
                };

                tooltipComponents.add(Component.literal("§8 • §f" + capitalized + ": " + colorCode + mode.name()));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}