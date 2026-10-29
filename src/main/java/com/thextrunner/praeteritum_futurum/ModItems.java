package com.thextrunner.praeteritum_futurum;
//again not sure what this does exactly

import com.thextrunner.praeteritum_futurum.item.custom.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
//more libraries more functions or something!

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PraeteritumFuturumMod.MODID);
    //this does... something

    // and this registers the item itself
    public static final DeferredItem<BlockItem> SALVAGE_FRAMING_ITEM =
            ITEMS.registerSimpleBlockItem("salvage_framing", ModBlocks.SALVAGE_FRAMING);
    //wait does that mean I have to do this for every single item I add to this mod????
    public static final DeferredItem<Item> SALVAGE_SCRAP =
            ITEMS.registerSimpleItem("salvage_scrap",
                    new Item.Properties());
    public static final DeferredItem<Item> SALVAGE_CUTTER = ITEMS.registerItem(
            "salvage_cutter",
            properties -> new SalvageCutterItem(properties.durability(250))
    );
    public static final DeferredItem<Item> REFINED_SCRAP = ITEMS.registerSimpleItem(
            "refined_scrap",
            new Item.Properties().rarity(Rarity.COMMON)
    );
    public static final DeferredItem<BlockItem> ADVANCED_SALVAGE_ITEM =
            ITEMS.registerSimpleBlockItem("advanced_salvage", ModBlocks.ADVANCED_SALVAGE);
    //seems like I do need to add one of these lines for every new block/item
    public static final DeferredItem<Item> STRANGE_ARTIFACT = ITEMS.registerSimpleItem(
            "strange_artifact",
            new Item.Properties()
                    .rarity(Rarity.RARE)
                    .component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
            //new Item.Properties().fireResistant()
            // AI recommended me to use .fireResistant() but I have a better question
            // what else can I add here as a property?
            //Turns out a lot actually
    );
    // The item form of the machine framework so it can be held and placed
    public static final DeferredItem<BlockItem> MACHINE_FRAMEWORK_ITEM =
            ITEMS.registerSimpleBlockItem("machine_framework",
                    ModBlocks.MACHINE_FRAMEWORK
            );

    public static final DeferredItem<Item> YOOL =
            ITEMS.registerSimpleItem("yool",
                    new Item.Properties().stacksTo(1)
            );

    public static final DeferredItem<InventoryModuleItem> INVENTORY_MODULE =
            ITEMS.registerItem("inventory_module", InventoryModuleItem::new);

    public static final DeferredItem<IoModuleItem> IO_MODULE =
            ITEMS.registerItem("io_module", IoModuleItem::new);

    public static final DeferredItem<FilterModuleItem> FILTER_MODULE =
            ITEMS.registerItem("filter_module", FilterModuleItem::new);

    public static final DeferredItem<SmeltingModuleItem> SMELTING_MODULE =
            ITEMS.registerItem("smelting_module", SmeltingModuleItem::new);

    public static final DeferredItem<StonecutterModuleItem> STONECUTTER_MODULE =
            ITEMS.registerItem("stonecutter_module", StonecutterModuleItem::new);

    public static final DeferredItem<ComposterModuleItem> COMPOSTER_MODULE =
            ITEMS.registerItem("composter_module", ComposterModuleItem::new);


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}