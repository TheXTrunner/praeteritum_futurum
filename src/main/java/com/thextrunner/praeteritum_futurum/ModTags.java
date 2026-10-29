package com.thextrunner.praeteritum_futurum;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> SALVAGE_BLOCKS = tag("salvage_blocks");

        private static TagKey<Block> tag(String name) {//Value of parameter 'name' is always '"salvage_blocks"' UNTIL IT ISN'T I might add more tags in the future, it stays like this for now
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
}