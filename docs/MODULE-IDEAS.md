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

### Jukebox / Note Block Module

**GitHub Issue:** [#16](https://github.com/TheXTrunner/praeteritum_futurum/issues/16)

**Summary:** Automates disc playback, with note-block functionality as a possible related feature.

**Proposed direction:** Explore how the machine could start jukebox playback and what useful note-block behavior belongs in a module. The exact mechanics and interface are still open.

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

### Dispenser/Dropper Module

**GitHub Issue:** [#15](https://github.com/TheXTrunner/praeteritum_futurum/issues/15)

**Summary:** Makes the machine act as a vanilla Dispenser or Dropper, projecting items from adjacent inventory slots.

**Proposed mechanics:**
- Uses adjacent IO/INPUT slots as the source of items to dispense.
- Projects items (arrows, snowballs, eggs, firework rockets, etc.) in the direction configured via an adjacent IO module or the module's face setting.
- Dispenser variant can also place blocks, activate levers/buttons, and harvest crops (vanilla dispenser behavior).
- Dropper variant simply drops items as entity drops in the target direction.
- Tick rate or activation can be tied to a configurable delay.

**Notes:** Vanilla behavior only; no custom projectiles or enhanced range. The module could be a single item that toggles between Dispenser and Dropper modes via the config slot.

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
- **Paired activation:** Two RC modules can be paired (e.g., via config slot with a linking item or frequency setting) to transmit signals across distance without wire.
- **Frequency-based channels:** Multiple pairs can operate independently using configurable channels/frequencies to avoid signal interference.
- **Signal types:** Supports both momentary (pulse) and toggle (latch) signal modes.
- **Range:** Unlimited range line-of-sight or configurable range limit for balance.

**Design options:**
- **Standalone module:** A separate RC module item that pairs with another RC module.

**Implementation notes:**
- Requires network packets for pairing synchronization and signal transmission between modules.
- Could use a simple linking item to establish pairs.
- Consider signal delay based on distance for realism, or instant transmission for simplicity.

### Woodcutter Module

**GitHub Issue:** [#11](https://github.com/TheXTrunner/praeteritum_futurum/issues/11)

**Summary:** Converts logs into stripped logs and planks into decorative wood variants.

**Proposed mechanics:**
- Acts as a dedicated wood-based stonecutter.
- Custom recipes for streamlining wood building blocks.

### Loom Module (Non-Vanilla)

**Summary:** Cross-color banner copier that consumes a custom fuel item.

**Reason:** Vanilla already supports banner copying (same base color), so a simple Loom module offers little value.

**Design:**
- **Color Mix item:** crafted by combining all three primary color dyes (red, blue, yellow/lime), acting like a printer cartridge. This is the fuel for the module.
- **Center config slot:** holds the source banner (the blueprint with patterns to copy).
- **Input slot:** accepts blank banners of *any* base color.
- **Output slot:** produces an exact clone of the source banner, but in the base color of the input blank banner.
- **Fuel consumption:** each copy consumes a portion of the Color Mix stack. The amount consumed could scale with the number of patterns on the source banner.

**Implementation notes:**
- Requires a new item (Color Mix) with its own texture, recipe, and registration.
- The module needs to read `DataComponents.BANNER_PATTERNS` from the source banner and apply it to a new banner of a different base color. This requires creating a `BannerPatternLayers` instance from the source's patterns, which is not directly accessible in NeoForge 1.21.1; may need reflection or a workaround similar to the Brewing module's hardcoded approach.
- The overlay should show a fuel bar for the Color Mix level, similar to the Smelting module.
