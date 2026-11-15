---
sessionId: session-261113-012609-1upf
---

# Requirements

### Overview & Goals

Add two new module types to the Machine Framework that work together to create cellular automata-like machines:
1. **Block Manipulator Module** — a single module that can both place blocks from inventory into the world AND break blocks in the world and store them in inventory, configured via direction modes (U/D/L/R)
2. **Redstone Module** — programmable logic circuit that controls when other modules activate

These modules tie together with the existing piston system, IO modules, and inventory modules to create machines that can build, deconstruct, and respond to logic signals — like living cellular automata.

### Scope

**In Scope:**
- Block Manipulator ModuleItem with direction configuration (U/D/L/R) and mode configuration (PLACE/BREAK per direction)
- Redstone Module with 6×9 (54-slot) internal circuit editor
- Four redstone components: Wire, Repeater, Comparator, Torch
- Custom menu screen for the redstone circuit editor
- Network packets for circuit editing
- Integration with existing IO modules, inventory modules, and piston system
- Config options for tick rates and behavior

**Out of Scope:**
- Piston Module (deferred to a future iteration — starting with single-face piston behavior)
- Advanced redstone components (pistons, droppers, hoppers, daylight sensors)
- Multi-face piston control
- Fluid/energy handling
- World generation or structure-related features

### User Stories

1. As a player, I want to configure a Block Manipulator Module to break blocks in one direction and store them in inventory so that the machine can automatically harvest blocks from the world.
2. As a player, I want to configure the same Block Manipulator Module to place blocks from inventory in another direction so that the machine can build structures.
3. As a player, I want to configure a Redstone Module with logic gates so that I can control when my block manipulator modules activate.
4. As a player, I want to wire redstone components in a 6×9 grid inside the Redstone Module so that I can create custom logic circuits.
5. As a player, I want the Redstone Module to read inputs from adjacent slots and send outputs to control other modules.

### Functional Requirements

**Block Manipulator Module:**
- Uses direction configuration (U/D/L/R) like processing modules
- Each direction can be configured as: DISABLED, PLACE, or BREAK
- PLACE mode: reads blocks from adjacent inventory modules (INPUT mode) and places them in the world in that direction
- BREAK mode: detects blocks in the world in that direction, breaks them, and stores them in adjacent inventory modules (OUTPUT mode)
- Respects `level.mayPlace()` and block placement rules for placing
- Preserves block entity data when breaking blocks with NBT
- Can be triggered by IO module detection OR by redstone signal from RedstoneModule
- Respects block hardness and break time (configurable speed)

**Redstone Module:**
- Opens a 6×9 circuit editor screen when configured
- Supports four components: Wire, Repeater, Comparator, Torch
- Signal propagation runs every tick (configurable rate)
- Reads redstone inputs from adjacent machine slots
- Outputs redstone signals (0-15 strength) to adjacent slots
- Components are placed by dragging RedstoneComponentItems into the grid
- Circuit state is saved in the module's NBT data

### Non-Functional Requirements

- **Performance**: Redstone signal propagation must not cause lag; limit to 54 slots per module
- **Compatibility**: Must work with existing piston mixin for block entity preservation
- **Configurability**: Tick rates and behavior controlled via Config.java
- **Network**: All circuit edits synced via serverbound packets with distance validation

### Design Rationale: Unified Block Manipulator Module

**Why a single module instead of two separate items?**

The existing codebase follows a clear pattern of **single modules with multiple modes**:

1. **IO Module** (`IoModuleItem.java`): Handles both INPUT and OUTPUT in one module using `FaceMode` enum (DISABLED, INPUT, OUTPUT, OPEN). Each face is configured independently.

2. **Processing Modules** (e.g., `SmeltingModuleItem.java`): Use `SlotMode` enum with multiple modes (DISABLED, INPUT, FUEL, OUTPUT, POTION, INGREDIENT). A single smelting module handles inputs, fuel, and outputs.

3. **No precedent for separate items**: There is no "InputModule" and "OutputModule" — the IO module does both. There is no "InputModule" and "FuelModule" for smelting — one module handles all roles.

**Benefits of the unified approach:**
- **Consistency**: Follows the established pattern of mode-based configuration within a single module
- **Flexibility**: A single module can break blocks in one direction while placing in another simultaneously
- **Efficiency**: Saves a slot in the 3×3 grid (27 slots total) — important for complex machines
- **Simplicity**: One module to craft, configure, and maintain instead of two
- **Extensibility**: Easy to add new modes later (e.g., MOVE, SWAP) without creating new items

**Implementation approach:**
- Extend `ProcessingModuleItem` (like smelting, stonecutter, etc.)
- Add new `ManipulatorMode` enum: DISABLED, PLACE, BREAK
- Override `getNextMode()` to cycle through DISABLED → PLACE → BREAK → DISABLED
- In `processModule()`, check each direction's mode and execute the corresponding action
- Use the existing `resolveAdjacentIoSlots()` pattern to find inventory modules for block storage/retrieval

# Delivery Steps

### ✓ Step 1: Block Manipulator Module
Implement BlockManipulatorModuleItem — a single module that can both place and break blocks, configured via direction modes (U/D/L/R) with each direction set to DISABLED, PLACE, or BREAK.

**New files:**
- `item/custom/BlockManipulatorModuleItem.java` — extends ProcessingModuleItem, handles both placing and breaking blocks based on direction mode

**Modified files:**
- `item/ModItems.java` — register the BlockManipulatorModuleItem
- `block/entity/MachineFrameworkBlockEntity.java` — add `placeBlock()` and `breakBlock()` helper methods for world interaction
- `Config.java` — add tick rate and break speed options

**Implementation details:**
- Uses the ProcessingModuleItem pattern with direction configuration (U/D/L/R)
- Adds `ManipulatorMode` enum: DISABLED, PLACE, BREAK
- Override `getNextMode()` to cycle: DISABLED → PLACE → BREAK → DISABLED
- In `processModule()`, check each direction's mode:
  - **PLACE**: reads blocks from adjacent inventory modules (INPUT mode), places them in the world in that direction
  - **BREAK**: detects blocks in the world in that direction, breaks them, stores them in adjacent inventory modules (OUTPUT mode)
- Uses `level.destroyBlock()` with proper block entity handling via `BlockEntity#saveAdditional`
- Checks for redstone signals from adjacent RedstoneModules before activating
- Config options: `blockModuleTickRate` (default: 5), `blockBreakSpeed` (default: 1.0x)
- Follows the same pattern as `SmeltingModuleItem` for direction-based mode configuration

### ✓ Step 2: Redstone Module with Circuit Editor
Implement RedstoneModuleItem — a module with an internal 6×9 (54-slot) circuit editor where players place redstone components to create custom logic circuits.

**New files:**
- `item/custom/RedstoneModuleItem.java` — extends LogisticsModuleItem, implements IConfigurableModule and IModuleMenuProvider
- `item/custom/RedstoneComponentItem.java` — items representing placeable redstone components (Wire, Repeater, Comparator, Torch)
- `api/module/RedstoneCircuit.java` — data model for the 54-slot circuit grid, handles signal propagation and logic evaluation
- `api/module/RedstoneComponent.java` — enum with component types, signal strength, and state
- `screen/RedstoneConfigMenu.java` — custom menu for the circuit editor (extends BaseMachineContainerMenu)
- `screen/RedstoneConfigScreen.java` — client-side screen rendering the 6×9 grid with component visuals
- `network/ServerboundPlaceRedstoneComponentPacket.java` — network packet for placing components in the circuit

**Modified files:**
- `item/ModItems.java` — register RedstoneModuleItem and 4 RedstoneComponentItems
- `screen/ModMenuTypes.java` — register RedstoneConfigMenu and screen
- `PraeteritumFuturumMod.java` — register the new network packet
- `block/entity/MachineFrameworkBlockEntity.java` — add `getRedstoneSignal(slotIndex)` and `setRedstoneSignal(slotIndex, strength)` methods
- `Config.java` — add `redstoneEvaluationTickRate` (default: 2)

**Implementation details:**
- Circuit grid stored as NBT in the module's CustomData component (54 slots × {componentType, signalStrength, state})
- Signal propagation runs every tick (configurable) in `tickModule`
- Module reads redstone inputs from adjacent slots (via IO modules or direct neighbor signals)
- Outputs sent to adjacent slots as redstone signals (0-15 strength)
- Components:
  - **Wire**: Passes signal from neighbors, decays by 1 per block
  - **Repeater**: Delays signal by configurable ticks (1-4), maintains signal strength
  - **Comparator**: Compares input signals, outputs difference
  - **Torch**: Inverts signal (NOT gate), outputs 15 when input is 0
- Circuit editor screen shows components with color-coded signal strengths
- Components placed by dragging RedstoneComponentItems into grid slots (like FilterModuleItem pattern)
- Integration: BlockManipulatorModule checks for redstone signals from adjacent RedstoneModules

### ✓ Step 3: Use Vanilla Redstone Items
Refactor the Redstone Module to use vanilla redstone items instead of custom component items. Players will place vanilla redstone dust, repeaters, comparators, and torches into the module's 54-slot circuit editor.

**Deleted files:**
- `item/custom/RedstoneComponentItem.java` — abstract base class for custom component items
- `item/custom/RedstoneWireComponentItem.java`
- `item/custom/RedstoneRepeaterComponentItem.java`
- `item/custom/RedstoneComparatorComponentItem.java`
- `item/custom/RedstoneTorchComponentItem.java`

**Modified files:**
- `item/ModItems.java` — remove the 4 custom component item registrations
- `item/custom/RedstoneModuleItem.java` — use vanilla items (Items.REDSTONE, Items.REPEATER, Items.COMPARATOR, Items.REDSTONE_TORCH) instead of custom items
- `datagen/ModItemModelProvider.java` — remove model generators for custom component items
- `datagen/ModLanguageProvider.java` — remove language entries for custom component items
- `datagen/ModRecipeProvider.java` — remove crafting recipes for custom component items

**Implementation details:**
- `isRedstoneComponentItem()` checks against vanilla items: `Items.REDSTONE`, `Items.REPEATER`, `Items.COMPARATOR`, `Items.REDSTONE_TORCH`
- `getComponentFromItem()` maps vanilla items to RedstoneComponent enum
- `getComponentItemStack()` creates stacks from vanilla items instead of custom ones
- No custom item textures needed — vanilla items already have textures

### ✓ Step 4: Redstone Component Interactions
Enhance the Redstone Module circuit editor with three interaction mechanics: left-click to move components, right-click to rotate components, and right-click on empty slots to create wall attachments for torches.

**Modified files:**
- `api/module/RedstoneComponent.java` — add `needsRotation()` helper method
- `api/module/RedstoneCircuit.java` — add rotation array (0-3 for 4 directions), wall attachment tracking, and methods to get/set rotation and wall attachments
- `item/custom/RedstoneModuleItem.java` — rewrite `handleMenuClick()` to support:
  - **Left click (button 0)**: Move component (current behavior — place or swap)
  - **Right click (button 1) on occupied slot**: Rotate component 90° clockwise (for comparators and repeaters)
  - **Right click (button 1) on empty slot**: Toggle wall attachment at that slot (visual indicator like disabled inventory slots)
- `network/ServerboundRotateRedstoneComponentPacket.java` — new packet for rotation updates
- `PraeteritumFuturumMod.java` — register the new rotation packet

**Implementation details:**
- Rotation stored as 0-3 (N/E/S/W) in RedstoneCircuit for each slot
- Wall attachment stored as a Direction enum (UP/DOWN/LEFT/RIGHT) representing which side of the slot the "wall" is on
- Torch components placed on wall attachments act as NOT gates when there's redstone input from the wall side
- Repeaters work as vanilla one-tick repeaters (no delay configuration yet)
- Comparators rotate their inputs/outputs (affecting signal direction in the circuit)
- Signal propagation in `propagateSignals()` uses rotation data for directional components
- Wall attachment shown as a ghost slot overlay (similar to disabled inventory slots in machine mode menu)

### ✓ Step 5: Visual Indicators and Creative Tab
Add visual indicators for rotation and wall attachments in the Redstone Module circuit editor, and register the new items in the creative tab.

**New files:**
- `screen/RedstoneConfigScreen.java` — custom screen for the redstone circuit editor with rotation and wall attachment overlays

**Modified files:**
- `ModCreativeTabs.java` — add BlockManipulatorModule and RedstoneModule to the creative tab
- `screen/ModMenuTypes.java` — register RedstoneConfigScreen for GENERIC_MODULE_CONFIG_MENU (replaces GenericModuleConfigScreen)

**Implementation details:**
- RedstoneConfigScreen extends AbstractContainerScreen and handles both FilterModuleItem and RedstoneModuleItem
- Rotation indicators: small orange text (N/E/S/W) in the bottom-right corner of occupied slots with a dark background box
- Wall attachment indicators: colored 3-pixel borders on the slot edges (green=UP, red=DOWN, blue=LEFT, yellow=RIGHT)
- Uses `guiGraphics.fill()` with blending for semi-transparent colored overlays
- Uses `guiGraphics.drawString()` for rotation text
- FilterModuleItem mode toggle button preserved for compatibility

### ✓ Step 6: Modular Screen Architecture
Refactor the screen hierarchy so `GenericModuleConfigScreen` remains a reusable base class for third-party custom modules, while `RedstoneConfigScreen` extends it for redstone-specific overlays.

**Modified files:**
- `screen/GenericModuleConfigScreen.java` — added Javadoc documenting it as the extensible base class for third-party modules
- `screen/GenericModuleConfigMenu.java` — added overloaded constructor accepting `MenuType<?>` parameter so custom modules can associate their own menu type with the generic config menu
- `screen/RedstoneConfigScreen.java` — refactored to extend `GenericModuleConfigScreen` instead of `AbstractContainerScreen`, removing all duplicated code (back button, filter mode toggle, background rendering)
- `screen/ModMenuTypes.java` — added `REDSTONE_MODULE_CONFIG_MENU` menu type and registered both `GenericModuleConfigScreen` (for generic menu) and `RedstoneConfigScreen` (for redstone menu)
- `item/custom/RedstoneModuleItem.java` — updated to use `REDSTONE_MODULE_CONFIG_MENU` via the new constructor, cleaned up unused imports

**Implementation details:**
- `RedstoneConfigScreen` now only contains redstone-specific overlay rendering (rotation indicators and wall attachment borders)
- `GenericModuleConfigScreen` provides: back button, filter mode toggle button, shulker box background, and tooltip rendering
- Third-party modules can extend `GenericModuleConfigScreen` and register their own `MenuType` with a custom screen
- The `GenericModuleConfigMenu` constructor with `MenuType<?>` parameter enables this extensibility pattern
- `GENERIC_MODULE_CONFIG_MENU` is registered to `GenericModuleConfigScreen` for general use
- `REDSTONE_MODULE_CONFIG_MENU` is registered to `RedstoneConfigScreen` for the redstone module specifically

### ✓ Step 7: Bug Fixes
Fixed two critical bugs that prevented the modules from working in-game.

**Modified files:**
- `item/custom/RedstoneModuleItem.java` — added `IConfigurableModule` interface so the module opens its config screen when clicked
- `item/custom/BlockManipulatorModuleItem.java` — added try-catch in `getManipulatorModes()` to safely handle invalid NBT enum values

**Bug 1: Redstone Module container not opening**
- **Root cause**: `RedstoneModuleItem` implemented `IModuleMenuProvider` but not `IConfigurableModule`. The `MachineInfoScreen.slotClicked()` checks for `IConfigurableModule` to open config screens.
- **Fix**: Added `IConfigurableModule` to the implements clause.

**Bug 2: Block Manipulator Module crash on tick**
- **Root cause**: `getManipulatorModes()` called `ManipulatorMode.valueOf()` on NBT strings that didn't match any enum constant, throwing `IllegalArgumentException`.
- **Fix**: Wrapped `valueOf()` in try-catch that defaults to `DISABLED` for unknown values.

### ✓ Step 8: Redstone UI Layout Fix
Fixed the Redstone Module circuit editor to use a proper double-chest layout (54 slots in 9×6 grid) with correct inventory positioning.

**New files:**
- `screen/RedstoneConfigMenu.java` — dedicated menu extending GenericModuleConfigMenu with double-chest inventory positions (y=140, y=198)

**Modified files:**
- `api/module/RedstoneCircuit.java` — changed grid from 6×9 to 9×6 (GRID_WIDTH=9, GRID_HEIGHT=6) to match vanilla double chest layout; fixed bug in `getAdjacentSlots()` where `slotIndex` was passed instead of `x`
- `screen/GenericModuleConfigMenu.java` — refactored inventory slot addition into `getInventoryYPositions()` override hook and `addPlayerInventory()` method; added `setCarried()` helper using reflection for cursor stack manipulation
- `screen/RedstoneConfigScreen.java` — updated to double-chest dimensions (imageHeight=222), uses double chest GUI texture, updated Javadoc
- `screen/ModMenuTypes.java` — updated REDSTONE_MODULE_CONFIG_MENU to use RedstoneConfigMenu constructor
- `item/custom/RedstoneModuleItem.java` — switched to RedstoneConfigMenu; rewrote `handleMenuClick()` for proper component placement/removal/swap; fixed `mayPickup` to allow component removal; updated grid layout comment

**Implementation details:**
- The 9×6 grid matches the vanilla double chest layout exactly (9 columns × 6 rows = 54 slots)
- Player inventory positioned at y=140 (main) and y=198 (hotbar), matching vanilla double chest
- Component interactions: left-click to place/remove/swap, right-click to rotate or toggle wall attachments
- Signal propagation code uses GRID_WIDTH/GRID_HEIGHT constants, works correctly with new dimensions

### ✓ Step 9: Bug Fixes and IO Module Integration
Fixed critical bugs in the Redstone Module GUI and Block Manipulator Module IO integration.

**Modified files:**
- `screen/RedstoneConfigScreen.java` — fixed GUI texture from non-existent `double_crafting.png` to vanilla `container/chest.png`; fixed `renderBg` to use `leftPos`/`topPos` instead of recalculating coordinates; moved tooltip rendering to after overlays
- `item/custom/RedstoneModuleItem.java` — fixed `handleMenuClick` to properly handle same-component clicks (pick up instead of no-op); added comments for clarity
- `item/custom/BlockManipulatorModuleItem.java` — rewrote `processModule` to read IO module face configurations for world direction detection; falls back to own direction config if no IO modules found

**Bug 1: Redstone Module GUI missing texture**
- **Root cause**: Used `double_crafting.png` which doesn't exist in vanilla Minecraft
- **Fix**: Changed to `container/chest.png` which is the vanilla chest texture

**Bug 2: Redstone Module GUI coordinates wrong**
- **Root cause**: `renderBg` recalculated x/y from screen center instead of using `leftPos`/`topPos` set by parent
- **Fix**: Used `leftPos` and `topPos` for proper positioning

**Bug 3: Block Manipulator not working with IO modules**
- **Root cause**: Only looked for adjacent Inventory Modules, ignoring IO module direction config
- **Fix**: Now reads IO module face modes to determine which world directions to interact with; uses inventory modules for block storage/retrieval

### ✓ Step 10: Block Manipulator IO-Driven Rewrite
Completely rewrote the Block Manipulator Module to be fully IO-module-driven, removing the old ManipulatorMode enum and direction config UI.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — complete rewrite: removed ManipulatorMode enum, direction config UI, and fallback system; now requires adjacent IO module where INPUT face = break blocks, OUTPUT face = place blocks, OPEN face = place then break; uses adjacent inventory modules for block storage; added tooltip explaining requirements

**Implementation details:**
- Module REQUIRES an adjacent IO module to function — no fallback to self-configured directions
- IO module face modes directly control behavior:
  - **INPUT**: breaks blocks in that world direction, stores in adjacent inventory
  - **OUTPUT**: places blocks from adjacent inventory in that world direction
  - **OPEN**: tries to place first, then break
- Break progress tracking uses channel 0 for direction ordinal and channel 1 for progress (fits within 4-channel limit)
- Tooltip explains the module requirements and IO face mode meanings
- Overlay shows "Requires IO Module" text instead of direction buttons

### ✓ Step 11: Fix Redstone UI, Component Storage, and Block Manipulator Indicators
Fix three critical issues: Redstone screen missing texture, component data not persisting, and Block Manipulator missing visual indicators.

**Modified files:**
- `screen/RedstoneConfigScreen.java` — fix the double chest texture rendering (chest.png is only 102px tall, need to use shulker_box texture or stack two chest textures)
- `network/ServerboundReturnToMachineMenuPacket.java` — save the module stack back to the machine's module handler when returning from config screen
- `item/custom/BlockManipulatorModuleItem.java` — add visual indicators showing IO module face modes in the overlay

**Implementation details:**
- Redstone screen: Use shulker_box.png (176x166) as the base texture, or render two chest textures stacked vertically
- Component storage: When the back button is clicked, the module stack needs to be synced back to the machine's module handler before reopening the machine menu
- Block Manipulator: Show colored indicators for each active direction (green for place, red for break) based on adjacent IO module config

### ✓ Step 12: Simplify Redstone Module to Double Chest Pattern
Refactored the Redstone Module to act like a modified double chest — a real 54-slot inventory backed by NBT, where vanilla redstone items can be placed, removed, and swapped using normal inventory mechanics.

**Modified files:**
- `item/custom/RedstoneModuleItem.java` — complete rewrite of inventory system: switched from ghost handler to NBT-backed `ItemStackHandler` stored in "RedstoneInventory" NBT (same pattern as FilterModuleItem); extended `LogisticsModuleItem` instead of `BaseModuleItem` for access to NBT helper methods; simplified `handleMenuClick` to only intercept right-clicks (rotation and wall attachments), letting vanilla slot handling manage all item placement, removal, and swapping; added `syncCircuitFromInventory` to keep RedstoneCircuit metadata in sync with stored items during tick processing; added `handleQuickMove` for shift-click support
- `screen/GenericModuleConfigMenu.java` — enhanced `removed()` to save inventory data from module slots back to the module stack before saving to the machine; added `saveModuleInventory` and `getRedstoneItemHandler` helper methods

**Implementation details:**
- The 54-slot grid is now a real `ItemStackHandler` serialized to NBT, just like a vanilla chest
- Items persist automatically when the menu closes via the `removed()` callback
- `mayPlace` filters to only allow redstone components (dust, repeater, comparator, torch)
- Right-click on occupied slots rotates components; right-click on empty slots toggles wall attachments
- The RedstoneCircuit metadata (rotations, wall attachments) is synced from the inventory during `tickModule`
- Shift-clicking redstone items from player inventory into the grid is supported

### ✓ Step 13: Fix Redstone UI Texture, Debug Logging, and Block Manipulator Logic
Fixed three issues: Redstone Module UI texture, verbose debug logging bloating logs, and Block Manipulator logic misunderstanding.

**Modified files:**
- `screen/RedstoneConfigScreen.java` — replaced `shulker_box.png` with vanilla `container/generic_54.png` (the actual double chest texture, 176×222px); updated `renderBg` to blit the full texture instead of stacking partial textures
- `Config.java` — added `enableDebugLogging` boolean config option (default: false) with safe accessor `isDebugLoggingEnabled()`
- `block/entity/MachineFrameworkBlockEntity.java` — gated all LOGGER.info/warn calls behind `Config.isDebugLoggingEnabled()`
- `item/custom/BlockManipulatorModuleItem.java` — gated all LOGGER.debug calls behind `Config.isDebugLoggingEnabled()`
- `item/custom/IoModuleItem.java` — added Config import, gated all LOGGER.debug calls behind `Config.isDebugLoggingEnabled()`
- `mixin/PistonBaseBlockMixin.java` — added `pistonDebug()` and `pistonWarn()` helper methods that gate logging behind config; replaced all 25+ LOGGER calls with the helpers

**Implementation details:**
- The `generic_54.png` texture is the vanilla double chest GUI texture used by all 54-slot inventories
- Debug logging is now completely off by default — players must explicitly enable it in the config
- All existing LOGGER calls are preserved but only execute when debug logging is enabled
- The piston mixin uses helper methods to avoid repeating the config check on every log line
- Block Manipulator logic rewritten: IO module faces now map to adjacent grid slots (UP/DOWN/LEFT/RIGHT), not world directions. Each face mode (INPUT/OUTPUT/OPEN) on an adjacent Inventory Module determines whether that slot is used for breaking or placing blocks. The world direction for placing/breaking is still determined by the face's world direction mapping.

### ✓ Step 14: Block Manipulator Cross Config Rewrite
Rewrite the Block Manipulator Module to use the processing module cross config pattern (U/D/L/R buttons + X close) where each direction checks the adjacent slot type and offers appropriate modes.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — complete rewrite: add `ManipulatorMode` enum (DISABLED, PLACE, BREAK), use `ProcessingModuleItem`'s cross config UI, override `getNextMode()` for mode cycling, check adjacent slot type per direction, remove IO module requirement for config but keep it for world direction lookup, remove progress tracking for instant breaks

**Implementation details:**
- Uses the exact same cross config UI as processing modules (U/D/L/R buttons + X close)
- `ManipulatorMode` enum: DISABLED, PLACE, BREAK with color coding (gray, green, red)
- Each direction checks the adjacent slot at tick time:
  - If adjacent is **Inventory Module**: mode cycles DISABLED → PLACE → BREAK → DISABLED
    - PLACE: picks up a placeable block from that inventory slot and places it in the world
    - BREAK: looks for a block in the world and stores it in that inventory slot
  - If adjacent is **IO Module**: mode cycles DISABLED → ENABLED → DISABLED
    - ENABLED: this direction is active, reads the IO module's face config for world direction
- The IO module's face config (INPUT/OUTPUT/OPEN) determines the world direction for block interaction
- If no IO module is adjacent, the direction itself is the world direction (UP=up, DOWN=down, etc.)
- No progress tracking for block breaking — instant break for testing purposes
- Overlay renders like processing modules with the cross pattern and X button

### ✓ Step 15: Border-Aware Cross Config UI
Modified the cross config UI for all processing modules to hide direction buttons for modules at border positions. For example, a module at slot 0 (top-left corner) only shows "D" (down) and "R" (right) buttons since there are no slots above or to the left.

**Modified files:**
- `item/custom/ProcessingModuleItem.java` — added `hasAdjacentSlot()` method to check if a direction has an adjacent slot, added `overlaySlotIndex` field with getter/setter for border-aware rendering, updated `getProcessingClickedDirection()` and `renderCustomOverlay()` to use slot index
- `item/custom/BlockManipulatorModuleItem.java` — updated `getClickedDirection()` and `renderCustomOverlay()` to use `hasAdjacentSlot()` for border-aware button rendering and click handling
- `screen/MachineInfoScreen.java` — set the overlay slot index on ProcessingModuleItem before rendering the custom overlay

**Implementation details:**
- `hasAdjacentSlot(slotIndex, dir)` checks grid boundaries: UP hides at row 0, DOWN hides at row 2, WEST hides at col 0, EAST hides at col 8
- The `overlaySlotIndex` field stores the current slot index during rendering, set by MachineInfoScreen
- Click handling only checks buttons for directions with adjacent slots
- Rendering skips drawing buttons for directions without adjacent slots
- The "X" close button always shows regardless of position
- Falls back to rendering all buttons if slot index is unknown (third-party overlay compatibility)

### ✓ Step 16: Fix Config Translations and Block Manipulator Direction Sync
Fixed two critical bugs: config UI showing raw translation keys and Block Manipulator direction config being lost on world load.

**Modified files:**
- `datagen/ModLanguageProvider.java` — added 4 missing config translation keys (blockModuleTickRate, blockBreakTime, redstoneEvaluationTickRate, enableDebugLogging)
- `network/ServerboundToggleModuleDirectionPacket.java` — added BlockManipulatorModuleItem handler so direction changes are synced to the server
- Ran `./gradlew runData` to regenerate the language JSON files

**Bug 1: Config UI shows raw keys**
- **Root cause**: The 4 new config options were added to Config.java and ModLanguageProvider.java, but `runData` was never run to regenerate the `en_us.json` file. The generated JSON was missing the translation keys.
- **Fix**: Added the keys to ModLanguageProvider.java and ran datagen to regenerate the JSON.

**Bug 2: Block Manipulator config reset on world load**
- **Root cause**: `ServerboundToggleModuleDirectionPacket` only handled `ProcessingModuleItem`, calling `setSlotMode()` which writes to the wrong NBT key. The BlockManipulatorModule's `getNextMode()` override was never called on the server, so the client-side config change was never synced.
- **Fix**: Added `BlockManipulatorModuleItem` check before `ProcessingModuleItem` in the packet handler, calling `getNextMode()` which uses the correct `setManipulatorMode()` internally.

### ✓ Step 17: Fix Tooltip Crash and Block Placing Logic
Fixed two bugs: tooltip crash when hovering over the Block Manipulator module, and block placing occurring even without an IO module OUTPUT face.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — removed `super.appendHoverText()` call that parsed ManipulatorMode values as ProcessingModuleItem.SlotMode (crash); rewrote `processModule()` to require an adjacent IO module and match Inventory Module modes (PLACE/BREAK) to IO face modes (OUTPUT/INPUT); added `findAdjacentIoModule()`, `findAdjacentInventoryModules()`, and `findInventorySlotByMode()` helper methods; updated class Javadoc

**Bug 1: Tooltip crash — IllegalArgumentException: No enum constant SlotMode.PLACE**
- **Root cause**: `appendHoverText()` called `super.appendHoverText()` which invoked `ProcessingModuleItem.getSlotModes()`, trying to parse "PLACE" from the `SlotMode` enum. `SlotMode` only has INPUT, OUTPUT, FUEL, INGREDIENT, POTION — not PLACE or BREAK.
- **Fix**: Removed the `super.appendHoverText()` call entirely; the Block Manipulator's tooltip is self-contained.

**Bug 2: Block placing on top of the machine without IO OUTPUT**
- **Root cause**: The old `processModule()` code treated an Inventory Module with PLACE mode as a standalone action, placing blocks in the grid direction without checking for an IO module. This meant any PLACE-configured inventory slot would place blocks even with no IO module present.
- **Fix**: Rewrote `processModule()` to require an adjacent IO module. The IO module's face modes now control the action: INPUT face → finds an Inventory Module with BREAK mode, OUTPUT face → finds an Inventory Module with PLACE mode, OPEN face → does both. Without an IO module, the module does nothing.

### ✓ Step 18: Block Manipulator Tooltip and Block Placing Sound
Simplified the Block Manipulator Module's verbose tooltip and added a sound effect when blocks are placed.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — replaced the 8-line verbose tooltip with a simple "Configured" indicator (matching ProcessingModuleItem's pattern); added block placing sound effect using `SoundEvents.ITEM_FRAME_ADD_ITEM` after successful `setBlock()` call

**Implementation details:**
- Tooltip now checks if any direction has a non-DISABLED mode and shows "Configured" in gray, just like processing modules
- Block placing plays a sound effect at the target position using the BLOCKS sound source
- The detailed module documentation is in the class Javadoc instead of the tooltip

### ✓ Step 19: Block Entity Data Preservation
Added block entity data preservation so the Block Manipulator Module can break and place blocks with NBT data (chests, furnaces, Machine Framework blocks, etc.) without losing their contents or configuration.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — updated `breakBlockInSlot()` to capture block entity NBT using `saveCustomOnly()` and store it in the item's `CustomData` component; updated `placeBlockFromSlot()` to restore block entity NBT using `handleUpdateTag()` after placing; switched from `new ItemStack(block)` to `block.getCloneItemStack()` to preserve block state properties (facing, axis, etc.)

**Implementation details:**
- Breaking: `saveCustomOnly(registryAccess)` captures the block entity's custom NBT data, stored under "BlockEntityTag" in the item's CustomData component
- Placing: After `setBlock()`, if the placed block has a block entity and the item contains "BlockEntityTag", the data is restored via `handleUpdateTag()`
- `getCloneItemStack()` preserves block state properties (facing, axis, etc.) instead of creating a plain item stack
- Works with any block entity type: chests retain contents, furnaces retain smelting progress, Machine Framework blocks retain module configs and inventory

### ✓ Step 20: Config Cycling QoL and Block State Restoration
Fixed two issues: config UI locking out cycling for non-IO/Inventory modules, and block state (facing) being lost when placing Machine Framework blocks.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — changed the config click handler so non-IO/Inventory modules can cycle back to DISABLED (instead of being locked out); added `NbtUtils.readBlockState()` to restore the original block state from NBT data when placing, preserving the facing direction of Machine Framework blocks

**Bug 1: Config UI locks out cycling**
- **Root cause**: The `handleCustomClick` method returned early when the adjacent module was neither IO nor Inventory, preventing any cycling action
- **Fix**: Changed the else branch to allow cycling to DISABLED, so players can reset a direction that was previously configured but now has an incompatible module

**Bug 2: Machine placed with wrong facing**
- **Root cause**: `placeBlockFromSlot()` used `block.defaultBlockState()` which creates a fresh block state with default properties, losing the original facing
- **Fix**: Before creating the block state, check if the stored NBT contains a "State" key and use `NbtUtils.readBlockState()` to restore the original block state with all properties (facing, axis, etc.) intact

### ✓ Step 21: Fix Block Entity Drop Duping
Fixed the Block Manipulator Module to prevent duping of modules and items when breaking blocks with block entities.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — added inventory clearing logic in `breakBlockInSlot()` to clear the block entity's inventory handlers before calling `destroyBlock()`, so items don't drop as entities since they're already preserved in NBT

**Bug: Module and item duping when breaking Machine Framework blocks**
- **Root cause**: `level.destroyBlock(targetPos, false)` only prevents the block item from dropping, but the block entity's internal inventory (modules, items) is still dropped by the block's `dropResources()` method. This meant the NBT preserved the data AND the items dropped as entities, effectively duping them.
- **Fix**: After capturing the block entity NBT but before calling `destroyBlock()`, clear the block entity's inventory:
  - For Machine Framework blocks: clear both `moduleHandler` and `inventoryHandler` directly
  - For other block entities (chests, furnaces, etc.): use the `ItemHandler.BLOCK` capability to extract all items
- The cleared items are not lost — they're already captured in the NBT data stored in the block item's `CustomData` component

### ✓ Step 22: Fix Block State Preservation for Facing Direction
Fixed the Block Manipulator Module to preserve the block state (facing, axis, etc.) so placed machines retain their original orientation instead of always facing north.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — added `NbtUtils.writeBlockState()` in `breakBlockInSlot()` to capture the full block state separately from block entity data, stored under "BlockState" in CustomData; updated `placeBlockFromSlot()` to read the "BlockState" key and restore the original block state using `NbtUtils.readBlockState()`

**Bug: Placed machines always face north**
- **Root cause**: `saveCustomOnly()` only captures block entity custom data (inventory, modules), not block state properties (facing, axis). The "State" key was never present in the block entity NBT, so `placeBlockFromSlot()` fell back to `defaultBlockState()` which creates a north-facing block.
- **Fix**: Capture the block state separately using `NbtUtils.writeBlockState(targetState)` and store it in a "BlockState" key in the item's CustomData. During placement, read this key first to restore the original block state with all properties intact.

### ✓ Step 23: Fix Block Entity Inventory Preservation
Fixed the Block Manipulator Module to capture and restore the inventory of block entities like chests, furnaces, etc. so their contents are preserved when broken and placed.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — added manual inventory capture using NeoForge's ItemHandler capability in `breakBlockInSlot()`, storing items in an "Items" NBT tag; added manual inventory restoration using IItemHandlerModifiable in `placeBlockFromSlot()`

**Bug: Chest contents not preserved**
- **Root cause**: `saveCustomOnly()` only captures custom block entity data, not the inventory items stored in vanilla block entities like chests. The inventory was cleared to prevent drops, but never captured into NBT.
- **Fix**: After `saveCustomOnly()`, read the block entity's inventory via the ItemHandler capability and serialize each item stack manually to an "Items" NBT tag. During placement, deserialize this tag and restore the items using IItemHandlerModifiable. Works for any block entity with an ItemHandler capability (chests, furnaces, barrels, etc.).

### ✓ Step 24: Fix Manual Chest Placement Consistency
Fixed the Block Manipulator Module so that chests broken by the machine can also be manually placed by the player with their contents preserved, matching vanilla behavior.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — merged inventory data into the BlockEntityTag instead of a separate "Items" tag, so vanilla block placement restores the inventory; removed the separate inventory restoration code from `placeBlockFromSlot()` since `handleUpdateTag` now handles everything

**Bug: Manual chest placement loses contents**
- **Root cause**: The inventory was stored in a custom "Items" tag inside CustomData that only our machine code read. When a player manually placed the chest item, vanilla Minecraft only read the "BlockEntityTag" and ignored our custom "Items" tag.
- **Fix**: Merge the inventory data (Size + Items list) directly into the BlockEntityTag. This is the same format vanilla chests use, so both machine placement and manual player placement restore the contents correctly.

### ✓ Step 25: Smart Block Placement and Redstone Filter
Implemented player-like block placement logic and redstone component filtering.

**Modified files:**
- `Config.java` — added `allowFloatingBlocks` boolean config option (default: false) with safe accessor `isAllowFloatingBlocks()`
- `item/custom/BlockManipulatorModuleItem.java` — rewrote `placeBlockFromSlot()` with smart placement logic that tries wall mounting first (based on IO facing), then floor, then ceiling; added redstone component filter in `breakBlockInSlot()` to skip redstone-related blocks; added helper methods `trySmartPlacement()`, `createPlacementState()`, `canBlockSurvive()`, and `placeBlockAt()`
- `datagen/ModLanguageProvider.java` — added language keys for the new config option

**Implementation details:**
- Smart placement acts like a player: for wall-mounted blocks (torches, levers, buttons, tripwire hooks), tries to attach to the side facing the machine first, then other horizontal directions
- For floor/ceiling blocks (torches, hanging signs), tries floor placement first, then ceiling
- For regular blocks, uses the IO facing direction for orientation
- If smart placement fails and `allowFloatingBlocks` is enabled, falls back to placing with stored block state (allowing floating blocks for creative builds)
- Redstone components (redstone wire, repeater, comparator, redstone torch, lever, buttons, rails, tripwire) are skipped in break mode — they belong in the Redstone Module
- Blocks that can't survive at a position are dropped as item entities instead of being forcibly placed

### ✓ Step 26: Fix Smart Placement Orientation Bugs
Fixed critical bugs in the smart placement logic that caused blocks to be placed in their default state instead of with proper orientation.

**Modified files:**
- `item/custom/BlockManipulatorModuleItem.java` — rewrote `trySmartPlacement()` to use `List<Direction>` instead of `PlacementAttempt` objects; fixed `createPlacementState()` to only apply `BlockStateProperties.FACING` when the direction is horizontal (N/E/S/W), preventing crashes when worldDirection is UP/DOWN; added horizontal fallback for vertical placement directions; removed unused `PlacementAttempt` inner class

**Bug 1: Vertical directions used as FACING**
- **Root cause**: `createPlacementState()` passed `worldDirection` (which could be UP or DOWN) to `BlockStateProperties.FACING`, which only accepts N/E/S/W. This caused either crashes or incorrect placement.
- **Fix**: Only apply FACING when `facing.getAxis().isHorizontal()` is true. For vertical placement directions, use a horizontal fallback (attachDirection if horizontal, otherwise NORTH).

**Bug 2: Wall-mounted blocks tried UP/DOWN directions**
- **Root cause**: Wall-mounted blocks (torches, levers, buttons) were added to the attempt list with `attachDirection`, which could be UP or DOWN if the machine was above/below the target.
- **Fix**: Filter `attachDirection` to only include it if it's horizontal, since wall-mounted blocks can't face UP/DOWN.