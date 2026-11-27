# Modules

All modules that can be placed in the Machine Framework's 27-slot grid.

## Processing Modules

Modules that transform items over time. Each uses direction buttons to point to adjacent inventory slots.

| Module | Description | Available Modes |
|--------|-------------|-----------------|
| **Smelting Module** | Furnace-style smelting with fuel support | INPUT, FUEL, OUTPUT |
| **Blast Module** | Faster ore and metal smelting (100-tick cook time) | INPUT, FUEL, OUTPUT |
| **Smoker Module** | Faster food cooking (100-tick cook time) | INPUT, FUEL, OUTPUT |
| **Stonecutter Module** | Instant stonecutter recipes | INPUT, OUTPUT |
| **Composter Module** | Turn items into bone meal | INPUT, OUTPUT |
| **Grindstone Module** | Disenchant items or repair equipment | INPUT, OUTPUT |
| **Cartography Module** | Use cartography table recipes | INPUT, OUTPUT |
| **Anvil Module** | Use anvil recipes | INPUT, OUTPUT |
| **Brewing Module** | Use brewing stand recipes | POTION, INGREDIENT |
| **Jukebox Module** | Play music discs with playlist support | QUEUE, SHUFFLE |

### Mode descriptions

Each processing module declares its own slot modes. Common modes include INPUT and OUTPUT; special modes belong to their owning module classes:

- **INPUT** - pull items from this slot
- **OUTPUT** - send results to this slot
- **FUEL** - pull fuel from this slot (smelting, blasting, smoking only)
- **POTION** - pull potions to brew from this slot (brewing only, up to 3 slots)
- **INGREDIENT** - pull brewing ingredients and fuel (Blaze Powder) from this slot (brewing only, 1 slot; Blaze Powder is auto-detected as fuel when the brewer is out of fuel)
- **QUEUE** - play discs in order from the connected IO module (jukebox only)
- **SHUFFLE** - play discs in random order from the connected IO module (jukebox only)

Any direction not yet assigned to a mode will simply be ignored.

## Logistics Modules

Modules that manage item flow, filtering, and storage.

| Module | Description | Available Modes |
|--------|-------------|-----------------|
| **Inventory Module** | Internal item storage within the framework | None (simple storage) |
| **IO Module** | Directional item routing and world-redstone input/output | INPUT, OUTPUT, OPEN, DISABLED |
| **Filter Module** | Whitelist/blacklist item filtering with ghost item containers | None (configured via screen) |
| **Ejection Module** | Eject items with dispenser or dropper behavior | DISPENSE, DROP, CATCH |

### Mode descriptions

- **IO Module** - the configuration shows 6 buttons, each representing one physical face of the machine block. These settings route both items and redstone signals:
  - **Up** - the top face of the machine
  - **Down** - the bottom face of the machine
  - **Left** - the left face relative to the machine's front
  - **Right** - the right face relative to the machine's front
  - **Front** - the front face of the machine (the side with the block facing)
  - **Back** - the back face of the machine
  - Each face can be set to a mode independently:
    - **INPUT** - pull items from adjacent inventories and accept redstone from that world face
    - **OUTPUT** - push items to adjacent inventories and emit the circuit output as weak redstone power from that face
    - **OPEN** - allow item and redstone signals in both directions
    - **DISABLED** - no item or redstone transfer on that face
    - Faces not yet assigned will simply be ignored

- **Ejection Module** - direction buttons adjacent to inventory slots:
  - **DISPENSE** - use dispenser behavior (arrows shoot, snowballs throw, etc.)
  - **DROP** - drop items as entity drops
  - **CATCH** - capture returned item entities and route them to this inventory slot (ex. shears + sheep = wool)
  - Directions not yet assigned will simply be ignored.

## Redstone Module

The Redstone Module provides a 9-by-6 circuit grid with vanilla-inspired components, timed behavior, and signal connections to the world through I/O Modules. See [[Redstone]] for component behavior, Config Mode controls, I/O signal routing, and known limits.

## Redstone Control

The control button in a module's machine configuration overlay sets how its normal tick responds to redstone:

- **IGNORE** - run normally, without checking redstone. This is the default.
- **HIGH** - run while the strongest eligible signal is above 0.
- **LOW** - run while the strongest eligible signal is 0.
- **PULSE** - available on the Ejection Module only; queue one ejection pass for each off-to-on signal change. A held signal does not retrigger it.

Regular processing modules support IGNORE, HIGH, and LOW, except the Jukebox. The Ejection Module supports all four options. A paused processing module keeps its progress and items; I/O transfers are a separate machine phase and can continue.

Control signals combine the strongest world input from I/O faces set to INPUT or OPEN with the facing-edge output of any immediately adjacent Redstone Module. No redstone component grid is needed for simple direct world-signal control. The basic Filter, I/O, Inventory, Redstone, and Jukebox modules do not use this control button.

## How Modules Work

- Place a module in any slot of the Machine Framework by opening it with the Yool wrench by right-clicking
- Right-Click the machine with an empty hand and click on the corresponding module to configure direction buttons (U/D/L/R) to point to adjacent modules
- Use IO modules to connect to external inventories (these extract and push items from/to adjacent inventory modules)
- Use Filter modules to control which items pass through
- Mix and match to create custom automation pipelines

## Blueprint System

Copy configurations across machines without reconfiguring each module:

- **Copy:** Shift-right-click a configured machine with a blank blueprint to save its layout and module settings.
- **Paste:** Right-click an empty machine you can modify with a filled blueprint. In Survival, it consumes the required clean module items and the exact items stored inside Redstone Module grids. Creative pasting is free. A failed paste should leave both the machine and inventory unchanged.
- **Reset:** Place a configured module or blueprint by itself in any crafting grid to clear its saved data with the Data Cleaning recipe. Components inside a Redstone Module return to your inventory, with overflow dropped nearby.

The normal Blueprint tooltip summarizes its contents. Hold Shift over it in your inventory to see only the materials you are missing, including embedded redstone components. Circuit layouts and durable settings are copied, but in-progress pulses and pending timing transitions are cleared when pasted.

## More Information

- [[FAQ]] - Common questions about modules and configuration
