---
sessionId: session-261111-123210-11h3
---

# Current State Assessment

## Praeteritum Futurum — Project Audit

### What the mod does
A modular machine and logistics mod for Minecraft 1.21.1 on NeoForge. Players place a **Machine Framework** block and configure it with modules (Inventory, IO, Filter, Smelting, Stonecutter, Composter, Grindstone) to create custom automation pipelines. Includes structures (Ruined Outpost), tools (Salvage Cutter, Yool wrench, Anomaly Compass), and a Blueprint system for copying configurations.

### Architecture summary
- **64 Java source files** in `src/main/java/com/thextrunner/praeteritum_futurum/`
- Packages: `block/`, `block/entity/`, `item/custom/`, `screen/`, `network/`, `inventory/`, `datagen/`, `worldgen/`, `recipe/`, `client/`, `event/`, `api/module/`
- Custom packet-based networking (6 serverbound packets) for client-server sync
- Inline overlay UI system for module configuration (no full container screens for most modules)
- DeferredRegister-based registration for blocks, items, block entities, menu types, recipe serializers, and structure placement types
- Data generators for block states, item models, languages, tags, and recipes
- Config system with 3 values (Yool rotation toggle, IO tick rate, structure spawn rate)
- Custom `ConfigDrivenRandomSpreadPlacement` for config-driven structure spawning

### Strengths
- **Exceptional documentation** — nearly every class, method, and field has detailed Javadoc explaining purpose, flow, and design rationale
- **Clean modular API** — `IConfigurableModule`, `IModuleMenuProvider`, `ICustomScreenProvider` interfaces enable extensible module design
- **Proper NeoForge patterns** — DeferredRegister, capabilities, packet handling, data generators all follow current best practices
- **Config safety** — safe accessor methods prevent NPE if config isn't built yet
- **Transparency** — README acknowledges AI collaboration, `Gemini/` directory tracks development history

### Issues identified
The following sections detail areas for improvement, organized by priority and category.

# Improvement Areas

## 1. Project Hygiene & Repository Cleanup

### Files to retain (user confirmed)
- `machine_framework_front(old).png` and `machine_framework_old.png` — old machine framework textures, kept for reference
- `GETROTATED.py` — Python script that generates the 32 Anomaly Compass frames from `anomaly_base.png` and `anomaly_needle.png` using PIL nearest-neighbor rotation. This is a simple and effective approach; replacing it with a Gradle task would add unnecessary complexity.

### `.gitignore` gaps
- `.venv/` (Python virtual environment) is not ignored — contains hundreds of files
- `blockbench/` directory should be reviewed — Blockbench project files are large and may not need to be tracked
- `Gemini/` directory should be reviewed for size — devlog screenshots can be large
- `update.json` in root is fine but should be verified it's the correct update checker format

### License inconsistency
- `gradle.properties`: `mod_license=All Rights Reserved`
- `neoforge.mods.toml`: `license="MIT"`
- These should match. The README doesn't specify a license either.

## 2. Code Quality

### Hardcoded magic numbers
- `MachineFrameworkBlockEntity.java`: grid size `27` and channel count `4` repeated throughout (lines 45, 51, 59, 83, 124, 182-183, 200)
- Should be named constants like `GRID_SIZE = 27`, `NUM_CHANNELS = 4`

### Deprecated API usage
- `YoolInteractHandler.java` line 63: `state.rotate(Rotation.COUNTERCLOCKWISE_90)` is deprecated with a `@SuppressWarnings("deprecation")` — should investigate the modern replacement

### Fully qualified imports
- `MachineFrameworkBlockEntity.java` uses fully qualified class names inline (e.g., `com.thextrunner.praeteritum_futurum.item.custom.ProcessingModuleItem`) instead of imports — inconsistent style
- `IoModuleItem.java` same pattern

### Duplicate class naming
- `ModBlocks.java` exists in both `com.thextrunner.praeteritum_futurum` (root, doesn't exist on disk) and `com.thextrunner.praeteritum_futurum.block` — the root one appears to have been deleted but glob search still shows it. Verify cleanup is complete.

### `@SuppressWarnings("unused")` overuse
- `ICustomScreenProvider.java`, `BaseModuleItem.java` (2 methods), `YoolInteractHandler.java` — these suppressions may hide genuinely dead code

### Comment hygiene
- `MachineFrameworkBlockEntity.java` line 172: `//IT IS USED just that IntelliJ IDEA doesn't know where` — inline developer note should be removed or converted to a proper annotation

## 3. CI/CD Improvements

### GitHub Actions workflow is minimal
- `.github/workflows/build.yml` only runs `./gradlew build`
- Missing: upload build artifacts (the JAR), publish on release tags, run data generation validation
- No `jar` task output is preserved

### No release automation
- No automated publishing to CurseForge, Modrinth, or GitHub Releases
- `update.json` exists for in-game update checking but isn't automated

## 4. Transparency & Documentation

### README improvements
- Backlog section is comprehensive but could be converted to GitHub Issues for better tracking
- No LICENSE file in the repository root
- No contribution guidelines beyond a single paragraph
- No changelog or version history

### Missing mod documentation
- No in-game recipe book integration beyond what datagen provides
- JEI integration for custom recipes (DataCleaningRecipe) should be verified
- No wiki or external documentation for the module system API

## 5. Testing & Validation

### No test infrastructure
- No unit tests or integration tests
- No GameTest (NeoForge gametest server is configured but no tests written)
- Data generation output should be validated (run `runData` and commit results)

### Config validation
- `STRUCTURE_SPAWN_RATE` at `0.0` produces spacing of `999999` — this should be documented in the config comment as "effectively disabled" rather than truly zero

## 6. Feature Completeness

### Partially implemented features
- Structure spawn rate config: the `ConfigDrivenRandomSpreadPlacement` is implemented and the JSON references it — this appears **complete** now, but the README still says it's "not yet connected to runtime"
- Machine rotation: listed in backlog as "if needed"
- Config file: partially done per README, but all 3 config values appear wired now

### Resource completeness
- All items have English language entries
- No additional language files (could be a future goal)
- Block models use simple cube_all — no custom 3D models for the machine framework

## 7. Security & Networking

### No packet validation
- All 6 serverbound packets trust client input without validation (e.g., `ServerboundToggleIoFacePacket` doesn't verify the player can actually reach the block)
- Should add distance checks and permission validation

### Protocol versioning
- Network registrar uses version `"1"` — should increment on breaking packet changes

## 8. Performance Considerations

### `getAdjacentSlots()` creates new objects every call
- Returns `int[]` via `neighbors.stream().mapToInt(i -> i).toArray()` — allocates ArrayList + stream + array each tick
- Could use a pre-allocated array or return indices directly

### `getEffectiveFaceMode()` iterates all 27 slots
- Called frequently during IO operations — could cache the result

### `drawConfigButton()` loads textures every frame
- `graphics.blit()` with `TextureAtlasSprite` lookup happens every render frame for each IO button — sprites could be cached

# Delivery Steps

### ✓ Step 1: Clean up repository hygiene
Fix .gitignore, resolve the license inconsistency, and update documentation.
- Add `.venv/` to `.gitignore` to exclude the Python virtual environment
- Change `mod_license` in `gradle.properties` from `All Rights Reserved` to `MIT` to match `neoforge.mods.toml`
- Add an `LICENSE` file to the repository root with the MIT license text
- Update README backlog: mark structure spawn rate config as complete since `ConfigDrivenRandomSpreadPlacement` is now wired
- **Keep** `machine_framework_front(old).png`, `machine_framework_old.png`, and `GETROTATED.py` — user confirmed these are in use

### ✓ Step 2: Improve code quality
Address hardcoded magic numbers, deprecated APIs, and code style issues.
- Extract magic numbers in `MachineFrameworkBlockEntity.java` into named constants (`GRID_SIZE`, `NUM_CHANNELS`, etc.)
- Replace fully qualified class names with proper import statements in `MachineFrameworkBlockEntity.java` and `IoModuleItem.java`
- Investigate and replace the deprecated `rotate()` call in `YoolInteractHandler.java`
- Remove the inline developer comment on line 172 of `MachineFrameworkBlockEntity.java`
- Review all `@SuppressWarnings("unused")` annotations and either remove them or justify the usage

### ✓ Step 3: Add packet validation and security
Add distance and permission checks to all serverbound packets.
- Add a maximum reach distance check in each packet handler (e.g., player must be within 6 blocks of the machine)
- Verify the block entity still exists and belongs to the correct player context before applying changes
- Document the protocol versioning strategy in a comment in `PraeteritumFuturumMod.java`

### ✓ Step 4: Enhance CI/CD pipeline
Improve the GitHub Actions workflow to produce useful build artifacts.
- Add `./gradlew jar` to the build step and upload the resulting JAR as a build artifact
- Add a release job that triggers on tags and uploads to GitHub Releases
- Optionally add a step to validate data generation output (`./gradlew runData -- --server`)
- Consider adding a Spotless or Checkstyle step for code formatting validation

### ✓ Step 5: Improve transparency and documentation
Enhance project documentation for contributors and users.
- Create a `CHANGELOG.md` with the current version (0.1.1) and its features
- Convert README backlog items into GitHub Issues for better tracking
- Add a brief `CONTRIBUTING.md` with development setup instructions
- Document the module API (`IConfigurableModule`, `IModuleMenuProvider`, etc.) in a `docs/` folder or as Java package-info.java files
- Verify JEI integration for the `DataCleaningRecipe` custom recipe serializer