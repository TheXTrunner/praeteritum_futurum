# Praeteritum Futurum Architecture

[Documentation index](README.md) | [Project README](../README.md) | [Contributing guide](../CONTRIBUTING.md)

This is a code-oriented map of the mod for contributors and module authors. It describes the current implementation, rather than a design proposal. For setup and build commands, see [CONTRIBUTING.md](../CONTRIBUTING.md); for the public extension workflow, see the [Module API Guide](MODULE-API-GUIDE.md).

## Platform and Entry Points

- Minecraft 1.21.1, NeoForge, Java 21, and Gradle.
- `PraeteritumFuturumMod` is the common-side entry point. It registers blocks, block entities, items, menus, recipes, network payload handlers, capabilities, config, and worldgen placement types.
- `PraeteritumFuturumModClient` contains client-only setup, including screens, renderers, and item properties.
- The Java package root is `com.thextrunner.praeteritum_futurum`.

## Package Map

| Package | Responsibility |
|---|---|
| `api.module` | Public interfaces used to configure modules and provide custom menus, screens, and text input. |
| `block`, `block.entity` | Machine block, block entity type, relative-side mapping, machine inventory, module data, persistence, and ticking. |
| `item`, `item.custom` | Item registration and module implementations, including the module base classes. |
| `inventory` | Item-handler adapters for module constraints, config slots, single slots, and read-only views. |
| `network` | Serverbound payloads for machine and module UI actions. Handlers validate the request before changing server state. |
| `screen` | Machine and module menus, screens, and menu-type registration. |
| `mixin`, `accessor` | Piston integration and access to piston moving-block-entity data needed to preserve machine block-entity state. |
| `gametest` | Development GameTest fixtures and regression tests for piston edge cases. |
| `recipe` | Custom recipe types and serializers. |
| `worldgen` | Config-driven structure placement and registration. |
| `datagen` | Generated block states, models, tags, language entries, and recipes. |
| `event` | Gameplay event handlers, such as Yool interactions. |

The central classes to start reading are `MachineFrameworkBlockEntity`, `BaseModuleItem`, `ProcessingModuleItem`, `LogisticsModuleItem`, and `IoModuleItem`.

## Machine and Module Runtime

`MachineFrameworkBlockEntity` owns three related pieces of state:

- A 27-slot module grid (`GRID_SIZE`), arranged as three rows by nine columns.
- A 27-slot internal item inventory, with module-aware constraints.
- Four integer data channels for each module slot (`NUM_CHANNELS`). Modules use these channels for small per-slot runtime values such as progress and mode state.

Each server tick processes installed modules. IO input and output attempts run at the configured interval; processing modules run through their module tick hooks. The normal block-entity ticker only runs when Minecraft ticks the machine's own chunk. IO additionally checks that the adjacent target chunk is inside the block-ticking area before requesting its item capability. The mod does not add chunk loading.

Machine state is serialized by `saveAdditional` and restored by `loadAdditional`. The saved data includes modules, internal inventory, module channels, owner UUID, and public/private access state. The block entity exposes its item handler through NeoForge's item-handler capability.

## Menus and Network Updates

The machine configuration menu edits the module grid. Module-specific configuration can use the generic module menu or a module-provided menu/screen. Client UI actions send serverbound payloads registered in `PraeteritumFuturumMod`; packet handlers validate the player and target before applying changes. Block-entity state is synchronized back to clients through block-entity update data.

Keep client-only classes and APIs in the client entry point or client packages so dedicated servers do not load client classes.

## Piston Integration and Data Safety

`PistonBaseBlockMixin` handles piston movement when the Machine Framework block is part of the resolved movement. It allows this specific block entity to be pushable, resolves the full piston structure (including sticky branches), and checks required chunks before allowing a move that could affect a machine. If a required destination is not loaded, the move is rejected before machine data is changed.

For valid moves, machine block-entity data is carried through the piston moving block entity using the accessor in `accessor`. `PistonBlockEntityMixin` restores that data when movement finishes. The mixins leave ordinary piston operations to vanilla when no Machine Framework block is involved. Development-only fixtures under `gametest` exercise cases that are difficult to reproduce reliably by walking between chunks.

Detailed manual layouts, expected outcomes, and log interpretation are in [Piston Test Scenarios](PISTON_TEST_SCENARIOS.md) and [Piston Troubleshooting](PISTON_TROUBLESHOOTING.md).

## Useful Source Locations

- Registrations: `block/ModBlocks.java`, `block/entity/ModBlockEntities.java`, `item/ModItems.java`, `screen/ModMenuTypes.java`, and `recipe/ModRecipeSerializers.java`.
- Machine state and tick flow: `block/entity/MachineFrameworkBlockEntity.java`.
- Module contracts and examples: `api/module/` and `item/custom/`.
- Network payload registration: `PraeteritumFuturumMod.java`; payload implementations are in `network/`.
- Piston movement preservation: `mixin/` and `accessor/`.
- Automated piston regressions: `gametest/` and `src/main/resources/data/praeteritum_futurum/structure/`.
- Config defaults and limits: `Config.java`.