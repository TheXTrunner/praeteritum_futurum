package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Cycles a processing module's redstone-control policy. */
public record ServerboundCycleRedstoneControlPacket(int slotIndex, BlockPos blockPos,
                                                    boolean reverse) implements CustomPacketPayload {
    public static final Type<ServerboundCycleRedstoneControlPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "cycle_redstone_control")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundCycleRedstoneControlPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ServerboundCycleRedstoneControlPacket::slotIndex,
                    BlockPos.STREAM_CODEC,
                    ServerboundCycleRedstoneControlPacket::blockPos,
                    ByteBufCodecs.BOOL,
                    ServerboundCycleRedstoneControlPacket::reverse,
                    ServerboundCycleRedstoneControlPacket::new
            );

    @Override
    public @NotNull Type<ServerboundCycleRedstoneControlPacket> type() {
        return TYPE;
    }

    public static void handle(ServerboundCycleRedstoneControlPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ModulePacketHandler.handleAtBlock(player, packet.blockPos(), packet.slotIndex(),
                    item -> item instanceof BaseModuleItem module && module.supportsRedstoneControl(),
                    stack -> {
                        if (stack.getItem() instanceof BaseModuleItem module) {
                            module.cycleRedstoneControlMode(stack, packet.reverse());
                        }
                    },
                    "cycle module redstone control");
        });
    }
}