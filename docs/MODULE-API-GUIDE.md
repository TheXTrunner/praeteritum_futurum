# Module API Guide for Third-Party Modders

[Documentation index](README.md) | [Project README](../README.md) | [Contributing guide](../CONTRIBUTING.md)

This guide walks through creating custom modules for the **Praeteritum Futurum** Machine Framework. Modules are items placed in its 27-slot grid, each providing specialized functionality (smelting, filtering, IO control, etc.). Start with dependency setup, then follow the base-class and complete-module examples. The guide targets third-party mod developers familiar with Java, Gradle, and NeoForge.

---

## Table of Contents

1. [Prerequisites & Dependency Setup](#1-prerequisites--dependency-setup)
2. [Base Classes](#2-base-classes)
3. [API Interfaces](#3-api-interfaces)
5. [Creating a Processing Module](#6-creating-a-processing-module)
6. [Creating a Logistics Module](#7-creating-a-logistics-module)
7. [Creating a Base Module](#8-creating-a-base-module)
8. [Registration](#9-registration)
9. [Textures](#10-textures)
10. [Data Tags](#11-data-tags)
11. [Network Packets](#12-network-packets)
12. [Example: Complete Module](#13-example-complete-module)
13. [Creating a Custom Brewing Module](#14-creating-a-custom-brewing-module)

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
- `com.thextrunner.praeteritum_futurum.api.module.*` (all interfaces and data models)
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
  - `renderModuleOverlay()` - draws visual indicators on the module slot
  - `handleCustomClick()` - handles clicks on the module's overlay
  - `tickModule()` - called every tick on the server (auto-dispatched by MachineFrameworkBlockEntity)
  - `isInsideOverlay()` - defines the clickable area of the overlay

### `ProcessingModuleItem` (extends `BaseModuleItem`)
**For modules that transform items over time.** Use this for smelting-like, brewing-like, crafting-like operations.

- Provides: slot direction configuration (U/D/L/R arrows), progress bar overlay, config slot support, IO slot resolution
- Constructor: `public ProcessingModuleItem(Properties properties)`
- **Must override:**
  - `getModuleConfigKey()` - returns the NBT key for storing slot mode data
  - `processModule()` - your core processing logic (called every tick)
- **Optional overrides:**
  - `getMaxInputs()` - max input slots (default: unlimited)
  - `getMaxOutputs()` - max output slots (default: unlimited)
  - `hasConfigSlot()` - returns `true` to show a center config button (default: `false`)
  - `acceptsConfigSlotItem()` - restricts what items can be placed in the config slot (default: accepts all)
  - `useDefaultConfigSlotClick()` - returns `false` to handle config slot clicks yourself (default: `true`)
  - `getNextMode()` - controls the slot mode cycle (default: DISABLED -> INPUT -> FUEL -> OUTPUT)
  - `getProgressPercent()` - returns 0.0-1.0 for the progress bar overlay

### `LogisticsModuleItem` (extends `BaseModuleItem`, implements `IConfigurableModule`)
**For modules that manage item/liquid flow, filtering, or logistics.** Use this for filter modules, IO modules, liquid piping, redstone logic, etc.

- Provides: face-mode configuration, `IConfigurableModule` interface for custom config screens
- Constructor: `public LogisticsModuleItem(Properties properties)`
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
For modules using `GenericModuleConfigMenu` with custom slots (like FilterModule's 27 filter slots or RedstoneModule's 54-slot circuit grid).

```java
AbstractContainerMenu createMenu(int id, Inventory playerInventory, ItemStack moduleStack, BlockPos blockPos)
Component getDisplayName(ItemStack moduleStack)
int getModuleSlots()  // number of custom slots to add
```

**Screen extensibility:** When creating a custom screen for your module, extend `GenericModuleConfigScreen` instead of `AbstractContainerScreen`. This gives you the standard back button, filter mode toggle, and background rendering for free. Then add your custom overlays in the `render()` method. The `RedstoneConfigScreen` in the built-in mod demonstrates this pattern: it extends `GenericModuleConfigScreen` and adds rotation indicators and wall attachment borders on top.

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
String getTextInputLabel()   // e.g., "Custom Name"
String getTextInputHint()    // e.g., "Enter a name..."
int getMaxTextLength()       // max characters allowed
String getExistingText()     // current stored text (for pre-filling)
void setText(String text)    // called when player saves text
```

**No custom screen code is needed.** `MachineInfoScreen` automatically provides the inline panel when it detects an `ITextInputModule` in a config slot. Just make sure `hasConfigSlot()` returns `true`.

### `ICustomScreenProvider`
For modules that render custom inline overlays directly on the machine UI.

```java
void renderCustomScreen(GuiGraphics graphics, int x, int y, ItemStack stack, MachineFrameworkBlockEntity blockEntity)
```

---

## 6. Creating a Processing Module

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

**Important:** `tickModule()` is automatically called for all `BaseModuleItem` instances every server tick. No special registration is needed.

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
| `ServerboundToggleModuleDirectionPacket` | Cycles slot modes (U/D/L/R) |
| `ServerboundSetModuleConfigItemPacket` | Sets a config or filter item in NBT |
| `ServerboundSetModuleTextPacket` | Sets text on any `ITextInputModule` |
| `ServerboundOpenModuleConfigPacket` | Opens a module's config screen from the client |
| `ServerboundToggleIoFacePacket` | Toggles IO face modes |

For custom interactions, register your own packets using NeoForge's networking system.

---

## 13. Example: Complete Module

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
    protected void processModule(Level level, BlockPos pos, MachineFrameworkBlockEntity be,
                                  int slotIndex, ItemStack stack) {
        // Resolve slots
        Map<Direction, SlotMode> modes = getSlotModes(stack, NBT_KEY);
        ResolvedIoSlots resolved = resolveAdjacentIoSlots(be, slotIndex, modes);

        // Collect potion slots and ingredient slot
        List<Integer> potionSlots = new ArrayList<>();
        int ingredientSlot = -1;
        for (Map.Entry<Direction, SlotMode> entry : modes.entrySet()) {
            int slot = getAdjacentSlotIndex(slotIndex, entry.getKey());
            if (slot == -1) continue;
            if (entry.getValue() == SlotMode.INPUT) {
                potionSlots.add(slot);
            }
        }
        // The 4th input slot is the ingredient slot
        // (resolve based on your slot configuration)

        // Read fuel state from module NBT
        int fuelTime = getChannelValue(be, slotIndex, 0);
        int brewTime = getChannelValue(be, slotIndex, 1);
        boolean isBurning = fuelTime > 0;

        // Check ingredient slot for Blaze Powder (fuel) or brewing ingredient
        ItemStack ingredient = be.getItemHandler().getStackInSlot(ingredientSlot);
        if (!ingredient.isEmpty() && ingredient.is(Items.BLAZE_POWDER) && !isBurning) {
            // Load fuel
            ingredient.shrink(1);
            fuelTime = MAX_FUEL_TIME;
            setChannelValue(be, slotIndex, 0, fuelTime);
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
        setChannelValue(be, slotIndex, 1, brewTime);

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
            setChannelValue(be, slotIndex, 0, fuelTime);
            setChannelValue(be, slotIndex, 1, brewTime);
        }
        be.setChanged();
    }

    // Helper methods for potion type checking and extraction
    // (similar to BrewingModuleItem helpers)
}
```

### Step 3: Register and Configure

Follow the standard registration steps (sections 7-9 of this guide):

- Register the item with NeoForge
- Create item and overlay textures
- Add to `praeteritum_futurum:machine_modules` and `processing_modules` tags
- Add language keys and a crafting recipe

### Important Notes

- **In-place brewing**: Potions transform in their input slots, so no output slots are needed. This fits within the 4-adjacent-slot limit.
- **Fuel**: Blaze Powder is auto-detected from the ingredient slot. Each powder provides 20 brew batches.
- **PotionContents constructor**: Always pass `Optional.empty()` and `Collections.emptyList()` for the custom effects parameter. Passing `null` will cause a crash when serializing potions.
- **Splash and lingering conversions**: If your mod has splash/lingering-style conversions, handle them as item-type changes (like the Brewing Module does with gunpowder and dragon breath), not as separate potion types.

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
