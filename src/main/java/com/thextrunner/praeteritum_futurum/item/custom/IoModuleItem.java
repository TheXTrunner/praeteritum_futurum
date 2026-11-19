package com.thextrunner.praeteritum_futurum.item.custom;

import com.mojang.logging.LogUtils;
import com.thextrunner.praeteritum_futurum.Config;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.api.module.CyclingMode;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.custom.MachineFrameworkBlock;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.network.ServerboundToggleIoFacePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.gui.Font;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import java.awt.Color;
import java.util.*;

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
        // Extended area to cover all 6 face buttons, the close button, and the inventory overlap segment
        return mouseX >= slotX - 36 && mouseX <= slotX + 52 &&
                mouseY >= slotY - 18 && mouseY <= slotY + 38;
    }

    /**
     * Determines which relative side button the player clicked in the overlay UI.
     */
    private RelativeSide getIoClickedRelativeSide(int slotX, int slotY, double mx, double my) {
        if (isInside(mx, my, slotX, slotY)) return RelativeSide.FRONT;
        if (isInside(mx, my, slotX, slotY - 18)) return RelativeSide.UP;
        if (mx >= slotX && mx <= slotX + 16 && my >= slotY + 18 && my <= slotY + 38) return RelativeSide.DOWN;
        if (isInside(mx, my, slotX - 18, slotY)) return RelativeSide.LEFT;
        if (isInside(mx, my, slotX + 18, slotY)) return RelativeSide.RIGHT;
        if (isInside(mx, my, slotX + 36, slotY)) return RelativeSide.BACK;
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

        // Draw the close button at the 2nd slot to the left of center, 2 rows below the top row
        drawCloseButton(graphics, font, slotX - 18, slotY + 18);
    }

    /**
     * Checks if the user clicked the close button on the overlay menu.
     * The IO Module places the close button at the 2nd slot to the left of center, 2 rows below the top row.
     */
    @Override
    public boolean isCloseButton(double mouseX, double mouseY, int slotX, int slotY) {
        // Check the 2nd slot to the left of center, 2 rows below (offset by +18 from center Y)
        return mouseX >= slotX - 34 && mouseX <= slotX - 18 && mouseY >= slotY + 2 && mouseY <= slotY + 18;
    }

    /**
     * Renders an individual relative face configuration button using the base block texture,
     * the dynamic rainbow glow layer, and the IO status overlay.
     */
    private void drawConfigButton(GuiGraphics graphics, int x, int y, FaceMode mode, RelativeSide side) {
        // Draw dark background shell for the button container
        graphics.fill(x, y, x + 16, y + 16, 0xFF222222);

        Minecraft mc = Minecraft.getInstance();
        long time = mc.level != null ? mc.level.getGameTime() : System.currentTimeMillis() / 50;
        String sideName = side.name().toLowerCase();

        // Layer 0: Render the actual base block face texture without tint
        TextureAtlasSprite blockSprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/machine_framework_" + sideName));
        graphics.blit(x + 1, y + 1, 0, 14, 14, blockSprite, 1.0f, 1.0f, 1.0f, 1.0f);

        // Layer 1: Render the dynamic rainbow glowing texture on top
        TextureAtlasSprite glowSprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/machine_framework_" + sideName + "_glow"));

        float hue = (time * 0.015f) % 1.0f;
        int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f);
        float pulse = (float) (Math.sin(time * 0.1) * 0.3f + 0.7f);
        int r = (int) (((rgb >> 16) & 0xFF) * pulse);
        int g = (int) (((rgb >> 8) & 0xFF) * pulse);
        int b = (int) ((rgb & 0xFF) * pulse);

        float red = r / 255.0f;
        float green = g / 255.0f;
        float blue = b / 255.0f;

        graphics.blit(x + 1, y + 1, 0, 14, 14, glowSprite, red, green, blue, 1.0f);

        // Layer 2: Render the IO module status overlay texture if active
        if (mode != null && mode != FaceMode.DISABLED) {
            TextureAtlasSprite ioSprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "block/machine_framework_io_overlay"));

            int ioColor;
            if (mode == FaceMode.OPEN) {
                float cycle = (float) ((Math.sin(time * 0.15f) + 1.0f) * 0.5f);
                int ioR = (int) (0x33 + (0xCC - 0x33) * cycle);
                int ioG = (int) (0x66 + (0x33 - 0x66) * cycle);
                int ioB = (int) (0xCC + (0x33 - 0xCC) * cycle);
                ioColor = (ioR << 16) | (ioG << 8) | ioB;
            } else {
                ioColor = mode.getColor();
            }

            float ioRed = ((ioColor >> 16) & 0xFF) / 255.0f;
            float ioGreen = ((ioColor >> 8) & 0xFF) / 255.0f;
            float ioBlue = (ioColor & 0xFF) / 255.0f;

            graphics.blit(x + 1, y + 1, 0, 14, 14, ioSprite, ioRed, ioGreen, ioBlue, 1.0f);
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
     * Helper check to see if mouse coordinates are inside a standard 16x16 button area.
     */
    private boolean isInside(double mx, double my, int x, int y) {
        return mx >= x && mx < x + 16 && my >= y && my < y + 16;
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
        OPEN(3, 0xFF00FFCC);

        private final int id;
        private final int color;

        FaceMode(int id, int color) {
            this.id = id;
            this.color = color;
        }

        public int getColor() {
            return color;
        }

        /**
         * Returns the correct color for a given time, handling dynamic transitions for OPEN mode.
         */
        public int getDynamicColor(long time) {
            if (this == OPEN) {
                float cycle = (float) ((Math.sin(time * 0.15f) + 1.0f) * 0.5f);
                int ioR = (int) (0x33 + (0xCC - 0x33) * cycle);
                int ioG = (int) (0x66 + (0x33 - 0x66) * cycle);
                int ioB = (int) (0xCC + (0x33 - 0xCC) * cycle);
                return (ioR << 16) | (ioG << 8) | ioB;
            }
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
                    case OPEN -> "§a";
                };

                tooltipComponents.add(Component.literal("§8 • §f" + capitalized + ": " + colorCode + mode.name()));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}