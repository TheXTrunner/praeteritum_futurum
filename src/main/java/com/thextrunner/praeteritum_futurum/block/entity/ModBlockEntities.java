package com.thextrunner.praeteritum_futurum.block.entity;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registers all Block Entity Types for the mod.
 * <p>
 * A Block Entity (BE) is the data container attached to a block. While a Block defines
 * how something looks and behaves, a Block Entity stores dynamic data — like inventory
 * contents, custom NBT, or processing state.
 * <p>
 * The Machine Framework block requires a Block Entity because it stores module slots,
 * inventory items, and IO face configuration. Plain blocks (like Salvage Framing) don't
 * need one.
 * <p>
 * The @SuppressWarnings("DataFlowIssue") annotation is required because the IDE can't
 * statically verify that .build(null) is safe — but it is, as long as the block type
 * is valid (which it is, since we pass ModBlocks.MACHINE_FRAMEWORK.get()).
 */
public class ModBlockEntities {
    // DeferredRegister for block entity types, tied to our mod ID
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PraeteritumFuturumMod.MODID);

    /**
     * Machine Framework Block Entity — stores the machine's internal state:
     * module slots, inventory items, IO face modes, and processing data.
     * Linked to ModBlocks.MACHINE_FRAMEWORK so only that block spawns this BE type.
     */
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<MachineFrameworkBlockEntity>> MACHINE_FRAMEWORK_BE =
            BLOCK_ENTITIES.register("machine_framework_be", () ->
                    BlockEntityType.Builder.of(
                            MachineFrameworkBlockEntity::new,    // Constructor to instantiate the BE
                            ModBlocks.MACHINE_FRAMEWORK.get()   // The block this BE is attached to
                    ).build(null)                                // null tag is valid here; suppresses IDE warning
            );

    /**
     * Registers this DeferredRegister with the mod's event bus.
     * Called from PraeteritumFuturumMod's constructor.
     */
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}