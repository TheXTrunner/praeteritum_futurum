<!--suppress MarkdownUnresolvedLinkLabel -->
# Changelog

All notable changes to Praeteritum Futurum will be documented in this file.

[Project README](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/README.md) | [Documentation index](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/README.md)

## [0.2.1] - 2026-11-27

### Fixed
- Release builds now run data generation before packaging, ensuring generated models, block states, translations, tags, and recipes are included in the mod jar.

## [0.2.0] - 2026-11-27

### Added
- **Scrap Block** - the block form of Salvage Scrap; crafted from 9 Salvage Scrap (reversible). In the redstone circuit grid, it acts as a WALL component that can be powered by adjacent redstone, enabling torch inversion circuits
- **Redstone Module** - a 9×6, 54-slot circuit grid with dust, repeaters, comparators, torches, levers, buttons, observers, redstone blocks, Scrap Block wall cells, and a configurable Signal Source
- **Redstone circuit behavior** - signal strengths and dust connections, comparator compare/subtract modes, side inputs, repeater delays, scheduled comparator and torch transitions, observer pulses, button timing, and attached-torch burnout
- **Redstone module chaining and I/O** - adjacent redstone modules share slot-aligned borders and settle as one circuit; configured I/O faces read world signals and expose the strongest circuit edge output as weak redstone power
- **Configurable Signal Source** - a redstone grid component with an editable 0-15 output (default 0), a numeric tile readout, and a slider with precise step buttons
- **Direct redstone control for eligible processing modules** - `INPUT` and `OPEN` I/O faces can control opted-in modules directly from world redstone, without requiring a redstone module; the Jukebox and basic Filter Module are intentionally excluded
- **Ejection Module redstone control** - ignore redstone, repeat while powered or unpowered, or eject once on each rising edge
- **Blueprint material requirements** - Survival pasting requires clean modules and every component item stored inside Redstone Modules; hold Shift over the blueprint to see missing materials, while Creative pasting is free
- **Clean redstone blueprint copies** - circuit layouts and durable settings are retained, but pending transitions, pulses, locks, and burnout runtime state are reset on paste
- **Data Cleaning component recovery** - components stored inside a Redstone Module are returned to the player's inventory, with overflow dropped nearby
- **Shift-click item transfer** - modules with custom config slots (Filter, Redstone) now support vanilla-like shift-click item transfer between the module's slots and the player's inventory

### Changed
- **Module configuration tooltips** now explain buttons, icons, and modes with colored, state-aware descriptions; slot tooltips are suppressed behind active inline controls
- **I/O module face display** now uses the current machine frame and face textures, with centered color markers for INPUT, OUTPUT, and OPEN modes
- **IO face overlays** now use proper baked models instead of colored quads, improving visual accuracy and making overlay textures easier to maintain
- **Machine Framework blockstate and models** are now hand-crafted resources instead of data-generated, allowing more control over the model output
- **Module configuration** is now persisted correctly when the machine saves, fixing a bug where module changes could be lost
- **Spectator mode** no longer crashes when opening machine menus
- Internal refactoring of network packet handling and client rendering for better maintainability (details in [ARCHITECTURE.md](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/ARCHITECTURE.md))

### Testing
- Multiplayer redstone configuration sync has not yet been verified with multiple clients. Please follow the [redstone client and multiplayer checks](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/REDSTONE-TESTS.md#13-client-and-multiplayer-checks) and report whether open screens stay synchronized and remain isolated to the correct module.

## [0.1.3] - 2026-11-17

### Added
- **Ejection Module** for ejecting items from the machine to the world
  - Three ejection styles: `DISPENSE` (uses vanilla `DispenseItemBehavior` - arrows fire, snowballs throw, potions splash, eggs throw, spawn eggs spawn, bone meal fertilizes, boats place, buckets collect/place fluids, armor equips mobs, etc.), `DROP` (simple entity drop - items eject as pickups), and `CATCH` (captures item entities spawned by DISPENSE/DROP and routes them to a designated output slot)
  - DISPENSE mode delegates entirely to vanilla dispenser logic, so all current and future dispenser-compatible items work automatically
  - CATCH mode captures items returned by dispenser behaviors (e.g., shearing sheep drops wool, bucket collecting returns filled bucket, armor ejection) before they fall to the ground
  - Direction buttons adapt to adjacent module type: inventory modules cycle DISPENSE/DROP/CATCH, IO modules cycle ENABLED/DISABLED
  - Only interacts with `OUTPUT` and `OPEN` IO faces; `INPUT` faces are ignored
  - Requires an adjacent IO module to determine world directions and adjacent Inventory modules for item storage
- **Adjacent-module detection infrastructure** in `BaseModuleItem`
  - `AdjacentModuleType` enum for classifying modules (INVENTORY, IO, FILTER, PROCESSING, LOGISTICS)
  - `getAllowedAdjacentModuleTypes()` hook for configurable modules to declare which adjacent types they care about
  - Shared UI helpers (`getClickedDirection`, `hasAdjacentSlot`, `isInsideButton`) eliminate code duplication across modules
  - Processing modules now detect only Inventory modules, preventing invalid direction configuration
- Generic direction-toggle packet handler for all configurable modules via `BaseModuleItem.toggleDirectionMode()`
- **Blast Module** for faster ore and metal smelting
  - Uses `RecipeType.BLASTING` with 100-tick cook time (faster than Smelting Module)
  - Crafted with a Blast Furnace and Refined Scrap
- **Smoker Module** for faster food cooking
  - Uses `RecipeType.SMOKING` with 100-tick cook time (faster than Smelting Module)
  - Crafted with a Smoker and Refined Scrap
- **Jukebox Module** for automated music disc playback
  - Plays music discs with sound and vanilla song title subtitles
  - Playlist building from connected external inventories via IO modules
  - Queue mode: plays discs in strict order, looping when finished
  - Shuffle mode: shuffles playlist once, plays through, then reshuffles
  - Disc lifecycle: extracts disc from chest, plays it, returns it after playback
  - Disc retention: holds finished disc in module slot when no output inventories are available, auto-returns when a chest appears
  - Single-disc auto-replay when it is the only disc in any connected inventory
  - Uses `c:music_discs` Common Tags for disc detection
  - In the machine UI, holding a music disc and clicking the Jukebox Module slot places it directly; empty hand or invalid item opens the config overlay instead
- **Husk sand drops:** Husks have a chance to drop sand on death, with configurable drop chance and amount

### Changed
- **`GridDirection` enum** replaces raw `Direction` for all grid-adjacent slot navigation
  - Separates grid directions (`UP`, `DOWN`, `LEFT`, `RIGHT`) from world-facing IO directions (`NORTH`, `SOUTH`, `WEST`, `EAST`, `UP`, `DOWN`)
  - Prevents confusion between grid-adjacent slot lookup and world-facing block directions
  - All configurable module overlays, NBT config maps, and adjacent-module detection now use `GridDirection`
- **Slot mode cycle is now declarative** via `getSupportedModes()` in `ProcessingModuleItem`
  - Each module declares which `SlotMode` values it supports (e.g., Smelting uses INPUT/FUEL/OUTPUT; Brewing uses POTION/INGREDIENT)
  - The base `getCycledMode()` iterates only over supported modes, so modules never cycle into modes they don't use
  - Per-mode limits are controlled by `getMaxForMode()`, eliminating duplicate cycling logic across subclasses
- **ProcessingModuleItem NBT save/restore** now uses a stalled-flag guard instead of a slot-empty check, fixing mid-progress module pickup for all processing modules (Smelting, Blast, Smoker, Brewing)
- **MachineFrameworkBlockEntity data channels** increased from 4 to 6 to support module-specific state (e.g., Jukebox stalled flag and shuffle cycle)
## [0.1.2] - 2026-11-15

### Added
- **Blueprint system for copying machine configurations**
- Blacklist mode for Filter Module
- **Brewing Module** for automated potion brewing
  - 3 potion slots with in-place brewing, 1 ingredient slot for both fuel and ingredients
  - Dedicated `POTION` and `INGREDIENT` slot modes for clear labeling
  - Uses a hardcoded vanilla recipe map because the NeoForge `PotionBrewing` API is unavailable for this use in 1.21.1. Mod-added potions are not supported by the built-in module; the [Module API Guide](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/MODULE-API-GUIDE.md#13-creating-a-custom-brewing-module) describes an add-on approach.
- **Cartography Module** for map cloning, zoom-out, and locking operations
- **Anvil Module** for item repair, enchantment combining, and renaming
  - Text input panel for configuring custom names directly in the machine UI
  - Configurable "too expensive" repair cost threshold via config file
- **Module API** for third-party mod development
  - `BaseModuleItem`, `ProcessingModuleItem`, and `LogisticsModuleItem` constructors made public
  - `ITextInputModule` interface for modules needing text configuration
  - Generic network packets for module text and config screen communication
  - Comprehensive API guide at https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/MODULE-API-GUIDE.md
- **Title screen splash texts** for the main menu

### Changed
- **SlotMode enum** extended with `POTION` and `INGREDIENT` modes for the Brewing module
- **Config system** added for runtime-tunable mod settings (Config-driven structure spawn rate is read on world creation only)
  - IO transfer tick rate configuration
  - Yool rotation toggle configuration
  - Anvil Module max repair cost: override the "Too Expensive" limit (0 = no limit)
- Various internal logic improvements to machine and module behavior

## [0.1.1] - 2026-10-30

### Added
- Ruined Outpost structures spawning in the Overworld
- Anomaly Compass for locating structures via right-click
- Salvage Framing and Advanced Salvage custom blocks
- Salvage Cutter and Yool wrench tools for dual inventory mechanics
- Salvage Scrap and Refined Scrap materials
- Strange Artifact dropped from advanced salvage blocks using the cutter
- Machine Framework block entity with 27-slot modular grid
- Inventory Module for accessing internal slots
- Directional IO modules for routing inputs and outputs
- Filter Module with whitelist mode
- Ghost item filter containers for precise inventory control
- Smelting Module, Stonecutter Module, Composter Module, and Grindstone Module
- Data Cleaning recipe for resetting module and blueprint configuration
