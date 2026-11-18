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
 *
 *   <li>{@link com.thextrunner.praeteritum_futurum.api.module.IItemHandlerModule} —
 *       For modules that expose an item handler to IO modules for item transfer.
 *       Implement this so adjacent IO modules can pull items into and push items out of
 *       your module's slot, without requiring an Inventory module intermediary.
 *       Example: JukeboxModuleItem.</li>
 *
 *   <li>{@link com.thextrunner.praeteritum_futurum.api.module.CyclingMode} —
 *       Generic interface for mode enums that support forward and backward cycling.
 *       Implement this on your mode enum to enable shift-click reverse mode cycling
 *       on direction buttons. All built-in mode enums implement this: SlotMode,
 *       JukeboxMode, InventoryMode, IoMode, and FaceMode.</li>
 * </ul>
 *
 * <h2>Inline Overlay System</h2>
 * <p>
 * Modules can render custom UI overlays directly on the machine configuration screen.
 * {@link com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem} provides the overlay infrastructure:
 * </p>
 * <p>
 * <b>Overlay lifecycle:</b> Override {@code opensInlineOverlay()} to return {@code true} to enable the pipeline.
 * Then implement {@code isInsideOverlay()} (clickable area), {@code handleCustomClick()} (click handling),
 * and {@code renderCustomOverlay()} (rendering). The close button is detected by {@code isCloseButton()} —
 * the default checks the standard position above the center slot.
 * </p>
 * <p>
 * <b>Center slot management:</b> The center slot can serve different purposes. Override these hooks:
 * </p>
 * <ul>
 *   <li>{@code usesCenterSlot()} — returns {@code false} by default. When {@code false}, the overlay renders
 *       a dark hatch lid to indicate the slot has no function. When {@code true}, the module controls rendering.</li>
 *   <li>{@code allowsCenterSlotInteraction()} — returns {@code false} by default. When {@code true}, clicks on
 *       the center slot pass through to normal inventory interaction while the overlay is open.</li>
 *   <li>{@code hasConfigSlot()} — for ghost config items stored in NBT (e.g., Stonecutter's target item).</li>
 *   <li>{@code hasInteractableInventorySlot()} — opt-in hook for player-accessible inventory slots in the machine UI.</li>
 *   <li>{@code hidesSlotItemInMachineScreen()} — returns {@code false} by default. When {@code true}, the machine
 *       screen hides the slot item so the module's overlay fully covers the slot without the item showing through
 *       (e.g., Jukebox Module hides the disc so the progress bar overlay is visible).</li>
 * </ul>
 * <p>
 * <b>UI drawing helpers:</b> To avoid duplicating rendering code, {@code BaseModuleItem} provides:
 * </p>
 * <ul>
 *   <li>{@code drawCloseButton()} — standard 16×16 close button with raised border; position is controlled by the slot coordinates passed</li>
 *   <li>{@code drawDarkHatchLid()} — dark gray panel for unused center slots</li>
 * </ul>
 * <p>
 * {@link com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem} additionally provides:
 * </p>
 * <ul>
 *   <li>{@code drawDirectionButtons()} — draws all 4 direction buttons with border awareness</li>
 *   <li>{@code drawProcessingButton()} / {@code drawProcessingButtonWithCheck()} — single direction button rendering</li>
 * </ul>
 * <p>
 * <b>Click detection helpers:</b> {@code BaseModuleItem} provides {@code getClickedDirection()} (border-aware
 * direction button hit detection) and {@code isInsideButton()} (static 16×16 button hit test).
 * </p>
 * <p>
 * All modules inherit {@code getOverlaySlotIndex()} and {@code setOverlaySlotIndex()} from {@code BaseModuleItem}
 * for border-aware button rendering. The overlay renderer ({@code MachineInfoScreen}) sets the slot index
 * generically — no hardcoded instanceof checks needed.
 * </p>
 *
 * <h2>Interactable Inventory Slot</h2>
 * <p>
 * By default, modules that implement {@link com.thextrunner.praeteritum_futurum.api.module.IItemHandlerModule}
 * cannot have their inventory slots interacted with by the player in the machine UI — items can only be
 * transferred via IO modules or automation. To allow player interaction, a module must opt in by overriding
 * {@code hasInteractableInventorySlot()} in {@link com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem}
 * to return {@code true}.
 * </p>
 * <p>
 * {@code MachineFrameworkBlockEntity.hasItemHandlerModule()} checks both the {@code IItemHandlerModule} interface
 * and the {@code hasInteractableInventorySlot()} hook. Only if both pass does {@code ModuleAwareSlotItemHandler}
 * allow the player to pick up and place items in that module's slot.
 * </p>
 * <p>
 * Override {@code hasInteractableInventorySlot()} to return {@code true} for modules that need a player-accessible
 * slot (e.g., Jukebox Module's disc slot). The default is {@code false} to prevent accidental item manipulation.
 * </p>
 *
 * <h2>Module Removal Guard</h2>
 * <p>
 * Modules can prevent themselves from being removed via the wrench/config menu while in an active state.
 * {@link com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem} provides the {@code mayPickupModule()}
 * hook that defaults to {@code true}. The {@code ModuleConfigSlotItemHandler} checks both the inventory slot
 * emptiness and this hook — both must pass for the module to be removable.
 * </p>
 * <p>
 * Override {@code mayPickupModule()} to return {@code false} while the module is busy (e.g., playing a song,
 * mid-operation). Example: {@code JukeboxModuleItem} returns {@code false} while a song is playing,
 * locking the module in place until playback stops.
 * </p>
 *
 * <h2>Direction Config Validation</h2>
 * <p>
 * Modules that use {@link com.thextrunner.praeteritum_futurum.block.GridDirection}-based configuration
 * (e.g., pointing to an adjacent Inventory or IO module) can automatically reset stale configs when the
 * adjacent slot becomes a border or holds a disallowed module type. This prevents broken configs when
 * players rearrange modules.
 * </p>
 * <p>
 * To enable it, override three hooks in {@link com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem}
 * and call {@code validateAndSyncDirectionConfig()} at the start of your click handler:
 * </p>
 * <ul>
 *   <li>{@code isDirectionConfigured(ItemStack, GridDirection)} — checks if a direction has an active config</li>
 *   <li>{@code resetDirection(ItemStack, GridDirection, MachineFrameworkBlockEntity, int)} — client-side reset</li>
 *   <li>{@code resetDirectionMode(ItemStack, GridDirection, MachineFrameworkBlockEntity, int)} — server-side reset</li>
 * </ul>
 * <p>
 * The {@code validateAndSyncDirectionConfig()} helper combines validation and server sync in one call.
 * See {@link com.thextrunner.praeteritum_futurum.item.custom.EjectionModuleItem} for an example.
 * </p>
 *
 * <h2>Shift-Click Reverse Mode Cycling</h2>
 * <p>
 * All module direction buttons support shift-click to cycle modes in reverse order.
 * {@link com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem} provides the {@code isShiftHeld()}
 * helper that wraps {@code Screen.hasShiftDown()} for reliable GUI-context shift detection — all subclasses
 * call this instead of duplicating the call. Never use {@code player.isShiftKeyDown()} in GUI contexts,
 * as it doesn't reflect keyboard state during mouse events.
 * </p>
 * <p>
 * The shift state is sent to the server via the packet, so the server handles the cycling direction.
 * Each mode enum implements the {@code CyclingMode} interface, which requires both {@code next()} and
 * {@code previous()} methods. The {@code toggleDirectionMode()} hook receives a {@code shiftHeld} parameter
 * that subclasses use to choose the cycling direction.
 * </p>
 * <p>
 * To enable it for your custom module, implement {@code CyclingMode} on your mode enum:
 * </p>
 * <pre>{@code
 * public enum MyMode implements CyclingMode<MyMode> {
 *     DISABLED, INPUT, OUTPUT;
 *
 *     @Override public MyMode next() {
 *         return values()[(ordinal() + 1) % values().length];
 *     }
 *
 *     @Override public MyMode previous() {
 *         return values()[(ordinal() - 1 + values().length) % values().length];
 *     }
 * }
 * }</pre>
 * <p>
 * Then in your {@code toggleDirectionMode()} override, use the {@code shiftHeld} parameter:
 * </p>
 * <pre>{@code
 * @Override
 * public void toggleDirectionMode(ItemStack stack, GridDirection direction,
 *                                  MachineFrameworkBlockEntity be, int slotIndex, boolean shiftHeld) {
 *     MyMode current = getMyMode(stack, direction);
 *     MyMode next = shiftHeld ? current.previous() : current.next();
 *     setMyMode(stack, direction, next);
 * }
 * }</pre>
 * <p>
 * All built-in modules support this: Processing, Jukebox, Ejection, and IO modules.
 * </p>
 *
 * <h2>NBT Helpers</h2>
 * <p>
 * {@link com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem} provides three static helpers
 * for reading and writing module CustomData NBT, available to all module types:
 * </p>
 * <ul>
 *   <li>{@code getCustomTag(stack, key)} — reads a sub-compound from CustomData NBT (returns empty tag if missing)</li>
 *   <li>{@code modifyCustomData(stack, key, action)} — modifies a sub-compound with direct component replacement
 *       for immediate client-side UI updates</li>
 *   <li>{@code updateStackSize(stack, hasConfig)} — sets max stack size to 1 when configured, or removes
 *       the component when unconfigured</li>
 * </ul>
 * <p>
 * These were previously only in {@code LogisticsModuleItem} but are now in {@code BaseModuleItem} so all
 * modules can use them without duplication.
 * </p>
 *
 * <h2>Module Object Storage &amp; Cleanup</h2>
 * <p>
 * Modules sometimes need to persist objects across ticks (e.g., song players, timers, connection state)
 * and clean them up when the machine is destroyed. {@code MachineFrameworkBlockEntity} provides two
 * generic systems so modules manage their own lifecycle without the block entity knowing about
 * module-specific state.
 * </p>
 * <p>
 * <b>Module object storage:</b> Call {@code be.setModuleObject(slotIndex, object)} to store an arbitrary
 * object keyed by module slot index. Retrieve it with {@code be.getModuleObject(slotIndex)}. The block
 * entity does not interpret these objects — they belong entirely to the module. When the machine is
 * destroyed, all stored objects are cleared.
 * </p>
 * <p>
 * <b>Module cleanup:</b> Call {@code be.registerModuleCleanup(slotIndex, runnable)} to register a cleanup
 * action executed when the machine block is destroyed. The block entity calls {@code runAllModuleCleanups()}
 * from {@code onRemove()}, executing all registered runnables and clearing both the cleanup map and the
 * module object map. Cleanup errors are silently ignored to avoid crashing on machine destruction.
 * </p>
 * <p>
 * Example: {@code JukeboxModuleItem} stores a {@code JukeboxSongPlayer} instance via {@code setModuleObject()}
 * so it persists across ticks, allowing the song to be stopped when the disc is removed or the machine
 * is destroyed.
 * </p>
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
 *   <li>{@code JukeboxModuleItem} — Uses IItemHandlerModule to receive discs from IO modules</li>
 * </ul>
 */
package com.thextrunner.praeteritum_futurum.api.module;
