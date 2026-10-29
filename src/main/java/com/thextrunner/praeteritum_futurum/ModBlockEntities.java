package com.thextrunner.praeteritum_futurum;

import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PraeteritumFuturumMod.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<MachineFrameworkBlockEntity>> MACHINE_FRAMEWORK_BE =
            BLOCK_ENTITIES.register("machine_framework_be", () ->
                    BlockEntityType.Builder.of(
                            MachineFrameworkBlockEntity::new,
                            ModBlocks.MACHINE_FRAMEWORK.get()
                    ).build(null)
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}