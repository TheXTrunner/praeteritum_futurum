package com.thextrunner.praeteritum_futurum.item.custom;

/**
 * Inventory Module — provides storage slots inside the Machine Framework.
 * <p>
 * This is the foundational module: without it, a machine has no internal storage.
 * Each Inventory module makes its corresponding grid position's inventory slot
 * accessible for item storage and IO transfer.
 */
public class InventoryModuleItem extends BaseModuleItem {
    public InventoryModuleItem(Properties properties) {
        super(properties);
    }
}