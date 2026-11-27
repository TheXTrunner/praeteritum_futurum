package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.api.module.ITextInputModule;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
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
 * Generic packet sent when the player sets text on a module implementing {@link ITextInputModule}.
 * <p>
 * Flow: Player types text in the inline text input panel (in MachineInfoScreen) → client sends this packet
 * → server calls ITextInputModule.setText() to store the value in the module's NBT data.
 * <p>
 * This packet is used by any module implementing ITextInputModule, making it reusable
 * for third-party modders.
 */
public record ServerboundSetModuleTextPacket(String text, int slotIndex, BlockPos blockPos) implements CustomPacketPayload {
    public static final Type<ServerboundSetModuleTextPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("praeteritum_futurum", "set_module_text"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetModuleTextPacket> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, ServerboundSetModuleTextPacket::text,
            net.minecraft.network.codec.ByteBufCodecs.INT, ServerboundSetModuleTextPacket::slotIndex,
            BlockPos.STREAM_CODEC, ServerboundSetModuleTextPacket::blockPos,
            ServerboundSetModuleTextPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundSetModuleTextPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ModulePacketHandler.handleAtBlock(player, packet.blockPos(), packet.slotIndex,
                    ITextInputModule.class::isInstance,
                    stack -> {
                        ITextInputModule textModule = (ITextInputModule) stack.getItem();
                        // Validate text length against the module's maximum allowed length
                        if (packet.text().length() > textModule.getMaxTextLength()) {
                            PraeteritumFuturumMod.LOGGER.warn("Player {} sent text exceeding max length ({}/{}) for module at {}",
                                    player.getName().getString(), packet.text().length(), textModule.getMaxTextLength(), packet.blockPos());
                            return;
                        }
                        textModule.setText(stack, packet.text());
                    },
                    "set module text");
            // Refresh the block state so clients see updated text
            if (player.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity blockEntity) {
                player.level().sendBlockUpdated(packet.blockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            }
        });
    }
}
