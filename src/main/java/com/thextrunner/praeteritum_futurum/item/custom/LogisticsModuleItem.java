package com.thextrunner.praeteritum_futurum.item.custom;

import com.thextrunner.praeteritum_futurum.api.module.IConfigurableModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Base class for logistics modules (Filter, IO, and future modules like liquid piping,
 * redstone logic, etc.). Extending this class gives you face-mode configuration
 * and the {@link IConfigurableModule} interface for custom config screens.
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

    protected static void updateStackSize(ItemStack stack, boolean hasConfiguration) {
        if (hasConfiguration) {
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        } else {
            stack.remove(DataComponents.MAX_STACK_SIZE);
        }
    }

    protected static CompoundTag getCustomTag(ItemStack stack, String key) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(key)) {
                return tag.getCompound(key);
            }
        }
        return new CompoundTag();
    }

    protected static void modifyCustomData(ItemStack stack, String key, Consumer<CompoundTag> action) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag subTag = tag.contains(key) ? tag.getCompound(key) : new CompoundTag();
            action.accept(subTag);
            tag.put(key, subTag);
        });
    }
}