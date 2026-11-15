# Deferred Module Ideas

These are proposals, not implemented features. Check the [project README](../README.md) for the current feature overview, or return to the [documentation index](README.md).

Modules planned for future implementation, organized by release timeline.

---

# Vanilla Modules (Upcoming — pre 1.0.0)

These modules automate existing vanilla Minecraft mechanics and are planned for upcoming builds.

## Trader Module

**GitHub Issue:** #12

**Summary:** Configurable module that inputs emeralds and outputs traded items.

**Proposed mechanics:**
- Requires capturing a villager (somehow) to be placed inside the machine as an item.
- Detects a job station through an IO slot or adjacent module acting as a station.
- Allows rerolling trades with a single button.

**Notes:** Complex implementation — requires villager capture mechanics and job station detection.

## Crafter Module

**GitHub Issue:** #9

**Summary:** Automates the auto-crafting behavior of vanilla Crafter blocks.

**Proposed mechanics:**
- Interfaces with vanilla Crafter auto-crafting behavior.
- May include filtering for inputs and outputs.
- Will likely require a custom UI.

## Piston Module

**GitHub Issue:** #8

**Summary:** Lets the machine push or pull blocks in the world.

**Proposed mechanics:**
- Highly configurable push/pull behavior.
- ~~Needs to determine whether pushing/pulling block entities is possible in Java.~~ They can be pushed now.

**Notes:** High complexity but it is possible now!.

## Redstone Logic Modules

**GitHub Issue:** #7

**Summary:** Add redstone modules to tie together modules with redstone logic and custom circuits.

**Proposed mechanics:**
- Interface with processing modules and IO modules.
- Create redstone logic on block faces and within the grid itself.

## More Logistic Modules

**GitHub Issue:** #6

**Summary:** Modules to handle liquids, piping of items and liquids, and advanced IO/Filter modules.

**Proposed mechanics:**
- Expand automation by utilizing the grid itself as a logistic chain.
- Liquid handling and piping.
- Advanced filtering beyond the current Filter module.

## Dispenser/Dropper Module

**Summary:** Makes the machine act as a vanilla Dispenser or Dropper, projecting items from adjacent inventory slots.

**Proposed mechanics:**
- Uses adjacent IO/INPUT slots as the source of items to dispense.
- Projects items (arrows, snowballs, eggs, firework rockets, etc.) in the direction configured via an adjacent IO module or the module's face setting.
- Dispenser variant can also place blocks, activate levers/buttons, and harvest crops (vanilla dispenser behavior).
- Dropper variant simply drops items as entity drops in the target direction.
- Tick rate or activation can be tied to redstone signals or a configurable delay.

**Notes:** Vanilla behavior only — no custom projectiles or enhanced range. The module could be a single item that toggles between Dispenser and Dropper modes via the config slot.

## Fluid/Block Interaction Module

**GitHub Issue:** #14

**Summary:** A module that creates products from the mix of fluids and blocks.

**Proposed mechanics:**
- Pulls fluids from tank modules to interact with items in inventory slots.
- Examples: lava + water = obsidian, water + blue ice = packed ice, etc.
- Cobblestone generator automation.

# Non-Vanilla Modules (Post 1.0.0)

These modules require new items, non-vanilla mechanics, or custom design beyond what vanilla Minecraft provides.

## Fletching Module

**GitHub Issue:** #13

**Summary:** Streamlines the creation of arrows and tipped arrows.

**Proposed mechanics:**
- Inputs materials or potions (as fuel/charge) and outputs arrows/tipped arrows.
- Needs non-vanilla design — vanilla fletching table has no meaningful use beyond villager trading.

**Ideas for non-vanilla extensions:**
- Bulk arrow crafting with custom recipes (e.g., tipped arrows from potions + feathers)
- Spectral arrow crafting from glow ink sacs
- Custom arrow types (explosive, piercing, etc.) if other mods add them
- Feather/stick processing (splitting, sharpening)

## Smithing Table

**Summary:** Deferred — niche use case.

**Reason:** Players only use the Smithing Table a couple of times per playthrough (diamond to netherite upgrade, occasional armor trims). Not worth automating unless given additional uses.

**Ideas for extended use:**
- Custom upgrade materials beyond netherite
- Modular armor enhancement system
- Tool/weapon upgrade paths (e.g., adding enchantment-like properties through materials)
- If the mod adds new armor tiers or materials, Smithing Table becomes more relevant

## Block Placer / Block Breaker Module

**GitHub Issue:** #10

**Summary:** Places and breaks blocks in the world for structure building automation.

**Proposed mechanics:**
- Places/breaks blocks according to adjacent IO slots.
- Inputs or outputs items in adjacent inventory slots.
- Pairs well with Piston modules for structure building.

**Notes:** Should be released in its own update alongside Piston modules.

## Woodcutter Module

**GitHub Issue:** #11

**Summary:** Converts logs into stripped logs and planks into decorative wood variants.

**Proposed mechanics:**
- Acts as a dedicated wood-based stonecutter.
- Custom recipes for streamlining wood building blocks.

## Loom Module (Non-Vanilla)

**Summary:** Cross-color banner copier that consumes a custom fuel item.

**Reason:** Vanilla already supports banner copying (same base color), so a simple Loom module offers little value.

**Design:**
- **Color Mix item** — crafted by combining all three primary color dyes (red, blue, yellow/lime), acting like a printer cartridge. This is the fuel for the module.
- **Center config slot** — holds the source banner (the blueprint with patterns to copy).
- **Input slot** — accepts blank banners of *any* base color.
- **Output slot** — produces an exact clone of the source banner, but in the base color of the input blank banner.
- **Fuel consumption** — each copy consumes a portion of the Color Mix stack. The amount consumed could scale with the number of patterns on the source banner.

**Implementation notes:**
- Requires a new item (Color Mix) with its own texture, recipe, and registration.
- The module needs to read `DataComponents.BANNER_PATTERNS` from the source banner and apply it to a new banner of a different base color. This requires creating a `BannerPatternLayers` instance from the source's patterns, which is not directly accessible in NeoForge 1.21.1 — may need reflection or a workaround similar to the Brewing module's hardcoded approach.
- The overlay should show a fuel bar for the Color Mix level, similar to the Smelting module.
