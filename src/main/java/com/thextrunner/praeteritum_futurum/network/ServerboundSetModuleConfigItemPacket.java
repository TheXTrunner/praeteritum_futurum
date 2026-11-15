package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
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
            Player player = context.player();
            if (player.distanceToSqr(packet.blockPos().getX() + 0.5, packet.blockPos().getY() + 0.5, packet.blockPos().getZ() + 0.5) > 64.0) {
                PraeteritumFuturumMod.LOGGER.warn("Player {} tried to set module config from too far away at {}", player.getName().getString(), packet.blockPos());
                return;
            }
            if (player.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity blockEntity) {
                ItemStack moduleStack = blockEntity.getModuleHandler().getStackInSlot(packet.slotIndex);
                if (moduleStack.getItem() instanceof ProcessingModuleItem procModule) {
                    // Validate that the module accepts this item for its config slot
                    if (!packet.targetItem().isEmpty() && !procModule.acceptsConfigSlotItem(packet.targetItem())) {
                        return;
                    }
                    procModule.setConfigSlotItem(moduleStack, packet.targetItem, player.level().registryAccess());
                    blockEntity.setChanged();
                    player.level().sendBlockUpdated(packet.blockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
                }
            }
        });
    }
}