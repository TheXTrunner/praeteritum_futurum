# Internal Development Guide

> **Internal use only.** This document tracks development processes, versioning strategy, and branching workflow. It is not part of the public documentation.

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
- `0.1.3` — Could add a single new module (e.g., Crafter Module)
- `0.1.4` — Could add another module (e.g., Dispenser Module)

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

### GitHub Release Steps

1. Create a tag: `git tag v0.1.2`
2. Push the tag: `git push origin v0.1.2`
3. On GitHub, create a new release from the tag
4. Attach the jar file
5. Copy the relevant section from `CHANGELOG.md` into the release body
6. Assign related issues to the release's milestone

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

## Notes

- The `.github/` directory is for internal development documentation only. It is not included in the public docs index (`docs/README.md`).
- If any section of this guide becomes public-facing, move it to `docs/` and update `docs/README.md` accordingly.
- Keep this document updated when the branching strategy or release process changes.
