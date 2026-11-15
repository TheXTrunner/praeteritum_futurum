package com.thextrunner.praeteritum_futurum.worldgen;

import com.mojang.serialization.MapCodec;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom structure placement types for the mod using NeoForge's DeferredRegister.
 * <p>
 * Structure placements control how structures are distributed across the world.
 * Vanilla provides {@code minecraft:random_spread} and {@code minecraft:concentric_rings},
 * but these have fixed spacing values defined in JSON. To make spacing configurable
 * through the mod's config screen, we need a custom placement type that reads
 * the config value at runtime.
 * <p>
 * Uses {@link DeferredRegister} which handles registration timing automatically,
 * ensuring the placement type is registered before the registry freezes.
 */
public class ModStructurePlacements {

    /**
     * Deferred register for structure placement types.
     * <p>
     * This handles registering our custom placement type at the correct time
     * during mod initialization, before the STRUCTURE_PLACEMENT registry freezes.
     */
    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, PraeteritumFuturumMod.MODID);

    /**
     * The registered config-driven random spread placement type.
     * <p>
     * This placement type reads the mod's config spawn rate multiplier at runtime
     * to adjust structure spacing, allowing players to control structure frequency
     * from the config screen.
     */
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<ConfigDrivenRandomSpreadPlacement>>
            CONFIG_DRIVEN_RANDOM_SPREAD = STRUCTURE_PLACEMENT_TYPES.register(
                    "config_driven_random_spread",
                    () -> explicitStructureTypeTyping(ConfigDrivenRandomSpreadPlacement.CODEC));

    /**
     * Helper method to explicitly type the StructurePlacementType lambda.
     * <p>
     * Some IDEs cannot resolve the typing correctly with a double lambda (() -> () -> codec),
     * so this method explicitly states the return type for proper DeferredRegister handling.
     *
     * @param codec The MapCodec for the structure placement type
     * @param <T>   The structure placement class type
     * @return A properly typed StructurePlacementType instance
     */
    private static <T extends StructurePlacement> StructurePlacementType<T> explicitStructureTypeTyping(MapCodec<T> codec) {
        return () -> codec;
    }

    /**
     * Registers the structure placement types DeferredRegister with the mod event bus.
     *
     * @param modEventBus The mod's event bus
     */
    public static void register(IEventBus modEventBus) {
        STRUCTURE_PLACEMENT_TYPES.register(modEventBus);
        PraeteritumFuturumMod.LOGGER.info("Registered custom structure placement type: praeteritum_futurum:config_driven_random_spread");
    }
}
