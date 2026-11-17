package com.thextrunner.praeteritum_futurum.item;

import com.thextrunner.praeteritum_futurum.PraeteritumFuturumMod;
import com.thextrunner.praeteritum_futurum.block.ModBlocks;
import com.thextrunner.praeteritum_futurum.item.custom.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registration class for all custom items in the mod.
 * <p>
 * There are three ways to register items in NeoForge, each used in different scenarios:
 * <p>
 * 1. registerSimpleItem() — For basic items with no custom behavior. Just passes properties
 *    to the vanilla Item constructor. Example: salvage_scrap, refined_scrap.
 * <p>
 * 2. registerSimpleBlockItem() — For the pickable item form of a block. Every placeable block
 *    needs a corresponding BlockItem so it can exist in the player's inventory. This links
 *    the item to its block via the DeferredBlock handle. Example: salvage_framing_item.
 * <p>
 * 3. registerItem() — For items with custom behavior (custom class). Takes a lambda that
 *    receives Item.Properties and returns a new instance of the custom item class.
 *    Example: anomaly_compass, salvage_cutter, all modules.
 * <p>
 * Each item is given a registry name (the string passed to ITEMS.register) which becomes
 * its resource location: praeteritum_futurum:item_name.
 */
public class ModItems {
    // The DeferredRegister holds all our item definitions, tied to our mod ID.
    // It's registered to the mod event bus in PraeteritumFuturumMod's constructor.
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PraeteritumFuturumMod.MODID);

    /**
     * Anomaly Compass — a tool that points toward nearby anomaly structures.
     * Uses a custom item class (AnomalyCompassItem) for direction-finding logic.
     * Stacks to 1 so each compass is unique.
     */
    public static final DeferredItem<Item> ANOMALY_COMPASS = ITEMS.registerItem("anomaly_compass",
            properties -> new AnomalyCompassItem(properties.stacksTo(1))
    );

    /**
     * Salvage Framing BlockItem — the pickable form of the Salvage Framing block.
     * registerSimpleBlockItem creates a BlockItem that, when used, places the linked block.
     */
    public static final DeferredItem<BlockItem> SALVAGE_FRAMING_ITEM =
            ITEMS.registerSimpleBlockItem("salvage_framing",
                    ModBlocks.SALVAGE_FRAMING
            );

    /**
     * Salvage Scrap — a basic material item obtained from mining salvage blocks.
     * Used as a crafting component for other items in the mod.
     */
    public static final DeferredItem<Item> SALVAGE_SCRAP =
            ITEMS.registerSimpleItem("salvage_scrap",
                    new Item.Properties()
            );

    /**
     * Salvage Cutter — a custom tool with 250 durability points.
     * Uses SalvageCutterItem for custom tool behavior (mining speed, attack damage, etc.).
     */
    public static final DeferredItem<Item> SALVAGE_CUTTER =
            ITEMS.registerItem("salvage_cutter",
                    properties -> new SalvageCutterItem(properties.durability(250))
            );

    /**
     * Refined Scrap — an upgraded material made from Salvage Scrap.
     * Marked as UNCOMMON rarity so it displays with a yellow tint in the inventory.
     */
    public static final DeferredItem<Item> REFINED_SCRAP = ITEMS.registerSimpleItem(
            "refined_scrap",
            new Item.Properties()
                    .rarity(Rarity.UNCOMMON)
    );

    /**
     * Advanced Salvage BlockItem — the pickable form of the Advanced Salvage block.
     */
    public static final DeferredItem<BlockItem> ADVANCED_SALVAGE_ITEM =
            ITEMS.registerSimpleBlockItem("advanced_salvage",
                    ModBlocks.ADVANCED_SALVAGE);

    /**
     * Strange Artifact — a rare item found in structure loot.
     * Marked as RARE rarity (gold tint) and has an enchantment glint overlay
     * to make it visually stand out in the inventory.
     * <p>
     * Note: .fireResistant() is available as a property (used by Netherite items)
     * but is not applied here. Other useful Item.Properties methods include:
     * - stacksTo(int): max stack size
     * - durability(int): for tools/weapons
     * - food(FoodProperties): for edible items
     * - fireResistant(): immune to fire/lava damage
     * - craftRemainder(Item): what's left after crafting (e.g., buckets)
     */
    public static final DeferredItem<Item> STRANGE_ARTIFACT =
            ITEMS.registerSimpleItem("strange_artifact",
                    new Item.Properties()
                            .rarity(Rarity.RARE)
                            .component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
            );

    /**
     * Machine Framework BlockItem — the pickable form of the Machine Framework block.
     * This block has a custom renderer (BlockEntityWithoutLevelRenderer) registered
     * in ClientModSetup, so the item in-hand shows the full 3D model with emissive layers.
     */
    public static final DeferredItem<BlockItem> MACHINE_FRAMEWORK_ITEM =
            ITEMS.registerSimpleBlockItem("machine_framework",
                    ModBlocks.MACHINE_FRAMEWORK
            );

    /**
     * Yool — a special tool. Uses YoolItem for custom right-click behavior with the machine framework
     * Stacks to 1.
     */
    public static final DeferredItem<Item> YOOL =
            ITEMS.registerItem("yool",
                    properties -> new YoolItem(properties.stacksTo(1))
            );

    // --- Machine Modules ---
    // Modules are the core gameplay mechanic: players place them into a Machine Framework
    // to give it functionality. Each module type extends BaseModuleItem and implements
    // specific behavior (storage, IO, filtering, processing).
    // They use method reference syntax (ClassName::new) which is shorthand for
    // () -> new ClassName() — the registerItem lambda receives Properties and returns the item.

    /**
     * Inventory Module — provides additional storage slots inside the Machine Framework.
     * This is the foundational module: without it, a machine has no internal storage.
     */
    public static final DeferredItem<InventoryModuleItem> INVENTORY_MODULE =
            ITEMS.registerItem("inventory_module", InventoryModuleItem::new);

    /**
     * IO Module — controls item input/output faces on the Machine Framework.
     * Players can configure which sides accept items (input) and which eject them (output).
     * Uses custom overlay rendering to show the configured face modes on the item model.
     */
    public static final DeferredItem<IoModuleItem> IO_MODULE =
            ITEMS.registerItem("io_module", IoModuleItem::new);

    /**
     * Filter Module — allows or blocks specific items from passing through the machine.
     * Players set a filter item to whitelist or blacklist that item type.
     */
    public static final DeferredItem<FilterModuleItem> FILTER_MODULE =
            ITEMS.registerItem("filter_module", FilterModuleItem::new);

    /**
     * Smelting Module — adds furnace-like smelting capability to the machine.
     * Processes items using fuel, similar to a vanilla furnace but integrated into
     * the machine's processing pipeline.
     */
    public static final DeferredItem<SmeltingModuleItem> SMELTING_MODULE =
            ITEMS.registerItem("smelting_module", SmeltingModuleItem::new);

    /**
     * Blast Module — adds blast furnace smelting capability to the machine.
     * Smelts ores and metals faster than the Smelting Module (100 ticks instead of 200).
     */
    public static final DeferredItem<BlastModuleItem> BLAST_MODULE =
            ITEMS.registerItem("blast_module", BlastModuleItem::new);

    /**
     * Smoker Module — adds smoker cooking capability to the machine.
     * Cooks food items faster than the Smelting Module (100 ticks instead of 200).
     */
    public static final DeferredItem<SmokerModuleItem> SMOKER_MODULE =
            ITEMS.registerItem("smoker_module", SmokerModuleItem::new);

    /**
     * Stonecutter Module — adds stonecutter processing to the machine.
     * Can cut stone, stone bricks, glass, and other stonecutter-compatible materials.
     */
    public static final DeferredItem<StonecutterModuleItem> STONECUTTER_MODULE =
            ITEMS.registerItem("stonecutter_module", StonecutterModuleItem::new);

    /**
     * Composter Module — adds composter processing to the machine.
     * Converts plant-based items into bone meal automatically.
     */
    public static final DeferredItem<ComposterModuleItem> COMPOSTER_MODULE =
            ITEMS.registerItem("composter_module", ComposterModuleItem::new);

    /**
     * Grindstone Module — adds grindstone functionality to the machine.
     * Can repair tools and remove enchantments from equipment, returning some experience.
     */
    public static final DeferredItem<GrindstoneModuleItem> GRINDSTONE_MODULE =
            ITEMS.registerItem("grindstone_module", GrindstoneModuleItem::new);

    /**
     * Cartography Module — adds cartography table functionality to the machine.
     * Can clone maps, scale maps, and create maps.
     */
    public static final DeferredItem<CartographyModuleItem> CARTOGRAPHY_MODULE =
            ITEMS.registerItem("cartography_module", CartographyModuleItem::new);

    /**
     * Anvil Module — adds anvil functionality to the machine.
     * Can repair items and combine enchanted items.
     */
    public static final DeferredItem<AnvilModuleItem> ANVIL_MODULE =
            ITEMS.registerItem("anvil_module", AnvilModuleItem::new);

    /**
     * Brewing Module — adds brewing stand functionality to the machine.
     * Can brew potions using Blaze Powder as fuel and various ingredients.
     */
    public static final DeferredItem<BrewingModuleItem> BREWING_MODULE =
            ITEMS.registerItem("brewing_module", BrewingModuleItem::new);

    /**
     * Ejection Module — adds dispenser/dropper functionality to the machine.
     * Can dispense items (arrows, snowballs, seeds, etc.) with smart dispenser behavior
     * or simply drop them as entity drops (dropper behavior).
     */
    public static final DeferredItem<EjectionModuleItem> EJECTION_MODULE =
            ITEMS.registerItem("ejection_module", EjectionModuleItem::new);

    /**
     * Jukebox Module — plays music discs held in its own inventory slot.
     * The module slot holds a single disc; placing a disc starts playback,
     * removing it stops. Adjacent IO modules auto-feed discs from external
     * inventories in queue or shuffle mode.
     */
     public static final DeferredItem<JukeboxModuleItem> JUKEBOX_MODULE =
             ITEMS.registerItem("jukebox_module", JukeboxModuleItem::new);

    /**
     * Blueprint — a utility item that copies the entire configuration of one Machine Framework
     * to another. Right-click a configured machine to save its setup, then right-click another
     * machine to apply the saved configuration using modules in inventory.
     * Stacks to 1 since each blueprint holds one config.
     */
    public static final DeferredItem<Item> BLUEPRINT =
            ITEMS.registerItem("blueprint",
                    properties -> new BlueprintItem(properties.stacksTo(1))
            );

    /**
     * Registers this DeferredRegister with the mod's event bus.
     * Called from PraeteritumFuturumMod's constructor. This is what actually tells
     * NeoForge to create and register all the items defined above.
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}