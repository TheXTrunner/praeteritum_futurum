package com.thextrunner.praeteritum_futurum.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thextrunner.praeteritum_futurum.Config;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Global loot modifier that adds sand drops to Husks based on config values.
 * <p>
 * This modifier reads config values at runtime, so changes to the config file
 * take effect immediately without requiring a world reload. The config controls:
 * - Whether the drop is enabled
 * - The chance of dropping sand (0.0 to 1.0)
 * - The min/max count of sand dropped
 * <p>
 * The modifier is applied only to Husk entity loot tables, as specified by the
 * loot table ID condition in the JSON modifier file.
 */
public class HuskSandLootModifier extends LootModifier {

    public static final MapCodec<HuskSandLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, HuskSandLootModifier::new));

    public HuskSandLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull LootContext context) {
        // Only apply if enabled in config
        if (!Config.isHuskSandDropEnabled()) {
            return generatedLoot;
        }

        // Check if the target entity is a Husk
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity == null || entity.getType() != EntityType.HUSK) {
            return generatedLoot;
        }

        // Roll for the chance to drop sand
        double chance = Config.getHuskSandDropChance();
        if (chance <= 0.0 || Math.random() > chance) {
            return generatedLoot;
        }

        // Determine the count of sand to drop
        int min = Config.getHuskSandDropMin();
        int max = Config.getHuskSandDropMax();
        if (min > max) {
            // If min > max, just drop min amount
            generatedLoot.add(new ItemStack(Items.SAND, min));
        } else if (min == max) {
            generatedLoot.add(new ItemStack(Items.SAND, min));
        } else {
            int count = min + (int) (Math.random() * (max - min + 1));
            if (count > 0) {
                generatedLoot.add(new ItemStack(Items.SAND, count));
            }
        }

        return generatedLoot;
    }
}
