package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ServerboundSetModuleConfigItemPacket(int slotIndex, ItemStack targetItem, BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<ServerboundSetModuleConfigItemPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("praeteritum_futurum", "set_module_config_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetModuleConfigItemPacket> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT, ServerboundSetModuleConfigItemPacket::slotIndex,
            ItemStack.OPTIONAL_STREAM_CODEC, ServerboundSetModuleConfigItemPacket::targetItem,
            BlockPos.STREAM_CODEC, ServerboundSetModuleConfigItemPacket::blockPos,
            ServerboundSetModuleConfigItemPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundSetModuleConfigItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity blockEntity) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(packet.slotIndex);
                if (moduleStack.getItem() instanceof ProcessingModuleItem procModule) {
                    procModule.setConfigSlotItem(moduleStack, packet.targetItem, player.level().registryAccess());
                    blockEntity.setChanged();
                    player.level().sendBlockUpdated(packet.blockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
                }
            }
        });
    }
}