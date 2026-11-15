# Module API Guide for Third-Party Modders

This guide explains how to create custom modules for the **Praeteritum Futurum** Machine Framework system. Modules are items placed in the 3×3 module grid of a Machine Framework block, each providing specialized functionality (smelting, filtering, IO control, etc.).

---

## Table of Contents

1. [Prerequisites & Dependency Setup](#1-prerequisites--dependency-setup)
2. [Base Classes](#2-base-classes)
3. [API Interfaces](#3-api-interfaces)
4. [Creating a Processing Module](#4-creating-a-processing-module)
5. [Creating a Logistics Module](#5-creating-a-logistics-module)
6. [Creating a Base Module](#6-creating-a-base-module)
7. [Registration](#7-registration)
8. [Textures](#8-textures)
9. [Data Tags](#9-data-tags)
10. [Network Packets](#10-network-packets)
11. [Example: Complete Module](#11-example-complete-module)
12. [Comparison with Other Mod APIs](#12-comparison-with-other-mod-apis)

---

## 1. Prerequisites & Dependency Setup

Before creating a module, your mod needs to depend on Praeteritum Futurum.

### Adding the Dependency in `build.gradle`

Add Praeteritum Futurum as a compile-time dependency so you can access the API classes:

```groovy
// In your build.gradle dependencies block
dependencies {
    // Add Praeteritum Futurum as a compile-only dependency
    compileOnly "com.thextrunner:praeteritum-futurum:<version>"
    
    // Runtime dependency for testing
    runtimeOnly "com.thextrunner:praeteritum-futurum:<version>"
}
```

If you're developing locally and Praeteritum Futurum isn't published to Maven yet, use a local file reference:

```groovy
dependencies {
    compileOnly files("libs/praeteritum-futurum-<version>.jar")
    runtimeOnly files("libs/praeteritum-futurum-<version>.jar")
}
```

### Declaring the Dependency in `mods.toml`

Tell NeoForge that your mod requires Praeteritum Futurum:

```toml
[[dependencies.your_mod_id]]
    modId="praeteritum_futurum"
    mandatory=true
    versionRange="[1.0.0,)"
    ordering="NONE"
    side="BOTH"
```

### What You Get

Once the dependency is set up, you can import and extend any of the public API classes:

- `com.thextrunner.praeteritum_futurum.item.custom.BaseModuleItem`
- `com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem`
- `com.thextrunner.praeteritum_futurum.item.custom.LogisticsModuleItem`
- `com.thextrunner.praeteritum_futurum.api.module.*` (all interfaces)
- `com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity`
- `com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigMenu`
- `com.thextrunner.praeteritum_futurum.screen.GenericModuleConfigScreen`

---

## 2. Base Classes

There are three base classes you can extend, depending on your module's purpose:

### `BaseModuleItem`
**Root class for all modules.** Use this if your module doesn't fit the processing or logistics patterns.

- Provides: overlay rendering, click handling, slot hiding, face modes, tick hooks
- Constructor: `public BaseModuleItem(Properties properties)`
- Key methods to override:
  - `renderModuleOverlay()` — draws visual indicators on the module slot
  - `handleCustomClick()` — handles clicks on the module's overlay
  - `tickModule()` — called every tick on the server (auto-dispatched by MachineFrameworkBlockEntity)
  - `isInsideOverlay()` — defines the clickable area of the overlay

### `ProcessingModuleItem` (extends `BaseModuleItem`)
**For modules that transform items over time.** Use this for smelting-like, brewing-like, crafting-like operations.

- Provides: slot direction configuration (U/D/L/R arrows), progress bar overlay, config slot support, IO slot resolution
- Constructor: `public ProcessingModuleItem(Properties properties)`
- **Must override:**
  - `getModuleConfigKey()` — returns the NBT key for storing slot mode data
  - `processModule()` — your core processing logic (called every tick)
- **Optional overrides:**
  - `getMaxInputs()` — max input slots (default: unlimited)
  - `getMaxOutputs()` — max output slots (default: unlimited)
  - `hasConfigSlot()` — returns `true` to show a center config button (default: `false`)
  - `acceptsConfigSlotItem()` — restricts what items can be placed in the config slot (default: accepts all)
  - `useDefaultConfigSlotClick()` — returns `false` to handle config slot clicks yourself (default: `true`)
  - `getNextMode()` — controls the slot mode cycle (default: DISABLED → INPUT → FUEL → OUTPUT)
  - `getProgressPercent()` — returns 0.0–1.0 for the progress bar overlay

### `LogisticsModuleItem` (extends `BaseModuleItem`, implements `IConfigurableModule`)
**For modules that manage item/liquid flow, filtering, or logistics.** Use this for filter modules, IO modules, liquid piping, redstone logic, etc.

- Provides: face-mode configuration, `IConfigurableModule` interface for custom config screens
- Constructor: `public LogisticsModuleItem(Properties properties)`
- **Must override:**
  - `openConfiguration()` — opens a server-side container menu for configuration

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
AbstractContainerMenu createMenu(int id, Inventory playerInventory, ItemStack moduleStack, BlockPos blockPos)
Component getDisplayName(ItemStack moduleStack)
int getModuleSlots()  // number of custom slots to add
```

### `ITextInputModule`
For modules that need text input (like Anvil module renaming). Implement this to get a free inline text input panel that appears on top of the machine config screen.

```java
String getTextInputLabel()   // e.g., "Custom Name"
String getTextInputHint()    // e.g., "Enter a name..."
int getMaxTextLength()       // max characters allowed
String getExistingText()     // current stored text (for pre-filling)
void setText(String text)    // called when player saves text
```

**No custom screen code is needed** — `MachineInfoScreen` automatically provides the inline panel when it detects an `ITextInputModule` in a config slot. Just make sure `hasConfigSlot()` returns `true`.

### `ICustomScreenProvider`
For modules that render custom inline overlays directly on the machine UI.

```java
void renderCustomScreen(GuiGraphics graphics, int x, int y, ItemStack stack, MachineFrameworkBlockEntity blockEntity)
```

---

## 4. Creating a Processing Module

This is the most common module type. Here's the minimal structure:

```java
public class MyProcessingModule extends ProcessingModuleItem {
    private static final String NBT_KEY = "MyModuleModes";

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
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity be, int slotIndex, ItemStack stack) {
        // 1. Resolve adjacent IO slots
        Map<Direction, SlotMode> modes = getSlotModes(stack, NBT_KEY);
        ResolvedIoSlots resolved = resolveAdjacentIoSlots(be, slotIndex, modes);

        // 2. Read inputs from resolved.inputSlots
        // 3. Check recipe / process
        // 4. Write output to resolved.outputSlots
        // 5. Update progress channels
    }
}
```

### Slot Mode Configuration

Each adjacent slot (Up, Down, Left, Right) can be configured to one of four modes:
- `DISABLED`: slot is ignored
- `INPUT`: slot provides items to this module
- `FUEL`: slot provides fuel (for smelting-like modules)
- `OUTPUT`: slot receives items from this module

Players configure these by clicking the arrow buttons (U/D/L/R) displayed on the module overlay.

### Channel System

Each slot has 4 integer channels for storing progress/state data. Use these helper methods:

```java
int value = getChannelValue(be, slotIndex, channel);
setChannelValue(be, slotIndex, channel, value);
```

---

## 5. Creating a Logistics Module

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
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, ItemStack moduleStack, BlockPos blockPos) {
        return new GenericModuleConfigMenu(id, playerInventory, moduleStack, blockPos);
    }

    @Override
    public Component getDisplayName(ItemStack moduleStack) {
        return Component.translatable("module.my_mod.my_module");
    }

    @Override
    public int getModuleSlots() {
        return 9; // number of custom config slots
    }
}
```

---

## 6. Creating a Base Module

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
        // Your per-tick logic — this IS called automatically
    }

    @Override
    public boolean isInsideOverlay(double mouseX, double mouseY, int slotX, int slotY) {
        // Define clickable area
        return mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
    }
}
```

**Important:** `tickModule()` is automatically called for all `BaseModuleItem` instances every server tick. You don't need to register anything special.

---

## 7. Registration

Register your module item using NeoForge's registry system:

```java
// In your mod's item registration
public static final DeferredItem<MyProcessingModule> MY_MODULE =
    ITEMS.register("my_module", () -> new MyProcessingModule(
        new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
    ));
```

---

## 8. Textures

### Item Texture
Place a 16×16 PNG at:
```
assets/<your_mod_id>/textures/item/my_module.png
```

### Overlay Texture
Place a 16×16 PNG at:
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

## 9. Data Tags

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

## 10. Network Packets

The following packets are pre-registered and reusable by any module:

| Packet | Purpose |
|--------|---------|
| `ServerboundToggleModuleDirectionPacket` | Cycles slot modes (U/D/L/R) |
| `ServerboundSetModuleConfigItemPacket` | Sets a config/filter item in NBT |
| `ServerboundSetModuleTextPacket` | Sets text on any `ITextInputModule` |
| `ServerboundOpenModuleConfigPacket` | Opens a module's config screen from the client |
| `ServerboundToggleIoFacePacket` | Toggles IO face modes |

For custom interactions, register your own packets using NeoForge's networking system.

---

## 11. Example: Complete Module

Here's a complete minimal processing module that converts one item to another:

```java
package com.example.mymod.item;

import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem;
import com.thextrunner.praeteritum_futurum.block.entity.MachineFrameworkBlockEntity;
import com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem.SlotMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ConverterModuleItem extends ProcessingModuleItem {
    private static final String NBT_KEY = "ConverterModes";

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
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity be, int slotIndex, ItemStack stack) {
        Map<Direction, SlotMode> modes = getSlotModes(stack, NBT_KEY);
        ResolvedIoSlots resolved = resolveAdjacentIoSlots(be, slotIndex, modes);

        // Need at least 1 input and 1 output
        if (resolved.inputSlots.isEmpty() || resolved.outputSlots.isEmpty()) return;

        // Find an input slot with cobblestone
        for (int inputSlot : resolved.inputSlots) {
            ItemStack input = be.getItemHandler().getStackInSlot(inputSlot);
            if (!input.isEmpty() && input.is(Items.COBBLESTONE)) {
                // Find an empty output slot
                for (int outputSlot : resolved.outputSlots) {
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

## Summary Checklist

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

---

## 12. Comparison with Other Mod APIs

This section compares the Praeteritum Futurum Module API with other popular mod APIs to help you understand where it stands.

### Create Mod Addon API

**How Create works:** Create provides a template repository and CLI bootstrapper for addons. Addons are standalone mods that depend on Create and use its public classes (like `create.foundation.block` and `create.kinetics`).

| Feature | Create | Praeteritum Futurum |
|---------|--------|---------------------|
| Template repo | ✅ Provided | ❌ Not yet provided |
| Public base classes | ✅ Extensive | ✅ 3 base classes |
| Data-only addons | ✅ Via data packs | ✅ Via data tags |
| Documentation | ✅ Wiki + examples | ✅ This guide |
| Network packets | ✅ Pre-registered | ✅ Pre-registered |
| Config screen system | ✅ Custom | ✅ GenericModuleConfigMenu |

**What Create does better:** Provides a ready-to-use template repository and CLI bootstrapper. A modder can clone the template and start coding immediately.

**What Praeteritum Futurum does better:** The module system is more self-contained — you extend a base class and the machine framework handles all the rest (slot resolution, progress tracking, overlay rendering). Create addons often need to implement their own block entities and tile entity rendering from scratch.

### Architectury API

**How Architectury works:** Architectury is a cross-loader abstraction layer. It provides gradle plugins, templates, and API abstractions for developing mods that work on both Fabric and NeoForge.

| Feature | Architectury | Praeteritum Futurum |
|---------|--------------|---------------------|
| Cross-loader support | ✅ Fabric + NeoForge | ❌ NeoForge only |
| Gradle plugin | ✅ Full toolchain | ❌ Standard NeoForge |
| Event hooks | ✅ 90+ events | ❌ N/A |
| Registry abstraction | ✅ Unified | ❌ NeoForge-native |
| Template mods | ✅ Provided | ❌ Not yet provided |

**Note:** Architectury is a different category — it's a development toolchain, not a gameplay API. The comparison is only relevant for the template and documentation aspects.

### What's Missing Compared to Create

1. **Template repository** — Create provides a GitHub template you can clone. Praeteritum Futurum doesn't yet have one. The complete example in this guide serves the same purpose, but a template repo would lower the barrier to entry.

2. **Dedicated API JAR** — Create ships a separate API JAR that addons depend on. Praeteritum Futurum currently ships everything in one JAR. This isn't a blocker, but a separate API JAR would reduce the dependency footprint for addons.

3. **API versioning** — Create uses semantic versioning for its API. Praeteritum Futurum should follow the same pattern to ensure addon compatibility across versions.

### What Praeteritum Futurum Does Better

1. **Zero boilerplate for common patterns** — The `ITextInputModule` interface gives you a complete text input config screen with no custom code. The `GenericModuleConfigMenu` gives you a slot-based config screen. Create addons often need to implement their own GUI infrastructure.

2. **Automatic server-side dispatch** — `tickModule()` is called automatically for all module types. You don't need to register your module with any event system or service loader.

3. **Additive data tags** — Third-party modules work purely via data tags. No code changes to the base mod are needed. The tags are additive (`"values"`, not `"replace": true`), so multiple mods can add modules without conflict.

4. **Inline overlay system** — The module config overlay renders directly on the machine UI, not as a separate full-screen. This is more compact and keeps the player in context.

### Recommendations for Future API Improvements

- **Create a template repository** — Follow Create's model: a GitHub template with a pre-configured `build.gradle`, `mods.toml`, and a skeleton module class.
- **Publish a separate API JAR** — Ship the API classes as a separate artifact so addons have a minimal dependency.
- **Add API versioning** — Use semantic versioning and document breaking changes in the changelog.
- **Consider a `ModuleRegistry` event** — Instead of requiring data tags, an event could let mods register modules programmatically. This would be optional alongside the existing tag system.
