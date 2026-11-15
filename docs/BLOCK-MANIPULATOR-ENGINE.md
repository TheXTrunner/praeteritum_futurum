# Block Manipulator Module — Engine Architecture

[Documentation index](README.md) | [Architecture overview](ARCHITECTURE.md)

This guide documents the internal architecture of the Block Manipulator Module, which places and breaks blocks in the world on behalf of a Machine Framework. It explains the three-class design, the placement algorithm, the breaking algorithm, and how to extend the module with new block types.

## Why Three Classes?

The Block Manipulator was originally a single 3,000+ line class. It was refactored into three focused classes to make the code maintainable and extensible:

| Class | Lines | Responsibility |
|---|---|---|
| `BlockManipulatorModuleItem` | ~530 | Orchestrator: UI, NBT config, tick scheduling, IO/Inventory module discovery |
| `BlockManipulatorPlacementEngine` | ~2,460 | Placement: smart orientation, block-type-specific placement, state creation |
| `BlockManipulatorBreakingEngine` | ~250 | Breaking: instant break, NBT preservation, stacking block handling |

The orchestrator knows *when* and *where* to place or break. The engines know *how*.

## Class Responsibilities

### `BlockManipulatorModuleItem` — Orchestrator

The main module item class. It handles:

- **UI rendering**: overlay buttons (U/D/L/R/X), mode cycling, click handling
- **NBT serialization**: saving and loading `ManipulatorMode` per direction
- **Tick orchestration**: finding adjacent IO and Inventory modules, dispatching to placement/breaking engines
- **Tooltip**: showing whether the module is configured

Key method: `processModule()` — runs every tick, finds IO modules, determines world direction, and calls the appropriate engine.

### `BlockManipulatorPlacementEngine` — Placement

All block placement logic lives here. The entry point is `placeBlockFromSlot()`, which:

1. Extracts block state and block entity NBT from the stored item
2. Merges CONTAINER data component (vanilla 1.21+ inventory storage) into block entity NBT
3. Dispatches to the appropriate special-case handler based on block type
4. Falls back to `trySmartPlacement()` for generic blocks
5. As a last resort, places with stored state if floating blocks are allowed

The placement engine contains:

- **15 special-case `tryPlaceX` methods** for block types that need custom logic
- **`trySmartPlacement()`** — the generic placement algorithm with orientation logic
- **`createPlacementState()`** — creates a `BlockState` with correct properties for a given facing
- **`getWallVariant()`** — maps floor/ceiling blocks to wall-mounted equivalents
- **Utility methods**: `canBlockSurvive()`, `isPositionValidForPlacement()`, `getWallPlacementDirections()`

### `BlockManipulatorBreakingEngine` — Breaking

All block breaking logic lives here. The entry point is `breakBlockInSlot()`, which:

1. Gets the block's clone item stack (preserving block state properties)
2. Adjusts the drop count for stacking blocks (candles, sea pickles, snow, etc.)
3. Preserves block entity data for Machine Framework blocks, shulker boxes, and beehives
4. Inserts the item into the inventory slot
5. Destroys the block and any related blocks (double-height halves, kelp columns)

## Placement Algorithm

### Dispatch Order

`placeBlockFromSlot()` checks block types in this order:

1. **Scaffolding** — stacks vertically or extends horizontally
2. **Double-height blocks** (doors, tall plants) — checks floor support, places both halves
3. **Beds** — places head + foot parts with correct facing
4. **Stacking blocks** (candles, sea pickles, snow, pink petals, turtle eggs) — increases count
5. **Pointed dripstone** — extends chains or starts new from ceiling/floor
6. **Wall-attached blocks** (amethyst, glow lichen, vines, sculk vein) — tries all 6 directions
7. **Bell** — floor > ceiling > wall priority
8. **Kelp** — climbs water columns, places in empty water slots
9. **Generic smart placement** — orientation-based placement for all other blocks
10. **Floating block fallback** — places with stored state if config allows

### Smart Placement (`trySmartPlacement`)

For blocks not handled by a special-case method, `trySmartPlacement()` categorizes the block and tries orientations in priority order:

| Block category | Placement order |
|---|---|
| Wall-only (ladders, wall signs, tripwire hooks) | Opposite wall → side walls (never the machine) |
| Face-attached (buttons, levers) | Opposite wall → side walls → floor → ceiling |
| Floor/ceiling (torches, lanterns, trapdoors) | Floor (DOWN) → ceiling (UP) |
| Wall-variant blocks (torches, signs, banners, skulls) | Wall variant first, then floor/ceiling |
| Regular blocks (logs, chains, dispensers) | World direction |

The machine's own wall is **never** used as a placement surface — blocks are always placed facing away from the machine.

### State Creation (`createPlacementState`)

Creates a `BlockState` with the correct properties for a given facing direction:

- Restores from stored NBT if present (preserving saved block state)
- Applies `HORIZONTAL_FACING`, `FACING`, `ATTACH_FACE`, `AXIS`, `HANGING`
- Special handling for stairs (HALF), trapdoors (HALF + FACING), slabs (TYPE)
- Applies `WATERLOGGED` based on current fluid state
- For Machine Framework blocks, preserves the saved facing from NBT

## Breaking Algorithm

### Block Type Handling

| Block type | Behavior |
|---|---|
| Bedrock, barrier, portal frames | Skipped — cannot be broken |
| Redstone components | Skipped — belong in Redstone Module |
| Stacking blocks | Extracts all stacked items (candles ×4, snow ×8, etc.) |
| Kelp | Breaks entire column, stores correct count |
| Machine Framework | Preserves all modules, inventory, config via CUSTOM_DATA |
| Shulker boxes | Preserves contents via CONTAINER component |
| Beehives/nests | Uses Silk Touch simulation to preserve bees |
| Double-height blocks | Removes both halves |
| Beds | Removes both head and foot parts |

### NBT Preservation

For Machine Framework blocks, the breaking engine:
1. Writes the block state to NBT (facing, axis, etc.)
2. Saves all machine framework custom data (modules, inventory, config)
3. Attaches both to the drop item via `CUSTOM_DATA` component
4. Clears the machine's inventory to prevent duplicate drops

## Adding Support for a New Block Type

### Step 1: Identify the Block's Placement Rules

Determine how the block behaves in vanilla:
- Does it stack on itself? (candles, snow, sea pickles)
- Does it need a specific attachment surface? (amethyst, glow lichen, vines)
- Is it a multi-block structure? (doors, beds, kelp)
- Does it have special state properties? (dripstone thickness, bell attachment)

### Step 2: Add a Dispatch Check

In `BlockManipulatorPlacementEngine.placeBlockFromSlot()`, add a new `instanceof` check in the dispatch chain:

```java
// New block type: special case — brief description
if (block instanceof NewBlockType) {
    if (tryPlaceNewBlockType(level, targetPos, block, blockStateNbt, blockEntityNbt,
            itemHandler, invSlot, worldDirection, attachDirection)) {
        return;
    }
    // If placement fails, reject entirely (or fall through to smart placement)
    return;
}
```

Place the check in the correct position in the dispatch order — before the generic `trySmartPlacement()` fallback.

### Step 3: Implement the `tryPlaceX` Method

Create a new private static method following the existing pattern:

```java
/**
 * Brief description of what this method does and any special rules.
 */
private static boolean tryPlaceNewBlockType(
        Level level,
        BlockPos targetPos,
        Block block,
        CompoundTag blockStateNbt,
        CompoundTag blockEntityNbt,
        ItemStackHandler itemHandler,
        int invSlot,
        Direction worldDirection,
        Direction attachDirection
) {
    // 1. Check if target position is valid
    if (!isPositionValidForPlacement(level, targetPos, block)) {
        return false;
    }

    // 2. Build placement logic (try directions, check survival, etc.)
    // ...

    // 3. Place the block and consume from inventory
    if (placeBlockAt(level, targetPos, placeState, blockEntityNbt,
            itemHandler, invSlot, block, worldDirection)) {
        return true;
    }

    return false;
}
```

### Step 4: Handle Breaking (if needed)

If the new block type has special breaking behavior (stacking, multi-block, etc.), add handling in `BlockManipulatorBreakingEngine.breakBlockInSlot()`:

```java
// For stacking blocks, set the drop count to match the stacking property
if (block instanceof NewBlockType) {
    int count = targetState.getValue(NewBlockType.COUNT_PROPERTY);
    dropStack.setCount(count);
}
```

### Step 5: Document the New Block Type

Update this document's dispatch order table and block type handling table to include the new block type.

## Future Improvements

### Strategy Pattern (Option B)

The current dispatch chain in `placeBlockFromSlot()` works well but grows linearly with each new block type. A future refactoring could replace the `if (block instanceof X)` chain with a registry of placement strategies:

```java
public interface BlockPlacementStrategy {
    boolean accepts(Block block);
    boolean tryPlace(Level level, BlockPos pos, Block block,
                     CompoundTag blockStateNbt, CompoundTag blockEntityNbt,
                     ItemStackHandler itemHandler, int invSlot,
                     Direction worldDirection, Direction attachDirection,
                     Direction machineFacing);
}
```

Each strategy would be registered at mod load, making it trivial for other mods to add placement support without modifying the core engine. This is the pattern used by mods like Applied Energistics 2 and Create.

### Progress Tracking for Breaking

Currently, block breaking is instant (no progress tracking). Adding progress tracking would make the module more realistic and allow breaking of blocks that require tools or have high hardness.

### Block Entity Data for Vanilla Blocks

Currently, only Machine Framework blocks, shulker boxes, and beehives preserve block entity data. Extending this to all vanilla block entities (chests, furnaces, signs, etc.) would make the module a complete block cloning tool.

## Related Files

- `BlockManipulatorModuleItem.java` — Orchestrator class
- `BlockManipulatorPlacementEngine.java` — Placement engine
- `BlockManipulatorBreakingEngine.java` — Breaking engine
- `IoModuleItem.java` — IO module configuration
- `InventoryModuleItem.java` — Inventory module configuration
- `Config.java` — Tick rate and floating block config
