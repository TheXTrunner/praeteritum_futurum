# Deferred Module Ideas

These are proposals, not implemented features. Check the [project README](../README.md) for the current feature overview, or return to the [documentation index](README.md).

Modules planned for future implementation, organized by release timeline.

---

## Vanilla Modules (Upcoming, pre 1.0.0)

These modules automate existing vanilla Minecraft mechanics and are planned for upcoming builds.

### Trader Module

**GitHub Issue:** [#12](https://github.com/TheXTrunner/praeteritum_futurum/issues/12)

**Summary:** Configurable module that inputs emeralds and outputs traded items.

**Proposed mechanics:**
- Requires capturing a villager (somehow) to be placed inside the machine as an item.
- Detects a job station through an IO slot or adjacent module acting as a station.
- Allows rerolling trades with a single button.

**Notes:** Complex implementation, requires villager capture mechanics and job station detection.

### Crafter Module

**GitHub Issue:** [#9](https://github.com/TheXTrunner/praeteritum_futurum/issues/9)

**Summary:** Automates the auto-crafting behavior of vanilla Crafter blocks.

**Proposed mechanics:**
- Interfaces with vanilla Crafter auto-crafting behavior.
- May include filtering for inputs and outputs.
- Will likely require a custom UI.


### Piston Module

**GitHub Issue:** [#8](https://github.com/TheXTrunner/praeteritum_futurum/issues/8)

**Summary:** Lets the machine push or pull blocks in the world.

**Proposed mechanics:**
- Highly configurable push/pull behavior.
- ~~Needs to determine whether pushing/pulling block entities is possible in Java.~~ They can be pushed now.

**Notes:** High complexity but it is possible now!.

### More Logistic Modules

**GitHub Issue:** [#6](https://github.com/TheXTrunner/praeteritum_futurum/issues/6)

**Summary:** Modules to handle liquids, piping of items and liquids, and advanced IO/Filter modules.

**Proposed mechanics:**
- Expand automation by utilizing the grid itself as a logistic chain.
- Liquid handling and piping.
- Advanced filtering beyond the current Filter module.

#### Entity Inventory Detection

**Summary:** Allow IO modules to detect and interact with entity inventories (e.g., minecarts with chests, hoppers on minecarts) in addition to block inventories.

**Proposed mechanics:**
- IO modules query entity capabilities at adjacent positions, not just block capabilities.
- Supports minecarts with chests, hoppers on minecarts, and other entity inventories.

**Notes:** See [.github/DEVELOPMENT.md](../.github/DEVELOPMENT.md#entity-inventory-detection) for implementation details.

### Fluid/Block Interaction Module

**GitHub Issue:** [#14](https://github.com/TheXTrunner/praeteritum_futurum/issues/14)

**Summary:** A module that creates products from the mix of fluids and blocks.

**Proposed mechanics:**
- Pulls fluids from tank modules to interact with items in inventory slots.
- Examples: lava + water = obsidian, water + blue ice = packed ice, etc.
- Cobblestone generator automation.
- Infinite water source.
- Lava generator (dripstone interaction)

### Apiary Module

**GitHub Issue:** [#17](https://github.com/TheXTrunner/praeteritum_futurum/issues/17)

**Summary:** Converts a bee hive block into an automated honey harvesting system.

**Proposed mechanics:**
- Places a bee hive block as part of the module structure.
- Uses adjacent IO module output faces to determine where bees spawn and pop off from.
- Handles full hive load automatically: the module manages bee population internally.
- **Honey harvesting:** Input an empty bottle, output a honey bottle.
- **Wax harvesting:** Input scissors (consumes durability), output wax (honeycomb block or wax item).

**Notes:** Needs to determine how to handle bee spawning and hive fullness detection. The module should prevent bees from leaving the hive structure and manage the pollination cycle internally.

## Implemented on Dev (Targeting 1.0.0)

These modules are already implemented on the dev branch but not yet released. They are tracked here for planning purposes.

### Ejection Module

**GitHub Issue:** [#15](https://github.com/TheXTrunner/praeteritum_futurum/issues/15)

**Summary:** Dispenses or drops items from the machine's inventory to the world.

**Implemented mechanics:**
- Per-direction mode configuration: DISPENSE or DROP
- Dispense mode shoots items like a dispenser
- Drop mode drops items as entity pickups

**Notes:** See [ARCHITECTURE.md](README.md#ejection-module-runtime) for runtime details.

### Blast Module

**GitHub Issue:** [#18](https://github.com/TheXTrunner/praeteritum_futurum/issues/18)

**Summary:** A variation of the Smelting Module that uses blast furnace recipes, smelting ores and metals faster.

**Implemented mechanics:**
- Uses `RecipeType.BLASTING` for faster cook time (100 ticks vs 200)
- Same INPUT/FUEL/OUTPUT direction structure as Smelting Module

**Notes:** Shares implementation pattern with Smelting and Smoker modules via `FuelProcessingModuleItem` base class.

### Smoker Module

**GitHub Issue:** [#18](https://github.com/TheXTrunner/praeteritum_futurum/issues/18)

**Summary:** A variation of the Smelting Module that uses smoker recipes, cooking food items faster.

**Implemented mechanics:**
- Uses `RecipeType.SMOKING` for faster cook time (100 ticks vs 200)
- Same INPUT/FUEL/OUTPUT direction structure as Smelting Module

**Notes:** Shares implementation pattern with Smelting and Blast modules via `FuelProcessingModuleItem` base class.

### Jukebox Module

**GitHub Issue:** [#16](https://github.com/TheXTrunner/praeteritum_futurum/issues/16)

**Summary:** Plays music discs with playlist support, including queue and shuffle modes.

**Implemented mechanics:**
- Accepts music discs as input
- Queue mode plays discs in order
- Shuffle mode randomizes play order
- Discs are consumed after playing

**Notes:** Note Block Module was split to a separate issue [#19](https://github.com/TheXTrunner/praeteritum_futurum/issues/19) for post-1.0.0.

### Block Manipulator Module

**Summary:** Places and breaks blocks in the world with smart orientation and NBT data preservation.

**Implemented mechanics:**
- Per-direction mode configuration: DISABLED, PLACE, or BREAK
- Smart block placement with automatic orientation detection
- NBT data preservation when breaking and placing blocks
- Integration with IO module for block input/output routing
- Support for double-height blocks
- Excludes redstone components from placement

**Notes:** Future documentation on this feature underway

### Redstone Module

**Summary:** A programmable logic circuit with a 6×9 (54-slot) circuit editor for custom redstone logic.

**Implemented mechanics:**
- `RedstoneCircuit` and `RedstoneComponent` API classes for custom circuit-based modules
- Signal propagation, rotation, and wall attachment support
- Custom config screen for circuit editing

**Notes:** Part of the 1.0.0 feature set alongside Block Manipulator and Piston push/pull.

## Non-Vanilla Modules (Post 1.0.0)

These modules require new items, non-vanilla mechanics, or custom design beyond what vanilla Minecraft provides.

### Fletching Module

**GitHub Issue:** [#13](https://github.com/TheXTrunner/praeteritum_futurum/issues/13)

**Summary:** Streamlines the creation of arrows and tipped arrows.

**Proposed mechanics:**
- Inputs materials or potions (as fuel/charge) and outputs arrows/tipped arrows.
- Needs non-vanilla design, vanilla fletching table has no meaningful use beyond villager trading.

**Ideas for non-vanilla extensions:**
- Bulk arrow crafting with custom recipes (e.g., tipped arrows from potions + feathers)
- Spectral arrow crafting from glow ink sacs
- Custom arrow types (explosive, piercing, etc.) if other mods add them
- Feather/stick processing (splitting, sharpening)

### Smithing Table

**Summary:** Deferred, niche use case.

**Reason:** Players only use the Smithing Table a couple of times per playthrough (diamond to netherite upgrade, occasional armor trims). Not worth automating unless given additional uses.

**Ideas for extended use:**
- Custom upgrade materials beyond netherite
- Modular armor enhancement system
- Tool/weapon upgrade paths (e.g., adding enchantment-like properties through materials)
- Future new armor tiers or materials?

### RC Module (Remote Signal)

**Summary:** A module that enables remote signal activation without physical connections.

**Proposed mechanics:**
- Two RC modules can be paired to transmit signals across distance without wire.
- Frequency-based channels allow multiple pairs to operate independently.
- Supports both momentary (pulse) and toggle (latch) signal modes.
- Configurable range limit for balance.

**Notes:** See [.github/DEVELOPMENT.md](../.github/DEVELOPMENT.md#rc-module-remote-signal) for implementation details.

### Woodcutter Module

**GitHub Issue:** [#11](https://github.com/TheXTrunner/praeteritum_futurum/issues/11)

**Summary:** Converts logs into stripped logs and planks into decorative wood variants.

**Proposed mechanics:**
- Acts as a dedicated wood-based stonecutter.
- Custom recipes for streamlining wood building blocks.

### Loom Module (Non-Vanilla)

**Summary:** Cross-color banner copier that consumes a custom fuel item.

**Proposed mechanics:**
- **Color Mix item:** crafted from primary color dyes, acts as fuel for the module.
- **Center config slot:** holds the source banner (blueprint with patterns to copy).
- **Input slot:** accepts blank banners of any base color.
- **Output slot:** produces an exact clone of the source banner in the input banner's base color.
- **Fuel consumption:** each copy consumes Color Mix, scaling with pattern count.

**Notes:** See [.github/DEVELOPMENT.md](../.github/DEVELOPMENT.md#loom-module) for implementation details.

### Note Block Module

**GitHub Issue:** [#19](https://github.com/TheXTrunner/praeteritum_futurum/issues/19)

**Summary:** Automates note block music playing, allowing machines to play melodies via configured sequences.

**Proposed mechanics:**
- Related to [#16](https://github.com/TheXTrunner/praeteritum_futurum/issues/16) (Jukebox Module, now implemented for v0.1.3)
- Note blocks have 12 pitches + 15 instrument types: UI for configuring sequences will need to be intuitive
- How does a player input a melody? (e.g., a "score" item, a config screen with a piano roll, or detecting adjacent note block patterns?)
- Does the module physically place note blocks, or simulate the sound server-side?
- Tempo/rate control: how fast does it cycle through notes?
- Can multiple Note Block modules sync to play harmonies?

**Notes:** Deferred to post-1.0.0 as the second part of the original Jukebox/Note block Module idea.

### Grinder Module

**Summary:** A module that attacks entities in the world based on IO module direction, functioning as an automated entity damager.

**Proposed mechanics:**
- Uses adjacent IO module output faces to determine which direction(s) to target entities.
- Detects entities within range and deals damage at a configurable tick rate.
- Damaged entities drop XP orbs, which the machine collects into its internal inventory.

**Open design questions:**
- **Damage scaling:** Fixed per hit, fuel-based, grid-scaling, or config-item-based?
- **XP capture:** From damage or kills only? Stored as XP bottles or custom item? Output to enchanting module?
- **Targeting rules:** All entities, hostile only, passive only, or filter-configurable?
- **Range and rate:** Configurable or fixed to prevent performance issues?

**Notes:** See [.github/DEVELOPMENT.md](../.github/DEVELOPMENT.md#grinder-module) for implementation details.
