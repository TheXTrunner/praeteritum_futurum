package com.thextrunner.praeteritum_futurum.api.module;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for modules that configure their behavior through text input.
 * <p>
 * When a module implements this interface, clicking the module in the machine menu
 * opens a text input screen where the player can type a value. The text is stored
 * in the module's NBT data and can be read during processing.
 * <p>
 * This interface is designed for third-party modders who want to create modules
 * that accept text configuration, such as:
 * <ul>
 *   <li>Anvil modules that rename items to a custom name</li>
 *   <li>Cartography modules that set map names</li>
 *   <li>Custom modules that accept string parameters</li>
 * </ul>
 * <p>
 * To use this interface, implement it on your module item class and override
 * the methods to define the text field label, existing value, and how to
 * store/retrieve the text value.
 * <p>
 * Example usage:
 * <pre>
 * public class MyModuleItem extends BaseModuleItem implements ITextInputModule {
 *     &#64;Override
 *     public Component getTextInputLabel() {
 *         return Component.translatable("module.my_mod.text_input.label");
 *     }
 *
 *     &#64;Override
 *     public String getExistingText(ItemStack stack) {
 *         // Read stored text from module NBT
 *         return "";
 *     }
 *
 *     &#64;Override
 *     public void setText(ItemStack stack, String text) {
 *         // Store text in module NBT
 *     }
 * }
 * </pre>
 */
public interface ITextInputModule {

    /**
     * Returns the label displayed above the text input field.
     * <p>
     * This is typically a translatable component, e.g., "Set Item Name" or "Enter Pattern".
     *
     * @return The label component shown in the text input screen
     */
    Component getTextInputLabel();

    /**
     * Returns the hint text displayed in the text input field when empty.
     * <p>
     * Override this to provide placeholder text, e.g., "Enter name..." or "Type here...".
     *
     * @return The hint component, or null to use no hint
     */
    @Nullable
    default Component getTextInputHint() {
        return null;
    }

    /**
     * Returns the maximum length of text allowed in the input field.
     *
     * @return The maximum text length (default: 50)
     */
    default int getMaxTextLength() {
        return 50;
    }

    /**
     * Returns the currently stored text value for this module.
     * <p>
     * Called when opening the text input screen to pre-fill the text field.
     *
     * @param stack The module item stack
     * @return The existing text value, or empty string if none is set
     */
    String getExistingText(ItemStack stack);

    /**
     * Stores the new text value in the module's NBT data.
     * <p>
     * Called when the player saves the text input. The implementation should
     * persist the text in the module stack's CustomData component.
     * <p>
     * If the text is empty, the implementation should clear any previously stored value.
     *
     * @param stack The module item stack
     * @param text  The new text value (may be empty string to clear)
     */
    void setText(ItemStack stack, String text);
}
