package com.thextrunner.praeteritum_futurum;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> SALVAGE_BLOCKS = tag("salvage_blocks");

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> MACHINE_MODULES = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "machine_modules")
        );

        public static final TagKey<Item> CONFIGURABLE_MODULES = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "configurable_modules")
        );

        public static final TagKey<Item> PROCESSING_MODULES = TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "processing_modules")
        );
    }

    public static class Structures {
        public static final TagKey<Structure> RUINED_OUTPOST = TagKey.create(
                Registries.STRUCTURE,
                ResourceLocation.fromNamespaceAndPath(PraeteritumFuturumMod.MODID, "ruined_outpost")
        );

        // Ready for whenever you add your next structure!
    }
}