# Changelog

All notable changes to Praeteritum Futurum will be documented in this file.

[Project README](README.md) | [Documentation index](docs/README.md)

## [Unreleased]

### Added
- Nothing yet

## [0.1.3] - 2026-11-17

### Added
- **Ejection Module** for ejecting items from the machine to the world
  - Three ejection styles: `DISPENSE` (uses vanilla `DispenseItemBehavior` — arrows fire, snowballs throw, potions splash, eggs throw, spawn eggs spawn, bone meal fertilizes, boats place, buckets collect/place fluids, armor equips mobs, etc.), `DROP` (simple entity drop — items eject as pickups), and `CATCH` (captures item entities spawned by DISPENSE/DROP and routes them to a designated output slot)
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
- **Machine UI click behavior** is now consistent: holding a valid item for a module places it directly; empty hand or invalid item opens the config overlay

## [0.1.2] - 2026-11-15

### Added
- **Blueprint system for copying machine configurations**
- Blacklist mode for Filter Module
- **Brewing Module** for automated potion brewing
  - 3 potion slots with in-place brewing, 1 ingredient slot for both fuel and ingredients
  - Dedicated `POTION` and `INGREDIENT` slot modes for clear labeling
  - Uses a hardcoded vanilla recipe map because the NeoForge `PotionBrewing` API is unavailable for this use in 1.21.1. Mod-added potions are not supported by the built-in module; the [Module API Guide](docs/MODULE-API-GUIDE.md#12-creating-a-custom-brewing-module) describes an add-on approach.
- **Cartography Module** for map cloning, zoom-out, and locking operations
- **Anvil Module** for item repair, enchantment combining, and renaming
  - Text input panel for configuring custom names directly in the machine UI
  - Configurable "too expensive" repair cost threshold via config file
- **Module API** for third-party mod development
  - `BaseModuleItem`, `ProcessingModuleItem`, and `LogisticsModuleItem` constructors made public
  - `ITextInputModule` interface for modules needing text configuration
  - Generic network packets for module text and config screen communication
  - Comprehensive API guide at `docs/MODULE-API-GUIDE.md`
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
