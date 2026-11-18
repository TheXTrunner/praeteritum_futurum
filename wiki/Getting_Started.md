# Getting Started

## Requirements

- **Minecraft:** 1.21.1
- **NeoForge:** Latest recommended build

## Installation

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1
2. Download the latest release JAR from [GitHub Releases](https://github.com/TheXTrunner/praeteritum_futurum/releases)
3. Place the JAR in your `mods/` folder
4. Launch the game

## What is Praeteritum Futurum?

Praeteritum Futurum lets you create fully customizable automation systems. Place a **Machine Framework** block, then configure each slot with modules to dictate what goes where and how items get processed. Every aspect of item flow is player-defined, from input/output routing to filtering and processing.

The mod also includes exploration elements: discover **Ruined Outpost** structures scattered across the Overworld using the **Anomaly Compass**, and salvage rare materials using specialized tools.

## Machine Framework Basics

- **27-slot modular grid:** configure each slot with a different module
- **Modular design:** mix and match modules to create custom automation pipelines
- **Blueprint system:** copy configurations across machines for easy replication
- **Public/Private toggle:** anti-grief protection that prevents other players from using or interacting with your machine
- **Data Cleaning recipe:** place a configured module or blueprint alone in any crafting grid to reset it to a clean state

## Available Modules

See [[Modules]] for a full list of modules and their descriptions.

## Exploration

- **Ruined Outpost structures** spawning in the Overworld
- **Anomaly Compass** for locating structures via right-click (sneak + right-click to cycle between nearby targets)
- **Salvage Cutter** for mining salvage blocks with increased speed
- **Yool wrench** for rotating directional blocks and hoppers (sneak + right-click on a Machine Framework to safely break and drop it)
- **Salvage Scrap** and **Refined Scrap** crafting materials
- **Strange Artifact** dropped from advanced salvage blocks

## Configuration

The mod includes several config options:

- **Yool rotation toggle:** enable/disable Yool wrench rotation behavior
- **IO transfer tick rate:** adjust how frequently IO modules transfer items
- **Anvil max repair cost:** override the "Too Expensive" limit for Anvil module (0 = no limit)
- **Debug logging:** enable verbose console logging for troubleshooting
- **Structure spawn rate:** control how often Ruined Outposts appear

## Need Help?

- [Report bugs](https://github.com/TheXTrunner/praeteritum_futurum/issues)
- [Ask questions](https://github.com/TheXTrunner/praeteritum_futurum/discussions) on GitHub Discussions
- Check the [[Module API Guide]] if you're a mod developer
