package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.item.custom.FilterModuleItem;
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
 * Packet sent when the player toggles a Filter module between whitelist and blacklist mode.
 * <p>
 * Flow: Player clicks the filter mode toggle button → client sends this packet
 * → server flips the filter mode (Whitelist ↔ Blacklist) on the module stack.
 */
public record ServerboundToggleFilterModePacket(int slotIndex, BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<ServerboundToggleFilterModePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "toggle_filter_mode")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundToggleFilterModePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ServerboundToggleFilterModePacket::slotIndex,
            BlockPos.STREAM_CODEC,
            ServerboundToggleFilterModePacket::blockPos,
            ServerboundToggleFilterModePacket::new
    );

    @Override
    public @NotNull Type<ServerboundToggleFilterModePacket> type() {
        return TYPE;
    }

    public static void handle(ServerboundToggleFilterModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ModulePacketHandler.handleAtBlock(player, packet.blockPos(), packet.slotIndex(),
                    FilterModuleItem.class::isInstance,
                    stack -> FilterModuleItem.toggleFilterMode(stack),
                    "toggle filter mode");
        });
    }
}