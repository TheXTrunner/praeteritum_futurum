package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.api.module.IConfigurableModule;

/**
 * Base class for logistics modules (Filter, IO, and future modules like liquid piping,
 * redstone logic, etc.). Extending this class gives you face-mode configuration
 * and the {@link IConfigurableModule} interface for custom config screens.
 * <p>
 * Direction config validation is inherited from {@link BaseModuleItem}. Subclasses that use
 * GridDirection-based config should override {@link BaseModuleItem#isDirectionConfigured},
 * {@link BaseModuleItem#resetDirection}, and {@link BaseModuleItem#resetDirectionMode},
 * then call {@link BaseModuleItem#validateAndSyncDirectionConfig} in their click handler.
 * See {@link EjectionModuleItem} for an example.
 * <p>
 * NBT helpers ({@code getCustomTag}, {@code modifyCustomData}, {@code updateStackSize}) are
 * inherited from {@link BaseModuleItem} — no need to redefine them.
 * <p>
 * Third-party modders should extend this class for modules that manage item/liquid flow,
 * filtering, or other logistics-related functionality.
 */
public abstract class LogisticsModuleItem extends BaseModuleItem implements IConfigurableModule {
    /**
     * Constructs a LogisticsModuleItem with the given properties.
     *
     * @param properties The item properties (stack size, rarity, etc.)
     */
    public LogisticsModuleItem(Properties properties) {
        super(properties);
    }
}