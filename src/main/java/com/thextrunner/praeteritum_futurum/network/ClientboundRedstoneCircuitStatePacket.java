package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuitState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Packet sent from server to client with the computed redstone circuit state.
 * <p>
 * The server runs propagation during tickModule, creates a RedstoneCircuitState snapshot,
 * and sends it to any client viewing the redstone config screen. The client screen
 * stores this state and uses it for rendering overlays and tooltips — no local
 * propagation needed.
 * <p>
 * This is essential for syncing stateful properties (like repeater lock state) that
 * are computed server-side and must be displayed on the client.
 */
public record ClientboundRedstoneCircuitStatePacket(BlockPos blockPos, int moduleSlotIndex, CompoundTag stateData) implements CustomPacketPayload {
    public static final Type<ClientboundRedstoneCircuitStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("praeteritum_futurum", "clientbound_redstone_circuit_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRedstoneCircuitStatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ClientboundRedstoneCircuitStatePacket::blockPos,
                    ByteBufCodecs.INT, ClientboundRedstoneCircuitStatePacket::moduleSlotIndex,
                    ByteBufCodecs.COMPOUND_TAG, ClientboundRedstoneCircuitStatePacket::stateData,
                    ClientboundRedstoneCircuitStatePacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundRedstoneCircuitStatePacket packet, IPayloadContext context) {
        // Handle on the client thread — update the screen's server-synced circuit state
        context.enqueueWork(() -> {
            RedstoneCircuitState state = RedstoneCircuitState.deserializeNBT(packet.stateData());
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof com.thextrunner.praeteritum_futurum.screen.RedstoneConfigScreen screen) {
                if (screen.matchesModule(packet.blockPos(), packet.moduleSlotIndex())) {
                    screen.setServerCircuitState(state);
                }
            }
        });
    }
}
