package com.thextrunner.praeteritum_futurum.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thextrunner.praeteritum_futurum.Config;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Custom structure placement that applies a config-driven spawn rate multiplier.
 * <p>
 * This extends {@link RandomSpreadStructurePlacement} so that vanilla's
 * {@code ChunkGenerator.findNearestMapStructure} method recognizes it as a
 * random_spread placement — enabling both {@code /locate structure} and compass
 * items to find structures placed with this type.
 * <p>
 * Since {@code RandomSpreadStructurePlacement.getSpacing()} and {@code getSeparation()}
 * are final in NeoForge 1.21.1 and cannot be overridden, we override
 * {@link #isPlacementChunk(ChunkGeneratorStructureState, int, int)} and
 * {@link #getPotentialStructureChunk(long, int, int)} instead to apply the config
 * multiplier during structure generation.
 * <p>
 * How it works:
 * <ol>
 *   <li>The JSON defines base spacing (e.g., 34 chunks) just like vanilla random_spread</li>
 *   <li>When checking if a structure should generate at a given chunk position, the
 *       effective spacing is calculated as: {@code baseSpacing / spawnRateMultiplier}</li>
 *   <li>A multiplier of 0.0 effectively disables spawning (spacing → 999999)</li>
 * </ol>
 * <p>
 * The config value is read at runtime when the world is being generated, so changing
 * the config affects new worlds. Existing worlds are unaffected because structure
 * placement is determined at world creation time.
 * <p>
 * Registered as {@code praeteritum_futurum:config_driven_random_spread} in the
 * structure_placement_type datapack registry.
 */
public class ConfigDrivenRandomSpreadPlacement extends RandomSpreadStructurePlacement {

    /**
     * Codec for deserializing this placement from JSON.
     * <p>
     * Uses the same fields as vanilla's random_spread: {@code spacing},
     * {@code separation}, {@code salt}, and optionally {@code spread_type},
     * plus the shared placement fields (locate_offset, frequency, etc.).
     * Example:
     * <pre>
     * {
     *   "type": "praeteritum_futurum:config_driven_random_spread",
     *   "spacing": 34,
     *   "separation": 12,
     *   "salt": 48291048
     * }
     * </pre>
     */
    public static final MapCodec<ConfigDrivenRandomSpreadPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
                Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO)
                        .forGetter(ConfigDrivenRandomSpreadPlacement::locateOffset),
                StructurePlacement.FrequencyReductionMethod.CODEC.optionalFieldOf("frequency_reduction_method",
                        StructurePlacement.FrequencyReductionMethod.DEFAULT)
                        .forGetter(ConfigDrivenRandomSpreadPlacement::frequencyReductionMethod),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F)
                        .forGetter(ConfigDrivenRandomSpreadPlacement::frequency),
                Codec.intRange(0, 2147483647).fieldOf("salt").forGetter(ConfigDrivenRandomSpreadPlacement::getSalt),
                StructurePlacement.ExclusionZone.CODEC.optionalFieldOf("exclusion_zone")
                        .forGetter(ConfigDrivenRandomSpreadPlacement::exclusionZone),
                Codec.intRange(0, 4096).fieldOf("spacing").forGetter(ConfigDrivenRandomSpreadPlacement::getBaseSpacing),
                Codec.intRange(0, 4096).fieldOf("separation").forGetter(ConfigDrivenRandomSpreadPlacement::getBaseSeparation),
                RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR)
                        .forGetter(ConfigDrivenRandomSpreadPlacement::getSpreadType)
        ).apply(instance, ConfigDrivenRandomSpreadPlacement::new);
    });

    private final int baseSpacing;
    private final int baseSeparation;
    private final int salt;
    private final RandomSpreadType spreadType;

    /**
     * Constructs a config-driven random spread placement from JSON-deserialized values.
     *
     * @param locateOffset           Vanilla locate offset (from placementCodec)
     * @param frequencyReductionMethod Frequency reduction method (from placementCodec)
     * @param frequency              Frequency (from placementCodec)
     * @param salt                   Random seed salt for structure placement
     * @param exclusionZone          Exclusion zone (from placementCodec)
     * @param baseSpacing            Base spacing in chunks (before config multiplier is applied)
     * @param baseSeparation         Minimum separation between structures (in chunks)
     * @param spreadType             How spacing scales with distance from world center (LINEAR or TRIANGULAR)
     */
    private ConfigDrivenRandomSpreadPlacement(
            Vec3i locateOffset,
            FrequencyReductionMethod frequencyReductionMethod,
            float frequency,
            int salt,
            Optional<ExclusionZone> exclusionZone,
            int baseSpacing,
            int baseSeparation,
            RandomSpreadType spreadType) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone,
                baseSpacing, baseSeparation, spreadType);
        this.baseSpacing = baseSpacing;
        this.baseSeparation = baseSeparation;
        this.salt = salt;
        this.spreadType = spreadType;
    }

    /**
     * Simple constructor for programmatic creation (no codec fields).
     */
    public ConfigDrivenRandomSpreadPlacement(int baseSpacing, int baseSeparation, RandomSpreadType spreadType, int salt) {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, salt, Optional.empty(),
                baseSpacing, baseSeparation, spreadType);
        this.baseSpacing = baseSpacing;
        this.baseSeparation = baseSeparation;
        this.salt = salt;
        this.spreadType = spreadType;
    }

    public int getBaseSpacing() {
        return baseSpacing;
    }

    public int getBaseSeparation() {
        return baseSeparation;
    }

    public int getSalt() {
        return salt;
    }

    public RandomSpreadType getSpreadType() {
        return spreadType;
    }

    /**
     * Calculates the effective spacing by applying the config spawn rate multiplier.
     * <p>
     * Formula: {@code effectiveSpacing = baseSpacing / spawnRateMultiplier}
     * <p>
     * Examples (with baseSpacing = 34):
     * <ul>
     *   <li>spawnRate = 1.0 → spacing 34 (default)</li>
     *   <li>spawnRate = 2.0 → spacing 17 (twice as many)</li>
     *   <li>spawnRate = 0.5 → spacing 68 (half as many)</li>
     *   <li>spawnRate = 0.0 → spacing 999999 (effectively disabled)</li>
     * </ul>
     *
     * @return The effective spacing in chunks, or 999999 if spawning is disabled
     */
    private int getEffectiveSpacing() {
        double rate = Config.getStructureSpawnRate();
        if (rate <= 0.0) {
            return 999999;
        }
        return Math.max(2, (int) Math.round(baseSpacing / rate));
    }

    /**
     * Calculates the effective separation to ensure spacing > separation.
     */
    private int getEffectiveSeparation() {
        int spacing = getEffectiveSpacing();
        return Math.min(baseSeparation, spacing - 1);
    }

    /**
     * Computes the potential structure chunk position using config-adjusted spacing.
     * <p>
     * This overrides the parent method to use {@link #getEffectiveSpacing()} instead
     * of the fixed base spacing, allowing the config to control structure density.
     */
    public ChunkPos getPotentialStructureChunk(long seed, int sourceX, int sourceZ) {
        int spacing = getEffectiveSpacing();
        int separation = getEffectiveSeparation();
        int spacedGridX = Math.floorDiv(sourceX, spacing);
        int spacedGridZ = Math.floorDiv(sourceZ, spacing);
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(seed, spacedGridX, spacedGridZ, this.salt);
        int limit = spacing - separation;
        if (limit < 1) limit = 1;
        int spreadX = this.spreadType.evaluate(random, limit);
        int spreadZ = this.spreadType.evaluate(random, limit);
        return new ChunkPos(spacedGridX * spacing + spreadX, spacedGridZ * spacing + spreadZ);
    }

    /**
     * Determines whether the given chunk is a placement chunk for this structure.
     * <p>
     * Uses config-adjusted spacing via {@link #getPotentialStructureChunk(long, int, int)}.
     */
    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        ChunkPos chunkPos = this.getPotentialStructureChunk(state.getLevelSeed(), x, z);
        return chunkPos.x == x && chunkPos.z == z;
    }

    /**
     * Returns the registered placement type for this custom placement.
     */
    @Override
    public @NotNull StructurePlacementType<?> type() {
        return ModStructurePlacements.CONFIG_DRIVEN_RANDOM_SPREAD.get();
    }
}
