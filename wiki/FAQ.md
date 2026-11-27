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

Left-click a module in the machine UI to open its configuration. Use the direction buttons to point at adjacent inventory slots for input, output, etc. Left-click cycles forward; shift-click cycles backward.

### What are the direction buttons?

Each direction button (U/D/L/R) cycles through modes: DISABLED, INPUT, OUTPUT... They tell the module which adjacent inventory slots to use for its work.

### How do blueprints work?

Blueprints are crafted from paper and refined scrap. Shift-right-click a machine to copy its configuration, then right-click an empty machine you can modify to paste it. In Survival, pasting consumes the required clean modules and every component item stored inside copied Redstone Modules. Hold Shift while hovering over the Blueprint in your inventory to see missing materials. Creative pasting is free. Circuit layouts and durable settings are copied, but active pulses and pending timing transitions are cleared.

Place a configured module or Blueprint alone in any crafting grid to clear its saved data. Cleaning a Redstone Module returns its components to your inventory and drops any overflow nearby.

### What is the Public/Private toggle?

The Public/Private toggle prevents other players from using or modifying your machine on multiplayer. Set it to Private for anti-grief protection.

### How do IO modules work?

I/O Modules connect your machine to external chests and hoppers. Place an I/O Module next to an Inventory Module, then configure its faces to point at the external block. Items transfer automatically at the configured tick rate. The same face settings control redstone: INPUT reads world power, OUTPUT exposes the machine's circuit output as weak power, OPEN does both, and DISABLED does neither. See [[Redstone]] for the shared input bus and circuit details.

### How do I use the Redstone Module?

Place redstone components in its 9-by-6 grid, then turn on Config Mode to configure them without moving the items. The [Redstone guide](Redstone) explains controls, component timing, module control, I/O behavior, and known limits.

## Modules

### Which modules are available?

See [[Modules]] for a complete list of all processing, logistics, and special modules.

### Can I create custom modules?

Yes. The mod has a public API for third-party modders. See the [Module API Guide](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/MODULE-API-GUIDE.md) for instructions.

### What modules are planned?

Check the [Module Roadmap](https://github.com/TheXTrunner/praeteritum_futurum/blob/main/docs/MODULE-IDEAS.md) for upcoming modules and future features.

## Exploration

### How do I find Ruined Outposts?

Use the Anomaly Compass. Right-click to look at the nearest structure, sneak and right-click to cycle between targets. See [[Exploration]] for more details.

### What are salvage blocks?

Salvage blocks spawn in Ruined Outposts. Mine them with a pickaxe to get Salvage Scrap, or use the Salvage Cutter to get Refined Scrap and Strange Artifacts. You can craft 9 Salvage Scrap into a Scrap Block, then craft the block back into 9 Salvage Scrap.

## Configuration

### Can I change how the mod works?

Yes, the mod has several config options:

- **Yool rotation toggle** - enable or disable Yool wrench rotation behavior
- **IO transfer tick rate** - adjust how frequently IO modules transfer items
- **Anvil max repair cost** - override the "Too Expensive" limit for the Anvil module (0 = no limit)
- **Debug logging** - enable verbose console logging for troubleshooting
- **Structure spawn rate** - control how often Ruined Outposts appear, note that this setting only works on world creation, not in already created worlds.

Find the config file in your `config/` folder after first launch.

## Tips

- Start with a simple setup: one processing module and a few inventory modules
- Use blueprints to copy working configurations to new machines
- Check module directions if something isn't working
- The Yool wrench is needed for both machine building and general block rotation
