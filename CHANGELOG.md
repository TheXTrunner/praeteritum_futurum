# Changelog

All notable changes to Praeteritum Futurum will be documented in this file.

[Project README](README.md) | [Documentation index](docs/README.md)

## [Unreleased], 0.1.2, In Development

### Added
- **Public/Private toggle** for anti-grief protection: set a machine to private to prevent other players from using or interacting with it.
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

## [0.1.1]  2026-10-30

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
- Smelting Module, Stonecutter Module, and Composter Module
