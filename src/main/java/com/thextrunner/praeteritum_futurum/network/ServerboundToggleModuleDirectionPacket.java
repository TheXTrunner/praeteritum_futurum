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
 * Packet sent when the player toggles a module's direction-button configuration.
 * <p>
 * Flow: Player clicks a U/D/L/R direction button in the module config overlay → client sends this packet
 * → server delegates to {@link BaseModuleItem#toggleDirectionMode} which subclasses override
 * to cycle their own modes (e.g., ProcessingModuleItem cycles INPUT/OUTPUT/FUEL,
 * EjectionModuleItem cycles DISPENSE/DROP or ENABLED/DISABLED).
 * <p>
 * Works for any {@link BaseModuleItem} subclass that overrides {@code toggleDirectionMode}.
 * <p>
 * Uses {@link GridDirection} for grid-adjacent slot directions (UP/DOWN/LEFT/RIGHT),
 * not {@link net.minecraft.core.Direction} (which is for world-facing block directions).
 * <p>
 * The {@code shiftHeld} flag indicates whether the player held shift, allowing the server
 * to cycle modes in reverse order. This makes shift-click reverse cycling universal
 * for all modules — no per-module shift detection needed.
 */
public record ServerboundToggleModuleDirectionPacket(int slotIndex, GridDirection direction, BlockPos blockPos, boolean shiftHeld) implements CustomPacketPayload {
    public static final Type<ServerboundToggleModuleDirectionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "toggle_module_direction")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundToggleModuleDirectionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ServerboundToggleModuleDirectionPacket::slotIndex,
            ByteBufCodecs.INT.map(
                    o -> GridDirection.VALUES[o],
                    GridDirection::ordinal
            ),
            ServerboundToggleModuleDirectionPacket::direction,
            BlockPos.STREAM_CODEC,
            ServerboundToggleModuleDirectionPacket::blockPos,
            StreamCodec.of((buf, b) -> buf.writeBoolean(b), buf -> buf.readBoolean()),
            ServerboundToggleModuleDirectionPacket::shiftHeld,
            ServerboundToggleModuleDirectionPacket::new
    );

    @Override
    public @NotNull Type<ServerboundToggleModuleDirectionPacket> type() {
        return TYPE;
    }

    public static void handle(ServerboundToggleModuleDirectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ModulePacketHandler.handleAtBlock(player, packet.blockPos(), packet.slotIndex(),
                    BaseModuleItem.class::isInstance,
                    stack -> {
                        BaseModuleItem module = (BaseModuleItem) stack.getItem();
                        MachineFrameworkBlockEntity blockEntity =
                                (MachineFrameworkBlockEntity) player.level().getBlockEntity(packet.blockPos());
                        module.toggleDirectionMode(stack, packet.direction(), blockEntity, packet.slotIndex(), packet.shiftHeld());
                    },
                    "toggle module direction");
        });
    }
}