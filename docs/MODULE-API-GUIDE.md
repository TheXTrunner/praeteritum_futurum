# Module API Guide for Third-Party Modders

[Documentation index](README.md) | [Project README](../README.md) | [Contributing guide](../CONTRIBUTING.md)

This guide walks through creating custom modules for the **Praeteritum Futurum** Machine Framework. Modules are items placed in its 27-slot grid, each providing specialized functionality (smelting, filtering, IO control, etc.). Start with dependency setup, then follow the base-class and complete-module examples. The guide targets third-party mod developers familiar with Java, Gradle, and NeoForge.

---

## Table of Contents

1. [Prerequisites & Dependency Setup](#1-prerequisites--dependency-setup)
2. [Base Classes](#2-base-classes)
3. [API Interfaces](#3-api-interfaces)
4. [Adjacent Module Detection](#4-adjacent-module-detection)
5. [Module Data Channels](#5-module-data-channels)
6. [Creating a Processing Module](#6-creating-a-processing-module)
7. [Creating a Logistics Module](#7-creating-a-logistics-module)
8. [Creating a Base Module](#8-creating-a-base-module)
9. [Registration](#9-registration)
10. [Textures](#10-textures)
11. [Data Tags](#11-data-tags)
12. [Network Packets](#12-network-packets)
13. [Example: Complete Module](#13-example-complete-module)
14. [Creating a Custom Brewing Module](#14-creating-a-custom-brewing-module)
15. [Inline Overlay System](#15-inline-overlay-system)
16. [Module Object Storage & Cleanup](#16-module-object-storage--cleanup)
17. [Module Implementation Checklist](#module-implementation-checklist)

---

## 1. Prerequisites & Dependency Setup

Before creating a module, your mod needs to depend on Praeteritum Futurum.

### Adding the Dependency in `build.gradle`

The project does not publish artifacts to a public Maven repository. For local development, place the mod JAR in the add-on project libs directory and declare it as a dependency:

```groovy
dependencies {
    compileOnly files("libs/praeteritum-futurum-<version>.jar")
    runtimeOnly files("libs/praeteritum-futurum-<version>.jar")
}
```

### Declaring the Dependency in `neoforge.mods.toml`

Tell NeoForge that your mod requires Praeteritum Futurum. Set the version range to the minimum release your add-on supports:

```toml
[[dependencies.your_mod_id]]
    modId="praeteritum_futurum"
    type="required"
    versionRange="[0.1.1,)"
    ordering="NONE"
    side="BOTH"
```

### What You Get

Once the dependency is set up, you can import and extend any of the public API classes:

- `com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem`
- `com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem`
- `com.thextrunner.praeteritum_futurum.item.custom.LogisticsModuleItem`
- `com.thextrunner.praeteritum_futurum.api.module.*` (module extension contracts and mode types)
- `com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity`
- `com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigMenu`
- `com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigScreen`

---

## 2. Base Classes

There are three base classes you can extend, depending on your module's purpose:

### `BaseModuleItem`
**Root class for all modules.** Use this if your module doesn't fit the processing or logistics patterns.

- Provides: overlay rendering, click handling, slot hiding, face modes, tick hooks, adjacent module detection, overlay slot index management, NBT helpers
- Constructor: `public BaseModuleItem(Properties properties)`
- Key methods to override:
  - `renderModuleOverlay()` - draws visual indicators on the module slot
  - `handleCustomClick()` - handles clicks on the module's overlay
  - `tickModule()` - dispatched by `MachineFrameworkBlockEntity` on server ticks; redstone-controlled modules can be skipped when their policy denies operation
  - `isInsideOverlay()` - defines the clickable area of the overlay
  - `opensInlineOverlay()` - returns `true` if the module uses inline overlays on the machine screen (default: `false`)
  - `isCloseButton()` - detects clicks on the overlay close button (default: no close button)
  - `toggleDirectionMode()` - server-side handler for direction-button clicks (see [Adjacent Module Detection](#4-adjacent-module-detection))
  - `getAllowedAdjacentModuleTypes()` - declares which adjacent module types this module cares about
- **Overlay slot index:** All modules inherit `getOverlaySlotIndex()` and `setOverlaySlotIndex()` from `BaseModuleItem`. The overlay renderer (e.g., `MachineInfoScreen`) sets the slot index before calling `renderCustomOverlay()`, enabling border-aware button rendering and adjacent-module detection without the screen needing to know about specific module types.
- **Module removal guard:** Override `mayPickupModule()` to return `false` while the module is in a state that should prevent removal (e.g., while a song is playing or an operation is in progress). The `ModuleConfigSlotItemHandler` checks both the inventory slot emptiness and this hook - both must pass for the module to be removable. Example: JukeboxModuleItem locks itself while a song is playing.
- **Interactable inventory slot:** Override `hasInteractableInventorySlot()` to return `true` so the player can pick up and place items in the module's slot via the machine UI. Defaults to `false` - items can only be transferred via IO modules or automation. Example: JukeboxModuleItem returns `true` so the player can manually load/remove music discs.
- **Item validation for interactable slots:** When `hasInteractableInventorySlot()` is `true`, also override `isItemValidForSlot(stack, blockEntity, slotIndex)` to return `true` for items the module accepts. The machine UI uses this to decide whether clicking the module with an item in hand should place the item or open the config overlay. This makes the click behavior consistent: holding a valid item always places it, while an empty hand or invalid item opens the config. Example: JukeboxModuleItem returns `true` for music discs.
- **Shift detection:** Call `isShiftHeld()` (inherited from `BaseModuleItem`) in your `handleCustomClick()` to detect whether the player held shift. This wraps `Screen.hasShiftDown()` for reliable GUI-context shift detection. Never use `player.isShiftKeyDown()` in GUI contexts.
- **NBT helpers:** Three static helpers are available on `BaseModuleItem` for all module types:
  - `getCustomTag(stack, key)` - reads a sub-compound from the module's CustomData NBT (returns empty tag if missing)
  - `modifyCustomData(stack, key, action)` - modifies a sub-compound in CustomData NBT with direct component replacement for immediate client-side UI updates
  - `updateStackSize(stack, hasConfig)` - sets max stack size to 1 when configured, or removes the component when unconfigured

#### Optional redstone control

`BaseModuleItem` includes an opt-in run-control hook. It does not affect a module unless `supportsRedstoneControl()` returns `true`. `ProcessingModuleItem` opts in by default, while a subclass can opt out; `EjectionModuleItem` opts in separately. The basic Filter, I/O, Inventory, Redstone, and Jukebox modules do not use this control.

- `getRedstoneControlSignal(blockEntity, slotIndex)` returns a signal from 0 to 15. It combines the strongest world input from I/O faces in `INPUT` or `OPEN` mode with the strongest facing-edge output from any immediately adjacent Redstone Module. It does not expose a particular circuit slot.
- `RedstoneControlMode.IGNORE`, `HIGH`, and `LOW` mean always allow, allow while powered, and allow while unpowered. The default stored policy is `IGNORE`.
- `getSupportedRedstoneControlModes()` declares which modes the module exposes. Override it for module-specific options, and use `setRedstoneControlMode()` or `cycleRedstoneControlMode()` to update the per-stack setting.
- `isRedstoneControlSatisfied(stack, blockEntity, slotIndex)` applies the selected policy. The machine uses this to gate the module's normal tick, not separate machine phases such as I/O item transfers.
- `PULSE` is available in the shared enum, but `allowsSignal()` only reports whether the current level is nonzero for this mode. A module that offers `PULSE` must track signal transitions and implement its own event handling; the Ejection Module is the built-in example.

The common configuration overlay shows a redstone-dust control button for opted-in modules. Click cycles supported policies forward; Shift-click cycles backward. This hook controls module behavior, not the internal redstone circuit solver.

### `ProcessingModuleItem` (extends `BaseModuleItem`)
**For modules that transform items over time.** Use this for smelting-like, brewing-like, crafting-like operations.

- Provides: slot direction configuration (U/D/L/R arrows), progress bar overlay, config slot support, IO slot resolution
- Constructor: `public ProcessingModuleItem(Properties properties)`
- **Must override:**
  - `getModuleConfigKey()` - returns the NBT key for storing slot mode data
  - `processModule()` - your core processing logic, called during the module tick when it is allowed to run; a redstone policy can pause that tick without discarding module state
- **Optional overrides:**
  - `getMaxInputs()` - max input slots (default: unlimited)
  - `getMaxOutputs()` - max output slots (default: unlimited)
  - `getInventorySlotLimit()` - restrict the max stack size for a specific inventory slot (default: 0 = no restriction); the Brewing Module uses this to limit ingredient slots to 1 item
  - `hasConfigSlot()` - returns `true` to show a center config button (default: `false`)
  - `acceptsConfigSlotItem()` - restricts what items can be placed in the config slot (default: accepts all)
  - `useDefaultConfigSlotClick()` - returns `false` to handle config slot clicks yourself (default: `true`)
  - `getSupportedModes()` - declares this module's local modes and direction-button cycle order (default: empty; override when using the inherited direction controls)
  - `getMaxForMode()` - returns the max slot count for a mode (default: delegates to `getMaxInputs()`/`getMaxOutputs()` for INPUT/OUTPUT roles; override for module-specific mode limits)
  - `getCycledMode()` - controls the slot mode cycle (usually inherited; override only for custom logic)
  - `getProgressPercent()` - returns 0.0-1.0 for the progress bar overlay
- **Module data channels:** Processing modules use `getModuleData(slot, channel)` and `setModuleData(slot, channel, value)` for per-tick state. See [Module Data Channels](#module-data-channels) below for the channel contract and NBT lifecycle rules.
- **NBT save/restore lifecycle:** The base class automatically saves and restores channel state when a module is picked up and placed back. See [NBT Save and Restore Lifecycle](#nbt-save-and-restore-lifecycle) for the rules and common pitfalls.

### `LogisticsModuleItem` (extends `BaseModuleItem`, implements `IConfigurableModule`)
**For modules that manage item flow, filtering, or logistics.** Use this for modules such as Filter and I/O. The built-in redstone circuit engine is internal and is not exposed as an extension API for custom grid components.

- Provides: `IConfigurableModule` interface for custom config screens
- Constructor: `public LogisticsModuleItem(Properties properties)`
- Inherits from `BaseModuleItem`: all overlay rendering, click handling, NBT helpers (`getCustomTag`, `modifyCustomData`, `updateStackSize`), direction config validation, and adjacent module detection
- **Must override:**
  - `openConfiguration()` - opens a server-side container menu for configuration

---

## 3. API Interfaces

All interfaces are in `com.thextrunner.praeteritum_futurum.api.module`.

### `IConfigurableModule`
Marker interface for modules with config screens. Implement this to define how your module opens its configuration.

```java
void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos)
```

### `IModuleMenuProvider`
For modules using `GenericModuleConfigMenu` with custom slots (like FilterModule's 27 filter slots).

```java
void addModuleSlots(Inventory playerInventory, ItemStack moduleStack, BlockPos blockPos, Consumer<Slot> slotAdder)
default void saveMenuData(GenericModuleConfigMenu menu, HolderLookup.Provider registries)
void handleMenuClick(GenericModuleConfigMenu menu, int slotId, int button, ClickType clickType, Player player)
ItemStack handleQuickMove(GenericModuleConfigMenu menu, Player player, int index)
```

`GenericModuleConfigMenu` calls `saveMenuData()` when it closes. Override this default no-op to persist custom slot contents or other menu-owned state; providers that persist every change immediately do not need to override it.

**Screen extensibility:** When creating a custom screen for your module, extend `GenericModuleConfigScreen` instead of `AbstractContainerScreen`. This gives you the standard back button, filter mode toggle, and background rendering for free. Then add your custom overlays in the `render()` method. The built-in `FilterConfigScreen` demonstrates this pattern: it extends `GenericModuleConfigScreen` and adds filter-specific rendering on top.

To associate a custom screen with your module, register a dedicated `MenuType` in `ModMenuTypes` and pass it to the `GenericModuleConfigMenu` constructor:

```java
// Register your menu type
public static final DeferredHolder<MenuType<?>, MenuType<GenericModuleConfigMenu>> MY_MODULE_CONFIG_MENU =
    MENUS.register("my_module_config_menu", () ->
        IMenuTypeExtension.create(GenericModuleConfigMenu::new));

// In your module's openConfiguration method
new GenericModuleConfigMenu(
    ModMenuTypes.MY_MODULE_CONFIG_MENU.get(),
    containerId, playerInventory, moduleStack, slotIndex, blockPos
);

// Register the screen
event.register(MY_MODULE_CONFIG_MENU.get(), MyModuleConfigScreen::new);
```

### `ITextInputModule`
For modules that need text input (like Anvil module renaming). Implement this to get a free inline text input panel that appears on top of the machine config screen.

```java
Component getTextInputLabel()           // e.g., Component.translatable("module.my_mod.label")
@Nullable Component getTextInputHint()  // e.g., Component.literal("Enter a name...") - default: null
int getMaxTextLength()                  // max characters allowed - default: 50
String getExistingText(ItemStack stack) // current stored text (for pre-filling)
void setText(ItemStack stack, String text)  // called when player saves text
```

**No custom screen code is needed.** `MachineInfoScreen` automatically provides the inline panel when it detects an `ITextInputModule` in a config slot. Just make sure `hasConfigSlot()` returns `true`.

### `ICustomScreenProvider`
For modules that provide custom container screens. Implement this to create a dedicated screen for your module's config menu. When a module implements this interface, the machine config screen will use the returned screen instance instead of the default `GenericModuleConfigScreen`.

```java
<T extends AbstractContainerMenu> AbstractContainerScreen<?> createScreen(T menu, Inventory playerInventory, Component title)
```

**How it works:** When the player opens a module's config screen, the framework checks if the module item implements `ICustomScreenProvider`. If it does, `createScreen()` is called to get the custom screen. If not, the default `GenericModuleConfigScreen` is used.

**Example use case:** The Redstone Module implements this interface to provide `RedstoneConfigScreen` - a circuit editor with a component palette and grid, completely different from the generic module config layout.

**To implement:** Have your module item implement `ICustomScreenProvider` and return your custom `AbstractContainerScreen` from `createScreen()`. Register your custom `MenuType` in `ModMenuTypes` and associate it with your screen class.

### `IItemHandlerModule`
For modules that expose an item handler to IO modules for direct item transfer. When a module implements this interface, adjacent IO modules can pull items into and push items out of the module's exposed item handler - without requiring an Inventory module intermediary.

```java
@Nullable
IItemHandlerModifiable getItemHandlerForSide(
    Level level,
    BlockPos pos,
    MachineFrameworkBlockEntity blockEntity,
    int slotIndex,
    ItemStack stack,
    Direction side
)
```

**How it works:** The machine framework calls `getItemHandlerForSide()` during IO transfer to get the target item handler. If the method returns `null`, the module is skipped for that transfer direction. The returned handler is used as the source or target of the transfer, depending on the IO module's face mode (INPUT/OUTPUT).

**Example use case:** The Jukebox Module implements this interface so its slot can accept music discs from external automation (pipes/hoppers). The module's slot holds a single disc; placing one starts playback, and removing it stops. The module also handles all disc extraction and return from connected chests via the IO module internally, so the IItemHandlerModule interface is primarily for external automation compatibility.

**To implement:** Have your module item implement `IItemHandlerModule` and return a non-null `IItemHandlerModifiable` from `getItemHandlerForSide()`. If you extend `BaseModuleItem`, the interface is already implemented - just override `getItemHandlerForSide()` to return your handler. For standalone items (not extending `BaseModuleItem`), implement the interface directly.

### `CyclingMode<T>`
Generic interface for mode enums that support forward and backward cycling. Implement this on your mode enum to enable shift-click reverse mode cycling on direction buttons.

```java
public interface CyclingMode<T> {
    T next();      // Returns the next mode in the cycle (forward order)
    T previous();  // Returns the previous mode in the cycle (reverse order)
}
```

**How it works:** When the player shift-clicks a direction button, the client sends the shift state to the server via the packet. Modes that use `CyclingMode<T>` can select `previous()` instead of `next()`. Processing modules do not use this interface for their local modes: `ProcessingModuleItem` cycles through the order returned by `getSupportedModes()`, in reverse when shift is held. Built-in jukebox, inventory, and IO mode enums implement this interface.

**To implement:** Add `implements CyclingMode<MyMode>` to a mode enum when your module handles cycling through `toggleDirectionMode()` and wants the interface's forward/reverse helpers. Processing modules should instead implement `ProcessingMode` on a module-local enum and declare its cycle in `getSupportedModes()`; see [Creating a Processing Module](#6-creating-a-processing-module).

### `ProcessingMode`

`ProcessingMode` is implemented by each processing module's own mode type; it is not a shared enum of every mode used by the framework. It provides the metadata and generic role that the shared processing base class needs:

- `getLabelChar()` returns the short label shown on the mode button.
- `getColor()` returns the mode button's ARGB color.
- `getDescription()` returns the mode-specific tooltip description.
- `getRole()` returns a shared role: `DISABLED`, `INPUT`, `OUTPUT`, or `OTHER`. Module-specific modes remain owned by their module and can use `OTHER` when they do not have generic input/output behavior.
- `getName()` defaults to the enum constant's name when implemented by an enum, or `toString()` otherwise; override it when a different persisted/display name is required.

The processing module declares which modes it supports and their cycle order through `getSupportedModes()`. The shared processing behavior applies only to the generic input/output roles; module-specific limits and behavior stay with the module that defines those modes.

---

## 4. Adjacent Module Detection

Configurable modules that use direction-based configuration (U/D/L/R buttons) can detect what type of module sits adjacent in each grid direction. This prevents users from configuring directions toward irrelevant module types and provides a consistent UX.

### GridDirection vs. World Direction

The mod uses two distinct direction systems:

- **`GridDirection`** - navigates the machine's 9-column by 3-row grid. Values: `UP`, `DOWN`, `LEFT`, `RIGHT`. All direction-button overlays, NBT config maps, and adjacent-module detection use `GridDirection`.
- **`Direction` (Minecraft)** - world-facing block directions (`NORTH`, `SOUTH`, `WEST`, `EAST`, `UP`, `DOWN`). Only the IO module uses these via `RelativeSide`.

Using `GridDirection` prevents bugs where `Direction.Plane.HORIZONTAL` (which includes `NORTH`/`SOUTH`) is mistakenly used for grid-adjacent slot lookup.

### How It Works

`BaseModuleItem` provides these shared methods:

- **`AdjacentModuleType` enum** - classifies modules into `NONE`, `INVENTORY`, `IO`, `FILTER`, `PROCESSING`, and `LOGISTICS`.
- **`getAdjacentSlotIndex(slotIndex, dir)`** - delegates to `GridDirection.getAdjacentSlotIndex()` to compute the adjacent slot, or -1 for grid borders.
- **`getAdjacentModuleType(blockEntity, slotIndex, dir)`** - returns the type of module sitting adjacent in a `GridDirection`.
- **`getClickedDirection(slotX, slotY, mx, my, slotIndex)`** - determines which direction button was clicked, with border awareness. Returns a `@Nullable GridDirection` (`null` if no button was clicked).

### Declaring Allowed Adjacent Types

Override `getAllowedAdjacentModuleTypes()` to declare which adjacent module types your module cares about:

```java
@Override
public List<AdjacentModuleType> getAllowedAdjacentModuleTypes() {
    return List.of(AdjacentModuleType.INVENTORY);
}
```

The base class uses this to:
1. Limit which direction buttons cycle modes - if no allowed module is adjacent, the button stays disabled.
2. Guide the user - only directions pointing to relevant modules can be configured.

### Server-Side Direction Toggle

Override `toggleDirectionMode()` to handle direction-button clicks on the server:

```java
@Override
public void toggleDirectionMode(ItemStack stack, GridDirection direction, MachineFrameworkBlockEntity blockEntity, int slotIndex, boolean shiftHeld) {
    AdjacentModuleType adjType = getAdjacentModuleType(blockEntity, slotIndex, direction);
    if (!isAdjacentModuleAllowed(adjType)) {
        return; // No relevant module adjacent - ignore
    }
    // Cycle your mode for this direction (use shiftHeld for reverse cycling)
    MyMode current = getMode(stack, direction);
    MyMode next = shiftHeld ? current.previous() : current.next();
    setMode(stack, direction, next);
}
```

The packet handler automatically delegates to this method - no custom packet registration needed.

### Direction Config Validation

When a player reconfigures a machine - for example, moving a module to a different slot or replacing an adjacent module with a border - previously configured directions may become stale. A direction that pointed to a valid adjacent module might now point to a border or a disallowed module type.

`BaseModuleItem` provides **automatic direction config validation** that detects and resets these stale configs. When the player opens the module overlay, any direction that is no longer valid is automatically reset to its disabled state.

**How it works:** The `validateDirectionConfig()` method iterates all grid directions and checks two conditions:
1. **Border check** - if the direction has no adjacent slot at all (grid border), the config is reset.
2. **Module type check** - if the adjacent slot exists but holds a disallowed module type, the config is reset.

**To enable it:** Override three hooks and call `validateAndSyncDirectionConfig()` at the start of your click handler:

```java
@Override
public boolean isDirectionConfigured(ItemStack stack, GridDirection dir) {
    // Return true if this direction has a non-default (non-disabled) config
    return getMode(stack, dir) != MyMode.DISABLED;
}

@Override
public void resetDirection(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
    // Client-side: Reset this direction's config to its default state
    setMode(stack, dir, MyMode.DISABLED);
}

@Override
public void resetDirectionMode(ItemStack stack, GridDirection dir, MachineFrameworkBlockEntity blockEntity, int slotIndex) {
    // Server-side: Reset this direction's config to its default state
    setMode(stack, dir, MyMode.DISABLED);
}

@Override
public void handleCustomClick(double mouseX, double mouseY, int slotX, int slotY, ItemStack stack,
                               MachineFrameworkBlockEntity blockEntity, int slotIndex, Minecraft minecraft) {
    // Validates config and syncs resets to server in one call
    validateAndSyncDirectionConfig(stack, blockEntity, slotIndex);

    // ... rest of your click handling
}
```

`validateAndSyncDirectionConfig()` is available on `BaseModuleItem`, so **all** module types inherit it - no need for separate shortcuts. It combines three steps: collects configured directions, validates and resets any that are invalid, then sends reset packets to the server.

**Shortcut for ProcessingModuleItem subclasses:** The `ProcessingModuleItem` base class already calls `validateAndSyncDirectionConfig()` in its `handleCustomClick()` implementation - no extra work needed. Just override the three hooks above.

**When it runs:** Validation runs when the player clicks the overlay, so stale configs are cleaned up immediately. The method returns `true` if any direction was reset, `false` if all configs were already valid.

**Built-in usage:** The Processing Module, Jukebox Module, and Ejection Module all use this pattern. If you place a Processing Module adjacent to an Inventory Module, set a direction to INPUT, then replace the Inventory Module with something else, the direction config will automatically reset to "DISABLED" the next time you open the overlay.

---

## 5. Module Data Channels

Module data channels are the primary mechanism for per-tick state in processing modules. Each module slot in the machine grid has an array of integer channels that persist across ticks while the machine is running, and can be saved to NBT when the module is picked up.

### Channel Array

Each module slot has `NUM_CHANNELS` integer channels (currently 6, indices 0 to 5). Access them via:

```java
// Read a channel value
int value = blockEntity.getModuleData(slotIndex, channel);

// Write a channel value (triggers block state sync to client)
blockEntity.setModuleData(slotIndex, channel, value);
```

Out-of-bounds access returns `0` (read) or is silently ignored (write).

### Standard Channel Assignments

| Channel | Standard name | Used by | Description |
|---------|------|-----|-------------|
| 0 | `burnTime` / `progress` | All processing modules | Current progress counter (ticks remaining) |
| 1 | `burnTimeTotal` / `total` | All processing modules | Maximum progress value (for progress bar calculation) |
| 2 | `cookTime` / `secondary` | Smelting/Blast/Smoker/Brewing | Secondary progress (e.g., cook time separate from burn time) |
| 3 | `cookTimeTotal` / `capacity` | Smelting/Blast/Smoker/Brewing | Secondary max value |
| 4 | Module-specific | Jukebox (stalled flag) | Reserved for module-specific boolean flags |
| 5 | Module-specific | Jukebox (shuffle cycle) | Reserved for module-specific state |

**Channels 0 to 3** are the standard processing channels managed by `ProcessingModuleItem`. **Channels 4 to 5** are reserved for module-specific state and are not interpreted by the base class.

### Rules for Using Channels

1. **Channels 0 to 3 are owned by the base class.** If you extend `ProcessingModuleItem`, channels 0 to 3 are used for the NBT save/restore lifecycle. You may read and write them in your `processModule()` implementation, but the base class may restore them from saved NBT on tick.

2. **Channels 4 to 5 are for module-specific state.** Use them for flags, counters, or indices that don't fit the standard burn/cook pattern. The base class saves and restores these channels along with 0 to 3, so they persist when a module is picked up and placed back.

3. **For more than 6 channels of state,** store additional data in the module's CustomData NBT using `BaseModuleItem.getCustomTag()` and `BaseModuleItem.modifyCustomData()`, or use `blockEntity.setModuleObject(slotIndex + offset, object)` for complex objects (like the Jukebox shuffle order list stored at `slotIndex + 100`).

4. **Channel values are integers.** For floating-point values, scale to an integer (e.g., multiply by 1000) and store the scaled value.

### NBT Save and Restore Lifecycle

`ProcessingModuleItem` automatically manages saving and restoring channel state so that a module picked up mid-operation resumes correctly when placed back.

#### Save (`saveProgressData`)

Called when a module is picked up from the grid. Saves all 6 channels to the module item's `CustomData` NBT under the key `ModuleChannels`. The save triggers when any of these conditions are true:

- Channel 0 (`burnTime`) > 0
- Channel 2 (`cookTime`) > 0
- Channel 4 (`stalled`) > 0

If none of these conditions are met, no NBT is saved - the module is considered idle.

**Important:** If your module uses channel 4 or 5 for state that should persist across pickup, ensure the save condition includes your channel. The current save condition checks `burnTime > 0 || cookTime > 0 || stalled > 0`. If you add a new flag in channel 5 that should trigger a save, you must update the save condition in `saveProgressData()`.

#### Restore (`tickModule`)

Called every tick before `processModule()`. Restores saved channels from NBT when:

- Channel 0 == 0 AND channel 2 == 0 (no active progress)
- Channel 4 == 0 (no stalled state)
- The module stack contains saved `ModuleChannels` NBT

**Why the stalled guard exists:** When a module is in a stalled state (channel 4 > 0), the restore is skipped so the stall handler in `processModule()` has a chance to run first. Without this guard, the restore would overwrite channels 0 and 2 with saved values (which are 0 when stalled), wiping the stalled flag before the stall handler can resolve it. This was the root cause of the Jukebox disc deletion bug.

#### Clear (`clearProgressData`)

Removes the `ModuleChannels` NBT from the module item. Call this when a processing operation completes successfully, and you don't want the progress to carry over.

### Common Pitfalls

| Pitfall | Symptom | Fix |
|---------|---------|-----|
| Using channel index >= NUM_CHANNELS | State silently dropped; module behaves as if channel is always 0 | Ensure `MachineFrameworkBlockEntity.NUM_CHANNELS` is large enough for your highest channel index |
| Not updating save condition | Module-specific state lost on pickup | Add your channel to the `if` condition in `saveProgressData()` |
| Not updating restore channel count | Module-specific state not restored on place back | Ensure the restore loop iterates over all channels you use |
| NBT restore overwrites active state | Module state resets mid-operation | The stalled guard (channel 4 check) prevents this; for other flags, add similar guards |
| Assuming module slot is empty for state detection | Guard never triggers - the module slot always contains the module item itself | Use channel values, not slot emptiness, to detect module state |

### Object Storage for Complex State

For state that doesn't fit in an integer channel (lists, compounds, etc.), use `setModuleObject()`:

```java
// Store complex objects associated with a slot
blockEntity.setModuleObject(slotIndex + 100, myObject);
Object obj = blockEntity.getModuleObject(slotIndex + 100);
```

The `+ 100` offset avoids collision with integer channel indices. The Jukebox Module uses this to store its shuffle order list. Object storage is **not** automatically saved to NBT - you must handle serialization yourself via CustomData NBT if persistence across chunk unload is needed.

### Shift-Click Reverse Mode Cycling

Direction buttons can support **shift-click to cycle modes in reverse order**. `ProcessingModuleItem` handles this automatically by traversing the module's `getSupportedModes()` order backward. Other module types can implement reverse cycling in `toggleDirectionMode()`; `CyclingMode<T>` provides `next()` and `previous()` helpers for that pattern.

**How it works:** In `handleCustomClick()`, call `isShiftHeld()` (inherited from `BaseModuleItem`) to detect whether the player held shift. The shift state is sent to the server via the packet (`ServerboundToggleModuleDirectionPacket` and `ServerboundToggleIoFacePacket`), so the server handles the cycling direction. Processing module mode enums implement `ProcessingMode`, not `CyclingMode`; the base processing class uses the declared supported-mode order for both directions.

The `isShiftHeld()` helper wraps `Screen.hasShiftDown()` so all modules use the same reliable GUI-context shift detection. Never use `player.isShiftKeyDown()` in GUI contexts - it doesn't reflect keyboard state during mouse events.

**For a custom non-processing module:** Implement `CyclingMode<T>` on your mode enum if you want shared forward/reverse helpers:

```java
public enum MyMode implements CyclingMode<MyMode> {
    DISABLED, INPUT, OUTPUT;

    @Override public MyMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override public MyMode previous() {
        return values()[(ordinal() - 1 + values().length) % values().length];
    }
}
```

Then in your `toggleDirectionMode()` override, use the `shiftHeld` parameter:

```java
@Override
public void toggleDirectionMode(ItemStack stack, GridDirection direction, MachineFrameworkBlockEntity be, int slotIndex, boolean shiftHeld) {
    MyMode current = getMyMode(stack, direction);
    MyMode next = shiftHeld ? current.previous() : current.next();
    setMyMode(stack, direction, next);
}
```

**Built-in usage:** Processing modules reverse their declared mode list automatically. Jukebox, inventory, and IO modes use `CyclingMode`; modules with other configuration models may implement their own click behavior.

---

## 6. Creating a Processing Module

```java
public class MyProcessingModule extends ProcessingModuleItem {
    private static final String NBT_KEY = "MyModuleModes";

    // This enum belongs to this module; there is no shared processing SlotMode enum.
    public enum SlotMode implements ProcessingMode {
        DISABLED("D", 0xFF555555, "No items pass through this side.", Role.DISABLED),
        INPUT("I", 0xFF3366CC, "Use items from the adjacent inventory as inputs.", Role.INPUT),
        OUTPUT("O", 0xFFCC3333, "Send processed items to the adjacent inventory.", Role.OUTPUT);

        private final String labelChar;
        private final int color;
        private final String description;
        private final Role role;

        SlotMode(String labelChar, int color, String description, Role role) {
            this.labelChar = labelChar;
            this.color = color;
            this.description = description;
            this.role = role;
        }

        @Override public String getLabelChar() { return labelChar; }
        @Override public int getColor() { return color; }
        @Override public String getDescription() { return description; }
        @Override public Role getRole() { return role; }
    }

    public MyProcessingModule(Properties properties) {
        super(properties);
    }

    @Override
    public String getModuleConfigKey() {
        return NBT_KEY;
    }

    @Override
    public int getMaxInputs() { return 2; }
    @Override
    public int getMaxOutputs() { return 1; }

    @Override
    public SlotMode[] getSupportedModes() {
        return new SlotMode[]{SlotMode.DISABLED, SlotMode.INPUT, SlotMode.OUTPUT};
    }

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity be, int slotIndex, ItemStack stack) {
        // 1. Resolve adjacent inventory slots according to this module's configured roles
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, NBT_KEY);
        ResolvedIoSlots resolved = resolveAdjacentIoSlots(be, slotIndex, modes);

        // 2. Read inputs from resolved.inputs()
        // 3. Check recipe / process
        // 4. Write output to resolved.outputs()
        // 5. Update progress channels
    }
}
```

### Defining Local Modes and Their Behavior

Every processing module defines the modes that make sense for itself. Its enum implements `ProcessingMode`, which provides the button label, ARGB color, tooltip description, and a generic `Role`. The mode name (by default, the enum constant name) is saved in the module's custom data, so keep names stable after release if existing module configurations must continue to load. The base method returns an empty array, but the inherited direction-button and tooltip behavior expects a supported `DISABLED` mode; override it with a non-empty local mode list when using those controls. If a module supplies a different configuration UI, it must also replace the inherited mode-handling paths rather than relying on the empty default.

The role describes only the behavior the shared framework can apply; it does not implement the module's processing logic:

| Role | Shared behavior |
|------|-----------------|
| `DISABLED` | Marks an inactive direction. This mode must be first in `getSupportedModes()`. |
| `INPUT` | Counts against `getMaxInputs()` and is returned as an input by `resolveAdjacentIoSlots()`. The module still decides which items are valid and how to process them. |
| `OUTPUT` | Counts against `getMaxOutputs()` and is returned as an output by `resolveAdjacentIoSlots()`. The module decides what it produces and writes. |
| `OTHER` | No generic input/output resolution or count limit is applied. Handle this mode in the module's own processing logic, or apply a custom limit in `getMaxForMode()`. |

The names below are examples used by built-in module-local enums, not members of a shared enum:

| Built-in mode | Role | Module-specific meaning |
|---------------|------|-------------------------|
| `INPUT` | `INPUT` | Provides items for processing. |
| `OUTPUT` | `OUTPUT` | Receives processed items. |
| `FUEL` | `OTHER` | Smelting, Blast, and Smoker modules handle the adjacent slot as fuel. |
| `POTION` | `INPUT` | Brewing module uses the slot for potion items that are processed in place. |
| `INGREDIENT` | `OTHER` | Brewing module handles the slot as a brewing ingredient. |

Built-in modules declare only their own cycle: Smelting, Blast, and Smoker use `DISABLED`, `INPUT`, `FUEL`, `OUTPUT`; Brewing uses `DISABLED`, `POTION`, `INGREDIENT`; Anvil, Cartography, Composter, Grindstone, and Stonecutter use `DISABLED`, `INPUT`, `OUTPUT`. Your `getSupportedModes()` array determines cycle order; shift-click traverses that order backward. The disabled mode must be first.

`resolveAdjacentIoSlots()` only classifies `INPUT` and `OUTPUT` roles. Modes such as `FUEL` and `INGREDIENT` have role `OTHER`, so inspect the local enum values in `processModule()` to handle them. For example, a module can set a one-slot fuel limit like this:

```java
@Override
protected int getMaxForMode(ProcessingMode mode) {
    if (mode == SlotMode.FUEL) return 1;
    return super.getMaxForMode(mode); // INPUT/OUTPUT use getMaxInputs()/getMaxOutputs()
}
```

The base class handles cycling, reverse cycling, role-based input/output limits, and wrapping. Override `getCycledMode()` or `getMaxForMode()` only when your module needs custom behavior.

### Channel System

Each slot has 6 integer channels for storing progress/state data. Use these methods on `MachineFrameworkBlockEntity`:

```java
int value = be.getModuleData(slotIndex, channel);
be.setModuleData(slotIndex, channel, value);
```

---

## 7. Creating a Logistics Module

```java
public class MyLogisticsModule extends LogisticsModuleItem implements IModuleMenuProvider {
    public MyLogisticsModule(Properties properties) {
        super(properties);
    }

    @Override
    public void openConfiguration(ServerPlayer player, ItemStack moduleStack, int slotIndex, BlockPos blockPos) {
        // Open a GenericModuleConfigMenu or custom menu
    }

    @Override
    public void addModuleSlots(Inventory playerInventory, ItemStack moduleStack, BlockPos blockPos, Consumer<Slot> slotAdder) {
        // Add 9 custom config slots
        for (int i = 0; i < 9; i++) {
            slotAdder.accept(new Slot(/* ... */));
        }
    }

    @Override
    public void handleMenuClick(GenericModuleConfigMenu menu, int slotId, int button, ClickType clickType, Player player) {
        // Handle clicks on custom slots
    }

    @Override
    public ItemStack handleQuickMove(GenericModuleConfigMenu menu, Player player, int index) {
        // Handle shift-click transfers
        return ItemStack.EMPTY;
    }
}
```

---

## 8. Creating a Base Module

For modules that don't fit the processing or logistics patterns:

```java
public class MyBaseModule extends BaseModuleItem {
    public MyBaseModule(Properties properties) {
        super(properties);
    }

    @Override
    public void renderModuleOverlay(GuiGraphics graphics, Slot slot, ItemStack stack, MachineFrameworkBlockEntity be) {
        // Draw your overlay
    }

    @Override
    public void tickModule(Level level, BlockPos pos, MachineFrameworkBlockEntity be, int slotIndex, ItemStack stack) {
        // Your per-tick logic (this IS called automatically)
    }

    @Override
    public boolean isInsideOverlay(double mouseX, double mouseY, int slotX, int slotY) {
        // Define clickable area
        return mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
    }
}
```

**Important:** `MachineFrameworkBlockEntity` dispatches `tickModule()` for installed modules on server ticks without extra registration. The redstone circuit is evaluated separately, and opted-in modules are skipped when their configured policy disallows operation; modules may also implement their own cadence inside the hook.

---

## 9. Registration

Register your module item using NeoForge's registry system:

```java
// In your mod's item registration
public static final DeferredItem<MyProcessingModule> MY_MODULE =
    ITEMS.register("my_module", () -> new MyProcessingModule(
        new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
    ));
```

---

## 10. Textures

### Item Texture
Place a 16-by-16 PNG at:
```
assets/<your_mod_id>/textures/item/my_module.png
```

### Overlay Texture
Place a 16-by-16 PNG at:
```
assets/<your_mod_id>/textures/gui/container/my_module_overlay.png
```

The overlay texture is blitted by your `renderModuleOverlay()` method. The default `ProcessingModuleItem` overlay shows U/D/L/R arrows and a progress bar; you can add your own visual elements on top.

### Item Model
Create a JSON model at:
```
assets/<your_mod_id>/models/item/my_module.json
```
```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "your_mod_id:item/my_module"
  }
}
```

### Language Keys
Add tooltips in your language file:
```json
{
  "item.your_mod_id.my_module": "My Module",
  "item.your_mod_id.my_module.tooltip": "Does something useful"
}
```

---

## 11. Data Tags

Your module **must** be added to the `praeteritum_futurum:machine_modules` tag to be accepted in module slots. Create a JSON file in your mod's data folder:

```
data/praeteritum_futurum/tags/item/machine_modules.json
```
```json
{
  "values": [
    "your_mod_id:my_module"
  ]
}
```

**Optional tags:**
- `praeteritum_futurum:processing_modules`: adds progress data save/restore on pickup
- `praeteritum_futurum:configurable_modules`: accepted in Data Cleaning recipe
- `praeteritum_futurum:logistics_modules`: for logistics categorization

These tags are **additive**; they use `"values"` (not `"replace": true`), so your data pack won't conflict with other mods.

---

## 12. Network Packets

The following packets are pre-registered and reusable by any module:

| Packet | Purpose |
|--------|---------|
| `ServerboundToggleModuleDirectionPacket` | Cycles direction modes (U/D/L/R) via `toggleDirectionMode()`. Carries a `GridDirection` for grid-adjacent slot directions. |
| `ServerboundSetModuleConfigItemPacket` | Sets a config or filter item in NBT |
| `ServerboundSetModuleTextPacket` | Sets text on any `ITextInputModule` |
| `ServerboundOpenModuleConfigPacket` | Opens a module's config screen from the client |
| `ServerboundToggleIoFacePacket` | Toggles IO face modes |

### Direction Toggle Delegation

`ServerboundToggleModuleDirectionPacket` works for any `BaseModuleItem` subclass that overrides `toggleDirectionMode()`. The packet handler checks `instanceof BaseModuleItem` and delegates - no custom packet registration needed. This is the same pattern used by `ServerboundToggleIoFacePacket` for IO-face configuration via `getFaceModes()`.

For custom interactions, register your own packets using NeoForge's networking system.

---

## 13. Example: Complete Module

Here's a complete minimal processing module that converts one item to another:

```java
package com.example.mymod.item;

import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.api.module.ProcessingMode;
import com.thextrunner.praeteritum_futurum.block.GridDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ConverterModuleItem extends ProcessingModuleItem {
    private static final String NBT_KEY = "ConverterModes";

    public enum SlotMode implements ProcessingMode {
        DISABLED("D", 0xFF555555, "No items pass through this side.", ProcessingMode.Role.DISABLED),
        INPUT("I", 0xFF3366CC, "Use items from the adjacent inventory as inputs.", ProcessingMode.Role.INPUT),
        OUTPUT("O", 0xFFCC3333, "Send processed items to the adjacent inventory.", ProcessingMode.Role.OUTPUT);

        private final String labelChar;
        private final int color;
        private final String description;
        private final ProcessingMode.Role role;

        SlotMode(String labelChar, int color, String description, ProcessingMode.Role role) {
            this.labelChar = labelChar;
            this.color = color;
            this.description = description;
            this.role = role;
        }

        @Override public String getLabelChar() { return labelChar; }
        @Override public int getColor() { return color; }
        @Override public String getDescription() { return description; }
        @Override public ProcessingMode.Role getRole() { return role; }
    }

    public ConverterModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public String getModuleConfigKey() {
        return NBT_KEY;
    }

    @Override
    public int getMaxInputs() { return 1; }
    @Override
    public int getMaxOutputs() { return 1; }

    @Override
    public SlotMode[] getSupportedModes() {
        return new SlotMode[]{SlotMode.DISABLED, SlotMode.INPUT, SlotMode.OUTPUT};
    }

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity be, int slotIndex, ItemStack stack) {
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, NBT_KEY);
        ResolvedIoSlots resolved = resolveAdjacentIoSlots(be, slotIndex, modes);

        // Need at least 1 input and 1 output
        if (resolved.inputs().isEmpty() || resolved.outputs().isEmpty()) return;

        // Find an input slot with cobblestone
        for (int inputSlot : resolved.inputs()) {
            ItemStack input = be.getItemHandler().getStackInSlot(inputSlot);
            if (!input.isEmpty() && input.is(Items.COBBLESTONE)) {
                // Find an empty output slot
                for (int outputSlot : resolved.outputs()) {
                    ItemStack output = be.getItemHandler().getStackInSlot(outputSlot);
                    if (output.isEmpty()) {
                        // Convert cobblestone to stone
                        input.shrink(1);
                        be.getItemHandler().setStackInSlot(outputSlot, new ItemStack(Items.STONE));
                        be.setChanged();
                        return;
                    }
                }
                break;
            }
        }
    }
}
```

Then register it, add it to the `machine_modules` tag, and create the textures. Done!

---

## 14. Creating a Custom Brewing Module

The built-in Brewing Module only supports vanilla potions because NeoForge's `PotionBrewing` API is broken in 1.21.1 (see [Create #9732](https://github.com/Creators-of-Create/Create/issues/9732)). If your mod adds custom potions, you can create your own brewing-like module (for example, a "Distillery Module") that supports your custom recipes.

### Why Not Extend the Brewing Module?

The Brewing Module uses a hardcoded recipe map (`BrewingRecipeItem`) that only knows about vanilla potions. There's no recipe registration hook, so mod-added potions cannot be added to it. The cleanest solution is to create a separate module for your mod's potions.

### Step 1: Define Your Recipes

Create a utility class with a recipe map similar to `BrewingRecipeItem`. Each recipe maps an input potion and ingredient item to an output potion:

```java
public class MyPotionRecipes {
    private static final Map<BrewingKey, Holder<Potion>> RECIPES = new HashMap<>();

    static {
        // Example: Awkward Potion + Custom Herb = Custom Potion
        put(Potions.AWKWARD, MyItems.CUSTOM_HERB.get(), MyPotions.CUSTOM_POTION.get());

        // Example: Custom Potion + Glowstone = Strong Custom Potion
        put(MyPotions.CUSTOM_POTION.get(), Items.GLOWSTONE_DUST, MyPotions.STRONG_CUSTOM_POTION.get());
    }

    private static void put(Holder<Potion> input, Item ingredient, Holder<Potion> output) {
        RECIPES.put(new BrewingKey(input, ingredient), output);
    }

    public static boolean hasRecipe(Holder<Potion> inputPotion, ItemStack ingredient) {
        return RECIPES.containsKey(new BrewingKey(inputPotion, ingredient.getItem()));
    }

    public static Holder<Potion> getOutput(Holder<Potion> inputPotion, ItemStack ingredient) {
        return RECIPES.get(new BrewingKey(inputPotion, ingredient.getItem()));
    }

    // BrewingKey class (same as in BrewingRecipeItem)
    private static class BrewingKey {
        private final Holder<Potion> potion;
        private final Item ingredient;
        private final int hashCode;

        BrewingKey(Holder<Potion> potion, Item ingredient) {
            this.potion = potion;
            this.ingredient = ingredient;
            this.hashCode = System.identityHashCode(potion.value()) ^ ingredient.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BrewingKey other)) return false;
            return this.potion.value() == other.potion.value() && this.ingredient == other.ingredient;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
```

### Step 2: Create the Module Class

Extend `ProcessingModuleItem` and implement brewing logic. The slot layout uses 3 potion input slots and 1 ingredient slot, with Blaze Powder auto-detected as fuel:

```java
public class DistilleryModuleItem extends ProcessingModuleItem {
    private static final String NBT_KEY = "DistilleryModes";
    private static final int BREW_TIME = 200; // ticks per batch
    private static final int MAX_FUEL_TIME = 400; // 20 batches per Blaze Powder

    public enum SlotMode implements ProcessingMode {
        DISABLED("D", 0xFF555555, "No items pass through this side.", ProcessingMode.Role.DISABLED),
        POTION("P", 0xFF9933CC, "Use potion items from the adjacent inventory.", ProcessingMode.Role.INPUT),
        INGREDIENT("G", 0xFF33CC66, "Use ingredients from the adjacent inventory.", ProcessingMode.Role.OTHER);

        private final String labelChar;
        private final int color;
        private final String description;
        private final ProcessingMode.Role role;

        SlotMode(String labelChar, int color, String description, ProcessingMode.Role role) {
            this.labelChar = labelChar;
            this.color = color;
            this.description = description;
            this.role = role;
        }

        @Override public String getLabelChar() { return labelChar; }
        @Override public int getColor() { return color; }
        @Override public String getDescription() { return description; }
        @Override public ProcessingMode.Role getRole() { return role; }
    }

    public DistilleryModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public String getModuleConfigKey() {
        return NBT_KEY;
    }

    @Override
    public int getMaxInputs() { return 3; } // 3 potion slots
    @Override
    public int getMaxOutputs() { return 0; } // in-place brewing

    @Override
    protected int getMaxForMode(ProcessingMode mode) {
        if (mode == SlotMode.INGREDIENT) return 1;
        return super.getMaxForMode(mode);
    }

    @Override
    public SlotMode[] getSupportedModes() {
        return new SlotMode[]{SlotMode.DISABLED, SlotMode.POTION, SlotMode.INGREDIENT};
    }

    @Override
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity be,
                                  int slotIndex, ItemStack stack) {
        // Resolve slots
        Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, NBT_KEY);

        // Collect potion slots and ingredient slot
        List<Integer> potionSlots = new ArrayList<>();
        int ingredientSlot = -1;
        for (GridDirection dir : GridDirection.VALUES) {
            int slot = getAdjacentSlotIndex(slotIndex, dir);
            if (slot == -1) continue;
            if (modes.get(dir) == SlotMode.POTION) {
                potionSlots.add(slot);
            } else if (modes.get(dir) == SlotMode.INGREDIENT && ingredientSlot == -1) {
                ingredientSlot = slot;
            }
        }

        if (ingredientSlot == -1) return;

        // Read fuel state from module channels
        int fuelTime = be.getModuleData(slotIndex, 0);
        int brewTime = be.getModuleData(slotIndex, 1);
        boolean isBurning = fuelTime > 0;

        // Check ingredient slot for Blaze Powder (fuel) or brewing ingredient
        ItemStack ingredient = be.getItemHandler().getStackInSlot(ingredientSlot);
        if (!ingredient.isEmpty() && ingredient.is(Items.BLAZE_POWDER) && !isBurning) {
            // Load fuel
            ingredient.shrink(1);
            fuelTime = MAX_FUEL_TIME;
            be.setModuleData(slotIndex, 0, fuelTime);
            be.setChanged();
            return;
        }

        // Check if any potion has a valid recipe
        if (!isBurning || ingredient.isEmpty()) return;
        boolean anyRecipe = false;
        for (int potionSlot : potionSlots) {
            ItemStack potion = be.getItemHandler().getStackInSlot(potionSlot);
            if (!potion.isEmpty() && isPotionItem(potion)) {
                Holder<Potion> potionType = getPotionHolder(potion);
                if (potionType != null && MyPotionRecipes.hasRecipe(potionType, ingredient)) {
                    anyRecipe = true;
                    break;
                }
            }
        }
        if (!anyRecipe) return;

        // Brew
        brewTime++;
        be.setModuleData(slotIndex, 1, brewTime);

        if (brewTime >= BREW_TIME) {
            // Batch complete - apply recipes
            for (int potionSlot : potionSlots) {
                ItemStack potion = be.getItemHandler().getStackInSlot(potionSlot);
                if (!potion.isEmpty() && isPotionItem(potion)) {
                    Holder<Potion> potionType = getPotionHolder(potion);
                    if (potionType != null && MyPotionRecipes.hasRecipe(potionType, ingredient)) {
                        Holder<Potion> output = MyPotionRecipes.getOutput(potionType, ingredient);
                        if (output != null) {
                            // Create the output potion in-place
                            ItemStack result = potion.copy();
                            result.set(DataComponents.POTION_CONTENTS,
                                new PotionContents(Optional.of(output), Optional.empty(), Collections.emptyList()));
                            be.getItemHandler().setStackInSlot(potionSlot, result);
                        }
                    }
                }
            }
            // Consume ingredient and fuel
            ingredient.shrink(1);
            fuelTime -= 20;
            brewTime = 0;
            be.setModuleData(slotIndex, 0, fuelTime);
            be.setModuleData(slotIndex, 1, brewTime);
        }
        be.setChanged();
    }

    // Helper methods for potion type checking and extraction
    // (similar to BrewingModuleItem helpers)
}
```

### Step 3: Register and Configure

Follow the standard registration steps (sections 8-10 of this guide):

- Register the item with NeoForge
- Create item and overlay textures
- Add to `praeteritum_futurum:machine_modules` and `processing_modules` tags
- Add language keys and a crafting recipe

### Important Notes

- **In-place brewing**: Potions transform in their input slots, so no output slots are needed. This fits within the 4-adjacent-slot limit.
- **Fuel**: Blaze Powder is auto-detected from the ingredient slot. Each powder provides 20 brew batches.
- **PotionContents constructor**: Always pass `Optional.empty()` and `Collections.emptyList()` for the custom effects parameter. Passing `null` will cause a crash when serializing potions.
- **Splash and lingering conversions**: If your mod has splash/lingering-style conversions, handle them as item-type changes (like the Brewing Module does with gunpowder and dragon breath), not as separate potion types.

## 15. Inline Overlay System

The inline overlay system lets modules render custom UI directly on the machine config screen, without opening a separate container menu. This is used by Processing modules, the Jukebox Module, the Ejection Module, and the IO Module.

### Enabling Inline Overlays

Override `opensInlineOverlay()` to return `true`. This tells `MachineInfoScreen` to set up the overlay rendering pipeline for your module:

```java
@Override
public boolean opensInlineOverlay() {
    return true;
}
```

When `false` (the default), the module opens a separate container menu via `IConfigurableModule` instead.

### Overlay Rendering Lifecycle

When a player clicks a module slot that has an inline overlay, this is the call sequence:

1. **`isInsideOverlay(mouseX, mouseY, slotX, slotY)`** - called during mouse events to determine if the click is within the overlay area. Override to expand the clickable area beyond the center slot (e.g., to include direction buttons).
2. **`handleCustomClick(mouseX, mouseY, slotX, slotY, stack, blockEntity, slotIndex, minecraft)`** - called on click. Handle button clicks, mode cycling, etc.
3. **`renderCustomOverlay(graphics, slotX, slotY, stack, blockEntity, font)`** - called every render frame while the overlay is open. Draw your buttons, labels, and indicators here.
4. **`isCloseButton(mouseX, mouseY, slotX, slotY)`** - called to detect clicks on the close (X) button. All modules use the same standard position above the center slot.

### Center Slot Management

The center slot (the module slot itself) can serve different purposes depending on the module. The system provides hooks to control its behavior:

| Hook | Purpose | Default |
|------|---------|---------|
| `usesCenterSlot()` | Whether the module uses the center slot at all | `false` |
| `allowsCenterSlotInteraction()` | Whether clicks on the center slot pass through to normal inventory interaction while the overlay is open | `false` |
| `hasConfigSlot()` | Whether the center slot holds a ghost config item (stored in NBT) | `false` |
| `hasInteractableInventorySlot()` | Whether the module's inventory slot is player-accessible in the machine UI | `false` |
| `isItemValidForSlot(stack, blockEntity, slotIndex)` | Whether an item can be placed in the module's inventory slot (used to decide click behavior: place item vs. open config) | `false` |
| `hidesSlotItemInMachineScreen(stack, blockEntity, slotIndex)` | Whether the module's slot item should be hidden in the machine screen so the overlay fully covers the slot | `false` |

**When `usesCenterSlot()` is `false`:** The overlay renders a dark hatch lid over the center slot to clearly indicate it has no function. Use `drawDarkHatchLid(graphics, slotX, slotY)` in your overlay.

**When `usesCenterSlot()` is `true`:** The module controls how the center slot renders. Examples:
- **Ghost item** (Stonecutter, Anvil): `hasConfigSlot()` returns `true`; the overlay renders a gray background + ghost item.
- **Real inventory** (Jukebox): `allowsCenterSlotInteraction()` returns `true`; the real item is visible through normal slot rendering.
- **Custom rendering** (Anvil): The overlay renders a button with a label that opens a text input panel.

### UI Drawing Helpers

`BaseModuleItem` and `ProcessingModuleItem` provide protected helper methods to avoid duplicating UI rendering code across modules:

| Helper | Class | Description |
|--------|-------|-------------|
| `drawCloseButton(graphics, font, slotX, slotY)` | `BaseModuleItem` | Draws the standard 16-by-16 close (X) button with raised border - position is controlled by the `slotX`/`slotY` parameters passed |
| `drawDarkHatchLid(graphics, slotX, slotY)` | `BaseModuleItem` | Draws a dark gray panel over the center slot to indicate it has no function |
| `drawDirectionButtons(graphics, font, slotX, slotY, blockEntity, modes)` | `ProcessingModuleItem` | Draws the 4 direction buttons (U/D/L/R) around a module slot, with border awareness |
| `drawProcessingButton(graphics, font, x, y, mode)` | `ProcessingModuleItem` | Draws a single direction button with the mode's color and label |
| `drawProcessingButtonWithCheck(graphics, font, slotIndex, blockEntity, x, y, dir, mode)` | `ProcessingModuleItem` | Like `drawProcessingButton`, but checks if the adjacent module type is allowed and renders disabled if not |

**Usage example:**

```java
@Override
public void renderCustomOverlay(GuiGraphics graphics, int slotX, int slotY, ItemStack stack,
                                 MachineFrameworkBlockEntity blockEntity, Font font) {
    // Direction buttons (from ProcessingModuleItem)
    Map<GridDirection, ProcessingMode> modes = getSlotModes(stack, getModuleConfigKey());
    drawDirectionButtons(graphics, font, slotX, slotY, blockEntity, modes);

    // Center slot - dark hatch lid if unused
    if (!usesCenterSlot()) {
        drawDarkHatchLid(graphics, slotX, slotY);
    }

    // Close button
    drawCloseButton(graphics, font, slotX, slotY);
}
```

### Click Detection Helpers

| Helper | Class | Description |
|--------|-------|-------------|
| `getClickedDirection(slotX, slotY, mx, my, slotIndex)` | `BaseModuleItem` | Determines which direction button (U/D/L/R) was clicked, with border awareness. Returns `null` if no button was clicked. |
| `isInsideButton(mx, my, x, y)` | `BaseModuleItem` (static) | Checks if the mouse is inside a 16-by-16 button centered at `(x, y)`. |
| `isShiftHeld()` | `BaseModuleItem` | Returns whether the player is holding shift during a GUI interaction. Wraps `Screen.hasShiftDown()` for reliable shift detection. |

### Slot Hiding

Override `shouldHideSlot(configuringSlotIndex, targetSlotIndex)` to control which adjacent slots are hidden when the overlay is open. The default implementation hides the module slot and its 4 adjacent neighbors, plus the diagonal bottom-left slot.

### Overlay Slot Index

All modules inherit `getOverlaySlotIndex()` and `setOverlaySlotIndex()` from `BaseModuleItem`. The overlay renderer sets the slot index before calling `renderCustomOverlay()`, enabling border-aware button rendering and adjacent-module detection. You don't need to manage this manually - just call `getOverlaySlotIndex()` when you need to know the grid slot position.

### Module Removal Guard

Override `mayPickupModule()` to return `false` while the module is in a state that should prevent removal. The `ModuleConfigSlotItemHandler` checks both the inventory slot emptiness and this hook - both must pass for the module to be removable.

```java
@Override
public boolean mayPickupModule(Level level, BlockPos pos, MachineFrameworkBlockEntity blockEntity,
                                int slotIndex, ItemStack stack) {
    // Lock the module while a song is playing
    return getProgressPercent(stack, blockEntity, slotIndex) <= 0;
}
```

### Complete Overlay Example

Here's a minimal inline overlay module:

```java
public class MyOverlayModule extends BaseModuleItem {
    public MyOverlayModule(Properties properties) {
        super(properties);
    }

    @Override
    public boolean opensInlineOverlay() {
        return true;
    }

    @Override
    public boolean isInsideOverlay(double mouseX, double mouseY, int slotX, int slotY) {
        // Cover the center slot and a button above it
        boolean center = mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
        boolean button = BaseModuleItem.isInsideButton(mouseX, mouseY, slotX + 8, slotY - 10);
        boolean close = isCloseButton(mouseX, mouseY, slotX, slotY);
        return center || button || close;
    }

    @Override
    public void handleCustomClick(double mouseX, double mouseY, int slotX, int slotY,
                                   ItemStack stack, MachineFrameworkBlockEntity blockEntity,
                                   int slotIndex, Minecraft minecraft) {
        if (isCloseButton(mouseX, mouseY, slotX, slotY)) {
            // Close the overlay
            minecraft.screen.close();
            return;
        }
        if (BaseModuleItem.isInsideButton(mouseX, mouseY, slotX + 8, slotY - 10)) {
            // Handle custom button click
            // ...
        }
    }

    @Override
    public void renderCustomOverlay(GuiGraphics graphics, int slotX, int slotY,
                                     ItemStack stack, MachineFrameworkBlockEntity blockEntity, Font font) {
        // Custom button above the center slot
        graphics.fill(slotX - 6, slotY - 18, slotX + 22, slotY - 2, 0xFF888888);
        graphics.fill(slotX - 5, slotY - 17, slotX + 21, slotY - 3, 0xFF44AA44);
        graphics.drawCenteredString(font, "GO", slotX + 8, slotY - 14, 0xFFFFFF);

        // Center slot - unused, show dark hatch lid
        drawDarkHatchLid(graphics, slotX, slotY);

        // Close button
        drawCloseButton(graphics, font, slotX, slotY);
    }
}
```

## 16. Module Object Storage & Cleanup

Modules sometimes need to persist objects across ticks (e.g., song players, timers, connection state) and clean them up when the machine is destroyed. The block entity provides two generic systems so modules can manage their own lifecycle without the block entity knowing about module-specific state.

### Module Object Storage

The block entity maintains a map of module slot indices to arbitrary objects. Modules use `setModuleObject()` to store an object and `getModuleObject(slotIndex, ObjectType.class)` to retrieve it with a runtime type check. The getter returns `null` when the slot has no stored object and throws `ClassCastException` if the stored value has the wrong type.

```java
// Store an object (e.g., a JukeboxSongPlayer)
be.setModuleObject(slotIndex, myObject);

// Retrieve it later
MyObject obj = be.getModuleObject(slotIndex, MyObject.class);

// Remove it (pass null)
be.setModuleObject(slotIndex, (MyObject) null);
```

**How it works:** The block entity stores objects in a `Map<Integer, Object>` keyed by module slot index. The block entity does not interpret these objects - they belong entirely to the module. When the machine is destroyed, all stored objects are cleared.

**Example use case:** The Jukebox Module stores a `JukeboxSongPlayer` instance so it persists across ticks. This allows the song to be stopped when the disc is removed or the machine is destroyed, and enables the colorful song title subtitle to display correctly.

### Module Cleanup

When a machine block is destroyed, all registered module cleanup actions are executed. This allows modules to stop ongoing operations (sounds, timers, network connections) before the block entity is removed.

```java
// Register a cleanup action (called when the machine is destroyed)
be.registerModuleCleanup(slotIndex, () -> {
    // Stop any ongoing operations
    myObject.stop();
});
```

**How it works:** The block entity stores cleanup runnables in a `Map<Integer, Runnable>`. When the machine block is destroyed (via `onRemove()`), it calls `runAllModuleCleanups()` which executes all registered runnables and clears both the cleanup map and the module object map. If an action throws, the error is logged and the remaining cleanup actions still run.

**Example use case:** The Jukebox Module registers a cleanup that stops the `JukeboxSongPlayer` when the machine is destroyed, preventing orphaned sounds from continuing to play.

### Combined Usage Pattern

The typical pattern for modules that need persistent state:

```java
@Override
protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity be,
                              int slotIndex, ItemStack stack) {
    // Get or create the persistent object
    MyObject obj = be.getModuleObject(slotIndex, MyObject.class);
    if (obj == null) {
        MyObject createdObject = new MyObject();
        be.setModuleObject(slotIndex, createdObject);
        // Register cleanup for when the machine is destroyed
        be.registerModuleCleanup(slotIndex, () -> createdObject.stop(level));
        obj = createdObject;
    }
    // Use the object...
    obj.doSomething();
}
```

---

## Module Implementation Checklist

- [ ] Extend the appropriate base class (`ProcessingModuleItem`, `LogisticsModuleItem`, or `BaseModuleItem`)
- [ ] Override the required methods
- [ ] Register the item with NeoForge registry
- [ ] Create item texture (`textures/item/<name>.png`)
- [ ] Create overlay texture (`textures/gui/container/<name>_overlay.png`)
- [ ] Create item model JSON
- [ ] Add language keys
- [ ] Add to `praeteritum_futurum:machine_modules` tag
- [ ] (Optional) Add to `processing_modules` / `configurable_modules` tags
- [ ] (Optional) Add a crafting recipe
