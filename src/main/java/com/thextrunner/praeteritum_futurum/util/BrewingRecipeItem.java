package com.thextrunner.praeteritum_futurum.util;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

import java.util.HashMap;
import java.util.Map;

/**
 * Hardcoded vanilla brewing recipes for Minecraft 1.21.1.
 * <p>
 * This exists because NeoForge's {@code PotionBrewing} API is broken in 1.21.1:
 * {@code mix()} returns the ingredient item instead of the brewed potion,
 * and {@code hasMix()} may return incorrect results.
 * See: <a href="https://github.com/Creators-of-Create/Create/issues/9732">Create #9732</a>
 * <p>
 * In 1.21.1, splash and lingering potions are not separate potion types - they are
 * the same potion stored in different items (potion, splash_potion, lingering_potion).
 * This recipe map only tracks the potion type transformation; the item type is handled
 * separately in the BrewingModuleItem.
 * <p>
 * Third-party mods adding custom potions should create their own module via the API
 * rather than expecting these hardcoded recipes to support mod-added content.
 */
public class BrewingRecipeItem {

    private static final Map<BrewingKey, Holder<Potion>> RECIPES = new HashMap<>();

    static {
        // === Base potions ===
        put(Potions.WATER, Items.NETHER_WART, Potions.AWKWARD);

        // === Effect potions from Awkward base ===
        put(Potions.AWKWARD, Items.GOLDEN_CARROT, Potions.NIGHT_VISION);
        put(Potions.NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.INVISIBILITY);

        put(Potions.AWKWARD, Items.RABBIT_FOOT, Potions.LEAPING);
        put(Potions.LEAPING, Items.GLOWSTONE_DUST, Potions.STRONG_LEAPING);

        put(Potions.AWKWARD, Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE);

        put(Potions.AWKWARD, Items.SUGAR, Potions.SWIFTNESS);
        put(Potions.SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);

        put(Potions.AWKWARD, Items.PUFFERFISH, Potions.WATER_BREATHING);

        put(Potions.AWKWARD, Items.GLISTERING_MELON_SLICE, Potions.HEALING);
        put(Potions.HEALING, Items.GLOWSTONE_DUST, Potions.STRONG_HEALING);
        put(Potions.HEALING, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
        put(Potions.STRONG_HEALING, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);

        put(Potions.AWKWARD, Items.SPIDER_EYE, Potions.POISON);
        put(Potions.POISON, Items.GLOWSTONE_DUST, Potions.STRONG_POISON);

        put(Potions.AWKWARD, Items.GHAST_TEAR, Potions.REGENERATION);
        put(Potions.REGENERATION, Items.GLOWSTONE_DUST, Potions.STRONG_REGENERATION);

        put(Potions.AWKWARD, Items.BLAZE_POWDER, Potions.STRENGTH);
        put(Potions.STRENGTH, Items.GLOWSTONE_DUST, Potions.STRONG_STRENGTH);
        put(Potions.STRENGTH, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);

        put(Potions.AWKWARD, Items.TURTLE_HELMET, Potions.TURTLE_MASTER);
        put(Potions.AWKWARD, Items.PHANTOM_MEMBRANE, Potions.SLOW_FALLING);

        // === Trial Chamber potions (1.21) - no extended/strong variants ===
        put(Potions.AWKWARD, Items.COBWEB, Potions.WEAVING);
        put(Potions.AWKWARD, Items.SLIME_BLOCK, Potions.OOZING);
        put(Potions.AWKWARD, Items.BREEZE_ROD, Potions.WIND_CHARGED);
        put(Potions.AWKWARD, Items.STONE, Potions.INFESTED);

        // === Duration extensions (Redstone) ===
        put(Potions.NIGHT_VISION, Items.REDSTONE, Potions.LONG_NIGHT_VISION);
        put(Potions.INVISIBILITY, Items.REDSTONE, Potions.LONG_INVISIBILITY);
        put(Potions.LEAPING, Items.REDSTONE, Potions.LONG_LEAPING);
        put(Potions.FIRE_RESISTANCE, Items.REDSTONE, Potions.LONG_FIRE_RESISTANCE);
        put(Potions.SWIFTNESS, Items.REDSTONE, Potions.LONG_SWIFTNESS);
        put(Potions.SLOWNESS, Items.REDSTONE, Potions.LONG_SLOWNESS);
        put(Potions.POISON, Items.REDSTONE, Potions.LONG_POISON);
        put(Potions.REGENERATION, Items.REDSTONE, Potions.LONG_REGENERATION);
        put(Potions.TURTLE_MASTER, Items.REDSTONE, Potions.LONG_TURTLE_MASTER);
        put(Potions.SLOW_FALLING, Items.REDSTONE, Potions.LONG_SLOW_FALLING);
        put(Potions.WATER_BREATHING, Items.REDSTONE, Potions.LONG_WATER_BREATHING);

        // === Corrupted potions (Fermented Spider Eye) ===
        put(Potions.AWKWARD, Items.FERMENTED_SPIDER_EYE, Potions.MUNDANE);
        put(Potions.WATER, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
        put(Potions.LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.THICK);
        put(Potions.FIRE_RESISTANCE, Items.FERMENTED_SPIDER_EYE, Potions.THICK);
        put(Potions.WATER_BREATHING, Items.FERMENTED_SPIDER_EYE, Potions.THICK);
        put(Potions.REGENERATION, Items.FERMENTED_SPIDER_EYE, Potions.THICK);
        put(Potions.SLOW_FALLING, Items.FERMENTED_SPIDER_EYE, Potions.THICK);
    }

    private static void put(Holder<Potion> input, Item ingredient, Holder<Potion> output) {
        RECIPES.put(new BrewingKey(input, ingredient), output);
    }

    /**
     * Check if a brewing recipe exists for the given input potion and ingredient.
     */
    public static boolean hasRecipe(Holder<Potion> inputPotion, ItemStack ingredient) {
        return RECIPES.containsKey(new BrewingKey(inputPotion, ingredient.getItem()));
    }

    /**
     * Get the output potion for the given input potion and ingredient.
     * Returns null if no recipe is found.
     */
    public static Holder<Potion> getOutput(Holder<Potion> inputPotion, ItemStack ingredient) {
        return RECIPES.get(new BrewingKey(inputPotion, ingredient.getItem()));
    }

    /**
     * Check if the ingredient converts a regular potion to a splash potion.
     */
    public static boolean isSplashConversion(ItemStack ingredient) {
        return ingredient.is(Items.GUNPOWDER);
    }

    /**
     * Check if the ingredient converts a splash potion to a lingering potion.
     */
    public static boolean isLingeringConversion(ItemStack ingredient) {
        return ingredient.is(Items.DRAGON_BREATH);
    }

    /**
     * Simple key for the recipe map: input potion + ingredient item.
     */
    private static class BrewingKey {
        private final Holder<Potion> potion;
        private final Item ingredient;
        private final int hashCode;

        BrewingKey(Holder<Potion> potion, Item ingredient) {
            this.potion = potion;
            this.ingredient = ingredient;
            this.hashCode = System.identityHashCode(potion.value()) ^ ingredient.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BrewingKey other)) return false;
            return this.potion.value() == other.potion.value() && this.ingredient == other.ingredient;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
