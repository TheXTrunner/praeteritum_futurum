package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class IoModuleItem extends LogisticsModuleItem {
    public IoModuleItem(Properties properties) {
        super(properties);
    }

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

    @Override
    public void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos) {
        // Client-side overlay handled via screen state
    }
    public enum FaceMode {
        DISABLED(0),
        INPUT(1),
        OUTPUT(2),
        OPEN(3); // Both push (insert) and pull (extract)

        private final int id;

        FaceMode(int id) {
            this.id = id;
        }

        public static FaceMode fromId(int id) {
            for (FaceMode mode : values()) {
                if (mode.id == id) return mode;
            }
            return DISABLED;
        }

        public FaceMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    public static Map<Direction, FaceMode> getFaceModes(ItemStack stack) {
        Map<Direction, FaceMode> modes = new HashMap<>();
        CompoundTag facesTag = getCustomTag(stack, "FaceModes");
        for (Direction dir : Direction.values()) {
            if (facesTag.contains(dir.getName())) {
                modes.put(dir, FaceMode.fromId(facesTag.getInt(dir.getName())));
            } else {
                modes.put(dir, FaceMode.DISABLED);
            }
        }
        return modes;
    }

    public static void setFaceMode(ItemStack stack, Direction direction, FaceMode mode) {
        modifyCustomData(stack, "FaceModes", facesTag -> facesTag.putInt(direction.getName(), mode.ordinal()));

        boolean hasConfig = getFaceModes(stack).values().stream().anyMatch(m -> m != FaceMode.DISABLED);
        updateStackSize(stack, hasConfig);
    }

    public static Set<Direction> getEnabledFaces(ItemStack stack) {
        Set<Direction> enabledFaces = EnumSet.noneOf(Direction.class);
        Map<Direction, FaceMode> modes = getFaceModes(stack);
        for (Map.Entry<Direction, FaceMode> entry : modes.entrySet()) {
            if (entry.getValue() != FaceMode.DISABLED) {
                enabledFaces.add(entry.getKey());
            }
        }
        return enabledFaces;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            tooltipComponents.add(Component.literal("Modes: " + getFaceModes(stack)));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}