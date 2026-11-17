# Praeteritum Futurum

[![CurseForge](https://cf.way2muchnoise.eu/short/praeteritum-futurum.svg)](https://www.curseforge.com/minecraft/mc-mods/praeteritum-futurum)<br>
[![Modrinth](https://img.shields.io/modrinth/dt/praeteritum-futurum?label=modrinth)](https://modrinth.com/mod/praeteritum-futurum)<br>
[![Build](https://github.com/TheXTrunner/praeteritum_futurum/actions/workflows/build.yml/badge.svg)](https://github.com/TheXTrunner/praeteritum_futurum/actions/workflows/build.yml)

A modular machine and logistics mod for Minecraft on NeoForge. Build custom automation pipelines by configuring a single Machine Framework block with interchangeable modules; no need for dozens of specialized machines.

## Overview

Praeteritum Futurum lets you create fully customizable automation systems. Place a **Machine Framework** block, then configure each slot with modules to dictate what goes where and how items get processed. Every aspect of item flow is player-defined, from input/output routing to filtering and processing.

The mod also includes exploration elements: discover **Ruined Outpost** structures scattered across the Overworld using the **Anomaly Compass**, and salvage rare materials using specialized tools.

## Features

### Machine Framework
- **27-slot modular grid:** configure each slot with a different module
- **Modular design:** mix and match modules to create custom automation pipelines
- **Blueprint system:** copy configurations across machines for easy replication
- **Public/Private toggle:** anti-grief protection that prevents other players from using or interacting with your machine
- **Data Cleaning recipe:** place a configured module or blueprint alone in any crafting grid to reset it to a clean state

### Modules
| Module | Description |
|--------|-------------|
| **Inventory Module** | Internal item storage within the framework |
| **IO Module** | Directional input/output routing for adjacent slots |
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

### Exploration
- **Ruined Outpost structures** spawning in the Overworld
- **Anomaly Compass** for locating structures via right-click (sneak + right-click to cycle between nearby targets)
- **Salvage Cutter** for mining salvage blocks with increased speed
- **Yool wrench** for rotating directional blocks and hoppers (sneak + right-click on a Machine Framework to safely break and drop it)
- **Salvage Scrap** and **Refined Scrap** crafting materials
- **Strange Artifact** dropped from advanced salvage blocks
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

The [documentation index](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/README.md) is the entry point for project documentation. Common paths:

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
