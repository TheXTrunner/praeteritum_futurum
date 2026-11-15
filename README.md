# Praeteritum Futurum

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

### Exploration
- **Ruined Outpost structures** spawning in the Overworld
- **Anomaly Compass** for locating structures via right-click
- **Salvage Cutter** and **Yool wrench** tools with dual inventory mechanics
- **Salvage Scrap** and **Refined Scrap** crafting materials
- **Strange Artifact** dropped from advanced salvage blocks

### Configuration
- **Yool rotation toggle:** enable/disable Yool wrench rotation behavior
- **IO transfer tick rate:** adjust how frequently IO modules transfer items
- **Anvil max repair cost:** override the "Too Expensive" limit for Anvil module (0 = no limit)
- **Debug logging:** enable verbose console logging for troubleshooting
- **Structure spawn rate:** control how often Ruined Outposts appear

## Requirements

- **Minecraft:** 1.21.1
- **NeoForge:** Latest recommended build

## Manual Installation

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1
2. Download the latest release JAR from [GitHub Releases](https://github.com/TheXTrunner/praeteritum_futurum/releases)
3. Place the JAR in your `mods/` folder
4. Launch the game

## Documentation

The [documentation index](./docs/README.md) is the entry point for project documentation. Common paths:

- To develop the project, start with [CONTRIBUTING.md](./CONTRIBUTING.md); it links to the [architecture overview](./docs/ARCHITECTURE.md) and focused guides.
- To create an add-on module, use the [Module API Guide](./docs/MODULE-API-GUIDE.md).

## Release History and Future Ideas

See [CHANGELOG.md](./CHANGELOG.md) for release history. Report and track planned work in [GitHub Issues](https://github.com/TheXTrunner/praeteritum_futurum/issues). The [Deferred Module Ideas](./docs/MODULE-IDEAS.md) page contains proposals, not implemented features.

## Contributing

Contributions, bug reports, and feature requests are welcome!

- **Report bugs:** [Issues Tracker](https://github.com/TheXTrunner/praeteritum_futurum/issues)
- **Suggest features:** [Issues Tracker](https://github.com/TheXTrunner/praeteritum_futurum/issues)
- **Submit code:** Open a pull request

See [CONTRIBUTING.md](./CONTRIBUTING.md) for development setup instructions.

## License

This project is licensed under the [MIT License](./LICENSE).

## Development & Transparency

This project was developed in close collaboration with AI assistance (Google's Gemini and JetBrains' Junie) to design, architect, and debug complex NeoForge mechanics, custom item components, and container synchronization.

Check out the [`Gemini/`](./Gemini) directory for the complete chronological devlog, including architectural notes, conversation archives, and **timestamped development screenshots**.
