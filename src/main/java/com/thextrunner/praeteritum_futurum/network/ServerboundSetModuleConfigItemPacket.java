package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Packet sent when the player sets a config/filter item for a processing module.
 * <p>
 * Flow: Player places an item in the config slot of the module screen → client sends this packet
 * → server stores the item reference in the module's NBT data.
 * <p>
 * Used by processing modules (Smelting, Stonecutter) to remember which item to process,
 * and by filter modules to remember which item to allow/block.
 */
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
            ServerPlayer player = (ServerPlayer) context.player();
            ModulePacketHandler.handleAtBlock(player, packet.blockPos(), packet.slotIndex,
                    ProcessingModuleItem.class::isInstance,
                    stack -> {
                        ProcessingModuleItem procModule = (ProcessingModuleItem) stack.getItem();
                        // Validate that the module accepts this item for its config slot
                        if (!packet.targetItem().isEmpty() && !procModule.acceptsConfigSlotItem(packet.targetItem())) {
                            return;
                        }
                        procModule.setConfigSlotItem(stack, packet.targetItem, player.level().registryAccess());
                    },
                    "set module config item");
            // Refresh the block state so clients see updated config
            if (player.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity blockEntity) {
                player.level().sendBlockUpdated(packet.blockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            }
        });
    }
}