# Praeteritum Futurum

[![CurseForge](https://cf.way2muchnoise.eu/short_1774651.svg)](https://www.curseforge.com/minecraft/mc-mods/praeteritum-futurum)
[![Modrinth](https://img.shields.io/modrinth/dt/praeteritum-futurum?label=modrinth)](https://modrinth.com/mod/praeteritum-futurum)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-blue.svg)](https://minecraft.net)<br>
[![Build](https://github.com/TheXTrunner/praeteritum_futurum/actions/workflows/build.yml/badge.svg)](https://github.com/TheXTrunner/praeteritum_futurum/actions/workflows/build.yml)

A modular machine, logistics, and redstone mod for Minecraft on NeoForge. Build custom automation pipelines by configuring a single Machine Framework block with interchangeable modules, then add redstone logic to control processing and connect machines.

## Overview

Praeteritum Futurum lets you create customizable automation systems. Place a **Machine Framework** block, then configure each slot with modules to dictate what goes where and how items get processed. Every aspect of item flow is player-defined, from input/output routing to filtering and processing. The Redstone Module adds an editable 2D circuit grid, and selected modules can be controlled by signals from the grid or from configured I/O faces.

The mod also includes exploration elements: discover **Ruined Outpost** structures scattered across the Overworld using the **Anomaly Compass**, and salvage rare materials using specialized tools.

## Features

### Machine Framework
- **27-slot modular grid:** configure each slot with a different module
- **Modular design:** mix and match modules to create custom automation pipelines
- **Blueprint system:** copy configured modules across machines; Survival pasting consumes the required clean modules and embedded redstone components, while Creative pasting is free
- **Public/Private toggle:** anti-grief protection that prevents other players from using or interacting with your machine
- **Data Cleaning recipe:** clears a module's or blueprint's saved configuration; components inside a Redstone Module return to your inventory, and any overflow drops nearby

### Modules
| Module | Description |
|--------|-------------|
| **Inventory Module** | Internal item storage within the framework |
| **IO Module** | Directional item routing and world redstone signal input/output |
| **Filter Module** | Whitelist/blacklist item filtering with ghost item containers |
| **Smelting Module** | Furnace-style smelting with fuel support |
| **Stonecutter Module** | Instant stonecutter recipes |
| **Composter Module** | Turn items into bone meal |
| **Grindstone Module** | Disenchant items or repair equipment |
| **Cartography Module** | Use cartography table recipes |
| **Anvil Module** | Use anvil recipes |
| **Brewing Module** | Use brewing stand recipes |
| **Blast Module** | Faster ore and metal smelting (100-tick cook time) |
| **Smoker Module** | Faster food cooking (100-tick cook time) |
| **Jukebox Module** | Play music discs with playlist support (queue and shuffle modes) |
| **Ejection Module** | Eject items with dispenser or dropper behavior |
| **Redstone Module** | Build 2D redstone circuits, connect adjacent redstone modules, and exchange signals through configured I/O faces |

### Redstone Control
- Build circuits from dust, repeaters, comparators, torches, levers, buttons, observers, redstone blocks, Scrap Block wall cells, and a configurable 0-15 Signal Source
- Configure processing modules and the Ejection Module to ignore signals, run while powered, or run while unpowered; the Ejection Module also supports one ejection pass per rising signal edge
- Use I/O face modes to read world signals or expose circuit output as weak redstone power; adjacent redstone modules in the same machine share aligned circuit borders
- The grid is a 2D, vanilla-inspired system. Vertical dust paths and some vanilla update details are not modeled

### Exploration
- **Ruined Outpost structures** spawning in the Overworld
- **Anomaly Compass** for locating structures via right-click (sneak + right-click to cycle between nearby targets)
- **Salvage Cutter** for mining salvage blocks with increased speed
- **Yool wrench** for rotating directional blocks and hoppers (sneak + right-click on a Machine Framework to safely break and drop it)
- **Salvage Scrap** and **Refined Scrap** crafting materials
- **Scrap Block** for compacting 9 Salvage Scrap into one block, reversible back into 9
- **Strange Artifact** dropped from advanced salvage blocks

### Miscellaneous
- **Husk sand drops:** Husks have a chance to drop sand on death, reflecting their desert biome origin

### Configuration
- **Yool rotation toggle:** enable/disable Yool wrench rotation behavior
- **IO transfer tick rate:** adjust how frequently IO modules transfer items
- **Anvil max repair cost:** override the "Too Expensive" limit for Anvil module (0 = no limit)
- **Debug logging:** enable verbose console logging for troubleshooting
- **Structure spawn rate:** control how often Ruined Outposts appear
- **Husk sand drop:** enable/disable Husk sand drops and configure drop chance and amount

## Requirements

- **Minecraft:** 1.21.1
- **NeoForge:** Latest recommended build

## Manual Installation

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1
2. Download the latest release JAR from [GitHub Releases](https://github.com/TheXTrunner/praeteritum_futurum/releases)
3. Place the JAR in your `mods/` folder
4. Launch the game

## Documentation

The [documentation index](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/README.md) is the entry point for project documentation. Players can start with the [Redstone Module guide](https://github.com/TheXTrunner/praeteritum_futurum/wiki/Redstone); the [playtest guide](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/REDSTONE-TESTS.md) explains how to check edge cases and report results. Common developer paths:

- To develop the project, start with [CONTRIBUTING.md](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/CONTRIBUTING.md); it links to the [architecture overview](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/ARCHITECTURE.md) and focused guides.
- To create an add-on module, use the [Module API Guide](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/MODULE-API-GUIDE.md).

## Release History and Future Ideas

See [CHANGELOG.md](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/CHANGELOG.md) for release history. Report and track planned work in [GitHub Issues](https://github.com/TheXTrunner/praeteritum_futurum/issues). The [Deferred Module Ideas](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/MODULE-IDEAS.md) page contains proposals, not implemented features.

## Contributing

Contributions, bug reports, and feature requests are welcome!

- **Report bugs:** [Issues Tracker](https://github.com/TheXTrunner/praeteritum_futurum/issues)
- **Report security vulnerabilities:** [Security Policy](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/.github/SECURITY.md)
- **Suggest features:** [Issues Tracker](https://github.com/TheXTrunner/praeteritum_futurum/issues)
- **Discuss:** [GitHub Discussions](https://github.com/TheXTrunner/praeteritum_futurum/discussions)
- **Submit code:** Open a pull request

See [CONTRIBUTING.md](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/CONTRIBUTING.md) for development setup instructions.

## License

This project is licensed under the [MIT License](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/LICENSE).

## Development & Transparency

This project was developed in close collaboration with AI assistance (Google's Gemini and JetBrains' Junie) to design, architect, and debug complex NeoForge mechanics, custom item components, and container synchronization.

Check out the [`Gemini/`](https://github.com/TheXTrunner/praeteritum_futurum/tree/main/Gemini) directory for the complete chronological devlog, including architectural notes, conversation archives, and **timestamped development screenshots**.
