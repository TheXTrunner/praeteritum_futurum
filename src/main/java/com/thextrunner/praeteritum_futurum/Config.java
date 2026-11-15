package com.thextrunner.praeteritum_futurum;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod configuration system using NeoForge's ModConfigSpec API.
 * <p>
 * This class defines all user-editable settings for the mod. Each config value
 * is a typed ModConfigSpec.*Value (BooleanValue, IntValue, DoubleValue, etc.)
 * that NeoForge automatically serializes to a TOML config file at:
 * <code>.minecraft/config/praeteritum_futurum-common.toml</code>
 * <p>
 * How it works:
 * 1. BUILDER is a fluent API that chains .comment() and .define() calls
 * 2. Each .define() call creates a typed config value with a name and default
 * 3. .build() at the end creates the final ModConfigSpec object
 * 4. The spec is registered in PraeteritumFuturumMod's constructor
 * <p>
 * Config categories are created with BUILDER.push("category_name") and closed
 * with BUILDER.pop(). Values in the same category appear grouped in the TOML file.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ========================================================================
    // General Settings
    // ========================================================================
    public static final class General {
        static {
            BUILDER.translation("text.autoconfig.praeteritum_futurum.category.general")
                    .push("general");
        }

        /**
         * Whether the Yool wrench can rotate blocks.
         * <p>
         * When disabled, right-clicking with a Yool on directional blocks will have
         * no effect. This is useful for servers that want to prevent accidental
         * block rotation or for players who prefer manual rotation.
         */
        public static final ModConfigSpec.BooleanValue ENABLE_YOOL_ROTATION = BUILDER
                .comment(
                        "Enable the Yool wrench block rotation feature",
                        "When disabled, right-clicking with a Yool on directional blocks will do nothing"
                )
                .translation("text.autoconfig.praeteritum_futurum.option.general.enableYoolRotation")
                .define("enableYoolRotation", true);

        static {
            BUILDER.pop();
        }
    }

    // ========================================================================
    // Machine Settings
    // ========================================================================
    public static final class Machine {
        static {
            BUILDER.translation("text.autoconfig.praeteritum_futurum.category.machine")
                    .push("machine");
        }

        /**
         * Tick interval for IO module item transfers.
         * <p>
         * Controls how often IO modules attempt to pull/push items. A value of 1
         * means every tick. The default of 8 matches a hopper's 8-tick transfer cooldown
         * per adjacent inventory-module connection.
         * <p>
         * Higher values reduce server load but slow down item transfer rates.
         * For reference, Minecraft runs at 20 ticks per second, so:
         * - 1 = up to 20 transfer attempts/second
         * - 8 = up to 2.5 transfer attempts/second (hopper cadence; default)
         * - 20 = up to 1 transfer attempt/second
         */
        public static final ModConfigSpec.IntValue IO_TRANSFER_TICK_RATE = BUILDER
                .comment(
                        "How many ticks between IO module item transfer attempts",
                        "Higher values = slower transfers but less server load",
                        "Default: 8 (one transfer attempt every 8 ticks, like a hopper)",
                        "Range: 1-100"
                )
                .translation("text.autoconfig.praeteritum_futurum.option.machine.ioTransferTickRate")
                .defineInRange("ioTransferTickRate", 8, 1, 100);

        /**
         * Maximum repair cost threshold for the Anvil module.
         * <p>
         * In vanilla Minecraft, anvil operations with a repair cost above 40 are
         * marked as "Too Expensive" and cannot be performed. This config allows
         * the Anvil module to bypass that limit.
         * <p>
         * - 0 = no limit (default, allows any repair cost)
         * - 40 = vanilla behavior
         * - any positive value = custom threshold
         */
        public static final ModConfigSpec.IntValue ANVIL_MAX_REPAIR_COST = BUILDER
                .comment(
                        "Maximum repair cost for Anvil module operations",
                        "0 = no limit (default), 40 = vanilla behavior",
                        "Range: 0-100"
                )
                .translation("text.autoconfig.praeteritum_futurum.option.machine.anvilMaxRepairCost")
                .defineInRange("anvilMaxRepairCost", 0, 0, 100);

        /**
         * Tick interval for Redstone Module signal propagation.
         * <p>
         * Controls how often the redstone circuit evaluates and propagates signals.
         * A value of 1 means every tick. The default of 2 means every other tick.
         */
        public static final ModConfigSpec.IntValue REDSTONE_EVALUATION_TICK_RATE = BUILDER
                .comment(
                        "How many ticks between Redstone Module signal evaluations",
                        "Higher values = slower logic but less server load",
                        "Default: 2 (one evaluation every 2 ticks)",
                        "Range: 1-100"
                )
                .translation("text.autoconfig.praeteritum_futurum.option.machine.redstoneEvaluationTickRate")
                .defineInRange("redstoneEvaluationTickRate", 2, 1, 100);

        /**
         * Enable verbose debug logging for machine modules.
         * <p>
         * When enabled, modules will log detailed information about their operations
         * to the server console/log files. This includes IO module transfers and network
         * packet handling.
         * <p>
         * WARNING: This generates a large amount of log output and should only be
         * enabled for debugging purposes. Keep this disabled on production servers.
         */
        public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING = BUILDER
                .comment(
                        "Enable verbose debug logging for all machine modules",
                        "Generates a large amount of log output — only enable for debugging",
                        "Default: false (disabled)"
                )
                .translation("text.autoconfig.praeteritum_futurum.option.machine.enableDebugLogging")
                .define("enableDebugLogging", false);

        static {
            BUILDER.pop();
        }
    }

    // ========================================================================
    // World Generation Settings
    // ========================================================================
    public static final class WorldGen {
        static {
            BUILDER.translation("text.autoconfig.praeteritum_futurum.category.worldgen")
                    .push("worldgen");
        }

        /**
         * Multiplier for Ruined Outpost structure spawn rate.
         * <p>
         * This multiplier is intended to affect the spacing between structures in the world.
         * The base spacing is 34 chunks. The intended formula is:
         * <code>effectiveSpacing = baseSpacing / spawnRateMultiplier</code>
         * <p>
         * Examples:
         * - 1.0 = default spawn rate (spacing 34)
         * - 2.0 = twice as many structures (spacing 17)
         * - 0.5 = half as many structures (spacing 68)
         * - 0.0 = effectively disables structure spawning (spacing becomes very large)
         * <p>
         * Note: This config value is defined but not yet wired to runtime code.
         * NeoForge 1.21.1 requires a custom StructurePlacement type registered via
         * the datapack registry system to modify structure spacing at load time.
         * See README.md todo list for implementation details.
         */
        public static final ModConfigSpec.DoubleValue STRUCTURE_SPAWN_RATE = BUILDER
                .comment(
                        "Multiplier for Ruined Outpost structure spawn rate",
                        "1.0 = default, 2.0 = twice as many, 0.5 = half as many, 0.0 = disabled",
                        "Range: 0.0-10.0",
                        "This setting only works at world creation!"
                )
                .translation("text.autoconfig.praeteritum_futurum.option.worldgen.ruinedOutpostSpawnRate")
                .defineInRange("ruinedOutpostSpawnRate", 1.0, 0.0, 10.0);

        static {
            BUILDER.pop();
        }
    }

    /**
     * Force initialization of all inner config classes so their static blocks
     * populate the BUILDER before we call .build(). Without this, the inner
     * classes are never loaded (Java loads inner classes lazily on first access),
     * and the spec is built empty — resulting in a blank config screen.
     */
    @SuppressWarnings("unused")
    private static final void initConfigClasses() {
        // Accessing a static field forces the inner class to initialize
        Object ignore = General.ENABLE_YOOL_ROTATION;
        ignore = Machine.IO_TRANSFER_TICK_RATE;
        ignore = Machine.ANVIL_MAX_REPAIR_COST;
        ignore = Machine.REDSTONE_EVALUATION_TICK_RATE;
        ignore = Machine.ENABLE_DEBUG_LOGGING;
        ignore = WorldGen.STRUCTURE_SPAWN_RATE;
    }

    /**
     * The final built config spec — this is what gets registered with NeoForge.
     * Built after all inner class static blocks have run (ensured by initConfigClasses).
     */
    static final ModConfigSpec SPEC;
    static {
        initConfigClasses();
        SPEC = BUILDER.build();
    }

    // ========================================================================
    // Safe accessors — these return the default value if called before the spec
    // is built (e.g., during early world loading before mod construction completes).
    // This prevents NullPointerException crashes when block entities tick early.
    // ========================================================================

    /**
     * Safely gets the IO transfer tick rate config value.
     * Returns the config default value (8 ticks) if the config spec hasn't been built yet.
     */
    public static int getIoTransferTickRate() {
        try {
            return Machine.IO_TRANSFER_TICK_RATE.get();
        } catch (NullPointerException e) {
            // Spec not built yet — match the configured default of 8 ticks
            return 8;
        }
    }

    /**
     * Safely gets the Yool rotation enabled flag.
     * Returns the default value (true) if the config spec hasn't been built yet.
     */
    public static boolean isYoolRotationEnabled() {
        try {
            return General.ENABLE_YOOL_ROTATION.get();
        } catch (NullPointerException e) {
            // Spec not built yet — return the default value
            return true;
        }
    }

    /**
     * Safely gets the structure spawn rate multiplier.
     * Returns the default value (1.0) if the config spec hasn't been built yet.
     * Used by ConfigDrivenRandomSpreadPlacement to adjust structure spacing at runtime.
     */
    public static double getStructureSpawnRate() {
        try {
            return WorldGen.STRUCTURE_SPAWN_RATE.get();
        } catch (NullPointerException e) {
            // Spec not built yet — return the default value
            return 1.0;
        }
    }

    /**
     * Safely gets the Anvil module max repair cost config value.
     * Returns the default value (0 = no limit) if the config spec hasn't been built yet.
     */
    public static int getAnvilMaxRepairCost() {
        try {
            return Machine.ANVIL_MAX_REPAIR_COST.get();
        } catch (NullPointerException e) {
            // Spec not built yet — return the default value (no limit)
            return 0;
        }
    }

    /**
     * Safely gets the Redstone Module evaluation tick rate config value.
     * Returns the default value (2 ticks) if the config spec hasn't been built yet.
     */
    public static int getRedstoneEvaluationTickRate() {
        try {
            return Machine.REDSTONE_EVALUATION_TICK_RATE.get();
        } catch (NullPointerException e) {
            // Spec not built yet — return the default value
            return 2;
        }
    }

    /**
     * Safely gets the debug logging enabled flag.
     * Returns the default value (false) if the config spec hasn't been built yet.
     */
    public static boolean isDebugLoggingEnabled() {
        try {
            return Machine.ENABLE_DEBUG_LOGGING.get();
        } catch (NullPointerException e) {
            // Spec not built yet — return the default value (disabled)
            return false;
        }
    }
}
