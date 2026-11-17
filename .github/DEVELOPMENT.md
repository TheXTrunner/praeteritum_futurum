# Internal Development Guide

> **Internal use only.** This document tracks development processes, versioning strategy, branching workflow, and feature planning.

---

## Table of Contents

**Development Process:**
1. [Versioning Strategy](#versioning-strategy)
2. [Branching Workflow](#branching-workflow)
3. [Release Process](#release-process)
4. [Git Commands Reference](#git-commands-reference)

**Feature Planning:**
5. [Deferred Features Tracking](#deferred-features-tracking)
6. [In-Game Documentation Plan](#in-game-documentation-plan-targeting-100)
7. [Module Implementation Details](#module-implementation-details)
8. [Blueprint String Sharing](#blueprint-string-sharing)

---

## Versioning Strategy

This project uses **Semantic Versioning (SemVer)**: `MAJOR.MINOR.PATCH` (e.g., `0.1.2`).

### Version Number Meaning

| Part | When it increases | What it means |
|---|---|---|
| **MAJOR** (0 → 1) | Major, stable release | The mod is feature-complete and stable. Breaking changes reset to 0. |
| **MINOR** (0.1 → 0.2) | New features, non-breaking | Adds new functionality without breaking existing setups. |
| **PATCH** (0.1.1 → 0.1.2) | Bug fixes, small improvements | Fixes bugs, small tweaks, no new major features. |

### Current Versioning Phase

The project is in the **initial development phase** (`0.x`). The leading `0.` means the API and core mechanics can still change.

### Release Examples

**0.1.x series** — Minor feature additions:
- `0.1.1` — Released: Ruined Outpost structures, Salvage Framing and Advanced Salvage blocks, Anomaly Compass, Salvage Cutter and Yool wrench tools, Salvage Scrap and Refined Scrap materials, Strange Artifact, Machine Framework block entity, Inventory Module, directional IO modules, Ghost item filter containers, Smelting/Stonecutter/Composter modules
- `0.1.2` — Released: Brewing/Cartography/Anvil/Grindstone modules, Blueprint system, Public/Private toggle, Filter blacklist mode, Module API, config system changes, title screen splash texts
- `0.1.3` — Released: Blast/Smoker/Ejection/Jukebox modules, GridDirection refactor, adjacent-module detection infrastructure
- `0.1.4` — Could add another module (e.g., Crafter Module)

**0.2.x series** — Bigger feature sets:
- `0.2.0` — Could add a group of related modules (e.g., fluid system + advanced IO)
- `0.2.1` — Bug fixes for 0.2.0

**1.0.0** — The stable showcase release:
- Includes Block Manipulator + Piston push/pull + Redstone Module
- Signals that the mod's core API is stable
- Future breaking changes would go to `2.0.0`

### When to Bump Each Number

- **PATCH** (0.1.2 → 0.1.3): One new module or several bug fixes
- **MINOR** (0.1.x → 0.2.x): A group of features that change how the mod feels (e.g., fluid system, new machine types)
- **MAJOR** (0.x → 1.0): The mod is stable, feature-complete for its core vision, and the API is confident

---

## Branching Workflow

### Branch Types

| Branch | Purpose | Example |
|---|---|---|
| `main` | Always matches the latest release | Current stable state |
| `release/<version>` | Frozen release branch for publishing | `release/0.1.2` |
| `dev` | Long-term development branch for major features | Block Manipulator + Piston + Redstone Module (targeting 1.0.0) |
| `feature/<name>` | Temporary branch for individual features | `feature/crafter-module` |

### Branch Lifecycle

1. **`main`** is the source of truth for the latest release. It should always build and run.
2. **`release/<version>`** branches are created from `main` when preparing a release. They are frozen after creation and used only for building the release jar.
3. **`dev`** is a long-term branch for features that are not yet ready for release. It may contain incomplete or experimental code.
4. **`feature/<name>`** branches are created from `main` for individual features. Once tested, they are merged back to `main`.

### Typical Workflow for a New Feature (0.1.x)

1. Create a feature branch from `main`: `git switch -c feature/crafter-module`
2. Develop the feature (code, tests, resources)
3. Test thoroughly in-game
4. Merge back to `main`: `git switch main; git merge feature/crafter-module`
5. When ready to release, create a release branch: `git switch -c release/0.1.3`
6. Build and publish the jar from the release branch
7. Switch back to `main` for continued development

### Typical Workflow for Major Features (1.0.0)

1. Develop on `dev` branch
2. When features are polished, merge `dev` into `main`: `git switch main; git merge dev`
3. Create release branch from `main`: `git switch -c release/1.0.0`
4. Build and publish

---

## Release Process

### Pre-Release Checklist

1. Ensure `main` branch builds cleanly (`./gradlew.bat build`)
2. Verify `CHANGELOG.md` reflects all changes for this version
3. Check that no deferred features are accidentally included
4. Update `mod_version` in `gradle.properties`

### Creating a Release Branch

1. Switch to `main`: `git switch main`
2. Pull latest changes: `git pull`
3. Create release branch: `git switch -c release/<version>`
4. Remove any deferred features (source files, generated resources, data generator references)
5. Update `gradle.properties` version if not already done
6. Verify build: `./gradlew.bat build`
7. Commit all changes
8. Switch back to `main`: `git switch main`

### Building the Release Jar

1. Switch to the release branch: `git switch release/<version>`
2. Build: `./gradlew.bat build`
3. The jar will be in `build/libs/praeteritum_futurum-<version>.jar`
4. Test the jar in a fresh Minecraft instance
5. Upload to GitHub Releases

### GitHub Release Steps (Automated)

Publishing to CurseForge, Modrinth, and GitHub Releases is fully automated via GitHub Actions. When you push a tag, the workflow builds the mod and uploads it to all three platforms.

1. Create a tag: `git tag v0.1.3`
2. Push the tag: `git push origin v0.1.3`
3. GitHub Actions will automatically:
   - Build the mod jar
   - Create a GitHub Release with the jar attached
   - Publish to CurseForge (project ID: 1774651)
   - Publish to Modrinth (project ID: praeteritum-futurum)
4. Check the Actions tab on GitHub to verify the release job succeeded

### Publishing Configuration

The publishing setup uses two Gradle plugins:

- **CurseForgeGradle** (`net.darkhax.curseforgegradle`) — uploads to CurseForge
- **Minotaur** (`com.modrinth.minotaur`) — uploads to Modrinth

Both are configured in `build.gradle` and triggered by the `.github/workflows/build.yml` release job.

**GitHub Secrets required:**

| Secret Name | Where to get it | Used by |
|---|---|---|
| `CURSEFORGE_API_KEY` | https://curseforge.com/account → API Keys | CurseForgeGradle |
| `MODRINTH_TOKEN` | https://modrinth.com/user/thextrunner/settings → Access Tokens | Minotaur |

**To add secrets to GitHub:**
1. Go to your repository: https://github.com/TheXTrunner/praeteritum_futurum/settings/secrets/actions
2. Click "New repository secret"
3. Enter the secret name (e.g., `CURSEFORGE_API_KEY`)
4. Paste the API key/token value
5. Click "Add secret"
6. Repeat for the other secret

**⚠️ Never share API keys in chat, commits, or files.** They are stored encrypted in GitHub Secrets and only accessible to GitHub Actions.

**Manual publishing (local testing):**
You can also publish manually from your machine by setting the environment variables:
```
set CURSEFORGE_API_KEY=your_key_here
./gradlew.bat curseforge
```
```
set MODRINTH_TOKEN=your_token_here
./gradlew.bat modrinth
```

---

## Git Commands Reference

| Command | What it does |
|---|---|
| `git branch` | List all branches (`*` marks current branch) |
| `git switch main` | Switch to main branch |
| `git switch dev` | Switch to dev branch |
| `git switch release/0.1.2` | Switch to a release branch |
| `git status` | See what files have changed |
| `git add <file>` | Stage a file for commit |
| `git commit -m "message"` | Create a commit with a message |
| `git push` | Push commits to GitHub |
| `git pull` | Pull latest changes from GitHub |
| `git log --oneline` | View commit history (short format) |

---

# Feature Planning

> This section tracks upcoming features, implementation details, and design decisions. Module summaries live in `docs/MODULE-IDEAS.md`; reference here for code patterns, API details, and design rationale.

---

## Deferred Features Tracking

### Currently Deferred (on `dev` branch, targeting 1.0.0)

| Feature | Status | Files |
|---|---|---|
| **Block Manipulator Module** | Refactored, placement/breaking engines complete | `BlockManipulatorModuleItem.java`, `BlockManipulatorPlacementEngine.java`, `BlockManipulatorBreakingEngine.java` |
| **Piston Push/Pull** | Mixins and game tests complete | `PistonBaseBlockMixin.java`, `PistonBlockEntityMixin.java`, `PistonBlockEntityPatch.java`, game tests |
| **Redstone Module** | Circuit grid, config screen, component types complete | `RedstoneModuleItem.java`, `RedstoneCircuit.java`, `RedstoneComponent.java`, config screen |

### Future Modules (on `dev` branch, post-1.0.0)

See `docs/MODULE-IDEAS.md` for the full list of proposed modules. These are not yet implemented and are for planning purposes only.

---

## In-Game Documentation Plan (Targeting 1.0.0)

Before 1.0.0, the mod should have in-game documentation so players can learn mechanics without leaving the game. This section tracks the plan.

### Why It Matters

Currently, all documentation is external (GitHub README, docs folder, CHANGELOG). A player playing the mod has no way to learn what modules do without leaving the game. Competing mods solve this with:

- **Create mod:** Ponder system — visual in-game tutorials for every block/item
- **Applied Energistics 2:** GuideME — an in-game guidebook with pages for every mechanic

### Approach Options (To Decide)

1. **Rich tooltips (Low effort, immediate win):** Add descriptive tooltips to every module item explaining what it does, its modes, and how to configure it. This is the minimum viable in-game documentation.

2. **JEI/REI plugin (Medium effort):** Create a JEI/REI plugin that shows module descriptions alongside recipes. Most players already have JEI/REI installed, so this reaches them where they already look.

3. **GuideME guidebook (High effort, post-1.0.0):** Integrate GuideME to provide an in-game book with pages for every mechanic. This is the AE2 approach and provides the best player experience but requires significant setup.

4. **Ponder-like system (Very high effort, post-1.0.0):** Create visual in-game tutorials like Create's Ponder system. This is the gold standard but requires custom rendering and animation work.

### Recommended Path

- **Now (pre-1.0.0):** Implement rich tooltips on all module items. The README already has one-line descriptions for each module; those should live in the item tooltips too.
- **Post-1.0.0:** Evaluate JEI/REI plugin integration or GuideME based on community feedback and available time.

### Implementation Notes

- Tooltips should include: module name, brief description, available modes, and any special requirements (e.g., "Requires adjacent IO module").
- Consider using `Component` formatting for colored, structured tooltips.
- Reference existing tooltip implementations in the codebase (e.g., `AnomalyCompassItem`, `YoolItem`) for style consistency.

---

## Module Implementation Details

Technical implementation notes for proposed modules. The public-facing `docs/MODULE-IDEAS.md` contains only brief summaries and GitHub issue links — this section holds the code patterns, API references, Java snippets, and design decisions for when development begins.

**Do not duplicate module summaries here.** Keep the summaries in `docs/MODULE-IDEAS.md` and only put implementation-specific details in this section.

### Entity Inventory Detection

Allow IO modules to detect and interact with entity inventories (e.g., minecarts with chests, hoppers on minecarts) in addition to block inventories.

**Current limitation:** IO modules currently query only `Capabilities.ItemHandler.BLOCK` for adjacent block positions, ignoring any entities occupying those positions.

**Reference implementation:** In `MachineFrameworkBlockEntity.java`, both `pullItemsFromSide()` and `pushItemsToSide()` would need to also query `Capabilities.ItemHandler.ENTITY` for entities at the target position.

**Reference methods:**

For `pullItemsFromSide()`, after the block capability check:
```java
// Check entities at the target position
BlockPos sourcePos = worldPosition.relative(side);
List<Entity> entities = level.getEntities(null, AABB.ofBlock(sourcePos));
for (Entity entity : entities) {
    IItemHandler entityHandler = entity.getCapability(
        Capabilities.ItemHandler.ENTITY,
        side.getOpposite()
    );
    if (entityHandler != null) {
        IItemHandler targetHandler = new SingleSlotItemHandler(inventoryHandler, adj);
        transferItems(entityHandler, targetHandler, moduleSlot, adj);
    }
}
```

For `pushItemsToSide()`, after the block capability check:
```java
// Check entities at the target position
BlockPos targetPos = worldPosition.relative(side);
List<Entity> entities = level.getEntities(null, AABB.ofBlock(targetPos));
for (Entity entity : entities) {
    IItemHandler entityHandler = entity.getCapability(
        Capabilities.ItemHandler.ENTITY,
        side.getOpposite()
    );
    if (entityHandler != null) {
        IItemHandler sourceHandler = new SingleSlotItemHandler(inventoryHandler, adj);
        transferItems(sourceHandler, entityHandler, moduleSlot, adj);
    }
}
```

**Considerations:**
- Entity capability queries may be more expensive than block queries; consider caching or throttling.
- Need to handle entity movement — an entity present during one tick may move away by the next.
- Should entity transfers respect the same chunk-ticking boundaries as block transfers?
- May want to limit to specific entity types (e.g., only minecarts) to avoid unexpected interactions.

### RC Module (Remote Signal)

**Implementation notes:**
- Requires network packets for pairing synchronization and signal transmission between modules.
- Could use a simple linking item to establish pairs.
- Consider signal delay based on distance for realism, or instant transmission for simplicity.
- Design option: standalone module item that pairs with another RC module instance.

### Loom Module

**Requires new item:** Color Mix — crafted by combining all three primary color dyes (red, blue, yellow/lime), acting like a printer cartridge. Needs its own texture, recipe, and registration.

**Banner pattern handling:** The module needs to read `DataComponents.BANNER_PATTERNS` from the source banner and apply it to a new banner of a different base color. This requires creating a `BannerPatternLayers` instance from the source's patterns, which is not directly accessible in NeoForge 1.21.1; may need reflection or a workaround similar to the Brewing module's hardcoded approach.

**UI:** The overlay should show a fuel bar for the Color Mix level, similar to the Smelting module.

### Grinder Module

**Damage scaling options:**
- Fixed damage per hit (e.g., 1 heart per tick).
- Damage tied to a fuel/charge item consumed from an input slot.
- Damage scaling with the number of grinder modules in a machine grid.
- Configurable damage via a config slot item (e.g., different tools or materials set the damage level).

**XP drop implementation options:**
- Should the module capture XP orbs from entity deaths and store them as an item (e.g., XP Bottle or custom XP storage item)?
- Should XP be captured from damage dealt, or only from entity kills?
- How to handle XP orb entity collection — scanning for nearby `ItemEntity`-like XP orbs and converting them to inventory items?
- Should there be a way to output stored XP (e.g., to an Enchanting module or as XP bottles)?

**Implementation approach:**
- Will need entity detection similar to the Ejection Module.
- Damage application can use `LivingEntity.hurt()` with a custom damage source.
- XP capture requires detecting `XPOrb` entities near the machine and converting them to an inventory-storable form.
- Consider performance implications of per-tick entity scanning; may need throttling or range limits.

---

## Blueprint String Sharing

Allow players to share Blueprint configurations outside the game as encoded strings, so they can paste them in Discord, chat, or forums instead of explaining setups with screenshots.

### The Problem

Currently, if a player wants to share a machine setup, they must either:
1. Explain the module positions and configs verbally
2. Take screenshots of each module's configuration
3. Walk someone through the setup in person

None of these are efficient. The Blueprint system already copies entire machine configs, but only within the same world instance.

### How Other Mods Handle It

| Mod | Method | Format |
|-----|--------|--------|
| **Blueprint Strings Mod** | JSON string copy/paste | Raw JSON, no compression |
| **WorldEdit** | Paste service (EngineHub) | Upload to server, get short URL |
| **Create** | Schematic upload (createmod.com) | Upload to dedicated website, clickable link |

### Recommended Approach: GZIP + Base64 String

**Why GZIP + Base64:**
- Blueprint data is small (a few modules times a few config values each)
- After compression, a typical Blueprint is ~700-1100 characters
- No external dependencies — Java has `GZIPOutputStream` and `Base64` built-in
- No server needed — the string is self-contained
- Short enough to paste in Discord or chat without issues

**String format:**
- Prefix for validation: `PF-BP:` (Praeteritum Futurum Blueprint)
- Example output: `§7Blueprint: §fPF-BP:H4sIAAAAAAAA...`
- The prefix allows quick validation before decoding

### Implementation Options

**Option A: Chat Commands (Simplest, recommended start)**
- `/blueprint export` — copies encoded string to clipboard
- `/blueprint import <string>` — decodes and applies to held Blueprint item
- No new UI needed

**Option B: GUI-Based (More polished)**
- Add "Export" and "Import" buttons to the Blueprint item screen
- Export shows the string in a text field for copying
- Import opens a text input field for pasting

**Option C: Both**
- Chat commands for quick sharing
- GUI for a better user experience

### String Length Estimate

For a typical 5-module machine:
- **Raw NBT:** ~2-3 KB
- **GZIP compressed:** ~500-800 bytes
- **Base64 encoded:** ~700-1100 characters

### Implementation Steps

1. Add two methods to `BlueprintItem.java`: `encodeToBase64(ItemStack)` and `decodeFromBase64(String)`
2. Add two chat commands: `/blueprint export` and `/blueprint import <string>`
3. Add the `PF-BP:` prefix for validation so players know it's a Praeteritum Futurum Blueprint
4. Handle error cases: invalid prefix, corrupted data, version mismatch

---

## Notes

- The `.github/` directory is for internal development documentation only. It is not included in the public docs index (`docs/README.md`).
- If any section of this guide becomes public-facing, move it to `docs/` and update `docs/README.md` accordingly.
- Keep this document updated when the branching strategy or release process changes.
