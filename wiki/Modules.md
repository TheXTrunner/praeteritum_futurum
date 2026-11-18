# Modules

All modules that can be placed in the Machine Framework's 27-slot grid.

## Processing Modules

Modules that transform items over time.

| Module | Description |
|--------|-------------|
| **Smelting Module** | Furnace-style smelting with fuel support |
| **Blast Module** | Faster ore and metal smelting (100-tick cook time) |
| **Smoker Module** | Faster food cooking (100-tick cook time) |
| **Stonecutter Module** | Instant stonecutter recipes |
| **Composter Module** | Turn items into bone meal |
| **Grindstone Module** | Disenchant items or repair equipment |
| **Cartography Module** | Use cartography table recipes |
| **Anvil Module** | Use anvil recipes |
| **Brewing Module** | Use brewing stand recipes |

## Logistics Modules

Modules that manage item flow, filtering, and storage.

| Module | Description |
|--------|-------------|
| **Inventory Module** | Internal item storage within the framework |
| **IO Module** | Directional input/output routing for adjacent slots |
| **Filter Module** | Whitelist/blacklist item filtering with ghost item containers |

## Special Modules

| Module | Description |
|--------|-------------|
| **Jukebox Module** | Play music discs with playlist support (queue and shuffle modes) |
| **Ejection Module** | Eject items with dispenser or dropper behavior |

## How Modules Work

- Place a module in any slot of the Machine Framework
- Configure direction buttons (U/D/L/R) to point to adjacent modules
- Use IO modules to connect to external inventories
- Use Filter modules to control which items pass through
- Mix and match to create custom automation pipelines

## Blueprint System

Copy configurations across machines:
- Place a configured module in a crafting grid to create a blueprint
- Use the blueprint to replicate the configuration on another machine
- Place a configured module or blueprint alone in any crafting grid to reset it (Data Cleaning recipe)

## More Information

- [[FAQ]] - Common questions about modules and configuration
