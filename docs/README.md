# Developer Documentation

This folder contains technical documentation for contributors and third-party mod developers. For player-facing documentation, see the [GitHub Wiki](https://github.com/TheXTrunner/praeteritum_futurum/wiki).

## Documentation Structure

| Audience | Location | Content |
|---|---|---|
| **Players** | [GitHub Wiki](https://github.com/TheXTrunner/praeteritum_futurum/wiki) | Getting started, module reference, redstone guide, quick start |
| **Contributors and code readers** | `docs/` (this folder) | Code architecture, API reference, redstone implementation, internal roadmap |
| **Third-party modders** | `docs/MODULE-API-GUIDE.md` | Full API guide for creating custom modules |

## Files in This Folder

| File | Purpose |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Code-oriented map: package structure, class relationships, runtime behavior |
| [MODULE-API-GUIDE.md](MODULE-API-GUIDE.md) | Step-by-step guide for third-party modders creating custom modules |
| [MODULE-IDEAS.md](MODULE-IDEAS.md) | Internal roadmap of planned modules (not public-facing) |
| [REDSTONE-MODULE.md](REDSTONE-MODULE.md) | Redstone circuit system: architecture, components, signal propagation, and UI |
| [REDSTONE-TESTS.md](REDSTONE-TESTS.md) | Player playtest scenarios, edge-case checks, and issue-reporting guidance |

## Contributor Workflow

1. Read the [project README](../README.md) for the mod overview.
2. Follow [CONTRIBUTING.md](../CONTRIBUTING.md) to set up, build, and run the project.
3. Open [ARCHITECTURE.md](ARCHITECTURE.md) to locate the code you need to change.
4. Open [MODULE-API-GUIDE.md](MODULE-API-GUIDE.md) if you are creating a custom module from another mod.
