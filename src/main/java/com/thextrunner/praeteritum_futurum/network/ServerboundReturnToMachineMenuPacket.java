package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Packet sent when the player closes a module config screen and returns to the main machine menu.
 * <p>
 * Flow: Player clicks the back button in a module config screen → client sends this packet
 * → server re-opens the MachineConfigMenu for the machine at the given position.
 * <p>
 * This ensures the player returns to the correct machine even if they moved while
 * the module config screen was open.
 */
public record ServerboundReturnToMachineMenuPacket(BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<ServerboundReturnToMachineMenuPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "return_to_machine")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundReturnToMachineMenuPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ServerboundReturnToMachineMenuPacket::blockPos,
            ServerboundReturnToMachineMenuPacket::new
    );

    @Override
    public @NotNull Type<ServerboundReturnToMachineMenuPacket> type() {
        return TYPE;
    }

    public static void handle(ServerboundReturnToMachineMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.distanceToSqr(packet.blockPos().getX() + 0.5, packet.blockPos().getY() + 0.5, packet.blockPos().getZ() + 0.5) > 64.0) {
                PraeteritumFuturumMod.LOGGER.warn("Player {} tried to return to machine menu from too far away at {}", player.getName().getString(), packet.blockPos());
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(packet.blockPos());
            if (blockEntity instanceof MachineFrameworkBlockEntity frameworkEntity) {
                player.openMenu(new net.minecraft.world.MenuProvider() {
                    @Override
                    public @NotNull net.minecraft.network.chat.Component getDisplayName() {
                        return frameworkEntity.getDisplayName();
                    }

                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.@NotNull Inventory playerInventory, net.minecraft.world.entity.player.@NotNull Player player) {
                        return frameworkEntity.createInfoMenu(containerId, playerInventory, player);
                    }
                }, buf -> buf.writeBlockPos(packet.blockPos()));
            }
        });
    }
}