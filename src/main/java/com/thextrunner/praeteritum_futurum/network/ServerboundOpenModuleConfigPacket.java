package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.api.module.IConfigurableModule;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
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

public record ServerboundOpenModuleConfigPacket(int slotIndex, BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<ServerboundOpenModuleConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("praeteritum_futurum", "open_module_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundOpenModuleConfigPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ServerboundOpenModuleConfigPacket::slotIndex,
            BlockPos.STREAM_CODEC,
            ServerboundOpenModuleConfigPacket::blockPos,
            ServerboundOpenModuleConfigPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundOpenModuleConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity be) {
                    ItemStack moduleStack = be.getModuleHandler().getStackInSlot(packet.slotIndex);
                    if (moduleStack.getItem() instanceof IConfigurableModule configurable) {
                        configurable.openConfiguration(serverPlayer, moduleStack, packet.slotIndex, packet.blockPos());
                    }
                }
            }
        });
    }
}