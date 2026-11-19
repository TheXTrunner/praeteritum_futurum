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
import net.minecraft.world.item.ItemStack;
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
            if (player.distanceToSqr(packet.blockPos().getX() + 0.5, packet.blockPos().getY() + 0.5, packet.blockPos().getZ() + 0.5) > 64.0) {
                PraeteritumFuturumMod.LOGGER.warn("Player {} tried to reset module direction from too far away at {}", player.getName().getString(), packet.blockPos());
                return;
            }
            if (player.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity blockEntity) {
                ItemStack stack = blockEntity.getModuleHandler().getStackInSlot(packet.slotIndex());

                if (stack.getItem() instanceof BaseModuleItem module) {
                    module.resetDirectionMode(stack, packet.direction(), blockEntity, packet.slotIndex());
                    blockEntity.setChanged();
                }
            }
        });
    }
}
