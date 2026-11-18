package com.thextrunner.praeteritum_futurum.loot;

import com.mojang.serialization.MapCodec;
import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Registers custom global loot modifier serializers.
 * <p>
 * Uses the DeferredRegister pattern to register MapCodec instances with NeoForge's
 * GLOBAL_LOOT_MODIFIER_SERIALIZERS registry. Each codec tells NeoForge how to
 * deserialize a custom loot modifier type from JSON.
 */
public class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, PraeteritumFuturumMod.MODID);

    /**
     * The Husk sand drop loot modifier — adds configurable sand drops to Husk entity loot.
     * The modifier reads config values at runtime, so config changes take effect immediately.
     */
    public static final Supplier<MapCodec<HuskSandLootModifier>> HUSK_SAND =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("husk_sand", () -> HuskSandLootModifier.CODEC);
}
