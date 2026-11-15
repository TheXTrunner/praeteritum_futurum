/**
 * Module API for the Praeteritum Futurum mod.
 * <p>
 * This package defines the core interfaces and data models that enable extensible module design
 * for the Machine Framework block. Each module type implements one or more of
 * these interfaces to provide its functionality.
 * </p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link com.thextrunner.praeteritum_futurum.api.module.IConfigurableModule} —
 *       Base interface for modules that can be configured by the player.
 *       Implement this to define how your module opens its configuration screen.</li>
 *
 *   <li>{@link com.thextrunner.praeteritum_futurum.api.module.IModuleMenuProvider} —
 *       For modules that use standard NeoForge container menus (full-screen GUIs).
 *       Implement this if your module needs a dedicated inventory screen.</li>
 *
 *   <li>{@link com.thextrunner.praeteritum_futurum.api.module.ITextInputModule} —
 *       For modules that configure their behavior through text input.
 *       Implement this to get a free inline text input panel (overlay) with Save/Clear buttons
 *       that appears on top of the machine config screen. The module defines the label, hint,
 *       and max length. No custom screen code is needed — MachineInfoScreen handles the panel
 *       automatically when it detects an ITextInputModule in a config slot. Example: AnvilModuleItem.</li>
 *
 *   <li>{@link com.thextrunner.praeteritum_futurum.api.module.ICustomScreenProvider} —
 *       For modules that render custom inline overlays directly on the machine UI.
 *       Implement this for specialized client-side rendering.</li>
 * </ul>
 *
 * <h2>Redstone Circuit API</h2>
 * <p>
 * The following classes provide a programmable redstone logic system for creating
 * custom circuit-based modules (like the built-in RedstoneModuleItem).
 * </p>
 * <ul>
 *   <li>{@link com.thextrunner.praeteritum_futurum.api.module.RedstoneComponent} —
 *       Enum defining the four redstone component types: Wire, Repeater, Comparator, and Torch.
 *       Each component has a display name, color, and signal behavior.</li>
 *
 *   <li>{@link com.thextrunner.praeteritum_futurum.api.module.RedstoneCircuit} —
 *       Data model for a 6×9 (54-slot) redstone circuit grid. Handles component placement,
 *       rotation, wall attachments, signal propagation, and NBT serialization.
 *       Use this to create custom logic circuits inside your modules.</li>
 * </ul>
 * <p>
 * To use the redstone circuit API in a custom module:
 * </p>
 * <ol>
 *   <li>Store a {@code RedstoneCircuit} instance in your module's {@code CustomData} component</li>
 *   <li>Call {@code RedstoneCircuit.fromStack()} to load the circuit from an item stack</li>
 *   <li>Call {@code circuit.propagateSignals(inputSignals)} each tick to evaluate the circuit</li>
 *   <li>Call {@code circuit.toStack()} to save the updated circuit state</li>
 * </ol>
 *
 * <h2>Creating a New Module</h2>
 * <ol>
 *   <li>Extend {@link com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem}</li>
 *   <li>Implement the relevant interfaces from this package</li>
 *   <li>Register the item in {@link com.thextrunner.praeteritum_futurum.item.ModItems}</li>
 *   <li>Add the item to the {@link com.thextrunner.praeteritum_futurum.ModTags.Items#MACHINE_MODULES} tag</li>
 * </ol>
 *
 * <h2>Examples</h2>
 * <ul>
 *   <li>{@code IoModuleItem} — Uses inline overlay for face configuration</li>
 *   <li>{@code ProcessingModuleItem} — Uses inline overlay for slot direction config</li>
 *   <li>{@code FilterModuleItem} — Uses GenericModuleConfigMenu for filter item slots</li>
 *   <li>{@code AnvilModuleItem} — Uses ITextInputModule for custom name input</li>
 *   <li>{@code RedstoneModuleItem} — Uses RedstoneCircuit for programmable logic circuits</li>
 * </ul>
 */
package com.thextrunner.praeteritum_futurum.api.module;
