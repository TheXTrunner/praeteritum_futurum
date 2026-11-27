package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
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

/**
 * Packet sent when a module's direction config is auto-reset because the adjacent
 * module is no longer valid (border or disallowed module type).
 * <p>
 * Flow: Client validates direction config on overlay open → finds stale config →
 * resets client-side NBT → sends this packet → server resets its copy of the NBT.
 * <p>
 * This ensures both client and server agree on the direction state after a machine
 * layout change (e.g., replacing an adjacent module with a different type).
 */
public record ServerboundResetModuleDirectionPacket(int slotIndex, GridDirection direction, BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<ServerboundResetModuleDirectionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "reset_module_direction")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundResetModuleDirectionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ServerboundResetModuleDirectionPacket::slotIndex,
            ByteBufCodecs.INT.map(
                    o -> GridDirection.VALUES[o],
                    GridDirection::ordinal
            ),
            ServerboundResetModuleDirectionPacket::direction,
            BlockPos.STREAM_CODEC,
            ServerboundResetModuleDirectionPacket::blockPos,
            ServerboundResetModuleDirectionPacket::new
    );

    @Override
    public @NotNull Type<ServerboundResetModuleDirectionPacket> type() {
        return TYPE;
    }

    public static void handle(ServerboundResetModuleDirectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ModulePacketHandler.handleAtBlock(player, packet.blockPos(), packet.slotIndex(),
                    BaseModuleItem.class::isInstance,
                    stack -> {
                        BaseModuleItem module = (BaseModuleItem) stack.getItem();
                        MachineFrameworkBlockEntity blockEntity =
                                (MachineFrameworkBlockEntity) player.level().getBlockEntity(packet.blockPos());
                        module.resetDirectionMode(stack, packet.direction(), blockEntity, packet.slotIndex());
                    },
                    "reset module direction");
        });
    }
}
