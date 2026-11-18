# Developer Documentation

This folder contains technical documentation for contributors and third-party mod developers. For player-facing documentation, see the [GitHub Wiki](https://github.com/TheXTrunner/praeteritum_futurum/wiki).

## Documentation Structure

| Audience | Location | Content |
|---|---|---|
| **Players** | [GitHub Wiki](https://github.com/TheXTrunner/praeteritum_futurum/wiki) | Getting started, module reference, quick start guide |
| **Contributors** | `docs/` (this folder) | Code architecture, API reference, internal roadmap |
| **Third-party modders** | `docs/MODULE-API-GUIDE.md` | Full API guide for creating custom modules |

## Files in This Folder

| File | Purpose |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Code-oriented map: package structure, class relationships, runtime behavior |
| [MODULE-API-GUIDE.md](MODULE-API-GUIDE.md) | Step-by-step guide for third-party modders creating custom modules |
| [MODULE-IDEAS.md](MODULE-IDEAS.md) | Internal roadmap of planned modules (not public-facing) |

## Keeping Docs in Sync

The wiki mirrors the architecture and API guide for public consumption. When you make changes to the code:

1. Update the relevant file in `docs/` first (source of truth).
2. Apply the same changes to the corresponding wiki page so the public wiki stays accurate.
3. The wiki repo is cloned at `E:\Projects\praeteritum_futurum.wiki` - push changes there after updating `docs/`.

## Contributor Workflow

1. Read the [project README](../README.md) for the mod overview.
2. Follow [CONTRIBUTING.md](../CONTRIBUTING.md) to set up, build, and run the project.
3. Open [ARCHITECTURE.md](ARCHITECTURE.md) to locate the code you need to change.
4. Open [MODULE-API-GUIDE.md](MODULE-API-GUIDE.md) if you are creating a custom module from another mod.
