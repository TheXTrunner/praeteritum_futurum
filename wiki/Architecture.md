# Praeteritum Futurum Architecture

This is a code-oriented map of the mod for contributors and module authors. It describes the current implementation, rather than a design proposal. For setup and build commands, see [[Contributing]]; for the public extension workflow, see the [[Module API Guide]].

## Platform and Entry Points

- Minecraft 1.21.1, NeoForge, Java 21, and Gradle.
- `PraeteritumFuturumMod` is the common-side entry point. It registers blocks, block entities, items, menus, recipes, network payload handlers, capabilities, config, and worldgen placement types.
- `PraeteritumFuturumModClient` contains client-only setup, including screens, renderers, and item properties.
- The Java package root is `com.thextrunner.praeteritum_futurum`.

## Package Map

| Package | Responsibility |
|---|---|
| `api.module` | Public interfaces used to configure modules and provide custom menus, screens, and text input. |
| `block`, `block.entity` | Machine block, block entity type, relative-side mapping, machine inventory, module data, persistence, and ticking. |
| `item`, `item.custom` | Item registration and module implementations, including the module base classes. |
| `inventory` | Item-handler adapters for module constraints, config slots, single slots, and read-only views. |
| `network` | Serverbound payloads for machine and module UI actions. Handlers validate the request before changing server state. |
| `screen` | Machine and module menus, screens, and menu-type registration. |
| `recipe` | Custom recipe types and serializers. |
| `worldgen` | Config-driven structure placement and registration. |
| `datagen` | Generated block states, models, tags, language entries, and recipes. |
| `event` | Gameplay event handlers, such as Yool interactions. |
| `loot` | Loot table modifiers, such as the Husk sand drop modifier. |

The central classes to start reading are `MachineFrameworkBlockEntity`, `BaseModuleItem`, `ProcessingModuleItem`, `LogisticsModuleItem`, `EjectionModuleItem`, and `IoModuleItem`.

## Grid vs. World Directions

The mod uses two distinct direction systems that must not be confused:

- **`GridDirection`** — navigates the machine's internal 3×9 grid of slots. It has four values: `UP` (row above), `DOWN` (row below), `LEFT` (slot to the left), `RIGHT` (slot to the right). All direction-button overlays (U/D/L/R), NBT config maps, and adjacent-module detection use `GridDirection`. It lives in `block/GridDirection.java`.

- **`Direction` (Minecraft)** — represents world-facing block directions (`NORTH`, `SOUTH`, `WEST`, `EAST`, `UP`, `DOWN`). Only the IO module uses these, via `RelativeSide`, to determine which real-world block faces interact with the world.

Using `GridDirection` for grid navigation prevents bugs where `Direction.Plane.HORIZONTAL` (which includes `NORTH`/`SOUTH`) is mistakenly used for grid-adjacent slot lookup, since `NORTH`/`SOUTH` are not valid grid directions.

## Slot Mode Cycle Configuration

Each processing module declares which slot modes it supports via `getSupportedModes()`, returning an array like `{DISABLED, INPUT, FUEL, OUTPUT}`. The base `getCycledMode()` method in `ProcessingModuleItem` iterates only over these supported modes, so a Smelting Module never cycles into POTION or INGREDIENT modes that belong to the Brewing Module.

Per-mode slot limits are controlled by `getMaxForMode()`, which delegates to `getMaxInputs()` and `getMaxOutputs()` for INPUT and OUTPUT modes. Subclasses with special modes override `getMaxForMode()` to provide custom limits (e.g., Smelting limits FUEL to 1; Brewing limits INGREDIENT to `getMaxIngredients()`).

This eliminates the need for each subclass to implement custom cycling logic — most modules only override `getSupportedModes()` and optionally `getMaxForMode()`.

## Adjacent Module Detection

Configurable modules that use direction-based configuration (U/D/L/R buttons) need to know what type of module sits adjacent in each grid direction. `BaseModuleItem` provides shared infrastructure for this:

- **`AdjacentModuleType` enum** — classifies modules into `NONE`, `INVENTORY`, `IO`, `FILTER`, `PROCESSING`, and `LOGISTICS`.
- **`getAdjacentSlotIndex()`** — delegates to `GridDirection.getAdjacentSlotIndex()` to compute the grid slot index adjacent to a given slot, returning -1 for grid borders.
- **`getAdjacentModuleType()`** — returns the `AdjacentModuleType` of the module sitting adjacent in a `GridDirection`.
- **`getAllowedAdjacentModuleTypes()`** — abstract hook subclasses override to declare which adjacent types they care about. For example, processing modules return only `INVENTORY`, while the Ejection Module returns both `INVENTORY` and `IO`.
- **`isAdjacentModuleAllowed()` / `hasAllowedAdjacentModule()`** — check if an adjacent module type is relevant for configuration.
- **`getClickedDirection()` / `isInsideButton()`** — shared UI helpers for direction-button click detection with border awareness. Returns a `GridDirection`.

This prevents users from configuring directions toward irrelevant module types (e.g., a processing module pointing at a Filter module) and provides a consistent pattern for any future configurable module.

## NBT Helpers

`BaseModuleItem` provides three static helpers for reading and writing module CustomData NBT, available to all module types:

- **`getCustomTag(stack, key)`** — reads a sub-compound from the module's CustomData NBT. Returns an empty `CompoundTag` if the key doesn't exist.
- **`modifyCustomData(stack, key, action)`** — modifies a sub-compound in CustomData NBT. Uses direct component replacement (not `CustomData.update()`) so client-side UI updates immediately without waiting for a sync packet.
- **`updateStackSize(stack, hasConfig)`** — sets the item's max stack size to 1 when the module has configuration (like a tool), or removes the component when unconfigured.

These were previously only in `LogisticsModuleItem` but are now in `BaseModuleItem` so all modules (Processing, Logistics, and custom) can use them without duplication.

## Direction Toggle Packet Pattern

When a player clicks a direction button on a module overlay, the client sends a `ServerboundToggleModuleDirectionPacket` carrying a `GridDirection`. The server handler delegates to `BaseModuleItem.toggleDirectionMode()`, which subclasses override to cycle their own modes. This is the same pattern used by `getFaceModes()` for IO-face configuration: the packet handler checks `instanceof BaseModuleItem` and delegates to the subclass.

Any configurable module that extends `BaseModuleItem` and overrides `toggleDirectionMode()` will automatically work with the direction-toggle packet — no packet handler changes needed.

## Inline Overlay System

Modules can render custom UI overlays directly on the machine configuration screen. The overlay system is built into `BaseModuleItem` and supports both simple overlays (progress bars, direction buttons) and full inline overlays (custom buttons, close buttons).

**Overlay lifecycle:** When a player clicks a module slot that has an inline overlay, `MachineInfoScreen` calls these methods in sequence:
1. **`opensInlineOverlay()`** — returns `false` by default. Override to return `true` to enable the overlay pipeline.
2. **`isInsideOverlay()`** — defines the clickable area. Base covers only the center slot; subclasses expand for buttons.
3. **`handleCustomClick()`** — handles click events (button clicks, mode cycling).
4. **`renderCustomOverlay()`** — draws the overlay every render frame while open.
5. **`isCloseButton()`** — detects clicks on the close (X) button. All modules use the same standard position above the center slot.

**Overlay slot index:** `BaseModuleItem` provides `getOverlaySlotIndex()` and `setOverlaySlotIndex()`. The overlay renderer calls `setOverlaySlotIndex()` on the `BaseModuleItem` instance — no hardcoded `instanceof` checks in `MachineInfoScreen`.

**Center slot management:** The center slot can serve different purposes. `BaseModuleItem` provides these hooks:
- **`usesCenterSlot()`** — returns `false` by default. When `false`, the overlay renders a dark hatch lid to indicate the slot has no function. When `true`, the module controls the center slot rendering.
- **`allowsCenterSlotInteraction()`** — returns `false` by default. When `true`, clicks on the center slot pass through to normal inventory interaction while the overlay is open (e.g., Jukebox Module's disc slot).
- **`hasConfigSlot()`** — for ghost config items stored in NBT (e.g., Stonecutter's target item).
- **`hasInteractableInventorySlot()`** — opt-in hook for player-accessible inventory slots in the machine UI.
- **`hidesSlotItemInMachineScreen()`** — returns `false` by default. When `true`, `MachineInfoScreen` hides the slot item so the module's overlay fully covers the slot without the item showing through (e.g., Jukebox Module hides the disc so the progress bar overlay is visible).

**UI drawing helpers:** To avoid duplicating rendering code, `BaseModuleItem` and `ProcessingModuleItem` provide protected helpers:
- **`drawCloseButton()`** — standard 16×16 close button with raised border (in `BaseModuleItem`). Position is controlled by the `slotX`/`slotY` parameters passed — most modules use the standard top-left position, but IO Module passes offset coordinates to place it below the center slot.
- **`drawDarkHatchLid()`** — dark gray panel for unused center slots (in `BaseModuleItem`).
- **`drawDirectionButtons()`** — draws all 4 direction buttons with border awareness (in `ProcessingModuleItem`).
- **`drawProcessingButton()` / `drawProcessingButtonWithCheck()`** — single direction button rendering (in `ProcessingModuleItem`).

**Click detection helpers:** `BaseModuleItem` provides `getClickedDirection()` (border-aware direction button hit detection) and `isInsideButton()` (static 16×16 button hit test).

**Example:** The Jukebox Module uses `opensInlineOverlay()`, `isInsideOverlay()` for direction buttons and close button, `usesCenterSlot()` returning `true` for its real disc slot, `allowsCenterSlotInteraction()` for disc pickup, and calls `drawCloseButton()` in its overlay.

## Direction Config Validation

Modules that use `GridDirection`-based configuration (e.g., pointing to an adjacent Inventory or IO module) can have stale configs if the player rearranges modules. `BaseModuleItem` provides automatic validation to reset invalid configs:

- **`validateDirectionConfig()`** — iterates all grid directions and resets any that are no longer valid. A direction is invalid if the adjacent slot is a grid border or holds a disallowed module type (per `getAllowedAdjacentModuleTypes()`).
- **`isDirectionConfigured()`** — subclass hook that checks if a direction has an active config (e.g., a non-DISABLED mode).
- **`resetDirection()`** — subclass hook for client-side reset (clears the NBT config for a direction).
- **`resetDirectionMode()`** — subclass hook for server-side reset (mirrors the client-side reset on the server).
- **`validateAndSyncDirectionConfig()`** — convenience helper that combines collecting configured directions, validating, and sending `ServerboundResetModuleDirectionPacket` for each reset direction. Subclasses call this at the start of their `handleCustomClick()` handler.

The validation runs on click, so stale configs are cleaned up immediately when the player opens the overlay. `ProcessingModuleItem`, `JukeboxModuleItem`, and `EjectionModuleItem` all use this pattern. For external mod developers, override the three hooks and call `validateAndSyncDirectionConfig()` in your click handler.

### Shift-Click Reverse Mode Cycling

All module direction buttons support shift-click to cycle modes in reverse. `BaseModuleItem` provides the `isShiftHeld()` helper that wraps `Screen.hasShiftDown()` for reliable GUI-context shift detection — all subclasses call this instead of duplicating the `Screen.hasShiftDown()` call. The shift state is sent to the server via the packet (`ServerboundToggleModuleDirectionPacket` and `ServerboundToggleIoFacePacket`), so the server handles the cycling direction. Each mode enum implements the `CyclingMode<T>` interface (`api.module.CyclingMode`), which requires both `next()` and `previous()` methods. The `toggleDirectionMode()` hook receives a `shiftHeld` parameter that subclasses use to choose the cycling direction.

All built-in modules support this: Processing, Jukebox, Ejection, and IO modules. For custom modules, implement `CyclingMode<T>` on your mode enum and use the `shiftHeld` parameter in your `toggleDirectionMode()` override.

## IItemHandlerModule API

IO modules can transfer items to and from any module that implements `IItemHandlerModule` (`api.module.IItemHandlerModule`). This interface exposes `getItemHandlerForSide()`, returning an `IItemHandlerModifiable` that the IO module can pull from and push to.

`BaseModuleItem` implements `IItemHandlerModule` and returns `null` by default. Subclasses that need IO module item transfer — like `JukeboxModuleItem` — override `getItemHandlerForSide()` to return a non-null handler. This enables the IO module to feed items into modules that are not Inventory modules.

For external mod developers: implement `IItemHandlerModule` on your module item class and return a non-null `IItemHandlerModifiable` from `getItemHandlerForSide()` to enable IO module item transfer. No need to extend `BaseModuleItem`.

## Interactable Inventory Slot

By default, modules that implement `IItemHandlerModule` cannot have their inventory slots interacted with by the player in the machine UI — items can only be transferred via IO modules or automation. To allow player interaction, a module must opt in by overriding `hasInteractableInventorySlot()` in `BaseModuleItem` to return `true`.

`MachineFrameworkBlockEntity.hasItemHandlerModule()` checks both the `IItemHandlerModule` interface and the `hasInteractableInventorySlot()` hook. Only if both pass does `ModuleAwareSlotItemHandler` allow the player to pick up and place items in that module's slot.

Override `hasInteractableInventorySlot()` to return `true` for modules that need a player-accessible slot (e.g., Jukebox Module's disc slot). The default is `false` to prevent accidental item manipulation in modules that shouldn't be directly interacted with.

## Module Removal Guard

Modules can prevent themselves from being removed via the wrench/config menu while in an active state. `BaseModuleItem` provides the `mayPickupModule()` hook that defaults to `true`. The `ModuleConfigSlotItemHandler.mayPickup()` method checks both the inventory slot emptiness and this hook — both must pass for the module to be removable.

Override `mayPickupModule()` to return `false` while the module is busy (e.g., playing a song, mid-operation). Example: `JukeboxModuleItem` returns `false` while a song is actively playing (progress > 0 and total > 0), locking the module in place until playback stops or finishes. This prevents data loss and orphaned state when rearranging modules.

## Jukebox Module Runtime

The Jukebox Module (`JukeboxModuleItem`) is a playlist manager that plays music discs with sound and the vanilla song title subtitle. It uses a single disc slot that physically holds the disc item, and a configured IO module to build a playlist from connected external inventories.

- **Disc slot** — the module's inventory slot holds one music disc. Placing a disc starts playback; removing it stops the music and the playlist advances to the next disc. The slot rejects non-music-disc items, acting as if it has a built-in Filter module. The player can interact with this slot via the config overlay's center slot.
- **Configuration** — one direction button points to THE IO module. Clicking that button cycles the mode: DISABLED → QUEUE → SHUFFLE. Changing the mode resets the playlist position to 0.
- **Playlist building** — when auto-play is enabled (QUEUE or SHUFFLE), the module reads the configured IO module's OPEN faces, then builds a single ordered playlist from all connected external inventories. Playlist order: face order (UP → FRONT → BACK → LEFT → RIGHT → DOWN) → slot index (0→1→2...). All slots are included (even empty ones) so the order is stable.
- **Queue mode** — plays discs in strict playlist order, looping back to position 0 when finished.
- **Shuffle mode** — searches the playlist in random order when looking for the next disc, but advances sequentially through the shuffled order. Reshuffles when the playlist loops.
- **Disc lifecycle** — the module extracts the disc from the external chest, holds it in its slot while playing with sound and subtitle via `JukeboxSongPlayer`, then returns it after playback completes. Return priority: (1) original slot, (2) any slot in the same inventory, (3) any slot in any other inventory with an OPEN face. If all inventories are full, the module stays idle with the disc in its slot until space opens or the player removes it.
- **Sound and subtitle** — uses `JukeboxSongPlayer` to play the disc's sound and trigger the colorful song title subtitle, matching vanilla jukebox behavior. Each machine creates its own `JukeboxSongPlayer` instance so multiple machines can play different songs simultaneously.
- **Music disc detection** — uses the `c:music_discs` Common Tags tag and `JukeboxPlayable` data component fallback to detect music discs.
- **Removal guard** — overrides `mayPickupModule()` to return `false` while a song is playing, preventing the module from being removed mid-playback.

## Module Object Storage & Cleanup

Modules sometimes need to persist objects across ticks (e.g., song players, timers, connection state) and clean them up when the machine is destroyed. `MachineFrameworkBlockEntity` provides two generic systems so modules manage their own lifecycle without the block entity knowing about module-specific state.

**Module object storage:** `setModuleObject(slotIndex, object)` stores an arbitrary object keyed by module slot index. `getModuleObject(slotIndex)` retrieves it. The block entity does not interpret these objects — they belong entirely to the module. When the machine is destroyed, all stored objects are cleared.

**Module cleanup:** `registerModuleCleanup(slotIndex, runnable)` registers a cleanup action executed when the machine block is destroyed. `runAllModuleCleanups()` is called from `MachineFrameworkBlock.onRemove()` and executes all registered runnables, then clears both the cleanup map and the module object map. Cleanup errors are silently ignored to avoid crashing on machine destruction.

**Example:** The Jukebox Module stores a `JukeboxSongPlayer` instance via `setModuleObject()` so it persists across ticks. This allows the song to be stopped when the disc is removed or the machine is destroyed, and enables the colorful song title subtitle to display correctly.

## Brewing Module Runtime

The Brewing Module (`BrewingModuleItem`) brews potions in-place using Blaze Powder as fuel. It uses 3 potion slots and 1 ingredient slot, with potions transforming in their input slots (no output slots needed).

- **Fuel detection regardless of config** — Blaze Powder is auto-detected from the ingredient slot and consumed as fuel even if no potion slots are configured. This allows the module to fuel up with minimal setup (only ingredient slot needed). The fuel check runs before the "need 3 potion slots" validation, so the module can load fuel independently of brewing operations.
- **Blaze Powder dual purpose** — when fuel is depleted, Blaze Powder in the ingredient slot is consumed as fuel (20 batches per powder). When fuel is already active, Blaze Powder is treated as a brewing ingredient (for Strength potions). This allows Blaze Powder to serve both roles without conflict.
- **In-place brewing** — potions transform in their input slots, so no output slots are needed. This fits within the 4-adjacent-slot limit (3 potions + 1 ingredient).
- **Batch brewing** — all 3 potions brew simultaneously in one batch. The ingredient is consumed once per batch, and fuel decrements by 1 per batch.
- **Progress tracking** — uses module data channels: Channel 0 = fuel time remaining, Channel 1 = total fuel time, Channel 2 = brew progress, Channel 3 = total brew time.

## Ejection Module Runtime

The Ejection Module (`EjectionModuleItem`) ejects items from the machine to the world on each tick. Its `tickModule()` method:

1. **Finds an adjacent IO module** — if none, does nothing. The IO module determines which world-facing directions to eject toward.
2. **Reads the IO module's face config** — gets the `FaceMode` map (OUTPUT, OPEN, INPUT, DISABLED per face). Only `OUTPUT` and `OPEN` faces trigger ejection; `INPUT` faces are ignored.
3. **Finds adjacent Inventory modules** — collects all adjacent inventory slots.
4. **Finds the CATCH slot** (if configured) — a single inventory slot designated to receive items returned by dispenser behaviors.
5. **For each OUTPUT/OPEN IO face:**
   - Finds all inventory slots configured for `DISPENSE` mode (up to 3 slots).
   - Finds all inventory slots configured for `DROP` mode (up to 3 slots).
   - For each DISPENSE slot: delegates to vanilla `DispenseItemBehavior` via reflection. If a CATCH slot is configured, captures any newly spawned `ItemEntity` objects and inserts them into the catch slot. Entities are always discarded from the world after capture attempt — CATCH mode suppresses all item drops.
   - For each DROP slot: if a CATCH slot is configured and can accept the item, routes it directly; otherwise spawns an `ItemEntity` at the machine's face.

**Entity capture mechanism:** Before calling vanilla dispense, the module records all pre-existing `ItemEntity` objects in a 4×4×4 AABB around the machine. After the dispense call, it scans again and captures only entities that didn't exist before. This prevents capturing unrelated item entities already near the machine.

**Overflow voiding:** If the catch slot is full or contains a different item type, the caught item is lost rather than persisting as an entity drop. This is intentional: CATCH mode is opt-in, and the player is responsible for managing the catch slot. DROP mode without CATCH provides a safe fallback where items drop as entities in the world.

**Vanilla behavior delegation:** Uses reflection to access `DispenserBlock.BEHAVIORS` (a `Map<Item, DispenseItemBehavior>`) and creates a `BlockSource` pointing at the machine's position with a fake `DispenserBlock` state facing the ejection direction. This means all current and future dispenser-compatible items work automatically without code changes.

## Machine and Module Runtime

`MachineFrameworkBlockEntity` owns three related pieces of state:

- A 27-slot module grid (`GRID_SIZE`), arranged as three rows by nine columns.
- A 27-slot internal item inventory, with module-aware constraints.
- Four integer data channels for each module slot (`NUM_CHANNELS`). Modules use these channels for small per-slot runtime values such as progress and mode state.

Each server tick processes installed modules. IO input and output attempts run at the configured interval; processing modules run through their module tick hooks. The normal block-entity ticker only runs when Minecraft ticks the machine's own chunk. IO additionally checks that the adjacent target chunk is inside the block-ticking area before requesting its item capability. The mod does not add chunk loading.

Machine state is serialized by `saveAdditional` and restored by `loadAdditional`. The saved data includes modules, internal inventory, module channels, owner UUID, and public/private access state. The block entity exposes its item handler through NeoForge's item-handler capability.

## Menus and Network Updates

The machine configuration menu edits the module grid. Module-specific configuration can use the generic module menu or a module-provided menu/screen. Client UI actions send serverbound payloads registered in `PraeteritumFuturumMod`; packet handlers validate the player and target before applying changes. Block-entity state is synchronized back to clients through block-entity update data.

Keep client-only classes and APIs in the client entry point or client packages so dedicated servers do not load client classes.

## Useful Source Locations

- Registrations: `block/ModBlocks.java`, `block/entity/ModBlockEntities.java`, `item/ModItems.java`, `screen/ModMenuTypes.java`, and `recipe/ModRecipeSerializers.java`.
- Machine state and tick flow: `block/entity/MachineFrameworkBlockEntity.java`.
- Module contracts and examples: `api/module/` and `item/custom/`.
- Network payload registration: `PraeteritumFuturumMod.java`; payload implementations are in `network/`.
- Config defaults and limits: `Config.java`.
