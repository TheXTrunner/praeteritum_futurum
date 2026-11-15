package com.thextrunner.praeteritum_futurum.datagen;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Entry point for NeoForge's data generation system.
 * <p>
 * When you run {@code gradlew runData}, NeoForge fires a GatherDataEvent and this class
 * registers all the providers that generate JSON files (models, block states, languages,
 * tags, recipes). The generated files are written to {@code src/generated/resources/}.
 * <p>
 * Providers are split into two groups:
 * - Client: item models, block states, language files (only needed on the client)
 * - Server: block tags, item tags, recipes (needed on both sides)
 * <p>
 * Run with flags to control what's generated:
 * - {@code --client}: only client providers
 * - {@code --server}: only server providers
 * - (no flags): both client and server providers
 */
@EventBusSubscriber(modid = PraeteritumFuturumMod.MODID)
public class DataGenerators {

    /**
     * Called by NeoForge when runData is executed. Registers all data providers.
     * <p>
     * The lookupProvider gives access to registry data (block/item holders) needed
     * for tag providers. The existingFileHelper prevents model providers from
     * referencing assets that don't exist.
     */
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Client providers — generate JSON files only used by the client
        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput, "en_us"));

        // Server providers — generate JSON files used by both client and server
        ModBlockTagProvider blockTagsProvider = new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagsProvider);
        // Item tag provider depends on block tag provider for inherited tags
        generator.addProvider(event.includeServer(), new ModItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));
    }
}