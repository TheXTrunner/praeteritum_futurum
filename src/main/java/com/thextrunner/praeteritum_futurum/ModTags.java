package com.thextrunner.praeteritum_futurum;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Central definition of all mod tag keys.
 * <p>
 * Tags are used to group blocks, items, and structures so they can be referenced
 * collectively in recipes, data providers, and game logic. Instead of checking for
 * each individual item, code can check if an item belongs to a tag (e.g., "is this
 * a processing module?").
 * <p>
 * Tags are organized by registry type in nested classes: Blocks, Items, Structures.
 * Each tag key is a ResourceLocation: praeteritum_futurum:tag_name.
 */
public class ModTags {

    /**
     * Block tags — used in block tag providers, recipes, and block entity logic.
     */
    public static class Blocks {
        /** Blocks that can be salvaged for materials (Salvage Framing, Advanced Salvage). */
        public static final TagKey<Block> SALVAGE_BLOCKS = tag("salvage_blocks");

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, name));
        }
    }

    /**
     * Item tags — used in recipes, creative tab filtering, and module slot validation.
     * <p>
     * Hierarchy:
     * - machine_modules: all modules (parent tag)
     *   - logistics_modules: IO, Filter, Inventory modules
     *   - processing_modules: Smelting, Stonecutter, Composter, Grindstone modules
     *   - configurable_modules: modules that have a config screen
     * - blueprints: the Blueprint utility item
     */
    public static class Items {
        /** All machine modules — parent tag containing all module sub-tags. */
        public static final TagKey<Item> MACHINE_MODULES = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "machine_modules")
        );

        /** Logistics modules: Inventory, IO, and Filter modules that manage item flow. */
        public static final TagKey<Item> LOGISTICS_MODULES = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "logistics_modules")
        );

        /** Modules that provide a configuration screen (Filter, IO, Processing modules). */
        public static final TagKey<Item> CONFIGURABLE_MODULES = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "configurable_modules")
        );

        /** Processing modules: Smelting, Stonecutter, Composter, Grindstone. */
        public static final TagKey<Item> PROCESSING_MODULES = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "processing_modules")
        );

        /** The Blueprint utility item for copying machine configurations. */
        public static final TagKey<Item> BLUEPRINTS = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "blueprints")
        );
    }

    /**
     * Structure tags — used for world generation and structure-related features.
     */
    public static class Structures {
        /** The Ruined Outpost structure — the mod's primary structure. */
        public static final TagKey<Structure> RUINED_OUTPOST = TagKey.create(
                Registries.STRUCTURE,
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "ruined_outpost")
        );
        // More structures will be added as the mod expands
    }
}