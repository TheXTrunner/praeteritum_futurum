# FAQ

Common questions about Praeteritum Futurum.

## General

### What is Praeteritum Futurum?

A modular automation mod for Minecraft. Instead of dozens of specialized machines, you build custom machines by placing modules in a single Machine Framework block.

### How do I install the mod?

Install NeoForge for Minecraft 1.21.1, download the latest JAR from [GitHub Releases](https://github.com/TheXTrunner/praeteritum_futurum/releases), place it in your mods folder, and launch the game. See [[Getting-Started]] for details.

### Where can I get help?

- [Report bugs](https://github.com/TheXTrunner/praeteritum_futurum/issues) on GitHub Issues
- [Ask questions](https://github.com/TheXTrunner/praeteritum_futurum/discussions) on GitHub Discussions

## Machine Framework

### How do I place modules?

Right-click the Machine Framework with the **Yool wrench** to place modules in the grid. Right-click normally to open the machine UI.

### How do I configure modules?

Left-click a module in the machine UI to open its configuration. Use the direction buttons to point at adjacent inventory slots for input, output, and fuel. Left-click cycles forward; shift-click cycles backward.

### What are the direction buttons?

Each direction button (U/D/L/R) cycles through modes: DISABLED, INPUT, FUEL, OUTPUT. They tell the module which adjacent slots to use for its work.

### How do blueprints work?

Place a configured module in a crafting grid to create a blueprint. Use the blueprint to copy the configuration to another machine. Place a configured module or blueprint alone in any crafting grid to reset it.

### What is the Public/Private toggle?

The Public/Private toggle prevents other players from using or modifying your machine on multiplayer. Set it to Private for anti-grief protection.

### How do IO modules work?

IO modules connect your machine to external chests and hoppers. Place an IO module next to an inventory module, then configure its faces to point at the external block. Items transfer automatically at the configured tick rate.

## Modules

### Which modules are available?

See [[Modules]] for a complete list of all processing, logistics, and special modules.

### Can I create custom modules?

Yes! The mod has a public API for third-party modders. See the [[Module-API-Guide]] for instructions.

### What modules are planned?

Check the [[Module-Roadmap]] for upcoming modules and future features.

## Exploration

### How do I find Ruined Outposts?

Use the Anomaly Compass. Right-click to look at the nearest structure, sneak and right-click to cycle between targets. See [[Exploration]] for more details.

### What are salvage blocks?

Salvage blocks spawn in Ruined Outposts. Mine them with the Salvage Cutter to get Salvage Scrap, Refined Scrap, and sometimes Strange Artifacts.

## Configuration

### Can I change how the mod works?

Yes, the mod has several config options:

- **Yool rotation toggle** - enable or disable Yool wrench rotation behavior
- **IO transfer tick rate** - adjust how frequently IO modules transfer items
- **Anvil max repair cost** - override the "Too Expensive" limit for the Anvil module (0 = no limit)
- **Debug logging** - enable verbose console logging for troubleshooting
- **Structure spawn rate** - control how often Ruined Outposts appear

Find the config file in your `config/` folder after first launch.

## Tips

- Start with a simple setup: one processing module and a few inventory modules
- Use blueprints to copy working configurations to new machines
- Check module directions if something isn't working
- The Yool wrench is needed for both machine building and general block rotation
