package com.thextrunner.praeteritum_futurum.network;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.redstone.RedstoneCircuit;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.RedstoneModuleItem;
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
 * Packet sent when the player configures a redstone component in Config Mode.
 * <p>
 * Flow: Player clicks a component in Config Mode → client sends this packet
 * → server performs the configuration action (toggle lever, rotate component, etc.).
 */
public record ServerboundRedstoneConfigPacket(int moduleSlotIndex, int gridSlotIndex, int action,
                                              int signalSourceStrength, BlockPos blockPos) implements CustomPacketPayload {
    
    // Action types
    public static final int ACTION_TOGGLE_LEVER = 0;
    public static final int ACTION_ROTATE_COMPONENT = 1;
    public static final int ACTION_ROTATE_TORCH = 2;
    public static final int ACTION_CYCLE_LEVER_ATTACHMENT = 3;
    public static final int ACTION_TOGGLE_COMPARATOR_MODE = 4;
    public static final int ACTION_CYCLE_REPEATER_DELAY = 5;
    public static final int ACTION_PRESS_BUTTON = 6;
    public static final int ACTION_CYCLE_BUTTON_ATTACHMENT = 7;
    public static final int ACTION_SET_SIGNAL_SOURCE_STRENGTH = 8;

    public ServerboundRedstoneConfigPacket(int moduleSlotIndex, int gridSlotIndex, int action, BlockPos blockPos) {
        this(moduleSlotIndex, gridSlotIndex, action, -1, blockPos);
    }
    
    public static final Type<ServerboundRedstoneConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "redstone_config")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundRedstoneConfigPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ServerboundRedstoneConfigPacket::moduleSlotIndex,
            ByteBufCodecs.INT,
            ServerboundRedstoneConfigPacket::gridSlotIndex,
            ByteBufCodecs.INT,
            ServerboundRedstoneConfigPacket::action,
            ByteBufCodecs.INT,
            ServerboundRedstoneConfigPacket::signalSourceStrength,
            BlockPos.STREAM_CODEC,
            ServerboundRedstoneConfigPacket::blockPos,
            ServerboundRedstoneConfigPacket::new
    );

    public boolean isValid() {
        return moduleSlotIndex >= 0 && moduleSlotIndex < MachineFrameworkBlockEntity.GRID_SIZE
                && gridSlotIndex >= 0 && gridSlotIndex < RedstoneCircuit.GRID_SIZE
                && isKnownAction(action)
                && (action == ACTION_SET_SIGNAL_SOURCE_STRENGTH
                        ? signalSourceStrength >= 0 && signalSourceStrength <= 15
                        : signalSourceStrength == -1);
    }

    private static boolean isKnownAction(int action) {
        return switch (action) {
            case ACTION_TOGGLE_LEVER,
                 ACTION_ROTATE_COMPONENT,
                 ACTION_ROTATE_TORCH,
                 ACTION_CYCLE_LEVER_ATTACHMENT,
                 ACTION_TOGGLE_COMPARATOR_MODE,
                 ACTION_CYCLE_REPEATER_DELAY,
                 ACTION_PRESS_BUTTON,
                 ACTION_CYCLE_BUTTON_ATTACHMENT,
                 ACTION_SET_SIGNAL_SOURCE_STRENGTH -> true;
            default -> false;
        };
    }

    @Override
    public @NotNull Type<ServerboundRedstoneConfigPacket> type() {
        return TYPE;
    }

    public static void handle(ServerboundRedstoneConfigPacket packet, IPayloadContext context) {
        if (!packet.isValid()) return;

        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ModulePacketHandler.handleAtBlock(player, packet.blockPos(), packet.moduleSlotIndex(),
                    RedstoneModuleItem.class::isInstance,
                    stack -> {
                        RedstoneCircuit circuit = RedstoneCircuit.fromStack(stack, player.registryAccess());
                        int action = packet.action();
                        int slot = packet.gridSlotIndex();
                        
                        switch (action) {
                            case ACTION_TOGGLE_LEVER -> circuit.toggleLever(slot);
                            case ACTION_ROTATE_COMPONENT -> circuit.rotateComponent(slot);
                            case ACTION_ROTATE_TORCH -> circuit.rotateTorch(slot);
                            case ACTION_CYCLE_LEVER_ATTACHMENT -> circuit.cycleLeverAttachment(slot);
                            case ACTION_TOGGLE_COMPARATOR_MODE -> circuit.toggleComparatorMode(slot);
                            case ACTION_CYCLE_REPEATER_DELAY -> circuit.cycleRepeaterDelay(slot);
                            case ACTION_PRESS_BUTTON -> circuit.pressButton(slot, player.level().getGameTime());
                            case ACTION_CYCLE_BUTTON_ATTACHMENT -> circuit.cycleButtonAttachment(slot);
                            case ACTION_SET_SIGNAL_SOURCE_STRENGTH ->
                                    circuit.setSignalSourceStrength(slot, packet.signalSourceStrength());
                        }
                        
                        // Save updated circuit NBT to the module stack
                        circuit.toStack(stack);
                    },
                    "configure redstone component"
            );
            
            // Recompute the whole machine so neighboring modules immediately reflect this change.
            if (player.level().getBlockEntity(packet.blockPos()) instanceof MachineFrameworkBlockEntity machineBe) {
                RedstoneModuleItem.recomputeMachineCircuits(player.level(), packet.blockPos(), machineBe);
            }
        });
    }
}
