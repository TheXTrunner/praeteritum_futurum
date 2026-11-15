package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem;
import net.minecraft.core.BlockPos;
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

/**
 * Packet sent when the player toggles a processing module's slot direction.
 * <p>
 * Flow: Player clicks a direction arrow in the module config screen → client sends this packet
 * → server cycles the slot mode (Disabled → Input → Output → Disabled) for that direction.
 * <p>
 * Only affects ProcessingModuleItem subclasses (Smelting, Stonecutter, Composter, Grindstone).
 */
public record ServerboundToggleModuleDirectionPacket(int slotIndex, Direction direction, BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<ServerboundToggleModuleDirectionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "toggle_module_direction")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundToggleModuleDirectionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ServerboundToggleModuleDirectionPacket::slotIndex,
            Direction.STREAM_CODEC,
            ServerboundToggleModuleDirectionPacket::direction,
            BlockPos.STREAM_CODEC,
            ServerboundToggleModuleDirectionPacket::blockPos,
            ServerboundToggleModuleDirectionPacket::new
    );

    @Override
    public @NotNull Type<ServerboundToggleModuleDirectionPacket> type() {
        return TYPE;
    }

    public static void handle(ServerboundToggleModuleDirectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.distanceToSqr(packet.blockPos().getX() + 0.5, packet.blockPos().getY() + 0.5, packet.blockPos().getZ() + 0.5) > 64.0) {
                PraeteritumFuturumMod.LOGGER.warn("Player {} tried to toggle module direction from too far away at {}", player.getName().getString(), packet.blockPos());
                return;
            }
            if (player.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity blockEntity) {
                ItemStack stack = blockEntity.getModuleHandler().getStackInSlot(packet.slotIndex());

                if (stack.getItem() instanceof ProcessingModuleItem procModule) {
                    ProcessingModuleItem.SlotMode nextMode = procModule.getNextMode(stack, packet.direction());
                    ProcessingModuleItem.setSlotMode(stack, packet.direction(), nextMode, procModule.getModuleConfigKey());
                    blockEntity.setChanged();
                }
            }
        });
    }
}