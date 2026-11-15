package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Packet sent when the player toggles the public/private lock on a Machine Framework.
 * <p>
 * Flow: Player clicks the lock button in the machine screen → client sends this packet
 * → server toggles the isPublic flag on the block entity.
 */
public record ServerboundToggleMachinePublicPacket(BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<ServerboundToggleMachinePublicPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "toggle_machine_public")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundToggleMachinePublicPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ServerboundToggleMachinePublicPacket::blockPos,
            ServerboundToggleMachinePublicPacket::new
    );

    @Override
    public @NotNull Type<ServerboundToggleMachinePublicPacket> type() {
        return TYPE;
    }

    /**
     * Handles the packet on the server side, toggling the public/private state.
     * Validates that the player is within reach distance of the target block.
     */
    public static void handle(ServerboundToggleMachinePublicPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.distanceToSqr(packet.blockPos().getX() + 0.5, packet.blockPos().getY() + 0.5, packet.blockPos().getZ() + 0.5) > 64.0) {
                PraeteritumFuturumMod.LOGGER.warn("Player {} tried to toggle machine public from too far away at {}", player.getName().getString(), packet.blockPos());
                return;
            }
            if (player.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity blockEntity) {
                blockEntity.togglePublic();
            }
        });
    }
}
