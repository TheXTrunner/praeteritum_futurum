package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.RelativeSide;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

/**
 * Packet sent when the player toggles an IO module's face mode.
 * <p>
 * Flow: Player clicks a face button in the module config screen → client sends this packet
 * → server updates the face mode (Disabled → Input → Output → Disabled) on the module stack.
 * <p>
 * The RelativeSide enum represents the four directions relative to the machine's facing
 * (Front, Back, Left, Right), independent of the machine's actual rotation.
 */
public record ServerboundToggleIoFacePacket(int slotIndex, RelativeSide relativeSide, BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<ServerboundToggleIoFacePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "toggle_io_face")
    );

    /**
     * StreamCodec for serializing and deserializing RelativeSide over the network using RegistryFriendlyByteBuf.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, RelativeSide> RELATIVE_SIDE_STREAM_CODEC = StreamCodec.of(
            (buf, side) -> buf.writeInt(side.ordinal()),
            buf -> RelativeSide.values()[buf.readInt()]
    );

    /**
     * Main StreamCodec for the packet payload.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundToggleIoFacePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ServerboundToggleIoFacePacket::slotIndex,
            RELATIVE_SIDE_STREAM_CODEC,
            ServerboundToggleIoFacePacket::relativeSide,
            BlockPos.STREAM_CODEC,
            ServerboundToggleIoFacePacket::blockPos,
            ServerboundToggleIoFacePacket::new
    );

    @Override
    public @NotNull Type<ServerboundToggleIoFacePacket> type() {
        return TYPE;
    }

    /**
     * Handles the packet on the server side, updating the module face mode based on RelativeSide.
     * Validates that the player is within reach distance of the target block.
     */
    public static void handle(ServerboundToggleIoFacePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.distanceToSqr(packet.blockPos().getX() + 0.5, packet.blockPos().getY() + 0.5, packet.blockPos().getZ() + 0.5) > 64.0) {
                PraeteritumFuturumMod.LOGGER.warn("Player {} tried to toggle IO face from too far away at {}", player.getName().getString(), packet.blockPos());
                return;
            }
            if (player.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity blockEntity) {
                ItemStack stack = blockEntity.getModuleHandler().getStackInSlot(packet.slotIndex());
                if (stack.getItem() instanceof BaseModuleItem module) {
                    Map<RelativeSide, IoModuleItem.FaceMode> modes = module.getFaceModes(stack);
                    IoModuleItem.FaceMode currentMode = modes.get(packet.relativeSide());
                    IoModuleItem.FaceMode nextMode = currentMode.next();
                    IoModuleItem.setFaceMode(stack, packet.relativeSide(), nextMode);
                    blockEntity.setChanged();
                }
            }
        });
    }
}