package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.IoModuleItem;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ServerboundToggleIoFacePacket(int slotIndex, Direction direction, net.minecraft.core.BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<ServerboundToggleIoFacePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "toggle_io_face")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundToggleIoFacePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ServerboundToggleIoFacePacket::slotIndex,
            Direction.STREAM_CODEC,
            ServerboundToggleIoFacePacket::direction,
            net.minecraft.core.BlockPos.STREAM_CODEC,
            ServerboundToggleIoFacePacket::blockPos,
            ServerboundToggleIoFacePacket::new
    );

    @Override
    public @NotNull Type<ServerboundToggleIoFacePacket> type() {
        return TYPE;
    }

    public static void handle(ServerboundToggleIoFacePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            // Directly grab the block entity from the world position sent by the packet
            if (player.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity blockEntity) {
                ItemStack stack = blockEntity.getModuleHandler().getStackInSlot(packet.slotIndex());
                if (stack.is(com.thextrunner.praeteritum_futurum.ModItems.IO_MODULE.get())) {
                    var modes = IoModuleItem.getFaceModes(stack);
                    var currentMode = modes.get(packet.direction());
                    var nextMode = currentMode.next();
                    IoModuleItem.setFaceMode(stack, packet.direction(), nextMode);
                    blockEntity.setChanged();
                    // Optional: sync client block entity data here if needed
                }
            }
        });
    }
}