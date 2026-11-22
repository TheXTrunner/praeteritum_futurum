# Quick Start: Your First Machine

This guide walks you through setting up a basic smelting machine in survival. By the end, you'll have an automated setup that smelts ores using the Machine Framework.

## What You'll Build

A simple smelting setup with four modules:
- **Inventory Module** - stores raw ores (input)
- **Inventory Module** - stores fuel
- **Smelting Module** - smelts the ores
- **Inventory Module** - collects the smelted output

## Step 1: Craft the Machine Framework

Craft a Machine Framework block. This is the core block of the mod where you place modules to build custom machines.

![Machine Framework block](images/quickstart-framework-block.png)

## Step 2: Place the Machine Framework

Place the Machine Framework block on the ground. Right-click to open the interface. You'll see a 27-slot grid shaped like a chest inventory (9 columns across, 3 rows down).

![Machine Framework placed](images/quickstart-framework-placed.png)

## Step 3: Place the Modules

Right-click the Machine Framework with the **Yool wrench** to place modules in the grid slots. For this setup, you'll need:
- **3x Inventory Modules** - for storing items and fuel
- **1x Smelting Module** - for smelting

Place them in the grid. A simple layout could look like this:

```
[Inventory] [Smelting] [Inventory]
   (input)    (process)  (output)
```

Put the third Inventory Module somewhere adjacent to the Smelting Module for fuel.

![Modules placed in grid](images/quickstart-modules-placed.png)

## Step 4: Open the Machine UI

Right-click the Machine Framework normally (without the Yool wrench) to open its UI. You'll see the modules you placed in the grid.

![Machine UI open](images/quickstart-machine-ui.png)

## Step 5: Configure the Smelting Module

Left-click on the Smelting Module in the UI to configure it. You'll see direction buttons that let you tell the module where to get its input, fuel, and where to send its output. The Smelting Module uses INPUT, FUEL, and OUTPUT slot modes. Other modules have their own set of modes.

- Set one direction to **INPUT** pointing at the Inventory Module that holds raw ores
- Set another direction to **FUEL** pointing at the Inventory Module that holds fuel
- Set the third direction to **OUTPUT** pointing at the Inventory Module that will collect smelted items

Each direction button cycles through the module's available modes when you left-click. Shift-click to cycle back.

![Smelting Module configured](images/quickstart-module-configured.png)

## Step 6: Load Items and Fuel

- Open the input Inventory Module and place raw ores (like iron ore) in it
- Open the fuel Inventory Module and place coal or other fuel in it
- The output Inventory Module should be empty to collect results

## Step 7: Run the Machine

Once everything is configured and loaded, the Smelting Module will automatically start processing. You should see a progress bar showing the smelting progress. When complete, smelted items will appear in the output Inventory Module.

![Machine running with progress bar](images/quickstart-machine-running.png)

## Understanding the Basics

### How Modules Communicate

Modules need to know which slots to use for their work. Left-clicking a module in the UI lets you configure direction buttons. Each module defines its own slot modes. For example, the Smelting Module uses these modes:
- **INPUT** - pull items from this slot
- **OUTPUT** - send items to this slot
- **FUEL** - pull fuel from this slot
- **DISABLED** - ignore this slot

Other modules define different modes that match what they do. A Stonecutter Module might only need INPUT and OUTPUT, while an IO Module handles external transfers.

### The 27-Slot Grid

The Machine Framework has 27 slots arranged like a chest inventory (9x3). You can place any module in any slot and arrange them however you want. The key is configuring each module to point to the right slots for its specific needs.

### IO Modules for External Connection

To connect your machine to external chests or hoppers, use **IO Modules**. They transfer items between the machine's internal inventory and the outside world.

Place an IO Module in the grid next to an Inventory Module, then configure it to point at the external chest.

## Next Steps

Once you're comfortable with this basic setup, try:

- **Add a Filter Module** - control which items pass through by setting whitelists or blacklists
- **Chain multiple processing modules** - connect a Smelting Module to a Stonecutter Module for multi-step processing
- **Use blueprints** - copy your working configuration to other machines
- **Try other module combinations** - see [[Modules]] for all available options

## Tips

- Start simple. A single processing module with the inventory modules it needs is enough to learn the basics.
- Use the blueprint system to save configurations you like.
- The Public/Private toggle prevents other players from modifying your machine on multiplayer.
- If something isn't working, check that your module directions are set correctly and point to the right slots.
