# Documentation Index

This page connects the project overview, contributor workflow, and focused guides.

## Choose a Starting Point

| If you want to... | Start here |
|---|---|
| Learn what the mod does or install it | [Project README](../README.md) |
| Set up a development environment or contribute code | [Contributing Guide](../CONTRIBUTING.md) |
| Create a module for another mod | [Module API Guide](MODULE-API-GUIDE.md) |
| Run or understand piston and simulation-distance tests | [Piston Test Scenarios](PISTON_TEST_SCENARIOS.md) |
| Diagnose piston or IO behavior from logs | [Piston Troubleshooting](PISTON_TROUBLESHOOTING.md) |
| Browse possible future modules | [Deferred Module Ideas](MODULE-IDEAS.md) |

## Suggested Workflow

1. Read the [README](../README.md) for the project overview and player-facing features.
2. If you plan to change the code, follow [CONTRIBUTING.md](../CONTRIBUTING.md) to set up, build, and run the project.
3. Open the focused guide for the area you are changing. For piston or IO changes, use the test scenarios and troubleshooting guide together.
4. Update the relevant guide when a code change changes player behavior, a test procedure, or the meaning of a log message.

## What Each Guide Contains

- **Module API Guide:** dependency setup, module base classes and interfaces, registration, resources, and working examples for third-party modders.
- **Piston Test Scenarios:** manual piston and simulation-distance procedures, expected outcomes, and the GameTest command for automated safeguards.
- **Piston Troubleshooting:** how to distinguish loaded from ticking chunks, interpret piston and IO logs, and understand the edge cases already exercised.
- **Deferred Module Ideas:** planning notes. These are proposals and should not be read as a list of implemented features.
